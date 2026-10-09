package com.pulsegpt.ai.client.dto;

import java.util.List;

public record AiBatchCommentProcessResponse(
        List<AiCommentProcessResponse> results,
        Integer total,
        Integer successful,
        Integer failed
) {}
