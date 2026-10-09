package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductionChecklistDto(
        List<ChecklistItemDto> items
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChecklistItemDto(
            String phase,
            String task,
            boolean completed
    ) {}
}
