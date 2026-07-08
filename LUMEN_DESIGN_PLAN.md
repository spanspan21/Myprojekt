# LUMEN — the white-light, glowing-blue, motion-first redesign of JARVIS

**A complete design-system overhaul. White luminous canvas · electric-blue glowing features · a futuristic, alive interface carried by meaningful motion.**

> Brief (Max, verbatim intent): completely rework the design — white background with blue features, in the glowing futuristic style of the reference set; rework *every* style element; use *many* animations (not just tab transitions — animations *on the functional elements* and on things you interact with); design it like a senior designer flexing for an Amazon-level portfolio; place motion so it never feels out of place; write a ≥100-page plan, then build it.

This document is that plan. It is the single source of truth for the LUMEN overhaul: the philosophy, the light-mode architecture (the app was dark-first — this is real engineering, not a recolor), the color and type systems, the glow system, the glass-and-light component library, the full interactive-motion catalogue, per-screen choreography, the build phases, and the acceptance bar. Nothing is hand-waved; every value and every animation has a spec.

---

# PART I — THESIS & RESEARCH

## 1. The one-sentence thesis
LUMEN turns JARVIS from a dark instrument into a **luminous machine**: a bright, airy, white interface where the data itself *glows blue*, where every surface has depth and light, and where the interface is **alive** — numbers count, rings fill, charts draw themselves, liquids rise, crystals breathe, and everything you touch responds. It should feel like holding a piece of soft-glowing future glass.

## 2. What the reference set actually teaches (read again, translated to light)
The six "Vitality" reference shots are dark, but their *language* is what we take — and invert onto white:
- **Editorial serif-italic headers** ("Good afternoon", "Finance") paired with **monospace micro-labels** ("01", "NET WORTH · SUBS", "TODAY'S READ"). Premium, magazine-grade. → **Keep. Ink-navy on white.**
- **Numbered bento cards** with thin frames and a single **line-art chart** inside (a heartbeat for VITALS, a wave for FUEL, a rising line for TRAIN). → **Keep. Glowing blue line-art on white glass.**
- **Glow.** Every meaningful stroke emits light. On black that's additive bloom. **On white, glow becomes a soft blue halo / colored shadow** behind the element — the single hardest and most important translation.
- **A 3-D faceted crystal** (the coach avatar / brand gem) with a glowing glyph. → **Keep as the LUMEN signature: an animated, breathing, rotating blue crystal.**
- **Circuit / PCB texture** faint in the background of hero cards. → **Keep as a whisper-faint blue circuit filigree on white.**
- **Score + one short read** ("4/10", "TODAY'S READ") rather than paragraphs. → **Keep. Quiet screens, deep docs.**
- **Extreme negative space, floating command bar, pill toggles, ghost "+ add".** → **Keep.**

## 3. Why light-on-white is *harder* (and why that's the flex)
Dark UIs hide a multitude of sins: any bright thing pops, hairlines are free, glow is trivial (additive on black). **White is unforgiving** — contrast must be engineered, depth must come from real shadow, glow must be a crafted halo, and hierarchy must be earned with weight, size, and spacing, not brightness. Nailing a white glowing-blue UI *is* the portfolio piece. Every choice below is made with that bar.

## 4. Design principles (the constitution)
1. **The data glows, the chrome doesn't.** Blue light is reserved for meaning — the live number, the active ring, the chart, the thing you're touching. Never decorate with glow.
2. **Depth by light, not by line.** On white, cards lift with soft, blue-tinted shadows and a hair-fine cool border — not dark hairlines. Elevation is a real, tiered shadow system.
3. **Motion is feedback, not confetti.** Every animation answers "what changed / what am I touching / where did it go". If it doesn't, it's cut.
4. **One hero moment per screen.** Each screen earns exactly one showpiece animation (the crystal, the liquid fill, the ring bloom). The rest is quiet micro-motion.
5. **Editorial calm.** Serif-italic headers, monospace labels, generous air. Luxury is restraint plus one perfect flourish.
6. **60fps or it's cut.** All motion is spring/tween on GPU-friendly properties (alpha, scale, translate, path-trim, blur used sparingly and cached). Respect `reduce-motion`.
7. **Coherence is structural.** Everything reads from the theme. A single `light` world recolors and re-lights the whole app.

