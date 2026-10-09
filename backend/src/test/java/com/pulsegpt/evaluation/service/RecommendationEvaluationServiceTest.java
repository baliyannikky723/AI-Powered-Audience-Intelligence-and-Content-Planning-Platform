package com.pulsegpt.evaluation.service;

import com.pulsegpt.evaluation.EvaluationRecordRepository;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.RecommendationStatus;
import com.pulsegpt.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationEvaluationService Unit Tests")
class RecommendationEvaluationServiceTest {

    @Mock
    private ContentRecommendationRepository recommendationRepository;

    @Mock
    private EvaluationRecordRepository evaluationRecordRepository;

    private RecommendationEvaluationService service;

    private User testUser;
    private UUID recId;

    @BeforeEach
    void setUp() {
        service = new RecommendationEvaluationService(recommendationRepository, evaluationRecordRepository);
        testUser = User.builder().id(UUID.randomUUID()).email("researcher@pulsegpt.ai").build();
        recId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Evaluate recommendation calculates evidence coverage and check quality")
    void testEvaluateRecommendation() {
        ContentRecommendation rec = ContentRecommendation.builder()
                .id(recId)
                .user(testUser)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .evidenceSnapshot(Map.of("evidenceCount", 3, "evidenceIds", List.of("ev1", "ev2", "ev3")))
                .validationJson(Map.of(
                        "valid", true,
                        "checks", List.of(
                                Map.of("checkName", "SCHEMA", "passed", true),
                                Map.of("checkName", "EVIDENCE", "passed", true),
                                Map.of("checkName", "UNSUPPORTED_CLAIMS", "passed", true),
                                Map.of("checkName", "RELEVANCE", "passed", true),
                                Map.of("checkName", "NOVELTY", "passed", true),
                                Map.of("checkName", "SAFETY", "passed", true)
                        )
                ))
                .build();

        when(recommendationRepository.findByIdAndUserId(recId, testUser.getId()))
                .thenReturn(Optional.of(rec));

        Map<String, Object> eval = service.evaluateRecommendation(recId, testUser);

        assertThat(eval.get("recommendationId")).isEqualTo(recId.toString());
        assertThat(eval.get("evidenceCount")).isEqualTo(3);
        assertThat(eval.get("evidenceCoverage")).isEqualTo(1.0);
        assertThat(eval.get("validationQuality")).isEqualTo(1.0);
        assertThat(eval.get("validationPassed")).isEqualTo(true);
        assertThat(eval.get("passedChecks")).isEqualTo(6);
    }

    @Test
    @DisplayName("Get aggregated metrics calculates averages for user recommendations")
    void testGetAggregatedMetrics() {
        ContentRecommendation r1 = ContentRecommendation.builder()
                .user(testUser)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .status(RecommendationStatus.VALIDATED)
                .validationPassed(true)
                .evidenceSnapshot(Map.of("evidenceCount", 3))
                .build();

        ContentRecommendation r2 = ContentRecommendation.builder()
                .user(testUser)
                .generationMode(GenerationMode.BASELINE)
                .status(RecommendationStatus.VALIDATED)
                .validationPassed(true)
                .evidenceSnapshot(Map.of("evidenceCount", 0))
                .build();

        when(recommendationRepository.findAll()).thenReturn(List.of(r1, r2));

        Map<String, Object> agg = service.getAggregatedRecommendationMetrics(testUser.getId());

        assertThat(agg.get("totalEvaluated")).isEqualTo(2);
        assertThat(agg.get("evidenceGroundedCount")).isEqualTo(1);
        assertThat(agg.get("baselineCount")).isEqualTo(1);
        assertThat((Double) agg.get("passRate")).isEqualTo(1.0);
    }
}
