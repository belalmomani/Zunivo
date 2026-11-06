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
| 12 | Unit tests (87 declared) | Written, not run by the author |

## Not implemented
- **Designer pages** (profiles, published works, real download counts): needs a Supabase project. Schema + RLS are in `docs/supabase/schema.sql`; the screen says "not connected".
- In-app **copyright report / review** flow (only the `copyright_reports` table exists in the schema).
- Public publishing of store projects; private GitHub repositories (needs OAuth, never a token in code).
- Accounts, login/logout, cloud sync (nothing needs them yet).
- Rewarded ads; custom Start.io consent API call (Start.io's own consent notice is used).
- Instrumented/UI tests; pixel-exact logo and custom fonts.

## What the user must do
Signing key + secrets; Start.io production App ID; Supabase project (optional); fill in `docs/PRIVACY_POLICY.md`; Play Data-safety forms if publishing on Google Play.
