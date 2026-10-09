package com.pulsegpt.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TopicRecencyWarning(
        @Schema(description = "Warning type identifier", example = "TOPIC_RECENCY_WARNING")
        String type,

        @Schema(description = "User-facing warning message", example = "This topic was scheduled recently within the last 7 days.")
        String message,

        @Schema(description = "Warning severity: WARNING or INFO", example = "WARNING")
        String severity,

        @Schema(description = "Topic ID involved in the recency alert")
        UUID topicId,

        @Schema(description = "Previous scheduled start timestamp")
        Instant lastScheduledDate
) {}
