# BEST DESIGN — the editorial luxe overhaul

*A complete visual redesign of JARVIS in white + sapphire blue, inspired by the "Vitality" reference set, applied to the whole app — plus a full net-worth Finance tab, a de-cluttered surface, and a huge in-app documentation.*

Owner: Max. Reference: six screenshots in `/Best design` from a Patreon build ("Vitality"). This document is the spec + the checklist so nothing is lost.

---

## PART 0 — WHAT MAX ASKED (decomposed)

1. **New design idea, implemented in white + blue** — both beautiful shades, genuinely luxurious. Plus other cool accent tones I invent.
2. **Strongly inspired by the reference images** (the "Vitality" app).
3. **Expand the Finance tab** like the reference (net-worth tracker).
4. **Make the whole app prettier by removing unnecessary explanations** — keep only what matters, like the reference.
5. **A huge structured Documentation in Settings** that explains every feature, tool, *and algorithm*, how to use everything — leaving nothing out, generous, so it's clear for Max.
6. **Apply the design to the ENTIRE app — everything.**
7. **Verify at the end** that it's truly perfectly done.
8. **Write a detailed plan first** (this document).

## PART 1 — THE REFERENCE, READ

The reference DNA, extracted from the six images:
- **Deep near-black ground** with a faint radial glow; extreme negative space.
- **Editorial serif-italic** for big headers and panel titles ("Finance", "Good afternoon", "Bank accounts", "Stocks · investments").
- **Monospace micro-labels** with a leading dot/number ("·05 YOUR MONEY COACH", "·02 YOUR COACH", "01", "ALLOCATION", "NET WORTH · SUBS · ORDERS · WISHLIST"), uppercase, wide tracking, tiny.
- **Big score numbers** in a light serif ("4/10", "6/10") with a small "/10".
- **Bento cards** — thin hairline borders, dark fills, generous rounding, a numbered corner, a "→" affordance, a line-art chart inside.
- **Glowing line-art charts** — sparklines and area charts with a soft gradient fill and a bright endpoint dot.
- **Donut allocation charts** with a centred total and a dotted legend + percentages.
- **A faceted 3D gem/crystal** (purple in ref) as a signature hero/avatar.
- **Macro bars** — thin horizontal bars with coloured segments + a kcal number.
- **Ghost buttons** ("+ add"), **pill toggles** (1D/1W/1M/1Y/All, currency), **chip suggestions**.
- **Minimal copy** — a single short "read", never a paragraph of rationale.

Constraint from project history: **no on-device LLM / no AI coach.** The reference's "ask your coach" chat is NOT built. The *score + one-line read* pattern is echoed using the existing non-LLM PrimeEngine/directives.

## PART 2 — THE DESIGN SYSTEM ("AZURE")

A new `ThemeSpec` named **Azure** — *"Sapphire & porcelain — the editorial luxe"* — set as the **default** so it recolours the whole app at once, and kept in the theme picker.

### 2.1 Colour (white + sapphire on cool near-black)
- **Ground:** `void #04060B` · `bg #070B13` · `bgElevated #0C121E` · `surface #111827` · `surfaceHi #18223400` · `bgTop #0A0F1A` — a deep, cool blue-black (not green-black, not neutral) for luxurious depth.
- **The white ("weiß"):** `ivory #ECF2FF` — a cool porcelain white, faintly blue, elegant not sterile. `textPrimary #F3F7FF` · `textMuted #93A2BE` · `textDim #55617A`.
- **The blue ("blau"):** `accent #5AA2FF` — a sapphire/azure that glows on dark; lighter glow `#8FBEFF`.
- **Metal (lux hairline):** `#C9D8F0` cool platinum · deep `#8FA6C8`.
- **Cool jewel accents (the "andere coole farbtöne"), per module:**
  home = azure `#5AA2FF` · calendar = lavender `#A48CF5` · train = coral `#FF7E6B` · fuel = mint `#3FD9B0` · body = cyan `#46C6F0` · guard = gold `#E7B45C` · skills = violet `#9B8CFF` · mind = periwinkle `#7E8CF0` · finance = emerald `#37D69A` · school = sky `#5D9BF5`.
- **Semantic:** good `#37D69A` · warn `#E8B45C` · crit `#F2647A`.
- **Effects:** `nebulaAlpha 0.09`, `nebulaWarmth 0` (cool), `grainAlpha 5`, `vignette 0.20`, `specular 0.18` (the hairline light), `glow 1.1` (line-art glow). Radii generous: `rHero 22 · rCard 18 · rElem 12 · rMicro 8`.

### 2.2 Typography (editorial)
Extend `Type.kt` with two device families — `Serif` (`FontFamily.Serif`) and `Mono` (`FontFamily.Monospace`) — and a `displaySerif` flag on `ThemeSpec`:
- **`Display` → Serif** for the Azure theme (editorial headers/titles, often italic).
- **`Mono`** for the micro-labels/overlines (uppercase, tracked, tiny, with a leading "·").
- **`Body` → Manrope** (data + text, unchanged; readability is theme-invariant).
- Big numbers: light-weight serif (`bigNumberWeight 500`) for the score/hero-number feel.

