package com.pulsegpt.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;
import java.util.Optional;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationResult(
        boolean valid,
        List<CheckResult> checks,
        String failureSummary
) {
    public Optional<CheckResult> getCheck(String checkName) {
        if (checks == null) {
            return Optional.empty();
        }
        return checks.stream()
                .filter(c -> c.checkName().equalsIgnoreCase(checkName))
                .findFirst();
    }
}
