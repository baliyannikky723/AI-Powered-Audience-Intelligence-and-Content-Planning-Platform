package com.pulsegpt.ai.client.dto;

import java.util.List;

public record AiEmbedRequest(
        List<String> texts
) {}
