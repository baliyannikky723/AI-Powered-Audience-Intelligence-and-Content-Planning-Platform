package com.pulsegpt.rag.service;

import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.memory.dto.AudienceInterestResponse;
import com.pulsegpt.memory.dto.AudienceMemoryContext;
import com.pulsegpt.memory.dto.AudienceQuestionResponse;
import com.pulsegpt.rag.dto.RagEvidenceItem;
import com.pulsegpt.recommendation.EvidenceSourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RAG Evidence Fusion Service Unit Tests")
class RagEvidenceFusionServiceTest {

    private RagEvidenceFusionService fusionService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fusionService = new RagEvidenceFusionService();
    }

    @Test
    @DisplayName("Should fuse multi-source evidence, deduplicate, enforce diversity, and assign citation IDs")
    void testEvidenceFusionAndCitationAssignment() {
        List<RagEvidenceItem> vectorComments = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            vectorComments.add(RagEvidenceItem.builder()
                    .evidenceId("comment:" + i)
                    .sourceType(EvidenceSourceType.COMMENT)
                    .sourceId("c" + i)
                    .userId(userId)
                    .text("Comment feedback #" + i)
                    .similarity(0.80)
                    .recency(0.90)
                    .relevance(0.85)
                    .quality(0.80)
                    .evidenceScore(0.82)
                    .createdAt(Instant.now())
                    .build());
        }

        List<RagEvidenceItem> structured = List.of(
                RagEvidenceItem.builder()
                        .evidenceId("topic:101")
                        .sourceType(EvidenceSourceType.TOPIC)
                        .sourceId("t101")
                        .userId(userId)
                        .text("Topic: Spring Boot 3.4")
                        .similarity(0.90)
                        .recency(1.0)
                        .relevance(0.95)
                        .quality(0.90)
                        .evidenceScore(0.92)
                        .createdAt(Instant.now())
                        .build()
        );

        AudienceMemoryContext memoryContext = AudienceMemoryContext.builder()
                .activeInterests(List.of(
                        AudienceInterestResponse.builder()
                                .id(UUID.randomUUID())
                                .topicId(UUID.randomUUID())
                                .topicName("Neo4j Knowledge Graphs")
                                .status(AudienceInterestStatus.ACTIVE)
                                .confidence(0.94)
                                .evidenceCount(120)
                                .lastSeenAt(Instant.now().minus(Duration.ofDays(2)))
                                .build()
                ))
                .weakeningInterests(Collections.emptyList())
                .recurringQuestions(List.of(
                        AudienceQuestionResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .questionHash("qhash-123")
                                .normalizedText("How to manage knowledge graph memory decay?")
                                .confidence(0.88)
                                .evidenceCount(24)
                                .firstSeenAt(Instant.now().minus(Duration.ofDays(10)))
                                .lastSeenAt(Instant.now().minus(Duration.ofDays(1)))
                                .build()
                ))
                .relatedTopics(Collections.emptyList())
                .recentContentIdeas(Collections.emptyList())
                .build();

        List<RagEvidenceItem> fused = fusionService.fuseEvidence(
                vectorComments,
                structured,
                memoryContext,
                userId,
                12
        );

        assertThat(fused).isNotEmpty();
        assertThat(fused.size()).isLessThanOrEqualTo(12);

        // Verify citation IDs [E1], [E2], ...
        for (int i = 0; i < fused.size(); i++) {
            assertThat(fused.get(i).citationId()).isEqualTo("[E" + (i + 1) + "]");
        }

        // Verify source diversity
        List<EvidenceSourceType> sourceTypes = fused.stream().map(RagEvidenceItem::sourceType).toList();
        assertThat(sourceTypes).contains(EvidenceSourceType.COMMENT, EvidenceSourceType.TOPIC, EvidenceSourceType.MEMORY);
    }
}
