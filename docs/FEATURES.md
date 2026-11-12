# Feature status (honest)

Legend: **Implemented** = code written; **not yet built/run** applies to everything until CI is green and `docs/QA_CHECKLIST.md` is done.

| # | Area | Status |
|---|------|--------|
| 1 | Search on Iconify, details (set, author, license), categories, approximate style filters, AR keyword dictionary, cache, error states | Implemented |
| 2 | License handling: policy, warnings, attribution text, export gating, no-derivatives block | Implemented |
| 3 | Icon editor: color, background, size, padding, rotate, flip, 6 previews, compare, save/reopen/delete/reset, SVG + PNG export | Implemented |
| 4 | Image to icon set: on-device palette, simplicity estimate, harmony palette, category suggestions, exclude/alternatives, create pack | Implemented (no AI service, by design) |
| 5 | Favorites, folders, packs (rename, duplicate, delete with confirm, reorder, file names, pack color), 13 ready-made packs | Implemented |
| 6 | ZIP export (svg, README, LICENSES, colors, preview) with license exclusion and read-back verification | Implemented |
| 7 | My store: GitHub import (info / info+small files), per-item review, accept/reject all, approve, update diff, APK/archive download with warnings + checksum, install via system installer | Implemented (public repositories only) |
| 8 | Start.io banner (demo ID in debug, off without App ID) | Implemented, untested |
| 9 | Update from GitHub Releases with checksum + package + version + signature checks; release workflow | Implemented |
| 10 | Preferences (learning, opt-in history), storage/cache, delete all data, privacy screen + draft policy | Implemented |
| 11 | Arabic/English, RTL/LTR, saved language | Implemented |
| 11c | Recently opened icons on Home (local, last 12, honors the learning switch, cleared from My preferences) | Implemented |
| 11b | First-run welcome tour (3 steps, skippable, shown once) and search suggestions on Home | Implemented |
| 12 | Designer pages: browse, work detail (licenses re-fetched from Iconify), email-code sign-in, profile, publish a pack (license-filtered, private first), visibility switch with confirmation, real download counts, copyright report, delete my page | Implemented; needs a Supabase project (`docs/supabase/README.md`); never run |
| 13 | Unit tests (96 declared) | Written, not run by the author |

## Not implemented
- Reviewing copyright reports inside the app (you review them in the Supabase dashboard); deleting the sign-in account itself (dashboard).
- Public publishing of store projects; private GitHub repositories (needs OAuth, never a token in code).
- Accounts, login/logout, cloud sync (nothing needs them yet).
- Rewarded ads; custom Start.io consent API call (Start.io's own consent notice is used).
- Network-dependent UI tests (search results, exports, GitHub import); pixel-exact logo and custom fonts. Offline UI smoke tests exist (`docs/TEST_REPORT.md`).

## What the user must do
Signing key + secrets; Start.io production App ID; Supabase project (optional); fill in `docs/PRIVACY_POLICY.md`; Play Data-safety forms if publishing on Google Play.
