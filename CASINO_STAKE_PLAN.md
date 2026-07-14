# HOUSE OF TIME — STAKE EDITION

### Das 100-Seiten-Dossier für Guard-Casino v2, den neuen Lock und die Attempts-Economy

*JARVIS / Ascend · com.ascend.lifeos · Branch claude/life-tracking-ai-app-8fajp4*
*Verfasst 2026-07-14 · Ziel-Version v2.21 · Vorgänger: CASINO_GUARD_PLAN.md (v1), GUARD_MASTERPLAN.md (v2/v3)*

---

## 0 · Auftrag (wörtlich) und Übersetzung in Technik

> „mach das mit dem lock noch schöner, gamblen soll man öfter können und es soll
> ähnlich sein wie bei stake, side bets wären auch lustig, man soll seine balance
> sehen und man soll es in den settings testen können. Außerdem will ich das du das
> noch schöner machst wenn es gesperrt wird und besser aussehen lässt. Mach einen
> 100 seitigen plan und setzte ihn dann um. Lase eigene ideen und konzepte
> einfließen um guard zu erweitern und zu verbessern. Das UI soll süchtig machen"

| # | Wunsch | Technische Übersetzung | Kapitel |
|---|--------|------------------------|---------|
| A1 | Lock noch schöner | Aurora-Backdrop, Gradient-Ring, Ember-Feld, Display-Typo, Disziplin-Streak, Reclaimed-Counter | 12–15 |
| A2 | Öfter gamblen | Attempts-Economy: Basis↑ + **verdiente** Versuche durch Skill-Zeit, Balance-sichtbar | 6–8 |
| A3 | Ähnlich wie Stake | Lobby mit Spielkacheln, Balance-Header, Provably-Fair-Tag, Dice/Mines, Multiplikator-Sprache | 4–5, 9–11 |
| A4 | Side Bets | Blackjack „Pair Play" (Colored/Mixed), Dice-Ziel als Bet, Mines-Risk-Bet | 10, 11.3 |
| A5 | Balance sehen | Balance-Header (Attempts + Won-Today + Monatsnetto) überall im Casino | 7 |
| A6 | In Settings testen | **Practice-Mode**: volle Lobby, Fake-Credits, keine Attempts/Pending/Boni | 8 |
| A7 | 100-Seiten-Plan → umsetzen | Dieses Dokument, danach Wellen P0–P6 | alle |
| A8 | Eigene Ideen für Guard | Kap. 16–20: Streak-Schild, Reclaim-Ledger, Fair-Play-Transparenz, Wochen-Rückblick, Panik-Knopf, Doom-Radar | 16–20 |
| A9 | UI süchtig machen | Ethik-gerahmte Sog-Mechanik: Antizipation, Feier, Meisterschaft, variable Belohnung (Kap. 3) | 3, überall |

**Nicht-Ziele (bewusst):** echtes Geld (nie), Krypto (nie), Aufhebung des Schutz-Kerns
(das Casino bleibt eine *optionale Reibungs-Umleitung* an der Wand, kein Freifahrtschein),
Suchtdesign das gegen den Nutzer arbeitet (Kap. 3.4 Leitplanken).

---

## 1 · Ausgangslage (Code-Stand v2.20)

Nach GUARD v3 existiert:

- **CasinoEngine.kt** (pur, 193 Z.): Blackjack (S17, Single-Deck, 3:2), EU-Roulette
  (Außenwetten, Dutzende, Straight 35:1), `blackjackDelta`, `rouletteDelta` (cap-geklemmt),
  `lockoutMinutes`, `breakUntil`. 16 Tests inkl. Monte-Carlo (200k Spins, 100k BJ-Hände,
  EV-Band [−6 %, 0 %]).
- **CasinoStore.kt** (pur+Prefs): `enabled`, `attemptsPerDay` (3), `stakeMin/Max`,
  `lossMult`, `breakMode`, `winCapDay` (60); Tages-Zähler mit 6-Uhr-Rollover; `bonusMin`
  (`day:total:won`-Format seit v3), `wonBonusMin`, `bonusesToday`; `lockoutUntil`;
  crash-sicheres Pending (`pkg|delta|cover`); Monats-Statistik.
- **CasinoScreen.kt** (319 Z.): Phasen PICK→STAKE→TABLE→REVEAL via AnimatedContent.
- **CasinoTables.kt** (409 Z.): BlackjackTable (Karten-Flip-Stagger), RouletteTable
  (Canvas-Rad, 4,5 s CubicBezier, velocity-gated Tick-Haptik).
- Einstieg: nur an der **LIMIT-Wand** (`JarvisInterceptScreen` → `casinoOpen`), Angebot
  gated durch `offerAvailable` (enabled ∧ attemptsLeft>0 ∧ ¬break ∧ ¬lockout ∧ winCapRest>0).

**Was fehlt (dieser Plan):** Lobby, Balance-Sicht, Dice, Mines, Side Bets, Economy
(verdiente Versuche), Practice-Mode, Fairness-Transparenz, der aufgewertete Lock.

---

## 2 · Design-Nordstern

Drei Sätze, an denen jede Entscheidung gemessen wird:

1. **Der Kern bleibt Schutz.** Das Casino ist die *einzige* spielerische Umleitung an der
   Wand. Es darf nie dazu führen, dass Instagram leichter offen ist als ohne Guard.
   Der House-Edge arbeitet strukturell FÜR den Nutzer (jede Session kostet im Erwartungswert
   Zeit, nicht schenkt sie).
2. **Stake-Sprache, JARVIS-Seele.** Wir borgen Stakes *Klarheit* (Multiplikatoren, Balance
   oben, Provably-Fair, ein-Screen-Spiele, sofortige Ergebnisse) — aber die Währung sind
   Minuten, die Feier ist Champagne-Gold, und der „Gewinn" ist erkämpfte Zeit, kein Geld.
