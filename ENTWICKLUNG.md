# 📘 Entwicklungsgeschichte – Leo's Messenger

Leo's Messenger wurde schrittweise zu einer vollständigen Android-SMS- und MMS-App entwickelt.  
Von Anfang an lag der Schwerpunkt auf **einfacher Bedienung**, **klarer Darstellung** und einer Funktion, die bei vielen Messenger-Apps fehlt:  
**Nachrichtentexte sollen frei markiert und auch nur teilweise kopiert werden können.**

Die folgende Übersicht zeigt die wichtigsten Entwicklungsschritte von **V1.1.0 bis V3.0.2**.

---

## 🎨 V1.1.x – Design und Kontakte

Mit der V1.1-Serie wurde die zunächst einfache SMS-Oberfläche deutlich erweitert.

### Wichtige Neuerungen
- verschiedene Farben für empfangene und gesendete Nachrichten
- eigene Farbeinstellungen für die Chat-Übersicht
- verschiedene Nachrichtendesigns
- frei markier- und kopierbare Nachrichtentexte
- verbesserte Kontaktzuordnung
- unbekannte Telefonnummern können als Kontakt übernommen werden
- Integration mit den Android-Kontakten
- erste Benachrichtigungs-, Ton- und Vibrationseinstellungen

In dieser Entwicklungsphase entstanden auch die charakteristischen Nachrichtenblasen mit den kleinen seitlichen Dreiecken.

---

## 🛠️ V1.2.x – Stabilität, Papierkorb und Bedienung

Die V1.2-Serie konzentrierte sich stärker auf Bedienkomfort und Stabilität.

### Hinzu kamen unter anderem
- zuverlässigere Zuordnung von Telefonnummern zu Kontakten
- Kennzeichnung ungelesener Nachrichten
- einstellbare Schriftgröße
- Löschen einzelner Nachrichten
- Wischgesten in der Chat-Übersicht
- eigener Papierkorb für Chats
- Wiederherstellen und endgültiges Löschen von Chats
- Sortierung nach Zeit oder Name

Ein Chat im Papierkorb kann bei Eingang einer neuen SMS desselben Absenders automatisch wieder in die aktive Chat-Liste übernommen werden.

---

## ✨ V1.3.x – Nachrichtenanimationen

Mit V1.3 wurde die Darstellung der Nachrichten deutlich dynamischer.

Ein Schwerpunkt war die Entwicklung einer eigenen Animation für neu eintreffende und gesendete Nachrichten.

Nach mehreren Entwicklungsstufen wurde die Nachrichtenanimation auf eine Overlay-Technik umgestellt. Dadurch konnten neue Nachrichten unabhängig von der eigentlichen Nachrichtenliste animiert und anschließend an ihrer endgültigen Position dargestellt werden.

Zusätzlich wurde die Löschanimation der Chat-Übersicht erweitert.

---

## 📩 V1.4.x – SMS-Empfang und Animationen

In der V1.4-Serie wurden insbesondere der SMS-Empfang im Hintergrund und die Animationen weiter verbessert.

### Wichtige Änderungen
- zuverlässigerer SMS-Empfang
- verbesserte Hintergrundverarbeitung
- Benachrichtigungston und Vibration
- Vermeidung doppelt empfangener Nachrichten
- neue Nachrichten werden während der Animation nicht mehr kurzzeitig doppelt angezeigt
- weitere Optimierungen der Bewegung des Nachrichtenverlaufs
- Stabilitätsverbesserungen bei der Chat-Übersicht

Ein wichtiger Teil dieser Entwicklungsphase war die Abstimmung zwischen dem tatsächlichen SMS-Empfang von Android und der sichtbaren Animation innerhalb der App.

---

## 🚀 V1.5.x – Neue Animationsarchitektur

Die V1.5-Serie brachte eine grundlegende Überarbeitung der Nachrichtenanimation.

Statt die Zielposition einer neuen Nachricht zu schätzen, ermittelt Leo's Messenger nun die tatsächliche Position der Nachricht innerhalb der Benutzeroberfläche.

### Dadurch konnten mehrere vorherige Probleme beseitigt werden
- neue Nachrichten landen an ihrer tatsächlichen Zielposition
- kein störender Sprung vom unteren zum oberen Bildschirmbereich
- gesendete Nachrichten werden animiert
- empfangene Nachrichten werden ebenfalls animiert
- der bestehende Nachrichtenverlauf bewegt sich passend zur neuen Nachricht
- Sende- und Empfangsanimation arbeiten mit derselben Oberfläche zusammen

