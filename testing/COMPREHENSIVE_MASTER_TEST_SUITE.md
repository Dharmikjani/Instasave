# 🧪 InstaSaver Pro — Comprehensive Master Test Plan & Detailed Scenario Matrix

**Document Version:** 2.0 (Deep Industrial QA Edition)  
**Standard:** IEEE 829 & Android Quality Guidelines  
**Coverage Target:** 100% End-to-End • 0% Hallucination • 66 Rigorous Test Cases

---

## 🎯 1. Test Architecture & Methodology

```
+---------------------------------------------------------------------------------------+
|                       MASTER TEST RUNNER & VERIFICATION SUITE                         |
+---------------------------------------------------------------------------------------+
     │
     ├── Dimension 1: Authentication & Session Management (8 Test Cases)
     ├── Dimension 2: Profile Explorer & Scraping Engine (14 Test Cases)
     ├── Dimension 3: Media Downloader & Direct URLs (12 Test Cases)
     ├── Dimension 4: Storage, MediaStore & OS Scoped Storage (8 Test Cases)
     ├── Dimension 5: Concurrency, Bulk "Download All" & Anti-Bot Protection (8 Test Cases)
     ├── Dimension 6: In-App Player, Watchlist & Local Gallery (10 Test Cases)
     └── Dimension 7: UI/UX, Themes & Responsiveness (6 Test Cases)
```

---

## 📋 2. Exhaustive Test Case Matrix (66 Test Cases)

### 🔐 DIMENSION 1: Authentication & Session Management

| TC ID | Test Scenario | Preconditions | Input / Action | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-AUTH-01** | First Launch Guest State | Fresh install, no cookies | App launches | Top bar shows `🔓 Login (Unlock All)`, `isLoggedIn = false`, requests default to mobile headers. | Session state defaults to Guest without crashes. |
| **TC-AUTH-02** | WebView Official Login Dialog | Guest mode | Click `[ 🔓 Login (Unlock All) ]` | Dialog opens with official `https://www.instagram.com/accounts/login/`, SSL green, JavaScript enabled. | WebView renders official IG login page. |
| **TC-AUTH-03** | Cookie Interception on Success | User submits credentials on IG page | WebView redirects to `instagram.com` with `sessionid` | `CookieManager` intercepts cookies (`sessionid`, `csrftoken`, `ds_user_id`), saves to encrypted prefs, closes dialog. | Cookies extracted without reading user password. |
| **TC-AUTH-04** | Session Persistence Across App Restart | User logged in | Kill app from RAM, relaunch | App restores session from prefs, top bar shows `🔐 Connected`, OkHttp client initializes with saved cookies. | Session survives process death and cold restart. |
| **TC-AUTH-05** | Cookie Domain Matching (`instagram.com` vs `www.instagram.com`) | Cookies saved under `instagram.com` | OkHttp requests `https://www.instagram.com/api/...` | `InstagramHttpClient` attaches cookies to BOTH `www.instagram.com` and `instagram.com` without dropping. | Host mismatch bug 100% resolved. |
| **TC-AUTH-06** | `X-CSRFToken` Header Auto-Injection | `csrftoken` cookie exists | Send API request to `/api/v1/users/web_profile_info/` | Interceptor automatically parses `csrftoken` and injects `X-CSRFToken: [token]` header. | Instagram does not reject request with 403. |
| **TC-AUTH-07** | Manual Logout Flow | User logged in | Click `[ 🔐 Connected ]` chip | Clears local SharedPreferences, calls `CookieManager.removeAllCookies()`, updates UI to `🔓 Login (Unlock All)`. | All auth state completely purged. |
| **TC-AUTH-08** | Expired Session Handling | Session expired by IG server | Make API request returning HTTP 401 | App catches 401, prompts user to reconnect, does not crash. | Graceful re-login prompt displayed. |

---

### 👤 DIMENSION 2: Profile Explorer & Scraping Engine

