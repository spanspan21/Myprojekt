# THE HOUSE OF TIME — Casino Unlock for JARVIS Guard
### Vollständiges Ideen-, Design- und Implementierungs-Dossier
**Version 1.0 · 2026-07-13 · ~50 Seiten**

---

## Teil 0 — Inhalt

1. Mandat & Vision
2. Ethik & Leitplanken
3. Die Verhaltenspsychologie dahinter
4. Zeit-Ökonomie I: Die Währung
5. Zeit-Ökonomie II: Odds, Payouts, Expected Value
6. Die fünf Mechanik-Familien (Prototyp-Erkenntnisse)
7. Ideen-Katalog A — Spiele & Wetten (mit Verdicts)
8. Ideen-Katalog B — Meta-Systeme (mit Verdicts)
9. Ideen-Katalog C — Anti-Missbrauch & Schutz (mit Verdicts)
10. Design-Sprache: Was "clean wie Stake" konkret heißt
11. Die fünf Design-Optionen im Vergleich (Entscheidung)
12. Screen-Spezifikation: Die Zustandsmaschine des Overlays
13. Blackjack-Spezifikation
14. Roulette-Spezifikation
15. Suspense-Design: Die Kunst der Verzögerung
16. Sound & Haptik
17. Software-Architektur
18. State, Persistenz & Crash-Sicherheit ("resolve-then-animate")
19. Anti-Cheat & Edge-Cases
20. Settings-Spezifikation
21. Statistik & Dashboard-Anbindung
22. Testplan
23. Performance & Batterie
24. Rollout & Defaults
25. Zukunfts-Roadmap
26. Anhang A: Vollständige Payout-/EV-Tabellen
27. Anhang B: Copy-Katalog (alle UI-Texte)
28. Anhang C: Motion-Token-Tabelle
29. Entscheidungs-Q&A

---

## 1 · Mandat & Vision

**Der Wunsch (wörtlich):** Wenn eine App gesperrt ist, soll man um die Freischaltung
spielen können — Blackjack oder Roulette, frei wählbar. Bei Roulette auch Zahlen-Wetten.
Der Einsatz sind Minuten, die Höhe frei wählbar. In der App einstellbar: wie lange die
Pause ist, wenn das Limit erreicht ist, wie hoch das Limit ist, etc. Das Overlay soll
sehr clean sein mit sauberen Animationen. Das Ergebnis (Gewinn/Verlust) kommt nicht
sofort, sondern mit kleiner Verzögerung. Funktional auf dem Niveau einer echten
Gambling-App wie Stake. Optional — als eine Guard-Option, die man nicht nehmen muss.

**Die Vision in einem Satz:** *Das Haus gewinnt immer — und das Haus bist du.*

JARVIS Guard sperrt Apps bereits hart (Tageslimit, Budgets, Fokus-Fenster). Casino
Unlock macht aus der härtesten Stelle des Systems — dem Moment, in dem das Limit
zuschlägt und man trotzdem rein will — ein Spiel mit echten, selbst gewählten
Konsequenzen. Statt zu snoozen (was sich wie Betrug anfühlt) oder aufzugeben (was
sich wie Strafe anfühlt), tritt man gegen das Haus an:

- **Gewinn** → sofortige, ehrlich erspielte Extra-Minuten für genau diese App, heute.
- **Verlust** → die App bleibt nicht nur zu, sie ist *zusätzlich* gesperrt — die Pause,
  die man vorher selbst konfiguriert hat.

Der Clou: Da jede Casino-Wette einen negativen Erwartungswert hat (House Edge), ist
das System **mathematisch ein Commitment-Device**. Wer regelmäßig spielt, verliert
netto Bildschirmzeit — freiwillig, mit Nervenkitzel, und ohne dass es sich nach
Bestrafung anfühlt. Die Alternative (nicht spielen) ist immer die rationale; das
Spiel ist der emotionale Ausweg mit eingebautem Preis. Genau das macht es zur
*besseren* Snooze-Funktion: Der bestehende Snooze gibt 5 Minuten gratis und
untergräbt die Disziplin; das Casino nimmt im Schnitt Zeit und stärkt sie.

**Name im Produkt:** "Casino Unlock" (Settings-Label), im Overlay: **"HOUSE OF TIME"**.

---

## 2 · Ethik & Leitplanken

Dieses Feature benutzt absichtlich Glücksspiel-Mechaniken. Das ist vertretbar — und
nur vertretbar — weil sämtliche klassischen Schadensvektoren strukturell fehlen:

| Echtes Glücksspiel | House of Time |
|---|---|
| Einsatz: Geld (extern, unbegrenzt nachladbar) | Einsatz: eigene Bildschirmzeit-Freigabe (intern, hart gedeckelt) |
| Gewinn: Geld → Sucht-Spirale nach oben | Gewinn: Minuten in einer App, die man *selbst* begrenzen wollte |
| Verlust: real ruinös | Verlust: die App bleibt zu — das war der Default ohnehin |
| Haus profitiert von Kontrollverlust | "Haus" = die eigene Disziplin; Edge zahlt auf das Ziel ein |
| Verfügbar 24/7, überall | Nur im Intercept-Moment, nur mit Versuchen-Budget, nie in Schutzfenstern |

Daraus folgen **sieben nicht verhandelbare Leitplanken**, die in Code gegossen werden:

1. **Opt-in.** Default AUS. Aktivierung nur bewusst in den Guard-Settings.
2. **Hartes Versuchs-Limit pro Tag** (einstellbar, Default 3). Danach: konfigurierbare
   Pause ("Break after limit"), Default bis Mitternacht.
3. **Gewinn-Deckel pro Tag** (Default 60 min): Selbst ein Lauf von Glückstreffern kann
   das Tageslimit nicht beliebig aufblasen.
4. **Härtere Regeln schlagen das Casino immer.** In Phone-free-Fenstern, Fokus-Sessions,
   Morning-Block und bei Doomscroll-Lockout gibt es **kein** Spiel-Angebot. Das Casino
   kann nur das *weichste* Sperr-Instrument (Tageslimit) verhandeln.
5. **Verlust wirkt sofort und unumkehrbar** (Lockout-Timestamp in Prefs). Kein
   Doppel-oder-nichts-Nachkaufen; die Versuche sind pro Tag, nicht pro Sperre.
6. **Volle Transparenz:** Die Odds stehen auf jedem Wett-Button. Die Statistik-Zeile
   in den Settings zeigt Netto-Bilanz ("House +142 min this month") — niemand soll
   sich einreden, er gewinne langfristig.
7. **Kein Einsatz von Nacht/Gesundheit:** Es werden ausschließlich Guard-Minuten
   verhandelt. Schlafenszeit-Blöcke, Bedtime etc. sind unantastbar.

Zur Verzögerungs-Mechanik (Suspense): Sie erhöht die emotionale Wirkung *beider*
Ausgänge. Da der EV negativ ist und die Limits hart sind, macht sie das Feature nicht
gefährlicher — sie macht den Verlust einprägsamer ("teachable moment") und den seltenen
Gewinn zu einem echten Fest. Peak-End-Regel: Beide Peaks arbeiten für das Langzeitziel.

---

## 3 · Die Verhaltenspsychologie dahinter

Warum funktioniert das — und warum ist es der bestehenden Snooze-Leiter überlegen?

