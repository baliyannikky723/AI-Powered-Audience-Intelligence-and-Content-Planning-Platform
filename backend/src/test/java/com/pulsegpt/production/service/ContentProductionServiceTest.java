package com.pulsegpt.production.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.production.*;
import com.pulsegpt.production.dto.*;
import com.pulsegpt.production.validation.ProductionAssetValidator;
import com.pulsegpt.recommendation.*;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.recommendation.service.EvidenceRetrievalService;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRole;
import com.pulsegpt.validation.CheckResult;
import com.pulsegpt.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Content Production Service Unit Tests")
class ContentProductionServiceTest {

    @Mock
    private ContentProductionAssetRepository productionAssetRepository;

    @Mock
    private ContentRecommendationRepository recommendationRepository;

    @Mock
    private CalendarItemRepository calendarItemRepository;

    @Mock
    private EvidenceRetrievalService evidenceRetrievalService;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private ProductionAssetValidator productionAssetValidator;

    @Mock
    private AuditService auditService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ContentProductionService productionService;

    private User testUser;
    private User otherUser;
    private ContentRecommendation validRec;
    private CalendarItem calendarItem;
    private Topic topic;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("creator@pulsegpt.ai")
                .name("Creator User")
                .role(UserRole.CREATOR)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@pulsegpt.ai")
                .name("Other User")
                .role(UserRole.CREATOR)
                .build();

        topic = Topic.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("System Design & Microservices")
                .build();

        validRec = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .topic(topic)
                .title("Architecting Event-Driven Microservices")
                .angle("Deep dive into Kafka and RabbitMQ trade-offs")
                .targetAudience("Senior Developers")
                .problemAddress("Handling eventual consistency and dead-letter queues")
                .keyPoints(List.of("Event sourcing vs CDC", "Idempotency patterns"))
                .contentType("VIDEO")
                .status(RecommendationStatus.APPROVED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .build();

        calendarItem = CalendarItem.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(validRec)
                .topic(topic)
                .title("Event Driven Architecture Video")
                .platform(PlatformType.YOUTUBE)
                .scheduledAt(Instant.now().plusSeconds(86400))
                .build();
    }

    private Map<String, Object> createMockDraftMap() {
        Map<String, Object> briefMap = new HashMap<>();
        briefMap.put("title", "Architecting Event-Driven Microservices");
        briefMap.put("contentType", "VIDEO");
        briefMap.put("platform", "YOUTUBE");
        briefMap.put("targetAudience", "Senior Developers");
        briefMap.put("audienceProblem", "Eventual consistency issues");
        briefMap.put("audienceEvidence", List.of("Frequent viewer questions"));
        briefMap.put("coreMessage", "Building robust distributed systems");
        briefMap.put("contentAngle", "Practical architecture design");
        briefMap.put("keyPoints", List.of("Event sourcing", "Idempotency"));
        briefMap.put("tone", "Technical");
        briefMap.put("callToAction", "Subscribe for architecture deep dives");
        briefMap.put("successObjective", "Clarify microservice patterns");

        return Map.of(
                "brief", briefMap,
                "outline", Map.of(
                        "format", "VIDEO_10_PART_STRUCTURE",
                        "sections", List.of(
                                Map.of("sectionTitle", "1. Hook", "purpose", "Engage", "talkingPoints", List.of("Point 1")),
                                Map.of("sectionTitle", "2. Intro", "purpose", "Frame", "talkingPoints", List.of("Point 2")),
                                Map.of("sectionTitle", "3. Solution", "purpose", "Solve", "talkingPoints", List.of("Point 3"))
                        ),
                        "keyTakeaway", "Master event-driven architecture"
                ),
                "hooks", List.of(
                        Map.of("hookType", "QUESTION", "text", "Struggling with eventual consistency?", "rationale", "Direct"),
                        Map.of("hookType", "PROBLEM", "text", "Microservices break when you ignore message ordering.", "rationale", "Pain point"),
                        Map.of("hookType", "CONTRAST", "text", "REST vs Events is not a binary choice.", "rationale", "Contrast")
                ),
                "titles", List.of(
                        Map.of("titleType", "HOW_TO", "text", "How to Build Event-Driven Systems", "rationale", "Intent"),
                        Map.of("titleType", "DIRECT_BENEFIT", "text", "Fix Eventual Consistency in Microservices", "rationale", "Benefit"),
                        Map.of("titleType", "QUESTION", "text", "Are Microservices Overrated?", "rationale", "Curiosity")
                ),
                "ctas", List.of(
                        Map.of("ctaType", "COMMENT_ENGAGEMENT", "text", "Which message broker do you prefer?"),
                        Map.of("ctaType", "SUBSCRIBE_FOLLOW", "text", "Subscribe for weekly system design breakdowns.")
                ),
                "thumbnail", Map.of(
                        "concept", "Architectural diagram split with glowing queue arrows",
                        "visualSubject", "Microservice nodes in sync",
                        "composition", "Isometric perspective",
                        "textOverlay", "EVENT DRIVEN",
                        "emotion", "Authoritative",
                        "style", "Modern digital art"
                ),
                "checklist", Map.of(
                        "items", List.of(
                                Map.of("phase", "PRE_PRODUCTION", "task", "Prepare architecture diagrams", "completed", false),
                                Map.of("phase", "PRODUCTION", "task", "Record code demo", "completed", false),
                                Map.of("phase", "POST_PRODUCTION", "task", "Add chapter markers", "completed", false)
                        )
                ),
                "evidenceIds", List.of("topic:event_driven")
        );
    }

