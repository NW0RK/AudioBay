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
- [x] Add ETA display and file-selection controls to Downloads UI.
- [x] Connect Pause, Resume, and Cancel buttons to `TorrentDownloadManager`.
- [x] Implement ongoing foreground notification updates in `TorrentDownloadService`.
- [x] Implement Wi-Fi-only download constraint checking before starting transfers.
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


## Wi-Fi-only and selective tracks — 2026-11-01

- [x] Persist a Wi-Fi-only preference in DataStore and expose it in Settings.
- [x] Pause active transfers on metered/unavailable connections and mark them Waiting for Wi-Fi.
- [x] Resume waiting transfers automatically when an unmetered connection returns; keep the dataSync
  service alive while waiting so the poll loop can react.
- [x] Never gate shared-folder publication or remove completed books because of the network policy.
- [x] Show ETA in the Downloads queue and format unknown/partial estimates safely.
- [x] List torrent files after metadata arrives, persist the catalogue, and allow audio-only toggles.
- [x] Reject empty or non-audio selections; persist selection so a later resume applies it natively.
- [x] Unit tests: DataStore preference, ETA formatting, Settings toggle, file-selection policy.
- [x] Final checks: APK/test APK assembly, lint, 64 JVM tests and 3 device tests
  (`.tools/wifi-selective-final-checks.log`, `.tools/wifi-selective-unit-tests.log`,
  `.tools/wifi-selective-device-tests.log`).

## Search bug fix — 2026-11-01

- [x] Normalize mixed-case catalog search requests and repeated whitespace.
- [x] Validate canonical query to reject home/challenge responses without discarding description/series matches.
- [x] Retain server results through pagination and rank direct title/author matches first.
- [x] Keep verified empty catalog results on the original source.
- [x] Add four regressions; APK assembly, 52 unit tests and lint pass.
- [x] Complete phone verification of both Witcher and The Last Wish after installation; Sapkowski listings are visible.

## Modern Android UI refresh
- [x] Refresh theme, adaptive grids, localized navigation, search controls, detail action, queue menus, and settings headings.
- [ ] Complete presentation-state extraction and focused UI tests.
- [x] Verify debug build, existing unit tests, and Android lint using the bundled Java 17 JDK.
- [ ] Verify device layouts and visual states.

- [x] Expand visual refresh with discovery hero, taller cover cards, grouped Settings cards, detail cover panel, collapsible download setup, and removal of source badges from book cards; build, existing unit tests, and lint pass.

- [x] Apply reference-inspired coral/navy styling, rounded navigation, featured cover shelf, catalog rows, and large cover detail layout; preserve downloader-only behavior. Debug build, existing unit tests, and lint pass.

- [x] Raise icon-only navigation pill above system insets; improve Search layout, explicit keyboard submission, whitespace normalization, automatic pagination, and result count. Add search submission/repeated-query tests; debug build, unit tests and lint pass.

- [x] Add read-only detail comments with author/date, selectable text, refresh, default-compatible cached model, and parser tests for nested replies/empty pages; build, unit tests, and lint pass.
- [ ] Verify comments against a live populated page (sample URLs returned HTTP 404).

- [x] Let screen content scroll behind floating navigation, add trailing scroll space for final items, and move detail download action into scroll content. Debug build and lint pass.

- [x] Add adaptive audiobook download launcher icon with navy, cream and coral artwork and Android themed icon support.

## Frosted glass reference design — 2026-11-01

- [x] Add a cover-led book detail layout with a circular frosted download action overlapping the artwork, centered title/author, and natural cover proportions.
- [x] Share translucent gradient panels, fine glass rims, plum surfaces, coral accents and rounded shapes across discovery, search cards, Downloads, Settings and navigation.
- [x] Add the glass action to download setup and automatically expand a book's prepared form; preserve metadata/folder validation and downloader-only behavior.
- [x] Hide floating navigation on book details; improve navigation contrast over scrolling content.
- [x] Verify debug APK assembly, all 68 existing unit tests and Android lint (`.tools/glass-design-checks.log`); install on the connected phone and inspect discovery, detail, and the prepared download form.


## Richer book information and Saved � 2026-11-01

