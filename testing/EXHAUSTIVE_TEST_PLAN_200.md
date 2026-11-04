# 📋 InstaSaver Pro — 200+ Exhaustive Test Scenarios & Master Test Plan

**Document Version:** 3.0 (Zero-Omission Enterprise QA Edition)  
**Standard:** ISO/IEC/IEEE 29119 Software Testing Standard  
**Goal:** 100% Comprehensive Coverage • Zero Blindspots • 0% Hallucination

---

## 🧭 Master Test Architecture (10 Modules • 200+ Scenarios)

```
┌───────────────────────────────────────────────────────────────────────────────────────┐
│                      INSTASAVER PRO — EXHAUSTIVE TEST MATRIX                          │
├───────────────────────────────────────────────────────────────────────────────────────┤
│ Module 1: URL Variations, Parsing & Malformed Links (25 Scenarios)                    │
│ Module 2: Network Codes, Redirects, Timeouts & Fallbacks (20 Scenarios)               │
│ Module 3: Authentication, WebView Login & Cookie Engine (20 Scenarios)                │
│ Module 4: Profile Explorer & Creator Grid Extraction (25 Scenarios)                   │
│ Module 5: Media Extractor (Reels, Posts, Carousels, Stories, Audios) (25 Scenarios)  │
│ Module 6: Concurrency Queue, Rate-Limiting & Stress Testing (20 Scenarios)            │
│ Module 7: Storage, MediaStore & OS Scoped Storage (15 Scenarios)                      │
│ Module 8: In-App ExoPlayer & Playback Lifecycle (15 Scenarios)                        │
│ Module 9: Watchlist, Offline Mode & Local Gallery (15 Scenarios)                      │
│ Module 10: Security, Sanitization, Injection & Edge Conditions (20 Scenarios)         │
├───────────────────────────────────────────────────────────────────────────────────────┤
│ Total Planned Scenarios: 200 Test Cases                                               │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 MODULE 1: URL Variations, Parsing & Malformed Links (25 Scenarios)

| ID | Test Scenario | Input Data | Expected Behavior |
|:---|:---|:---|:---|
| **M1-01** | Standard Public Reel URL | `https://www.instagram.com/reel/C7xY9Z1A2B3/` | Extracts shortcode `C7xY9Z1A2B3` |
| **M1-02** | Reel URL without trailing slash | `https://www.instagram.com/reel/C7xY9Z1A2B3` | Extracts shortcode `C7xY9Z1A2B3` |
| **M1-03** | Reel URL with IG tracking `igsh` | `https://instagram.com/reel/C7xY9Z1A2B3/?igsh=MWx5YXAz` | Strips tracking, extracts `C7xY9Z1A2B3` |
| **M1-04** | Reel URL with UTM parameters | `https://instagram.com/reel/C7xY9Z1A2B3/?utm_source=ig_web_copy_link` | Strips UTM, extracts `C7xY9Z1A2B3` |
| **M1-05** | Reel URL with Facebook click ID | `https://instagram.com/reel/C7xY9Z1A2B3/?fbclid=IwAR123` | Strips fbclid, extracts `C7xY9Z1A2B3` |
| **M1-06** | Share Reel URL format | `https://www.instagram.com/share/reel/CQabcdef123/` | Extracts shortcode `CQabcdef123` |
| **M1-07** | Shortened Share URL | `https://instagram.com/share/reel/CQabcdef123` | Extracts shortcode `CQabcdef123` |
| **M1-08** | Feed Post URL (Photo) | `https://www.instagram.com/p/DB12345XYZ/` | Extracts shortcode `DB12345XYZ` |
| **M1-09** | Feed Post URL (Video) | `https://instagram.com/p/DB98765ZYX/` | Extracts shortcode `DB98765ZYX` |
| **M1-10** | Carousel Post URL | `https://www.instagram.com/p/CarouselPost123/` | Extracts shortcode `CarouselPost123` |
| **M1-11** | IGTV URL | `https://www.instagram.com/tv/B_xyz987654/` | Extracts shortcode `B_xyz987654` |
| **M1-12** | Mobile Web domain (`m.instagram.com`) | `https://m.instagram.com/reel/C7xY9Z1A2B3/` | Extracts shortcode `C7xY9Z1A2B3` |
| **M1-13** | Bare domain (`instagr.am`) | `https://instagr.am/p/C7xY9Z1A2B3/` | Extracts shortcode `C7xY9Z1A2B3` |
| **M1-14** | HTTP insecure URL | `http://www.instagram.com/reel/C7xY9Z1A2B3/` | Auto-upgrades to HTTPS, extracts code |
| **M1-15** | URL with leading/trailing spaces | `  https://www.instagram.com/reel/C7xY9Z1A2B3/  ` | Trims spaces, extracts code |
| **M1-16** | URL with newlines from clipboard | `\nhttps://www.instagram.com/reel/C7xY9Z1A2B3/\n` | Strips newlines, extracts code |
| **M1-17** | Shortcode containing hyphen & underscore | `https://instagram.com/reel/C-123_456-ABC/` | Extracts `C-123_456-ABC` |
| **M1-18** | Uppercase schema and domain | `HTTPS://WWW.INSTAGRAM.COM/REEL/C7xY9Z1A2B3/` | Normalizes to lowercase, extracts code |
| **M1-19** | URL-encoded Instagram link | `https%3A%2F%2Fwww.instagram.com%2Freel%2FC7xY9Z1A2B3%2F` | Decodes URL entities, extracts code |
| **M1-20** | Non-Instagram link (YouTube) | `https://www.youtube.com/watch?v=dQw4w9WgXcQ` | Rejects with "Invalid Instagram link" |
| **M1-21** | Non-Instagram link (Facebook Reel) | `https://www.facebook.com/reel/1234567890` | Rejects cleanly |
| **M1-22** | Non-Instagram link (TikTok) | `https://www.tiktok.com/@user/video/123` | Rejects cleanly |
| **M1-23** | Non-Instagram link (Twitter/X) | `https://x.com/user/status/12345` | Rejects cleanly |
| **M1-24** | Random text input | `This is not a url just plain text` | Rejects cleanly |
| **M1-25** | Empty / Blank input | `   ` | Rejects cleanly without crash |

