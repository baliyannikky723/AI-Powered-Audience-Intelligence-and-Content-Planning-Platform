package com.pulsegpt.rag.dto;

import lombok.Builder;

@Builder
public record RagValidationCheckResult(
        String checkName,
        boolean passed,
        String reason
) {
}
