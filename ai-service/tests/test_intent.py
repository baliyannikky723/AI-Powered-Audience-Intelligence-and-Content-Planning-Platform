from app.schemas.intent import IntentLabel
from app.services.intent_service import IntentService


def test_intent_question():
    service = IntentService()
    res = service.classify_intent("How can I deploy this application on Kubernetes?")
    assert res.label == IntentLabel.QUESTION
    assert res.score >= 0.7


def test_intent_feature_request():
    service = IntentService()
    res = service.classify_intent("Please make a video on Spring Boot microservices next!")
    assert res.label == IntentLabel.FEATURE_REQUEST
    assert res.score >= 0.7


def test_intent_complaint():
    service = IntentService()
    res = service.classify_intent("The audio is bad and too quiet to hear anything.")
    assert res.label == IntentLabel.COMPLAINT


def test_intent_praise():
    service = IntentService()
    res = service.classify_intent("Great video! Subscribed immediately.")
    assert res.label == IntentLabel.PRAISE


def test_intent_spam():
    service = IntentService()
    res = service.classify_intent("check my channel", is_spam=True)
    assert res.label == IntentLabel.SPAM
