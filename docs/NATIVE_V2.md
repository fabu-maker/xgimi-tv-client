# Native Android TV 0.2.0 – technical contract

## Objective and user acceptance
- No WebView and no on-screen website UI, including in video playback.
- A native home screen, browseable movie/series shelves, thumbnails, remote focus, search, detail, local watchlist/history.
- Android 9+ (`minSdk=28`). New native Media3 video player.
- Separate the public page catalogue source from the player source.

## Source analysis (2026-10-20)
Public HTML `/`, `/movies`, `/tv`, and detail pages `/movie/watch/{slug}`, `/tv/watch/{slug}` are visible. The site help describes aggregation from third-party playback servers and expiring playback access. No documented authorized native stream API or distribution licence has been demonstrated.

## Data architecture
`CatalogRepository` fetches **only public HTML** for allowlisted paths; Jsoup extracts first-party film/series watch links and image metadata. No account cookies, API key discovery, player URL extraction or anti-bot evasion. `BingeBangParser` strictly rejects unexpected origins and non-HTTPS poster URLs. Parsing is inherently brittle; test with live pages on a target device before making compatibility claims. Search is against bounded first-page catalogues; it is not the full site index.

## UI architecture
Kotlin Android native widgets: sidebar navigation, horizontal poster rails, detail and local list screens, search, focus indicator. No WebView imports. Asynchronous I/O, bounded HTML downloads, 4-thread I/O pool, bounded bitmap reads with LruCache. Does not use the prior WebView page.

## Media architecture / limitations
`NativeVideo` uses AndroidX Media3 ExoPlayer. `PlaybackPolicy` allows a single public demo video only. Playback of catalogue titles remains intentionally unavailable pending an authorized documented provider stream source. Login, DRM, cookies or access passes are never bypassed. The pilot cannot currently satisfy full Netflix-like playback of BingeBang titles; release must be described as a *native preview*, not a fully functional streaming client.

## Safety and releases
Reuse `dev.projectortv.client` application ID and the existing encrypted signing key for upgradeability. Increment versionCode. Do not silently replace v0.1.0. Publish v0.2.0 under a fresh GitHub release after unit tests, Android lint, signed APK verification. Play Protect classification and Xgimi device test remain separate unresolved gates.
