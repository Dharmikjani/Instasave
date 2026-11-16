# Project: InstaSaver Pro — Enterprise Multi-Module Refactoring & Dagger-Hilt Architecture

> **Document Version:** 2.0.0-ENTERPRISE  
> **Status:** ACTIVE EXECUTION  
> **Target Platform:** Android 8.0 (API 26) to Android 16 (API 36)  
> **Architecture Pattern:** Multi-Module Clean Architecture + Unidirectional Data Flow (MVI) + Dagger-Hilt DI  
> **State Folder:** `C:\Users\Dell\.gemini\antigravity\scratch\teamwork\d175b367-7df9-4e50-8d49-b81c223580a3\.agents\teamwork\`  
> **Working Directory:** `c:\Users\Dell\Downloads\new kipi devope`

---

## 1. Enterprise IT Team Structure & Work Breakdown (Requirement R1)

```
                                  +-----------------------+
                                  |          CEO          |
                                  |  (Executive & Legal)  |
                                  +-----------+-----------+
                                              |
                        +---------------------+---------------------+
                        |                                           |
            +-----------+-----------+                   +-----------+-----------+
            |    Project Manager    |                   | Architecture Designer |
            |  (Agile / Governance) |                   |  (Principal Architect)|
            +-----------+-----------+                   +-----------+-----------+
                        |                                           |
         +--------------+--------------+             +--------------+--------------+
         |                             |             |                             |
+--------+--------+           +--------+-------+     |                             |
|    Business     |           |   QA Lead /    |     |                             |
|  Analyst (BA)   |           |  Automation    |     |                             |
+-----------------+           +----------------+     |                             |
                                                     |                             |
                              +----------------------+----------------------+
                              |                                             |
                     +--------+--------+                           +--------+--------+
                     | Backend Developer|                          |Frontend Developer|
                     | (SRE / Ingestion)|                          | (Compose / MVI) |
                     +--------+--------+                           +--------+--------+
                              |                                             |
                              +----------------------+----------------------+
                                                     |
                                            +--------+--------+
                                            | DevOps Engineer |
                                            | (CI/CD, Build)  |
                                            +-----------------+
```

### 1.1 The 8 Mandatory IT Roles: Mandates, Responsibilities, Toolsets & KPIs

#### 1. Business Analyst (BA)
* **Mandate:** Translates user requirements and edge-case contracts into formal Gherkin specifications and screen functional documents.
* **Responsibilities:**
  - Define user journeys, edge-case tables (carousel pagination, audio demuxing, private profile gates).
  - Maintain the Feature Traceability Matrix (FTM) linking requirements FR-01 through FR-15 to acceptance tests.
  - Coordinate with CEO and Legal Counsel for Google Play Developer Policy compliance.
* **Deliverables:** PRD v2.0, User Story Catalog with Given-When-Then acceptance criteria, Edge-Case Specification.
* **Toolsets:** Jira / Linear, Confluence, Miro, Charles Proxy, Postman, Figma.
* **KPIs:** Requirement Ambiguity Index < 2%; 100% of user stories meeting Definition of Ready (DoR).

#### 2. Backend Developer (Scraper, Ingestion Pipeline, Mirror Gateways, Resilience APIs)
* **Mandate:** Implements the 4-tier Scraper Resiliency Engine (SRE), HTTP client infrastructure, session management, and fallback mirror gateways.
* **Responsibilities:**
  - Build Tier 0 (Room Cache with TTL), Tier 1 (TLS-impersonated HTTP Client), Tier 2 (Headless WebView DOM scraper), and Tier 3 (Distributed Mirror Gateway) in `:core:extractor` and `:core:network`.
  - Maintain `InstagramSessionManager` for optional 1-tap authenticated cookie harvesting.
  - Build the Remote Dynamic Hotfix Rule Engine.
* **Deliverables:** `:core:extractor` and `:core:network` modules, `ResilientExtractorRepository`, parser test suites with `MockWebServer`.
* **Toolsets:** Kotlin 2.3+, OkHttp 4.12, Jsoup 1.18, Android WebView API, Charles Proxy, MockWebServer.
* **KPIs:** Extraction Success Rate ≥ 99.2%; failover transition latency < 1.8s; 0 unhandled parsing crashes.

#### 3. Frontend Developer (Android / Jetpack Compose / MVI Presentation, Design System)
* **Mandate:** Develops the user interface and presentation layer using Jetpack Compose, strict MVI unidirectional data flow, and Material 3 AMOLED styling.
* **Responsibilities:**
  - Implement MVI architecture across all feature modules (`DownloaderViewModel`, `ProfileViewModel`, `GalleryViewModel`, `WatchlistViewModel`).
  - Build AMOLED Dark Design System tokens (`#0B0E14` pure black, `#161B26` surface, Instagram sunset gradient).
  - Implement layout-matched Shimmer Skeleton screens to eliminate jarring full-screen spinners.
  - Develop `:feature:downloader`, `:feature:profile`, `:feature:watchlist`, and `:feature:gallery`.
