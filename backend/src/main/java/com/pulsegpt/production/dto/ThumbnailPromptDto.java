package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ThumbnailPromptDto(
        String concept,
        String visualSubject,
        String composition,
        String textOverlay,
        String emotion,
        String style
) {
}
