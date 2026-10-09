package com.pulsegpt.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.recommendation.*;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.recommendation.dto.RecommendationGenerateRequest;
import com.pulsegpt.recommendation.dto.RecommendationResponse;
import com.pulsegpt.recommendation.mapper.ContentRecommendationMapper;
import com.pulsegpt.security.RateLimitingService;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRole;
import com.pulsegpt.validation.CheckResult;
import com.pulsegpt.validation.RecommendationValidator;
import com.pulsegpt.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentRecommendationServiceTest {

    @Mock
    private ContentRecommendationRepository recommendationRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private EvidenceRetrievalService evidenceRetrievalService;

    @Mock
    private RecommendationValidator recommendationValidator;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private AuditService auditService;

    private final ContentRecommendationMapper recommendationMapper = Mappers.getMapper(ContentRecommendationMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ContentRecommendationService recommendationService;

    private User testUser;
    private Topic testTopic;
    private RecommendationContext sampleContext;

    @BeforeEach
    void setUp() {
        recommendationService = new ContentRecommendationService(
                recommendationRepository,
                topicRepository,
                evidenceRetrievalService,
                recommendationValidator,
                aiServiceClient,
                rateLimitingService,
                auditService,
                recommendationMapper,
                objectMapper
        );

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("creator@example.com")
                .role(UserRole.CREATOR)
                .build();

        testTopic = Topic.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Spring Boot Performance")
                .keywords(List.of("spring", "boot", "performance", "virtual threads"))
                .commentCount(50)
                .build();

        EvidenceItem evidenceItem = EvidenceItem.builder()
                .evidenceId("topic:" + testTopic.getId())
                .sourceType(EvidenceSourceType.TOPIC)
                .sourceId(testTopic.getId())
                .userId(testUser.getId())
                .summary("Frequently discusses virtual threads and HikariCP connection pooling")
                .relevanceScore(0.95)
                .createdAt(Instant.now())
                .build();

        RecommendationGenerateRequest req = RecommendationGenerateRequest.builder()
                .topicId(testTopic.getId())
                .contentType("VIDEO")
                .goal("EDUCATIONAL")
                .maxIdeas(1)
                .mode(GenerationMode.EVIDENCE_GROUNDED)
                .build();

        sampleContext = RecommendationContext.builder()
                .request(req)
                .evidenceItems(List.of(evidenceItem))
                .topics(List.of(testTopic))
                .questions(List.of())
                .representativeComments(List.of())
                .contentHistory(List.of())
                .previousRecommendations(List.of())
                .build();
    }

    @Test
    @DisplayName("Should generate, validate, and accept recommendation when initial draft passes all 6 checks")
    void testGenerate_SuccessWithoutRepair() {
        when(rateLimitingService.tryConsumeAi(testUser.getId())).thenReturn(true);
        when(evidenceRetrievalService.retrieveEvidence(eq(testUser), any())).thenReturn(sampleContext);
        when(topicRepository.findByIdAndUserId(testTopic.getId(), testUser.getId())).thenReturn(Optional.of(testTopic));

        AiRecommendationDraft draft = AiRecommendationDraft.builder()
                .title("5 Spring Boot 3.2 Virtual Threads Pitfalls")
                .contentType("VIDEO")
                .angle("Deep performance benchmarking for backend developers")
                .targetAudience("Spring Developers")
                .problemAddressed("Pinning carrier threads when using synchronized blocks")
                .keyPoints(List.of("Avoid synchronized keywords in virtual thread paths", "Configure HikariCP correctly"))
                .hook("Are your virtual threads actually slowing down your Spring Boot app?")
                .callToAction("Subscribe for more Spring Boot internals!")
                .evidenceIds(List.of("topic:" + testTopic.getId()))
                .confidence(0.93)
                .build();

        AiRecommendationGenerateResponse aiResponse = AiRecommendationGenerateResponse.builder()
                .drafts(List.of(draft))
                .modelName("gemini-1.5-flash")
                .promptVersion("RECOMMENDATION_PROMPT_V1")
                .generationMode("EVIDENCE_GROUNDED")
                .build();

        when(aiServiceClient.generateRecommendations(any())).thenReturn(aiResponse);

        ValidationResult validResult = ValidationResult.builder()
                .valid(true)
                .checks(List.of(
                        CheckResult.passed("SCHEMA", "Schema valid"),
                        CheckResult.passed("EVIDENCE", "Evidence verified"),
                        CheckResult.passed("HALLUCINATION", "No hallucination detected"),
                        CheckResult.passed("RELEVANCE", "Relevant to topic"),
                        CheckResult.passed("NOVELTY", "Idea is novel"),
                        CheckResult.passed("SAFETY", "Safety check passed")
                ))
                .build();

        when(recommendationValidator.validate(eq(draft), any(), any())).thenReturn(validResult);
        when(recommendationRepository.save(any(ContentRecommendation.class))).thenAnswer(invocation -> {
            ContentRecommendation rec = invocation.getArgument(0);
            rec.setId(UUID.randomUUID());
            return rec;
        });

        RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                testTopic.getId(), "VIDEO", "EDUCATIONAL", null, 1, GenerationMode.EVIDENCE_GROUNDED
        );

        List<RecommendationResponse> responses = recommendationService.generateRecommendations(testUser, request);

        assertThat(responses).hasSize(1);
        RecommendationResponse response = responses.get(0);
        assertThat(response.status()).isEqualTo(RecommendationStatus.VALIDATED);
        assertThat(response.title()).isEqualTo("5 Spring Boot 3.2 Virtual Threads Pitfalls");
        assertThat(response.repairAttempted()).isFalse();

        verify(aiServiceClient, never()).repairRecommendation(any());
        verify(recommendationRepository).save(any());
    }

    @Test
    @DisplayName("Should execute ONE-SHOT REPAIR and succeed if repaired draft passes validation")
    void testGenerate_RepairSuccess() {
        when(rateLimitingService.tryConsumeAi(testUser.getId())).thenReturn(true);
        when(evidenceRetrievalService.retrieveEvidence(eq(testUser), any())).thenReturn(sampleContext);
        when(topicRepository.findByIdAndUserId(testTopic.getId(), testUser.getId())).thenReturn(Optional.of(testTopic));

        AiRecommendationDraft initialDraft = AiRecommendationDraft.builder()
                .title("Why 99% of developers fail with virtual threads") // Unsupported percentage
                .contentType("VIDEO")
                .angle("Deep performance benchmarking")
                .targetAudience("Spring Developers")
                .problemAddressed("Pinning carrier threads")
                .hook("Are your threads slow?")
                .callToAction("Subscribe!")
                .evidenceIds(List.of("topic:" + testTopic.getId()))
                .confidence(0.90)
                .build();

        AiRecommendationDraft repairedDraft = AiRecommendationDraft.builder()
                .title("How Virtual Threads Can Pin Carrier Threads in Spring Boot")
                .contentType("VIDEO")
                .angle("Deep performance benchmarking")
                .targetAudience("Spring Developers")
                .problemAddressed("Pinning carrier threads")
                .hook("Are your threads slow?")
                .callToAction("Subscribe!")
                .evidenceIds(List.of("topic:" + testTopic.getId()))
                .confidence(0.91)
                .build();

        when(aiServiceClient.generateRecommendations(any())).thenReturn(
                AiRecommendationGenerateResponse.builder()
                        .drafts(List.of(initialDraft))
                        .modelName("gemini-1.5-flash")
                        .promptVersion("RECOMMENDATION_PROMPT_V1")
                        .generationMode("EVIDENCE_GROUNDED")
                        .build()
        );

        ValidationResult failedResult = ValidationResult.builder()
                .valid(false)
                .checks(List.of(
                        CheckResult.passed("SCHEMA", "Valid"),
                        CheckResult.failed("HALLUCINATION", "Unsupported statistical claim: 99%")
                ))
                .build();

        ValidationResult repairedValidResult = ValidationResult.builder()
                .valid(true)
                .checks(List.of(
                        CheckResult.passed("SCHEMA", "Valid"),
                        CheckResult.passed("HALLUCINATION", "Cleaned")
                ))
                .build();

        when(recommendationValidator.validate(eq(initialDraft), any(), any())).thenReturn(failedResult);
        when(aiServiceClient.repairRecommendation(any())).thenReturn(
                AiRecommendationRepairResponse.builder()
                        .repairedDraft(repairedDraft)
                        .repairExplanation("Removed 99% claim")
                        .build()
        );
        when(recommendationValidator.validate(eq(repairedDraft), any(), any())).thenReturn(repairedValidResult);

        when(recommendationRepository.save(any(ContentRecommendation.class))).thenAnswer(invocation -> {
            ContentRecommendation rec = invocation.getArgument(0);
            rec.setId(UUID.randomUUID());
            return rec;
        });

        RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                testTopic.getId(), "VIDEO", "EDUCATIONAL", null, 1, GenerationMode.EVIDENCE_GROUNDED
        );

        List<RecommendationResponse> responses = recommendationService.generateRecommendations(testUser, request);

        assertThat(responses).hasSize(1);
        RecommendationResponse response = responses.get(0);
        assertThat(response.status()).isEqualTo(RecommendationStatus.VALIDATED);
        assertThat(response.repairAttempted()).isTrue();
        assertThat(response.title()).isEqualTo("How Virtual Threads Can Pin Carrier Threads in Spring Boot");

        verify(aiServiceClient, times(1)).repairRecommendation(any());
    }

    @Test
    @DisplayName("Should REJECT recommendation when repair retry fails validation (Stopping Rule: Max 1 retry)")
    void testGenerate_RepairFails_ResultsInRejected() {
        when(rateLimitingService.tryConsumeAi(testUser.getId())).thenReturn(true);
        when(evidenceRetrievalService.retrieveEvidence(eq(testUser), any())).thenReturn(sampleContext);

        AiRecommendationDraft initialDraft = AiRecommendationDraft.builder()
                .title("Draft Title")
                .contentType("VIDEO")
                .angle("Angle")
                .targetAudience("Audience")
                .problemAddressed("Problem")
                .hook("Hook")
                .callToAction("CTA")
                .evidenceIds(List.of("topic:" + testTopic.getId()))
                .confidence(0.90)
                .build();

        AiRecommendationDraft repairedDraftStillInvalid = AiRecommendationDraft.builder()
                .title("Repaired Draft Title")
                .contentType("VIDEO")
                .angle("Angle")
                .targetAudience("Audience")
                .problemAddressed("Problem")
                .hook("Hook")
                .callToAction("CTA")
                .evidenceIds(List.of("topic:" + testTopic.getId(), "comment:FABRICATED"))
                .confidence(0.90)
                .build();

        when(aiServiceClient.generateRecommendations(any())).thenReturn(
                AiRecommendationGenerateResponse.builder()
                        .drafts(List.of(initialDraft))
                        .modelName("gemini-1.5-flash")
                        .promptVersion("RECOMMENDATION_PROMPT_V1")
                        .generationMode("EVIDENCE_GROUNDED")
                        .build()
        );

        ValidationResult failedResult1 = ValidationResult.builder()
                .valid(false)
                .checks(List.of(CheckResult.failed("EVIDENCE", "Missing evidence")))
                .build();

        ValidationResult failedResult2 = ValidationResult.builder()
                .valid(false)
                .checks(List.of(CheckResult.failed("EVIDENCE", "Still contains fabricated ID")))
                .build();

        when(recommendationValidator.validate(eq(initialDraft), any(), any())).thenReturn(failedResult1);
        when(aiServiceClient.repairRecommendation(any())).thenReturn(
                AiRecommendationRepairResponse.builder()
                        .repairedDraft(repairedDraftStillInvalid)
                        .repairExplanation("Attempted fix")
                        .build()
        );
        when(recommendationValidator.validate(eq(repairedDraftStillInvalid), any(), any())).thenReturn(failedResult2);

        when(recommendationRepository.save(any(ContentRecommendation.class))).thenAnswer(invocation -> {
            ContentRecommendation rec = invocation.getArgument(0);
            rec.setId(UUID.randomUUID());
            return rec;
        });

        RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                null, "VIDEO", "EDUCATIONAL", null, 1, GenerationMode.EVIDENCE_GROUNDED
        );

        List<RecommendationResponse> responses = recommendationService.generateRecommendations(testUser, request);

        assertThat(responses).hasSize(1);
        RecommendationResponse response = responses.get(0);
        assertThat(response.status()).isEqualTo(RecommendationStatus.REJECTED);
        assertThat(response.repairAttempted()).isTrue();

        // Ensure LLM repair was called EXACTLY ONCE
        verify(aiServiceClient, times(1)).repairRecommendation(any());
    }

    @Test
    @DisplayName("Should throw 429 RATE_LIMIT_EXCEEDED when AI rate limit is exhausted")
    void testGenerate_RateLimitExceeded() {
        when(rateLimitingService.tryConsumeAi(testUser.getId())).thenReturn(false);

        RecommendationGenerateRequest request = new RecommendationGenerateRequest(
                testTopic.getId(), "VIDEO", "EDUCATIONAL", null, 1, GenerationMode.EVIDENCE_GROUNDED
        );

        assertThatThrownBy(() -> recommendationService.generateRecommendations(testUser, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("AI recommendation rate limit exceeded");

        verifyNoInteractions(evidenceRetrievalService);
        verifyNoInteractions(aiServiceClient);
    }

    @Test
    @DisplayName("Should throw 404 when querying recommendation of another user (Tenant Isolation)")
    void testGetRecommendationById_CrossUser_ThrowsNotFound() {
        UUID recommendationId = UUID.randomUUID();
        when(recommendationRepository.findByIdAndUserId(recommendationId, testUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getRecommendationById(testUser, recommendationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recommendation not found");
    }
}
