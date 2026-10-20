> Historisches Dokument zu einem früheren Entwicklungsstand. Aktueller Befund und Prüfstatus: [SMARTTUBE_BINGEBANG_AUDIT.md](SMARTTUBE_BINGEBANG_AUDIT.md).

# Code-Review-Protokoll – 2026-10-20

Review-Typ: analytischer Selbstreview der aktuellen Implementierung, keine unabhängige externe Person, kein ausgeführter Android-Build.

| Befund | Risiko | Maßnahme | Nachprüfung |
|---|---|---|---|
| R01 Falsche Kalenderroute (`/calendar`) im frühen Stand | Navigation defekt | Korrigiert auf `/tv/calendar` anhand Live-Webseite | Kernprüfung mit Section URLs bestanden; Geräte-Test offen |
| R02 Suchzustand könnte beim Öffnen verloren gehen | Suche reagiert nicht | Suchzustand nach WebView-Initialisierung gesetzt | Code-Review; realer Website-DOM-Test offen |
| R03 Browser-URL mit Query-Tokens könnte als Favorit gespeichert werden | Lokale Speicherung vertraulicher Parameter | Persistierte URLs entfernen Queries und Fragments | Kotlin-Prüfung erfolgreich |
| R04 Medien- und DRM-Unterstützung unbewiesen | Video startet unter Umständen nicht | Website-Player wird gekapselt; kein Video-Extractor | offenes Gerätetest-Gate |
| R05 Website-Katalog hat keine dokumentierte, freigegebene API | Natives Filmkarten-/Sucherlebnis nicht robust implementierbar | WebView-Adapter als gekennzeichneter Zwischenstand | offenes Integrations-Gate |
| R06 Release-Zertifikat fehlt | Keine fortlaufend updatefähige Release-APK | Langfristige Release-Signierung erforderlich; nur CI Debug-APK vorgesehen | Release-Gate offen |
| R07 Website-Rechte unklar | Rechtliches Veröffentlichungsrisiko | Keine Distribution bis zur Prüfung; kein behaupteter offizieller Client | offen |

| R08 D-Pad-Startfokus anfangs nicht garantiert | Fernbedienungsbedienung beim Start | Fokus der ersten nativen Taste beim Anzeigen anfordern | Quellcode angepasst, Gerätetest offen |
| R09 HTTP-Fehler ohne Statusausgabe | Fehlerdiagnose | `onReceivedHttpError` und Navigationszustand ergänzt | Quellcode angepasst, Emulatorprüfung offen |

## Grenzen der Prüfung
Kotlin-Core wurde mit lokaler Kotlin-Kommandozeile kompiliert und geprüft. Statische Prüfungen kontrollieren das Vorhandensein wichtiger Android-Mechanismen und XML-Wohlgeformtheit. Sie stellen keine Android-Compilation, Instrumentierung, Laufzeitprüfung, unabhängige Codeprüfung oder vollständige Sicherheitsanalyse dar.
