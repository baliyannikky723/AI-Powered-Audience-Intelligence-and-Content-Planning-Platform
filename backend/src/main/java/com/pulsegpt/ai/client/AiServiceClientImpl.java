package com.pulsegpt.ai.client;

import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.config.AppProperties;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import org.slf4j.MDC;
import java.time.Duration;

@Slf4j
@Component
public class AiServiceClientImpl implements AiServiceClient {

    private final RestClient restClient;
    private final AppProperties appProperties;

    public AiServiceClientImpl(AppProperties appProperties, RestClient.Builder restClientBuilder) {
        this.appProperties = appProperties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = appProperties.getAiService().getTimeoutMs();
        requestFactory.setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = restClientBuilder
                .baseUrl(appProperties.getAiService().getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Internal-Service-Key", appProperties.getAiService().getInternalApiKey())
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId != null && !correlationId.isBlank()) {
                        request.getHeaders().set("X-Correlation-ID", correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public AiHealthResponse getHealth() {
        try {
            return restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(AiHealthResponse.class);
        } catch (Exception ex) {
            log.warn("AI Service health check failed: {}", ex.getMessage());
            return new AiHealthResponse("DOWN", "pulsegpt-ai-service", "unknown", false);
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleProcessCommentFallback")
    public AiCommentProcessResponse processComment(AiCommentProcessRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/process/comment")
                    .body(request)
                    .retrieve()
                    .body(AiCommentProcessResponse.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("AI Service authentication failed: Invalid internal API key");
            throw new ApiException("AI Service authentication failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI_AUTH_ERROR");
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Service rate limited (429)");
            throw new ApiException("AI Service is temporarily overloaded", HttpStatus.TOO_MANY_REQUESTS, "AI_SERVICE_RATE_LIMITED");
        } catch (HttpClientErrorException ex) {
            log.error("AI Service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service processing error: " + ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode().value()), "AI_CLIENT_ERROR");
        } catch (HttpServerErrorException ex) {
            log.error("AI Service internal error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service internal error", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException ex) {
            log.error("AI Service connection / timeout error: {}", ex.getMessage());
            throw new ApiException("AI Service connection timed out or unavailable", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleProcessBatchFallback")
    public AiBatchCommentProcessResponse processBatch(AiBatchCommentProcessRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/process/comments/batch")
                    .body(request)
                    .retrieve()
                    .body(AiBatchCommentProcessResponse.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("AI Service authentication failed: Invalid internal API key");
            throw new ApiException("AI Service authentication failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI_AUTH_ERROR");
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Service rate limited (429)");
            throw new ApiException("AI Service is temporarily overloaded", HttpStatus.TOO_MANY_REQUESTS, "AI_SERVICE_RATE_LIMITED");
        } catch (HttpClientErrorException ex) {
            log.error("AI Service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service batch error: " + ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode().value()), "AI_CLIENT_ERROR");
        } catch (HttpServerErrorException ex) {
            log.error("AI Service internal error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service internal error", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException ex) {
            log.error("AI Service connection / timeout error: {}", ex.getMessage());
            throw new ApiException("AI Service connection timed out or unavailable", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleEmbedFallback")
    public AiEmbedResponse generateEmbeddings(AiEmbedRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/embed")
                    .body(request)
                    .retrieve()
                    .body(AiEmbedResponse.class);
        } catch (Exception ex) {
            log.error("AI Service embedding error: {}", ex.getMessage());
            throw new ApiException("AI Service embedding error: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleClusterFallback")
    public AiClusteringRunResponse clusterComments(AiClusteringRunRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/cluster")
                    .body(request)
                    .retrieve()
                    .body(AiClusteringRunResponse.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("AI Service authentication failed: Invalid internal API key");
            throw new ApiException("AI Service authentication failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI_AUTH_ERROR");
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Service rate limited (429)");
            throw new ApiException("AI Service is temporarily overloaded", HttpStatus.TOO_MANY_REQUESTS, "AI_SERVICE_RATE_LIMITED");
        } catch (HttpClientErrorException ex) {
            log.error("AI Service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service clustering error: " + ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode().value()), "AI_CLIENT_ERROR");
        } catch (HttpServerErrorException ex) {
            log.error("AI Service internal error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service internal error", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException ex) {
            log.error("AI Service connection / timeout error during clustering: {}", ex.getMessage());
            throw new ApiException("AI Service connection timed out or unavailable during clustering", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleGenerateRecommendationsFallback")
    public AiRecommendationGenerateResponse generateRecommendations(AiRecommendationGenerateRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/recommendations/generate")
                    .body(request)
                    .retrieve()
                    .body(AiRecommendationGenerateResponse.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("AI Service authentication failed: Invalid internal API key");
            throw new ApiException("AI Service authentication failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI_AUTH_ERROR");
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Service rate limited (429)");
            throw new ApiException("AI Service is temporarily overloaded", HttpStatus.TOO_MANY_REQUESTS, "AI_SERVICE_RATE_LIMITED");
        } catch (HttpClientErrorException ex) {
            log.error("AI Service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service recommendation error: " + ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode().value()), "AI_CLIENT_ERROR");
        } catch (HttpServerErrorException ex) {
            log.error("AI Service internal error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service internal error", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException ex) {
            log.error("AI Service connection / timeout error during recommendation: {}", ex.getMessage());
            throw new ApiException("AI Service connection timed out or unavailable during recommendation", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleRepairRecommendationFallback")
    public AiRecommendationRepairResponse repairRecommendation(AiRecommendationRepairRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/recommendations/repair")
                    .body(request)
                    .retrieve()
                    .body(AiRecommendationRepairResponse.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("AI Service authentication failed: Invalid internal API key");
            throw new ApiException("AI Service authentication failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI_AUTH_ERROR");
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Service rate limited (429)");
            throw new ApiException("AI Service is temporarily overloaded", HttpStatus.TOO_MANY_REQUESTS, "AI_SERVICE_RATE_LIMITED");
        } catch (HttpClientErrorException ex) {
            log.error("AI Service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service repair error: " + ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode().value()), "AI_CLIENT_ERROR");
        } catch (HttpServerErrorException ex) {
            log.error("AI Service internal error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service internal error", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException ex) {
            log.error("AI Service connection / timeout error during repair: {}", ex.getMessage());
            throw new ApiException("AI Service connection timed out or unavailable during repair", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleGenerateProductionDraftFallback")
    public AiProductionGenerateResponse generateProductionDraft(AiProductionGenerateRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/production/generate")
                    .body(request)
                    .retrieve()
                    .body(AiProductionGenerateResponse.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("AI Service authentication failed: Invalid internal API key");
            throw new ApiException("AI Service authentication failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI_AUTH_ERROR");
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Service rate limited (429)");
            throw new ApiException("AI Service is temporarily overloaded", HttpStatus.TOO_MANY_REQUESTS, "AI_SERVICE_RATE_LIMITED");
        } catch (HttpClientErrorException ex) {
            log.error("AI Service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service production generation error: " + ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode().value()), "AI_CLIENT_ERROR");
        } catch (HttpServerErrorException ex) {
            log.error("AI Service internal error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service internal error", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException ex) {
            log.error("AI Service connection / timeout error during production generation: {}", ex.getMessage());
            throw new ApiException("AI Service connection timed out or unavailable during production generation", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    @Retry(name = "aiServiceRetry", fallbackMethod = "handleRepairProductionDraftFallback")
    public AiProductionRepairResponse repairProductionDraft(AiProductionRepairRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/production/repair")
                    .body(request)
                    .retrieve()
                    .body(AiProductionRepairResponse.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("AI Service authentication failed: Invalid internal API key");
            throw new ApiException("AI Service authentication failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI_AUTH_ERROR");
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Service rate limited (429)");
            throw new ApiException("AI Service is temporarily overloaded", HttpStatus.TOO_MANY_REQUESTS, "AI_SERVICE_RATE_LIMITED");
        } catch (HttpClientErrorException ex) {
            log.error("AI Service client error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service production repair error: " + ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode().value()), "AI_CLIENT_ERROR");
        } catch (HttpServerErrorException ex) {
            log.error("AI Service internal error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("AI Service internal error", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException ex) {
            log.error("AI Service connection / timeout error during production repair: {}", ex.getMessage());
            throw new ApiException("AI Service connection timed out or unavailable during production repair", HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
        }
    }


    // Fallback methods for Resilience4j
    public AiCommentProcessResponse handleProcessCommentFallback(AiCommentProcessRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for comment {}: {}", request.commentId(), t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service call failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }

    public AiBatchCommentProcessResponse handleProcessBatchFallback(AiBatchCommentProcessRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for batch of {} comments: {}", request.comments().size(), t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service batch call failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }

    public AiEmbedResponse handleEmbedFallback(AiEmbedRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for embeddings: {}", t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service embedding failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }

    public AiClusteringRunResponse handleClusterFallback(AiClusteringRunRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for clustering run {}: {}", request.runId(), t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service clustering call failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }

    public AiRecommendationGenerateResponse handleGenerateRecommendationsFallback(AiRecommendationGenerateRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for recommendation request {}: {}", request.requestId(), t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service recommendation call failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }

    public AiRecommendationRepairResponse handleRepairRecommendationFallback(AiRecommendationRepairRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for recommendation repair {}: {}", request.requestId(), t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service repair call failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }

    public AiProductionGenerateResponse handleGenerateProductionDraftFallback(AiProductionGenerateRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for production draft generation {}: {}", request.requestId(), t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service production generation failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }

    public AiProductionRepairResponse handleRepairProductionDraftFallback(AiProductionRepairRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for production draft repair {}: {}", request.requestId(), t.getMessage());
        if (t instanceof ApiException apiException) throw apiException;
        throw new ApiException("AI Service production repair failed after retries: " + t.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
    }
}

