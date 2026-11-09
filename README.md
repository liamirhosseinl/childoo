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
| **Languages** | English (more languages planned) |

## Features

**Current prototype**

- **18 learning games on three islands.** Number Planet (counting, number bonds, balance, number line, fair sharing, patterns, and the arcade games Space Hockey and Number Snake), Word Lagoon (first sounds, spelling, rhyme, categories, sight words, Word Snake) and Courage Mountain (calm breathing, movement, memory, Penalty Kick). Every game explains *why* after a correct answer.
- **The child chooses.** When play time is up, the child picks an island. Each island shows its level and progress, and the choices build an interests chart for the parent.
- **Adaptive and fair.** Levels follow each child's results per game; random tapping is detected and answered with one more round, never with punishment. Each game levels up on its own as the child improves.
- **An animated buddy.** Lotli, the axolotl, plays along in every game: waving hello, cheering right answers, comforting after a miss and celebrating level-ups, all with fully animated character clips. Nuzo and Embrix are coming soon.
- **Coins and rewards.** The child meets the buddy on the first challenge. Every finished game pays coins, with a bonus for first-try answers and daily streaks; coins level the buddy up.
- **Kid's Corner.** The child's own space, open without the PIN: see the buddy, spend coins on hats, glasses and backpacks, and dress up. Purchases never cost real money.
- **Gentle sound and full screen.** Soft effects and quiet generated music per world, each switchable by the parent; challenges and the app run full screen. (Read-aloud returns with recorded voices per language.)
- **App-aware play timer.** Counts only the time spent in the apps the parent chose, and only while the screen is on.
- **Challenge overlay.** A full-screen challenge appears over the target app as soon as the interval is used up. The Back button cannot dismiss it.
- **Fair by design.** If the child leaves the app, the challenge steps aside. It returns, unsolved, the moment they come back. Picture-in-picture and split screen count as on screen.
- **Video pauses.** The video behind the challenge pauses while the child plays and resumes afterwards.
- **Parent PIN.** Settings and the off switch sit behind a 4-digit PIN, with lockout after repeated tries.
- **Self-healing.** A watchdog restarts protection if the system kills it.
- **Survives restarts.** Protection resumes automatically after a reboot or an app update. Play time is persisted, so force-closing does not reset it.
- **Guided onboarding.** One decision per screen: child, apps, interval, permissions, PIN. Only two system switches are required, and the app returns to the front by itself after each one.
- **Parent dashboard.** Live countdown to the next challenge, challenges solved, protected apps and interval at a glance.
- **Instant test mode.** Trigger a challenge on demand to verify the setup in seconds.

**On the roadmap**

- Nuzo and Embrix animation sets (same pipeline as Lotli: `games/scripts/character-clips.py` with a spec in `games/scripts/clips/`)
- Parent PIN gate, tamper alerts and weekly progress reports
- Subscription plans and a parent web dashboard
- iOS app (Screen Time API)

## Architecture

Childoo is a multi-module Android project. Each module has one responsibility and can be developed, tested and replaced on its own.

```
┌──────────────────────────────────────────────────────────────┐
│ :app        Onboarding · parent PIN · dashboard · app picker │
└──────┬──────────────────────────────────────────────┬────────┘
       │                                              │
┌──────▼──────────────┐   ┌───────────────────┐   ┌───▼──────────────┐
│ :lockkit            │──▶│ :gamehost         │   │ :designsystem    │
│ App detection       │   │ WebView · bridge  │   │ Theme · fonts    │
│ Monitor service     │   │ Voice · haptics   │   │ Buddy · widgets  │
│ Overlay · watchdog  │   └────────┬──────────┘   └──────────────────┘
└──────┬──────────────┘            │ assets/games (built from games/)
       │                  ┌────────▼──────────┐
       │                  │ games/ (TypeScript)│
       │                  │ Engine · 18 games │
       │                  └───────────────────┘
┌──────▼──────────────────────────────────────────────────────┐
│ :core       Settings (DataStore) · play-time rules · PIN      │
└─────────────────────────────────────────────────────────────┘
```

| Module | Responsibility | Key types |
|---|---|---|
| `core` | Persisted parent settings and the pure play-time logic | `SettingsRepository`, `LockSettings`, `PlayTimeAccumulator` |
| `designsystem` | Brand theme, typography, the animated buddy and shared components | `ChildooTheme`, `Buddy`, `StarrySky`, `ChunkyButton` |
| `gamehost` | Runs the web game engine offline in a WebView, with voice and haptics | `GameView`, `GameBridge`, `GameConfig`, `GameResult` |
| `games/` | The game engine, all games and the character clip player (TypeScript, no framework) | `runSession`, `pickGame`, `Stage`, `GAMES`, `createClipMascot` |
| `challenge` | Native fallback challenge for devices without a working WebView | `ChallengeScreen`, `MathQuestion` |
| `lockkit` | Everything that enforces the rule at runtime | `UsageMonitorService`, `ForegroundAppDetector`, `OverlayController`, `LockKit` |
| `app` | Parent onboarding, PIN gate, dashboard and settings | `OnboardingFlow`, `ParentGate`, `HomeScreen`, `GuidedPermissions` |

### How enforcement works

1. `UsageMonitorService` runs as a foreground service (`specialUse`, parental control) and ticks once per second.
2. `ForegroundAppDetector` reads `UsageStatsManager` events incrementally to know which app is in front.
3. `PlayTimeAccumulator` adds time only when a target app is in front and the screen is interactive. Long gaps such as device sleep are capped, so they never count as play.
4. When the interval is reached, `OverlayController` attaches a `TYPE_APPLICATION_OVERLAY` window, takes audio focus so the video pauses, and loads the game engine from the app's own assets.
5. The engine picks a game for the child's age and history, plays it, and hands back the result. The new level is stored, the counter resets and play resumes.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose · Material 3 |
| Games | TypeScript, Vite, Vitest; no runtime dependencies |
| Typography | Fredoka (display) and Nunito (text), bundled |
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

The games are built into `gamehost/src/main/assets/games` and committed, so the Android build needs no Node.js. After changing anything in `games/`:

```bash
cd games
npm ci
npm test          # engine rules
npm run build     # rebuilds the bundle in gamehost/src/main/assets/games
npm run dev       # play in a browser: http://localhost:5173/?game=space-scale&level=3
```

Then build the app:

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

1. Complete onboarding: pick YouTube, choose **Try it out (1 min)**, grant the two permissions, create a PIN.
2. Tap **Try a game now** to confirm the overlay appears.
3. Turn protection on.
4. Open YouTube and watch for a minute — the challenge appears.

## Privacy

Childoo is built for children, so privacy is the default rather than a setting.

- No account, sign-in or personal data is required.
- Usage data is read on the device only to measure play time, and is never stored per app or sent anywhere.
- The app requests only launcher-visible apps (`<queries>`) instead of the full package list.
- No analytics, advertising or third-party SDKs are included.

## Third-party assets

- [Fredoka](https://github.com/google/fonts/tree/main/ofl/fredoka) and [Nunito](https://github.com/google/fonts/tree/main/ofl/nunito) fonts, SIL Open Font License 1.1 — see `docs/licenses/`.

## Project structure

```
childoo/
├── app/                 Parent setup app
├── lockkit/             Runtime enforcement
├── challenge/           Challenge UI and content
├── designsystem/        Theme, typography and characters
├── gamehost/            WebView host for the games
├── games/               Game engine and games (TypeScript)
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
