# Actionable Task Checklist — AudioBookBay Android

This checklist tracks preparation tasks completed by **Google Antigravity** and future implementation tasks to be executed by **OpenAI Codex**.

---

## Preparation Phase (Antigravity) — Completed Tasks

- [x] **Research & Feasibility:**
  - [x] Investigate AudioBookBay.lu public structure, API absence, and anti-bot constraints.
  - [x] Research LibriVox & Internet Archive APIs as clean, legal public-domain fallback.
  - [x] Evaluate Android BitTorrent libraries (`libtorrent4j` vs alternatives) and identify maven coordinates `org.libtorrent4j:libtorrent4j:2.1.0-39`.
  - [x] Research Android 14+ foreground service requirements (`dataSync` and `mediaPlayback` types) and Android 15 6-hour background limit.
  - [x] Research AndroidX Media3 / ExoPlayer audiobook capabilities (M4B chapter parsing, speed adjustment, sleep timer, throttled Room writes).
- [x] **Project Scaffolding & Configuration:**
  - [x] Create root `settings.gradle.kts` and `build.gradle.kts`.
  - [x] Establish centralized version catalog in `gradle/libs.versions.toml` (AGP 8.8.2, Kotlin 2.1.0, Compose BOM 2025.12.00, Hilt 2.57.1, Room 2.7.1, Media3 1.5.1, libtorrent4j 2.1.0-39).
  - [x] Create `gradle.properties` and `gradle/wrapper/gradle-wrapper.properties`.
  - [x] Create `gradlew` and `gradlew.bat` executable scripts.
  - [x] Add comprehensive `.gitignore`.
  - [x] Configure `app/build.gradle.kts` with KSP, Hilt, Room, Compose, Media3, libtorrent4j, Retrofit, and Jsoup.
  - [x] Configure ProGuard / R8 rules in `app/proguard-rules.pro`.
  - [x] Configure `AndroidManifest.xml` with permissions, `MainActivity`, `TorrentDownloadService`, and `AudioPlaybackService`.
  - [x] Add theme and color definitions in `ui/theme/Theme.kt` and XML resources (`strings.xml`, `themes.xml`).
- [x] **Architecture, Domain Models & Contracts:**
  - [x] Define domain models: `Audiobook`, `AudiobookDetail`, `Chapter`, `TorrentInfo`, `DownloadItem`, `PlaybackState`, `Bookmark`, `Category`, `Resource`, `UiState`.
  - [x] Define architectural contract: `AudiobookRepository`.
  - [x] Define architectural contract: `AudiobookSearchRepository`.
  - [x] Define architectural contract: `TorrentDownloadManager`.
  - [x] Define architectural contract: `AudiobookPlayer`.
  - [x] Define architectural contract: `LibraryRepository`.
  - [x] Define architectural contract: `PlaybackProgressRepository`.
- [x] **Data Layer Infrastructure:**
  - [x] Create Room Database `AppDatabase` with 5 entities (`AudiobookEntity`, `ChapterEntity`, `PlaybackProgressEntity`, `DownloadEntity`, `BookmarkEntity`).
  - [x] Implement Room DAOs: `AudiobookDao`, `PlaybackProgressDao`, `DownloadDao`, `BookmarkDao`.
  - [x] Configure Preferences DataStore for preferences and search history.
  - [x] Implement `AudioBookBayParser` with Jsoup for parsing listings and extracting info hashes.
  - [x] Define `LibriVoxApi` Retrofit interface.
  - [x] Implement `AudiobookRemoteDataSourceImpl` with automatic LibriVox fallback.
  - [x] Implement repository classes: `AudiobookRepositoryImpl`, `AudiobookSearchRepositoryImpl`, `LibraryRepositoryImpl`, `PlaybackProgressRepositoryImpl`.
- [x] **Service & DI Infrastructure:**
  - [x] Scaffold `TorrentDownloadService` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC` and notification channel.
  - [x] Scaffold `AudioPlaybackService` extending Media3 `MediaSessionService` with `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
  - [x] Create Hilt DI modules: `AppModule`, `DatabaseModule`, `NetworkModule`, `RepositoryModule`, `EngineModule`.
  - [x] Implement engine stubs: `TorrentDownloadManagerImpl`, `AudiobookPlayerImpl`.
- [x] **UI & Navigation:**
  - [x] Configure type-safe navigation routes (`NavRoute`) in `NavRoutes.kt`.
  - [x] Configure `AudioBookBayNavHost` with Material 3 Bottom Navigation bar.
  - [x] Scaffold 8 placeholder screens: `HomeScreen`, `SearchScreen`, `CategoriesScreen`, `AudiobookDetailScreen`, `DownloadsScreen`, `LibraryScreen`, `NowPlayingScreen`, `SettingsScreen`.
