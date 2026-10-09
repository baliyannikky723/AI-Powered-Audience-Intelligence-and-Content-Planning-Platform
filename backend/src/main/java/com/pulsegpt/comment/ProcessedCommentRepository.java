package com.pulsegpt.comment;

import com.pulsegpt.platform.PlatformType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedCommentRepository extends JpaRepository<ProcessedComment, UUID>, JpaSpecificationExecutor<ProcessedComment> {

    Optional<ProcessedComment> findByRawCommentId(UUID rawCommentId);

    Page<ProcessedComment> findBySentimentLabel(SentimentLabel sentimentLabel, Pageable pageable);

    Page<ProcessedComment> findByIntent(IntentType intent, Pageable pageable);

    Page<ProcessedComment> findByLanguage(String language, Pageable pageable);

    List<ProcessedComment> findByProcessedAtBetween(Instant start, Instant end);

    long countBySentimentLabel(SentimentLabel sentimentLabel);

    Optional<ProcessedComment> findByIdAndRawCommentPostPlatformAccountUserId(UUID id, UUID userId);

    @Query("SELECT pc FROM ProcessedComment pc " +
           "WHERE pc.rawComment.post.platformAccount.user.id = :userId " +
           "AND pc.embedding IS NOT NULL " +
           "AND pc.isSpam = false " +
           "ORDER BY pc.rawComment.publishedAt DESC")
    List<ProcessedComment> findEligibleForClusteringByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT pc FROM ProcessedComment pc " +
           "WHERE pc.rawComment.post.platformAccount.user.id = :userId " +
           "AND pc.rawComment.post.platformAccount.platform = :platform " +
           "AND pc.embedding IS NOT NULL " +
           "AND pc.isSpam = false " +
           "ORDER BY pc.rawComment.publishedAt DESC")
    List<ProcessedComment> findEligibleForClusteringByUserIdAndPlatform(
            @Param("userId") UUID userId,
            @Param("platform") PlatformType platform,
            Pageable pageable);

    @Query("SELECT pc FROM ProcessedComment pc " +
           "WHERE pc.rawComment.post.platformAccount.user.id = :userId " +
           "AND pc.rawComment.publishedAt >= :start " +
           "AND pc.rawComment.publishedAt <= :end " +
           "AND pc.embedding IS NOT NULL " +
           "AND pc.isSpam = false " +
           "ORDER BY pc.rawComment.publishedAt DESC")
    List<ProcessedComment> findEligibleForClusteringByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    @Query("SELECT pc FROM ProcessedComment pc " +
           "WHERE pc.rawComment.post.platformAccount.user.id = :userId " +
           "AND pc.rawComment.post.platformAccount.platform = :platform " +
           "AND pc.rawComment.publishedAt >= :start " +
           "AND pc.rawComment.publishedAt <= :end " +
           "AND pc.embedding IS NOT NULL " +
           "AND pc.isSpam = false " +
           "ORDER BY pc.rawComment.publishedAt DESC")
    List<ProcessedComment> findEligibleForClusteringByUserIdPlatformAndDateRange(
            @Param("userId") UUID userId,
            @Param("platform") PlatformType platform,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);
}
