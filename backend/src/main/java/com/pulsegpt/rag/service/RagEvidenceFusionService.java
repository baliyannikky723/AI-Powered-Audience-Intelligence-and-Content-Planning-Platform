package com.pulsegpt.rag.service;

import com.pulsegpt.memory.dto.AudienceInterestResponse;
import com.pulsegpt.memory.dto.AudienceMemoryContext;
import com.pulsegpt.memory.dto.AudienceQuestionResponse;
import com.pulsegpt.rag.dto.RagEvidenceItem;
import com.pulsegpt.recommendation.EvidenceSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagEvidenceFusionService {

    /**
     * Fuses multi-source evidence (Vector comments, Topics, Questions, Trends, Graph Memory, Content History),
     * deduplicates, balances source diversity, and assigns citation IDs [E1], [E2], ...
     */
    public List<RagEvidenceItem> fuseEvidence(
            List<RagEvidenceItem> vectorComments,
            List<RagEvidenceItem> structuredItems,
            AudienceMemoryContext graphMemory,
            UUID userId,
            int maxBudget) {

        List<RagEvidenceItem> allRaw = new ArrayList<>();

        if (vectorComments != null) {
            allRaw.addAll(vectorComments);
        }
        if (structuredItems != null) {
            allRaw.addAll(structuredItems);
        }

        // Convert Graph Memory into RagEvidenceItems
        if (graphMemory != null) {
            if (graphMemory.activeInterests() != null) {
                for (AudienceInterestResponse interest : graphMemory.activeInterests()) {
                    double relevance = 0.95;
                    long daysSince = interest.lastSeenAt() != null
                            ? Math.max(0, Duration.between(interest.lastSeenAt(), Instant.now()).toDays())
                            : 0;
                    double recency = Math.max(0.1, 1.0 - (daysSince / 180.0));
                    double volume = Math.min(1.0, interest.evidenceCount() / 100.0);
                    double quality = interest.confidence() != null ? interest.confidence() : 0.90;
                    double score = Math.round(((0.40 * relevance) + (0.25 * recency) + (0.20 * volume) + (0.15 * quality)) * 100.0) / 100.0;

                    String label = interest.topicName() != null ? interest.topicName() : "Topic " + interest.topicId();
                    String topicIdStr = interest.topicId() != null ? interest.topicId().toString() : UUID.randomUUID().toString();

                    allRaw.add(RagEvidenceItem.builder()
                            .evidenceId("graph:topic:" + topicIdStr)
                            .sourceType(EvidenceSourceType.MEMORY)
                            .sourceId(topicIdStr)
                            .userId(userId)
                            .text("Audience Memory: " + label + " (Confidence: " + Math.round(quality * 100) + "%, Status: ACTIVE)")
                            .similarity(0.90)
                            .recency(recency)
                            .relevance(relevance)
                            .quality(quality)
                            .evidenceScore(score)
                            .metadata(Map.of(
                                    "status", "ACTIVE",
                                    "confidence", quality,
                                    "evidenceCount", interest.evidenceCount()
                            ))
                            .createdAt(interest.lastSeenAt() != null ? interest.lastSeenAt() : Instant.now())
                            .build());
                }
            }

            if (graphMemory.weakeningInterests() != null) {
                for (AudienceInterestResponse interest : graphMemory.weakeningInterests()) {
                    double relevance = 0.80;
                    long daysSince = interest.lastSeenAt() != null
                            ? Math.max(0, Duration.between(interest.lastSeenAt(), Instant.now()).toDays())
                            : 45;
                    double recency = Math.max(0.1, 1.0 - (daysSince / 180.0));
                    double volume = Math.min(1.0, interest.evidenceCount() / 100.0);
                    double quality = interest.confidence() != null ? interest.confidence() : 0.50;
                    double score = Math.round(((0.40 * relevance) + (0.25 * recency) + (0.20 * volume) + (0.15 * quality)) * 100.0) / 100.0;

                    String label = interest.topicName() != null ? interest.topicName() : "Topic " + interest.topicId();
                    String topicIdStr = interest.topicId() != null ? interest.topicId().toString() : UUID.randomUUID().toString();

                    allRaw.add(RagEvidenceItem.builder()
                            .evidenceId("graph:weakening:" + topicIdStr)
                            .sourceType(EvidenceSourceType.MEMORY)
                            .sourceId(topicIdStr)
                            .userId(userId)
                            .text("Audience Memory (Decaying): " + label + " (Confidence: " + Math.round(quality * 100) + "%, Status: WEAKENING)")
                            .similarity(0.75)
                            .recency(recency)
                            .relevance(relevance)
                            .quality(quality)
                            .evidenceScore(score)
                            .metadata(Map.of(
                                    "status", "WEAKENING",
                                    "confidence", quality,
                                    "evidenceCount", interest.evidenceCount()
                            ))
                            .createdAt(interest.lastSeenAt() != null ? interest.lastSeenAt() : Instant.now())
                            .build());
                }
            }

            if (graphMemory.recurringQuestions() != null) {
                for (AudienceQuestionResponse q : graphMemory.recurringQuestions()) {
                    double relevance = 0.90;
                    double recency = 0.85;
                    double volume = Math.min(1.0, q.evidenceCount() / 30.0);
                    double quality = q.confidence() != null ? q.confidence() : 0.85;
                    double score = Math.round(((0.40 * relevance) + (0.25 * recency) + (0.20 * volume) + (0.15 * quality)) * 100.0) / 100.0;

                    allRaw.add(RagEvidenceItem.builder()
                            .evidenceId("graph:question:" + q.questionHash())
                            .sourceType(EvidenceSourceType.QUESTION)
                            .sourceId(q.questionHash())
                            .userId(userId)
                            .text("Recurring Inquired Question: \"" + q.normalizedText() + "\" (" + q.evidenceCount() + "x recurring)")
                            .similarity(0.85)
                            .recency(recency)
                            .relevance(relevance)
                            .quality(quality)
                            .evidenceScore(score)
                            .metadata(Map.of(
                                    "questionHash", q.questionHash(),
                                    "evidenceCount", q.evidenceCount(),
                                    "confidence", quality
                            ))
                            .createdAt(q.lastSeenAt() != null ? q.lastSeenAt() : Instant.now())
                            .build());
                }
            }
        }

        // 1. Deduplicate by sourceType + sourceId & text uniqueness
        Map<String, RagEvidenceItem> dedupMap = new LinkedHashMap<>();
        for (RagEvidenceItem item : allRaw) {
            String primaryKey = item.sourceType() + ":" + item.sourceId();
            if (!dedupMap.containsKey(primaryKey)) {
                dedupMap.put(primaryKey, item);
            } else {
                RagEvidenceItem existing = dedupMap.get(primaryKey);
                if (item.evidenceScore() > existing.evidenceScore()) {
                    dedupMap.put(primaryKey, item);
                }
            }
        }

        List<RagEvidenceItem> uniqueList = new ArrayList<>(dedupMap.values());

        // 2. Group by SourceType for Diverse Budget Allocation
        Map<EvidenceSourceType, List<RagEvidenceItem>> byType = uniqueList.stream()
                .sorted(Comparator.comparingDouble(RagEvidenceItem::evidenceScore).reversed())
                .collect(Collectors.groupingBy(RagEvidenceItem::sourceType));

        // Source Target Quotas
        int maxComments = Math.min(5, maxBudget);
        int maxTopics = 2;
        int maxQuestions = 2;
        int maxMemory = 2;
        int maxHistory = 1;

        List<RagEvidenceItem> selected = new ArrayList<>();
        Set<String> addedEvidenceIds = new HashSet<>();

        addSubset(selected, byType.get(EvidenceSourceType.COMMENT), maxComments, addedEvidenceIds);
        addSubset(selected, byType.get(EvidenceSourceType.TOPIC), maxTopics, addedEvidenceIds);
        addSubset(selected, byType.get(EvidenceSourceType.QUESTION), maxQuestions, addedEvidenceIds);
        addSubset(selected, byType.get(EvidenceSourceType.MEMORY), maxMemory, addedEvidenceIds);
        addSubset(selected, byType.get(EvidenceSourceType.CONTENT_HISTORY), maxHistory, addedEvidenceIds);

        // Fill remaining budget dynamically with highest ranked leftover evidence
        if (selected.size() < maxBudget) {
            List<RagEvidenceItem> leftovers = uniqueList.stream()
                    .filter(item -> !addedEvidenceIds.contains(item.evidenceId()))
                    .sorted(Comparator.comparingDouble(RagEvidenceItem::evidenceScore).reversed())
                    .toList();

            for (RagEvidenceItem item : leftovers) {
                if (selected.size() >= maxBudget) break;
                selected.add(item);
                addedEvidenceIds.add(item.evidenceId());
            }
        }

        // 3. Final ranking by evidenceScore descending
        selected.sort(Comparator.comparingDouble(RagEvidenceItem::evidenceScore).reversed());

        // 4. Assign deterministic stable citation IDs: [E1], [E2], ...
        List<RagEvidenceItem> citedResults = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            String citationId = "[E" + (i + 1) + "]";
            citedResults.add(selected.get(i).withCitationId(citationId));
        }

        return citedResults;
    }

    private void addSubset(
            List<RagEvidenceItem> target,
            List<RagEvidenceItem> source,
            int limit,
            Set<String> addedSet) {

        if (source == null || source.isEmpty()) return;
        int count = 0;
        for (RagEvidenceItem item : source) {
            if (count >= limit) break;
            if (addedSet.add(item.evidenceId())) {
                target.add(item);
                count++;
            }
        }
    }
}