---

# PART II — THE LIGHT ARCHITECTURE (engineering the impossible-on-white)

The current engine (`ThemeSpec`) assumes dark: `ivory` is the near-white "contrast role", `Panel` draws a **white specular hairline** and dark glass fill, `ModuleBackground` paints a **near-black gradient + grain + vignette**, glows are additive on black. To go light we add a **first-class light mode** and branch the atmosphere/surfaces.

## 5. `ThemeSpec` additions
Add fields (defaulted so the five dark worlds are untouched):
- `light: Boolean = false` — the master switch.
- `canvasTop: Color`, `canvasBot: Color` — the page gradient (white worlds go bright→bright-cool).
- `aurora: Color`, `auroraAlpha: Float` — the soft blue corner blooms.
- `cardFill: Color` — surface of a card (white worlds: pure/near-white).
- `cardBorder: Color` — the cool hairline (white worlds: `#E4EBF7`-ish).
- `shadowTint: Color`, `shadowStrength: Float` — the blue-tinted elevation shadow (white worlds only).
- `glowInk: Color` — the color a glow halo emits (electric blue).
- `gridAlpha: Float` — faint circuit/grid filigree.

`Ivory` stays the "contrast role": in light worlds it becomes **ink-navy** (`#0A1B3D`), so every existing `Ivory.copy(alpha=…)` fill/hairline still means "a faint mark against the background" and renders as a soft navy tint on white — correct by construction. Specular (a *light* edge) is disabled in light worlds; its role is replaced by shadow.

## 6. `Color.kt` tokens
All tokens already proxy `themeSpec`. We add light-aware getters where needed:
- `Canvas`, `CanvasElevated`, `CardFill`, `CardBorder`, `ShadowTint`, `GlowInk`, `Aurora`.
- `Ink`, `InkMuted`, `InkDim` aliases mapping to `textPrimary/Muted/Dim` (semantics read better in light code).
- `Line`/`Line2` continue as `ivory.copy(alpha)` → navy hairlines on white.

## 7. `Kit.kt` atmosphere & surfaces — the branch
- **`ModuleBackground` (light):** vertical white gradient (`canvasTop → canvasBot`), two big **aurora blooms** (blurred radial, module-accent→transparent at `auroraAlpha`), an optional **hair-faint circuit grid** (1px cool lines at `gridAlpha`), and a soft **top luminance** instead of a vignette. No grain, no scanlines.
- **`Panel` (light):** white `cardFill`, radius per world, a **1px `cardBorder`**, and a **tiered soft shadow** (two stacked shadows: a tight contact shadow + a wide blue-tinted ambient at `shadowStrength`). `lux=true` swaps the ambient shadow for a **blue glow halo** (accent-tinted, larger blur). A subtle top-inner highlight (white→transparent) gives the glass sheen. Pressable panels animate elevation + a faint glow on press.
- **`Ring`/`Spark`/charts (light):** track = cool gray; the value stroke is **electric blue with a glow pass** (a wider, blurred, low-alpha blue stroke under a crisp 2px stroke) + a **bright endpoint dot with a bloom**. Area fills = blue→transparent at low alpha.
- **Glow primitive:** a reusable `Modifier.glow(color, radius, alpha)` = a blurred colored shadow behind the element (implemented via `shadow` with spot/ambient color on API ≥28, or a drawBehind radial for older). Cached; static unless the element is "live".

