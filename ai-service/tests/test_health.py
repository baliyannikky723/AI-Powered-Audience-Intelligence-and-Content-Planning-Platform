def test_health_check(client):
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "pulsegpt-ai-service"
    assert "version" in data
    assert "models_loaded" in data
