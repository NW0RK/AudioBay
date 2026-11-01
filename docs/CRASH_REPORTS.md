# Manual crash reporting

After a crash, reopen AudioBay, open Settings, and scroll to Crash reports. Expand a report to review it. Report to GitHub opens a public issue draft with a short excerpt. Describe the steps, optionally use Copy reports to paste the complete sanitized reports, and submit the issue yourself. GitHub login may be required. Nothing is uploaded automatically.

Up to five reports are retained in app-private no-backup storage. Delete saved reports removes these copies, not submitted GitHub issues. Uninstall removes local reports. Exception messages, URLs, book metadata and full native paths are deliberately excluded; device model, Android version, ABI, app version, timestamps, exception classes and stack frames remain.

Android 11+ provides historical exits. Native stack frames are collected where Android exposes readable text; binary/unavailable tombstones may yield only an exit reason. ANR entries contain only exit metadata. The installed version shown on historical exits may differ from the version that crashed. Java and Android exit records can describe the same incident. Failures before Application.onCreate and native crashes on older Android may not produce a useful report. Reports are collected on entering Settings or tapping Refresh.

Release mapping files must be kept locally for decoding obfuscated frames. This feature does not fix the underlying S25 crash.

## 0.1.4 recovery improvements

Native engine startup and torrent submission now write a durable, metadata-free checkpoint before entering native code. On the next process start, an unfinished checkpoint becomes an interruption report. This can also result from force-stop, reboot or Android killing the process; it is explicitly not labeled a confirmed crash. Normal engine shutdown clears the checkpoint. Handled torrent failures now save sanitized exception frames too.

Android exit collection includes all reason codes, not just CRASH, CRASH_NATIVE and ANR; failure to query Android no longer hides local reports. A new history watermark allows previously ignored exits to be reconsidered. Reports retain chronological timestamps. Create diagnostic report is always available, even without Android crash records. It contains the current app/device details and the last active torrent checkpoint, not a recovered crash stack.

## 0.1.5 phone-only export

After a failure, reopen AudioBay and open Settings > Crash reports > Export diagnostic file. Choose where to save AudioBay-diagnostics.txt using Android's document picker. Review the text, then attach it manually to the existing GitHub issue from the phone browser. No USB, developer options, debugging permission, root or automatic upload is required. Picker cancellation leaves existing reports intact. Export errors are shown and can be retried.

The file includes up to 80 durable, timestamped torrent-stage events across process restarts, memory page size, device/app details, saved reports, and previous/current own-UID log summaries where Android permits. Native submission return, first progress read, priority application and status read are checkpointed separately. These checkpoints localize the last completed/entered stage but are not native stack traces or proof of a particular root cause.

Android does not grant ordinary third-party apps access to all device-wide system logs. The app performs a bounded own-UID logcat query with a three-second timeout; unavailability is stated in the export. A strict allowlist keeps signal identifiers, known Java stack frames and a security-denial indicator. Arbitrary log messages and raw logs are never included in exported files; the temporary private capture is deleted after processing. Delete saved reports also deletes the event journal and saved log summary. Active torrent checkpoints continue to track the engine until normal shutdown.

Native ELF LOAD segments in the 0.1.4 APK were verified as 16 KB aligned, including libtorrent4j. Alignment alone therefore does not establish a cause for the S25 interruption.

## 0.1.6 S25 investigation and workaround

Reproduced on the user's S25 running Android 17: MediaProvider's libfuse_jni aborts in NodeTracker::CheckTracked during pf_open with `active_nodes_.find(node) != active_nodes_.end()`. Android vold subsequently finds AudioBay's open external staging descriptor and logs `Sending Interrupt to pid ...`; AudioBay records reason SIGNALED/status SIGINT. The initiating native crash is in the system MediaProvider process, not an AudioBay Java exception.

Torrent I/O now uses filesDir/torrent-staging/<hash>, avoiding the external emulated-storage FUSE path. SAF remains the only publication route to the user-selected shared folder. Existing unverified external staging is preserved and its resume file archived in private state storage; resuming starts a fresh transfer in internal staging. This consumes internal storage and can restart partial progress. Legacy completed/shared files are not moved or deleted. Interrupted verified publication retains its original source for recovery.