* **Deliverables:** Presentation code in all feature modules, design system tokens, MVI ViewModels with `StateFlow`/`SharedFlow`.
* **Toolsets:** Android Studio, Jetpack Compose 2.3.20, Material 3, Coil Compose 2.7.0, Media3 ExoPlayer 1.5.1.
* **KPIs:** UI frame drop rate < 0.5%; 60/120fps smooth scrolling; cold-to-render transition latency < 200ms.

#### 4. DevOps Engineer (Build Caching, Multi-Module Optimization, CI/CD, Proguard/R8)
* **Mandate:** Owns build infrastructure, Gradle multi-module optimization, dependency management, Proguard/R8 shrinking rules, and automated CI/CD release pipelines.
* **Responsibilities:**
  - Optimize multi-module Gradle configuration: centralize dependencies in `gradle/libs.versions.toml`.
  - Configure Gradle Configuration Cache, Parallel Execution, and Build Cache.
  - Harden `proguard-rules.pro` for release minification (Room, Hilt, Kotlinx Serialization, ExoPlayer reflection protection).
  - Build GitHub Actions CI/CD workflows and Fastlane release deployment.
* **Deliverables:** Configured `settings.gradle.kts`, root `build.gradle.kts`, module build scripts, hardened `proguard-rules.pro`, GitHub Actions CI.
* **Toolsets:** Gradle 9.1+, AGP 9.0.1, GitHub Actions, Fastlane, Proguard/R8 Shrinker, Bundletool.
* **KPIs:** Clean build duration < 90s; incremental build < 12s; release binary size < 15 MB; CI lead time < 7m.

#### 5. QA Engineer (Test Automation, JUnit5/Robolectric, Maestro E2E, Chaos Testing)
* **Mandate:** Formulates and executes the comprehensive test automation strategy across the test pyramid (Unit, Integration, Storage, Chaos, and E2E UI).
* **Responsibilities:**
  - Build and maintain the test pyramid: Unit tests (JUnit 5 + MockWebServer), Integration & Storage tests (Robolectric across API 26-36), Network Chaos Suite (fault-injecting interceptors), and E2E UI tests (Maestro / Espresso).
  - Verify Scoped Storage MediaStore file insertion and atomic `IS_PENDING` flag handling.
  - Validate WorkManager background download auto-resumption after process-kill.
* **Deliverables:** Automated test suites in `testing/` and module `src/test`, Maestro test flows, network chaos verification reports.
* **Toolsets:** JUnit 5, Robolectric 4.14+, Turbine, Kotest, Maestro CLI, Android Emulator Matrix (API 26-36).
* **KPIs:** Automated test pass rate 100%; code coverage ≥ 85% across core/domain logic; defect detection efficiency ≥ 95%.

#### 6. Architecture Designer (Principal Architect: Module Boundaries, Dagger-Hilt DI, ADRs)
* **Mandate:** Chief technical authority governing system topology, clean architecture separation, module boundaries, Dagger-Hilt DI graphs, and Architectural Decision Records (ADRs).
* **Responsibilities:**
  - Define and enforce multi-module boundaries: strict unidirectional DAG (`:app` → `:feature:*` → `:core:*`), zero horizontal dependencies between feature modules.
  - Architect Dagger-Hilt DI graph: root `SingletonComponent` in `:app`, feature scopes in `ViewModelScoped`, domain interface contracts.
  - Enforce concurrency safety: Kotlin Coroutines structured concurrency, explicit dispatcher injection (`@IoDispatcher`).
  - Author and maintain formal ADRs (ADR-001 through ADR-004).
