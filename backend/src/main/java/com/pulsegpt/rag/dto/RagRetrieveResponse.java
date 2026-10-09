package com.pulsegpt.rag.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RagRetrieveResponse(
        String query,
        List<RagEvidenceItem> evidence,
        RagRetrievalMetadata retrievalMetadata
) {
}