Mit **V1.5.8** wurde diese neue Animationslogik zu einer stabilen Grundlage für die weitere Entwicklung.

### 💬 V1.5.9 – Chat-Übersicht

Die Löschanimation der Chat-Übersicht wurde erweitert.

Nach dem Wegfliegen eines gelöschten Chats wird der freigewordene Platz weich geschlossen. Die darunterliegenden Chats rücken dadurch flüssig nach oben, bevor der gelöschte Chat endgültig in den Papierkorb verschoben wird.

---

## 🔄 V1.6.0 – Animierte Chat-Sortierung

Mit V1.6.0 wurde auch die automatische Neusortierung der Chat-Übersicht animiert.

Trifft eine neue Nachricht ein, kann sich die Position eines Chats entsprechend der gewählten Sortierung verändern.  
Bei der Einstellung **„Zeit – neueste zuerst“** bewegt sich beispielsweise der Chat mit der neuen Nachricht nach oben, während die anderen Chats weich nach unten verschoben werden.

Damit werden nun nicht nur einzelne Nachrichten, sondern auch Veränderungen innerhalb der gesamten Chat-Übersicht animiert.

---

## 🧱 V1.6.1 – Stabilitätsupdate

Nach Einführung der animierten Chat-Sortierung in V1.6.0 wurde die Stabilität der Chat-Übersicht weiter verbessert.

### Wichtige Änderungen
- Absturz bei bestimmten älteren oder importierten Chats behoben
- doppelte Schlüssel in der Chat-Liste verhindert
- verbesserte Behandlung von Chats ohne eindeutig zugeordnete Telefonnummer
- animierte Neusortierung der Chat-Übersicht beibehalten
- weiche Löschanimation und automatisches Nachrücken der Chats beibehalten
- funktionierende Sende- und Empfangsanimationen unverändert übernommen

---

## 🧹 V1.6.2 – Codebereinigung

Mit V1.6.2 wurde der Quellcode von älteren Entwicklungs- und Testbestandteilen bereinigt.

### Wichtige Änderungen
- alte Test-Chats „Testkontakt“, „Familie“ und „Notizen“ entfernt
- nicht mehr benötigte Testfunktion `startChats()` entfernt
- alte, nicht mehr benötigte Migrationslogik entfernt
- bei einer Neuinstallation werden keine künstlichen Demo-Chats mehr angelegt
- bestehende gespeicherte Chats und Einstellungen bleiben bei einem Update erhalten
- keine Änderungen an der bewährten SMS-, Animations-, Papierkorb-, Sortier- und Kontaktlogik

V1.6.2 konzentriert sich damit bewusst auf die Bereinigung des Quellcodes, ohne die bereits funktionierenden Kernfunktionen von Leo's Messenger zu verändern.

---

## 📱 V1.7.0 – Letzter SMS-Stand vor MMS

V1.7.0 bildete den letzten Entwicklungsstand, bevor Leo's Messenger mit Version 2.0 um MMS erweitert wurde.

Die vorhandenen **SMS-, Kontakt-, Sortier-, Papierkorb- und Darstellungsfunktionen** blieben dabei die Grundlage für den nächsten großen Entwicklungsschritt.

---

## 🖼️ V2.0.0 – Start der MMS-Erweiterung

Mit V2.0.0 begann der Ausbau von Leo's Messenger von einer reinen SMS-App zu einer **SMS- und MMS-App**.

### Wichtige Erweiterungen
- grundlegende MMS-Unterstützung ergänzt
- Bildanhänge können ausgewählt werden
- Bilder werden zusammen mit Text als MMS verarbeitet
- empfangene MMS werden in bestehende Chats integriert
- SMS und MMS erscheinen gemeinsam im Chatverlauf
- Bilddarstellung innerhalb der Nachrichtenansicht ergänzt
- eigene MMS-Verarbeitung in `MmsSupport.kt` ausgelagert

Der Sprung auf die Hauptversion **2.0** kennzeichnet damit die größte funktionale Erweiterung seit Beginn der Entwicklung.

---

## 📤 V2.0.1 – MMS-Versand und Status

Die MMS-Funktion wurde technisch weiter ausgebaut und robuster gemacht.

### Wichtige Änderungen
- verbesserte Auswahl von Bildanhängen
- Bildvorschau vor dem Versand
- Bilder werden für den MMS-Versand verkleinert
- MMS-Sendestatus ergänzt
- Rückmeldung bei erfolgreichem oder fehlgeschlagenem Versand
- Behandlung der benötigten Bild- und Dateizugriffe verbessert

