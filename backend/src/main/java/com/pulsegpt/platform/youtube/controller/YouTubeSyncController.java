package com.pulsegpt.platform.youtube.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.exception.RateLimitExceededException;
import com.pulsegpt.platform.youtube.dto.IngestionSummaryResponse;
import com.pulsegpt.platform.youtube.service.YouTubeIngestionService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.security.RateLimitingService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/integrations/youtube")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "YouTube Integration", description = "Endpoints for managing YouTube synchronization and data ingestion")
public class YouTubeSyncController {

    private final YouTubeIngestionService youTubeIngestionService;
    private final CurrentUserService currentUserService;
    private final RateLimitingService rateLimitingService;

    @PostMapping("/sync")
    @Operation(summary = "Trigger idempotent synchronization of YouTube channel videos and comments")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Synchronization completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No connected YouTube account found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Ingestion sync already in progress"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit or quota threshold reached")
    })
    public ResponseEntity<ApiResponse<IngestionSummaryResponse>> sync() {
        User currentUser = currentUserService.requireUser();

        // Enforce expensive sync operation rate limit (e.g. 3 requests / 10 minutes)
        if (!rateLimitingService.tryConsumeSync(currentUser.getId())) {
            log.warn("YouTube sync rate limit exceeded for user: {}", currentUser.getId());
            throw new RateLimitExceededException("YouTube synchronization rate limit exceeded. Please wait before syncing again.");
        }

        IngestionSummaryResponse summary = youTubeIngestionService.sync(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("YouTube synchronization completed successfully", summary));
    }
}
