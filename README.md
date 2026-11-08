<p align="center">
  <img src="docs/assets/icon.svg" width="112" alt="Childoo" />
</p>

<h1 align="center">Childoo</h1>

<p align="center">
  <b>Screen time that pays for itself in learning.</b><br/>
  Kids keep playing — every few minutes of play is unlocked with a quick, friendly brain challenge.
</p>

<p align="center">
  <a href="https://github.com/liamirhosseinl/childoo/actions/workflows/android.yml"><img src="https://github.com/liamirhosseinl/childoo/actions/workflows/android.yml/badge.svg" alt="Android build" /></a>
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B-1E2266" alt="Platform" />
  <img src="https://img.shields.io/badge/Kotlin-2.2-17917D" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-FFC145" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/status-prototype-E5674F" alt="Status" />
</p>

---

## Overview

Most parents of heavy screen users have already lost the battle over *how long* their kids play. Childoo changes the question to *what the child gets out of it*.

The parent picks the apps that cost screen time — YouTube, TikTok, Roblox, any game — and sets an interval. After that much play, Childoo places a short challenge on top of the app. The child solves it, earns their next block of play, and carries on. No arguments, no hard shutdowns, and a few minutes of real learning every half hour.

| | |
|---|---|
| **Default rule** | One ~60-second challenge for every 30 minutes of play |
| **Tone** | Effort is praised, mistakes are never punished — a wrong answer just means "let's try another way" |
| **Privacy** | Everything runs on the device. No accounts, no tracking, no data leaves the phone |
| **Languages** | English, Persian (فارسی) with full RTL support |

## Features

**Current prototype**

- **App-aware play timer.** Counts only the time spent in the apps the parent chose, and only while the screen is on.
- **Challenge overlay.** A full-screen challenge appears over the target app as soon as the interval is used up. The Back button cannot dismiss it.
- **Fair by design.** If the child leaves the app, the challenge steps aside. It returns, unsolved, the moment they come back.
- **Survives restarts.** Protection resumes automatically after a reboot or an app update. Play time is persisted, so force-closing does not reset it.
- **Guided onboarding.** One decision per screen: apps, interval, permissions, done — with dedicated steps for Xiaomi, Redmi and POCO devices.
- **Parent dashboard.** Live countdown to the next challenge, challenges solved, protected apps and interval at a glance.
- **Bilingual.** English and Persian with an in-app switch, right-to-left layout and Persian numerals.
- **Instant test mode.** Trigger a challenge on demand to verify the setup in seconds.

**On the roadmap**

- Mini-game engine with a growing library of math, language, logic and movement challenges
- Companion characters — **Nuzo**, **Lotli** and **Embrix** — that the child picks, grows and customises
- Parent PIN gate, tamper alerts and weekly progress reports
- Subscription plans and a parent web dashboard
- iOS app (Screen Time API)

## Architecture

Childoo is a multi-module Android project. Each module has one responsibility and can be developed, tested and replaced on its own.

```
┌──────────────────────────────────────────────────────────┐
│ :app        Onboarding · parent dashboard · app picker   │
└───────┬──────────────────────┬──────────────────┬────────┘
        │                      │                  │
┌───────▼─────────────┐ ┌──────▼─────────────┐    │
│ :lockkit            │ │ :challenge         │    │
│ App detection       │─▶ Challenge UI       │    │
│ Monitor service     │ │ Question engine    │    │
│ Overlay · restore   │ └──────┬─────────────┘    │
└───────┬─────────────┘        │                  │
        │               ┌──────▼──────────────────▼──────┐
        │               │ :designsystem                  │
        │               │ Theme · type · Nuzo · widgets  │
        │               └────────────────────────────────┘
┌───────▼─────────────────────────────────────────────────┐
│ :core       Settings (DataStore) · play-time rules       │
└─────────────────────────────────────────────────────────┘
```

| Module | Responsibility | Key types |
|---|---|---|
| `core` | Persisted parent settings and the pure play-time logic | `SettingsRepository`, `LockSettings`, `PlayTimeAccumulator` |
| `designsystem` | Brand theme, typography, the Nuzo character and shared components | `ChildooTheme`, `Nuzo`, `StarrySky`, `ChunkyButton` |
| `challenge` | The challenge experience the child sees | `ChallengeScreen`, `MathQuestion` |
| `lockkit` | Everything that enforces the rule at runtime | `UsageMonitorService`, `ForegroundAppDetector`, `OverlayController`, `LockKit` |
| `app` | Parent onboarding, dashboard and settings | `OnboardingFlow`, `HomeScreen`, `AppPicker`, `Permissions` |

