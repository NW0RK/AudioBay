# AGENTS.md — Coding Agent Guidelines & Project Instructions

> **Primary Audience:** AI Coding Agents (such as OpenAI Codex), Autonomous Contributors, and Human Engineers.  
> **Project:** AudioBookBay Android Client (`com.audiobookbay.app`)  
> **Repository:** `AudioBookBayAndroid`  
> **Status:** Preparation Phase Complete — Ready for Feature Implementation  

---

## 1. Project Purpose & Vision

AudioBookBay Android is a native, modern Android application engineered to allow users to discover, search, download, organize, and listen to audiobooks directly on their Android devices.

Key highlights:
- **Built-in BitTorrent Client:** Powered by an abstracted torrent engine (using `libtorrent4j`), enabling native direct-to-device audiobook downloads without requiring third-party torrent apps.
- **Modern Audiobook Player:** Built on AndroidX Media3 / ExoPlayer with background playback, sleep timer, playback speed control, lock screen media controls, and chapter navigation.
- **Discoverability & Flexibility:** Scrapes and parses AudioBookBay.lu public catalog with a built-in legal public-domain fallback (LibriVox REST API & Internet Archive).
- **Clean Android Architecture:** Designed according to Clean Architecture + MVVM principles, Jetpack Compose, Material 3, Hilt Dependency Injection, Room persistence, and Kotlin Flow.

---

## 2. Technology Stack & Key Coordinates

All dependencies are centrally configured in [`gradle/libs.versions.toml`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/gradle/libs.versions.toml).

| Layer / Concern | Technology | Version | Notes |
|---|---|---|---|
| **Language** | Kotlin | `2.1.0` | With Kotlin Compose Compiler plugin |
| **Android Build** | AGP | `8.8.2` | Gradle `8.11.1`, JVM 17 |
| **SDK Levels** | Min: `26`, Target: `35`, Compile: `35` | — | Note: `libtorrent4j` requires API ≥ 28 for native binaries |
| **UI Framework** | Jetpack Compose + Material 3 | BOM `2025.12.00` | Dynamic color, edge-to-edge support |
| **Navigation** | Navigation Compose | `2.8.9` | Type-safe Kotlin Serialization routes |
| **Dependency Injection** | Dagger Hilt | `2.57.1` | Using KSP (`2.1.0-1.0.29`), `hilt-navigation-compose` |
| **Local Persistence** | AndroidX Room | `2.7.1` | SQLite database via KSP compiler |
| **Preferences** | AndroidX DataStore | `1.1.4` | Preferences DataStore (async, reactive) |
| **Media Playback** | AndroidX Media3 (ExoPlayer) | `1.5.1` | `media3-session`, `media3-exoplayer`, `media3-ui` |
| **Torrent Engine** | `libtorrent4j` | `2.1.0-39` | MIT licensed rasterbar wrapper + native ABIs |
| **Networking & HTTP** | OkHttp + Retrofit | `4.12.0` / `2.11.0` | Kotlinx Serialization converter |
| **HTML Parsing** | Jsoup | `1.18.3` | Clean DOM scraping with resilient selector fallbacks |
| **Background Tasks** | Android Services + WorkManager | `2.10.0` | `FOREGROUND_SERVICE_DATA_SYNC` & `MEDIA_PLAYBACK` |
| **Logging** | Timber | `5.0.1` | Debug-only logging tree planted in `AudioBookBayApp` |
| **Testing** | JUnit 4, MockK, Turbine, Coroutines Test | — | Unit and Flow testing stack |

---

## 3. Directory & Package Structure

```
AudioBookBayAndroid/
├── app/
│   ├── build.gradle.kts                   # App module build and dependency declarations
│   ├── proguard-rules.pro                 # Proguard/R8 rules (libtorrent, Retrofit, Room)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # Permissions, Services, and MainActivity
│       │   ├── java/com/audiobookbay/app/
│       │   │   ├── AudioBookBayApp.kt     # Application class (@HiltAndroidApp, Timber)
│       │   │   ├── domain/                # Pure business logic, models, contracts
│       │   │   │   ├── model/             # Domain entities (Audiobook, Chapter, DownloadItem...)
│       │   │   │   └── repository/        # Architectural contracts & repository interfaces
│       │   │   ├── data/                  # Data access layer
│       │   │   │   ├── local/             # Room AppDatabase, DAOs, and database entities
│       │   │   │   ├── remote/            # HTML Scraper, LibriVox REST API, RemoteDataSource
│       │   │   │   └── repository/        # Concrete repository implementations
│       │   │   ├── di/                    # Hilt dependency injection modules
│       │   │   ├── torrent/               # Torrent engine implementation & background service
│       │   │   ├── player/                # Media3 ExoPlayer wrapper & MediaSessionService
│       │   │   └── ui/                    # Presentation layer
│       │   │       ├── MainActivity.kt    # Root single activity (edge-to-edge)
│       │   │       ├── navigation/        # Type-safe NavHost and NavRoute definitions
│       │   │       ├── theme/             # Material3 ColorScheme and Theme
│       │   │       └── screens/           # 8 Scaffolded screens (Home, Search, Details...)
│       │   └── res/                       # Strings, theme resources, and drawables
│       └── test/                          # Unit tests (parsers, domain mappers)
├── docs/                                  # Architectural & implementation documents
│   ├── ARCHITECTURE.md                    # Deep dive into architecture & contracts
│   ├── RESEARCH.md                        # Verified research findings & feasibility
│   ├── IMPLEMENTATION_PLAN.md             # 8-Phase implementation roadmap
│   ├── CODEX_HANDOFF.md                   # Explicit step-by-step instructions for Codex
│   └── TASKS.md                           # Actionable task checklist
├── gradle/
│   ├── libs.versions.toml                 # Centralized dependency catalog
│   └── wrapper/gradle-wrapper.properties  # Gradle 8.11.1 distribution config
├── build.gradle.kts                       # Root project build file
├── settings.gradle.kts                    # Settings, pluginManagement, repositories
├── gradle.properties                      # JVM args, AndroidX, and parallel build settings
├── gradlew / gradlew.bat                  # Gradle wrapper executable scripts
└── AGENTS.md                              # This file
```

