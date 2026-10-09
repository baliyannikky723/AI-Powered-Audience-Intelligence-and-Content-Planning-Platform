package com.pulsegpt.validation;

import com.pulsegpt.ai.client.dto.AiRecommendationDraft;
import com.pulsegpt.comment.Post;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecommendationValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w\\.-]+@[\\w\\.-]+\\.\\w+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(pulse-|sk-|ey[A-Za-z0-9-_]{20,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSUPPORTED_STAT_PATTERN = Pattern.compile("(\\b\\d{1,3}%|\\b\\d+x\\s+growth|\\beveryone\\s+wants|\\ball\\s+viewers)", Pattern.CASE_INSENSITIVE);

    public ValidationResult validate(AiRecommendationDraft draft, RecommendationContext context) {
        return validate(draft, context, GenerationMode.EVIDENCE_GROUNDED);
    }

    public ValidationResult validate(AiRecommendationDraft draft, RecommendationContext context, GenerationMode mode) {
        List<CheckResult> checks = new ArrayList<>();

        // CHECK 1: Schema Validation
        checks.add(validateSchema(draft, mode));

        // CHECK 2: Evidence ID Validation
        checks.add(validateEvidenceIds(draft, context, mode));

        // CHECK 3: Hallucination & Unsupported Claim Check
        checks.add(validateHallucinations(draft, context));

        // CHECK 4: Relevance Check
        checks.add(validateRelevance(draft, context));

        // CHECK 5: Duplicate / Novelty Check
        checks.add(validateNovelty(draft, context));

        // CHECK 6: Safety & Privacy Check
        checks.add(validateSafetyAndPrivacy(draft));

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

    private CheckResult validateSchema(AiRecommendationDraft draft, GenerationMode mode) {
        if (draft.title() == null || draft.title().trim().length() < 5) {
            return new CheckResult("SCHEMA", false, "Title is missing or shorter than 5 characters");
        }
        if (draft.angle() == null || draft.angle().trim().length() < 10) {
            return new CheckResult("SCHEMA", false, "Angle is missing or shorter than 10 characters");
        }
        if (draft.hook() == null || draft.hook().trim().length() < 10) {
            return new CheckResult("SCHEMA", false, "Hook is missing or shorter than 10 characters");
        }
        if (draft.keyPoints() == null || draft.keyPoints().size() < 2) {
            return new CheckResult("SCHEMA", false, "At least 2 structured key points required");
        }
        if (draft.confidence() != null && (draft.confidence() < 0.0 || draft.confidence() > 1.0)) {
            return new CheckResult("SCHEMA", false, "Confidence score must be between 0.0 and 1.0");
        }
        if (mode == GenerationMode.EVIDENCE_GROUNDED && (draft.evidenceIds() == null || draft.evidenceIds().isEmpty())) {
            return new CheckResult("SCHEMA", false, "Evidence-grounded recommendations must provide evidence IDs");
        }
        return new CheckResult("SCHEMA", true, "All required schema fields are valid");
    }

    private CheckResult validateEvidenceIds(AiRecommendationDraft draft, RecommendationContext context, GenerationMode mode) {
        if (mode == GenerationMode.BASELINE) {
            return new CheckResult("EVIDENCE", true, "Baseline mode does not require evidence grounding");
        }

        Set<String> validIds = context.evidenceItems().stream()
                .map(EvidenceItem::evidenceId)
                .collect(Collectors.toSet());

        List<String> unknownIds = new ArrayList<>();
        if (draft.evidenceIds() != null) {
            for (String eid : draft.evidenceIds()) {
                if (!validIds.contains(eid)) {
                    unknownIds.add(eid);
                }
            }
        }

        if (!unknownIds.isEmpty()) {
            return new CheckResult("EVIDENCE", false, "Referenced unknown or fabricated evidence IDs: " + unknownIds);
        }
        return new CheckResult("EVIDENCE", true, "All referenced evidence IDs exist in retrieved context");
    }

    private CheckResult validateHallucinations(AiRecommendationDraft draft, RecommendationContext context) {
        StringBuilder sb = new StringBuilder();
        if (draft.title() != null) sb.append(draft.title()).append(" ");
        if (draft.problemAddressed() != null) sb.append(draft.problemAddressed()).append(" ");
        if (draft.angle() != null) sb.append(draft.angle()).append(" ");
        if (draft.hook() != null) sb.append(draft.hook()).append(" ");
        if (draft.callToAction() != null) sb.append(draft.callToAction()).append(" ");
        if (draft.reason() != null) sb.append(draft.reason()).append(" ");
        String combinedText = sb.toString().toLowerCase();

        var matcher = UNSUPPORTED_STAT_PATTERN.matcher(combinedText);
        if (matcher.find()) {
            String claim = matcher.group();
            // Check if claim is substantiated in any evidence summary
            boolean substantiated = context.evidenceItems().stream()
                    .anyMatch(e -> e.summary() != null && e.summary().toLowerCase().contains(claim.toLowerCase()));

            if (!substantiated) {
                return new CheckResult("HALLUCINATION", false, "Unsubstantiated quantitative claim detected without evidence backing: '" + claim + "'");
            }
        }
        return new CheckResult("HALLUCINATION", true, "No unsupported statistical claims detected");
    }

    private CheckResult validateRelevance(AiRecommendationDraft draft, RecommendationContext context) {
        if (context.request() != null && context.request().contentType() != null) {
            if (draft.contentType() != null && !draft.contentType().equalsIgnoreCase(context.request().contentType())) {
                return new CheckResult("RELEVANCE", false, "Draft content type '" + draft.contentType() + "' differs from requested format '" + context.request().contentType() + "'");
            }
        }
        return new CheckResult("RELEVANCE", true, "Recommendation aligns with audience topic and requested format");
    }

    private CheckResult validateNovelty(AiRecommendationDraft draft, RecommendationContext context) {
        String draftTitle = draft.title().toLowerCase();

        // Check against recent posts in content history
        if (context.contentHistory() != null) {
            for (Post p : context.contentHistory()) {
                if (computeJaccardSimilarity(draftTitle, p.getTitle().toLowerCase()) >= 0.85) {
                    return new CheckResult("NOVELTY", false, "Recommendation is too similar to existing post: '" + p.getTitle() + "'");
                }
            }
        }

        // Check against previous recommendations
        if (context.previousRecommendations() != null) {
            for (ContentRecommendation cr : context.previousRecommendations()) {
                if (computeJaccardSimilarity(draftTitle, cr.getTitle().toLowerCase()) >= 0.85) {
                    return new CheckResult("NOVELTY", false, "Recommendation is a duplicate of recent recommendation: '" + cr.getTitle() + "'");
                }
            }
        }

        return new CheckResult("NOVELTY", true, "Idea is novel and non-duplicate");
    }

    private CheckResult validateSafetyAndPrivacy(AiRecommendationDraft draft) {
        StringBuilder sb = new StringBuilder();
        if (draft.title() != null) sb.append(draft.title()).append(" ");
        if (draft.hook() != null) sb.append(draft.hook()).append(" ");
        if (draft.callToAction() != null) sb.append(draft.callToAction()).append(" ");
        if (draft.problemAddressed() != null) sb.append(draft.problemAddressed()).append(" ");
        String text = sb.toString();

        if (EMAIL_PATTERN.matcher(text).find()) {
            return new CheckResult("SAFETY", false, "PII Violation: Raw email address detected in recommendation draft");
        }
        if (PHONE_PATTERN.matcher(text).find()) {
            return new CheckResult("SAFETY", false, "PII Violation: Phone number detected in recommendation draft");
        }
        if (SECRET_PATTERN.matcher(text).find()) {
            return new CheckResult("SAFETY", false, "Security Violation: Access key or secret pattern detected in draft");
        }
        return new CheckResult("SAFETY", true, "No PII, leaked secrets, or unsafe content detected");
    }

    private double computeJaccardSimilarity(String s1, String s2) {
        Set<String> set1 = Arrays.stream(s1.split("\\W+")).filter(w -> !w.isBlank()).collect(Collectors.toSet());
        Set<String> set2 = Arrays.stream(s2.split("\\W+")).filter(w -> !w.isBlank()).collect(Collectors.toSet());

        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / (double) union.size();
    }
}
