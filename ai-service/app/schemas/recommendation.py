from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class EvidenceItemSchema(BaseModel):
    evidence_id: str = Field(..., description="Unique evidence reference identifier (e.g. topic:123, comment:456)")
    source_type: str = Field(..., description="Evidence type: TOPIC, COMMENT, QUESTION, CONTENT_HISTORY, etc.")
    source_id: str = Field(..., description="Underlying entity identifier")
    summary: str = Field(..., description="PII-sanitized textual summary or snippet of the evidence")
    relevance_score: float = Field(default=1.0, ge=0.0, le=1.0, description="Normalized relevance score")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Metadata such as keywords, sentiment, likes, etc.")


class RecommendationRequestSchema(BaseModel):
    request_id: str = Field(..., description="Trace/Request correlation ID")
    topic_id: Optional[str] = Field(default=None, description="Target topic ID if scoped to a topic")
    topic_name: Optional[str] = Field(default=None, description="Target topic title/name")
    content_type: str = Field(default="VIDEO", description="Content format: VIDEO, SHORT, POST, ARTICLE, THREAD")
    goal: str = Field(default="EDUCATIONAL", description="Content goal: EDUCATIONAL, ENGAGEMENT, CONVERSION, ENTERTAINMENT")
    target_audience: Optional[str] = Field(default="Tech Enthusiasts & Developers", description="Target audience persona")
    max_ideas: int = Field(default=3, ge=1, le=10, description="Number of ideas to generate")
    mode: str = Field(default="EVIDENCE_GROUNDED", description="Generation mode: EVIDENCE_GROUNDED or BASELINE")
    evidence: List[EvidenceItemSchema] = Field(default_factory=list, description="Retrieved and ranked audience evidence items")
    content_history: List[Dict[str, Any]] = Field(default_factory=list, description="Recent creator content history for context and novelty")
    previous_recommendations: List[Dict[str, Any]] = Field(default_factory=list, description="Prior recommendations to prevent duplication")


class RecommendationDraftSchema(BaseModel):
    title: str = Field(..., min_length=5, max_length=500, description="Compelling, descriptive content title")
    content_type: str = Field(default="VIDEO", description="Content format")
    angle: str = Field(..., min_length=10, description="Unique angle, narrative hook, or thesis")
    target_audience: str = Field(..., description="Audience demographic or segment addressed")
    problem_addressed: str = Field(..., description="Specific audience question, pain point, or inquiry addressed")
    key_points: List[str] = Field(..., min_length=2, description="Structured outline or key talking points")
    hook: str = Field(..., min_length=10, description="3-second opening hook or visual attention grabber")
    call_to_action: str = Field(..., description="Audience call to action (e.g. comment below, subscribe, try tool)")
    evidence_ids: List[str] = Field(default_factory=list, description="IDs of evidence supporting this idea")
    confidence: float = Field(default=0.85, ge=0.0, le=1.0, description="Confidence score")
    reason: str = Field(..., description="Evidence justification explaining why this content will perform well")


class RecommendationGenerationResponse(BaseModel):
    request_id: str
    generation_mode: str
    prompt_version: str
    model_name: str
    model_version: str
    drafts: List[RecommendationDraftSchema]
    execution_time_ms: float


class RecommendationRepairRequest(BaseModel):
    request_id: str
    original_draft: RecommendationDraftSchema
    failed_checks: List[Dict[str, Any]] = Field(..., description="List of failed check names and error reasons")
    evidence: List[EvidenceItemSchema] = Field(default_factory=list, description="Original evidence context")
    mode: str = Field(default="EVIDENCE_GROUNDED", description="Generation mode")


class RecommendationRepairResponse(BaseModel):
    request_id: str
    repaired_draft: RecommendationDraftSchema
    repair_explanation: str
    execution_time_ms: float
