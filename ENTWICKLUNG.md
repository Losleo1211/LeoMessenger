# 📘 Entwicklungsgeschichte – Leo's Messenger

Leo's Messenger wurde schrittweise zu einer vollständigen Android-SMS- und MMS-App entwickelt.  
Von Anfang an lag der Schwerpunkt auf **einfacher Bedienung**, **klarer Darstellung** und einer Funktion, die bei vielen Messenger-Apps fehlt:  
**Nachrichtentexte sollen frei markiert und auch nur teilweise kopiert werden können.**

Die folgende Übersicht zeigt die wichtigsten Entwicklungsschritte von **V1.1.0 bis V2.0.5**.

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

V2.0.5 ist damit der aktuelle Entwicklungsstand von Leo's Messenger und verbindet die bisherigen SMS-Funktionen erstmals mit einer umfassenden MMS-Erweiterung.

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