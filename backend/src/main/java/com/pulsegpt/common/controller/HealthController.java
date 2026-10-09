package com.pulsegpt.common.controller;

import com.pulsegpt.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "System health and status endpoints")
public class HealthController {

    @Value("${spring.application.name:PulseGPT-Backend}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @GetMapping
    @Operation(summary = "Health check endpoint", description = "Returns the status of the PulseGPT backend service")
    public ResponseEntity<ApiResponse<HealthStatus>> checkHealth() {
        HealthStatus status = HealthStatus.builder()
                .service(applicationName)
                .status("UP")
                .version("v2.0.0")
                .profile(activeProfile)
                .timestamp(Instant.now().toString())
                .components(Map.of(
                        "system", "OPERATIONAL",
                        "database", "CONFIGURED",
                        "security", "INITIALIZED"
                ))
                .build();

        return ResponseEntity.ok(ApiResponse.ok("PulseGPT Backend is healthy and operational", status));
    }

    @Data
    @Builder
    public static class HealthStatus {
        private String service;
        private String status;
        private String version;
        private String profile;
        private String timestamp;
        private Map<String, String> components;
    }
}
