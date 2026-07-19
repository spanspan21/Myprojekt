# JARVIS — Privacy Policy

*Effective: 19 July 2026 · Contact: maximilian.spannbrucker12@gmail.com*

## The short version

**Your data never leaves your phone.** JARVIS is an offline-first personal
tracking app. There is no account, no cloud requirement, no advertising, no
analytics, no tracking SDKs. Everything you log — workouts, meals, sleep,
finances, school grades, screen-time rules — is stored locally on your device
and nowhere else, unless you explicitly turn on one of the optional
integrations listed below.

## What the app stores (locally, on your device only)

- Training data: workouts, sets, personal records, plans, activity logs
- Nutrition data: logged foods, recipes, water intake, targets
- Body data: weight, measurements, optional progress photos, optional
  menstrual-cycle dates, sleep and readiness metrics
- Health-Connect data you grant access to (sleep, steps, heart rate, weight)
  — read locally from Android Health Connect; JARVIS never uploads it
- Life data: calendar events, habits, financial transactions you enter,
  school grades, focus/screen-time settings
- App settings and preferences

All of this lives in your device's private app storage. Deleting the app (or
using the in-app wipe) deletes it. The in-app backup produces an encrypted
file that only you hold.

## Optional network features (each off by default or user-initiated)

| Feature | What is sent | To whom |
|---|---|---|
| Food barcode/text search | the barcode or search term | Open Food Facts (public database) |
| Weather-aware hydration | approximate location (coarse, if granted) | Open-Meteo (no account, no key) |
| Calendar feeds | your ICS feed URL is fetched | the calendar host you configured |
| WebUntis timetable | your Untis credentials (stored Keystore-encrypted on device) | your school's Untis server |
| Open-banking import | bank authorisation via redirect | GoCardless / Enable Banking (only if you connect a bank) |
| Personal cloud mirror | your data, to YOUR own configured endpoint | the server you configured (off unless you set it up) |

None of these are required. If you never enable them, JARVIS makes no network
requests that carry personal data.

## What we (the developer) receive

Nothing. There is no server operated by the developer that receives user
data. Crash logs are stored locally and shared only if you choose to send
them.

## Permissions, honestly explained

- **Usage access / display over other apps / accessibility** — power the
  optional Focus Guard (app limits and the blocking screen). Processing is
  entirely on-device; the accessibility service reads only window-change
  events (never screen content) and is used solely to enforce YOUR rules.
- **Health Connect read permissions** — import your sleep/steps/heart data
  locally. Never uploaded.
- **Camera** — barcode scanning and optional progress photos. Photos stay in
  private app storage.
- **Calendar read** — show your events next to your plan.
- **Notifications** — the reminders you opt into (max. 4 scheduled pings/day).

## Children

JARVIS is not directed at children under 13. The optional focus-casino
minigame uses no real money and cannot be converted to anything of value.

## Changes

Policy changes will be listed here with a new effective date. Because there
is no server-side data, changes can never retroactively affect data that
already sits only on your device.
