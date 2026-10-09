import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.schemas.clustering import (
    ClusteringCommentInput,
    ClusteringConfig,
    ClusteringRunRequest,
)
from app.services.clustering_service import ClusteringService
from app.services.ctfidf_service import CTfidfService

client = TestClient(app)
API_KEY_HEADER = {"X-Internal-Service-Key": "pulse-internal-secret-key-3f8a9e"}


@pytest.fixture
def ctfidf_service():
    return CTfidfService()


@pytest.fixture
def clustering_service(ctfidf_service):
    return ClusteringService(ctfidf_service=ctfidf_service)


def _generate_synthetic_embeddings(n_samples_per_cluster=10, n_clusters=2, dim=384, noise_count=3):
    """Generates synthetic well-separated 384-d clusters and outlier noise."""
    rng = np.random.RandomState(42)
    embeddings = []
    texts = []
    comment_inputs = []

    # Cluster 0: Battery & Charging
    battery_texts = [
        "The battery life is amazing on this phone",
        "Battery drain issue after update",
        "Fast charging is very quick and efficient",
        "How is battery backup during gaming?",
        "Charging speed reaches 100% in 30 minutes",
        "Battery optimization is great",
        "Phone battery lasts two full days",
        "Overnight battery drain test results",
        "Fast charging adapter included in box",
        "Battery performance benchmark review",
    ]
    center_0 = rng.normal(loc=1.0, scale=0.1, size=(dim,))
    center_0 = center_0 / np.linalg.norm(center_0)

    for i in range(n_samples_per_cluster):
        vec = center_0 + rng.normal(loc=0.0, scale=0.02, size=(dim,))
        vec = vec / np.linalg.norm(vec)
        text = battery_texts[i % len(battery_texts)]
        comment_inputs.append(
            ClusteringCommentInput(
                comment_id=f"battery_{i}",
                text=text,
                embedding=vec.tolist(),
                language="en",
                sentiment="POSITIVE" if i % 2 == 0 else "NEGATIVE",
                intent="FEEDBACK",
                platform="YOUTUBE",
            )
        )

    # Cluster 1: Camera & Video
    camera_texts = [
        "Camera quality in low light is superb",
        "Night mode photo looks crisp and clear",
        "Video recording has excellent stabilization",
        "Front selfie camera is sharp",
        "Portrait mode blur effect is natural",
        "Camera zoom lens 10x is impressive",
        "4k 60fps video quality test sample",
        "Cinematic video mode looks cinematic",
        "Main camera sensor delivers rich colors",
        "Camera vs DSLR comparison shootout",
    ]
    center_1 = rng.normal(loc=-1.0, scale=0.1, size=(dim,))
    center_1 = center_1 / np.linalg.norm(center_1)

    for i in range(n_samples_per_cluster):
        vec = center_1 + rng.normal(loc=0.0, scale=0.02, size=(dim,))
        vec = vec / np.linalg.norm(vec)
        text = camera_texts[i % len(camera_texts)]
        comment_inputs.append(
            ClusteringCommentInput(
                comment_id=f"camera_{i}",
                text=text,
                embedding=vec.tolist(),
                language="en",
                sentiment="POSITIVE",
                intent="PRAISE",
                platform="YOUTUBE",
            )
        )

    # Noise / Outliers
    for i in range(noise_count):
        noise_vec = rng.normal(loc=0.0, scale=5.0, size=(dim,))
        noise_vec = noise_vec / np.linalg.norm(noise_vec)
        comment_inputs.append(
            ClusteringCommentInput(
                comment_id=f"noise_{i}",
                text=f"Random nonsense text {i} xyz abc 123",
                embedding=noise_vec.tolist(),
                language="en",
                sentiment="NEUTRAL",
                intent="OTHER",
                platform="YOUTUBE",
            )
        )

    return comment_inputs


