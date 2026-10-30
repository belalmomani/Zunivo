# Zunivo — Execution Plan (9 phases)

Each phase ends with: build via CI, tests, fixes, a short report, and updated task list.
A phase is not "done" until its acceptance criteria pass. Anything unverified is listed as unverified.

| # | Phase | Acceptance criteria |
|---|-------|---------------------|
| 1 | Environment check + architecture + skeleton | Skeleton compiles in CI; unit test passes; branding check passes; docs written |
| 2 | App shell + UI (approved mockup) | Splash, home, bottom nav (Home/Search/Favorites/More), side menu inside More; AR/EN + RTL/LTR switch persists after restart |
| 3 | Iconify search + categories | Real search against api.iconify.design; paging; cache; offline/timeout/empty states; icon details with set/author/license |
| 4 | Editor + image analysis | Edit color/size/padding/rotate/flip; save as separate copy; PNG/SVG export; local color extraction from image |
| 5 | Folders, favorites, packs | Room storage survives restart; reorder; ZIP export with README + license info; blocked export when license forbids |
| 6 | Designers + "My Store" GitHub import | Draft per repo; per-item accept/reject/review; explicit approve button; safe download/unzip; Supabase RLS (no secret keys in app) |
| 7 | Start.io + in-app update | Test ad id only; banner never covers controls; GitHub Releases update check, download, hash check, system installer |
| 8 | Smart preferences, performance, privacy | Preferences viewable/editable/clearable; bounded caches; privacy policy draft matches real behavior |
| 9 | Tests, docs, release prep | Clean CI build; test report; README; PROJECT_INFO real timestamp; signing via env vars only |

## Known external inputs (not available to the assistant)
- Production signing keystore (env vars `ZUNIVO_KEYSTORE_PATH/PASSWORD`, `ZUNIVO_KEY_ALIAS`, `ZUNIVO_KEY_PASSWORD`)
- Start.io production app id
- Supabase project URL + anon key
