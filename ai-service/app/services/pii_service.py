import re
from typing import Tuple

# Standard regex patterns for PII detection
EMAIL_PATTERN = re.compile(
    r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"
)
PHONE_PATTERN = re.compile(
    r"(?:(?:\+?\d{1,3}[\s-]?)?\(?\d{2,4}\)?[\s.-]?)?\b\d{3}[\s.-]?\d{3,4}[\s.-]?\d{3,4}\b"
)
IP_PATTERN = re.compile(
    r"\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b"
)


class PiiService:

    @staticmethod
    def mask_pii(text: str) -> Tuple[bool, str]:
        """
        Detects and masks emails, phone numbers, and IP addresses in the processed text.
        Returns (pii_detected, pii_masked_text).
        """
        if not text:
            return False, ""

        masked_text = text
        pii_detected = False

        # 1. Mask emails
        if EMAIL_PATTERN.search(masked_text):
            pii_detected = True
            masked_text = EMAIL_PATTERN.sub("[EMAIL_REDACTED]", masked_text)

        # 2. Mask phone numbers
        if PHONE_PATTERN.search(masked_text):
            # Verify potential phone has at least 7 digits to avoid false positive single numbers
            matches = list(PHONE_PATTERN.finditer(masked_text))
            for m in matches:
                digits_count = len(re.sub(r"\D", "", m.group(0)))
                if digits_count >= 7:
                    pii_detected = True
                    masked_text = masked_text.replace(m.group(0), "[PHONE_REDACTED]")

        # 3. Mask IP addresses
        if IP_PATTERN.search(masked_text):
            pii_detected = True
            masked_text = IP_PATTERN.sub("[IP_REDACTED]", masked_text)

        return pii_detected, masked_text
