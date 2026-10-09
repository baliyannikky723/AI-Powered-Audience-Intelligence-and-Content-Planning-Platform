package com.pulsegpt.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.AiEmbedResponse;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.comment.RawComment;
import com.pulsegpt.rag.dto.RagEvidenceItem;
import com.pulsegpt.recommendation.EvidenceSourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("Vector Retrieval Service Unit Tests")
class VectorRetrievalServiceTest {

    @Mock
    private ProcessedCommentRepository processedCommentRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VectorRetrievalService vectorRetrievalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vectorRetrievalService = new VectorRetrievalService(
                processedCommentRepository,
                aiServiceClient,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should compute accurate cosine similarity between orthogonal and parallel vectors")
    void testCosineSimilarityCalculation() {
        List<Double> v1 = List.of(1.0, 0.0, 0.0);
        List<Double> v2 = List.of(1.0, 0.0, 0.0);
        List<Double> v3 = List.of(0.0, 1.0, 0.0);

        assertThat(vectorRetrievalService.computeCosineSimilarity(v1, v2)).isEqualTo(1.0);
        assertThat(vectorRetrievalService.computeCosineSimilarity(v1, v3)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should retrieve, score, and rank semantic comments above similarity threshold")
    void testRetrieveSemanticComments() {
        UUID userId = UUID.randomUUID();
        String query = "How to configure Spring Boot metrics?";

        when(aiServiceClient.generateEmbeddings(any())).thenReturn(
                new AiEmbedResponse("all-MiniLM-L6-v2", "1.0", 384, List.of(List.of(1.0, 0.0, 0.0)))
        );

        RawComment raw1 = RawComment.builder()
                .id(UUID.randomUUID())
                .rawText("Please cover Micrometer and Spring Boot metrics email: test@example.com")
                .authorDisplayName("DevUser1")
                .publishedAt(Instant.now())
                .build();

        ProcessedComment pc1 = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(raw1)
                .normalizedText("Please cover Micrometer and Spring Boot metrics email: test@example.com")
                .embedding("[1.0, 0.0, 0.0]")
                .build();

        RawComment raw2 = RawComment.builder()
                .id(UUID.randomUUID())
                .rawText("Unrelated comment about cooking recipes")
                .authorDisplayName("ChefUser")
                .publishedAt(Instant.now())
                .build();

        ProcessedComment pc2 = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(raw2)
                .normalizedText("Unrelated comment about cooking recipes")
                .embedding("[0.0, 1.0, 0.0]")
                .build();

        when(processedCommentRepository.findEligibleForClusteringByUserIdAndDateRange(
                eq(userId), any(), any(), any(Pageable.class)
        )).thenReturn(List.of(pc1, pc2));

        List<RagEvidenceItem> results = vectorRetrievalService.retrieveSemanticComments(
                userId, query, null, 180, 10, 0.50
        );

        assertThat(results).hasSize(1);
        RagEvidenceItem item = results.get(0);
        assertThat(item.sourceType()).isEqualTo(EvidenceSourceType.COMMENT);
        assertThat(item.similarity()).isEqualTo(1.0);
        assertThat(item.text()).doesNotContain("test@example.com");
        assertThat(item.text()).contains("[EMAIL_REDACTED]");
    }
}
