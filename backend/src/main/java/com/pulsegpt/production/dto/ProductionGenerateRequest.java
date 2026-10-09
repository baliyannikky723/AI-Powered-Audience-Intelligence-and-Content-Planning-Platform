package com.pulsegpt.production.dto;

import com.pulsegpt.production.ProductionAssetType;
import com.pulsegpt.recommendation.GenerationMode;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ProductionGenerateRequest(
        @NotNull(message = "recommendationId is required")
        UUID recommendationId,
        UUID calendarItemId,
        List<ProductionAssetType> assetTypes,
        GenerationMode mode
) {
}