**3.1 Variable Ratio Reinforcement (Skinner).** Belohnung mit unvorhersehbarem
Ausgang erzeugt die stabilste Verhaltensbindung. Hier gebunden wird aber nicht das
Scrollen, sondern *der Umgang mit der Sperre*: Der Moment "Limit erreicht" wird von
einem reinen Frustrations-Ereignis zu einem Entscheidungs-Ereignis mit Agency.

**3.2 Loss Aversion (Kahneman/Tversky, λ ≈ 2.25).** Verluste wiegen gut doppelt so
schwer wie gleich große Gewinne. Ein verlorener 20-Minuten-Einsatz (→ 20 min Lockout)
schmerzt stärker, als 20 gewonnene Minuten freuen. Genau deshalb *reduziert* das
Feature bei realistischem Spielverhalten die Gesamt-Screen-Time: Nach 1–2 Verlusten
lässt man es öfter ganz bleiben — der Intercept hat dann schon gewonnen.

**3.3 Near-Miss & Anticipation.** Der Dopamin-Peak liegt vor der Auflösung, nicht
danach (Schultz: reward prediction). Die geforderte Verzögerung zwischen letztem
Spiel-Ereignis (Rad steht, letzte Dealer-Karte liegt) und der Ergebnis-Anzeige ist
das Design-Fenster dafür: **650 ms Stille** — lang genug für den Puls, kurz genug,
um nicht künstlich zu wirken. (Stake, Slots und Lootboxen nutzen 400–1200 ms.)

**3.4 Commitment Device (Schelling/Ainslie).** Die Settings werden im "kalten"
Zustand konfiguriert (nüchtern, in der App), die Konsequenzen treffen das "heiße"
Ich im Craving-Moment. Klassische Odysseus-am-Mast-Konstruktion: Das kalte Ich
bestimmt Einsatzgrenzen, Versuchsanzahl, Verlust-Pause — das heiße Ich kann nur
noch innerhalb dieser Mauern spielen.

**3.5 Fairness-Empfinden.** Der bestehende Snooze fühlt sich wie Selbstbetrug an
(Regel gebrochen, nichts bezahlt). Ein verlorenes Spiel fühlt sich *verdient* an —
man hat seinen Versuch gehabt. Das reduziert den Drang, Guard ganz abzuschalten
(die reale Gefahr jedes Blockers: der Rage-Uninstall). Das Casino ist ein
**Druckventil mit Eintrittspreis**.

**3.6 Warum kein "Skill-Einfluss-Fake"?** Blackjack hat echte Entscheidungen
(Hit/Stand/Double) mit echtem EV-Einfluss — das ist erlaubte, ehrliche Agency.
Roulette ist pure Chance — auch ehrlich. Verboten sind Pseudo-Skill-Elemente
("Tippe im richtigen Moment!"), die Kontrolle vorgaukeln. Alles, was nach Skill
aussieht, muss mathematisch Skill sein.

---

## 4 · Zeit-Ökonomie I: Die Währung

**Die Einheit ist die Minute.** Konkret existieren drei Zeit-Töpfe:

1. **Bonus-Minuten (Gewinn-Seite).** `bonus[pkg][dayKey] += payout`. Der Guard-Check
   für das Tageslimit wird zu `usedMs >= (limitMin + bonusMin) * 60_000`. Bonus gilt
   nur für die eine App, nur heute. Übernacht verfällt er (dayKey-Rotation) — kein
   Ansparen, kein Horten, keine "Zeit-Bank" (bewusst, siehe Ideen-Katalog B).
2. **Lockout (Verlust-Seite).** `lockoutUntil[pkg] = now + verlorene Minuten × Faktor`.
   Während des Lockouts zeigt der Intercept den Countdown und KEIN Spiel-Angebot.
   Der Lockout ist unabhängig vom Limit — selbst wenn theoretisch noch Limit-Minuten
   übrig wären (z. B. durch früheren Bonus), bleibt die App zu.
3. **Der Einsatz selbst.** Kommt aus keinem Topf — er ist ein *Versprechen*:
   "Ich zahle mit Wartezeit, wenn ich verliere." Deshalb braucht es kein Guthaben-
   System; die Deckung ist der Lockout. (Verworfen: Einsatz aus verbleibenden
   Limit-Minuten abbuchen — fühlte sich doppelt bestrafend an und ist im
   Limit-erreicht-Moment ohnehin leer.)

**Einsatz-Spanne:** einstellbar, Default 5–60 min, Schrittweite 5. Im Overlay wählt
ein Chip-Stepper (kein Slider — präziser bei NOT_FOCUSABLE-Overlays, und Chips mit
festen Werten 5/10/15/25/40/60 lesen sich wie Casino-Chips).

**Verlust-Faktor ("Loss multiplier"):** Der Lockout ist `Einsatz × Faktor`.
Default ×1 (fair, lesbar), Optionen ×2 und ×3 für Hartgesottene (die
MONO-Prototyp-Erkenntnis: manche wollen echte Abschreckung).

---

## 5 · Zeit-Ökonomie II: Odds, Payouts, Expected Value

Grundsatz: **Echte Casino-Odds, ehrlich ausgezahlt, ehrlich negativ.** Keine
geschönten Wahrscheinlichkeiten — der House Edge ist das Feature.

### 5.1 Roulette (europäisch, eine Null)

| Wette | Gewinnt bei | P(win) | Payout (auf Einsatz S) | EV |
|---|---|---|---|---|
| Rot / Schwarz | 18/37 | 48.65 % | +1 S | −2.70 % |
| Gerade / Ungerade | 18/37 | 48.65 % | +1 S | −2.70 % |
| 1–18 / 19–36 | 18/37 | 48.65 % | +1 S | −2.70 % |
| Dutzend (1–12, 13–24, 25–36) | 12/37 | 32.43 % | +2 S | −2.70 % |
| Kolonne | 12/37 | 32.43 % | +2 S | −2.70 % |
| **Straight (eine Zahl)** | 1/37 | 2.70 % | +35 S, **gedeckelt** | −2.70 % vor Cap |

Der Straight-Payout kollidiert mit dem Tages-Gewinndeckel: 35 × 25 min = 875 min ist
absurd. **Entscheidung:** Straight zahlt `min(35 × S, winCapRestHeute)` und der
Wett-Button zeigt die *echte* mögliche Auszahlung ("hits pay +120m" statt "+875m").
Damit bleibt die Anzeige ehrlich. Wer den Nervenkitzel der 1/37-Chance will, bekommt
ihn — aber das Haus of Time zahlt maximal den Tagesdeckel aus.

### 5.2 Blackjack (Single-Deck pro Hand neu gemischt, Dealer steht auf Soft 17)

| Ereignis | Payout | Anmerkung |
|---|---|---|
| Gewinn (normal) | +1 S | |
| **Blackjack (A + 10er aus zwei Karten)** | +1.5 S (aufgerundet) | klassisch 3:2 |
| Push | 0, Hand wird neu gegeben | Versuch nicht verbraucht |
| Verlust / Bust | Lockout S × Faktor | |
| **Double Down** | Einsatz ×2, genau eine Karte | echte Entscheidung, echtes Risiko |

Basisstrategie-EV bei diesen Regeln: ≈ −0.5 % bis −1 % (deutlich besser als Roulette
— Blackjack ist die "Skill-Option", Roulette die "Glücks-Option"; das ist gewollt
und wird nirgends versteckt). Ohne Basisstrategie spielt der Durchschnittsmensch
≈ −2 bis −4 %. Kein Split in v1 (UI-Komplexität im Overlay; Kandidat für v2),
keine Insurance (mathematisch immer schlecht — wir bieten keine bekannt schlechten
Wetten an, siehe Katalog A), kein Surrender in v1.

