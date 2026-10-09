package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CtaVariantDto(
        String ctaType,
        String text
) {
}
