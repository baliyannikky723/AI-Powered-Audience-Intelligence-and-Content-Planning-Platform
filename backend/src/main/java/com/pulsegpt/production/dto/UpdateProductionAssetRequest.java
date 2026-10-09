package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateProductionAssetRequest(
        Map<String, Object> contentJson,
        String title,
        String hook,
        String cta,
        String notes
) {
}
