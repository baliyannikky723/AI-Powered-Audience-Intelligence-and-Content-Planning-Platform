package com.pulsegpt.topic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClusteringSummaryResponse(
        UUID runId,
        String status,
        int totalSubmitted,
        int clusteredCount,
        int noiseCount,
        int clusterCount,
        int matchedExistingTopicsCount,
        int newTopicsCreatedCount,
        List<TopicResponse> topics,
        Map<String, Object> metrics,
        Map<String, String> modelVersions,
        Instant completedAt
) {}
