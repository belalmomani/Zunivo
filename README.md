# Zunivo (زينوفو)

Android app for searching, editing, organizing and exporting icons (Iconify), with designer pages
and a personal store that imports GitHub projects into reviewable drafts.

Package: `com.zunivo.app` · Repo: https://github.com/belalmomani/Zunivo

## Status
Phase 1 of 9 (skeleton). See `docs/PLAN.md` and `docs/ARCHITECTURE.md`. Full README is completed in phase 9.

## Build
CI (GitHub Actions) builds and tests on every push: `.github/workflows/android.yml`.
Local: JDK 17 + Android SDK, then `gradle :app:assembleDebug` (Gradle 9.1.0; no wrapper committed yet — generate with `gradle wrapper`).

## Signing
Never commit keys. Release signing reads env vars `ZUNIVO_KEYSTORE_PATH`, `ZUNIVO_KEYSTORE_PASSWORD`, `ZUNIVO_KEY_ALIAS`, `ZUNIVO_KEY_PASSWORD`.

## Licenses
Icons come from Iconify sets with differing licenses; Zunivo keeps and shows each icon's license and author.
