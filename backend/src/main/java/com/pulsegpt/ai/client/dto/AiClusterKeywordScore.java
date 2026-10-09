package com.pulsegpt.ai.client.dto;

import lombok.Builder;

@Builder
public record AiClusterKeywordScore(
        String keyword,
        Double score
) {}
