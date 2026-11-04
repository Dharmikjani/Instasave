import re

def sanitize_username(input_val: str) -> str:
    # If full URL pasted into username search
    if "instagram.com/" in input_val or "instagr.am/" in input_val:
        m = re.search(r"instagram\.com/([A-Za-z0-9_.-]+)", input_val)
        if m:
            input_val = m.group(1)

    # Strip whitespace, @, leading/trailing slashes
    clean = input_val.strip().lstrip("@").strip("/")
    # Strip null bytes
    clean = clean.replace("\0", "")
    # Remove emojis and non-standard chars
    clean = re.sub(r"[^\w.-]", "", clean)
    # Truncate to 30 chars (Instagram max username length)
    return clean[:30]

def test_security_sanitization():
    checks = [
        ("SQL Injection neutralized", sanitize_username("' OR '1'='1") == "OR11"),
        ("XSS script tag stripped", sanitize_username("<script>alert('xss')</script>") == "scriptalertxssscript"),
        ("Full URL in username extracted", sanitize_username("https://www.instagram.com/virat.kohli/") == "virat.kohli"),
        ("Leading and trailing slashes stripped", sanitize_username("/viratkohli/") == "viratkohli"),
        ("Leading @ stripped", sanitize_username("@mrbeast") == "mrbeast"),
        ("Emojis in username stripped", sanitize_username("mrbeast🔥🚀") == "mrbeast"),
        ("Null bytes stripped", sanitize_username("user\0name") == "username"),
        ("10,000 char buffer truncated to max 30", len(sanitize_username("a" * 10000)) == 30)
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "Security, Sanitization & Input Buffer Overflow Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    r = test_security_sanitization()
    print(f"{r['suite']}: {r['passed']}/{r['total']} PASSED")

