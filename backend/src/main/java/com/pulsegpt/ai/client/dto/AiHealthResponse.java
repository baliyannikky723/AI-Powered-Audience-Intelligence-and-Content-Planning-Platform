package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiHealthResponse(
        String status,
        String service,
        String version,
        @JsonProperty("models_loaded") Boolean modelsLoaded
) {}
