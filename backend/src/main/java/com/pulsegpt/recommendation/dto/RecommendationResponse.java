package com.pulsegpt.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.RecommendationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationResponse(
        UUID id,
        UUID topicId,
        String topicName,
        String title,
        String description,
        String contentType,
        String angle,
        String targetAudience,
        String problemAddressed,
        String hook,
        String callToAction,
        List<String> keyPoints,
        Priority priority,
        String reason,
        RecommendationStatus status,
        GenerationMode generationMode,
        Double confidence,
        Boolean validationPassed,
        Map<String, Object> evidenceSnapshot,
        Map<String, Object> validation,
        Boolean repairAttempted,
        Map<String, Object> repairResult,
        String llmModel,
        String promptVersion,
        Instant approvedAt,
        UUID approvedBy,
        Instant createdAt,
        Instant updatedAt
) {}
