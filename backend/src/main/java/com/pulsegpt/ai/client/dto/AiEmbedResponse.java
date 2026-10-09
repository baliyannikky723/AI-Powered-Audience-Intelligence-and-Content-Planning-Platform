package com.pulsegpt.ai.client.dto;

import java.util.List;

public record AiEmbedResponse(
        String model,
        String version,
        Integer dimension,
        List<List<Double>> embeddings
) {}
