def test_single_comment_processing(client):
    payload = {
        "comment_id": "yt_comment_001",
        "text": "Awesome video! Can you make a tutorial on PostgreSQL pgvector? Contact me at test@example.com",
    }
    response = client.post("/api/v1/process/comment", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["comment_id"] == "yt_comment_001"
    assert data["language"] == "en"
    assert data["is_spam"] is False
    assert data["pii_detected"] is True
    assert "[EMAIL_REDACTED]" in data["pii_masked_text"]
    assert "test@example.com" not in data["pii_masked_text"]
    assert data["sentiment"]["label"] in ["POSITIVE", "NEUTRAL", "NEGATIVE"]
    assert data["intent"]["label"] in ["QUESTION", "FEATURE_REQUEST", "FEEDBACK", "PRAISE", "COMPLAINT", "SPAM", "OTHER"]
    assert len(data["embedding"]) == 384
    assert data["embedding_dimension"] == 384
    assert "model_versions" in data
    assert data["model_versions"]["algorithm"] == "pulsegpt-nlp-v1"


def test_batch_processing_and_deduplication(client):
    payload = {
        "comments": [
            {"comment_id": "c1", "text": "This is great!"},
            {"comment_id": "c2", "text": "This is great!"},  # duplicate
            {"comment_id": "c3", "text": "How do I fix error 404?"},
        ]
    }
    response = client.post("/api/v1/process/comments/batch", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["total"] == 3
    assert data["successful"] == 3
    assert data["failed"] == 0
    results = data["results"]
    assert results[0]["is_duplicate"] is False
    assert results[1]["is_duplicate"] is True
    assert results[2]["is_duplicate"] is False
