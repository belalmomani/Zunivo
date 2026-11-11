# Manual QA checklist (device or emulator, debug APK from CI)

Tick each item only after doing it. Note the result in `docs/TEST_REPORT.md`.

## Launch and navigation
- [ ] Fresh install: splash, then the 3-step welcome tour (Skip works at any time); it does not appear on the next launch
- [ ] Home: tapping a suggested search (e.g. home / بيت) opens Search with results
- [ ] App starts, splash then Home; bottom bar Home/Search/Favorites/More; back button behaves
- [ ] Rotate the phone on Search, Editor and Store: state is kept
## Language
- [ ] Settings > Language: Arabic is RTL, English is LTR; choice survives closing the app; all screens translated
## Search and details
- [ ] Search "home" and "بيت": results appear, icons render, "show more" works
- [ ] Airplane mode: clear message + retry; no crash
- [ ] Open an icon: set, author, license, license note shown; copy name / attribution works
- [ ] A set with unknown or non-commercial license shows the warning
## Editor
- [ ] Change color/background/size/padding/rotate/flip; all six previews; compare with original
- [ ] Save, close the app, reopen from "Saved edits"; reset; delete
- [ ] Export SVG and PNG: share sheet opens, files open in another app; SVG contains the source/license comment
- [ ] A CC-BY-ND icon cannot be modified/exported modified
## Image to icon set
- [ ] Pick a photo/logo: colors, simplicity, harmony palette; choose categories; exclude/alternatives; create pack
## Favorites, folders, packs
- [ ] Add/remove favorite; create folder; add icons; reorder; rename file; duplicate; delete with confirmation
- [ ] Start a ready-made pack; export ZIP; open the ZIP elsewhere: svg/, README.md, LICENSES.txt, colors.json, preview.png; excluded icons listed
- [ ] Restart the app: everything is still there
## My store
- [ ] Paste a real public repo link: owner/name/size shown first; import info only
- [ ] Import a repo with no APK: exact "no APK" message
- [ ] Reject one image, accept the description only; change a decision after "accept all"; approve; only accepted items shown
- [ ] Close and reopen: draft/project restored; refresh shows a change list before applying
- [ ] Download an APK (warning first), checksum status shown; install goes through Android's installer
## Ads and updates
- [ ] Debug build shows a Start.io test banner that never covers a button; release build without App ID shows none
- [ ] Settings > Updates: up to date / new version / no release / offline / bad file cases
## Preferences, storage, privacy
- [ ] History off by default; turn on, search, see shortcuts; turn off: deleted
- [ ] Storage screen sizes; clear cache; delete all data restarts the app empty
- [ ] Privacy screen matches what the app does; CI `permissions.txt` reviewed
