Dieser Stand dient der Geräteprüfung. **Die geforderte funktionierende BingeBang-Streaming-App ist noch nicht nachgewiesen.**

Realer Website-Test am 20.10.2026: Startseite und Film-Link geöffnet. Beim Filmstart zeigte BingeBang „Quick check before you watch“ und eine VPN/Proxy-Prüfung. Kein Videoelement geladen. Nach einmaligem Neuladen unverändert. Die Prüfung wurde nicht umgangen. Konto, Film- und Serienwiedergabe, Ton, Untertitel und kontobasiertes Fortsetzen bleiben ungeprüft.

Änderungen gegenüber 0.5.0:

- SmartTube-Kartenpräsentation und Listenfokus aus Commit `b0b2b5f4113bbf9ab2fab252d287f0b711ff280f` angepasst; MIT-Hinweise enthalten.
- AndroidX Leanback 1.2.0 für horizontale, wiederverwendete Kartenlisten.
- Gemeinsame Website-Sitzung für Katalog, Originalformulare und Originalplayer.
- Zurück-Navigation, sichtbarer Fokus, Behandlung verspäteter Katalogantworten, Suche nach Dokumentbereitschaft und Anzeige der beobachteten Browserprüfung.
- Keine eigenen Konten, Ersatzmerkliste, erfundenen Katalogtitel oder Demonstrationsvideos.

Der Veröffentlichungsworkflow verlangt Unit-Tests, Release-Lint, Build, Lizenzinventar, Offline-Instrumentierung auf einem Android-TV-Systemimage und Prüfung der APK-Signatur gegen den vorhandenen Projektschlüssel. Die Instrumentierung verwendet ausschließlich synthetische HTML-Testdaten aus dem Test-APK. Sie prüft keine echten BingeBang-Videos und kein echtes Konto.

Ein physischer Xgimi wurde nicht angeschlossen oder getestet. Das genaue Xgimi-Modell, seine Android-Version und seine WebView-Version sind unbekannt. Mindestversion: Android 9. Die APK verwendet dieselbe Paketkennung und Signatur wie die bisherigen Projektversionen.

Details und Nachweise: `docs/SMARTTUBE_BINGEBANG_AUDIT.md` und der zu diesem Commit gehörende GitHub-Actions-Lauf. Diese Vorabversion gilt nicht als abgeschlossene Abnahme.
