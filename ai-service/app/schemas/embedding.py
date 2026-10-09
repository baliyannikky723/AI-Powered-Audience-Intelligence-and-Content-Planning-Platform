from typing import List

from pydantic import BaseModel, Field


class EmbeddingRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=100, description="List of texts to embed")


class EmbeddingResponse(BaseModel):
    model: str
    version: str
    dimension: int
    embeddings: List[List[float]]
