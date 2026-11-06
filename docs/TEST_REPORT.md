# Test report

**Honest status:** the author of this code worked without an Android SDK, Gradle or network access. Nothing here has been compiled or run
by the author. The first real result is the GitHub Actions run. Fill the table below from that run.

| Item | Result |
|---|---|
| `gradle :app:testDebugUnitTest` | _(not run yet - paste CI result)_ |
| `gradle :app:assembleDebug` | _(not run yet)_ |
| Manual QA (`docs/QA_CHECKLIST.md`) | _(not done)_ |

## Unit tests written (87, JVM only)
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
