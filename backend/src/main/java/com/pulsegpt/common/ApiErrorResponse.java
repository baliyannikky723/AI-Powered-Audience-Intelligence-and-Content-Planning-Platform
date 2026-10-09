package com.pulsegpt.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    @Builder.Default
    private boolean success = false;

    private String errorCode;

    private String message;

    private Map<String, String> fieldErrors;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private String correlationId;

    private String path;

    public static ApiErrorResponse of(int status, String errorCode, String message, String path, String correlationId) {
        return ApiErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .build();
    }
}
