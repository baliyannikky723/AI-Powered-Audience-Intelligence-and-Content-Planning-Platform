package com.pulsegpt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.auth.RefreshToken;
import com.pulsegpt.auth.RefreshTokenRepository;
import com.pulsegpt.auth.dto.*;
import com.pulsegpt.auth.service.AuthService;
import com.pulsegpt.auth.service.RefreshTokenService;
import com.pulsegpt.common.exception.UnauthorizedException;
import com.pulsegpt.common.exception.ValidationException;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import com.pulsegpt.user.UserRole;
import com.pulsegpt.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication, JWT, and Security Isolation Unit Tests")
class AuthenticationSecurityTest {

    private PasswordEncoder passwordEncoder;
    private AppProperties appProperties;
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditService auditService;

    private RefreshTokenService refreshTokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        appProperties = new AppProperties();
        appProperties.getJwt().setSecret("very_secure_jwt_secret_key_that_is_at_least_256_bits_long_pulsegpt_2026!");
        appProperties.getJwt().setExpirationMs(900000L); // 15 mins
        appProperties.getJwt().setRefreshExpirationMs(604800000L); // 7 days

        jwtService = new JwtService(appProperties);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, appProperties, auditService);

        com.pulsegpt.auth.service.CookieService cookieService = new com.pulsegpt.auth.service.CookieService(appProperties);
        RateLimitingService rateLimitingService = new RateLimitingService(appProperties);
        com.pulsegpt.auth.mapper.AuthMapper authMapper = new com.pulsegpt.auth.mapper.AuthMapperImpl();

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                cookieService,
                rateLimitingService,
                auditService,
                authMapper
        );
    }

    @Nested
    @DisplayName("Password and Registration Security")
    class RegistrationTests {

        @Test
        @DisplayName("Password hashing: BCrypt strength >= 12 and salt verification")
        void testPasswordHashing() {
            String rawPassword = "StrongPassword123!@#";
            String encoded = passwordEncoder.encode(rawPassword);

            assertThat(encoded).isNotEqualTo(rawPassword);
            assertThat(encoded).startsWith("$2a$12$");
            assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
            assertThat(passwordEncoder.matches("WrongPassword", encoded)).isFalse();
        }

        @Test
        @DisplayName("Registration normalizes email to lowercase and prevents duplicate emails")
        void testRegistrationEmailNormalizationAndDuplicatePrevention() {
            when(userRepository.existsByEmail("creator@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            RegisterRequest request = RegisterRequest.builder()
                    .name("Jane Creator")
                    .email("  CREATOR@Example.COM  ")
                    .password("SuperP@ssw0rd123")
                    .confirmPassword("SuperP@ssw0rd123")
                    .build();

            UserResponse response = authService.register(request, "Mozilla/5.0", "127.0.0.1");

            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("creator@example.com");
            assertThat(response.role()).isEqualTo(UserRole.CREATOR);

            // Duplicate registration attempt
            when(userRepository.existsByEmail("creator@example.com")).thenReturn(true);
            assertThatThrownBy(() -> authService.register(request, "Mozilla/5.0", "127.0.0.1"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already in use");
        }

        @Test
        @DisplayName("Registration fails when password confirmation mismatches")
        void testPasswordConfirmationMismatch() {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Jane Creator")
                    .email("jane@example.com")
                    .password("SuperP@ssw0rd123")
                    .confirmPassword("DifferentP@ssw0rd456")
                    .build();

            assertThatThrownBy(() -> authService.register(request, "Mozilla/5.0", "127.0.0.1"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Passwords do not match");
        }
    }

    @Nested
    @DisplayName("JWT Token Management")
    class JwtTests {

        @Test
        @DisplayName("JWT Access Token: Generates signed token, extracts claims, validates signature and role")
        void testJwtAccessTokens() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Admin User")
                    .email("admin@pulsegpt.ai")
                    .role(UserRole.ADMIN)
                    .active(true)
                    .build();

            String token = jwtService.generateAccessToken(user);

            assertThat(token).isNotBlank();
            assertThat(jwtService.validateToken(token)).isTrue();
            assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
            assertThat(jwtService.extractEmail(token)).isEqualTo("admin@pulsegpt.ai");
            assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("JWT Validation: Rejects malformed and tampered tokens")
        void testJwtTamperingRejection() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Creator")
                    .email("creator@pulsegpt.ai")
                    .role(UserRole.CREATOR)
                    .active(true)
                    .build();

            String token = jwtService.generateAccessToken(user);
            String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

            assertThat(jwtService.validateToken(tamperedToken)).isFalse();
            assertThat(jwtService.validateToken("invalid.token.structure")).isFalse();
        }
    }

    @Nested
    @DisplayName("Login & Security Isolation")
    class LoginTests {

        @Test
        @DisplayName("Login fails with generic error on wrong password to prevent user enumeration")
        void testLoginInvalidPassword() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Creator")
                    .email("creator@pulsegpt.ai")
                    .passwordHash(passwordEncoder.encode("CorrectPassword123!"))
                    .role(UserRole.CREATOR)
                    .active(true)
                    .build();

            when(userRepository.findByEmail("creator@pulsegpt.ai")).thenReturn(Optional.of(user));

            LoginRequest request = LoginRequest.builder()
                    .email("creator@pulsegpt.ai")
                    .password("WrongPassword999!")
                    .build();

            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThatThrownBy(() -> authService.login(request, response, "UserAgent", "127.0.0.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Login fails when user account is disabled (active = false)")
        void testLoginDisabledUser() {
            User disabledUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("Suspended Creator")
                    .email("suspended@pulsegpt.ai")
                    .passwordHash(passwordEncoder.encode("CorrectPassword123!"))
                    .role(UserRole.CREATOR)
                    .active(false)
                    .build();

            when(userRepository.findByEmail("suspended@pulsegpt.ai")).thenReturn(Optional.of(disabledUser));

            LoginRequest request = LoginRequest.builder()
                    .email("suspended@pulsegpt.ai")
                    .password("CorrectPassword123!")
                    .build();

            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThatThrownBy(() -> authService.login(request, response, "UserAgent", "127.0.0.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("Login succeeds with valid credentials and sets HttpOnly refresh cookie")
        void testLoginSuccess() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Active Creator")
                    .email("active@pulsegpt.ai")
                    .passwordHash(passwordEncoder.encode("ValidPassword123!"))
                    .role(UserRole.CREATOR)
                    .active(true)
                    .build();

            when(userRepository.findByEmail("active@pulsegpt.ai")).thenReturn(Optional.of(user));

            LoginRequest request = LoginRequest.builder()
                    .email("active@pulsegpt.ai")
                    .password("ValidPassword123!")
                    .build();

            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthResponse authResponse = authService.login(request, response, "Mozilla/5.0", "127.0.0.1");

            assertThat(authResponse).isNotNull();
            assertThat(authResponse.accessToken()).isNotBlank();
            assertThat(authResponse.user().email()).isEqualTo("active@pulsegpt.ai");
            assertThat(response.getHeader("Set-Cookie")).contains("pulsegpt_refresh_token=");
            assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly");
        }
    }

    @Nested
    @DisplayName("Refresh Token Rotation & Anti-Reuse Security")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh token rotation: Old token is revoked, new token is issued")
        void testRefreshTokenRotation() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Rotation User")
                    .email("rotate@pulsegpt.ai")
                    .role(UserRole.CREATOR)
                    .active(true)
                    .build();

            String rawToken = "sample_raw_refresh_token_12345678901234567890";
            String tokenHash = refreshTokenService.hashToken(rawToken);

            RefreshToken existingToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken r = inv.getArgument(0);
                if (r.getId() == null) r.setId(UUID.randomUUID());
                return r;
            });

            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthResponse authResponse = authService.refresh(rawToken, response, "Agent", "127.0.0.1");

            assertThat(authResponse).isNotNull();
            assertThat(authResponse.accessToken()).isNotBlank();
            assertThat(existingToken.isRevoked()).isTrue();
            assertThat(existingToken.getReplacedByTokenId()).isNotNull();
        }

        @Test
        @DisplayName("Security Defense: Reusing a revoked refresh token triggers session termination for that user")
        void testReusedRevokedTokenRevokesAllSessions() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Attacked User")
                    .email("attacked@pulsegpt.ai")
                    .role(UserRole.CREATOR)
                    .active(true)
                    .build();

            String rawToken = "stolen_compromised_token";
            String tokenHash = refreshTokenService.hashToken(rawToken);

            RefreshToken alreadyRevokedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revokedAt(Instant.now().minusSeconds(300)) // Already revoked!
                    .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(alreadyRevokedToken));

            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThatThrownBy(() -> authService.refresh(rawToken, response, "AttackerAgent", "10.0.0.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Token reuse detected");

            // Verify all tokens for this user were revoked immediately
            verify(refreshTokenRepository).revokeAllUserTokens(eq(user.getId()), any(Instant.class));
        }

        @Test
        @DisplayName("Expired refresh token is rejected")
        void testExpiredRefreshTokenFails() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Expired User")
                    .email("expired@pulsegpt.ai")
                    .role(UserRole.CREATOR)
                    .active(true)
                    .build();

            String rawToken = "expired_token";
            String tokenHash = refreshTokenService.hashToken(rawToken);

            RefreshToken expiredToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().minusSeconds(100)) // Expired
                    .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThatThrownBy(() -> authService.refresh(rawToken, response, "Agent", "127.0.0.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Logout revokes the token and clears HTTP-Only cookie with maxAge=0")
        void testLogoutRevocationAndCookieClearing() {
            String rawToken = "token_to_logout";
            String tokenHash = refreshTokenService.hashToken(rawToken);

            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            MockHttpServletResponse response = new MockHttpServletResponse();
            LogoutResponse logoutResponse = authService.logout(rawToken, null, response);

            assertThat(logoutResponse.success()).isTrue();
            assertThat(token.isRevoked()).isTrue();
            assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
        }
    }
}
