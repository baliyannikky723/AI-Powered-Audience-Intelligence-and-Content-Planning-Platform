package com.pulsegpt.calendar.dto;

import com.pulsegpt.platform.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Builder
public record CalendarSuggestionResponse(
        @Schema(description = "Date evaluated for slot suggestions", example = "2026-10-15")
        LocalDate date,

        @Schema(description = "Target platform evaluated", example = "YOUTUBE")
        PlatformType platform,

        @Schema(description = "Timezone used for evaluating posting windows", example = "Asia/Kolkata")
        String timezone,

        @Schema(description = "Deterministic slot recommendations for the day")
        List<SuggestedSlot> slots
) {
    @Builder
    public record SuggestedSlot(
            @Schema(description = "Slot window name: Morning, Afternoon, Evening", example = "Evening")
            String slotName,

            @Schema(description = "Local time string", example = "18:00")
            String time,

            @Schema(description = "UTC start instant for scheduling")
            Instant scheduledStart,

            @Schema(description = "UTC end instant for scheduling")
            Instant scheduledEnd,

            @Schema(description = "Whether the slot is available (no conflict)", example = "true")
            boolean available,

            @Schema(description = "Reason or conflicting item title if unavailable")
            String conflictReason
    ) {}
}
