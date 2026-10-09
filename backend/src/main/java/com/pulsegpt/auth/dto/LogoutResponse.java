package com.pulsegpt.auth.dto;

import lombok.Builder;

@Builder
public record LogoutResponse(
        boolean success,
        String message
) {}