---

## ⌨️ V2.0.2 – Anpassung der Eingabe bei geöffneter Tastatur

Die Eingabezeile wurde an das Verhalten moderner Android-Geräte angepasst.

### Wichtige Änderungen
- problematisches zusätzliches IME-Padding entfernt
- Höhe des Texteingabefeldes begrenzt
- Eingabe auf wenige Zeilen beschränkt
- bessere Platzaufteilung bei eingeblendeter Bildschirmtastatur

---

## 📏 V2.0.3 – Kompaktere Eingabezeile

Die untere Eingabezeile wurde weiter verkleinert, damit mehr Platz für den eigentlichen Nachrichtenverlauf bleibt.

### Wichtige Änderungen
- kleinere Mindest- und Maximalhöhe des Textfeldes
- kompaktere Außenabstände
- kleinere Bildvorschau
- kompaktere Schaltflächen für Anhang und Senden

---

## 🧩 V2.0.4 – Weitere Optimierung der Texteingabe

Die Eingabezeile wurde nochmals überarbeitet und platzsparender gestaltet.

### Wichtige Änderungen
- einzeilige, kompaktere Texteingabe
- kleinere Bedienelemente
- reduzierte Abstände
- geringerer Platzverbrauch im unteren Bildschirmbereich

---

## 🌟 V2.0.5 – MMS und kompakte Oberfläche

Mit V2.0.5 wurde die MMS-Erweiterung als zusammenhängender Entwicklungsstand veröffentlicht.

### Neu und verbessert
- MMS-Unterstützung mit Bildanhängen
- Bilder können ausgewählt und als MMS versendet werden
- empfangene MMS mit Bildern werden im Chat angezeigt
- Bildvorschau vor dem Versenden
- Bilder können im Chat größer geöffnet werden
- SMS und MMS werden gemeinsam im selben Chatverlauf dargestellt
- MMS-Sendestatus integriert
- Bildauswahl direkt über die Eingabeleiste
- Eingabezeile deutlich kompakter gestaltet
- weniger Platzverbrauch im unteren Bildschirmbereich
- SMS-Funktionen bleiben weiterhin vollständig erhalten

---

## 📐 V2.0.6 – Display-Randabstände

Die Eingabezeile wurde für Geräte mit abgerundeten Displayrändern angepasst.

### Wichtige Änderungen
- zusätzlicher Sicherheitsabstand links und rechts
- Büroklammer und Sende-Pfeil werden nicht mehr so leicht vom Displayrand abgeschnitten
- kompakte Eingabeleiste bleibt erhalten

---

## 🎞️ V2.0.7 – Gboard-Rich-Content

Leo's Messenger wurde um die direkte Übergabe von Medieninhalten aus Gboard erweitert.

### Wichtige Änderungen
- Vorbereitung für GIFs, Sticker und Bilder aus Gboard
- Bildinhalte können als MMS-Anhang übernommen werden
- unterstützte Bild-MIME-Typen wurden ergänzt

---

## 🧩 V2.0.8 – Android Receive Content

Die Rich-Content-Unterstützung wurde auf die AndroidX-Receive-Content-API umgestellt.

### Wichtige Änderungen
- `image/gif`, `image/webp`, `image/png`, `image/jpeg` und `image/*`
- verbesserte Übergabe von Bildern, GIFs und Stickern
- weitere Integration in die MMS-Verarbeitung

---

## 🛠️ V2.0.9 – Stabilisierung des Rich-Content-Eingabefeldes

Ein Absturz beim Erzeugen des neuen Eingabefeldes wurde behoben.

### Wichtige Änderungen
- Initialisierung des Rich-Content-Eingabefeldes korrigiert
- Textänderungs-Callback stabilisiert
- GIF-/Sticker-/Bild-Unterstützung bleibt erhalten

---

## ⌨️ V2.0.10 – Tastatur- und Safe-Drawing-Anpassung

Die Eingabezeile wurde weiter an die Android-Bildschirmtastatur angepasst.

### Wichtige Änderungen
- zusätzliche Safe-Drawing-Ränder
- bessere Berücksichtigung der Bildschirmtastatur
- weniger Abschneiden der Eingabezeile bei geöffnetem Gboard

---

## 🖼️ V2.0.11 – Erweiterte Gboard-Erkennung

Die unterstützten Medienformate werden Gboard nun bei der Eingabeverbindung ausdrücklich gemeldet.

