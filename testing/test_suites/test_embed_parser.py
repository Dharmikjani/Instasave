import re
import os

HASHTAG_REGEX = re.compile(r"#([\w]+)")

def parse_embed_html(html_content: str):
    # Extract video src
    video_match = re.search(r'<video[^>]+class="EmbeddedVideo"[^>]+src="([^"]+)"', html_content)
    video_url = video_match.group(1).replace("&amp;", "&") if video_match else None

    # Extract poster
    poster_match = re.search(r'<video[^>]+poster="([^"]+)"', html_content)
    poster_url = poster_match.group(1).replace("&amp;", "&") if poster_match else None

    # Extract author username
    user_match = re.search(r'<a[^>]+class="CaptionUsername"[^>]*>@?([^<]+)</a>', html_content)
    username = user_match.group(1).strip() if user_match else None

    # Extract caption
    caption_match = re.search(r'<div class="Caption">\s*([^<]+)\s*</div>', html_content, re.DOTALL)
    caption = caption_match.group(1).strip() if caption_match else ""

    # Extract hashtags
    hashtags = ["#" + tag for tag in HASHTAG_REGEX.findall(caption)]

    return {
        "video_url": video_url,
        "poster_url": poster_url,
        "username": username,
        "caption": caption,
        "hashtags": hashtags
    }

def test_embed_parser():
    mock_file = os.path.join(os.path.dirname(__file__), "..", "mock_data", "sample_embed.html")
    with open(mock_file, "r", encoding="utf-8") as f:
        html = f.read()

    data = parse_embed_html(html)

    checks = [
        ("Video URL Extracted", data["video_url"] is not None and "reel_video_1080p.mp4" in data["video_url"]),
        ("Poster Extracted", data["poster_url"] is not None and "reel_poster.jpg" in data["poster_url"]),
        ("Username Matches", data["username"] == "mrbeast"),
        ("Caption Extracted", "I gave away a private island!" in data["caption"]),
        ("Hashtags Parsed", "#giveaway" in data["hashtags"] and "#mrbeast" in data["hashtags"] and len(data["hashtags"]) == 4),
        ("Lossless CDN Cleaned (&amp; replaced)", "&amp;" not in (data["video_url"] or ""))
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "Instagram Embed Engine Parser Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    res = test_embed_parser()
    print(f"{res['suite']}: {res['passed']}/{res['total']} PASSED")

