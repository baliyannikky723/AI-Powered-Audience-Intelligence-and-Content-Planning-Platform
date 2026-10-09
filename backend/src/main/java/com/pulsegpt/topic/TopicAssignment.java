package com.pulsegpt.topic;

import com.pulsegpt.comment.ProcessedComment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topic_assignments", indexes = {
        @Index(name = "idx_topic_assignments_topic_id", columnList = "topic_id"),
        @Index(name = "idx_topic_assignments_comment_id", columnList = "processed_comment_id"),
        @Index(name = "idx_topic_assignments_run_id", columnList = "clustering_run_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_assignment_run_comment", columnNames = {"clustering_run_id", "processed_comment_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_comment_id", nullable = false)
    private ProcessedComment processedComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clustering_run_id", nullable = false)
    private ClusteringRun clusteringRun;

    @Column(name = "cluster_id", nullable = false)
    private int clusterId;

    @Builder.Default
    @Column(name = "is_noise", nullable = false)
    private boolean isNoise = false;

    @Column(name = "membership_probability")
    private Double membershipProbability;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;
}
