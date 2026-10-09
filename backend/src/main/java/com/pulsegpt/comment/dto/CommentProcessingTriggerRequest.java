package com.pulsegpt.comment.dto;

import com.pulsegpt.platform.PlatformType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentProcessingTriggerRequest {

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 500, message = "Limit cannot exceed 500 comments per processing run")
    @Builder.Default
    private int limit = 100;

    private PlatformType platform;

    private Instant from;

    private Instant to;
}
