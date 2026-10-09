package com.pulsegpt.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentProcessingSummaryResponse {
    private String status;
    private int totalSubmitted;
    private int processedCount;
    private int failedCount;
    private int duplicateCount;
    private int spamCount;
    private int piiMaskedCount;
    private Map<String, String> modelVersions;
    private Instant completedAt;
}