---

## 🌐 MODULE 2: Network Codes, Redirects, Timeouts & Fallbacks (20 Scenarios)

| ID | Test Scenario | Network Condition | Expected Behavior |
|:---|:---|:---|:---|
| **M2-01** | HTTP 200 Clean Response | Normal server state | Parses JSON/HTML immediately |
| **M2-02** | HTTP 301 Moved Permanently | Canonical redirect | Follows redirect to target URL |
| **M2-03** | HTTP 302 Redirect to Login | Unauthenticated mobile IP | Triggers HTML fallback parser |
| **M2-04** | HTTP 400 Bad Request | Corrupted request payload | Returns clean error to UI |
| **M2-05** | HTTP 401 Unauthorized | Expired session cookie | Alerts user to re-login |
| **M2-06** | HTTP 403 CSRF Mismatch | Missing X-CSRFToken header | Triggers auto-recovery with token injection |
| **M2-07** | HTTP 404 Not Found | Post deleted by creator | Shows "Post has been deleted (404)" |
| **M2-08** | HTTP 429 Too Many Requests | Rate limit hit by IG | Activates exponential backoff delay |
| **M2-09** | HTTP 500 Internal Error | IG server outage | Shows "Instagram server error (500)" |
| **M2-10** | HTTP 502 Bad Gateway | Cloudflare / Proxy glitch | Auto-retries once after 1 second |
| **M2-11** | HTTP 503 Service Unavailable | Maintenance mode | Shows "Service busy, please retry" |
| **M2-12** | HTTP 504 Gateway Timeout | Upstream CDN lag | Aborts without hanging app |
| **M2-13** | Connect Timeout (15s) | Dropped packets on connect | Throws SocketTimeoutException handled |
| **M2-14** | Read Timeout (30s) | Slow video download stream | Throws ReadTimeoutException handled |
| **M2-15** | DNS Lookup Failure | No internet / Airplane mode | Catches UnknownHostException gracefully |
| **M2-16** | SSL Handshake Interrupted | Proxy interception | SSLHandshakeException handled cleanly |
| **M2-17** | Network Drop mid-stream | Wi-Fi cut at 40% progress | Cleans up partial file, shows error |
| **M2-18** | Cellular to Wi-Fi handover | Network switches during download | Connection handles socket migration |
| **M2-19** | Zero Internet on Launch | Offline start | Renders offline gallery immediately |
| **M2-20** | CDN URL with `&amp;` entities | HTML unescaping needed | Replaces `&amp;` with `&` for video player |

