package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.comment.*;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.recommendation.*;
import com.pulsegpt.recommendation.dto.RecommendationGenerateRequest;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignment;
import com.pulsegpt.topic.TopicAssignmentRepository;
import com.pulsegpt.topic.TopicRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3I — Evidence-Grounded Content Recommendation Engine Integration Tests")
class ContentRecommendationIntegrationTest {

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
    private TopicRepository topicRepository;

    @MockitoBean
    private TopicAssignmentRepository topicAssignmentRepository;

    @MockitoBean
    private ContentRecommendationRepository recommendationRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User creatorA;
    private User creatorB;
    private String tokenA;
    private String tokenB;
    private Topic topicA;
    private ProcessedComment processedCommentA;

    @BeforeEach
    void setUp() {
        reset(aiServiceClient);

        creatorA = User.builder()
                .id(UUID.randomUUID())
                .email("creator.a@pulsegpt.dev")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .name("Creator Alpha")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        creatorB = User.builder()
                .id(UUID.randomUUID())
                .email("creator.b@pulsegpt.dev")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .name("Creator Beta")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        tokenA = jwtService.generateAccessToken(creatorA);
        tokenB = jwtService.generateAccessToken(creatorB);

        when(userRepository.findById(creatorA.getId())).thenReturn(Optional.of(creatorA));
        when(userRepository.findByEmail(creatorA.getEmail())).thenReturn(Optional.of(creatorA));
        when(userRepository.findById(creatorB.getId())).thenReturn(Optional.of(creatorB));
        when(userRepository.findByEmail(creatorB.getEmail())).thenReturn(Optional.of(creatorB));

        topicA = Topic.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .name("React Native Performance Optimization")
                .keywords(List.of("battery", "performance", "react-native", "memory"))
                .commentCount(25)
                .build();

        when(topicRepository.findByIdAndUserId(topicA.getId(), creatorA.getId())).thenReturn(Optional.of(topicA));
        when(topicRepository.findByUserIdAndActiveTrue(creatorA.getId())).thenReturn(List.of(topicA));

        RawComment rawComment = RawComment.builder()
                .id(UUID.randomUUID())
                .rawText("How do you stop battery drain with background geolocation in React Native?")
                .build();

        processedCommentA = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(rawComment)
                .normalizedText("How do you stop battery drain with background geolocation in React Native?")
                .sentimentLabel(SentimentLabel.NEUTRAL)
                .intent(IntentType.QUESTION)
                .sentimentScore(0.0)
                .language("en")
                .build();

        TopicAssignment assignment = TopicAssignment.builder()
                .id(UUID.randomUUID())
                .topic(topicA)
                .processedComment(processedCommentA)
                .clusterId(1)
                .isNoise(false)
                .membershipProbability(0.95)
                .build();

        when(topicAssignmentRepository.findByTopicId(topicA.getId())).thenReturn(List.of(assignment));
        when(postRepository.findByPlatformAccountUserIdOrderByPublishedAtDesc(eq(creatorA.getId()), any()))
                .thenReturn(List.of());
        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(creatorA.getId()))
                .thenReturn(List.of());

