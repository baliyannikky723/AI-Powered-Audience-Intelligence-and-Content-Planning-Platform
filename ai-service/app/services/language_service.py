import re
from typing import Optional, Tuple

import langdetect
from langdetect import DetectorFactory

from app.config import get_settings

# Enforce deterministic language detection seed
DetectorFactory.seed = 42

HINGLISH_KEYWORDS = {
    "bhai", "kya", "accha", "achha", "bahut", "bohot", "hai", "hain",
    "mujhe", "karo", "karna", "nahi", "nahin", "hota", "hote", "bhi",
    "ye", "yeh", "wo", "woh", "sir", "plz", "batao", "samajh", "aaya",
    "shukriya", "dhanyawad", "mast", "zabardast", "badiya", "badhiya",
    "kaise", "kese", "kare", "karein", "dekh", "dekho", "dekha"
}


class LanguageService:

    def __init__(self):
        self.settings = get_settings()

    def detect_language(self, text: str, hint: Optional[str] = None) -> Tuple[str, bool]:
        """
        Detects ISO 639-1 language code and whether text is Hinglish.
        Returns (language_code, is_hinglish).
        """
        if hint and len(hint.strip()) >= 2:
            return hint.strip().lower(), False

        if not text or not text.strip():
            return "und", False

        cleaned = text.strip()
        # If text is too short or only punctuation/emojis/numbers
        alphanumeric = re.sub(r"[^\w\s]", "", cleaned)
        if len(alphanumeric.strip()) < 3:
            return "und", False

        # Check Hinglish baseline heuristic
        words = set(re.findall(r"\b[a-zA-Z]{2,}\b", cleaned.lower()))
        hinglish_matches = words.intersection(HINGLISH_KEYWORDS)
        is_hinglish = len(hinglish_matches) >= 2 or (len(words) <= 5 and len(hinglish_matches) >= 1)

        try:
            detected = langdetect.detect(cleaned)
            # If Latin script with Hinglish markers, preserve detection but mark Hinglish
            if is_hinglish:
                return "hi-Latn", True
            return str(detected), False
        except Exception:
            if is_hinglish:
                return "hi-Latn", True
            return "und", False

    @property
    def version(self) -> str:
        return self.settings.language_detector_version