- [x] Parse optional language, posting date, uploader, format, bitrate and size within catalog posts; exclude comment metadata and preserve unknown/variable bitrate values.
- [x] Show compact language/format/size summaries and labeled detail metadata without inventing missing language or format.
- [x] Add a dedicated Saved tab, accessible save/remove controls, offline snapshots, newest-first ordering, and recoverable read/write failures.
- [x] Persist Saved separately through Room, repository contracts and Hilt; identify books by source plus ID and preserve existing tables through additive v3-to-v4 migration.
- [x] Add parser/cache compatibility and repository/ViewModel regressions for duplicate prevention, source identity, failure/retry, and synchronized state.
- [x] Final build, lint and device verification, including migration/reopen persistence and accessible save/remove controls.
- [x] Verify five-tab layout and Saved empty state visually on Samsung SM-A525F; device UI tests cover all five tabs, Saved navigation, metadata summary, accessible save/remove and failed-write retry.
- [ ] Manually exercise an uncached saved detail while offline; existing detail error/retry flow is retained.

Verification: assembleDebug, testDebugUnitTest (73 tests), lintDebug and four persistence/download device tests passed (`.tools/saved-complete.log`). Two additional Saved UI tests passed (`.tools/saved-ui-passed.log`). Verified debug APK installed on the Android 14 phone. Temporary UI-test interruptions were caused by the phone sleeping and a competing instrumentation run; final isolated UI run passed.

## Reliable Home and compact download setup - 2026-11-01

- [x] Handle Home without restoring saved nested pages; reset discovery to the top on every tap while retaining loaded catalog data and avoiding duplicate Home entries.
- [x] Consume scroll resets once so normal Back navigation can retain browsing position.
- [x] Extract stateless download setup with catalog book/author summary, expandable metadata and link editing, compact destination row and optional full path; retain the glass download action.
- [x] Automatically show missing required metadata; preserve manual magnet/torrent imports, queue edit/resume, SAF selection, busy states, disabled actions and inline errors.
- [x] Add six form UI tests covering edits, missing metadata/folder, busy state, imports, resume/error, and narrow-screen large text; add two saved-stack/Home regression tests.
- [x] Verify debug/test APK builds, 73 JVM tests, lint and all eight new device UI tests (`.tools/home-compact-checks.log`, `.tools/home-compact-device-tests.log`).
- [x] Inspect the compact form on the connected phone and verify Home after scrolling and after discovery/book/download navigation (`.tools/compact-form.png`, `.tools/compact-home-reset.xml`, `.tools/compact-home-flow.xml`).


## Compact audiobook details - 2026-11-01

- [x] Remove title bar; overlay accessible contrasting Back button, retaining navigation during loading/errors and existing status-bar inset.
- [x] Show narrators on separate lines; handle separators, blank values, ambiguous names and Unicode.
- [x] assembleDebug, testDebugUnitTest (three new formatting tests), lintDebug passed (.tools/compact-details-checks.log).
- [ ] Device visual checks for cover position, Back, narrow screens and large text; device connection unavailable during this run.

## Floating navigation pill refinement — 2026-11-01

- [x] Extract stateless glass navigation with five equally spaced outlined icons, 44 dp circular selection, 24 dp icons, 180 ms tint/opacity transitions, 64 dp height, 8 dp inner padding and a centered 400 dp maximum width; preserve existing routes, Home reset, detail hiding and system insets.
- [x] Verify debug assembly, existing unit tests and lint; five focused device tests pass for accessible tabs, selection/callbacks, minimum touch sizes, equal spacing in dark/light layouts and Home navigation (`.tools/navigation-pill-checks.log`, `.tools/navigation-pill-lint.log`, `.tools/navigation-pill-device.log`). Visually inspect the installed app on Samsung SM-A525F with button navigation (`.tools/navigation-pill.png`).
- [ ] Visually verify on a physical tablet and a device using gesture navigation; light theme and width variants currently have automated layout coverage only.

- [x] Improve detail readability with spaced information and description panels, one subtly blurred download button below the cover, and separated torrent/comments sections. Build/lint verification blocked locally by missing Java.

## AI book and author name correction — 2026-11-01

