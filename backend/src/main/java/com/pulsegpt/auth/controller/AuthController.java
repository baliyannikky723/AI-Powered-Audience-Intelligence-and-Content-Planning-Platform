package com.pulsegpt.auth.controller;

import com.pulsegpt.auth.dto.*;
import com.pulsegpt.auth.service.AuthService;
import com.pulsegpt.auth.service.CookieService;
import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import com.pulsegpt.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, token rotation, and sessions")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;
    private final CurrentUserService currentUserService;

    @PostMapping("/register")
    @Operation(summary = "Register a new creator account", description = "Creates a new user account with role CREATOR.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or passwords mismatch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        UserResponse userResponse = authService.register(request, userAgent, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Account registered successfully", userResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and issues an Access JWT & Refresh Token.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse authResponse = authService.login(request, httpResponse, userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates the refresh token and returns a new Access JWT.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid, expired, or reused refresh token")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String rawToken = cookieService.extractRefreshTokenFromCookie(httpRequest)
                .or(() -> requestBody != null ? java.util.Optional.ofNullable(requestBody.refreshToken()) : java.util.Optional.empty())
                .orElse(null);

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse authResponse = authService.refresh(rawToken, httpResponse, userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", authResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes the active refresh token and clears session cookies.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful")
    })
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String rawToken = cookieService.extractRefreshTokenFromCookie(httpRequest)
                .or(() -> requestBody != null ? java.util.Optional.ofNullable(requestBody.refreshToken()) : java.util.Optional.empty())
                .orElse(null);

        User currentUser = currentUserService.getCurrentUser().orElse(null);
        LogoutResponse logoutResponse = authService.logout(rawToken, currentUser, httpResponse);
        return ResponseEntity.ok(ApiResponse.ok("Logout successful", logoutResponse));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user profile", description = "Returns user profile of the authenticated principal.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        User currentUser = currentUserService.requireUser();
        UserResponse response = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully", response));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
