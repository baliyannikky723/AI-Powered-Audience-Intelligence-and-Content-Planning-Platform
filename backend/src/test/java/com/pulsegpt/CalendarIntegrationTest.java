package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.audit.AuditLog;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.calendar.CalendarItemStatus;
import com.pulsegpt.calendar.dto.CreateCalendarItemRequest;
import com.pulsegpt.calendar.dto.UpdateCalendarItemRequest;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.RecommendationStatus;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3J — Content Calendar & Planning Management Integration Tests")
class CalendarIntegrationTest {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ContentRecommendationRepository recommendationRepository;

    @MockitoBean
    private TopicRepository topicRepository;

    @MockitoBean
    private CalendarItemRepository calendarItemRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User creatorA;
    private User creatorB;
    private String tokenA;
    private String tokenB;
    private Topic topicA;
    private ContentRecommendation validatedRec;
    private ContentRecommendation approvedRec;
    private ContentRecommendation rejectedRec;
    private ContentRecommendation generatedRec;

    @BeforeEach
    void setUp() {
        creatorA = User.builder()
                .id(UUID.randomUUID())
                .email("creator.a@pulsegpt.dev")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .name("Creator Alpha")
                .role(UserRole.CREATOR)
                .build();

        creatorB = User.builder()
                .id(UUID.randomUUID())
                .email("creator.b@pulsegpt.dev")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .name("Creator Beta")
                .role(UserRole.CREATOR)
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
                .name("Production RAG Optimization")
                .description("Cluster around vector database latency, hybrid search, and token costs")
                .keywords(List.of("rag", "vector", "latency", "tokens"))
                .build();

        when(topicRepository.findByIdAndUserId(topicA.getId(), creatorA.getId())).thenReturn(Optional.of(topicA));

        validatedRec = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .topic(topicA)
                .title("5 Costly Mistakes In Production RAG Applications")
                .description("Deep dive into chunking, hybrid search, and evaluation guardrails")
                .contentType("VIDEO")
                .hook("Why do 80% of RAG architectures fail in production?")
                .status(RecommendationStatus.VALIDATED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .confidence(0.92)
                .validationPassed(true)
                .priority(Priority.HIGH)
                .build();

        approvedRec = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .topic(topicA)
                .title("React 19 Server Actions vs Traditional REST Endpoints")
                .description("Comparison tutorial on caching, security, and optimistic state")
                .contentType("VIDEO")
                .status(RecommendationStatus.APPROVED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .confidence(0.89)
                .validationPassed(true)
                .approvedAt(Instant.now().minus(Duration.ofHours(2)))
                .approvedBy(creatorA.getId())
                .priority(Priority.MEDIUM)
                .build();

        rejectedRec = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .topic(topicA)
                .title("Unsubstantiated Speculative Trends")
                .description("Low confidence halluncinated draft")
                .contentType("VIDEO")
                .status(RecommendationStatus.REJECTED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .confidence(0.40)
                .validationPassed(false)
                .build();

        generatedRec = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .topic(topicA)
                .title("Draft in progress")
                .status(RecommendationStatus.GENERATED)
                .build();

        when(recommendationRepository.findByIdAndUserId(validatedRec.getId(), creatorA.getId())).thenReturn(Optional.of(validatedRec));
        when(recommendationRepository.findByIdAndUserId(approvedRec.getId(), creatorA.getId())).thenReturn(Optional.of(approvedRec));
        when(recommendationRepository.findByIdAndUserId(rejectedRec.getId(), creatorA.getId())).thenReturn(Optional.of(rejectedRec));
        when(recommendationRepository.findByIdAndUserId(generatedRec.getId(), creatorA.getId())).thenReturn(Optional.of(generatedRec));
        when(recommendationRepository.save(any(ContentRecommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        when(calendarItemRepository.save(any(CalendarItem.class))).thenAnswer(inv -> {
            CalendarItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(Instant.now());
                item.setUpdatedAt(Instant.now());
            }
            return item;
        });
    }

    @Nested
    @DisplayName("1. Creator Recommendation Approval Tests")
    class RecommendationApprovalTests {

        @Test
        @DisplayName("Creator can explicitly approve a VALIDATED recommendation")
        void approveValidatedRecommendation_Success() throws Exception {
            mockMvc.perform(post("/recommendations/" + validatedRec.getId() + "/approve")
                            .header("Authorization", tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.approvedAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.approvedBy").value(creatorA.getId().toString()));

            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, atLeastOnce()).save(auditCaptor.capture());
            assertThat(auditCaptor.getAllValues()).anyMatch(a ->
                    "RECOMMENDATION_APPROVED".equals(a.getAction()) &&
                    creatorA.getId().equals(a.getUser().getId()) &&
                    validatedRec.getId().toString().equals(a.getResourceId())
            );
        }

        @Test
        @DisplayName("Approving an already APPROVED recommendation is idempotent")
        void approveAlreadyApprovedRecommendation_Idempotent() throws Exception {
            mockMvc.perform(post("/recommendations/" + approvedRec.getId() + "/approve")
                            .header("Authorization", tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));
        }

        @Test
        @DisplayName("Reject approval of REJECTED recommendation with 400 Bad Request")
        void approveRejectedRecommendation_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/recommendations/" + rejectedRec.getId() + "/approve")
                            .header("Authorization", tokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_RECOMMENDATION_STATUS"));
        }

        @Test
        @DisplayName("Reject approval of GENERATED unvalidated recommendation with 400 Bad Request")
        void approveGeneratedRecommendation_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/recommendations/" + generatedRec.getId() + "/approve")
                            .header("Authorization", tokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_RECOMMENDATION_STATUS"));
        }

        @Test
        @DisplayName("Cross-user recommendation approval returns 404 Not Found")
        void approveCrossUserRecommendation_Returns404() throws Exception {
            mockMvc.perform(post("/recommendations/" + validatedRec.getId() + "/approve")
                            .header("Authorization", tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("2. Scheduling & Past/Timezone Validation Tests")
    class SchedulingValidationTests {

        @Test
        @DisplayName("Create calendar item from APPROVED recommendation succeeds")
        void scheduleApprovedRecommendation_Success() throws Exception {
            Instant startTime = Instant.now().plus(Duration.ofDays(3)).truncatedTo(ChronoUnit.HOURS);
            Instant endTime = startTime.plus(Duration.ofHours(1));

            CreateCalendarItemRequest req = CreateCalendarItemRequest.builder()
                    .recommendationId(approvedRec.getId())
                    .title("React 19 Deep Dive Video")
                    .platform(PlatformType.YOUTUBE)
                    .contentType("VIDEO")
                    .scheduledStart(startTime)
                    .scheduledEnd(endTime)
                    .timezone("Asia/Kolkata")
                    .priority(Priority.HIGH)
                    .notes("Detailed walkthrough with github repository demo")
                    .build();

            mockMvc.perform(post("/calendar")
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("React 19 Deep Dive Video"))
                    .andExpect(jsonPath("$.data.platform").value("YOUTUBE"))
                    .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"))
                    .andExpect(jsonPath("$.data.recommendationId").value(approvedRec.getId().toString()))
                    .andExpect(jsonPath("$.data.topicId").value(topicA.getId().toString()));
        }

        @Test
        @DisplayName("Reject scheduling unapproved (VALIDATED only) recommendation with 400 Bad Request")
        void scheduleUnapprovedRecommendation_ReturnsBadRequest() throws Exception {
            Instant startTime = Instant.now().plus(Duration.ofDays(3));

            CreateCalendarItemRequest req = CreateCalendarItemRequest.builder()
                    .recommendationId(validatedRec.getId()) // Only VALIDATED, not APPROVED
                    .title("Unapproved Draft")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(startTime)
                    .timezone("UTC")
                    .build();

            mockMvc.perform(post("/calendar")
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("UNAPPROVED_RECOMMENDATION"));
        }

        @Test
        @DisplayName("Reject scheduling in the past with 400 Bad Request")
        void schedulePastDate_ReturnsBadRequest() throws Exception {
            Instant pastTime = Instant.now().minus(Duration.ofDays(2));

            CreateCalendarItemRequest req = CreateCalendarItemRequest.builder()
                    .recommendationId(approvedRec.getId())
                    .title("Past Tutorial")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(pastTime)
                    .timezone("UTC")
                    .build();

            mockMvc.perform(post("/calendar")
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("PAST_DATE_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("Reject invalid IANA timezone with 400 Bad Request")
        void scheduleInvalidTimezone_ReturnsBadRequest() throws Exception {
            Instant futureTime = Instant.now().plus(Duration.ofDays(2));

            CreateCalendarItemRequest req = CreateCalendarItemRequest.builder()
                    .recommendationId(approvedRec.getId())
                    .title("Invalid Timezone Post")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(futureTime)
                    .timezone("Invalid/Mars_Timezone")
                    .build();

            mockMvc.perform(post("/calendar")
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_TIMEZONE"));
        }
    }

    @Nested
    @DisplayName("3. Deterministic Conflict Detection Tests")
    class ConflictDetectionTests {

        @Test
        @DisplayName("Overlapping schedule on SAME platform returns 409 Conflict with structured payload")
        void samePlatformOverlap_Returns409Conflict() throws Exception {
            Instant slotStart = Instant.now().plus(Duration.ofDays(4)).truncatedTo(ChronoUnit.HOURS);
            Instant slotEnd = slotStart.plus(Duration.ofHours(1));

            CalendarItem existingItem = CalendarItem.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .title("Existing Scheduled Video")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(slotStart)
                    .scheduledEnd(slotEnd)
                    .status(CalendarItemStatus.SCHEDULED)
                    .timezone("UTC")
                    .build();

            when(calendarItemRepository.findOverlappingItems(
                    eq(creatorA.getId()),
                    eq(PlatformType.YOUTUBE),
                    any(Instant.class),
                    any(Instant.class),
                    anyList(),
                    isNull()
            )).thenReturn(List.of(existingItem));

            Instant newStart = slotStart.plus(Duration.ofMinutes(30));
            Instant newEnd = newStart.plus(Duration.ofHours(1));

            CreateCalendarItemRequest req = CreateCalendarItemRequest.builder()
                    .recommendationId(approvedRec.getId())
                    .title("Conflicting YouTube Video")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(newStart)
                    .scheduledEnd(newEnd)
                    .timezone("UTC")
                    .build();

            mockMvc.perform(post("/calendar")
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.conflict").value(true))
                    .andExpect(jsonPath("$.conflicts[0].calendarItemId").value(existingItem.getId().toString()))
                    .andExpect(jsonPath("$.conflicts[0].title").value("Existing Scheduled Video"))
                    .andExpect(jsonPath("$.conflicts[0].platform").value("YOUTUBE"));
        }

        @Test
        @DisplayName("Scheduling on DIFFERENT platforms at the same time is allowed")
        void differentPlatformSimultaneousSchedule_Allowed() throws Exception {
            Instant slotStart = Instant.now().plus(Duration.ofDays(4)).truncatedTo(ChronoUnit.HOURS);
            Instant slotEnd = slotStart.plus(Duration.ofHours(1));

            // YouTube is occupied, but Instagram is free!
            when(calendarItemRepository.findOverlappingItems(
                    eq(creatorA.getId()),
                    eq(PlatformType.INSTAGRAM),
                    any(Instant.class),
                    any(Instant.class),
                    anyList(),
                    isNull()
            )).thenReturn(Collections.emptyList());

            CreateCalendarItemRequest req = CreateCalendarItemRequest.builder()
                    .recommendationId(approvedRec.getId())
                    .title("Instagram Carousel Announcement")
                    .platform(PlatformType.INSTAGRAM)
                    .contentType("CAROUSEL")
                    .scheduledStart(slotStart)
                    .scheduledEnd(slotEnd)
                    .timezone("UTC")
                    .build();

            mockMvc.perform(post("/calendar")
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.platform").value("INSTAGRAM"));
        }

        @Test
        @DisplayName("Conflict check endpoint returns deterministic conflict status")
        void conflictCheckEndpoint_Success() throws Exception {
            Instant start = Instant.now().plus(Duration.ofDays(2));
            Instant end = start.plus(Duration.ofHours(1));

            when(calendarItemRepository.findOverlappingItems(
                    eq(creatorA.getId()),
                    eq(PlatformType.YOUTUBE),
                    any(Instant.class),
                    any(Instant.class),
                    anyList(),
                    isNull()
            )).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/calendar/conflicts")
                            .header("Authorization", tokenA)
                            .param("platform", "YOUTUBE")
                            .param("start", start.toString())
                            .param("end", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.conflict").value(false));
        }
    }

    @Nested
    @DisplayName("4. Topic Diversity & Recency Warning Tests")
    class TopicDiversityTests {

        @Test
        @DisplayName("Scheduling same topic within 7 days triggers advisory TOPIC_RECENCY_WARNING")
        void sameTopicRecency_ReturnsWarningInResponse() throws Exception {
            Instant startTime = Instant.now().plus(Duration.ofDays(3));

            CalendarItem recentItem = CalendarItem.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .topic(topicA)
                    .title("Recent Post on Same Topic")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(startTime.minus(Duration.ofDays(2)))
                    .scheduledEnd(startTime.minus(Duration.ofDays(2)).plus(Duration.ofHours(1)))
                    .status(CalendarItemStatus.SCHEDULED)
                    .build();

            when(calendarItemRepository.findOverlappingItems(any(), any(), any(), any(), any(), isNull()))
                    .thenReturn(Collections.emptyList());

            when(calendarItemRepository.findRecentItemsByTopic(
                    eq(creatorA.getId()),
                    eq(topicA.getId()),
                    any(Instant.class),
                    any(Instant.class),
                    anyList(),
                    isNull()
            )).thenReturn(List.of(recentItem));

            CreateCalendarItemRequest req = CreateCalendarItemRequest.builder()
                    .recommendationId(approvedRec.getId())
                    .topicId(topicA.getId())
                    .title("New Post on Same Topic")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(startTime)
                    .timezone("UTC")
                    .build();

            mockMvc.perform(post("/calendar")
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.warnings[0].type").value("TOPIC_RECENCY_WARNING"))
                    .andExpect(jsonPath("$.data.warnings[0].severity").value("WARNING"));
        }
    }

    @Nested
    @DisplayName("5. Rescheduling, Cancellation, and Lifecycle Tests")
    class LifecycleAndReschedulingTests {

        @Test
        @DisplayName("PATCH /calendar/{id} reschedules item and re-runs conflict detection")
        void rescheduleCalendarItem_Success() throws Exception {
            CalendarItem item = CalendarItem.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .title("Original Schedule")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(Instant.now().plus(Duration.ofDays(2)))
                    .scheduledEnd(Instant.now().plus(Duration.ofDays(2)).plus(Duration.ofHours(1)))
                    .status(CalendarItemStatus.SCHEDULED)
                    .timezone("UTC")
                    .build();

            when(calendarItemRepository.findByIdAndUserId(item.getId(), creatorA.getId())).thenReturn(Optional.of(item));
            when(calendarItemRepository.findOverlappingItems(any(), any(), any(), any(), any(), eq(item.getId())))
                    .thenReturn(Collections.emptyList());

            Instant newStart = Instant.now().plus(Duration.ofDays(5));
            UpdateCalendarItemRequest updateReq = UpdateCalendarItemRequest.builder()
                    .scheduledStart(newStart)
                    .notes("Rescheduled due to recording studio availability")
                    .build();

            mockMvc.perform(patch("/calendar/" + item.getId())
                            .header("Authorization", tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes").value("Rescheduled due to recording studio availability"));
        }

        @Test
        @DisplayName("POST /calendar/{id}/cancel marks item CANCELLED and preserves provenance")
        void cancelCalendarItem_Success() throws Exception {
            CalendarItem item = CalendarItem.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .title("Video to Cancel")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(Instant.now().plus(Duration.ofDays(2)))
                    .scheduledEnd(Instant.now().plus(Duration.ofDays(2)).plus(Duration.ofHours(1)))
                    .status(CalendarItemStatus.SCHEDULED)
                    .timezone("UTC")
                    .build();

            when(calendarItemRepository.findByIdAndUserId(item.getId(), creatorA.getId())).thenReturn(Optional.of(item));

            mockMvc.perform(post("/calendar/" + item.getId() + "/cancel")
                            .header("Authorization", tokenA)
                            .param("reason", "TOPIC_NO_LONGER_RELEVANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());
        }

        @Test
        @DisplayName("Cross-user calendar access returns 404 Not Found")
        void crossUserCalendarAccess_Returns404() throws Exception {
            CalendarItem item = CalendarItem.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .title("Creator A Private Item")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(Instant.now().plus(Duration.ofDays(2)))
                    .status(CalendarItemStatus.SCHEDULED)
                    .build();

            when(calendarItemRepository.findByIdAndUserId(item.getId(), creatorB.getId())).thenReturn(Optional.empty());

            mockMvc.perform(get("/calendar/" + item.getId())
                            .header("Authorization", tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("6. Queries, Views, and Slot Suggestions Tests")
    class QueriesAndSuggestionsTests {

        @Test
        @DisplayName("GET /calendar returns paginated and user-scoped calendar items")
        @SuppressWarnings("unchecked")
        void getCalendarItems_Success() throws Exception {
            CalendarItem item = CalendarItem.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .title("Scheduled Video")
                    .platform(PlatformType.YOUTUBE)
                    .scheduledStart(Instant.now().plus(Duration.ofDays(1)))
                    .scheduledEnd(Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(1)))
                    .status(CalendarItemStatus.SCHEDULED)
                    .timezone("UTC")
                    .build();

            when(calendarItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(item)));

            mockMvc.perform(get("/calendar")
                            .header("Authorization", tokenA)
                            .param("page", "0")
                            .param("size", "20")
                            .param("view", "MONTH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Scheduled Video"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /calendar/suggestions returns deterministic morning, afternoon, and evening slots")
        void getSuggestions_ReturnsDeterministicSlots() throws Exception {
            when(calendarItemRepository.findOverlappingItems(any(), any(), any(), any(), any(), isNull()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/calendar/suggestions")
                            .header("Authorization", tokenA)
                            .param("date", "2026-10-15")
                            .param("platform", "YOUTUBE")
                            .param("timezone", "Asia/Kolkata"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.slots").isArray())
                    .andExpect(jsonPath("$.data.slots.length()").value(3))
                    .andExpect(jsonPath("$.data.slots[0].slotName").value("Morning"))
                    .andExpect(jsonPath("$.data.slots[0].time").value("09:00"))
                    .andExpect(jsonPath("$.data.slots[1].slotName").value("Afternoon"))
                    .andExpect(jsonPath("$.data.slots[1].time").value("13:00"))
                    .andExpect(jsonPath("$.data.slots[2].slotName").value("Evening"))
                    .andExpect(jsonPath("$.data.slots[2].time").value("18:00"))
                    .andExpect(jsonPath("$.data.slots[0].available").value(true));
        }
    }
}
