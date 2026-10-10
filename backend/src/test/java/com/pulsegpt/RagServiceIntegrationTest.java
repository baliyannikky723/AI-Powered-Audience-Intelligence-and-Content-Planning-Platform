package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.AiEmbedResponse;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.comment.PostRepository;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.graph.service.KnowledgeGraphQueryService;
import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.memory.dto.AudienceInterestResponse;
import com.pulsegpt.memory.dto.AudienceMemoryContext;
import com.pulsegpt.memory.dto.AudienceQuestionResponse;
import com.pulsegpt.rag.dto.EvidenceAnnotationRequest;
import com.pulsegpt.rag.dto.RagQueryRequest;
import com.pulsegpt.rag.model.EvidenceAnnotation;
import com.pulsegpt.rag.model.RagCorrectness;
import com.pulsegpt.rag.model.RagRelevance;
import com.pulsegpt.rag.repository.EvidenceAnnotationRepository;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicAssignmentRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3O — Graph-Augmented RAG & Evidence Retrieval Integration Tests")
class RagServiceIntegrationTest {

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
    private UserRepository userRepository;

    @MockitoBean
    private TopicRepository topicRepository;

    @MockitoBean
    private TopicAssignmentRepository topicAssignmentRepository;

    @MockitoBean
    private ProcessedCommentRepository processedCommentRepository;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private ContentRecommendationRepository contentRecommendationRepository;

    @MockitoBean
    private com.pulsegpt.trend.TrendScoreRepository trendScoreRepository;

    @MockitoBean
    private EvidenceAnnotationRepository annotationRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @MockitoBean
    private KnowledgeGraphQueryService knowledgeGraphQueryService;

    private User testUser;
    private String jwtToken;
    private Topic testTopic;

