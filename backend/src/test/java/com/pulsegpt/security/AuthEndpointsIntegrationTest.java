package com.pulsegpt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.auth.dto.LoginRequest;
import com.pulsegpt.auth.dto.RegisterRequest;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authentication and Role Authorization Endpoints Tests")
class AuthEndpointsIntegrationTest {

    @TestConfiguration
    static class TestDataSourceConfig {
        @Bean
        @Primary
        public DataSource dataSource() throws SQLException {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(ds.getConnection(any(), any())).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(metaData);
            when(metaData.getConnection()).thenReturn(conn);
            when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(metaData.getDatabaseProductVersion()).thenReturn("16.0");
            when(metaData.getDatabaseMajorVersion()).thenReturn(16);
            when(metaData.getDatabaseMinorVersion()).thenReturn(0);
            when(metaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(metaData.getDriverVersion()).thenReturn("42.7.2");
            when(metaData.getDriverMajorVersion()).thenReturn(42);
            when(metaData.getDriverMinorVersion()).thenReturn(7);
            return ds;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    private User creatorUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        creatorUser = User.builder()
                .id(UUID.randomUUID())
                .name("Creator John")
                .email("john@creator.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!@#"))
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Admin Boss")
                .email("boss@pulsegpt.ai")
                .passwordHash(passwordEncoder.encode("AdminPass123!@#"))
                .role(UserRole.ADMIN)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Unauthenticated request to /auth/me returns 401 Unauthorized")
    void testUnauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("Unauthorized"));
    }

    @Test
    @DisplayName("Authenticated CREATOR can access /auth/me with Bearer token")
    void testAuthenticatedCreatorCanAccessMe() throws Exception {
        when(userRepository.findById(creatorUser.getId())).thenReturn(Optional.of(creatorUser));
        String token = jwtService.generateAccessToken(creatorUser);

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@creator.com"))
                .andExpect(jsonPath("$.data.role").value("CREATOR"));
    }

    @Test
    @DisplayName("CREATOR attempting to access /admin/overview is rejected with 403 Forbidden")
    void testCreatorAccessingAdminEndpointReturns403() throws Exception {
        when(userRepository.findById(creatorUser.getId())).thenReturn(Optional.of(creatorUser));
        String token = jwtService.generateAccessToken(creatorUser);

        mockMvc.perform(get("/admin/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("Forbidden"));
    }

    @Test
    @DisplayName("ADMIN accessing /admin/overview is permitted with 200 OK")
    void testAdminAccessingAdminEndpointReturns200() throws Exception {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        String token = jwtService.generateAccessToken(adminUser);

        mockMvc.perform(get("/admin/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemStatus").value("HEALTHY"));
    }

    @Test
    @DisplayName("Registering with invalid password returns 400 Validation Error")
    void testRegisterWeakPasswordReturns400() throws Exception {
        RegisterRequest weakPassRequest = RegisterRequest.builder()
                .name("New User")
                .email("new@example.com")
                .password("weak")
                .confirmPassword("weak")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakPassRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
