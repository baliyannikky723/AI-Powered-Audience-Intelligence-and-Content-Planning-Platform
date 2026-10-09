package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record AiClusteringRunResponse(
        @JsonProperty("run_id") String runId,
        String status,
        String algorithm,
        @JsonProperty("algorithm_version") String algorithmVersion,
        @JsonProperty("total_comments") Integer totalComments,
        @JsonProperty("clustered_count") Integer clusteredCount,
        @JsonProperty("noise_count") Integer noiseCount,
        @JsonProperty("cluster_count") Integer clusterCount,
        List<AiClusterTopicResult> topics,
        List<AiCommentAssignmentResult> assignments,
        AiClusteringMetrics metrics,
        Map<String, Object> config,
        @JsonProperty("model_versions") Map<String, String> modelVersions,
        @JsonProperty("execution_time_ms") Double executionTimeMs
) {}
