package com.pulsegpt.export.dto;

import com.pulsegpt.export.ExportType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record ExportResponse(
        UUID id,
        UUID productionAssetId,
        UUID recommendationId,
        ExportType exportType,
        String fileName,
        String mimeType,
        String contentHash,
        Integer version,
        Long fileSizeBytes,
        Instant createdAt,
        String downloadUrl,
        Map<String, Object> metadata
) {}
