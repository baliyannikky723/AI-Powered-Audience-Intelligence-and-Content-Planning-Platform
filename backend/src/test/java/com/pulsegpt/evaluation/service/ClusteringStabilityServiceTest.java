package com.pulsegpt.evaluation.service;

import com.pulsegpt.topic.ClusteringRun;
import com.pulsegpt.topic.ClusteringRunRepository;
import com.pulsegpt.topic.TopicRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClusteringStabilityService Unit Tests")
class ClusteringStabilityServiceTest {

    @Mock
    private ClusteringRunRepository clusteringRunRepository;

    @Mock
    private TopicRepository topicRepository;

    private ClusteringStabilityService service;

    private UUID userId;
    private UUID runId1;
    private UUID runId2;

    @BeforeEach
    void setUp() {
        service = new ClusteringStabilityService(clusteringRunRepository, topicRepository);
        userId = UUID.randomUUID();
        runId1 = UUID.randomUUID();
        runId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("Evaluate clustering stability returns matched clusters, average similarity, and stability score")
    void testEvaluateClusteringStability() {
        ClusteringRun run1 = ClusteringRun.builder()
                .id(runId1)
                .clusterCount(5)
                .inputCommentCount(100)
                .noiseCount(12)
                .algorithm("HDBSCAN")
                .build();

        ClusteringRun run2 = ClusteringRun.builder()
                .id(runId2)
                .clusterCount(5)
                .inputCommentCount(100)
                .noiseCount(14)
                .algorithm("HDBSCAN")
                .build();

        when(clusteringRunRepository.findByIdAndUserId(runId1, userId)).thenReturn(Optional.of(run1));
        when(clusteringRunRepository.findByIdAndUserId(runId2, userId)).thenReturn(Optional.of(run2));

        Map<String, Object> result = service.evaluateClusteringStability(runId1, runId2, userId);

        assertThat(result.get("runId1")).isEqualTo(runId1.toString());
        assertThat(result.get("runId2")).isEqualTo(runId2.toString());
        assertThat(result.get("clustersRun1")).isEqualTo(5);
        assertThat(result.get("clustersRun2")).isEqualTo(5);
        assertThat((Integer) result.get("matchedClusters")).isGreaterThanOrEqualTo(1);
        assertThat((Double) result.get("stabilityScore")).isGreaterThan(0.0);
    }
}
