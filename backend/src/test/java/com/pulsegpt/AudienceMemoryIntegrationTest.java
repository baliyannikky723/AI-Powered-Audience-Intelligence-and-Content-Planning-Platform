package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.comment.RawCommentRepository;
import com.pulsegpt.graph.repository.Neo4jKnowledgeGraphRepository;
import com.pulsegpt.memory.AudienceInterest;
import com.pulsegpt.memory.AudienceInterestRepository;
import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.memory.dto.AudienceInterestResponse;
import com.pulsegpt.memory.dto.AudienceQuestionResponse;
import com.pulsegpt.memory.dto.RelatedTopicResponse;
import com.pulsegpt.memory.dto.TopicContentIdeaResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3N Audience Memory & Knowledge Graph Integration Tests")
class AudienceMemoryIntegrationTest {

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
    private AudienceInterestRepository interestRepository;

    @MockitoBean
    private ContentRecommendationRepository recommendationRepository;

    @MockitoBean
    private CalendarItemRepository calendarItemRepository;

    @MockitoBean
    private RawCommentRepository rawCommentRepository;

    @MockitoBean
    private ProcessedCommentRepository processedCommentRepository;

    @MockitoBean
    private Neo4jKnowledgeGraphRepository graphRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User testUser;
    private String jwtToken;
    private UUID userId;
    private UUID topicId;
    private Topic sampleTopic;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        topicId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .name("Creator Test")
                .email("creator@pulsegpt.test")
                .passwordHash("hashed")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        sampleTopic = Topic.builder()
                .id(topicId)
                .user(testUser)
                .name("Spring Boot Cloud Architecture")
                .commentCount(120)
                .active(true)
                .build();

        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        jwtToken = "Bearer " + jwtService.generateAccessToken(testUser);
    }

    @Nested
    @DisplayName("1. Summary Endpoint Tests")
    class SummaryEndpointTests {

        @Test
        @DisplayName("GET /api/v1/memory/summary returns active, weakening, questions, and counts")
        void testGetSummary() throws Exception {
            when(graphRepository.findInterestsByStatus(anyString(), any())).thenReturn(List.of(
                    AudienceInterestResponse.builder()
                            .id(topicId)
                            .topicId(topicId)
                            .topicName("Spring Boot Cloud Architecture")
                            .confidence(0.85)
                            .evidenceCount(120)
                            .status(AudienceInterestStatus.ACTIVE)
                            .lastSeenAt(Instant.now())
                            .build()
            ));

            when(graphRepository.findRecurringQuestions(anyString(), anyInt())).thenReturn(List.of(
                    AudienceQuestionResponse.builder()
                            .id("q1")
                            .questionHash("hash123")
                            .normalizedText("How to deploy Spring Boot to Kubernetes?")
                            .confidence(0.92)
                            .evidenceCount(35)
                            .topicId(topicId)
                            .topicName("Spring Boot Cloud Architecture")
                            .build()
            ));

            when(graphRepository.findRelatedTopics(anyString(), anyString(), anyInt())).thenReturn(List.of());
            when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

            mockMvc.perform(get("/memory/summary")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeInterests").isArray())
                    .andExpect(jsonPath("$.activeInterests[0].topicName").value("Spring Boot Cloud Architecture"))
                    .andExpect(jsonPath("$.activeInterests[0].confidence").value(0.85))
                    .andExpect(jsonPath("$.recurringQuestions").isArray())
                    .andExpect(jsonPath("$.recurringQuestions[0].normalizedText").value("How to deploy Spring Boot to Kubernetes?"));
        }

        @Test
        @DisplayName("Unauthenticated request to /memory/summary returns 401 Unauthorized")
        void testUnauthenticatedSummary() throws Exception {
            mockMvc.perform(get("/memory/summary"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("2. Interests Endpoint Tests")
    class InterestsEndpointTests {

        @Test
        @DisplayName("GET /api/v1/memory/interests returns active interests")
        void testGetInterests() throws Exception {
            when(graphRepository.findInterestsByStatus(anyString(), any())).thenReturn(List.of(
                    AudienceInterestResponse.builder()
                            .id(topicId)
                            .topicId(topicId)
                            .topicName("Spring Boot Cloud Architecture")
                            .confidence(0.88)
                            .evidenceCount(120)
                            .status(AudienceInterestStatus.ACTIVE)
                            .build()
            ));

            mockMvc.perform(get("/memory/interests")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].topicName").value("Spring Boot Cloud Architecture"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("GET /api/v1/memory/interests/{topicId} returns interest detail")
        void testGetInterestByTopic() throws Exception {
            when(topicRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(sampleTopic));
            when(interestRepository.findByUserIdAndTopicId(any(), any())).thenReturn(Optional.of(
                    AudienceInterest.builder()
                            .id(UUID.randomUUID())
                            .user(testUser)
                            .topic(sampleTopic)
                            .confidence(0.85)
                            .evidenceCount(120)
                            .status(AudienceInterestStatus.ACTIVE)
                            .halfLifeDays(45)
                            .lastSeenAt(Instant.now())
                            .build()
            ));

            mockMvc.perform(get("/memory/interests/" + topicId)
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.topicId").value(topicId.toString()))
                    .andExpect(jsonPath("$.topicName").value("Spring Boot Cloud Architecture"))
                    .andExpect(jsonPath("$.confidence").value(0.85))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("3. Questions & Related Topics Tests")
    class QuestionsAndRelatedTests {

        @Test
        @DisplayName("GET /api/v1/memory/questions returns recurring questions")
        void testGetQuestions() throws Exception {
            when(graphRepository.findRecurringQuestions(anyString(), anyInt())).thenReturn(List.of(
                    AudienceQuestionResponse.builder()
                            .id("q1")
                            .questionHash("hash123")
                            .normalizedText("What is the best way to handle graph queries?")
                            .confidence(0.95)
                            .evidenceCount(40)
                            .build()
            ));

            mockMvc.perform(get("/memory/questions")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].questionHash").value("hash123"))
                    .andExpect(jsonPath("$[0].confidence").value(0.95));
        }

        @Test
        @DisplayName("GET /api/v1/memory/topics/{topicId}/related returns related topics")
        void testGetRelatedTopics() throws Exception {
            when(topicRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(sampleTopic));
            UUID targetTopicId = UUID.randomUUID();
            when(graphRepository.findRelatedTopics(anyString(), anyString(), anyInt())).thenReturn(List.of(
                    RelatedTopicResponse.builder()
                            .sourceTopicId(topicId)
                            .targetTopicId(targetTopicId)
                            .targetTopicName("Microservice Observability")
                            .weight(0.75)
                            .cooccurrenceCount(18)
                            .build()
            ));

            mockMvc.perform(get("/memory/topics/" + topicId + "/related")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].targetTopicName").value("Microservice Observability"))
                    .andExpect(jsonPath("$[0].weight").value(0.75));
        }

        @Test
        @DisplayName("GET /api/v1/memory/topics/{topicId}/content-ideas returns associated ideas")
        void testGetTopicContentIdeas() throws Exception {
            when(topicRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(sampleTopic));
            when(graphRepository.findTopicContentIdeas(anyString(), anyString())).thenReturn(List.of(
                    TopicContentIdeaResponse.builder()
                            .id(UUID.randomUUID())
                            .topicId(topicId)
                            .title("Building Resilient Microservices with Spring Boot 3")
                            .angle("VIDEO")
                            .status("DRAFT")
                            .build()
            ));

            mockMvc.perform(get("/memory/topics/" + topicId + "/content-ideas")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].title").value("Building Resilient Microservices with Spring Boot 3"));
        }
    }

    @Nested
    @DisplayName("4. Graph Rebuild Tests")
    class GraphRebuildTests {

        @Test
        @DisplayName("POST /api/v1/memory/rebuild clears and reconstructs user graph")
        void testRebuildEndpoint() throws Exception {
            when(topicRepository.findByUserId(any())).thenReturn(List.of(sampleTopic));
            when(interestRepository.findByUserIdAndTopicId(any(), any())).thenReturn(Optional.empty());

            mockMvc.perform(post("/memory/rebuild")
                            .header(HttpHeaders.AUTHORIZATION, jwtToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.topicCount").value(1));
        }
    }
}
