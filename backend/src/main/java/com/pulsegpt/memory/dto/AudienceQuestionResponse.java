package com.pulsegpt.memory.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AudienceQuestionResponse(
        String id,
        String questionHash,
        String normalizedText,
        Double confidence,
        int evidenceCount,
        Instant firstSeenAt,
        Instant lastSeenAt,
        UUID topicId,
        String topicName
) {
}
