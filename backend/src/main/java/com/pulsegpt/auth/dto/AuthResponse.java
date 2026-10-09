package com.pulsegpt.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.user.dto.UserResponse;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        String refreshToken
) {
    public static AuthResponse of(String accessToken, long expiresIn, UserResponse user, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .refreshToken(refreshToken)
                .build();
    }
}
