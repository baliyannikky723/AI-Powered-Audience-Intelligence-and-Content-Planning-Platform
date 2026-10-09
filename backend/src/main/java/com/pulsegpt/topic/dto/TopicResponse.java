package com.pulsegpt.topic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TopicResponse(
        UUID id,
        String name,
        String description,
        List<String> keywords,
        Map<String, Double> keywordScores,
        int commentCount,
        Map<String, Integer> sentimentDistribution,
        Map<String, Integer> intentDistribution,
        Map<String, Integer> languageDistribution,
        Map<String, Integer> platformDistribution,
        String algorithm,
        String algorithmVersion,
        boolean active,
        Instant firstSeenAt,
        Instant lastSeenAt,
        UUID lastClusteringRunId,
        Instant createdAt,
        Instant updatedAt
) {}
