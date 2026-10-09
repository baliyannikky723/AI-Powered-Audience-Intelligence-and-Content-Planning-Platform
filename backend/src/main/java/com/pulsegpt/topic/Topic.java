package com.pulsegpt.topic;

import com.pulsegpt.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "topics", indexes = {
        @Index(name = "idx_topics_user_id", columnList = "user_id"),
        @Index(name = "idx_topics_active", columnList = "active"),
        @Index(name = "idx_topics_name", columnList = "name"),
        @Index(name = "idx_topics_last_run", columnList = "last_clustering_run_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private List<String> keywords;

    @Column(name = "centroid", columnDefinition = "TEXT")
    private String centroid;

    @Column(length = 64)
    private String algorithm;

    @Column(name = "algorithm_version", length = 32)
    private String algorithmVersion;

    @Builder.Default
    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keyword_scores", columnDefinition = "jsonb")
    private Map<String, Double> keywordScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sentiment_distribution", columnDefinition = "jsonb")
    private Map<String, Integer> sentimentDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "intent_distribution", columnDefinition = "jsonb")
    private Map<String, Integer> intentDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "language_distribution", columnDefinition = "jsonb")
    private Map<String, Integer> languageDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "platform_distribution", columnDefinition = "jsonb")
    private Map<String, Integer> platformDistribution;

    @CreationTimestamp
    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @UpdateTimestamp
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_clustering_run_id")
    private ClusteringRun lastClusteringRun;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
