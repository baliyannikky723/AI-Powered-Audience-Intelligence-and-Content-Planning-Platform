package com.pulsegpt.calendar.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.calendar.CalendarItemStatus;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.platform.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarItemResponse(
        @Schema(description = "Calendar Item unique ID")
        UUID id,

        @Schema(description = "User ID")
        UUID userId,

        @Schema(description = "Linked Recommendation ID (if planned from recommendation)")
        UUID recommendationId,

        @Schema(description = "Linked Topic ID")
        UUID topicId,

        @Schema(description = "Linked Topic Name")
        String topicName,

        @Schema(description = "Title of the planned content piece")
        String title,

        @Schema(description = "Description")
        String description,

        @Schema(description = "Editorial notes / production outline")
        String notes,

        @Schema(description = "Content format (VIDEO, SHORT, POST, etc.)")
        String contentType,

        @Schema(description = "Target publishing platform")
        PlatformType platform,

        @Schema(description = "Scheduled start timestamp (ISO 8601)")
        Instant scheduledStart,

        @Schema(description = "Scheduled end timestamp (ISO 8601)")
        Instant scheduledEnd,

        @Schema(description = "Creator IANA timezone")
        String timezone,

        @Schema(description = "Planning status (PLANNED, SCHEDULED, CANCELLED, COMPLETED, DRAFT)")
        CalendarItemStatus status,

        @Schema(description = "Content priority")
        Priority priority,

        @Schema(description = "Approval timestamp")
        Instant approvedAt,

        @Schema(description = "Cancellation timestamp")
        Instant cancelledAt,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt,

        @Schema(description = "Evidence snapshot summary from upstream recommendation")
        Map<String, Object> evidenceSnapshot,

        @Schema(description = "Validation status from upstream recommendation")
        Boolean validationPassed,

        @Schema(description = "Topic recency warnings (if any)")
        List<TopicRecencyWarning> warnings
) {}
