package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.admin.ApiQuotaUsage;
import com.pulsegpt.admin.ApiQuotaUsageRepository;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.PostRepository;
import com.pulsegpt.comment.RawComment;
import com.pulsegpt.comment.RawCommentRepository;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.ingestion.IngestionRun;
import com.pulsegpt.ingestion.IngestionRunRepository;
import com.pulsegpt.ingestion.IngestionRunStatus;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.platform.youtube.client.YouTubeApiClient;
import com.pulsegpt.platform.youtube.client.dto.*;
import com.pulsegpt.platform.youtube.service.OAuthStateService;
import com.pulsegpt.platform.youtube.service.QuotaService;
import com.pulsegpt.platform.youtube.service.TokenEncryptionService;
import com.pulsegpt.platform.youtube.service.YouTubeTokenService;
import com.pulsegpt.security.JwtService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3F — YouTube OAuth, Token Security, Ingestion & Quota Integration Tests")
class YouTubeIntegrationTest {

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

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private TokenEncryptionService tokenEncryptionService;

    @Autowired
    private OAuthStateService oAuthStateService;

    @MockitoBean
    private YouTubeApiClient youTubeApiClient;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PlatformAccountRepository platformAccountRepository;

    @MockitoBean
    private IngestionRunRepository ingestionRunRepository;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private RawCommentRepository rawCommentRepository;

    @MockitoBean
    private ApiQuotaUsageRepository apiQuotaUsageRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User userA;
    private User userB;
    private String tokenUserA;
    private String tokenUserB;

    private PlatformAccount youtubeAccountA;

    @BeforeEach
    void setUp() {
        reset(userRepository, platformAccountRepository, ingestionRunRepository,
                postRepository, rawCommentRepository, apiQuotaUsageRepository,
                auditLogRepository, youTubeApiClient);

        userA = User.builder()
                .id(UUID.randomUUID())
                .name("Creator John")
                .email("john@creator.com")
                .passwordHash(passwordEncoder.encode("Password123!@#"))
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        userB = User.builder()
                .id(UUID.randomUUID())
                .name("Creator Alice")
                .email("alice@creator.com")
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

        youtubeAccountA = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .user(userA)
                .platform(PlatformType.YOUTUBE)
                .externalAccountId("UC_creator_john_channel")
                .accountName("John Tech Hub")
                .accessTokenEncrypted(tokenEncryptionService.encrypt("VALID_ACCESS_TOKEN_123"))
                .refreshTokenEncrypted(tokenEncryptionService.encrypt("VALID_REFRESH_TOKEN_456"))
                .tokenExpiresAt(Instant.now().plusSeconds(3600))
                .status(PlatformAccountStatus.CONNECTED)
                .connectedAt(Instant.now())
                .metadata(Map.of("uploadsPlaylistId", "UU_uploads_playlist_123"))
                .build();
    }

    @Nested
    @DisplayName("1. Token Encryption & Cryptographic Security")
    class TokenEncryptionTests {

        @Test
        @DisplayName("AES-256-GCM encryption and decryption roundtrip successfully")
        void testEncryptionRoundtrip() {
            String secretToken = "ya29.a0AfH6SMA-test-google-oauth-token-987654321";
            String encrypted = tokenEncryptionService.encrypt(secretToken);

            assertThat(encrypted).isNotNull().isNotEqualTo(secretToken);
            String decrypted = tokenEncryptionService.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(secretToken);
        }

        @Test
        @DisplayName("Random IV per operation produces distinct ciphertexts for identical plaintext")
        void testRandomIvUniqueness() {
            String secretToken = "same_oauth_token_value";
            String enc1 = tokenEncryptionService.encrypt(secretToken);
            String enc2 = tokenEncryptionService.encrypt(secretToken);

            assertThat(enc1).isNotEqualTo(enc2);
            assertThat(tokenEncryptionService.decrypt(enc1)).isEqualTo(secretToken);
            assertThat(tokenEncryptionService.decrypt(enc2)).isEqualTo(secretToken);
        }

