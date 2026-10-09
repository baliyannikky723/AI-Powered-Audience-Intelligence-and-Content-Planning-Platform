from app.services.spam_service import SpamService


def test_spam_detection_legitimate():
    service = SpamService()
    is_spam, score, reasons = service.evaluate_spam("Thank you for this wonderful tutorial, helped me understand transformer architecture!")
    assert is_spam is False
    assert score < 0.5
    assert len(reasons) == 0


def test_spam_detection_promo():
    service = SpamService()
    is_spam, score, reasons = service.evaluate_spam("Contact on WhatsApp +1234567890 for bitcoin crypto profit guaranteed!")
    assert is_spam is True
    assert score >= 0.65
    assert len(reasons) > 0


def test_spam_repetition():
    service = SpamService()
    is_spam, score, reasons = service.evaluate_spam("sooooooooooooooo coooooooooool aaaaaaaaaaaa")
    assert "EXCESSIVE_CHARACTER_REPETITION" in reasons
