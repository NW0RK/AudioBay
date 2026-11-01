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
- [ ] Create `HomeViewModel` injecting `AudiobookRepository`.
- [ ] Replace placeholder in `HomeScreen` with 2-column `LazyVerticalGrid` of audiobook cards.
- [ ] Add Coil `AsyncImage` for book cover loading with placeholder & error drawables.
- [ ] Implement pull-to-refresh on `HomeScreen`.
- [ ] Implement pagination (load more items as user scrolls to the bottom).
- [ ] Add loading skeletons / shimmers and error retry banner.

### Phase 2: Search, Details & Caching
- [ ] Create `SearchViewModel` with 300ms debounce on search queries.
- [ ] Update `SearchScreen` with active search bar, search history chips, and results grid.
- [ ] Create `DetailViewModel` fetching full audiobook details by ID/slug.
- [ ] Build `AudiobookDetailScreen` UI: Large cover art, expandable description, author/narrator chips, torrent details (size, seeders, format).
- [ ] Cache viewed audiobooks into Room DB (`audiobookDao.upsertAudiobook`).
- [ ] Build `CategoriesScreen` with grid of genres (Sci-Fi, Fantasy, Mystery, etc.) leading to filtered category lists.

### Phase 3: BitTorrent Engine Implementation
- [ ] In `TorrentDownloadManagerImpl`, instantiate and configure `org.libtorrent4j.SessionManager`.
- [ ] Configure `SettingsPack` (DHT enable, peer limits, upload/download rate caps).
- [ ] Implement `AlertListener` handling (`TorrentAddedAlert`, `PieceFinishedAlert`, `TorrentFinishedAlert`, `SaveResumeDataAlert`).
- [ ] Implement magnet link parsing and download queuing in `startDownload`.
- [ ] Implement `.torrent` file parsing and queuing in `startDownloadFromFile`.
- [ ] Implement selective file priority (ignore non-audio files, prioritize intro tracks).
- [ ] Save fast-resume data to disk on pause/stop to allow instantaneous resume.

### Phase 4: Download Management & Persistence
- [ ] Create `DownloadsViewModel` observing `DownloadDao.observeAllDownloads()`.
- [ ] Update `DownloadsScreen` with active download cards showing progress bar, speed (KB/s), peers, and ETA.
- [ ] Connect Pause, Resume, and Cancel buttons to `TorrentDownloadManager`.
- [ ] Implement ongoing foreground notification updates in `TorrentDownloadService`.
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
- [ ] Write unit tests for `HomeViewModel`, `SearchViewModel`, and `DownloadsViewModel`.
- [ ] Write unit tests for `TorrentDownloadManagerImpl` alert parsing.
- [ ] Verify memory footprint during active torrent transfers.
- [ ] Verify Android 15 foreground service timeout handling (`Service.onTimeout`).
- [ ] Run ProGuard/R8 release build check.
