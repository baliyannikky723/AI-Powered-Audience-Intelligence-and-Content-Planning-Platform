package com.pulsegpt.evaluation.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PulseGptMetrics Unit Tests")
class PulseGptMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private PulseGptMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new PulseGptMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Record ingestion metrics increments counters and timers")
    void testIngestionMetrics() {
        metrics.recordIngestionRun("YOUTUBE", "SUCCESS", 1200L, 45);
        metrics.recordIngestionFailure("YOUTUBE");

        assertThat(meterRegistry.find("pulsegpt.ingestion.runs").counter()).isNotNull();
        assertThat(meterRegistry.find("pulsegpt.ingestion.runs").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pulsegpt.ingestion.comments").counter().count()).isEqualTo(45.0);
        assertThat(meterRegistry.find("pulsegpt.ingestion.failures").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Record NLP metrics increments processed, spam, dupes, pii counters")
    void testNlpMetrics() {
        metrics.recordNlpProcessed(50, 5, 2, 1, 350L);

        assertThat(meterRegistry.find("pulsegpt.nlp.comments.processed").counter().count()).isEqualTo(50.0);
        assertThat(meterRegistry.find("pulsegpt.nlp.spam.detected").counter().count()).isEqualTo(5.0);
        assertThat(meterRegistry.find("pulsegpt.nlp.duplicates.detected").counter().count()).isEqualTo(2.0);
        assertThat(meterRegistry.find("pulsegpt.nlp.pii.detected").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Record recommendation and validation metrics")
    void testRecommendationMetrics() {
        metrics.recordRecommendationGenerated("EVIDENCE_GROUNDED", 420L);
        metrics.recordRecommendationValidated("EVIDENCE_GROUNDED", true);
        metrics.recordRecommendationRepaired("EVIDENCE_GROUNDED", true);
        metrics.recordValidationFailure("HALLUCINATION");

        assertThat(meterRegistry.find("pulsegpt.recommendation.generated").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pulsegpt.recommendation.validated").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pulsegpt.validation.failures").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Record production and creator workflow metrics")
    void testProductionAndCreatorMetrics() {
        metrics.recordProductionGenerated("EVIDENCE_GROUNDED", "CONTENT_BRIEF", 250L);
        metrics.recordProductionValidated("EVIDENCE_GROUNDED", "CONTENT_BRIEF", true);
        metrics.recordProductionApproved("EVIDENCE_GROUNDED", "CONTENT_BRIEF");
        metrics.recordCreatorEdit();
        metrics.recordCreatorRevision();
        metrics.recordTimeToApproval(Duration.ofMinutes(15));
        metrics.recordExport("PDF", 120L, 54000L);

        assertThat(meterRegistry.find("pulsegpt.production.approved").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pulsegpt.creator.edits").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pulsegpt.creator.revisions").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("pulsegpt.export.count").counter().count()).isEqualTo(1.0);
    }
}
