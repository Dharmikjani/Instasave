import re

def generate_file_name(author: str, shortcode: str, media_type: str, timestamp: str = "20261104_170000"):
    clean_author = re.sub(r"[^a-zA-Z0-9_]", "", author)
    ext = {
        "REEL": "mp4",
        "POST_VIDEO": "mp4",
        "AUDIO": "m4a",
        "POST_IMAGE": "jpg",
        "PROFILE_DP": "jpg"
    }.get(media_type, "jpg")

    return f"Insta_{clean_author}_{timestamp}_{shortcode}.{ext}"

def get_relative_path(author: str, media_type: str):
    clean_author = re.sub(r"[^a-zA-Z0-9_]", "", author)
    if media_type in ("REEL", "POST_VIDEO"):
        return f"Movies/InstaSaver/{clean_author}"
    elif media_type == "AUDIO":
        return "Music/InstaSaver"
    else:
        return f"Pictures/InstaSaver/{clean_author}"

def get_mime_type(media_type: str):
    return {
        "REEL": "video/mp4",
        "POST_VIDEO": "video/mp4",
        "AUDIO": "audio/mp4",
        "POST_IMAGE": "image/jpeg",
        "PROFILE_DP": "image/jpeg"
    }.get(media_type, "image/jpeg")

def test_storage_naming():
    checks = [
        ("Reel Filename Extension (.mp4)", generate_file_name("virat.kohli", "C7xY9Z", "REEL").endswith(".mp4")),
        ("Audio Filename Extension (.m4a)", generate_file_name("virat.kohli", "C7xY9Z", "AUDIO").endswith(".m4a")),
        ("Photo Filename Extension (.jpg)", generate_file_name("virat.kohli", "C7xY9Z", "POST_IMAGE").endswith(".jpg")),
        ("Author Sanitized (virat.kohli -> viratkohli)", "Insta_viratkohli_" in generate_file_name("virat.kohli", "C7xY9Z", "REEL")),
        ("Reel Destination Path (Movies/InstaSaver)", get_relative_path("mrbeast", "REEL") == "Movies/InstaSaver/mrbeast"),
        ("Photo Destination Path (Pictures/InstaSaver)", get_relative_path("mrbeast", "POST_IMAGE") == "Pictures/InstaSaver/mrbeast"),
        ("Audio Destination Path (Music/InstaSaver)", get_relative_path("mrbeast", "AUDIO") == "Music/InstaSaver"),
        ("MIME Types Correct", get_mime_type("REEL") == "video/mp4" and get_mime_type("AUDIO") == "audio/mp4" and get_mime_type("POST_IMAGE") == "image/jpeg")
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "MediaStore Naming & Path Architecture Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    res = test_storage_naming()
    print(f"{res['suite']}: {res['passed']}/{res['total']} PASSED")

