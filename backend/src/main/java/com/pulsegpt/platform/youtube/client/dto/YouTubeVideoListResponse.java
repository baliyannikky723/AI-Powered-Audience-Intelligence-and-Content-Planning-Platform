package com.pulsegpt.platform.youtube.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouTubeVideoListResponse(
        String kind,
        String etag,
        String nextPageToken,
        String prevPageToken,
        PageInfo pageInfo,
        List<VideoItem> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(
            int totalResults,
            int resultsPerPage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoItem(
            String id,
            Snippet snippet,
            ContentDetails contentDetails,
            Statistics statistics
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(
            String publishedAt,
            String channelId,
            String title,
            String description,
            String channelTitle,
            ResourceId resourceId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResourceId(
            String kind,
            String videoId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentDetails(
            String duration
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Statistics(
            String viewCount,
            String likeCount,
            String commentCount
    ) {}
}
