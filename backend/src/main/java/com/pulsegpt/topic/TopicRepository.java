package com.pulsegpt.topic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID>, JpaSpecificationExecutor<Topic> {

    List<Topic> findByUserIdAndActiveTrue(UUID userId);

    List<Topic> findByUserId(UUID userId);

    Page<Topic> findByUserId(UUID userId, Pageable pageable);

    Optional<Topic> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    long countByUserIdAndActiveTrue(UUID userId);
}
