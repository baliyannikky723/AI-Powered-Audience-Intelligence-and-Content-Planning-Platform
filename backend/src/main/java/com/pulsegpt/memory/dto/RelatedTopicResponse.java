package com.pulsegpt.memory.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RelatedTopicResponse(
        UUID sourceTopicId,
        UUID targetTopicId,
        String targetTopicName,
        Double weight,
        int cooccurrenceCount
) {
}
