import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.schemas.recommendation import (
    EvidenceItemSchema,
    RecommendationDraftSchema,
    RecommendationRepairRequest,
    RecommendationRequestSchema,
)
from app.services.recommendation_generator_service import RecommendationGeneratorService

client = TestClient(app)
API_KEY_HEADER = {"X-Internal-Service-Key": "pulse-internal-secret-key-3f8a9e"}


@pytest.fixture
def recommendation_service():
    return RecommendationGeneratorService()


class TestRecommendationService:

    def test_evidence_grounded_generation(self, recommendation_service):
        evidence = [
            EvidenceItemSchema(
                evidence_id="topic:top_1",
                source_type="TOPIC",
                source_id="top_1",
                summary="Battery Life & Fast Charging",
                relevance_score=0.95,
                metadata={"keywords": ["battery", "charging"]},
            ),
            EvidenceItemSchema(
                evidence_id="question:comm_1",
                source_type="QUESTION",
                source_id="comm_1",
                summary="How is the battery backup during 4k video recording?",
                relevance_score=0.88,
            ),
        ]

        req = RecommendationRequestSchema(
            request_id="req-123",
            topic_id="top_1",
            topic_name="Battery Life & Fast Charging",
            content_type="VIDEO",
            goal="EDUCATIONAL",
            max_ideas=2,
            mode="EVIDENCE_GROUNDED",
            evidence=evidence,
        )

        res = recommendation_service.generate_recommendations(req)
        assert res.request_id == "req-123"
        assert res.generation_mode == "EVIDENCE_GROUNDED"
        assert len(res.drafts) == 2
        for draft in res.drafts:
            assert draft.title
            assert draft.angle
            assert len(draft.key_points) >= 2
            assert draft.hook
            assert len(draft.evidence_ids) > 0
            assert draft.confidence >= 0.70

    def test_baseline_generation(self, recommendation_service):
        req = RecommendationRequestSchema(
            request_id="req-base",
            topic_name="Camera Performance",
            content_type="VIDEO",
            max_ideas=2,
            mode="BASELINE",
        )

        res = recommendation_service.generate_recommendations(req)
        assert res.generation_mode == "BASELINE"
        assert len(res.drafts) == 2
        for draft in res.drafts:
            assert draft.evidence_ids == []
            assert draft.confidence == 0.50

    def test_repair_draft(self, recommendation_service):
        evidence = [
            EvidenceItemSchema(
                evidence_id="topic:top_1",
                source_type="TOPIC",
                source_id="top_1",
                summary="Thermal Throttling",
                relevance_score=0.90,
            )
        ]

        orig_draft = RecommendationDraftSchema(
            title="Why 90% of Users Face Overheating! Contact user@example.com",
            content_type="VIDEO",
            angle="90% of viewers are seeing issues",
            target_audience="Gamers",
            problem_addressed="90% of all viewers have severe thermal issues",
            key_points=["Point 1", "Point 2"],
            hook="Is your phone overheating during gaming?",
            call_to_action="Call 123-456-7890",
            evidence_ids=["fabricated:id_999"],
            confidence=0.80,
            reason="Unsubstantiated 90% claim",
        )

        repair_req = RecommendationRepairRequest(
            request_id="repair-1",
            original_draft=orig_draft,
            failed_checks=[
                {"name": "EVIDENCE", "reason": "Unknown evidence ID: fabricated:id_999"},
                {"name": "HALLUCINATION", "reason": "Unsupported claim: 90%"},
                {"name": "SAFETY", "reason": "PII detected: user@example.com, 123-456-7890"},
            ],
            evidence=evidence,
        )

        res = recommendation_service.repair_recommendation(repair_req)
        repaired = res.repaired_draft
        assert "90%" not in repaired.problem_addressed
        assert "[EMAIL_REDACTED]" in repaired.title or "user@example.com" not in repaired.title
        assert "123-456-7890" not in repaired.call_to_action
        assert "fabricated:id_999" not in repaired.evidence_ids
        assert "topic:top_1" in repaired.evidence_ids
        assert len(repaired.hook) >= 10


class TestRecommendationApiEndpoints:

    def test_generate_endpoint_success(self):
        payload = {
            "request_id": "api-rec-1",
            "topic_name": "Fast Charging",
            "content_type": "VIDEO",
            "max_ideas": 1,
            "mode": "EVIDENCE_GROUNDED",
            "evidence": [
                {
                    "evidence_id": "topic:top_1",
                    "source_type": "TOPIC",
                    "source_id": "top_1",
                    "summary": "Fast Charging speeds",
                    "relevance_score": 0.90,
                }
            ],
        }

        response = client.post("/api/v1/recommendations/generate", json=payload, headers=API_KEY_HEADER)
        assert response.status_code == 200
        data = response.json()
        assert data["request_id"] == "api-rec-1"
        assert len(data["drafts"]) == 1

    def test_repair_endpoint_success(self):
        payload = {
            "request_id": "api-repair-1",
            "original_draft": {
                "title": "Fixing Charging Issues",
                "content_type": "VIDEO",
                "angle": "Hands-on troubleshooting guide",
                "target_audience": "Smartphone Users",
                "problem_addressed": "Slow charging complaints",
                "key_points": ["Step 1", "Step 2"],
                "hook": "Is your phone charging slowly? Here is the fix.",
                "call_to_action": "Subscribe for more tips",
                "evidence_ids": ["topic:top_1"],
                "confidence": 0.85,
                "reason": "Audience request",
            },
            "failed_checks": [{"name": "SCHEMA", "reason": "Minor validation adjustment"}],
            "evidence": [
                {
                    "evidence_id": "topic:top_1",
                    "source_type": "TOPIC",
                    "source_id": "top_1",
                    "summary": "Charging speeds",
                    "relevance_score": 0.90,
                }
            ],
        }

        response = client.post("/api/v1/recommendations/repair", json=payload, headers=API_KEY_HEADER)
        assert response.status_code == 200
        data = response.json()
        assert data["request_id"] == "api-repair-1"
        assert "repaired_draft" in data
