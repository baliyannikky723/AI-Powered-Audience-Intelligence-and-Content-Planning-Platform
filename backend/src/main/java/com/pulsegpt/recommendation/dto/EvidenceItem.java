package com.pulsegpt.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.recommendation.EvidenceSourceType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceItem(
        String evidenceId,
        EvidenceSourceType sourceType,
        UUID sourceId,
        UUID userId,
        String summary,
        double relevanceScore,
        Instant createdAt,
        Map<String, Object> metadata
) {}
