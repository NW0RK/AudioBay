# Research & Feasibility Report — AudioBookBay Android

This document captures the verified research findings, technical trade-offs, Android platform constraints, library evaluations, and mitigation strategies conducted during the preparation phase.

---

## 1. AudioBookBay.lu Integration Feasibility

### 1.1 Official API Availability
- **Finding:** **AudioBookBay.lu does NOT provide any official REST API, GraphQL API, RSS feed, or developer documentation.**
- It is a community-indexed torrent catalog powered by a custom WordPress installation and theme.
- **Community Precedents:** Third-party tools (such as Jackett, Prowlarr, npm packages, and Python scripts) rely exclusively on HTML scraping and Torznab adapters.

### 1.2 Access Patterns, Security & Anti-Bot Protections
- **Cloudflare Protection:** AudioBookBay sits behind Cloudflare. The domain frequently operates in "High" or "Under Attack" challenge mode.
- **TLS Fingerprinting & Challenge Pages:** Standard OkHttp / Retrofit requests may receive HTTP 403 Forbidden or JavaScript challenge pages when requesting web pages from mobile carrier IP addresses.
- **Domain Volatility:** Due to ISP blocking and copyright actions, the top-level domain changes periodically (historical domains include `.is`, `.nl`, `.se`, `.lu`).
- **Authentication & Magnet Links:**
  - Direct `.torrent` file downloads and on-page clickable magnet buttons require user login.
  - **Verified Workaround:** The **raw 40-character hex Info Hash is visible on detail pages to anonymous visitors** in an HTML table row:
    ```html
    <tr>
        <td>Info Hash:</td>
        <td>4a8a35e2528b97d27e2832810a0e28f3521b38e4</td>
    </tr>
    ```
  - An authentic magnet link can be constructed client-side without registration:
    ```
    magnet:?xt=urn:btih:[INFO_HASH]&dn=[URL_ENCODED_TITLE]&tr=[TRACKER_URL]
    ```

### 1.3 Legal, Copyright & Store Policy Risks
- **Copyright:** AudioBookBay predominantly indexes copyrighted commercial audiobooks without publisher authorization.
- **Store Policy:** Submitting an application exclusively branded as or dedicated to AudioBookBay directly violates Google Play Developer Content Policies regarding copyright infringement and intellectual property.
- **Recommendation:** Position the application as a **Universal Audiobook Player & Torrent Downloader** with multi-source support, featuring legal public-domain audiobooks (LibriVox, Internet Archive) as default sources, and treating AudioBookBay as an optional or community-configurable indexer.

---

## 2. Alternative Legal Audiobook Sources

To ensure the app can be developed, tested, and distributed legitimately without legal or anti-bot obstacles:

### 2.1 LibriVox REST API (Primary Legal Fallback)
- **Base URL:** `https://librivox.org/api/feed/audiobooks/`
- **Authentication:** None required (100% free, open).
- **Features:** JSON and XML output, query filtering by `title`, `author`, `genre`, pagination via `limit` and `offset`.
- **Media Hosting:** Direct MP3 downloads and zip files hosted on the Internet Archive.
- **Implemented Contract:** Included in the project as [`LibriVoxApi.kt`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/java/com/audiobookbay/app/data/remote/LibriVoxApi.kt).

### 2.2 Internet Archive
- **API:** Comprehensive metadata API (`https://archive.org/metadata/{identifier}`) and direct downloads.
- **Collection:** Houses the LibriVox audio catalog and historical radio/open audiobooks.

---

## 3. BitTorrent Engine Evaluation for Android

We evaluated four potential BitTorrent implementations for native Android:

| Library / Solution | Status / Activity | License | Android ABIs | Verdict |
|---|---|---|---|---|
| **`libtorrent4j`** (Alden Torres) | **Active** (v2.1.0-39, Feb 2026) | MIT / BSD-3-Clause | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` | **RECOMMENDED & CONFIGURED ✅** |
| `TorrentStream-Android` | Archived (May 2023) | Apache 2.0 | Outdated native bindings | ❌ Do NOT use (abandoned) |
| Direct C++ libtorrent via JNI | Manual | BSD-3-Clause | Custom CMake build | ❌ Redundant (libtorrent4j already does this) |
| Pure Java `ttorrent` | Inactive (last release 2019) | Apache 2.0 | Pure Java | ❌ High battery drain, missing DHT / uTP |

### 3.1 `libtorrent4j` Details & Configuration
- **Coordinates:**
  ```kotlin
  implementation("org.libtorrent4j:libtorrent4j:2.1.0-39")
  implementation("org.libtorrent4j:libtorrent4j-android-arm64:2.1.0-39")
  // Other ABIs available: arm, x86, x86_64
  ```
- **License:** MIT License for the Java/Kotlin wrapper, BSD-3-Clause for the underlying rasterbar C++ core. 100% compatible with proprietary and open-source applications.
- **Minimum Android SDK:** API 28 (Android 9.0+) as of `2.1.0-39`.
- **API Capabilities:** Magnet links (`SessionManager.fetchMagnet`), `.torrent` files (`TorrentInfo`), pause/resume (`TorrentHandle.pause()`), selective file priorities (`TorrentHandle.prioritizeFiles()`), and fast resume data saving.
- **Storage Constraint:** Native C++ code requires POSIX file paths. It cannot stream to `content://` URIs without complex JNI POSIX wrappers. Files must be directed to app-specific directories (`context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`).

