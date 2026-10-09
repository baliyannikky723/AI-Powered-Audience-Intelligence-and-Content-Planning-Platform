package com.pulsegpt.rag.service;

import com.pulsegpt.comment.IntentType;
import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.PostRepository;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.rag.dto.RagEvidenceItem;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.recommendation.EvidenceSourceType;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignment;
import com.pulsegpt.topic.TopicAssignmentRepository;
import com.pulsegpt.topic.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredRetrievalService {

    private final TopicRepository topicRepository;
    private final TopicAssignmentRepository topicAssignmentRepository;
    private final ProcessedCommentRepository processedCommentRepository;
    private final PostRepository postRepository;
    private final ContentRecommendationRepository contentRecommendationRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w\\.-]+@[\\w\\.-]+\\.\\w+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    @Transactional(readOnly = true)
    public List<RagEvidenceItem> retrieveStructuredEvidence(UUID userId, UUID targetTopicId, String queryText) {
        List<RagEvidenceItem> items = new ArrayList<>();

        // 1. Retrieve Topics
        List<Topic> topics = new ArrayList<>();
        if (targetTopicId != null) {
            topicRepository.findByIdAndUserId(targetTopicId, userId).ifPresent(topics::add);
        }
        if (topics.isEmpty()) {
            topics = topicRepository.findByUserIdAndActiveTrue(userId);
        }

        for (Topic t : topics) {
            double recency = 1.0;
            if (t.getUpdatedAt() != null) {
                long daysOld = Duration.between(t.getUpdatedAt(), Instant.now()).toDays();
                recency = Math.max(0.2, 1.0 - (daysOld / 180.0));
            }
            double volume = Math.min(1.0, t.getCommentCount() / 100.0);
            double quality = 0.90;
            double relevance = 0.95;
            double score = Math.round(((0.40 * relevance) + (0.25 * recency) + (0.20 * volume) + (0.15 * quality)) * 100.0) / 100.0;

            String keywordStr = t.getKeywords() != null ? String.join(", ", t.getKeywords()) : "";
            String summary = sanitizeText(t.getName() + " - Keywords: " + keywordStr);

            items.add(RagEvidenceItem.builder()
                    .evidenceId("topic:" + t.getId())
                    .sourceType(EvidenceSourceType.TOPIC)
                    .sourceId(t.getId().toString())
                    .userId(userId)
                    .text(summary)
                    .similarity(0.85)
                    .recency(recency)
                    .relevance(relevance)
                    .quality(quality)
                    .evidenceScore(score)
                    .metadata(Map.of(
                            "topicName", t.getName(),
                            "commentCount", t.getCommentCount(),
                            "keywords", t.getKeywords() != null ? t.getKeywords() : Collections.emptyList()
                    ))
                    .createdAt(t.getCreatedAt() != null ? t.getCreatedAt() : Instant.now())
                    .build());
        }

        // 2. Retrieve Questions from Topic Assignments
        List<ProcessedComment> questions = new ArrayList<>();
        for (Topic t : topics) {
            List<TopicAssignment> assignments = topicAssignmentRepository.findByTopicId(t.getId());
            for (TopicAssignment ta : assignments) {
                if (ta.isNoise()) continue;
                ProcessedComment pc = ta.getProcessedComment();
                if (pc.getIntent() == IntentType.QUESTION) {
                    if (questions.size() < 15) questions.add(pc);
                }
            }
        }

        for (ProcessedComment q : questions) {
            String text = q.getNormalizedText() != null ? q.getNormalizedText() : q.getRawComment().getRawText();
            Instant pubAt = q.getRawComment().getPublishedAt() != null ? q.getRawComment().getPublishedAt() : q.getProcessedAt();
            long daysOld = Duration.between(pubAt != null ? pubAt : Instant.now(), Instant.now()).toDays();
            double recency = Math.max(0.1, 1.0 - (daysOld / 180.0));
            double relevance = 0.90;
            double quality = q.getSentimentScore() != null ? Math.abs(q.getSentimentScore()) : 0.85;
            double volume = 0.6;
            double score = Math.round(((0.40 * relevance) + (0.25 * recency) + (0.20 * volume) + (0.15 * quality)) * 100.0) / 100.0;

            items.add(RagEvidenceItem.builder()
                    .evidenceId("question:" + q.getId())
                    .sourceType(EvidenceSourceType.QUESTION)
                    .sourceId(q.getId().toString())
                    .userId(userId)
                    .text(sanitizeText(text))
                    .similarity(0.80)
                    .recency(recency)
                    .relevance(relevance)
                    .quality(quality)
                    .evidenceScore(score)
                    .metadata(Map.of(
                            "intent", "QUESTION",
                            "sentiment", q.getSentimentLabel() != null ? q.getSentimentLabel().name() : "NEUTRAL"
                    ))
                    .createdAt(pubAt)
                    .build());
        }

        // 3. Retrieve Content History Posts
        List<Post> posts = postRepository.findByPlatformAccountUserIdOrderByPublishedAtDesc(
                userId, PageRequest.of(0, 5));

        for (Post p : posts) {
            double recency = 1.0;
            if (p.getPublishedAt() != null) {
                long daysOld = Duration.between(p.getPublishedAt(), Instant.now()).toDays();
                recency = Math.max(0.1, 1.0 - (daysOld / 365.0));
            }
            double score = Math.round(((0.40 * 0.70) + (0.25 * recency) + (0.20 * 0.4) + (0.15 * 0.8)) * 100.0) / 100.0;

            items.add(RagEvidenceItem.builder()
                    .evidenceId("content_history:" + p.getId())
                    .sourceType(EvidenceSourceType.CONTENT_HISTORY)
                    .sourceId(p.getId().toString())
                    .userId(userId)
                    .text(sanitizeText("Past Published Content: " + p.getTitle()))
                    .similarity(0.70)
                    .recency(recency)
                    .relevance(0.70)
                    .quality(0.80)
                    .evidenceScore(score)
                    .metadata(Map.of(
                            "title", p.getTitle(),
                            "url", p.getUrl() != null ? p.getUrl() : ""
                    ))
                    .createdAt(p.getPublishedAt() != null ? p.getPublishedAt() : Instant.now())
                    .build());
        }

        return items;
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        String clean = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL_REDACTED]");
        clean = PHONE_PATTERN.matcher(clean).replaceAll("[PHONE_REDACTED]");
        return clean.trim();
    }
}
