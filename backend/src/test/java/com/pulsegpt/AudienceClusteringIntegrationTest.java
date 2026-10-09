package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.comment.*;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.topic.*;
import com.pulsegpt.topic.dto.ClusteringTriggerRequest;
import com.pulsegpt.topic.service.AudienceClusteringService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3H — Audience Clustering & Topic Modeling Integration Tests")
class AudienceClusteringIntegrationTest {

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
    private AudienceClusteringService audienceClusteringService;

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
    private ClusteringRunRepository clusteringRunRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User userA;
    private User userB;
    private String tokenUserA;
    private String tokenUserB;

    private PlatformAccount platformAccountA;
    private Post postA;
    private ProcessedComment processedComment1;
    private ProcessedComment processedComment2;

    @BeforeEach
    void setUp() {
        reset(userRepository, platformAccountRepository, postRepository, rawCommentRepository,
                processedCommentRepository, topicRepository, topicAssignmentRepository,
                clusteringRunRepository, auditLogRepository, aiServiceClient);

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

        tokenUserA = jwtService.generateAccessToken(userA);
        tokenUserB = jwtService.generateAccessToken(userB);

        when(userRepository.findById(userA.getId())).thenReturn(Optional.of(userA));
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(userRepository.findById(userB.getId())).thenReturn(Optional.of(userB));
        when(userRepository.findByEmail(userB.getEmail())).thenReturn(Optional.of(userB));

        platformAccountA = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .user(userA)
                .platform(PlatformType.YOUTUBE)
                .externalAccountId("UC_John_Tech")
                .accountName("Tech Insights John")
                .status(PlatformAccountStatus.CONNECTED)
                .build();

        postA = Post.builder()
                .id(UUID.randomUUID())
                .platformAccount(platformAccountA)
                .externalPostId("video_xyz_99")
                .title("Phone 15 Pro In-Depth Review")
                .publishedAt(Instant.now().minusSeconds(86400))
                .build();

        RawComment rawComment1 = RawComment.builder()
                .id(UUID.randomUUID())
                .post(postA)
                .externalCommentId("comm_1")
                .authorDisplayName("Viewer Dave")
                .rawText("Battery life is awesome and charges super fast")
                .publishedAt(Instant.now().minusSeconds(40000))
                .build();

        RawComment rawComment2 = RawComment.builder()
                .id(UUID.randomUUID())
                .post(postA)
                .externalCommentId("comm_2")
                .authorDisplayName("Viewer Alice")
                .rawText("How is the battery backup during gaming?")
                .publishedAt(Instant.now().minusSeconds(30000))
                .build();

        List<Double> dummyEmbedding = new ArrayList<>(Collections.nCopies(384, 0.05));

        processedComment1 = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(rawComment1)
                .normalizedText("Battery life is awesome and charges super fast")
                .language("en")
                .sentimentLabel(SentimentLabel.POSITIVE)
                .sentimentScore(0.92)
                .intent(IntentType.FEEDBACK)
                .priority(Priority.MEDIUM)
                .isSpam(false)
                .embedding(dummyEmbedding.toString())
                .embeddingModel("sentence-transformers/all-MiniLM-L6-v2")
                .embeddingDimension(384)
                .build();

        processedComment2 = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(rawComment2)
                .normalizedText("How is the battery backup during gaming?")
                .language("en")
                .sentimentLabel(SentimentLabel.NEUTRAL)
                .sentimentScore(0.50)
                .intent(IntentType.QUESTION)
                .priority(Priority.MEDIUM)
                .isSpam(false)
                .embedding(dummyEmbedding.toString())
                .embeddingModel("sentence-transformers/all-MiniLM-L6-v2")
                .embeddingDimension(384)
                .build();

        when(clusteringRunRepository.save(any(ClusteringRun.class))).thenAnswer(inv -> {
            ClusteringRun r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });

        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
    }

    @Nested
    @DisplayName("1. Audience Clustering Execution & AI Integration")
    class ClusteringExecutionTests {

