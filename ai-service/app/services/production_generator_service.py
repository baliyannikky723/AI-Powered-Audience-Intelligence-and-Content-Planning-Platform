import re
import time
from typing import Any, Dict, List, Optional, Set

from app.schemas.production import (
    ChecklistItemSchema,
    ContentBriefSchema,
    CtaVariantSchema,
    HookVariantSchema,
    ProductionChecklistSchema,
    ProductionDraftPayloadSchema,
    ProductionGenerateRequestSchema,
    ProductionGenerateResponseSchema,
    ProductionRepairRequestSchema,
    ProductionRepairResponseSchema,
    ScriptOutlineSchema,
    ScriptOutlineSectionSchema,
    ThumbnailPromptSchema,
    TitleVariantSchema,
)
from app.schemas.recommendation import EvidenceItemSchema


class ProductionGeneratorService:
    """
    Evidence-Grounded Content Production Copilot Service.
    
    Transforms validated recommendations and audience intelligence into
    structured production artifacts (briefs, outlines, hooks, titles, CTAs,
    thumbnail concepts, and checklists) while maintaining strict prompt-injection
    defense and deterministic evidence traceability.
    """

    def __init__(
        self,
        prompt_version: str = "PRODUCTION_COPILOT_PROMPT_V1",
        model_name: str = "pulsegpt-production-copilot-v1",
        model_version: str = "1.0.0",
    ):
        self.prompt_version = prompt_version
        self.model_name = model_name
        self.model_version = model_version

    def generate_production_draft(
        self, request: ProductionGenerateRequestSchema
    ) -> ProductionGenerateResponseSchema:
        start_time = time.perf_counter()

        # Defend against prompt injection in untrusted fields
        sanitized_title = self._sanitize_prompt_injection(request.recommendation_title)
        sanitized_angle = self._sanitize_prompt_injection(request.recommendation_angle or "")
        sanitized_topic = self._sanitize_prompt_injection(request.topic_name or "General Topic")
        sanitized_problem = self._sanitize_prompt_injection(request.problem_addressed or "")
        sanitized_audience = self._sanitize_prompt_injection(request.target_audience or "Core Audience")

        is_baseline = request.mode == "BASELINE"
        evidence_list = [] if is_baseline else request.evidence
        evidence_ids = [e.evidence_id for e in evidence_list]

        # 1. Content Brief
        brief = self._generate_brief(
            title=sanitized_title,
            angle=sanitized_angle,
            topic=sanitized_topic,
            problem=sanitized_problem,
            audience=sanitized_audience,
            content_type=request.content_type,
            platform=request.platform,
            key_points=request.key_points,
            evidence=evidence_list,
            is_baseline=is_baseline,
        )

        # 2. Script / Content Outline
        outline = self._generate_outline(
            title=sanitized_title,
            topic=sanitized_topic,
            content_type=request.content_type,
            key_points=request.key_points,
            evidence=evidence_list,
        )

        # 3. Hook Variations (3-5 variants)
        hooks = self._generate_hooks(
            title=sanitized_title,
            topic=sanitized_topic,
            evidence=evidence_list,
        )

        # 4. Title Variations (3-5 variants)
        titles = self._generate_titles(
            title=sanitized_title,
            topic=sanitized_topic,
            content_type=request.content_type,
        )

        # 5. CTA Variations (2-3 options)
        ctas = self._generate_ctas(
            platform=request.platform,
            content_type=request.content_type,
            topic=sanitized_topic,
        )

        # 6. Thumbnail Concept / Prompt (Textual concept only)
        thumbnail = self._generate_thumbnail_prompt(
            title=sanitized_title,
            topic=sanitized_topic,
            platform=request.platform,
        )

        # 7. Production Checklist
        checklist = self._generate_checklist(
            content_type=request.content_type,
            platform=request.platform,
        )

        draft = ProductionDraftPayloadSchema(
            brief=brief,
            outline=outline,
            hooks=hooks,
            titles=titles,
            ctas=ctas,
            thumbnail=thumbnail,
            checklist=checklist,
            evidence_ids=evidence_ids,
        )

        execution_time = (time.perf_counter() - start_time) * 1000.0

        return ProductionGenerateResponseSchema(
            request_id=request.request_id,
            generation_mode=request.mode,
            prompt_version=self.prompt_version,
            model_name=self.model_name,
            model_version=self.model_version,
            draft=draft,
            execution_time_ms=round(execution_time, 2),
        )

    def repair_production_draft(
        self, request: ProductionRepairRequestSchema
    ) -> ProductionRepairResponseSchema:
        start_time = time.perf_counter()
        orig = request.original_draft
        failed_check_names = [fc.get("name", "") for fc in request.failed_checks]
        failed_reasons = [fc.get("reason", "") for fc in request.failed_checks]

        valid_evidence_ids = {e.evidence_id for e in request.evidence}

        # 1. Repair Evidence IDs
        repaired_evidence_ids = [eid for eid in orig.evidence_ids if eid in valid_evidence_ids]
        if not repaired_evidence_ids and valid_evidence_ids:
            repaired_evidence_ids = [list(valid_evidence_ids)[0]]

        # 2. Repair Brief (redact PII, eliminate unsupported claims)
        repaired_brief = None
        if orig.brief:
            repaired_core = self._clean_unsupported_claims(self._sanitize_pii(orig.brief.core_message))
            repaired_angle = self._clean_unsupported_claims(self._sanitize_pii(orig.brief.content_angle))
            repaired_problem = self._clean_unsupported_claims(self._sanitize_pii(orig.brief.audience_problem))
            repaired_title = self._clean_unsupported_claims(self._sanitize_pii(orig.brief.title))
            repaired_points = [
                self._clean_unsupported_claims(self._sanitize_pii(p)) for p in orig.brief.key_points
            ]
            repaired_brief = ContentBriefSchema(
                title=repaired_title if len(repaired_title) >= 5 else request.recommendation_title,
                content_type=orig.brief.content_type,
                platform=orig.brief.platform,
                target_audience=self._sanitize_pii(orig.brief.target_audience),
                audience_problem=repaired_problem,
                audience_evidence=[self._sanitize_pii(e) for e in orig.brief.audience_evidence],
                core_message=repaired_core,
                content_angle=repaired_angle,
                key_points=repaired_points if len(repaired_points) >= 2 else ["Key overview", "Detailed solution"],
                tone=orig.brief.tone,
                call_to_action=self._sanitize_pii(orig.brief.call_to_action),
                success_objective=orig.brief.success_objective,
            )

        # 3. Repair Hooks
        repaired_hooks = []
        for h in orig.hooks:
            cleaned_text = self._clean_unsupported_claims(self._sanitize_pii(h.text))
            if len(cleaned_text) < 10:
                cleaned_text = f"Discover what you need to know about {request.recommendation_title}."
            repaired_hooks.append(
                HookVariantSchema(
                    hook_type=h.hook_type,
                    text=cleaned_text,
                    rationale=self._sanitize_pii(h.rationale),
                )
            )
        if len(repaired_hooks) < 3:
            repaired_hooks = self._generate_hooks(request.recommendation_title, "Audience Insights", request.evidence)

        # 4. Repair Titles
        repaired_titles = []
        for t in orig.titles:
            cleaned_text = self._clean_unsupported_claims(self._sanitize_pii(t.text))
            if len(cleaned_text) < 5:
                cleaned_text = request.recommendation_title
            repaired_titles.append(
                TitleVariantSchema(
                    title_type=t.title_type,
                    text=cleaned_text,
                    rationale=self._sanitize_pii(t.rationale),
                )
            )
        if len(repaired_titles) < 3:
            repaired_titles = self._generate_titles(request.recommendation_title, "Audience Insights", request.content_type)

        # 5. Repair Outline
        repaired_outline = orig.outline
        if not repaired_outline or len(repaired_outline.sections) < 3:
            repaired_outline = self._generate_outline(
                request.recommendation_title, "Audience Insights", request.content_type, [], request.evidence
            )
        else:
            repaired_sections = []
            for s in repaired_outline.sections:
                repaired_sections.append(
                    ScriptOutlineSectionSchema(
                        section_title=self._clean_unsupported_claims(self._sanitize_pii(s.section_title)),
                        purpose=self._clean_unsupported_claims(self._sanitize_pii(s.purpose)),
                        talking_points=[
                            self._clean_unsupported_claims(self._sanitize_pii(tp)) for tp in s.talking_points
                        ],
                        estimated_duration_seconds=s.estimated_duration_seconds,
                    )
                )
            repaired_outline = ScriptOutlineSchema(
                format=orig.outline.format,
                sections=repaired_sections,
                key_takeaway=self._clean_unsupported_claims(self._sanitize_pii(orig.outline.key_takeaway)),
            )

        # 6. Repair CTAs
        repaired_ctas = []
        for c in orig.ctas:
            repaired_ctas.append(
                CtaVariantSchema(
                    cta_type=c.cta_type,
                    text=self._clean_unsupported_claims(self._sanitize_pii(c.text)),
                )
            )
        if len(repaired_ctas) < 2:
            repaired_ctas = self._generate_ctas(request.platform, request.content_type, "Insights")

        # 7. Repair Thumbnail Prompt
        repaired_thumbnail = orig.thumbnail
        if not repaired_thumbnail:
            repaired_thumbnail = self._generate_thumbnail_prompt(request.recommendation_title, "Insights", request.platform)
        else:
            repaired_thumbnail = ThumbnailPromptSchema(
                concept=self._clean_unsupported_claims(self._sanitize_pii(orig.thumbnail.concept)),
                visual_subject=self._sanitize_pii(orig.thumbnail.visual_subject),
                composition=self._sanitize_pii(orig.thumbnail.composition),
                text_overlay=self._sanitize_pii(orig.thumbnail.text_overlay)[:30],
                emotion=orig.thumbnail.emotion,
                style=orig.thumbnail.style,
            )

        # 8. Repair Checklist
        repaired_checklist = orig.checklist or self._generate_checklist(request.content_type, request.platform)

        repaired_draft = ProductionDraftPayloadSchema(
            brief=repaired_brief,
            outline=repaired_outline,
            hooks=repaired_hooks,
            titles=repaired_titles,
            ctas=repaired_ctas,
            thumbnail=repaired_thumbnail,
            checklist=repaired_checklist,
            evidence_ids=repaired_evidence_ids,
        )

        explanation = f"Repaired production draft validation issues: {'; '.join(failed_reasons)}"
        execution_time = (time.perf_counter() - start_time) * 1000.0

        return ProductionRepairResponseSchema(
            request_id=request.request_id,
            repaired_draft=repaired_draft,
            repair_explanation=explanation,
            execution_time_ms=round(execution_time, 2),
        )

    # --------------------------------------------------------------------------
    # Internal Generation Helpers
    # --------------------------------------------------------------------------

    def _generate_brief(
        self,
        title: str,
        angle: str,
        topic: str,
        problem: str,
        audience: str,
        content_type: str,
        platform: str,
        key_points: List[str],
        evidence: List[EvidenceItemSchema],
        is_baseline: bool,
    ) -> ContentBriefSchema:
        ev_summaries = [e.summary for e in evidence[:3]] if evidence else []
        if is_baseline:
            ev_summaries = ["Industry trend baseline (no audience signals linked)"]

        pts = key_points if key_points else [
            f"Core conceptual breakdown of {topic}",
            f"Practical implementation steps and walkthrough",
            f"Common pitfalls and best practices",
        ]

        return ContentBriefSchema(
            title=title,
            content_type=content_type,
            platform=platform,
            target_audience=audience or f"{topic} Practitioners & Community",
            audience_problem=problem or f"Audience seeks clarity and actionable guidance regarding {topic}.",
            audience_evidence=ev_summaries,
            core_message=f"A practical and grounded guide to mastering {topic} with clarity and proven methods.",
            content_angle=angle or f"Hands-on walkthrough focused on real-world questions from the audience.",
            key_points=pts,
            tone="Informative, Direct, Engaging, Solution-Oriented",
            call_to_action="Let us know your experience in the comments and subscribe for more deep dives!",
            success_objective="Address core viewer inquiries while providing immediate actionable value.",
        )

    def _generate_outline(
        self,
        title: str,
        topic: str,
        content_type: str,
        key_points: List[str],
        evidence: List[EvidenceItemSchema],
    ) -> ScriptOutlineSchema:
        ev_sample = evidence[0].summary if evidence else f"audience interest in {topic}"
        pts = key_points if key_points else [f"Analyzing {topic}", "Execution steps", "Optimization takeaways"]

        if content_type.upper() in ["VIDEO", "SHORT", "REEL"]:
            # 10-part video structure
            sections = [
                ScriptOutlineSectionSchema(
                    section_title="1. Hook",
                    purpose="Capture immediate attention and frame the core viewer struggle",
                    talking_points=[f"Highlight the common misconception or burning question about {topic}"],
                    estimated_duration_seconds=15,
                ),
                ScriptOutlineSectionSchema(
                    section_title="2. Introduction",
                    purpose="Set expectations and state the value proposition",
                    talking_points=[f"Briefly introduce why {topic} is critical right now"],
                    estimated_duration_seconds=25,
                ),
                ScriptOutlineSectionSchema(
                    section_title="3. Problem Statement",
                    purpose="Validate audience pain points with evidence",
                    talking_points=[f"Reference feedback: '{ev_sample}'", "Explain why traditional approaches fail"],
                    estimated_duration_seconds=45,
                ),
                ScriptOutlineSectionSchema(
                    section_title="4. Context & Background",
                    purpose="Provide necessary background fundamentals",
                    talking_points=["Foundational concepts required before diving in"],
                    estimated_duration_seconds=40,
                ),
                ScriptOutlineSectionSchema(
                    section_title="5. Main Point 1 — Fundamentals",
                    purpose="Address the primary question systematically",
                    talking_points=[pts[0] if len(pts) > 0 else f"First key pillar of {topic}"],
                    estimated_duration_seconds=60,
                ),
                ScriptOutlineSectionSchema(
                    section_title="6. Main Point 2 — Practical Application",
                    purpose="Demonstrate concrete implementation steps",
                    talking_points=[pts[1] if len(pts) > 1 else f"Step-by-step practical workflow"],
                    estimated_duration_seconds=90,
                ),
                ScriptOutlineSectionSchema(
                    section_title="7. Main Point 3 — Common Pitfalls",
                    purpose="Highlight what to avoid",
                    talking_points=[pts[2] if len(pts) > 2 else f"Mistakes to avoid and edge cases"],
                    estimated_duration_seconds=60,
                ),
                ScriptOutlineSectionSchema(
                    section_title="8. Real-world Example / Walkthrough",
                    purpose="Concrete case study or benchmark demonstration",
                    talking_points=["Walk through a concrete real-world scenario end-to-end"],
                    estimated_duration_seconds=75,
                ),
                ScriptOutlineSectionSchema(
                    section_title="9. Summary & Synthesis",
                    purpose="Recap main actionable takeaways",
                    talking_points=["Rapid summary of key takeaways and actionable checklist"],
                    estimated_duration_seconds=30,
                ),
                ScriptOutlineSectionSchema(
                    section_title="10. Call to Action",
                    purpose="Direct audience engagement",
                    talking_points=["Ask a targeted discussion question in the comments", "Channel subscribe prompt"],
                    estimated_duration_seconds=15,
                ),
            ]
            format_name = "VIDEO_10_PART_STRUCTURE"
        else:
            # 7-part article / post structure
            sections = [
                ScriptOutlineSectionSchema(
                    section_title="1. Headline & Hook",
                    purpose="Draw the reader into the central thesis",
                    talking_points=[f"Compelling framing of {topic} challenges"],
                ),
                ScriptOutlineSectionSchema(
                    section_title="2. Introduction & Stakes",
                    purpose="Explain why this topic matters right now",
                    talking_points=["Current landscape and importance"],
                ),
                ScriptOutlineSectionSchema(
                    section_title="3. The Core Problem",
                    purpose="Detail the specific audience challenge",
                    talking_points=[f"Addressing: '{ev_sample}'"],
                ),
                ScriptOutlineSectionSchema(
                    section_title="4. Main Sections / Deep Dive",
                    purpose="Step-by-step breakdown of solutions",
                    talking_points=pts,
                ),
                ScriptOutlineSectionSchema(
                    section_title="5. Evidence & Practical Examples",
                    purpose="Substantiate recommendations with concrete examples",
                    talking_points=["Benchmark results and illustrated example"],
                ),
                ScriptOutlineSectionSchema(
                    section_title="6. Conclusion & Summary",
                    purpose="Synthesize takeaways into action items",
                    talking_points=["Key takeaway points to remember"],
                ),
                ScriptOutlineSectionSchema(
                    section_title="7. Call to Action",
                    purpose="Prompt reader interaction and discussion",
                    talking_points=["Discussion prompt for readers to share their workflow"],
                ),
            ]
            format_name = "WRITTEN_7_PART_STRUCTURE"

        return ScriptOutlineSchema(
            format=format_name,
            sections=sections,
            key_takeaway=f"Clear, evidence-backed mastery of {topic} with practical implementation takeaways.",
        )

    def _generate_hooks(
        self,
        title: str,
        topic: str,
        evidence: List[EvidenceItemSchema],
    ) -> List[HookVariantSchema]:
        ev_sample = evidence[0].summary if evidence else f"questions on {topic}"

        return [
            HookVariantSchema(
                hook_type="QUESTION",
                text=f"Have you ever struggled to get consistent results with {topic}? You're not alone.",
                rationale="Directly questions the viewer's experience, building immediate relatability.",
            ),
            HookVariantSchema(
                hook_type="PROBLEM",
                text=f"Most people approach {topic} the hard way. Here is the exact fix based on real feedback.",
                rationale="Identifies an existing pain point and promises an actionable solution.",
            ),
            HookVariantSchema(
                hook_type="CONTRAST",
                text=f"You might think {topic} is complicated, but with the right framework, it takes just minutes.",
                rationale="Contrasts perceived difficulty with practical simplicity to spark curiosity.",
            ),
            HookVariantSchema(
                hook_type="CURIOSITY",
                text=f"There's one crucial detail about {topic} that rarely gets explained properly.",
                rationale="Creates a curiosity gap without resorting to deceptive clickbait.",
            ),
            HookVariantSchema(
                hook_type="PRACTICAL",
                text=f"In this breakdown, we solve: '{ev_sample[:80]}' with a clear step-by-step guide.",
                rationale="Immediately signals utility grounded in verified audience inquiries.",
            ),
        ]

    def _generate_titles(
        self,
        title: str,
        topic: str,
        content_type: str,
    ) -> List[TitleVariantSchema]:
        return [
            TitleVariantSchema(
                title_type="HOW_TO",
                text=f"How to Master {topic}: The Step-by-Step Guide",
                rationale="Clear, searchable, and intent-focused for high discoverability.",
            ),
            TitleVariantSchema(
                title_type="DIRECT_BENEFIT",
                text=f"Fix Your {topic} Issues Fast (Clear Walkthrough)",
                rationale="Highlights the immediate payoff and resolution.",
            ),
            TitleVariantSchema(
                title_type="CURIOSITY_GAP",
                text=f"The Truth About {topic}: What Actually Works",
                rationale="Encourages viewers seeking authoritative clarity over hype.",
            ),
            TitleVariantSchema(
                title_type="QUESTION",
                text=f"Are You Making These Mistakes with {topic}?",
                rationale="Triggers self-evaluation and high click-through intent.",
            ),
        ]

    def _generate_ctas(
        self,
        platform: str,
        content_type: str,
        topic: str,
    ) -> List[CtaVariantSchema]:
        p = platform.upper()
        if "YOUTUBE" in p:
            return [
                CtaVariantSchema(
                    cta_type="COMMENT_ENGAGEMENT",
                    text=f"What is your biggest hurdle with {topic}? Drop a comment below and let's discuss!",
                ),
                CtaVariantSchema(
                    cta_type="SUBSCRIBE_FOLLOW",
                    text="If you found this breakdown helpful, hit subscribe for more evidence-backed creator insights.",
                ),
                CtaVariantSchema(
                    cta_type="QUESTION_DISCUSSION",
                    text=f"Which method do you currently use for {topic}? Let me know in the comments!",
                ),
            ]
        elif "LINKEDIN" in p or "ARTICLE" in content_type.upper():
            return [
                CtaVariantSchema(
                    cta_type="QUESTION_DISCUSSION",
                    text=f"How are you currently handling {topic} in your team? Share your thoughts below.",
                ),
                CtaVariantSchema(
                    cta_type="SAVE_SHARE",
                    text="Save this post for your next review or repost it to help your network.",
                ),
            ]
        else:
            return [
                CtaVariantSchema(
                    cta_type="COMMENT_ENGAGEMENT",
                    text=f"Drop your questions about {topic} below!",
                ),
                CtaVariantSchema(
                    cta_type="SAVE_SHARE",
                    text="Bookmark this guide for quick reference!",
                ),
            ]

    def _generate_thumbnail_prompt(
        self,
        title: str,
        topic: str,
        platform: str,
    ) -> ThumbnailPromptSchema:
        return ThumbnailPromptSchema(
            concept=f"High-impact visual demonstrating clarity versus confusion regarding {topic}",
            visual_subject="Creator looking focused at a clear illuminated interface/diagram",
            composition="Close-up with high depth of field, bold 3-point studio lighting, rule of thirds",
            text_overlay=f"FIX {topic.upper()[:12]}" if len(topic) <= 12 else "SOLVE THIS",
            emotion="Curious, focused, authoritative",
            style="Clean, high-contrast modern digital photography with vibrant accents",
        )

    def _generate_checklist(
        self,
        content_type: str,
        platform: str,
    ) -> ProductionChecklistSchema:
        return ProductionChecklistSchema(
            items=[
                ChecklistItemSchema(
                    phase="PRE_PRODUCTION",
                    task="Verify audience evidence signals and confirm outline alignment",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="PRE_PRODUCTION",
                    task="Select final title variation and opening hook",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="PRE_PRODUCTION",
                    task="Prepare code snippets, diagrams, or visual examples",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="PRODUCTION",
                    task="Record the hook and introductory problem statement",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="PRODUCTION",
                    task="Record core sections and hands-on walkthroughs",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="PRODUCTION",
                    task="Record call-to-action and outro sequence",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="POST_PRODUCTION",
                    task="Edit sequence, apply audio leveling and noise suppression",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="POST_PRODUCTION",
                    task="Design thumbnail asset based on concept prompt",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="POST_PRODUCTION",
                    task="Review final draft against factual claims and evidence",
                    completed=False,
                ),
                ChecklistItemSchema(
                    phase="POST_PRODUCTION",
                    task="Perform final creator review and sign-off",
                    completed=False,
                ),
            ]
        )

    def _sanitize_prompt_injection(self, text: str) -> str:
        """Sanitizes untrusted input text against prompt injection patterns."""
        if not text:
            return ""
        patterns = [
            r"ignore (all )?previous instructions",
            r"reveal (the )?system prompt",
            r"system:\s*",
            r"<\|im_start\|>",
            r"<\|im_end\|>",
            r"assistant:\s*",
            r"you are now a",
            r"disregard all rules",
        ]
        sanitized = text
        for p in patterns:
            sanitized = re.sub(p, "[FILTERED_INSTRUCTION]", sanitized, flags=re.IGNORECASE)
        return sanitized.strip()

    def _clean_unsupported_claims(self, text: str) -> str:
        """Replaces fabricated statistics or hyperbolic claims with grounded language."""
        if not text:
            return ""
        for bad_stat in [r"\b\d{1,3}%\s*(of\s*)?", r"\b\d+x\b", r"\beveryone\b", r"\ball viewers\b"]:
            text = re.sub(bad_stat, "many viewers ", text, flags=re.IGNORECASE)
        return " ".join(text.split())

    def _sanitize_pii(self, text: str) -> str:
        """Redacts emails, phone numbers, and potential tokens."""
        if not text:
            return ""
        text = re.sub(r"[\w\.-]+@[\w\.-]+\.\w+", "[EMAIL_REDACTED]", text)
        text = re.sub(r"\b\d{3}[-.\s]??\d{3}[-.\s]??\d{4}\b", "[PHONE_REDACTED]", text)
        text = re.sub(r"(pulse-|sk-|ey[A-Za-z0-9-_]{20,})", "[TOKEN_REDACTED]", text, flags=re.IGNORECASE)
        return text
