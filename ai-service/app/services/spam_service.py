import re
from typing import List, Tuple

from app.config import get_settings

SPAM_PATTERNS = [
    (re.compile(r"\b(whatsapp|telegram|signal)\b.*?[0-9+\s-]{7,}", re.IGNORECASE), 0.8, "PROMOTIONAL_MESSAGING_CONTACT"),
    (re.compile(r"\b(crypto|bitcoin|eth|forex|binary options|invest|profit guaranteed|wallet)\b", re.IGNORECASE), 0.6, "CRYPTO_INVESTMENT_PROMOTION"),
    (re.compile(r"\b(dm me|check my bio|visit my profile|subscribe to my channel|sub4sub|follow me back)\b", re.IGNORECASE), 0.5, "SELF_PROMOTION"),
    (re.compile(r"\b(free subscribers|free views|free followers|grow your channel)\b", re.IGNORECASE), 0.7, "ENGAGEMENT_BOT_SPAM"),
    (re.compile(r"https?://\S+|t\.me/\S+|bit\.ly/\S+|wa\.me/\S+", re.IGNORECASE), 0.4, "EXTERNAL_LINK_URL"),
]


class SpamService:

    def __init__(self):
        self.settings = get_settings()

    def evaluate_spam(self, text: str) -> Tuple[bool, float, List[str]]:
        """
        Baseline heuristic spam detector.
        Returns (is_spam, spam_score, spam_reasons).
        """
        if not text or not text.strip():
            return False, 0.0, []

        score = 0.0
        reasons: List[str] = []

        # 1. Pattern matches
        for pattern, weight, reason in SPAM_PATTERNS:
            if pattern.search(text):
                score += weight
                reasons.append(reason)

        # 2. Repeated characters (e.g. "aaaaaaa", "loooooool")
        if re.search(r"(.)\1{5,}", text):
            score += 0.35
            reasons.append("EXCESSIVE_CHARACTER_REPETITION")

        # 3. Excessive URLs
        urls = re.findall(r"https?://\S+", text)
        if len(urls) >= 2:
            score += 0.4
            reasons.append("MULTIPLE_URLS")

        # 4. Excessive mentions / symbols
        mentions = re.findall(r"@\w+", text)
        if len(mentions) >= 3:
            score += 0.3
            reasons.append("EXCESSIVE_MENTIONS")

        # 5. Repetitive word sequences
        words = text.lower().split()
        if len(words) >= 6:
            unique_words = set(words)
            if len(unique_words) / len(words) < 0.35:
                score += 0.45
                reasons.append("REPETITIVE_WORD_LOOP")

        # Cap score at 1.0
        final_score = min(round(score, 2), 1.0)
        is_spam = final_score >= self.settings.spam_threshold

        return is_spam, final_score, reasons

    @property
    def version(self) -> str:
        return self.settings.processing_algorithm_version
