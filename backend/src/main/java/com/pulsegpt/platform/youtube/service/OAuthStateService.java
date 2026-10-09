package com.pulsegpt.platform.youtube.service;

import com.pulsegpt.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OAuthStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private final Map<String, OAuthStateRecord> stateStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private record OAuthStateRecord(UUID userId, Instant createdAt) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(STATE_TTL));
        }
    }

    public String generateState(UUID userId) {
        cleanExpiredStates();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        stateStore.put(state, new OAuthStateRecord(userId, Instant.now()));
        return state;
    }

    public UUID validateAndConsumeState(String state) {
        if (state == null || state.isBlank()) {
            throw new ApiException("Missing OAuth state parameter", HttpStatus.BAD_REQUEST, "INVALID_OAUTH_STATE");
        }

        OAuthStateRecord record = stateStore.remove(state); // Single-use consumption
        if (record == null) {
            log.warn("OAuth state not found or already consumed: {}", state);
            throw new ApiException("Invalid or reused OAuth state parameter", HttpStatus.BAD_REQUEST, "INVALID_OAUTH_STATE");
        }

        if (record.isExpired()) {
            log.warn("OAuth state expired for user: {}", record.userId());
            throw new ApiException("OAuth state parameter has expired. Please initiate connection again.", HttpStatus.BAD_REQUEST, "OAUTH_STATE_EXPIRED");
        }

        return record.userId();
    }

    private void cleanExpiredStates() {
        stateStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
