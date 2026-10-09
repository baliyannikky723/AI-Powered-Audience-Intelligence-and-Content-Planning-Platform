package com.pulsegpt.rag.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RagValidationResult(
        boolean valid,
        List<RagValidationCheckResult> checks,
        String failureSummary,
        double citationValidityScore,
        boolean unsupportedClaimsDetected,
        boolean promptInjectionDetected
) {
}