### 2.3 Shared components (the leverage — restyle once, propagate everywhere)
In `ui/kit/Kit.kt`:
- **`JarvisHeader`** → serif title (optionally italic), a mono overline with a leading "·", a thin context line.
- **`SectionLabel`** → mono, tracked, uppercase, with a leading number/dot ("·02"); the champagne dot becomes a thin accent tick.
- **`Panel`** → the bento card: hairline border in `metal`, a whisper of top specular, generous rounding, dark fill; a numbered corner variant.
- **`Ring` / `Spark` / charts** → thinner strokes, a soft accent glow, a bright endpoint dot (matches the reference line-art).
- **Buttons/chips** → ghost "+ add", pill toggles, suggestion chips.
- **`ModuleBackground`** → cool nebula + faint grid lines (the reference's subtle circuit texture), vignette.
Restyling these covers the majority of every screen.

### 2.4 Shell & dock
The `AscendApp` dock and the shell transition (already the calm fade) get the Azure treatment: serif labels where titled, mono for the tiny nav labels, sapphire selection glow.

## PART 3 — FINANCE, EXPANDED (the reference's headline)

Rebuild `ui/finance/FinanceHome` into a **net-worth command centre** matching the reference, using the existing `FinanceStore` where possible and adding the missing models:
- **Header:** serif "Finance", a mono micro-tab row ("NET WORTH · ACCOUNTS · INVESTMENTS · SUBS"), a currency chip (EUR default; the app is single-currency today — keep EUR, design the toggle).
- **Net-worth chart:** an area chart with a soft sapphire gradient fill + glowing line + endpoint dot; time toggles (1M / 3M / 1Y / All) driven by **snapshots** (a new lightweight net-worth snapshot store).
- **Stat row:** `% OF NW`, `ALL-TIME HIGH`, `ALL-TIME LOW`, `SNAPSHOTS`.
- **Allocation donut:** slices per holding class with a centred total and a dotted legend + %.
- **Panels (serif headers, mono data, "+ add" ghost, "×" delete):**
  - **Accounts** — name + balance (existing accounts in FinanceStore).
  - **Investments** — ticker chip + shares + price + %, a mini sparkline, a value; a "Ticker / Shares / +" add row. (Manual price entry — no live API; label it manual.)
  - **Crypto** — coin + value + "+ add".
  - **Other assets** — name + value (home, car…).
  - **Debt / liabilities** — name + value, shown red.
- Net worth = accounts + investments + crypto + other − debt. A snapshot is written on each change so the chart has history.
Data-model additions live in `FinanceStore` (Prefs/JSON, additive defaults so old data loads). No live market API (respect "no cost features"); prices are manual with a clear label.

## PART 4 — DE-CLUTTER (remove unnecessary explanations)

The app is currently very verbose — every card carries a rationale sentence. Strip to the essential, reference-style:
- Home briefing, Train prescription notes, Fuel cards, Sleep, Prime — cut the long "why" lines to a short read or move them behind the "Details" affordance / the new Documentation.
- Keep the *one* short insight (the reference's "TODAY'S READ" pattern) where it adds value; delete the paragraphs.
- Onboarding/empty-state copy trimmed.
The rule: **one line, not three.** The depth moves into Part 5.

## PART 5 — THE DOCUMENTATION (in Settings)

A new **"Documentation"** entry in Settings opening a huge, structured, scrollable reference — every feature, tool, *and algorithm*, and how to use it. Sections:
- **Getting started** — the dock, the shell, contexts.
- **Home / Today** — readiness scan, missions, systems.
- **Prime** — the index, how the score is computed, the directives.
- **Train** — the assignment, calibration (the 3 phases + what each test seeds), the MEV→MRV volume model, the RIR ramp, rest-by-load, the vest prescription, progression chains, skill goals, the freshness/recovery model.
- **Mobility & Stretching** — the six routines, when each is suggested, the mobility prescription.
- **Fuel** — macros, hydration, the food-quality score (every signal: NOVA, additives, sat-fat, sugar, salt, fibre), the Details expander.
- **Vitals / Sleep** — recovery, the 8–9h target + consistency, SE.
- **Calendar, School, Mind, Guard, Skills** — what each does, the algorithms (SM-2 spaced repetition, screen-time score…).
- **Finance** — net worth, allocation, snapshots, how to add holdings.
- **Automations** — protocols + custom rules.
- **The design** — themes, how to switch.
Generous, structured, nothing left out — the algorithms explained in plain language. This is where the removed explanations go, organised.

## PART 6 — APPLY TO EVERYTHING + VERIFY

- Apply the Azure system to every screen (via the theme + kit; then per-screen polish for the headliners: Home, Finance, Fuel, Prime, Train, Settings).
- Build green (`assembleDebug` + tests), install on the emulator, and **walk every tab** confirming the white+blue editorial look, the Finance expansion, the trimmed copy, and the Documentation.
- Fix anything that isn't right; iterate until it holds.

## PART 7 — EXECUTION ORDER

1. **Foundation** — the Azure `ThemeSpec` (default) + `Type.kt` serif/mono + `Color.kt` any new tokens. *Recolours everything.*
2. **Kit** — restyle `Panel`, `SectionLabel`, `JarvisHeader`, charts, buttons, `ModuleBackground`. *Propagates the editorial look.*
3. **Finance** — the net-worth tracker (models + UI).
4. **Documentation** — the Settings reference.
5. **De-clutter** — strip verbose copy on the headline screens.
6. **Per-screen polish** — Home, Fuel, Prime, Train, dock.
7. **Verify** — build, install, walk every tab, fix, iterate.

Status tags advance as each ships. Verification screenshots are the proof.