* **Deliverables:** Enterprise Architecture Specification, Dagger-Hilt module binding definitions, formal ADRs, code review standards.
* **Toolsets:** PlantUML / Mermaid, Android Studio Architecture Linter, Detekt, ArchUnit, LeakCanary.
* **KPIs:** Exactly 0 circular dependencies; exactly 0 architecture layer violations; 100% data sources behind domain interfaces.

#### 7. Chief Executive Officer (CEO: Executive Oversight, Legal Compliance, Monetization, Strategy)
* **Mandate:** Directs executive vision, legal and regulatory compliance, product monetization, market positioning, and corporate governance.
* **Responsibilities:**
  - Product strategy: position InstaSaver Pro as the premier high-speed, privacy-first, zero-watermark media utility.
  - Oversee legal compliance: Fair-Use doctrine review under 17 U.S.C. § 107 and EU Directive 2019/790; maintain DMCA takedown procedure.
  - Ensure adherence to Google Play Developer Policies (User Data disclosure, Scoped Storage compliance, no trademark violations).
  - Define monetization strategy (Freemium vs Pro tier) and authorize executive release gates.
* **Deliverables:** Commercialization Blueprint, Legal Compliance Whitepaper, Play Store Brand Strategy, Executive Release Sign-Off.
* **Toolsets:** Financial Modeling, Legal Compliance Frameworks, App Analytics, Google Play Console.
* **KPIs:** Exactly 0 copyright strikes or Play Store suspensions; Customer LTV/CAC > 3.5x; User retention (D30) ≥ 45%.

#### 8. Project Manager (PM: Agile Sprints, Scrum Ceremonies, Risk Matrix, Gating)
* **Mandate:** Drives operational excellence, sprint execution, cross-functional alignment, timeline management, risk mitigation, and milestone gating.
* **Responsibilities:**
  - Facilitate Scrum ceremonies: Sprint Planning, Daily Standups, Backlog Grooming, Sprint Review/Demo, and Retrospectives.
  - Track sprint velocity, burndown charts, and critical path dependencies.
  - Maintain the Comprehensive Risk Management Matrix (RAID log).
  - Enforce Definition of Ready (DoR) and Definition of Done (DoD) at every sprint boundary; coordinate Quality Gates 1 to 4.
* **Deliverables:** Master Agile Project Plan (Sprints 1-4), Enterprise Risk Matrix (RAID Log), Sprint Burndown Reports, Milestone Gate Reviews.
* **Toolsets:** Jira Software / Linear, Confluence, Slack, Miro, Google Sheets.
* **KPIs:** Sprint velocity variance ≤ 5%; on-time milestone delivery rate 100%; critical blocker SLA < 4 business hours.

---

### 1.2 RACI Matrix Across Modules & Lifecycle Phases

**Legend:** **R** = Responsible, **A** = Accountable, **C** = Consulted, **I** = Informed

| Module / Scope | BA | Backend | Frontend | DevOps | QA | Architect | CEO | PM |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `:core:model` | C | R | R | I | C | **A** | I | I |
| `:core:network` | I | R | C | C | C | **A** | I | I |
| `:core:database` | C | R | C | I | C | **A** | I | I |
| `:core:storage` | C | C | R | I | C | **A** | I | I |
| `:core:extractor` | C | R | I | I | C | **A** | C | I |
| `:feature:downloader` | C | C | R | I | C | C | I | **A** |
| `:feature:profile` | C | C | R | I | C | C | I | **A** |
| `:feature:gallery` | C | I | R | I | C | C | I | **A** |
| `:feature:watchlist` | C | C | R | I | C | C | I | **A** |
| `:app` (DI Assembly) | I | C | R | R | C | **A** | I | I |
| **Phase 0: Discovery & Architecture** | R | C | C | C | C | **A** | C | R |
| **Sprint 1: Multi-Module & DI Setup** | I | R | R | R | C | **A** | I | R |
| **Sprint 2: SRE Scraper & WorkManager** | C | R | C | C | R | **A** | I | R |
| **Sprint 3: MVI Compose UI/UX** | C | C | R | I | R | C | C | **A** |
| **Sprint 4: Hardening, E2E QA & Release**| C | C | C | R | R | C | **A** | R |

