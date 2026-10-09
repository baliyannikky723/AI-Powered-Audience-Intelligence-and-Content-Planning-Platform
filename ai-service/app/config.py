from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    service_name: str = "pulsegpt-ai-service"
    service_version: str = "0.1.0"
    environment: str = "development"
    debug: bool = False

    # Security
    internal_api_key: str = "pulse-internal-secret-key-3f8a9e"
    require_api_key: bool = True

    # Processing limits
    max_batch_size: int = 100
    max_text_length: int = 5000

    # Model Configuration
    offline_mode: bool = True
    embedding_model_name: str = "sentence-transformers/all-MiniLM-L6-v2"
    embedding_dimension: int = 384
    embedding_model_version: str = "all-MiniLM-L6-v2"
    normalize_embeddings: bool = True

    # Sentiment Configuration
    sentiment_model_name: str = "vader-lexicon-v1"
    sentiment_model_version: str = "1.0.0"

    # Intent Configuration
    intent_model_name: str = "heuristic-intent-classifier"
    intent_model_version: str = "baseline-v1"

    # Language Detector Configuration
    language_detector_version: str = "langdetect-1.0.9"

    # Cleaning & Spam Configuration
    processing_algorithm_version: str = "pulsegpt-nlp-v1"
    spam_threshold: float = 0.65

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
