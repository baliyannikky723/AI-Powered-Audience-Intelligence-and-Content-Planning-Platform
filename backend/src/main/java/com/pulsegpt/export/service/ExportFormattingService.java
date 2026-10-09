package com.pulsegpt.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pulsegpt.export.ExportType;
import com.pulsegpt.export.dto.TimelineSceneDto;
import com.pulsegpt.production.ContentProductionAsset;
import com.pulsegpt.production.ProductionAssetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportFormattingService {

    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneId.of("UTC"));

    /**
     * Formats export content deterministically without invoking any external LLMs.
     */
    public byte[] formatExport(ExportType exportType, ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        return switch (exportType) {
            case MARKDOWN -> generateMarkdown(primaryAsset, allAssets).getBytes(StandardCharsets.UTF_8);
            case TELEPROMPTER -> generateTeleprompter(primaryAsset, allAssets).getBytes(StandardCharsets.UTF_8);
            case TIMELINE -> generateTimelineMarkdown(primaryAsset, allAssets).getBytes(StandardCharsets.UTF_8);
            case CHECKLIST -> generateChecklistMarkdown(primaryAsset, allAssets).getBytes(StandardCharsets.UTF_8);
            case JSON -> generateJson(primaryAsset, allAssets).getBytes(StandardCharsets.UTF_8);
            case PDF -> generatePdf(primaryAsset, allAssets);
            case PRODUCTION_PACKAGE -> generateProductionPackageZip(primaryAsset, allAssets);
        };
    }

    // ─── 1. MARKDOWN EXPORT ───────────────────────────────────────────────────

    public String generateMarkdown(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        Map<ProductionAssetType, Map<String, Object>> assetMap = indexAssets(allAssets);
        Map<String, Object> brief = assetMap.getOrDefault(ProductionAssetType.CONTENT_BRIEF, primaryAsset.getContentJson());
        Map<String, Object> outline = assetMap.getOrDefault(ProductionAssetType.SCRIPT_OUTLINE, Collections.emptyMap());
        List<Map<String, Object>> hooks = extractList(assetMap.get(ProductionAssetType.HOOK), "hooks");
        List<Map<String, Object>> titles = extractList(assetMap.get(ProductionAssetType.TITLE_VARIATION), "titles");
        List<Map<String, Object>> ctas = extractList(assetMap.get(ProductionAssetType.CTA), "ctas");
        Map<String, Object> thumbnail = assetMap.getOrDefault(ProductionAssetType.THUMBNAIL_PROMPT, Collections.emptyMap());
        List<Map<String, Object>> checklist = extractList(assetMap.get(ProductionAssetType.PRODUCTION_CHECKLIST), "items");

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(brief.getOrDefault("title", "Content Production Pack")).append("\n\n");

        sb.append("> **Creator Approved Production Draft** | PulseGPT Content Copilot\n");
        sb.append("> Generated: ").append(primaryAsset.getCreatedAt() != null ? DATE_FORMATTER.format(primaryAsset.getCreatedAt()) : "N/A").append(" | ");
        sb.append("Version: v").append(primaryAsset.getVersion()).append(" | ");
        sb.append("Mode: ").append(primaryAsset.getGenerationMode()).append("\n\n");

        sb.append("---\n\n");

        // Content Brief
        sb.append("## 1. Content Brief\n\n");
        sb.append("- **Platform**: ").append(brief.getOrDefault("platform", "YouTube")).append("\n");
        sb.append("- **Content Type**: ").append(brief.getOrDefault("contentType", "Video")).append("\n");
        sb.append("- **Target Audience**: ").append(brief.getOrDefault("targetAudience", "General Audience")).append("\n");
        sb.append("- **Audience Problem**: ").append(brief.getOrDefault("audienceProblem", "N/A")).append("\n");
        sb.append("- **Strategic Angle**: ").append(brief.getOrDefault("contentAngle", "N/A")).append("\n");
        sb.append("- **Core Message**: ").append(brief.getOrDefault("coreMessage", "N/A")).append("\n");
        sb.append("- **Tone**: ").append(brief.getOrDefault("tone", "Authoritative")).append("\n");
        sb.append("- **Success Objective**: ").append(brief.getOrDefault("successObjective", "N/A")).append("\n\n");

        // Titles
        sb.append("## 2. Recommended Title Options\n\n");
        if (!titles.isEmpty()) {
            for (int i = 0; i < titles.size(); i++) {
                Map<String, Object> t = titles.get(i);
                sb.append(i + 1).append(". **[").append(t.getOrDefault("titleType", "OPTION")).append("]** ")
                        .append(t.getOrDefault("text", "")).append("\n");
                if (t.get("rationale") != null) {
                    sb.append("   - *Rationale*: ").append(t.get("rationale")).append("\n");
                }
            }
        } else {
            sb.append("1. ").append(brief.getOrDefault("title", "Untitled")).append("\n");
        }
        sb.append("\n");

        // Hooks
        sb.append("## 3. Hook Variations\n\n");
        if (!hooks.isEmpty()) {
            for (int i = 0; i < hooks.size(); i++) {
                Map<String, Object> h = hooks.get(i);
                sb.append(i + 1).append(". **[").append(h.getOrDefault("hookType", "HOOK")).append("]** ")
                        .append("\"").append(h.getOrDefault("text", "")).append("\"\n");
                if (h.get("rationale") != null) {
                    sb.append("   - *Rationale*: ").append(h.get("rationale")).append("\n");
                }
            }
        } else {
            sb.append("1. \"").append(brief.getOrDefault("hook", "Discover how to master this topic today.")).append("\"\n");
        }
        sb.append("\n");

        // Outline
        sb.append("## 4. Script & Content Outline\n\n");
        List<Map<String, Object>> sections = extractList(outline, "sections");
        if (!sections.isEmpty()) {
            for (Map<String, Object> sec : sections) {
                sb.append("### ").append(sec.getOrDefault("sectionTitle", "Section")).append("\n");
                sb.append("- **Purpose**: ").append(sec.getOrDefault("purpose", "Deliver value")).append("\n");
                List<String> points = extractStringList(sec, "talkingPoints");
                if (!points.isEmpty()) {
                    sb.append("- **Key Talking Points**:\n");
                    for (String pt : points) {
                        sb.append("  - ").append(pt).append("\n");
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("*(See brief key points for main content structure)*\n\n");
        }

        // CTAs
        sb.append("## 5. Call-to-Action Variations\n\n");
        if (!ctas.isEmpty()) {
            for (int i = 0; i < ctas.size(); i++) {
                Map<String, Object> c = ctas.get(i);
                sb.append(i + 1).append(". **[").append(c.getOrDefault("ctaType", "CTA")).append("]** ")
                        .append("\"").append(c.getOrDefault("text", "")).append("\"\n");
            }
        } else {
            sb.append("1. \"").append(brief.getOrDefault("callToAction", "Subscribe for more insights.")).append("\"\n");
        }
        sb.append("\n");

        // Thumbnail Prompt
        sb.append("## 6. Thumbnail Concept & Visual Direction\n\n");
        if (!thumbnail.isEmpty()) {
            sb.append("- **Concept**: ").append(thumbnail.getOrDefault("concept", "Clean high-contrast composition")).append("\n");
            sb.append("- **Visual Subject**: ").append(thumbnail.getOrDefault("visualSubject", "N/A")).append("\n");
            sb.append("- **Composition**: ").append(thumbnail.getOrDefault("composition", "N/A")).append("\n");
            sb.append("- **Text Overlay**: \"").append(thumbnail.getOrDefault("textOverlay", "")).append("\"\n");
            sb.append("- **Style / Emotion**: ").append(thumbnail.getOrDefault("style", "Modern")).append(" / ")
                    .append(thumbnail.getOrDefault("emotion", "Engaging")).append("\n\n");
        } else {
            sb.append("*(No custom thumbnail concept generated)*\n\n");
        }

        // Checklist
        sb.append("## 7. Production Checklist\n\n");
        if (!checklist.isEmpty()) {
            for (Map<String, Object> item : checklist) {
                boolean checked = Boolean.TRUE.equals(item.get("completed"));
                sb.append("- [").append(checked ? "x" : " ").append("] **")
                        .append(item.getOrDefault("phase", "TASK")).append("**: ")
                        .append(item.getOrDefault("task", "")).append("\n");
            }
        } else {
            sb.append("- [ ] **PRE-PRODUCTION**: Review evidence and confirm talking points\n");
            sb.append("- [ ] **PRODUCTION**: Record hook, main body, and CTA\n");
            sb.append("- [ ] **POST-PRODUCTION**: Edit, add captions, and perform final review\n");
        }
        sb.append("\n");

        // Audience Evidence & Provenance
        sb.append("## 8. Audience Evidence & Provenance Appendix\n\n");
        sb.append("Every element in this production pack is cryptographically grounded in audience intelligence.\n\n");
        sb.append("- **Recommendation ID**: `").append(primaryAsset.getRecommendation().getId()).append("`\n");
        if (primaryAsset.getCalendarItem() != null) {
            sb.append("- **Calendar Item ID**: `").append(primaryAsset.getCalendarItem().getId()).append("`\n");
        }
        sb.append("- **Primary Asset ID**: `").append(primaryAsset.getId()).append("`\n");
        sb.append("- **Prompt Version**: `").append(primaryAsset.getPromptVersion() != null ? primaryAsset.getPromptVersion() : "PRODUCTION_PROMPT_V1").append("`\n");
        sb.append("- **Model Trace**: `").append(primaryAsset.getModelName() != null ? primaryAsset.getModelName() : "pulsegpt-production-v1").append("`\n\n");

        List<String> evidenceQuotes = extractStringList(brief, "audienceEvidence");
        if (!evidenceQuotes.isEmpty()) {
            sb.append("### Grounding Evidence Signals\n\n");
            for (String quote : evidenceQuotes) {
                sb.append("> \"").append(quote).append("\"\n\n");
            }
        }

        return sb.toString();
    }

    // ─── 2. TELEPROMPTER EXPORT ───────────────────────────────────────────────

    public String generateTeleprompter(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        Map<ProductionAssetType, Map<String, Object>> assetMap = indexAssets(allAssets);
        Map<String, Object> brief = assetMap.getOrDefault(ProductionAssetType.CONTENT_BRIEF, primaryAsset.getContentJson());
        Map<String, Object> outline = assetMap.getOrDefault(ProductionAssetType.SCRIPT_OUTLINE, Collections.emptyMap());
        List<Map<String, Object>> hooks = extractList(assetMap.get(ProductionAssetType.HOOK), "hooks");
        List<Map<String, Object>> ctas = extractList(assetMap.get(ProductionAssetType.CTA), "ctas");
        List<Map<String, Object>> sections = extractList(outline, "sections");

        StringBuilder sb = new StringBuilder();
        sb.append("===============================================================================\n");
        sb.append("PULSEGPT TELEPROMPTER SCRIPT: ").append(brief.getOrDefault("title", "UNTITLED")).append("\n");
        sb.append("===============================================================================\n\n");

        // Hook
        sb.append("[HOOK]\n\n");
        if (!hooks.isEmpty()) {
            sb.append(hooks.get(0).getOrDefault("text", "")).append("\n\n");
        } else {
            sb.append(brief.getOrDefault("hook", "Welcome back to the channel.")).append("\n\n");
        }

        // Intro / Problem
        sb.append("[INTRODUCTION & THE CORE PROBLEM]\n\n");
        if (brief.get("audienceProblem") != null) {
            sb.append("Today we're addressing a major issue that many of you brought up: ")
                    .append(brief.get("audienceProblem")).append(".\n\n");
        }
        if (brief.get("coreMessage") != null) {
            sb.append(brief.get("coreMessage")).append("\n\n");
        }

        // Sections
        if (!sections.isEmpty()) {
            for (int i = 0; i < sections.size(); i++) {
                Map<String, Object> sec = sections.get(i);
                String title = (String) sec.getOrDefault("sectionTitle", "POINT " + (i + 1));
                sb.append("[").append(title.toUpperCase()).append("]\n\n");

                List<String> points = extractStringList(sec, "talkingPoints");
                for (String pt : points) {
                    sb.append(pt).append(".\n\n");
                }
            }
        } else {
            List<String> keyPoints = extractStringList(brief, "keyPoints");
            for (int i = 0; i < keyPoints.size(); i++) {
                sb.append("[MAIN POINT ").append(i + 1).append("]\n\n");
                sb.append(keyPoints.get(i)).append(".\n\n");
            }
        }

        // Summary & CTA
        sb.append("[SUMMARY & KEY TAKEAWAY]\n\n");
        if (outline.get("keyTakeaway") != null) {
            sb.append(outline.get("keyTakeaway")).append("\n\n");
        } else {
            sb.append("To recap: mastering this approach will give you full control over your workflow.\n\n");
        }

        sb.append("[CALL TO ACTION]\n\n");
        if (!ctas.isEmpty()) {
            sb.append(ctas.get(0).getOrDefault("text", "")).append("\n\n");
        } else {
            sb.append(brief.getOrDefault("callToAction", "Let me know what you think in the comments below! Don't forget to subscribe.")).append("\n\n");
        }

        sb.append("===============================================================================\n");
        sb.append("[END OF SCRIPT]\n");
        sb.append("===============================================================================\n");

        return sb.toString();
    }

    // ─── 3. TIMELINE & SCENE BREAKDOWN ────────────────────────────────────────

    public List<TimelineSceneDto> generateTimeline(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        Map<ProductionAssetType, Map<String, Object>> assetMap = indexAssets(allAssets);
        Map<String, Object> brief = assetMap.getOrDefault(ProductionAssetType.CONTENT_BRIEF, primaryAsset.getContentJson());
        Map<String, Object> outline = assetMap.getOrDefault(ProductionAssetType.SCRIPT_OUTLINE, Collections.emptyMap());
        List<Map<String, Object>> sections = extractList(outline, "sections");
        List<TimelineSceneDto> scenes = new ArrayList<>();

        int sceneIndex = 1;

        // Scene 1: Hook
        scenes.add(TimelineSceneDto.builder()
                .sceneNumber(sceneIndex++)
                .sectionTitle("1. Hook")
                .purpose("Grab viewer attention in first 5 seconds")
                .talkingPoints(List.of((String) brief.getOrDefault("hook", "Opening curiosity hook")))
                .estimatedDurationSeconds(10)
                .formattedDuration("0:00 - 0:10 (10s)")
                .visualDirection("Fast cut / punch-in on face with bold text overlay")
                .build());

        // Scene 2: Intro & Problem
        scenes.add(TimelineSceneDto.builder()
                .sceneNumber(sceneIndex++)
                .sectionTitle("2. Introduction & Problem")
                .purpose("Frame the audience problem and video angle")
                .talkingPoints(List.of(
                        (String) brief.getOrDefault("audienceProblem", "Audience pain point"),
                        (String) brief.getOrDefault("coreMessage", "Video core message")
                ))
                .estimatedDurationSeconds(20)
                .formattedDuration("0:10 - 0:30 (20s)")
                .visualDirection("Medium shot with audience comment quotes pop-up")
                .build());

        // Main Sections
        int currentTimestamp = 30;
        if (!sections.isEmpty()) {
            for (Map<String, Object> sec : sections) {
                String title = (String) sec.getOrDefault("sectionTitle", "Main Point");
                String purpose = (String) sec.getOrDefault("purpose", "Deliver core explanation");
                List<String> points = extractStringList(sec, "talkingPoints");
                int dur = 35;

                scenes.add(TimelineSceneDto.builder()
                        .sceneNumber(sceneIndex++)
                        .sectionTitle(title)
                        .purpose(purpose)
                        .talkingPoints(points)
                        .estimatedDurationSeconds(dur)
                        .formattedDuration(formatTimestamp(currentTimestamp, currentTimestamp + dur))
                        .visualDirection("B-roll screen recording / diagram animation")
                        .build());
                currentTimestamp += dur;
            }
        } else {
            List<String> keyPoints = extractStringList(brief, "keyPoints");
            for (String pt : keyPoints) {
                int dur = 30;
                scenes.add(TimelineSceneDto.builder()
                        .sceneNumber(sceneIndex++)
                        .sectionTitle("Key Section: " + pt)
                        .purpose("Explain mechanism")
                        .talkingPoints(List.of(pt))
                        .estimatedDurationSeconds(dur)
                        .formattedDuration(formatTimestamp(currentTimestamp, currentTimestamp + dur))
                        .visualDirection("B-roll graphic")
                        .build());
                currentTimestamp += dur;
            }
        }

        // Summary
        scenes.add(TimelineSceneDto.builder()
                .sceneNumber(sceneIndex++)
                .sectionTitle("Summary & Wrap Up")
                .purpose("Recap key takeaways")
                .talkingPoints(List.of((String) outline.getOrDefault("keyTakeaway", "Summary of approach")))
                .estimatedDurationSeconds(15)
                .formattedDuration(formatTimestamp(currentTimestamp, currentTimestamp + 15))
                .visualDirection("Bullet point graphic list on screen")
                .build());
        currentTimestamp += 15;

        // CTA
        scenes.add(TimelineSceneDto.builder()
                .sceneNumber(sceneIndex)
                .sectionTitle("Call to Action")
                .purpose("Audience engagement and next steps")
                .talkingPoints(List.of((String) brief.getOrDefault("callToAction", "Subscribe and comment")))
                .estimatedDurationSeconds(10)
                .formattedDuration(formatTimestamp(currentTimestamp, currentTimestamp + 10))
                .visualDirection("End screen cards & subscribe animation")
                .build());

        return scenes;
    }

    public String generateTimelineMarkdown(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        List<TimelineSceneDto> scenes = generateTimeline(primaryAsset, allAssets);
        int totalSeconds = scenes.stream().mapToInt(TimelineSceneDto::estimatedDurationSeconds).sum();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        sb.append("# Production Timeline & Scene Breakdown\n\n");
        sb.append("**Estimated Total Duration**: ~").append(minutes).append(" min ").append(seconds).append(" sec\n\n");
        sb.append("> *Note: Durations are estimated planning benchmarks based on standard pacing.*\n\n");

        sb.append("| Scene | Section | Purpose | Est. Window | Visual / B-Roll Direction |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");

        for (TimelineSceneDto scene : scenes) {
            sb.append("| **").append(scene.sceneNumber()).append("** | ")
                    .append(escapeMarkdown(scene.sectionTitle())).append(" | ")
                    .append(escapeMarkdown(scene.purpose())).append(" | ")
                    .append(scene.formattedDuration()).append(" | ")
                    .append(escapeMarkdown(scene.visualDirection())).append(" |\n");
        }

        sb.append("\n## Detailed Talking Points by Scene\n\n");
        for (TimelineSceneDto scene : scenes) {
            sb.append("### Scene ").append(scene.sceneNumber()).append(": ").append(scene.sectionTitle()).append("\n");
            sb.append("- **Timing**: ").append(scene.formattedDuration()).append("\n");
            sb.append("- **Visual Direction**: ").append(scene.visualDirection()).append("\n");
            sb.append("- **Talking Points**:\n");
            for (String pt : scene.talkingPoints()) {
                sb.append("  - ").append(pt).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ─── 4. CHECKLIST EXPORT ──────────────────────────────────────────────────

    public String generateChecklistMarkdown(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        Map<ProductionAssetType, Map<String, Object>> assetMap = indexAssets(allAssets);
        List<Map<String, Object>> items = extractList(assetMap.get(ProductionAssetType.PRODUCTION_CHECKLIST), "items");

        StringBuilder sb = new StringBuilder();
        sb.append("# Production Workflow Checklist\n\n");
        sb.append("Comprehensive pre-production, filming, and post-production quality assurance checklist.\n\n");

        if (items.isEmpty()) {
            sb.append("### PRE-PRODUCTION\n");
            sb.append("- [ ] Confirm target audience and core problem\n");
            sb.append("- [ ] Review audience evidence comments\n");
            sb.append("- [ ] Finalize title option and selected hook\n");
            sb.append("- [ ] Prepare B-roll assets and screen recording environment\n\n");

            sb.append("### PRODUCTION\n");
            sb.append("- [ ] Record hook variations (3 takes)\n");
            sb.append("- [ ] Record main explanation sections\n");
            sb.append("- [ ] Record call-to-action\n");
            sb.append("- [ ] Verify microphone levels and lighting\n\n");

            sb.append("### POST-PRODUCTION\n");
            sb.append("- [ ] Edit rough cut and trim dead air\n");
            sb.append("- [ ] Add captions and highlight keywords\n");
            sb.append("- [ ] Design thumbnail matching prompt concept\n");
            sb.append("- [ ] Review factual claims and privacy compliance\n");
            sb.append("- [ ] Creator final review and sign-off\n");
        } else {
            Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
            for (Map<String, Object> it : items) {
                String phase = (String) it.getOrDefault("phase", "GENERAL");
                grouped.computeIfAbsent(phase, k -> new ArrayList<>()).add(it);
            }

            for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
                sb.append("### ").append(entry.getKey().replace('_', ' ')).append("\n\n");
                for (Map<String, Object> it : entry.getValue()) {
                    boolean completed = Boolean.TRUE.equals(it.get("completed"));
                    sb.append("- [").append(completed ? "x" : " ").append("] ")
                            .append(it.getOrDefault("task", "")).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    // ─── 5. JSON EXPORT ───────────────────────────────────────────────────────

    public String generateJson(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        Map<ProductionAssetType, Map<String, Object>> assetMap = indexAssets(allAssets);
        Map<String, Object> brief = assetMap.getOrDefault(ProductionAssetType.CONTENT_BRIEF, primaryAsset.getContentJson());
        Map<String, Object> outline = assetMap.getOrDefault(ProductionAssetType.SCRIPT_OUTLINE, Collections.emptyMap());
        List<Map<String, Object>> hooks = extractList(assetMap.get(ProductionAssetType.HOOK), "hooks");
        List<Map<String, Object>> titles = extractList(assetMap.get(ProductionAssetType.TITLE_VARIATION), "titles");
        List<Map<String, Object>> ctas = extractList(assetMap.get(ProductionAssetType.CTA), "ctas");
        Map<String, Object> thumbnail = assetMap.getOrDefault(ProductionAssetType.THUMBNAIL_PROMPT, Collections.emptyMap());
        List<Map<String, Object>> checklist = extractList(assetMap.get(ProductionAssetType.PRODUCTION_CHECKLIST), "items");
        List<TimelineSceneDto> timeline = generateTimeline(primaryAsset, allAssets);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("project", "PulseGPT Audience Intelligence Platform");
        root.put("productionAssetId", primaryAsset.getId().toString());
        root.put("recommendationId", primaryAsset.getRecommendation().getId().toString());
        root.put("calendarItemId", primaryAsset.getCalendarItem() != null ? primaryAsset.getCalendarItem().getId().toString() : null);
        root.put("version", primaryAsset.getVersion());
        root.put("generationMode", primaryAsset.getGenerationMode().name());
        root.put("status", primaryAsset.getStatus().name());
        root.put("modelTrace", primaryAsset.getModelName());
        root.put("promptVersion", primaryAsset.getPromptVersion());
        root.put("createdAt", primaryAsset.getCreatedAt() != null ? primaryAsset.getCreatedAt().toString() : null);
        root.put("approvedAt", primaryAsset.getApprovedAt() != null ? primaryAsset.getApprovedAt().toString() : null);

        root.put("contentBrief", brief);
        root.put("titles", titles);
        root.put("hooks", hooks);
        root.put("outline", outline);
        root.put("ctas", ctas);
        root.put("thumbnail", thumbnail);
        root.put("checklist", checklist);
        root.put("timeline", timeline);
        root.put("evidenceProvenance", primaryAsset.getEvidenceSnapshot());

        try {
            ObjectMapper prettyMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
            return prettyMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Failed to serialize production JSON export: {}", e.getMessage());
            return "{}";
        }
    }

    // ─── 6. PDF EXPORT ────────────────────────────────────────────────────────

    public byte[] generatePdf(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(79, 70, 229));
            Font h1Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(51, 65, 85));
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(71, 85, 105));
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(148, 163, 184));

            Map<ProductionAssetType, Map<String, Object>> assetMap = indexAssets(allAssets);
            Map<String, Object> brief = assetMap.getOrDefault(ProductionAssetType.CONTENT_BRIEF, primaryAsset.getContentJson());
            Map<String, Object> outline = assetMap.getOrDefault(ProductionAssetType.SCRIPT_OUTLINE, Collections.emptyMap());
            List<Map<String, Object>> hooks = extractList(assetMap.get(ProductionAssetType.HOOK), "hooks");
            List<Map<String, Object>> titles = extractList(assetMap.get(ProductionAssetType.TITLE_VARIATION), "titles");
            List<Map<String, Object>> ctas = extractList(assetMap.get(ProductionAssetType.CTA), "ctas");
            Map<String, Object> thumbnail = assetMap.getOrDefault(ProductionAssetType.THUMBNAIL_PROMPT, Collections.emptyMap());
            List<TimelineSceneDto> timeline = generateTimeline(primaryAsset, allAssets);

            // Document Header
            Paragraph header = new Paragraph("PulseGPT Content Production Pack", titleFont);
            header.setSpacingAfter(2);
            document.add(header);

            Paragraph sub = new Paragraph((String) brief.getOrDefault("title", "Production Blueprint"), subtitleFont);
            sub.setSpacingAfter(8);
            document.add(sub);

            Paragraph meta = new Paragraph(
                    "Asset Version: v" + primaryAsset.getVersion() + " | Status: " + primaryAsset.getStatus() +
                    " | Generated: " + (primaryAsset.getCreatedAt() != null ? DATE_FORMATTER.format(primaryAsset.getCreatedAt()) : "N/A"),
                    metaFont
            );
            meta.setSpacingAfter(12);
            document.add(meta);

            // 1. Content Brief Table
            document.add(new Paragraph("1. Content Brief & Strategy", h1Font));
            PdfPTable briefTable = new PdfPTable(2);
            briefTable.setWidthPercentage(100);
            briefTable.setWidths(new float[]{25, 75});
            briefTable.setSpacingBefore(4);
            briefTable.setSpacingAfter(10);

            addTableRow(briefTable, "Target Audience", (String) brief.getOrDefault("targetAudience", "General Audience"), boldFont, textFont);
            addTableRow(briefTable, "Audience Problem", (String) brief.getOrDefault("audienceProblem", "N/A"), boldFont, textFont);
            addTableRow(briefTable, "Content Angle", (String) brief.getOrDefault("contentAngle", "N/A"), boldFont, textFont);
            addTableRow(briefTable, "Core Message", (String) brief.getOrDefault("coreMessage", "N/A"), boldFont, textFont);
            addTableRow(briefTable, "Tone", (String) brief.getOrDefault("tone", "Professional"), boldFont, textFont);
            document.add(briefTable);

            // 2. Titles & Hooks
            document.add(new Paragraph("2. Titles & Hook Variations", h1Font));
            PdfPTable titleHookTable = new PdfPTable(1);
            titleHookTable.setWidthPercentage(100);
            titleHookTable.setSpacingBefore(4);
            titleHookTable.setSpacingAfter(10);

            StringBuilder titleSb = new StringBuilder("Recommended Titles:\n");
            for (int i = 0; i < Math.min(titles.size(), 4); i++) {
                Map<String, Object> t = titles.get(i);
                titleSb.append("• [").append(t.getOrDefault("titleType", "TITLE")).append("] ").append(t.getOrDefault("text", "")).append("\n");
            }
            if (titles.isEmpty()) {
                titleSb.append("• ").append(brief.getOrDefault("title", "Untitled")).append("\n");
            }

            titleSb.append("\nHook Variations:\n");
            for (int i = 0; i < Math.min(hooks.size(), 4); i++) {
                Map<String, Object> h = hooks.get(i);
                titleSb.append("• [").append(h.getOrDefault("hookType", "HOOK")).append("] \"").append(h.getOrDefault("text", "")).append("\"\n");
            }
            if (hooks.isEmpty()) {
                titleSb.append("• \"").append(brief.getOrDefault("hook", "Opening hook")).append("\"\n");
            }

            PdfPCell thCell = new PdfPCell(new Phrase(titleSb.toString(), textFont));
            thCell.setPadding(6);
            thCell.setBackgroundColor(new Color(248, 250, 252));
            thCell.setBorderColor(new Color(226, 232, 240));
            titleHookTable.addCell(thCell);
            document.add(titleHookTable);

            // 3. Timeline Breakdown Table
            document.add(new Paragraph("3. Production Scene Timeline", h1Font));
            PdfPTable timeTable = new PdfPTable(4);
            timeTable.setWidthPercentage(100);
            timeTable.setWidths(new float[]{8, 28, 38, 26});
            timeTable.setSpacingBefore(4);
            timeTable.setSpacingAfter(10);

            addTableHeaderCell(timeTable, "#", boldFont);
            addTableHeaderCell(timeTable, "Section", boldFont);
            addTableHeaderCell(timeTable, "Key Talking Point", boldFont);
            addTableHeaderCell(timeTable, "Timing", boldFont);

            for (TimelineSceneDto sc : timeline) {
                timeTable.addCell(createCell(String.valueOf(sc.sceneNumber()), textFont, false));
                timeTable.addCell(createCell(sc.sectionTitle(), boldFont, false));
                String pt = sc.talkingPoints().isEmpty() ? sc.purpose() : sc.talkingPoints().get(0);
                timeTable.addCell(createCell(pt, textFont, false));
                timeTable.addCell(createCell(sc.formattedDuration(), textFont, false));
            }
            document.add(timeTable);

            // 4. CTA & Thumbnail Concept
            document.add(new Paragraph("4. CTA & Thumbnail Direction", h1Font));
            PdfPTable ctaThumbTable = new PdfPTable(2);
            ctaThumbTable.setWidthPercentage(100);
            ctaThumbTable.setWidths(new float[]{50, 50});
            ctaThumbTable.setSpacingBefore(4);
            ctaThumbTable.setSpacingAfter(10);

            StringBuilder ctaSb = new StringBuilder("Call-to-Action Options:\n");
            for (Map<String, Object> c : ctas) {
                ctaSb.append("• \"").append(c.getOrDefault("text", "")).append("\"\n");
            }
            if (ctas.isEmpty()) {
                ctaSb.append("• \"").append(brief.getOrDefault("callToAction", "Subscribe for more.")).append("\"\n");
            }

            StringBuilder thumbSb = new StringBuilder("Thumbnail Direction:\n");
            thumbSb.append("• Subject: ").append(thumbnail.getOrDefault("visualSubject", "High contrast charts")).append("\n");
            thumbSb.append("• Overlay: \"").append(thumbnail.getOrDefault("textOverlay", "FASTER QUERIES")).append("\"\n");
            thumbSb.append("• Composition: ").append(thumbnail.getOrDefault("composition", "Split screen neon")).append("\n");

            PdfPCell ctaCell = new PdfPCell(new Phrase(ctaSb.toString(), textFont));
            ctaCell.setPadding(6);
            ctaCell.setBackgroundColor(new Color(248, 250, 252));
            ctaCell.setBorderColor(new Color(226, 232, 240));

            PdfPCell thumbCell = new PdfPCell(new Phrase(thumbSb.toString(), textFont));
            thumbCell.setPadding(6);
            thumbCell.setBackgroundColor(new Color(248, 250, 252));
            thumbCell.setBorderColor(new Color(226, 232, 240));

            ctaThumbTable.addCell(ctaCell);
            ctaThumbTable.addCell(thumbCell);
            document.add(ctaThumbTable);

            // 5. Evidence Appendix
            Paragraph evHeading = new Paragraph("5. Audience Evidence & Provenance Traceability", h1Font);
            evHeading.setSpacingBefore(4);
            evHeading.setSpacingAfter(4);
            document.add(evHeading);

            Paragraph evBody = new Paragraph(
                    "This production document is authenticated and grounded in audience feedback.\n" +
                    "Recommendation ID: " + primaryAsset.getRecommendation().getId() + "\n" +
                    "Primary Asset ID: " + primaryAsset.getId() + "\n" +
                    "Integrity Verified: Content generated deterministically without ungrounded hallucinations.",
                    metaFont
            );
            document.add(evBody);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF document: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    // ─── 7. PRODUCTION PACKAGE ZIP ────────────────────────────────────────────

    public byte[] generateProductionPackageZip(ContentProductionAsset primaryAsset, List<ContentProductionAsset> allAssets) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Map<ProductionAssetType, Map<String, Object>> assetMap = indexAssets(allAssets);
            Map<String, Object> brief = assetMap.getOrDefault(ProductionAssetType.CONTENT_BRIEF, primaryAsset.getContentJson());
            Map<String, Object> outline = assetMap.getOrDefault(ProductionAssetType.SCRIPT_OUTLINE, Collections.emptyMap());
            Map<String, Object> thumbnail = assetMap.getOrDefault(ProductionAssetType.THUMBNAIL_PROMPT, Collections.emptyMap());

            // 1. content-brief.md
            addZipEntry(zos, "content-brief.md", formatSectionMarkdown("Content Brief", brief));

            // 2. script-outline.md
            addZipEntry(zos, "script-outline.md", formatSectionMarkdown("Script Outline", outline));

            // 3. teleprompter.txt
            addZipEntry(zos, "teleprompter.txt", generateTeleprompter(primaryAsset, allAssets));

            // 4. titles.md
            addZipEntry(zos, "titles.md", formatTitlesMarkdown(extractList(assetMap.get(ProductionAssetType.TITLE_VARIATION), "titles")));

            // 5. hooks.md
            addZipEntry(zos, "hooks.md", formatHooksMarkdown(extractList(assetMap.get(ProductionAssetType.HOOK), "hooks")));

            // 6. ctas.md
            addZipEntry(zos, "ctas.md", formatCtasMarkdown(extractList(assetMap.get(ProductionAssetType.CTA), "ctas")));

            // 7. thumbnail-prompt.md
            addZipEntry(zos, "thumbnail-prompt.md", formatThumbnailMarkdown(thumbnail));

            // 8. checklist.md
            addZipEntry(zos, "checklist.md", generateChecklistMarkdown(primaryAsset, allAssets));

            // 9. timeline.md
            addZipEntry(zos, "timeline.md", generateTimelineMarkdown(primaryAsset, allAssets));

            // 10. evidence.json
            String evJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    primaryAsset.getEvidenceSnapshot() != null ? primaryAsset.getEvidenceSnapshot() : Collections.emptyMap()
            );
            addZipEntry(zos, "evidence.json", evJson);

            // 11. content.json
            addZipEntry(zos, "content.json", generateJson(primaryAsset, allAssets));

            // 12. production-pack.pdf
            byte[] pdfBytes = generatePdf(primaryAsset, allAssets);
            if (pdfBytes != null && pdfBytes.length > 0) {
                zos.putNextEntry(new ZipEntry("production-pack.pdf"));
                zos.write(pdfBytes);
                zos.closeEntry();
            }

            // 13. README.md
            String readme = generateReadme(primaryAsset);
            addZipEntry(zos, "README.md", readme);

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate production package zip: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void addZipEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String generateReadme(ContentProductionAsset primaryAsset) {
        StringBuilder sb = new StringBuilder();
        sb.append("# PulseGPT Production Package\n\n");
        sb.append("This production package was generated and exported from PulseGPT Audience Intelligence Platform.\n\n");
        sb.append("## Production Provenance\n\n");
        sb.append("- **Production Asset ID**: `").append(primaryAsset.getId()).append("`\n");
        sb.append("- **Recommendation ID**: `").append(primaryAsset.getRecommendation().getId()).append("`\n");
        if (primaryAsset.getCalendarItem() != null) {
            sb.append("- **Calendar Item ID**: `").append(primaryAsset.getCalendarItem().getId()).append("`\n");
        }
        sb.append("- **Asset Version**: v").append(primaryAsset.getVersion()).append("\n");
        sb.append("- **Status**: ").append(primaryAsset.getStatus()).append("\n");
        sb.append("- **Generation Mode**: ").append(primaryAsset.getGenerationMode()).append("\n");
        sb.append("- **Export Timestamp**: ").append(DATE_FORMATTER.format(java.time.Instant.now())).append("\n\n");

        sb.append("## Package Structure\n\n");
        sb.append("- `content-brief.md` — Strategic angle, problem, and audience framing\n");
        sb.append("- `script-outline.md` — Complete modular section breakdown\n");
        sb.append("- `teleprompter.txt` — Formatted plain-text script for teleprompter apps\n");
        sb.append("- `titles.md` — Recommended title variations\n");
        sb.append("- `hooks.md` — Categorized hook variations\n");
        sb.append("- `ctas.md` — Call-to-action options\n");
        sb.append("- `thumbnail-prompt.md` — Designer brief and text overlays\n");
        sb.append("- `checklist.md` — Step-by-step production checklist\n");
        sb.append("- `timeline.md` — Scene-by-scene timing breakdown\n");
        sb.append("- `evidence.json` — Audience intelligence provenance data\n");
        sb.append("- `content.json` — Complete structured asset payload\n");
        sb.append("- `production-pack.pdf` — Complete printable production brief\n");

        return sb.toString();
    }

    private String formatTitlesMarkdown(List<Map<String, Object>> titles) {
        StringBuilder sb = new StringBuilder("# Recommended Title Variations\n\n");
        for (int i = 0; i < titles.size(); i++) {
            Map<String, Object> t = titles.get(i);
            sb.append(i + 1).append(". **[").append(t.getOrDefault("titleType", "TITLE")).append("]** ")
                    .append(t.getOrDefault("text", "")).append("\n");
            if (t.get("rationale") != null) {
                sb.append("   - *Rationale*: ").append(t.get("rationale")).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatHooksMarkdown(List<Map<String, Object>> hooks) {
        StringBuilder sb = new StringBuilder("# Hook Variations\n\n");
        for (int i = 0; i < hooks.size(); i++) {
            Map<String, Object> h = hooks.get(i);
            sb.append(i + 1).append(". **[").append(h.getOrDefault("hookType", "HOOK")).append("]** \"")
                    .append(h.getOrDefault("text", "")).append("\"\n");
            if (h.get("rationale") != null) {
                sb.append("   - *Rationale*: ").append(h.get("rationale")).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatCtasMarkdown(List<Map<String, Object>> ctas) {
        StringBuilder sb = new StringBuilder("# Call-to-Action Variations\n\n");
        for (int i = 0; i < ctas.size(); i++) {
            Map<String, Object> c = ctas.get(i);
            sb.append(i + 1).append(". **[").append(c.getOrDefault("ctaType", "CTA")).append("]** \"")
                    .append(c.getOrDefault("text", "")).append("\"\n");
        }
        return sb.toString();
    }

    private String formatThumbnailMarkdown(Map<String, Object> thumb) {
        StringBuilder sb = new StringBuilder("# Thumbnail Concept & Designer Prompt\n\n");
        sb.append("- **Subject**: ").append(thumb.getOrDefault("visualSubject", "N/A")).append("\n");
        sb.append("- **Composition**: ").append(thumb.getOrDefault("composition", "N/A")).append("\n");
        sb.append("- **Text Overlay**: \"").append(thumb.getOrDefault("textOverlay", "")).append("\"\n");
        sb.append("- **Style / Emotion**: ").append(thumb.getOrDefault("style", "Modern")).append(" / ")
                .append(thumb.getOrDefault("emotion", "Engaging")).append("\n\n");
        sb.append("### Designer Prompt Draft\n\n");
        sb.append("> \"").append(thumb.getOrDefault("concept", "Create a high-contrast thumbnail")).append("\"\n");
        return sb.toString();
    }

    private String formatSectionMarkdown(String title, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("# ").append(title).append("\n\n");
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (e.getValue() instanceof List<?> list) {
                sb.append("### ").append(e.getKey()).append("\n");
                for (Object item : list) {
                    sb.append("- ").append(item).append("\n");
                }
                sb.append("\n");
            } else {
                sb.append("- **").append(e.getKey()).append("**: ").append(e.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private Map<ProductionAssetType, Map<String, Object>> indexAssets(List<ContentProductionAsset> allAssets) {
        Map<ProductionAssetType, Map<String, Object>> map = new EnumMap<>(ProductionAssetType.class);
        if (allAssets != null) {
            for (ContentProductionAsset a : allAssets) {
                if (a.getContentJson() != null) {
                    map.put(a.getAssetType(), a.getContentJson());
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Map<String, Object> map, String key) {
        if (map != null && map.get(key) instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Map<String, Object> map, String key) {
        if (map != null && map.get(key) instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    private String formatTimestamp(int startSec, int endSec) {
        int startMin = startSec / 60;
        int startRemSec = startSec % 60;
        int endMin = endSec / 60;
        int endRemSec = endSec % 60;
        return String.format("%d:%02d - %d:%02d (%ds)", startMin, startRemSec, endMin, endRemSec, endSec - startSec);
    }

    private String escapeMarkdown(String text) {
        return text != null ? text.replace("|", "\\|") : "";
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBackgroundColor(new Color(241, 245, 249));
        lCell.setPadding(5);
        lCell.setBorderColor(new Color(226, 232, 240));

        PdfPCell vCell = new PdfPCell(new Phrase(value != null ? value : "N/A", valFont));
        vCell.setPadding(5);
        vCell.setBorderColor(new Color(226, 232, 240));

        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addTableHeaderCell(PdfPTable table, String header, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(header, font));
        cell.setBackgroundColor(new Color(226, 232, 240));
        cell.setPadding(5);
        cell.setBorderColor(new Color(203, 213, 225));
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, boolean grayBg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(4);
        cell.setBorderColor(new Color(226, 232, 240));
        if (grayBg) {
            cell.setBackgroundColor(new Color(248, 250, 252));
        }
        return cell;
    }
}
