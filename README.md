# Projektor TV für BingeBang

Android-TV-Client im Entwicklungszweig `bingebang-smarttube-audit`, aufbauend auf `tv-facade` 0.5.0. Ziel: Xgimi mit Android TV/Google TV, Android 9 oder neuer.

**Die reale BingeBang-Wiedergabe und das Benutzerkonto sind noch nicht verifiziert.** Beim Filmstart im Cloud-Browser am 20.10.2026 erschien eine Cloudflare-/VPN-Prüfung. Ein physischer Xgimi wurde nicht getestet. Der detaillierte Befund steht in [docs/SMARTTUBE_BINGEBANG_AUDIT.md](docs/SMARTTUBE_BINGEBANG_AUDIT.md).

## Download zur Geräteprüfung

[Signierte Prüf-APK: urlvanish.com/xgimi06](https://urlvanish.com/xgimi06) · [Installationshinweise](docs/INSTALL_0_6.md) · [Prüfprotokoll](docs/VERIFIED_0_6.md)

CI-Lauf 39483073752: Build erfolgreich, 5 JVM-Tests und 4 Offline-Tests auf Android TV bestanden; Release-Lint mit 0 Fehlern und 12 Warnungen. Signatur und Download-Prüfsumme sind geprüft. Dies ist eine Vorabversion mit offener Live-/Xgimi-Abnahme.

## Implementierter Stand

- Native horizontale Leanback-Reihen mit angepassten SmartTube-Karten und Fokussteuerung.
- Titel aus dem DOM der originalen Website; begrenzter Poster-Cache.
- Eine persistente WebView für echte Website-Anmeldung, Originaldetails, Merkliste und Player.
- Website-Suche, D-Pad-Fokushilfe, Zurück-Navigation, Vollbild-Container, HTTP-/Rendererfehler.
- Keine eigenen Konten, lokalen Ersatzfavoriten oder Demonstrationsvideos.

Implementierter Zugriff ist kein Nachweis funktionierender Kontosynchronisation oder Wiedergabe.

## Build und Prüfung

JDK 17, Gradle 8.9, Android SDK 35 / Build Tools 35.0.0:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest
gradle --no-daemon :app:connectedDebugAndroidTest
```

Die Instrumentierung startet ausschließlich die im Debug-APK enthaltene `OfflineTvActivity` und verwendet eigene HTML-Testdaten. Es gibt dabei keine Requests an BingeBang. `collectRuntimeLicenses` kontrolliert aufgelöste Laufzeitabhängigkeiten und erzeugt das mitgelieferte Lizenzinventar.

Der CI-Workflow veröffentlicht nach bestandenen automatischen Prüfungen eine signierte **Vorabversion zur Geräteprüfung**. Die vorhandene Projekt-Signatur bleibt erhalten. Testdaten und Offline-Aktivität sind aus dem Release-APK ausgeschlossen. Alle frühen Dokumente zu Version 0.1–0.5 sind historisch; maßgeblich sind Audit und [Releasehinweise 0.6](docs/RELEASE_0_6.md).

[GitHub Actions](https://github.com/fabu-maker/xgimi-tv-client/actions) · [Releases](https://github.com/fabu-maker/xgimi-tv-client/releases)

## Quellcode und Lizenzen

SmartTube-Quellstand: `b0b2b5f4113bbf9ab2fab252d287f0b711ff280f`. Übernommen und angepasst: `VideoCardPresenter`, `CustomListRowPresenter` und zugehörige Fokuskonstanten. Die Original-Lizenz und Änderungsvermerke liegen im Code und in `app/src/main/assets/licenses/`. AndroidX Leanback 1.2.0 ist inzwischen als veraltet gekennzeichnet; diese Abhängigkeit ergibt sich aus der gewählten Komponentenübernahme.

Die App ist ein unabhängiges Projekt. SmartTube-Markenlogos und Grafiken wurden nicht übernommen.