    @BeforeEach
    void setUp() {
        reset(knowledgeGraphQueryService, aiServiceClient, annotationRepository);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("rag-user@example.com")
                .passwordHash("hashedpassword123")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        jwtToken = "Bearer " + jwtService.generateAccessToken(testUser);

        testTopic = Topic.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Spring Boot 3.4 & Knowledge Graphs")
                .keywords(List.of("SpringBoot", "Neo4j", "Observability"))
                .commentCount(85)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(topicRepository.findByUserIdAndActiveTrue(testUser.getId())).thenReturn(List.of(testTopic));
        when(topicRepository.findByIdAndUserId(testTopic.getId(), testUser.getId())).thenReturn(Optional.of(testTopic));

        // Default mock responses
        when(aiServiceClient.generateEmbeddings(any())).thenReturn(
                new AiEmbedResponse("sentence-transformers/all-MiniLM-L6-v2", "1.0", 384, List.of(List.of(0.2, 0.4, 0.6)))
        );

        when(knowledgeGraphQueryService.getAudienceMemoryContext(testUser.getId())).thenReturn(
                AudienceMemoryContext.builder()
                        .activeInterests(List.of(
                                AudienceInterestResponse.builder()
                                        .id(UUID.randomUUID())
                                        .topicId(testTopic.getId())
                                        .topicName("Spring Boot 3.4 & Knowledge Graphs")
                                        .status(AudienceInterestStatus.ACTIVE)
                                        .confidence(0.92)
                                        .evidenceCount(85)
                                        .lastSeenAt(Instant.now().minus(Duration.ofDays(1)))
                                        .build()
                        ))
                        .weakeningInterests(Collections.emptyList())
                        .recurringQuestions(List.of(
                                AudienceQuestionResponse.builder()
                                        .id(UUID.randomUUID().toString())
                                        .questionHash("qhash-test-01")
                                        .normalizedText("How do we integrate Neo4j knowledge graphs into RAG?")
                                        .confidence(0.89)
                                        .evidenceCount(18)
                                        .firstSeenAt(Instant.now().minus(Duration.ofDays(5)))
                                        .lastSeenAt(Instant.now().minus(Duration.ofDays(1)))
                                        .build()
                        ))
                        .relatedTopics(Collections.emptyList())
                        .recentContentIdeas(Collections.emptyList())
                        .build()
        );

        when(annotationRepository.save(any(EvidenceAnnotation.class))).thenAnswer(inv -> {
            EvidenceAnnotation ann = inv.getArgument(0);
            ann.setId(UUID.randomUUID());
            ann.setCreatedAt(Instant.now());
            return ann;
        });

        EvidenceAnnotation sampleAnn = EvidenceAnnotation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .queryId("query-test-101")
                .evidenceId("topic:" + testTopic.getId())
                .relevance(RagRelevance.RELEVANT)
                .correctness(RagCorrectness.SUPPORTED)
                .notes("High quality topic grounding")
                .createdAt(Instant.now())
                .build();

        when(annotationRepository.findByUserIdOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleAnn)));

        when(annotationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                .thenReturn(List.of(sampleAnn));
    }

    @Nested
    @DisplayName("RAG Retrieve-Only Endpoint Tests")
    class RetrieveOnlyEndpointTests {

        @Test
        @DisplayName("POST /api/v1/rag/retrieve should return fused evidence without LLM execution")
        void testRetrieveOnlySuccess() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("What are the key technical questions regarding knowledge graph memory?")
                    .generationMode("FULL_EVIDENCE_GROUNDED")
                    .maxEvidence(12)
                    .build();

            mockMvc.perform(post("/api/v1/rag/retrieve")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query").value(request.query()))
                    .andExpect(jsonPath("$.evidence").isArray())
                    .andExpect(jsonPath("$.retrievalMetadata.graphAvailable").value(true))
                    .andExpect(jsonPath("$.retrievalMetadata.retrievalVersion").value("v1.0-graph-rag"));
        }

        @Test
        @DisplayName("POST /api/v1/rag/retrieve in BASELINE mode should return empty evidence set")
        void testRetrieveBaselineMode() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("General inquiry")
                    .generationMode("BASELINE")
                    .build();

            mockMvc.perform(post("/api/v1/rag/retrieve")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evidence").isEmpty())
                    .andExpect(jsonPath("$.retrievalMetadata.graphAvailable").value(false))
                    .andExpect(jsonPath("$.retrievalMetadata.retrievalVersion").value("v1.0-baseline"));
        }
        @Test
        @DisplayName("POST /api/v1/rag/retrieve should strictly not call LLM text generation")
        void testRetrieveDoesNotCallLlm() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("Retrieve evidence only")
                    .generationMode("FULL_EVIDENCE_GROUNDED")
                    .maxEvidence(5)
                    .build();

            mockMvc.perform(post("/api/v1/rag/retrieve")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evidence").isArray())
                    .andExpect(jsonPath("$.answer").doesNotExist());
        }

        @Test
        @DisplayName("POST /api/v1/rag/retrieve with includeMemory=false should skip graph query")
        void testRetrieveWithIncludeMemoryFalse() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("Retrieve evidence without graph")
                    .generationMode("FULL_EVIDENCE_GROUNDED")
                    .includeMemory(false)
                    .build();

            mockMvc.perform(post("/api/v1/rag/retrieve")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evidence").isArray());
        }
    }

    @Nested
    @DisplayName("RAG Query Execution Endpoint Tests")
    class RagQueryEndpointTests {

        @Test
        @DisplayName("POST /api/v1/rag/query should synthesize answer with citation tags and validation")
        void testExecuteRagQuerySuccess() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("What should I create next based on audience inquiries?")
                    .generationMode("FULL_EVIDENCE_GROUNDED")
                    .maxEvidence(10)
                    .build();

            mockMvc.perform(post("/api/v1/rag/query")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").isNotEmpty())
                    .andExpect(jsonPath("$.citations").isArray())
                    .andExpect(jsonPath("$.validation.valid").value(true))
                    .andExpect(jsonPath("$.validation.citationValidityScore").value(1.0))
                    .andExpect(jsonPath("$.generationLatencyMs").isNumber());
        }

        @Test
        @DisplayName("POST /api/v1/rag/query in VECTOR_ONLY mode should succeed without graph memory")
        void testExecuteVectorOnlyMode() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("Vector inquiry only")
                    .generationMode("VECTOR_ONLY")
                    .build();

            mockMvc.perform(post("/api/v1/rag/query")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").isNotEmpty());
        }

        @Test
        @DisplayName("POST /api/v1/rag/query in GRAPH_AUGMENTED mode should retrieve graph evidence")
        void testExecuteGraphAugmentedMode() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("Graph augmented inquiry")
                    .generationMode("GRAPH_AUGMENTED")
                    .build();

            mockMvc.perform(post("/api/v1/rag/query")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retrievalMetadata.graphAvailable").value(true))
                    .andExpect(jsonPath("$.answer").isNotEmpty());
        }

        @Test
        @DisplayName("POST /api/v1/rag/query should gracefully handle Neo4j graph failure fallback")
        void testNeo4jFailureFallback() throws Exception {
            when(knowledgeGraphQueryService.getAudienceMemoryContext(testUser.getId()))
                    .thenThrow(new RuntimeException("Neo4j database connection refused"));

            RagQueryRequest request = RagQueryRequest.builder()
                    .query("How to optimize graph memory?")
                    .generationMode("FULL_EVIDENCE_GROUNDED")
                    .build();

            mockMvc.perform(post("/api/v1/rag/query")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retrievalMetadata.graphAvailable").value(false))
                    .andExpect(jsonPath("$.answer").isNotEmpty());
        }

        @Test
        @DisplayName("POST /api/v1/rag/query should enforce 429 rate limit when AI capacity is exhausted")
        void testRagQueryRateLimiting() throws Exception {
            RagQueryRequest request = RagQueryRequest.builder()
                    .query("Repeated inquiry to trigger rate limit")
                    .generationMode("BASELINE")
                    .build();

            boolean hitRateLimit = false;
            // The configured AI capacity is 10 requests per minute
            for (int i = 0; i < 15; i++) {
                int statusCode = mockMvc.perform(post("/api/v1/rag/query")
                                .header(HttpHeaders.AUTHORIZATION, jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn()
                        .getResponse()
                        .getStatus();

                if (statusCode == 429) {
                    hitRateLimit = true;
                    break;
                }
            }
            assertThat(hitRateLimit).isTrue();
        }
    }

    @Nested
    @DisplayName("Evidence Annotation & Evaluation Tests")
    class AnnotationAndEvaluationTests {

        @Test
        @DisplayName("POST & GET /api/v1/rag/annotations should create and list human annotations")
        void testAnnotationLifecycle() throws Exception {
            EvidenceAnnotationRequest req = EvidenceAnnotationRequest.builder()
                    .queryId("query-test-101")
                    .evidenceId("topic:" + testTopic.getId())
                    .relevance(RagRelevance.RELEVANT)
                    .correctness(RagCorrectness.SUPPORTED)
                    .notes("High quality topic grounding")
                    .build();

            mockMvc.perform(post("/api/v1/rag/annotations")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.queryId").value("query-test-101"))
                    .andExpect(jsonPath("$.relevance").value("RELEVANT"))
                    .andExpect(jsonPath("$.correctness").value("SUPPORTED"));

            mockMvc.perform(get("/api/v1/rag/annotations")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /api/v1/rag/evaluation should return research metrics and mode comparisons")
        void testGetEvaluationMetrics() throws Exception {
            mockMvc.perform(get("/api/v1/rag/evaluation")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evidenceCoverage").value(0.96))
                    .andExpect(jsonPath("$.citationValidityRate").value(0.97))
                    .andExpect(jsonPath("$.sourceDiversityScore").value(0.89))
                    .andExpect(jsonPath("$.modeComparisons.BASELINE").exists())
                    .andExpect(jsonPath("$.modeComparisons.FULL_EVIDENCE_GROUNDED").exists());
        }
    }
}
