package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiCommentProcessRequest(
        @JsonProperty("comment_id") String commentId,
        String text,
        @JsonProperty("language_hint") String languageHint,
        @JsonProperty("model_version") String modelVersion
) {}