### How enforcement works

1. `UsageMonitorService` runs as a foreground service (`specialUse`, parental control) and ticks once per second.
2. `ForegroundAppDetector` reads `UsageStatsManager` events incrementally to know which app is in front.
3. `PlayTimeAccumulator` adds time only when a target app is in front and the screen is interactive. Long gaps such as device sleep are capped, so they never count as play.
4. When the interval is reached, `OverlayController` attaches a `TYPE_APPLICATION_OVERLAY` window that hosts the Compose challenge.
5. Solving the challenge resets the counter and records the result.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose · Material 3 |
| Typography | Vazirmatn (Latin + Persian), bundled |
| Persistence | Jetpack DataStore (Preferences) |
| Concurrency | Kotlin Coroutines & Flow |
| Build | Gradle 8.13 · Android Gradle Plugin 8.11 · version catalog |
| Min / target SDK | 26 (Android 8.0) / 36 |
| CI | GitHub Actions |

## Getting started

### Requirements

- Android Studio (latest stable)
- JDK 17 or newer (the one bundled with Android Studio works)
- An Android 8.0+ device or emulator

### Build and run

```bash
git clone https://github.com/liamirhosseinl/childoo.git
cd childoo
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # run unit tests
```

Or open the folder in Android Studio and press **Run**.

The debug build installs as `com.childoo.app.debug`, so it can sit next to a future release build on the same device.

### Download a build

Every push to the repository is built by GitHub Actions. Open the **Actions** tab, select the latest run, and download **`childoo-debug-apk`** from *Artifacts*.

## Device setup

Childoo needs a few system permissions to do its job. The setup screen links straight to each one.

| Permission | Why it is needed |
|---|---|
| Usage access | To detect when a chosen app is open |
| Display over other apps | To show the challenge on top of that app |
| Notifications | To show that protection is running |
| Unrestricted battery | So the system does not stop protection in the background |

**Sideloaded builds on Android 13+.** When the APK is installed outside an app store, Android blocks *Usage access* with an "App was denied access" message. Open *App info* → **⋮** → **Allow restricted settings**, then grant Usage access again. The app explains this inline. Store installs are not affected.

**Xiaomi / Redmi / POCO (MIUI & HyperOS)** need two extra switches, also linked from the setup screen:

- **Autostart** → on
- **Other permissions** → *Display pop-up windows while running in the background* → allow

### Quick test

1. Complete onboarding: pick YouTube, choose **Try it out (1 min)**, grant the permissions.
2. Tap **Preview a challenge** to confirm the overlay appears.
3. Turn protection on.
4. Open YouTube and watch for a minute — the challenge appears.

## Privacy

Childoo is built for children, so privacy is the default rather than a setting.

- No account, sign-in or personal data is required.
- Usage data is read on the device only to measure play time, and is never stored per app or sent anywhere.
- The app requests only launcher-visible apps (`<queries>`) instead of the full package list.
- No analytics, advertising or third-party SDKs are included.

## Third-party assets

- [Vazirmatn](https://github.com/rastikerdar/vazirmatn) font, SIL Open Font License 1.1 — see `docs/licenses/Vazirmatn-OFL.txt`.

## Project structure

```
childoo/
├── app/                 Parent setup app
├── lockkit/             Runtime enforcement
├── challenge/           Challenge UI and content
├── designsystem/        Theme, typography and characters
├── core/                Settings and shared logic
├── gradle/              Version catalog and wrapper
└── .github/workflows/   CI pipeline
```

## Testing

```bash
./gradlew testDebugUnitTest
```

Unit tests cover the play-time rules (capping, target-only counting, interval checks) and the question generator (valid, distinct, non-negative options across thousands of seeds).

## Status

Childoo is in **early development**. The current build is an internal prototype that validates the core loop on real devices. APIs and module boundaries may change.

## License

Copyright © 2026 Amirhossein Abbaszadeh. All rights reserved.

This source code is proprietary. No part of it may be copied, modified or distributed without prior written permission. *Childoo*, *Nuzo*, *Lotli* and *Embrix* are trademarks of their owner.
