package com.pulsegpt.memory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AudienceInterestRepository extends JpaRepository<AudienceInterest, UUID> {

    Optional<AudienceInterest> findByUserIdAndTopicId(UUID userId, UUID topicId);

    List<AudienceInterest> findByUserIdAndStatus(UUID userId, AudienceInterestStatus status);

    Page<AudienceInterest> findByUserId(UUID userId, Pageable pageable);

    List<AudienceInterest> findByUserIdOrderByConfidenceDesc(UUID userId);
}
