package com.pulsegpt.user.dto;

import com.pulsegpt.user.UserRole;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt
) {}
