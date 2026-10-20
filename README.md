# Projektor TV · BingeBang-TV-Fassade (v0.5)

**Unabhängiges Android-TV-Projekt, nicht von BingeBang autorisiert oder betrieben.**

Die App stellt den bestehenden BingeBang-Dienst auf Android TV über eine D-Pad-gerechte Startseite dar. Die Website ist die einzige Daten- und Sitzungsebene:

- Startseite, Filme, Serien: Native Android-TV-Posterleisten werden aus den öffentlichen Titel-Links extrahiert, die die **tatsächlich in einem Android WebView geladene Website** enthält.
- Suche, Explore, Kalender, My Space, Login, Registrierung, Favoriten, Verlauf, Einstellungen und Wiedergabe: bleiben Funktionen von BingeBang. Die App betreibt keine eigene Konten-, Merkliste-, Streaming- oder Synchronisierungslogik.
- **Eine persistente WebView-Instanz** wird für alle Seiten verwendet. Sie behält Session-Cookies und Website-LocalStorage. Es gibt keine App-eigenen Login-Formulare, keine Native-JS-Bridge zum Auslesen von Passwörtern, keine Stream-URL-Extraktion und keinen DRM-Bypass.
- Der Film-Play-Knopf navigiert zur Original-Titelseite und versucht, den regulären Play-Knopf des Diensteanbieters anzuklicken.
- Die Website-Ansicht wird ausschließlich für Anbieterfunktionen eingeblendet, bei denen eine vollständige native Integration nicht dokumentiert ist.

## Bedienung
Mit der Fernbedienung links Start, Filme, Serien, Entdecken, Kalender, Mein Bereich, Suche wählen. Native Posterzeilen zeigen Titel aus dem geladenen DOM. Titel anklicken, dann Ansehen oder weitere Website-Funktionen.

## Prüfstand
GitHub Actions kompiliert Android 9+ APKs, prüft Android Lint, Unit-Tests und APK-Signatur. Die echte Xgimi-Fernbedienung, vollständige Titellisten, BingeBang-Anmeldung und Filmwiedergabe auf dem Zielgerät sind **noch nicht nachgewiesen**. Ändert der Anbieter sein DOM, können native Posterlisten leer bleiben; dafür gibt es den Website-Ansicht-Fallback. Keine App kann verhindern, dass der Anbieter Server, Zugangsschutz oder Schnittstellen ändert.

## Datenschutz/Sicherheit
Das Hauptfenster erlaubt nur HTTPS unter genau `bingebang.st`. Drittanbieter-Frames des Originalplayers können technisch Cookies benötigen. JavaScript läuft im WebView, aber es gibt keine exponierte `addJavascriptInterface`-Bridge. Es erfolgt keine eigene Verarbeitung der Zugangsdaten. Die App nutzt die offiziellen Website-Formulare und ist kein offizieller BingeBang-Client.

## Code
- `app/src/main/java/dev/projectortv/app/TvFacadeActivity.kt`: native TV-UI und Fernbedienung
- `app/src/main/java/dev/projectortv/app/SitePageBridge.kt`: einzelner Seitenzustand, DOM-Katalog, Website-Funktionen/Player
- `.github/workflows/publish-apk.yml`: signierter Android-Release, frische Version `v0.5.0-tv-facade`
