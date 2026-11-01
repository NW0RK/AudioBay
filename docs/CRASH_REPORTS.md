# Manual crash reporting

After a crash, reopen AudioBay, open Settings, and scroll to Crash reports. Expand a report to review it. Report to GitHub opens a public issue draft with a short excerpt. Describe the steps, optionally use Copy reports to paste the complete sanitized reports, and submit the issue yourself. GitHub login may be required. Nothing is uploaded automatically.

Up to five reports are retained in app-private no-backup storage. Delete saved reports removes these copies, not submitted GitHub issues. Uninstall removes local reports. Exception messages, URLs, book metadata and full native paths are deliberately excluded; device model, Android version, ABI, app version, timestamps, exception classes and stack frames remain.

Android 11+ provides historical exits. Native stack frames are collected where Android exposes readable text; binary/unavailable tombstones may yield only an exit reason. ANR entries contain only exit metadata. The installed version shown on historical exits may differ from the version that crashed. Java and Android exit records can describe the same incident. Failures before Application.onCreate and native crashes on older Android may not produce a useful report. Reports are collected on entering Settings or tapping Refresh.

Release mapping files must be kept locally for decoding obfuscated frames. This feature does not fix the underlying S25 crash.

## 0.1.4 recovery improvements

Native engine startup and torrent submission now write a durable, metadata-free checkpoint before entering native code. On the next process start, an unfinished checkpoint becomes an interruption report. This can also result from force-stop, reboot or Android killing the process; it is explicitly not labeled a confirmed crash. Normal engine shutdown clears the checkpoint. Handled torrent failures now save sanitized exception frames too.

Android exit collection includes all reason codes, not just CRASH, CRASH_NATIVE and ANR; failure to query Android no longer hides local reports. A new history watermark allows previously ignored exits to be reconsidered. Reports retain chronological timestamps. Create diagnostic report is always available, even without Android crash records. It contains the current app/device details and the last active torrent checkpoint, not a recovered crash stack.
