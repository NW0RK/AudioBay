# Codex Continuation & Handoff Guide

> **To OpenAI Codex:** Welcome! Google Antigravity has completed the comprehensive research, architecture design, dependency configuration, directory scaffolding, domain modeling, and initial implementation. This document tells you **exactly what is ready, where to start, and what to build next**.

---

## 1. What Has Already Been Implemented

1. **Gradle Build Toolchain & Dependencies:**
   - Kotlin `2.1.0` with built-in Compose compiler plugin.
   - Android Gradle Plugin `8.8.2` targeting Android API 35 (`minSdk = 26`).
   - Centralized Version Catalog in [`gradle/libs.versions.toml`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/gradle/libs.versions.toml).
   - Gradle wrapper scripts (`gradlew`, `gradlew.bat`, `gradle-wrapper.properties` configured for Gradle `8.11.1`).
   - Proguard rules for `libtorrent4j`, Retrofit, OkHttp, Room, and Kotlin Serialization in [`app/proguard-rules.pro`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/proguard-rules.pro).

2. **Application Architecture & Contracts:**
   - Strict Clean Architecture with pure domain layer in [`app/src/main/java/com/audiobookbay/app/domain/`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/domain/).
   - 6 Core Domain Contracts defined:
     - `AudiobookRepository`
     - `AudiobookSearchRepository`
     - `TorrentDownloadManager`
     - `AudiobookPlayer`
     - `LibraryRepository`
     - `PlaybackProgressRepository`
   - Complete domain models: `Audiobook`, `AudiobookDetail`, `Chapter`, `TorrentInfo`, `DownloadItem`, `PlaybackState`, `Bookmark`, `Category`, `Resource`, `UiState`.

3. **Data Layer & Persistence:**
   - **Room Database (`AppDatabase`):** Entities (`AudiobookEntity`, `ChapterEntity`, `PlaybackProgressEntity`, `DownloadEntity`, `BookmarkEntity`) and DAOs (`AudiobookDao`, `PlaybackProgressDao`, `DownloadDao`, `BookmarkDao`).
   - **DataStore:** Configured for user preferences and search query history.
   - **HTML Scraper (`AudioBookBayParser`):** Jsoup parser extracting book listings, cover thumbnails, descriptions, and the 40-character hex info hash from tables.
   - **Legal Fallback API (`LibriVoxApi`):** Retrofit contract for public-domain audiobooks.
   - **Repository Implementations:** `AudiobookRepositoryImpl`, `AudiobookSearchRepositoryImpl`, `LibraryRepositoryImpl`, `PlaybackProgressRepositoryImpl`.

4. **Service Infrastructure (Android 14+ Compliant):**
   - `TorrentDownloadService` registered with `foregroundServiceType="dataSync"`.
   - `AudioPlaybackService` registered with `foregroundServiceType="mediaPlayback"`.
   - Full permission declarations in [`AndroidManifest.xml`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/AndroidManifest.xml).

5. **Dependency Injection (Hilt):**
   - Modules in `com.audiobookbay.app.di`: `AppModule`, `DatabaseModule`, `NetworkModule`, `RepositoryModule`, `EngineModule`.

6. **UI & Navigation:**
   - `MainActivity` (Edge-to-edge, single activity).
   - `AudioBookBayTheme` (Material3 with dynamic color support).
   - `AudioBookBayNavHost` with type-safe `NavRoute` and bottom navigation bar.
   - 8 Scaffolded screens: `HomeScreen`, `SearchScreen`, `CategoriesScreen`, `AudiobookDetailScreen`, `DownloadsScreen`, `LibraryScreen`, `NowPlayingScreen`, `SettingsScreen`.

7. **Unit Tests:**
   - `AudioBookBayParserTest` (validates HTML parsing, infohash extraction, and magnet link assembly).
   - `DomainMappingTest` (validates Entity <-> Domain conversions).

---

## 2. What Remains Unfinished (Your Implementation Scope)

