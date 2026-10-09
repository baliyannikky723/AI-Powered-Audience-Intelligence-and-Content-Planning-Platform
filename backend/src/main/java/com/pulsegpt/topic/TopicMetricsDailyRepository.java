package com.pulsegpt.topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicMetricsDailyRepository extends JpaRepository<TopicMetricsDaily, UUID> {

    Optional<TopicMetricsDaily> findByTopicIdAndMetricDate(UUID topicId, LocalDate metricDate);

    List<TopicMetricsDaily> findByTopicIdOrderByMetricDateDesc(UUID topicId);

    List<TopicMetricsDaily> findByTopicIdAndMetricDateBetweenOrderByMetricDateAsc(UUID topicId, LocalDate start, LocalDate end);
}