---

## 🔐 MODULE 3: Authentication, WebView Login & Cookie Engine (20 Scenarios)

| ID | Test Scenario | Preconditions | Expected Behavior |
|:---|:---|:---|:---|
| **M3-01** | Guest State Initial Launch | Fresh app install | `isLoggedIn = false`, top bar shows `🔓 Login (Unlock All)` |
| **M3-02** | WebView Official Login Dialog | Tap login chip | Renders official `https://www.instagram.com/accounts/login/` |
| **M3-03** | Cookie Capture on Success | User enters credentials | Captures `sessionid`, `csrftoken`, `ds_user_id` |
| **M3-04** | Session Persistence | Cold restart app | Restores cookies from SharedPreferences |
| **M3-05** | Domain Resolution (Bare vs WWW) | Request `www.instagram.com` | Injects cookies saved under `instagram.com` |
| **M3-06** | `X-CSRFToken` Auto-Injection | Cookie contains `csrftoken` | Injects `X-CSRFToken: [token]` into headers |
| **M3-07** | `X-IG-App-ID` Header | Any API call | Always sends `X-IG-App-ID: 936619743392459` |
| **M3-08** | `X-ASBD-ID` Header | Any API call | Always sends `X-ASBD-ID: 129477` |
| **M3-09** | `X-Requested-With` Header | AJAX endpoints | Always sends `X-Requested-With: XMLHttpRequest` |
| **M3-10** | Mobile User-Agent Header | Any request | Sends realistic modern Android Chrome UA |
| **M3-11** | Manual Logout Flow | User taps connected chip | Calls `removeAllCookies()`, clears prefs, sets `isLoggedIn = false` |
| **M3-12** | Two-Factor Auth (2FA) SMS | User has 2FA enabled | WebView displays 2FA SMS code entry page |
| **M3-13** | Authenticator App (TOTP) | User has TOTP enabled | WebView displays TOTP prompt smoothly |
| **M3-14** | CAPTCHA / Bot Challenge | IG presents visual challenge | User solves challenge inside WebView |
| **M3-15** | Remember Me Checked | Login form | Session cookie lifetime extends across months |
| **M3-16** | Account Password Security | App architecture | App NEVER reads or logs username/password |
| **M3-17** | Corrupted Cookie Recovery | Malformed cookie string | Cleanses malformed pairs without crashing |
| **M3-18** | Multi-Account Cookie Flush | Re-login with different user | Flushes old user cookies before new login |
| **M3-19** | CookieManager System Sync | Android OS update | Calls `CookieManager.getInstance().flush()` |
| **M3-20** | Top Bar Session State Sync | Logout event | Top bar immediately reflects `🔓 Login (Unlock All)` |

---

## 👤 MODULE 4: Profile Explorer & Creator Grid Extraction (25 Scenarios)

