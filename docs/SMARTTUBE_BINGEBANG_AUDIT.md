# SmartTube / BingeBang: technischer Befund

Stand: 20.10.2026. Abnahme des gewünschten Streaming-Clients: **offen**.

## Festgehaltene Quellstände

| Projekt | Commit | Untersuchung |
|---|---|---|
| SmartTube | `b0b2b5f4113bbf9ab2fab252d287f0b711ff280f` | README, Gradle-Konfiguration, Android-Module, relevante UI-, Dienst- und Playerpfade, Lizenzen |
| MediaServiceCore (SmartTube-Untermodul) | `b83ff1db5cdd8de097bd811eafd907b344b27e87` | Module, Serviceverträge, YouTube-Kopplung |
| SharedModules (SmartTube-Untermodul) | `d4f09b11133036d90826baf012e120d06486ae73` | Versionskonstanten, Hilfs- und Update-Module |
| xgimi-tv-client/main | `343b4e91fb74f44c15ba1c256ccbefae96a98ddb` | alter 0.1-Stand und Veröffentlichungsmechanik |
| xgimi-tv-client/tv-facade | `1beba82c95ee45219d869a3dde488035cb679427` | Ausgangspunkt der Änderungen, Version 0.5 |

Der SmartTube-Hauptbaum enthielt 4.628 Einträge vor dem Auschecken der Untermodule. Die relevanten Ausführungspfade wurden untersucht. Es erfolgte keine vollständige zeilenweise Sicherheitsprüfung aller Dateien und keine Ausführung der SmartTube-App. Die offizielle Website und README wurden zusätzlich abgerufen. Die dortige Meldung über eine früher kompromittierte Entwicklungsumgebung wurde gesehen. Es wurden keine SmartTube-APKs oder Signaturschlüssel übernommen.

