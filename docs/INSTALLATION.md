# Installation auf Xgimi (nach erfolgreichem Build)

Für den späteren Direktdownload muss eine **tatsächlich gebaute, signierte APK auf einem öffentlichen HTTPS-Server** liegen. Diese URL existiert noch nicht.

1. Auf dem Xgimi in `Einstellungen → Apps/Sicherheit` die Installation aus unbekannten Quellen für die Downloader-App erlauben (Bezeichnung abhängig von Android-Version).
2. In Downloader die *zukünftig bereitgestellte, verifizierte HTTPS-APK-Adresse* eintragen. Nur APKs aus einer vertrauenswürdigen Quelle installieren.
3. Download öffnen, Installation bestätigen; danach `Projektor TV` aus dem Android-TV-Startbildschirm starten.
4. Nach der Installation die Erlaubnis zur Installation unbekannter Apps möglichst wieder deaktivieren.

**Aktuelle Einschränkung:** Eine Link-Adresse kann erst nach einem erfolgreichen Build und HTTPS-Upload geliefert werden. Lokale Sandbox-Downloads sind kein öffentlicher HTTPS-Link zum Beamer.

## Release und Updates
- Android-Paketkennung: `dev.projectortv.client`.
- Für Updates sind derselbe App-Signaturschlüssel und höhere `versionCode`-Werte nötig.
- Ein produktiver Schlüssel muss außerhalb von Git sicher gesichert werden. Der automatisch generierte Debug-Key ist kein langfristig geeigneter Release-Key.
- Android installiert eine über Downloader bezogene APK nur nach expliziter Nutzerbestätigung.
- Empfohlen: Versioniertes `https://<eigene-domain>/releases/projektor-tv-<version>.apk` mit SHA-256 und Release Notes. Das ist ein **Muster**, kein aktiver Link.

Die GitHub-Releases-Vorbereitung einschließlich Setup des Repository-Secrets ist in `docs/PUBLISHING.md` erklärt. Die neue öffentliche URL existiert erst nach dem erfolgreichen Cloud-Build.
