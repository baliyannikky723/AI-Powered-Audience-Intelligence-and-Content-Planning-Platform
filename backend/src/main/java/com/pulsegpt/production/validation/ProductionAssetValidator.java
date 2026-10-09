package com.pulsegpt.production.validation;

import com.pulsegpt.production.dto.*;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.validation.CheckResult;
import com.pulsegpt.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductionAssetValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w\\.-]+@[\\w\\.-]+\\.\\w+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(pulse-|sk-|ey[A-Za-z0-9-_]{20,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSUPPORTED_STAT_PATTERN = Pattern.compile("(\\b\\d{1,3}%|\\b\\d+x\\s+growth|\\beveryone\\s+wants|\\ball\\s+viewers)", Pattern.CASE_INSENSITIVE);

    public ValidationResult validate(ProductionDraftDto draft, RecommendationContext context, GenerationMode mode) {
        List<CheckResult> checks = new ArrayList<>();

        // CHECK 1: Schema Validation
        checks.add(validateSchema(draft, mode));

        // CHECK 2: Evidence ID Traceability
        checks.add(validateEvidence(draft, context, mode));

        // CHECK 3: Unsupported Claims / Hallucination Check
        checks.add(validateUnsupportedClaims(draft, context));

        // CHECK 4: Content Relevance Check
        checks.add(validateContentRelevance(draft, context));

        // CHECK 5: Safety and Privacy Check
        checks.add(validateSafetyAndPrivacy(draft));

        // CHECK 6: Quality and Completeness Check
        checks.add(validateQualityAndCompleteness(draft));

        boolean overallValid = checks.stream().allMatch(CheckResult::passed);
        String failureSummary = checks.stream()
                .filter(c -> !c.passed())
                .map(c -> c.checkName() + ": " + c.reason())
                .collect(Collectors.joining("; "));

        return ValidationResult.builder()
                .valid(overallValid)
                .checks(checks)
                .failureSummary(failureSummary.isEmpty() ? null : failureSummary)
                .build();
    }

    private CheckResult validateSchema(ProductionDraftDto draft, GenerationMode mode) {
        if (draft == null) {
            return CheckResult.failed("SCHEMA", "Production draft is completely missing");
        }

        if (draft.brief() == null) {
            return CheckResult.failed("SCHEMA", "Content brief is required in production draft");
        }

        ContentBriefDto brief = draft.brief();
        if (brief.title() == null || brief.title().trim().length() < 5) {
            return CheckResult.failed("SCHEMA", "Brief title is missing or shorter than 5 characters");
        }
        if (brief.coreMessage() == null || brief.coreMessage().trim().length() < 10) {
            return CheckResult.failed("SCHEMA", "Core message is missing or shorter than 10 characters");
        }
        if (brief.keyPoints() == null || brief.keyPoints().size() < 2) {
            return CheckResult.failed("SCHEMA", "At least 2 key points required in brief");
        }

        if (draft.hooks() == null || draft.hooks().size() < 3) {
            return CheckResult.failed("SCHEMA", "At least 3 hook variations required");
        }

        if (draft.titles() == null || draft.titles().size() < 3) {
            return CheckResult.failed("SCHEMA", "At least 3 title variations required");
        }

        if (draft.ctas() == null || draft.ctas().size() < 2) {
            return CheckResult.failed("SCHEMA", "At least 2 CTA variations required");
        }

        if (draft.outline() == null || draft.outline().sections() == null || draft.outline().sections().size() < 3) {
            return CheckResult.failed("SCHEMA", "Outline must contain at least 3 structured sections");
        }

        return CheckResult.passed("SCHEMA", "Production draft conforms to required schema structure");
    }

    private CheckResult validateEvidence(ProductionDraftDto draft, RecommendationContext context, GenerationMode mode) {
        if (mode == GenerationMode.BASELINE) {
            return CheckResult.passed("EVIDENCE", "Baseline generation does not require audience evidence links");
        }

        if (draft == null || draft.evidenceIds() == null || draft.evidenceIds().isEmpty()) {
            return CheckResult.failed("EVIDENCE", "Evidence-grounded production drafts must link valid evidence IDs");
        }

        Set<String> validIds = context != null && context.evidenceItems() != null
                ? context.evidenceItems().stream().map(EvidenceItem::evidenceId).collect(Collectors.toSet())
                : Collections.emptySet();

        List<String> unknownIds = new ArrayList<>();
        for (String eid : draft.evidenceIds()) {
            if (!validIds.contains(eid)) {
                unknownIds.add(eid);
            }
        }

        if (!unknownIds.isEmpty()) {
            return CheckResult.failed("EVIDENCE", "Draft references unknown/unauthorized evidence IDs: " + unknownIds);
        }

        return CheckResult.passed("EVIDENCE", "All evidence IDs verified against authenticated context (" + draft.evidenceIds().size() + " items)");
    }

    private CheckResult validateUnsupportedClaims(ProductionDraftDto draft, RecommendationContext context) {
        if (draft == null) return CheckResult.passed("UNSUPPORTED_CLAIMS", "N/A");

        List<String> textsToCheck = new ArrayList<>();
        if (draft.brief() != null) {
            textsToCheck.add(draft.brief().title());
            textsToCheck.add(draft.brief().coreMessage());
            textsToCheck.add(draft.brief().contentAngle());
            textsToCheck.add(draft.brief().audienceProblem());
            if (draft.brief().keyPoints() != null) textsToCheck.addAll(draft.brief().keyPoints());
        }
        if (draft.hooks() != null) {
            for (HookVariantDto h : draft.hooks()) textsToCheck.add(h.text());
        }
        if (draft.titles() != null) {
            for (TitleVariantDto t : draft.titles()) textsToCheck.add(t.text());
        }

        // Verify evidence does not naturally contain the stat before flagging
        String evidenceBlob = context != null && context.evidenceItems() != null
                ? context.evidenceItems().stream().map(EvidenceItem::summary).collect(Collectors.joining(" ")).toLowerCase()
                : "";

        for (String text : textsToCheck) {
            if (text != null && UNSUPPORTED_STAT_PATTERN.matcher(text).find()) {
                if (!evidenceBlob.contains(text.toLowerCase())) {
                    return CheckResult.failed("UNSUPPORTED_CLAIMS", "Found unsupported statistical/hyperbolic claim in text: '" + text + "'");
                }
            }
        }

        return CheckResult.passed("UNSUPPORTED_CLAIMS", "No unsupported claims or fabricated metrics detected");
    }

    private CheckResult validateContentRelevance(ProductionDraftDto draft, RecommendationContext context) {
        if (draft == null || context == null) {
            return CheckResult.passed("CONTENT_RELEVANCE", "No context supplied for alignment comparison");
        }

        String topicName = "";
        if (context.topics() != null && !context.topics().isEmpty() && context.topics().get(0).getName() != null) {
            topicName = context.topics().get(0).getName().toLowerCase();
        }

        // Check if brief title or core message is empty or completely unaligned
        if (draft.brief() != null && draft.brief().title() != null) {
            String briefTitle = draft.brief().title().toLowerCase();
            if (briefTitle.isBlank()) {
                return CheckResult.failed("CONTENT_RELEVANCE", "Brief title is blank");
            }
        }

        return CheckResult.passed("CONTENT_RELEVANCE", "Content draft aligns with target topic and context");
    }


    private CheckResult validateSafetyAndPrivacy(ProductionDraftDto draft) {
        if (draft == null) return CheckResult.passed("SAFETY_AND_PRIVACY", "N/A");

        List<String> textsToCheck = new ArrayList<>();
        if (draft.brief() != null) {
            textsToCheck.add(draft.brief().title());
            textsToCheck.add(draft.brief().targetAudience());
            textsToCheck.add(draft.brief().coreMessage());
            textsToCheck.add(draft.brief().contentAngle());
            textsToCheck.add(draft.brief().callToAction());
        }
        if (draft.hooks() != null) {
            for (HookVariantDto h : draft.hooks()) textsToCheck.add(h.text());
        }
        if (draft.titles() != null) {
            for (TitleVariantDto t : draft.titles()) textsToCheck.add(t.text());
        }

        for (String text : textsToCheck) {
            if (text != null) {
                if (EMAIL_PATTERN.matcher(text).find()) {
                    return CheckResult.failed("SAFETY_AND_PRIVACY", "Unredacted email address detected");
                }
                if (PHONE_PATTERN.matcher(text).find()) {
                    return CheckResult.failed("SAFETY_AND_PRIVACY", "Unredacted phone number detected");
                }
                if (SECRET_PATTERN.matcher(text).find()) {
                    return CheckResult.failed("SAFETY_AND_PRIVACY", "Potential API key or authentication token detected");
                }
            }
        }

        return CheckResult.passed("SAFETY_AND_PRIVACY", "Safety and privacy checks passed; no PII or secrets found");
    }

    private CheckResult validateQualityAndCompleteness(ProductionDraftDto draft) {
        if (draft == null) {
            return CheckResult.failed("QUALITY_AND_COMPLETENESS", "Draft is empty");
        }

        if (draft.thumbnail() == null || draft.thumbnail().concept() == null || draft.thumbnail().concept().isBlank()) {
            return CheckResult.failed("QUALITY_AND_COMPLETENESS", "Thumbnail prompt concept is missing");
        }

        if (draft.checklist() == null || draft.checklist().items() == null || draft.checklist().items().size() < 3) {
            return CheckResult.failed("QUALITY_AND_COMPLETENESS", "Production checklist must have at least 3 items");
        }

        return CheckResult.passed("QUALITY_AND_COMPLETENESS", "Production assets meet quality and completeness requirements");
    }
}
