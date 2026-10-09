from enum import Enum
from typing import List

from pydantic import BaseModel, Field


class SentimentLabel(str, Enum):
    POSITIVE = "POSITIVE"
    NEUTRAL = "NEUTRAL"
    NEGATIVE = "NEGATIVE"


class SentimentResult(BaseModel):
    label: SentimentLabel
    score: float = Field(..., ge=0.0, le=1.0, description="Confidence score between 0.0 and 1.0")


class SentimentRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=100, description="List of comment texts to score")


class SentimentBatchResponse(BaseModel):
    model: str
    version: str
    results: List[SentimentResult]
