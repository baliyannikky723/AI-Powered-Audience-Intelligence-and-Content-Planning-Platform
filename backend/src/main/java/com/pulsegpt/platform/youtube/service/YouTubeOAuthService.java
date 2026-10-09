package com.pulsegpt.platform.youtube.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.platform.youtube.client.YouTubeApiClient;
import com.pulsegpt.platform.youtube.client.dto.GoogleTokenResponse;
import com.pulsegpt.platform.youtube.client.dto.YouTubeChannelResponse;
import com.pulsegpt.platform.youtube.dto.YouTubeConnectResponse;
import com.pulsegpt.platform.youtube.dto.YouTubeStatusResponse;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube.readonly openid email profile";

    private final AppProperties appProperties;
    private final OAuthStateService oAuthStateService;
    private final YouTubeApiClient youTubeApiClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final PlatformAccountRepository platformAccountRepository;
    private final UserRepository userRepository;
    private final QuotaService quotaService;
    private final AuditService auditService;

    public YouTubeConnectResponse generateConnectUrl(User currentUser) {
        String state = oAuthStateService.generateState(currentUser.getId());

        String authorizationUrl = UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id", appProperties.getYoutube().getClientId())
                .queryParam("redirect_uri", appProperties.getYoutube().getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", YOUTUBE_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();

        auditService.logAuthEvent(currentUser, "YOUTUBE_CONNECT_STARTED", currentUser.getId().toString(),
                Map.of("platform", PlatformType.YOUTUBE.name()));

        return new YouTubeConnectResponse(authorizationUrl, state);
    }

    @Transactional
    public String handleCallback(String code, String state) {
        UUID userId;
        try {
            userId = oAuthStateService.validateAndConsumeState(state);
        } catch (ApiException ex) {
            log.warn("OAuth state validation failed: {}", ex.getMessage());
            return UriComponentsBuilder.fromUriString(appProperties.getYoutube().getFrontendRedirectErrorUrl())
                    .queryParam("error", ex.getErrorCode())
                    .build().toUriString();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        try {
            // 1. Exchange authorization code for tokens
            GoogleTokenResponse tokenResponse = youTubeApiClient.exchangeAuthorizationCode(code);
            String accessToken = tokenResponse.accessToken();
            String refreshToken = tokenResponse.refreshToken();
            long expiresIn = tokenResponse.expiresIn() != null ? tokenResponse.expiresIn() : 3600L;

            // 2. Retrieve YouTube channel information
            YouTubeChannelResponse channelResponse = youTubeApiClient.getMyChannel(accessToken);
            if (channelResponse.items() == null || channelResponse.items().isEmpty()) {
                throw new ApiException("No YouTube channel found for the authenticated Google account",
                        HttpStatus.BAD_REQUEST, "YOUTUBE_CHANNEL_NOT_FOUND");
            }

            YouTubeChannelResponse.ChannelItem channel = channelResponse.items().get(0);
            String externalChannelId = channel.id();
            String channelTitle = channel.snippet() != null ? channel.snippet().title() : "YouTube Channel";

            // 3. Prevent cross-user account hijacking (check if channel connected to another user)
            Optional<PlatformAccount> existingOther = platformAccountRepository.findAll().stream()
                    .filter(a -> a.getPlatform() == PlatformType.YOUTUBE
                            && externalChannelId.equals(a.getExternalAccountId())
                            && !a.getUser().getId().equals(userId)
                            && a.getStatus() == PlatformAccountStatus.CONNECTED)
                    .findFirst();

            if (existingOther.isPresent()) {
                log.warn("Conflict: Channel {} is already connected by another user {}", externalChannelId, existingOther.get().getUser().getId());
                throw new ApiException("This YouTube channel is already connected to another user",
                        HttpStatus.CONFLICT, "YOUTUBE_ACCOUNT_CONFLICT");
            }

            // 4. Find or create PlatformAccount for this user
            PlatformAccount account = platformAccountRepository
                    .findByUserIdAndPlatformAndExternalAccountId(userId, PlatformType.YOUTUBE, externalChannelId)
                    .orElseGet(() -> PlatformAccount.builder()
                            .user(user)
                            .platform(PlatformType.YOUTUBE)
                            .externalAccountId(externalChannelId)
                            .accountName(channelTitle)
                            .build());

            account.setAccountName(channelTitle);
            account.setAccessTokenEncrypted(tokenEncryptionService.encrypt(accessToken));
            if (refreshToken != null && !refreshToken.isBlank()) {
                account.setRefreshTokenEncrypted(tokenEncryptionService.encrypt(refreshToken));
            }
            account.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            account.setStatus(PlatformAccountStatus.CONNECTED);
            account.setConnectedAt(Instant.now());
            account.setDisconnectedAt(null);

            Map<String, Object> metadata = new HashMap<>();
            if (channel.snippet() != null) {
                metadata.put("customUrl", channel.snippet().customUrl());
                if (channel.snippet().thumbnails() != null && channel.snippet().thumbnails().defaultThumbnail() != null) {
                    metadata.put("thumbnailUrl", channel.snippet().thumbnails().defaultThumbnail().url());
                }
            }
            if (channel.statistics() != null) {
                metadata.put("subscriberCount", channel.statistics().subscriberCount());
                metadata.put("viewCount", channel.statistics().viewCount());
                metadata.put("videoCount", channel.statistics().videoCount());
            }
            if (channel.contentDetails() != null && channel.contentDetails().relatedPlaylists() != null) {
                metadata.put("uploadsPlaylistId", channel.contentDetails().relatedPlaylists().uploads());
            }
            account.setMetadata(metadata);

            platformAccountRepository.save(account);
            quotaService.recordQuotaUsage(account, "channels.list", QuotaService.COST_CHANNEL_LIST);

            auditService.logAuthEvent(user, "YOUTUBE_CONNECTED", account.getId().toString(),
                    Map.of("channelId", externalChannelId, "channelTitle", channelTitle));

            return UriComponentsBuilder.fromUriString(appProperties.getYoutube().getFrontendRedirectSuccessUrl())
                    .queryParam("accountId", account.getId().toString())
                    .build().toUriString();

        } catch (ApiException e) {
            log.warn("YouTube connection error: {}", e.getMessage());
            auditService.logSecurityEvent(user, "YOUTUBE_CONNECTION_FAILED", "PLATFORM_ACCOUNT", null, e.getMessage());
            return UriComponentsBuilder.fromUriString(appProperties.getYoutube().getFrontendRedirectErrorUrl())
                    .queryParam("error", e.getErrorCode())
                    .build().toUriString();
        } catch (Exception e) {
            log.error("Unexpected error handling YouTube OAuth callback: {}", e.getMessage(), e);
            auditService.logSecurityEvent(user, "YOUTUBE_CONNECTION_FAILED", "PLATFORM_ACCOUNT", null, e.getMessage());
            return UriComponentsBuilder.fromUriString(appProperties.getYoutube().getFrontendRedirectErrorUrl())
                    .queryParam("error", "INTERNAL_ERROR")
                    .build().toUriString();
        }
    }

    @Transactional
    public void disconnect(User currentUser) {
        PlatformAccount account = platformAccountRepository.findByUserId(currentUser.getId()).stream()
                .filter(a -> a.getPlatform() == PlatformType.YOUTUBE && a.getStatus() == PlatformAccountStatus.CONNECTED)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No connected YouTube account found for current user"));

        // Attempt external Google token revocation
        try {
            String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
            if (accessToken != null) {
                youTubeApiClient.revokeToken(accessToken);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke token during YouTube disconnect for account {}: {}", account.getId(), e.getMessage());
        }

        account.setAccessTokenEncrypted(null);
        account.setRefreshTokenEncrypted(null);
        account.setStatus(PlatformAccountStatus.DISCONNECTED);
        account.setDisconnectedAt(Instant.now());
        platformAccountRepository.save(account);

        auditService.logAuthEvent(currentUser, "YOUTUBE_DISCONNECTED", account.getId().toString(),
                Map.of("platform", PlatformType.YOUTUBE.name()));
    }

    @Transactional(readOnly = true)
    public YouTubeStatusResponse getStatus(User currentUser) {
        Optional<PlatformAccount> accountOpt = platformAccountRepository.findByUserId(currentUser.getId()).stream()
                .filter(a -> a.getPlatform() == PlatformType.YOUTUBE)
                .findFirst();

        if (accountOpt.isEmpty()) {
            return new YouTubeStatusResponse(false, null, null, null, null, null, null, 0L, appProperties.getYoutube().getQuotaLimit());
        }

        PlatformAccount account = accountOpt.get();
        boolean isConnected = account.getStatus() == PlatformAccountStatus.CONNECTED;
        long quotaUsedToday = quotaService.getQuotaUsedToday(account);

        return new YouTubeStatusResponse(
                isConnected,
                account.getId(),
                account.getAccountName(),
                account.getExternalAccountId(),
                account.getStatus(),
                account.getConnectedAt(),
                account.getLastSyncedAt(),
                quotaUsedToday,
                appProperties.getYoutube().getQuotaLimit()
        );
    }
}
