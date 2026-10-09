package com.pulsegpt.calendar.dto;

import com.pulsegpt.platform.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CalendarConflictDetail(
        @Schema(description = "ID of the existing conflicting calendar item")
        UUID calendarItemId,

        @Schema(description = "Title of the existing conflicting calendar item")
        String title,

        @Schema(description = "Platform of the conflicting calendar item")
        PlatformType platform,

        @Schema(description = "Scheduled start time of the existing item")
        Instant scheduledStart,

        @Schema(description = "Scheduled end time of the existing item")
        Instant scheduledEnd
) {}
