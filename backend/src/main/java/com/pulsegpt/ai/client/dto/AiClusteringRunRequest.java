package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AiClusteringRunRequest(
        @JsonProperty("run_id") String runId,
        List<AiClusteringCommentInput> comments,
        AiClusteringConfig config
) {}
