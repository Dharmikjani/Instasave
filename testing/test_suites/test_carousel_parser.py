import json
import os

def parse_carousel_json(raw_json: str):
    obj = json.loads(raw_json)
    item = obj["items"][0]
    user = item["user"]

    username = user["username"]
    caption = item["caption"]["text"]
    carousel_media = item.get("carousel_media", [])

    slides = []
    for i, slide in enumerate(carousel_media):
        is_video = slide.get("media_type") == 2
        slide_download = None
        if is_video:
            slide_download = slide["video_versions"][0]["url"]
        else:
            slide_download = slide["image_versions2"]["candidates"][0]["url"]

        thumb = slide["image_versions2"]["candidates"][0]["url"]

        slides.append({
            "index": i + 1,
            "is_video": is_video,
            "download_url": slide_download,
            "thumbnail_url": thumb
        })

    return {
        "username": username,
        "caption": caption,
        "slide_count": len(slides),
        "slides": slides
    }

def test_carousel_parser():
    mock_file = os.path.join(os.path.dirname(__file__), "..", "mock_data", "sample_carousel.json")
    with open(mock_file, "r", encoding="utf-8") as f:
        content = f.read()

    data = parse_carousel_json(content)

    checks = [
        ("Username Matches", data["username"] == "natgeo"),
        ("Slide Count is 3", data["slide_count"] == 3),
        ("Slide 1 is Photo", data["slides"][0]["is_video"] is False and "slide1_photo.jpg" in data["slides"][0]["download_url"]),
        ("Slide 2 is Video", data["slides"][1]["is_video"] is True and "slide2_video.mp4" in data["slides"][1]["download_url"]),
        ("Slide 3 is Photo", data["slides"][2]["is_video"] is False and "slide3_photo.jpg" in data["slides"][2]["download_url"])
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "Multi-Slide Carousel Parser Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    res = test_carousel_parser()
    print(f"{res['suite']}: {res['passed']}/{res['total']} PASSED")

