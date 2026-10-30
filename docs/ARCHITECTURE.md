# Zunivo — Architecture

Single Gradle module `:app` for now (split into feature modules only if build time requires it).
Package root: `com.zunivo.app`.

```
core/        app constants, result/error types, dispatchers
ui/          theme, reusable components, navigation, screens (one package per feature)
feature/     search, editor, folders, packs, designers, store (ViewModel + UI state per feature)
data/        repositories; remote (Iconify, GitHub, Supabase); local (Room, DataStore, file cache)
domain/      models and use-cases (license rules live here, independent of UI)
```

Rules
- UI -> ViewModel (StateFlow of Loading / Success / Error / Empty) -> Repository -> data sources.
- Network: Ktor or Retrofit decided in phase 3; HTTPS only; timeouts; bounded disk cache.
- Local: Room for folders, favorites, packs, drafts; DataStore for settings and preferences.
- Licensing is a first-class model: every icon keeps set, author, license and link; copies keep the original's info;
  export/share is blocked when the license forbids it or is unknown.
- Imported GitHub content is untrusted: never executed, size/extension checked, zip-slip safe extraction.
- Secrets: none in the APK or repo. Signing and service keys come from env vars / CI secrets.
- Languages: Arabic (RTL) and English (LTR) resources from day one; user choice persisted.

Versions (chosen from official Android docs, **not yet verified by a real build**):
AGP 9.2.1 (built-in Kotlin) · Gradle 9.4.1 · Kotlin 2.3.21 · Compose BOM 2026.06.00 · compileSdk 37 / targetSdk 36 · minSdk 26 · JDK 17.
