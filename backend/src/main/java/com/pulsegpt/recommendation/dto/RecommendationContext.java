package com.pulsegpt.recommendation.dto;

import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.memory.dto.AudienceMemoryContext;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.topic.Topic;
import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationContext(
        RecommendationGenerateRequest request,
        List<EvidenceItem> evidenceItems,
        List<Topic> topics,
        List<ProcessedComment> questions,
        List<ProcessedComment> representativeComments,
        List<Post> contentHistory,
        List<ContentRecommendation> previousRecommendations,
        AudienceMemoryContext memoryContext
) {}
