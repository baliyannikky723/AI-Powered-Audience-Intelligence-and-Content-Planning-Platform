package com.pulsegpt.rag.dto;

import com.pulsegpt.rag.model.RagCorrectness;
import com.pulsegpt.rag.model.RagRelevance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record EvidenceAnnotationRequest(
        @NotBlank(message = "queryId is required")
        String queryId,
        @NotBlank(message = "evidenceId is required")
        String evidenceId,
        @NotNull(message = "relevance is required")
        RagRelevance relevance,
        @NotNull(message = "correctness is required")
        RagCorrectness correctness,
        String notes
) {
}
