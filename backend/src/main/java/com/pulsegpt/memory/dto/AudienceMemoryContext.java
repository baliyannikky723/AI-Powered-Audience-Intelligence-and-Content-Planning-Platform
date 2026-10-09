package com.pulsegpt.memory.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AudienceMemoryContext(
        List<AudienceInterestResponse> activeInterests,
        List<AudienceInterestResponse> weakeningInterests,
        List<AudienceQuestionResponse> recurringQuestions,
        List<RelatedTopicResponse> relatedTopics,
        List<TopicContentIdeaResponse> recentContentIdeas,
        String memoryVersion,
        boolean available
) {
    public static AudienceMemoryContext empty() {
        return AudienceMemoryContext.builder()
                .activeInterests(List.of())
                .weakeningInterests(List.of())
                .recurringQuestions(List.of())
                .relatedTopics(List.of())
                .recentContentIdeas(List.of())
                .memoryVersion("2026-10-v1")
                .available(false)
                .build();
    }
}
