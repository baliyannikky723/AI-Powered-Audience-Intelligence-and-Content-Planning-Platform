package com.pulsegpt.graph.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.graph.repository.Neo4jKnowledgeGraphRepository;
import com.pulsegpt.memory.AudienceInterest;
import com.pulsegpt.memory.AudienceInterestRepository;
import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.memory.dto.*;
import com.pulsegpt.memory.service.AudienceMemoryProperties;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphQueryService {

    private final Neo4jKnowledgeGraphRepository graphRepository;
    private final AudienceInterestRepository interestRepository;
    private final TopicRepository topicRepository;
    private final ContentRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final AudienceMemoryProperties properties;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public List<AudienceInterestResponse> getActiveInterests(UUID userId) {
        return executeQuery(userId, "getActiveInterests", () -> {
            List<AudienceInterestResponse> neo4jResults = graphRepository.findInterestsByStatus(userId.toString(), "ACTIVE");
            if (!neo4jResults.isEmpty()) return neo4jResults;

            // Fallback to PostgreSQL
            List<AudienceInterest> pgResults = interestRepository.findByUserIdAndStatus(userId, AudienceInterestStatus.ACTIVE);
            return pgResults.stream()
                    .map(this::mapFromEntity)
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public List<AudienceInterestResponse> getWeakeningAudienceSignals(UUID userId) {
        return executeQuery(userId, "getWeakeningAudienceSignals", () -> {
            List<AudienceInterestResponse> neo4jResults = graphRepository.findInterestsByStatus(userId.toString(), "WEAKENING");
            if (!neo4jResults.isEmpty()) return neo4jResults;

            List<AudienceInterest> pgResults = interestRepository.findByUserIdAndStatus(userId, AudienceInterestStatus.WEAKENING);
            return pgResults.stream()
                    .map(this::mapFromEntity)
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public List<AudienceInterestResponse> getStrongestAudienceSignals(UUID userId) {
        return executeQuery(userId, "getStrongestAudienceSignals", () -> {
            List<AudienceInterestResponse> allInterests = graphRepository.findInterestsByStatus(userId.toString(), null);
            if (!allInterests.isEmpty()) {
                return allInterests.stream()
                        .sorted(Comparator.comparingDouble(AudienceInterestResponse::confidence).reversed())
                        .limit(10)
                        .toList();
            }

            List<AudienceInterest> pgResults = interestRepository.findByUserIdOrderByConfidenceDesc(userId);
            return pgResults.stream()
                    .limit(10)
                    .map(this::mapFromEntity)
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public List<AudienceQuestionResponse> getRecurringQuestions(UUID userId) {
        return executeQuery(userId, "getRecurringQuestions", () -> {
            return graphRepository.findRecurringQuestions(userId.toString(), 20);
        });
    }

    @Transactional(readOnly = true)
    public List<RelatedTopicResponse> getRelatedTopics(UUID userId, UUID topicId) {
        return executeQuery(userId, "getRelatedTopics", () -> {
            return graphRepository.findRelatedTopics(userId.toString(), topicId.toString(), 10);
        });
    }

    @Transactional(readOnly = true)
    public List<TopicContentIdeaResponse> getTopicContentIdeas(UUID userId, UUID topicId) {
        return executeQuery(userId, "getTopicContentIdeas", () -> {
            List<TopicContentIdeaResponse> results = graphRepository.findTopicContentIdeas(userId.toString(), topicId.toString());
            if (!results.isEmpty()) return results;

            // Fallback to PostgreSQL
            List<ContentRecommendation> recs = recommendationRepository.findByUserIdAndTopicId(userId, topicId);
            return recs.stream()
                    .map(r -> TopicContentIdeaResponse.builder()
                            .id(r.getId())
                            .topicId(topicId)
                            .title(r.getTitle())
                            .angle(r.getContentType() != null ? r.getContentType() : "VIDEO")
                            .status(r.getStatus() != null ? r.getStatus().name() : "DRAFT")
                            .createdAt(r.getCreatedAt())
                            .scheduledAt(null)
                            .platform(null)
                            .build())
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public AudienceMemorySummaryResponse getAudienceGraphSummary(UUID userId) {
        return executeQuery(userId, "getAudienceGraphSummary", () -> {
            List<AudienceInterestResponse> active = getActiveInterests(userId);
            List<AudienceInterestResponse> weakening = getWeakeningAudienceSignals(userId);
            List<AudienceQuestionResponse> questions = getRecurringQuestions(userId);

            int relatedTopicCount = 0;
            for (AudienceInterestResponse act : active) {
                relatedTopicCount += getRelatedTopics(userId, act.topicId()).size();
            }

            int contentIdeaCount = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId).size();

            return AudienceMemorySummaryResponse.builder()
                    .activeInterests(active)
                    .weakeningInterests(weakening)
                    .recurringQuestions(questions)
                    .relatedTopicCount(relatedTopicCount)
                    .contentIdeaCount(contentIdeaCount)
                    .lastUpdated(Instant.now())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public AudienceMemoryContext getAudienceMemoryContext(UUID userId) {
        if (!properties.isEnabled()) {
            return AudienceMemoryContext.empty();
        }

        try {
            List<AudienceInterestResponse> active = getActiveInterests(userId);
            List<AudienceInterestResponse> weakening = getWeakeningAudienceSignals(userId);
            List<AudienceQuestionResponse> questions = getRecurringQuestions(userId);

            List<RelatedTopicResponse> relatedTopics = new ArrayList<>();
            for (AudienceInterestResponse act : active.stream().limit(5).toList()) {
                relatedTopics.addAll(getRelatedTopics(userId, act.topicId()));
            }

            List<TopicContentIdeaResponse> recentIdeas = new ArrayList<>();
            for (AudienceInterestResponse act : active.stream().limit(3).toList()) {
                recentIdeas.addAll(getTopicContentIdeas(userId, act.topicId()));
            }

            return AudienceMemoryContext.builder()
                    .activeInterests(active)
                    .weakeningInterests(weakening)
                    .recurringQuestions(questions)
                    .relatedTopics(relatedTopics)
                    .recentContentIdeas(recentIdeas)
                    .memoryVersion("2026-10-v1")
                    .available(true)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to retrieve audience memory context for user {}: {}", userId, e.getMessage());
            return AudienceMemoryContext.empty();
        }
    }

    private <T> T executeQuery(UUID userId, String operation, QuerySupplier<T> supplier) {
        long start = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = supplier.get();
            sample.stop(Timer.builder("pulsegpt.memory.query.duration")
                    .tag("operation", operation)
                    .tag("status", "SUCCESS")
                    .register(meterRegistry));
            meterRegistry.counter("pulsegpt.memory.query", "operation", operation, "status", "SUCCESS").increment();
            return result;
        } catch (Exception e) {
            log.warn("Knowledge graph query {} failed for user {}: {}", operation, userId, e.getMessage());
            meterRegistry.counter("pulsegpt.memory.query", "operation", operation, "status", "FAILURE").increment();
            throw new RuntimeException("Knowledge graph query failed: " + operation, e);
        }
    }

    private AudienceInterestResponse mapFromEntity(AudienceInterest ai) {
        return AudienceInterestResponse.builder()
                .id(ai.getId())
                .topicId(ai.getTopic().getId())
                .topicName(ai.getTopic().getName())
                .confidence(Math.round(ai.getConfidence() * 100.0) / 100.0)
                .evidenceCount(ai.getEvidenceCount())
                .lastSeenAt(ai.getLastSeenAt())
                .status(ai.getStatus())
                .halfLifeDays(ai.getHalfLifeDays())
                .trend(ai.getConfidence() >= 0.7 ? "GROWING" : (ai.getConfidence() >= 0.4 ? "STABLE" : "WEAKENING"))
                .evidenceIds(List.of("topic:" + ai.getTopic().getId()))
                .build();
    }

    @FunctionalInterface
    private interface QuerySupplier<T> {
        T get() throws Exception;
    }
}
