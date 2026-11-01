# UI/UX refinement — 2026-11-02

Reviewed against interface-design (https://github.com/Dammyjay93/interface-design), Vercel guidelines (https://vercel.com/design/guidelines), and native Compose accessibility guidance. Keep the warm editorial identity, Manrope, opaque surfaces and subtle press feedback.

## Decisions

The primary task is finding a book and saving it for another audiobook app. Use a labeled persistent Prepare download action on details, then a single filled Start download action in setup. Missing input produces field-specific feedback and focus guidance; no transfer starts until metadata and folder checks pass.

Queue-action errors belong on individual items, with independent pending state. Completed published items show their folder and Open folder rather than network statistics. Publication failures say Retry saving. Unfinished removal explains that the transfer stops and shared files remain.

Preserve the existing Search reset, full artwork, Saved and downloader-only scope. Home leads with available catalog content before empty recommendation setup; populated personal picks can lead. Use supporting colors for metadata and semantic colors with text for download status.

Appearance offers Dark (existing default), Light and System. The Wi-Fi setting uses a single labeled toggle target. Custom controls expose focus feedback; section headings and download status changes have accessibility semantics. User revision: bottom navigation always shows five icons in a single 56 dp row. Visible labels are removed; localized accessibility labels and selected-tab feedback remain.

## Validation

136 unit tests, debug lint and public optimized release build passed; 23 focused device tests passed. The signed APK upgrades the A52 without uninstalling. Light-theme status/navigation icon contrast was corrected after inspecting the actual Settings screen.

Galaxy A52 SM-A525F, 1080 x 2400, system font scale 1.5. Deterministic screenshots additionally cover 2x system font scale, 150% app font size, light/dark details, Unicode catalog text and empty states. Device checks cover setup validation, busy states, file import, persistent detail actions, appearance, navigation and saved-card presentation.

This is an implementation and device visual review, not a user study. TalkBack speech and external keyboard traversal still benefit from hands-on assistive-technology testing.

## User revisions in 0.2.7

Larger Home and personal-pick cover cards; larger artwork in list rows; one column in category results and Saved grid view. Search retains its original adaptive 150 dp grid sizing. Its idle recommendation and Browse action are removed, as is the redundant description above the input. Settings offers default-off AI name formatting, which preserves original result identities and never blocks the initial search response. Provider failures retain originals.
