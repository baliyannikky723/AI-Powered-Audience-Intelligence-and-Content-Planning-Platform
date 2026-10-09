package com.pulsegpt.memory.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MemoryRebuildResponse(
        String status,
        UUID userId,
        int topicCount,
        int questionCount,
        int contentIdeaCount,
        int relationshipCount,
        long durationMs,
        Instant timestamp
) {
}
