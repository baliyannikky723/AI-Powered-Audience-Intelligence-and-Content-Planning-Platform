package com.pulsegpt.graph.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.comment.IntentType;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.graph.repository.Neo4jKnowledgeGraphRepository;
import com.pulsegpt.memory.AudienceInterest;
import com.pulsegpt.memory.AudienceInterestRepository;
import com.pulsegpt.memory.dto.MemoryRebuildResponse;
import com.pulsegpt.memory.service.AudienceMemoryProperties;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignment;
import com.pulsegpt.topic.TopicAssignmentRepository;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphProjectionService {

    private final Neo4jKnowledgeGraphRepository graphRepository;
    private final AudienceInterestRepository interestRepository;
    private final TopicRepository topicRepository;
    private final TopicAssignmentRepository topicAssignmentRepository;
    private final ProcessedCommentRepository processedCommentRepository;
    private final ContentRecommendationRepository recommendationRepository;
    private final CalendarItemRepository calendarItemRepository;
    private final UserRepository userRepository;
    private final AudienceMemoryProperties properties;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w\\.-]+@[\\w\\.-]+\\.\\w+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    public void projectAudience(UUID userId) {
        if (!properties.isEnabled()) return;
        try {
            graphRepository.mergeAudience(userId.toString(), Instant.now());
        } catch (Exception e) {
            handleProjectionError("projectAudience", userId, e);
        }
    }

    @Transactional(readOnly = true)
    public void projectTopic(UUID topicId) {
        if (!properties.isEnabled()) return;
        try {
            Topic topic = topicRepository.findById(topicId).orElse(null);
            if (topic == null || topic.getUser() == null) return;
            UUID userId = topic.getUser().getId();

            AudienceInterest interest = interestRepository.findByUserIdAndTopicId(userId, topicId).orElse(null);
            double confidence = interest != null ? interest.getConfidence() : 0.85;
            String status = interest != null ? interest.getStatus().name() : (topic.isActive() ? "ACTIVE" : "INACTIVE");
            int evidenceCount = interest != null ? interest.getEvidenceCount() : topic.getCommentCount();
            Instant firstSeen = topic.getCreatedAt() != null ? topic.getCreatedAt() : Instant.now();
            Instant lastSeen = interest != null ? interest.getLastSeenAt() : (topic.getUpdatedAt() != null ? topic.getUpdatedAt() : Instant.now());

            graphRepository.mergeTopic(
                    userId.toString(),
                    topic.getId().toString(),
                    topic.getName(),
                    status,
                    confidence,
                    firstSeen,
                    lastSeen,
                    evidenceCount,
                    Instant.now()
            );
        } catch (Exception e) {
            handleProjectionError("projectTopic", null, e);
        }
    }

    @Transactional(readOnly = true)
    public void projectQuestion(UUID commentId) {
        if (!properties.isEnabled()) return;
        try {
            ProcessedComment pc = processedCommentRepository.findById(commentId).orElse(null);
            if (pc == null || pc.getIntent() != IntentType.QUESTION) return;
            if (pc.getRawComment() == null || pc.getRawComment().getPost() == null ||
                    pc.getRawComment().getPost().getPlatformAccount() == null) return;

            User user = pc.getRawComment().getPost().getPlatformAccount().getUser();
            if (user == null) return;

            String rawText = pc.getNormalizedText() != null ? pc.getNormalizedText() : pc.getRawComment().getRawText();
            String cleanText = sanitizeText(rawText);
            String hash = generateHash(cleanText);

            // Find topic if assigned
            List<TopicAssignment> assignments = topicAssignmentRepository.findByProcessedCommentId(pc.getId());
            String topicId = null;
            if (!assignments.isEmpty() && assignments.get(0).getTopic() != null) {
                topicId = assignments.get(0).getTopic().getId().toString();
            }

            Instant seenAt = pc.getRawComment().getPublishedAt() != null ? pc.getRawComment().getPublishedAt() : Instant.now();
            double confidence = pc.getSentimentScore() != null ? pc.getSentimentScore() : 0.85;

            graphRepository.mergeQuestion(
                    user.getId().toString(),
                    hash,
                    cleanText,
                    topicId,
                    confidence,
                    seenAt,
                    seenAt,
                    1,
                    Instant.now()
            );
        } catch (Exception e) {
            handleProjectionError("projectQuestion", null, e);
        }
    }

    @Transactional(readOnly = true)
    public void projectRecommendation(UUID recommendationId) {
        if (!properties.isEnabled()) return;
        try {
            ContentRecommendation rec = recommendationRepository.findById(recommendationId).orElse(null);
            if (rec == null || rec.getUser() == null) return;

            String topicId = rec.getTopic() != null ? rec.getTopic().getId().toString() : null;
            graphRepository.mergeContentIdea(
                    rec.getUser().getId().toString(),
                    rec.getId().toString(),
                    topicId,
                    rec.getTitle(),
                    rec.getContentType() != null ? rec.getContentType() : "VIDEO",
                    rec.getStatus() != null ? rec.getStatus().name() : "DRAFT",
                    Instant.now()
            );
        } catch (Exception e) {
            handleProjectionError("projectRecommendation", null, e);
        }
    }

    @Transactional(readOnly = true)
    public void projectCalendarItem(UUID calendarItemId) {
        if (!properties.isEnabled()) return;
        try {
            CalendarItem item = calendarItemRepository.findById(calendarItemId).orElse(null);
            if (item == null || item.getUser() == null) return;

            String recId = item.getRecommendation() != null ? item.getRecommendation().getId().toString() : null;
            graphRepository.mergeCalendarItem(
                    item.getUser().getId().toString(),
                    item.getId().toString(),
                    recId,
                    item.getTitle(),
                    item.getScheduledAt(),
                    item.getPlatform() != null ? item.getPlatform().name() : "YOUTUBE",
                    item.getStatus() != null ? item.getStatus().name() : "SCHEDULED"
            );
        } catch (Exception e) {
            handleProjectionError("projectCalendarItem", null, e);
        }
    }

    @Transactional
    public MemoryRebuildResponse rebuildUserGraph(UUID userId) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);

        log.info("MEMORY_REBUILD_STARTED: Rebuilding knowledge graph for user {}", userId);

        int topicCount = 0;
        int questionCount = 0;
        int contentIdeaCount = 0;
        int relationshipCount = 0;

        try {
            // 1. Clear user's graph projection in Neo4j safely (User-scoped ONLY)
            graphRepository.clearUserGraph(userId.toString());

            // 2. Project Audience node
            Instant now = Instant.now();
            graphRepository.mergeAudience(userId.toString(), now);

            // 3. Project Topics & Interests
            List<Topic> topics = topicRepository.findByUserId(userId);
            for (Topic t : topics) {
                AudienceInterest interest = interestRepository.findByUserIdAndTopicId(userId, t.getId()).orElse(null);
                double confidence = interest != null ? interest.getConfidence() : 0.85;
                String status = interest != null ? interest.getStatus().name() : (t.isActive() ? "ACTIVE" : "INACTIVE");
                int evidenceCount = interest != null ? interest.getEvidenceCount() : t.getCommentCount();
                Instant firstSeen = t.getCreatedAt() != null ? t.getCreatedAt() : now;
                Instant lastSeen = interest != null ? interest.getLastSeenAt() : (t.getUpdatedAt() != null ? t.getUpdatedAt() : now);

                graphRepository.mergeTopic(
                        userId.toString(),
                        t.getId().toString(),
                        t.getName(),
                        status,
                        confidence,
                        firstSeen,
                        lastSeen,
                        evidenceCount,
                        now
                );
                topicCount++;
                relationshipCount++; // INTERESTED_IN
            }

            // 4. Project Topic-to-Topic Co-occurrences
            for (int i = 0; i < topics.size(); i++) {
                for (int j = i + 1; j < topics.size(); j++) {
                    Topic t1 = topics.get(i);
                    Topic t2 = topics.get(j);
                    double weight = calculateTopicSimilarity(t1, t2);
                    if (weight >= 0.25) {
                        graphRepository.mergeTopicRelation(userId.toString(), t1.getId().toString(), t2.getId().toString(), weight, 1);
                        relationshipCount++;
                    }
                }
            }

            // 5. Project Questions (Consolidated & deduplicated by hash)
            Map<String, QuestionConsolidation> questionMap = new HashMap<>();
            List<TopicAssignment> assignments = topicAssignmentRepository.findByUserId(userId);

            for (TopicAssignment ta : assignments) {
                if (ta.isNoise()) continue;
                ProcessedComment pc = ta.getProcessedComment();
                if (pc.getIntent() == IntentType.QUESTION) {
                    String clean = sanitizeText(pc.getNormalizedText() != null ? pc.getNormalizedText() : pc.getRawComment().getRawText());
                    String hash = generateHash(clean);
                    Instant seen = pc.getRawComment().getPublishedAt() != null ? pc.getRawComment().getPublishedAt() : now;
                    double conf = pc.getSentimentScore() != null ? pc.getSentimentScore() : 0.85;
                    String topicId = ta.getTopic() != null ? ta.getTopic().getId().toString() : null;

                    questionMap.compute(hash, (k, existing) -> {
                        if (existing == null) {
                            return new QuestionConsolidation(hash, clean, topicId, conf, seen, seen, 1);
                        } else {
                            existing.count++;
                            if (seen.isAfter(existing.lastSeen)) existing.lastSeen = seen;
                            if (seen.isBefore(existing.firstSeen)) existing.firstSeen = seen;
                            existing.confidence = Math.min(0.99, existing.confidence + 0.02);
                            return existing;
                        }
                    });
                }
            }

            for (QuestionConsolidation q : questionMap.values()) {
                graphRepository.mergeQuestion(
                        userId.toString(),
                        q.hash,
                        q.text,
                        q.topicId,
                        q.confidence,
                        q.firstSeen,
                        q.lastSeen,
                        q.count,
                        now
                );
                questionCount++;
                relationshipCount += 2; // ASKS + BELONGS_TO
            }

            // 6. Project Recommendations & Calendar Items
            List<ContentRecommendation> recommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            for (ContentRecommendation rec : recommendations) {
                String tid = rec.getTopic() != null ? rec.getTopic().getId().toString() : null;
                graphRepository.mergeContentIdea(
                        userId.toString(),
                        rec.getId().toString(),
                        tid,
                        rec.getTitle(),
                        rec.getContentType() != null ? rec.getContentType() : "VIDEO",
                        rec.getStatus() != null ? rec.getStatus().name() : "DRAFT",
                        rec.getCreatedAt() != null ? rec.getCreatedAt() : now
                );
                contentIdeaCount++;
                if (tid != null) relationshipCount++; // GENERATES
            }

            List<CalendarItem> calendarItems = calendarItemRepository.findByUserId(userId);
            for (CalendarItem ci : calendarItems) {
                String recId = ci.getRecommendation() != null ? ci.getRecommendation().getId().toString() : null;
                graphRepository.mergeCalendarItem(
                        userId.toString(),
                        ci.getId().toString(),
                        recId,
                        ci.getTitle(),
                        ci.getScheduledAt(),
                        ci.getPlatform() != null ? ci.getPlatform().name() : "YOUTUBE",
                        ci.getStatus() != null ? ci.getStatus().name() : "SCHEDULED"
                );
                if (recId != null) relationshipCount++; // SCHEDULED_ON
            }

            long duration = System.currentTimeMillis() - startTime;
            sample.stop(Timer.builder("pulsegpt.memory.projection.duration")
                    .tag("mode", "FULL_REBUILD")
                    .tag("status", "SUCCESS")
                    .register(meterRegistry));

            meterRegistry.counter("pulsegpt.memory.projection", "mode", "FULL_REBUILD", "status", "SUCCESS").increment();
            meterRegistry.counter("pulsegpt.memory.rebuild", "status", "SUCCESS").increment();

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                auditService.logAuditEvent(user, "MEMORY_REBUILT", userId.toString(), Map.of(
                        "operation", "FULL_REBUILD",
                        "topicCount", topicCount,
                        "questionCount", questionCount,
                        "contentIdeaCount", contentIdeaCount,
                        "relationshipCount", relationshipCount,
                        "durationMs", duration
                ));
            }

            log.info("MEMORY_REBUILD_COMPLETED: User {} rebuilt in {}ms (topics={}, questions={}, ideas={}, rels={})",
                    userId, duration, topicCount, questionCount, contentIdeaCount, relationshipCount);

            return MemoryRebuildResponse.builder()
                    .status("SUCCESS")
                    .userId(userId)
                    .topicCount(topicCount)
                    .questionCount(questionCount)
                    .contentIdeaCount(contentIdeaCount)
                    .relationshipCount(relationshipCount)
                    .durationMs(duration)
                    .timestamp(Instant.now())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("MEMORY_REBUILD_FAILED for user {}: {}", userId, e.getMessage(), e);

            meterRegistry.counter("pulsegpt.memory.projection", "mode", "FULL_REBUILD", "status", "FAILURE").increment();
            meterRegistry.counter("pulsegpt.memory.projection.failures", "operation", "rebuildUserGraph").increment();

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                auditService.logSecurityEvent(user, "MEMORY_PROJECTION_FAILED", "Memory rebuild failed: " + e.getMessage());
            }

            return MemoryRebuildResponse.builder()
                    .status("FAILED")
                    .userId(userId)
                    .topicCount(0)
                    .questionCount(0)
                    .contentIdeaCount(0)
                    .relationshipCount(0)
                    .durationMs(duration)
                    .timestamp(Instant.now())
                    .build();
        }
    }

    @Transactional
    public void projectIncremental(UUID userId) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Project active audience
            projectAudience(userId);

            // Project all active topics
            List<Topic> topics = topicRepository.findByUserIdAndActiveTrue(userId);
            for (Topic t : topics) {
                projectTopic(t.getId());
            }

            sample.stop(Timer.builder("pulsegpt.memory.projection.duration")
                    .tag("mode", "INCREMENTAL")
                    .tag("status", "SUCCESS")
                    .register(meterRegistry));
            meterRegistry.counter("pulsegpt.memory.projection", "mode", "INCREMENTAL", "status", "SUCCESS").increment();

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                auditService.logAuditEvent(user, "MEMORY_PROJECTED", userId.toString(), Map.of(
                        "operation", "INCREMENTAL",
                        "topicCount", topics.size(),
                        "durationMs", System.currentTimeMillis() - startTime
                ));
            }
        } catch (Exception e) {
            handleProjectionError("projectIncremental", userId, e);
        }
    }

    private double calculateTopicSimilarity(Topic t1, Topic t2) {
        if (t1.getKeywords() == null || t2.getKeywords() == null) return 0.0;
        Set<String> set1 = new HashSet<>(t1.getKeywords());
        Set<String> set2 = new HashSet<>(t2.getKeywords());
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private void handleProjectionError(String operation, UUID userId, Exception e) {
        log.warn("Neo4j Knowledge Graph operation {} failed: {}", operation, e.getMessage());
        meterRegistry.counter("pulsegpt.memory.projection.failures", "operation", operation).increment();
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                auditService.logSecurityEvent(user, "MEMORY_PROJECTION_FAILED", "Graph projection failed: " + operation);
            }
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        String clean = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL_REDACTED]");
        clean = PHONE_PATTERN.matcher(clean).replaceAll("[PHONE_REDACTED]");
        return clean.trim();
    }

    private String generateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hashBytes[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(text.getBytes()).toString().substring(0, 16);
        }
    }

    private static class QuestionConsolidation {
        String hash;
        String text;
        String topicId;
        double confidence;
        Instant firstSeen;
        Instant lastSeen;
        int count;

        public QuestionConsolidation(String hash, String text, String topicId, double confidence, Instant firstSeen, Instant lastSeen, int count) {
            this.hash = hash;
            this.text = text;
            this.topicId = topicId;
            this.confidence = confidence;
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
            this.count = count;
        }
    }
}