---

## 4. Architectural Conventions & Rules

1. **Clean Architecture Separation:**
   - **Domain Layer (`domain/`):** Must NOT import Android framework classes (except standard Android utilities where strictly necessary, such as `File`). Contains only domain models, sealed interfaces (`Resource`, `UiState`), and repository contracts.
   - **Data Layer (`data/`):** Implements domain repository interfaces. Interacts with Room, DataStore, OkHttp, Retrofit, and Jsoup.
   - **Torrent Layer (`torrent/`):** Must implement `TorrentDownloadManager`. Keeps `libtorrent4j` internals encapsulated so the torrent engine can be swapped if necessary.
   - **Player Layer (`player/`):** Must implement `AudiobookPlayer`. Keeps AndroidX Media3 / ExoPlayer internals isolated from Compose screens.
   - **UI Layer (`ui/`):** Stateless Compose screens receiving UI state models and event lambdas. Communicates with ViewModels (injected with Hilt).

2. **Reactive Flow Patterns:**
   - Data flows upwards via Kotlin `Flow` / `StateFlow`.
   - Never expose `MutableStateFlow` from ViewModels or repositories; always expose read-only `StateFlow` or `Flow`.
   - In Compose screens, collect state using `collectAsStateWithLifecycle()`.

3. **Error Handling & State Representation:**
   - Use `Resource<T>` in repositories (`Resource.Success`, `Resource.Error`, `Resource.Loading`).
   - Use `UiState<T>` in presentation layers (`UiState.Idle`, `UiState.Loading`, `UiState.Success`, `UiState.Error`).
   - Never crash the UI on remote parsing errors or network disconnections; gracefully emit `Resource.Error`.

---

## 5. Coding Standards

- **Kotlin Idioms:** Use data classes, sealed interfaces, extension functions, and when expressions.
- **Coroutines:** Always specify dispatchers (`Dispatchers.IO` for disk/network, `Dispatchers.Default` for parsing heavy data). Inject dispatchers via `@IoDispatcher` and `@MainDispatcher` qualifiers in `AppModule.kt`.
- **String Resources:** Put user-facing strings in `res/values/strings.xml` to support localization.
- **Compose Best Practices:**
  - Keep composable functions focused and modular.
  - Pass lambdas (`onNavigateToDetail`, `onPlayClick`) rather than passing NavControllers down into child composables.
  - Support Material3 dynamic color and dark mode seamlessly.
- **Logging:** Use `Timber.d()`, `Timber.i()`, `Timber.w()`, and `Timber.e()`. Do NOT use raw `android.util.Log` or `println()`.

---

## 6. Build, Test, and Verification Commands

```bash
# Check Gradle tasks and project configuration
./gradlew tasks

# Compile debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lintDebug

# Clean build cache
./gradlew clean
```

*(Note for Windows CMD/PowerShell: use `gradlew.bat` or `.\gradlew`)*

---

## 7. Security, Privacy & Legal Constraints

1. **Copyright & Content Safety:**
   - AudioBookBay indexes third-party torrents. The application must support lawful downloading of openly licensed, creative commons, and public domain material.
   - The app includes built-in integration with **LibriVox** and the **Internet Archive** to provide verified, copyright-free audiobooks.
   - Do NOT implement bypasses for authentication, Cloudflare bot protection, CAPTCHAs, or paywalls.
2. **Storage Boundaries (Scoped Storage):**
   - Modern Android (API 30+) restricts arbitrary filesystem writes.
   - Store downloaded audio files in **app-specific external storage** (`context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`) or use standard Storage Access Framework (SAF).
   - App-specific storage does NOT require runtime `WRITE_EXTERNAL_STORAGE` permissions on Android 10+.
3. **Android 14+ Foreground Service Policy:**
   - Long-running downloads MUST use `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
   - Audio playback MUST use `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
   - Android 15 introduces a 6-hour cumulative limit on background `dataSync` services; handle `Service.onTimeout()` gracefully.

---

## 8. Definition of Done (DoD) for Future Codex Tasks

Before considering any implementation task complete:
1. The code compiles without syntax or type errors.
2. Unit tests are added or updated to cover new logic.
3. Relevant architectural contracts in `domain/repository/` are followed.
4. Dependency injection is wired via Hilt.
5. Code follows Kotlin and Android best practices.
6. The task in [`docs/TASKS.md`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/docs/TASKS.md) is marked as checked `[x]`.


## User-approved product revision — 2026-11-01 (takes precedence above)

This app is now a downloader only. Do not implement or restore a native player,
Media3 playback service, listening library, bookmarks, sleep timer or mini-player.
The user listens in a separate app. Keep discovery, search, details, Downloads and
Settings. Final shared storage defaults to Android/AudioBooks and is selected through
SAF with a persistent grant; arrange audio as Author/Book Title/tracks, preserving
disc folders and Unicode. Missing author/title must be requested before downloading.
libtorrent uses private app-specific staging; publish verified content through SAF,
checkpoint and verify copies, then delete staging. Completed shared files survive
queue removal and uninstall. Never silently use an alternate shared root or bypass
Android folder access restrictions. Legacy Room tables are retained for upgrades.
