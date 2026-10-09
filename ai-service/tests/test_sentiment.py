from app.schemas.sentiment import SentimentLabel
from app.services.sentiment_service import SentimentService


def test_sentiment_positive():
    service = SentimentService()
    res = service.analyze_sentiment("This is the best and most amazing tutorial ever! Love it ❤️🔥")
    assert res.label == SentimentLabel.POSITIVE
    assert res.score >= 0.6


def test_sentiment_negative():
    service = SentimentService()
    res = service.analyze_sentiment("Horrible audio quality, completely useless and waste of time.")
    assert res.label == SentimentLabel.NEGATIVE
    assert res.score >= 0.6


def test_sentiment_neutral():
    service = SentimentService()
    res = service.analyze_sentiment("Timestamp 12:45 is where the explanation starts.")
    assert res.label == SentimentLabel.NEUTRAL
    assert 0.0 <= res.score <= 1.0