| ID | Test Scenario | Target Profile | Expected Behavior |
|:---|:---|:---|:---|
| **M4-01** | Mega Celebrity (270M+ followers) | `@virat.kohli` | Followers shows `271M`, Posts shows `1,058` |
| **M4-02** | High-Volume Creator (60M+) | `@mrbeast` | Stats format as `62.5M`, bio loaded |
| **M4-03** | Small Creator (< 1000 followers) | `@smallcreator` | Formats as raw integer e.g. `450` |
| **M4-04** | Mid-Tier Creator (50K - 999K) | `@midcreator` | Formats as `150.4K` |
| **M4-05** | Verified Blue Tick Account | Any verified user | Blue checkmark icon renders beside name |
| **M4-06** | Unverified Account | Standard user | No blue tick icon rendered |
| **M4-07** | Felix Reels Timeline Extraction | User with active reels | Populates `reels` list from `edge_felix_video_timeline` |
| **M4-08** | Timeline Posts Extraction | User with photo posts | Populates `posts` list from `edge_owner_to_timeline_media` |
| **M4-09** | De-duplication of Reels in Feed | Reel posted to feed | Item appears once in Reels tab, not duplicated |
| **M4-10** | HTML Fallback Meta Description Regex | API returns 302 | Regex extracts followers, following, and posts |
| **M4-11** | Full HD DP Resolution | Any public profile | Extracts `hd_profile_pic_url_info` (1080x1080) |
| **M4-12** | HD DP Zoom Modal Display | Tap avatar | Fullscreen 1:1 dialog with pinch-to-zoom |
| **M4-13** | Save HD DP Button Action | Inside DP modal | Queues avatar download to `Pictures/InstaSaver/` |
| **M4-14** | Bio with Multilingual Text | Hindi/Russian/Arabic bio | Renders clean UTF-8 text |
| **M4-15** | Bio with Line Breaks | Multi-line formatted bio | Preserves newline formatting |
| **M4-16** | External Link Extraction | User with bio website link | Extracts and makes external link clickable |
| **M4-17** | Private Account (Followed) | User follows private account | Displays all media using session cookies |
| **M4-18** | Private Account (Not Followed) | User does not follow | Displays "This account is private." |
| **M4-19** | Non-Existent Username | `random_user_not_exist_404` | Displays "Profile does not exist" |
| **M4-20** | Leading `@` in search input | `@username` | Sanitizes input to `username` |
| **M4-21** | Trailing whitespace in search | `username   ` | Trims whitespace cleanly |
| **M4-22** | Reels Tab Count Badge | Profile loaded | Shows `Reels (XX)` matching list size |
| **M4-23** | Posts Tab Count Badge | Profile loaded | Shows `Posts (XX)` matching list size |
| **M4-24** | 3-Column Grid Responsive Layout | Any device width | Grid adjusts column width evenly |
| **M4-25** | Reload Profile Button Click | Connected user with 0 reels | Re-triggers profile search pipeline |

---

## 📥 MODULE 5: Media Extractor (Reels, Posts, Carousels, Stories, Audios) (25 Scenarios)

| ID | Test Scenario | Content Type | Expected Behavior |
|:---|:---|:---|:---|
| **M5-01** | Standard 1080p60 Reel | Reel | Direct CDN MP4 URL resolved |
| **M5-02** | Reel Thumbnail Extraction | Reel | High-res poster image resolved |
| **M5-03** | Reel Video Duration | Reel | Duration seconds parsed |
| **M5-04** | Single Photo Feed Post | Image Post | 1080x1350 or 1080x1080 JPG resolved |
| **M5-05** | Single Video Feed Post | Video Post | MP4 URL resolved |
| **M5-06** | Carousel 2-Slide Post | Carousel | Both slides extracted |
| **M5-07** | Carousel 5-Slide Mixed | Carousel (Photo+Video) | Photo slides as JPG, Video slides as MP4 |
| **M5-08** | Carousel 10-Slide Post | Max length carousel | All 10 slides extracted with correct index |
| **M5-09** | Carousel Slide Order Preservation | Carousel | Slide 1 is index 1, Slide 10 is index 10 |
| **M5-10** | Audio Stream Extraction | Reel with music | Resolves background M4A audio stream |
| **M5-11** | Audio Metadata Tagging | Audio | Track title and artist name populated |
| **M5-12** | Audio Cover Artwork | Audio | Album art image URL populated |
| **M5-13** | Original Caption Text Extraction | Post with long caption | Full text extracted |
| **M5-14** | Hashtag Extraction | Post with 10 hashtags | List of hashtags parsed with `#` symbol |
| **M5-15** | Post with Zero Hashtags | Caption without tags | Returns emptyList() without crashing |
| **M5-16** | Caption with Special Emojis | Caption with 50 emojis | Emojis preserved in clipboard copy |
| **M5-17** | Embed Capioned Scraping | Public post | Primary embed scraper extracts video tag |
| **M5-18** | OpenGraph Meta-Tag Scraping | Secondary tier | Extracts `og:video` and `og:image` |
| **M5-19** | JSON-LD Script Scraping | Tertiary tier | Extracts schema.org JSON-LD payload |
| **M5-20** | Unescaping `\u0026` in CDN URLs | CDN URL | Unescapes unicode ampersand |
| **M5-21** | Unescaping `&amp;` in CDN URLs | CDN URL | Unescapes XML ampersand |
| **M5-22** | Age-Restricted Reel Handling | Age-gated media | Uses session cookies to bypass gate |
| **M5-23** | Music Copyright Muted Reel | Muted video | Video video stream still downloads |
| **M5-24** | Square 1:1 Aspect Ratio Media | Photo post | Aspect ratio 1.0 handled in UI |
| **M5-25** | Portrait 9:16 Aspect Ratio Media | Reel | Aspect ratio 0.5625 handled in UI |

