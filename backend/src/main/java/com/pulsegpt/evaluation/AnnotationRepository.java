package com.pulsegpt.evaluation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, UUID> {

    List<Annotation> findByProcessedCommentId(UUID processedCommentId);

    Page<Annotation> findByLabelType(AnnotationLabelType labelType, Pageable pageable);

    List<Annotation> findByAnnotatorId(UUID annotatorId);
}