### 5.3 Monte-Carlo-Sanity (Teil des Testplans)

Der Unit-Test `CasinoEngineEvTest` spielt 200 000 automatische Runden je Wett-Typ
und asserted den empirischen EV in einer Toleranz (Roulette −2.7 % ± 0.6 pp;
Blackjack mit fixer einfacher Strategie im erwarteten Band). Damit ist die Engine
beweisbar ehrlich — der "provably fair"-Gedanke aus Katalog B in Testform.

---

## 6 · Die fünf Mechanik-Familien (Prototyp-Erkenntnisse)

Aus den 10 Browser-Prototypen (5 Welten × BJ/Roulette), die heute getestet wurden:

| Familie | Kern | Stärke | Schwäche | Verdict |
|---|---|---|---|---|
| **Double or Nothing** (SOVEREIGN) | 1 Versuch, Win=frei, Lose=×2 | maximale Klarheit, größter Einzelmoment | binär, kein Einsatz-Gefühl, "frei" ist zu groß | Mechanik-Kern verworfen, *Moment-Design* übernommen |
| **Time Stakes** (NEON) | Minuten setzen, ±Payout, mehrere Chips | genau der User-Wunsch: wählbarer Einsatz, echtes Wallet-Gefühl | braucht Limits gegen Grinding | **✅ KERN von v1** |
| **Best of Three** (GLACIER) | Match statt Einzelhand | sanft, weniger Varianz | verwässert den Moment, 3× so lang im Overlay | ❌ (Overlay soll kurz sein) |
| **Earn-back** (TERRA) | fixe ±45 min, 3 Runden | grindbar, planbar | fühlt sich nach Arbeit an, nicht nach Casino | ❌ als Modus; die *Idee* lebt im Stake-Stepper |
| **High Stakes** (MONO) | Verlust ×3 | echte Abschreckung | zu brutal als Default | ✅ als einstellbarer Loss-Faktor ×1/×2/×3 |

**Synthese v1:** Time-Stakes-Ökonomie (NEON) + Ein-Moment-Dramaturgie (SOVEREIGN)
+ konfigurierbarer Verlust-Faktor (MONO) + die harten Schutzmauern aus Teil 2.
Best-of-3 und Earn-back sterben — beide verlängern die Zeit *im Overlay*, und die
oberste UX-Regel des Intercepts ist: **Der Aufenthalt im Overlay ist kurz.** Ein
Spiel, eine Auflösung, raus.

---

## 7 · Ideen-Katalog A — Spiele & Wetten

Jede Idee mit Verdict: ✅ v1 · 🔜 v2-Kandidat · ❌ verworfen (mit Grund).

**Blackjack**
- ✅ **Hit / Stand** — Pflicht.
- ✅ **Double Down** (beliebige erste 2 Karten): verdoppelt Einsatz + Lockout-Risiko,
  eine Karte. Der beste "eine echte Entscheidung mehr"-Hebel.
- ✅ **Blackjack zahlt 3:2** — der kleine Jackpot-Moment, den BJ braucht.
- ✅ **Push = neue Hand, Versuch bleibt** — sonst fühlt sich der Versuch gestohlen an.
- ✅ **Dealer steht auf Soft 17** (S17) — spielerfreundlichste Standardregel.
- 🔜 **Split** (gleiche Ränge): zwei Hände parallel im schmalen Overlay ist v2-UI-Arbeit.
- 🔜 **Surrender** (halber Lockout, Hand aufgeben): interessant als "Reißleine",
  aber v1 hält die Aktionsleiste bei 3 Buttons.
- ❌ **Insurance**: mathematisch immer −EV, existiert in echten Casinos als Falle.
  Wir bieten keine Wetten an, die ein informierter Spieler nie nimmt.
- ❌ **Multi-Deck-Shoe mit Kartenzählen**: Single-Deck, pro Hand gemischt. Zählen
  soll explizit nichts bringen (sonst wird das Feature zur Skill-Farm).
- ❌ **Side-Bets (Perfect Pairs etc.)**: Side-Bets sind die schlechtesten Wetten im
  Casino; gleiche Logik wie Insurance.

**Roulette**
- ✅ **Rot/Schwarz, Gerade/Ungerade, Hoch/Tief** — die 1:1-Bank.
- ✅ **Dutzende** (2:1) — der Mittelweg.
- ✅ **Straight Number 0–36** (35:1, am Tagesdeckel gekappt) — expliziter User-Wunsch,
  über ein echtes Tableau-Grid wählbar.
- ✅ **Europäisches Rad (eine Null)** — halber House Edge des amerikanischen; die
  faire Wahl, und die Null bleibt der ehrliche Haus-Moment.
- 🔜 **Kolonnen** (2:1): mathematisch identisch mit Dutzenden; v1 spart die dritte
  Außenwetten-Reihe, das Tableau bleibt luftiger.
- 🔜 **Split/Corner/Street** (17:1, 8:1, 11:1): braucht Präzisions-Tap zwischen
  Zellen — auf einem Overlay ohne Zoom fummelig; v2 mit Lupe-Interaktion.
- ❌ **Amerikanisches Rad / Doppel-Null**: schlechtere Odds ohne Gegenwert.
- ❌ **La Partage/En Prison**: reduziert den Edge auf 1.35 % — klingt spielerfreundlich,
  aber wir *wollen* den vollen 2.7 %-Edge als Disziplin-Steuer. Bewusste Härte.

**Weitere Spiele**
- 🔜 **Crash** ("Cash out before JARVIS crashes"): der Stake-Klassiker; passt thematisch
  perfekt (Multiplikator auf Einsatz-Minuten, Absturzkurve). Größter v2-Kandidat.
- 🔜 **Coin Flip / "JARVIS Flip"**: die 5-Sekunden-Variante für kleine Einsätze.
- ❌ **Slots**: reines variable-ratio-Maximum ohne jede Entscheidung — das ist die
  Suchtmaschine ohne den Agency-Gegenwert. Widerspricht Teil 2.
- ❌ **Poker gegen JARVIS**: zu lang fürs Overlay, KI-Gegner-Illusion problematisch.

---

## 8 · Ideen-Katalog B — Meta-Systeme

- ✅ **Netto-Bilanz-Statistik** ("House +142 min this month"): eine Zeile in den
  Settings + im Overlay-Footer. Ehrlichkeit als Feature. Gespeichert als zwei
  Zähler (wonMin/lostMin, monatlich rotierend).
- ✅ **Session-Seed-Anzeige** (Mini-"provably fair"): Das Overlay zeigt unten klein
  den Seed der Runde (z. B. `#a3f7`). Kryptografisch simpel (Random.nextInt als
  Hex), aber es kommuniziert: Das Ergebnis stand fest, bevor die Animation lief —
  was dank resolve-then-animate (Teil 18) sogar exakt stimmt.
- ✅ **Verzögerungs-Slider versteckt?** Nein — die Suspense-Dauer ist Design-Konstante
  (650 ms), kein Setting. Zu viele Regler verwässern.