| TC ID | Test Scenario | Preconditions | Input / Action | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-PROF-01** | Mega Celebrity Public Profile (e.g. `virat.kohli`) | App connected or guest | Search `virat.kohli` | 1. Avatar loaded.<br>2. Full Name "Virat Kohli".<br>3. Followers shows "271M" (not `--`).<br>4. Following shows "279" (not `--`).<br>5. Posts shows "1,058" (not `0`).<br>6. Reels tab populated with videos. | Zero empty stats, reels grid populated. |
| **TC-PROF-02** | Creator with Only Reels | Public creator who only uploads reels | Search username | Felix timeline parsed, Reels tab shows all videos, Posts tab shows 0. | Felix timeline properly mapped to Reels. |
| **TC-PROF-03** | Creator with Only Photos | Public photographer | Search username | Timeline parsed, Posts tab shows all photos, Reels tab shows 0. | Static photos mapped to Posts tab. |
| **TC-PROF-04** | Creator with Mixed Content | Profile with Reels + Carousels + Photos | Search username | Reels tab shows all videos; Posts tab shows carousels and images without duplication. | D-duplicated across tabs. |
| **TC-PROF-05** | Private Account (Followed by User) | User logged in, follows account | Search private friend's username | Profile loads with all reels and posts because session cookies authorize access. | Private media accessible for authorized followers. |
| **TC-PROF-06** | Private Account (Not Followed) | Guest or un-followed user | Search private username | Shows avatar, name, bio, and clear message: *"This account is private."* Grid remains empty gracefully. | Clear privacy notice displayed. |
| **TC-PROF-07** | Non-Existent Username | Any state | Search `random_nonexistent_user_99999` | Shows error card: *"Unable to load profile. Profile might not exist."* | Handled without crashing. |
| **TC-PROF-08** | Profile with Zero Posts | Fresh account with 0 posts | Search fresh user | Profile loads with `Posts: 0`, and message: *"No reels or posts loaded yet."* | Clean empty state displayed. |
| **TC-PROF-09** | Leading `@` and Whitespace in Username | Any state | Search `  @virat.kohli  ` | Input sanitized to `virat.kohli` before request. | Sanitization trims whitespace and `@`. |
| **TC-PROF-10** | Multilingual & Emoji Bio | Profile with Hindi/Tamil/Emoji bio | Search creator | Bio text renders full Unicode without garbled symbols (`???`). | UTF-8 encoding preserved. |
| **TC-PROF-11** | Full HD DP Resolution | Any public profile | Load profile | Extracts `hd_profile_pic_url_info.url` (1080x1080) instead of thumbnail (150x150). | Maximum resolution avatar URL. |
| **TC-PROF-12** | HD DP Zoom Modal | Profile loaded | Tap circular DP | Fullscreen modal opens with 1:1 image, pinch-to-zoom, and `[ ⬇ Save HD DP ]` button. | DP zoom modal interactive. |
| **TC-PROF-13** | HTML Fallback Meta Regex | API returns 302/401 | Load profile from HTML | Regex `([0-9.,]+[KMBkmb]?)\s*Followers...` extracts accurate followers, following, and posts count. | Meta description numbers parsed into stats. |
| **TC-PROF-14** | HTML Embedded Script JSON Parser | API blocked, HTML has scripts | Parse script tags | Extracts shortcodes and display URLs from embedded JSON blocks. | Media recovered even when API blocked. |

---

### 📥 DIMENSION 3: Media Downloader & Direct URLs (Home Screen)

| TC ID | Test Scenario | Preconditions | Input / Action | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-DOWN-01** | Public Reel URL Extraction | URL from Instagram reel | Paste `https://www.instagram.com/reel/C7xY9Z1A2B3/` | Shortcode `C7xY9Z1A2B3` extracted, Embed scraper fetches 1080p MP4 and poster. | MediaPreviewCard appears with video thumbnail. |
| **TC-DOWN-02** | Public Single Photo URL | URL from photo post | Paste `https://www.instagram.com/p/DB12345XYZ/` | High-res JPG URL extracted, `Download Photo` button enabled. | Direct image download URL ready. |
| **TC-DOWN-03** | Carousel Multi-Slide Post | Post with 5 slides | Paste carousel URL | `carouselItems` populated with 5 slides, horizontal thumbnail strip renders `Slide 1` to `Slide 5`. | Carousel slides selectable. |
| **TC-DOWN-04** | Audio Extraction (M4A) | Reel with background music | Click `[ 🎵 Audio (M4A) ]` | Audio stream extracted, tagged with track title and artist, queued to `Music/InstaSaver`. | Independent audio file created. |
| **TC-DOWN-05** | Long IGTV / High Bitrate 1080p | IGTV / long reel URL | Paste IGTV link | Embed parser resolves full-length MP4 URL without 15-second clipping. | Full duration media fetched. |
| **TC-DOWN-06** | Lossless CDN URL Cleaning | CDN URL with `&amp;` and `\u0026` | Process URL | All HTML entities unescaped (`&amp;` -> `&`, `\u0026` -> `&`). | Clean direct playable URL. |
| **TC-DOWN-07** | Android Share Sheet (`ACTION_SEND`) | Instagram app open | Tap Share -> Select InstaSaver | App opens directly, URL auto-populated into input box, fetch initiated automatically. | Zero manual copy-pasting needed. |
| **TC-DOWN-08** | Smart Clipboard Pill Detection | Instagram link in clipboard | Open InstaSaver | Frosted floating pill appears at bottom: `⚡ Reel link detected! [Download Now]`. | 1-tap download shortcut active. |
| **TC-DOWN-09** | Manual [ PASTE ] Button Click | Link in clipboard | Click `[ PASTE ]` | Clipboard text read and pasted into text box; auto-triggers fetch. | Immediate text box population. |
| **TC-DOWN-10** | [ ✖ Clear ] Button Click | URL in text box | Click `[ ✖ ]` | Clears text field and hides current media preview card. | State reset to idle. |
| **TC-DOWN-11** | [ 📋 Copy Caption ] Button | Media card visible | Click `[ 📋 Copy Caption ]` | Copies caption and hashtags to system clipboard, shows Toast: *"Caption copied!"*. | Clipboard contains caption text. |
| **TC-DOWN-12** | Invalid Domain / Non-IG Link | Any state | Paste `https://youtube.com/watch` or `https://facebook.com/reel` | Regex rejects URL, shows error card: *"Please enter a valid Instagram link"*. | Non-Instagram URLs strictly blocked. |

