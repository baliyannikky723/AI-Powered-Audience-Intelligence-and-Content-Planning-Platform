package com.pulsegpt.trend;

import com.pulsegpt.topic.Topic;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trend_scores", indexes = {
        @Index(name = "idx_trend_scores_topic_id", columnList = "topic_id"),
        @Index(name = "idx_trend_scores_date", columnList = "metric_date"),
        @Index(name = "idx_trend_scores_status", columnList = "status"),
        @Index(name = "idx_trend_scores_total_score", columnList = "total_score")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_trend_scores_topic_date", columnNames = {"topic_id", "metric_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Builder.Default
    @Column(name = "frequency_score", nullable = false)
    private double frequencyScore = 0.0;

    @Builder.Default
    @Column(name = "growth_score", nullable = false)
    private double growthScore = 0.0;

    @Builder.Default
    @Column(name = "recency_score", nullable = false)
    private double recencyScore = 0.0;

    @Builder.Default
    @Column(name = "engagement_score", nullable = false)
    private double engagementScore = 0.0;

    @Builder.Default
    @Column(name = "consistency_score", nullable = false)
    private double consistencyScore = 0.0;

    @Builder.Default
    @Column(name = "total_score", nullable = false)
    private double totalScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TrendStatus status;

    @Column(name = "algorithm_version", length = 32)
    private String algorithmVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
