package com.pulsegpt.recommendation;

import com.pulsegpt.comment.Priority;
import com.pulsegpt.topic.Topic;
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
@Table(name = "content_recommendations", indexes = {
        @Index(name = "idx_recommendations_user_id", columnList = "user_id"),
        @Index(name = "idx_recommendations_topic_id", columnList = "topic_id"),
        @Index(name = "idx_recommendations_status", columnList = "status"),
        @Index(name = "idx_recommendations_gen_mode", columnList = "generation_mode"),
        @Index(name = "idx_recommendations_priority", columnList = "priority"),
        @Index(name = "idx_recommendations_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "content_type", length = 64)
    private String contentType = "VIDEO";

    @Column(name = "angle", columnDefinition = "TEXT")
    private String angle;

    @Column(name = "target_audience")
    private String targetAudience;

    @Column(name = "problem_addressed", columnDefinition = "TEXT")
    private String problemAddress;

    @Column(name = "hook", columnDefinition = "TEXT")
    private String hook;

    @Column(name = "call_to_action", columnDefinition = "TEXT")
    private String callToAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_points", columnDefinition = "jsonb")
    private List<String> keyPoints;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Priority priority;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationStatus status = RecommendationStatus.GENERATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 32)
    private RecommendationMode mode;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false, length = 32)
    private GenerationMode generationMode = GenerationMode.EVIDENCE_GROUNDED;

    @Builder.Default
    @Column(name = "confidence")
    private Double confidence = 0.85;

    @Builder.Default
    @Column(name = "validation_passed")
    private Boolean validationPassed = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> evidenceSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draft_json", columnDefinition = "jsonb")
    private Map<String, Object> draftJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_json", columnDefinition = "jsonb")
    private Map<String, Object> validationJson;

    @Builder.Default
    @Column(name = "repair_attempted")
    private Boolean repairAttempted = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "repair_result_json", columnDefinition = "jsonb")
    private Map<String, Object> repairResultJson;

    @Column(name = "llm_model", length = 64)
    private String llmModel;

    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
