# Test report

**Honest status:** the author of this code worked without an Android SDK, Gradle or network access. Nothing here has been compiled or run
by the author. The first real result is the GitHub Actions run. Fill the table below from that run.

| Item | Result |
|---|---|
| `gradle :app:testDebugUnitTest` | _(not run yet - paste CI result)_ |
| `gradle :app:assembleDebug` | _(not run yet)_ |
| Manual QA (`docs/QA_CHECKLIST.md`) | _(not done)_ |

## Unit tests written (91, JVM only)
| File | Tests | Covers |
|---|---|---|
| LicensePolicyTest | 8 | SPDX -> kind, notice, commercial/modification/distribution flags |
| IconRefTest, IconifyParserTest | 7 | safe icon ids, URL building, `/search` JSON parsing |
| QueryExpanderTest, StyleFilterTest | 7 | Arabic dictionary, synonyms, style guess/filters |
| EditParamsTest, SvgToolsTest, EditJsonTest | 12 | edit URL params, SVG safety + composer, edit persistence |
| ImageAnalysisTest | 7 | palette, simplicity, HSL harmony |
| LibraryOpsTest, LibraryJsonTest, PackPlannerTest | 13 | favorites/folders/packs rules, persistence, which licenses may be exported, README/LICENSES text |
| StoreLogicTest | 13 | GitHub URL/host rules, README images, GitHub parsers, draft builder, review ops, update merge |
| UpdateLogicTest | 4 | version comparison, checksum file, latest-release parsing |
| PrefsTest | 9 | opt-in history, bounded learning, ordering, clearing, JSON sanitizing |
| NavStateTest, AppInfoTest | 7 | navigation/back rules, identity constants |

Fixtures for Iconify/GitHub responses are hand-written from the documented shapes, not captured live.

## Not covered by automated tests
Compose UI, ViewModels with the network, Coil image loading, AndroidSVG rendering (PNG export, ZIP preview), Start.io banner,
APK install/verify (`ApkVerifier`), file downloads, clear-all-data. These need a device or emulator: use `docs/QA_CHECKLIST.md`.

## Third-party API names: how far they were checked (no compiler was available)
| API used | Checked against |
|---|---|
| Start.io: `StartAppSDK.initParams(..).setReturnAdsEnabled(false).init()`, `setTestAdsEnabled(true)`, `Banner` in Compose, demo App ID | Start.io's official demo repository (StartApp-SDK/android-sdk-demo) - matches |
| Coil 3: `SingletonImageLoader.get(context)`, `diskCache?.clear()`, `memoryCache?.clear()`, `AsyncImage` | Coil docs and examples - matches; `diskCache.size` not confirmed |
| AndroidSVG: `SVG.getFromString`, `renderToCanvas(Canvas)` | library README from memory - **unconfirmed** |
| GitHub / Iconify JSON shapes | official docs; fixtures hand-written |
If one of these fails to compile, it will be the first CI error.

## UI smoke tests (instrumented, on an emulator)
`app/src/androidTest/.../SmokeTest.kt` (9 tests, offline): bottom navigation after the splash, search tab, empty favorites, editor guidance,
store rejecting an invalid link, privacy screen, settings -> storage, designers "not connected" state, and switching to Arabic (then restoring the system language).
Run: GitHub > Actions > **UI tests (emulator)** > Run workflow (or locally `gradle :app:connectedDebugAndroidTest` with an emulator).
Ads never start during these tests (`AdsConfig` detects the test library). Result: _(not run yet)_.
They check that screens open and key texts show; they do **not** cover icon search results, exports, GitHub import or ads, which need the network or real files (see `docs/QA_CHECKLIST.md`).
