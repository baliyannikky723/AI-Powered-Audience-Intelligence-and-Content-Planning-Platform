package com.pulsegpt.rag.service;

import com.pulsegpt.rag.dto.RagCitationDto;
import com.pulsegpt.rag.dto.RagEvidenceItem;
import com.pulsegpt.rag.dto.RagValidationCheckResult;
import com.pulsegpt.rag.dto.RagValidationResult;
import com.pulsegpt.rag.model.RagMode;
import com.pulsegpt.recommendation.EvidenceSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RagCitationValidator {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[E(\\d+)\\]");
    private static final Pattern MALFORMED_CITATION_PATTERN = Pattern.compile("(\\[E[a-zA-Z_-]+\\]|\\[citation[^\\]]*\\]|\\[ref[^\\]]*\\])", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w\\.-]+@[\\w\\.-]+\\.\\w+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(pulse-|sk-|ey[A-Za-z0-9-_]{20,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSUPPORTED_STAT_PATTERN = Pattern.compile("(\\b\\d{1,3}%|\\b\\d+x\\s+growth|\\beveryone\\s+wants|\\ball\\s+viewers|\\balways\\s+prefers)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRAPH_TREND_PATTERN = Pattern.compile("(increasingly\\s+interested|growing\\s+interest|weakening\\s+interest|shifting\\s+focus)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INJECTION_PATTERN = Pattern.compile("(ignore\\s+previous\\s+instructions|system\\s+prompt|reveal\\s+instructions|bypass\\s+rules)", Pattern.CASE_INSENSITIVE);

    public RagValidationResult validateRagAnswer(
            String answer,
            List<RagEvidenceItem> evidence,
            RagMode mode,
            UUID userId) {

        List<RagValidationCheckResult> checks = new ArrayList<>();
        Map<String, RagEvidenceItem> citationMap = evidence.stream()
                .filter(e -> e.citationId() != null)
                .collect(Collectors.toMap(RagEvidenceItem::citationId, e -> e, (a, b) -> a));

        // CHECK 1: Citation Validity
        Set<String> citedTokens = extractCitationTokens(answer);
        List<String> invalidCitations = new ArrayList<>();
        int validCitationsCount = 0;

        Matcher malformedMatcher = MALFORMED_CITATION_PATTERN.matcher(answer);
        while (malformedMatcher.find()) {
            invalidCitations.add(malformedMatcher.group() + " (Malformed Citation)");
        }

        for (String token : citedTokens) {
            RagEvidenceItem item = citationMap.get(token);
            if (item == null) {
                invalidCitations.add(token + " (Unknown Citation)");
            } else if (!item.userId().equals(userId)) {
                invalidCitations.add(token + " (Cross-Tenant Citation)");
            } else {
                validCitationsCount++;
            }
        }

        double citationValidityRate = citedTokens.isEmpty() ? 1.0 : (double) validCitationsCount / citedTokens.size();

        if (!invalidCitations.isEmpty()) {
            checks.add(new RagValidationCheckResult(
                    "CITATION_VALIDITY",
                    false,
                    "Detected fabricated, malformed, or unauthorized citation IDs: " + invalidCitations
            ));
        } else if (mode == RagMode.FULL_EVIDENCE_GROUNDED && citedTokens.isEmpty() && !evidence.isEmpty()) {
            checks.add(new RagValidationCheckResult(
                    "CITATION_VALIDITY",
                    false,
                    "Evidence-grounded response should cite at least one supporting evidence item [En]"
            ));
        } else {
            checks.add(new RagValidationCheckResult(
                    "CITATION_VALIDITY",
                    true,
                    "All referenced citation IDs are authentic and user-scoped (" + validCitationsCount + " valid)"
            ));
        }

        // CHECK 2: Unsupported Claims Detection
        boolean unsupportedClaimsFound = false;
        Matcher statMatcher = UNSUPPORTED_STAT_PATTERN.matcher(answer);
        if (statMatcher.find()) {
            String claim = statMatcher.group();
            boolean substantiated = evidence.stream()
                    .anyMatch(e -> e.text() != null && e.text().toLowerCase().contains(claim.toLowerCase()));

            if (!substantiated) {
                unsupportedClaimsFound = true;
                checks.add(new RagValidationCheckResult(
                        "UNSUPPORTED_CLAIMS",
                        false,
                        "Unsubstantiated quantitative claim detected without evidence backing: '" + claim + "'"
                ));
            }
        }
        if (!unsupportedClaimsFound) {
            checks.add(new RagValidationCheckResult(
                    "UNSUPPORTED_CLAIMS",
                    true,
                    "No unsupported statistical or absolute claims detected"
            ));
        }

        // CHECK 3: Graph Trend Claim Validity
        Matcher trendMatcher = GRAPH_TREND_PATTERN.matcher(answer);
        if (trendMatcher.find()) {
            String trendClaim = trendMatcher.group();
            boolean hasGraphOrTrendEvidence = evidence.stream()
                    .anyMatch(e -> e.sourceType() == EvidenceSourceType.MEMORY
                            || e.sourceType() == EvidenceSourceType.TOPIC
                            || e.sourceType() == EvidenceSourceType.TREND);

            if (!hasGraphOrTrendEvidence) {
                checks.add(new RagValidationCheckResult(
                        "GRAPH_CLAIM_VALIDITY",
                        false,
                        "Trend claim '" + trendClaim + "' asserted without audience memory or topic trend evidence backing"
                ));
            } else {
                checks.add(new RagValidationCheckResult(
                        "GRAPH_CLAIM_VALIDITY",
                        true,
                        "Audience trend assertions are grounded in knowledge graph memory"
                ));
            }
        } else {
            checks.add(new RagValidationCheckResult(
                    "GRAPH_CLAIM_VALIDITY",
                    true,
                    "No ungrounded graph claims detected"
            ));
        }

        // CHECK 4: Prompt Injection Defense
        boolean injectionDetected = INJECTION_PATTERN.matcher(answer).find();
        if (injectionDetected) {
            checks.add(new RagValidationCheckResult(
                    "PROMPT_INJECTION_DEFENSE",
                    false,
                    "Potential prompt injection leak or bypassed instruction detected"
            ));
        } else {
            checks.add(new RagValidationCheckResult(
                    "PROMPT_INJECTION_DEFENSE",
                    true,
                    "Untrusted audience inputs safely contained as passive evidence"
            ));
        }

        // CHECK 5: Safety & Privacy (PII / Secrets)
        boolean hasEmail = EMAIL_PATTERN.matcher(answer).find();
        boolean hasPhone = PHONE_PATTERN.matcher(answer).find();
        boolean hasSecret = SECRET_PATTERN.matcher(answer).find();

        if (hasEmail || hasPhone || hasSecret) {
            checks.add(new RagValidationCheckResult(
                    "SAFETY_PRIVACY",
                    false,
                    "PII or sensitive secret detected in RAG response"
            ));
        } else {
            checks.add(new RagValidationCheckResult(
                    "SAFETY_PRIVACY",
                    true,
                    "Privacy filters verified: 0 PII / secrets exposed"
            ));
        }

        boolean overallValid = checks.stream().allMatch(RagValidationCheckResult::passed);
        String failureSummary = checks.stream()
                .filter(c -> !c.passed())
                .map(c -> c.checkName() + ": " + c.reason())
                .collect(Collectors.joining("; "));

        return RagValidationResult.builder()
                .valid(overallValid)
                .checks(checks)
                .failureSummary(failureSummary.isEmpty() ? null : failureSummary)
                .citationValidityScore(citationValidityRate)
                .unsupportedClaimsDetected(unsupportedClaimsFound)
                .promptInjectionDetected(injectionDetected)
                .build();
    }

    public List<RagCitationDto> buildCitationDtos(String answer, List<RagEvidenceItem> evidence) {
        Set<String> citedTokens = extractCitationTokens(answer);
        Map<String, RagEvidenceItem> itemMap = evidence.stream()
                .filter(e -> e.citationId() != null)
                .collect(Collectors.toMap(RagEvidenceItem::citationId, e -> e, (a, b) -> a));

        List<RagCitationDto> list = new ArrayList<>();
        for (String token : citedTokens) {
            RagEvidenceItem item = itemMap.get(token);
            if (item != null) {
                list.add(RagCitationDto.builder()
                        .citationId(token)
                        .evidenceId(item.evidenceId())
                        .sourceType(item.sourceType())
                        .snippet(item.text())
                        .relevanceScore(item.evidenceScore())
                        .valid(true)
                        .build());
            } else {
                list.add(RagCitationDto.builder()
                        .citationId(token)
                        .evidenceId("UNKNOWN")
                        .snippet("Unknown or fabricated citation")
                        .relevanceScore(0.0)
                        .valid(false)
                        .build());
            }
        }
        return list;
    }

    private Set<String> extractCitationTokens(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}
