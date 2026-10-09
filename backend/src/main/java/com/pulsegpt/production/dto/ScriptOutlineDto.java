package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScriptOutlineDto(
        String format,
        List<ScriptOutlineSectionDto> sections,
        String keyTakeaway
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ScriptOutlineSectionDto(
            String sectionTitle,
            String purpose,
            List<String> talkingPoints,
            Integer estimatedDurationSeconds
    ) {}
}