- [x] Add a Hilt-injected Gemini corrector with structured nullable title/author output, validation, a 15-second timeout, cancellation and serialized requests; no request logging, redirects or automatic retries.
- [x] Correct fresh catalog download forms automatically; provide Fix names and Undo for manual and queued-download metadata; preserve manual edits and restored state, discard stale responses, and keep manual downloads available on failure.
- [x] Load the private key from ignored local.properties with an environment override; verify it is absent from tracked files and document private-build limitations in BOOK_NAME_CORRECTION.md.
- [x] Use gemini-3.5-flash-lite after live API verification showed that Google rejects 2.5 Flash-Lite for new users; verify the replacement's documented free tier and live correction of The Hobbit / J. R. R. Tolkien (.tools/book-names-smoke.log).
- [x] Verify debug assembly, 89 passing JVM tests, lint and eight passing download-form UI tests on Samsung SM-A525F; compile the UI test APK (.tools/book-names-checks.log, .tools/book-names-device.log).

## Download startup crash handling - 2026-11-01

- [x] Handle libtorrent4j's wrapped LinkageError and subsequent NoClassDefFoundError as download failures, preserving the UI on first start and retry.
- [x] Cover both native loading failures and exercise the actual injected manager, foreground service and Downloads screen on the Samsung SM-A525F with a synthetic content-free magnet; remove test queue/staging afterward.
- [x] Verify assembleDebug, 90 JVM tests, lintDebug and the startup device test (.tools/download-crash-checks.log, .tools/download-startup-device.log); patched debug build installed.
- [ ] Confirm the original download on the patched build. Original crash not reproduced: app absent from primary profile and device crash buffer empty. Native loading error handling is a confirmed code defect; real-service startup test passes on this phone.

## Installable preview and personal API key - 2026-11-01

- [x] Add masked Gemini API-key entry, replacement and removal in Settings with busy/error feedback; never restore or redisplay the credential.
- [x] Encrypt personal credentials with Android Keystore AES-GCM in no-backup storage; update correction controls reactively and preserve manual downloads.
- [x] Add shared-build mode that excludes private build-time credentials regardless of local/environment configuration.
- [x] Prepare optimized v0.1.0 preview APK, release notes and SHA-256 checksum; verify APK alignment and v2/v3 signatures using the existing local development certificate.
- [x] Verify release assembly, release lint (0 errors), all 95 JVM tests and device-test APK compilation (.tools/release-checks.log, .tools/release-final-tests.log).
- [ ] Run the new encrypted-key persistence/removal device test and visually verify Settings; no phone connected during this release task.

## API-key recovery and in-app updates - 2026-11-01

- [x] Accept quoted Gemini keys and invisible clipboard edge characters; explicitly handle first-use Keystore aliases and verify encrypt/decrypt plus persisted bytes before reporting success.
- [x] Preserve previous credential bytes on failed replacement, retain entered text on failure, and distinguish format/encryption/storage errors with category-only logging.
- [x] Scan all fetched branch/tag history and working files for credential patterns and the known local key; no findings. Make NW0RK/AudioBay public as explicitly selected.
- [x] Add a Hilt-backed domain update contract, semantic version/preview selection, daily automatic checks on app opening, manual checks and in-app availability notice.
- [x] Add explicit update download, progress, cancellation/retry, private cache storage and restoration of verified APKs after process recreation.
- [x] Check SHA-256, package ID, version name/increasing code, SDK, torrent-native ABI and installed signing-certificate compatibility before installation.
- [x] Add narrow read-only FileProvider handoff, Android install-source permission flow, final system confirmation and retained install action after denial; never uninstall as a fallback.
- [x] Verify optimized 0.1.1/code 2 assembly, 115 JVM tests, release lint (0 errors) and device-test APK compilation (.tools/update-final-checks.log). Prepare release APK, matching development signature and checksum.
- [ ] Reproduce the originally reported key-save failure on the phone and run the isolated real-Keystore test plus optimized Settings visual checks; no Android device connected.
- [ ] Exercise actual permission denial/installer cancellation and an installed upgrade while verifying Saved books, queue, persistent folder grants, preferences and the saved key. No Room or audiobook-storage migration was introduced.

- [x] Accept dotted Gemini credentials (including AQ. format) unchanged; cover clipboard cleanup and rejection of embedded spaces, labels and invisible characters.
- [x] Prepare public 0.1.2/code 3 with unchanged signing certificate; verify 116 release tests, assembly, lint, alignment, v2/v3 signatures and private-key exclusion. Make updater repository tests independent of the current app version.

