import re
from typing import List

from app.config import get_settings
from app.schemas.intent import IntentLabel, IntentResult

QUESTION_PATTERNS = [
    re.compile(r"\?"),
    re.compile(r"\b(how|what|why|when|where|who|can you|could you|is there|kaise|kya|kyun)\b", re.IGNORECASE),
]

FEATURE_REQUEST_PATTERNS = [
    re.compile(r"\b(please make|please do|can you make a video on|video on|tutorial on|next video|feature request|add support|banao|agla video)\b", re.IGNORECASE),
]

COMPLAINT_PATTERNS = [
    re.compile(r"\b(audio is bad|sound issue|video quality|too quiet|can't hear|clickbait|wasted time|not working|broken|error|kharab)\b", re.IGNORECASE),
]

PRAISE_PATTERNS = [
    re.compile(r"\b(great video|amazing content|best tutorial|keep it up|love this|loved it|subbed|subscribed|thank you|thanks a lot|zabardast|mast)\b", re.IGNORECASE),
]

FEEDBACK_PATTERNS = [
    re.compile(r"\b(suggestion|in my opinion|i think|maybe you should|try to|it would be better|feedback|advice)\b", re.IGNORECASE),
]


class IntentService:

    def __init__(self):
        self.settings = get_settings()

    def classify_intent(self, text: str, is_spam: bool = False) -> IntentResult:
        """
        Classifies intent of a comment into QUESTION, FEEDBACK, COMPLAINT, PRAISE,
        FEATURE_REQUEST, SPAM, or OTHER.
        """
        if is_spam:
            return IntentResult(label=IntentLabel.SPAM, score=0.95)

        if not text or not text.strip():
            return IntentResult(label=IntentLabel.OTHER, score=0.5)

        cleaned = text.strip()

        # 1. Check Feature Request
        for pat in FEATURE_REQUEST_PATTERNS:
            if pat.search(cleaned):
                return IntentResult(label=IntentLabel.FEATURE_REQUEST, score=0.88)

        # 2. Check Question
        for pat in QUESTION_PATTERNS:
            if pat.search(cleaned):
                return IntentResult(label=IntentLabel.QUESTION, score=0.85)

        # 3. Check Complaint
        for pat in COMPLAINT_PATTERNS:
            if pat.search(cleaned):
                return IntentResult(label=IntentLabel.COMPLAINT, score=0.82)

        # 4. Check Feedback
        for pat in FEEDBACK_PATTERNS:
            if pat.search(cleaned):
                return IntentResult(label=IntentLabel.FEEDBACK, score=0.80)

        # 5. Check Praise
        for pat in PRAISE_PATTERNS:
            if pat.search(cleaned):
                return IntentResult(label=IntentLabel.PRAISE, score=0.85)

        return IntentResult(label=IntentLabel.OTHER, score=0.50)

    def classify_batch(self, texts: List[str]) -> List[IntentResult]:
        return [self.classify_intent(t) for t in texts]

    @property
    def model_name(self) -> str:
        return self.settings.intent_model_name

    @property
    def version(self) -> str:
        return self.settings.intent_model_version
