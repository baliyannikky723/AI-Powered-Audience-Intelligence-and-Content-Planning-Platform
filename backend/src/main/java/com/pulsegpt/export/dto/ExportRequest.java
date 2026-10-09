package com.pulsegpt.export.dto;

import com.pulsegpt.export.ExportType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ExportRequest(
        @NotNull(message = "Export type is required")
        ExportType exportType
) {}
