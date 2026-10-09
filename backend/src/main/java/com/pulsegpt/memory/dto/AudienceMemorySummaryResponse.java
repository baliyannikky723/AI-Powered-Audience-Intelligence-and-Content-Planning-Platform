package com.pulsegpt.memory.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record AudienceMemorySummaryResponse(
        List<AudienceInterestResponse> activeInterests,
        List<AudienceInterestResponse> weakeningInterests,
        List<AudienceQuestionResponse> recurringQuestions,
        int relatedTopicCount,
        int contentIdeaCount,
        Instant lastUpdated
) {
}
