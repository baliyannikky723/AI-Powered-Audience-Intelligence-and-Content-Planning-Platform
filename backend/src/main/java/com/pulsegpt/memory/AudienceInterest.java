package com.pulsegpt.memory;

import com.pulsegpt.topic.Topic;
import com.pulsegpt.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audience_interests", indexes = {
        @Index(name = "idx_audience_interests_user_id", columnList = "user_id"),
        @Index(name = "idx_audience_interests_topic_id", columnList = "topic_id"),
        @Index(name = "idx_audience_interests_status", columnList = "status"),
        @Index(name = "idx_audience_interests_confidence", columnList = "confidence")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_audience_interests_user_topic", columnNames = {"user_id", "topic_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudienceInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Builder.Default
    @Column(nullable = false)
    private double confidence = 0.0;

    @Builder.Default
    @Column(name = "evidence_count", nullable = false)
    private int evidenceCount = 0;

    @Builder.Default
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Builder.Default
    @Column(name = "half_life_days", nullable = false)
    private int halfLifeDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AudienceInterestStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
