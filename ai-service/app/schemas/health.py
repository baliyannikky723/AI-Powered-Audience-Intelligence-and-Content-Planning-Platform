from pydantic import BaseModel, Field


class HealthResponse(BaseModel):
    status: str = Field(..., description="Service status: UP or DOWN")
    service: str = Field(..., description="Service name")
    version: str = Field(..., description="Service version")
    models_loaded: bool = Field(default=True, description="Whether core models are loaded and ready")
