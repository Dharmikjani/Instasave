import os
import zipfile

def test_apk_integrity():
    apk_path = os.path.join(os.path.dirname(__file__), "..", "..", "InstaSaver-v1.0.apk")
    apk_exists = os.path.isfile(apk_path)
    file_size_mb = os.path.getsize(apk_path) / (1024 * 1024) if apk_exists else 0

    has_manifest = False
    has_dex = False
    has_resources = False
    has_res_dir = False

    if apk_exists:
        with zipfile.ZipFile(apk_path, "r") as z:
            names = z.namelist()
            has_manifest = "AndroidManifest.xml" in names
            has_dex = any(n.startswith("classes") and n.endswith(".dex") for n in names)
            has_resources = "resources.arsc" in names
            has_res_dir = any(n.startswith("res/") for n in names)

    checks = [
        ("APK File Exists on Disk", apk_exists),
        ("APK File Size Valid (5MB - 50MB)", 5.0 <= file_size_mb <= 50.0),
        ("Binary AndroidManifest.xml Present", has_manifest),
        ("DEX Bytecode (classes.dex) Compiled", has_dex),
        ("Resources ARSC (resources.arsc) Compiled", has_resources),
        ("Asset & Drawable Resources Packaged", has_res_dir)
    ]

    passed = sum(1 for _, ok in checks if ok)
    failed = len(checks) - passed

    return {
        "suite": "Binary APK Package Integrity & Compilation Test",
        "total": len(checks),
        "passed": passed,
        "failed": failed,
        "details": [(name, True, ok, ok) for name, ok in checks]
    }

if __name__ == "__main__":
    res = test_apk_integrity()
    print(f"{res['suite']}: {res['passed']}/{res['total']} PASSED")