---

## 2. Architecture & Dependency Topology

### 2.1 Multi-Module Directed Acyclic Graph (DAG)

```
                            +--------------------+
                            |        :app        |
                            +---------+----------+
                                      |
         +----------------------------+----------------------------+
         |                            |                            |
         v                            v                            v
+------------------+         +------------------+         +------------------+
| :feature:download|         | :feature:profile |         | :feature:gallery |
+--------+---------+         +--------+---------+         +--------+---------+
         |                            |                            |
         |         +------------------+----------------------------+
         |         |
         |         |         +--------------------+
         |         |         | :feature:watchlist |
         |         |         +---------+----------+
         |         |                   |
         +---------+---------+---------+
                             |
         +-------------------+-------------------+
         |                                       |
         v                                       v
+------------------+                   +------------------+
| :core:extractor  |                   |  :core:storage   |
+--------+---------+                   +--------+---------+
         |                                      |
         +-------------------+------------------+
                             |
         +-------------------+-------------------+
         |                                       |
         v                                       v
+------------------+                   +------------------+
|  :core:network   |                   |  :core:database  |
+--------+---------+                   +--------+---------+
         |                                       |
         +-------------------+-------------------+
                             |
                             v
                    +------------------+
                    |   :core:model    | (Pure Domain Entities - Leaf Node)
                    +------------------+
```

### 2.2 Dagger-Hilt DI Hierarchy & Providers
- **`@HiltAndroidApp` Application (`InstaSaverApplication.kt`)**: Roots the dependency injection tree in `:app`.
- **`NetworkModule` (`:core:network`)**: `@Provides @Singleton fun provideOkHttpClient(): OkHttpClient` with custom TLS socket factory, cookie synchronization, and domain-restricted headers.
- **`DatabaseModule` (`:core:database`)**: `@Provides @Singleton fun provideDatabase(@ApplicationContext context: Context): InstaSaverDatabase`, providing `MediaCacheDao`, `WatchlistDao`, and `DownloadHistoryDao`.
- **`StorageModule` (`:core:storage`)**: `@Provides @Singleton fun provideMediaStorageManager(@ApplicationContext context: Context, okHttpClient: OkHttpClient): MediaStorageManager`.
- **`ExtractorModule` (`:core:extractor`)**: `@Provides @Singleton fun provideInstagramExtractor(client: OkHttpClient): InstagramExtractor`, `@Provides @Singleton fun provideProfileScraper(client: OkHttpClient): ProfileScraper`.
- **`WorkManagerModule` (`:feature:downloader`)**: Injects `HiltWorkerFactory` into WorkManager configuration.
- **`ViewModelScoped` (`:feature:*`)**: `@HiltViewModel` annotations on `DownloaderViewModel`, `ProfileViewModel`, `GalleryViewModel`, and `WatchlistViewModel`.

---

