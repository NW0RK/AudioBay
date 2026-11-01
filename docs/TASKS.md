# Release tasks

- [x] Review pending changes and unfinished implementation placeholders.
- [x] Increment release to version 0.1.8 (code 9).
- [x] Pass unit tests, lint and public release build; sign and checksum APK.

## Editorial redesign and personalized discovery â€” 0.1.9

- [x] Replace glass surfaces and circular ripples with an opaque editorial design, bundled Manrope, labeled navigation and subtle motion.
- [x] Generate Gemini recommendations explicitly from completed download history and verified catalog candidates.
- [x] Preserve download history across queue removal, backfill published completions, and support clearing history and cached picks.
- [x] Add grounded-result, explicit-generation, history retention, migration and device UI checks.
- [x] Pass 129 unit tests, lint, public optimized release build and 15 connected-device checks.
- [x] Prepare version 0.1.9 (code 10), preserve the upgrade certificate and generate the public APK checksum.

## Search, catalog and icon update - 0.2

- [x] Reset Search on each navigation tap, retaining suggestions and detail Back behavior.
- [x] Show complete full-width detail covers in the scrolling page with accessible navigation/download controls.
- [x] Correct category URLs/slugs and reject unrecognized pages with retryable errors.
- [x] Put home language/format on one line and file size below.
- [x] Apply the generated app icon and prepare updater-compatible version 0.2.0 (code 11).
- [x] Pass 132 unit tests, lint, optimized public build and 10 device UI checks; verify signing certificate and checksum.

## Metadata separator and font-size settings - 0.2.2

- [x] Correct the catalog separator using an explicit Unicode middle dot.
- [x] Add persistent app-wide font size from 80% to 150%, in 1% steps, with a preview and reset.
- [x] Preserve Android text scaling; scale app typography and line spacing together.
- [x] Pass 135 unit tests, debug lint, public debug/release builds and three connected Android UI checks.
- [x] Prepare version 0.2.2 (code 13), verify the existing upgrade certificate and generate the public APK checksum.

## AudioHarbour branding — 0.2.3

- [x] Rename visible app branding to AudioHarbour with AH short name.
- [x] Generate and apply the flat ember lighthouse icon with flame inside the lantern chamber.
- [x] Preserve installation identity, stored data, update compatibility and signing certificate.
- [x] Verify combined changes with unit tests, lint and optimized public release build.

## Compact discovery header - 0.2.4

- [x] Remove the Your next good story motto and its occupied header space.
- [x] Pass unit tests, lint and public optimized build; preserve the signing certificate and generate release checksum.

## UI/UX refinement - 0.2.5

- [x] Apply interface-design and Vercel review recommendations while retaining the editorial identity and downloader-only scope.
- [x] Unify download actions, validate inputs inline, isolate queue failures and clarify completion/publication states.
- [x] Add persistent Dark/Light/System themes, labeled toggle targets, focus feedback and adaptive large-text navigation.
- [x] Verify 136 unit tests, lint and public debug/release builds; inspect Galaxy A52 screenshots and pass 23 focused device checks.
- [x] Prepare version 0.2.5 (code 16), preserve the upgrade certificate, exclude private build-time credentials and generate APK checksum.

## Icon-only navigation and crash-report removal - 0.2.6

- [x] Keep all five navigation tabs in one 56 dp row with no visible text; retain accessibility labels and selected state.
- [x] Remove crash recording, startup diagnostics, torrent report hooks, Settings controls, export code and obsolete reporting tests.
- [x] Pass 132 unit tests, debug lint, public debug/release builds and seven Galaxy A52 navigation checks, including maximum text size.
- [x] Install and visually verify the signed release on the A52; preserve the upgrade certificate, exclude private credentials and create APK checksum.

## Larger covers and optional AI Search formatting - 0.2.7

- [x] Enlarge Home/personal-pick shelves and list artwork; use one column for category results and Saved grid view.
- [x] Preserve Search cover sizing and remove the empty-search prompt, descriptive filler and Browse button.
- [x] Add default-off, persisted AI Search formatting with background batches, identity-safe parsing, bounded cache, cancellation and original-name fallback.
- [x] Cover preference persistence, missing keys, failures, stale queries, pagination and malformed AI output; pass 142 unit tests, lint, public builds and 15 A52 UI checks.
- [x] Install and visually inspect the signed release on the Galaxy A52; verify public credentials exclusion, upgrade certificate and APK checksum.
