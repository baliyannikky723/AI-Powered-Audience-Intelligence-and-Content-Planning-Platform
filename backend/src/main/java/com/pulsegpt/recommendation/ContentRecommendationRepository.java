package com.pulsegpt.recommendation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRecommendationRepository extends JpaRepository<ContentRecommendation, UUID>, JpaSpecificationExecutor<ContentRecommendation> {

    List<ContentRecommendation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<ContentRecommendation> findByUserId(UUID userId, Pageable pageable);

    Page<ContentRecommendation> findByUserIdAndMode(UUID userId, RecommendationMode mode, Pageable pageable);

    Optional<ContentRecommendation> findByIdAndUserId(UUID id, UUID userId);

    List<ContentRecommendation> findByUserIdAndTopicId(UUID userId, UUID topicId);

    long countByUserId(UUID userId);
}
