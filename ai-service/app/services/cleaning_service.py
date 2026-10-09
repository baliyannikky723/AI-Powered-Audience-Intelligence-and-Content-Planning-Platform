import html
import re
import unicodedata
from typing import Tuple

from app.config import get_settings


class CleaningService:

    def __init__(self):
        self.settings = get_settings()

    def clean_and_normalize(self, raw_text: str) -> Tuple[str, str]:
        """
        Cleans and normalizes text.
        Returns:
          cleaned_text: Cleaned text preserving emojis, casing, and semantic structure.
          normalized_text: Standardized normalized representation for matching and downstream deduplication.
        """
        if not raw_text:
            return "", ""

        # 1. Unicode NFKC Normalization
        text = unicodedata.normalize("NFKC", raw_text)

        # 2. HTML Entity Unescaping (e.g., &amp; -> &, &quot; -> ")
        text = html.unescape(text)

        # 3. Collapse control characters (except newline)
        text = re.sub(r"[\r\t\f\v]+", " ", text)
        text = re.sub(r"\n{3,}", "\n\n", text)

        # 4. Collapse excessive repeated punctuation (e.g., !!!!!! -> !)
        text = re.sub(r"!{2,}", "!", text)
        text = re.sub(r"\?{2,}", "?", text)
        text = re.sub(r"\.{4,}", "...", text)

        # 5. Normalize whitespace per line
        lines = [re.sub(r"[ ]{2,}", " ", line).strip() for line in text.split("\n")]
        cleaned_text = "\n".join(line for line in lines if line)

        # 6. Build normalized_text for hashing/matching
        # Lowercase, remove excess whitespace, standardize quotes
        norm = cleaned_text.lower()
        norm = re.sub(r"[\"\'`]", "", norm)
        norm = re.sub(r"\s+", " ", norm).strip()

        return cleaned_text, norm

    @property
    def version(self) -> str:
        return self.settings.processing_algorithm_version
