package com.pulsegpt.platform.youtube.client;

import com.pulsegpt.platform.youtube.client.dto.*;

import java.util.List;

public interface YouTubeApiClient {

    GoogleTokenResponse exchangeAuthorizationCode(String code);

    GoogleTokenResponse refreshAccessToken(String refreshToken);

    void revokeToken(String token);

    YouTubeChannelResponse getMyChannel(String accessToken);

    YouTubeVideoListResponse getPlaylistVideos(String playlistId, String pageToken, int maxResults, String accessToken);

    YouTubeVideoListResponse getVideosByIds(List<String> videoIds, String accessToken);

    YouTubeCommentThreadListResponse getCommentThreads(String videoId, String pageToken, int maxResults, String accessToken);
}
