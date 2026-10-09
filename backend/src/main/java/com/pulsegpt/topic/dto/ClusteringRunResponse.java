package com.pulsegpt.topic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.topic.ClusteringRunStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClusteringRunResponse(
        UUID id,
        ClusteringRunStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant timeWindowStart,
        Instant timeWindowEnd,
        int inputCommentCount,
        int clusteredCommentCount,
        int noiseCount,
        int clusterCount,
        String algorithm,
        String algorithmVersion,
        String embeddingModel,
        Integer embeddingDimension,
        Map<String, Object> config,
        Map<String, Object> metrics,
        String errorMessage,
        Instant createdAt
) {}
