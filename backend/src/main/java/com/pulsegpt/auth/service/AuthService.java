package com.pulsegpt.auth.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.auth.dto.*;
import com.pulsegpt.auth.mapper.AuthMapper;
import com.pulsegpt.common.exception.RateLimitExceededException;
import com.pulsegpt.common.exception.UnauthorizedException;
import com.pulsegpt.common.exception.ValidationException;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.security.RateLimitingService;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import com.pulsegpt.user.UserRole;
import com.pulsegpt.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final RateLimitingService rateLimitingService;
    private final AuditService auditService;
    private final AuthMapper authMapper;

    @Transactional
    public UserResponse register(RegisterRequest request, String userAgent, String ipAddress) {
        if (!request.isPasswordConfirmed()) {
            throw new ValidationException("Passwords do not match");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ValidationException("Email address is already in use");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CREATOR) // Public registration always defaults to CREATOR
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        auditService.logAuthEvent(savedUser, "REGISTER", savedUser.getId().toString(),
                Map.of("email", normalizedEmail, "ip", ipAddress != null ? ipAddress : "unknown", "userAgent", userAgent != null ? userAgent : "unknown"));

        return authMapper.toUserResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response, String userAgent, String ipAddress) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // 1. Rate Limiting Check
        if (!rateLimitingService.tryConsumeLogin(ipAddress, normalizedEmail)) {
            log.warn("Rate limit exceeded for login attempt: email={}, ip={}", normalizedEmail, ipAddress);
            throw new RateLimitExceededException("Too many login attempts. Please try again in 1 minute.");
        }

        // 2. Lookup user (Generic failure to prevent email enumeration)
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.logAuthEvent(null, "LOGIN_FAILURE", normalizedEmail,
                    Map.of("reason", "INVALID_CREDENTIALS", "ip", ipAddress != null ? ipAddress : "unknown"));
            throw new UnauthorizedException("Invalid email or password");
        }

        // 3. Verify active status
        if (!user.isActive()) {
            auditService.logAuthEvent(user, "ACCOUNT_DISABLED_LOGIN_ATTEMPT", user.getId().toString(),
                    Map.of("ip", ipAddress != null ? ipAddress : "unknown"));
            throw new UnauthorizedException("User account is disabled. Please contact support.");
        }

        // 4. Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // 5. Generate Access JWT & Refresh Token
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

        // 6. Set HTTP-Only Cookie
        ResponseCookie cookie = cookieService.createRefreshTokenCookie(rawRefreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 7. Audit log
        auditService.logAuthEvent(user, "LOGIN_SUCCESS", user.getId().toString(),
                Map.of("ip", ipAddress != null ? ipAddress : "unknown"));

        UserResponse userResponse = authMapper.toUserResponse(user);
        return AuthResponse.of(accessToken, jwtService.getExpirationMs() / 1000, userResponse, rawRefreshToken);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, HttpServletResponse response, String userAgent, String ipAddress) {
        RefreshTokenService.TokenRotationResult rotation = refreshTokenService.rotateRefreshToken(rawRefreshToken, userAgent, ipAddress);
        User user = rotation.user();

        String newAccessToken = jwtService.generateAccessToken(user);
        ResponseCookie cookie = cookieService.createRefreshTokenCookie(rotation.newRawRefreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        auditService.logAuthEvent(user, "TOKEN_REFRESH", user.getId().toString(),
                Map.of("ip", ipAddress != null ? ipAddress : "unknown"));

        UserResponse userResponse = authMapper.toUserResponse(user);
        return AuthResponse.of(newAccessToken, jwtService.getExpirationMs() / 1000, userResponse, rotation.newRawRefreshToken());
    }

    @Transactional
    public LogoutResponse logout(String rawRefreshToken, User currentUser, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(rawRefreshToken);
        }

        // Clear the cookie
        ResponseCookie clearCookie = cookieService.createClearRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        if (currentUser != null) {
            auditService.logAuthEvent(currentUser, "LOGOUT", currentUser.getId().toString(), Map.of());
        }

        return LogoutResponse.builder()
                .success(true)
                .message("Successfully logged out")
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(User currentUser) {
        return authMapper.toUserResponse(currentUser);
    }
}
