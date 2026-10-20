> Historisches Dokument zu einem früheren Entwicklungsstand. Aktueller Befund und Prüfstatus: [SMARTTUBE_BINGEBANG_AUDIT.md](SMARTTUBE_BINGEBANG_AUDIT.md).

# Testplan und Prüfprotokoll

## Unit-Tests (CI)
- Exact-Hostname-Policy: HTTPS erforderlich; blockiert host-prefix, userinfo, fremden Port und nicht-HTTPS-URI.
- Kanonische native Kataloglinks alle unter erlaubtem Host.
- Favoriten: Einfügen, Deduplikation, Entfernen, Maximalgröße 40, Fremd-URLs abweisen.

## Manuelle TV-Matrix (NICHT durchgeführt)
| ID | Testfall | Akzeptanz | Status |
|---|---|---|---|
| TV-01 | App in Android-TV-Launcher | Kachel sichtbar, App startet | offen |
| TV-02 | Alle nativen Buttons mit D-Pad | Fokus sichtbar, OK aktiviert | offen |
| TV-03 | Zurück aus Katalog | Innerhalb der Website zurück, danach native Startseite | offen |
| TV-04 | Standort ohne Internet | Verständlicher Fehler und Reload | offen |
| TV-05 | Film-/Serienseite | Navigierbar, keine externe Browser-App nötig | offen |
| TV-06 | Suche | Suchbegriff wird angenommen und Treffer erscheinen | offen |
| TV-07 | Player | Start, Pause, Seek, Zurück | offen |
| TV-08 | Vollbild | Player füllt Display, Zurück schließt es | offen |
| TV-09 | Untertitel und Audiospur | Über D-Pad auswählbar | offen |
| TV-10 | Android App Unterbrechung | Rückkehr ohne Absturz/Audiohänger | offen |
| TV-11 | 720p/1080p/4K | Kein unzumutbares Buffering auf Gerät | offen |
| TV-12 | Login/Favoriten | Sitzung korrekt, lokale Favoriten sicher | offen |
| TV-13 | Externe URL im Hauptfenster | Aufruf wird blockiert | offen |
| TV-14 | App nach Update | Daten bleiben bei gleicher Signatur erhalten | offen |

## Beobachtungen
- Web-Inhalte statisch geprüft, keine automatisierte Browser-Sitzung auf dem Zielgerät.
- Local JVM core-smoke test ist separat von Android-Unit-Tests ausführbar.
- Android SDK, Gradle und Xgimi-Hardware stehen im aktuellen Container nicht zur Verfügung.
