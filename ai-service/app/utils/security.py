from fastapi import Depends, HTTPException, Request, Security, status
from fastapi.security.api_key import APIKeyHeader

from app.config import Settings, get_settings

API_KEY_HEADER = APIKeyHeader(name="X-Internal-Service-Key", auto_error=False)


async def verify_api_key(
    request: Request,
    api_key_header: str = Security(API_KEY_HEADER),
    settings: Settings = Depends(get_settings),
) -> str:
    # In test environment or if explicitly disabled, skip checking
    if not settings.require_api_key or settings.environment == "test":
        return "authorized-test"

    if not api_key_header or api_key_header != settings.internal_api_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-Internal-Service-Key header",
        )

    return api_key_header
