package com.pulsegpt.ai.client.dto;

import java.util.List;

public record AiBatchCommentProcessRequest(
        List<AiCommentProcessRequest> comments
) {}
