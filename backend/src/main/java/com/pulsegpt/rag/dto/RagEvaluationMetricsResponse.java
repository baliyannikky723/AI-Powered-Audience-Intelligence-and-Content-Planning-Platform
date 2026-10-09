package com.pulsegpt.rag.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record RagEvaluationMetricsResponse(
        Double precisionAtK,
        Double recallAtK,
        String humanEvaluationStatus,
        double evidenceCoverage,
        double citationValidityRate,
        double sourceDiversityScore,
        double memoryUtilizationRate,
        double graphUtilizationRate,
        long avgRetrievalLatencyMs,
        long avgGenerationLatencyMs,
        long totalQueriesEvaluated,
        long totalAnnotationsCount,
        Map<String, Object> modeComparisons
) {
}
