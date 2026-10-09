package com.pulsegpt.evaluation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class PulseGptMetrics {

    private final MeterRegistry meterRegistry;

    // NLP Counters
    private final Counter nlpCommentsProcessed;
    private final Counter nlpSpamDetected;
    private final Counter nlpDuplicatesDetected;
    private final Counter nlpPiiDetected;
    private final Timer nlpProcessingDuration;

    // Clustering
    private final Counter clusteringRuns;
    private final Timer clusteringDuration;

    // Validation
    private final Timer recommendationValidationDuration;
    private final Timer productionValidationDuration;

    // Creator Workflow
    private final Counter creatorRevisions;
    private final Counter creatorEdits;
    private final Timer creatorTimeToApproval;

    public PulseGptMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // NLP
        this.nlpCommentsProcessed = Counter.builder("pulsegpt.nlp.comments.processed")
                .description("Total audience comments processed through NLP pipeline")
                .register(meterRegistry);

        this.nlpSpamDetected = Counter.builder("pulsegpt.nlp.spam.detected")
                .description("Total spam/promotional comments flagged")
                .register(meterRegistry);

        this.nlpDuplicatesDetected = Counter.builder("pulsegpt.nlp.duplicates.detected")
                .description("Total near-duplicate comments detected")
                .register(meterRegistry);

        this.nlpPiiDetected = Counter.builder("pulsegpt.nlp.pii.detected")
                .description("Total PII items scrubbed or detected")
                .register(meterRegistry);

        this.nlpProcessingDuration = Timer.builder("pulsegpt.nlp.processing.duration")
                .description("Latency of NLP pipeline processing per batch")
                .register(meterRegistry);

        // Clustering
        this.clusteringRuns = Counter.builder("pulsegpt.clustering.runs")
                .description("Total topic clustering executions")
                .register(meterRegistry);

        this.clusteringDuration = Timer.builder("pulsegpt.clustering.duration")
                .description("Latency of topic clustering execution")
                .register(meterRegistry);

        // Validation Timers
        this.recommendationValidationDuration = Timer.builder("pulsegpt.recommendation.validation.duration")
                .description("Latency of recommendation validation checks")
                .register(meterRegistry);

        this.productionValidationDuration = Timer.builder("pulsegpt.production.validation.duration")
                .description("Latency of production asset validation checks")
                .register(meterRegistry);

        // Creator Workflow
        this.creatorRevisions = Counter.builder("pulsegpt.creator.revisions")
                .description("Total revisions made across production assets")
                .register(meterRegistry);

        this.creatorEdits = Counter.builder("pulsegpt.creator.edits")
                .description("Total creator manual edits performed")
                .register(meterRegistry);

        this.creatorTimeToApproval = Timer.builder("pulsegpt.creator.time_to_approval")
                .description("Time taken from generation to creator approval")
                .register(meterRegistry);
    }

    // --- Ingestion ---
    public void recordIngestionRun(String platform, String status, long durationMs, int commentsCount) {
        Counter.builder("pulsegpt.ingestion.runs")
                .tag("platform", platform)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        Counter.builder("pulsegpt.ingestion.comments")
                .tag("platform", platform)
                .register(meterRegistry)
                .increment(commentsCount);

        Timer.builder("pulsegpt.ingestion.duration")
                .tag("platform", platform)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordIngestionFailure(String platform) {
        Counter.builder("pulsegpt.ingestion.failures")
                .tag("platform", platform)
                .register(meterRegistry)
                .increment();
    }

    // --- NLP ---
    public void recordNlpProcessed(int count, int spam, int dupes, int pii, long durationMs) {
        nlpCommentsProcessed.increment(count);
        if (spam > 0) nlpSpamDetected.increment(spam);
        if (dupes > 0) nlpDuplicatesDetected.increment(dupes);
        if (pii > 0) nlpPiiDetected.increment(pii);
        nlpProcessingDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // --- Clustering ---
    public void recordClusteringRun(long durationMs) {
        clusteringRuns.increment();
        clusteringDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // --- Recommendations ---
    public void recordRecommendationGenerated(String mode, long durationMs) {
        Counter.builder("pulsegpt.recommendation.generated")
                .tag("generationMode", mode)
                .register(meterRegistry)
                .increment();

        Timer.builder("pulsegpt.recommendation.generation.duration")
                .tag("generationMode", mode)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRecommendationValidated(String mode, boolean passed) {
        if (passed) {
            Counter.builder("pulsegpt.recommendation.validated")
                    .tag("generationMode", mode)
                    .register(meterRegistry)
                    .increment();
        } else {
            Counter.builder("pulsegpt.recommendation.rejected")
                    .tag("generationMode", mode)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordRecommendationRepaired(String mode, boolean success) {
        Counter.builder("pulsegpt.recommendation.repaired")
                .tag("generationMode", mode)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    public void recordRecommendationValidationDuration(long durationMs) {
        recommendationValidationDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // --- Validation Checks ---
    public void recordValidationFailure(String checkName) {
        Counter.builder("pulsegpt.validation.failures")
                .tag("checkName", checkName)
                .register(meterRegistry)
                .increment();
    }

    // --- Production Copilot ---
    public void recordProductionGenerated(String mode, String assetType, long durationMs) {
        Counter.builder("pulsegpt.production.generated")
                .tag("generationMode", mode)
                .tag("assetType", assetType)
                .register(meterRegistry)
                .increment();

        Timer.builder("pulsegpt.production.generation.duration")
                .tag("generationMode", mode)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordProductionValidated(String mode, String assetType, boolean passed) {
        if (passed) {
            Counter.builder("pulsegpt.production.validated")
                    .tag("generationMode", mode)
                    .tag("assetType", assetType)
                    .register(meterRegistry)
                    .increment();
        } else {
            Counter.builder("pulsegpt.production.rejected")
                    .tag("generationMode", mode)
                    .tag("assetType", assetType)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordProductionRepaired(String mode, String assetType, boolean success) {
        Counter.builder("pulsegpt.production.repaired")
                .tag("generationMode", mode)
                .tag("assetType", assetType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    public void recordProductionApproved(String mode, String assetType) {
        Counter.builder("pulsegpt.production.approved")
                .tag("generationMode", mode)
                .tag("assetType", assetType)
                .register(meterRegistry)
                .increment();
    }

    public void recordProductionValidationDuration(long durationMs) {
        productionValidationDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // --- Creator Workflow ---
    public void recordCreatorEdit() {
        creatorEdits.increment();
    }

    public void recordCreatorRevision() {
        creatorRevisions.increment();
    }

    public void recordTimeToApproval(Duration duration) {
        creatorTimeToApproval.record(duration);
    }

    // --- Export Metrics ---
    public void recordExport(String exportType, long durationMs, long bytes) {
        Counter.builder("pulsegpt.export.count")
                .tag("exportType", exportType)
                .register(meterRegistry)
                .increment();

        Timer.builder("pulsegpt.export.duration")
                .tag("exportType", exportType)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        Counter.builder("pulsegpt.export.bytes")
                .tag("exportType", exportType)
                .register(meterRegistry)
                .increment(bytes);
    }

    public void recordExportFailure(String exportType) {
        Counter.builder("pulsegpt.export.failures")
                .tag("exportType", exportType)
                .register(meterRegistry)
                .increment();
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