### Wichtige Änderungen
- MIME-Typen werden über die InputConnection bereitgestellt
- Gboard kann GIFs, Sticker und Bilder besser als unterstützten Inhalt erkennen
- MMS-Übergabe bleibt erhalten

---

## 📱 V2.0.12 – IME-Verhalten überarbeitet

Das Verhalten der Eingabezeile beim Öffnen der Bildschirmtastatur wurde weiter überarbeitet.

### Wichtige Änderungen
- unerwünschter großer Leerraum beim Fokussieren des Textfeldes reduziert
- Tastatur- und Fensterverhalten angepasst
- Rich-Content-Funktionen bleiben erhalten

---

## ⬆️ V2.0.13 – Chatbereich über der Tastatur

Der Chatbereich wurde so angepasst, dass er sich bei geöffneter Bildschirmtastatur nach oben verschiebt.

### Wichtige Änderungen
- Chatverlauf bleibt oberhalb von Gboard sichtbar
- Eingabezeile bleibt direkt über der Tastatur
- automatisches Scrollen zur letzten Nachricht ergänzt

---

## ℹ️ V2.0.14 – Versionsanzeige und IME-Korrekturen

Neben weiteren Tastaturkorrekturen wurde die Versionsanzeige im Menü überarbeitet.

### Wichtige Änderungen
- aktuelle App-Version sollte automatisch angezeigt werden
- Tastaturhöhe und Chatbereich besser aufeinander abgestimmt
- weiteres Feintuning des Nachrichtenverlaufs

---

## 🧱 V2.0.15 – BuildConfig-Unterstützung

Für die automatische Versionsanzeige wurde zunächst die Erzeugung von `BuildConfig` aktiviert.

### Wichtige Änderungen
- `buildConfig = true`
- Versionsanzeige im Info-Bereich vorbereitet

---

## 🔧 V2.0.16 – Versionsanzeige ohne BuildConfig

Da `BuildConfig` im Projekt weiterhin Probleme verursachte, wurde die Versionsabfrage auf den Android-PackageManager umgestellt.

### Wichtige Änderungen
- Version wird direkt aus der installierten App gelesen
- weniger Abhängigkeit von generiertem Build-Code
- Tastatur- und MMS-Funktionen bleiben unverändert erhalten

---

## ✅ V2.0.17 – Versionsanzeige vollständig korrigiert

Der letzte verbliebene `BuildConfig`-Verweis wurde entfernt.

### Wichtige Änderungen
- keine `BuildConfig`-Abhängigkeit mehr
- Menü und Info-Dialog lesen die installierte Versionsnummer direkt aus
- Versionsanzeige arbeitet wieder zuverlässig

---

## 🎬 V2.0.18 – Flüssigere Tastaturbewegung

Die Bewegung des Chatbereichs beim Öffnen der Bildschirmtastatur wurde animiert.

### Wichtige Änderungen
- weichere Bewegung der Eingabezeile
- Chatbereich wird gleichzeitig verkleinert
- Übergang beim Öffnen von Gboard verbessert

---

## 🚀 V2.0.19 – Synchronisierte Gboard-/IME-Animation

Mit V2.0.19 wurde die Tastaturbewegung nochmals grundlegend verbessert.

### Neu und verbessert
- Chat und Eingabezeile folgen direkt der tatsächlichen Android-/Gboard-IME-Animation
- eigener zeitgesteuerter Tastatur-Tween entfernt
- flüssigere Synchronisierung zwischen Tastatur, Eingabezeile und Nachrichtenverlauf
- Chatbereich bleibt oberhalb der Tastatur sichtbar
- automatisches Scrollen zur letzten Nachricht bleibt erhalten
- SMS-, MMS-, GIF-, Sticker- und Bildfunktionen bleiben vollständig erhalten
- Versionsanzeige bleibt dynamisch und zuverlässig

**V2.0.19 bildete den Abschluss der 2.0.19-Entwicklungsstufe und die Grundlage für die folgenden Optimierungen bis Version 3.0.2.**

---

## 🔙 V2.0.20 – Tastatur beim Verlassen des Chats

Beim Wechsel aus einem geöffneten Chat zurück in die Chatübersicht blieb die Bildschirmtastatur teilweise geöffnet.

### Wichtige Änderungen
- Tastatur wird beim Verlassen des Chats sofort geschlossen
- Fokus wird aus dem Texteingabefeld entfernt
- Gboard bleibt nicht mehr über der Chatübersicht stehen

---

## ⚙️ V2.0.21 – MMS-, Vibrations- und Benachrichtigungseinstellungen

