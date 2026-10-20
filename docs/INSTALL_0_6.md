# Geräteprüfung auf Xgimi – Version 0.6.0

**Diese APK ist ein signierter Entwicklungsstand. Echte BingeBang-Anmeldung und Film-/Serienwiedergabe auf Xgimi sind noch nicht bestätigt.**

Kurzer APK-Link: **https://urlvanish.com/xgimi06**

Am 20.10.2026 geprüft: Der Link antwortet mit HTTP 301 direkt auf die APK dieser [GitHub-Vorabversion](https://github.com/fabu-maker/xgimi-tv-client/releases/tag/v0.6.0-device-check). Auch die Kurzlink-API bestätigt dasselbe Ziel. Die heruntergeladene APK wurde gegen ihre veröffentlichte SHA-256-Prüfsumme geprüft.

## Installation

1. Voraussetzung: Xgimi mit Android TV/Google TV ab Android 9. Das konkrete Gerät wurde hier nicht geprüft.
2. Im vorhandenen Browser oder in der Download-App des Beamers `urlvanish.com/xgimi06` eingeben und die APK herunterladen.
3. Die APK öffnen. Falls Android nachfragt, der verwendeten Download-/Datei-App das Installieren dieser APK erlauben.
4. „Projektor TV“ aus der TV-Appübersicht starten. Bei einer vorhandenen Projektversion ist dieselbe Paketkennung und Signatur vorgesehen, sodass Android ein Update anbieten kann. Das tatsächliche Update auf dem Xgimi ist noch ungeprüft.

Das Repository oder Signatur-Secret musst du dafür nicht neu einrichten. Bei einem Signatur-/Installationsfehler nicht vorschnell die bisherige App löschen: Dadurch kann ihre lokale Website-Sitzung verloren gehen. Für die Diagnose reichen Modell, Android-Version und genauer Fehlertext; keine Passwörter senden.

## Bedienung dieses Stands

- Pfeile und OK wählen Menüpunkte, Reihen und Titelkarten aus.
- „Ansehen“ öffnet die Originaltitelseite und folgt einem dort tatsächlich vorhandenen Play-Link. Falls keine passende Schaltfläche erkannt wird, bleiben die Originalseite und ihre Episodenauswahl sichtbar.
- „Details, Folgen und Merkliste“ sowie „Mein Bereich“ öffnen die entsprechenden Originalansichten. Es gibt keine eigene Merkliste.
- Die Website-Leiste enthält „TV-Übersicht“, „Zurück“, „Inhalt bedienen“, „Suche“, „Konto“ und „Neu laden“.
- Die Menü-Taste fokussiert im Website-Modus diese Leiste, sofern deine Fernbedienung eine Menü-Taste besitzt. Zurück verlässt zuerst den Vollbildcontainer und folgt anschließend der Seitennavigation.
- Anmeldung erfolgt ausschließlich in den Originalformularen. Die App liest keine Passwortfelder aus.

Die Fernbedienbarkeit eines fremden Player-Iframes und die Bildschirmtastatur auf dem Xgimi sind noch offen. Eine Browserprüfseite wird unverändert angezeigt; diese Version umgeht sie nicht.

## Erster notwendiger Gerätetest

Zuerst einen Film und eine Serienepisode über den Originalplayer starten. Jeweils Bild/Ton, Pause/Fortsetzen, Spulen, Vollbild, Untertitel und Zurück prüfen. Erst danach Anmeldung, echte Merkliste, Verlauf und Fortsetzen nach App-Neustart abnehmen. Sobald eine Funktion scheitert, den genauen Bildschirmtext und das Xgimi-Modell festhalten; ein sichtbarer Katalog allein ist kein Wiedergabenachweis.

Optionales iPhone: Die [offizielle Google-TV-Anleitung](https://support.google.com/googletv/answer/11136134?co=GENIE.Platform%3DiOS&hl=de) beschreibt Pairing und Texteingabe. Ob dein Xgimi den erforderlichen Remote-Dienst bereitstellt, wurde nicht getestet. Die App setzt diese Smartphone-Steuerung nicht voraus.

[Prüfergebnisse](VERIFIED_0_6.md) · [Technischer Befund und Grenzen](SMARTTUBE_BINGEBANG_AUDIT.md)
