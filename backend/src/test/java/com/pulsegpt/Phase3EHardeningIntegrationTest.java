package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.comment.*;
import com.pulsegpt.platform.*;
import com.pulsegpt.platform.dto.UpdatePlatformAccountStatusRequest;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3E — Hardening, Security, Error Handling & API Quality Tests")
class Phase3EHardeningIntegrationTest {

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
    private PlatformAccountRepository platformAccountRepository;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private RawCommentRepository rawCommentRepository;

    @MockitoBean
    private ProcessedCommentRepository processedCommentRepository;

    @MockitoBean
    private TopicRepository topicRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User userA;
    private User userB;
    private String tokenUserA;
    private String tokenUserB;

    private PlatformAccount accountA;
    private Post postA;
    private RawComment rawCommentA;
    private ProcessedComment processedCommentA;
    private Topic topicA;

    @BeforeEach
    void setUp() {
        userA = User.builder()
                .id(UUID.randomUUID())
                .name("Creator User A")
                .email("hardened_a@pulsegpt.ai")
                .passwordHash(passwordEncoder.encode("Password123!@#"))
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        userB = User.builder()
                .id(UUID.randomUUID())
                .name("Creator User B")
                .email("hardened_b@pulsegpt.ai")
                .passwordHash(passwordEncoder.encode("Password123!@#"))
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        when(userRepository.findById(userA.getId())).thenReturn(Optional.of(userA));
        when(userRepository.findById(userB.getId())).thenReturn(Optional.of(userB));
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(userRepository.findByEmail(userB.getEmail())).thenReturn(Optional.of(userB));

        tokenUserA = jwtService.generateAccessToken(userA);
        tokenUserB = jwtService.generateAccessToken(userB);

        accountA = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .user(userA)
                .platform(PlatformType.YOUTUBE)
                .externalAccountId("yt_hardening_123")
                .accountName("Hardening Channel")
                .accessTokenEncrypted("SECRET_ACCESS_TOKEN_12345")
                .refreshTokenEncrypted("SECRET_REFRESH_TOKEN_67890")
                .status(PlatformAccountStatus.CONNECTED)
                .connectedAt(Instant.now())
                .build();

        postA = Post.builder()
                .id(UUID.randomUUID())
                .platformAccount(accountA)
                .externalPostId("post_hardening_456")
                .title("Hardening Security and Clean Architecture")
                .url("https://youtube.com/watch?v=post_hardening_456")
                .viewsCount(12000L)
                .likesCount(800L)
                .commentsCount(150L)
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        rawCommentA = RawComment.builder()
                .id(UUID.randomUUID())
                .post(postA)
                .externalCommentId("comm_hardening_789")
                .authorDisplayName("SecureCoder")
                .authorExternalId("UC_securecoder")
                .rawText("Top notch security architecture!")
                .likes(50)
                .replies(4)
                .publishedAt(Instant.now())
                .importedAt(Instant.now())
                .build();

        processedCommentA = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(rawCommentA)
                .normalizedText("Top notch security architecture!")
                .language("en")
                .sentimentLabel(SentimentLabel.POSITIVE)
                .sentimentScore(0.98)
                .intent(IntentType.PRAISE)
                .isSpam(false)
                .isDuplicate(false)
                .piiMasked(false)
                .processingVersion("v1.0.0")
                .processedAt(Instant.now())
                .build();

        topicA = Topic.builder()
                .id(UUID.randomUUID())
                .user(userA)
                .name("Security & Tenant Isolation")
                .description("Discussions regarding robust multi-tenant authorization")
                .keywords(List.of("security", "isolation", "idor"))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("1. HTTP Security Headers & Infrastructure")
    class SecurityHeadersTests {

        @Test
        @DisplayName("Responses must include standard security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy)")
        void testHttpSecurityHeaders() throws Exception {
            mockMvc.perform(get("/platform-accounts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andExpect(header().exists("Content-Security-Policy"));
        }
    }

    @Nested
    @DisplayName("2. Error Handling & Validation Hardening")
    class ErrorHandlingHardeningTests {

        @Test
        @DisplayName("Malformed JSON payload returns 400 MALFORMED_REQUEST")
        void testMalformedJsonReturns400() throws Exception {
            mockMvc.perform(patch("/platform-accounts/{id}/status", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": INVALID_JSON_UNQUOTED"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                    .andExpect(jsonPath("$.correlationId").exists());
        }

        @Test
        @DisplayName("Invalid UUID path variable returns 400 INVALID_PARAMETER")
        void testInvalidUuidPathVariableReturns400() throws Exception {
            mockMvc.perform(get("/posts/{id}", "not-a-valid-uuid-12345")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                    .andExpect(jsonPath("$.correlationId").exists());
        }

        @Test
        @DisplayName("Unsupported HTTP method returns 405 METHOD_NOT_ALLOWED")
        void testUnsupportedHttpMethodReturns405() throws Exception {
            mockMvc.perform(post("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("Search query exceeding 200 characters is rejected with 400 VALIDATION_ERROR")
        void testOversizedSearchQueryRejected() throws Exception {
            String oversizedSearch = "a".repeat(205);
            mockMvc.perform(get("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("search", oversizedSearch))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Invalid enum filter value returns 400 INVALID_PARAMETER or MALFORMED_REQUEST")
        void testInvalidEnumFilterValueRejected() throws Exception {
            mockMvc.perform(get("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("platform", "NON_EXISTENT_PLATFORM"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("3. IDOR (Insecure Direct Object Reference) Protection")
    class IdorProtectionTests {

        @Test
        @DisplayName("User B cannot access User A's platform account (returns 404 NOT FOUND)")
        void testUserBCannotAccessUserAPlatformAccount() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/platform-accounts/{id}", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("User B cannot update User A's platform account status (returns 404 NOT FOUND)")
        void testUserBCannotUpdateUserAPlatformAccountStatus() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            com.pulsegpt.platform.dto.UpdatePlatformAccountStatusRequest req =
                    new com.pulsegpt.platform.dto.UpdatePlatformAccountStatusRequest(PlatformAccountStatus.DISCONNECTED);

            mockMvc.perform(patch("/platform-accounts/{id}/status", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("User B cannot delete/disconnect User A's platform account (returns 404 NOT FOUND)")
        void testUserBCannotDisconnectUserAPlatformAccount() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(delete("/platform-accounts/{id}", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("User B cannot access User A's post by ID (returns 404 NOT FOUND)")
        void testUserBCannotAccessUserAPost() throws Exception {
            when(postRepository.findByIdAndPlatformAccountUserId(postA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/posts/{id}", postA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("User B cannot access User A's raw comment by ID (returns 404 NOT FOUND)")
        void testUserBCannotAccessUserARawComment() throws Exception {
            when(rawCommentRepository.findByIdAndPostPlatformAccountUserId(rawCommentA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/comments/{id}", rawCommentA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("User B cannot access User A's processed comment by ID (returns 404 NOT FOUND)")
        void testUserBCannotAccessUserAProcessedComment() throws Exception {
            when(processedCommentRepository.findByIdAndRawCommentPostPlatformAccountUserId(processedCommentA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/comments/processed/{id}", processedCommentA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("User B cannot access User A's topic by ID (returns 404 NOT FOUND)")
        void testUserBCannotAccessUserATopic() throws Exception {
            when(topicRepository.findByIdAndUserId(topicA.getId(), userB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/topics/{id}", topicA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("4. Privacy & Secret Leakage Prevention")
    class PrivacyAndSecretLeakageTests {

        @Test
        @DisplayName("Platform Account response NEVER leaks encrypted access/refresh tokens or internal secrets")
        void testNoSecretTokensLeakedInPlatformAccount() throws Exception {
            when(platformAccountRepository.findByIdAndUserId(accountA.getId(), userA.getId()))
                    .thenReturn(Optional.of(accountA));

            mockMvc.perform(get("/platform-accounts/{id}", accountA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessTokenEncrypted").doesNotExist())
                    .andExpect(jsonPath("$.data.refreshTokenEncrypted").doesNotExist())
                    .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                    .andExpect(content().string(not(containsString("SECRET_ACCESS_TOKEN"))))
                    .andExpect(content().string(not(containsString("SECRET_REFRESH_TOKEN"))));
        }

        @Test
        @DisplayName("Error responses NEVER leak Java stack traces or database schema fragments")
        void testErrorResponseContainsNoStackTrace() throws Exception {
            mockMvc.perform(get("/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                            .param("sort", "illegal_field"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.stackTrace").doesNotExist())
                    .andExpect(content().string(not(containsString("java.lang"))))
                    .andExpect(content().string(not(containsString("org.springframework"))));
        }
    }
}
