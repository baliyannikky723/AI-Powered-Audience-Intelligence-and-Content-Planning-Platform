import hashlib
import logging
from typing import List

import numpy as np

from app.config import get_settings

logger = logging.getLogger(__name__)


class EmbeddingService:

    def __init__(self):
        self.settings = get_settings()
        self._model = None
        self._is_loaded = False
        self._fallback_mode = False

    def load_model(self) -> None:
        """Loads sentence-transformers embedding model or initializes deterministic fallback if offline."""
        if self._is_loaded:
            return

        if self.settings.offline_mode or self.settings.environment == "test":
            self._fallback_mode = True
            self._is_loaded = True
            logger.info("Running embedding service in deterministic feature mode.")
            return

        try:
            from sentence_transformers import SentenceTransformer
            logger.info("Loading embedding model: %s", self.settings.embedding_model_name)
            self._model = SentenceTransformer(self.settings.embedding_model_name)
            self._is_loaded = True
            self._fallback_mode = False
            logger.info("Embedding model loaded successfully.")
        except Exception as e:
            logger.warning("Could not load HuggingFace sentence-transformers model (%s). Using deterministic feature embedding: %s", self.settings.embedding_model_name, e)
            self._fallback_mode = True
            self._is_loaded = True

    def embed_texts(self, texts: List[str]) -> List[List[float]]:
        """
        Generates 384-dimensional embeddings for a batch of texts.
        """
        if not self._is_loaded:
            self.load_model()

        if not texts:
            return []

        # Replace empty strings with single space
        sanitized = [t.strip() if t and t.strip() else " " for t in texts]

        if not self._fallback_mode and self._model is not None:
            try:
                embeddings = self._model.encode(
                    sanitized,
                    normalize_embeddings=self.settings.normalize_embeddings,
                    show_progress_bar=False,
                )
                return [[round(float(val), 6) for val in vec] for vec in embeddings]
            except Exception as e:
                logger.error("Error during model inference, falling back to deterministic vectors: %s", e)

        # Deterministic 384-dimension fallback generator for offline / test environments
        return [self._generate_deterministic_vector(t) for t in sanitized]

    def _generate_deterministic_vector(self, text: str) -> List[float]:
        """Generates a reproducible 384-dimensional unit vector based on SHA-256 and word hashes."""
        dim = self.settings.embedding_dimension
        vec = np.zeros(dim, dtype=np.float32)

        # Seed vector with character n-grams and text hash
        text_bytes = text.encode("utf-8")
        h = hashlib.sha256(text_bytes).digest()

        for i in range(min(len(h), dim)):
            vec[i] = (h[i] - 128) / 128.0

        words = text.lower().split()
        for idx, word in enumerate(words):
            word_hash = int(hashlib.md5(word.encode("utf-8")).hexdigest()[:8], 16)
            pos = (word_hash + idx * 7) % dim
            vec[pos] += 1.0

        # L2 normalize
        norm = np.linalg.norm(vec)
        if norm > 0:
            vec = vec / norm
        else:
            vec[0] = 1.0

        return [round(float(x), 6) for x in vec.tolist()]

    @property
    def model_name(self) -> str:
        return self.settings.embedding_model_name

    @property
    def version(self) -> str:
        return self.settings.embedding_model_version

    @property
    def dimension(self) -> int:
        return self.settings.embedding_dimension

    @property
    def is_ready(self) -> bool:
        return self._is_loaded