        @Test
        @DisplayName("Execute clustering successfully with UMAP + HDBSCAN + c-TF-IDF")
        void testClusteringSuccess() throws Exception {
            when(processedCommentRepository.findEligibleForClusteringByUserId(eq(userA.getId()), any()))
                    .thenReturn(List.of(processedComment1, processedComment2));

            when(topicRepository.findByUserIdAndActiveTrue(userA.getId()))
                    .thenReturn(new ArrayList<>());

            List<Double> centroid = new ArrayList<>(Collections.nCopies(384, 0.05));
            AiClusteringRunResponse mockAiResponse = AiClusteringRunResponse.builder()
                    .runId("run-123")
                    .status("COMPLETED")
                    .algorithm("SEMANTIC_UMAP_HDBSCAN_CTFIDF")
                    .algorithmVersion("pulsegpt-cluster-v1")
                    .totalComments(2)
                    .clusteredCount(2)
                    .noiseCount(0)
                    .clusterCount(1)
                    .topics(List.of(
                            AiClusterTopicResult.builder()
                                    .clusterId(0)
                                    .label("Battery Life & Fast Charging")
                                    .rawKeywords(List.of("battery", "charging", "backup"))
                                    .keywords(List.of(
                                            new AiClusterKeywordScore("battery", 1.8),
                                            new AiClusterKeywordScore("charging", 1.5)
                                    ))
                                    .commentCount(2)
                                    .centroid(centroid)
                                    .sentimentDistribution(Map.of("POSITIVE", 1, "NEUTRAL", 1))
                                    .intentDistribution(Map.of("FEEDBACK", 1, "QUESTION", 1))
                                    .languageDistribution(Map.of("en", 2))
                                    .platformDistribution(Map.of("YOUTUBE", 2))
                                    .build()
                    ))
                    .assignments(List.of(
                            new AiCommentAssignmentResult(processedComment1.getId().toString(), 0, false, 0.98),
                            new AiCommentAssignmentResult(processedComment2.getId().toString(), 0, false, 0.95)
                    ))
                    .metrics(AiClusteringMetrics.builder()
                            .clusterCount(1)
                            .noiseCount(0)
                            .noiseRatio(0.0)
                            .largestClusterSize(2)
                            .smallestClusterSize(2)
                            .averageClusterSize(2.0)
                            .build())
                    .modelVersions(Map.of("algorithm", "pulsegpt-cluster-v1", "umap", "0.5.12", "hdbscan", "sklearn-hdbscan"))
                    .executionTimeMs(12.5)
                    .build();

            when(aiServiceClient.clusterComments(any())).thenReturn(mockAiResponse);

            ClusteringTriggerRequest request = ClusteringTriggerRequest.builder()
                    .maxComments(100)
                    .minClusterSize(2)
                    .umapEnabled(true)
                    .build();

            mockMvc.perform(post("/topics/cluster")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.data.clusterCount", is(1)))
                    .andExpect(jsonPath("$.data.clusteredCount", is(2)))
                    .andExpect(jsonPath("$.data.noiseCount", is(0)))
                    .andExpect(jsonPath("$.data.newTopicsCreatedCount", is(1)))
                    .andExpect(jsonPath("$.data.topics[0].name", is("Battery Life & Fast Charging")));

            verify(topicRepository, times(1)).save(any(Topic.class));
            verify(topicAssignmentRepository, times(1)).saveAll(any());
        }

        @Test
        @DisplayName("Stable Topic Matching — Reuses existing Topic when centroid similarity is high")
        void testStableTopicMatching() {
            List<Double> centroid = new ArrayList<>(Collections.nCopies(384, 0.05));

            Topic existingTopic = Topic.builder()
                    .id(UUID.randomUUID())
                    .user(userA)
                    .name("Battery Life & Charging")
                    .commentCount(10)
                    .centroid(centroid.toString())
                    .active(true)
                    .build();

            when(processedCommentRepository.findEligibleForClusteringByUserId(eq(userA.getId()), any()))
                    .thenReturn(List.of(processedComment1));

            when(topicRepository.findByUserIdAndActiveTrue(userA.getId()))
                    .thenReturn(new ArrayList<>(List.of(existingTopic)));

            AiClusteringRunResponse mockAiResponse = AiClusteringRunResponse.builder()
                    .runId("run-456")
                    .status("COMPLETED")
                    .algorithm("SEMANTIC_UMAP_HDBSCAN_CTFIDF")
                    .algorithmVersion("pulsegpt-cluster-v1")
                    .totalComments(1)
                    .clusteredCount(1)
                    .noiseCount(0)
                    .clusterCount(1)
                    .topics(List.of(
                            AiClusterTopicResult.builder()
                                    .clusterId(0)
                                    .label("Battery Backup")
                                    .rawKeywords(List.of("battery", "backup"))
                                    .commentCount(1)
                                    .centroid(centroid)
                                    .build()
                    ))
                    .assignments(List.of(
                            new AiCommentAssignmentResult(processedComment1.getId().toString(), 0, false, 0.99)
                    ))
                    .metrics(AiClusteringMetrics.builder().clusterCount(1).noiseCount(0).build())
                    .build();

            when(aiServiceClient.clusterComments(any())).thenReturn(mockAiResponse);

            var summary = audienceClusteringService.executeClustering(userA, ClusteringTriggerRequest.builder().build());

            assertThat(summary.matchedExistingTopicsCount()).isEqualTo(1);
            assertThat(summary.newTopicsCreatedCount()).isEqualTo(0);
            assertThat(existingTopic.getCommentCount()).isEqualTo(11);
        }

        @Test
        @DisplayName("Clustering handles AI Service failure gracefully")
        void testClusteringServiceFailure() {
            when(processedCommentRepository.findEligibleForClusteringByUserId(eq(userA.getId()), any()))
                    .thenReturn(List.of(processedComment1));

            when(aiServiceClient.clusterComments(any()))
                    .thenThrow(new RuntimeException("Connection refused to AI Microservice"));

            assertThatThrownBy(() -> audienceClusteringService.executeClustering(userA, ClusteringTriggerRequest.builder().build()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Clustering execution failed");

            verify(clusteringRunRepository, atLeast(2)).save(any(ClusteringRun.class));
        }
    }

    @Nested
    @DisplayName("2. Multi-Tenant Topic & Run Security")
    class MultiTenantSecurityTests {

        @Test
        @DisplayName("User A cannot access User B's topics — returns 404")
        void testCrossUserTopicAccessForbidden() throws Exception {
            UUID topicIdB = UUID.randomUUID();
            when(topicRepository.findByIdAndUserId(topicIdB, userA.getId())).thenReturn(Optional.empty());

            mockMvc.perform(get("/topics/{id}", topicIdB)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User A cannot access User B's clustering run — returns 404")
        void testCrossUserClusteringRunAccessForbidden() throws Exception {
            UUID runIdB = UUID.randomUUID();
            when(clusteringRunRepository.findByIdAndUserId(runIdB, userA.getId())).thenReturn(Optional.empty());

            mockMvc.perform(get("/clustering/runs/{id}", runIdB)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isNotFound());
        }
    }
}