## Manual crash reporting - 2026-11-01

- [x] Save up to five sanitized crash reports in private no-backup phone storage; preserve Android default exception handling.
- [x] Collect Android 11+ historical crash/native crash/ANR exit information on opening Settings; native frames are best effort and omit private paths.
- [x] Add Settings report preview, copy, delete, refresh and manual GitHub issue draft; explain public visibility and require user submission.
- [x] Verify 119 release unit tests, isolated real-phone persistence/handler delegation test, release lint (0 errors), public release assembly, existing certificate, APK alignment and credential exclusion.

## Missing crash-report recovery - 2026-11-01

- [x] Persist native-engine checkpoints before startup/submission/shutdown and recover unfinished sessions on process restart without depending on Android crash classification.
- [x] Save sanitized handled torrent errors; include other Android exit classifications and isolate history-read failures from local report display.
- [x] Provide Create diagnostic report even when no crash record exists; explicitly distinguish interruptions and manual diagnostics from confirmed crashes.
- [x] Verify 119 release unit tests, three phone tests (handler delegation, native interruption recovery and manual fallback when exit history fails), release lint with zero errors, assembly, signing/alignment and public APK credential exclusion.

## No-PC torrent diagnostic export - 2026-11-01

- [x] Add always-available Android document-picker export with success/error feedback and manual GitHub attachment instructions; no automatic transmission or debugging requirement.
- [x] Persist a bounded 80-event torrent timeline before/after native submission and first progress, priority and status operations; include memory page size.
- [x] Capture bounded own-UID Android log summaries on restart and export, with a three-second timeout and strict message allowlist; explicitly disclose device-wide log restrictions.
- [x] Verify 120 unit tests, four real-phone diagnostic tests, public release assembly, lint (zero errors), and 16 KB native-library alignment of the previously reported APK.

## S25 Android 17 storage-daemon workaround - 2026-11-01

- [x] Reproduce on SM-S931B: MediaProvider FUSE NodeTracker assertion occurs while opening external torrent staging; vold finds the app's open staging descriptor and explicitly sends Interrupt, explaining reason 2/status 2. Evidence retained privately in .tools/s25-live.log.
- [x] Use private internal torrent staging for new and resumed native transfers; keep SAF publication and the selected shared root unchanged.
- [x] Relocate legacy unverified queue entries on resume without copying through FUSE; preserve old partial files and archive old resume data, then restart with fresh verification.
- [x] Verify 120 unit tests, release lint with zero errors, release assembly, and real S25 native transfer/restart/publication plus legacy-staging recovery tests. User confirmed the issue is fixed.
- [x] Prepare official 0.1.6 with the existing certificate, public-build credential exclusion, APK alignment/signature checks and SHA-256 checksum.

Device-test caution: Gradle connected-device test cleanup reinstalled the app on the personal S25 and cleared app data. Do not run connectedDebugAndroidTest on a personal device again; use an isolated test app/device or direct adb install -r and instrumentation without automatic uninstall cleanup. User explicitly declined restoring old downloads.

## Warm literary downloader redesign — 2026-11-01

- [x] Replace coral/plum styling with complete ink/amber light and dark schemes, fixed tones, semantic download colors, and theme-aware glass surfaces. Keep dark mode and disabled dynamic color as defaults.
- [x] Bundle Lora with its OFL license; define all typography roles with three weights and 120% title / 150% body line spacing.
- [x] Share stateless cover and row cards across Home, Search, Categories and Saved, with screen-level Saved/download subscriptions, download-setup shortcuts, and publication-aware progress.
- [x] Add featured/recent shelves, category tiles and repository-backed category results with cancellation, pagination and retry.
- [x] Add structured empty/error states, Saved grid/list and sorting controls with saved-state restoration, and queue/Saved-load retry actions.
- [x] Retain the floating navigation pill and tab order, centrally consume measured navigation/system insets, and add calm press/state/progress feedback plus a bounded nested-scroll detail cover.
- [x] Add unit tests for sorting, category state, status mapping, contrast and typography; add UI tests for actions, restoration, Home reset, cover collapse and offline visual fixtures.
- [ ] Complete debug/release build, unit tests, lint and device/visual verification; record results below.
- [ ] Commit and push the redesign. APK signing and GitHub release publication are deferred to the other machine at the user's request.
