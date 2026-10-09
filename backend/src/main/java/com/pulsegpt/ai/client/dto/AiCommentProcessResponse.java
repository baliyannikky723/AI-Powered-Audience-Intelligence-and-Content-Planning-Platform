package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pulsegpt.comment.Priority;

import java.util.List;

public record AiCommentProcessResponse(
        @JsonProperty("comment_id") String commentId,
        String language,
        @JsonProperty("is_hinglish") Boolean isHinglish,
        @JsonProperty("cleaned_text") String cleanedText,
        @JsonProperty("normalized_text") String normalizedText,
        @JsonProperty("is_spam") Boolean isSpam,
        @JsonProperty("spam_score") Double spamScore,
        @JsonProperty("spam_reasons") List<String> spamReasons,
        @JsonProperty("is_duplicate") Boolean isDuplicate,
        @JsonProperty("pii_detected") Boolean piiDetected,
        @JsonProperty("pii_masked_text") String piiMaskedText,
        AiSentimentResult sentiment,
        AiIntentResult intent,
        Priority priority,
        List<Double> embedding,
        @JsonProperty("embedding_dimension") Integer embeddingDimension,
        @JsonProperty("model_versions") AiModelVersions modelVersions,
        Boolean success,
        @JsonProperty("error_message") String errorMessage
) {}
