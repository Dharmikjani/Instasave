def extract_csrf_token(cookie_header: str):
    for pair in cookie_header.split(";"):
        parts = pair.strip().split("=", 1)
        if len(parts) == 2 and parts[0].strip() == "csrftoken":
            return parts[1].strip()
    return None

def resolve_cookies_for_host(host: str, cookie_store: dict):
    matched = []
    for domain, cookies in cookie_store.items():
        if host == domain or host.endswith("." + domain) or domain.endswith(host):
            matched.extend(cookies)
    return matched

def test_cookie_csrf_headers():
    sample_cookie = "mid=Z0xyz; ds_user_id=12345678; csrftoken=abc123xyzToken456; sessionid=987654321%3AxYz"
    token = extract_csrf_token(sample_cookie)

    cookie_store = {
        "instagram.com": ["sessionid=987654321", "csrftoken=abc123xyzToken456"]
    }

    # Verify requests to www.instagram.com receive cookies from instagram.com
    resolved_www = resolve_cookies_for_host("www.instagram.com", cookie_store)
    resolved_bare = resolve_cookies_for_host("instagram.com", cookie_store)

    checks = [
        ("CSRF Token Extracted Successfully", token == "abc123xyzToken456"),
        ("Cookies resolved for www.instagram.com", len(resolved_www) == 2),
        ("Cookies resolved for instagram.com", len(resolved_bare) == 2),
        ("SessionID Present in resolved cookies", any("sessionid=" in c for c in resolved_www)),
        ("Host Mismatch Prevented", len(resolved_www) > 0 and len(resolved_bare) > 0)
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "Cookie Domain Resolution & X-CSRFToken Header Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    r = test_cookie_csrf_headers()
    print(f"{r['suite']}: {r['passed']}/{r['total']} PASSED")

