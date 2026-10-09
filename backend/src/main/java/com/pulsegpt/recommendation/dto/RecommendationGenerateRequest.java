package com.pulsegpt.recommendation.dto;

import com.pulsegpt.recommendation.GenerationMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RecommendationGenerateRequest(
        UUID topicId,
        String contentType,
        String goal,
        String targetAudience,
        @Min(value = 1, message = "maxIdeas must be at least 1")
        @Max(value = 10, message = "maxIdeas cannot exceed 10")
        Integer maxIdeas,
        GenerationMode mode
) {
    public String getContentTypeOrDefault() {
        return (contentType != null && !contentType.isBlank()) ? contentType.trim().toUpperCase() : "VIDEO";
    }

    public String getGoalOrDefault() {
        return (goal != null && !goal.isBlank()) ? goal.trim().toUpperCase() : "EDUCATIONAL";
    }

    public int getMaxIdeasOrDefault() {
        return (maxIdeas != null && maxIdeas > 0) ? Math.min(maxIdeas, 10) : 3;
    }

    public GenerationMode getModeOrDefault() {
        return mode != null ? mode : GenerationMode.EVIDENCE_GROUNDED;
    }
}
