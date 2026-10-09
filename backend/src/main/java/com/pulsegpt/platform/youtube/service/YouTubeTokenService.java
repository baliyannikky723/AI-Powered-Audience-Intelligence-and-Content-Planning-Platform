package com.pulsegpt.platform.youtube.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.youtube.client.YouTubeApiClient;
import com.pulsegpt.platform.youtube.client.dto.GoogleTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeTokenService {

    private static final Duration EXPIRY_BUFFER = Duration.ofMinutes(5);

    private final YouTubeApiClient youTubeApiClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final PlatformAccountRepository platformAccountRepository;
    private final AuditService auditService;

    @Transactional
    public String getValidAccessToken(PlatformAccount account) {
        if (account.getStatus() != PlatformAccountStatus.CONNECTED && account.getStatus() != PlatformAccountStatus.SYNCING) {
            throw new ApiException("YouTube account is not connected. Status: " + account.getStatus(),
                    HttpStatus.UNAUTHORIZED, "YOUTUBE_REAUTH_REQUIRED");
        }

        Instant expiry = account.getTokenExpiresAt();
        boolean isExpiredOrNearExpiry = expiry == null || Instant.now().plus(EXPIRY_BUFFER).isAfter(expiry);

        if (!isExpiredOrNearExpiry) {
            return tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        }

        // Token expired, refresh it
        log.info("Refreshing expired YouTube access token for account: {}", account.getId());
        String refreshToken = tokenEncryptionService.decrypt(account.getRefreshTokenEncrypted());
        if (refreshToken == null || refreshToken.isBlank()) {
            account.setStatus(PlatformAccountStatus.ERROR);
            platformAccountRepository.save(account);
            auditService.logSecurityEvent(account.getUser(), "YOUTUBE_TOKEN_REFRESH_FAILED", "PLATFORM_ACCOUNT",
                    account.getId().toString(), "Missing refresh token");
            throw new ApiException("Missing refresh token for YouTube account. Re-authentication required.",
                    HttpStatus.UNAUTHORIZED, "YOUTUBE_REAUTH_REQUIRED");
        }

        try {
            GoogleTokenResponse response = youTubeApiClient.refreshAccessToken(refreshToken);
            String newAccessToken = response.accessToken();
            long expiresIn = response.expiresIn() != null ? response.expiresIn() : 3600L;

            account.setAccessTokenEncrypted(tokenEncryptionService.encrypt(newAccessToken));
            account.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));

            if (response.refreshToken() != null && !response.refreshToken().isBlank()) {
                account.setRefreshTokenEncrypted(tokenEncryptionService.encrypt(response.refreshToken()));
            }

            platformAccountRepository.save(account);
            auditService.logAuthEvent(account.getUser(), "YOUTUBE_TOKEN_REFRESHED", account.getId().toString(),
                    Map.of("expiresIn", expiresIn));

            return newAccessToken;
        } catch (Exception e) {
            log.warn("Failed to refresh YouTube access token for account {}: {}", account.getId(), e.getMessage());
            account.setStatus(PlatformAccountStatus.ERROR);
            platformAccountRepository.save(account);
            auditService.logSecurityEvent(account.getUser(), "YOUTUBE_TOKEN_REFRESH_FAILED", "PLATFORM_ACCOUNT",
                    account.getId().toString(), e.getMessage());
            throw new ApiException("Failed to refresh YouTube credentials. Please reconnect your account.",
                    HttpStatus.UNAUTHORIZED, "YOUTUBE_REAUTH_REQUIRED");
        }
    }
}
