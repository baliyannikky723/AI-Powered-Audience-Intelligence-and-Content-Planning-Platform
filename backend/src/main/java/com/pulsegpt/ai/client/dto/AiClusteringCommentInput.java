package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AiClusteringCommentInput(
        @JsonProperty("comment_id") String commentId,
        String text,
        List<Double> embedding,
        String language,
        String sentiment,
        String intent,
        String platform,
        @JsonProperty("published_at") String publishedAt
) {}
