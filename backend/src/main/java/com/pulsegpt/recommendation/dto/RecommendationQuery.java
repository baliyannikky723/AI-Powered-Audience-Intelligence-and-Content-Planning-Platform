package com.pulsegpt.recommendation.dto;

import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.RecommendationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RecommendationQuery(
        @Min(value = 0, message = "Page index must not be less than zero")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        Integer size,

        String sort,

        String direction,

        UUID topicId,

        RecommendationStatus status,

        GenerationMode mode
) {
    public int getPageOrDefault() {
        return page != null ? page : 0;
    }

    public int getSizeOrDefault() {
        return size != null ? size : 20;
    }

    public String getSortOrDefault() {
        return (sort != null && !sort.isBlank()) ? sort : "createdAt";
    }

    public String getDirectionOrDefault() {
        return (direction != null && !direction.isBlank()) ? direction : "DESC";
    }
}
