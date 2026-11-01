# Manual crash reporting

After a crash, reopen AudioBay, open Settings, and scroll to Crash reports. Expand a report to review it. Report to GitHub opens a public issue draft with a short excerpt. Describe the steps, optionally use Copy reports to paste the complete sanitized reports, and submit the issue yourself. GitHub login may be required. Nothing is uploaded automatically.

Up to five reports are retained in app-private no-backup storage. Delete saved reports removes these copies, not submitted GitHub issues. Uninstall removes local reports. Exception messages, URLs, book metadata and full native paths are deliberately excluded; device model, Android version, ABI, app version, timestamps, exception classes and stack frames remain.

Android 11+ provides historical exits. Native stack frames are collected where Android exposes readable text; binary/unavailable tombstones may yield only an exit reason. ANR entries contain only exit metadata. The installed version shown on historical exits may differ from the version that crashed. Java and Android exit records can describe the same incident. Failures before Application.onCreate and native crashes on older Android may not produce a useful report. Reports are collected on entering Settings or tapping Refresh.

Release mapping files must be kept locally for decoding obfuscated frames. This feature does not fix the underlying S25 crash.
