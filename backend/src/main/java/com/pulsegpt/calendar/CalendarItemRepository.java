package com.pulsegpt.calendar;

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
public interface CalendarItemRepository extends JpaRepository<CalendarItem, UUID>, JpaSpecificationExecutor<CalendarItem> {

    List<CalendarItem> findByUserIdAndScheduledAtBetweenOrderByScheduledAtAsc(UUID userId, Instant start, Instant end);

    List<CalendarItem> findByUserIdAndScheduledStartBetweenOrderByScheduledStartAsc(UUID userId, Instant start, Instant end);

    List<CalendarItem> findByUserId(UUID userId);

    Page<CalendarItem> findByUserId(UUID userId, Pageable pageable);

    Optional<CalendarItem> findByIdAndUserId(UUID id, UUID userId);

    List<CalendarItem> findByUserIdAndStatus(UUID userId, CalendarItemStatus status);

    long countByUserId(UUID userId);

    @Query("SELECT c FROM CalendarItem c WHERE c.user.id = :userId AND c.platform = :platform " +
           "AND c.status IN :activeStatuses " +
           "AND c.scheduledStart < :end AND c.scheduledEnd > :start " +
           "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    List<CalendarItem> findOverlappingItems(
            @Param("userId") UUID userId,
            @Param("platform") PlatformType platform,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("activeStatuses") List<CalendarItemStatus> activeStatuses,
            @Param("excludeId") UUID excludeId
    );

    @Query("SELECT c FROM CalendarItem c WHERE c.user.id = :userId AND c.topic.id = :topicId " +
           "AND c.status IN :activeStatuses " +
           "AND c.scheduledStart >= :since AND c.scheduledStart <= :until " +
           "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    List<CalendarItem> findRecentItemsByTopic(
            @Param("userId") UUID userId,
            @Param("topicId") UUID topicId,
            @Param("since") Instant since,
            @Param("until") Instant until,
            @Param("activeStatuses") List<CalendarItemStatus> activeStatuses,
            @Param("excludeId") UUID excludeId
    );

    boolean existsByUserIdAndRecommendationIdAndStatusIn(UUID userId, UUID recommendationId, List<CalendarItemStatus> activeStatuses);
}

