# 💬 Leo's Messenger

**Leo's Messenger** ist ein schlanker SMS-Messenger für Android mit besonderem Fokus auf einfache Bedienung, Datenschutz und frei markierbare Nachrichtentexte.

Ein wesentlicher Grund für die Entwicklung war eine Funktion, die bei vielen Messenger- und SMS-Apps fehlt oder nur umständlich möglich ist:

> **Beliebige Textstellen innerhalb einer SMS markieren und kopieren.**

Leo's Messenger wird unabhängig entwickelt und der Quellcode ist öffentlich einsehbar.

---

## 📱 Aktuelle Version

**V1.6.1**

Leo's Messenger wird laufend weiterentwickelt und auf realen Android-Geräten getestet.

---

## ✨ Funktionen

### 💬 SMS & Nachrichten

- SMS senden und empfangen
- Verwendung als Standard-SMS-App unter Android
- Vorhandenen SMS-Verlauf vom Smartphone einlesen
- Automatische Synchronisierung neuer Nachrichten
- Empfang von SMS auch im Hintergrund
- Benachrichtigungen bei neuen Nachrichten
- Benachrichtigungston und Vibration
- Vorschau eingehender Nachrichten
- Ungelesene Chats werden deutlich hervorgehoben
- Telefonnummern werden vorhandenen Kontakten zugeordnet

### 📋 Texte markieren und kopieren

Eine der Hauptfunktionen von Leo's Messenger:

**Beliebige Teile einer Nachricht können frei markiert und kopiert werden.**

Es muss nicht die komplette SMS kopiert werden. Telefonnummern, Adressen, Codes, Namen oder einzelne Sätze können direkt ausgewählt werden.

### 🎨 Darstellung

- Standard-Design
- Sprechblasen-Design
- Unterschiedliche Farben für gesendete und empfangene Nachrichten
- Eigene Farbe für die Chatübersicht
- Automatische Anpassung der Textfarbe an helle und dunkle Hintergründe
- Einstellbare Schriftgröße
- Zeitangaben direkt neben den Nachrichten
- Statusanzeige für gesendete Nachrichten
- Animation beim Senden und Empfangen
- Animiertes Löschen von Chats

### 📂 Chatverwaltung

- Chats durchsuchen
- Chats umbenennen
- Kontakte direkt aus Telefonnummern erstellen
- Telefonnummer direkt aus einem Chat anrufen
- Chatübersicht sortieren nach:
  - Neueste zuerst
  - Älteste zuerst
  - Name A–Z
  - Name Z–A
- Einzelne Nachrichten löschen
- Chats in den Papierkorb verschieben
- Chats aus dem Papierkorb wiederherstellen
- Endgültiges Löschen aus dem Papierkorb

Wird von einem Absender erneut eine SMS empfangen, dessen Chat sich noch im Papierkorb befindet, kann der bisherige Nachrichtenverlauf wieder dem aktiven Chat zugeordnet werden.

---

## 🎯 Warum Leo's Messenger?

Bei verschiedenen SMS-Apps ist es nicht oder nur umständlich möglich, einen bestimmten Teil einer Nachricht zu markieren und zu kopieren.

Leo's Messenger wurde deshalb ursprünglich mit einem ganz einfachen Ziel entwickelt:

**Nachricht öffnen → gewünschten Text markieren → kopieren.**

Aus dieser Idee ist inzwischen ein vollständiger SMS-Messenger mit eigener Chatverwaltung, Designs, Benachrichtigungen und weiteren Komfortfunktionen entstanden.

---

## 🔍 Quellcode & Sicherheit

Der Quellcode von **Leo's Messenger ist öffentlich einsehbar**.

Damit können Anwender, Entwickler und Sicherheitsforscher nachvollziehen, welche Funktionen die App ausführt und welche Android-Berechtigungen dafür verwendet werden.

Leo's Messenger enthält:

- **keine Werbung**
- **kein integriertes Werbe-Tracking**
- **keine absichtlich integrierten Schadfunktionen**

### ⚠️ Hinweis zu Sicherheitsprogrammen

Leo's Messenger benötigt als SMS-App weitreichende Android-Berechtigungen, unter anderem zum **Lesen, Empfangen und Senden von SMS** sowie für die **Zuordnung von Telefonnummern zu Kontakten**.

Da die App derzeit als APK außerhalb des Google Play Stores bereitgestellt wird und diese sensiblen Berechtigungen benötigt, können einzelne Sicherheitsprogramme die App möglicherweise als verdächtig einstufen.

Eine solche Warnung sollte anhand der jeweiligen Erkennungsdetails und des hier veröffentlichten Quellcodes überprüft werden.

---

## 🔐 Datenschutz

Leo's Messenger ist als klassischer SMS-Messenger konzipiert.

SMS werden über die SMS-Funktionen des Smartphones gesendet und empfangen. Für diese Funktionen benötigt die App entsprechende Android-Systemberechtigungen.

Chat- und Einstellungsdaten werden lokal auf dem Gerät verwaltet.

Der öffentlich zugängliche Quellcode ermöglicht eine unabhängige Überprüfung der implementierten Funktionen.

---

## 🔑 Benötigte Android-Berechtigungen

Abhängig von Android-Version und verwendeten Funktionen benötigt Leo's Messenger unter anderem:

- SMS senden
- SMS empfangen
- SMS lesen
- Kontakte lesen
- Benachrichtigungen anzeigen
- MMS-/WAP-Push-Funktionen für die Einbindung als Standard-SMS-App

Die Berechtigungen dienen den entsprechenden Messenger-Funktionen.

---

## 🛠️ Entwicklung

Leo's Messenger wird entwickelt mit:

- **Android**
- **Kotlin**
- **Jetpack Compose**
- **Android Studio**

Getestet wird die App unter anderem auf einem **vivo X300**.

---

## 📦 Installation

Fertige Versionen werden im Bereich **Releases** als APK bereitgestellt.

Da die APK außerhalb des Google Play Stores installiert wird, muss Android gegebenenfalls die Installation aus der verwendeten Quelle erlauben.

Nach der Installation kann Leo's Messenger als Standard-SMS-App ausgewählt werden.

---

## 🧪 Projektstatus

Leo's Messenger befindet sich in aktiver Entwicklung.

Fehlerberichte und Hinweise sind willkommen. Für Fehler und Verbesserungsvorschläge kann der **Issues-Bereich** dieses GitHub-Repositories verwendet werden.

---

## ⚖️ Haftungsausschluss

Die Nutzung von Leo's Messenger erfolgt auf eigene Verantwortung.

Trotz sorgfältiger Entwicklung und Tests kann keine Gewähr für Fehlerfreiheit, ständige Verfügbarkeit oder vollständige Kompatibilität mit allen Android-Geräten und Android-Versionen übernommen werden.

Für Datenverluste, entgangene Nachrichten oder sonstige unmittelbare oder mittelbare Schäden, die durch die Verwendung der Software entstehen, wird – soweit gesetzlich zulässig – keine Haftung übernommen.

---

## 👨‍💻 Entwickler

**Leo / Leos-Net**

Smart-Home- und Technik-Enthusiast  
Android · Raspberry Pi · ioBroker · Smart Home

---

⭐ Wenn dir Leo's Messenger gefällt, kannst du das Projekt auf GitHub mit einem **Star** unterstützen.

leos-net.de