---

## ⚡ MODULE 6: Concurrency Queue, Rate-Limiting & Stress Testing (20 Scenarios)

| ID | Test Scenario | Stress Load | Expected Behavior |
|:---|:---|:---|:---|
| **M6-01** | Single Download Task | 1 item | Starts immediately, completes with URI |
| **M6-02** | 2 Simultaneous Downloads | 2 items | Both download concurrently (Semaphore capacity = 2) |
| **M6-03** | 3 Simultaneous Downloads | 3 items | Item 1 & 2 run; Item 3 waits in queue |
| **M6-04** | 10 Items Batch Download | 10 items | Max 2 running at any instant, drains queue |
| **M6-05** | 50 Items Profile Bulk Download | 50 items | Runs without OOM or thread pool starvation |
| **M6-06** | 350ms Request Pacing | Batch queue | 350ms delay between consecutive task starts |
| **M6-07** | "Download All Reels" Trigger | Profile explorer | Dispatches all creator reels to queue |
| **M6-08** | "Download All Posts" Trigger | Profile explorer | Dispatches all creator posts to queue |
| **M6-09** | Individual Item Failure in Batch | Item 4 of 10 gives 404 | Items 1-3, 5-10 complete successfully |
| **M6-10** | Progress StateFlow Updates | Downloading file | Emits continuous 0.0 to 1.0 progress updates |
| **M6-11** | Byte Count Tracking | 25MB file | Reports bytesDownloaded and totalBytes |
| **M6-12** | Download Completion Flag | Finish file | Sets `isCompleted = true` on completion |
| **M6-13** | Rapid Tap Debouncing | 10 rapid taps on button | Debounces clicks, enqueues only 1 task |
| **M6-14** | Memory Leak Test during 50 Downloads | 50 items | Heap memory remains stable (< 80MB) |
| **M6-15** | Background Queue Execution | Home button pressed | Downloads continue in background service |
| **M6-16** | Notification Percentage Sync | Active download | Percentage matches exact bytes transferred |
| **M6-17** | Duplicate Shortcode Handling | Same reel queued twice | Skips or overwrites cleanly |
| **M6-18** | Thread Pool Dispatchers | Heavy I/O | All network and disk operations on `Dispatchers.IO` |
| **M6-19** | UI Main Thread Unblocked | Heavy batch running | UI maintains 60fps scrolling without stutter |
| **M6-20** | Cancel Batch Queue Action | 20 items queued | Aborts remaining queued items on demand |

---

## 💾 MODULE 7: Storage, MediaStore & OS Scoped Storage (15 Scenarios)

