package com.pulsegpt.rag.model;

import com.pulsegpt.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_annotations", indexes = {
        @Index(name = "idx_evidence_annotations_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_evidence_annotations_query_id", columnList = "query_id"),
        @Index(name = "idx_evidence_annotations_evidence_id", columnList = "evidence_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "query_id", nullable = false, length = 128)
    private String queryId;

    @Column(name = "evidence_id", nullable = false, length = 128)
    private String evidenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relevance", nullable = false, length = 32)
    private RagRelevance relevance;

    @Enumerated(EnumType.STRING)
    @Column(name = "correctness", nullable = false, length = 32)
    private RagCorrectness correctness;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
