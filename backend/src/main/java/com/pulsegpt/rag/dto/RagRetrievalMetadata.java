package com.pulsegpt.rag.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record RagRetrievalMetadata(
        String retrievalVersion,
        String embeddingModel,
        int topK,
        double similarityThreshold,
        int evidenceBudget,
        int evidenceCount,
        boolean graphAvailable,
        boolean vectorAvailable,
        Map<String, Integer> sourceCounts,
        long retrievalLatencyMs
) {
}
