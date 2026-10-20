> Historisches Dokument zu einem früheren Entwicklungsstand. Aktueller Befund und Prüfstatus: [SMARTTUBE_BINGEBANG_AUDIT.md](SMARTTUBE_BINGEBANG_AUDIT.md).

# Architekturentscheidung ADR-001 (2026-10-20)

## Ausgangsbefund
- Website ist über HTTPS erreichbar und zeigt Filme, Serien, Explore, Suche und Player.
- Bekannte Website-Routen: `/`, `/movies`, `/tv`, `/explore`, `/tv/calendar`, `/help`.
- Website-eigener Player nutzt mehrere externe Server; öffentliche dokumentierte API wurde nicht gefunden.
- Gerätehardware/Android-Version/Videoformatcodecs fehlen und wurden nicht ermittelt.
- Rechtsstatus der zugänglichen Medien und Zustimmung zur Nutzung durch Dritte wurden nicht verifiziert.

## Entscheidung
- Native Android-Activity mit programmatischer, fernbedienungsfähiger Oberfläche. minSdk 28 (Android 9).
- Pure Kotlin core (`UrlPolicy`, `Section`, `Favorites`) trennt Validierung von Android-APIs.
- Android WebView nur für derzeit technisch nicht nativ integrierbare Katalogansichten/Player; JavaScript und DOM Storage für Website nötig.
- Keine extrahierten Video-URLs, kein Scraping privater APIs, keine DRM-Umgehung, keine Zugangsdatensammlung.
- Keine fremden Seiten in Hauptansicht: WebViewClient prüft exakten HTTPS-Origin. Subresources und eingebettete Fremd-Frames können technisch weiterhin aus der Website stammen; vor Freigabe ist eine externe Quellenprüfung nötig.
- Cookie-Speicherung verbleibt bei WebView. Third-Party-Cookies deaktiviert, Autofill nicht durch App implementiert.
- Lesezeichen sind URLs ohne Query-Parameter/Fragments (zum Vermeiden persistierter Zugriffstokens) und werden lokal im privaten App-Speicher abgelegt; Verlauf ist eine Liste der zuletzt geöffneten Seiten. Dies ist **kein** positionsgenauer Wiedergabeverlauf.

## Risiken und Konsequenzen
- UI der Website kann auf D-Pad-Geräten unzureichend navigierbar sein.
- Eingebettete Medien können aus WebView inkompatibel sein (DRM, Codec, CORS, Player-Fullscreen, Third-Party-Cookies).
- Search-Adapter basiert auf Website-DOM und kann nach Website-Updates nicht mehr funktionieren.
- Keine Integration auf Stream- oder Episodebene; keine zuverlässige lokale Fortsetzung.
- Android TV-Signierung und Play-Store-Publikation müssen mit kontrolliertem dauerhaften Schlüssel erfolgen.
- Seite empfiehlt keine Fremd-Apps, die ihren Namen verwenden; daher eigenständiger App-Name, ohne offizielle Zugehörigkeit zu behaupten.

## Untersuchte Alternativen
- Direkter Media3-Player: wegen fehlender autorisierter, stabiler API/Streams nicht implementiert.
- HTML-Scraper für Katalog: fragil, ungeklärte Nutzungs- und Sicherheitsfolgen; verworfen.
- Völliger WebView-Wrapper: verworfen zugunsten nativer TV-Navigation.

## Entscheidungsgate
Für produktive Freigabe erforderlich: Rechteklärung, vollständiger Gerätetest, verifizierte Navigation/Suche/Player, kontrollierte Signierung, stabile Update-URL.
