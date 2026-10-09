package com.pulsegpt.platform.youtube.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.PostRepository;
import com.pulsegpt.comment.RawComment;
import com.pulsegpt.comment.RawCommentRepository;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.ingestion.IngestionRun;
import com.pulsegpt.ingestion.IngestionRunRepository;
import com.pulsegpt.ingestion.IngestionRunStatus;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountRepository;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.platform.youtube.client.YouTubeApiClient;
import com.pulsegpt.platform.youtube.client.dto.YouTubeChannelResponse;
import com.pulsegpt.platform.youtube.client.dto.YouTubeCommentThreadListResponse;
import com.pulsegpt.platform.youtube.client.dto.YouTubeVideoListResponse;
import com.pulsegpt.platform.youtube.dto.IngestionSummaryResponse;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeIngestionService {

    private final PlatformAccountRepository platformAccountRepository;
    private final IngestionRunRepository ingestionRunRepository;
    private final PostRepository postRepository;
    private final RawCommentRepository rawCommentRepository;
    private final YouTubeTokenService youTubeTokenService;
    private final YouTubeApiClient youTubeApiClient;
    private final QuotaService quotaService;
    private final AppProperties appProperties;
    private final AuditService auditService;

    @Transactional
    public IngestionSummaryResponse sync(User currentUser) {
        PlatformAccount account = platformAccountRepository.findByUserId(currentUser.getId()).stream()
                .filter(a -> a.getPlatform() == PlatformType.YOUTUBE && a.getStatus() == PlatformAccountStatus.CONNECTED)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No connected YouTube account found for user: " + currentUser.getId()));

        // 1. Concurrent Ingestion Protection
        if (ingestionRunRepository.existsByPlatformAccountIdAndStatus(account.getId(), IngestionRunStatus.STARTED)) {
            log.warn("Ingestion sync rejected: already running for account: {}", account.getId());
            throw new ApiException("An ingestion sync is already in progress for this YouTube account",
                    HttpStatus.CONFLICT, "INGESTION_ALREADY_RUNNING");
        }

        // 2. Initialize Ingestion Run
        IngestionRun run = IngestionRun.builder()
                .platformAccount(account)
                .status(IngestionRunStatus.STARTED)
                .startedAt(Instant.now())
                .build();
        run = ingestionRunRepository.save(run);

        auditService.logAuthEvent(currentUser, "YOUTUBE_SYNC_STARTED", account.getId().toString(),
                Map.of("runId", run.getId().toString()));

        int postsProcessed = 0;
        int duplicatePosts = 0;
        int commentsProcessed = 0;
        int duplicateComments = 0;
        int itemsFailed = 0;

        try {
            // 3. Obtain valid access token (auto-refreshed if expired)
            String accessToken = youTubeTokenService.getValidAccessToken(account);

            // 4. Determine Uploads Playlist ID
            String uploadsPlaylistId = null;
            if (account.getMetadata() != null && account.getMetadata().containsKey("uploadsPlaylistId")) {
                uploadsPlaylistId = (String) account.getMetadata().get("uploadsPlaylistId");
            }
            if (uploadsPlaylistId == null || uploadsPlaylistId.isBlank()) {
                quotaService.checkQuotaAvailable(account, QuotaService.COST_CHANNEL_LIST);
                YouTubeChannelResponse channelResponse = youTubeApiClient.getMyChannel(accessToken);
                quotaService.recordQuotaUsage(account, "channels.list", QuotaService.COST_CHANNEL_LIST);

                if (channelResponse.items() != null && !channelResponse.items().isEmpty()) {
                    YouTubeChannelResponse.ChannelItem item = channelResponse.items().get(0);
                    if (item.contentDetails() != null && item.contentDetails().relatedPlaylists() != null) {
                        uploadsPlaylistId = item.contentDetails().relatedPlaylists().uploads();
                    }
                }
            }

            if (uploadsPlaylistId == null || uploadsPlaylistId.isBlank()) {
                throw new ApiException("Uploads playlist not found for YouTube channel", HttpStatus.BAD_REQUEST, "YOUTUBE_UPLOADS_PLAYLIST_NOT_FOUND");
            }

            // 5. Determine Lookback Timestamp for Incremental Sync
            Instant lookbackTime;
            if (account.getLastSyncedAt() == null) {
                int initialDays = appProperties.getYoutube().getInitialSyncDays();
                lookbackTime = Instant.now().minus(Duration.ofDays(initialDays));
                log.info("Performing INITIAL YouTube sync for account {} with lookback of {} days ({})",
                        account.getId(), initialDays, lookbackTime);
            } else {
                lookbackTime = account.getLastSyncedAt().minus(Duration.ofHours(1)); // 1h overlap buffer
                log.info("Performing INCREMENTAL YouTube sync for account {} since {}", account.getId(), lookbackTime);
            }

            // 6. Ingest Videos
            int maxVideos = appProperties.getYoutube().getMaxVideosPerSync();
            String videoPageToken = null;
            List<Post> ingestedPosts = new ArrayList<>();
            boolean hasMoreVideos = true;

            while (hasMoreVideos && ingestedPosts.size() < maxVideos) {
                quotaService.checkQuotaAvailable(account, QuotaService.COST_PLAYLIST_ITEMS_LIST);
                YouTubeVideoListResponse playlistResponse = youTubeApiClient.getPlaylistVideos(
                        uploadsPlaylistId, videoPageToken, Math.min(50, maxVideos - ingestedPosts.size()), accessToken);
                quotaService.recordQuotaUsage(account, "playlistItems.list", QuotaService.COST_PLAYLIST_ITEMS_LIST);

                if (playlistResponse.items() == null || playlistResponse.items().isEmpty()) {
                    break;
                }

                List<String> videoIdsToFetch = new ArrayList<>();
                Map<String, YouTubeVideoListResponse.VideoItem> playlistItemsMap = new HashMap<>();

                for (YouTubeVideoListResponse.VideoItem item : playlistResponse.items()) {
                    if (item.snippet() != null && item.snippet().resourceId() != null) {
                        String videoId = item.snippet().resourceId().videoId();
                        if (videoId != null && !videoId.isBlank()) {
                            Instant publishedAt = parseInstantSafe(item.snippet().publishedAt());
                            if (publishedAt != null && publishedAt.isBefore(lookbackTime)) {
                                hasMoreVideos = false; // Uploads are sorted newest first; older than lookback can stop
                                break;
                            }
                            videoIdsToFetch.add(videoId);
                            playlistItemsMap.put(videoId, item);
                        }
                    }
                }

                // Batch fetch video details & metrics (viewsCount, likesCount, commentsCount)
                if (!videoIdsToFetch.isEmpty()) {
                    quotaService.checkQuotaAvailable(account, QuotaService.COST_VIDEOS_LIST);
                    YouTubeVideoListResponse detailsResponse = youTubeApiClient.getVideosByIds(videoIdsToFetch, accessToken);
                    quotaService.recordQuotaUsage(account, "videos.list", QuotaService.COST_VIDEOS_LIST);

                    Map<String, YouTubeVideoListResponse.VideoItem> detailsMap = new HashMap<>();
                    if (detailsResponse.items() != null) {
                        for (YouTubeVideoListResponse.VideoItem detail : detailsResponse.items()) {
                            detailsMap.put(detail.id(), detail);
                        }
                    }

                    for (String videoId : videoIdsToFetch) {
                        try {
                            YouTubeVideoListResponse.VideoItem pItem = playlistItemsMap.get(videoId);
                            YouTubeVideoListResponse.VideoItem dItem = detailsMap.get(videoId);

                            String title = pItem.snippet() != null ? pItem.snippet().title() : "Untitled Video";
                            Instant publishedAt = parseInstantSafe(pItem.snippet() != null ? pItem.snippet().publishedAt() : null);
                            if (publishedAt == null) {
                                publishedAt = Instant.now();
                            }

                            Long views = 0L;
                            Long likes = 0L;
                            Long comments = 0L;
                            if (dItem != null && dItem.statistics() != null) {
                                views = parseLongSafe(dItem.statistics().viewCount());
                                likes = parseLongSafe(dItem.statistics().likeCount());
                                comments = parseLongSafe(dItem.statistics().commentCount());
                            }

                            Optional<Post> existingPostOpt = postRepository.findByPlatformAccountIdAndExternalPostId(account.getId(), videoId);
                            Post post;
                            if (existingPostOpt.isPresent()) {
                                post = existingPostOpt.get();
                                post.setTitle(title);
                                post.setViewsCount(views);
                                post.setLikesCount(likes);
                                post.setCommentsCount(comments);
                                post.setUpdatedAt(Instant.now());
                                duplicatePosts++;
                            } else {
                                post = Post.builder()
                                        .platformAccount(account)
                                        .externalPostId(videoId)
                                        .title(title)
                                        .url("https://www.youtube.com/watch?v=" + videoId)
                                        .publishedAt(publishedAt)
                                        .viewsCount(views)
                                        .likesCount(likes)
                                        .commentsCount(comments)
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build();
                                postsProcessed++;
                            }
                            post = postRepository.save(post);
                            ingestedPosts.add(post);
                        } catch (Exception ex) {
                            log.error("Failed to ingest video {}: {}", videoId, ex.getMessage());
                            itemsFailed++;
                        }
                    }
                }

                videoPageToken = playlistResponse.nextPageToken();
                if (videoPageToken == null || videoPageToken.isBlank()) {
                    hasMoreVideos = false;
                }
            }

            // 7. Ingest Comments for Each Video
            int maxCommentsPerVideo = appProperties.getYoutube().getMaxCommentsPerVideo();

            for (Post post : ingestedPosts) {
                String commentPageToken = null;
                int videoCommentsFetched = 0;
                boolean hasMoreComments = true;

                while (hasMoreComments && videoCommentsFetched < maxCommentsPerVideo) {
                    try {
                        quotaService.checkQuotaAvailable(account, QuotaService.COST_COMMENT_THREADS_LIST);
                        YouTubeCommentThreadListResponse threadResponse = youTubeApiClient.getCommentThreads(
                                post.getExternalPostId(), commentPageToken, Math.min(100, maxCommentsPerVideo - videoCommentsFetched), accessToken);
                        quotaService.recordQuotaUsage(account, "commentThreads.list", QuotaService.COST_COMMENT_THREADS_LIST);

                        if (threadResponse.items() == null || threadResponse.items().isEmpty()) {
                            break;
                        }

                        for (YouTubeCommentThreadListResponse.CommentThreadItem thread : threadResponse.items()) {
                            if (thread.snippet() != null && thread.snippet().topLevelComment() != null) {
                                YouTubeCommentThreadListResponse.TopLevelComment top = thread.snippet().topLevelComment();
                                String commentId = top.id();
                                YouTubeCommentThreadListResponse.CommentSnippet snippet = top.snippet();

                                if (snippet != null && commentId != null) {
                                    String authorName = snippet.authorDisplayName();
                                    String authorExtId = snippet.authorChannelId() != null ? snippet.authorChannelId().value() : null;
                                    String rawText = snippet.textOriginal() != null ? snippet.textOriginal() : snippet.textDisplay();
                                    Instant publishedAt = parseInstantSafe(snippet.publishedAt());
                                    if (publishedAt == null) publishedAt = Instant.now();
                                    int likes = snippet.likeCount() != null ? snippet.likeCount().intValue() : 0;
                                    int replies = thread.snippet().totalReplyCount() != null ? thread.snippet().totalReplyCount().intValue() : 0;

                                    if (rawText != null && !rawText.isBlank()) {
                                        Optional<RawComment> existingCommentOpt = rawCommentRepository
                                                .findByPostIdAndExternalCommentId(post.getId(), commentId);

                                        if (existingCommentOpt.isPresent()) {
                                            RawComment existing = existingCommentOpt.get();
                                            existing.setLikes(likes);
                                            existing.setReplies(replies);
                                            rawCommentRepository.save(existing);
                                            duplicateComments++;
                                        } else {
                                            RawComment newComment = RawComment.builder()
                                                    .post(post)
                                                    .externalCommentId(commentId)
                                                    .authorDisplayName(authorName)
                                                    .authorExternalId(authorExtId)
                                                    .rawText(rawText)
                                                    .publishedAt(publishedAt)
                                                    .likes(likes)
                                                    .replies(replies)
                                                    .importedAt(Instant.now())
                                                    .build();
                                            rawCommentRepository.save(newComment);
                                            commentsProcessed++;
                                        }
                                        videoCommentsFetched++;
                                    }
                                }
                            }
                        }

                        commentPageToken = threadResponse.nextPageToken();
                        if (commentPageToken == null || commentPageToken.isBlank()) {
                            hasMoreComments = false;
                        }
                    } catch (ApiException e) {
                        if ("YOUTUBE_QUOTA_EXCEEDED".equals(e.getErrorCode())) {
                            log.warn("Quota limit reached while fetching comments. Halting comment sync.");
                            hasMoreComments = false;
                            break;
                        }
                        log.warn("Comment sync skipped for video {}: {}", post.getExternalPostId(), e.getMessage());
                        break;
                    } catch (Exception e) {
                        log.warn("Error fetching comments for video {}: {}", post.getExternalPostId(), e.getMessage());
                        break;
                    }
                }
            }

            // 8. Update Run & PlatformAccount on Success
            account.setLastSyncedAt(Instant.now());
            account.setStatus(PlatformAccountStatus.CONNECTED);
            platformAccountRepository.save(account);

            run.setStatus(IngestionRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            run.setFetchedCount(postsProcessed + duplicatePosts + commentsProcessed + duplicateComments);
            run.setInsertedCount(postsProcessed + commentsProcessed);
            run.setDuplicateCount(duplicatePosts + duplicateComments);
            run.setFailedCount(itemsFailed);
            ingestionRunRepository.save(run);

            auditService.logAuthEvent(currentUser, "YOUTUBE_SYNC_COMPLETED", account.getId().toString(),
                    Map.of("runId", run.getId().toString(),
                            "postsInserted", postsProcessed,
                            "commentsInserted", commentsProcessed,
                            "duplicatePosts", duplicatePosts,
                            "duplicateComments", duplicateComments));

            return new IngestionSummaryResponse(
                    run.getId(),
                    IngestionRunStatus.COMPLETED,
                    postsProcessed,
                    commentsProcessed,
                    duplicatePosts,
                    duplicateComments,
                    itemsFailed,
                    run.getStartedAt(),
                    run.getCompletedAt(),
                    "YouTube sync completed successfully"
            );

        } catch (Exception e) {
            log.error("YouTube sync failed for account {}: {}", account.getId(), e.getMessage(), e);

            run.setStatus(IngestionRunStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            run.setInsertedCount(postsProcessed + commentsProcessed);
            run.setDuplicateCount(duplicatePosts + duplicateComments);
            run.setFailedCount(itemsFailed + 1);
            ingestionRunRepository.save(run);

            account.setStatus(PlatformAccountStatus.CONNECTED);
            platformAccountRepository.save(account);

            auditService.logSecurityEvent(currentUser, "YOUTUBE_SYNC_FAILED", "INGESTION_RUN",
                    run.getId().toString(), e.getMessage());

            if (e instanceof ApiException apiEx) {
                throw apiEx;
            }
            throw new ApiException("YouTube sync operation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "YOUTUBE_SYNC_FAILED");
        }
    }

    private Instant parseInstantSafe(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return null;
        try {
            return Instant.parse(isoDate);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}
