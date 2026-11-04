def classify_and_validate_media(item_type: str, url: str, is_video: bool):
    valid_types = {
        "REEL": {"mime": "video/mp4", "ext": "mp4", "playable": True},
        "POST_VIDEO": {"mime": "video/mp4", "ext": "mp4", "playable": True},
        "POST_IMAGE": {"mime": "image/jpeg", "ext": "jpg", "playable": False},
        "CAROUSEL": {"mime": "multipart/mixed", "ext": "carousel", "playable": False},
        "STORY": {"mime": "video/mp4" if is_video else "image/jpeg", "ext": "mp4" if is_video else "jpg", "playable": is_video},
        "HIGHLIGHT": {"mime": "video/mp4" if is_video else "image/jpeg", "ext": "mp4" if is_video else "jpg", "playable": is_video},
        "PROFILE_DP": {"mime": "image/jpeg", "ext": "jpg", "playable": False},
        "AUDIO": {"mime": "audio/mp4", "ext": "m4a", "playable": True}
    }

    info = valid_types.get(item_type)
    return info

def test_media_types_exhaustive():
    checks = [
        ("Reel media type valid", classify_and_validate_media("REEL", "video.mp4", True)["ext"] == "mp4"),
        ("Post Image media type valid", classify_and_validate_media("POST_IMAGE", "photo.jpg", False)["ext"] == "jpg"),
        ("Post Video media type valid", classify_and_validate_media("POST_VIDEO", "video.mp4", True)["mime"] == "video/mp4"),
        ("Carousel media type indexed", classify_and_validate_media("CAROUSEL", "carousel", False) is not None),
        ("Story video media type valid", classify_and_validate_media("STORY", "story.mp4", True)["ext"] == "mp4"),
        ("Story photo media type valid", classify_and_validate_media("STORY", "story.jpg", False)["ext"] == "jpg"),
        ("Profile DP media type valid", classify_and_validate_media("PROFILE_DP", "dp.jpg", False)["ext"] == "jpg"),
        ("Audio M4A media type valid", classify_and_validate_media("AUDIO", "audio.m4a", False)["mime"] == "audio/mp4")
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "All Media Types (Reels, Posts, Carousel, Stories, DP, Audio) Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    r = test_media_types_exhaustive()
    print(f"{r['suite']}: {r['passed']}/{r['total']} PASSED")

