package com.pulsegpt.topic;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "topic_metrics_daily", indexes = {
        @Index(name = "idx_topic_metrics_topic_id", columnList = "topic_id"),
        @Index(name = "idx_topic_metrics_date", columnList = "metric_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_topic_metrics_topic_date", columnNames = {"topic_id", "metric_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicMetricsDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Builder.Default
    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Builder.Default
    @Column(name = "unique_comment_count", nullable = false)
    private int uniqueCommentCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private long engagement = 0L;

    @Column(name = "average_sentiment")
    private Double averageSentiment;

    @Column(name = "centroid_similarity")
    private Double centroidSimilarity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
