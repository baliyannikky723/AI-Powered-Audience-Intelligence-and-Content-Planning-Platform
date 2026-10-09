package com.pulsegpt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();
    private YouTube youtube = new YouTube();
    private AiService aiService = new AiService();

    @Getter
    @Setter
    public static class AiService {
        private String baseUrl = "http://localhost:8000";
        private String internalApiKey = "pulse-internal-secret-key-3f8a9e";
        private int timeoutMs = 10000;
        private int maxBatchSize = 32;
        private int retryAttempts = 3;
        private long backoffDelayMs = 500L;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs = 900000L; // 15 minutes
        private long refreshExpirationMs = 604800000L; // 7 days
    }

    @Getter
    @Setter
    public static class Cookie {
        private String name = "pulsegpt_refresh_token";
        private boolean httpOnly = true;
        private boolean secure = false; // Dev default; overridden in prod
        private String sameSite = "Lax";
        private String path = "/api/v1/auth";
        private int maxAgeSeconds = 604800; // 7 days
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        private String allowedHeaders = "*";
        private boolean allowCredentials = true;
        private long maxAgeSeconds = 3600L;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private long loginCapacity = 5L;
        private long loginRefillTokens = 5L;
        private long loginDurationMinutes = 1L;

        private long userCapacity = 120L;
        private long userRefillTokens = 120L;
        private long userDurationMinutes = 1L;

        private long aiCapacity = 10L;
        private long aiRefillTokens = 10L;
        private long aiDurationMinutes = 1L;

        private long syncCapacity = 3L;
        private long syncRefillTokens = 3L;
        private long syncDurationMinutes = 10L;
    }

    @Getter
    @Setter
    public static class YouTube {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "http://localhost:8080/api/v1/integrations/youtube/callback";
        private String frontendRedirectSuccessUrl = "http://localhost:5173/app/integrations?status=success";
        private String frontendRedirectErrorUrl = "http://localhost:5173/app/integrations?status=error";
        private String tokenEncryptionKey = "01234567890123456789012345678901"; // 32 chars for AES-256
        private int initialSyncDays = 90;
        private int maxVideosPerSync = 50;
        private int maxCommentsPerVideo = 100;
        private long quotaLimit = 10000L;
        private int quotaWarningThreshold = 80;
        private int quotaStopThreshold = 90;
    }
}
