# Play Console — Data-Safety-Formular (Vorausfüllung)

*Stand 2026-07-19 — aus dem Code verifiziert (Manifest + Netzwerk-Call-Sites).
Gilt für den künftigen `store`-Flavor; der `sovereign`-Sideload-Build ist
nicht Play-gebunden.*

## Kernaussagen

- **Werden Daten erhoben (collected)?** Nein — keine Daten verlassen das
  Gerät Richtung Entwickler. „Collected" im Play-Sinn = Übertragung an den
  Entwickler/Dritte durch die App. JARVIS überträgt nichts an den Entwickler.
- **Werden Daten geteilt (shared)?** Nein im Standardzustand. Nutzer-initiierte
  Anfragen an Dritt-Dienste (OFF-Barcode, Open-Meteo, eigene ICS-Feeds,
  WebUntis, GoCardless) sind „user-initiated actions" — im Formular als
  optionale Features mit eigener Erklärung dokumentieren, nicht als Sharing
  durch die App deklarationspflichtig, ABER: konservativ deklarieren wir:
  - *Approximate location* → Open-Meteo (optional, Wetter-Hydration), nicht
    verknüpft mit Identität, kein Tracking, abschaltbar.
  - *Financial info* → nur bei aktiv verbundenem Open Banking; Verarbeitung
    on-device; Auth läuft beim Anbieter.
- **Verschlüsselung in transit?** Ja (alle optionalen Calls über HTTPS).
- **Löschung?** Vollständig durch App-Deinstallation bzw. In-App-Wipe;
  Backups liegen ausschließlich beim Nutzer.
- **Unabhängige Sicherheitsprüfung?** Nein (ehrlich ankreuzen).

## Deklarationen je Datentyp (Formular-Matrix)

| Play-Kategorie | Erhoben? | Geteilt? | Anmerkung |
|---|---|---|---|
| Health & Fitness | Nein* | Nein | on-device; Health-Connect-Reads bleiben lokal |
| Financial info | Nein* | Nein | Eingaben + optionaler Bank-Import bleiben lokal |
| Location (approx.) | Nein* | optional an Open-Meteo | nur wenn Wetter-Feature an; ohne Identität |
| Personal info | Nein | Nein | kein Account, kein Name-Pflichtfeld |
| Photos | Nein* | Nein | Fortschrittsfotos privat on-device |
| App activity / Device IDs | Nein | Nein | keine Analytics/Ads-SDKs (verifiziert: keine) |

\* = „processed ephemerally / on device" — im Formular über die
On-device-Processing-Erklärungen abbilden.

## Zusatz-Deklarationen

- **AccessibilityService-Formular**: Zweck = Enforcement der vom Nutzer
  definierten App-Limits (Focus Guard); liest NUR Window-State-Events, nie
  Inhalte; Video-Nachweis beilegen. Ablehnungsrisiko real → Fallback-Plan:
  Store-Flavor ohne A11y-Instant-Lock (UsageStats-Poll-Modus).
- **PACKAGE_USAGE_STATS**: Declared-Use „app usage tracking for user-set
  screen-time limits".
- **QUERY_ALL_PACKAGES**: im Store-Flavor ENTFERNEN → `<queries>`-Filter auf
  Launcher-Intents reicht für den App-Picker.
- **Health Connect**: Datentypen einzeln deklarieren (sleep, steps, HR,
  weight); Begründung „personal health dashboard, on-device".
- **IARC**: Casino-Minigame = simuliertes Glücksspiel ohne Echtgeld → in der
  IARC-Abfrage wahrheitsgemäß angeben (führt je nach Region zu 12+/16+) ODER
  Casino im Store-Flavor deaktivieren (empfohlen für die erste Einreichung —
  geringeres Alters-Rating und null Diskussionsfläche).
