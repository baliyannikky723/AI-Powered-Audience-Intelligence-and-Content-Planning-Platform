package com.pulsegpt.production.validation;

import com.pulsegpt.production.dto.*;
import com.pulsegpt.recommendation.EvidenceSourceType;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Production Asset 6-Check Validator Unit Tests")
class ProductionAssetValidatorTest {

    private ProductionAssetValidator validator;
    private RecommendationContext sampleContext;

    @BeforeEach
    void setUp() {
        validator = new ProductionAssetValidator();

        Topic topic = Topic.builder()
                .id(UUID.randomUUID())
                .name("Database Performance")
                .build();

        EvidenceItem ev1 = EvidenceItem.builder()
                .evidenceId("topic:db_perf")
                .sourceType(EvidenceSourceType.TOPIC)
                .sourceId(UUID.randomUUID())
                .summary("Database indexing queries and slow logs")
                .relevanceScore(0.95)
                .build();

        sampleContext = RecommendationContext.builder()
                .topics(List.of(topic))
                .evidenceItems(List.of(ev1))
                .build();
    }


    private ProductionDraftDto createValidDraft() {
        return ProductionDraftDto.builder()
                .brief(ContentBriefDto.builder()
                        .title("Mastering Database Performance in Spring Boot")
                        .contentType("VIDEO")
                        .platform("YOUTUBE")
                        .targetAudience("Backend Developers")
                        .audienceProblem("Slow query response times during peak loads")
                        .audienceEvidence(List.of("Database indexing queries and slow logs"))
                        .coreMessage("Optimize query performance through composite indexing and execution plan analysis.")
                        .contentAngle("Hands-on performance benchmarking and tuning.")
                        .keyPoints(List.of("Understanding B-Tree indexes", "Analyzing EXPLAIN query plans", "Configuring connection pools"))
                        .tone("Direct, Technical, Engaging")
                        .callToAction("Drop your DB tuning questions in the comments!")
                        .successObjective("Demystify query performance")
                        .build())
                .outline(ScriptOutlineDto.builder()
                        .format("VIDEO_STRUCTURED_OUTLINE")
                        .sections(List.of(
                                new ScriptOutlineDto.ScriptOutlineSectionDto("1. Hook", "Engage viewer", List.of("Show slow vs fast query benchmark"), 15),
                                new ScriptOutlineDto.ScriptOutlineSectionDto("2. Introduction", "Set context", List.of("Explain indexing fundamentals"), 30),
                                new ScriptOutlineDto.ScriptOutlineSectionDto("3. Hands-on", "Walkthrough", List.of("Step by step code example"), 60)
                        ))
                        .keyTakeaway("Effective indexing resolves 90 percent of standard latency bottlenecks.")
                        .build())
                .hooks(List.of(
                        new HookVariantDto("QUESTION", "Is your database query slowing down under load?", "Direct question"),
                        new HookVariantDto("PROBLEM", "Most developers add indexes randomly without profiling.", "Problem framing"),
                        new HookVariantDto("CONTRAST", "A 10-second query can become 2ms with one index.", "Contrast")
                ))
                .titles(List.of(
                        new TitleVariantDto("HOW_TO", "How to Optimize SQL Queries in Spring Boot", "Search intent"),
                        new TitleVariantDto("DIRECT_BENEFIT", "Fix Slow Spring Boot Database Queries Fast", "Actionable"),
                        new TitleVariantDto("QUESTION", "Why Are Your PostgreSQL Queries Running Slow?", "Diagnostic")
                ))
                .ctas(List.of(
                        new CtaVariantDto("COMMENT_ENGAGEMENT", "What is your biggest DB bottleneck? Let us know below!"),
                        new CtaVariantDto("SUBSCRIBE_FOLLOW", "Subscribe for more advanced backend tutorials.")
                ))
                .thumbnail(ThumbnailPromptDto.builder()
                        .concept("Visual split showing red query slow bar vs green fast bar")
                        .visualSubject("Developer looking relieved at fast terminal benchmark")
                        .composition("Side by side split screen with bold lighting")
                        .textOverlay("FIX SLOW QUERIES")
                        .emotion("Focused, authoritative")
                        .style("Clean, modern tech digital art")
                        .build())
                .checklist(ProductionChecklistDto.builder()
                        .items(List.of(
                                new ProductionChecklistDto.ChecklistItemDto("PRE_PRODUCTION", "Verify query plans", false),
                                new ProductionChecklistDto.ChecklistItemDto("PRODUCTION", "Record screen terminal", false),
                                new ProductionChecklistDto.ChecklistItemDto("POST_PRODUCTION", "Add captions and review", false)
                        ))
                        .build())
                .evidenceIds(List.of("topic:db_perf"))
                .build();
    }

    @Test
    @DisplayName("Valid production draft passes all 6 checks")
    void testValidDraftPassesAllChecks() {
        ProductionDraftDto draft = createValidDraft();
        ValidationResult result = validator.validate(draft, sampleContext, GenerationMode.EVIDENCE_GROUNDED);

        assertThat(result.valid()).isTrue();
        assertThat(result.checks()).hasSize(6);
        assertThat(result.checks()).allMatch(c -> c.passed());
    }

