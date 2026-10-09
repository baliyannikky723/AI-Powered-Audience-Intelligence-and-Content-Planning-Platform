from typing import List, Optional

from pydantic import BaseModel, Field

from app.schemas.intent import IntentResult
from app.schemas.sentiment import SentimentResult


class ModelVersions(BaseModel):
    language: str
    sentiment: str
    intent: str
    embedding: str
    algorithm: str


class CommentProcessRequest(BaseModel):
    comment_id: str = Field(..., description="Unique comment identifier from platform / database")
    text: str = Field(..., max_length=10000, description="Raw comment text")
    language_hint: Optional[str] = Field(default=None, description="Optional caller hint for language")
    model_version: Optional[str] = Field(default=None, description="Optional target processing version")


class CommentProcessResponse(BaseModel):
    comment_id: str
    language: str
    is_hinglish: bool = False
    cleaned_text: str
    normalized_text: str
    is_spam: bool = False
    spam_score: float = 0.0
    spam_reasons: List[str] = Field(default_factory=list)
    is_duplicate: bool = False
    pii_detected: bool = False
    pii_masked_text: str
    sentiment: SentimentResult
    intent: IntentResult
    priority: str = Field(default="MEDIUM", description="Suggested priority: HIGH, MEDIUM, LOW")
    embedding: List[float] = Field(default_factory=list)
    embedding_dimension: int = 384
    model_versions: ModelVersions
    success: bool = True
    error_message: Optional[str] = None


class BatchCommentProcessRequest(BaseModel):
    comments: List[CommentProcessRequest] = Field(
        ..., min_length=1, max_length=100, description="List of comments to process in batch"
    )


class BatchCommentProcessResponse(BaseModel):
    results: List[CommentProcessResponse]
    total: int
    successful: int
    failed: int
