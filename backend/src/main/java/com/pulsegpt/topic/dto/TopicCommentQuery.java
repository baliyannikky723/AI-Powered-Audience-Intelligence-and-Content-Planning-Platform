package com.pulsegpt.topic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record TopicCommentQuery(
        @Min(value = 0, message = "Page index must not be less than zero")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        Integer size,

        String sort,

        String direction
) {
    public int getPageOrDefault() {
        return page != null ? page : 0;
    }

    public int getSizeOrDefault() {
        return size != null ? size : 20;
    }
}