## 3. Feature Inventory

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Canonical Shortcode Extraction | URL parsing across `/reel/`, `/reels/`, `/p/`, `/tv/`, `/share/` | M3 | Survey / Extractor |
| 2 | Tier 1 Embed Page Scraping | Scrapes video/image URLs from embed page | M3 | Survey / Extractor |
| 3 | Tier 2 OpenGraph Metadata Scraping | Scrapes `og:video`, `og:image`, `og:description` | M3 | Survey / Extractor |
| 4 | Tier 3 Script Tag JSON Extraction | Parses raw HTML script tags with video/display URL regex | M3 | Survey / Extractor |
| 5 | Tier 4 Private Web API Extraction | Fetches `/api/v1/media/{id}/info/` with session headers | M3 | Survey / Extractor |
| 6 | Multi-Slide Carousel Parsing | Parses `carousel_media` arrays with individual download URLs | M3 | Survey / Extractor |
| 7 | Ghost Profile Explorer | Public profile inspection (bio, avatar, metrics, posts) without login | M3 | Survey / Extractor |
| 8 | Meta Stats Regex Parser | Parses follower, following, post counts from meta tags | M3 | Survey / Extractor |
| 9 | Public Mirror Fallback Engine | Fallback scraping via Picuki/Imginn | M3 | Survey / Extractor |
| 10 | Concurrency Guard & Rate Limiter | Max 2 concurrent downloads with 350ms batch pacing | M4 | Survey / Downloader |
| 11 | Atomic MediaStore Ingestion | Scoped storage write with `IS_PENDING = 1` atomicity | M2 | Survey / Storage |
| 12 | Native Lossless Audio Demuxer | AAC audio extraction from MP4 to `.m4a` via MediaMuxer | M2 | Survey / Storage |
| 13 | Disk Space Preflight Check | Evaluates free storage via `StatFs` against 20MB threshold | M2 | Survey / Storage |
| 14 | MediaStore Saved Media Scanner | Scans `MediaStore.Video` and `Images` for `Insta_%` media | M4 | Survey / Gallery |
| 15 | Pinned Creators Room Database | Room DB persistence for pinned creators and media cache | M2 | Survey / Database |
| 16 | Smart Cookie Jar & CSRF Injector | Synchronizes cookies with Android CookieManager | M2 | Survey / Network |
| 17 | Domain-Restricted Header Gate | Strips internal IG headers on CDN URLs to prevent 403s | M2 | Survey / Network |
| 18 | Anonymous Guest Cookie Provisioning | Generates synthetic guest session cookies | M2 | Survey / Network |
| 19 | UI State Permutation Suppression | Eliminates contradictory UI states in Downloader | M4 | Survey / Downloader |
| 20 | Hardware-Accelerated Video Player | AndroidX Media3 ExoPlayer integration in Compose | M4 | Survey / Gallery |

---

## 4. Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| **M1** | Build & Tooling Setup | Configure `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, register all 10 modules, configure Dagger-Hilt & KSP plugins | None | DONE |
| **M2** | Core Infrastructure & Data Layer | Implement `:core:model`, `:core:network`, `:core:database` (Room), `:core:storage` (MediaStore & audio demuxer) with Dagger-Hilt modules | M1 | DONE |
| **M3** | Ingestion & Extraction Engine | Implement `:core:extractor` (4-tier media extraction, profile scraper, mirror fallback, DI bindings) | M2 | DONE |
| **M4** | Feature Modules Migration & Presentation | Implement `:feature:downloader` (WorkManager + UI), `:feature:profile` (UI), `:feature:gallery` (ExoPlayer + UI), `:feature:watchlist` (UI) with MVI ViewModels | M3 | DONE |
| **M5** | Application Wiring, Navigation & DI Integration | Configure `:app` (`InstaSaverApplication`, `@HiltAndroidApp`, `MainActivity`, bottom navigation routing, manifest) | M4 | DONE |
| **M6** | Verification & Build Validation | Run `./gradlew.bat assembleDebug` / `testDebugUnitTest`, run Python master test suite (103/103 pass), verify 0 circular dependencies | M5 | DONE |

---

## 5. Interface Contracts

### 5.1 Domain Models (`:core:model`)
```kotlin
package com.instasaver.core.model

import kotlinx.serialization.Serializable

enum class MediaType { VIDEO, IMAGE, CAROUSEL, AUDIO }

@Serializable
data class MediaItem(
    val shortcode: String,
    val mediaType: MediaType,
    val videoUrl: String? = null,
    val thumbnailUrl: String,
    val caption: String = "",
    val authorUsername: String = "",
    val carouselSlides: List<CarouselSlide> = emptyList(),
    val audioTrack: AudioTrack? = null
)

@Serializable
data class CarouselSlide(
    val index: Int,
    val mediaType: MediaType,
    val mediaUrl: String,
    val thumbnailUrl: String
)

@Serializable
data class AudioTrack(
    val audioUrl: String,
    val title: String,
    val artist: String
)

@Serializable
data class ProfileData(
    val username: String,
    val fullName: String,
    val bio: String,
    val profilePicUrl: String,
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val followersCount: String = "0",
    val followingCount: String = "0",
    val postsCount: String = "0",
    val reels: List<MediaItem> = emptyList(),
    val posts: List<MediaItem> = emptyList()
)

data class DownloadProgress(
    val shortcode: String,
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val savedUri: String? = null
)
```