Quellen: [SmartTube am geprüften Commit](https://github.com/yuliskov/SmartTube/tree/b0b2b5f4113bbf9ab2fab252d287f0b711ff280f), [offizielle Website](https://smarttubeapp.github.io/).

## Modulstruktur und Wiederverwendung

| Modul / Bereich | Codebefund | Entscheidung für diesen Entwicklungsstand |
|---|---|---|
| `smarttubetv` | Android-App; Leanback-Fragmente, Kartenpräsentierer, Playeroberfläche, Suchansicht, Ressourcen | Zwei kleine, dienstunabhängig anpassbare Präsentierer übernommen |
| `common` | Presenter, Datenmodelle, Playercontroller, Einstellungen; direkte Abhängigkeit auf `:youtubeapi` | Keine pauschale Übernahme |
| `MediaServiceCore/mediaserviceinterfaces` | Verträge für ContentService, MediaItemService, SignInService, MediaItemFormatInfo | Erfordert einen belegten BingeBang-Dienstadapter |
| `MediaServiceCore/youtubeapi` | YouTubeServiceManager liefert die verwendeten Dienste | Für BingeBang unbrauchbar ohne Ersatzimplementierung |
| `SharedModules` | sharedutils, sharedtests, appupdatechecker2, commons-io, j2v8 | Keine dieser Bibliotheken übernommen |
| `leanback-1.0.0`, `fragment-1.1.0` | Mitgelieferte, angepasste AndroidX-Kopien; Root-Build schließt Originalartefakte aus | Offizielle AndroidX-Leanback-Version 1.2.0 verwendet; Fork-Patches nicht pauschal importiert |
| `exoplayer-amzn-2.10.6` | Eigener ExoPlayer-Baum mit Kern, HLS/DASH, UI und Erweiterungen | Keine Einbindung ohne verifizierte Medienquelle |
| `leanbackassistant` | TV-Empfehlungen und Plattformanbindung | Vorerst nicht übernommen |
| `chatkit`, `doubletapplayerview`, `slidableactivity`, `filepicker-lib` | Chat, Touch-Gesten, Activity-Übergänge, Dateiauswahl | Kein erforderlicher Bestandteil der aktuellen Integration |

### Oberfläche, Reihen, Karten und Fokus

`tv/ui/browse/BrowseFragment.java`, `tv/ui/browse/video/MultipleRowsFragment.java` und `VideoRowsFragment.java` verwenden Leanback-Fragmente, `ArrayObjectAdapter`, `ListRow`, eigene Gruppenadapter und Presenter. `VideoRowsFragment.getMainPresenter()` bindet unmittelbar an `BrowsePresenter.instance()`.

`VideoCardPresenter.java` bindet das SmartTube-Modell `Video`, lädt Bilder in Zielgröße, behandelt Auswahlfarben, bereinigt Bildreferenzen beim Recycling und besitzt YouTube-bezogene Vorschaubilder, Badges und ClickbaitRemover. Die Übernahme reduziert diesen Presenter auf Seitentitel, Poster, Auswahlzustand und Recycling. Die vorhandene begrenzte Bildladefunktion ersetzt seine Glide-Abhängigkeit.

`CustomListRowPresenter.java` und `ViewUtil.java` konfigurieren kleinen Fokuszoom, ausgeschaltete Fokusabdunklung und ausgeschalteten Reihenauswahleffekt. Diese Einstellungen werden direkt in `TvListRowPresenter.kt` verwendet. `FocusFixBrowseFrameLayout.focusSearch()` gibt stets den aktuellen Fokus zurück; dieser spezielle Patch wurde wegen seiner umgebenden Fokuslogik nicht isoliert eingebaut.

Die neue App verwendet `RowsSupportFragment`, horizontale Leanback-Listen und große Karten. Der seitliche Menübereich ist scrollbar. Die Website erhält eine begrenzte räumliche D-Pad-Navigation für sichtbare Bedienelemente und eine Fokusmarkierung. Eingabefelder, native Auswahlfelder und erkannte Playerbedienelemente behalten ihre eigene Tastaturbehandlung. Die Bedienung fremder Player-Iframes ist nicht damit nachgewiesen.

### Dienstkopplung, Suche und Details

`BasePresenter.java`, Zeilen 282–298, ruft für Kommentare, Katalog, Anmeldung, Benachrichtigungen und Medieninformationen direkt `YouTubeServiceManager.instance()` auf. `BrowsePresenter.initRowAndGridMapping()` ordnet Startseite, Abonnements, Verlauf und Playlists den YouTube-ContentService-Methoden zu. `SearchPresenter.loadSearchData()` verwendet `ContentService.getSearchObserve(...)`; Rx-Subscriptions werden bei neuen Anfragen beziehungsweise beim Verlassen aufgelöst.

`SearchTagsFragment` verbindet Text-/Spracherkennung, Tastaturkonfiguration und Ergebnislisten mit dem SearchPresenter. Die README beschreibt die vorhandene Android-TV-Systemtastatur. Diese Suchanbindung liefert keine BingeBang-Ergebnisse. Die App öffnet deshalb die originale Website-Suche. Es gibt keine neu erfundene Such-API.

`DetailsPresenter` übergibt ein vorhandenes `Video` an die DetailsView. Dieses Modell trägt unter anderem YouTube-Video-, Kanal- und Playlistinformationen. BingeBang-Staffeln und Episoden sind dadurch nicht abgebildet. Die App verwendet dafür derzeit die originale Detailseite.

### Wiedergabearchitektur und Fernbedienung

`VideoLoaderController.loadFormatInfo()` (Zeilen 248–261) verwendet `YouTubeServiceManager` und `getFormatInfoObserve(video.videoId)`. `processFormatInfo()` entscheidet anschließend zwischen DASH-Formaten, DASH-/HLS-Manifesten und URL-Listen. Ein Name oder eine Website-Detail-URL reicht für diesen Player nicht aus.

`PlaybackFragment` verbindet `ExoPlayerController`, TrackSelector, MediaSessionConnector, Leanback-PlayerGlue und Suchleisten-Vorschaudaten. `SeekModePlaybackFragment` verwaltet Beginn, Positionsänderung und Ende des Spulvorgangs. `PlayerKeyTranslator` enthält umkonfigurierbare Zuordnungen für Play/Pause, Vor-/Rücklauf, nächste/vorherige Medien und weitere Tasten. `VideoStateController` und die Service-Verbindungen betreffen den SmartTube-/YouTube-Verlauf. Diese Kontologik wird nicht übertragen.

Der ExoPlayer-Initializer bestimmt unter anderem Puffergrenzen anhand des Gerätespeichers. VideoLoaderController räumt laufende Rx-Anfragen auf und verwendet verzögerte Wiederholungen. SmartTube lädt Poster mit Größenbegrenzung und Disk-Caching; sein GlideCachingModule setzt 10 MB Festplattencache. Diese Werte belegen keine geeignete Konfiguration für einen noch unbekannten BingeBang-Medienstrom.

Für den aktuellen BingeBang-Stand bleibt die Wiedergabe im Originalplayer der WebView. `onShowCustomView`/`onHideCustomView` verwalten die Android-Vollbildansicht. Bild, Ton, Spulen, Untertitel, Hardware-Decodierung, HDR und accountbezogenes Fortsetzen sind nicht auf realen Inhalten geprüft. Das Projekt enthält keinen Ersatzplayer mit Demonstrationsfilm.

### Einstellungen, Anmeldung und Updates

`AccountSettingsPresenter`, `YTSignInPresenter` und `MediaServiceManager` verwenden YouTube-/Google-Konten und die zugehörigen SignInService-Verträge. Ein Import dieser Klassen würde die gewünschte BingeBang-Sitzung nicht bereitstellen. Die App verwendet BingeBang-Formulare in derselben WebView und liest keine Formularwerte, Passwörter oder Cookies aus. CookieManager übernimmt Speicherung und Flush. Das Verhalten beim erneuten Anmelden ist noch offen.

`AppUpdatePresenter` verwendet `appupdatechecker2` und SmartTube-spezifische Update-Manifest-URLs. Die App übernimmt diese Adressen nicht. Das bestehende verschlüsselte Projekt-Keystore und dessen GitHub-Actions-Secret bleiben die Grundlage für APK-Updates. Paketkennung bleibt `dev.projectortv.client`, versionCode steigt von 5 auf 6. Erwarteter SHA-256-Zertifikatsfingerabdruck: `C5733EC4C9AE98FF546FAAD957B2310F65386C419C7D12F63A6444E8AD68B94B`.

### Build

SmartTube: Gradle Wrapper 7.5, Android Gradle Plugin 7.4.2, in SharedModules Kotlin 1.8.10, compileSdk 34, Standard-minSdk 17 und mehrere Flavor-/ABI-Varianten. Stbeta/ststable überschreiben das Standard-targetSdk mit compileSdk; stfdroid verwendet minSdk 21. Die README nennt zugleich eine ältere JDK-Empfehlung. Ein erfolgreicher Build dieses gesamten Forks wurde hier nicht behauptet oder ausgeführt.

Client: AGP 8.7.3, Kotlin 1.9.25, Gradle 8.9, compileSdk/targetSdk 35, minSdk 28. Die Übernahme der vollständigen SmartTube-Buildstruktur würde zusätzliche Forks, Ressourcen, Untermodule und Dienste einführen. Der konkrete Nutzen der beiden angepassten UI-Komponenten ist ohne diese Kopplung verfügbar. Leanback 1.2.0 ist eine stabile Version; Google kennzeichnet Leanback inzwischen als veraltet und verweist für neue Projekte auf Compose for TV. Hier dient die Bibliothek der gewünschten Wiederverwendung der untersuchten SmartTube-Komponenten. Das ist ein Wartungsnachteil dieser Wahl.

## Reale Website-Beobachtungen

Umgebung: bereitgestellter Desktop-Cloud-Browser, nicht Android WebView, nicht Android TV, nicht Xgimi. Es wurden keine Zugangsdaten bereitgestellt oder eingegeben.

1. `https://bingebang.st/` öffnete sich erfolgreich. DOM zeigte Home, Movies, Series, Calendar, Explore, My Space, Search und Account.
2. Sichtbare Katalogbereiche enthielten Film-/Serienreihen, Titel-Links, Play, More info und Add to watchlist. Die tatsächlichen Pfade `/movies`, `/tv`, `/tv/calendar`, `/explore`, `/my-space`, `/movie/watch/...`, `/tv/watch/...` und ein `/movie/play/...`-Link waren sichtbar.
3. Der tatsächlich betätigte Play-Link führte zu `/movie/play/tenzing-2026-q2jvwx`.
4. Die Zielseite zeigte „Quick check before you watch“, „You seem to be using a VPN/proxy“ und einen Hinweis zur Roboterprüfung. Ein einmaliges Neuladen führte wieder zu dieser Seite.
5. DOM-Inspektion ergab kein Videoelement und zunächst kein Player-Iframe. Ein Cloudflare-Turnstile-Skript war eingebunden. Der inline sichtbare Prüfcode referenzierte `POST /api/player/gate`. Dieser Prüf-Endpunkt wurde nicht durch die App oder durch eigene HTTP-Aufrufe angesprochen. Er ist kein Nachweis einer Katalog-, Konto- oder Streaming-API.
6. Nach dem fortbestehenden Hindernis wurden weitere Abrufe dieser Website beendet. Es gab keine alternative IP, keinen anderen User-Agent, keine Cookie-Übertragung und keinen Versuch, den Prüfmechanismus zu umgehen.

Der Browserzugang erlaubte DOM- und Konsoleninspektion. Ein vollständiger HAR-Mitschnitt mit Request-/Response-Headern wurde nicht erzeugt. Session-Cookienamen, Authentifizierungsablauf, Kontoschnittstellen, Suchrequests, Staffel-/Episodenrequests und Medienmanifest sind nicht verifiziert. Die Aussagen der Website über Favoriten, Verlauf, Untertitel und mehrere Server wurden gesehen, aber nicht funktional bestätigt.

Dieser Befund beweist eine Sperre für den verwendeten Cloud-Browser. Er beweist weder einen generellen Ausfall der Website noch eine prinzipielle Unmöglichkeit auf dem Xgimi.

## Architekturentscheidung und offene Alternativen

| Variante | Voraussetzung / belegter Befund | Status |
|---|---|---|
| Vollständiger SmartTube-Fork | YouTube-Kopplung in Basis-Presenter, Suche, Anmeldung und Medienauflösung muss ersetzt werden | Quellcodeprüfung erfolgt; kein BingeBang-Dienstvertrag verifiziert |
| Native Oberfläche mit eigener HTTP-API | Nachgewiesene Requests und authentifizierter Sitzungsvertrag erforderlich | Nicht nachgewiesen |
| Reine angepasste WebView | Originalwebsite besitzt Navigation und Player; D-Pad, TV-WebView und Zugriff müssen funktionieren | Desktop-Playerprüfung blockiert; Live-WebView auf Android TV noch nicht geprüft |
| Native Leanback-Reihen über einer WebView-Sitzung | Titel aus derselben Originalseite; Konto und Player verbleiben dort | Implementiert; Live-Nachweis weiterhin offen |

Die hybride Integration wird als überprüfbarer Entwicklungsstand weitergeführt. Ohne erfolgreiche Wiedergabeprüfung kann keine Variante als funktionierendes Endergebnis gelten.

## Lizenznachweis

Übernommen: `TvCardPresenter.kt` aus SmartTube `VideoCardPresenter.java`, `TvListRowPresenter.kt` aus `CustomListRowPresenter.java` und den drei Konstanten in `ViewUtil.java`. Quellcommit und Änderungen stehen in den Dateiköpfen. MIT-Lizenz und Copyright liegen in `app/src/main/assets/licenses/SmartTube-MIT.txt` und sind im Info-Dialog erreichbar.

AndroidX Leanback und dessen aufgelöste transitive Laufzeitabhängigkeiten sowie Kotlin und jsoup werden über `collectRuntimeLicenses` geprüft. Der Task liest originale Maven-POM-Lizenzangaben, bricht bei fehlender oder unerwarteter Lizenz ab und übernimmt eingebettete License-/Notice-Dateien. Vollständige Apache-2.0- und MIT-Texte sind als Assets enthalten. Das CI-Artefakt und die signierte Veröffentlichung enthalten das erzeugte Versionsinventar. Für nicht übernommene SmartTube-Untermodule wird keine abschließende Lizenzfreigabe behauptet. Eine pauschale Aussage „alles MIT“ wäre wegen der eingebundenen Apache- und weiteren Drittkomponenten falsch.

JUnit 4.13.2 ist eine Testabhängigkeit (EPL 1.0), Hamcrest Core 1.3 verwendet BSD; AndroidX-Testbibliotheken sind Apache 2.0. Sie gehören zum getrennten Test-APK beziehungsweise JVM-Testlauf. Die Produktions-APK enthält die Offline-Testaktivität und synthetischen HTML-Seiten nicht.

## Prüfmatrix

| Ebene | Was damit geprüft wird | Status dieses Dokuments vor dem CI-Lauf |
|---|---|---|
| Statische Prüfung | JavaScript-Syntax, Patch-/Whitespace-Check | bestanden |
| Android-Kompilierung | Kotlin/Java, Ressourcen, Manifest, APK-Erzeugung | Lauf angefordert; Ergebnis separat festhalten |
| JVM-Tests | URL-Policy und HTML-Parser | Lauf angefordert |
| Release-Lint | Android-Quellcode-/Ressourcenprüfungen | Lauf angefordert |
| Lizenzinventar | Tatsächlich aufgelöste Laufzeitmodule und Lizenzangaben | Lauf angefordert |
| Android-TV-Instrumentierung | Offline-HTML, echte WebView, D-Pad, native Karten, Zurück, Prüfseite | Lauf angefordert; keine echten Websitezugriffe |
| Signatur | Paket/Version und vorhandener Projektfingerabdruck | Veröffentlichung erst nach erfolgreicher Prüfung |
| BingeBang im Desktop-Browser | Startseite, Film-Link, Sperrseite | beobachtet; Video nicht gestartet |
| Reales BingeBang-Konto | Login, Registrierung, Merkliste, Verlauf | nicht geprüft |
| Reale Filme/Serien auf Android TV | Bild, Ton, Vollbild, Pause, Spulen, Untertitel | nicht geprüft |
| Physischer Xgimi | Bedienung, Performance, Codecs, Neuöffnung | nicht geprüft; kein Gerät verbunden |

## Fortsetzung und Abnahme

Benötigt werden zuerst ein regulär erreichbarer Originalplayer und danach ein Android-TV-/Xgimi-Lauf mit diesem Player. Der Nutzer muss eine erforderliche Browserprüfung beziehungsweise Anmeldung selbst durchführen; Passwörter gehören nicht in Chat oder Repository. Eine Browserprüfung in einem privaten Browser überträgt keine Sitzung in den hier verwendeten Browser.

Auf dem Zielgerät sind Modell, Android-/WebView-Version, Film und Serienepisode, Bild und Ton, Vollbild, Play/Pause, Vor-/Rücklauf, Untertitel, Zurück, App-Neustart, Anmeldung, reale Merkliste und kontobasierter Fortschritt zu protokollieren. Ausfall und Einschränkung werden je Funktion dokumentiert. Eine stabile Veröffentlichung setzt diese Beobachtungen voraus.

Optionale iPhone-Fernbedienung: Die [offizielle Google-TV-Anleitung für iPhone/iPad](https://support.google.com/googletv/answer/11136134?co=GENIE.Platform%3DiOS&hl=de) beschreibt Gerätauswahl, Pairing-Code und Texteingabe. Die Nutzung mit diesem Xgimi bleibt vom Modell und seinem Remote-Dienst abhängig und ist ungeprüft. Eine eigene Smartphone-Pairing-Schnittstelle wurde nicht implementiert. SmartTubes YouTube-TV-Code-Verbindung lässt sich ohne den zugehörigen YouTube-Dienst nicht als BingeBang-Steuerung übernehmen.
