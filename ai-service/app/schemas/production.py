from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

from app.schemas.recommendation import EvidenceItemSchema


class ContentBriefSchema(BaseModel):
    title: str = Field(..., description="Working title of the content")
    content_type: str = Field(default="VIDEO", description="Content format: VIDEO, ARTICLE, POST")
    platform: str = Field(default="YOUTUBE", description="Target platform: YOUTUBE, LINKEDIN, X, etc.")
    target_audience: str = Field(..., description="Target audience segment grounded in evidence")
    audience_problem: str = Field(..., description="Core audience problem or inquiry identified from evidence")
    audience_evidence: List[str] = Field(default_factory=list, description="Direct evidence summaries / quotes")
    core_message: str = Field(..., description="Single overriding takeaway")
    content_angle: str = Field(..., description="Unique perspective or approach")
    key_points: List[str] = Field(default_factory=list, description="Essential points covered")
    tone: str = Field(default="Informative, Engaging, Authoritative", description="Tone and voice")
    call_to_action: str = Field(..., description="Primary call to action")
    success_objective: str = Field(default="Drive deep engagement and address top audience questions", description="Goal of content")


class ScriptOutlineSectionSchema(BaseModel):
    section_title: str = Field(..., description="Section title or scene name")
    purpose: str = Field(..., description="Goal of this section in the narrative flow")
    talking_points: List[str] = Field(default_factory=list, description="Bulleted talking points")
    estimated_duration_seconds: Optional[int] = Field(None, description="Optional time estimate in seconds")


class ScriptOutlineSchema(BaseModel):
    format: str = Field(default="VIDEO_STRUCTURED_OUTLINE", description="Format descriptor")
    sections: List[ScriptOutlineSectionSchema] = Field(default_factory=list, description="Sequential sections")
    key_takeaway: str = Field(..., description="Final conclusion / core takeaway")


class HookVariantSchema(BaseModel):
    hook_type: str = Field(..., description="Category: QUESTION, PROBLEM, CONTRAST, CURIOSITY, PRACTICAL")
    text: str = Field(..., description="Hook text/script opening line")
    rationale: str = Field(..., description="Why this hook connects with the audience")


class TitleVariantSchema(BaseModel):
    title_type: str = Field(..., description="Type: HOW_TO, DIRECT_BENEFIT, CURIOSITY_GAP, STORY, QUESTION")
    text: str = Field(..., description="Proposed title")
    rationale: str = Field(..., description="Reason for title formulation")


class CtaVariantSchema(BaseModel):
    cta_type: str = Field(..., description="Type: COMMENT_ENGAGEMENT, SUBSCRIBE_FOLLOW, SAVE_SHARE, QUESTION_DISCUSSION")
    text: str = Field(..., description="Call to action line")


class ThumbnailPromptSchema(BaseModel):
    concept: str = Field(..., description="High level thumbnail concept")
    visual_subject: str = Field(..., description="Primary foreground subject or character")
    composition: str = Field(..., description="Arrangement, lighting, and framing")
    text_overlay: str = Field(..., description="Short punchy text overlay (2-4 words max)")
    emotion: str = Field(..., description="Target emotional tone")
    style: str = Field(default="Clean, High-Contrast Modern Digital Art", description="Visual aesthetic style")


class ChecklistItemSchema(BaseModel):
    phase: str = Field(..., description="Phase: PRE_PRODUCTION, PRODUCTION, POST_PRODUCTION")
    task: str = Field(..., description="Actionable checklist task")
    completed: bool = Field(default=False, description="Completion status")


class ProductionChecklistSchema(BaseModel):
    items: List[ChecklistItemSchema] = Field(default_factory=list, description="List of checklist items")



class ProductionDraftPayloadSchema(BaseModel):
    brief: Optional[ContentBriefSchema] = None
    outline: Optional[ScriptOutlineSchema] = None
    hooks: List[HookVariantSchema] = Field(default_factory=list)
    titles: List[TitleVariantSchema] = Field(default_factory=list)
    ctas: List[CtaVariantSchema] = Field(default_factory=list)
    thumbnail: Optional[ThumbnailPromptSchema] = None
    checklist: Optional[ProductionChecklistSchema] = None
    evidence_ids: List[str] = Field(default_factory=list)


class ProductionGenerateRequestSchema(BaseModel):
    request_id: str
    recommendation_id: str
    calendar_item_id: Optional[str] = None
    recommendation_title: str
    recommendation_angle: Optional[str] = None
    content_type: str = "VIDEO"
    platform: str = "YOUTUBE"
    target_audience: Optional[str] = None
    problem_addressed: Optional[str] = None
    key_points: List[str] = Field(default_factory=list)
    topic_name: Optional[str] = None
    topic_keywords: List[str] = Field(default_factory=list)
    evidence: List[EvidenceItemSchema] = Field(default_factory=list)
    mode: str = "EVIDENCE_GROUNDED"  # BASELINE or EVIDENCE_GROUNDED
    requested_asset_types: List[str] = Field(default_factory=list)


class ProductionGenerateResponseSchema(BaseModel):
    request_id: str
    generation_mode: str
    prompt_version: str
    model_name: str
    model_version: str
    draft: ProductionDraftPayloadSchema
    execution_time_ms: float


class ProductionRepairRequestSchema(BaseModel):
    request_id: str
    original_draft: ProductionDraftPayloadSchema
    failed_checks: List[Dict[str, Any]] = Field(default_factory=list)
    evidence: List[EvidenceItemSchema] = Field(default_factory=list)
    recommendation_title: str
    content_type: str = "VIDEO"
    platform: str = "YOUTUBE"


class ProductionRepairResponseSchema(BaseModel):
    request_id: str
    repaired_draft: ProductionDraftPayloadSchema
    repair_explanation: str
    execution_time_ms: float