| ID | Test Scenario | Android Version | Expected Behavior |
|:---|:---|:---|:---|
| **M7-01** | MediaStore `IS_PENDING = 1` | Android 10+ (API 29-36) | File hidden from gallery while downloading |
| **M7-02** | MediaStore `IS_PENDING = 0` | Android 10+ | File becomes visible on completion |
| **M7-03** | Video Relative Path | Android 10+ | Saved in `Movies/InstaSaver/[Creator]/` |
| **M7-04** | Photo Relative Path | Android 10+ | Saved in `Pictures/InstaSaver/[Creator]/` |
| **M7-05** | Audio Relative Path | Android 10+ | Saved in `Music/InstaSaver/` |
| **M7-06** | Legacy Storage Fallback | Android 8/9 (API 26-28) | Saves via Environment public directories |
| **M7-07** | Creator Name Sanitization | Creator name with dots/slashes | Sanitizes to alphanumeric + underscore |
| **M7-08** | File Naming Format | Any download | `Insta_[Creator]_[Timestamp]_[Shortcode].[ext]` |
| **M7-09** | Video MIME Type | MP4 video | `video/mp4` |
| **M7-10** | Audio MIME Type | M4A audio | `audio/mp4` |
| **M7-11** | Photo MIME Type | JPG photo | `image/jpeg` |
| **M7-12** | Pre-Flight Disk Space Check | Available space < 20MB | Aborts with error: "Insufficient storage" |
| **M7-13** | Pre-Flight Disk Space OK | Available space > 20MB | Proceed to download |
| **M7-14** | Partial File Deletion on Error | Stream aborted at 50% | Calls `contentResolver.delete(uri)` |
| **M7-15** | Media Scanner Indexing | Download complete | File appears in Google Photos immediately |

---

## 🎬 MODULE 8: In-App ExoPlayer & Playback Lifecycle (15 Scenarios)

| ID | Test Scenario | Playback Condition | Expected Behavior |
|:---|:---|:---|:---|
| **M8-01** | Remote Video Streaming | Video CDN URL | ExoPlayer buffers and plays remote MP4 |
| **M8-02** | Local Video Playback | Content URI from Gallery | ExoPlayer plays locally saved file |
| **M8-03** | Infinite Reel Loop | Video reaches end | Automatically replays without freeze |
| **M8-04** | In-Line Card Player | Tap play on preview card | Video plays directly inside card |
| **M8-05** | Fullscreen Player Modal | Tap video in grid | Opens fullscreen black dialog with player |
| **M8-06** | Play / Pause Toggle | Tap player surface | Toggles playback state |
| **M8-07** | Audio Focus Loss (Phone Call) | Incoming phone call | ExoPlayer pauses audio automatically |
| **M8-08** | Audio Focus Gain | Call ends | ExoPlayer resumes playback |
| **M8-09** | Screen Orientation Change | Rotate Portrait -> Landscape | Playback continues without restarting |
| **M8-10** | Dialog Dismissal Lifecycle | Close player modal | `DisposableEffect` calls `exoPlayer.release()` |
| **M8-11** | App Background Lifecycle | Press Home button | Releases media codec, frees RAM |
| **M8-12** | App Return from Background | Return to app | Prepares player cleanly |
| **M8-13** | Hardware Acceleration Check | 1080p60 video | MediaCodec uses GPU decoding (no CPU lag) |
| **M8-14** | Corrupted Video URL | Invalid media stream | Shows player error toast without crash |
| **M8-15** | Muted Video Playback | Video with no audio | Renders video frames cleanly |

---

## 📁 MODULE 9: Watchlist, Offline Mode & Local Gallery (15 Scenarios)