- [x] **Testing & Documentation:**
  - [x] Add parser unit tests in `AudioBookBayParserTest`.
  - [x] Add domain mapping unit tests in `DomainMappingTest`.
  - [x] Create `AGENTS.md`.
  - [x] Create `docs/ARCHITECTURE.md`.
  - [x] Create `docs/RESEARCH.md`.
  - [x] Create `docs/IMPLEMENTATION_PLAN.md`.
  - [x] Create `docs/CODEX_HANDOFF.md`.
  - [x] Create `docs/TASKS.md`.

---

## Implementation Phase (Codex) — Pending Tasks

### Phase 1: Audiobook Discovery & Remote Data
- [x] Create `HomeViewModel` injecting `AudiobookRepository`.
- [x] Replace placeholder in `HomeScreen` with 2-column `LazyVerticalGrid` of audiobook cards.
- [x] Add Coil `AsyncImage` for book cover loading with placeholder & error drawables.
- [x] Implement pull-to-refresh on `HomeScreen`.
- [x] Implement pagination (load more items as user scrolls to the bottom).
- [x] Add loading indicator, empty state, and recoverable error/retry controls.
- [ ] Add loading skeletons/shimmers as optional visual polish.

### Phase 2: Search, Details & Caching
- [x] Create `SearchViewModel` with 300ms debounce on search queries.
- [x] Update `SearchScreen` with active search bar, search history chips, and results grid.
- [x] Create `DetailViewModel` fetching full audiobook details by ID/slug.
- [ ] Build `AudiobookDetailScreen` UI: Large cover art, expandable description, author/narrator chips, torrent details (size, seeders, format).
- [x] Cache listings and complete details in Room without overwriting owned library records (`catalog_cache`, migration 1 to 2).
- [ ] Build `CategoriesScreen` with grid of genres (Sci-Fi, Fantasy, Mystery, etc.) leading to filtered category lists.

### Phase 3: BitTorrent Engine Implementation
- [x] In `TorrentDownloadManagerImpl`, instantiate and configure `org.libtorrent4j.SessionManager`.
- [x] Configure `SettingsPack` (DHT enable, peer limits, upload/download rate caps).
- [x] Handle actual `AddTorrentAlert` and resume-data alerts; poll native progress/completion every 2 seconds (the configured library has no `TorrentAddedAlert`).
- [x] Implement magnet link parsing and download queuing in `startDownload`.
- [x] Implement `.torrent` file parsing and queuing in `startDownloadFromFile`.
- [x] Select audio files, validate paths, and support explicit file priorities.
- [ ] Prioritize introductory tracks when appropriate.
- [x] Save fast-resume data to disk on pause/stop to allow instantaneous resume.

### Phase 4: Download Management & Persistence
- [x] Create `DownloadsViewModel` observing `DownloadDao.observeAllDownloads()`.
- [x] Replace Downloads placeholder with manual magnet/torrent import and real queue cards (progress, speed, peers, localized status, errors).
- [ ] Add ETA display and file-selection controls to Downloads UI.
- [x] Connect Pause, Resume, and Cancel buttons to `TorrentDownloadManager`.
- [x] Implement ongoing foreground notification updates in `TorrentDownloadService`.
- [ ] Implement Wi-Fi-only download constraint checking before starting transfers.
- [ ] Automatically transfer completed downloads to `LibraryRepository`.

### Phase 5: Audiobook Player (Media3 / ExoPlayer)
- [ ] In `AudiobookPlayerImpl`, implement `playAudiobook` creating Media3 `MediaItem` playlists from chapter files.
- [ ] Implement playback speed adjustment (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x, 3.0x).
- [ ] Implement sleep timer (countdown timer and end-of-chapter pause).
- [ ] Build `NowPlayingScreen` UI: Large cover, scrub bar with time labels, skip forward 30s, rewind 15s.
- [ ] Add chapter list bottom sheet modal in player screen.
- [ ] Verify media notification transport controls on lock screen.
- [ ] Implement audio focus handling for incoming phone calls and navigation voice prompts.

### Phase 6: Local Library & Progress Tracking
- [ ] Create `LibraryViewModel` observing `LibraryRepository.observeLibraryAudiobooks()`.
- [ ] Build `LibraryScreen` displaying offline audiobooks with listening progress indicators.
- [ ] Wire single-tap on a library book to auto-resume playback from the saved millisecond timestamp.
- [ ] Implement local directory scanner to discover already-downloaded audiobooks on device storage.
- [ ] Add bookmarking support (save and view bookmarks in player).
- [ ] Implement storage cleanup (delete downloaded files and remove from library).

