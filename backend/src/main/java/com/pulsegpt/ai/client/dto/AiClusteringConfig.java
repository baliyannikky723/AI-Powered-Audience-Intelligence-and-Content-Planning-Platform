package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AiClusteringConfig(
        String algorithm,
        @JsonProperty("umap_enabled") Boolean umapEnabled,
        @JsonProperty("random_state") Integer randomState,
        @JsonProperty("n_neighbors") Integer nNeighbors,
        @JsonProperty("n_components") Integer nComponents,
        @JsonProperty("min_dist") Double minDist,
        String metric,
        @JsonProperty("min_cluster_size") Integer minClusterSize,
        @JsonProperty("min_samples") Integer minSamples,
        @JsonProperty("cluster_selection_method") String clusterSelectionMethod,
        @JsonProperty("top_keywords") Integer topKeywords,
        @JsonProperty("ngram_range") List<Integer> ngramRange,
        @JsonProperty("k_clusters") Integer kClusters
) {}
