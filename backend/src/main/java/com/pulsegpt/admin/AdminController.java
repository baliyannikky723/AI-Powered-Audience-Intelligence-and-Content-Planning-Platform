package com.pulsegpt.admin;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints restricted to users with ROLE_ADMIN")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get system admin metrics overview", description = "Restricted to administrators.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActive(true);

        Map<String, Object> metrics = Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "systemStatus", "HEALTHY"
        );

        return ResponseEntity.ok(ApiResponse.ok("Admin metrics retrieved", metrics));
    }
}
