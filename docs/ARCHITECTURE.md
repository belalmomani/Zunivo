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

## Phase 5: favorites, folders, packs (implemented, not yet built)
- `domain/LibraryModels` + `LibraryOps`: favourites, folders and packs as one `IconCollection` model (max 200 icons); every entry keeps a snapshot of set/author/license. Pure and unit-tested.
- `data/local/LibraryStore`: `library.json` in private storage (atomic write); no account needed; survives restarts.
- `feature/library/LibraryViewModel`: create / rename / duplicate / delete / reorder / rename files / pack colour; ready-made packs are built live from Iconify search (`StarterPacks`).
- `PackExporter`: ZIP with `svg/*.svg`, `README.md`, `LICENSES.txt`, `colors.json`, `preview.png`; `PackPlanner` excludes unknown, unrecognized, non-commercial and no-derivatives licenses; the archive is re-opened and read back before sharing.
- Designer pages: `docs/supabase/schema.sql` (tables + RLS, real download counts, report table). The app screen says "not connected"; no fake designers. The service_role key must never be in the app.

## Phase 6: "My store" - GitHub import (implemented, not yet built)
- `domain/StoreModels`: `GitHubUrl` (github.com/owner/repo only), `GitHubHosts` (https + GitHub hosts only, also for every redirect), draft/item/decision models, `DraftOps` (accept/reject/reset, cover, order, approve), `DraftMerge` (diff + merge on refresh).
- `data/remote/GitHubApi`: public REST API without a token; repository, README, releases, CHANGELOG.md; streaming download with size caps, manual redirect checks, partial-file cleanup. Private repositories are not supported.
- `feature/store/DraftBuilder`: only real data becomes items; anything missing stays NOT_FOUND (for example "no APK" is shown as such, never invented). README images from non-GitHub hosts are listed but never fetched.
- `StoreViewModel` + `DraftStore`: drafts persist as JSON in private storage; imported files are stored, never executed, never extracted. APK/source archive are downloaded only on an explicit per-item action after a warning, then SHA-256 is compared with GitHub's digest when one exists (mismatch deletes the file); installing uses Android's own installer after the user allows it.
- Original data (name, description, owner, license, source link) is kept separate from editable display data. Owner, source link and license can never be hidden. Nothing appears in "My projects" before the explicit approve button.
- Not done yet: public publishing of projects, private repositories (needs OAuth/device flow, never a token in code), choosing arbitrary repository files.

## Phase 7: ads + in-app updates (implemented, not yet built)
- Ads: Start.io SDK 5.3.1 (`com.startapp:inapp-sdk`), initialised through the manifest `APPLICATION_ID` meta-data (documented method). The meta-data name/value come from build config: debug = Start.io's public demo ID + test ads; release = your `STARTIO_APP_ID`, or a dummy key so the SDK never starts. `AdsConfig.enabled` guards every SDK call. One banner (`AdBanner`) sits above the navigation bar; return ads are disabled in the manifest; ads are never required for a feature. Start.io's own consent notice is left enabled. Rewarded ads are not implemented.
- Updates: `UpdateViewModel` reads `releases/latest` of the GitHub repo, compares versions numerically, downloads the APK (GitHub hosts only, 200 MB cap), verifies SHA-256 (asset digest or a `<apk>.sha256` asset) and, with `ApkVerifier`, package name + newer versionCode + identical signing certificate, then opens Android's installer. A mismatch deletes the file. `-PPLAY_BUILD=true` disables the updater.
- Release automation: `.github/workflows/release.yml` (signed APK + checksum, refuses to publish unsigned). See `docs/RELEASING.md`.

## Phase 8: preferences, storage, privacy (implemented, not yet built)
- `domain/UserPrefs` + `PrefsOps` (pure, tested): learned category/colour usage (bounded to 30 keys each) and an opt-in, capped (20) search history that is deleted when switched off. Learning only re-orders (Home/Search categories, recent colours in the editor); nothing is hidden. Stored in `prefs.json` in private storage; shown, editable and erasable in *My preferences*.
- *Storage and data*: shows image-cache / temporary / data sizes, clears Coil caches and temp export/update folders, and "Delete all local data" uses Android's `ActivityManager.clearApplicationUserData()` (everything is removed and the app restarts empty).
- Bounded caches: search pages LRU(40); Coil disk cache (library default, bounded); exports and updates temp files are deleted after 24 h.
- Privacy: sectioned in-app draft (what is stored, what is sent to whom, ads, permissions, controls) + `docs/PRIVACY_POLICY.md`. CI lists the merged-manifest permissions (`permissions.txt` artifact) so SDK-added permissions are reviewed.
- Offline: previously viewed icons usually reappear from Coil's cache but this is not guaranteed; saved edits, favorites, folders, packs and drafts always work offline (export and search need internet).
