package com.pulsegpt.topic.dto;

import com.pulsegpt.platform.PlatformType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ClusteringTriggerRequest(
        Instant startDate,
        Instant endDate,
        PlatformType platform,
        @Min(value = 5, message = "maxComments must be at least 5")
        @Max(value = 5000, message = "maxComments cannot exceed 5000")
        Integer maxComments,
        String algorithm,
        @Min(value = 2, message = "minClusterSize must be at least 2")
        Integer minClusterSize,
        Boolean umapEnabled,
        Integer kClusters
) {
    public int getMaxCommentsOrDefault() {
        return (maxComments != null && maxComments > 0) ? Math.min(maxComments, 5000) : 500;
    }
}
