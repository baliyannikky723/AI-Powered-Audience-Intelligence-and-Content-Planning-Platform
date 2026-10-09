package com.pulsegpt.memory.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.comment.IntentType;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.comment.RawComment;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudienceMemoryServiceTest {

    @Mock
    private AudienceInterestRepository interestRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicAssignmentRepository topicAssignmentRepository;

    @Mock
    private ProcessedCommentRepository processedCommentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KnowledgeGraphProjectionService projectionService;

    @Mock
    private AuditService auditService;

    private AudienceMemoryProperties properties;
    private AudienceMemoryService memoryService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        properties = new AudienceMemoryProperties();
        properties.setEnabled(true);
        properties.setHalfLifeDays(45);
        properties.setEvidenceWindowDays(180);
        properties.setActiveThreshold(0.60);
        properties.setWeakeningThreshold(0.30);
        properties.setMaxConfidence(0.99);
        properties.setConfidenceScaleFactor(200.0);

        memoryService = new AudienceMemoryService(
                interestRepository,
                topicRepository,
                topicAssignmentRepository,
                processedCommentRepository,
                userRepository,
                projectionService,
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
    @DisplayName("1. Confidence Formula Tests")
    class ConfidenceFormulaTests {

        @Test
        @DisplayName("Zero evidence produces zero confidence")
        void testZeroEvidence() {
            double conf = memoryService.calculateConfidence(0, 1.0);
            assertThat(conf).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Calculates canonical exponential confidence accurately")
        void testCanonicalConfidenceFormula() {
            // formula: (1 - exp(-100 / 200)) * 0.95 = (1 - 0.6065) * 0.95 = 0.3935 * 0.95 = 0.3738
            double conf = memoryService.calculateConfidence(100, 0.95);
            assertThat(conf).isCloseTo(0.374, within(0.01));

            // High evidence count (500) approaches consistency score
            double highConf = memoryService.calculateConfidence(500, 0.95);
            assertThat(highConf).isGreaterThan(0.85);
            assertThat(highConf).isLessThanOrEqualTo(0.99);
        }

        @Test
        @DisplayName("Caps maximum confidence at 0.99")
        void testConfidenceCapping() {
            double conf = memoryService.calculateConfidence(10000, 1.0);
            assertThat(conf).isEqualTo(0.99);
        }
    }

    @Nested
    @DisplayName("2. Memory Decay Formula Tests")
    class MemoryDecayFormulaTests {

        @Test
        @DisplayName("Confidence decays to exactly half after one half-life period")
        void testExactHalfLifeDecay() {
            Instant now = Instant.now();
            Instant seen45DaysAgo = now.minus(Duration.ofDays(45));

            double initialConf = 0.80;
            double decayed = memoryService.calculateDecayedConfidence(initialConf, seen45DaysAgo, 45, now);

            // Exactly half of 0.80 = 0.40
            assertThat(decayed).isCloseTo(0.40, within(0.01));
        }

        @Test
        @DisplayName("Confidence decays to 25% after two half-life periods (90 days)")
        void testTwoHalfLivesDecay() {
            Instant now = Instant.now();
            Instant seen90DaysAgo = now.minus(Duration.ofDays(90));

            double initialConf = 0.80;
            double decayed = memoryService.calculateDecayedConfidence(initialConf, seen90DaysAgo, 45, now);

            // 0.80 * (1/4) = 0.20
            assertThat(decayed).isCloseTo(0.20, within(0.01));
        }

        @Test
        @DisplayName("Recent observation has minimal or zero decay")
        void testZeroDecayForImmediateObservation() {
            Instant now = Instant.now();
            double initialConf = 0.85;
            double decayed = memoryService.calculateDecayedConfidence(initialConf, now, 45, now);

            assertThat(decayed).isEqualTo(0.85);
        }
    }

    @Nested
    @DisplayName("3. Memory State Determination Tests")
    class MemoryStateTests {

        @Test
        @DisplayName("Confidence >= 0.60 classifies as ACTIVE")
        void testActiveState() {
            assertThat(memoryService.determineStatus(0.60)).isEqualTo(AudienceInterestStatus.ACTIVE);
            assertThat(memoryService.determineStatus(0.95)).isEqualTo(AudienceInterestStatus.ACTIVE);
        }

        @Test
        @DisplayName("0.30 <= Confidence < 0.60 classifies as WEAKENING")
        void testWeakeningState() {
            assertThat(memoryService.determineStatus(0.30)).isEqualTo(AudienceInterestStatus.WEAKENING);
            assertThat(memoryService.determineStatus(0.59)).isEqualTo(AudienceInterestStatus.WEAKENING);
        }

        @Test
        @DisplayName("Confidence < 0.30 classifies as INACTIVE")
        void testInactiveState() {
            assertThat(memoryService.determineStatus(0.29)).isEqualTo(AudienceInterestStatus.INACTIVE);
            assertThat(memoryService.determineStatus(0.05)).isEqualTo(AudienceInterestStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("4. User Memory Aggregation & Projection")
    class MemoryAggregationTests {

        @Test
        @DisplayName("Aggregates user topics within evidence window and saves audience interests")
        void testAggregateAndUpdateUserMemory() {
            Topic topic = Topic.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .name("Spring Boot & Microservices")
                    .commentCount(150)
                    .keywordScores(Map.of("spring", 0.9, "microservices", 0.85))
                    .active(true)
                    .updatedAt(Instant.now().minus(Duration.ofDays(5)))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(topicRepository.findByUserId(userId)).thenReturn(List.of(topic));
            when(topicAssignmentRepository.findByTopicId(topic.getId())).thenReturn(List.of());
            when(interestRepository.findByUserIdAndTopicId(userId, topic.getId())).thenReturn(Optional.empty());

            memoryService.aggregateAndUpdateUserMemory(userId);

            ArgumentCaptor<AudienceInterest> captor = ArgumentCaptor.forClass(AudienceInterest.class);
            verify(interestRepository).save(captor.capture());

            AudienceInterest saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(testUser);
            assertThat(saved.getTopic()).isEqualTo(topic);
            assertThat(saved.getConfidence()).isGreaterThan(0.4);
            assertThat(saved.getStatus()).isNotNull();

            verify(projectionService).projectIncremental(userId);
            verify(auditService).logAuditEvent(eq(testUser), eq("MEMORY_PROJECTED"), eq(userId.toString()), any());
        }
    }
}
