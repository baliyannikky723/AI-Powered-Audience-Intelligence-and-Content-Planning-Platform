package com.pulsegpt.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RagQueryRequest(
        @NotBlank(message = "Query text is required")
        String query,
        String generationMode, // BASELINE, VECTOR_ONLY, GRAPH_AUGMENTED, FULL_EVIDENCE_GROUNDED / EVIDENCE_GROUNDED
        String platform,
        UUID topicId,
        Integer maxEvidence,
        Integer timeRangeDays,
        Boolean includeMemory,
        Boolean includeQuestions,
        Boolean includeTrends,
        Boolean includeContentHistory
) {
}
