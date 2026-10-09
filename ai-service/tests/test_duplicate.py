from app.services.duplicate_service import DuplicateService


def test_batch_duplicate_detection():
    texts = [
        "great video thanks",
        "another question about python",
        "great video thanks",
        "great video thanks",
    ]
    results = DuplicateService.mark_batch_duplicates(texts)
    assert results[0][0] is False  # first occurrence
    assert results[1][0] is False  # unique
    assert results[2][0] is True   # duplicate
    assert results[3][0] is True   # duplicate
