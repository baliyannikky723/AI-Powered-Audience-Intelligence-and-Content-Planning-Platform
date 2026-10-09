package com.pulsegpt.evaluation.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.evaluation.DatasetSnapshot;
import com.pulsegpt.evaluation.ExperimentRun;
import com.pulsegpt.evaluation.dto.CompleteExperimentRequest;
import com.pulsegpt.evaluation.dto.CreateExperimentRequest;
import com.pulsegpt.evaluation.dto.CreateSnapshotRequest;
import com.pulsegpt.evaluation.dto.ResearchMetricsResponse;
import com.pulsegpt.evaluation.metrics.PulseGptMetrics;
import com.pulsegpt.evaluation.registry.ReproducibilityMetadata;
import com.pulsegpt.evaluation.service.ClusteringStabilityService;
import com.pulsegpt.evaluation.service.DatasetSnapshotService;
import com.pulsegpt.evaluation.service.ExperimentService;
import com.pulsegpt.evaluation.service.ProductionEvaluationService;
import com.pulsegpt.evaluation.service.RecommendationEvaluationService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/research")
@RequiredArgsConstructor
@Tag(name = "Research & Observability", description = "Endpoints for experiment tracking, evaluation, clustering stability, and research metrics")
public class ResearchController {

    private final ExperimentService experimentService;
    private final DatasetSnapshotService snapshotService;
    private final RecommendationEvaluationService recommendationEvaluationService;
    private final ProductionEvaluationService productionEvaluationService;
    private final ClusteringStabilityService clusteringStabilityService;
    private final PulseGptMetrics pulseGptMetrics;
    private final CurrentUserService currentUserService;

    @GetMapping("/metrics")
    @Operation(summary = "Get aggregated research and operational metrics")
    public ResponseEntity<ApiResponse<ResearchMetricsResponse>> getMetrics() {
        User user = currentUserService.requireUser();
        log.info("Fetching research metrics for user {}", user.getId());

        Map<String, Object> recMetrics = recommendationEvaluationService.getAggregatedRecommendationMetrics(user.getId());
        Map<String, Object> prodMetrics = productionEvaluationService.getAggregatedProductionMetrics(user.getId());

        ResearchMetricsResponse response = ResearchMetricsResponse.builder()
                .systemMetrics(Map.of(
                        "jvmUptimeSec", Runtime.getRuntime().totalMemory() / (1024 * 1024),
                        "availableProcessors", Runtime.getRuntime().availableProcessors(),
                        "freeMemoryMb", Runtime.getRuntime().freeMemory() / (1024 * 1024)
                ))
                .nlpMetrics(Map.of(
                        "status", "OPTIMAL",
                        "supportedLanguages", new String[]{"en", "hi", "hi-Latn"},
                        "embeddingDimension", 384
                ))
                .clusteringMetrics(Map.of(
                        "algorithm", "HDBSCAN",
                        "metric", "euclidean",
                        "minClusterSize", 3
                ))
                .recommendationMetrics(recMetrics)
                .productionMetrics(prodMetrics)
                .creatorWorkflowMetrics(Map.of(
                        "avgRevisionsPerAsset", prodMetrics.getOrDefault("avgRevisionCount", 1.0),
                        "creatorEditRate", prodMetrics.getOrDefault("creatorEditRate", 0.0),
                        "approvalRate", prodMetrics.getOrDefault("approvalRate", 0.0)
                ))
                .exportMetrics(Map.of(
                        "supportedFormatsCount", 7,
                        "pdfEngine", "OpenPDF",
                        "deterministicExport", true
                ))
                .reproducibilityMetadata(ReproducibilityMetadata.defaults().toMap())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/experiments")
    @Operation(summary = "Create an experiment run")
    public ResponseEntity<ApiResponse<ExperimentRun>> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request) {
        User user = currentUserService.requireUser();
        log.info("Creating experiment run {} of type {}", request.getExperimentName(), request.getExperimentType());
        ExperimentRun run = experimentService.createExperiment(
                request.getExperimentName(),
                request.getExperimentType(),
                request.getDescription(),
                request.getBaselineMode(),
                request.getTreatmentMode(),
                request.getDatasetSnapshotId(),
                request.getParameters(),
                user
        );
        return ResponseEntity.ok(ApiResponse.ok(run));
    }

