# Projektor TV – Xgimi Android-TV Client (Prototyp 0.1.0)

Stand 2026-10-20. Unabhängiges Softwareprojekt ohne Verbindung zu BingeBang. Kein offizieller Client. Quellcode ist ein funktionsorientierter Entwicklungsprototyp. Es liegt **keine** kompilierte APK vor und es gab **keinen** Gerätestart. Die signierte APK und GitHub-HTTPS-Veröffentlichung sind als Workflow vorbereitet, in dieser Umgebung noch nicht ausgeführt.

## Funktionsumfang im Quellcode
- Native Startseite und native Buttons für Filme, Serien, Entdecken, Kalender, Hilfe.
- TV-Launcher (`LEANBACK_LAUNCHER`) und D-Pad-fokussierbare Bedienelemente.
- WebView für nicht dokumentierte Website-Funktionen mit Custom-View-Fullscreen.
- HTTPS-/Origin-Prüfung für Hauptfenster, abgesicherte WebView-Optionen und Reload/Fehlerstatus.
- URL-Favoriten und zuletzt geöffnete Seiten nur lokal; keine positionsgenaue Medien-Fortsetzung.
- Suchdialog mit **experimenteller** DOM-Anbindung; Website-DOM und tatsächliche Ergebnisse nicht auf Android TV geprüft.
- Keine eigenen Backend-Endpoints, keine Weiterleitung an freie Videoquellen und keine DRM-Umgehung.

## Buildvoraussetzungen
- JDK 17
- Android SDK mit Plattform 35 und Build-Tools 35.0.0
- Gradle 8.9
- Netzwerkzugang zu Googles Maven-Repository und Maven Central für Plugins und Android-Abhängigkeiten

## Build
```
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
Die bei Erfolg erzeugte `app/build/outputs/apk/debug/app-debug.apk` ist Android-seitig mit dem automatisch erzeugten **Debug-Zertifikat** signiert (nur für internen Testbetrieb). Der dauerhafte Release-Key ist erzeugt und verschlüsselt gespeichert. Der separate GitHub-Secret fehlt noch. Das Release-Workflow-Skript enthält `apksigner verify` und veröffentlicht erst nach erfolgreichem Build. **Dieses Skript wurde noch nicht auf einem Android-Runner ausgeführt.**

Bei vorhandener Kotlin-Kommandozeile sind die unabhängigen Kernchecks ohne Android SDK ausführbar:
```
./tools/check-core.sh
```

## Dokumente
- `docs/ARCHITECTURE.md`: ADR, Website-Befund und Sicherheitsgrenzen
- `docs/BACKLOG.md`: Rollen, Prioritäten, Freigabegates
- `docs/QA_PLAN.md`: Testmatrix, Status und Prüfgrenzen
- `docs/INSTALLATION.md`: künftige Installation und Updates
- `.github/workflows/android-ci.yml`: GitHub Actions – Tests, Lint, APK-Build, Artefaktupload nach Repository-Einbindung
- `.github/workflows/publish-apk.yml`: Release-Workflow mit dauerhaftem verschlüsselten Schlüssel, Signaturprüfung und GitHub-Releases-Upload. Benötigt einmalig GitHub Actions Secret und Repository-Zugriff.
- `docs/PUBLISHING.md`: genaue GitHub- und Downloader-Schritte

## Datenschutz und Recht
Der Website-Player verweist auf externe Medienquellen. Die Rechte zur Nutzung sind nicht nachgewiesen. Keine öffentliche Distribution unter dem Namen des Website-Betreibers. Keine Zusicherung, dass Inhalte legal oder technisch auf dem Zielgerät abspielbar sind.
