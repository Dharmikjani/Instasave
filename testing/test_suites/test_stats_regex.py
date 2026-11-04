import re

STATS_PATTERN = re.compile(
    r"([0-9.,]+[KMBkmb]?)\s*Followers,\s*([0-9.,]+[KMBkmb]?)\s*Following,\s*([0-9.,]+)\s*Posts",
    re.IGNORECASE
)

def parse_stats_from_og_desc(og_desc: str):
    followers = "--"
    following = "--"
    posts = 0

    # Normalize non-breaking spaces
    normalized = og_desc.replace('\u00A0', ' ').replace('\u202F', ' ').replace('&nbsp;', ' ').strip()

    match = STATS_PATTERN.search(normalized)
    if match:
        followers = match.group(1)
        following = match.group(2)
        posts = int(match.group(3).replace(",", ""))
    else:
        folM = re.search(r"([0-9.,]+[KMBkmb]?)\s*Followers", normalized, re.I)
        if folM: followers = folM.group(1)
        fngM = re.search(r"([0-9.,]+[KMBkmb]?)\s*Following", normalized, re.I)
        if fngM: following = fngM.group(1)
        pstM = re.search(r"([0-9.,]+)\s*Posts", normalized, re.I)
        if pstM: posts = int(pstM.group(1).replace(",", ""))

    # Clean bio extraction
    clean_bio = ""
    if "photos and videos from" in normalized:
        clean_bio = normalized.split("):")[-1].strip() if "):" in normalized else ""
    else:
        clean_bio = normalized

    return {
        "followers": followers,
        "following": following,
        "posts": posts,
        "clean_bio": clean_bio
    }

def test_stats_regex():
    sample_desc = "271M Followers, 279 Following, 1,058 Posts - See Instagram photos and videos from Virat Kohli (@virat.kohli)"
    res = parse_stats_from_og_desc(sample_desc)

    # Test with Non-Breaking Space (\u00A0) exactly as Instagram produces
    sample_desc_nbsp = "271M\u00A0Followers,\u00A0279\u00A0Following,\u00A01,058\u00A0Posts - See Instagram photos and videos from Virat Kohli (@virat.kohli)"
    res_nbsp = parse_stats_from_og_desc(sample_desc_nbsp)

    sample_desc2 = "1.5K Followers, 420 Following, 85 Posts - See Instagram photos and videos from Tech Guy (@techguy): Passionate coder and android developer."
    res2 = parse_stats_from_og_desc(sample_desc2)

    checks = [
        ("Virat Kohli Followers parsed as 271M", res["followers"] == "271M"),
        ("Virat Kohli Following parsed as 279", res["following"] == "279"),
        ("Virat Kohli Posts parsed as 1058", res["posts"] == 1058),
        ("Followers is NOT empty string or --", res["followers"] != "--" and res["followers"] != ""),
        ("Posts is NOT 0", res["posts"] != 0),
        ("NBSP: Virat Kohli Followers parsed as 271M with \\u00A0", res_nbsp["followers"] == "271M"),
        ("NBSP: Virat Kohli Following parsed as 279 with \\u00A0", res_nbsp["following"] == "279"),
        ("NBSP: Virat Kohli Posts parsed as 1058 with \\u00A0", res_nbsp["posts"] == 1058),
        ("Small Creator Followers parsed as 1.5K", res2["followers"] == "1.5K"),
        ("Small Creator Following parsed as 420", res2["following"] == "420"),
        ("Small Creator Posts parsed as 85", res2["posts"] == 85),
        ("Clean Bio extracted without boilerplate", res2["clean_bio"] == "Passionate coder and android developer.")
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "Meta Description Stats & Bio Regex Parser Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    r = test_stats_regex()
    print(f"{r['suite']}: {r['passed']}/{r['total']} PASSED")

