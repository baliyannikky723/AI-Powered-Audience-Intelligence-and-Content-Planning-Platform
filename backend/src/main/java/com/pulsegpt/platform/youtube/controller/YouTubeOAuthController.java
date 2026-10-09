package com.pulsegpt.platform.youtube.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.platform.youtube.dto.YouTubeConnectResponse;
import com.pulsegpt.platform.youtube.dto.YouTubeStatusResponse;
import com.pulsegpt.platform.youtube.service.YouTubeOAuthService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/integrations/youtube")
@RequiredArgsConstructor
@Tag(name = "YouTube Integration", description = "OAuth 2.0 connection, status, and synchronization endpoints for YouTube")
public class YouTubeOAuthController {

    private final YouTubeOAuthService youTubeOAuthService;
    private final CurrentUserService currentUserService;

    @GetMapping("/connect")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate Google OAuth authorization URL for YouTube")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authorization URL generated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<YouTubeConnectResponse>> connect() {
        User currentUser = currentUserService.requireUser();
        YouTubeConnectResponse response = youTubeOAuthService.generateConnectUrl(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("OAuth authorization URL generated successfully", response));
    }

    @GetMapping("/callback")
    @Operation(summary = "Google OAuth 2.0 callback redirect handler")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        if (error != null && !error.isBlank()) {
            log.warn("OAuth authorization denied by user or Google: {}", error);
            response.sendRedirect(youTubeOAuthService.handleCallback(null, state));
            return;
        }

        String redirectUrl = youTubeOAuthService.handleCallback(code, state);
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/disconnect")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Disconnect YouTube platform account and revoke tokens")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account disconnected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No connected account found")
    })
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        User currentUser = currentUserService.requireUser();
        youTubeOAuthService.disconnect(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("YouTube account disconnected successfully", null));
    }

    @GetMapping("/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get YouTube connection status and quota usage for current user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<YouTubeStatusResponse>> getStatus() {
        User currentUser = currentUserService.requireUser();
        YouTubeStatusResponse status = youTubeOAuthService.getStatus(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("YouTube integration status retrieved", status));
    }
}
