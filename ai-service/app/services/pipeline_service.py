import logging
from typing import List

from app.config import get_settings
from app.schemas.intent import IntentLabel
from app.schemas.processing import (
    BatchCommentProcessRequest,
    BatchCommentProcessResponse,
    CommentProcessRequest,
    CommentProcessResponse,
    ModelVersions,
)
from app.schemas.sentiment import SentimentLabel
from app.services.cleaning_service import CleaningService
from app.services.duplicate_service import DuplicateService
from app.services.embedding_service import EmbeddingService
from app.services.intent_service import IntentService
from app.services.language_service import LanguageService
from app.services.pii_service import PiiService
from app.services.sentiment_service import SentimentService
from app.services.spam_service import SpamService

logger = logging.getLogger(__name__)


class PipelineService:

    def __init__(
        self,
        language_service: LanguageService,
        cleaning_service: CleaningService,
        spam_service: SpamService,
        duplicate_service: DuplicateService,
        pii_service: PiiService,
        sentiment_service: SentimentService,
        intent_service: IntentService,
        embedding_service: EmbeddingService,
    ):
        self.settings = get_settings()
        self.language_service = language_service
        self.cleaning_service = cleaning_service
        self.spam_service = spam_service
        self.duplicate_service = duplicate_service
        self.pii_service = pii_service
        self.sentiment_service = sentiment_service
        self.intent_service = intent_service
        self.embedding_service = embedding_service

    def process_single(self, request: CommentProcessRequest) -> CommentProcessResponse:
        """Processes a single comment through the entire NLP pipeline."""
        return self.process_batch(BatchCommentProcessRequest(comments=[request])).results[0]

    def process_batch(self, request: BatchCommentProcessRequest) -> BatchCommentProcessResponse:
        """Processes a batch of comments deterministically."""
        comments = request.comments
        if not comments:
            return BatchCommentProcessResponse(results=[], total=0, successful=0, failed=0)

        results: List[CommentProcessResponse] = []
        cleaned_texts: List[str] = []
        normalized_texts: List[str] = []

        # 1. Clean & Normalize all items
        for item in comments:
            raw = item.text if item.text is not None else ""
            cleaned, norm = self.cleaning_service.clean_and_normalize(raw)
            cleaned_texts.append(cleaned)
            normalized_texts.append(norm)

        # 2. Batch duplicate detection
        duplicate_flags = self.duplicate_service.mark_batch_duplicates(normalized_texts)

        # 3. Batch embeddings
        embeddings = self.embedding_service.embed_texts(cleaned_texts)

        # 4. Construct per-comment responses
        successful_count = 0
        failed_count = 0

        model_versions = ModelVersions(
            language=self.language_service.version,
            sentiment=self.sentiment_service.version,
            intent=self.intent_service.version,
            embedding=self.embedding_service.version,
            algorithm=self.settings.processing_algorithm_version,
        )

        for i, item in enumerate(comments):
            try:
                raw = item.text or ""
                cleaned = cleaned_texts[i]
                norm = normalized_texts[i]
                is_duplicate, _ = duplicate_flags[i]
                emb = embeddings[i] if i < len(embeddings) else []

                # Language detection
                lang, is_hinglish = self.language_service.detect_language(cleaned, item.language_hint)

                # Spam detection
                is_spam, spam_score, spam_reasons = self.spam_service.evaluate_spam(cleaned)

                # PII masking (applied on cleaned text for processed display)
                pii_detected, pii_masked_text = self.pii_service.mask_pii(cleaned)

                # Sentiment
                sentiment_res = self.sentiment_service.analyze_sentiment(cleaned)

                # Intent
                intent_res = self.intent_service.classify_intent(cleaned, is_spam=is_spam)

                # Priority assignment heuristic:
                # HIGH: Complaint, Question, Feature Request with strong sentiment
                # LOW: Spam or Duplicate
                # MEDIUM: General feedback/praise
                if is_spam:
                    priority = "LOW"
                elif intent_res.label in {IntentLabel.COMPLAINT, IntentLabel.QUESTION, IntentLabel.FEATURE_REQUEST}:
                    priority = "HIGH"
                elif sentiment_res.label == SentimentLabel.NEGATIVE:
                    priority = "HIGH"
                elif is_duplicate:
                    priority = "LOW"
                else:
                    priority = "MEDIUM"

                response = CommentProcessResponse(
                    comment_id=item.comment_id,
                    language=lang,
                    is_hinglish=is_hinglish,
                    cleaned_text=cleaned,
                    normalized_text=norm,
                    is_spam=is_spam,
                    spam_score=spam_score,
                    spam_reasons=spam_reasons,
                    is_duplicate=is_duplicate,
                    pii_detected=pii_detected,
                    pii_masked_text=pii_masked_text,
                    sentiment=sentiment_res,
                    intent=intent_res,
                    priority=priority,
                    embedding=emb,
                    embedding_dimension=self.embedding_service.dimension,
                    model_versions=model_versions,
                    success=True,
                )
                results.append(response)
                successful_count += 1
            except Exception as ex:
                logger.error("Error processing comment %s: %s", item.comment_id, ex, exc_info=True)
                results.append(
                    CommentProcessResponse(
                        comment_id=item.comment_id,
                        language="und",
                        cleaned_text=item.text or "",
                        normalized_text=item.text or "",
                        pii_masked_text=item.text or "",
                        sentiment=self.sentiment_service.analyze_sentiment(""),
                        intent=self.intent_service.classify_intent("", is_spam=False),
                        embedding=[],
                        embedding_dimension=self.embedding_service.dimension,
                        model_versions=model_versions,
                        success=False,
                        error_message=str(ex),
                    )
                )
                failed_count += 1

        return BatchCommentProcessResponse(
            results=results,
            total=len(comments),
            successful=successful_count,
            failed=failed_count,
        )
