package com.pulsegpt.comment.dto;

import com.pulsegpt.comment.IntentType;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.comment.SentimentLabel;
import com.pulsegpt.platform.PlatformType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ProcessedCommentQuery(
        @Min(value = 0, message = "Page index must not be less than zero")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        Integer size,

        String sort,

        String direction,

        UUID postId,

        PlatformType platform,

        SentimentLabel sentiment,

        IntentType intent,

        Priority priority,

        String language,

        Boolean isSpam,

        Boolean isDuplicate,

        Instant from,

        Instant to,

        @Size(max = 255, message = "Search query must not exceed 255 characters")
        String search
) {
    public int getPageOrDefault() {
        return page != null ? page : 0;
    }

    public int getSizeOrDefault() {
        return size != null ? size : 20;
    }

    public String getSortOrDefault() {
        return (sort != null && !sort.isBlank()) ? sort : "processedAt";
    }

    public String getDirectionOrDefault() {
        return (direction != null && !direction.isBlank()) ? direction : "DESC";
    }
}
