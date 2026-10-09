package com.pulsegpt.calendar.dto;

import com.pulsegpt.calendar.CalendarItemStatus;
import com.pulsegpt.platform.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CalendarQuery(
        @Schema(description = "Start of scheduling time range filter")
        Instant startDate,

        @Schema(description = "End of scheduling time range filter")
        Instant endDate,

        @Schema(description = "Filter by target platform")
        PlatformType platform,

        @Schema(description = "Filter by content type (VIDEO, SHORT, etc.)")
        String contentType,

        @Schema(description = "Filter by calendar status")
        CalendarItemStatus status,

        @Schema(description = "Filter by topic ID")
        UUID topicId,

        @Schema(description = "Calendar view type: MONTH, WEEK, DAY, LIST", example = "MONTH")
        String view,

        @Min(0)
        @Schema(description = "Page index (0-based)", defaultValue = "0")
        Integer page,

        @Min(1)
        @Max(100)
        @Schema(description = "Page size (max 100)", defaultValue = "20")
        Integer size,

        @Schema(description = "Sort field (scheduledStart, createdAt, title, status, priority)", defaultValue = "scheduledStart")
        String sort,

        @Schema(description = "Sort direction: asc or desc", defaultValue = "asc")
        String direction
) {
    public int getPageOrDefault() {
        return page != null ? page : 0;
    }

    public int getSizeOrDefault() {
        return size != null ? Math.min(size, 100) : 20;
    }
}