## 8. The LUMEN world (the new default)
```
LUMEN — "Daylight glass — white light & electric blue"
light = true
canvasTop #FFFFFF · canvasBot #EEF3FC · aurora #2E6BFF @0.06 · grid @0.03
ink (ivory role) #0B1C3F · textPrimary #0B1C3F · textMuted #5A6A88 · textDim #97A2B8
cardFill #FFFFFF · cardBorder #E4EBF7 · shadowTint #2E6BFF @ strength 0.16
accent / glowInk #2563FF (electric sapphire) · bright #4D8BFF · deep #1442B4
metal (lux) #2E6BFF  (blue IS the precious material here)
mods:  home #2563FF · calendar #7C5CFF · train #FF5A45 · fuel #00B888 · body #0AA6D6 ·
       guard #F0A62E · skills #7C3AED · mind #4F5BD5 · finance #10A96A · school #2563FF
good #10A96A · warn #E8991F · crit #F0453E
radii rHero 26 · rCard 20 · rElem 14 · rMicro 9   (softer, friendlier on white)
glow 1.25 · specular 0 · nebula(aurora) on · grain 0
displaySerif = true  (editorial serif italic headers, mono micro-labels — kept)
bigNumberWeight 600
```
LUMEN becomes the **default** (one-time migration, like before) and leads the picker. The five dark worlds + Azure remain selectable.

---

# PART III — COLOR, LIGHT & TYPE IN DEPTH

