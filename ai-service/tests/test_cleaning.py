from app.services.cleaning_service import CleaningService


def test_cleaning_whitespace_and_html():
    service = CleaningService()
    raw = "OMG!!!!    &amp; this is AMAZING &quot;AI&quot; 🔥🔥   \n\n\n\nReally good."
    cleaned, norm = service.clean_and_normalize(raw)

    assert "&amp;" not in cleaned
    assert "&" in cleaned
    assert '"AI"' in cleaned
    assert "🔥" in cleaned  # emojis preserved
    assert "!!!!" not in cleaned
    assert "!" in cleaned
    assert norm == "omg! & this is amazing ai 🔥🔥 really good."


def test_cleaning_empty():
    service = CleaningService()
    cleaned, norm = service.clean_and_normalize("")
    assert cleaned == ""
    assert norm == ""
