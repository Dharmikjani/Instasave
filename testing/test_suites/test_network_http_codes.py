def simulate_http_handler(status_code: int, has_session: bool):
    if status_code == 200:
        return {"action": "PARSE_JSON", "user_error": None}
    elif status_code == 302:
        if not has_session:
            return {"action": "TRIGGER_FALLBACK_HTML", "user_error": None}
        else:
            return {"action": "TRIGGER_FALLBACK_HTML", "user_error": None}
    elif status_code == 401:
        return {"action": "EXPIRED_SESSION_ALERT", "user_error": "Session expired. Please reconnect."}
    elif status_code == 403:
        return {"action": "CSRF_AUTO_RETRY", "user_error": "Access restricted. Re-injecting headers."}
    elif status_code == 404:
        return {"action": "SHOW_ERROR", "user_error": "Profile or post does not exist (404)."}
    elif status_code == 429:
        return {"action": "RATE_LIMIT_BACKOFF", "user_error": "Too many requests. Please wait a moment."}
    elif status_code >= 500:
        return {"action": "SERVER_RETRY", "user_error": "Instagram server temporarily busy (5xx)."}
    return {"action": "GENERIC_ERROR", "user_error": "Network error occurred."}

def test_network_http_codes():
    checks = [
        ("HTTP 200 parses JSON", simulate_http_handler(200, False)["action"] == "PARSE_JSON"),
        ("HTTP 302 triggers HTML fallback", simulate_http_handler(302, False)["action"] == "TRIGGER_FALLBACK_HTML"),
        ("HTTP 401 shows expired session alert", simulate_http_handler(401, True)["action"] == "EXPIRED_SESSION_ALERT"),
        ("HTTP 403 triggers CSRF auto-retry", simulate_http_handler(403, True)["action"] == "CSRF_AUTO_RETRY"),
        ("HTTP 404 shows clean not-found message", simulate_http_handler(404, False)["action"] == "SHOW_ERROR"),
        ("HTTP 429 triggers rate limit backoff", simulate_http_handler(429, False)["action"] == "RATE_LIMIT_BACKOFF"),
        ("HTTP 500 triggers server retry", simulate_http_handler(500, False)["action"] == "SERVER_RETRY"),
        ("HTTP 503 handled gracefully", simulate_http_handler(503, False)["action"] == "SERVER_RETRY")
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "HTTP Status Code, Redirect & Error Fallback Matrix Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    r = test_network_http_codes()
    print(f"{r['suite']}: {r['passed']}/{r['total']} PASSED")

