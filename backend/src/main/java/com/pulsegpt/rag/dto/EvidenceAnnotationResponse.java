package com.pulsegpt.rag.dto;

import com.pulsegpt.rag.model.EvidenceAnnotation;
import com.pulsegpt.rag.model.RagCorrectness;
import com.pulsegpt.rag.model.RagRelevance;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record EvidenceAnnotationResponse(
        UUID id,
        UUID userId,
        String queryId,
        String evidenceId,
        RagRelevance relevance,
        RagCorrectness correctness,
        String notes,
        Instant createdAt
) {
    public static EvidenceAnnotationResponse fromEntity(EvidenceAnnotation entity) {
        return EvidenceAnnotationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .queryId(entity.getQueryId())
                .evidenceId(entity.getEvidenceId())
                .relevance(entity.getRelevance())
                .correctness(entity.getCorrectness())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
