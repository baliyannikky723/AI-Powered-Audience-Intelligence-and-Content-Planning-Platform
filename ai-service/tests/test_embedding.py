import numpy as np

from app.services.embedding_service import EmbeddingService


def test_embedding_generation_and_dimension():
    service = EmbeddingService()
    texts = ["First comment text", "Second comment about neural networks"]
    embeddings = service.embed_texts(texts)

    assert len(embeddings) == 2
    for emb in embeddings:
        assert len(emb) == 384
        # Verify L2 norm is ~1.0
        norm = np.linalg.norm(np.array(emb))
        assert abs(norm - 1.0) < 0.05


def test_embedding_determinism():
    service = EmbeddingService()
    text = "Identical text for reproducible embedding check"
    emb1 = service.embed_texts([text])[0]
    emb2 = service.embed_texts([text])[0]

    assert emb1 == emb2
