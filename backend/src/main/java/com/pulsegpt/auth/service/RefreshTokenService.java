package com.pulsegpt.auth.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.auth.RefreshToken;
import com.pulsegpt.auth.RefreshTokenRepository;
import com.pulsegpt.common.exception.UnauthorizedException;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public record TokenRotationResult(String newRawRefreshToken, User user) {}

    @Transactional
    public String createRefreshToken(User user, String userAgent, String ipAddress) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now().plusMillis(appProperties.getJwt().getRefreshExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public TokenRotationResult rotateRefreshToken(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(rawRefreshToken.trim());
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = oldToken.getUser();

        // Check for Token Reuse Attack (token was already revoked)
        if (oldToken.isRevoked()) {
            log.warn("Security Alert: Reused revoked refresh token detected for user: {}. Revoking all sessions.", user.getId());
            refreshTokenRepository.revokeAllUserTokens(user.getId(), Instant.now());
            auditService.logSecurityEvent(user, "TOKEN_REUSE_DETECTED", "REFRESH_TOKEN", oldToken.getId().toString(), "Reused revoked refresh token");
            throw new UnauthorizedException("Security violation: Token reuse detected. Please log in again.");
        }

        // Check for expiration
        if (oldToken.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        // Check user active status
        if (!user.isActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        // Revoke the old token
        oldToken.setRevokedAt(Instant.now());

        // Issue new refresh token
        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);
        Instant expiresAt = Instant.now().plusMillis(appProperties.getJwt().getRefreshExpirationMs());

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(expiresAt)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        RefreshToken savedNewToken = refreshTokenRepository.save(newToken);
        oldToken.setReplacedByTokenId(savedNewToken.getId());
        refreshTokenRepository.save(oldToken);

        return new TokenRotationResult(newRawToken, user);
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken.trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
