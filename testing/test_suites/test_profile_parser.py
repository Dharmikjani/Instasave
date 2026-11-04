import json
import os

def format_count(count: int) -> str:
    if count >= 1_000_000:
        return f"{count / 1_000_000:.1f}M"
    elif count >= 1_000:
        return f"{count / 1_000:.1f}K"
    return str(count)

def parse_profile_json(raw_json: str):
    obj = json.loads(raw_json)
    user = obj["data"]["user"]

    username = user["username"]
    full_name = user["full_name"]
    bio = user["biography"]
    is_verified = user["is_verified"]
    is_private = user["is_private"]
    hd_avatar = user.get("profile_pic_url_hd") or user.get("profile_pic_url")

    followers_raw = user["edge_followed_by"]["count"]
    following_raw = user["edge_follow"]["count"]
    posts_raw = user["edge_owner_to_timeline_media"]["count"]

    reels_dict = {}
    posts_dict = {}

    # 1. Parse Felix Video Timeline (Reels)
    felix_edges = user.get("edge_felix_video_timeline", {}).get("edges", [])
    for edge in felix_edges:
        node = edge["node"]
        shortcode = node["shortcode"]
        reels_dict[shortcode] = {
            "shortcode": shortcode,
            "is_video": True,
            "download_url": node.get("video_url", node["display_url"]),
            "thumbnail_url": node["display_url"]
        }

    # 2. Parse Timeline Media
    timeline_edges = user.get("edge_owner_to_timeline_media", {}).get("edges", [])
    for edge in timeline_edges:
        node = edge["node"]
        shortcode = node["shortcode"]
        is_video = node.get("is_video", False)
        item = {
            "shortcode": shortcode,
            "is_video": is_video,
            "download_url": node.get("video_url", node["display_url"]) if is_video else node["display_url"],
            "thumbnail_url": node["display_url"]
        }
        if is_video:
            if shortcode not in reels_dict:
                reels_dict[shortcode] = item
        else:
            posts_dict[shortcode] = item

    return {
        "username": username,
        "full_name": full_name,
        "bio": bio,
        "is_verified": is_verified,
        "is_private": is_private,
        "hd_avatar": hd_avatar,
        "followers_formatted": format_count(followers_raw),
        "following_formatted": format_count(following_raw),
        "posts_count": posts_raw,
        "reels_count": len(reels_dict),
        "posts_count_parsed": len(posts_dict)
    }

def test_profile_parser():
    mock_file = os.path.join(os.path.dirname(__file__), "..", "mock_data", "sample_profile.json")
    with open(mock_file, "r", encoding="utf-8") as f:
        content = f.read()

    data = parse_profile_json(content)

    checks = [
        ("Username Matches", data["username"] == "mrbeast"),
        ("Full Name Matches", data["full_name"] == "MrBeast"),
        ("Verified Badge True", data["is_verified"] is True),
        ("Account is Not Private", data["is_private"] is False),
        ("HD Avatar Extracted", data["hd_avatar"] == "https://scontent.cdninstagram.com/avatar_hd.jpg"),
        ("Followers Formatted (62.5M)", data["followers_formatted"] == "62.5M"),
        ("Following Formatted (215)", data["following_formatted"] == "215"),
        ("Reels & Posts Segregated (1 reel via Felix, 1 post via Timeline)", data["reels_count"] == 1 and data["posts_count_parsed"] == 1)
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "Ghost Profile Scraper & Stats Parser Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    res = test_profile_parser()
    print(f"{res['suite']}: {res['passed']}/{res['total']} PASSED")
