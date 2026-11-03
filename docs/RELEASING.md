# Releasing Zunivo

## One-time setup
1. Create a release keystore **once** and keep it forever (losing it means users cannot update):
   `keytool -genkeypair -v -keystore zunivo.jks -alias zunivo -keyalg RSA -keysize 4096 -validity 10000`
2. Never commit the keystore or its passwords. Add them as repository secrets:
   `ZUNIVO_KEYSTORE_BASE64` (`base64 -w0 zunivo.jks`), `ZUNIVO_KEYSTORE_PASSWORD`, `ZUNIVO_KEY_ALIAS`, `ZUNIVO_KEY_PASSWORD`.
3. Ads (optional): create the app in the Start.io portal and add its App ID as secret `STARTIO_APP_ID`.
   Without it, release builds contain no active ads. Debug builds use Start.io's public demo ID with test ads.
   Local release builds: `STARTIO_APP_ID=... gradle :app:assembleRelease` (or `-PSTARTIO_APP_ID=...`).

## Every release
1. In `app/build.gradle.kts` raise `versionCode` by 1 and set `versionName` (e.g. `0.2.0`). Never change `applicationId` or the signing key.
2. Update the changelog text (`changelog_text` in both strings files) and `PROJECT_INFO.txt` KEY_CHANGES.
3. Commit, then tag and push: `git tag v0.2.0 && git push origin v0.2.0`.
4. The `Release` workflow checks that the tag equals `versionName`, runs the tests, builds a **signed** APK, and publishes
   `Zunivo-v0.2.0.apk` and `Zunivo-v0.2.0.apk.sha256` as a GitHub Release. The in-app updater downloads that APK, checks the checksum,
   the package name, that the version is newer and that the signing certificate equals the installed app's, then hands it to Android's installer.

## Google Play
Play does not allow apps to update themselves from APKs, and `REQUEST_INSTALL_PACKAGES` is a restricted permission there.
For a Play build use `-PPLAY_BUILD=true` (turns the self-updater off) and remove the permission from the manifest for that build; review Play's current policies first.
Ads also need the Data safety form and the advertising-ID declaration to be filled in honestly.

## What is not verified yet
Nothing in this repository has been built or run by the assistant that wrote it; the first CI run is the first real compile.
