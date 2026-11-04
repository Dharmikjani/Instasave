import re

SHORTCODE_REGEX = re.compile(r"(?:reels?|p|tv|share/(?:reel|p))/([A-Za-z0-9_-]+)")

def extract_shortcode(url: str):
    clean = url.strip()
    if "instagram.com/" not in clean and "instagr.am/" not in clean:
        return None
    match = SHORTCODE_REGEX.search(clean)
    return match.group(1) if match else None

def test_url_sanitizer():
    test_cases = [
        # (Input URL, Expected Shortcode)
        ("https://www.instagram.com/reel/C7xY9Z1A2B3/", "C7xY9Z1A2B3"),
        ("https://www.instagram.com/reels/C7xY9Z1A2B3/", "C7xY9Z1A2B3"), # Plural /reels/
        ("https://instagram.com/reel/C7xY9Z1A2B3?igsh=MWx5YXAzZWlndnA3", "C7xY9Z1A2B3"),
        ("https://www.instagram.com/p/DB12345XYZ/?utm_source=ig_web_copy_link", "DB12345XYZ"),
        ("https://instagram.com/p/DB12345XYZ/", "DB12345XYZ"),
        ("https://www.instagram.com/share/reel/CQabcdef123/", "CQabcdef123"),
        ("https://www.instagram.com/share/p/CQabcdef123/", "CQabcdef123"), # /share/p/
        ("https://instagram.com/share/reel/CQabcdef123?utm_campaign=share", "CQabcdef123"),
        ("https://www.instagram.com/tv/B_xyz987654/", "B_xyz987654"),
        ("https://www.instagram.com/virat.kohli/reel/C-123_456-ABC/", "C-123_456-ABC"),
        ("https://www.instagram.com/reel/C-123_456-ABC/?utm_medium=copy_link", "C-123_456-ABC"),
        ("  https://www.instagram.com/reel/SpacedUrl123/   ", "SpacedUrl123"),
        ("http://instagram.com/p/HttpUrlWithoutS123/", "HttpUrlWithoutS123"),
        # Negative / Invalid test cases
        ("https://www.youtube.com/watch?v=dQw4w9WgXcQ", None),
        ("https://twitter.com/user/status/1234567890", None),
        ("https://www.facebook.com/reel/123456789", None),
        ("just plain text without link", None),
        ("https://instagram.com/explore/tags/coding/", None),
        ("", None),
    ]

    passed = 0
    failed = 0
    details = []

    for url, expected in test_cases:
        actual = extract_shortcode(url)
        is_ok = actual == expected
        if is_ok:
            passed += 1
        else:
            failed += 1
        details.append((url, expected, actual, is_ok))

    return {
        "suite": "URL Sanitizer & Shortcode Extraction Test",
        "total": len(test_cases),
        "passed": passed,
        "failed": failed,
        "details": details
    }

if __name__ == "__main__":
    res = test_url_sanitizer()
    print(f"{res['suite']}: {res['passed']}/{res['total']} PASSED")
