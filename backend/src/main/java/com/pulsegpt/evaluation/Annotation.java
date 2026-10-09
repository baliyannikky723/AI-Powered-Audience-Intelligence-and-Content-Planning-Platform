package com.pulsegpt.evaluation;

import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "annotations", indexes = {
        @Index(name = "idx_annotations_processed_comment", columnList = "processed_comment_id"),
        @Index(name = "idx_annotations_annotator", columnList = "annotator_id"),
        @Index(name = "idx_annotations_label_type", columnList = "label_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_comment_id", nullable = false)
    private ProcessedComment processedComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annotator_id")
    private User annotator;

    @Enumerated(EnumType.STRING)
    @Column(name = "label_type", nullable = false, length = 64)
    private AnnotationLabelType labelType;

    @Column(nullable = false)
    private String label;

    private Double confidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
