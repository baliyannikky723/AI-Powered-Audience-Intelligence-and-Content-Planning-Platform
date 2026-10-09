package com.pulsegpt.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckResult(
        String checkName,
        boolean passed,
        String reason
) {
    public static CheckResult passed(String checkName, String reason) {
        return new CheckResult(checkName, true, reason);
    }

    public static CheckResult failed(String checkName, String reason) {
        return new CheckResult(checkName, false, reason);
    }
}
