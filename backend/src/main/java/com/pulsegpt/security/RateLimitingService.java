package com.pulsegpt.security;

import com.pulsegpt.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final AppProperties appProperties;

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<UUID, Bucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<UUID, Bucket> aiBuckets = new ConcurrentHashMap<>();

    private final Map<UUID, Bucket> syncBuckets = new ConcurrentHashMap<>();

    public boolean tryConsumeLogin(String ipAddress, String email) {
        String key = (ipAddress != null ? ipAddress : "unknown") + ":" + (email != null ? email.trim().toLowerCase() : "unknown");
        Bucket bucket = loginBuckets.computeIfAbsent(key, k -> createLoginBucket());
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeUser(UUID userId) {
        if (userId == null) {
            return true;
        }
        Bucket bucket = userBuckets.computeIfAbsent(userId, k -> createUserBucket());
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeAi(UUID userId) {
        if (userId == null) {
            return true;
        }
        Bucket bucket = aiBuckets.computeIfAbsent(userId, k -> createAiBucket());
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeSync(UUID userId) {
        if (userId == null) {
            return true;
        }
        Bucket bucket = syncBuckets.computeIfAbsent(userId, k -> createSyncBucket());
        return bucket.tryConsume(1);
    }

    private Bucket createLoginBucket() {
        AppProperties.RateLimit config = appProperties.getRateLimit();
        Bandwidth limit = Bandwidth.classic(
                config.getLoginCapacity(),
                Refill.greedy(config.getLoginRefillTokens(), Duration.ofMinutes(config.getLoginDurationMinutes()))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createUserBucket() {
        AppProperties.RateLimit config = appProperties.getRateLimit();
        Bandwidth limit = Bandwidth.classic(
                config.getUserCapacity(),
                Refill.greedy(config.getUserRefillTokens(), Duration.ofMinutes(config.getUserDurationMinutes()))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createAiBucket() {
        AppProperties.RateLimit config = appProperties.getRateLimit();
        Bandwidth limit = Bandwidth.classic(
                config.getAiCapacity(),
                Refill.greedy(config.getAiRefillTokens(), Duration.ofMinutes(config.getAiDurationMinutes()))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createSyncBucket() {
        AppProperties.RateLimit config = appProperties.getRateLimit();
        Bandwidth limit = Bandwidth.classic(
                config.getSyncCapacity(),
                Refill.greedy(config.getSyncRefillTokens(), Duration.ofMinutes(config.getSyncDurationMinutes()))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
