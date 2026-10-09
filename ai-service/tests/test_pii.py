from app.services.pii_service import PiiService


def test_pii_email_and_phone():
    text = "Please reach out to john.doe@example.com or call +1 555-123-4567 regarding the sponsorship."
    pii_detected, masked = PiiService.mask_pii(text)

    assert pii_detected is True
    assert "[EMAIL_REDACTED]" in masked
    assert "[PHONE_REDACTED]" in masked
    assert "john.doe@example.com" not in masked
    assert "555-123-4567" not in masked


def test_no_pii():
    text = "Great video! I really loved the third tip about database indexes."
    pii_detected, masked = PiiService.mask_pii(text)

    assert pii_detected is False
    assert masked == text
