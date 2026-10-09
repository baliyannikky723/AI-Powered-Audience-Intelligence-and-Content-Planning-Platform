package com.pulsegpt.export.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record TimelineSceneDto(
        int sceneNumber,
        String sectionTitle,
        String purpose,
        List<String> talkingPoints,
        int estimatedDurationSeconds,
        String formattedDuration,
        String visualDirection
) {}
