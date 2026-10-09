package com.pulsegpt.graph.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.calendar.CalendarItemStatus;
import com.pulsegpt.comment.*;
import com.pulsegpt.graph.model.*;
import com.pulsegpt.graph.repository.Neo4jKnowledgeGraphRepository;
import com.pulsegpt.memory.AudienceInterest;
import com.pulsegpt.memory.AudienceInterestRepository;
import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.memory.dto.*;
import com.pulsegpt.memory.service.AudienceMemoryProperties;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignment;
import com.pulsegpt.topic.TopicAssignmentRepository;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private Neo4jKnowledgeGraphRepository graphRepository;

    @Mock
    private AudienceInterestRepository interestRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicAssignmentRepository topicAssignmentRepository;

    @Mock
    private ProcessedCommentRepository processedCommentRepository;

    @Mock
    private ContentRecommendationRepository recommendationRepository;

    @Mock
    private CalendarItemRepository calendarItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    private AudienceMemoryProperties properties;
    private KnowledgeGraphProjectionService projectionService;
    private KnowledgeGraphQueryService queryService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        properties = new AudienceMemoryProperties();
        properties.setEnabled(true);
        properties.setHalfLifeDays(45);
        properties.setEvidenceWindowDays(180);

        projectionService = new KnowledgeGraphProjectionService(
                graphRepository,
                interestRepository,
                topicRepository,
                topicAssignmentRepository,
                processedCommentRepository,
                recommendationRepository,
                calendarItemRepository,
                userRepository,
                properties,
                auditService,
                new SimpleMeterRegistry()
        );

        queryService = new KnowledgeGraphQueryService(
                graphRepository,
                interestRepository,
                topicRepository,
                recommendationRepository,
                userRepository,
                properties,
                auditService,
                new SimpleMeterRegistry()
        );

        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("creator@example.com")
                .build();
    }

    @Nested
    @DisplayName("1. Graph Node Model Validations")
    class NodeModelTests {

        @Test
        @DisplayName("AudienceNode entity builder and getters")
        void testAudienceNode() {
            AudienceNode node = AudienceNode.builder()
                    .id("audience:" + userId)
                    .userId(userId.toString())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            assertThat(node.getId()).isEqualTo("audience:" + userId);
            assertThat(node.getUserId()).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("TopicNode entity builder and getters")
        void testTopicNode() {
            UUID topicId = UUID.randomUUID();
            TopicNode node = TopicNode.builder()
                    .id("topic:" + topicId)
                    .userId(userId.toString())
                    .topicId(topicId.toString())
                    .label("Graph Theory")
                    .status("ACTIVE")
                    .confidence(0.88)
                    .evidenceCount(42)
                    .build();

            assertThat(node.getTopicId()).isEqualTo(topicId.toString());
            assertThat(node.getConfidence()).isEqualTo(0.88);
            assertThat(node.getLabel()).isEqualTo("Graph Theory");
        }

        @Test
        @DisplayName("QuestionNode entity builder and getters")
        void testQuestionNode() {
            QuestionNode node = QuestionNode.builder()
                    .id("question:hash123")
                    .userId(userId.toString())
                    .questionHash("hash123")
                    .normalizedText("How to use Neo4j in Spring Boot?")
                    .confidence(0.92)
                    .evidenceCount(15)
                    .build();

            assertThat(node.getQuestionHash()).isEqualTo("hash123");
            assertThat(node.getNormalizedText()).contains("Neo4j");
        }

        @Test
        @DisplayName("ContentIdeaNode entity builder and getters")
        void testContentIdeaNode() {
            UUID recId = UUID.randomUUID();
            ContentIdeaNode node = ContentIdeaNode.builder()
                    .id("rec:" + recId)
                    .userId(userId.toString())
                    .title("Mastering Knowledge Graphs")
                    .angle("VIDEO")
                    .status("DRAFT")
                    .build();

            assertThat(node.getTitle()).isEqualTo("Mastering Knowledge Graphs");
            assertThat(node.getStatus()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("CalendarItemNode entity builder and getters")
        void testCalendarItemNode() {
            UUID calId = UUID.randomUUID();
            CalendarItemNode node = CalendarItemNode.builder()
                    .id("cal:" + calId)
                    .userId(userId.toString())
                    .title("Upload Video on Friday")
                    .platform("YOUTUBE")
                    .status("SCHEDULED")
                    .build();

            assertThat(node.getTitle()).isEqualTo("Upload Video on Friday");
            assertThat(node.getPlatform()).isEqualTo("YOUTUBE");
        }
    }

    @Nested
    @DisplayName("2. Graph Projections & Relationships")
    class ProjectionTests {

        @Test
        @DisplayName("projectAudience calls mergeAudience with user scope")
        void testProjectAudience() {
            projectionService.projectAudience(userId);
            verify(graphRepository).mergeAudience(eq(userId.toString()), any(Instant.class));
        }

        @Test
        @DisplayName("projectTopic calls mergeTopic with correct properties")
        void testProjectTopic() {
            UUID topicId = UUID.randomUUID();
            Topic topic = Topic.builder()
                    .id(topicId)
                    .user(testUser)
                    .name("Java Virtual Threads")
                    .commentCount(80)
                    .active(true)
                    .build();

            when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
            when(interestRepository.findByUserIdAndTopicId(userId, topicId)).thenReturn(Optional.empty());

            projectionService.projectTopic(topicId);

            verify(graphRepository).mergeTopic(
                    eq(userId.toString()),
                    eq(topicId.toString()),
                    eq("Java Virtual Threads"),
                    eq("ACTIVE"),
                    anyDouble(),
                    any(Instant.class),
                    any(Instant.class),
                    eq(80),
                    any(Instant.class)
            );
        }

        @Test
        @DisplayName("projectQuestion normalizes text, hashes, and links to topic")
        void testProjectQuestion() {
            UUID commentId = UUID.randomUUID();
            PlatformAccount account = PlatformAccount.builder().user(testUser).build();
            Post post = Post.builder().platformAccount(account).build();
            RawComment raw = RawComment.builder().post(post).rawText("How do virtual threads work? contact john@example.com").build();
            ProcessedComment pc = ProcessedComment.builder()
                    .id(commentId)
                    .rawComment(raw)
                    .intent(IntentType.QUESTION)
                    .normalizedText("how do virtual threads work? contact john@example.com")
                    .sentimentScore(0.88)
                    .build();

            UUID topicId = UUID.randomUUID();
            Topic topic = Topic.builder().id(topicId).build();
            TopicAssignment ta = TopicAssignment.builder().topic(topic).processedComment(pc).build();

            when(processedCommentRepository.findById(commentId)).thenReturn(Optional.of(pc));
            when(topicAssignmentRepository.findByProcessedCommentId(commentId)).thenReturn(List.of(ta));

            projectionService.projectQuestion(commentId);

            verify(graphRepository).mergeQuestion(
                    eq(userId.toString()),
                    anyString(),
                    contains("[EMAIL_REDACTED]"),
                    eq(topicId.toString()),
                    eq(0.88),
                    any(),
                    any(),
                    eq(1),
                    any()
            );
        }

        @Test
        @DisplayName("projectRecommendation calls mergeContentIdea")
        void testProjectRecommendation() {
            UUID recId = UUID.randomUUID();
            UUID topicId = UUID.randomUUID();
            Topic topic = Topic.builder().id(topicId).build();
            ContentRecommendation rec = ContentRecommendation.builder()
                    .id(recId)
                    .user(testUser)
                    .topic(topic)
                    .title("Deep Dive into Java Concurrency")
                    .contentType("VIDEO")
                    .status(com.pulsegpt.recommendation.RecommendationStatus.GENERATED)
                    .build();

            when(recommendationRepository.findById(recId)).thenReturn(Optional.of(rec));

            projectionService.projectRecommendation(recId);

            verify(graphRepository).mergeContentIdea(
                    eq(userId.toString()),
                    eq(recId.toString()),
                    eq(topicId.toString()),
                    eq("Deep Dive into Java Concurrency"),
                    eq("VIDEO"),
                    eq("GENERATED"),
                    any()
            );
        }

        @Test
        @DisplayName("projectCalendarItem calls mergeCalendarItem")
        void testProjectCalendarItem() {
            UUID calId = UUID.randomUUID();
            UUID recId = UUID.randomUUID();
            ContentRecommendation rec = ContentRecommendation.builder().id(recId).build();
            CalendarItem cal = CalendarItem.builder()
                    .id(calId)
                    .user(testUser)
                    .recommendation(rec)
                    .title("Live Stream: Q&A")
                    .platform(PlatformType.YOUTUBE)
                    .status(CalendarItemStatus.SCHEDULED)
                    .scheduledAt(Instant.now())
                    .build();

            when(calendarItemRepository.findById(calId)).thenReturn(Optional.of(cal));

            projectionService.projectCalendarItem(calId);

            verify(graphRepository).mergeCalendarItem(
                    eq(userId.toString()),
                    eq(calId.toString()),
                    eq(recId.toString()),
                    eq("Live Stream: Q&A"),
                    any(),
                    eq("YOUTUBE"),
                    eq("SCHEDULED")
            );
        }
    }

    @Nested
    @DisplayName("3. Full Rebuild & Tenant Isolation")
    class RebuildAndIsolationTests {

        @Test
        @DisplayName("rebuildUserGraph clears ONLY user graph and projects all user entities")
        void testRebuildUserGraph() {
            Topic t1 = Topic.builder()
                    .id(UUID.randomUUID())
                    .name("Topic A")
                    .keywords(List.of("java", "spring"))
                    .commentCount(50)
                    .active(true)
                    .build();
            Topic t2 = Topic.builder()
                    .id(UUID.randomUUID())
                    .name("Topic B")
                    .keywords(List.of("java", "concurrency"))
                    .commentCount(30)
                    .active(true)
                    .build();

            when(topicRepository.findByUserId(userId)).thenReturn(List.of(t1, t2));
            when(interestRepository.findByUserIdAndTopicId(eq(userId), any())).thenReturn(Optional.empty());
            when(topicAssignmentRepository.findByUserId(userId)).thenReturn(List.of());
            when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
            when(calendarItemRepository.findByUserId(userId)).thenReturn(List.of());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            MemoryRebuildResponse response = projectionService.rebuildUserGraph(userId);

            assertThat(response.status()).isEqualTo("SUCCESS");
            assertThat(response.topicCount()).isEqualTo(2);
            assertThat(response.userId()).isEqualTo(userId);

            // Verifies user graph was cleared safely with user scope only
            verify(graphRepository).clearUserGraph(userId.toString());
            verify(graphRepository, times(2)).mergeTopic(eq(userId.toString()), anyString(), anyString(), anyString(), anyDouble(), any(), any(), anyInt(), any());
            verify(auditService).logAuditEvent(eq(testUser), eq("MEMORY_REBUILT"), eq(userId.toString()), any());
        }
    }

    @Nested
    @DisplayName("4. Query Service & Graceful Fallback")
    class QueryAndFallbackTests {

        @Test
        @DisplayName("getActiveInterests returns graph interests and falls back to PostgreSQL if graph is empty")
        void testGetActiveInterestsFallback() {
            when(graphRepository.findInterestsByStatus(userId.toString(), "ACTIVE")).thenReturn(List.of());

            Topic topic = Topic.builder().id(UUID.randomUUID()).name("AI Agents").build();
            AudienceInterest interest = AudienceInterest.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .topic(topic)
                    .confidence(0.82)
                    .evidenceCount(45)
                    .status(AudienceInterestStatus.ACTIVE)
                    .lastSeenAt(Instant.now())
                    .build();

            when(interestRepository.findByUserIdAndStatus(userId, AudienceInterestStatus.ACTIVE)).thenReturn(List.of(interest));

            List<AudienceInterestResponse> results = queryService.getActiveInterests(userId);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).topicName()).isEqualTo("AI Agents");
            assertThat(results.get(0).confidence()).isEqualTo(0.82);
        }

        @Test
        @DisplayName("getAudienceMemoryContext returns structured context for recommendation engine")
        void testGetAudienceMemoryContext() {
            when(graphRepository.findInterestsByStatus(userId.toString(), "ACTIVE")).thenReturn(List.of(
                    AudienceInterestResponse.builder().topicId(UUID.randomUUID()).topicName("Microservices").confidence(0.85).build()
            ));
            when(graphRepository.findInterestsByStatus(userId.toString(), "WEAKENING")).thenReturn(List.of());
            when(graphRepository.findRecurringQuestions(eq(userId.toString()), anyInt())).thenReturn(List.of(
                    AudienceQuestionResponse.builder().id("q1").questionHash("h1").normalizedText("How to scale?").confidence(0.9).build()
            ));

            AudienceMemoryContext context = queryService.getAudienceMemoryContext(userId);

            assertThat(context.available()).isTrue();
            assertThat(context.activeInterests()).hasSize(1);
            assertThat(context.recurringQuestions()).hasSize(1);
            assertThat(context.memoryVersion()).isEqualTo("2026-10-v1");
        }
    }
}
