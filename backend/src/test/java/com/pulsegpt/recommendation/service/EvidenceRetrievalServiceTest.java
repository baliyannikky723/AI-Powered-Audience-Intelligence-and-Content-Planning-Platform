package com.pulsegpt.recommendation.service;

import com.pulsegpt.comment.*;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.recommendation.dto.RecommendationGenerateRequest;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignment;
import com.pulsegpt.topic.TopicAssignmentRepository;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceRetrievalServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private ProcessedCommentRepository processedCommentRepository;

    @Mock
    private TopicAssignmentRepository topicAssignmentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ContentRecommendationRepository recommendationRepository;

    @Mock
    private com.pulsegpt.graph.service.KnowledgeGraphQueryService knowledgeGraphQueryService;

    private EvidenceRetrievalService evidenceRetrievalService;

    private User testUser;
    private Topic sampleTopic;

    @BeforeEach
    void setUp() {
        evidenceRetrievalService = new EvidenceRetrievalService(
                topicRepository,
                processedCommentRepository,
                topicAssignmentRepository,
                postRepository,
                recommendationRepository,
                knowledgeGraphQueryService
        );

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("creator@example.com")
                .role(UserRole.CREATOR)
                .build();

        sampleTopic = Topic.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Database Sharding & Scaling")
                .keywords(List.of("database", "sharding", "postgresql", "scaling"))
                .commentCount(45)
                .build();
    }

    @Test
    @DisplayName("Should retrieve, score, and rank audience evidence deterministically")
    void testRetrieveEvidence_EvidenceGroundedMode() {
        when(topicRepository.findByIdAndUserId(sampleTopic.getId(), testUser.getId())).thenReturn(Optional.of(sampleTopic));

        RawComment rawComment = RawComment.builder()
                .id(UUID.randomUUID())
                .rawText("How do you handle cross-shard foreign keys in PostgreSQL?")
                .build();

        ProcessedComment questionComment = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(rawComment)
                .normalizedText("How do you handle cross-shard foreign keys in PostgreSQL?")
                .intent(IntentType.QUESTION)
                .sentimentLabel(SentimentLabel.NEUTRAL)
                .sentimentScore(0.0)
                .build();

        TopicAssignment assignment = TopicAssignment.builder()
                .id(UUID.randomUUID())
                .topic(sampleTopic)
                .processedComment(questionComment)
                .clusterId(1)
                .isNoise(false)
                .membershipProbability(0.95)
                .build();

        when(topicAssignmentRepository.findByTopicId(sampleTopic.getId())).thenReturn(List.of(assignment));

        Post recentPost = Post.builder()
                .id(UUID.randomUUID())
                .title("Introduction to PostgreSQL Indexing")
                .platformAccount(PlatformAccount.builder().user(testUser).platform(PlatformType.YOUTUBE).build())
                .publishedAt(Instant.now())
                .build();

        when(postRepository.findByPlatformAccountUserIdOrderByPublishedAtDesc(eq(testUser.getId()), any()))
                .thenReturn(List.of(recentPost));

        ContentRecommendation priorRec = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .title("5 Common PostgreSQL Mistakes")
                .build();

        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                .thenReturn(List.of(priorRec));

        RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                sampleTopic.getId(), "VIDEO", "EDUCATIONAL", null, 3, GenerationMode.EVIDENCE_GROUNDED
        );

        RecommendationContext context = evidenceRetrievalService.retrieveEvidence(testUser, request);

        assertThat(context).isNotNull();
        assertThat(context.topics()).hasSize(1);
        assertThat(context.topics().get(0).getName()).isEqualTo("Database Sharding & Scaling");
        assertThat(context.evidenceItems()).isNotEmpty();
        assertThat(context.contentHistory()).hasSize(1);
        assertThat(context.previousRecommendations()).hasSize(1);

        // Ensure evidence contains structured reference identifiers
        assertThat(context.evidenceItems()).anyMatch(e -> e.evidenceId().startsWith("topic:"));
        assertThat(context.evidenceItems()).anyMatch(e -> e.evidenceId().startsWith("question:"));
    }

    @Test
    @DisplayName("In BASELINE mode, evidence retrieval should return minimal/empty audience evidence")
    void testRetrieveEvidence_BaselineMode() {
        RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                null, "VIDEO", "ENTERTAINMENT", null, 1, GenerationMode.BASELINE
        );

        RecommendationContext context = evidenceRetrievalService.retrieveEvidence(testUser, request);

        assertThat(context).isNotNull();
        assertThat(context.evidenceItems()).isEmpty();
        assertThat(context.topics()).isEmpty();
    }
}
