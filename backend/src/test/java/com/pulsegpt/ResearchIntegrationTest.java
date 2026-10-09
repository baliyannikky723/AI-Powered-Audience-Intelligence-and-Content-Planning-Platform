package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.evaluation.DatasetSnapshot;
import com.pulsegpt.evaluation.DatasetSnapshotRepository;
import com.pulsegpt.evaluation.EvaluationRecordRepository;
import com.pulsegpt.evaluation.ExperimentRun;
import com.pulsegpt.evaluation.ExperimentRunRepository;
import com.pulsegpt.evaluation.dto.CompleteExperimentRequest;
import com.pulsegpt.evaluation.dto.CreateExperimentRequest;
import com.pulsegpt.evaluation.dto.CreateSnapshotRequest;
import com.pulsegpt.production.ContentProductionAssetRepository;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.topic.ClusteringRunRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3M Research & Observability Integration Tests")
class ResearchIntegrationTest {

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

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ExperimentRunRepository experimentRunRepository;

    @MockitoBean
    private DatasetSnapshotRepository datasetSnapshotRepository;

    @MockitoBean
    private EvaluationRecordRepository evaluationRecordRepository;

    @MockitoBean
    private ContentRecommendationRepository recommendationRepository;

    @MockitoBean
    private ContentProductionAssetRepository productionAssetRepository;

    @MockitoBean
    private ClusteringRunRepository clusteringRunRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @MockitoBean
    private com.pulsegpt.comment.RawCommentRepository rawCommentRepository;

    @MockitoBean
    private com.pulsegpt.comment.ProcessedCommentRepository processedCommentRepository;

    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin-researcher@pulsegpt.ai")
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        adminToken = "Bearer " + jwtService.generateAccessToken(adminUser);
    }

    @Nested
    @DisplayName("GET /api/v1/research/metrics")
    class MetricsTests {

        @Test
        @DisplayName("Returns structured research metrics response")
        void testGetResearchMetrics() throws Exception {
            mockMvc.perform(get("/research/metrics")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.systemMetrics").exists())
                    .andExpect(jsonPath("$.data.reproducibilityMetadata").exists())
                    .andExpect(jsonPath("$.data.reproducibilityMetadata.algorithmName").value("HDBSCAN"));
        }
    }

    @Nested
    @DisplayName("Experiment Run Lifecycle APIs")
    class ExperimentLifecycleTests {

        @Test
        @DisplayName("Create experiment run successfully")
        void testCreateExperiment() throws Exception {
            CreateExperimentRequest request = CreateExperimentRequest.builder()
                    .experimentName("Recommendation Grounding Study")
                    .experimentType("RECOMMENDATION_EVALUATION")
                    .description("Comparing grounded vs ungrounded recommendations")
                    .baselineMode("BASELINE")
                    .treatmentMode("EVIDENCE_GROUNDED")
                    .parameters(Map.of("sampleCount", 50))
                    .build();

            when(experimentRunRepository.save(any(ExperimentRun.class)))
                    .thenAnswer(inv -> {
                        ExperimentRun run = inv.getArgument(0);
                        run.setId(UUID.randomUUID());
                        return run;
                    });

            mockMvc.perform(post("/research/experiments")
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.experimentName").value("Recommendation Grounding Study"))
                    .andExpect(jsonPath("$.data.status").value("CREATED"));
        }

        @Test
        @DisplayName("Start and complete experiment run")
        void testStartAndCompleteExperiment() throws Exception {
            UUID expId = UUID.randomUUID();
            ExperimentRun run = ExperimentRun.builder()
                    .id(expId)
                    .user(adminUser)
                    .experimentName("Clustering Stability Test")
                    .experimentType("CLUSTERING_STABILITY")
                    .status("CREATED")
                    .build();

            when(experimentRunRepository.findByIdAndUserId(expId, adminUser.getId()))
                    .thenReturn(Optional.of(run));
            when(experimentRunRepository.save(any(ExperimentRun.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/research/experiments/" + expId + "/start")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("RUNNING"));

            CompleteExperimentRequest completeReq = CompleteExperimentRequest.builder()
                    .metrics(Map.of("stabilityScore", 0.89))
                    .build();

            mockMvc.perform(post("/research/experiments/" + expId + "/complete")
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completeReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.metrics.stabilityScore").value(0.89));
        }

        @Test
        @DisplayName("List experiments returns paged response")
        void testListExperiments() throws Exception {
            when(experimentRunRepository.findByUserId(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/research/experiments")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("Dataset Snapshots APIs")
    class DatasetSnapshotTests {

        @Test
        @DisplayName("Create dataset snapshot successfully")
        void testCreateSnapshot() throws Exception {
            CreateSnapshotRequest req = CreateSnapshotRequest.builder()
                    .name("Baseline YouTube Comments Q4")
                    .description("Frozen sample for reproducibility")
                    .platform("YOUTUBE")
                    .dateFrom(Instant.now().minusSeconds(86400 * 30))
                    .dateTo(Instant.now())
                    .build();

            when(datasetSnapshotRepository.save(any(DatasetSnapshot.class)))
                    .thenAnswer(inv -> {
                        DatasetSnapshot s = inv.getArgument(0);
                        s.setId(UUID.randomUUID());
                        return s;
                    });

            mockMvc.perform(post("/research/snapshots")
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Baseline YouTube Comments Q4"));
        }

        @Test
        @DisplayName("List dataset snapshots returns paged list")
        void testListSnapshots() throws Exception {
            when(datasetSnapshotRepository.findByUserId(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/research/snapshots")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("Aggregated Evaluation APIs")
    class AggregatedEvaluationTests {

        @Test
        @DisplayName("Get recommendations evaluation metrics")
        void testGetRecommendationsEvaluation() throws Exception {
            mockMvc.perform(get("/research/recommendations/evaluation")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Get production copilot evaluation metrics")
        void testGetProductionEvaluation() throws Exception {
            mockMvc.perform(get("/research/production/evaluation")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
