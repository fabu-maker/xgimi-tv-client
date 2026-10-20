> Historisches Dokument zu einem früheren Entwicklungsstand. Aktueller Befund und Prüfstatus: [SMARTTUBE_BINGEBANG_AUDIT.md](SMARTTUBE_BINGEBANG_AUDIT.md).

# Rollen, Meilensteine, Backlog und Freigabegates

Rollen sind Arbeits- und Prüfperspektiven innerhalb **einer** Assistenz, keine gestarteten autonomen Agenten.

| Verantwortung | Artefakt/Abnahmekriterium | Status |
|---|---|---|
| Projektleitung | Anforderungen, Prioritäten, offene Punkte, Freigabegates | erarbeitet |
| Softwarearchitektur | ADR-001, Domain- und WebView-Isolation | erarbeitet |
| Android-TV-Entwicklung | Native Navigationsansicht, D-Pad-Fokus, Back, TV-Manifest | implementiert, nicht auf Gerät geprüft |
| Website/Streaming | reale Seite, bekannte Routen, WebView-Fullscreen, dokumentierte Risiken | implementiert, nicht auf Gerät geprüft |
| QA/Security | Kern-URL-Tests, Sicherheitscheck, manueller Abnahmeplan | Kernchecks ausführbar; Gerät offen |
| DevOps/Release | Gradle-Projekt, CI-Konfiguration, Git, Build/Signing-Plan | vorbereitet, CI nicht gelaufen |

## P0 – Build und Bedienbarkeit
- [x] Projekt erzeugen, reine Kernfunktionen und Kotlin-Tests
- [x] Native Startseite und TV-Manifest
- [x] Native Bereichsbuttons und lokale Favoriten
- [x] WebView-Sandbox, HTTPS-Origin-Prüfung, Fullscreen-Callback, Fehlermeldungen
- [x] CI-Spezifikation mit Gradle Unit-Tests, Debug-APK, Lint
- [ ] App mit Android SDK vollständig kompilieren und Android-Lint prüfen
- [ ] APK auf Android-9-Emulator starten
- [ ] APK auf Xgimi installieren, Fernbedienungstest durchführen
- [ ] Website-Suche im Xgimi-WebView und Film-/Seriennavigation prüfen
- [ ] Medienwiedergabe/Untertitel/Zurück/Player-Fullscreen verifizieren

## P1 – Produktqualität
- [ ] Rechte- und Integrationsfreigabe
- [ ] Vertraglich erlaubte Schnittstelle für nativen Katalog evaluieren
- [ ] Gerätespezifische Player-/Codec-Matrix inkl. Suspend/Resume
- [ ] Fehlertelemetrie lokal mit expliziter Zustimmung und ohne Personendaten
- [ ] Release-Keystore sicher aufbewahren, signierte APK prüfen
- [ ] Privaten HTTPS-Release-Endpunkt etablieren

## P2 – nach validierter Schnittstelle
- [ ] Episodebezogener Fortsetzungsstand
- [ ] Native Suche mit eigenen Resultatkarten
- [ ] Check auf neuere signierte Version mit expliziter Installation
- [ ] Barrierefreiheit und stabiler Fokus bei Bildwechseln

## Freigabekriterien (M1–M4)
M1: Build erfolgreich, Unit-Tests/Lint grün und Debug-APK per SHA-256 nachweisbar.
M2: Systemnavigation auf Android 9 und auf echtem Xgimi nachweisbar.
M3: Website-Rechteklärung und reproduzierbare Positivtests für Medien/Untertitel/Resume.
M4: Release-APK mit langfristigem Schlüssel und verifiziertem HTTPS-Download.

Keines der vier Meilensteine ist in diesem Lauf vollständig abgeschlossen.

## Automatisierte Release-Strecke (20.10.2026)
- [x] Dauerhaften App-Signaturschlüssel erzeugen und lokale Schlüssel-/Verschlüsselungsprobe durchführen
- [x] Verschlüsselte Signaturdatei und GitHub-Publish-Workflow vorbereiten
- [ ] GitHub-Repository öffentlich erstellen und für ChatGPT-GitHub freigeben
- [ ] Einmaliges GitHub-Actions-Secret hinterlegen
- [ ] Quellen übertragen und Android-Release-Build bis zu grünen Tests ausführen
- [ ] Signiertes APK-Asset samt SHA-256 in GitHub Releases nachweislich publizieren
- [ ] HTTPS-Dateiabruf und Installation auf Xgimi praktisch testen
