package com.pulsegpt.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchMetricsResponse {

    private Map<String, Object> systemMetrics;
    private Map<String, Object> nlpMetrics;
    private Map<String, Object> clusteringMetrics;
    private Map<String, Object> recommendationMetrics;
    private Map<String, Object> productionMetrics;
    private Map<String, Object> creatorWorkflowMetrics;
    private Map<String, Object> exportMetrics;
    private Map<String, Object> reproducibilityMetadata;
}
