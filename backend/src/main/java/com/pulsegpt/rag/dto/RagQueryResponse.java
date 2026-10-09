package com.pulsegpt.rag.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RagQueryResponse(
        String answer,
        List<RagCitationDto> citations,
        List<RagEvidenceItem> evidence,
        RagRetrievalMetadata retrievalMetadata,
        RagValidationResult validation,
        long generationLatencyMs
) {
}
