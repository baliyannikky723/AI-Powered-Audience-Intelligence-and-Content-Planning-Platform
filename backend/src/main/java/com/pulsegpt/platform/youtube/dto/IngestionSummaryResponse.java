package com.pulsegpt.platform.youtube.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.ingestion.IngestionRunStatus;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IngestionSummaryResponse(
        UUID runId,
        IngestionRunStatus status,
        int postsProcessed,
        int commentsProcessed,
        int duplicatePosts,
        int duplicateComments,
        int itemsFailed,
        Instant startedAt,
        Instant completedAt,
        String message
) {}
