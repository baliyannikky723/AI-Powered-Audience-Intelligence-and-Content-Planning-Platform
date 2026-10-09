package com.pulsegpt.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record CalendarConflictResponse(
        @Schema(description = "Indicates if a scheduling conflict exists", example = "true")
        boolean conflict,

        @Schema(description = "User-facing summary message")
        String message,

        @Schema(description = "List of conflicting calendar items")
        List<CalendarConflictDetail> conflicts
) {}
