package com.pulsegpt.ai.client.dto;

import com.pulsegpt.comment.IntentType;

public record AiIntentResult(
        IntentType label,
        Double score
) {}
