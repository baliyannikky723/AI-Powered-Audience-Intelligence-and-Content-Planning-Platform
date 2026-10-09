package com.pulsegpt.memory.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.graph.service.KnowledgeGraphProjectionService;
import com.pulsegpt.memory.AudienceInterest;
import com.pulsegpt.memory.AudienceInterestRepository;
import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignment;
import com.pulsegpt.topic.TopicAssignmentRepository;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceMemoryService {

    private final AudienceInterestRepository interestRepository;
    private final TopicRepository topicRepository;
    private final TopicAssignmentRepository topicAssignmentRepository;
    private final ProcessedCommentRepository processedCommentRepository;
    private final UserRepository userRepository;
    private final KnowledgeGraphProjectionService projectionService;
    private final AudienceMemoryProperties properties;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    /**
     * Compute canonical memory confidence:
     * confidence = (1 - exp(-evidence_count / 200)) * consistency, capped at 0.99.
     */
    public double calculateConfidence(int evidenceCount, double consistency) {
        if (evidenceCount <= 0) return 0.0;
        double scale = properties.getConfidenceScaleFactor() > 0 ? properties.getConfidenceScaleFactor() : 200.0;
        double raw = (1.0 - Math.exp(-((double) evidenceCount) / scale)) * consistency;
        return Math.min(properties.getMaxConfidence(), Math.max(0.0, Math.round(raw * 1000.0) / 1000.0));
    }

    /**
     * Compute exponential decay:
     * confidence_t = confidence_0 * exp(-ln(2) * days_since_last_seen / half_life)
     */
    public double calculateDecayedConfidence(double initialConfidence, Instant lastSeenAt, int halfLifeDays, Instant now) {
        if (initialConfidence <= 0.0 || lastSeenAt == null) return 0.0;
        if (halfLifeDays <= 0) halfLifeDays = properties.getHalfLifeDays();

        long secondsSince = Math.max(0, Duration.between(lastSeenAt, now).toSeconds());
        double daysSince = secondsSince / 86400.0;
        double decayed = initialConfidence * Math.exp(-Math.log(2.0) * daysSince / halfLifeDays);
        return Math.min(properties.getMaxConfidence(), Math.max(0.0, Math.round(decayed * 1000.0) / 1000.0));
    }

    /**
     * Determine memory state based on confidence thresholds:
     * ACTIVE: >= 0.60
     * WEAKENING: 0.30 <= conf < 0.60
     * INACTIVE: < 0.30
     */
    public AudienceInterestStatus determineStatus(double confidence) {
        if (confidence >= properties.getActiveThreshold()) {
            return AudienceInterestStatus.ACTIVE;
        } else if (confidence >= properties.getWeakeningThreshold()) {
            return AudienceInterestStatus.WEAKENING;
        } else {
            return AudienceInterestStatus.INACTIVE;
        }
    }

    @Transactional
    public void aggregateAndUpdateUserMemory(UUID userId) {
        if (!properties.isEnabled()) {
            log.debug("Audience memory is disabled in configuration.");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Instant now = Instant.now();
        Instant windowStart = now.minus(Duration.ofDays(properties.getEvidenceWindowDays()));

        log.info("MEMORY_AGGREGATION: Updating audience memory for user {} with evidence window of {} days",
                userId, properties.getEvidenceWindowDays());

        List<Topic> topics = topicRepository.findByUserId(userId);
        int updatedCount = 0;

        for (Topic topic : topics) {
            // Retrieve topic assignments within evidence window
            List<TopicAssignment> assignments = topicAssignmentRepository.findByTopicId(topic.getId());
            long windowObservations = assignments.stream()
                    .filter(a -> !a.isNoise())
                    .filter(a -> a.getProcessedComment() != null &&
                            a.getProcessedComment().getRawComment() != null &&
                            a.getProcessedComment().getRawComment().getPublishedAt() != null &&
                            a.getProcessedComment().getRawComment().getPublishedAt().isAfter(windowStart))
                    .count();

            // Find most recent observation
            Instant lastSeen = assignments.stream()
                    .filter(a -> !a.isNoise())
                    .map(a -> a.getProcessedComment().getRawComment().getPublishedAt())
                    .filter(Objects::nonNull)
                    .max(Instant::compareTo)
                    .orElse(topic.getUpdatedAt() != null ? topic.getUpdatedAt() : now);

            int evidenceCount = (int) windowObservations;
            if (evidenceCount == 0 && topic.getCommentCount() > 0) {
                evidenceCount = topic.getCommentCount();
            }

            double consistency = 0.95;
            if (topic.getKeywordScores() != null && !topic.getKeywordScores().isEmpty()) {
                consistency = topic.getKeywordScores().values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.95);
                consistency = Math.min(1.0, Math.max(0.5, consistency));
            }

            double initialConfidence = calculateConfidence(evidenceCount, consistency);
            double decayedConfidence = calculateDecayedConfidence(initialConfidence, lastSeen, properties.getHalfLifeDays(), now);
            AudienceInterestStatus status = determineStatus(decayedConfidence);

            AudienceInterest interest = interestRepository.findByUserIdAndTopicId(userId, topic.getId())
                    .orElseGet(() -> AudienceInterest.builder()
                            .user(user)
                            .topic(topic)
                            .halfLifeDays(properties.getHalfLifeDays())
                            .build());

            interest.setConfidence(decayedConfidence);
            interest.setEvidenceCount(evidenceCount);
            interest.setLastSeenAt(lastSeen);
            interest.setStatus(status);
            interest.setHalfLifeDays(properties.getHalfLifeDays());

            interestRepository.save(interest);
            updatedCount++;
        }

        // Project updated audience memory to Neo4j knowledge graph
        projectionService.projectIncremental(userId);

        auditService.logAuditEvent(user, "MEMORY_PROJECTED", userId.toString(), Map.of(
                "topicsUpdated", updatedCount,
                "evidenceWindowDays", properties.getEvidenceWindowDays(),
                "halfLifeDays", properties.getHalfLifeDays()
        ));
    }
}
