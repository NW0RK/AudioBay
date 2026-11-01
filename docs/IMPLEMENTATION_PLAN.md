# Phased Implementation Plan — AudioBookBay Android

This document outlines the 8-phase roadmap for completing the application. Each phase includes clear priorities, dependencies, technical tasks, acceptance criteria, and verification steps designed for sequential execution by **OpenAI Codex**.

---

## Roadmap Overview

```mermaid
flowchart LR
    P1["Phase 1: Discovery & Remote"] --> P2["Phase 2: Search & Cache"]
    P2 --> P3["Phase 3: Torrent Engine"]
    P3 --> P4["Phase 4: Download Management"]
    P4 --> P5["Phase 5: Media3 Player"]
    P5 --> P6["Phase 6: Library & Progress"]
    P6 --> P7["Phase 7: UI Polish"]
    P7 --> P8["Phase 8: Hardening & Release"]
```

---

## Phase 1: Audiobook Discovery & Remote Data Integration

**Goal:** Establish reliable remote fetching from AudioBookBay with graceful fallback to LibriVox API.

- **Priority:** High  
- **Dependencies:** None (Scaffolding already in place)
- **Key Files to Touch:**
  - [`app/src/main/java/com/audiobookbay/app/data/remote/AudioBookBayParser.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/data/remote/AudioBookBayParser.kt)
  - [`app/src/main/java/com/audiobookbay/app/data/remote/AudiobookRemoteDataSourceImpl.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/data/remote/AudiobookRemoteDataSourceImpl.kt)
  - [`app/src/main/java/com/audiobookbay/app/ui/screens/home/HomeScreen.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/ui/screens/home/HomeScreen.kt)
  - Create `HomeViewModel.kt`

### Tasks:
1. Implement `HomeViewModel` exposing `StateFlow<UiState<List<Audiobook>>>`.
2. Connect `HomeScreen` with a LazyColumn/Grid displaying cover cards with Coil (`AsyncImage`).
3. Add pull-to-refresh (`PullToRefreshBox` or Material3 pull-refresh).
4. Verify fallback behavior when AudioBookBay returns 403 (switches automatically to LibriVox).

### Acceptance Criteria:
- Launching the app populates the Home screen with audiobook cards (title, author, cover image).
- Pull-to-refresh re-queries the remote source.
- Network disconnection displays an error banner with a retry button.

### Verification:
```bash
./gradlew testDebugUnitTest --tests com.audiobookbay.app.AudioBookBayParserTest
```

---

## Phase 2: Search, Details, and Caching

**Goal:** Implement fast, debounced catalog search and rich detail screens with Room caching.

- **Priority:** High  
- **Dependencies:** Phase 1
- **Key Files to Touch:**
  - [`app/src/main/java/com/audiobookbay/app/ui/screens/search/SearchScreen.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/ui/screens/search/SearchScreen.kt)
  - [`app/src/main/java/com/audiobookbay/app/ui/screens/detail/AudiobookDetailScreen.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/ui/screens/detail/AudiobookDetailScreen.kt)
  - Create `SearchViewModel.kt`, `DetailViewModel.kt`

### Tasks:
1. Implement debounced search in `SearchViewModel` (300ms debounce using Kotlin Flow `debounce`).
2. Persist recent search queries in DataStore via `AudiobookSearchRepository`.
3. Build `AudiobookDetailScreen` UI: Large cover, synopsis, author/narrator chips, format/bitrate info, download button.
4. Cache fetched audiobook metadata in Room DB (`audiobookDao.upsertAudiobook`).

### Acceptance Criteria:
- Typing in the search bar triggers results without freezing the UI.
- Navigating to detail page loads synopsis, format, and torrent metadata.
- Detail page is viewable offline once cached in Room.

---

## Phase 3: Torrent Engine Integration

**Goal:** Integrate `libtorrent4j` native session manager behind `TorrentDownloadManager`.

- **Priority:** Critical  
- **Dependencies:** Phase 2
- **Key Files to Touch:**
  - [`app/src/main/java/com/audiobookbay/app/torrent/TorrentDownloadManagerImpl.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/torrent/TorrentDownloadManagerImpl.kt)
  - [`app/src/main/java/com/audiobookbay/app/torrent/service/TorrentDownloadService.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/torrent/service/TorrentDownloadService.kt)

### Tasks:
1. In `TorrentDownloadManagerImpl`, initialize `org.libtorrent4j.SessionManager`.
2. Configure `SettingsPack` (enable DHT, peer exchange, max connections, disk cache).
3. Implement `AlertListener` handling:
   - `TorrentAddedAlert`
   - `PieceFinishedAlert`
   - `TorrentFinishedAlert`
   - `SaveResumeDataAlert`
4. Wire `startDownload` to parse magnet URIs or `.torrent` files and add them to `SessionManager`.
5. Direct download destinations to `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`.

### Acceptance Criteria:
- Starting a download begins peer discovery via DHT and logs transfer rates.
- Files download to the app's external files directory.
- Fast-resume data is persisted so downloads resume after restart.

---

## Phase 4: Download Management & Persistence

**Goal:** Provide full user control over active downloads (pause, resume, cancel, speed indicators).

- **Priority:** High  
- **Dependencies:** Phase 3
- **Key Files to Touch:**
  - [`app/src/main/java/com/audiobookbay/app/ui/screens/downloads/DownloadsScreen.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/ui/screens/downloads/DownloadsScreen.kt)
  - Create `DownloadsViewModel.kt`
  - [`app/src/main/java/com/audiobookbay/app/data/local/dao/DownloadDao.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/data/local/dao/DownloadDao.kt)

### Tasks:
1. Connect `DownloadsScreen` to observe `DownloadDao.observeAllDownloads()`.
2. Render download items with:
   - Progress bar (0–100%)
   - Speed indicators (KB/s, MB/s)
   - Estimated Time Remaining (ETA)
   - Pause, Resume, and Cancel action buttons.
3. Update `TorrentDownloadService` ongoing notification with download progress and total speed.
4. Implement automatic transition to `COMPLETED` and trigger addition to `LibraryRepository`.

### Acceptance Criteria:
- Tapping "Pause" halts download without losing downloaded pieces.
- Tapping "Resume" restarts download cleanly.
- Completed downloads move from active queue to the user's Library.

---

## Phase 5: Audiobook Player (AndroidX Media3)

**Goal:** Complete the modern audiobook playback experience with lock-screen controls and sleep timer.

- **Priority:** High  
- **Dependencies:** Phase 4
- **Key Files to Touch:**
  - [`app/src/main/java/com/audiobookbay/app/player/AudiobookPlayerImpl.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/player/AudiobookPlayerImpl.kt)
  - [`app/src/main/java/com/audiobookbay/app/player/service/AudioPlaybackService.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/player/service/AudioPlaybackService.kt)
  - [`app/src/main/java/com/audiobookbay/app/ui/screens/nowplaying/NowPlayingScreen.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/ui/screens/nowplaying/NowPlayingScreen.kt)
  - Create `PlayerViewModel.kt`

