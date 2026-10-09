from fastapi import APIRouter, Depends, HTTPException, status

from app.config import get_settings
from app.schemas.clustering import ClusteringRunRequest, ClusteringRunResponse
from app.schemas.embedding import EmbeddingRequest, EmbeddingResponse
from app.schemas.health import HealthResponse
from app.schemas.intent import IntentBatchResponse, IntentRequest
from app.schemas.processing import (
    BatchCommentProcessRequest,
    BatchCommentProcessResponse,
    CommentProcessRequest,
    CommentProcessResponse,
)
from app.schemas.production import (
    ProductionGenerateRequestSchema,
    ProductionGenerateResponseSchema,
    ProductionRepairRequestSchema,
    ProductionRepairResponseSchema,
)
from app.schemas.recommendation import (
    RecommendationGenerationResponse,
    RecommendationRepairRequest,
    RecommendationRepairResponse,
    RecommendationRequestSchema,
)
from app.schemas.sentiment import SentimentBatchResponse, SentimentRequest
from app.services.cleaning_service import CleaningService
from app.services.clustering_service import ClusteringService
from app.services.ctfidf_service import CTfidfService
from app.services.duplicate_service import DuplicateService
from app.services.embedding_service import EmbeddingService
from app.services.intent_service import IntentService
from app.services.language_service import LanguageService
from app.services.pii_service import PiiService
from app.services.pipeline_service import PipelineService
from app.services.production_generator_service import ProductionGeneratorService
from app.services.recommendation_generator_service import RecommendationGeneratorService
from app.services.sentiment_service import SentimentService
from app.services.spam_service import SpamService
from app.utils.security import verify_api_key

router = APIRouter()

# Instantiate singleton services
_language_service = LanguageService()
_cleaning_service = CleaningService()
_spam_service = SpamService()
_duplicate_service = DuplicateService()
_pii_service = PiiService()
_sentiment_service = SentimentService()
_intent_service = IntentService()
_embedding_service = EmbeddingService()
_ctfidf_service = CTfidfService()
_clustering_service = ClusteringService(ctfidf_service=_ctfidf_service)
_recommendation_service = RecommendationGeneratorService()
_production_service = ProductionGeneratorService()


_pipeline_service = PipelineService(
    language_service=_language_service,
    cleaning_service=_cleaning_service,
    spam_service=_spam_service,
    duplicate_service=_duplicate_service,
    pii_service=_pii_service,
    sentiment_service=_sentiment_service,
    intent_service=_intent_service,
    embedding_service=_embedding_service,
)


@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    settings = get_settings()
    return HealthResponse(
        status="UP",
        service=settings.service_name,
        version=settings.service_version,
        models_loaded=_embedding_service.is_ready,
    )


@router.post(
    "/api/v1/embed",
    response_model=EmbeddingResponse,
    tags=["Embeddings"],
    dependencies=[Depends(verify_api_key)],
)
async def generate_embeddings(request: EmbeddingRequest):
    settings = get_settings()
    if len(request.texts) > settings.max_batch_size:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Batch size exceeds maximum limit of {settings.max_batch_size}",
        )

    embeddings = _embedding_service.embed_texts(request.texts)
    return EmbeddingResponse(
        model=_embedding_service.model_name,
        version=_embedding_service.version,
        dimension=_embedding_service.dimension,
        embeddings=embeddings,
    )


@router.post(
    "/api/v1/sentiment",
    response_model=SentimentBatchResponse,
    tags=["Sentiment"],
    dependencies=[Depends(verify_api_key)],
)
async def analyze_sentiment(request: SentimentRequest):
    settings = get_settings()
    if len(request.texts) > settings.max_batch_size:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Batch size exceeds maximum limit of {settings.max_batch_size}",
        )

    results = _sentiment_service.analyze_batch(request.texts)
    return SentimentBatchResponse(
        model=_sentiment_service.model_name,
        version=_sentiment_service.version,
        results=results,
    )


@router.post(
    "/api/v1/intent",
    response_model=IntentBatchResponse,
    tags=["Intent"],
    dependencies=[Depends(verify_api_key)],
)
async def classify_intent(request: IntentRequest):
    settings = get_settings()
    if len(request.texts) > settings.max_batch_size:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Batch size exceeds maximum limit of {settings.max_batch_size}",
        )

    results = _intent_service.classify_batch(request.texts)
    return IntentBatchResponse(
        model=_intent_service.model_name,
        version=_intent_service.version,
        results=results,
    )


@router.post(
    "/api/v1/process/comment",
    response_model=CommentProcessResponse,
    tags=["Processing"],
    dependencies=[Depends(verify_api_key)],
)
async def process_single_comment(request: CommentProcessRequest):
    return _pipeline_service.process_single(request)


@router.post(
    "/api/v1/process/comments/batch",
    response_model=BatchCommentProcessResponse,
    tags=["Processing"],
    dependencies=[Depends(verify_api_key)],
)
async def process_comment_batch(request: BatchCommentProcessRequest):
    settings = get_settings()
    if len(request.comments) > settings.max_batch_size:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Batch size exceeds maximum limit of {settings.max_batch_size}",
        )
    return _pipeline_service.process_batch(request)


@router.post(
    "/api/v1/cluster",
    response_model=ClusteringRunResponse,
    tags=["Clustering"],
    dependencies=[Depends(verify_api_key)],
)
async def cluster_comments(request: ClusteringRunRequest):
    if len(request.comments) > 5000:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Maximum 5000 comments allowed per clustering run.",
        )
    return _clustering_service.cluster_comments(request)


@router.post(
    "/api/v1/recommendations/generate",
    response_model=RecommendationGenerationResponse,
    tags=["Recommendations"],
    dependencies=[Depends(verify_api_key)],
)
async def generate_recommendations(request: RecommendationRequestSchema):
    """
    Evidence-grounded recommendation generation endpoint.
    Transforms retrieved audience evidence into structured content ideas.
    """
    return _recommendation_service.generate_recommendations(request)


@router.post(
    "/api/v1/recommendations/repair",
    response_model=RecommendationRepairResponse,
    tags=["Recommendations"],
    dependencies=[Depends(verify_api_key)],
)
async def repair_recommendation(request: RecommendationRepairRequest):
    """
    One-shot recommendation repair endpoint to address validation failures.
    """
    return _recommendation_service.repair_recommendation(request)


@router.post(
    "/api/v1/production/generate",
    response_model=ProductionGenerateResponseSchema,
    tags=["Production"],
    dependencies=[Depends(verify_api_key)],
)
async def generate_production_draft(request: ProductionGenerateRequestSchema):
    """
    Evidence-grounded content production draft generation endpoint.
    Transforms recommendations and audience evidence into structured production assets.
    """
    return _production_service.generate_production_draft(request)


@router.post(
    "/api/v1/production/repair",
    response_model=ProductionRepairResponseSchema,
    tags=["Production"],
    dependencies=[Depends(verify_api_key)],
)
async def repair_production_draft(request: ProductionRepairRequestSchema):
    """
    One-shot production draft repair endpoint to address validation failures.
    """
    return _production_service.repair_production_draft(request)

