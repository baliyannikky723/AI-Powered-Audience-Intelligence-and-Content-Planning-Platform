package com.pulsegpt.validation;

import com.pulsegpt.ai.client.dto.AiRecommendationDraft;
import com.pulsegpt.comment.Post;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.EvidenceSourceType;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.recommendation.dto.RecommendationGenerateRequest;
import com.pulsegpt.topic.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationValidatorTest {

    private RecommendationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RecommendationValidator();
    }

    private RecommendationContext createSampleContext(String validEvidenceId) {
        UUID topicId = UUID.randomUUID();
        EvidenceItem item = EvidenceItem.builder()
                .evidenceId(validEvidenceId)
                .sourceType(EvidenceSourceType.TOPIC)
                .sourceId(topicId)
                .userId(UUID.randomUUID())
                .summary("Frequently discusses battery life and background tasks in React Native")
                .relevanceScore(0.95)
                .createdAt(Instant.now())
                .metadata(Map.of("label", "Mobile Battery Optimization"))
                .build();

        Topic topic = Topic.builder()
                .id(topicId)
                .name("Mobile Battery Optimization")
                .keywords(List.of("battery", "background", "react native", "optimization"))
                .build();

        Post recentPost = Post.builder()
                .id(UUID.randomUUID())
                .title("Introduction to React Native 2026")
                .build();

        ContentRecommendation priorRec = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .title("How to Structure Redux Toolkit")
                .build();

        RecommendationGenerateRequest request = RecommendationGenerateRequest.builder()
                .topicId(topicId)
                .contentType("VIDEO")
                .goal("EDUCATIONAL")
                .maxIdeas(1)
                .mode(GenerationMode.EVIDENCE_GROUNDED)
                .build();

        return RecommendationContext.builder()
                .request(request)
                .evidenceItems(List.of(item))
                .topics(List.of(topic))
                .questions(List.of())
                .representativeComments(List.of())
                .contentHistory(List.of(recentPost))
                .previousRecommendations(List.of(priorRec))
                .build();
    }

    @Test
    @DisplayName("Should pass all 6 checks for a perfectly grounded recommendation draft")
    void testValidate_PassAllChecks() {
        String evidenceId = "topic:12345";
        RecommendationContext context = createSampleContext(evidenceId);

        AiRecommendationDraft draft = AiRecommendationDraft.builder()
                .title("5 Best Ways to Fix React Native Battery Drain in Production")
                .contentType("VIDEO")
                .angle("Deep-dive practical performance troubleshooting for mobile engineers")
                .targetAudience("Mobile App Developers")
                .problemAddressed("Apps draining user phone batteries due to unthrottled background jobs")
                .keyPoints(List.of("Audit geolocation listeners", "Throttle background timer tasks"))
                .hook("Is your mobile app draining user batteries overnight?")
                .callToAction("Comment your biggest mobile performance issue below!")
                .evidenceIds(List.of(evidenceId))
                .confidence(0.92)
                .build();

        ValidationResult result = validator.validate(draft, context);

        assertThat(result.valid()).isTrue();
        assertThat(result.checks()).hasSize(6);
        assertThat(result.checks()).allMatch(CheckResult::passed);
    }

    @Test
    @DisplayName("Check 1 (SCHEMA): Fails if required fields are missing or empty")
    void testCheck1_SchemaFailure() {
        RecommendationContext context = createSampleContext("topic:12345");

        AiRecommendationDraft draft = AiRecommendationDraft.builder()
                .title("") // Blank title
                .contentType("VIDEO")
                .angle("Deep-dive practical performance troubleshooting")
                .targetAudience("Mobile Developers")
                .problemAddressed("Battery drain")
                .hook("Hook text")
                .callToAction("CTA text")
                .evidenceIds(List.of("topic:12345"))
                .confidence(0.92)
                .build();

        ValidationResult result = validator.validate(draft, context);

        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("SCHEMA")).isPresent();
        assertThat(result.getCheck("SCHEMA").get().passed()).isFalse();
    }

    @Test
    @DisplayName("Check 2 (EVIDENCE): Fails if draft references non-existent / fabricated evidence IDs")
    void testCheck2_FabricatedEvidenceId() {
        RecommendationContext context = createSampleContext("topic:valid-id");

        AiRecommendationDraft draft = AiRecommendationDraft.builder()
                .title("React Native Performance Optimization Tips")
                .contentType("VIDEO")
                .angle("Practical performance troubleshooting for engineers")
                .targetAudience("Mobile App Developers")
                .problemAddressed("Apps draining battery due to unthrottled background jobs")
                .hook("Is your mobile app draining batteries?")
                .callToAction("Subscribe for more React Native tips!")
                .evidenceIds(List.of("topic:valid-id", "comment:FABRICATED-999")) // Fabricated ID!
                .confidence(0.88)
                .build();

        ValidationResult result = validator.validate(draft, context);

        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("EVIDENCE")).isPresent();
        assertThat(result.getCheck("EVIDENCE").get().passed()).isFalse();
        assertThat(result.getCheck("EVIDENCE").get().reason()).contains("FABRICATED-999");
    }

    @Test
    @DisplayName("Check 3 (HALLUCINATION): Fails if draft makes specific percentage claims not in evidence")
    void testCheck3_UnsupportedPercentageClaim() {
        RecommendationContext context = createSampleContext("topic:valid-id");

        AiRecommendationDraft draft = AiRecommendationDraft.builder()
                .title("Why 90% of your audience hates slow battery drain") // "90%" not in evidence
                .contentType("VIDEO")
                .angle("Practical performance troubleshooting for mobile engineers")
                .targetAudience("Mobile App Developers")
                .problemAddressed("Apps draining user phone batteries due to unthrottled background jobs")
                .hook("Did you know 95% of users uninstall slow apps?") // "95%" not in evidence
                .callToAction("Comment below!")
                .evidenceIds(List.of("topic:valid-id"))
                .confidence(0.85)
                .build();

        ValidationResult result = validator.validate(draft, context);

        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("HALLUCINATION")).isPresent();
        assertThat(result.getCheck("HALLUCINATION").get().passed()).isFalse();
        assertThat(result.getCheck("HALLUCINATION").get().reason()).contains("Unsubstantiated quantitative claim");
    }

    @Test
    @DisplayName("Check 5 (DUPLICATE/NOVELTY): Fails if title is too similar to existing content history")
    void testCheck5_DuplicateContent() {
        RecommendationContext context = createSampleContext("topic:valid-id");

        AiRecommendationDraft draft = AiRecommendationDraft.builder()
                .title("Introduction to React Native 2026") // Exactly in recentPostTitles!
                .contentType("VIDEO")
                .angle("Deep-dive practical performance troubleshooting for engineers")
                .targetAudience("Mobile App Developers")
                .problemAddressed("Apps draining battery due to unthrottled background jobs")
                .hook("Is your mobile app draining batteries?")
                .callToAction("Subscribe for more tips!")
                .evidenceIds(List.of("topic:valid-id"))
                .confidence(0.85)
                .build();

        ValidationResult result = validator.validate(draft, context);

        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("NOVELTY")).isPresent();
        assertThat(result.getCheck("NOVELTY").get().passed()).isFalse();
        assertThat(result.getCheck("NOVELTY").get().reason()).contains("too similar to existing post");
    }

    @Test
    @DisplayName("Check 6 (SAFETY/PII): Fails if draft contains raw email addresses or secret tokens")
    void testCheck6_PiiLeak() {
        RecommendationContext context = createSampleContext("topic:valid-id");

        AiRecommendationDraft draft = AiRecommendationDraft.builder()
                .title("Reach out to test.user@example.com for private React Native benchmarks")
                .contentType("VIDEO")
                .angle("Performance troubleshooting for engineers")
                .targetAudience("Mobile App Developers")
                .problemAddressed("Apps draining battery due to unthrottled background jobs")
                .hook("Is your mobile app draining batteries?")
                .callToAction("Email me at secret@company.org to get the slides")
                .evidenceIds(List.of("topic:valid-id"))
                .confidence(0.85)
                .build();

        ValidationResult result = validator.validate(draft, context);

        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("SAFETY")).isPresent();
        assertThat(result.getCheck("SAFETY").get().passed()).isFalse();
        assertThat(result.getCheck("SAFETY").get().reason()).contains("PII Violation");
    }
}