---

## 4. Android Background Execution & Foreground Services

Modern Android operating systems (API 31 through API 35) enforce strict background execution rules:

### 4.1 Foreground Service Types (Android 14+)
- **Download Service:** Requires `android.permission.FOREGROUND_SERVICE_DATA_SYNC` and `android:foregroundServiceType="dataSync"`.
- **Audio Playback Service:** Requires `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `android:foregroundServiceType="mediaPlayback"`.
- **Manifest Setup:** Already configured in [`AndroidManifest.xml`](file:///c:/Users/niko0/Documents/GitHub/AudioBookBayAndroid/app/src/main/AndroidManifest.xml).

### 4.2 Android 15 (API 35) 6-Hour Cumulative Background Limit
- In Android 15, `dataSync` foreground services are subject to a **6-hour cumulative quota per 24-hour period** while the application is in the background without user interaction.
- If the quota expires, the system triggers `Service.onTimeout(startId, fgsType)`. The service **must immediately invoke `stopSelf()`**, or the OS terminates the process.
- **Mitigation:** When a user opens the app or interacts with the download notification, the background timer is refreshed.

### 4.3 Battery Optimization & Doze Mode
- In Doze mode, Android suspends background network traffic.
- **Mitigations:**
  1. Hold a `PowerManager.PARTIAL_WAKE_LOCK` and high-performance `WifiManager.WifiLock` during active torrent transfers.
  2. Provide a setting item in `SettingsScreen` directing the user to `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`.

---

## 5. AndroidX Media3 / ExoPlayer Evaluation

AndroidX Media3 is Google's unified media framework, completely replacing legacy ExoPlayer 2 and `MediaBrowserServiceCompat`.

### 5.1 Format Compatibility & Audiobook Features
- **Format Support:** Full hardware and software decoding for MP3, M4A, M4B (AAC), FLAC, and OGG/Opus.
- **M4B Chapter Parsing:** ExoPlayer extracts chapter metadata natively from MP4 atom boxes in M4B files.
- **Variable Playback Speed:** Supported out of the box via `player.setPlaybackSpeed(float)` with automatic pitch preservation (no pitch distortion).
- **Audio Focus Management:** Automatically ducks and pauses for incoming calls and notifications via `AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build()`.
- **Lock Screen & Media Notifications:** Automatically synthesized by `MediaSessionService` without requiring legacy `PlayerNotificationManager`.

### 5.2 Persistence Best Practices
- **Throttling:** Do NOT write playback progress to SQLite on every millisecond tick. Throttle progress writes to once every 10–15 seconds during continuous playback.
- **Force-Save Triggers:** Immediately flush playback position upon:
  - User pause action
  - Chapter skip / seek
  - Application backgrounding (`Activity.onStop()`)
  - Service `onTaskRemoved()`

---

## 6. Summary of Key Risks & Mitigations

| Risk | Impact | Recommended Solution |
|---|---|---|
| **AudioBookBay Cloudflare challenge** | App cannot scrape catalog via simple HTTP | Fallback to LibriVox API; allow user-configurable base URL; consider WebView cookie sync if necessary |
| **Domain change / ISP block** | Dead endpoints | Base URL is configurable in `Settings` and stored in DataStore |
| **Android 15 FGS timeout** | Background download abruptly terminated | Save fast-resume data continuously; handle `onTimeout()` |
| **Storage permission rejections** | App cannot save torrent files | Use app-specific storage (`getExternalFilesDir`) which needs no runtime permission |
| **libtorrent4j memory pressure** | OOM or native crash on low-end devices | Limit concurrent active torrents to 2; limit max connections via `SettingsPack` |