---

### 💾 DIMENSION 4: Storage, MediaStore & OS Scoped Storage

| TC ID | Test Scenario | Preconditions | Input / Action | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-STOR-01** | MediaStore `IS_PENDING = 1` Atomic Write | Android 10+ (API 29-36) | Start 30MB download | File created with `IS_PENDING = 1`. During download, file is hidden from gallery. On completion, `IS_PENDING = 0`. | No corrupt 0KB files in gallery. |
| **TC-STOR-02** | Video Destination Directory | Download Reel | Complete download | File saved to `Environment.DIRECTORY_MOVIES + "/InstaSaver/[Creator]/"`. | Saved in creator-specific folder. |
| **TC-STOR-03** | Photo Destination Directory | Download Photo | Complete download | File saved to `Environment.DIRECTORY_PICTURES + "/InstaSaver/[Creator]/"`. | Saved in Pictures folder. |
| **TC-STOR-04** | Audio Destination Directory | Extract Audio | Complete download | File saved to `Environment.DIRECTORY_MUSIC + "/InstaSaver/"`. | Saved in Music folder. |
| **TC-STOR-05** | Standardized File Naming | Any media download | Complete download | Format: `Insta_[Creator]_[YYYYMMDD_HHMMSS]_[Shortcode].[ext]`. | Deterministic, searchable filenames. |
| **TC-STOR-06** | Pre-Flight Low Storage Check | Phone space < 20MB | Click download | Checks `StatFs`. If available < 20MB, aborts with error: *"Insufficient phone storage"*. | Prevents OS crash on full disk. |
| **TC-STOR-07** | Network Drop Mid-Download Clean-up | Download in progress | Kill connection at 50% | Catches exception, deletes partial MediaStore entry via `contentResolver.delete()`. | Zero orphaned files left behind. |
| **TC-STOR-08** | Public Gallery Visibility | Download complete | Open Google Photos / Gallery | Video appears immediately in "Movies" / "InstaSaver" album without phone reboot. | MediaStore index refreshed instantly. |

---

### ⚡ DIMENSION 5: Concurrency, Bulk "Download All" & Anti-Bot Protection

| TC ID | Test Scenario | Preconditions | Input / Action | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-CONC-01** | Concurrency Semaphore Pool | 10 items queued | Start batch download | `Semaphore(2)` limits active simultaneous downloads to maximum 2 at any instant. | Never opens more than 2 download sockets. |
| **TC-CONC-02** | Polite Pacing Delay | Batch download initiated | Queue items | 350ms delay between consecutive item dispatches to protect user's IP address. | Anti-rate-limit delay verified. |
| **TC-CONC-03** | "Download All Reels" on Profile | Profile with 12 reels | Click `Download All` -> `Download All Reels` | Queues all 12 reels to `DownloadQueueManager`. Toast shows: *"Queued all 12 Reels for download!"*. | All reels processed sequentially. |
| **TC-CONC-04** | "Download All Posts" on Profile | Profile with 8 posts | Click `Download All` -> `Download All Posts` | Queues all 8 posts to `DownloadQueueManager`. | All posts processed sequentially. |
| **TC-CONC-05** | Single Item Failure Resilience | Batch has 1 invalid item | Item 3 returns 404 | Item 3 reports error; items 1, 2, 4, 5 proceed and finish successfully (`SupervisorJob`). | Batch does not abort on single error. |
| **TC-CONC-06** | Live Download Progress State | Active download | Observe progress bar | `DownloadProgress` StateFlow emits `progress` from 0.0 to 1.0 with downloaded bytes. | Real-time percentage displayed. |
| **TC-CONC-07** | Download Complete State | Download finishes | Observe card | Progress bar replaced by green text: `✓ Saved to Phone Gallery!`. | Clear success indicator. |
| **TC-CONC-08** | Rapid Multiple Click Debouncing | Media card visible | Tap Download button 5 times in 0.5s | Debounce logic ignores redundant taps; only 1 download task is enqueued. | No duplicate simultaneous downloads. |

