package com.pulsegpt.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {

    Page<Post> findByPlatformAccountId(UUID platformAccountId, Pageable pageable);

    Optional<Post> findByPlatformAccountIdAndExternalPostId(UUID platformAccountId, String externalPostId);

    boolean existsByPlatformAccountIdAndExternalPostId(UUID platformAccountId, String externalPostId);

    Optional<Post> findByIdAndPlatformAccountUserId(UUID id, UUID userId);

    Page<Post> findByPlatformAccountUserId(UUID userId, Pageable pageable);

    List<Post> findByPlatformAccountUserIdOrderByPublishedAtDesc(UUID userId, Pageable pageable);
}
