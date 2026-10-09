package com.pulsegpt.topic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicAssignmentRepository extends JpaRepository<TopicAssignment, UUID> {

    Page<TopicAssignment> findByTopicId(UUID topicId, Pageable pageable);

    @Query("SELECT ta FROM TopicAssignment ta WHERE ta.topic.id = :topicId AND ta.topic.user.id = :userId")
    Page<TopicAssignment> findByTopicIdAndUserId(@Param("topicId") UUID topicId, @Param("userId") UUID userId, Pageable pageable);

    Page<TopicAssignment> findByClusteringRunId(UUID clusteringRunId, Pageable pageable);

    @Query("SELECT ta FROM TopicAssignment ta WHERE ta.clusteringRun.id = :runId AND ta.clusteringRun.user.id = :userId")
    Page<TopicAssignment> findByClusteringRunIdAndUserId(@Param("runId") UUID runId, @Param("userId") UUID userId, Pageable pageable);

    Optional<TopicAssignment> findByClusteringRunIdAndProcessedCommentId(UUID clusteringRunId, UUID processedCommentId);

    List<TopicAssignment> findByTopicId(UUID topicId);

    @Query("SELECT ta FROM TopicAssignment ta WHERE ta.topic.user.id = :userId")
    List<TopicAssignment> findByUserId(@Param("userId") UUID userId);

    List<TopicAssignment> findByProcessedCommentId(UUID processedCommentId);

    long countByTopicId(UUID topicId);

    long countByClusteringRunIdAndIsNoiseTrue(UUID clusteringRunId);
}