    @GetMapping("/experiments")
    @Operation(summary = "List experiment runs")
    public ResponseEntity<ApiResponse<Page<ExperimentRun>>> getExperiments(
            @PageableDefault(size = 20) Pageable pageable) {
        User user = currentUserService.requireUser();
        Page<ExperimentRun> page = experimentService.getExperiments(user, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/experiments/{id}")
    @Operation(summary = "Get experiment run details")
    public ResponseEntity<ApiResponse<ExperimentRun>> getExperiment(
            @PathVariable UUID id) {
        User user = currentUserService.requireUser();
        ExperimentRun run = experimentService.getExperiment(id, user);
        return ResponseEntity.ok(ApiResponse.ok(run));
    }

    @PostMapping("/experiments/{id}/start")
    @Operation(summary = "Start an experiment run")
    public ResponseEntity<ApiResponse<ExperimentRun>> startExperiment(
            @PathVariable UUID id) {
        User user = currentUserService.requireUser();
        log.info("Starting experiment run {}", id);
        ExperimentRun run = experimentService.startExperiment(id, user);
        return ResponseEntity.ok(ApiResponse.ok(run));
    }

    @PostMapping("/experiments/{id}/complete")
    @Operation(summary = "Complete an experiment run with final metrics")
    public ResponseEntity<ApiResponse<ExperimentRun>> completeExperiment(
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteExperimentRequest request) {
        User user = currentUserService.requireUser();
        log.info("Completing experiment run {}", id);
        ExperimentRun run = experimentService.completeExperiment(
                id,
                request != null ? request.getMetrics() : null,
                user
        );
        return ResponseEntity.ok(ApiResponse.ok(run));
    }

    @GetMapping("/clustering/{runId}/stability")
    @Operation(summary = "Evaluate clustering stability between two runs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClusteringStability(
            @PathVariable UUID runId,
            @RequestParam(required = false) UUID compareRunId) {
        User user = currentUserService.requireUser();
        UUID secondRunId = compareRunId != null ? compareRunId : runId;
        Map<String, Object> stability = clusteringStabilityService.evaluateClusteringStability(runId, secondRunId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(stability));
    }

    @GetMapping("/recommendations/evaluation")
    @Operation(summary = "Get aggregated recommendation evaluation statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationEvaluation() {
        User user = currentUserService.requireUser();
        Map<String, Object> metrics = recommendationEvaluationService.getAggregatedRecommendationMetrics(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @GetMapping("/recommendations/{id}/evaluation")
    @Operation(summary = "Evaluate single recommendation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluateSingleRecommendation(
            @PathVariable UUID id) {
        User user = currentUserService.requireUser();
        Map<String, Object> metrics = recommendationEvaluationService.evaluateRecommendation(id, user);
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @GetMapping("/production/evaluation")
    @Operation(summary = "Get aggregated production copilot evaluation statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductionEvaluation() {
        User user = currentUserService.requireUser();
        Map<String, Object> metrics = productionEvaluationService.getAggregatedProductionMetrics(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @GetMapping("/production/{id}/evaluation")
    @Operation(summary = "Evaluate single production asset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluateSingleProductionAsset(
            @PathVariable UUID id) {
        User user = currentUserService.requireUser();
        Map<String, Object> metrics = productionEvaluationService.evaluateProductionAsset(id, user);
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @PostMapping("/snapshots")
    @Operation(summary = "Create a dataset snapshot for research reproducibility")
    public ResponseEntity<ApiResponse<DatasetSnapshot>> createSnapshot(
            @Valid @RequestBody CreateSnapshotRequest request) {
        User user = currentUserService.requireUser();
        log.info("Creating dataset snapshot {}", request.getName());
        DatasetSnapshot snapshot = snapshotService.createSnapshot(
                request.getName(),
                request.getDescription(),
                request.getPlatform(),
                request.getDateFrom(),
                request.getDateTo(),
                user
        );
        return ResponseEntity.ok(ApiResponse.ok(snapshot));
    }

    @GetMapping("/snapshots")
    @Operation(summary = "List dataset snapshots")
    public ResponseEntity<ApiResponse<Page<DatasetSnapshot>>> getSnapshots(
            @PageableDefault(size = 20) Pageable pageable) {
        User user = currentUserService.requireUser();
        Page<DatasetSnapshot> page = snapshotService.getSnapshots(user, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
