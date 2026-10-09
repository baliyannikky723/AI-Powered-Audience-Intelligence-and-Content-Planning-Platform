package com.pulsegpt.calendar;

import com.pulsegpt.comment.Priority;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_items", indexes = {
        @Index(name = "idx_calendar_items_user_id", columnList = "user_id"),
        @Index(name = "idx_calendar_items_scheduled_at", columnList = "scheduled_at"),
        @Index(name = "idx_calendar_items_scheduled_start", columnList = "scheduled_start"),
        @Index(name = "idx_calendar_items_status", columnList = "status"),
        @Index(name = "idx_calendar_items_platform", columnList = "platform"),
        @Index(name = "idx_calendar_items_topic_id", columnList = "topic_id"),
        @Index(name = "idx_calendar_items_recommendation_id", columnList = "recommendation_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private ContentRecommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "content_type", length = 64)
    private String contentType;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "scheduled_start")
    private Instant scheduledStart;

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CalendarItemStatus status = CalendarItemStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlatformType platform;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 32)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void syncScheduleFields() {
        if (scheduledStart != null && scheduledAt == null) {
            scheduledAt = scheduledStart;
        } else if (scheduledAt != null && scheduledStart == null) {
            scheduledStart = scheduledAt;
        }
        if (scheduledStart != null && scheduledEnd == null) {
            scheduledEnd = scheduledStart.plusSeconds(3600);
        }
    }
}
