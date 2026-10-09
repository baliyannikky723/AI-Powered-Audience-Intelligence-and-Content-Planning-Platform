package com.pulsegpt.platform.youtube.client;

import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.platform.youtube.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class YouTubeApiClientImpl implements YouTubeApiClient {

    private static final String YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    private final RestClient restClient;
    private final AppProperties appProperties;

    public YouTubeApiClientImpl(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.restClient = restClientBuilder
                .baseUrl(YOUTUBE_BASE_URL)
                .build();
    }

    @Override
    public GoogleTokenResponse exchangeAuthorizationCode(String code) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("code", code);
            formData.add("client_id", appProperties.getYoutube().getClientId());
            formData.add("client_secret", appProperties.getYoutube().getClientSecret());
            formData.add("redirect_uri", appProperties.getYoutube().getRedirectUri());
            formData.add("grant_type", "authorization_code");

            return RestClient.create().post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (HttpClientErrorException e) {
            log.warn("Google OAuth code exchange failed with status: {}", e.getStatusCode());
            throw new ApiException("Failed to exchange authorization code with Google", HttpStatus.BAD_REQUEST, "OAUTH_CODE_EXCHANGE_FAILED");
        } catch (Exception e) {
            log.error("Unexpected error during Google OAuth code exchange: {}", e.getMessage());
            throw new ApiException("External Google OAuth service error", HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    public GoogleTokenResponse refreshAccessToken(String refreshToken) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("refresh_token", refreshToken);
            formData.add("client_id", appProperties.getYoutube().getClientId());
            formData.add("client_secret", appProperties.getYoutube().getClientSecret());
            formData.add("grant_type", "refresh_token");

            return RestClient.create().post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            log.warn("Google OAuth refresh token rejected or revoked: {}", e.getStatusCode());
            throw new ApiException("YouTube credentials expired or revoked. Re-authentication required.", HttpStatus.UNAUTHORIZED, "YOUTUBE_REAUTH_REQUIRED");
        } catch (Exception e) {
            log.error("Failed to refresh YouTube access token: {}", e.getMessage());
            throw new ApiException("Failed to refresh YouTube access token", HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_SERVICE_UNAVAILABLE");
        }
    }

    @Override
    public void revokeToken(String token) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("token", token);

            RestClient.create().post()
                    .uri(GOOGLE_REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully revoked Google OAuth token");
        } catch (Exception e) {
            log.warn("Non-fatal error revoking Google OAuth token: {}", e.getMessage());
        }
    }

    @Override
    public YouTubeChannelResponse getMyChannel(String accessToken) {
        return executeGet("/channels?part=snippet,contentDetails,statistics&mine=true", accessToken, YouTubeChannelResponse.class);
    }

    @Override
    public YouTubeVideoListResponse getPlaylistVideos(String playlistId, String pageToken, int maxResults, String accessToken) {
        StringBuilder uriBuilder = new StringBuilder("/playlistItems?part=snippet,contentDetails")
                .append("&playlistId=").append(playlistId)
                .append("&maxResults=").append(Math.min(maxResults, 50));

        if (pageToken != null && !pageToken.isBlank()) {
            uriBuilder.append("&pageToken=").append(pageToken);
        }

        return executeGet(uriBuilder.toString(), accessToken, YouTubeVideoListResponse.class);
    }

    @Override
    public YouTubeVideoListResponse getVideosByIds(List<String> videoIds, String accessToken) {
        if (videoIds == null || videoIds.isEmpty()) {
            return new YouTubeVideoListResponse("youtube#videoListResponse", null, null, null, null, List.of());
        }
        String idsJoined = String.join(",", videoIds);
        String uri = "/videos?part=snippet,contentDetails,statistics&id=" + idsJoined;
        return executeGet(uri, accessToken, YouTubeVideoListResponse.class);
    }

    @Override
    public YouTubeCommentThreadListResponse getCommentThreads(String videoId, String pageToken, int maxResults, String accessToken) {
        StringBuilder uriBuilder = new StringBuilder("/commentThreads?part=snippet,replies")
                .append("&videoId=").append(videoId)
                .append("&maxResults=").append(Math.min(maxResults, 100))
                .append("&textFormat=plainText")
                .append("&order=relevance");

        if (pageToken != null && !pageToken.isBlank()) {
            uriBuilder.append("&pageToken=").append(pageToken);
        }

        return executeGet(uriBuilder.toString(), accessToken, YouTubeCommentThreadListResponse.class);
    }

    private <T> T executeGet(String uri, String accessToken, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("YouTube API unauthorized on uri {}: {}", uri, e.getStatusCode());
            throw new ApiException("YouTube credentials expired or invalid. Re-authentication required.", HttpStatus.UNAUTHORIZED, "YOUTUBE_REAUTH_REQUIRED");
        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("YouTube API forbidden on uri {}: {}", uri, e.getMessage());
            if (e.getResponseBodyAsString().contains("quotaExceeded")) {
                throw new ApiException("YouTube API daily quota exceeded on Google's side", HttpStatus.TOO_MANY_REQUESTS, "YOUTUBE_QUOTA_EXCEEDED");
            }
            throw new ApiException("Access forbidden by YouTube API", HttpStatus.FORBIDDEN, "YOUTUBE_ACCESS_FORBIDDEN");
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("YouTube API resource not found on uri: {}", uri);
            throw new ApiException("YouTube resource not found", HttpStatus.NOT_FOUND, "YOUTUBE_RESOURCE_NOT_FOUND");
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("YouTube API rate limited on uri: {}", uri);
            throw new ApiException("YouTube API rate limit reached", HttpStatus.TOO_MANY_REQUESTS, "YOUTUBE_RATE_LIMITED");
        } catch (HttpServerErrorException e) {
            log.error("YouTube API server error on uri {}: {}", uri, e.getStatusCode());
            throw new ApiException("YouTube external server error", HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException e) {
            log.error("YouTube API timeout / network failure: {}", e.getMessage());
            throw new ApiException("Connection timeout communicating with YouTube API", HttpStatus.GATEWAY_TIMEOUT, "YOUTUBE_TIMEOUT");
        } catch (Exception e) {
            log.error("Unexpected failure querying YouTube API on uri {}: {}", uri, e.getMessage());
            throw new ApiException("Failed to query YouTube API", HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_API_ERROR");
        }
    }
}
