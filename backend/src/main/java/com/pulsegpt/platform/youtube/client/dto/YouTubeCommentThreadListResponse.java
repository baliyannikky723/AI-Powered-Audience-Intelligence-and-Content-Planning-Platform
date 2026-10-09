package com.pulsegpt.platform.youtube.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouTubeCommentThreadListResponse(
        String kind,
        String etag,
        String nextPageToken,
        PageInfo pageInfo,
        List<CommentThreadItem> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(
            int totalResults,
            int resultsPerPage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentThreadItem(
            String id,
            Snippet snippet,
            Replies replies
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(
            String videoId,
            TopLevelComment topLevelComment,
            Boolean canReply,
            Long totalReplyCount,
            Boolean isPublic
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopLevelComment(
            String id,
            CommentSnippet snippet
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentSnippet(
            String authorDisplayName,
            String authorProfileImageUrl,
            String authorChannelUrl,
            AuthorChannelId authorChannelId,
            String textDisplay,
            String textOriginal,
            String parentId,
            Long likeCount,
            String publishedAt,
            String updatedAt
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record AuthorChannelId(
                String value
        ) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Replies(
            List<ReplyComment> comments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReplyComment(
            String id,
            CommentSnippet snippet
    ) {}
}
