import pytest
from fastapi.testclient import TestClient

from app.config import Settings, get_settings
from app.main import create_app


def get_test_settings() -> Settings:
    return Settings(
        environment="test",
        offline_mode=True,
        require_api_key=False,
        internal_api_key="test-internal-key-12345",
        max_batch_size=100,
        embedding_dimension=384,
    )


@pytest.fixture(scope="session")
def client():
    app = create_app()
    app.dependency_overrides[get_settings] = get_test_settings
    with TestClient(app) as test_client:
        yield test_client
