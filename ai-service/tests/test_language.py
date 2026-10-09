from app.services.language_service import LanguageService


def test_language_detection_english():
    service = LanguageService()
    lang, is_hinglish = service.detect_language("This is a fantastic explanation of neural networks and gradient descent.")
    assert lang == "en"
    assert is_hinglish is False


def test_language_detection_hinglish():
    service = LanguageService()
    lang, is_hinglish = service.detect_language("Bhai ye video bahut accha hai samajh aa gaya")
    assert is_hinglish is True
    assert lang == "hi-Latn"


def test_language_detection_short_empty():
    service = LanguageService()
    lang, is_hinglish = service.detect_language("🔥")
    assert lang == "und"
    assert is_hinglish is False
