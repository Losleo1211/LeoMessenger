# Entwicklungsgeschichte – Leo's Messenger
Leo's Messenger wurde schrittweise zu einer vollständigen Android-SMS-App entwickelt. Der Schwerpunkt lag von Anfang an auf einer einfachen Bedienung und einer Funktion, die bei vielen Messenger-Apps fehlt: Nachrichtentexte sollen frei markiert und auch nur teilweise kopiert werden können.
Die folgende Übersicht zeigt die wichtigsten Entwicklungsschritte von V1.1.0 bis V1.6.0.

## V1.1.x – Design und Kontakte
Mit der V1.1-Serie wurde die zunächst einfache SMS-Oberfläche deutlich erweitert.
Wichtige Neuerungen:
- verschiedene Farben für empfangene und gesendete Nachrichten
- eigene Farbeinstellungen für die Chat-Übersicht
- verschiedene Nachrichtendesigns
- frei markier- und kopierbare Nachrichtentexte
- verbesserte Kontaktzuordnung
- unbekannte Telefonnummern können als Kontakt übernommen werden
- Integration mit den Android-Kontakten
- erste Benachrichtigungs-, Ton- und Vibrationseinstellungen
In dieser Entwicklungsphase entstanden auch die charakteristischen Nachrichtenblasen mit den kleinen seitlichen Dreiecken.

## V1.2.x – Stabilität, Papierkorb und Bedienung
Die V1.2-Serie konzentrierte sich stärker auf Bedienkomfort und Stabilität.
Hinzu kamen unter anderem:
- zuverlässigere Zuordnung von Telefonnummern zu Kontakten
- Kennzeichnung ungelesener Nachrichten
- einstellbare Schriftgröße
- Löschen einzelner Nachrichten
- Wischgesten in der Chat-Übersicht
- eigener Papierkorb für Chats
- Wiederherstellen und endgültiges Löschen von Chats
- Sortierung nach Zeit oder Name
Ein Chat im Papierkorb kann bei Eingang einer neuen SMS desselben Absenders automatisch wieder in die aktive Chat-Liste übernommen werden.

## V1.3.x – Nachrichtenanimationen
Mit V1.3 wurde die Darstellung der Nachrichten deutlich dynamischer.
Ein Schwerpunkt war die Entwicklung einer eigenen Animation für neu eintreffende und gesendete Nachrichten.
Nach mehreren Entwicklungsstufen wurde die Nachrichtenanimation auf eine Overlay-Technik umgestellt. Dadurch konnten neue Nachrichten unabhängig von der eigentlichen Nachrichtenliste animiert und anschließend an ihrer endgültigen Position dargestellt werden.
Zusätzlich wurde die Löschanimation der Chat-Übersicht erweitert.

## V1.4.x – SMS-Empfang und Animationen
In der V1.4-Serie wurden insbesondere der SMS-Empfang im Hintergrund und die Animationen weiter verbessert.
Wichtige Änderungen:
- zuverlässigerer SMS-Empfang
- verbesserte Hintergrundverarbeitung
- Benachrichtigungston und Vibration
- Vermeidung doppelt empfangener Nachrichten
- neue Nachrichten werden während der Animation nicht mehr kurzzeitig doppelt angezeigt
- weitere Optimierungen der Bewegung des Nachrichtenverlaufs
- Stabilitätsverbesserungen bei der Chat-Übersicht
Ein wichtiger Teil dieser Entwicklungsphase war die Abstimmung zwischen dem tatsächlichen SMS-Empfang von Android und der sichtbaren Animation innerhalb der App.

## V1.5.x – Neue Animationsarchitektur
Die V1.5-Serie brachte eine grundlegende Überarbeitung der Nachrichtenanimation.
Statt die Zielposition einer neuen Nachricht zu schätzen, ermittelt Leo's Messenger nun die tatsächliche Position der Nachricht innerhalb der Benutzeroberfläche.
Dadurch konnten mehrere vorherige Probleme beseitigt werden:
- neue Nachrichten landen an ihrer tatsächlichen Zielposition
- kein störender Sprung vom unteren zum oberen Bildschirmbereich
- gesendete Nachrichten werden animiert
- empfangene Nachrichten werden ebenfalls animiert
- der bestehende Nachrichtenverlauf bewegt sich passend zur neuen Nachricht
- Sende- und Empfangsanimation arbeiten mit derselben Oberfläche zusammen
Mit V1.5.8 wurde diese neue Animationslogik zu einer stabilen Grundlage für die weitere Entwicklung.

### V1.5.9 – Chat-Übersicht
Die Löschanimation der Chat-Übersicht wurde erweitert.
Nach dem Wegfliegen eines gelöschten Chats wird der freigewordene Platz weich geschlossen. Die darunterliegenden Chats rücken dadurch flüssig nach oben, bevor der gelöschte Chat endgültig in den Papierkorb verschoben wird.

## V1.6.0 – Animierte Chat-Sortierung
Mit V1.6.0 wurde auch die automatische Neusortierung der Chat-Übersicht animiert.
Trifft eine neue Nachricht ein, kann sich die Position eines Chats entsprechend der gewählten Sortierung verändern.
Bei der Einstellung **„Zeit – neueste zuerst“** bewegt sich beispielsweise der Chat mit der neuen Nachricht nach oben, während die anderen Chats weich nach unten verschoben werden.
Damit werden nun nicht nur einzelne Nachrichten, sondern auch Veränderungen innerhalb der gesamten Chat-Übersicht animiert.


## V1.6.1 – Stabilitätsupdate
Nach Einführung der animierten Chat-Sortierung in V1.6.0 wurde die Stabilität der Chat-Übersicht weiter verbessert.
Wichtige Änderungen:
- Absturz bei bestimmten älteren oder importierten Chats behoben
- doppelte Schlüssel in der Chat-Liste verhindert
- verbesserte Behandlung von Chats ohne eindeutig zugeordnete Telefonnummer
- animierte Neusortierung der Chat-Übersicht beibehalten
- weiche Löschanimation und automatisches Nachrücken der Chats beibehalten
- funktionierende Sende- und Empfangsanimationen unverändert übernommen

**V1.6.1 bildet damit die aktuelle stabile Grundlage von Leo's Messenger.**
## Weiterentwicklung
Leo's Messenger wird weiterhin schrittweise entwickelt.
Dabei gilt ein wichtiges Prinzip:
> Neue Funktionen sollen bestehende und bereits zuverlässig funktionierende Funktionen nicht beeinträchtigen.
Neue Funktionen werden deshalb in kleinen Versionsschritten integriert und getestet.
Die Entwicklung erfolgt mit **Kotlin und Jetpack Compose für Android**.

**Entwickler:** Leo  
**Projekt:** Leo's Messenger  
**Website:** leos-net.de
