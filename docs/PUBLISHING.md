# Automatische signierte APK und öffentliche HTTPS-Bereitstellung

Stand: 2026-10-20. **Release-Workflow vorbereitet, noch nicht auf GitHub ausgeführt.** Die lokal verfügbare Umgebung kann wegen fehlendem Android SDK/Gradle und blockierter externer Namensauflösung keine APK bauen oder veröffentlichen.

## Voraussetzungen

1. Neues **öffentliches**, leeres Repository `xgimi-tv-client` im eigenen GitHub-Konto erstellen. Ein öffentliches Repo ist für einen ohne Login zugänglichen APK-Download über GitHub Releases am einfachsten.
2. ChatGPTs GitHub-Verbindung für dieses Repository autorisieren, damit der Projektquellcode hineingeschrieben werden kann. Repository-Installation/-Berechtigung ist derzeit noch nicht gegeben.
3. In GitHub unter Repository → Settings → Secrets and variables → Actions das **Repository secret** `XGIMI_SIGNING_PASSPHRASE` hinterlegen. Der Wert liegt nur in der separat herunterladbaren privaten Datei `ACTIONS_SECRET.txt`. **Niemals** diese Datei oder den Klartext-Schlüssel in das öffentliche Repository hochladen.
4. Sobald das Repository zugänglich und der Secret gesetzt ist: die Projektdateien nach GitHub übertragen. Der Workflow `.github/workflows/publish-apk.yml` löst bei Push nach `main` oder `master` automatisch aus. Die Unit-Tests, Android Lint und das Signieren erfolgen im GitHub-Runner.

## Veröffentlichung

- Der Workflow entschlüsselt `signing/release.p12.enc` mit dem GitHub Actions Secret, baut `:app:assembleRelease`, prüft die APK mit `apksigner verify` und erzeugt eine SHA-256-Datei.
- Er erstellt GitHub Release `v0.1.0` mit `projektor-tv.apk` und `projektor-tv.apk.sha256`.
- Ist dieser Tag bereits vorhanden, bricht er sicherheitshalber ab, statt eine bereits veröffentlichte APK unter demselben Versionsnamen zu überschreiben.
- Nach **tatsächlich erfolgreicher** Veröffentlichung ist die Downloadadresse aus GitHubs Release-Ansicht abzulesen. Für das Konto `fabu-maker` und den geplanten Repository-Namen entspricht das Download-URL-Muster:

  `https://github.com/fabu-maker/xgimi-tv-client/releases/download/v0.1.0/projektor-tv.apk`

  **Dieses Muster ist gegenwärtig KEIN gültiger/überprüfter Downloadlink. Nicht auf dem Xgimi eingeben, bis die Release-Datei nachweislich existiert.**

## Xgimi-Installation nach erfolgreichem Release

1. Auf dem Xgimi „Downloader“ aus dem Play Store installieren.
2. In „Downloader“ den **verifizierten APK-Link** öffnen und die APK herunterladen.
3. Die erlaubte Installation unbekannter Apps für Downloader bei Androids Rückfrage zulassen, APK installieren.
4. Unter „Apps“ den TV-Client öffnen; anschließend die unbekannte Quellenberechtigung nach Wunsch wieder entziehen.

## Signatur und Updates

- Die verschlüsselte Datei `signing/release.p12.enc` ist Teil der Quellcodebasis. Ohne das separate geheime Passwort ist sie nicht als Signaturschlüssel nutzbar. Originalschlüssel und Passwort als private Backups sicher aufbewahren.
- Jedes Release benötigt *denselben* Schlüssel sowie höhere `versionCode`-Werte. Tag, `versionName` und Release-Notes bei einer neuen Version gemeinsam erhöhen.
- GitHub Releases bietet HTTPS-Download und dauerhafte Versionierung, ersetzt jedoch keinen automatischen Android-App-Update-Mechanismus. Aktualisierte APKs müssen auf dem Beamer erneut heruntergeladen/installiert werden, sofern kein eigener Updater implementiert wird.
- HTTPS-Veröffentlichung bedeutet keine Zusage, dass die Website, deren externe Player oder Medienrechte tatsächlich kompatibel/geklärt sind.
