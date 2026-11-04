def determine_ui_state(is_logged_in: bool, is_private: bool, reels_count: int, posts_count: int):
    # Determines what UI banner and buttons should be visible
    show_login_button = False
    show_reload_button = False
    status_message = ""

    if is_private:
        status_message = "This account is private."
        if not is_logged_in:
            show_login_button = True
    elif reels_count == 0 and posts_count == 0:
        status_message = "No reels or posts loaded yet."
        if not is_logged_in:
            show_login_button = True
        else:
            show_reload_button = True
    else:
        status_message = "MEDIA_GRID_RENDERED"

    return {
        "status_message": status_message,
        "show_login_button": show_login_button,
        "show_reload_button": show_reload_button
    }

def test_ui_state_matrix():
    # Case 1: Connected user, 0 reels -> Must show Reload button, NEVER Login button
    case1 = determine_ui_state(is_logged_in=True, is_private=False, reels_count=0, posts_count=0)
    # Case 2: Guest user, 0 reels -> Must show Login button
    case2 = determine_ui_state(is_logged_in=False, is_private=False, reels_count=0, posts_count=0)
    # Case 3: Connected user, 10 reels -> Media grid rendered
    case3 = determine_ui_state(is_logged_in=True, is_private=False, reels_count=10, posts_count=5)
    # Case 4: Guest user, Private account -> Shows private notice and login button
    case4 = determine_ui_state(is_logged_in=False, is_private=True, reels_count=0, posts_count=0)
    # Case 5: Connected user, Private account -> Shows private notice without redundant login button
    case5 = determine_ui_state(is_logged_in=True, is_private=True, reels_count=0, posts_count=0)

    checks = [
        ("Connected user with 0 reels shows Reload button", case1["show_reload_button"] is True),
        ("Connected user with 0 reels hides Login button (Fixes contradiction)", case1["show_login_button"] is False),
        ("Guest user with 0 reels shows Login button", case2["show_login_button"] is True),
        ("Populated profile renders grid", case3["status_message"] == "MEDIA_GRID_RENDERED"),
        ("Private account message for guest", case4["status_message"] == "This account is private."),
        ("Private account for connected user suppresses redundant login button", case5["show_login_button"] is False)
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "UI State Permutation & Contradiction Suppression Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    r = test_ui_state_matrix()
    print(f"{r['suite']}: {r['passed']}/{r['total']} PASSED")

