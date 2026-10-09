package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.production.ContentProductionAsset;
import com.pulsegpt.production.ContentProductionAssetRepository;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.production.ProductionAssetType;
import com.pulsegpt.production.dto.ProductionGenerateRequest;
import com.pulsegpt.production.dto.UpdateProductionAssetRequest;
import com.pulsegpt.recommendation.*;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3K — Content Scripting & Production Copilot Integration Tests")
class ContentProductionIntegrationTest {

    @TestConfiguration
    static class TestDataSourceConfig {
        @Bean
        @Primary
        public DataSource dataSource() throws SQLException {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(ds.getConnection(any(), any())).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(metaData);
            when(metaData.getConnection()).thenReturn(conn);
            when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(metaData.getDatabaseProductVersion()).thenReturn("16.0");
            when(metaData.getDatabaseMajorVersion()).thenReturn(16);
            when(metaData.getDatabaseMinorVersion()).thenReturn(0);
            when(metaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(metaData.getDriverVersion()).thenReturn("42.7.2");
            when(metaData.getDriverMajorVersion()).thenReturn(42);
            when(metaData.getDriverMinorVersion()).thenReturn(7);
            return ds;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TopicRepository topicRepository;

    @MockitoBean
    private ContentRecommendationRepository recommendationRepository;

    @MockitoBean
    private CalendarItemRepository calendarItemRepository;

    @MockitoBean
    private ContentProductionAssetRepository productionAssetRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private com.pulsegpt.recommendation.service.EvidenceRetrievalService evidenceRetrievalService;

    private User creatorA;
    private User creatorB;
    private String tokenA;
    private String tokenB;
    private Topic topicA;
    private ContentRecommendation recommendationA;
    private CalendarItem calendarItemA;

    @BeforeEach
    void setUp() {
        creatorA = User.builder()
                .id(UUID.randomUUID())
                .name("Alex Rivera")
                .email("alex@creators.com")
                .passwordHash("hashed")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        creatorB = User.builder()
                .id(UUID.randomUUID())
                .name("Bianca Chen")
                .email("bianca@creators.com")
                .passwordHash("hashed")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        when(userRepository.findByEmail(creatorA.getEmail())).thenReturn(Optional.of(creatorA));
        when(userRepository.findById(creatorA.getId())).thenReturn(Optional.of(creatorA));
        when(userRepository.findByEmail(creatorB.getEmail())).thenReturn(Optional.of(creatorB));
        when(userRepository.findById(creatorB.getId())).thenReturn(Optional.of(creatorB));

        tokenA = "Bearer " + jwtService.generateAccessToken(creatorA);
        tokenB = "Bearer " + jwtService.generateAccessToken(creatorB);

        topicA = Topic.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .name("PostgreSQL Performance")
                .active(true)
                .build();

        recommendationA = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .topic(topicA)
                .title("Optimizing PostgreSQL Query Plans")
                .angle("Deep dive into indexing and explain plans")
                .targetAudience("Database Engineers")
                .problemAddress("Slow query performance on unindexed join columns")
                .keyPoints(List.of("Index types", "Query plans", "Connection pooling"))
                .contentType("VIDEO")
                .status(RecommendationStatus.APPROVED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .build();

        calendarItemA = CalendarItem.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .recommendation(recommendationA)
                .topic(topicA)
                .title("PostgreSQL Optimization Video")
                .platform(PlatformType.YOUTUBE)
                .scheduledAt(Instant.now().plusSeconds(86400))
                .build();

        when(topicRepository.findByIdAndUserId(topicA.getId(), creatorA.getId())).thenReturn(Optional.of(topicA));
        when(topicRepository.findByUserIdAndActiveTrue(creatorA.getId())).thenReturn(List.of(topicA));
        when(recommendationRepository.findByIdAndUserId(recommendationA.getId(), creatorA.getId())).thenReturn(Optional.of(recommendationA));
        when(calendarItemRepository.findByIdAndUserId(calendarItemA.getId(), creatorA.getId())).thenReturn(Optional.of(calendarItemA));

        when(evidenceRetrievalService.retrieveEvidence(any(), any())).thenReturn(
                RecommendationContext.builder()
                        .topics(List.of(topicA))
                        .evidenceItems(List.of())
                        .questions(List.of())
                        .representativeComments(List.of())
                        .contentHistory(List.of())
                        .previousRecommendations(List.of())
                        .build()
        );
    }

    private Map<String, Object> createValidDraftPayload() {
        Map<String, Object> briefMap = new HashMap<>();
        briefMap.put("title", "Optimizing PostgreSQL Query Plans");
        briefMap.put("contentType", "VIDEO");
        briefMap.put("platform", "YOUTUBE");
        briefMap.put("targetAudience", "Database Engineers");
        briefMap.put("audienceProblem", "Slow queries");
        briefMap.put("audienceEvidence", List.of("Frequent viewer queries"));
        briefMap.put("coreMessage", "Systematic query tuning");
        briefMap.put("contentAngle", "Practical benchmarks");
        briefMap.put("keyPoints", List.of("B-Tree indexes", "Execution plans"));
        briefMap.put("tone", "Technical");
        briefMap.put("callToAction", "Subscribe for more DB tips");
        briefMap.put("successObjective", "Improve developer query performance");

        return Map.of(
                "brief", briefMap,
                "outline", Map.of(
                        "format", "VIDEO_10_PART_STRUCTURE",
                        "sections", List.of(
                                Map.of("sectionTitle", "1. Hook", "purpose", "Engage", "talkingPoints", List.of("Show slow vs fast query")),
                                Map.of("sectionTitle", "2. Intro", "purpose", "Frame", "talkingPoints", List.of("Explain fundamentals")),
                                Map.of("sectionTitle", "3. Deep Dive", "purpose", "Analyze", "talkingPoints", List.of("Explain plan breakdown"))
                        ),
                        "keyTakeaway", "Index strategy is essential"
                ),
                "hooks", List.of(
                        Map.of("hookType", "QUESTION", "text", "Why is your PostgreSQL query taking seconds?", "rationale", "Direct question"),
                        Map.of("hookType", "PROBLEM", "text", "Missing indexes cause full table scans under load.", "rationale", "Pain point"),
                        Map.of("hookType", "CONTRAST", "text", "One single index can reduce 10s to 5ms.", "rationale", "Contrast")
                ),
                "titles", List.of(
                        Map.of("titleType", "HOW_TO", "text", "How to Optimize PostgreSQL Queries", "rationale", "Intent"),
                        Map.of("titleType", "DIRECT_BENEFIT", "text", "Fix Slow PostgreSQL Queries in 5 Minutes", "rationale", "Benefit"),
                        Map.of("titleType", "QUESTION", "text", "Are You Using PostgreSQL Indexes Correctly?", "rationale", "Diagnostic")
                ),
                "ctas", List.of(
                        Map.of("ctaType", "COMMENT_ENGAGEMENT", "text", "What is your slowest query? Tell us in the comments!"),
                        Map.of("ctaType", "SUBSCRIBE_FOLLOW", "text", "Subscribe for weekly backend architecture breakdowns.")
                ),
                "thumbnail", Map.of(
                        "concept", "Red slow bar versus green fast bar comparison",
                        "visualSubject", "Developer observing clean EXPLAIN output",
                        "composition", "Side by side split",
                        "textOverlay", "FIX SLOW SQL",
                        "emotion", "Relieved, authoritative",
                        "style", "Clean modern tech art"
                ),
                "checklist", Map.of(
                        "items", List.of(
                                Map.of("phase", "PRE_PRODUCTION", "task", "Prepare test database schema", "completed", false),
                                Map.of("phase", "PRODUCTION", "task", "Record benchmark walkthrough", "completed", false),
                                Map.of("phase", "POST_PRODUCTION", "task", "Review claims and captions", "completed", false)
                        )
                ),
                "evidenceIds", List.of()
        );
    }


    @Nested
    @DisplayName("POST /api/v1/production-assets/generate")
    class GenerateProductionAssetsTests {

        @Test
        @DisplayName("Should generate production assets successfully from validated recommendation")
        void testGenerateProductionAssetsSuccess() throws Exception {
            AiProductionGenerateResponse aiResp = AiProductionGenerateResponse.builder()
                    .requestId("req-test-1")
                    .generationMode("BASELINE")
                    .promptVersion("PRODUCTION_PROMPT_V1")
                    .modelName("pulsegpt-production-v1")
                    .draft(createValidDraftPayload())
                    .executionTimeMs(150.0)
                    .build();

            when(aiServiceClient.generateProductionDraft(any())).thenReturn(aiResp);

            when(productionAssetRepository.save(any(ContentProductionAsset.class)))
                    .thenAnswer(inv -> {
                        ContentProductionAsset asset = inv.getArgument(0);
                        asset.setId(UUID.randomUUID());
                        asset.setCreatedAt(Instant.now());
                        asset.setUpdatedAt(Instant.now());
                        return asset;
                    });

            ProductionGenerateRequest request = ProductionGenerateRequest.builder()
                    .recommendationId(recommendationA.getId())
                    .calendarItemId(calendarItemA.getId())
                    .assetTypes(List.of(ProductionAssetType.CONTENT_BRIEF, ProductionAssetType.HOOK, ProductionAssetType.TITLE_VARIATION))
                    .mode(GenerationMode.BASELINE)
                    .build();

            mockMvc.perform(post("/production-assets/generate")
                            .header(HttpHeaders.AUTHORIZATION, tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].recommendationId", is(recommendationA.getId().toString())))
                    .andExpect(jsonPath("$.data[0].calendarItemId", is(calendarItemA.getId().toString())))
                    .andExpect(jsonPath("$.data[0].status", is("VALIDATED")));
        }

        @Test
        @DisplayName("Should return 400 if recommendation is not validated or approved")
        void testGenerateFailsOnUnapprovedRecommendation() throws Exception {
            recommendationA.setStatus(RecommendationStatus.GENERATED);

            ProductionGenerateRequest request = ProductionGenerateRequest.builder()
                    .recommendationId(recommendationA.getId())
                    .build();

            mockMvc.perform(post("/production-assets/generate")
                            .header(HttpHeaders.AUTHORIZATION, tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("INVALID_RECOMMENDATION_STATUS")));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void testGenerateFailsUnauthenticated() throws Exception {
            ProductionGenerateRequest request = ProductionGenerateRequest.builder()
                    .recommendationId(recommendationA.getId())
                    .build();

            mockMvc.perform(post("/production-assets/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/production-assets & GET /api/v1/production-assets/{id}")
    class SearchAndGetAssetsTests {

        @Test
        @DisplayName("Should retrieve paginated production assets for authenticated user")
        void testSearchAssets() throws Exception {
            ContentProductionAsset asset = ContentProductionAsset.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .recommendation(recommendationA)
                    .calendarItem(calendarItemA)
                    .assetType(ProductionAssetType.CONTENT_BRIEF)
                    .status(ProductionAssetStatus.VALIDATED)
                    .contentJson(Map.of("title", "Postgres Tuning"))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(productionAssetRepository.searchAssets(eq(creatorA.getId()), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(asset)));

            mockMvc.perform(get("/production-assets")
                            .header(HttpHeaders.AUTHORIZATION, tokenA)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].assetType", is("CONTENT_BRIEF")));
        }

        @Test
        @DisplayName("Should enforce user isolation: User B cannot access User A's production asset (404)")
        void testCrossUserAccessReturns404() throws Exception {
            UUID assetId = UUID.randomUUID();
            when(productionAssetRepository.findByIdAndUserId(assetId, creatorB.getId())).thenReturn(Optional.empty());

            mockMvc.perform(get("/production-assets/" + assetId)
                            .header(HttpHeaders.AUTHORIZATION, tokenB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("ASSET_NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("PATCH, APPROVE, ARCHIVE & EVIDENCE Lifecycle")
    class LifecycleTests {

        private UUID assetId;
        private ContentProductionAsset sampleAsset;

        @BeforeEach
        void initAsset() {
            assetId = UUID.randomUUID();
            sampleAsset = ContentProductionAsset.builder()
                    .id(assetId)
                    .user(creatorA)
                    .recommendation(recommendationA)
                    .calendarItem(calendarItemA)
                    .assetType(ProductionAssetType.HOOK)
                    .status(ProductionAssetStatus.VALIDATED)
                    .version(1)
                    .contentJson(Map.of("hook", "Original AI Draft Hook"))
                    .evidenceSnapshot(Map.of("topicName", "PostgreSQL", "evidenceCount", 2))
                    .revisionHistory(new ArrayList<>())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(productionAssetRepository.findByIdAndUserId(assetId, creatorA.getId())).thenReturn(Optional.of(sampleAsset));
            when(productionAssetRepository.save(any(ContentProductionAsset.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Creator can edit asset: transitions status to EDITED and increments version")
        void testEditAsset() throws Exception {
            UpdateProductionAssetRequest updateReq = UpdateProductionAssetRequest.builder()
                    .hook("Refined Creator Voice Hook")
                    .notes("Edited for better hook punchiness")
                    .build();

            mockMvc.perform(patch("/production-assets/" + assetId)
                            .header(HttpHeaders.AUTHORIZATION, tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("EDITED")))
                    .andExpect(jsonPath("$.data.version", is(2)))
                    .andExpect(jsonPath("$.data.contentJson.hook", is("Refined Creator Voice Hook")));
        }

        @Test
        @DisplayName("Creator can approve asset: transitions status to APPROVED (does NOT auto-publish)")
        void testApproveAsset() throws Exception {
            mockMvc.perform(post("/production-assets/" + assetId + "/approve")
                            .header(HttpHeaders.AUTHORIZATION, tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("APPROVED")))
                    .andExpect(jsonPath("$.data.approvedAt", notNullValue()));
        }

        @Test
        @DisplayName("Creator can archive asset: transitions status to ARCHIVED")
        void testArchiveAsset() throws Exception {
            mockMvc.perform(post("/production-assets/" + assetId + "/archive")
                            .header(HttpHeaders.AUTHORIZATION, tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("ARCHIVED")));
        }

        @Test
        @DisplayName("Creator can inspect evidence snapshot for asset")
        void testGetAssetEvidence() throws Exception {
            mockMvc.perform(get("/production-assets/" + assetId + "/evidence")
                            .header(HttpHeaders.AUTHORIZATION, tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.topicName", is("PostgreSQL")))
                    .andExpect(jsonPath("$.data.evidenceCount", is(2)));
        }
    }
}

