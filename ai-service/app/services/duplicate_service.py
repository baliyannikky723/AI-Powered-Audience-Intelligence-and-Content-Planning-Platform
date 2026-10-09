import hashlib
from typing import Set, Tuple


class DuplicateService:

    @staticmethod
    def calculate_hash(normalized_text: str) -> str:
        """Calculates SHA-256 hash of normalized comment text."""
        if not normalized_text:
            return ""
        return hashlib.sha256(normalized_text.encode("utf-8")).hexdigest()

    @staticmethod
    def mark_batch_duplicates(normalized_texts: list[str]) -> list[Tuple[bool, str]]:
        """
        Identifies exact duplicates within a batch.
        Returns a list of (is_duplicate, hash_value).
        """
        seen_hashes: Set[str] = set()
        results: list[Tuple[bool, str]] = []

        for text in normalized_texts:
            if not text:
                results.append((False, ""))
                continue

            h = DuplicateService.calculate_hash(text)
            if h in seen_hashes:
                results.append((True, h))
            else:
                seen_hashes.add(h)
                results.append((False, h))

        return results
