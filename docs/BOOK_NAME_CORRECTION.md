# Book and author name correction

Catalog names are automatically cleaned when a fresh download form loads. **Fix names**
also works for manually entered names and editable queued-download metadata. Review the
result before downloading; **Undo** restores the previous title and author. Editing any
input cancels a pending correction. Missing author/title fields still require user input.

Only the supplied title and author are sent to Google. Correction removes subtitles,
series/release labels and narrator credits while preserving the main title and genuine
author names. It does not update catalog records or rename completed shared downloads.
AI results may be wrong; the displayed, editable names determine download folder names.

## Private builds

Set `GEMINI_API_KEY` in the ignored root `local.properties`, or supply the environment
variable of the same name (which takes precedence). Do not commit a real key. Without
a key, correction controls are hidden and manual downloads continue to work.

The key is compiled into the APK and can be extracted. This configuration is intended
for private builds; use a backend for public distribution. Requests use the API-key
header and a dedicated client with logging, redirects and automatic retries disabled.

The model is `gemini-3.5-flash-lite`. Live verification on 2026-11-01 found that Google
rejects `gemini-2.5-flash-lite` for new users and explicitly recommends this replacement.
The replacement supports the free tier according to
[Google's pricing](https://ai.google.dev/gemini-api/docs/pricing).
Keep the Google project on the free tier: the Android client cannot enforce billing
configuration. Quota failures leave existing names intact; retry is explicit.

Requests time out after 15 seconds, and no alternate model is selected on failure.
Free-tier inputs may be used by Google to improve its products; see Google's pricing
and terms for current details.
