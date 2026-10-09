package com.pulsegpt.comment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.platform.PlatformType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostResponse(
        UUID id,
        UUID platformAccountId,
        PlatformType platform,
        String accountName,
        String externalPostId,
        String title,
        String url,
        Instant publishedAt,
        Long viewsCount,
        Long likesCount,
        Long commentsCount,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {}
