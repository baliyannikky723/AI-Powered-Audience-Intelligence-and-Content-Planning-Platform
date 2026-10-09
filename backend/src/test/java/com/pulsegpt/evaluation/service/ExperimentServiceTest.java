package com.pulsegpt.evaluation.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.evaluation.ExperimentRun;
import com.pulsegpt.evaluation.ExperimentRunRepository;
import com.pulsegpt.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExperimentService Unit Tests")
class ExperimentServiceTest {

    @Mock
    private ExperimentRunRepository experimentRunRepository;

    @Mock
    private RecommendationEvaluationService recommendationEvaluationService;

    @Mock
    private ProductionEvaluationService productionEvaluationService;

    @Mock
    private AuditService auditService;

    private ExperimentService service;

    private User testUser;
    private UUID experimentId;

    @BeforeEach
    void setUp() {
        service = new ExperimentService(
                experimentRunRepository,
                recommendationEvaluationService,
                productionEvaluationService,
                auditService
        );
        testUser = User.builder().id(UUID.randomUUID()).email("researcher@pulsegpt.ai").build();
        experimentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Create experiment saves experiment run in CREATED status")
    void testCreateExperiment() {
        when(experimentRunRepository.save(any(ExperimentRun.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ExperimentRun run = service.createExperiment(
                "Recommendation Benchmark 2026",
                "RECOMMENDATION_EVALUATION",
                "Testing evidence grounded vs baseline",
                "BASELINE",
                "EVIDENCE_GROUNDED",
                null,
                Map.of("sampleSize", 100),
                testUser
        );

        assertThat(run.getExperimentName()).isEqualTo("Recommendation Benchmark 2026");
        assertThat(run.getStatus()).isEqualTo("CREATED");
        assertThat(run.getBaselineMode()).isEqualTo("BASELINE");
        assertThat(run.getTreatmentMode()).isEqualTo("EVIDENCE_GROUNDED");
    }

    @Test
    @DisplayName("Start and complete experiment lifecycle")
    void testStartAndCompleteExperiment() {
        ExperimentRun exp = ExperimentRun.builder()
                .id(experimentId)
                .user(testUser)
                .experimentName("Production Test")
                .status("CREATED")
                .build();

        when(experimentRunRepository.findByIdAndUserId(experimentId, testUser.getId()))
                .thenReturn(Optional.of(exp));
        when(experimentRunRepository.save(any(ExperimentRun.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ExperimentRun running = service.startExperiment(experimentId, testUser);
        assertThat(running.getStatus()).isEqualTo("RUNNING");
        assertThat(running.getStartedAt()).isNotNull();

        ExperimentRun completed = service.completeExperiment(
                experimentId,
                Map.of("passRate", 0.95),
                testUser
        );
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getMetrics()).containsEntry("passRate", 0.95);
    }
}