Das Hauptmenü wurde um zusätzliche Einstellungen für MMS und Benachrichtigungen erweitert.

### Wichtige Änderungen
- MMS im Hauptmenü ein- und ausschaltbar
- Warnhinweis beim Aktivieren von MMS wegen möglicher Netzbetreiberkosten
- Vibration direkt im Hauptmenü ein- und ausschaltbar
- eigener Bereich für Benachrichtigungseinstellungen
- direkter Zugriff auf die Android-Benachrichtigungseinstellungen
- fertige Texte können über die Android-Funktion „Teilen“ an Leo's Messenger übergeben werden
- bei deaktiviertem MMS werden Bildanhänge blockiert

---

## 🧩 V2.0.22 – Kompatibilitätskorrektur im Hauptmenü

Ein Buildfehler in der verwendeten Material3-Version wurde behoben.

### Wichtige Änderungen
- nicht unterstützter Parameter `trailingContent` entfernt
- MMS- und Vibrationsschalter direkt in die Menüzeile integriert
- alle Funktionen aus V2.0.21 beibehalten

---

## 🚀 V3.0.0 – Neue Hauptversion

Mit V3.0.0 wurde Leo's Messenger strukturell weiter ausgebaut.

### Neu und verbessert
- Hauptmenü neu gegliedert
- Bereiche für Nachrichten, Benachrichtigungen & MMS, Verwaltung sowie Hilfe & Info
- Test-Benachrichtigung ergänzt
- Info-Bereich erweitert
- Anzeige, ob Leo's Messenger als Standard-SMS-App eingerichtet ist
- Anzeige des MMS-, Vibrations- und Benachrichtigungsstatus
- Übergabe fertiger Texte aus anderen Apps bleibt erhalten
- bestehende SMS-, MMS-, Gboard- und Rich-Content-Funktionen bleiben vollständig erhalten

Der Sprung auf **V3.0.0** kennzeichnet damit eine neue Entwicklungsstufe mit stärkerem Schwerpunkt auf Einstellungen, Bedienkomfort und Systemintegration.

---

## ⌨️ V3.0.1 – Chatbereich vollständig über der Tastatur

Die Anpassung des Chatverlaufs an die Bildschirmtastatur wurde grundlegend vereinfacht.

### Wichtige Änderungen
- Android übernimmt das Verkleinern des Chatfensters über `adjustResize`
- bisherige manuelle IME-Verschiebung entfernt
- der sichtbare Chatbereich wird beim Öffnen von Gboard vollständig nach oben geschoben
- Eingabezeile bleibt oberhalb der Tastatur
- automatisches Scrollen zur letzten Nachricht bleibt erhalten
- dadurch weniger Überlagerungen und weniger verdeckte Nachrichten

---

## ✅ V3.0.2 – MMS-Steuerung und Kostenhinweis

V3.0.2 konzentriert sich auf eine klarere MMS-Steuerung und eine übersichtlichere Bedienung.

### Neu und verbessert
- Menüpunkt für Testnachrichten wieder entfernt
- MMS-Schalter im Hauptmenü weiter überarbeitet
- Büroklammer bei deaktiviertem MMS sichtbar ausgegraut
- Bild-, GIF- und Sticker-Funktionen aus Gboard an den MMS-Status gekoppelt
- bei ausgeschaltetem MMS werden Bildanhänge nicht versendet
- MMS-Kostenhinweis wird beim erstmaligen Aktivieren angezeigt
- spätere Aktivierungen erfolgen ohne erneute Warnung
- eigener Menüpunkt **„MMS-Kostenhinweis erneut anzeigen“**
- Verbesserungen bei Tastatur- und Chatdarstellung aus V3.0.1 bleiben erhalten
- alle bewährten SMS-, MMS-, Kontakt-, Papierkorb- und Sortierfunktionen bleiben erhalten

**V3.0.2 bildet derzeit den aktuellen stabilen Entwicklungsstand von Leo's Messenger.**

---

## 🔮 Weiterentwicklung

Leo's Messenger wird weiterhin schrittweise entwickelt.

Dabei gilt ein wichtiges Prinzip:

> Neue Funktionen sollen bestehende und bereits zuverlässig funktionierende Funktionen nicht beeinträchtigen.

Neue Funktionen werden deshalb in kleinen Versionsschritten integriert und getestet.  
Die Entwicklung erfolgt mit **Kotlin und Jetpack Compose für Android**.

---

## 👨‍💻 Projektinformationen

**Entwickler:** Leo  
**Projekt:** Leo's Messenger  
**Website:** leos-net.de
