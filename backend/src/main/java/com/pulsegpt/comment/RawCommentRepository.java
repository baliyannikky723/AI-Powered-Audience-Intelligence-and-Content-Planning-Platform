package com.pulsegpt.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawCommentRepository extends JpaRepository<RawComment, UUID>, JpaSpecificationExecutor<RawComment> {

    Optional<RawComment> findByPostIdAndExternalCommentId(UUID postId, String externalCommentId);

    boolean existsByPostIdAndExternalCommentId(UUID postId, String externalCommentId);

    Page<RawComment> findByPostId(UUID postId, Pageable pageable);

    List<RawComment> findByPostIdAndPublishedAtBetween(UUID postId, Instant start, Instant end);

    long countByPostId(UUID postId);

    Optional<RawComment> findByIdAndPostPlatformAccountUserId(UUID id, UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM RawComment r WHERE r.post.platformAccount.user.id = :userId " +
            "AND NOT EXISTS (SELECT 1 FROM ProcessedComment p WHERE p.rawComment.id = r.id) " +
            "ORDER BY r.publishedAt DESC")
    List<RawComment> findUnprocessedByUserId(@org.springframework.data.repository.query.Param("userId") UUID userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM RawComment r WHERE r.post.platformAccount.user.id = :userId " +
            "AND r.post.platformAccount.platform = :platform " +
            "AND NOT EXISTS (SELECT 1 FROM ProcessedComment p WHERE p.rawComment.id = r.id) " +
            "ORDER BY r.publishedAt DESC")
    List<RawComment> findUnprocessedByUserIdAndPlatform(@org.springframework.data.repository.query.Param("userId") UUID userId,
                                                        @org.springframework.data.repository.query.Param("platform") com.pulsegpt.platform.PlatformType platform,
                                                        Pageable pageable);
}
