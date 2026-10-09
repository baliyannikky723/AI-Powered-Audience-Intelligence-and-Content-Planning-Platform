from fastapi.testclient import TestClient

from app.config import Settings, get_settings
from app.main import create_app


def test_api_key_security_enforcement():
    # App with API key enforcement enabled
    def secure_settings() -> Settings:
        return Settings(
            environment="production",
            require_api_key=True,
            internal_api_key="secret-api-key-999",
        )

    app = create_app()
    app.dependency_overrides[get_settings] = secure_settings

    with TestClient(app) as test_client:
        # 1. No key -> 401
        res = test_client.post("/api/v1/embed", json={"texts": ["Hello"]})
        assert res.status_code == 401

        # 2. Invalid key -> 401
        res = test_client.post(
            "/api/v1/embed",
            json={"texts": ["Hello"]},
            headers={"X-Internal-Service-Key": "wrong-key"},
        )
        assert res.status_code == 401

        # 3. Valid key -> 200
        res = test_client.post(
            "/api/v1/embed",
            json={"texts": ["Hello"]},
            headers={"X-Internal-Service-Key": "secret-api-key-999"},
        )
        assert res.status_code == 200