- 🔜 **Wochen-Report-Karte** ("Dein Casino-Monat: 4 Spiele, −35 min, längste
  Pechsträhne 3"): in WeeklyReport integrierbar, sobald v1-Daten fließen.
- 🔜 **Dashboard-Sync** (jarvis-cloud): casino-Payload in StorePayloads → Web-Karte.
- 🔜 **Streak-Bonus** ("3 disziplinierte Tage ohne Spiel → nächster Gewinn +25 %"):
  belohnt Nicht-Spielen — psychologisch elegant, aber v1 bleibt schlank.
- ❌ **Zeit-Bank / Bankroll ansparen**: Gewonnene Minuten über Tage horten
  widerspricht dem Tages-Reset des Guards; würde aus dem Ventil eine Ökonomie
  machen, die man farmen will.
- ❌ **VIP-Level/Ränge fürs Spielen**: Progression fürs Zocken belohnt exakt das
  falsche Verhalten. (Ein Rang fürs *Nicht*-Spielen wäre 🔜, siehe Streak-Bonus.)
- ❌ **Jackpot über Apps hinweg** ("gewinne den ganzen Tag frei"): kollidiert
  frontal mit Leitplanke 3 (Gewinn-Deckel).
- ❌ **Echtgeld-Skins, Chips-Optik mit €-Anmutung**: nichts darf wie Geld aussehen.
  Chips tragen Minuten-Beschriftung ("25m"), nie Währungssymbole.

---

## 9 · Ideen-Katalog C — Anti-Missbrauch & Schutz

- ✅ **Versuche pro Tag** (global, nicht pro App — sonst wird über 5 gesperrte Apps
  gefarmt): Default 3, Range 1–10. Zählt *gestartete Spiele* (Deal/Spin), nicht
  Overlay-Öffnungen.
- ✅ **Break after limit**: Sind die Versuche aufgebraucht, zeigt der Intercept für
  den Rest der Pause kein Casino an. Optionen: bis Mitternacht (Default) / 1 h /
  3 h / 6 h.
- ✅ **Loss-Lockout**: Einsatz × Faktor (×1/×2/×3) als App-spezifische Zusatzsperre.
- ✅ **Gewinn-Deckel/Tag**: Default 60 min, Range 15–240. Gilt über alle Apps summiert.
- ✅ **Casino nie in**: Phone-free-Window, Focus, Morning-Block, Doomscroll-Lockout,
  Gate-Mode. Nur der reine LIMIT-Intercept bietet das Spiel an.
- ✅ **Kein Casino unter 5 min Rest-Lockout**: Wenn ohnehin gleich frei, kein Spiel
  (verhindert "Ich zocke, weil eh nichts zu verlieren ist"-Exploits… genauer:
  Lockout-Countdown < Einsatzminimum → Angebot ausblenden). — Implementiert als:
  Angebot nur, wenn die App durch das Tageslimit gesperrt ist, nicht durch Lockout.
- ✅ **Force-Kill-Resistenz**: Ergebnis wird vor der Animation committed (Teil 18).
  Wer das Overlay im Suspense-Moment abschießt, findet das Ergebnis angewandt vor.
- ✅ **Kein Spiel bei ausgeschaltetem Guard**: trivial, aber explizit — CasinoStore
  ist reiner Anhang des Wellbeing-Systems.
- 🔜 **Panik-Knopf** ("Disable casino for 7 days", unumkehrbar): das nukleare
  Selbstbindungs-Werkzeug; v2, braucht bedachtes UI.
- ❌ **Elternkontroll-PIN etc.**: persönliche Einzel-Nutzer-App; falsche Baustelle.

---

## 10 · Design-Sprache: Was "clean wie Stake" konkret heißt

Analyse der Referenz (Stake.com, moderne Crypto-Casino-UIs, 2024–2026):

1. **Flat & dunkel.** Ein tiefes, entsättigtes Dunkelblau/Anthrazit als Fläche
   (#0F1923-Klasse), Panels nur eine Nuance heller. Keine Verläufe als Dekor,
   keine Glas-Effekte, kein Skeuomorphismus (kein Filz, kein Holz, keine
   fotorealistischen Chips).
2. **Eine Akzentfarbe, chirurgisch eingesetzt** (bei Stake: das Blau/Grün der CTA).
   Alles andere ist Graustufen-Typo. Gewinn-Grün und Verlust-Rot erscheinen
   ausschließlich im Ergebnis-Moment.
3. **Typo-Hierarchie statt Rahmen.** Zahlen groß und tabular, Labels klein und
   gesperrt (Letter-Spacing), kaum Linien — Gruppierung über Abstand.
4. **Micro-Motion 150–250 ms**, Ease-out, nie bouncy im Layout; Bounce ist dem
   einen Gewinn-Moment vorbehalten. Übergänge sind Fades + kleine Y-Shifts.
5. **Der Tisch ist die Bühne:** Spielfläche mittig, Aktionen unten in Daumen-Reichweite,
   Meta-Info (Einsatz, Odds) direkt an den Aktions-Elementen — nie im Kopfbereich.
6. **Ergebnis als Zustand, nicht als Popup.** Kein Modal-Alert; die Bühne selbst
   verwandelt sich (Farbe, Zahl, eine Zeile Text).

**Übersetzung auf JARVIS:** Das Intercept-Overlay hat bereits die richtige Basis
(Void-Schwarz, dezente Panels, eine Akzentfarbe). Der Casino-Flow erbt exakt diese
Anatomie und ersetzt nur die Bühne. JARVIS-DNA bleibt über zwei Details: die
Wordmark-Zeile ("HOUSE OF TIME · JARVIS GUARD") und der Champagne-Gold-Akzent
**ausschließlich** im Gewinn-Moment (SOVEREIGN-Erbe: Gold = Feier, nie Dekor).

---

## 11 · Die fünf Design-Optionen im Vergleich (Entscheidung)

| Option | Beschreibung | Pro | Contra | Verdict |
|---|---|---|---|---|
| A — Stake-Klon | 1:1 die Referenz-Ästhetik (eigenes Blau, eigene Fonts) | maximal "wie echte Gambling-App" | Fremdkörper in JARVIS; zweites Design-System zu pflegen | ❌ |
| B — SOVEREIGN-Casino | Obsidian + Champagne + Serifen (Prototyp 01/02) | luxuriös, markant | Serifen-Karten wirken im nüchternen Intercept-Kontext kostümiert; Gold als Flächenfarbe entwertet den Gewinn-Moment | ❌ (aber: Gewinn-Gold übernommen) |
| C — **Clean Core, Guard-tinted** | Void-Schwarz + bestehende Guard-Akzentfarbe (`Mod.Guard`), Stake-Anatomie, Gold nur im Win | fügt sich nahtlos ins bestehende Overlay; null neue Design-Schulden; theme-adaptiv gratis | weniger "eigene Welt" | **✅ v1** |
| D — NEON-Arcade | Magenta/Cyan, Scanlines (Prototyp 03/04) | hoher Wow-Faktor | laut, das Gegenteil von "sehr clean"; Scanline-Overlay über einem System-Overlay = visuelles Rauschen | ❌ |
| E — MONO-Terminal | Schwarz/weiß, Monospace (Prototyp 09/10) | radikal clean | zu asketisch für den "echte Gambling-App"-Anspruch; kein Farbkanal für Win/Lose | ❌ |

**Entscheidung C**, mit drei geerbten Einzelstücken: SOVEREIGNs Gold-Moment (Win),
MONOs Verlust-Nüchternheit (Lose-Screen ist bewusst still — kein rotes Drama,
nur Fakten + Countdown) und NEONs Chip-Stepper-Layout für den Einsatz.

---

## 12 · Screen-Spezifikation: Die Zustandsmaschine des Overlays

Der Casino-Flow lebt **im bestehenden Intercept-Overlay** (gleicher ComposeView,
gleiche Window-Params). `JarvisInterceptScreen` bekommt einen vierten Zweig:

```
InterceptMode.LIMIT ──[Casino verfügbar]──▶ InterceptCard mit 3. Button "House of Time"
                                                    │ tap
                                                    ▼
        ┌────────────────────────── CasinoScreen (AnimatedContent) ─────────────────────────┐
        │  PICK ──▶ STAKE ──▶ TABLE(BJ | ROULETTE) ──▶ SUSPENSE ──▶ REVEAL ──▶ [Ende]        │
        │   ▲ back    ▲ back        (spielt)             650 ms      Win/Lose                │
        └────────────────────────────────────────────────────────────────────────────────────┘
Ende(Win):  Overlay schließt nach 1.6 s → App ist offen (Limit + Bonus > used)
Ende(Lose): REVEAL zeigt Lockout-Countdown + "Accept" → Home. Overlay schließt.
```

**Phase PICK — Spielwahl.** Zwei große Karten nebeneinander (je ~46 % Breite,
Aspekt 1:1.15): "BLACKJACK — the skill table · edge ≈ 1 %" und "ROULETTE — the
wheel · edge 2.7 %". Ehrliche Edge-Angabe direkt auf der Wahlkarte. Darunter die
Versuchs-Pillen (wie bestehende Snooze-Leiter-Ästhetik) und die Zeile
"Attempts today · 2 of 3 left".

**Phase STAKE — Einsatz.** Titelzeile "Your stake", darunter Chip-Reihe
(5/10/15/25/40/60 min als runde Chips, gewählter Chip füllt sich mit Akzent),
darunter die Konsequenz-Vorschau in zwei ehrlichen Zeilen:
"Win → +25 min Instagram today" / "Lose → locked for 25 min extra".
CTA "Deal" bzw. "To the wheel". Zurück-Chevron oben links.

**Phase TABLE.** Siehe Teil 13/14.

**Phase SUSPENSE.** Nach dem letzten Spiel-Ereignis friert die Bühne ein;
eine Puls-Animation (Akzent-Ring um die Ergebnis-Zone, 650 ms, ein Herzschlag).
Keine Zahl, kein Text. Dann REVEAL.

**Phase REVEAL.**
- **Win:** Die Bühne tönt sich für 1.6 s in Champagne-Gold (radialer Glow hinter
  der Zentral-Zahl), große Zeile "+25 MIN", darunter "Instagram is open — the
  house honors its debts." Konfetti: nein (zu verspielt) — stattdessen ein
  einzelner Gold-Shimmer über die Karte (400 ms). Haptik: success. Overlay
  schließt sich selbst (kein Button nötig — die App liegt ja darunter).
- **Lose:** Still. Panel bleibt dunkel, eine Zeile "House wins." darunter
  nüchtern "Locked until 17:45 · your stake, your rules." + Button "Accept" →
  Home. Haptik: ein einzelner tiefer Tick. KEIN rotes Vollbild — die Stille
  ist die Botschaft.

**Sichtbarkeits-Logik des Einstiegs-Buttons** (alle müssen wahr sein):
`CasinoStore.enabled` ∧ `mode == LIMIT` ∧ `!lockedOut(Doomscroll)` ∧ kein
Phone-free/Focus/Morning-Kontext (bereits durch mode==LIMIT + statusText==null
abgedeckt) ∧ `attemptsLeftToday > 0` ∧ kein aktiver Break ∧ kein aktiver
Casino-Lockout für diese App ∧ `winCapRestHeute > 0`.

---

## 13 · Blackjack-Spezifikation

**Regeln (fix, v1):** Single-Deck, vor jeder Hand frisch gemischt (Fisher-Yates).
Dealer steht auf allen 17 (S17). Blackjack zahlt 3:2 (aufgerundet auf ganze
Minuten). Double Down auf beliebigen ersten zwei Karten, danach genau eine Karte.
Kein Split, keine Insurance, kein Surrender. Dealer-Blackjack gegen Spieler-
Blackjack = Push. Dealer zieht seine Karten erst NACH Stand des Spielers
(Hole-Card verdeckt bis dahin).

**Layout (von oben nach unten):**
1. Kopfzeile: "DEALER" + Punkte-Badge (zeigt `7 + ?` solange Hole verdeckt).
2. Dealer-Kartenreihe (Karten 60×86 dp, Radius 10, Überlappung −22 dp ab Karte 3).
3. Trennlinie (hairline, 12 % Ivory).
4. "YOU" + Punkte-Badge (bei Soft-Händen "8 / 18").
5. Spieler-Kartenreihe.
6. Einsatz-Chip klein rechts ("25m at stake").
7. Aktionsleiste: [HIT] [STAND] [DOUBLE] — Double nur bei 2 Karten sichtbar,
   und nur wenn `attemptsErlaubenDouble` (Double zählt als derselbe Versuch).

**Karten-Anatomie:** Fläche `#F6F1E6` (warmes Weiß, zum Void-Schwarz kalibriert),
Ecken-Index oben links + unten rechts (gedreht), Zentral-Pip groß. Rot-Suits im
Akzent-Rot `#C7554A` (entsättigt — kein Signal-Rot, das mit dem Lose-Rot
konkurriert), Schwarz-Suits `#1A1A22`. Verdeckte Karte: Void-Fläche mit
feinem diagonalem Linienraster + Akzent-Hairline.

**Deal-Choreografie:** Karten erscheinen einzeln, 120 ms Stagger, jede mit
Y-Offset −18 dp + rotationY 60°→0° (Flip-Andeutung), Spring (stiffness 380,
dampingRatio 0.82 — die IRON-MOTION-"spatial"-Feder). Dealer-Draw nach Stand:
500 ms pro Karte (Spannung durch Rhythmus). Hole-Card-Flip: 380 ms rotationY
mit Kamera-Distanz 12f.

**Punkte-Logik:** Asse 11→1 fallend (Standard-Soft-Handling). Badge zeigt bei
weichen Händen beide Werte.

---

## 14 · Roulette-Spezifikation

**Rad:** Europäische Segment-Folge (0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27,
13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28,
12, 35, 3, 26). Canvas-gezeichnet: Segment-Ring (außen r=1.0, innen r=0.62),
Zahlen-Ring auf r=0.81 (Zahlen mitrotierend, aufrecht relativ zum Segment),
Zeiger fix oben (kleines Dreieck, Akzentfarbe), Nabe zeigt nach dem Auslauf
die Ergebnis-Zahl. Farben: Rot entsättigt `#B04A3E`, Schwarz `#14141B`,
Null in Guard-Akzent. Keine Kugel-Simulation in v1 (Zeiger-Prinzip wie die
Prototypen — cleaner); 🔜 v2: gegenläufiger Kugel-Punkt.

**Spin-Animation (das Herzstück):** Gesamtdauer 4.2 s, ein `Animatable<Float>`
auf den Zielwinkel `base + 5×360 + segmentOffset(zahl) + jitter(±0.35 Segment)`:
- 0–0.4 s: Anlauf (easeIn)
- 0.4–4.2 s: langer Auslauf mit `CubicBezierEasing(0.08f, 0.6f, 0.04f, 1f)`
  (übernommen aus dem Prototyp-Feintuning — wirkt physisch, ohne Physik-Engine)
- Haptik: `Haptics.tick` bei jedem Segment-Übergang unter dem Zeiger, dadurch
  natürliches Ausklacken (Frequenz sinkt mit der Radgeschwindigkeit — gratis,
  weil an den Winkel gekoppelt).
- Danach: SUSPENSE (650 ms), erst dann färbt sich die Nabe und die Wette löst auf.

**Tableau (Wett-Auswahl), zwei Ebenen:**
1. **Außenwetten-Reihe** (Chips): RED · BLACK · EVEN · ODD · 1–18 · 19–36 (1:1)
   und 1st 12 · 2nd 12 · 3rd 12 (2:1). Zwei Zeilen à 3–4 Chips, Odds klein im Chip.
2. **"Pick a number"-Expander:** öffnet das 0–36-Grid (Casino-Tableau: 0 als
   schmale Zeile oben, dann 3 Spalten × 12 Reihen, Zellen 44 dp hoch — tapbar
   ohne Zoom). Gewählte Zahl bekommt einen Akzent-Ring; Payout-Vorschau aktualisiert
   sich live ("hits pay +120m today" — gedeckelt, Teil 5.1).

**CTA:** "SPIN" (volle Breite, Akzent) — erst aktiv, wenn genau eine Wette gewählt.

---

## 15 · Suspense-Design: Die Kunst der Verzögerung

Der User-Wunsch "Ergebnis nicht sofort, sondern mit kleiner Verzögerung" wird als
**dreistufige Auflösungs-Dramaturgie** umgesetzt (Zahlen = Design-Konstanten):

| Moment | Dauer | Was passiert | Was NICHT passiert |
|---|---|---|---|
| Spiel-Ende (Rad steht / letzte Karte liegt) | 0 ms | Bühne friert ein, laufende Loops stoppen | keine Farbe, kein Text, keine Zahl |
| **Suspense** | **650 ms** | Akzent-Ring pulst einmal um die Ergebnis-Zone (scale 0.96→1.04→1, alpha 0.4→0.9→0) | Buttons unsichtbar/inaktiv, kein Skip |
| Reveal | 320 ms | Ergebnis-Zustand blendet ein (Fade + Y-Shift 12 dp, spring ζ 0.9) | kein Modal, kein Overlay-im-Overlay |
| Aushall | Win 1.6 s / Lose ∞ | Win: Gold-Glow + Auto-Close · Lose: statischer Countdown + Accept | |

Warum 650 ms: unter ~400 ms wirkt es wie Latenz (Bug), über ~1000 ms wie Manipulation
(genervtes Warten). 650 ms liegt im Sweet Spot der Antizipations-Forschung und der
Referenz-Apps. Beim Blackjack zusätzlich: Der Dealer-Draw selbst ist schon Suspense
(500 ms/Karte) — die 650 ms starten nach der letzten Dealer-Karte.

Der Suspense ist **nicht überspringbar** (kein Tap-to-skip): Skip würde den einzigen
Zweck der Phase zerstören. Getestet wird sie als fixe Konstante `SUSPENSE_MS = 650L`
in der Engine-nahen UI-Schicht, damit ein einziger Wert sie überall steuert.

---

## 16 · Sound & Haptik

Vorhandene Bausteine: `Haptics.tick/success/…` (bereits im Gate-Breathing benutzt),
`SoundFx.levelUp/confirm` (App-Sounds). Overlay-Kontext = fremde App sichtbar →
**Sound aus, Haptik an** (man will nicht, dass das Handy im Bus Casino-Sounds spielt;
Haptik ist privat). Choreografie:

| Ereignis | Haptik |
|---|---|
| Karte gedealt / Hole-Flip | tick |
| Rad-Segment-Übergang | tick (frequenzgekoppelt, s. Teil 14) |
| Chip gewählt / Wette gesetzt | tick |
| Suspense-Puls | ein tick am Puls-Peak |
| Win-Reveal | success (Doppel-Impuls) |
| Lose-Reveal | ein einzelner langer/tiefer Impuls (`Haptics.error` falls vorhanden, sonst tick×1) |

---

## 17 · Software-Architektur

**Neue Dateien:**

```
data/casino/CasinoEngine.kt   — pure Spiellogik, kein Android-Import
data/casino/CasinoStore.kt    — Prefs: Settings, Tageszähler, Lockouts, Boni, Stats
wellbeing/CasinoScreen.kt     — Compose-UI des Flows (PICK→…→REVEAL)
app/src/test/…/CasinoEngineTest.kt — Unit- & Monte-Carlo-Tests
```

**Geänderte Dateien (chirurgisch):**

```
wellbeing/JarvisGuardService.kt
  · tick() Regel 5: effektives Limit = limitMin + CasinoStore.bonusMin(ctx, fg, today)
  · tick() neuer Vor-Check: aktiver Casino-Lockout → Overlay (LIMIT-Mode,
    statusText "House lockout · 17:45", casinoOffer = null)
  · showOverlay(): berechnet casinoOffer (oder null) und reicht ihn + zwei
    Callbacks durch (onCasinoWin(pkg, min), onCasinoLose(pkg, min))
wellbeing/JarvisInterceptScreen.kt
  · InterceptCard: dritte Aktion "HOUSE OF TIME · 2 attempts left" (nur offer != null)
  · Zustands-Umschalter InterceptCard ⇄ CasinoScreen
ui/hud/GuardScreen.kt
  · neue Settings-Sektion "Casino unlock" (Teil 20)
```

**Datenfluss eines Spiels:**
1. UI ruft `CasinoEngine.newBlackjack(seed)` / `CasinoEngine.spinRoulette(bet, seed)`.
2. Engine liefert das **fertige Ergebnis sofort** (alle Karten / die Zahl) —
   die UI animiert nur noch, was längst feststeht (Teil 18).
3. Bei Spiel-Start: `CasinoStore.recordAttempt(ctx)` (Versuch verbraucht, Prefs).
4. Bei Auflösung: `CasinoStore.settle(ctx, pkg, deltaMin)` → schreibt Bonus ODER
   Lockout + Statistik; Service-Callback räumt Overlay/Cooldown auf.

**Warum die Engine pur bleibt:** 200k-Runden-Monte-Carlo im JVM-Test (Teil 22)
und keine Context-Seuche in der Spiellogik. Die Engine kennt weder Prefs noch
Minuten-Semantik — sie kennt Karten, Räder und Payout-Faktoren.

---

## 18 · State, Persistenz & Crash-Sicherheit ("resolve-then-animate")

**Kernentscheidung:** Das Ergebnis wird **vor** der Animation berechnet UND
persistiert. Ablauf bei "SPIN":

```
1. result = engine.spinRoulette(bet)              // Zahl steht fest
2. CasinoStore.writePending(ctx, pkg, stake, deltaMin)   // Prefs, synchron
3. UI spielt 4.2 s Rad + 650 ms Suspense + Reveal
4. CasinoStore.commitPending(ctx)                 // pending → settled
```

Stirbt der Prozess zwischen 2 und 4 (Force-Kill, System-Kill, Crash), findet der
nächste `tick()` ein nicht-committetes Pending und wendet es an (`settlePending`).
Konsequenz: **Force-Kill im Suspense-Moment rettet niemanden vor dem Verlust** —
und stiehlt niemandem den Gewinn. Ohne diese Reihenfolge wäre der offensichtliche
Exploit: verlierende Animation sehen → App-Info → "Stopp erzwingen" → Versuch weg,
Lockout nie geschrieben.

**Prefs-Schema (CasinoStore, SharedPreferences `casino`):**

```
cas_enabled            Boolean  (false)
cas_attempts_per_day   Int      (3)
cas_stake_min/max      Int      (5 / 60)
cas_loss_mult          Int      (1)        // 1|2|3
cas_break_mode         String   ("midnight" | "60" | "180" | "360")
cas_win_cap_day        Int      (60)
cas_day                String   // dayKey der Zähler
cas_attempts_used      Int
cas_won_today          Int      // gegen win cap
cas_bonus_<pkg>        Int      // Bonus-Minuten heute (Tag im Wert kodiert: "2026-07-13:25")
cas_lockout_<pkg>      Long     // epoch ms
cas_break_until        Long     // epoch ms (Break nach Versuchs-Limit)
cas_pending            String   // "pkg|stake|delta" bis committed
cas_stat_won/lost      Int      // Monats-Statistik ("2026-07:120")
```

Tages-Rollover: `cas_day != todayKey()` → attempts_used/won_today/Boni nullen
(lazy beim ersten Zugriff — kein Alarm nötig). `todayKey()` folgt dem App-weiten
6-Uhr-Rollover — Mitternachts-Zocker bekommen um 00:01 also KEINE frischen
Versuche (bewusst: die "Break until midnight"-Option bricht real um 06:00, Copy
sagt deshalb "until tomorrow").

---

## 19 · Anti-Cheat & Edge-Cases

| Fall | Verhalten |
|---|---|
| Force-Kill während Spin/Suspense | Pending wird beim nächsten tick() angewandt (Teil 18) |
| Uhr verstellen (Lockout überspringen) | akzeptiert — wer die Systemuhr verstellt, hat den Blocker mental schon deinstalliert; kein Wettrüsten (konsistent mit bestehender Guard-Philosophie) |
| Twei gesperrte Apps, ein Break | Versuche + Break sind global — kein App-Hopping-Farming |
| Overlay vom System gekillt (Memory) | wie Force-Kill: Pending-Mechanik greift |
| Bonus > Limit übrig, aber Lockout aktiv | Lockout gewinnt (Reihenfolge im tick(): Lockout-Check vor Limit-Mathe) |
| Push bei letztem Versuch | Push verbraucht den Versuch NICHT (recordAttempt erst bei endgültiger Auflösung — Implementations-Detail: attempt wird bei Deal reserviert und bei Push zurückgegeben) |
| Doppel-Tap auf SPIN | CTA disabled ab erstem Tap (state-gated) |
| App wird während Win-Aushall geschlossen | Bonus ist bereits committed — nächster Open läuft normal |
| Prozess-Neustart mitten im Overlay | Overlay stirbt mit Service-Neustart; kein State im UI nötig (alles in Prefs) |
| dayKey-Wechsel während offenem Overlay | settle() liest dayKey frisch — schlimmstenfalls bucht ein 23:59-Spiel auf den neuen Tag; irrelevant klein |

---

## 20 · Settings-Spezifikation (GuardScreen, neue Sektion "Casino unlock")

Platzierung: eigene Karte unterhalb der bestehenden Override/Limits-Sektion,
gleiche Row-Idiome wie der Rest des GuardScreens.

| Setting | Control | Range / Optionen | Default | Copy |
|---|---|---|---|---|
| Casino unlock | Toggle | on/off | **off** | "Gamble for minutes when a limit hits. The house edge works for you." |
| Attempts per day | Stepper | 1–10 | 3 | "Games per day, across all apps" |
| Stake range | Range-Chips | 5–120 | 5–60 | "Chips offered at the table" |
| Loss lockout | Segmented | ×1 · ×2 · ×3 | ×1 | "Lose 25m → locked 25/50/75m" |
| Break after last attempt | Segmented | Until tomorrow · 1h · 3h · 6h | Until tomorrow | "When attempts run out" |
| Daily win cap | Stepper | 15–240 | 60 | "Max minutes the house pays per day" |
| (Fußzeile) | Statistik | — | — | "This month: you +12m · house +96m" |

Alle Steppers/Segmente nur aktiv, wenn Toggle an. Werte greifen ab dem nächsten
Intercept (kein Neustart nötig — alles Prefs-Reads im Service-Pfad).

---

## 21 · Statistik & Dashboard-Anbindung

v1 lokal: `cas_stat_won/lost` (Monat) + `cas_stat_games`. Anzeige: Settings-Fuß +
Overlay-Footer ("House leads +96m this month"). 🔜 v2: `StorePayloads.appendAll`
bekommt ein `casino`-Dokument → jarvis-cloud-Karte (Spiele-Historie, Bilanz-Kurve,
größter Win/Lose). Die Web-Seite existiert absichtlich noch nicht: erst Daten
sammeln, dann visualisieren.

---

## 22 · Testplan

**Unit (JVM, `CasinoEngineTest`):**
1. Kartenwerte: A=11/1-Kaskade, Bild=10, Soft-Anzeige ("8/18").
2. Dealer-Politik: zieht bis <17, steht auf allen 17 (inkl. Soft 17), Bust-Erkennung.
3. Blackjack-Erkennung nur bei 2 Karten; BJ vs BJ = Push; Payout 3:2 aufgerundet.
4. Double: genau eine Karte, Einsatz-Verdopplung im Resultat.
5. Roulette: Farb-/Parität-/Bereichs-/Dutzend-/Straight-Auflösung für alle 37 Zahlen
   (Tabellen-Test), Zero schlägt alle Außenwetten.
6. Payout-Mathe: alle Wett-Typen × Win/Lose → deltaMin korrekt inkl. Cap-Klemme.
7. **Monte-Carlo-EV:** 200k Runden Rot/Schwarz → EV −2.7 % ± 0.6 pp; 200k Runden
   Straight → Trefferquote 1/37 ± Toleranz; 100k BJ-Hände mit fixer Simple-Strategy
   → EV im Band [−6 %, 0 %] (grobe Ehrlichkeits-Schranke, kein exakter Strategie-Test).
8. Store-Mathe (mit Fake-Prefs? — Store ist Context-gebunden; die pure Anteile
   (Cap-Klemme, Break-Berechnung) leben als statische Funktionen in der Engine
   und werden dort getestet).

**On-Device-Checkliste (Release-Build):**
- Settings: Toggle + alle Regler bedienbar, Werte persistieren.
- Intercept ohne Casino (Toggle aus) unverändert. ← Regression!
- Intercept mit Casino: Button erscheint nur im LIMIT-Mode.
- BJ komplett: Deal-Stagger, Hit, Stand, Double, Push-Redeal, BJ-3:2.
- Roulette komplett: Außenwette, Zahlen-Grid, Spin-Auslauf, Tick-Haptik.
- Suspense fühlbar (650 ms), Win-Gold, Lose-Stille, Auto-Close/Home.
- Verlust → App bleibt zu, Countdown im nächsten Intercept, kein Spiel-Angebot.
- Versuche aufgebraucht → Break greift, Angebot weg.
- Force-Kill im Suspense → Ergebnis trotzdem angewandt (Pending-Test).

---

## 23 · Performance & Batterie

- Das Rad zeichnet in `Canvas` mit einem einzigen `rotate`-Layer; 60 fps nur
  während der 4.2 s Spin (Animatable), sonst statisch. Kein infiniteTransition
  im Idle (Ausnahme: der 650-ms-Suspense-Puls, endlich).
- Kartenflips sind `graphicsLayer`-Transformationen (GPU), keine Layout-Änderungen.
- Der Casino-Flow erzeugt KEINE zusätzlichen Service-Loops: alles läuft im
  bestehenden Overlay-Recomposer, der beim Schließen abgeräumt wird (bestehende
  Leak-Hygiene aus dem Audit bleibt unangetastet).
- Prefs-Writes: ≤ 4 pro Spiel (attempt, pending, commit, stats) — vernachlässigbar.

---

## 24 · Rollout & Defaults

- Feature-Flag = der Settings-Toggle selbst. Default **aus** → Null-Risiko-Rollout.
- Version 2.18, versionCode 20.
- Changelog-Eintrag ("House of Time — gamble minutes at the Guard wall. Off by
  default; find it in Guard settings.")
- Kein Migrations-Bedarf (nur neue Prefs-Keys).

## 25 · Zukunfts-Roadmap

v2-Kandidaten in Prioritätsreihenfolge: (1) Crash-Game, (2) Split/Surrender im BJ,
(3) Wochen-Report-Karte + Dashboard-Sync, (4) Streak-Bonus fürs Nicht-Spielen,
(5) Panik-Knopf (7-Tage-Selbstsperre), (6) Kolonnen + Innen-Wetten-Lupe im Roulette,
(7) Kugel-Animation im Rad.

---

## 26 · Anhang A: Vollständige Payout-/EV-Tabellen

**Roulette, Einsatz S Minuten, Verlust-Faktor f (Lockout = S×f):**

| Wette | P(win) | Win-Delta | Lose-Delta (f=1) | EV in Minuten (f=1) |
|---|---|---|---|---|
| Rot/Schwarz/Odd/Even/Low/High | 18/37 | +S | −S | −S/37 ≈ −0.027 S |
| Dutzend | 12/37 | +2S | −S | −S/37 |
| Straight | 1/37 | +min(35S, capRest) | −S | ≥ −S/37 (Cap kann EV weiter senken) |

Mit f=2 verdoppelt sich der Verlust-Term: EV(Rot, f=2) = (18·S − 19·2S)/37 = −20S/37
≈ −0.54 S. Die Settings-Copy macht das sichtbar ("Lose 25m → locked 50m").

**Blackjack (S17, 3:2, Double, kein Split), grobe EV-Bänder:**

| Spielweise | EV |
|---|---|
| Perfekte Basisstrategie | ≈ −0.7 % |
| "Gefühlsspieler" (hit bis 15, nie double) | ≈ −3 % |
| Worst reasonable (hit bis 17) | ≈ −6 % |

**Beispiel-Session (3 Versuche à 25 min, Rot/Schwarz, f=1):**
P(3 Verluste) ≈ 13.3 % → 75 min Lockout. P(≥1 Win) ≈ 86.7 %. E[Netto] ≈ −2 min.
Fazit: kurzfristig oft befriedigend, langfristig zahlt man — exakt die gewünschte
Ventil-Ökonomie.

## 27 · Anhang B: Copy-Katalog (final, EN wie App-Sprache)

- Entry-Button: `HOUSE OF TIME` + Unterzeile `2 attempts left today`
- PICK-Titel: `Pick your table` · BJ-Karte: `BLACKJACK · the skill table · edge ≈1%`
  · Roulette-Karte: `ROULETTE · the wheel · edge 2.7%`
- STAKE-Titel: `Your stake` · Vorschau-Win: `Win → +{n} min {app} today` ·
  Vorschau-Lose: `Lose → locked {n×f} min extra`
- BJ-Aktionen: `HIT` `STAND` `DOUBLE`
- Roulette-CTA: `SPIN` · Tableau-Expander: `Pick a number` · Payout-Live-Zeile:
  `straight {z} pays +{n}m today`
- Suspense: (kein Text — bewusst)
- Win: `+{n} MIN` / `{app} is open — the house honors its debts.`
- Lose: `House wins.` / `Locked until {HH:mm} · your stake, your rules.` / `Accept`
- Break: `Tables closed · back {tomorrow|in 1h|in 3h|in 6h}`
- Lockout-Intercept-Status: `House lockout · until {HH:mm}`
- Settings-Statistik: `This month: you +{w}m · house +{l}m`

## 28 · Anhang C: Motion-Token-Tabelle

| Token | Wert | Verwendung |
|---|---|---|
| `casinoPhase` | fade 220 ms + slideY 12 dp, spring(380, 0.9) | Phasenwechsel PICK→STAKE→TABLE |
| `cardDeal` | stagger 120 ms, Y −18 dp, rotY 60→0, spring(380, 0.82) | BJ-Karten |
| `holeFlip` | rotY 380 ms, cameraDistance 12f | Dealer-Aufdeckung |
| `dealerDraw` | 500 ms/Karte (delay, kein Easing-Trick) | Dealer zieht |
| `wheelSpin` | 4200 ms, CubicBezier(0.08, 0.6, 0.04, 1) | Rad |
| `SUSPENSE_MS` | 650 ms, Puls scale 0.96→1.04→1 | Auflösungs-Pause |
| `reveal` | 320 ms fade + Y 12 dp, spring ζ0.9 | Ergebnis |
| `winGlow` | radial Champagne, 1600 ms, dann Auto-Close | Gewinn |

## 29 · Entscheidungs-Q&A

**Warum Verlust = Lockout statt "Limit verdoppeln"?** Das Tageslimit ist eine
*Tages*-Größe; sie zu verdoppeln wäre am Folgetag vergessen und im Moment abstrakt.
Ein Countdown ("locked until 17:45") ist konkret, sichtbar, endlich — und exakt
die "Pause", die der User konfigurieren wollte.

**Warum Versuche global statt pro App?** Fünf gesperrte Apps wären sonst 15 Spiele.
Das Versuchs-Budget ist die Bankroll des Tages — eine Person, ein Budget.

**Warum kein Tap-to-skip im Suspense?** Die Verzögerung IST das Feature (User-Wunsch,
Teil 15). Skip würde sie zur Latenz degradieren.

**Warum Gold nur im Win?** Gold als Dauer-Dekor nutzt sich ab (SOVEREIGN-Lektion:
Champagne = Feier-Farbe). Der Gewinn-Moment soll der visuell teuerste der ganzen
App sein.

**Warum Chips statt Slider für den Einsatz?** Feste Beträge lesen sich wie Casino-
Chips (mentales Modell), sind präziser tapbar im NOT_FOCUSABLE-Overlay und
verhindern Zahlenfummelei im Craving-Moment.

**Warum bleibt der klassische Snooze daneben bestehen?** Casino ersetzt Snooze
nicht — es ist die Option für Tage, an denen man dem eigenen Urteil nicht traut.
Beide sichtbar: Snooze (grau, langweilig, gratis) vs. House (verlockend, teuer).
Dass viele das Spiel wählen werden, obwohl Snooze rational besser ist, ist die
ehrlichste Selbsterkenntnis, die dieses Feature liefern kann.

---
*Ende des Dossiers. Implementierung folgt diesem Dokument 1:1; Abweichungen werden
im Code kommentiert und hier nachgetragen.*
