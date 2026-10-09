package com.pulsegpt.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlatformAccountResponse(
        UUID id,
        PlatformType platform,
        String externalAccountId,
        String accountName,
        PlatformAccountStatus status,
        Instant connectedAt,
        Instant disconnectedAt,
        Instant lastSyncedAt,
        Map<String, Object> metadata
) {}
