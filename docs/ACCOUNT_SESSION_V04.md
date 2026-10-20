# Konto, Anmeldung und Wiedergabe – Implementierungsstand 0.4.0

## Im Code vorhanden
- Native Startseite, Katalogkarten, Filmsuche, Detailansichten und lokale Merkliste/History.
- Native Menüfunktion **Mein Konto** führt zu einer getrennten Kontoansicht.
- Registrierung, Passwort-Zurücksetzen und My Space öffnen ausschließlich Seiten unter der genauen HTTPS-Hostadresse `bingebang.st` in einem isolierten WebView. Der Host wird geprüft und fremde Hauptfenster-Navigation blockiert.
- Account-WebView und Originalplayer-WebView benutzen Android WebViews gemeinsamen CookieManager. Sitzungsspeicher wird beim Verlassen geflusht. Die App implementiert kein eigenes Passwortfeld, liest keine Login-Formulardaten aus und hat keine JavaScript-Native-Bridge.
- Abmelden-Schaltfläche entfernt nach Bestätigung lokale WebView-Cookies und WebStorage. Das ist **keine serverseitige Session-Revocation**.
- Der Filmplayer kann den Original-Webplayer verwenden. Der TV-Player lässt sich per Zurück wieder verlassen.

## Einschränkungen
- Kontofunktionen werden von der originalen Website bereitgestellt. Der Anmeldeprozess und die Kontoansicht enthalten daher sichtbar Webtechnologie. Ohne autorisierte Anbieter-Schnittstelle ist kein nativer Token-Flow implementiert.
- Die lokale Merkliste/History des nativen Clients werden noch nicht mit BingeBang-Speicherdaten synchronisiert. Der Webbereich "My Space" bietet die offiziellen serverseitigen Listen an.
- Die Katalogdaten werden ohne private API aus der öffentlichen HTML-Seite gelesen und sind abhängig vom Website-Layout und Netzwerkzugang des Beamers.
- Keine praktische Xgimi- oder Medienwiedergabeprüfung durchgeführt; Live-Video, Account-Neuanlage, Login, Profil- und Watchlist-Sync auf Gerät bleiben ungeprüft.
- Drittanbieter-Streamanbieter sowie urheberrechtliche Nutzungsrechte sind durch die APK-Signierung nicht geprüft oder gesichert.

## Sicherheitsentscheidung
Keine BingeBang-Passwörter im Kotlin-Code. TLS-Fehler werden nicht übergangen. Keine Cookie-Exfiltration und keine DRM-/Captcha-Umgehung. Web-Sitzung bleibt lokal im Android-WebView-Profilspeicher.

## Abnahmestatus
Die automatisierten Tests und ein signierter Build werden unter GitHub Actions protokolliert. Diese Prüfungen ersetzen keine reale Bedien- oder Authentifizierungsprüfung.
