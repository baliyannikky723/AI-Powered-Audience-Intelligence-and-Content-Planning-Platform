package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.comment.*;
import com.pulsegpt.comment.dto.CommentProcessingTriggerRequest;
import com.pulsegpt.comment.service.CommentProcessingService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3G — AI / NLP Comment Processing Pipeline Integration Tests")
class CommentProcessingIntegrationTest {

    @TestConfiguration
    static class TestDataSourceConfig {
        @Bean
        @Primary
        public DataSource dataSource() throws SQLException {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(ds.getConnection(any(), any())).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(metaData);
            when(metaData.getConnection()).thenReturn(conn);
            when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(metaData.getDatabaseProductVersion()).thenReturn("16.0");
            when(metaData.getDatabaseMajorVersion()).thenReturn(16);
            when(metaData.getDatabaseMinorVersion()).thenReturn(0);
            when(metaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(metaData.getDriverVersion()).thenReturn("42.7.2");
            when(metaData.getDriverMajorVersion()).thenReturn(42);
            when(metaData.getDriverMinorVersion()).thenReturn(7);
            return ds;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CommentProcessingService commentProcessingService;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PlatformAccountRepository platformAccountRepository;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private RawCommentRepository rawCommentRepository;

    @MockitoBean
    private ProcessedCommentRepository processedCommentRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User userA;
    private User userB;
    private String tokenUserA;
    private String tokenUserB;

    private PlatformAccount platformAccountA;
    private Post postA;
    private RawComment rawCommentA1;
    private RawComment rawCommentA2;

    @BeforeEach
    void setUp() {
        reset(userRepository, platformAccountRepository, postRepository,
                rawCommentRepository, processedCommentRepository, auditLogRepository, aiServiceClient);

        userA = User.builder()
                .id(UUID.randomUUID())
                .name("Creator John")
                .email("john@creator.com")
                .passwordHash(passwordEncoder.encode("Password123!@#"))
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        userB = User.builder()
                .id(UUID.randomUUID())
                .name("Creator Bob")
                .email("bob@creator.com")
                .passwordHash(passwordEncoder.encode("Password123!@#"))
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        when(userRepository.findById(userA.getId())).thenReturn(Optional.of(userA));
        when(userRepository.findById(userB.getId())).thenReturn(Optional.of(userB));
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(userRepository.findByEmail(userB.getEmail())).thenReturn(Optional.of(userB));

        tokenUserA = jwtService.generateAccessToken(userA);
        tokenUserB = jwtService.generateAccessToken(userB);

        platformAccountA = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .user(userA)
                .platform(PlatformType.YOUTUBE)
                .externalAccountId("UC_channel_123")
                .accountName("Tech Insights")
                .status(PlatformAccountStatus.CONNECTED)
                .build();

        postA = Post.builder()
                .id(UUID.randomUUID())
                .platformAccount(platformAccountA)
                .externalPostId("video_xyz_999")
                .title("PostgreSQL Optimization")
                .publishedAt(Instant.now().minusSeconds(86400))
                .build();

        rawCommentA1 = RawComment.builder()
                .id(UUID.randomUUID())
                .post(postA)
                .externalCommentId("comment_001")
                .authorDisplayName("DevFan")
                .rawText("Awesome video! How do I index pgvector columns? Contact me at test@example.com")
                .publishedAt(Instant.now().minusSeconds(3600))
                .likes(5)
                .build();

        rawCommentA2 = RawComment.builder()
                .id(UUID.randomUUID())
                .post(postA)
                .externalCommentId("comment_002")
                .authorDisplayName("Viewer2")
                .rawText("Bad audio, couldn't hear anything properly.")
                .publishedAt(Instant.now().minusSeconds(1800))
                .likes(1)
                .build();
    }

    @Nested
    @DisplayName("1. Processing Pipeline & Raw Text Immutability Tests")
    class ProcessingPipelineTests {

        @Test
        @DisplayName("Batch comment processing succeeds and preserves RawComment rawText untouched")
        void testSuccessfulBatchProcessing() {
            String originalRawText1 = rawCommentA1.getRawText();
            String originalRawText2 = rawCommentA2.getRawText();

            when(rawCommentRepository.findUnprocessedByUserId(eq(userA.getId()), any()))
                    .thenReturn(List.of(rawCommentA1, rawCommentA2));

            AiModelVersions versions = new AiModelVersions("langdetect-1.0.9", "1.0.0", "baseline-v1", "all-MiniLM-L6-v2", "pulsegpt-nlp-v1");

            AiCommentProcessResponse response1 = new AiCommentProcessResponse(
                    rawCommentA1.getId().toString(),
                    "en",
                    false,
                    "Awesome video! How do I index pgvector columns? Contact me at [EMAIL_REDACTED]",
                    "awesome video! how do i index pgvector columns? contact me at [email_redacted]",
                    false,
                    0.0,
                    List.of(),
                    false,
                    true,
                    "Awesome video! How do I index pgvector columns? Contact me at [EMAIL_REDACTED]",
                    new AiSentimentResult(SentimentLabel.POSITIVE, 0.92),
                    new AiIntentResult(IntentType.QUESTION, 0.88),
                    Priority.HIGH,
                    List.of(0.123, -0.456, 0.789),
                    384,
                    versions,
                    true,
                    null
            );

            AiCommentProcessResponse response2 = new AiCommentProcessResponse(
                    rawCommentA2.getId().toString(),
                    "en",
                    false,
                    "Bad audio, couldn't hear anything properly.",
                    "bad audio, couldnt hear anything properly.",
                    false,
                    0.0,
                    List.of(),
                    false,
                    false,
                    "Bad audio, couldn't hear anything properly.",
                    new AiSentimentResult(SentimentLabel.NEGATIVE, 0.85),
                    new AiIntentResult(IntentType.COMPLAINT, 0.82),
                    Priority.HIGH,
                    List.of(-0.111, 0.222, -0.333),
                    384,
                    versions,
                    true,
                    null
            );

            when(aiServiceClient.processBatch(any()))
                    .thenReturn(new AiBatchCommentProcessResponse(List.of(response1, response2), 2, 2, 0));

            var summary = commentProcessingService.processComments(userA, new CommentProcessingTriggerRequest(100, null, null, null));

            assertThat(summary.getStatus()).isEqualTo("COMPLETED");
            assertThat(summary.getProcessedCount()).isEqualTo(2);
            assertThat(summary.getPiiMaskedCount()).isEqualTo(1);

            // Verify ProcessedComment was saved for both
            verify(processedCommentRepository, times(2)).save(any(ProcessedComment.class));

            // CRITICAL: Verify original RawComment rawText was not altered!
            assertThat(rawCommentA1.getRawText()).isEqualTo(originalRawText1);
            assertThat(rawCommentA2.getRawText()).isEqualTo(originalRawText2);
        }

        @Test
        @DisplayName("Zero unprocessed comments returns graceful completed summary")
        void testNoUnprocessedComments() {
            when(rawCommentRepository.findUnprocessedByUserId(eq(userA.getId()), any()))
                    .thenReturn(Collections.emptyList());

            var summary = commentProcessingService.processComments(userA, new CommentProcessingTriggerRequest(100, null, null, null));

            assertThat(summary.getStatus()).isEqualTo("COMPLETED");
            assertThat(summary.getProcessedCount()).isEqualTo(0);
            verify(aiServiceClient, never()).processBatch(any());
        }
    }

    @Nested
    @DisplayName("2. REST API & User Isolation Tests")
    class RestApiAndIsolationTests {

        @Test
        @DisplayName("POST /api/v1/processing/comments requires authentication (401)")
        void testUnauthenticatedRequest() throws Exception {
            mockMvc.perform(post("/processing/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/v1/processing/comments processes comments successfully for authenticated creator")
        void testAuthenticatedProcessingEndpoint() throws Exception {
            when(rawCommentRepository.findUnprocessedByUserId(eq(userA.getId()), any()))
                    .thenReturn(List.of(rawCommentA1));

            AiModelVersions versions = new AiModelVersions("langdetect-1.0.9", "1.0.0", "baseline-v1", "all-MiniLM-L6-v2", "pulsegpt-nlp-v1");

            AiCommentProcessResponse item = new AiCommentProcessResponse(
                    rawCommentA1.getId().toString(),
                    "en",
                    false,
                    "Cleaned text",
                    "cleaned text",
                    false,
                    0.0,
                    List.of(),
                    false,
                    false,
                    "Cleaned text",
                    new AiSentimentResult(SentimentLabel.POSITIVE, 0.9),
                    new AiIntentResult(IntentType.PRAISE, 0.85),
                    Priority.MEDIUM,
                    List.of(0.1, 0.2),
                    384,
                    versions,
                    true,
                    null
            );

            when(aiServiceClient.processBatch(any()))
                    .thenReturn(new AiBatchCommentProcessResponse(List.of(item), 1, 1, 0));

            mockMvc.perform(post("/processing/comments")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentProcessingTriggerRequest(50, null, null, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.processedCount").value(1));
        }

        @Test
        @DisplayName("Creator B cannot process Creator A's comments (Isolation)")
        void testUserIsolation() throws Exception {
            // When user B requests processing, repo queries for user B's ID only
            when(rawCommentRepository.findUnprocessedByUserId(eq(userB.getId()), any()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/processing/comments")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.processedCount").value(0));

            // Verify user A's comments were never touched
            verify(rawCommentRepository, never()).findUnprocessedByUserId(eq(userA.getId()), any());
        }
    }

    @Nested
    @DisplayName("3. Resilience & Error Handling Tests")
    class ResilienceAndErrorHandlingTests {

        @Test
        @DisplayName("Per-item failure in batch is handled gracefully without failing the whole run")
        void testPartialBatchFailure() {
            when(rawCommentRepository.findUnprocessedByUserId(eq(userA.getId()), any()))
                    .thenReturn(List.of(rawCommentA1, rawCommentA2));

            AiModelVersions versions = new AiModelVersions("langdetect-1.0.9", "1.0.0", "baseline-v1", "all-MiniLM-L6-v2", "pulsegpt-nlp-v1");

            AiCommentProcessResponse successItem = new AiCommentProcessResponse(
                    rawCommentA1.getId().toString(), "en", false, "Clean", "clean",
                    false, 0.0, List.of(), false, false, "Clean",
                    new AiSentimentResult(SentimentLabel.POSITIVE, 0.8),
                    new AiIntentResult(IntentType.FEEDBACK, 0.7),
                    Priority.MEDIUM, List.of(0.1), 384, versions, true, null
            );

            AiCommentProcessResponse failedItem = new AiCommentProcessResponse(
                    rawCommentA2.getId().toString(), "und", false, "", "",
                    false, 0.0, List.of(), false, false, "",
                    null, null, null, List.of(), 384, versions, false, "Model inference error"
            );

            when(aiServiceClient.processBatch(any()))
                    .thenReturn(new AiBatchCommentProcessResponse(List.of(successItem, failedItem), 2, 1, 1));

            var summary = commentProcessingService.processComments(userA, new CommentProcessingTriggerRequest(100, null, null, null));

            assertThat(summary.getStatus()).isEqualTo("PARTIAL");
            assertThat(summary.getProcessedCount()).isEqualTo(1);
            assertThat(summary.getFailedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("AI Service unavailable propagates safe ApiException")
        void testAiServiceUnavailable() {
            when(rawCommentRepository.findUnprocessedByUserId(eq(userA.getId()), any()))
                    .thenReturn(List.of(rawCommentA1));

            when(aiServiceClient.processBatch(any()))
                    .thenThrow(new ApiException("AI Service connection timed out or unavailable", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE"));

            var summary = commentProcessingService.processComments(userA, new CommentProcessingTriggerRequest(100, null, null, null));

            assertThat(summary.getStatus()).isEqualTo("FAILED");
            assertThat(summary.getFailedCount()).isEqualTo(1);
            assertThat(summary.getProcessedCount()).isEqualTo(0);
        }
    }
}