| ID | Test Scenario | Preconditions | Expected Behavior |
|:---|:---|:---|:---|
| **M9-01** | Add Creator to Watchlist | In Profile Explorer | Adds creator to `WatchlistManager` StateFlow |
| **M9-02** | Duplicate Pin Prevention | Creator already in list | Button shows `[ ⭐ Pinned ]`, prevents duplicate |
| **M9-03** | Remove Creator from Watchlist | In Watchlist screen | Removes creator on delete icon click |
| **M9-04** | Watchlist JSON Persistence | Force close app | Reads pinned creators from SharedPreferences |
| **M9-05** | 1-Tap Watchlist Navigation | Tap creator card | Swaps to Profile tab and loads creator profile |
| **M9-06** | Empty Watchlist State Card | Zero pinned creators | Shows ghost icon + guide text |
| **M9-07** | Gallery MediaStore Query | Media saved on phone | Queries `DISPLAY_NAME LIKE 'Insta_%'` |
| **M9-08** | Gallery Sort Order | Multiple files | Sorted by date added descending (newest first) |
| **M9-09** | Gallery Filter: All | All chip selected | Shows both photos and videos |
| **M9-10** | Gallery Filter: Videos | Videos chip selected | Shows only video files (`isVideo = true`) |
| **M9-11** | Gallery Filter: Photos | Photos chip selected | Shows only image files (`isVideo = false`) |
| **M9-12** | Gallery Filter: Creator Chip | `@viratkohli` chip | Shows only media matching creator name |
| **M9-13** | Gallery Item Tap Play | Tap video grid item | Launches fullscreen ExoPlayer dialog |
| **M9-14** | Gallery 1-Click Share | Tap share on item | Launches Android Share sheet (`ACTION_SEND`) |
| **M9-15** | Offline Launch Mode | Airplane mode on launch | Opens Gallery tab with all saved media |

---

## 🛡️ MODULE 10: Security, Sanitization, Injection & Edge Conditions (20 Scenarios)

| ID | Test Scenario | Malicious / Edge Input | Expected Behavior |
|:---|:---|:---|:---|
| **M10-01** | SQL Injection in Search | `' OR '1'='1` | Sanitized to alphanumeric, no SQL executed |
| **M10-02** | XSS Script Tag in Input | `<script>alert('xss')</script>` | Sanitized, no JS execution |
| **M10-03** | JavaScript URI Scheme | `javascript:void(0)` | Rejects with invalid URL error |
| **M10-04** | 10,000 Character String | Massive input buffer | Truncated/handled without buffer overflow |
| **M10-05** | Unicode Null Bytes | `user\0name` | Strips null bytes cleanly |
| **M10-06** | Right-to-Left (RTL) Arabic Text | Arabic bio text | Renders in RTL layout correctly |
| **M10-07** | Emojis in Search Bar | `mrbeast🔥` | Strips emoji, extracts clean `mrbeast` |
| **M10-08** | Leading & Trailing Slashes | `/virat.kohli/` | Sanitizes to `virat.kohli` |
| **M10-09** | Full URL pasted into Username Search | `https://instagram.com/virat.kohli` | Extracts `virat.kohli` username automatically |
| **M10-10** | Zero Permission Storage Exploit | Android 13+ | Uses Scoped Storage without asking dangerous permissions |
| **M10-11** | Encrypted Session Storage | Cookie storage | Private SharedPreferences not world-readable |
| **M10-12** | WebView SSL Error Intercept | MITM proxy detected | Rejects unverified SSL certificates |
| **M10-13** | WebView URL Navigation Sandbox | WebView open | Restricts navigation to `instagram.com` domain only |
| **M10-14** | Zero Telemetry / Analytics Leak | App runtime | No user data sent to third-party tracking servers |
| **M10-15** | Memory Overhead on 100MB Video | Large video download | Streamed via 16KB buffer, heap delta < 5MB |
| **M10-16** | Background Task Termination | OS kills background task | Cleans up open sockets and file locks |
| **M10-17** | Dark Mode Theme Accessibility | Color contrast | Contrast ratio >= 4.5:1 (WCAG AA compliant) |
| **M10-18** | Minimum Touch Target Sizes | All buttons/icons | Target size >= 48dp x 48dp |
| **M10-19** | TalkBack Screen Reader | Accessibility labels | All buttons have clear `contentDescription` |
| **M10-20** | No Watermark Guarantee | Downloaded video | Video output identical byte-for-byte to IG CDN |

---

## 🎯 Final QA Conclusion
यह मास्टर टेस्ट प्लान कुल **200 विस्तृत परिदृश्यों (Scenarios)** को कवर करता है। इसमें URL पार्सिंग, नेटवर्क प्रोटोकॉल्स, कुकीज, सेमाफोर, मीडियास्टोर, प्लेयर लाइफसाइकिल, और सुरक्षा संबंधी एक भी पहलू अछूता नहीं है।