---

### 🎬 DIMENSION 6: In-App Player, Watchlist & Local Gallery

| TC ID | Test Scenario | Preconditions | Input / Action | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-PLAY-01** | ExoPlayer Media3 Streaming | Media card or Gallery | Tap `[ ▶ Play ]` | In-line or modal PlayerView renders video using ExoPlayer hardware acceleration. | Smooth 60fps playback. |
| **TC-PLAY-02** | Infinite Reel Loop Mode | Video playing | Video reaches end | `repeatMode = Player.REPEAT_MODE_ONE` loops video automatically without stopping. | Continuous seamless looping. |
| **TC-PLAY-03** | ExoPlayer Lifecycle Management | Video playing | Press Home button or dismiss dialog | `DisposableEffect` calls `exoPlayer.release()`, stops audio, and frees RAM. | Zero background audio or memory leaks. |
| **TC-WLST-01** | Add Creator to Watchlist | Profile loaded | Click `[ ⭐ + Watchlist ]` | Creator added to `WatchlistManager`, button switches to `[ ⭐ Pinned ]`, saved in SharedPreferences. | Pinned creator persisted. |
| **TC-WLST-02** | Remove Creator from Watchlist | Creator pinned | Click Remove icon in Watchlist tab | Creator removed from list and SharedPreferences. | Watchlist list updated. |
| **TC-WLST-03** | Watchlist Survives App Restart | Creators pinned | Force stop app, relaunch | Pinned creators load immediately from SharedPreferences. | Zero watchlist data loss. |
| **TC-WLST-04** | 1-Tap Navigation to Profile | Creator in Watchlist | Tap creator card | Automatically switches to Profile tab and loads creator's profile feed in ghost mode. | Instant navigation without typing. |
| **TC-GALL-01** | Local Gallery Scanner | Media downloaded | Open Gallery tab | Queries `MediaStore` for `DISPLAY_NAME LIKE 'Insta_%'`, lists all saved videos & photos. | All downloaded files displayed. |
| **TC-GALL-02** | Gallery Filter by Media Type | Gallery has videos & photos | Click `Videos` chip | Filter updates to show only video reels; clicking `Photos` shows only images. | Accurate media filtering. |
| **TC-GALL-03** | Gallery Filter by Creator Name | Files from `@viratkohli` and `@mrbeast` | Click `@viratkohli` chip | Shows only media downloaded from Virat Kohli. | Auto-creator grouping verified. |
| **TC-GALL-04** | 1-Click Share to WhatsApp | Media in gallery | Click `[ 🔗 Share ]` on item | Launches Android Share Chooser (`ACTION_SEND`) with content URI and read permission. | Direct WhatsApp/Telegram share. |

---

### 🎨 DIMENSION 7: UI/UX, Themes & Responsiveness

| TC ID | Test Scenario | Preconditions | Input / Action | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-UI-01** | AMOLED Deep Black Theme | App open | Observe theme colors | Background renders `#090C10`, surfaces `#131822`, accents Instagram Gradient. | True AMOLED dark mode verified. |
| **TC-UI-02** | Bottom Navigation Bar Tabs | App open | Tap Downloader, Profile, Watchlist, Gallery | Active tab highlights in `IgPink`, smoothly swaps screen content without lag. | Fluid 4-tab navigation. |
| **TC-UI-03** | Edge-to-Edge System Bars | Android 14+ | Observe status & nav bars | Transparent bars blend into AMOLED background (`enableEdgeToEdge`). | Immersive full-screen design. |
| **TC-UI-04** | Keyboard Dismissal on Search | Search bar focused | Tap Search button | Virtual keyboard collapses automatically, exposing results grid. | No screen obstruction by keyboard. |
| **TC-UI-05** | Screen Rotation State Retention | Profile loaded | Rotate device Portrait -> Landscape | `rememberSaveable` & StateFlow retain loaded profile and search input without re-fetching. | Zero state loss on orientation change. |
| **TC-UI-06** | Graceful Offline Mode Launch | Airplane mode active | Launch app | App opens cleanly, Home shows disclaimer, Gallery displays all offline saved media. | Zero freeze or crash when offline. |

---

## 🏁 3. Automated Master Test Suite Implementation
सभी ऑटोमेटेबल परिदृश्यों (URL सैनिटाइजेशन, पार्सर्स, कुकीज, हेडर इंजेक्शन, स्टेट्स रेगेक्स, और बाइनरी वेरिफिकेशन) को एक मास्टर स्क्रिप्ट `testing/run_all_tests.py` में समाहित किया गया है जिसे एक कमांड से कभी भी चलाया जा सकता है:
```powershell
python testing/run_all_tests.py
```

