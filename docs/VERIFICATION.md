> Historisches Dokument zu einem früheren Entwicklungsstand. Aktueller Befund und Prüfstatus: [SMARTTUBE_BINGEBANG_AUDIT.md](SMARTTUBE_BINGEBANG_AUDIT.md).

# Prüfbericht / Ausführung am 2026-10-20

## Tatsächlich durchgeführt

1. Website-Recherche mit Live-Abruf der Startseite, `/help`, `/movies`, `/tv` und `/tv/calendar`. Die erwarteten Pfade wurden aus realen Links übernommen.
2. Kotlin-Core-Compilation und JVM-Lauf: `./tools/check-core.sh` → PASS (3 erlaubte URLs, 9 blockierte URLs, 6 vordefinierte Routen, URL-Sanitisierung, Favoritenlogik).
3. `python tools/audit_static.py` → PASS (Wohlgeformtheit von Manifest und XML-Ressourcen, TV-Intent, Mindest-SDK, vorhandene Sicherheitsbehandlung und CI-Datei).
4. Git-Repository mit nachvollziehbaren lokalen Commits eingerichtet.
5. Vorbereitete CI: GitHub Actions installiert JDK/Android SDK/Gradle, führt Unit-Tests und Android-Lint aus und lädt im Erfolgsfall die per Debug-Key signierte Debug-APK hoch.

## Nicht durchgeführt

- Android Gradle Compilation, Android Lint und Android-JUnit-Tests: Gradle/Android SDK fehlen lokal; externe Repository-Downloads im Container wegen Namensauflösung nicht möglich. CI-Datei wurde erstellt, aber kein GitHub Actions Run angestoßen.
- APK-Signaturprüfung: keine APK gebaut.
- Android-Emulator / Xgimi-Installation: kein Gerät oder Emulator verbunden.
- Wiedergabe / Codec / Untertitel / Website-Suche / System-Unterbrechung: kein Test mit realem Android WebView.
- Unabhängiges Code-Review: nicht stattgefunden. Dokumentierte Prüfung war ein technischer Selbstreview.
- HTTPS-Upload und öffentlicher Direktdownload: kein Hosting zur Verfügung.

**Resultat:** Codebasis für einen Android-TV-Prototyp vorhanden. Die installierbare APK sowie die produktrelevanten Funktionsnachweise fehlen.

## Release-Vorbereitung, 20.10.2026

- Projekt-PKCS12-Signaturschlüssel mit RSA-3072 und Gültigkeit bis März 2054 lokal erzeugt.
- Verschlüsselten Schlüssel durch AES-256-CBC/PBKDF2 verifiziert: Entschlüsselung und `cmp` mit dem Original waren erfolgreich, `keytool -list` hat das Zertifikat erfolgreich gelesen.
- Ein GitHub-Workflow für Release-Build, Unit-Tests, Lint, APK-Signaturprüfung und HTTPS-Veröffentlichung wurde als YAML angelegt und syntaktisch geparst.
- Kotlin-Kernprüfungen und statische Android-Prüfung erneut bestanden.
- **Nicht durchgeführt:** Vollständige Android-Compilation, GitHub Actions Run, APK-Installation, HTTPS-Download und Xgimi-Wiedergabe. Lokale Umgebung hat kein Android SDK/Gradle und DNS für notwendige Downloads fehlt. GitHub-Repository/Repository-Zugriff ebenfalls noch nicht vorhanden.
