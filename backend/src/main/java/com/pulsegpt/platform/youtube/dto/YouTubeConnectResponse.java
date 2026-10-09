package com.pulsegpt.platform.youtube.dto;

public record YouTubeConnectResponse(
        String authorizationUrl,
        String state
) {}
