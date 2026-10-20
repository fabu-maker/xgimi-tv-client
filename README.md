# Projektor TV – Native Android TV Client (0.2.0 preview)

This branch is a **new native Android TV implementation** for older Xgimi Android TV devices (API 28+).

## Important limitations

**This is not a completed BingeBang streaming app.** It fetches public HTML catalogue pages and constructs native poster rows, titles, search, detail pages and locally stored favourites/history. No `WebView` is used. The page structure can change, and a complete/official API has not been identified. Native playback of the site's copyrighted catalogue **cannot currently be provided**: there is no verified authorized stream interface. Media3 can be tested using an openly available demo in **Info & Test**.

## Architecture

- `catalog/`: domain models, allowlisted public-site HTML parser, bounded HTTP repository and pure library rules
- `app/MainActivity.kt`: native TV navigation, poster shelves, details, catalogue search and state restoration
- `app/NativeState.kt`: offline favourites/history and demo position
- `tv/PosterLoader.kt`: HTTPS-only artwork with bounded download, cache and decode dimensions
- `player/NativeVideo.kt`: Media3 ExoPlayer and explicit demo source allowlist
- `.github/workflows/`: JVM tests, Android lint, debug build and signed preview release
- `docs/NATIVE_V2.md`: scope, acceptance criteria, integration risks

## Build

Requires JDK 17, Android SDK platform 35 and Gradle 8.9:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python3 tools/native_static_check.py
```

The signed preview build is created through GitHub Actions using the existing encrypted, privately backed-up release signing key. **Never commit the passphrase or decrypted `.p12`.** No device-based compatibility claim is made before testing on a physical Xgimi.

## Supported features

Native D-pad controls, browsing shelves for films/series, metadata-only catalogue details, local favourites, recently opened and search within loaded pages. Native Media3 demo playback. Error handling for unavailable source pages.

## Unsupported

Authenticated BingeBang account sync, full catalogue search, TV episodes / seasons extraction, BingeBang media playback, playback resume for BingeBang titles, subtitles and quality controls for third-party streams. Rights/API approval required.
