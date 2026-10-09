package com.pulsegpt.platform.dto;

import com.pulsegpt.platform.PlatformAccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdatePlatformAccountStatusRequest(
        @NotNull(message = "Status is required")
        PlatformAccountStatus status
) {}
