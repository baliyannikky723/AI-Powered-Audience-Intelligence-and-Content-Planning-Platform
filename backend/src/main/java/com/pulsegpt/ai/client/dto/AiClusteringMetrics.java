package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AiClusteringMetrics(
        @JsonProperty("cluster_count") Integer clusterCount,
        @JsonProperty("noise_count") Integer noiseCount,
        @JsonProperty("noise_ratio") Double noiseRatio,
        @JsonProperty("largest_cluster_size") Integer largestClusterSize,
        @JsonProperty("smallest_cluster_size") Integer smallestClusterSize,
        @JsonProperty("average_cluster_size") Double averageClusterSize,
        @JsonProperty("silhouette_score") Double silhouetteScore,
        @JsonProperty("davies_bouldin_index") Double daviesBouldinIndex,
        @JsonProperty("calinski_harabasz_score") Double calinskiHarabaszScore,
        @JsonProperty("metric_warnings") List<String> metricWarnings
) {}
