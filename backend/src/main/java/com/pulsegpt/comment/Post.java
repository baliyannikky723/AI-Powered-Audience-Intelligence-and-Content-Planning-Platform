package com.pulsegpt.comment;

import com.pulsegpt.platform.PlatformAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_platform_account_id", columnList = "platform_account_id"),
        @Index(name = "idx_posts_external_post_id", columnList = "external_post_id"),
        @Index(name = "idx_posts_published_at", columnList = "published_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_posts_platform_account_ext_id", columnNames = {"platform_account_id", "external_post_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_account_id", nullable = false)
    private PlatformAccount platformAccount;

    @Column(name = "external_post_id", nullable = false)
    private String externalPostId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Builder.Default
    @Column(name = "views_count")
    private Long viewsCount = 0L;

    @Builder.Default
    @Column(name = "likes_count")
    private Long likesCount = 0L;

    @Builder.Default
    @Column(name = "comments_count")
    private Long commentsCount = 0L;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
