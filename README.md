# Zunivo (زينوفو)

Android app for **finding, editing, organizing and exporting icons** (from [Iconify](https://iconify.design)), building matching
icon sets from an image's colors, keeping a personal **store** of GitHub projects you review item by item, with Arabic/English (RTL/LTR).

- Package: `com.zunivo.app` - Repository: https://github.com/belalmomani/Zunivo
- Kotlin + Jetpack Compose, single module `:app`, min SDK 26, compile SDK 37, target SDK 36
- **Status:** all code is written; it has **not been verified by a successful build and test run** (see `docs/TEST_REPORT.md`).

## Features (see `docs/FEATURES.md` for the honest done / not-done list)
Search with categories and license details - editor (color, background, size, padding, rotate/flip, previews, save, PNG/SVG export) -
image to icon set (on-device color analysis) - favorites, folders, packs with verified ZIP export - "My store" GitHub import with
accept/reject/review and explicit approval - Start.io banner - self-update from GitHub Releases - local preferences, storage tools, privacy screen.

## Project layout
```
app/src/main/java/com/zunivo/app/
  domain/    pure Kotlin: models, license policy, SVG tools, library/store/prefs rules   (unit-tested)
  data/      remote (Iconify, GitHub), local (JSON stores), repo (search cache)
  feature/   search, editor, studio, library, store, update, prefs  (ViewModels + logic)
  ui/        theme, components, navigation, screens
  ads/       Start.io switch          i18n/  language          core/  app constants
docs/        ARCHITECTURE, PLAN, FEATURES, TEST_REPORT, QA_CHECKLIST, RELEASING, PRIVACY_POLICY, supabase/schema.sql
scripts/     check_branding.sh, update_project_info.sh
.github/workflows/  android.yml (build + tests on every push), release.yml (signed release on a v* tag)
```

## Build
**Easiest: let GitHub build it.** Push to the repository; the *Android CI* workflow runs the branding check, the unit tests and
`assembleDebug`, and uploads the debug APK, `PROJECT_INFO.txt` (real build time) and `permissions.txt` as artifacts.

Local build: JDK 17, Android SDK 37, Gradle 9.4.1 (no wrapper is committed; run `gradle wrapper --gradle-version 9.4.1` once).
```
gradle :app:testDebugUnitTest     # unit tests
gradle :app:assembleDebug         # debug APK  -> app/build/outputs/apk/debug/
gradle :app:assembleRelease       # release APK (signed only if the signing env vars are set)
gradle :app:bundleRelease         # AAB for Google Play
```
Versions are pinned in `gradle/libs.versions.toml` (AGP 9.2.1, Kotlin 2.3.21, Compose BOM 2026.06.00, Coil 3.4.0, Start.io 5.3.1).

## Setup
- **Iconify:** no key needed (public API `https://api.iconify.design`; docs: github.com/iconify/website/docs/api).
- **Start.io:** debug builds use Start.io's public demo App ID with test ads. For production pass your App ID:
  `STARTIO_APP_ID=xxxx gradle :app:assembleRelease` (or `-PSTARTIO_APP_ID=xxxx`; in CI add the repository secret `STARTIO_APP_ID`).
  **Without it, release builds contain no active ads** and the SDK never starts. Where it goes: `app/build.gradle.kts` (BuildConfig field `STARTIO_APP_ID`).
- **GitHub Releases / updates:** tag `vX.Y.Z` matching `versionName`; the *Release* workflow publishes the signed APK and its `.sha256`. See `docs/RELEASING.md`.
- **Signing:** keystore and passwords only via env vars / CI secrets: `ZUNIVO_KEYSTORE_PATH`, `ZUNIVO_KEYSTORE_PASSWORD`, `ZUNIVO_KEY_ALIAS`, `ZUNIVO_KEY_PASSWORD`.
  Create the key once, keep it forever, never commit it. Raise `versionCode` by 1 for every release.
- **Supabase (designer pages, optional, not connected yet):** run `docs/supabase/schema.sql` (tables + Row Level Security). Only the public anon key may ever be used in the app, never `service_role`.

## Known limits
- Public publishing outside designer pages, private GitHub repositories, cloud sync and rewarded ads are **not implemented**; designer pages are written but need a Supabase project and have never run (see `docs/FEATURES.md`).
- The "Z" logo and app icon are drawn from the approved mockup but not pixel-matched; fonts are the system fonts.
- Style filters (outline/filled) and categories are approximate by design and say so in the UI.
- Offline: saved data works; icon images appear offline only if still in the image cache; search and export need internet.
- Google Play: APK self-update and `REQUEST_INSTALL_PACKAGES` conflict with Play policy; use `-PPLAY_BUILD=true` and review the policies first.

## Licenses and attribution
Icons keep the license of their set. Zunivo shows license and author for every icon, keeps them in exports (SVG comment, `README.md`,
`LICENSES.txt`), never exports icons with unknown / non-commercial / no-derivatives licenses in packs, and blocks modified copies of no-derivatives icons.
Libraries: Compose/AndroidX, Coil, AndroidSVG, OkHttp (Apache 2.0); Start.io SDK under Start.io's terms.
The license policy is a safety helper, not legal advice.

## Production checklist
1. Fix every CI failure until `Android CI` is green (unit tests + build).
2. Install the debug APK and go through `docs/QA_CHECKLIST.md`.
3. Create the release key, add the four signing secrets (+ optional `STARTIO_APP_ID`), complete `docs/PRIVACY_POLICY.md`.
4. Review `permissions.txt` from CI (SDK-added permissions) and the Start.io ProGuard rules against its current guide.
5. Raise `versionCode`/`versionName`, update the changelog, tag `vX.Y.Z`, let the *Release* workflow publish.
