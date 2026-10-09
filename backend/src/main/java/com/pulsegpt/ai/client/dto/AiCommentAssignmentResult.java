package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AiCommentAssignmentResult(
        @JsonProperty("comment_id") String commentId,
        @JsonProperty("cluster_id") Integer clusterId,
        @JsonProperty("is_noise") Boolean isNoise,
        @JsonProperty("membership_probability") Double membershipProbability
) {}
