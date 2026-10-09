package com.pulsegpt.auth.dto;

import lombok.Builder;

@Builder
public record RefreshTokenRequest(
        String refreshToken
) {}
