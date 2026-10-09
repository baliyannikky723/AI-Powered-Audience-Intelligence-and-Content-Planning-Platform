from enum import Enum
from typing import List

from pydantic import BaseModel, Field


class IntentLabel(str, Enum):
    QUESTION = "QUESTION"
    FEEDBACK = "FEEDBACK"
    COMPLAINT = "COMPLAINT"
    PRAISE = "PRAISE"
    FEATURE_REQUEST = "FEATURE_REQUEST"
    SPAM = "SPAM"
    OTHER = "OTHER"


class IntentResult(BaseModel):
    label: IntentLabel
    score: float = Field(..., ge=0.0, le=1.0, description="Classification confidence score")


class IntentRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=100, description="List of comment texts to classify")


class IntentBatchResponse(BaseModel):
    model: str
    version: str
    results: List[IntentResult]
