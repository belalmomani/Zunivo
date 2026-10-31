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

## Phase 3: search (implemented)
- `data/remote`: `IconifyApi` (HttpURLConnection, 10 s timeouts, 2 MB cap, typed `ApiException`) and `IconifyParser` (pure, unit-tested).
- `data/repo/SearchRepository`: merges up to 3 related queries round-robin, in-memory LRU cache (40 pages). Icon images are loaded and disk-cached by Coil.
- `feature/search`: `QueryExpander` (small Arabic dictionary + synonyms), `CategorySeeds`, `StyleFilter` (name-based, labelled approximate), `SearchViewModel` (400 ms debounce, 64 first, 999 on "show more").
- `domain/LicensePolicy`: maps the set's SPDX id to what the app may offer; unknown or unrecognized licenses block automatic distribution.
- Source of truth for endpoints: Iconify docs (`/search`, `/{prefix}/{name}.svg?color&height`). Search sets are fetched live; nothing is bundled.
- Added dependencies: Coil 3.4.0 (compose, svg, network-okhttp), lifecycle-viewmodel-compose 2.11.0, org.json (tests only).

## Phase 4: editor + image studio (implemented, not yet built)
- `domain/EditModels`: `EditParams` (colour, background, size, padding, rotate, flip) -> Iconify URL params; `IconEdit` stores params plus a snapshot of set / author / license so edits never lose their source.
- `domain/SvgTools`: `SvgSafety` rejects scripts, external hrefs, event attributes; `SvgComposer` wraps the downloaded SVG with padding/background and a provenance comment (source, original author, license, "adapted copy by the user").
- `feature/editor/EditorViewModel`: save/reopen/delete (JSON file in private storage, `data/local`), export SVG/PNG (PNG rendered with AndroidSVG), share via FileProvider. A no-derivatives license blocks modified copies; unknown licenses show a warning.
- `feature/studio`: on-device palette extraction (bucketed colours), simplicity estimate (colours + edges), HSL harmony; icon suggestions come from category keywords on Iconify. The image is never uploaded and no AI service is used.
- Packs/ZIP export, folders and favorites remain phase 5.
