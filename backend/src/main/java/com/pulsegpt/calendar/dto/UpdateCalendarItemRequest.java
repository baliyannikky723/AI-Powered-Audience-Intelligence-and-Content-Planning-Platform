package com.pulsegpt.calendar.dto;

import com.pulsegpt.calendar.CalendarItemStatus;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.platform.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UpdateCalendarItemRequest(
        @Size(max = 500, message = "Title cannot exceed 500 characters")
        @Schema(description = "Updated title")
        String title,

        @Schema(description = "Updated platform")
        PlatformType platform,

        @Schema(description = "Updated content type")
        String contentType,

        @Schema(description = "Updated scheduled start time")
        Instant scheduledStart,

        @Schema(description = "Updated scheduled end time")
        Instant scheduledEnd,

        @Schema(description = "Updated timezone")
        String timezone,

        @Schema(description = "Updated status")
        CalendarItemStatus status,

        @Schema(description = "Updated priority")
        Priority priority,

        @Schema(description = "Updated editorial notes")
        String notes
) {}
