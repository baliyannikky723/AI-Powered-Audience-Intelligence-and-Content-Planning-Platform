package com.pulsegpt.rag.dto;

import com.pulsegpt.recommendation.EvidenceSourceType;
import lombok.Builder;

@Builder
public record RagCitationDto(
        String citationId,
        String evidenceId,
        EvidenceSourceType sourceType,
        String snippet,
        double relevanceScore,
        boolean valid
) {
}
