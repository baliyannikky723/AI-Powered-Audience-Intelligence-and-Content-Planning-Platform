package com.pulsegpt.recommendation.service;

import com.pulsegpt.comment.IntentType;
import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.PostRepository;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.graph.service.KnowledgeGraphQueryService;
import com.pulsegpt.memory.dto.AudienceMemoryContext;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.recommendation.EvidenceSourceType;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.recommendation.dto.RecommendationGenerateRequest;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignment;
import com.pulsegpt.topic.TopicAssignmentRepository;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
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
public class EvidenceRetrievalService {

    private final TopicRepository topicRepository;
    private final ProcessedCommentRepository processedCommentRepository;
    private final TopicAssignmentRepository topicAssignmentRepository;
    private final PostRepository postRepository;
    private final ContentRecommendationRepository contentRecommendationRepository;
    private final KnowledgeGraphQueryService knowledgeGraphQueryService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w\\.-]+@[\\w\\.-]+\\.\\w+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    @Transactional(readOnly = true)
    public RecommendationContext retrieveEvidence(User user, RecommendationGenerateRequest request) {
        log.info("EVIDENCE_RETRIEVED: Starting evidence retrieval for user {} topicId {}", user.getId(), request.topicId());

        List<Topic> topics = new ArrayList<>();
        if (request.topicId() != null) {
            topicRepository.findByIdAndUserId(request.topicId(), user.getId()).ifPresent(topics::add);
        }
        if (topics.isEmpty()) {
            topics = topicRepository.findByUserIdAndActiveTrue(user.getId());
        }

        // 1. Retrieve Representative Comments & Questions
        List<ProcessedComment> questions = new ArrayList<>();
        List<ProcessedComment> representativeComments = new ArrayList<>();

        if (!topics.isEmpty()) {
            for (Topic t : topics) {
                List<TopicAssignment> assignments = topicAssignmentRepository.findByTopicId(t.getId());
                for (TopicAssignment ta : assignments) {
                    if (ta.isNoise()) continue;
                    ProcessedComment pc = ta.getProcessedComment();
                    if (pc.getIntent() == IntentType.QUESTION) {
                        if (questions.size() < 10) questions.add(pc);
                    } else {
                        if (representativeComments.size() < 10) representativeComments.add(pc);
                    }
                }
            }
        }

        // Fallback if no topic assignments yet: retrieve eligible processed comments directly
        if (questions.isEmpty() && representativeComments.isEmpty()) {
            List<ProcessedComment> directComments = processedCommentRepository.findEligibleForClusteringByUserId(
                    user.getId(), PageRequest.of(0, 20));
            for (ProcessedComment pc : directComments) {
                if (pc.getIntent() == IntentType.QUESTION) {
                    questions.add(pc);
                } else {
                    representativeComments.add(pc);
                }
            }
        }

        // 2. Retrieve Content History & Previous Recommendations
        List<Post> contentHistory = postRepository.findByPlatformAccountUserIdOrderByPublishedAtDesc(
                user.getId(), PageRequest.of(0, 10));

        List<ContentRecommendation> previousRecs = contentRecommendationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().limit(10).toList();

        // 3. Build & Rank Normalized Evidence Items
        List<EvidenceItem> evidenceItems = new ArrayList<>();

        // Add Topic Evidence
        for (Topic t : topics) {
            double relevance = 0.95;
            evidenceItems.add(EvidenceItem.builder()
                    .evidenceId("topic:" + t.getId())
                    .sourceType(EvidenceSourceType.TOPIC)
                    .sourceId(t.getId())
                    .userId(user.getId())
                    .summary(sanitizeText(t.getName() + " - " + (t.getKeywords() != null ? String.join(", ", t.getKeywords()) : "")))
                    .relevanceScore(relevance)
                    .createdAt(t.getCreatedAt() != null ? t.getCreatedAt() : Instant.now())
                    .metadata(Map.of(
                            "commentCount", t.getCommentCount(),
                            "keywords", t.getKeywords() != null ? t.getKeywords() : Collections.emptyList()
                    ))
                    .build());
        }

        // Add Question Evidence
        for (ProcessedComment q : questions) {
            String snippet = q.getNormalizedText() != null ? q.getNormalizedText() : q.getRawComment().getRawText();
            double score = calculateEvidenceScore(q, 1.0);
            evidenceItems.add(EvidenceItem.builder()
                    .evidenceId("question:" + q.getId())
                    .sourceType(EvidenceSourceType.QUESTION)
                    .sourceId(q.getId())
                    .userId(user.getId())
                    .summary(sanitizeText(snippet))
                    .relevanceScore(score)
                    .createdAt(q.getRawComment().getPublishedAt())
                    .metadata(Map.of("intent", "QUESTION", "sentiment", q.getSentimentLabel() != null ? q.getSentimentLabel().name() : "NEUTRAL"))
                    .build());
        }

        // Add Feedback / Comment Evidence
        for (ProcessedComment c : representativeComments) {
            String snippet = c.getNormalizedText() != null ? c.getNormalizedText() : c.getRawComment().getRawText();
            double score = calculateEvidenceScore(c, 0.8);
            evidenceItems.add(EvidenceItem.builder()
                    .evidenceId("comment:" + c.getId())
                    .sourceType(EvidenceSourceType.COMMENT)
                    .sourceId(c.getId())
                    .userId(user.getId())
                    .summary(sanitizeText(snippet))
                    .relevanceScore(score)
                    .createdAt(c.getRawComment().getPublishedAt())
                    .metadata(Map.of("sentiment", c.getSentimentLabel() != null ? c.getSentimentLabel().name() : "NEUTRAL"))
                    .build());
        }

        // Sort descending by relevance score & bound top 15
        evidenceItems.sort(Comparator.comparingDouble(EvidenceItem::relevanceScore).reversed());
        List<EvidenceItem> boundedEvidence = evidenceItems.stream().limit(15).toList();

        // Retrieve graph memory context when using EVIDENCE_GROUNDED mode
        AudienceMemoryContext memoryContext = null;
        if (request.getModeOrDefault() == GenerationMode.EVIDENCE_GROUNDED) {
            try {
                memoryContext = knowledgeGraphQueryService.getAudienceMemoryContext(user.getId());
            } catch (Exception e) {
                log.warn("Could not retrieve graph memory context for user {}: {}", user.getId(), e.getMessage());
                memoryContext = AudienceMemoryContext.empty();
            }
        }

        return RecommendationContext.builder()
                .request(request)
                .evidenceItems(boundedEvidence)
                .topics(topics)
                .questions(questions)
                .representativeComments(representativeComments)
                .contentHistory(contentHistory)
                .previousRecommendations(previousRecs)
                .memoryContext(memoryContext)
                .build();
    }

    private double calculateEvidenceScore(ProcessedComment pc, double intentWeight) {
        double recencyScore = 1.0;
        if (pc.getRawComment().getPublishedAt() != null) {
            long daysOld = Duration.between(pc.getRawComment().getPublishedAt(), Instant.now()).toDays();
            recencyScore = Math.max(0.2, 1.0 - (daysOld / 90.0));
        }
        double confidence = pc.getSentimentScore() != null ? pc.getSentimentScore() : 0.8;
        // Deterministic formula: 0.4*topicRelevance + 0.3*intentWeight + 0.2*recency + 0.1*confidence
        double total = (0.4 * 0.9) + (0.3 * intentWeight) + (0.2 * recencyScore) + (0.1 * confidence);
        return Math.round(total * 100.0) / 100.0;
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        String clean = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL_REDACTED]");
        clean = PHONE_PATTERN.matcher(clean).replaceAll("[PHONE_REDACTED]");
        return clean.trim();
    }
}
