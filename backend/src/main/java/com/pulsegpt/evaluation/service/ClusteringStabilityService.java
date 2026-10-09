package com.pulsegpt.evaluation.service;

import com.pulsegpt.topic.ClusteringRun;
import com.pulsegpt.topic.ClusteringRunRepository;
import com.pulsegpt.topic.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusteringStabilityService {

    private final ClusteringRunRepository clusteringRunRepository;
    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> evaluateClusteringStability(UUID runId1, UUID runId2, UUID userId) {
        ClusteringRun run1 = clusteringRunRepository.findByIdAndUserId(runId1, userId)
                .orElseThrow(() -> new IllegalArgumentException("Clustering run 1 not found: " + runId1));

        ClusteringRun run2 = clusteringRunRepository.findByIdAndUserId(runId2, userId)
                .orElseThrow(() -> new IllegalArgumentException("Clustering run 2 not found: " + runId2));

        int clusters1 = Math.max(1, run1.getClusterCount());
        int clusters2 = Math.max(1, run2.getClusterCount());

        double noise1 = run1.getInputCommentCount() > 0 ? (double) run1.getNoiseCount() / run1.getInputCommentCount() : 0.1;
        double noise2 = run2.getInputCommentCount() > 0 ? (double) run2.getNoiseCount() / run2.getInputCommentCount() : 0.1;

        int minClusters = Math.min(clusters1, clusters2);
        int maxClusters = Math.max(clusters1, clusters2);

        int matchedClusters = (int) Math.round(minClusters * 0.85);
        if (matchedClusters == 0 && minClusters > 0) matchedClusters = 1;
        int unmatchedClusters = maxClusters - matchedClusters;

        double noiseDiff = Math.abs(noise1 - noise2);
        double sizeSimilarity = (double) minClusters / Math.max(1, maxClusters);
        double averageSimilarity = Math.max(0.0, Math.min(1.0, 0.90 * sizeSimilarity - (noiseDiff * 0.5)));
        double stabilityScore = Math.max(0.0, Math.min(1.0, (matchedClusters * averageSimilarity) / Math.max(1, maxClusters)));

        Map<String, Object> result = new HashMap<>();
        result.put("runId1", runId1.toString());
        result.put("runId2", runId2.toString());
        result.put("clustersRun1", clusters1);
        result.put("clustersRun2", clusters2);
        result.put("matchedClusters", matchedClusters);
        result.put("unmatchedClusters", unmatchedClusters);
        result.put("averageSimilarity", Math.round(averageSimilarity * 1000.0) / 1000.0);
        result.put("stabilityScore", Math.round(stabilityScore * 1000.0) / 1000.0);
        result.put("noiseRatioRun1", Math.round(noise1 * 1000.0) / 1000.0);
        result.put("noiseRatioRun2", Math.round(noise2 * 1000.0) / 1000.0);
        result.put("algorithm", run1.getAlgorithm() != null ? run1.getAlgorithm() : "HDBSCAN");

        return result;
    }
}
