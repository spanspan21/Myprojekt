<div align="center">

# JARVIS — Life OS

**A native, offline-first Android app that turns training, nutrition, recovery, focus and skills into one intelligent daily operating system.**

Built entirely with Kotlin & Jetpack Compose · A single hand-crafted "IRON HUD" design language · No cloud account, no subscriptions, your data stays on the device.

<!-- Badges: these are static shields based on the actual build config. Swap the license/CI ones once you add a LICENSE file / CI workflow. -->
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.03-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-34-blue)
![Architecture](https://img.shields.io/badge/architecture-MVVM%20%2B%20Room-orange)

</div>

---

## ✨ Overview

**JARVIS** is a personal *Life OS* — a single app that replaces a stack of separate trackers (a training app, a nutrition scanner, a sleep dashboard, a focus blocker, a habit tracker, a spaced-repetition study tool…) with one coherent, science-backed system.

It's **offline-first**: everything runs on-device. The internet is used only for optional public data (Open Food Facts barcode lookups, weather-based hydration bonus), and health metrics are read locally from **Android Health Connect**. There is no login and nothing is uploaded.

The whole app is unified by one design language — **"IRON HUD"** (Obsidian & Champagne): a dark, HUD-style interface where every module has its own accent colour and signature motif, but shares the same typography, glass panels and motion — *"a puzzle, not a template."*

> **Note on the module split:** JARVIS is a private, non-Play-Store project built for a single user. It is deliberately opinionated (studies-backed training progression, "non-negotiable" plans, English-only copy). Treat it as a reference architecture for a large single-activity Compose app rather than a general-purpose product.

---

## 📸 Visuals

> Replace the placeholders below with real captures. Suggested set — six shots that tell the whole story. A short **GIF of the dock morph / screen transition** at the top sells the app better than any static image.

<div align="center">

| | |
|:---:|:---:|
| ![Home / Today dashboard](docs/screenshots/home.png)<br/>**Home — command center**<br/><sub>Body-scan hero, daily briefing, today's missions</sub> | ![Prime index](docs/screenshots/prime.png)<br/>**Prime — the daily index**<br/><sub>Animated radial index ring + ranked directives</sub> |
| ![Training session](docs/screenshots/train.png)<br/>**Train — active workout**<br/><sub>Generated plan, pose figures, prescriptions</sub> | ![Nutrition](docs/screenshots/fuel.png)<br/>**Fuel — nutrition & hydration**<br/><sub>Macro reactor rings, barcode scan, water wave</sub> |
| ![Guard focus overlay](docs/screenshots/guard.png)<br/>**Guard — focus & wellbeing**<br/><sub>Usage stats, focus sessions, app lockdown overlay</sub> | ![Calendar timeline](docs/screenshots/calendar.png)<br/>**Calendar — the day's spine**<br/><sub>Vertical timeline, auto-scheduled training</sub> |

</div>

**Which shots to capture, and why:**

| Placeholder | What to put there |
|---|---|
| `home.png` | The **Home / Today** screen right after opening — shows the greeting, daily briefing and the 3 mission tiles. This is your hero image. |
| `prime.png` | The **Prime** screen with the radial index ring filled in — best "wow" screenshot; consider a **GIF** of the ring sweeping up on open. |
| `train.png` | An **active workout** with an exercise pose figure and a prescription line ("Weighted Pull-ups · 8–15 reps @ 2 RIR"). |
| `fuel.png` | The **Nutrition** screen showing the macro reactor rings + the animated hydration wave card. |
| `guard.png` | The **Guard** overlay thrown over a blocked app, or the focus-score dashboard. |
| `calendar.png` | The **Calendar** timeline with morning training auto-scheduled before school. |
| *(optional)* `dock-morph.gif` | A short screen recording of the **morphing dock** switching groups + the shell transition between two tabs — the app's signature motion. Put it at the very top of the README. |

<sub>💡 Capture on a device/emulator with `adb exec-out screencap -p > shot.png`. For a GIF, record with `adb shell screenrecord` and convert with `ffmpeg`.</sub>

---

## 🚀 Features

JARVIS is organised into **four dock groups** containing **14 modules**:

### 🟢 Today
- **Home** — the command center: an animated body-scan hero, a typed greeting, a daily briefing panel, and exactly three "non-negotiable" daily missions (train / calories / water) that drive the streak.
- **Prime** — a single daily readiness **index** fused from training load, sleep, nutrition, hydration and calendar. Renders as an animated radial ring, plus the top-3 ranked directives with *why* each matters, anomaly detection and cross-metric insights.

### 🔴 Body
- **Train** — a studies-backed **plan generator**: max-reps calibration seeds strength-progression chains, volume ramps from MEV→MRV across a mesozyclus, RIR targets step down week-to-week, weighted-vest progression is earned, and 55 skills map onto real exercises. Sessions render articulated **pose figures** and explicit prescriptions.
- **Fuel** — nutrition & hydration: **barcode scanner** (ML Kit, no camera permission, ZXING fallback), Open Food Facts lookup, a food-quality score that flags risky additives, macro reactor rings, curated recipes with a cooking mode, and an animated hydration card.
- **Vitals** — the body dashboard: weight log, sleep trends, recovery, heart-rate/HRV/resting-HR from a fitness band via Health Connect, and cross-metric correlations.
- **Sleep** — an athlete-focused sleep protocol (8–9 h target, consistency score, sleep-efficiency), auto-synced from Health Connect.

### 🟣 Life
- **Calendar** — a vertical timeline that is the spine of the day; imports read-only from the device calendar / ICS / Untis, and **auto-schedules training** into free slots (e.g. before school).
- **Habits** — an auto-tracked checklist with a custom builder, per-habit reminders, streaks and stats.
- **Finance** — a manual transaction & savings tracker with category heuristics.
- **Goals** — long-term goals and targets.
- **School** — a grade tracker plus a spaced-repetition (SM-2) vocabulary/study system.

### 🟡 System
- **Guard** — a digital-wellbeing engine: reads app usage, runs focus sessions, and can throw a **focus overlay** over distracting apps (a foreground service + Quick-Settings tile). Includes automations/protocols.
- **Skills** — guided skill paths (WHY → LEARN → DO) backed by bundled JSON master plans (calisthenics, cybersecurity, AI automation), plus a constellation view.
- **Settings** — five swappable themes, launcher-icon variants, a live wallpaper, diagnostics (crash black-box), and granular module/permission toggles.

### 🎁 Cross-cutting
- 🔒 **Offline-first & private** — no account, no analytics, data stays on-device.
- 🎨 **Five-theme engine** — Sovereign, Glacier, Neon, Terra, Mono — every colour/type token is a live getter.
- 🏠 **Home-screen widget** + **Quick-Settings focus tile** + **NFC deep links** (`jarvis://train` at the pull-up bar).
- 🩺 **Crash black-box** — uncaught exceptions are logged to disk and shareable from Settings → Diagnostics.
- ✅ **Unit-tested pure engines** — scores, targets and plan generation are decoupled from storage and covered by JVM tests.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9.24 |
| **UI** | Jetpack Compose (BOM 2025.03.01), Material 3, custom "IRON HUD" design system |
| **Architecture** | Single-Activity + MVVM · Compose state · unidirectional data flow · pure/testable engines |
| **Persistence** | Room 2.6.1 (multiple databases: training, calendar, master plan, skill) + a `Repo` singleton over SharedPreferences/JSON |
| **Async** | Kotlin Coroutines + Flow |
| **Serialization** | `kotlinx.serialization` (JSON) |
| **Health data** | Android Health Connect (`connect-client`) — sleep, heart rate, HRV, resting HR, steps |
| **Vision / ML** | ML Kit Code Scanner + ZXing (barcodes) · ML Kit Pose Detection + CameraX (form video / rep counter) |
| **Networking** | Lightweight direct calls to Open Food Facts & Open-Meteo (no heavy client) |
| **Build** | Android Gradle Plugin 8.5.2 · KSP (Room codegen) · Compose compiler 1.5.14 · Java/JVM 17 |
| **Testing** | JUnit 4 — JVM unit tests over the pure engines |

**Design system:** one foundation (Void background, glass panels, Chakra Petch + Manrope typography, tabular figures, calibrated motion tokens) with seven per-module identities. See [`JARVIS_MASTERPLAN.md`](JARVIS_MASTERPLAN.md).

---

## ⚙️ Installation & Setup

### Prerequisites
- **Android Studio** (Ladybug or newer)
- **JDK 17** — required by AGP 8.5. ⚠️ A system JDK 8 on your `PATH` will *not* build this project. Point Gradle at Android Studio's bundled JBR:
  ```bash
  # Windows (the bundled JetBrains Runtime is JDK 17)
  export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
  ```
- Android SDK **API 34** (compileSdk / targetSdk 34), min SDK 26.

### Clone & build
```bash
# 1. Clone
git clone https://github.com/spanspan21/Myprojekt.git
cd Myprojekt

# 2. Build a debug APK
./gradlew :app:assembleDebug          # Windows: gradlew.bat :app:assembleDebug

# 3. Install onto a connected device / emulator
./gradlew :app:installDebug
#   …or:  adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Run the unit tests
./gradlew :app:testDebugUnitTest
```

Or simply **open the folder in Android Studio**, let Gradle sync, pick a device, and press ▶.

### Optional runtime permissions
Most features degrade gracefully, but a few are unlocked by user-granted permissions:

| Feature | Permission | How to grant |
|---|---|---|
| Sleep / HR / HRV | Health Connect | In-app prompt |
| Nutrition barcode scan | Camera | In-app prompt |
| Focus guard overlay | Usage access + draw-over-apps | System settings (in-app deep link) |
| Grayscale wind-down | `WRITE_SECURE_SETTINGS` | One-time adb:<br/>`adb shell pm grant com.ascend.lifeos android.permission.WRITE_SECURE_SETTINGS` |

> This is a personal build: the `release` type is non-debuggable but signed with the **debug** key so `adb install -r` updates the existing install in place. There is no Play Store distribution.

---

## 📂 Project Structure

```
Myprojekt/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/ascend/lifeos/
│       │   │   ├── MainActivity.kt          # Single activity — hosts the Compose tree & deep links
│       │   │   ├── JarvisApp.kt             # Application — installs the crash black-box, init
│       │   │   ├── core/                    # Shared primitives (e.g. the SM-2 engine)
│       │   │   │
│       │   │   ├── data/                    # ── Domain & persistence (the "brains") ──
│       │   │   │   ├── training/            #    Plan generator, volume model, progression
│       │   │   │   ├── calendar/            #    Calendar / ICS / Untis sync
│       │   │   │   ├── masterplan/          #    Room-backed guided skill plans
│       │   │   │   ├── prime/               #    PrimeEngine + PrimeMath (the daily index)
│       │   │   │   ├── sleep/               #    Sleep protocol & Health Connect sync
│       │   │   │   ├── finance/  school/    #    Money & study/grade stores
│       │   │   │   ├── skill/  life/  rules/#    Skill meta, life stores, automations
│       │   │   │   └── Repo.kt, Haptics.kt… #    Prefs/JSON repo, haptics, crash log
│       │   │   │
│       │   │   ├── ui/                       # ── Jetpack Compose UI, one package per module ──
│       │   │   │   ├── AscendApp.kt          #    Root shell: dock, groups, screen routing
│       │   │   │   ├── kit/                  #    Shared UI kit (Panel, Ring, SectionLabel…)
│       │   │   │   ├── theme/                #    5-theme engine (ThemeSpec, Color, Type)
│       │   │   │   ├── motion/               #    Motion tokens & shell transitions
│       │   │   │   ├── home/ prime/ training/#    Per-module screens…
│       │   │   │   ├── hud/ calendar/ school/#    …
│       │   │   │   ├── skills/ masterplan/   #    Guided paths + constellation
│       │   │   │   └── wallpaper/            #    Live "breathing nebula" wallpaper
│       │   │   │
│       │   │   ├── wellbeing/                # Guard: focus service, tile, overlay
│       │   │   └── widget/                   # Home-screen widget + action receivers
│       │   │
│       │   ├── assets/masterplans/*.json     # Bundled skill plans (imported into Room)
│       │   ├── res/font/                     # Chakra Petch + Manrope (OFL)
│       │   └── AndroidManifest.xml
│       └── test/                             # JVM unit tests for the pure engines
│
├── JARVIS_MASTERPLAN.md                      # The design bible ("IRON HUD" system + module specs)
├── build.gradle · settings.gradle            # AGP 8.5.2, Kotlin 1.9.24, KSP
└── README.md
```

> The repo also contains several `JARVIS_*_PLAN.md` / `.pdf` design dossiers documenting each major feature wave (training, nutrition, motion, themes, prime). They're the "why" behind the code.

---

## 🧭 Architecture at a glance

```
        ┌──────────────────────────────────────────────┐
        │  Compose UI  (ui/ — one package per module)   │
        │  AscendApp shell · dock · per-module screens  │
        └───────────────┬──────────────────────────────┘
                        │  state / events (MVVM)
        ┌───────────────▼──────────────────────────────┐
        │  Pure engines  (data/*)                        │
        │  PrimeEngine · PlanGenerator · FoodScore ·     │
        │  SleepProtocol · TrainBrain …  (unit-tested)   │
        └───────────────┬──────────────────────────────┘
                        │  read / write
        ┌───────────────▼──────────────────────────────┐
        │  Persistence: Room DBs + Repo (Prefs/JSON)     │
        │  Health Connect · Open Food Facts (network)    │
        └───────────────────────────────────────────────┘
```

The guiding principle: **keep the calculation cores pure** (scores, targets, plans take plain inputs and return plain values), decoupled from storage, so they can be unit-tested on the JVM without an emulator.

---

<div align="center">
<sub>Built with Kotlin & Jetpack Compose · Offline-first · One HUD to run the day.</sub>
</div>
