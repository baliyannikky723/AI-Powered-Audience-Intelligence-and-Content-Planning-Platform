import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import _embedding_service, router
from app.config import get_settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Load embedding model lazily / into memory
    logger.info("Initializing PulseGPT AI Service...")
    _embedding_service.load_model()
    logger.info("PulseGPT AI Service initialized successfully.")
    yield
    # Shutdown
    logger.info("Shutting down PulseGPT AI Service.")


import json
import time
import uuid
from starlette.requests import Request
from starlette.responses import Response

def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title="PulseGPT AI / NLP Processing Service",
        description="FastAPI service for language detection, cleaning, spam detection, PII masking, sentiment analysis, intent classification, and sentence embeddings.",
        version=settings.service_version,
        lifespan=lifespan,
    )

    @app.middleware("http")
    async def observability_middleware(request: Request, call_next):
        correlation_id = request.headers.get("X-Correlation-ID") or request.headers.get("X-Trace-ID") or uuid.uuid4().hex
        start_time = time.perf_counter()
        
        response: Response = await call_next(request)
        
        duration_ms = round((time.perf_counter() - start_time) * 1000, 2)
        response.headers["X-Correlation-ID"] = correlation_id
        response.headers["X-Response-Time"] = f"{duration_ms}ms"

        # Structured operational log without logging raw user content or tokens
        if not request.url.path.endswith("/health"):
            log_entry = {
                "traceId": correlation_id,
                "service": "ai-service",
                "method": request.method,
                "path": request.url.path,
                "status": response.status_code,
                "durationMs": duration_ms
            }
            logger.info("AUDIT_METRIC: %s", json.dumps(log_entry))

        return response

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(router)
    return app


app = create_app()
