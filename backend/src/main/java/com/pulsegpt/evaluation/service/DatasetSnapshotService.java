package com.pulsegpt.evaluation.service;

import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.comment.RawCommentRepository;
import com.pulsegpt.evaluation.DatasetSnapshot;
import com.pulsegpt.evaluation.DatasetSnapshotRepository;
import com.pulsegpt.evaluation.registry.ReproducibilityMetadata;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetSnapshotService {

    private final DatasetSnapshotRepository snapshotRepository;
    private final RawCommentRepository rawCommentRepository;
    private final ProcessedCommentRepository processedCommentRepository;

    @Transactional
    public DatasetSnapshot createSnapshot(String name, String description, String platform,
                                          Instant from, Instant to, User user) {
        long rawCount = rawCommentRepository.count();
        long processedCount = processedCommentRepository.count();

        ReproducibilityMetadata meta = ReproducibilityMetadata.defaults();

        DatasetSnapshot snapshot = DatasetSnapshot.builder()
                .user(user)
                .name(name)
                .description(description)
                .platform(platform != null ? platform : "YOUTUBE")
                .dateFrom(from)
                .dateTo(to != null ? to : Instant.now())
                .commentCount((int) rawCount)
                .processedCommentCount((int) processedCount)
                .embeddingModel(meta.getEmbeddingModel())
                .processingVersion(meta.getModelVersion())
                .clusteringVersion(meta.getAlgorithmVersion())
                .snapshotMetadata(Map.of(
                        "totalRaw", rawCount,
                        "totalProcessed", processedCount,
                        "embeddingDim", meta.getEmbeddingDimension(),
                        "seed", meta.getRandomSeed()
                ))
                .build();

        return snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public Page<DatasetSnapshot> getSnapshots(User user, Pageable pageable) {
        return snapshotRepository.findByUserId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public DatasetSnapshot getSnapshot(UUID id, User user) {
        return snapshotRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Dataset snapshot not found: " + id));
    }
}
