# App updates

Settings → App updates checks public GitHub releases from `NW0RK/AudioBay` without
authentication. Opening the app also checks at most once per 24 hours. Automatic
network errors are quiet; manual checks show errors. Preview releases are included.
Downloads and installation require separate user actions. Android owns the final
installation confirmation and may first require permission to install from AudioBay.

The updater downloads an ARM64 APK into private cache storage, verifies its published
SHA-256, package ID, version name/code, minimum SDK, native ABI and signing-certificate
compatibility, then shares only its read-only URI with Android's installer. Android
performs installation-time signature verification. There is no uninstall fallback.
Verified downloads survive process recreation while cache files remain available;
partial downloads are discarded and can be retried. Permission denial retains the APK.

## Publishing compatible releases

1. Increase both `versionCode` and semantic `versionName`; keep the application ID and
   signing certificate unchanged. The current preview certificate is the same local
   development certificate used for 0.1.0; preserve its keystore outside the repository.
2. Build with `-PpublicBuild=true`, which excludes private embedded Gemini credentials.
3. Publish a non-draft GitHub release tagged `v<version>`. Attach exactly one supported
   APK named `AudioBay-<version>-preview.apk` or `AudioBay-<version>.apk`, plus
   `SHA256SUMS.txt` containing its SHA-256 followed by two spaces and its exact filename.
   APKs must remain below 100 MiB. Both assets must finish uploading before discovery.
4. Run build, lint, unit and available device checks; record their actual results and
   include release notes. Future ABI variants require extending asset selection.

Version 0.1.0 has no updater, so 0.1.1 must be installed manually once. Later updates
can use this screen. GitHub distribution is the supported installation channel.

## Verification limits for 0.1.1

JVM tests cover semantic/pre-release ordering, release selection, daily throttling,
offline and rate-limit errors, checksum parsing and actual-byte mismatch, invalid APK
identity/signers/ABI/SDK, download cancellation/retry, storage failures, and restoring
a verified update. Compose tests cover explicit download/cancel controls and the
retained install action after a permission error; they compile but have not run.

No Android device was connected during implementation. Real Keystore execution,
the optimized Settings UI, Android permission/installer behavior, and preservation of
Saved books, queue, folder grants, preferences and credentials across installation
still require device verification. No database or audiobook-storage migration is made.