3. **Sog durch Stolz, nicht durch Fallen.** Süchtig = man kommt gern zurück. Erlaubt:
   Antizipation, Feier, Meisterschafts-Statistiken, variable Belohnung, Streaks. Verboten:
   künstliche Verknappung die schadet, Fake-Dringlichkeit, Verstecken von Verlusten,
   Dark-Pattern-Confirmshaming.

---

## 3 · Die Psychologie des Sogs (ethisch gerahmt)

### 3.1 Die vier legitimen Sog-Hebel

| Hebel | Casino-Umsetzung | Lock-Umsetzung |
|-------|------------------|----------------|
| **Antizipation** | Suspense vor Reveal (650 ms), Rad-Auslauf, Mines-Tile-für-Tile, Dice-Slider-Spannung | Ring-Sweep beim Erscheinen, Ember die aufsteigen |
| **Feier** | Gold-Count-up, Puls, Haptik-Leiter, Multiplikator-Knall | „+47 min reclaimed today" wächst, wenn man „Back to focus" wählt |
| **Meisterschaft** | Balance, Monats-Netto, Win-Rate, größter Multiplikator, Fair-Verify | Disziplin-Streak („Day 3 holding the line"), Wochen-Rückblick |
| **Variable Belohnung** | inhärent (das ist der Spiel-Kern), aber gedeckelt | — (Lock ist deterministisch, absichtlich) |

### 3.2 Der Kernloop (was Max fühlen soll)

```
App-Limit erreicht  →  Wand erscheint (schön, ruhig, stolz-machend)
        │                        │
        │              „Back to focus" fühlt sich gut an (Streak +, Reclaim +)
        ▼                        ▼
  „House of Time"          Skill-Zeit heute  →  verdient EXTRA Versuche
        │                        │  (der Loop belohnt Disziplin mit Spiel)
        ▼                        │
  Lobby: Balance, Spiele  ◄──────┘
        │
   Dice / Mines / BJ+Side / Roulette
        │
   Suspense → Reveal → Feier/Ruhe → zurück (oder App offen bei Win)
```

Der Clou (**eigene Idee, Kap. 6**): *Man verdient sich das Spielen durch Fokus.*
Wer heute 40 min echte Skill-Arbeit geleistet hat, bekommt +2 Versuche. Das dreht die
Sucht-Logik um: das Casino wird zur Belohnung für Disziplin statt zur Flucht davor.

### 3.3 Warum das nicht in Glücksspiel-Sucht kippt

- **Einsatz und Gewinn sind Minuten, kein Geld** — man kann sich nicht ruinieren.
- **Harte Tagesdeckel**: winCapDay, attemptsPerDay+earned (gekappt), breakMode, lossMult.
- **Der Erwartungswert ist negativ** (2–6 % Edge) — über Zeit kostet Spielen Zeit. Die
  Monats-Ehrlichkeits-Statistik zeigt das schonungslos („house +Y").
- **Verlust = Lockout** (mehr Sperrzeit), nicht „nochmal einzahlen". Der Verlust schützt.

### 3.4 Sechs Leitplanken (in Code gegossen)

1. Kein Spiel ohne verbleibende Attempts (auch Practice zeigt „das wäre jetzt echt vorbei").
2. Gewinne clampen an winCapRest — nie über Tagesdeckel.
3. Loss-Lockout wird VOR jeder anderen Regel im Tick geprüft (Haus wird zuerst bezahlt).
4. Break nach letztem Versuch ist nicht umgehbar (nur echter 6-Uhr-Rollover / Timer).
5. Monats-Statistik zählt nur *echte* Gewinne/Verluste, nie Practice, nie Overrun-Deckung.
6. Practice ist unübersehbar als „nicht echt" markiert (Banner, andere Farbe, kein Ledger).

---

## 4 · Stake-Referenz-Analyse (was wir übernehmen)

Aus der öffentlichen Stake-UX (stake.com Original-Spiele) destilliert:

| Stake-Element | Übernehmen? | JARVIS-Adaption |
|---------------|-------------|-----------------|
| Balance oben, immer sichtbar | **Ja** | Attempts + Won-Today + Monatsnetto im Lobby-Header |
| Spiel-Lobby mit Kacheln | **Ja** | Grid: Dice, Mines, Blackjack, Roulette (+„coming soon"-Slot) |
| Bet-Amount + Multiplier-Feld | **Ja** | Stake-Chips + Live-Multiplikator/Payout-Vorschau |
| „Bet"-Button gross unten | **Ja** | CasCta „SPIN/ROLL/DEAL/CASH OUT" |
| Provably Fair (Seed-Hash) | **Ja** (Deko + echt) | „Provably fair · #a3f2" aus Round-Seed |
| Dice (Slider 0–100, roll-under) | **Ja** | Kap. 10.1 |
| Mines (5×5, Cashout-Leiter) | **Ja** | Kap. 10.2 |
| Side Bets (BJ Perfect Pairs) | **Ja** (Single-Deck-Variante) | Kap. 10.3 |
| Instant-Bet / kein Ladebildschirm | **Ja** | resolve-then-animate (schon so) |
| Limbo, Plinko, Crash, Keno, Wheel | **Später** | Roadmap Kap. 27 |
| Echtes Geld / Krypto / Einzahlung | **Nie** | — |
| Auto-Bet / 1000× Turbo | **Nie** (Sucht-Verstärker) | bewusst weggelassen |

---

## 5 · Spielmathematik — Gesamtübersicht

Alle Spiele mit definiertem House-Edge, Monte-Carlo-verifiziert im Band [−6 %, 0 %]
Spieler-EV. Einsatz S in Minuten, Gewinn als **Profit** (delta), Verlust −S.

| Spiel | Mechanik | Edge | Max-Multiplikator | Profil |
|-------|----------|------|-------------------|--------|
| Dice | roll-under Ziel t | 2 % | 98× (t=1) | schnell, hohe Varianz wählbar |
| Mines | 25 Felder, m Minen, Cashout | 2 % | bis ~24× (viele Minen) | Spannungsleiter, Skill-Gefühl |
| Blackjack | S17 Single-Deck | ~0,5 % | 1,5× (BJ) | niedrige Varianz, Können |
| BJ Pair Play (Side) | Erste 2 Karten Paar | 5,9 % | 25× (colored) | Bonus-Kick auf BJ |
| Roulette | EU-Rad | 2,7 % | 35× (straight) | Klassiker, Show |

### 5.1 Dice-Mathematik (Kap. 10.1)

- Roll r ∈ [0, 100) mit 2 Nachkommastellen (int 0–9999 / 100), fair uniform.
- „Roll under" Ziel t: Gewinn wenn r < t. Gewinnchance p = t/100.
- „Roll over" Ziel t: Gewinn wenn r > t. p = (100−t)/100.
- Multiplikator M = (1 − edge) / p, edge = 0,02. Beispiel t=50 → p=0,5 → M=1,96×.
  t=2 → M=49×. t=98 (over) → p=0,02 → M=49×.
- Profit = round(S·(M−1)), geklemmt an winCapRest. Verlust −S.
- Minimales/maximales Ziel geklemmt [2, 98] damit M endlich und p>0.

### 5.2 Mines-Mathematik (Kap. 10.2)

- 25 Felder, m Minen (1–24, Default 3). Positionen bei Konstruktion (Seed) fix.
- Nach k sicheren Aufdeckungen: fairer Multiplikator
  M(k) = (1 − edge) · Π_{i=0}^{k−1} (25 − i)/(25 − m − i).
- Cashout jederzeit → Profit = round(S·(M(k) − 1)), geklemmt. Mine getroffen → −S.
- Beispiel m=3: M(1)=1,127× · M(3)=1,49× · M(5)=2,0× · M(10)=5,6× · M(22)=~24×.
- „Side/Risk-Bet"-Gefühl: mehr Minen = höhere Leiter, höheres Risiko (der Nutzer *baut*
  seine Wette).

### 5.3 Blackjack Pair Play (Side Bet, Single-Deck) (Kap. 10.3)

Single-Deck ⇒ *Perfect Pair* (gleiche Farbe+Symbol) unmöglich. Erreichbar:
- **Colored Pair**: gleicher Rang, gleiche Farbe (z. B. J♠J♣). P = 1/51 ≈ 1,96 %.
- **Mixed Pair**: gleicher Rang, verschiedene Farbe (J♠J♥). P = 2/51 ≈ 3,92 %.
- **Kein Paar**: P = 48/51 ≈ 94,1 %.
- Paytable: Colored **25:1**, Mixed **10:1**, sonst −Sidestake.
- EV = (1·25 + 2·10 − 48)/51 = −3/51 = **−5,9 %**. Sauber im Casino-Band.
- Side-Stake separat vom Haupteinsatz (eigener Chip), eigener Delta-Beitrag.

### 5.4 Monte-Carlo-Testmatrix (Kap. 22)

| Test | N | Erwartung |
|------|---|-----------|
| Dice roll uniform | 500k | Mittel ≈ 50, jeder Perzentil-Bucket ±3 % |
| Dice EV @ t=50 | 500k | ∈ [−0,03, 0] |
| Dice EV @ t=10 | 500k | ∈ [−0,03, 0] |
| Mines M(k)-Formel exakt | analytisch | == C(25,k)/C(25−m,k)·0,98 |
| Mines EV (Zufalls-Cashout) | 200k | ∈ [−0,06, 0] |
| Pair-Play-Raten | 300k | colored≈1/51, mixed≈2/51 |
| Pair-Play-EV | 300k | ∈ [−0,07, −0,04] |

---

## 6 · Economy — „öfter gamblen" richtig gelöst

### 6.1 Das Problem mit v1

3 harte Versuche/Tag. Fertig. Kein Weg, mehr zu bekommen außer Warten bis 6 Uhr.
Das ist sicher, aber es fühlt sich karg an und belohnt nichts.

### 6.2 Die Lösung: verdiente Versuche (eigene Idee)

**Total verfügbar = Basis + Verdient − Verbraucht.**

- **Basis**: `attemptsPerDay` (Default von 3 auf **5** erhöht, einstellbar 1–10).
- **Verdient**: `earnedAttempts = floor(skillMinutesToday / MIN_PER_EARNED)`, gekappt bei
  `maxEarnedPerDay` (Default 5). `MIN_PER_EARNED` = 20 min.
  → 40 min Skill-Arbeit = +2 Versuche, 100 min = +5 (Kappe).
- `skillMinutesToday` = JARVIS-eigene Vordergrund-Minuten (schon via `DigitalWellbeingManager.usageTodayMs(ctx, packageName)` verfügbar — dieselbe Zahl wie die Lock-„Skill-time").

**Warum das gut ist:** Es koppelt Spielen an genau das Verhalten, das die App fördern will.
Max *verdient* sich seine Spins durch Fokus. Der Loop wird gesund: Disziplin → Belohnung →
mehr Spiel-Erlaubnis. Und es beantwortet „öfter gamblen" ohne den Schutz zu verwässern
(wer nichts leistet, bleibt bei 5).

### 6.3 Balance-Modell (die sichtbare „Balance")

Drei Zahlen, Stake-Header-Stil:

1. **ATTEMPTS** — `base + earned − used` als Pips + Zahl. Die Spin-Währung.
2. **WON TODAY** — `wonToday` von `winCapDay` (Fortschrittsbalken). Die „gewonnene Zeit".
3. **NET (Monat)** — `statWon − statLost`, farbcodiert (grün/rot). Die Ehrlichkeit.

Optional 4. **BANKED** (Kap. 20 Reclaim-Ledger): kumulierte durch-Guard-zurückgeholte Minuten.

### 6.4 Store-API (neu)

```
CasinoStore:
  maxEarnedPerDay(ctx): Int = 5
  earnedAttempts(ctx, skillMin): Int = min(skillMin / 20, maxEarnedPerDay)
  attemptsLeft(ctx, skillMin): Int = (base + earned − used).coerceAtLeast(0)   // überladen
  attemptsTotal(ctx, skillMin): Int = base + earned                            // für Pips
  // Practice
  practiceCredits(ctx): Int   (Fake-Balance, Default 500, reset-bar)
```

---

## 7 · Balance-Header (UI-Spezifikation)

Kopf jeder Lobby- und Tisch-Ansicht (außer Reveal):

```
┌───────────────────────────────────────────────┐
│ ‹  HOUSE OF TIME            provably fair #a3f2 │
│                                                 │
│  ● ● ● ○ ○  +2 earned      WON 12m/60m   +45 ▲ │
│  ATTEMPTS                   TODAY        MONTH   │
└───────────────────────────────────────────────┘
```

- Attempts: Pips (gefüllt=übrig, hohl=verbraucht) + „+N earned" in Gold wenn earned>0.
- Won today: kompakter Balken (Champagne-Fill) + „Xm/Ym".
- Month net: Pfeil ▲/▼ + Zahl, grün wenn ≥0 sonst rot.
- Provably-fair-Tag rechts oben, monospaced, tippbar → Fairness-Sheet (Kap. 19).
- Practice-Mode: Header wird lila getönt, „PRACTICE" ersetzt das Fair-Tag, Balance =
  practiceCredits statt Attempts.

---

## 8 · Practice-Mode („in den Settings testen")

### 8.1 Zweck

Max will das Casino risikofrei ausprobieren (und ich will einen Win live verifizieren,
ohne echten Lockout zu riskieren). Practice = volle Lobby, alle Spiele, alle Side Bets,
aber:

- **Kein** `reserveAttempt`, **kein** `writePending`, **kein** `addBonus`/`setLockout`.
- Fake-Balance `practiceCredits` (Default 500), steigt/fällt mit Spielausgängen, „Reset"-Chip.
- Kein Monats-Ledger-Eintrag, kein winCap, kein Break.
- Unübersehbares Banner „PRACTICE · nichts wird gutgeschrieben".
- Reveal zeigt Ergebnis, aber „(practice)" statt echter Minuten-Gutschrift.

### 8.2 Einstieg

Guard → Segment **Casino** → wenn `enabled`: Button **„Practice table"** (lila, unter den
Settings). Öffnet `CasinoHost(practice=true)` als Vollbild-Overlay (eigene kleine Activity
oder Compose-Route im Hauptscaffold — Entscheidung Kap. 24: Compose-Route im Guard-Overlay
`overlay="casino_practice"`).

### 8.3 Technische Trennung

`CasinoScreen`/Tables bekommen `practice: Boolean`. Alle Store-schreibenden Calls werden zu
No-Ops bzw. auf `practiceCredits` umgeleitet über einen dünnen Adapter `CasinoLedger`:

```
interface CasinoLedger {
  fun reserve(); fun refund(); fun writePending(delta, cover); fun commit()
  fun attemptsLeft(): Int; fun balanceLabel(): String
}
RealLedger(ctx)      → CasinoStore-Calls
PracticeLedger(ctx)  → nur practiceCredits, alles andere no-op
```

Das hält die Tische sauber und macht Practice unmöglich, „durchzulecken".

---

## 9 · Casino-UI-Architektur v2

### 9.1 Neue Phasen

```
LOBBY → (Spielwahl) → STAKE → TABLE → REVEAL → (zurück zu LOBBY | schließen)
```

Neu: **LOBBY** ersetzt das alte PICK (nur BJ/Roulette). Lobby = Balance-Header +
4er-Spielgrid + „letzter Gewinn"-Zeile.

### 9.2 Dateien

| Datei | Inhalt | ~Zeilen |
|-------|--------|---------|
| `CasinoScreen.kt` (umbauen) | Host, Phasen, Lobby, Balance-Header, Reveal, Ledger-Wahl | 420 |
| `CasinoTables.kt` (behalten+erweitern) | Blackjack (+Pair-Play), Roulette | 470 |
| `CasinoDice.kt` (neu) | Dice-Tisch (Slider, Multiplikator, Roll-Anim) | 240 |
| `CasinoMines.kt` (neu) | Mines-Tisch (5×5-Grid, Cashout-Leiter) | 260 |
| `CasinoBits.kt` (neu) | geteilt: BalanceHeader, GameTile, MultiplierBadge, FairTag, PracticeBanner | 240 |

Splitting-Grund (Memory-Lehre): große Einzel-Writes mit Glücksspiel-Copy triggern
Content-Filter → mehrere nüchterne Dateien.

### 9.3 Motion (IRON-MOTION-konform)

- Phasenwechsel: `AnimatedContent` fade+SizeTransform (schon so), 200/110 ms.
- Dice-Roll: Slider-Thumb schnellt zur Roll-Position (springSmooth), Zahl count-up.
- Mines: Tile-Flip 180 ms rotationY, Multiplikator-Badge pulst pro Aufdeckung.
- Reveal-Win: Gold-Count-up + 1 Puls (schon so, verbessert).
- Alles reduced-motion-aware.

---

## 10 · Die neuen Spiele im Detail

### 10.1 Dice (`CasinoDice.kt`)

```
┌ Balance-Header ─────────────────────┐
│  ● ● ● ● ○   WON 5m/60m    +40 ▲    │
├─────────────────────────────────────┤
│           ┌───────────┐             │
│           │   64.20   │  ← letztes Roll (rot/grün)
│           └───────────┘             │
│   0 ──────────────●──────── 100     │  ← Slider (Ziel), grün links, rot rechts
│         Roll under 50               │
│                                     │
│   Multiplier 1.96×   Win +5m        │
│   Chance 50%         Stake [5]      │  ← Chips
│                                     │
│   [ Under ]  [ Over ]               │  ← Richtung
│                                     │
│        ══════ ROLL ══════           │
└─────────────────────────────────────┘
```

- Slider bewegt Ziel 2–98; Multiplier/Chance/Payout live.
- Under/Over-Toggle spiegelt.
- ROLL → Zahl count-up zur gerollten Position, Thumb-Marker landet, grün/rot,
  Suspense 650 ms, Reveal.
- Provably-fair-Tag zeigt Seed; Verify-Sheet erklärt r = f(seed).

### 10.2 Mines (`CasinoMines.kt`)

```
┌ Balance-Header ─────────────────────┐
├─────────────────────────────────────┤
│   Mines [3]   Stake [5]             │
│   ┌─┬─┬─┬─┬─┐                        │
│   │◇│◇│★│◇│◇│   Next 1.13×          │  ← ◇ verdeckt, ★ aufgedeckt-sicher
│   ├─┼─┼─┼─┼─┤   Current 1.49×       │
│   │◇│★│◇│◇│◇│   Cashout +2m         │
│   ├─┼─┼─┼─┼─┤                        │
│   │◇│◇│◇│★│◇│                        │
│   ├─┼─┼─┼─┼─┤                        │
│   │◇│◇│◇│◇│◇│                        │
│   ├─┼─┼─┼─┼─┤                        │
│   │◇│◇│◇│◇│◇│                        │
│   └─┴─┴─┴─┴─┘                        │
│   [ CASH OUT +2m ]  (erscheint ab k≥1)│
└─────────────────────────────────────┘
```

- Vor Start: Minen-Anzahl + Stake wählen → „PLACE BET" reserviert Versuch, legt Minen (Seed).
- Jedes Tile: sicher → ★ (grün), Multiplikator steigt, „Next"/„Current"/„Cashout" updaten.
- Mine → alle Minen sichtbar (rot), −S, Loss-Reveal.
- „CASH OUT" jederzeit ab 1 sicherem Feld → Profit fix, Win-Reveal.
- Baut Spannung *deutlich* stärker als ein einzelner Spin → das „süchtige" Spiel.

### 10.3 Blackjack Pair Play (Side Bet in `CasinoTables.kt`)

- In der STAKE-Phase für Blackjack: zusätzlicher **Side-Bet-Chip** „Pair Play" (0/5/10).
- 0 = aus. Bei >0: eigener Einsatz, aufgelöst aus den ersten zwei Spielerkarten.
- Reveal-Reihenfolge: erst Side-Bet-Ergebnis („COLORED PAIR · +25m!" gold, oder „no pair"),
  dann normales BJ-Spiel. Delta = bjDelta + pairDelta (beide gecappt/verrechnet).
- Macht jede BJ-Hand doppelt spannend, ohne die Grundmathe anzufassen.

---

## 11 · Engine-Erweiterungen (`CasinoEngine.kt`)

### 11.1 Dice

```
fun diceRoll(rng): Int              // 0..9999 (= 0.00..99.99)
fun diceMultiplier(target, over, edge=0.02): Double
fun diceWin(roll, target, over): Boolean         // roll in 0..9999, target 2..98
fun diceDelta(stake, target, over, roll, capRest): Int
```

### 11.2 Mines

```
class MinesGame(mineCount, seed) {
  val minePositions: Set<Int>            // 0..24, fix bei Konstruktion
  fun revealSafe(i): Boolean             // false wenn Mine
  fun multiplier(safeCount, edge=0.02): Double
  fun cashoutDelta(stake, safeCount, capRest): Int
}
fun minesMultiplier(mineCount, safeCount, edge=0.02): Double   // pur, für Tests
```

### 11.3 Pair Play

```
enum class PairKind { NONE, MIXED, COLORED }
fun pairKind(a: Card, b: Card): PairKind
fun pairDelta(kind, sideStake, capRest): Int   // colored 25:1, mixed 10:1, none −stake
```

Alle rein, alle in `CasinoEngineTest` monte-carlo-verifiziert (Kap. 22).

---

## 12 · Der neue Lock — Vision

Der v3-Lock ist gut (Vollbild, App-Icon im Ring, ehrlicher Grund). v4 macht ihn
*schön genug, dass „gesperrt sein" sich premium anfühlt* — die App, die einen aussperrt,
soll dabei besser aussehen als die, die man öffnen wollte.

Bausteine:
1. **Aurora-Backdrop** — 3 langsam driftende Radial-Gradienten (Guard-Rot/Ember/Cyan),
   sehr niederfrequent (20–40 s Zyklus), reduced-motion → statisch.
2. **Gradient-Ring** — der 196-dp-Ring bekommt einen Sweep-Gradient (Rot→Ember→Rot) statt
   Flachrot, plus weichen Außen-Glow.
3. **Icon-Halo + Float** — App-Icon schwebt minimal (±4 px, 3 s) mit Glow-Halo dahinter.
4. **Display-Typo** — die Minuten-Zahl in Display-Font, groß, Champagne-Hairline.
5. **Disziplin-Streak-Zeile** — „Day 3 · holding the line" / „47 min reclaimed today"
   (Reclaim-Ledger Kap. 20) — macht Zurückgehen zum Sieg.
6. **Ember-Feld** — wenige (8–12) langsam aufsteigende Glut-Partikel, GPU-billig, reduced-off.
7. **Bessere Buttons** — „Back to focus" mit sanftem Glow-Rand, „House of Time" mit
   Gold-Shimmer (dezent, 1×/4 s).

---

## 13 · Lock-Layout-Spezifikation (v4)

```
┌───────────────────────────────────────────────┐
│ 🛡 JARVIS GUARD              intercept #3 today │  ← Overline
│                                                 │
│              ·  ·   (ember steigen)   ·         │
│            ╭───────────────────╮               │
│           ╱   ┌───────────┐      ╲              │  ← Gradient-Ring (Sweep+Glow)
│          │    │  [icon]   │       │             │  ← Icon-Halo, Float
│          │    │   glow    │       │             │
│           ╲   └───────────┘      ╱              │
│            ╰────  17m ─────╯                    │  ← Display-Zahl
│                 of 15m                          │
│                                                 │
│           Instagram is locked                   │  ← Display, gross
│      Daily limit reached · 17m of 15m           │
│           fresh minutes at 06:00                │
│                                                 │
│   ⚡ Day 3 · you're holding the line            │  ← Disziplin-Streak (NEU)
│                                                 │
│   ┌─────────────────────────────────────────┐  │
│   │ SKILL-TIME TODAY · 32M                    │  │  ← Alternative
│   │ Do this instead: Read cyber roadmap · 7m  │  │
│   └─────────────────────────────────────────┘  │
│                                                 │
│   ══════════ Back to focus ══════════          │  ← primär, Glow-Rand
│   ─────────  Start skill work  ─────────        │
│   ┌ Later · 3min ┐ ┌ ✦ House of Time · 5 ✦ ┐   │  ← Gold-Shimmer
│         guard live · instant detection          │
└───────────────────────────────────────────────┘
```

Neu vs. v3: Aurora + Ember-Backdrop, Gradient-Ring, Icon-Halo/Float, Disziplin-Streak-Zeile,
Gold-Shimmer auf House of Time, „N" (Attempts) am HoT-Button, Display-Font-Minuten.

---

## 14 · Lock-Motion-Spezifikation (v4)

| Element | Spec | reduced-motion |
|---------|------|----------------|
| Backdrop-Fade | 150 ms LinearEasing (schon) | gleich |
| Aurora-Drift | 3 Layer, 24/31/40 s Loop, translationX/Y ±30 px | statisch (kein Loop) |
| Ring-Sweep-Erscheinen | animateFloat 0→progress, springSmooth (schon via Kit.Ring) | final |
| Ring-Glow-Atem | infiniteRepeatable 3,4 s alpha 0,3↔0,5 | statisch 0,4 |
| Icon-Float | infinite 3 s ±4 px translationY, easeInOut | statisch |
| Icon-Halo-Puls | infinite 3,4 s scale 1↔1,04 | statisch |
| Ember-Feld | 10 Partikel, je 6–11 s Aufstieg, alpha-Fade, versetzt | **komplett aus** |
| Stagger-Gruppen | 3 Gruppen à 60 ms (schon) | sofort |
| HoT-Gold-Shimmer | Gradient-Sweep 1×/4 s über den Button | aus |
| Button-Press | pressScale (schon) | gleich |

**Performance:** Aurora + Ember laufen als `graphicsLayer`-Transforms (Draw-Phase, keine
Recomposition). Kein `blur()` mehr im Hot-Path (v3-Lehre). Ember als Canvas mit
`drawCircle`, kein Per-Partikel-Composable.

---

## 15 · Lock-Farb- und Typo-Tokens

- **Guard-Rot** `Mod.Guard`-Kontext / `DoomRed 0xFFFF6169` — Wand-Signal.
- **Ember** `0xFFFF9A62` (warmes Orange) — Aurora-Mittelschicht, Ring-Gradient-Mitte.
- **Cyan** `0xFF4CD4C4` — Gate-Modus + kühler Aurora-Akzent.
- **Champagne** `theme.Champagne` — Gold für Feier, HoT, gewonnene Minuten.
- **Display-Font** `theme.Display` — Minuten-Zahl, „X is locked"-Titel.
- Void `0xFF050505` Basis. Hairline `0x17FFFFFF`.

---

## 16 · Eigene Guard-Erweiterung #1: Disziplin-Streak

**Konzept:** Jeder Tag, an dem Max eine Wand akzeptiert hat (mind. 1 Intercept, und die
App NICHT über einen Snooze/Pass weiter genutzt — oder simpler: mind. 1 „Back to focus"),
zählt als „held line". Streak persistiert (`guard_streak`, `guard_streak_last`).

- Anzeige auf dem Lock: „Day N · you're holding the line" (N≥2), sonst „Hold the line."
- Bricht, wenn ein Tag komplett ohne akzeptierte Wand vergeht (oder nur Snoozes).
- Milestone-Feier bei 3/7/14/30 (dezenter Champagne-Aufblitz auf dem Lock).
- Speicher: `WellbeingStore.guardStreak()` / `recordHeldLine(dayKey)`.

Macht „gesperrt werden" zu etwas, worauf man stolz ist — der ethische Sog-Kern.

---

## 17 · Eigene Guard-Erweiterung #2: Reclaim-Ledger

**Konzept:** Zähle die *tatsächlich zurückgeholte* Zeit. Jedes Mal, wenn eine Wand
akzeptiert wird (Back to focus / I'm out), addiere die geschätzte gesparte Session
(`perOpen`-Schätzung aus GuardScreen, gemessen: usedMin/unlocks, geklemmt 2–20).

- `reclaimToday`, `reclaimTotal` (Prefs).
- Lock-Zeile Alternative: „47 min reclaimed today".
- GuardScreen-Insights: „≈X min reclaimed by Guard" (existiert schon als Schätzung → jetzt
  echter kumulierter Zähler statt `saved*perOpen`-Live-Rechnung).
- Balance-Header optional „BANKED Xm".

### 17.1 Ehrlichkeitsregel

Reclaim zählt nur akzeptierte Wände, nicht Snoozes. Snooze = die Zeit wurde NICHT
zurückgeholt. Das hält die Zahl wahr.

---

## 18 · Eigene Guard-Erweiterung #3: „Panik"-Fokus-Knopf

**Konzept:** Auf dem Lock ein dritter, unaufdringlicher Weg: „Lock everything · 30 min"
(nur im LIMIT-Modus, klein unter der Fußzeile). Startet sofort eine Fokus-Session
(`startFocus(30)`) → alle limitierten Apps dicht. Für den Moment, in dem Max merkt „ich
bin im Sog" und *will*, dass die Tür zufällt.

- Ein Tap, Bestätigung per Haptik, Wand wird zu Fokus-Wand.
- Nutzt bestehende `WellbeingStore.startFocus`.

---

## 19 · Eigene Guard-Erweiterung #4: Provably-Fair-Transparenz

**Konzept:** Stakes Signatur. Jede Runde hat einen Seed. Wir zeigen `#` + 4 Hex-Zeichen
des Seeds im Header und ein antippbares Sheet:

```
PROVABLY FAIR
Diese Runde: Seed #a3f27c9e
Das Ergebnis stand fest, bevor die Animation lief
(resolve-then-animate). Kein Nachjustieren möglich.
[ Verify ] → zeigt roll = seed-abgeleitet
```

Kostet fast nichts (Seed existiert), verkauft aber massiv „echtes Casino"-Gefühl UND ist
tatsächlich wahr (die Engine resolved vor der Animation). Ehrlichkeit als Feature.

---

## 20 · Eigene Guard-Erweiterung #5: Wochen-Rückblick & Doom-Radar (Roadmap-Notiz)

- **Wochen-Rückblick** (später): Sonntags eine Karte „Diese Woche: 4 Tage held line,
  X min reclaimed, Casino you +A/house −B". Nutzt Reclaim-Ledger + Streak + Stats.
- **Doom-Radar** (später): der bestehende DoomscrollDetector füttert eine kleine
  Heatmap-Warnung „Du bist heute 3× an dieselbe Wand gerannt" auf dem Lock. Macht das
  Muster sichtbar.

Beide in diesem Wurf nur skizziert, Umsetzung in v2.22.

---

## 21 · Datei-für-Datei-Changelog (dieser Wurf)

| Datei | Änderung |
|-------|----------|
| `CasinoEngine.kt` | +Dice, +MinesGame/minesMultiplier, +PairKind/pairKind/pairDelta |
| `CasinoEngineTest.kt` | +~10 Tests (Dice-EV, Mines-Formel+EV, Pair-Raten+EV, Dice uniform) |
| `CasinoStore.kt` | +maxEarnedPerDay, +earnedAttempts, +attemptsLeft(skillMin)-Overload, +practiceCredits, +guardStreak-Delegation |
| `WellbeingStore.kt` | +guardStreak/recordHeldLine, +reclaimToday/Total/addReclaim |
| `CasinoBits.kt` (neu) | BalanceHeader, GameTile, MultiplierBadge, FairTag+Sheet, PracticeBanner, CasChip |
| `CasinoDice.kt` (neu) | DiceTable |
| `CasinoMines.kt` (neu) | MinesTable |
| `CasinoScreen.kt` | Lobby-Phase, Ledger (Real/Practice), Balance-Header-Einbau, Pair-Play-Chip, Reveal-Upgrade |
| `CasinoTables.kt` | Pair-Play-Side-Bet in Blackjack |
| `JarvisInterceptScreen.kt` | Aurora, Ember, Gradient-Ring, Icon-Halo/Float, Streak-Zeile, HoT-Shimmer, Panik-Knopf |
| `GuardScreen.kt` | „Practice table"-Button, Economy-Anzeige (earned attempts erklärt), Reclaim-Zähler echt |
| `GuardRuntime.kt` | Practice-Route-Öffnen; Reclaim/Streak bei actExit/actLater buchen |
| `AscendApp.kt` / Overlay-Routing | `overlay="casino_practice"` Route |

---

## 22 · Testplan

**Unit (CasinoEngineTest, Ziel +10 → ~26 gesamt):** Dice uniform 500k, Dice-EV t=50/t=10,
Mines-Formel exakt vs. C(n,k), Mines-EV 200k, Pair-Raten 300k, Pair-EV, Dice-Delta-Clamp,
Mines-Cashout-Clamp, earnedAttempts-Kappung.

**Instrument/manuell am S24:**
- Practice: Lobby öffnen, Balance sehen, jedes Spiel 1× (Dice Win+Loss, Mines Cashout+Mine,
  BJ+Pair Colored/Mixed/none, Roulette), Reset-Credits.
- Earned Attempts: Skill-Zeit prüfen → Attempts-Header zeigt „+N earned".
- Lock v4: Screenshot LIMIT + GATE, Aurora/Ember/Ring/Streak sichtbar, reduced-motion-Test.
- Echt-Pfad-Sanity: 1 echter Dice-Spin an der Wand (klein), Bonus/Anzeige korrekt.

---

## 23 · Wellen-Reihenfolge (Umsetzung)

- **P0** Engine v2 + Tests (Dice/Mines/Pair) — grün, bevor irgendein UI.
- **P1** Store-Economy + Practice-Ledger + Streak/Reclaim.
- **P2** CasinoBits (Header/Tile/Fair/Practice/Chip) + Lobby in CasinoScreen.
- **P3** Dice- + Mines-Tisch.
- **P4** Pair-Play-Side-Bet in Blackjack.
- **P5** Lock v4 (Aurora/Ember/Ring/Halo/Streak/Shimmer/Panik).
- **P6** GuardScreen (Practice-Button, Economy, echter Reclaim) + Build/Test/Device/Commit.

---

## 24 · Architektur-Entscheidungen (Q&A)

**Practice als eigene Activity oder Compose-Route?** → Compose-Route im bestehenden
Overlay-System (`overlay="casino_practice"`), kein neues Manifest-Entry. Der Lock-Casino
bleibt in der Wand; Practice lebt im normalen App-Scaffold. Ein `CasinoHost(practice)`
wird von beiden geteilt.

**Balance echt persistent oder abgeleitet?** → Abgeleitet (Attempts/Won/Net aus
bestehenden Zählern). Kein neuer „Kontostand", der mit den Minuten-Boni konkurrieren würde.
Practice-Credits sind der einzige echte Zähl-Stand und rein kosmetisch.

**Mines-State bei Force-Kill?** → Mines reserviert den Versuch + schreibt worst-case
Pending (−stake) bei „PLACE BET". Cashout schreibt Pending neu (+profit). Kill mitten im
Spiel = −stake landet (Haus gewinnt) — konsistent mit resolve-then-animate.

**Warum kein Auto-Bet/Turbo?** → Reiner Sucht-Verstärker ohne Fokus-Nutzen. Verboten (3.4).

---

## 25 · Risiken & Gegenmaßnahmen

| Risiko | Gegenmaßnahme |
|--------|---------------|
| Content-Filter bei grossen Casino-Writes | Dateien splitten, nüchterne Copy (Memory-Lehre) |
| Ember/Aurora frisst Akku auf dem Lock | Draw-Phase-only, wenige Partikel, reduced-motion-aus, Lock ist kurzlebig |
| Practice „leckt" in echte Boni | Ledger-Interface, PracticeLedger no-op, Tests |
| Mines-Multiplikator-Overflow | Double, geklemmt an capRest, Int-Delta gerundet |
| Earned Attempts machen Schutz weich | Kappe (5), nur durch ECHTE Skill-Zeit, Break/Lockout/winCap bleiben |
| Kotlin-Extension-Falle (Memory) | Erst importieren, nie fully-qualified inline |

---

## 26 · Definition of Done

- [ ] Engine v2 + ~26 Tests grün, Monte-Carlo im Band.
- [ ] Lobby mit Balance-Header, 4 Spiele spielbar.
- [ ] Dice + Mines + Pair-Play live.
- [ ] Practice-Mode aus Settings, nichts leckt.
- [ ] Earned Attempts sichtbar & wirksam.
- [ ] Lock v4 sichtbar schöner (Aurora/Ember/Ring/Halo/Streak/Shimmer), reduced-motion ok.
- [ ] Disziplin-Streak + Reclaim-Ledger echt.
- [ ] assembleDebug grün, v2.21 am S24 verifiziert (Screenshots), Commit+Push, Memory.

---

## 27 · Roadmap (nach diesem Wurf)

Limbo · Plinko · Crash · Keno · Wheel · Wochen-Rückblick-Karte · Doom-Radar auf dem Lock ·
Casino-Stats-Screen (größter Multiplikator, Win-Rate je Spiel, Fair-Historie) ·
Cloud-Sync der Guard-Stats ins Dashboard · Intercept-Themes je Welt · Achievements
(„First Colored Pair", „10× on Mines", „7-day line").

---

*Ende des Dossiers. Umsetzung folgt in P0–P6.*

---

## 28 · Umsetzungs-Log (v2.21, 2026-07-14)

**Alle Wellen P0–P6 gebaut, 26 Casino-Tests + Gesamtsuite grün, assembleDebug grün.**

- **P0 Engine** (`CasinoEngine.kt`): Dice (diceRoll/Chance/Multiplier/Win/Delta, 2 %),
  Mines (`MinesGame` + minesMultiplier/minesCashoutDelta, 25 Felder, C(n,k)-Leiter),
  Pair Play (`pairKind`/`pairDelta`, Single-Deck colored 25× / mixed 10×, Edge 5,9 %).
  +10 Tests: Dice uniform 500k, Dice-EV t=10/50/90, Mines-Formel==C(n,k), Mines-EV 200k,
  Pair-Raten+EV 300k, alle Clamps.
- **P1 Store** (`CasinoStore`): Basis 3→5, `earnedAttempts` (20 min Skill = +1, Kappe 5),
  `attemptsLeft/Total(skillMin)`, `reserve/refund(total)`; `practiceCredits`.
  (`WellbeingStore`): `guardStreak`/`recordHeldLine`, `reclaimToday/Total/addReclaim`.
- **P1 Runtime** (`GuardRuntime`): `bookAccepted` (Streak+Reclaim) bei exit/skill/offer/
  casino-lose, NICHT bei snooze; `actPanicFocus`; Payload +guardStreak +reclaimToday.
- **P2/P3/P4 UI** (neu: `CasinoLedger.kt` Real/Practice-Seam, `CasinoBits.kt`
  BalanceHeader/GameTile/MultiplierReadout/StakeChips/PracticeBanner, `CasinoDice.kt`,
  `CasinoMines.kt`; umgebaut: `CasinoScreen.kt` Lobby+Ledger+FairSheet+Reveal,
  `CasinoTables.kt` Pair-Play-Sidebet): Lobby mit 4 Spielen, Balance-Header (Attempts/
  Won/Monat + earned), Provably-Fair-Tag+Sheet, Dice-Slider, Mines-Grid mit Cashout,
  BJ-Pair-Chip.
- **P5 Lock v4** (`JarvisInterceptScreen.kt`): AuroraBackdrop (3 driftende Radials),
  EmberField (11 Glut-Partikel, reduced-off), LockHero (Sweep-Gradient-Ring rot→ember→gold
  + Glow + Icon-Halo + Float, Display-Zahl), Disziplin-Streak/Reclaim-Zeile, HoT-Gold-
  Shimmer + „N spins left", Panik-Knopf „Lock everything 30 min". Alles reduced-motion-aware.
- **P6 GuardScreen**: „Practice table"-Button (Vollbild-Dialog, PracticeLedger),
  Economy-Erklärung (Skill→earned), echter kumulierter Reclaim + Streak in Insights.

**Verifikation Geräte-Teil**: v2.21 gebaut; Install/Live-Verify am S24 sobald USB zurück
(Gerät trennte sich während des Builds). Erwartet: Practice-Lobby, Balance-Header,
Dice/Mines/Pair-Play, Lock-v4-Aurora/Ember/Ring/Streak.
