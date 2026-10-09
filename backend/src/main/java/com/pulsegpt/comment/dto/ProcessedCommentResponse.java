package com.pulsegpt.comment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.comment.IntentType;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.comment.SentimentLabel;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessedCommentResponse(
        UUID id,
        UUID rawCommentId,
        String normalizedText,
        String language,
        Boolean isHinglish,
        SentimentLabel sentimentLabel,
        Double sentimentScore,
        IntentType intent,
        Double spamScore,
        Boolean isSpam,
        Boolean isDuplicate,
        Boolean piiMasked,
        Priority priority,
        String embeddingModel,
        Integer embeddingDimension,
        Instant processedAt,
        String processingVersion
) {}
