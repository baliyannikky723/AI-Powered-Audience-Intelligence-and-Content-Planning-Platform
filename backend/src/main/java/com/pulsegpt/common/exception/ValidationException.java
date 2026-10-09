package com.pulsegpt.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class ValidationException extends ApiException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        this.fieldErrors = Map.of();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors != null ? fieldErrors : Map.of();
    }

    public ValidationException(String field, String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        this.fieldErrors = Map.of(field, message);
    }
}
