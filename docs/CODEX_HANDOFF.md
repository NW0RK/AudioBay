# AudioBookBay downloader handoff

Updated 2026-11-01. The user explicitly changed the product to a downloader only.
Built-in playback, listening progress UI, library UI, sleep timers and mini-player
are out of scope. Their service, navigation, bindings, contracts and Media3
dependencies have been removed. Legacy Room tables/models remain for safe upgrades.

## Current product

Four tabs: Home, Search, Downloads, Settings. Discovery/search/details remain from
commit a73670b. Details with an available magnet open a prefilled editable download
form. Manual magnet and torrent imports require author, book title and shared folder.
Missing metadata is requested rather than saved under an invented author name.

Default destination: internal storage Android/AudioBooks. First use requires the
system ACTION_OPEN_DOCUMENT_TREE picker. On the connected Samsung Android14 phone,
Android itself cannot be granted, but its AudioBooks subfolder CAN be created and
granted. That exact folder was created through the picker and its persistent grant
saved in DataStore. Settings and Downloads share the same folder preference.
If a phone denies a location, the user must explicitly choose another writable tree.
Do not replace SAF with guessed filesystem paths or request all-files access.

Completed layout: <selected root>/<Author>/<Book Title>/<original tracks/discs>.
A redundant torrent wrapper directory is stripped; disc folders and track names are
preserved. Unicode names are retained, unsafe characters sanitized, and folder
components capped at 180 UTF-8 bytes. Existing books are not overwritten: collisions
use Book Title (2), (3), etc. Author folders are reused.

## Pipeline and persistence

libtorrent still writes to owned app-specific staging (Download/torrents/<hash>).
After selected pieces verify, SavedTorrent persists the verified file manifest,
pauses/removes the native handle, and starts shared publication independently of
native session lifetime. Room v3 adds author, tree URI, published folder URI and
publication progress. Migration 2 to 3 preserves every legacy table and download;
1 to 2 to 3 upgrades are registered. Old request JSON fields have compatible defaults.
Legacy downloads request missing metadata before resuming/publication.

BookPublisher uses an opaque document backend, a durable private journal and a
hidden per-download pending directory under the author. Copies are SHA-256 verified
through the destination provider and checkpointed per file; retries reuse verified
copies. The completed book folder appears after successful directory rename. A marker
permits recovery if a process stops between rename and final journal commit.
Destination/metadata changes on failed publication restart copying from retained
staging without fetching torrent data again. Old inaccessible pending folders are
left intact rather than risking deletion in a previously selected location.

COMPLETED is set only after verified shared publication. Only then is owned staging
removed. Publication errors retain source audio and show a retry/edit action. Runtime
state overlays also preserve visible errors if low storage prevents a journal/Room
write. Shutdown cancels copying and records a recoverable publication failure. The
foreground dataSync service includes publication in its active work and keeps API35
onTimeout cleanup. Published files are never removed by queue removal.

## Verification

- 48 JVM unit tests, APK/test APK assembly and lint passed after publication work.
- Three Android device tests passed against the REAL configured Android/AudioBooks
  SAF tree: catalog persistence, Room v2-to-v3 migration, and generated lawful torrent
  transfer with partial pause, native session recreation, shared byte equality and
  staging cleanup. The publication test removes only its own test book and empty
  test-author folder afterward.
- The user chose folder/file verification only. Smart Audiobook Player is not installed
  in the phone's main profile; do not claim its UI was tested.
- Final verification passed after low-storage runtime recovery changes: assembly,
  lint and 48 unit tests; all three device tests passed again on the real configured
  SAF folder. Evidence: .tools/downloader-final-checks.log, .tools/metadata-tests.log,
  .tools/shared-folder-device-final.log. The final debug APK is installed on the phone.

## Build and device commands

```powershell
$env:JAVA_HOME = (Get-ChildItem .tools/jdk -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = "$PWD/.tools/gradle-cache"
$env:ANDROID_HOME = "$PWD/.tools/android-sdk"
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
.\.tools/android-sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
.\.tools/android-sdk/platform-tools/adb.exe install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
.\.tools/android-sdk/platform-tools/adb.exe shell am instrument -w -r -e useConfiguredFolder true com.audiobookbay.app.test/androidx.test.runner.AndroidJUnitRunner
```

Run instrumentation directly after installation to retain the user's app data and
SAF grant. Gradle connectedDebugAndroidTest previously removed the target app during
cleanup. Without useConfiguredFolder, native tests use an isolated file document
backend; do not confuse that with real SAF verification. All build tools/logs live
under ignored .tools. The user authorized this connected phone for installation and
testing. Wake it before UI work; it sleeps quickly. Its main Android user is 0.

## Remaining work beyond this request

Public DHT/tracker reliability, Android15 timeout on an API35 phone, Wi-Fi-only
constraints, richer selective-file UI, and release signing/minification checks remain.
Native ABIs currently include arm64 and downloads require API28; browsing minSdk is26.
No playback work should be reintroduced. The Android folder grant is mandatory and
cannot be silently manufactured. Files in shared storage remain after app uninstall.
