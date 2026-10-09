package com.pulsegpt.comment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.platform.PlatformType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RawCommentResponse(
        UUID id,
        UUID postId,
        String postTitle,
        PlatformType platform,
        String externalCommentId,
        String authorDisplayName,
        String rawText,
        Instant publishedAt,
        int likes,
        int replies,
        Map<String, Object> metadata,
        Instant importedAt
) {}