class TestClusteringService:

    def test_synthetic_clusters_and_noise(self, clustering_service):
        comments = _generate_synthetic_embeddings(n_samples_per_cluster=10, n_clusters=2, noise_count=3)
        req = ClusteringRunRequest(
            run_id="test-run-1",
            comments=comments,
            config=ClusteringConfig(
                min_cluster_size=4,
                umap_enabled=True,
                random_state=42,
            ),
        )

        res = clustering_service.cluster_comments(req)
        assert res.status == "COMPLETED"
        assert res.total_comments == 23
        assert res.cluster_count >= 2
        assert len(res.topics) >= 2
        assert len(res.assignments) == 23

        # Verify topic properties
        topic_labels = [t.label.lower() for t in res.topics]
        has_battery = any("battery" in l or "charging" in l for l in topic_labels)
        has_camera = any("camera" in l or "video" in l for l in topic_labels)
        assert has_battery or has_camera

        # Check centroids are 384-dimensional
        for t in res.topics:
            assert len(t.centroid) == 384
            assert t.comment_count >= 4
            assert len(t.keywords) > 0

    def test_empty_input(self, clustering_service):
        req = ClusteringRunRequest(
            run_id="empty-run",
            comments=[],
            config=ClusteringConfig(),
        )
        res = clustering_service.cluster_comments(req)
        assert res.status == "COMPLETED"
        assert res.total_comments == 0
        assert res.cluster_count == 0
        assert res.topics == []
        assert res.assignments == []

    def test_insufficient_samples_marks_as_noise(self, clustering_service):
        comments = [
            ClusteringCommentInput(
                comment_id="c1",
                text="Hello world",
                embedding=[0.1] * 384,
            ),
            ClusteringCommentInput(
                comment_id="c2",
                text="Hello python",
                embedding=[0.2] * 384,
            ),
        ]
        req = ClusteringRunRequest(
            run_id="small-run",
            comments=comments,
            config=ClusteringConfig(min_cluster_size=5),
        )
        res = clustering_service.cluster_comments(req)
        assert res.total_comments == 2
        assert res.cluster_count == 0
        assert res.noise_count == 2
        for a in res.assignments:
            assert a.is_noise is True
            assert a.cluster_id == -1

    def test_deterministic_random_state(self, clustering_service):
        comments = _generate_synthetic_embeddings(n_samples_per_cluster=8, n_clusters=2, noise_count=2)
        req1 = ClusteringRunRequest(
            run_id="run-1",
            comments=comments,
            config=ClusteringConfig(random_state=42),
        )
        req2 = ClusteringRunRequest(
            run_id="run-2",
            comments=comments,
            config=ClusteringConfig(random_state=42),
        )

        res1 = clustering_service.cluster_comments(req1)
        res2 = clustering_service.cluster_comments(req2)

        assert res1.cluster_count == res2.cluster_count
        assert res1.noise_count == res2.noise_count
        assert [a.cluster_id for a in res1.assignments] == [a.cluster_id for a in res2.assignments]

    def test_multilingual_ctfidf(self, ctfidf_service):
        docs = {
            0: ["Bhai battery backup bahut mast hai", "Battery drain issue fix karo please", "charging speed superfast"],
            1: ["Camera quality ekdum tagda hai", "Low light photos and 4k video clarity"],
        }
        res = ctfidf_service.compute_ctfidf(docs, top_n=5)
        assert 0 in res
        assert 1 in res
        kw_cluster_0 = [item.keyword for item in res[0]]
        assert any("battery" in k or "drain" in k or "charging" in k for k in kw_cluster_0)

    def test_kmeans_baseline(self, clustering_service):
        comments = _generate_synthetic_embeddings(n_samples_per_cluster=6, n_clusters=2, noise_count=0)
        req = ClusteringRunRequest(
            run_id="kmeans-run",
            comments=comments,
            config=ClusteringConfig(
                algorithm="BASELINE_TFIDF_KMEANS",
                k_clusters=2,
                random_state=42,
            ),
        )
        res = clustering_service.cluster_comments(req)
        assert res.status == "COMPLETED"
        assert res.algorithm == "BASELINE_TFIDF_KMEANS"
        assert res.cluster_count == 2
        assert len(res.topics) == 2


class TestClusteringApiEndpoints:

    def test_cluster_endpoint_success(self):
        comments = _generate_synthetic_embeddings(n_samples_per_cluster=6, n_clusters=2, noise_count=1)
        payload = {
            "run_id": "api-run-123",
            "comments": [c.model_dump() for c in comments],
            "config": {
                "min_cluster_size": 3,
                "umap_enabled": True,
                "random_state": 42,
            },
        }

        response = client.post("/api/v1/cluster", json=payload, headers=API_KEY_HEADER)
        assert response.status_code == 200
        data = response.json()
        assert data["run_id"] == "api-run-123"
        assert data["status"] == "COMPLETED"
        assert "metrics" in data
        assert "topics" in data
        assert "assignments" in data

    def test_cluster_endpoint_unauthorized(self):
        payload = {"run_id": "api-run-unauth", "comments": []}
        response = client.post("/api/v1/cluster", json=payload)
        assert response.status_code == 401