1. **`TorrentDownloadManagerImpl`:** Connect `org.libtorrent4j.SessionManager` to start torrent downloads, track piece progress, listen to alerts, and save fast-resume data.
2. **`AudioPlaybackService` & `AudiobookPlayerImpl`:** Wire ExoPlayer playlist controls and connect UI playback actions.
3. **UI ViewModels & Screen Implementations:**
   - Replace placeholder UI in `HomeScreen`, `SearchScreen`, `AudiobookDetailScreen`, `DownloadsScreen`, `LibraryScreen`, `NowPlayingScreen`.
   - Implement `HomeViewModel`, `SearchViewModel`, `DetailViewModel`, `DownloadsViewModel`, `PlayerViewModel`, `LibraryViewModel`.
4. **Persistent Mini-Player:** Render a floating bottom mini-player bar when an audiobook is playing while browsing.

---

## 3. Important Architecture Decisions You Must Follow

1. **Never import `libtorrent4j` in the UI layer:** All torrent operations must be mediated by `TorrentDownloadManager`.
2. **Never import ExoPlayer in the UI layer:** All playback operations must be mediated by `AudiobookPlayer`.
3. **File Storage Location:** Always write torrent files to `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`. Do NOT attempt to use arbitrary paths on `/sdcard/`, as Android Scoped Storage will deny POSIX file access.
4. **Throttled DB Writes:** In `PlaybackProgressRepository`, ensure playback position is saved at throttled intervals (every 10s) and on pause/stop, never on every frame tick.

---

## 4. Immediate Next Implementation Task (Start Here!)

### 👉 Task 1: Implement `HomeViewModel` and Connect `HomeScreen`
1. Create `app/src/main/java/com/audiobookbay/app/ui/screens/home/HomeViewModel.kt`:
   - Inject `AudiobookRepository`.
   - Expose `val uiState: StateFlow<UiState<List<Audiobook>>>`.
   - Call `audiobookRepository.getRecentAudiobooks(page = 1)` in `init`.
2. Update `HomeScreen.kt`:
   - Inject ViewModel using `hiltViewModel()`.
   - Render a `LazyVerticalGrid` (2 columns) displaying book cover images (`AsyncImage` from Coil), titles, and authors.
   - Handle `UiState.Loading` with a circular progress indicator, and `UiState.Error` with a retry button.
   - Wire `onNavigateToDetail` to click events.

---

## 5. Recommended Order of Development

1. **Phase 1 (Discovery):** `HomeViewModel` + `HomeScreen` (displays real book cards from AudioBookBay / LibriVox).
2. **Phase 2 (Search & Detail):** `SearchViewModel` + `DetailViewModel` (search queries and detail page).
3. **Phase 3 (BitTorrent Engine):** Wire `org.libtorrent4j.SessionManager` inside `TorrentDownloadManagerImpl`.
4. **Phase 4 (Downloads UI):** `DownloadsViewModel` + `DownloadsScreen` (progress bars, speed, pause/resume).
5. **Phase 5 (Player):** Connect `NowPlayingScreen` with `AudioPlaybackService` via `AudiobookPlayerImpl`.
6. **Phase 6 (Library):** `LibraryViewModel` + `LibraryScreen` (auto-resume, offline playback).

---

## 6. Files You Should Inspect First

1. [`app/src/main/java/com/audiobookbay/app/domain/repository/AudiobookRepository.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/domain/repository/AudiobookRepository.kt) — Repository contract.
2. [`app/src/main/java/com/audiobookbay/app/domain/repository/TorrentDownloadManager.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/domain/repository/TorrentDownloadManager.kt) — Torrent engine contract.
3. [`app/src/main/java/com/audiobookbay/app/torrent/TorrentDownloadManagerImpl.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/torrent/TorrentDownloadManagerImpl.kt) — Implementation skeleton with TODOs.
4. [`app/src/main/java/com/audiobookbay/app/data/remote/AudioBookBayParser.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/data/remote/AudioBookBayParser.kt) — HTML parsing and magnet link synthesis logic.
5. [`docs/TASKS.md`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/docs/TASKS.md) — The master checklist of development items.

---

## 7. Exact Commands to Run

```bash
# Verify compilation
./gradlew compileDebugKotlin

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```
*(On Windows cmd/powershell, run `gradlew.bat <task>` or `.\gradlew <task>`)*
