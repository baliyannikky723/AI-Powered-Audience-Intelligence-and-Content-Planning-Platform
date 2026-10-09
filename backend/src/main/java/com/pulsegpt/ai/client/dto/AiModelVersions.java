package com.pulsegpt.ai.client.dto;

public record AiModelVersions(
        String language,
        String sentiment,
        String intent,
        String embedding,
        String algorithm
) {}
