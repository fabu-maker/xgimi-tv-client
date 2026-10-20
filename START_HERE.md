# Einmalige Freigabe für den automatisierten APK-Download

Der technische Ablauf ist vorbereitet, kann aber ohne GitHub-Repository und Signatur-Secret nicht starten.

1. https://github.com/new öffnen, Repository `xgimi-tv-client` als **Public** anlegen; kein README für Initialdateien anlegen. (Öffentlich bedeutet: Der Quellcode wird öffentlich lesbar. Der private Signaturschlüssel ist verschlüsselt.)
2. Im Repository `Settings → Secrets and variables → Actions → New repository secret` öffnen. Name `XGIMI_SIGNING_PASSPHRASE` eingeben, als Wert die Passphrase aus der separat bereitgestellten `ACTIONS_SECRET.txt` verwenden. Speichern.
3. ChatGPT-GitHub-Verbindung für dieses Repository freigeben (in ChatGPT unter Plugins/GitHub gegebenenfalls Repository-Zugriff autorisieren).
4. Danach hier schreiben: „GitHub-Repository und Secret sind eingerichtet.“ Dann kann das Projekt über die GitHub-Verbindung übertragen, der signierte Cloud-Build gestartet und das Release überprüft werden.

**Aktuell existieren weder installierbare APK noch verifizierter HTTPS-Downloadlink.** Das Projekt ist ein Prototyp. Die Funktion des Webplayers wurde noch nicht auf dem Xgimi getestet. Medienrechte der Website sind nicht geklärt.
