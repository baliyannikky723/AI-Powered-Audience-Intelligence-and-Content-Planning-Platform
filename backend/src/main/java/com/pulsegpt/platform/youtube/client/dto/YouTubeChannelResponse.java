package com.pulsegpt.platform.youtube.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouTubeChannelResponse(
        String kind,
        String etag,
        List<ChannelItem> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChannelItem(
            String id,
            Snippet snippet,
            ContentDetails contentDetails,
            Statistics statistics
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(
            String title,
            String description,
            String customUrl,
            String publishedAt,
            Thumbnails thumbnails
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnails(
            Thumbnail defaultThumbnail,
            Thumbnail medium,
            Thumbnail high
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Thumbnail(
                String url,
                Integer width,
                Integer height
        ) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentDetails(
            RelatedPlaylists relatedPlaylists
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record RelatedPlaylists(
                String uploads,
                String likes
        ) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Statistics(
            String viewCount,
            String subscriberCount,
            Boolean hiddenSubscriberCount,
            String videoCount
    ) {}
}
