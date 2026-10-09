package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.production.ProductionAssetType;
import com.pulsegpt.recommendation.GenerationMode;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductionAssetResponse(
        UUID id,
        UUID userId,
        UUID recommendationId,
        String recommendationTitle,
        UUID calendarItemId,
        ProductionAssetType assetType,
        ProductionAssetStatus status,
        GenerationMode generationMode,
        Integer version,
        String promptVersion,
        String modelName,
        Map<String, Object> contentJson,
        Map<String, Object> evidenceSnapshot,
        List<String> evidenceIds,
        Map<String, Object> validationJson,
        List<Map<String, Object>> revisionHistory,
        Boolean repairAttempted,
        Map<String, Object> repairResultJson,
        Instant approvedAt,
        UUID approvedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