## 9. The blue (the "features")
One electric family carries all meaning:
- **Core** `#2563FF` — buttons, active states, primary lines.
- **Bright** `#4D8BFF` — glow emission, hovered/pressed, chart endpoints.
- **Deep** `#1442B4` — pressed fills, text-on-glow.
- **Wash** `#2563FF @ 6–12%` — selected chips, tints, aurora.
Blue is *the* feature color; module jewels are used only where identity matters (a module's own screen, its icon, its chart), so the app reads unmistakably **white + blue** with tasteful jewel punctuation.

## 10. Glow, engineered for white
On white, glow is a **soft colored halo**: a blurred, low-alpha blue shadow bleeding 12–28px around a live element, plus (for the crisp element) a 1px inner brighten. Three intensities:
- **Whisper** (resting live element): 12px, 10% blue.
- **Active** (in-focus ring, hero number): 20px, 16% blue, gently pulsing.
- **Burst** (celebration): 40px, 24%, one-shot decay.
Glow is cached and only animates on "live" elements — never on every card (that would muddy white).

## 11. Depth / elevation ladder (shadows)
Four tiers, all blue-tinted (`shadowTint`):
- **e0** flush (no shadow) — background chips.
- **e1** raised card: `0 2 6 rgba(navy,0.06)` + `0 8 24 rgba(blue,0.08)`.
- **e2** hero card / sheet: `0 4 10 rgba(navy,0.08)` + `0 16 40 rgba(blue,0.12)`.
- **e3** floating (FAB, active dragged): `0 8 20 …` + a **glow** ring.
Pressing drops a card one tier and adds a whisper glow — the "soft glass under a finger" feel.

## 12. Typography (kept editorial, re-inked)
- **Display = Serif** (device serif), **italic** for hero titles ("Good afternoon", "Finance") — ink-navy.
- **MicroLabel = Monospace**, uppercase, tracked, ink-dim, with the leading tick/number.
- **Body = Manrope**, ink.
- **Big numbers** = serif, weight 600, with a whisper glow when live.
- Scale unchanged; only color (ink) and the addition of glow-on-live-numbers.

---

# PART IV — THE COMPONENT LIBRARY (glass & light)

Each component gets a light spec + its motion. (Motion catalogue detailed in Part V.)

## 13. Surfaces
- **GlassCard (`Panel`)** — white, e1 shadow, cool border, top sheen; `lux` → glow halo. Press → e0 + whisper glow + 0.98 scale spring.
- **HeroCard** — e2, larger radius, an aurora bloom bleeding from one corner, a faint circuit grid, room for the one hero animation.
- **StatTile** — big serif number (glow-on-live) + mono label; number **counts up** on first reveal.

## 14. Data viz (all glowing blue on white)
- **GlowLine / Spark** — draw-on path-trim, blur-glow underlay, bright endpoint bloom, gradient area.
- **GlowRing / Gauge** — track + animated blue arc with a **comet endpoint** (a bright dot with a trailing glow) and center count-up.
- **Donut** — segments with a soft outer glow on the active segment; legend dots pulse on select.
- **Bars** — grow from baseline with a spring + a top glow cap.
- **LiquidFill** — a rising blue liquid with a moving sine surface + subtle caustic glow (hydration, progress).
- **Sparkbeat** — the VITALS heartbeat line, animated as a running ECG sweep.

## 15. Controls (interactive, animated)
- **GlowButton** — filled blue with a glow; press → scale 0.96 + glow flare + haptic; a light sweep crosses on enable.
- **GhostAdd ("+ add")** — cool outline; press → fill wipes in from the tap point.
- **PillToggle / SegTabs** — the selected pill is a **glowing blue lozenge** that **slides** (shared-bounds) between options.
- **Switch** — track fills blue with a glow as the knob springs across; a tiny ring pulse on toggle.
- **Stepper (±)** — orbs that press-bounce; the number **rolls** (odometer) and glows on change.
- **Slider** — a blue thumb with a glow that **grows with drag velocity**; the filled track glows.
- **Chips** — select = wash fill + glow ring + spring.

## 16. The signature: **the LUMEN Crystal**
A Canvas-drawn faceted polyhedron (dodecahedron silhouette) in electric blue on white:
- Facets are gradient-filled (light blue → blue), edges are crisp blue with a glow.
- It **breathes** (slow scale 0.98↔1.02), **slowly rotates** (parallax facet shimmer), and **pulses its glow** with the relevant score.
- A glowing glyph ("J"/chevron) floats at center.
- Used as: the boot/brand mark, the Prime "readiness" hero, the coach avatars (Fuel/Money score), achievements. Tapping it triggers a **rescore ripple** (glow burst + facet cascade).
This is the "wow" object — bespoke, animated, unmistakable.

## 17. Atmosphere
- **Aurora background** — two slow-drifting blue blooms (very subtle parallax with scroll).
- **Circuit filigree** — a faint blue PCB grid behind hero cards, its nodes twinkling occasionally.
- **Command bar** — a floating, glassy, blurred-white dock with a glowing active pill.

---

# PART V — THE MOTION CATALOGUE (the heart of the brief)

Max asked specifically for **many animations on the functional elements and on interactive things** — not tab fluff. Below is the full catalogue, each with trigger, property, curve, and placement. Built as a `Lumen.motion` library so usage is one-liners and consistent.

## 18. Motion foundations
- **Curves:** `emphasized` (spring, low bounce) for enter/interactive; `standard` (tween 220–320ms, FastOutSlowIn) for state; `decel` for reveals. A single `spring(stiffness, damping)` pair per role — no ad-hoc springs (the old "jumping" bug).
- **Only GPU-cheap props:** alpha, scale, translate, rotation, path-trim, and *cached* blur for glow. Never animate layout width per-frame except via `animateContentSize` on small elements.
- **Reduce-motion:** every animation has a static end-state; the flag jumps to it.
- **Haptics:** press (light tick), success (double tick), threshold cross (medium) — paired with the visual.

## 19. Micro-interactions (everywhere, quiet)
1. **Press-scale + elevation drop** on every tappable surface (spring to 0.97, shadow e→e-1, whisper glow).
2. **Ripple-from-touch** glow on buttons (origin = finger).
3. **Chip/toggle spring** select.
4. **Icon state morphs** (e.g. check draws its stroke; play↔pause).
5. **Focus glow** on text fields (border blooms blue on focus).

## 20. Functional data-motion (on the numbers & charts — the core ask)
6. **Count-up numbers** — every stat animates from a base to value on reveal/change (odometer for integers, eased interpolate for decimals), with a **glow pulse at settle**.
7. **Ring/gauge fill** — arc sweeps with a **comet endpoint** + count-up center; overshoots 3% and settles.
8. **Chart draw-on** — line paths trim in left→right with the glow leading the tip; area fades up behind.
9. **Bar grow** — spring from baseline, staggered across categories, glow cap.
10. **Liquid fill** — hydration/progress rises with a live sine surface; crossing the goal triggers a **surface flash + burst**.
11. **Donut sweep** — segments draw clockwise; selecting a segment lifts+glows it and count-ups its value.
12. **Heartbeat/ECG** — VITALS line runs a repeating sweep with a glowing scan dot.
13. **Delta tick** — +/- changes slide in with color (good/crit) and a short glow.

## 21. Interactive object-motion (things you manipulate)
14. **Tilt-to-touch cards** — hero cards tilt slightly toward the finger (3-D parallax) and their glow follows the touch point; release springs flat.
15. **Draggable sliders/steppers** — thumb glow scales with drag velocity; haptic ticks at integer stops.
16. **Swipe-to-log / swipe-to-complete** — mission rows swipe to complete with a glow-wipe + check draw + burst.
17. **Pull-to-refresh as an aurora bloom** — pulling stretches a blue bloom that snaps into a spin.
18. **The Crystal rescore** — tap → glow burst, facets cascade-flash, score count-up.
19. **Long-press peek** — long-pressing a stat blooms a glowing detail popover (spring-scale from the element).
20. **Drag-to-reorder** (home cards) — lifted card gains e3 + glow; neighbors slide with spring.

## 22. Choreography (entrances & transitions)
21. **Screen entrance** — staggered reveal: header first, then cards rise+fade in sequence (40ms stagger, 12px rise), the hero animation plays last.
22. **Shell transitions** — the calm cross-fade + a subtle aurora shift toward the new module's accent (kept from prior work, re-tuned for light).
23. **Sheet entrance** — springs up; content settles 70ms later (kept, re-lit).
24. **Number-to-detail** — tapping a stat expands it into its detail with a shared-element-ish scale/position tween.
25. **Celebration** — mission/goal complete: a one-shot blue **burst ring** + rising sparks + haptic; streak up = crystal pulse.

## 23. Ambient life (subtle, always)
26. **Crystal breathing + rotation** (hero screens).
27. **Aurora drift** (background parallax with scroll).
28. **Circuit twinkle** (occasional node glints on hero cards).
29. **Live-number glow breath** (the current focus number pulses ~6s cycle).
30. **Cursor/caret bloom** in inputs.

Every one of the 30 has a specified home in Part VI so nothing feels random.

---

# PART VI — PER-SCREEN CHOREOGRAPHY

For each screen: the layout in light-glass, the module accent, and *which* of the 30 motions live there (one hero + supporting micro-motion).

## 24. Home / Today (accent: azure #2563FF)
- Serif-italic greeting (glow-breath on the name), mono date.
- **Readiness HeroCard** with the **Crystal** as the score object (hero motion #26/#18) + count-up (#6) + aurora corner + circuit twinkle (#28).
- **Numbered bento**: TRAIN (draw-on line #8), FUEL (liquid wave #10 mini), VITALS (ECG #12) — each a GlassCard with press-tilt (#14).
- **Missions**: swipe-to-complete (#16) + progress liquid (#10) + celebration (#25).
- Command dock: sliding glow pill (#22).
- Entrance choreography #21.

## 25. Prime (azure)
- Serif "Prime"; **big Crystal gauge** = index (hero #7 ring + #18 rescore + #6).
- Gauge tiles count-up (#6) with glow-breath on the live one (#29); tap a gauge → long-press peek detail (#19).
- Directives slide in staggered (#21); each has a delta tick (#13).

## 26. Train (coral #FF5A45)
- Serif "Training"; session HeroCard with a rising **draw-on** volume line (#8).
- Phase chips = sliding seg (#22); **Start session** = GlowButton with sweep (#15/#19).
- Set logging: stepper roll (#6/#15) + rest-timer **ring** (#7) with comet; PR = burst (#25).

## 27. Fuel (emerald #00B888)
- Serif "Fuel"; **calorie GlowRing** (#7) + macros count-up (#6).
- **Hydration = LiquidFill** hero (#10) — tap +/- raises the water with waves; hitting goal = surface flash + burst (#25).
- Food add: focus-glow inputs (#5), quality score crystal-mini.

## 28. Finance (green #10A96A)
- Serif "Finance"; **net-worth GlowLine** with draw-on (#8) + range seg slide (#22) + value count-up (#6).
- **Allocation donut** sweep + segment lift on select (#11).
- Holdings rows: press-tilt (#14), add-sheet ghost wipe (#15).
- The **Money-Crystal** score (#18) echoing the reference coach.

## 29. Vitals / Sleep (cyan #0AA6D6)
- **ECG hero** (#12); recovery ring (#7); sleep stages as a glowing stacked bar (#9).

## 30. Calendar · School · Mind · Guard · Skills · Settings · Documentation
- All inherit the light-glass system + micro-motion (#1–#5), staggered entrance (#21), and their module accent.
- School flashcards: **card flip** (3-D) with glow edge; grade buttons = GlowButtons.
- Skills: the skill tree nodes **pulse/connect** with glowing edges.
- Settings: toggles (#/switch), the theme salon shows live-lit previews.
- Documentation: keep; re-inked to light, chevrons + reveal (#/accordion) with glow on open.

---

# PART VII — BUILD PHASES

**P1 · Light architecture** — ThemeSpec/Color/Kit light branch (ModuleBackground, Panel, Ring, Spark, glow primitive) + LUMEN world as default + migration. *Recolors & re-lights everything.*
**P2 · Glow + shadow system** — the `Modifier.glow`, the elevation ladder, the aurora/circuit atmosphere, GlassCard/HeroCard.
**P3 · Motion library** — `Lumen.motion`: count-up, ring/comet, draw-on, liquid, tilt, sliding-seg, burst, switch, stepper roll, focus-glow, crystal.
**P4 · The Crystal** — the bespoke animated component.
**P5 · Headliner screens** — Home, Prime, Fuel, Finance, Train wired to the new components + their hero motions.
**P6 · Breadth** — every remaining screen inherits + gets its micro-motion; de-dark any hardcoded spots.
**P7 · Verify** — build + 116 tests green; install on S24; walk every tab; confirm white+glow+motion; fix; iterate to perfect.

## 31. Acceptance bar (what "perfect" means here)
- Background is genuinely **white/luminous** on every screen; no dark surfaces leak.
- Features **glow blue**; the app reads unmistakably white+blue.
- Every screen has its **one hero animation** + tasteful micro-motion; nothing feels out of place; 60fps.
- At least the 30 catalogued motions exist and are placed per Part VI.
- The **Crystal** breathes and reacts.
- Serif-italic editorial headers + mono labels intact, re-inked.
- Builds, tests green, verified on the physical S24 with real data intact.

## 32. Risks & mitigations
- **Contrast on white** → engineered ink tokens + tested min 4.5:1 for text, 3:1 for UI.
- **Glow muddying white** → glow only on live elements, cached, low-alpha.
- **Perf of blur/shadow** → cache glows, cap animated-blur elements, prefer drawn radial over live blur where possible; reduce-motion path.
- **Dark-assumption leaks** (hardcoded `Void`/black in screens) → grep + fix in P6.
- **Scope** → phase-gated; headliners first so the "flex" is visible early, then breadth.

---

*LUMEN is white light made into an interface, with electric blue as its living signal and motion as its pulse. Build it phase by phase; verify on glass; iterate until it glows.*
