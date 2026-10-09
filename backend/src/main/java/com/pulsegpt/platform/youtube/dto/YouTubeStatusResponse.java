package com.pulsegpt.platform.youtube.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulsegpt.platform.PlatformAccountStatus;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record YouTubeStatusResponse(
        boolean connected,
        UUID accountId,
        String accountName,
        String externalAccountId,
        PlatformAccountStatus status,
        Instant connectedAt,
        Instant lastSyncedAt,
        long quotaUsedToday,
        long quotaLimit
) {}