    @Test
    @DisplayName("Schema validation fails on missing required sections or low variant count")
    void testSchemaValidationFailure() {
        ProductionDraftDto invalidDraft = ProductionDraftDto.builder()
                .brief(ContentBriefDto.builder()
                        .title("Short")
                        .coreMessage("Brief")
                        .keyPoints(List.of("One"))
                        .build())
                .hooks(List.of(new HookVariantDto("Q", "Single hook", "Why")))
                .titles(List.of(new TitleVariantDto("T", "Single title", "Why")))
                .ctas(Collections.emptyList())
                .outline(null)
                .build();

        ValidationResult result = validator.validate(invalidDraft, sampleContext, GenerationMode.EVIDENCE_GROUNDED);
        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("SCHEMA")).isPresent();
        assertThat(result.getCheck("SCHEMA").get().passed()).isFalse();
    }

    @Test
    @DisplayName("Evidence check fails when draft references unknown evidence IDs in EVIDENCE_GROUNDED mode")
    void testEvidenceValidationFailure() {
        ProductionDraftDto draftWithBadEvidence = createValidDraft();
        ProductionDraftDto modified = ProductionDraftDto.builder()
                .brief(draftWithBadEvidence.brief())
                .outline(draftWithBadEvidence.outline())
                .hooks(draftWithBadEvidence.hooks())
                .titles(draftWithBadEvidence.titles())
                .ctas(draftWithBadEvidence.ctas())
                .thumbnail(draftWithBadEvidence.thumbnail())
                .checklist(draftWithBadEvidence.checklist())
                .evidenceIds(List.of("unknown:forged_id"))
                .build();

        ValidationResult result = validator.validate(modified, sampleContext, GenerationMode.EVIDENCE_GROUNDED);
        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("EVIDENCE")).isPresent();
        assertThat(result.getCheck("EVIDENCE").get().passed()).isFalse();
    }

    @Test
    @DisplayName("Evidence check passes without evidence IDs in BASELINE mode")
    void testBaselineModePassesEvidenceCheck() {
        ProductionDraftDto draft = createValidDraft();
        ProductionDraftDto baselineDraft = ProductionDraftDto.builder()
                .brief(draft.brief())
                .outline(draft.outline())
                .hooks(draft.hooks())
                .titles(draft.titles())
                .ctas(draft.ctas())
                .thumbnail(draft.thumbnail())
                .checklist(draft.checklist())
                .evidenceIds(Collections.emptyList())
                .build();

        ValidationResult result = validator.validate(baselineDraft, sampleContext, GenerationMode.BASELINE);
        assertThat(result.getCheck("EVIDENCE").get().passed()).isTrue();
    }

    @Test
    @DisplayName("Unsupported claims check flags fabricated percentage statistics not in evidence")
    void testUnsupportedClaimsFailure() {
        ProductionDraftDto draft = createValidDraft();
        ContentBriefDto badBrief = ContentBriefDto.builder()
                .title("99% of developers do not know this secret")
                .contentType("VIDEO")
                .platform("YOUTUBE")
                .targetAudience("Developers")
                .audienceProblem("Everyone wants 10x growth")
                .audienceEvidence(List.of())
                .coreMessage("Core message with depth and clarity")
                .contentAngle("Unique angle")
                .keyPoints(List.of("P1", "P2"))
                .callToAction("Subscribe")
                .build();

        ProductionDraftDto modified = ProductionDraftDto.builder()
                .brief(badBrief)
                .outline(draft.outline())
                .hooks(draft.hooks())
                .titles(draft.titles())
                .ctas(draft.ctas())
                .thumbnail(draft.thumbnail())
                .checklist(draft.checklist())
                .evidenceIds(List.of("topic:db_perf"))
                .build();

        ValidationResult result = validator.validate(modified, sampleContext, GenerationMode.EVIDENCE_GROUNDED);
        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("UNSUPPORTED_CLAIMS")).isPresent();
        assertThat(result.getCheck("UNSUPPORTED_CLAIMS").get().passed()).isFalse();
    }

    @Test
    @DisplayName("Safety and privacy check flags unredacted email and secrets")
    void testSafetyAndPrivacyFailure() {
        ProductionDraftDto draft = createValidDraft();
        ContentBriefDto piiBrief = ContentBriefDto.builder()
                .title("Contact admin@company.com for database credentials")
                .contentType("VIDEO")
                .platform("YOUTUBE")
                .targetAudience("Developers")
                .audienceProblem("Missing passwords")
                .audienceEvidence(List.of())
                .coreMessage("Use secret key pulse-secret-api-token-99999")
                .contentAngle("Security walkthrough")
                .keyPoints(List.of("Point 1", "Point 2"))
                .callToAction("Email support@company.com")
                .build();

        ProductionDraftDto modified = ProductionDraftDto.builder()
                .brief(piiBrief)
                .outline(draft.outline())
                .hooks(draft.hooks())
                .titles(draft.titles())
                .ctas(draft.ctas())
                .thumbnail(draft.thumbnail())
                .checklist(draft.checklist())
                .evidenceIds(List.of("topic:db_perf"))
                .build();

        ValidationResult result = validator.validate(modified, sampleContext, GenerationMode.EVIDENCE_GROUNDED);
        assertThat(result.valid()).isFalse();
        assertThat(result.getCheck("SAFETY_AND_PRIVACY")).isPresent();
        assertThat(result.getCheck("SAFETY_AND_PRIVACY").get().passed()).isFalse();
    }
}
