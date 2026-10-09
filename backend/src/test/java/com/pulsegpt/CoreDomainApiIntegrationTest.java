package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.comment.SentimentLabel;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.PostRepository;
import com.pulsegpt.comment.RawComment;
import com.pulsegpt.comment.RawCommentRepository;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.platform.dto.UpdatePlatformAccountStatusRequest;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.topic.Topic;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Core REST APIs (Phase 3D) Domain & Security Integration Tests")
class CoreDomainApiIntegrationTest {

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
    private com.pulsegpt.audit.AuditLogRepository auditLogRepository;

    private User userA;
    private User userB;
    private String tokenUserA;
    private String tokenUserB;

    private PlatformAccount accountA;
    private Post postA;
    private RawComment rawCommentA;
    private ProcessedComment processedCommentA;
    private Topic topicA;

    @BeforeEach
    void setUp() {
        userA = User.builder()
                .id(UUID.randomUUID())
                .name("Creator User A")
                .email("usera@pulsegpt.ai")
                .passwordHash(passwordEncoder.encode("Password123!@#"))
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        userB = User.builder()
                .id(UUID.randomUUID())
                .name("Creator User B")
                .email("userb@pulsegpt.ai")
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

        accountA = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .user(userA)
                .platform(PlatformType.YOUTUBE)
                .externalAccountId("yt_channel_123")
                .accountName("Tech Talks Daily")
                .accessTokenEncrypted("ENC_SECRET_ACCESS_TOKEN")
                .refreshTokenEncrypted("ENC_SECRET_REFRESH_TOKEN")
                .status(PlatformAccountStatus.CONNECTED)
                .connectedAt(Instant.now())
                .build();

        postA = Post.builder()
                .id(UUID.randomUUID())
                .platformAccount(accountA)
                .externalPostId("yt_video_456")
                .title("Building AI Apps in Spring Boot")
                .url("https://youtube.com/watch?v=456")
                .viewsCount(50000L)
                .likesCount(2500L)
                .commentsCount(340L)
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        rawCommentA = RawComment.builder()
                .id(UUID.randomUUID())
                .post(postA)
                .externalCommentId("yt_comment_789")
                .authorDisplayName("DevFan99")
                .authorExternalId("UC_devfan99")
                .rawText("Amazing tutorial, please cover RAG next!")
                .likes(15)
                .replies(2)
                .publishedAt(Instant.now())
                .importedAt(Instant.now())
                .build();

        processedCommentA = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(rawCommentA)
                .normalizedText("Amazing tutorial, please cover RAG next!")
                .language("en")
                .sentimentLabel(SentimentLabel.POSITIVE)
                .sentimentScore(0.95)
                .intent(com.pulsegpt.comment.IntentType.FEATURE_REQUEST)
                .isSpam(false)
                .isDuplicate(false)
                .piiMasked(false)
                .processingVersion("v1.0.0")
                .processedAt(Instant.now())
                .build();

        topicA = Topic.builder()
                .id(UUID.randomUUID())
                .user(userA)
                .name("Spring Boot & AI")
                .description("Community discussions around AI integrations")
                .keywords(List.of("spring-ai", "rag", "llm"))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("1. Unauthenticated Access Protection")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("GET /platform-accounts without token returns 401 Unauthorized")
        void testPlatformAccountsUnauthenticated() throws Exception {
            mockMvc.perform(get("/platform-accounts"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("Unauthorized"));
        }

        @Test
        @DisplayName("GET /posts without token returns 401 Unauthorized")
        void testPostsUnauthenticated() throws Exception {
            mockMvc.perform(get("/posts"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("Unauthorized"));
        }

        @Test
        @DisplayName("GET /comments without token returns 401 Unauthorized")
        void testCommentsUnauthenticated() throws Exception {
            mockMvc.perform(get("/comments"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("Unauthorized"));
        }

        @Test
        @DisplayName("GET /comments/processed without token returns 401 Unauthorized")
        void testProcessedCommentsUnauthenticated() throws Exception {
            mockMvc.perform(get("/comments/processed"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("Unauthorized"));
        }

        @Test
        @DisplayName("GET /topics without token returns 401 Unauthorized")
        void testTopicsUnauthenticated() throws Exception {
            mockMvc.perform(get("/topics"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("Unauthorized"));
        }
    }

    @Nested
    @DisplayName("2. Platform Accounts API")
    class PlatformAccountApiTests {

        @Test
        @DisplayName("Creator can list own platform accounts without exposing sensitive tokens")
        void testListPlatformAccounts() throws Exception {
            when(platformAccountRepository.findByUserId(userA.getId()))
                    .thenReturn(List.of(accountA));

            mockMvc.perform(get("/platform-accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value(accountA.getId().toString()))
                    .andExpect(jsonPath("$.data[0].platform").value("YOUTUBE"))
                    .andExpect(jsonPath("$.data[0].accountName").value("Tech Talks Daily"))
                    .andExpect(jsonPath("$.data[0].status").value("CONNECTED"))
                    // Verify NO sensitive tokens leaked
                    .andExpect(jsonPath("$.data[0].accessTokenEncrypted").doesNotExist())
                    .andExpect(jsonPath("$.data[0].refreshTokenEncrypted").doesNotExist());
        }

        @Test
        @DisplayName("Creator can get own platform account by ID")
        void testGetPlatformAccountById() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userA.getId()))
                    .thenReturn(Optional.of(accountA));

            mockMvc.perform(get("/platform-accounts/{id}", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(accountA.getId().toString()))
                    .andExpect(jsonPath("$.data.externalAccountId").value("yt_channel_123"));
        }

        @Test
        @DisplayName("User B accessing User A's platform account returns 404 NOT FOUND (Cross-user isolation)")
        void testCrossUserPlatformAccountAccessReturns404() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/platform-accounts/{id}", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Creator can update status of own platform account")
        void testUpdatePlatformAccountStatus() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userA.getId()))
                    .thenReturn(Optional.of(accountA));
            when(platformAccountRepository.save(any(PlatformAccount.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UpdatePlatformAccountStatusRequest request = new UpdatePlatformAccountStatusRequest(PlatformAccountStatus.DISCONNECTED);

            mockMvc.perform(patch("/platform-accounts/{id}/status", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("DISCONNECTED"));
        }

        @Test
        @DisplayName("Creator can soft-disconnect platform account via DELETE")
        void testDisconnectPlatformAccount() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userA.getId()))
                    .thenReturn(Optional.of(accountA));
            when(platformAccountRepository.save(any(PlatformAccount.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(delete("/platform-accounts/{id}", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("3. Posts API")
    class PostsApiTests {

        @Test
        @DisplayName("Creator can list own posts with pagination and metadata")
        void testListPostsPaginated() throws Exception {
            when(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(postA)));

            mockMvc.perform(get("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "publishedAt")
                            .param("direction", "DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(postA.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].title").value("Building AI Apps in Spring Boot"))
                    .andExpect(jsonPath("$.data.content[0].viewsCount").value(50000))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.hasPrevious").value(false));
        }

        @Test
        @DisplayName("Creator can get post by ID")
        void testGetPostById() throws Exception {
            when(postRepository.findByIdAndPlatformAccountUserId(postA.getId(), userA.getId()))
                    .thenReturn(Optional.of(postA));

            mockMvc.perform(get("/posts/{id}", postA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(postA.getId().toString()));
        }

        @Test
        @DisplayName("User B accessing User A's post returns 404 NOT FOUND")
        void testCrossUserPostAccessReturns404() throws Exception {
            when(postRepository.findByIdAndPlatformAccountUserId(postA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/posts/{id}", postA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Invalid sort field returns 400 Bad Request")
        void testInvalidSortFieldRejected() throws Exception {
            mockMvc.perform(get("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("sort", "unsupportedField_DROP_TABLE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Page size > 100 is rejected with 400 Bad Request")
        void testExcessivePageSizeRejected() throws Exception {
            mockMvc.perform(get("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("size", "250"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Date range validation: 'from' after 'to' returns 400 Bad Request")
        void testInvalidDateRangeRejected() throws Exception {
            mockMvc.perform(get("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("from", "2026-10-10T00:00:00Z")
                            .param("to", "2026-10-01T00:00:00Z"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("4. Comments API (Raw & Processed)")
    class CommentsApiTests {

        @Test
        @DisplayName("Creator can list raw comments with user-scoped pagination")
        void testListRawComments() throws Exception {
            when(rawCommentRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(rawCommentA)));

            mockMvc.perform(get("/comments")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("search", "tutorial"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].authorDisplayName").value("DevFan99"))
                    .andExpect(jsonPath("$.data.content[0].rawText").value("Amazing tutorial, please cover RAG next!"));
        }

        @Test
        @DisplayName("Creator can get raw comment by ID")
        void testGetRawCommentById() throws Exception {
            when(rawCommentRepository.findByIdAndPostPlatformAccountUserId(rawCommentA.getId(), userA.getId()))
                    .thenReturn(Optional.of(rawCommentA));

            mockMvc.perform(get("/comments/{id}", rawCommentA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(rawCommentA.getId().toString()));
        }

        @Test
        @DisplayName("Creator can list processed comments with sentiment filter")
        void testListProcessedComments() throws Exception {
            when(processedCommentRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(processedCommentA)));

            mockMvc.perform(get("/comments/processed")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("sentiment", "POSITIVE")
                            .param("language", "en"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].sentimentLabel").value("POSITIVE"))
                    .andExpect(jsonPath("$.data.content[0].intent").value("FEATURE_REQUEST"))
                    .andExpect(jsonPath("$.data.content[0].language").value("en"));
        }

        @Test
        @DisplayName("User B accessing User A's raw comment returns 404 NOT FOUND")
        void testCrossUserRawCommentAccessReturns404() throws Exception {
            when(rawCommentRepository.findByIdAndPostPlatformAccountUserId(rawCommentA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/comments/{id}", rawCommentA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("User B accessing User A's processed comment returns 404 NOT FOUND")
        void testCrossUserProcessedCommentAccessReturns404() throws Exception {
            when(processedCommentRepository.findByIdAndRawCommentPostPlatformAccountUserId(processedCommentA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/comments/processed/{id}", processedCommentA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("5. Topics API")
    class TopicsApiTests {

        @Test
        @DisplayName("Creator can list own topics")
        void testListTopics() throws Exception {
            when(topicRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(topicA)));

            mockMvc.perform(get("/topics")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].name").value("Spring Boot & AI"))
                    .andExpect(jsonPath("$.data.content[0].keywords", hasItem("spring-ai")));
        }

        @Test
        @DisplayName("Creator can get topic by ID")
        void testGetTopicById() throws Exception {
            when(topicRepository.findByIdAndUserId(topicA.getId(), userA.getId()))
                    .thenReturn(Optional.of(topicA));

            mockMvc.perform(get("/topics/{id}", topicA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(topicA.getId().toString()));
        }

        @Test
        @DisplayName("User B accessing User A's topic returns 404 NOT FOUND")
        void testCrossUserTopicAccessReturns404() throws Exception {
            when(topicRepository.findByIdAndUserId(topicA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/topics/{id}", topicA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }
    }
}
