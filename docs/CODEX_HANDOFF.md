# Codex continuation — AudioBookBay Android

Updated 2026-11-01. Preserve the current project; do not restart scaffolding.

## Current state

The original Antigravity handoff overstated build readiness. This session fixed a
missing Gradle wrapper JAR (official checksum verified), an invalid settings DSL
block, missing launcher icons, an unqualified Hilt dispatcher, and two Media3
unstable-API lint errors. No dependency versions were upgraded unnecessarily.

Implemented and verified before torrent work:
- Home discovery: real covers/cards, pull-to-refresh, pagination, loading/empty/error
  states, deduplication, cancellation, and retry without discarding existing cards.
- Search: 300 ms debounce, cancelled stale queries, ordered DataStore history capped
  at 20, results grid and paging controls. The website sometimes ignores queries;
  unrelated listings are rejected so LibriVox can supply real matches.
- Details: type-safe route decoding, real covers, author/narrator/format/bitrate,
  expandable synopsis, chapter metadata, and raw available torrent information.
- LibriVox: documented coverart/id parameters, namespaced IDs, variable url_other
  JSON shape, transport-error fallback, source-consistent paging. Ascending API
  ordering avoids the unfinished recordings seen with newest IDs.
- Room v2 catalog_cache keeps remote snapshots separate from owned library records.
  Explicit migration 1 to 2 replaces destructive migration. Offline paging uses
  cached pages consistently. Full details survive database reopening offline.
- Parser: verified /abss/<slug>/ detail URL, inline metadata boundaries, relative
  links and plain category text; challenge pages cannot become fake details.

The native-engine milestone is implemented in TorrentDownloadManagerImpl. It uses the
actual libtorrent4j 2.1.0-39 API, atomic request/resume files, Room download records,
selected audio priorities, scoped storage checks, polling, native alerts, pause,
resume, and restart restoration. TorrentFilesTest covers selection/path safety.
TorrentTransferTest generates its own lawful WAV/torrent and uses two local native
sessions on the phone to exercise metadata, transfer, pause, restart and byte checks.
The local native transfer test now passes, including partial pause, manager/session
recreation, saved native resume data, completion and byte-for-byte checks. It does not
kill the Android process or test public DHT/tracker connectivity. Upload-mode gating
fixed a real metadata/zero-priority peer disconnect uncovered by this test.
TorrentDownloadService observes progress, stops when idle, handles onTimeout, and
checks idle cleanup under the manager lock to avoid pausing a newly queued transfer.
DownloadsViewModel and DownloadsScreen now support manual magnets, SAF torrent
imports (10 MiB limit), real progress/speed/peers/status, pause/resume/remove, notification
permission and errors. Removal retains content files. Wi-Fi-only is still unimplemented.
The final full build/test/lint/device run is recorded in .tools/final-checks.log.

## Environment and commands

No Java/Android SDK was installed initially. Local tools are ignored under .tools.
PowerShell, from repository root:

```powershell
$env:JAVA_HOME = (Get-ChildItem .tools/jdk -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = "$PWD/.tools/gradle-cache"
$env:ANDROID_HOME = "$PWD/.tools/android-sdk"
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
.\.tools/android-sdk/platform-tools/adb.exe devices
```

Network-enabled execution is needed for uncached dependencies. local.properties
points at .tools/android-sdk. The user authorized USB debugging and installation on
the connected Samsung SM-A525F, Android 14. Wake the phone before manual UI checks;
it sleeps quickly. Do not change the user's global network settings for offline tests.

Logs: .tools/verify.log (discovery checks), .tools/device-tests.log (Room device test),
.tools/engine-tests.log (native transfer checks), .tools/engine-build.log,
.tools/final-checks.log (successful full verification).
All are ignored. Test reports are under app/build/reports and app/build/outputs.

## Next work

1. Continue with lawful source-to-library integration and playback. The final full
   check passed: assembleDebug, 34 unit tests, lintDebug, and 2 Android-device tests
   (including partial pause/resume across native-session recreation). Evidence is
   in .tools/final-checks.log. The final APK was reinstalled after test cleanup.
2. Add ETA and file-selection controls, Wi-Fi-only constraints, and deletion choices.
3. Connect lawful source downloads now that a local native transfer is verified.
   LibriVox detail sections already contain audio URLs; Internet Archive metadata
   must be inspected for actual torrent files rather than inventing URLs.
4. Transfer completed audio into the library, implement robust Media3 playback,
   progress persistence, bookmarks/sleep timer and mini-player.
5. Categories/settings remain placeholders. Wi-Fi-only constraints are not implemented;
   the engine currently rejects that option explicitly rather than pretending it works.
6. Add migration/device/lifecycle/release checks; production readiness is still pending.

## Known limits

- Native ABIs currently include arm64 only. Downloads require API 28; the app still
  has minSdk 26 with an explicit engine guard. Resolve supported-device policy before release.
- Restored interrupted downloads require user resume to respect foreground-service
  launch limits; they do not silently restart in the background.
- Real public DHT/tracker transfers, Android 15 timeout behavior and process-kill
  recovery still need independent verification beyond the local synthetic test.
- Details explicitly state downloads/playback are under development; there is no
  misleading working-looking download button yet.
- No release signing or distribution has been performed.

The later live search fix uses documented LibriVox caret-prefix title queries and treats 404/no-match responses as empty results. Assembly, 34 unit tests and lint passed in .tools/search-checks.log; the corrected APK has now been installed successfully.
