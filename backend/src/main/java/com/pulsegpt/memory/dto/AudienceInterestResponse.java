package com.pulsegpt.memory.dto;

import com.pulsegpt.memory.AudienceInterestStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record AudienceInterestResponse(
        UUID id,
        UUID topicId,
        String topicName,
        Double confidence,
        int evidenceCount,
        Instant lastSeenAt,
        AudienceInterestStatus status,
        int halfLifeDays,
        String trend,
        List<String> evidenceIds
) {
}