    @Test
    @DisplayName("Generate production assets successfully from approved recommendation")
    void testGenerateProductionAssetsSuccess() {
        when(recommendationRepository.findByIdAndUserId(validRec.getId(), testUser.getId()))
                .thenReturn(Optional.of(validRec));

        when(calendarItemRepository.findByIdAndUserId(calendarItem.getId(), testUser.getId()))
                .thenReturn(Optional.of(calendarItem));

        EvidenceItem ev1 = EvidenceItem.builder()
                .evidenceId("topic:event_driven")
                .sourceType(EvidenceSourceType.TOPIC)
                .sourceId(UUID.randomUUID())
                .summary("Event driven discussion")
                .relevanceScore(0.9)
                .build();

        when(evidenceRetrievalService.retrieveEvidence(eq(testUser), any()))
                .thenReturn(RecommendationContext.builder().topics(List.of(topic)).evidenceItems(List.of(ev1)).build());

        Map<String, Object> draftMap = createMockDraftMap();

        AiProductionGenerateResponse aiResp = AiProductionGenerateResponse.builder()
                .requestId("req-123")
                .generationMode("EVIDENCE_GROUNDED")
                .promptVersion("PRODUCTION_PROMPT_V1")
                .modelName("pulsegpt-production-v1")
                .draft(draftMap)
                .executionTimeMs(120.0)
                .build();

        when(aiServiceClient.generateProductionDraft(any())).thenReturn(aiResp);

        ValidationResult passValidation = ValidationResult.builder()
                .valid(true)
                .checks(List.of(CheckResult.passed("SCHEMA", "Valid"), CheckResult.passed("EVIDENCE", "Valid")))
                .build();

        when(productionAssetValidator.validate(any(), any(), any())).thenReturn(passValidation);

        when(productionAssetRepository.save(any(ContentProductionAsset.class)))
                .thenAnswer(inv -> {
                    ContentProductionAsset saved = inv.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    saved.setCreatedAt(Instant.now());
                    saved.setUpdatedAt(Instant.now());
                    return saved;
                });

        ProductionGenerateRequest request = ProductionGenerateRequest.builder()
                .recommendationId(validRec.getId())
                .calendarItemId(calendarItem.getId())
                .assetTypes(List.of(ProductionAssetType.CONTENT_BRIEF, ProductionAssetType.SCRIPT_OUTLINE, ProductionAssetType.HOOK))
                .build();

        List<ProductionAssetResponse> results = productionService.generateProductionAssets(testUser, request);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).status()).isEqualTo(ProductionAssetStatus.VALIDATED);
        assertThat(results.get(0).recommendationId()).isEqualTo(validRec.getId());
        assertThat(results.get(0).calendarItemId()).isEqualTo(calendarItem.getId());

        verify(auditService, atLeastOnce()).logAuditEvent(eq(testUser), eq("PRODUCTION_ASSET_GENERATED"), eq("PRODUCTION_ASSET"), any(), any());
    }

    @Test
    @DisplayName("Generate production assets fails if recommendation is unapproved or draft")
    void testGenerateFailsOnUnapprovedRecommendation() {
        validRec.setStatus(RecommendationStatus.GENERATED); // not validated or approved
        when(recommendationRepository.findByIdAndUserId(validRec.getId(), testUser.getId()))
                .thenReturn(Optional.of(validRec));

        ProductionGenerateRequest request = ProductionGenerateRequest.builder()
                .recommendationId(validRec.getId())
                .build();

        assertThatThrownBy(() -> productionService.generateProductionAssets(testUser, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Recommendation must be VALIDATED or APPROVED");
    }

    @Test
    @DisplayName("1-shot repair triggers on validation failure and updates asset to repaired draft")
    void testOneShotRepairSucceeds() {
        when(recommendationRepository.findByIdAndUserId(validRec.getId(), testUser.getId()))
                .thenReturn(Optional.of(validRec));

        when(evidenceRetrievalService.retrieveEvidence(eq(testUser), any()))
                .thenReturn(RecommendationContext.builder().topics(List.of(topic)).evidenceItems(Collections.emptyList()).build());


        Map<String, Object> flawedDraft = createMockDraftMap();

        AiProductionGenerateResponse initialAiResp = AiProductionGenerateResponse.builder()
                .requestId("req-flawed")
                .generationMode("EVIDENCE_GROUNDED")
                .draft(flawedDraft)
                .build();

        when(aiServiceClient.generateProductionDraft(any())).thenReturn(initialAiResp);

        ValidationResult failValidation = ValidationResult.builder()
                .valid(false)
                .checks(List.of(CheckResult.failed("UNSUPPORTED_CLAIMS", "Fabricated percentage found")))
                .failureSummary("UNSUPPORTED_CLAIMS: Fabricated percentage found")
                .build();

        ValidationResult passValidation = ValidationResult.builder()
                .valid(true)
                .checks(List.of(CheckResult.passed("UNSUPPORTED_CLAIMS", "Repaired")))
                .build();

        when(productionAssetValidator.validate(any(), any(), any()))
                .thenReturn(failValidation) // First validation fails
                .thenReturn(passValidation); // Second validation after repair passes

        AiProductionRepairResponse repResp = AiProductionRepairResponse.builder()
                .requestId("rep-001")
                .repairedDraft(flawedDraft)
                .repairExplanation("Cleaned statistical exaggeration")
                .executionTimeMs(80.0)
                .build();

        when(aiServiceClient.repairProductionDraft(any())).thenReturn(repResp);

        when(productionAssetRepository.save(any(ContentProductionAsset.class)))
                .thenAnswer(inv -> {
                    ContentProductionAsset saved = inv.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    saved.setCreatedAt(Instant.now());
                    saved.setUpdatedAt(Instant.now());
                    return saved;
                });

        ProductionGenerateRequest request = ProductionGenerateRequest.builder()
                .recommendationId(validRec.getId())
                .assetTypes(List.of(ProductionAssetType.CONTENT_BRIEF))
                .build();

        List<ProductionAssetResponse> results = productionService.generateProductionAssets(testUser, request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(ProductionAssetStatus.VALIDATED);
        assertThat(results.get(0).repairAttempted()).isTrue();

        verify(aiServiceClient, times(1)).repairProductionDraft(any());
        verify(auditService, atLeastOnce()).logAuditEvent(eq(testUser), eq("PRODUCTION_ASSET_REPAIRED"), eq("PRODUCTION_ASSET"), any(), any());
    }

    @Test
    @DisplayName("Creator can edit an asset draft, bumping version and saving revision history")
    void testCreatorEditAsset() {
        UUID assetId = UUID.randomUUID();
        ContentProductionAsset asset = ContentProductionAsset.builder()
                .id(assetId)
                .user(testUser)
                .recommendation(validRec)
                .assetType(ProductionAssetType.HOOK)
                .status(ProductionAssetStatus.VALIDATED)
                .version(1)
                .contentJson(Map.of("hook", "Original Hook Text"))
                .revisionHistory(new ArrayList<>())
                .build();

        when(productionAssetRepository.findByIdAndUserId(assetId, testUser.getId()))
                .thenReturn(Optional.of(asset));

        when(productionAssetRepository.save(any(ContentProductionAsset.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateProductionAssetRequest updateReq = UpdateProductionAssetRequest.builder()
                .hook("Updated Creator Crafted Hook")
                .notes("Refined to match creator voice")
                .build();

        ProductionAssetResponse updated = productionService.updateAsset(testUser, assetId, updateReq);

        assertThat(updated.version()).isEqualTo(2);
        assertThat(updated.status()).isEqualTo(ProductionAssetStatus.EDITED);
        assertThat(updated.contentJson().get("hook")).isEqualTo("Updated Creator Crafted Hook");
        assertThat(updated.revisionHistory()).hasSize(1);

        verify(auditService).logAuditEvent(eq(testUser), eq("PRODUCTION_ASSET_EDITED"), eq("PRODUCTION_ASSET"), eq(assetId.toString()), any());
    }

    @Test
    @DisplayName("Creator can approve a production draft without automatic publishing")
    void testCreatorApproveAsset() {
        UUID assetId = UUID.randomUUID();
        ContentProductionAsset asset = ContentProductionAsset.builder()
                .id(assetId)
                .user(testUser)
                .recommendation(validRec)
                .assetType(ProductionAssetType.CONTENT_BRIEF)
                .status(ProductionAssetStatus.EDITED)
                .contentJson(Map.of("title", "Brief Title"))
                .build();

        when(productionAssetRepository.findByIdAndUserId(assetId, testUser.getId()))
                .thenReturn(Optional.of(asset));

        when(productionAssetRepository.save(any(ContentProductionAsset.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductionAssetResponse approved = productionService.approveAsset(testUser, assetId);

        assertThat(approved.status()).isEqualTo(ProductionAssetStatus.APPROVED);
        assertThat(approved.approvedAt()).isNotNull();
        assertThat(approved.approvedBy()).isEqualTo(testUser.getId());

        verify(auditService).logAuditEvent(eq(testUser), eq("PRODUCTION_ASSET_APPROVED"), eq("PRODUCTION_ASSET"), eq(assetId.toString()), any());
    }

    @Test
    @DisplayName("Creator can archive a production asset")
    void testArchiveAsset() {
        UUID assetId = UUID.randomUUID();
        ContentProductionAsset asset = ContentProductionAsset.builder()
                .id(assetId)
                .user(testUser)
                .recommendation(validRec)
                .assetType(ProductionAssetType.THUMBNAIL_PROMPT)
                .status(ProductionAssetStatus.VALIDATED)
                .contentJson(Map.of("concept", "Thumbnail Concept"))
                .build();

        when(productionAssetRepository.findByIdAndUserId(assetId, testUser.getId()))
                .thenReturn(Optional.of(asset));

        when(productionAssetRepository.save(any(ContentProductionAsset.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductionAssetResponse archived = productionService.archiveAsset(testUser, assetId);

        assertThat(archived.status()).isEqualTo(ProductionAssetStatus.ARCHIVED);
        verify(auditService).logAuditEvent(eq(testUser), eq("PRODUCTION_ASSET_ARCHIVED"), eq("PRODUCTION_ASSET"), eq(assetId.toString()), any());
    }

    @Test
    @DisplayName("Cross-user access returns 404 NOT_FOUND")
    void testCrossUserAccessReturns404() {
        UUID assetId = UUID.randomUUID();
        when(productionAssetRepository.findByIdAndUserId(assetId, otherUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionService.getAsset(otherUser, assetId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Production asset not found or unauthorized");
    }
}
