import re
from typing import List

from app.config import get_settings
from app.schemas.sentiment import SentimentLabel, SentimentResult

POSITIVE_WORDS = {
    "love", "great", "awesome", "excellent", "amazing", "good", "best",
    "helpful", "thank", "thanks", "fantastic", "wonderful", "cool", "useful",
    "superb", "brilliant", "perfect", "clear", "enjoyed", "liked", "nice",
    "accha", "mast", "zabardast", "badiya", "dhanyawad", "shukriya"
}

NEGATIVE_WORDS = {
    "hate", "bad", "terrible", "worst", "awful", "horrible", "useless",
    "boring", "waste", "poor", "broken", "annoying", "disappointed",
    "confusing", "wrong", "fail", "garbage", "trash", "scam", "clickbait",
    "bekaar", "bakwas", "ganda", "kharab"
}

POSITIVE_EMOJIS = {"🔥", "❤️", "👍", "👏", "🎉", "💯", "🙌", "😍", "🤩", "✨", "🚀"}
NEGATIVE_EMOJIS = {"👎", "😡", "🤬", "🤮", "💩", "💔", "😞", "😠"}

NEGATION_WORDS = {"not", "no", "never", "without", "hardly", "nahi", "nahin"}


class SentimentService:

    def __init__(self):
        self.settings = get_settings()

    def analyze_sentiment(self, text: str) -> SentimentResult:
        """
        Analyzes sentiment of text into POSITIVE, NEUTRAL, or NEGATIVE.
        Returns SentimentResult(label, score).
        """
        if not text or not text.strip():
            return SentimentResult(label=SentimentLabel.NEUTRAL, score=0.5)

        tokens = re.findall(r"\w+|[^\w\s]", text.lower())

        pos_score = 0.0
        neg_score = 0.0
        negated = False

        for i, token in enumerate(tokens):
            if token in NEGATION_WORDS:
                negated = True
                continue

            # Check positive word
            if token in POSITIVE_WORDS:
                if negated:
                    neg_score += 1.0
                else:
                    pos_score += 1.0
                negated = False
            # Check negative word
            elif token in NEGATIVE_WORDS:
                if negated:
                    pos_score += 0.8
                else:
                    neg_score += 1.0
                negated = False
            # Reset negation after 3 tokens
            elif i > 0 and token in {".", ",", "!", "?"}:
                negated = False

        # Emoji scoring
        for char in text:
            if char in POSITIVE_EMOJIS:
                pos_score += 1.0
            elif char in NEGATIVE_EMOJIS:
                neg_score += 1.0

        total = pos_score + neg_score
        if total == 0:
            return SentimentResult(label=SentimentLabel.NEUTRAL, score=0.5)

        if pos_score > neg_score:
            confidence = min(round(0.5 + (pos_score - neg_score) / (total + 1.0) * 0.5, 2), 0.99)
            return SentimentResult(label=SentimentLabel.POSITIVE, score=confidence)
        elif neg_score > pos_score:
            confidence = min(round(0.5 + (neg_score - pos_score) / (total + 1.0) * 0.5, 2), 0.99)
            return SentimentResult(label=SentimentLabel.NEGATIVE, score=confidence)
        else:
            return SentimentResult(label=SentimentLabel.NEUTRAL, score=0.5)

    def analyze_batch(self, texts: List[str]) -> List[SentimentResult]:
        return [self.analyze_sentiment(t) for t in texts]

    @property
    def model_name(self) -> str:
        return self.settings.sentiment_model_name

    @property
    def version(self) -> str:
        return self.settings.sentiment_model_version
