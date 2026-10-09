package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentBriefDto(
        String title,
        String contentType,
        String platform,
        String targetAudience,
        String audienceProblem,
        List<String> audienceEvidence,
        String coreMessage,
        String contentAngle,
        List<String> keyPoints,
        String tone,
        String callToAction,
        String successObjective
) {
}
