package com.pulsegpt.trend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendScoreRepository extends JpaRepository<TrendScore, UUID> {

    Optional<TrendScore> findByTopicIdAndMetricDate(UUID topicId, LocalDate metricDate);

    List<TrendScore> findByTopicIdOrderByMetricDateDesc(UUID topicId);

    List<TrendScore> findByTopicIdAndMetricDateBetweenOrderByMetricDateAsc(UUID topicId, LocalDate start, LocalDate end);

    List<TrendScore> findByStatusAndMetricDate(TrendStatus status, LocalDate metricDate);
}
