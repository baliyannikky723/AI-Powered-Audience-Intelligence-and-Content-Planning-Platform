package com.pulsegpt.topic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.comment.IntentType;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.comment.SentimentLabel;
import com.pulsegpt.platform.PlatformType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TopicCommentResponse(
        UUID assignmentId,
        UUID processedCommentId,
        UUID rawCommentId,
        String text,
        String authorDisplayName,
        SentimentLabel sentimentLabel,
        Double sentimentScore,
        IntentType intent,
        Priority priority,
        String language,
        Boolean isHinglish,
        Double membershipProbability,
        PlatformType platform,
        Instant publishedAt,
        Instant assignedAt
) {}