### Phase 7: UI Polish, Dark Theme & Mini-Player
- [ ] Build floating persistent mini-player bar above the bottom navigation bar.
- [ ] Implement dynamic color palette integration (Material 3).
- [ ] Polish animations (cross-fade transitions between screens, smooth expand/collapse).
- [ ] Add TalkBack content descriptions and accessibility labels.

### Phase 8: Testing & Hardening
- [x] Write unit tests for `HomeViewModel`, `SearchViewModel`, and `DownloadsViewModel`.
- [ ] Write unit tests for `TorrentDownloadManagerImpl` alert parsing.
- [ ] Verify memory footprint during active torrent transfers.
- [ ] Verify Android 15 foreground service timeout handling (`Service.onTimeout`).
- [ ] Run ProGuard/R8 release build check.


## Verification log — 2026-11-01

- Restored missing official Gradle wrapper JAR and verified its SHA-256 against
  Gradle's published checksum. Fixed invalid settings DSL and missing launcher
  resource references; qualified the injected IO dispatcher.
- Local Java 17, Android API 35 SDK and Gradle cache are under ignored `.tools/`.
- Discovery/search/detail build, 25 unit tests, and lint passed before native-engine work.
- Installed debug build on connected Samsung SM-A525F (Android 14); real listings,
  covers, and detail navigation verified. Search exposed ignored website queries;
  unrelated results now trigger LibriVox fallback.
- On-device CatalogPersistenceTest passed with real Room close/reopen and an offline
  remote source. The native lawful transfer test now passes, including partial pause, manager recreation, resume data and byte-for-byte audio verification.
- Downloads UI now supports manual imports and queue controls. Library and playback UI remain incomplete. Do not treat the app as
  production-ready. Detailed handoff and exact commands are in CODEX_HANDOFF.md.

- Native milestone: 33 unit tests and 2 Android-device tests passed, plus lint.
  The final full check after service-lifecycle cleanup also passed: assembleDebug,
  testDebugUnitTest, lintDebug, and connectedDebugAndroidTest (.tools/final-checks.log).
  The local transfer uses synthetic audio and direct loopback peers; it does not
  verify public DHT/tracker reachability, Android 15 timeout, or OS process-kill recovery.
- Removed obsolete broad storage permissions; torrent imports use the document picker
  and downloaded files use app-specific external storage. Imported torrents are capped
  at 10 MiB. Removing queue entries currently retains their content files.


## Accepted product change — downloader-only (supersedes player/library phases)

- [x] Remove native player, playback service/permission/dependencies, listening library navigation and bindings.
- [x] Require editable author/title metadata before manual or catalog downloads.
- [x] Provide shared-folder setup in Settings and Downloads; persist SAF read/write access.
- [x] Default setup to Android/AudioBooks and explain explicitly chosen alternatives when restricted.
- [x] Publish to Author/Book Title while preserving track names and disc folders.
- [x] Sanitize unsafe names, preserve Unicode and keep existing books via numbered collisions.
- [x] Add durable verified-copy checkpoints, publication progress, retry and completion states.
- [x] Keep verified staging on failure; remove it only after successful publication.
- [x] Preserve existing data through additive Room v3 migration and compatible saved request defaults.
- [x] Verify folder layout, collisions, interrupted copying, revoked access, disk-full failures and restart/rename recovery with unit tests.
- [x] Verify real SAF publication and byte equality for a lawful native torrent on the connected Android14 phone.
- [x] Verify Room v2-to-v3 migration on device while retaining legacy tables and download rows.
- [x] User explicitly selected folder/file verification without opening Smart Audiobook Player.
- [x] Record final build/lint/unit/device results after the last low-storage recovery changes.

Final result: APK and instrumentation APK assemble successfully; lint has no errors;
48 unit tests and all 3 Android-device tests pass, including real Android/AudioBooks
SAF publication, byte equality, migration and private staging cleanup. The installed
app retains the configured folder grant. Test audio and test-author folders are removed.

Playback, bookmarks, mini-player and listening progress tasks above are retired and
must not be implemented. Existing shared audiobooks belong to the user and survive
queue removal/app uninstall. Future work should stay focused on downloading.


## Search bug fix — 2026-11-01

- [x] Normalize mixed-case catalog search requests and repeated whitespace.
- [x] Validate canonical query to reject home/challenge responses without discarding description/series matches.
- [x] Retain server results through pagination and rank direct title/author matches first.
- [x] Keep verified empty catalog results on the original source.
- [x] Add four regressions; APK assembly, 52 unit tests and lint pass.
- [x] Complete phone verification of both Witcher and The Last Wish after installation; Sapkowski listings are visible.
