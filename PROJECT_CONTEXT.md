# Project Context: Loud Alarm - Solve2Wake

## 1. Project Goal & Identity
**Loud Alarm - Solve2Wake** is a high-efficacy Android alarm application designed for heavy sleepers. It enforces wakefulness by requiring users to solve cognitive or physical challenges before an alarm can be silenced. The app targets a premium feel with a dark-themed glassmorphism aesthetic and skeuomorphic UI elements.

- **Package**: `com.loud.alarm`
- **Current Version**: 1.0 (versionCode 6)
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 35 (Android 15)
- **Identity**: Premium, Reliable, Unignorable.

---

## 2. Architecture & Tech Stack
The project follows a modern, single-activity architecture with **MVVM** and **Clean Architecture** principles.

- **Language**: Kotlin 2.1.0+
- **DI**: Hilt (Dagger) 2.59.2
- **UI Framework**: Jetpack Compose (BOM 2026.03.00)
- **Database**: Room 2.8.0 (persistent alarm storage)
- **Preferences**: DataStore (for settings like vibration, fade-in, etc.)
- **Build System**: Gradle 9.3.1 (JVM Target 21)
- **Key Libraries**:
  - `CameraX 1.4.1` + `ML Kit (Play Services)`: Barcode scanning (QR/Barcode) and Image Labeling (Scan Sink/Object).
  - `Google Play Billing 8.3.0`: In-app purchases and subscriptions.
  - `Accompanist Permissions 0.37.3`: Modern permission handling.
  - `Material Icons Extended`: For a rich UI icon set.

---

## 3. The "Solve to Wake" Alarm Flow
The alarm logic is divided into several layers to ensure reliability even if the app process is restricted.

### A. Scheduling (`AlarmScheduler`)
- Uses `AlarmManager.setAlarmClock()` for precision.
- Schedules `AlarmReceiver` (BroadcastReceiver) which starts the `AlarmService`.

### B. Ringing (`AlarmService`)
- **Type**: Foreground Service with a high-priority notification.
- **Persistence**: Implements an `activityWatchdogRunnable` every 1.5s to ensure the `AlarmActivity` remains visible (re-launches it if the user tries to escape to Home).
- **Audio Control**: Uses `MediaPlayer` and `LoudnessEnhancer` to boost volume by 1.5x. Managed by a `volumeEnforcerRunnable` to prevent the user from lowering volume during ringing.
- **Safety**: Uses a `WakeLock` to keep the CPU awake and `acquire` for 10 minutes max.

### C. UI & Interaction (`AlarmActivity`)
- **Fullscreen**: Shows on lock screen (above keyguard). Uses `setShowWhenLocked(true)`.
- **Flow**: `DismissOrSnoozeScreen` -> (If Dismiss/Snooze) -> `ActiveChallengeScreen` -> (Solve all challenges) -> `Finish`.
- **Snooze Logic**: Snoozing **requires solving challenges** (Solve-to-Snooze). The challenge header changes to "SOLVE TO SNOOZE". Actual snooze logic only triggers after all selected challenges are completed.

### D. Post-Alarm (`WakeUpCheck`)
- **Premium Feature**: If enabled, schedules a `WakeUpCheckReceiver` 1–30 minutes after alarm dismissal.
- **Confirmation**: Shows a notification or mini-alarm to confirm the user didn't fall back asleep.

---

## 4. Project Structure (Key Directories)
- `com.loud.alarm.billing`: Manages Google Play Billing lifecycle and subscriptions.
- `com.loud.alarm.data`: Entity definitions (Alarm, VibrationPattern), Room DAO/Database, and Repositories.
- `com.loud.alarm.service`: Core background logic (BroadcastReceivers, foreground `AlarmService`, and Scheduling).
- `com.loud.alarm.ui`:
  - `alarm`: The ringing UI and logic.
  - `challenge`: Individual screens for all 12 challenge types (Math, Maze, Barcode, etc.).
  - `editor`: The complex `AlarmEditorScreen` (~108KB) for configuring alarm settings.
  - `home`: Main entry point with the alarm list.
  - `subscription`: Premium feature showcase and purchase flow.
  - `theme`: Glassmorphism definitions, colors (`Color.kt`), and typography (`Type.kt`).

---

## 5. Detailed Component Logic
### Challenge System
Challenges are managed via the `ChallengeType` enum and chained in `AlarmActivity`. To add a new challenge:
1. Add to `ChallengeType` enum in `data/Alarm.kt`.
2. Create a new screen in `ui/challenge/`.
3. Update `ActiveAlarmScreen` in `AlarmActivity.kt` to include the new screen in the `when` block.

### Design System (The "Premium" Feel)
- **Glassmorphism**: Transparent surfaces, blurred backgrounds, and subtle borders.
- **Backgrounds**: Uses `R.drawable.menu` with 40% black overlay throughout the app.
- **Skeuomorphism**: Custom toggle switches and buttons (e.g., `SkeuomorphicSwitch.kt`) to differentiate from standard Material3 apps.

---

## 6. Premium Features & Monetization
The app uses a subscription model (`BillingManager.kt`) to unlock:
- **Power Vibrations**: 5 custom patterns (Blast, SOS, Heartbeat, etc.).
- **Wake Up Check**: Follow-up confirmation alarm.
- **Advanced Challenges**: Scan Object, Maze, Memory, etc. (Free tier usually limited to Math/Barcode).
- **Ad-Free**: (Interface prepared for ad placement removal).

---

## 7. Developer Notes & Gotchas
- **File Sizes**: `AlarmEditorScreen.kt` is the primary hub of configuration and is very large (~108KB/4000+ lines). Handle edits with care to maintain performance and avoid lint errors.
- **Gradle & Java**: The project has been updated to **Java 21** and **Gradle 9.3.1**. Ensure the local environment matches to avoid compatibility errors. 
- **Permissions**: Camera permission is requested *only when a camera challenge is chosen* in the editor (deferred permission pattern).
- **ML Kit Variants**: We use "Thin" (Play Services) variants of ML Kit image labeling and barcode scanning to keep the APK size low (~30-35MB saving). 
- **Volume Enforcing**: The app intentionally overrides ringer mode to "Normal" and sets volume to max during alarms. This is core to the "Loud" value proposition.

---

## 8. Recent Progress & Current Objectives
- **Solved**: Incompatibility between Java 25 and older Gradle versions by pinning to Gradle 9.3.1.
- **Solved**: "Solve to Snooze" requirement implemented.
- **Solved**: UI Refinement of `SubscriptionScreen` and `OnboardingScreen` for a more premium look.
- **Current Focus**: Refining `AlarmEditorScreen` fields, optimizing `WakeUpCheck` reliability, and ensuring all challenges handle camera permissions and fallbacks gracefully.
- **Roadmap**: Enhancing the Maze challenge with more levels and adding "Spell Bee" difficulty tiers.

---

**This file is for AI context only.** Update it whenever major architecture changes, library versions, or core flow logic are modified.