        @Test
        @DisplayName("Decryption of tampered ciphertext fails securely")
        void testTamperedCiphertextFails() {
            String encrypted = tokenEncryptionService.encrypt("secret_value");
            String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

            assertThatThrownBy(() -> tokenEncryptionService.decrypt(tampered))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("2. OAuth Connect & State Protection")
    class OAuthConnectAndStateTests {

        @Test
        @DisplayName("Unauthenticated request to /connect returns 401 Unauthorized")
        void testUnauthenticatedConnectReturns401() throws Exception {
            mockMvc.perform(get("/integrations/youtube/connect"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Authenticated creator generates valid Google OAuth connect URL with state")
        void testGenerateConnectUrl() throws Exception {
            mockMvc.perform(get("/integrations/youtube/connect")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.authorizationUrl", containsString("accounts.google.com/o/oauth2/v2/auth")))
                    .andExpect(jsonPath("$.data.authorizationUrl", containsString("scope=https://www.googleapis.com/auth/youtube.readonly")))
                    .andExpect(jsonPath("$.data.state").isNotEmpty());
        }

        @Test
        @DisplayName("OAuth state is single-use: second consumption is rejected")
        void testOAuthStateSingleUse() {
            String state = oAuthStateService.generateState(userA.getId());
            UUID validatedUserId = oAuthStateService.validateAndConsumeState(state);
            assertThat(validatedUserId).isEqualTo(userA.getId());

            // Second consumption must fail
            assertThatThrownBy(() -> oAuthStateService.validateAndConsumeState(state))
                    .hasMessageContaining("Invalid or reused OAuth state");
        }

        @Test
        @DisplayName("Callback with invalid state redirects to frontend error URL")
        void testCallbackWithInvalidStateRedirectsToError() throws Exception {
            mockMvc.perform(get("/integrations/youtube/callback")
                            .param("code", "some_google_auth_code")
                            .param("state", "non_existent_or_forged_state"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/app/integrations?status=error*"));
        }
    }

    @Nested
    @DisplayName("3. OAuth Callback & Account Linking")
    class OAuthCallbackTests {

        @Test
        @DisplayName("Successful OAuth code exchange links YouTube channel and stores encrypted tokens")
        void testSuccessfulOAuthCallback() throws Exception {
            String state = oAuthStateService.generateState(userA.getId());

            GoogleTokenResponse tokenResponse = new GoogleTokenResponse(
                    "ya29.google_access_token_123", 3600L, "1//google_refresh_token_456",
                    "https://www.googleapis.com/auth/youtube.readonly", "Bearer", null, null, null
            );
            when(youTubeApiClient.exchangeAuthorizationCode("valid_auth_code")).thenReturn(tokenResponse);

            YouTubeChannelResponse.Snippet snippet = new YouTubeChannelResponse.Snippet(
                    "John Tech Hub", "Tech Channel", "@johntech", "2024-01-01T00:00:00Z", null
            );
            YouTubeChannelResponse.ContentDetails details = new YouTubeChannelResponse.ContentDetails(
                    new YouTubeChannelResponse.ContentDetails.RelatedPlaylists("UU_uploads_123", null)
            );
            YouTubeChannelResponse.Statistics stats = new YouTubeChannelResponse.Statistics("50000", "1200", false, "15");
            YouTubeChannelResponse.ChannelItem item = new YouTubeChannelResponse.ChannelItem("UC_john_channel_123", snippet, details, stats);
            YouTubeChannelResponse channelResponse = new YouTubeChannelResponse("youtube#channelListResponse", "etag", List.of(item));

            when(youTubeApiClient.getMyChannel("ya29.google_access_token_123")).thenReturn(channelResponse);
            when(platformAccountRepository.findAll()).thenReturn(List.of());
            when(platformAccountRepository.findByUserIdAndPlatformAndExternalAccountId(userA.getId(), PlatformType.YOUTUBE, "UC_john_channel_123"))
                    .thenReturn(Optional.empty());
            when(platformAccountRepository.save(any(PlatformAccount.class))).thenAnswer(inv -> {
                PlatformAccount acc = inv.getArgument(0);
                acc.setId(UUID.randomUUID());
                return acc;
            });

            mockMvc.perform(get("/integrations/youtube/callback")
                            .param("code", "valid_auth_code")
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/app/integrations?status=success*"));

            verify(platformAccountRepository).save(any(PlatformAccount.class));
        }

        @Test
        @DisplayName("User denied consent redirects cleanly to frontend error URL")
        void testDeniedConsentRedirectsToError() throws Exception {
            String state = oAuthStateService.generateState(userA.getId());

            mockMvc.perform(get("/integrations/youtube/callback")
                            .param("error", "access_denied")
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/app/integrations?status=error*"));
        }
    }

    @Nested
    @DisplayName("4. Status & Disconnect Endpoints")
    class StatusAndDisconnectTests {

        @Test
        @DisplayName("GET /status returns safe metadata without leaking tokens")
        void testGetIntegrationStatus() throws Exception {
            when(platformAccountRepository.findByUserId(userA.getId()))
                    .thenReturn(List.of(youtubeAccountA));
            when(apiQuotaUsageRepository.findByPlatformAccountIdAndUsageDate(eq(youtubeAccountA.getId()), any(LocalDate.class)))
                    .thenReturn(Optional.of(ApiQuotaUsage.builder().quotaUsed(25L).quotaLimit(10000L).build()));

            mockMvc.perform(get("/integrations/youtube/status")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.connected").value(true))
                    .andExpect(jsonPath("$.data.accountName").value("John Tech Hub"))
                    .andExpect(jsonPath("$.data.externalAccountId").value("UC_creator_john_channel"))
                    .andExpect(jsonPath("$.data.quotaUsedToday").value(25))
                    // Verify NO tokens leaked
                    .andExpect(jsonPath("$.data.accessTokenEncrypted").doesNotExist())
                    .andExpect(jsonPath("$.data.refreshTokenEncrypted").doesNotExist());
        }

        @Test
        @DisplayName("POST /disconnect revokes tokens and sets status to DISCONNECTED")
        void testDisconnect() throws Exception {
            when(platformAccountRepository.findByUserId(userA.getId()))
                    .thenReturn(List.of(youtubeAccountA));
            when(platformAccountRepository.save(any(PlatformAccount.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/integrations/youtube/disconnect")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(youTubeApiClient).revokeToken(anyString());
            verify(platformAccountRepository).save(any(PlatformAccount.class));
        }
    }

    @Nested
    @DisplayName("5. Ingestion Sync & Idempotency")
    class IngestionSyncTests {

        @Test
        @DisplayName("Triggering sync ingests videos and comment threads idempotently")
        void testSuccessfulIngestionSync() throws Exception {
            when(platformAccountRepository.findByUserId(userA.getId()))
                    .thenReturn(List.of(youtubeAccountA));
            when(ingestionRunRepository.existsByPlatformAccountIdAndStatus(youtubeAccountA.getId(), IngestionRunStatus.STARTED))
                    .thenReturn(false);
            when(ingestionRunRepository.save(any(IngestionRun.class))).thenAnswer(inv -> {
                IngestionRun r = inv.getArgument(0);
                if (r.getId() == null) r.setId(UUID.randomUUID());
                return r;
            });

            // Mock playlist videos response
            YouTubeVideoListResponse.Snippet vidSnippet = new YouTubeVideoListResponse.Snippet(
                    Instant.now().toString(), "UC_creator_john_channel", "Mastering Spring AI", "Walkthrough", "John Tech",
                    new YouTubeVideoListResponse.ResourceId("youtube#video", "vid_spring_ai_1")
            );
            YouTubeVideoListResponse.VideoItem playlistItem = new YouTubeVideoListResponse.VideoItem(
                    "item_1", vidSnippet, null, null
            );
            YouTubeVideoListResponse playlistResponse = new YouTubeVideoListResponse(
                    "youtube#playlistItemListResponse", "etag", null, null, null, List.of(playlistItem)
            );
            when(youTubeApiClient.getPlaylistVideos(eq("UU_uploads_playlist_123"), any(), anyInt(), anyString()))
                    .thenReturn(playlistResponse);

            // Mock video detail response (metrics)
            YouTubeVideoListResponse.Statistics vidStats = new YouTubeVideoListResponse.Statistics("15000", "850", "42");
            YouTubeVideoListResponse.VideoItem detailItem = new YouTubeVideoListResponse.VideoItem(
                    "vid_spring_ai_1", vidSnippet, null, vidStats
            );
            YouTubeVideoListResponse detailsResponse = new YouTubeVideoListResponse(
                    "youtube#videoListResponse", "etag", null, null, null, List.of(detailItem)
            );
            when(youTubeApiClient.getVideosByIds(eq(List.of("vid_spring_ai_1")), anyString()))
                    .thenReturn(detailsResponse);

            // Mock comment threads response
            YouTubeCommentThreadListResponse.CommentSnippet commentSnippet = new YouTubeCommentThreadListResponse.CommentSnippet(
                    "DevFan", "url", "channelUrl", new YouTubeCommentThreadListResponse.CommentSnippet.AuthorChannelId("UC_dev"),
                    "Great explanation of vector search!", "Great explanation of vector search!", null, 5L,
                    Instant.now().toString(), Instant.now().toString()
            );
            YouTubeCommentThreadListResponse.TopLevelComment topLevel = new YouTubeCommentThreadListResponse.TopLevelComment("comm_1", commentSnippet);
            YouTubeCommentThreadListResponse.Snippet threadSnippet = new YouTubeCommentThreadListResponse.Snippet("vid_spring_ai_1", topLevel, true, 0L, true);
            YouTubeCommentThreadListResponse.CommentThreadItem threadItem = new YouTubeCommentThreadListResponse.CommentThreadItem("thread_1", threadSnippet, null);
            YouTubeCommentThreadListResponse commentResponse = new YouTubeCommentThreadListResponse(
                    "youtube#commentThreadListResponse", "etag", null, null, List.of(threadItem)
            );
            when(youTubeApiClient.getCommentThreads(eq("vid_spring_ai_1"), any(), anyInt(), anyString()))
                    .thenReturn(commentResponse);

            when(postRepository.findByPlatformAccountIdAndExternalPostId(youtubeAccountA.getId(), "vid_spring_ai_1"))
                    .thenReturn(Optional.empty());
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            when(rawCommentRepository.findByPostIdAndExternalCommentId(any(), eq("comm_1")))
                    .thenReturn(Optional.empty());
            when(rawCommentRepository.save(any(RawComment.class))).thenAnswer(inv -> {
                RawComment c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            mockMvc.perform(post("/integrations/youtube/sync")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.postsProcessed").value(1))
                    .andExpect(jsonPath("$.data.commentsProcessed").value(1))
                    .andExpect(jsonPath("$.data.itemsFailed").value(0));

            verify(postRepository).save(any(Post.class));
            verify(rawCommentRepository).save(any(RawComment.class));
        }

        @Test
        @DisplayName("Concurrent sync request on same account returns 409 INGESTION_ALREADY_RUNNING")
        void testConcurrentSyncRejectedWith409() throws Exception {
            when(platformAccountRepository.findByUserId(userA.getId()))
                    .thenReturn(List.of(youtubeAccountA));
            when(ingestionRunRepository.existsByPlatformAccountIdAndStatus(youtubeAccountA.getId(), IngestionRunStatus.STARTED))
                    .thenReturn(true);

            mockMvc.perform(post("/integrations/youtube/sync")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INGESTION_ALREADY_RUNNING"));
        }
    }
}
