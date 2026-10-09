package com.pulsegpt.memory.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TopicContentIdeaResponse(
        UUID id,
        UUID topicId,
        String title,
        String angle,
        String status,
        Instant createdAt,
        Instant scheduledAt,
        String platform
) {
}
