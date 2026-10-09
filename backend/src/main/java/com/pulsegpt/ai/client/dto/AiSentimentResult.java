package com.pulsegpt.ai.client.dto;

import com.pulsegpt.comment.SentimentLabel;

public record AiSentimentResult(
        SentimentLabel label,
        Double score
) {}
