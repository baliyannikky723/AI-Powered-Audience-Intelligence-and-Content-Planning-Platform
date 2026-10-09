package com.pulsegpt.evaluation.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.evaluation.ExperimentRun;
import com.pulsegpt.evaluation.ExperimentRunRepository;
import com.pulsegpt.evaluation.registry.ReproducibilityMetadata;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentRunRepository experimentRunRepository;
    private final RecommendationEvaluationService recommendationEvaluationService;
    private final ProductionEvaluationService productionEvaluationService;
    private final AuditService auditService;

    @Transactional
    public ExperimentRun createExperiment(String name, String type, String description,
                                          String baselineMode, String treatmentMode,
                                          UUID datasetSnapshotId, Map<String, Object> parameters,
                                          User user) {
        ReproducibilityMetadata meta = ReproducibilityMetadata.defaults();

        ExperimentRun experiment = ExperimentRun.builder()
                .user(user)
                .experimentName(name)
                .experimentType(type)
                .description(description)
                .status("CREATED")
                .baselineMode(baselineMode != null ? baselineMode : "BASELINE")
                .treatmentMode(treatmentMode != null ? treatmentMode : "EVIDENCE_GROUNDED")
                .datasetSnapshotId(datasetSnapshotId)
                .configuration(parameters != null ? parameters : Map.of())
                .modelVersion(meta.getModelVersion())
                .promptVersion(meta.getPromptVersion())
                .algorithmVersion(meta.getAlgorithmVersion())
                .seed(meta.getRandomSeed())
                .metrics(Map.of())
                .build();

        return experimentRunRepository.save(experiment);
    }

    @Transactional
    public ExperimentRun startExperiment(UUID id, User user) {
        ExperimentRun exp = experimentRunRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Experiment run not found: " + id));

        exp.setStatus("RUNNING");
        exp.setStartedAt(Instant.now());
        return experimentRunRepository.save(exp);
    }

    @Transactional
    public ExperimentRun completeExperiment(UUID id, Map<String, Object> completedMetrics, User user) {
        ExperimentRun exp = experimentRunRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Experiment run not found: " + id));

        exp.setStatus("COMPLETED");
        exp.setCompletedAt(Instant.now());

        Map<String, Object> finalMetrics = new HashMap<>();
        if (exp.getMetrics() != null) {
            finalMetrics.putAll(exp.getMetrics());
        }
        if (completedMetrics != null) {
            finalMetrics.putAll(completedMetrics);
        } else {
            // Aggregate live metrics
            Map<String, Object> recAgg = recommendationEvaluationService.getAggregatedRecommendationMetrics(user.getId());
            Map<String, Object> prodAgg = productionEvaluationService.getAggregatedProductionMetrics(user.getId());
            finalMetrics.put("recommendationMetrics", recAgg);
            finalMetrics.put("productionMetrics", prodAgg);
        }

        exp.setMetrics(finalMetrics);
        return experimentRunRepository.save(exp);
    }

    @Transactional(readOnly = true)
    public Page<ExperimentRun> getExperiments(User user, Pageable pageable) {
        return experimentRunRepository.findByUserId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public ExperimentRun getExperiment(UUID id, User user) {
        return experimentRunRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Experiment run not found: " + id));
    }
}