### 5.2 Network & Extractor Contracts (`:core:network` ↔ `:core:extractor`)
```kotlin
package com.instasaver.core.network

import okhttp3.OkHttpClient

interface NetworkClientProvider {
    fun getHttpClient(): OkHttpClient
}
```

```kotlin
package com.instasaver.core.extractor

import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.ProfileData

interface ExtractorRepository {
    suspend fun extractMedia(url: String): Result<MediaItem>
    fun extractShortcode(url: String): String?
}

interface ProfileRepository {
    suspend fun getProfile(username: String): Result<ProfileData>
}
```

### 5.3 Storage & Database Contracts (`:core:storage`, `:core:database` ↔ `:feature:*`)
```kotlin
package com.instasaver.core.storage

import android.net.Uri
import com.instasaver.core.model.MediaItem
import java.io.File

interface MediaStorageService {
    suspend fun saveMediaToGallery(
        mediaItem: MediaItem,
        targetUrl: String? = null,
        onProgress: (progressPercent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): Result<Uri>
    
    fun hasSufficientSpace(): Boolean
    fun extractAudioTrackNative(videoCacheFile: File, outputFile: File): Boolean
}
```

```kotlin
package com.instasaver.core.database

import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.ProfileData
import kotlinx.coroutines.flow.Flow

interface DatabaseRepository {
    fun observePinnedCreators(): Flow<List<String>>
    suspend fun pinCreator(username: String)
    suspend fun unpinCreator(username: String)
    suspend fun isCreatorPinned(username: String): Boolean
    
    suspend fun getCachedMedia(shortcode: String): MediaItem?
    suspend fun cacheMedia(mediaItem: MediaItem)
    suspend fun getCachedProfile(username: String): ProfileData?
    suspend fun cacheProfile(profile: ProfileData)
}
```

---

## 6. Code Layout

```
c:\Users\Dell\Downloads\new kipi devope\
├── build.gradle.kts                   # Root build script (plugins alias declarations)
├── settings.gradle.kts                # Project include configuration (all 10 modules)
├── gradle/
│   ├── libs.versions.toml             # Centralized dependency catalog
│   └── wrapper/
│       └── gradle-wrapper.properties  # Gradle 9.1.0 distribution
├── app/                               # Application module (:app)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml        # Main launcher & Application declaration
│       └── kotlin/com/instasaver/app/
│           ├── InstaSaverApplication.kt # @HiltAndroidApp
│           ├── MainActivity.kt          # @AndroidEntryPoint, BottomNav, Navigation
│           ├── di/                      # Root Dagger-Hilt assembly modules
│           └── ui/theme/                # AMOLED Dark Theme tokens & Typography
├── core/
│   ├── model/                         # Pure domain entities (:core:model)
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/instasaver/core/model/
│   ├── network/                       # OkHttp, TLS, CookieJar, Session (:core:network)
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/instasaver/core/network/
│   ├── database/                      # Room DB, DAOs, Entities (:core:database)
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/instasaver/core/database/
│   ├── storage/                       # Scoped Storage MediaStore adapter (:core:storage)
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/instasaver/core/storage/
│   └── extractor/                     # 4-tier scraper & profile parser (:core:extractor)
│       ├── build.gradle.kts
│       └── src/main/kotlin/com/instasaver/core/extractor/
└── feature/
    ├── downloader/                    # Downloader screen & WorkManager (:feature:downloader)
    │   ├── build.gradle.kts
    │   └── src/main/kotlin/com/instasaver/feature/downloader/
    ├── profile/                       # Ghost Profile Explorer screen (:feature:profile)
    │   ├── build.gradle.kts
    │   └── src/main/kotlin/com/instasaver/feature/profile/
    ├── gallery/                       # MediaStore browser & ExoPlayer (:feature:gallery)
    │   ├── build.gradle.kts
    │   └── src/main/kotlin/com/instasaver/feature/gallery/
    └── watchlist/                     # Pinned creators screen (:feature:watchlist)
        ├── build.gradle.kts
        └── src/main/kotlin/com/instasaver/feature/watchlist/
```