        when(recommendationRepository.save(any(ContentRecommendation.class))).thenAnswer(invocation -> {
            ContentRecommendation rec = invocation.getArgument(0);
            if (rec.getId() == null) {
                rec.setId(UUID.randomUUID());
            }
            return rec;
        });
    }

    @Nested
    @DisplayName("Evidence-Grounded Recommendation Generation Tests")
    class EvidenceGroundedGenerationTests {

        @Test
        @DisplayName("Should generate, validate, and persist recommendation with evidence snapshot")
        void testGenerateRecommendation_Success() throws Exception {
            String topicEvidenceId = "topic:" + topicA.getId();

            AiRecommendationDraft draft = AiRecommendationDraft.builder()
                    .title("5 React Native Battery Drain Fixes for Mobile Devs")
                    .contentType("VIDEO")
                    .angle("Practical profiling and background task troubleshooting")
                    .targetAudience("Mobile Engineers")
                    .problemAddressed("Unoptimized background geolocation draining user batteries")
                    .keyPoints(List.of("Throttle GPS polling intervals", "Use headless JS with caution"))
                    .hook("Is your mobile app draining user batteries in the background?")
                    .callToAction("Comment your biggest React Native bug below!")
                    .evidenceIds(List.of(topicEvidenceId))
                    .confidence(0.92)
                    .build();

            AiRecommendationGenerateResponse aiResponse = AiRecommendationGenerateResponse.builder()
                    .drafts(List.of(draft))
                    .modelName("gemini-1.5-flash")
                    .promptVersion("RECOMMENDATION_PROMPT_V1")
                    .generationMode("EVIDENCE_GROUNDED")
                    .build();

            when(aiServiceClient.generateRecommendations(any())).thenReturn(aiResponse);

            RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                    topicA.getId(), "VIDEO", "EDUCATIONAL", null, 1, GenerationMode.EVIDENCE_GROUNDED
            );

            mockMvc.perform(post("/recommendations/generate")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].title").value("5 React Native Battery Drain Fixes for Mobile Devs"))
                    .andExpect(jsonPath("$.data[0].status").value("VALIDATED"))
                    .andExpect(jsonPath("$.data[0].generationMode").value("EVIDENCE_GROUNDED"))
                    .andExpect(jsonPath("$.data[0].validationPassed").value(true))
                    .andExpect(jsonPath("$.data[0].evidenceSnapshot").isMap())
                    .andExpect(jsonPath("$.data[0].repairAttempted").value(false));

            verify(recommendationRepository, atLeastOnce()).save(any(ContentRecommendation.class));
        }

        @Test
        @DisplayName("Should execute ONE repair retry when initial draft contains unsupported claim, then succeed")
        void testGenerateRecommendation_RepairSuccess() throws Exception {
            String topicEvidenceId = "topic:" + topicA.getId();

            AiRecommendationDraft initialDraft = AiRecommendationDraft.builder()
                    .title("Why 95% of users uninstall battery draining apps") // Unsupported 95%
                    .contentType("VIDEO")
                    .angle("Practical profiling and background task troubleshooting")
                    .targetAudience("Mobile Engineers")
                    .problemAddressed("Battery drain")
                    .keyPoints(List.of("Audit background timers", "Configure GPS polling"))
                    .hook("Did you know 95% of users uninstall slow apps?")
                    .callToAction("Subscribe for more tips!")
                    .evidenceIds(List.of(topicEvidenceId))
                    .confidence(0.88)
                    .build();

            AiRecommendationDraft repairedDraft = AiRecommendationDraft.builder()
                    .title("Fixing Background Battery Drain in React Native Apps")
                    .contentType("VIDEO")
                    .angle("Practical profiling and background task troubleshooting")
                    .targetAudience("Mobile Engineers")
                    .problemAddressed("Battery drain")
                    .keyPoints(List.of("Audit background timers", "Configure GPS polling"))
                    .hook("Is your app draining battery in the background?")
                    .callToAction("Subscribe for more tips!")
                    .evidenceIds(List.of(topicEvidenceId))
                    .confidence(0.90)
                    .build();

            when(aiServiceClient.generateRecommendations(any())).thenReturn(
                    AiRecommendationGenerateResponse.builder()
                            .drafts(List.of(initialDraft))
                            .modelName("gemini-1.5-flash")
                            .promptVersion("RECOMMENDATION_PROMPT_V1")
                            .generationMode("EVIDENCE_GROUNDED")
                            .build()
            );

            when(aiServiceClient.repairRecommendation(any())).thenReturn(
                    AiRecommendationRepairResponse.builder()
                            .repairedDraft(repairedDraft)
                            .repairExplanation("Removed ungrounded statistical claim")
                            .build()
            );

            RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                    topicA.getId(), "VIDEO", "EDUCATIONAL", null, 1, GenerationMode.EVIDENCE_GROUNDED
            );

            mockMvc.perform(post("/recommendations/generate")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].status").value("VALIDATED"))
                    .andExpect(jsonPath("$.data[0].repairAttempted").value(true))
                    .andExpect(jsonPath("$.data[0].title").value("Fixing Background Battery Drain in React Native Apps"));

            verify(aiServiceClient, times(1)).repairRecommendation(any());
        }

        @Test
        @DisplayName("Should mark status as REJECTED when repaired recommendation fails validation again")
        void testGenerateRecommendation_RepairFails_MarkedRejected() throws Exception {
            AiRecommendationDraft initialDraft = AiRecommendationDraft.builder()
                    .title("Battery Drain Optimization")
                    .contentType("VIDEO")
                    .angle("Practical profiling and background task troubleshooting")
                    .targetAudience("Engineers")
                    .problemAddressed("Battery drain")
                    .keyPoints(List.of("Audit background timers", "Configure GPS polling"))
                    .hook("Is your app draining battery in the background?")
                    .callToAction("Subscribe for more tips!")
                    .evidenceIds(List.of("topic:FABRICATED_1")) // Fabricated ID
                    .confidence(0.88)
                    .build();

            AiRecommendationDraft repairedDraftStillBad = AiRecommendationDraft.builder()
                    .title("Battery Drain Optimization Repaired")
                    .contentType("VIDEO")
                    .angle("Practical profiling and background task troubleshooting")
                    .targetAudience("Engineers")
                    .problemAddressed("Battery drain")
                    .keyPoints(List.of("Audit background timers", "Configure GPS polling"))
                    .hook("Is your app draining battery in the background?")
                    .callToAction("Subscribe for more tips!")
                    .evidenceIds(List.of("topic:FABRICATED_2")) // Still fabricated!
                    .confidence(0.88)
                    .build();

            when(aiServiceClient.generateRecommendations(any())).thenReturn(
                    AiRecommendationGenerateResponse.builder()
                            .drafts(List.of(initialDraft))
                            .modelName("gemini-1.5-flash")
                            .promptVersion("RECOMMENDATION_PROMPT_V1")
                            .generationMode("EVIDENCE_GROUNDED")
                            .build()
            );

            when(aiServiceClient.repairRecommendation(any())).thenReturn(
                    AiRecommendationRepairResponse.builder()
                            .repairedDraft(repairedDraftStillBad)
                            .repairExplanation("Failed fix")
                            .build()
            );

            RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                    topicA.getId(), "VIDEO", "EDUCATIONAL", null, 1, GenerationMode.EVIDENCE_GROUNDED
            );

            mockMvc.perform(post("/recommendations/generate")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].status").value("REJECTED"))
                    .andExpect(jsonPath("$.data[0].repairAttempted").value(true))
                    .andExpect(jsonPath("$.data[0].validationPassed").value(false));
        }
    }

    @Nested
    @DisplayName("Query, Pagination & Multi-Tenant Isolation Tests")
    class QueryAndTenantIsolationTests {

        @Test
        @DisplayName("Should prevent cross-user recommendation access (Tenant Isolation returns 404)")
        void testCrossUserAccess_Returns404() throws Exception {
            UUID recId = UUID.randomUUID();

            when(recommendationRepository.findByIdAndUserId(recId, creatorB.getId()))
                    .thenReturn(Optional.empty());

            // Creator B tries to access recommendation -> 404 NOT FOUND
            mockMvc.perform(get("/recommendations/" + recId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should retrieve paginated recommendations scoped strictly to authenticated user")
        void testGetRecommendations_UserScoped() throws Exception {
            ContentRecommendation recA = ContentRecommendation.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .topic(topicA)
                    .title("Alpha Rec 1")
                    .contentType("VIDEO")
                    .status(RecommendationStatus.VALIDATED)
                    .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                    .confidence(0.9)
                    .validationPassed(true)
                    .createdAt(Instant.now())
                    .build();

            when(recommendationRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(recA)));

            // Creator A requests their recommendations
            mockMvc.perform(get("/recommendations?page=0&size=10")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].title").value("Alpha Rec 1"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }
}