### Tasks:
1. Build `NowPlayingScreen` UI with:
   - Scrub bar with current position / total duration labels
   - Play/Pause, Skip 30s, Rewind 15s buttons
   - Playback speed bottom sheet (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x, 3.0x)
   - Sleep timer dialog (15m, 30m, 45m, 60m, End of Chapter)
   - Chapter list drawer/modal
2. Connect `PlayerViewModel` to `AudiobookPlayer` contract.
3. Ensure lock screen notification displays book title, chapter title, and transport controls.

### Acceptance Criteria:
- Audio continues playing smoothly when the app is in the background or device is locked.
- Speed alteration changes tempo without changing vocal pitch.
- Sleep timer pauses playback when expired.

---

## Phase 6: Local Library & Listening Progress

**Goal:** Manage offline audiobooks, resume listening positions, and handle local filesystem scans.

- **Priority:** Medium  
- **Dependencies:** Phase 5
- **Key Files to Touch:**
  - [`app/src/main/java/com/audiobookbay/app/ui/screens/library/LibraryScreen.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/ui/screens/library/LibraryScreen.kt)
  - Create `LibraryViewModel.kt`
  - [`app/src/main/java/com/audiobookbay/app/data/repository/PlaybackProgressRepositoryImpl.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/data/repository/PlaybackProgressRepositoryImpl.kt)

### Tasks:
1. Bind `LibraryScreen` to display downloaded audiobooks with progress percentage badges.
2. Implement auto-resume: tapping a book resumes playback from the exact saved millisecond position.
3. Add disk scanner to detect locally imported audiobook folders (`.mp3`, `.m4b`).
4. Implement book deletion and storage space cleanup.

### Acceptance Criteria:
- Closing and reopening the app preserves the listening position.
- Deleting an audiobook cleans up physical audio files from disk.

---

## Phase 7: UI Polish, Dark Theme & Accessibility

**Goal:** Elevate UX to a premium audiobook listening platform standard.

- **Priority:** Medium  
- **Dependencies:** Phase 6
- **Key Files to Touch:**
  - [`app/src/main/java/com/audiobookbay/app/ui/theme/Theme.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/ui/theme/Theme.kt)
  - Screen composables

### Tasks:
1. Implement persistent mini-player bar visible across all bottom-navigation tabs.
2. Add Material3 dynamic color theming based on device wallpaper (Android 12+).
3. Ensure edge-to-edge layout compliance (`WindowInsets`).
4. Add content descriptions for screen readers and TalkBack accessibility.

---

## Phase 8: Testing, Performance & Hardening

**Goal:** Verify stability, edge cases, and release readiness.

- **Priority:** High  
- **Dependencies:** All previous phases

### Tasks:
1. Write ViewModel unit tests using Turbine and MockK.
2. Verify Android 14/15 foreground service limits (`onTimeout` graceful shutdown).
3. Test Wi-Fi-only restriction logic.
4. Run R8/ProGuard minification check on release build.

### Verification:
```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease
```
