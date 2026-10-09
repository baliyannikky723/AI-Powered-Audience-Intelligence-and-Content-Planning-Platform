package com.pulsegpt.calendar.dto;

import com.pulsegpt.comment.Priority;
import com.pulsegpt.platform.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CreateCalendarItemRequest(
        @Schema(description = "Optional ID of an APPROVED content recommendation to plan")
        UUID recommendationId,

        @Schema(description = "Optional topic ID associated with this planned content")
        UUID topicId,

        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title cannot exceed 500 characters")
        @Schema(description = "Title of the planned content piece", example = "5 Mistakes in Production RAG")
        String title,

        @Schema(description = "Target publishing platform", example = "YOUTUBE")
        @NotNull(message = "Platform is required")
        PlatformType platform,

        @Schema(description = "Content format/type (e.g. VIDEO, SHORT, POST, CAROUSEL)", example = "VIDEO")
        String contentType,

        @Schema(description = "Scheduled start timestamp (ISO 8601)", example = "2026-10-15T18:00:00Z")
        @NotNull(message = "Scheduled start time is required")
        Instant scheduledStart,

        @Schema(description = "Scheduled end timestamp (ISO 8601, optional - defaults to start + 1 hour)", example = "2026-10-15T19:00:00Z")
        Instant scheduledEnd,

        @Schema(description = "Creator IANA timezone identifier", example = "Asia/Kolkata")
        @NotBlank(message = "Timezone is required")
        String timezone,

        @Schema(description = "Priority level: LOW, MEDIUM, HIGH", example = "HIGH")
        Priority priority,

        @Schema(description = "Editorial notes, production brief, or outline", example = "Tutorial based on audience questions")
        String notes
) {}
