# Project Context: Loud Alarm - Solve2Wake

## Overview
**Loud Alarm - Solve2Wake** is an Android application designed to ensure users wake up by requiring them to solve various challenges before the alarm can be silenced. The app features a premium subscription model, a dark-themed glassmorphism UI, and a robust alarm scheduling system.

- **Package**: `com.loud.alarm`
- **Version**: 1.0 (versionCode 5)
- **Min SDK**: 26 | **Target/Compile SDK**: 35

## Tech Stack
- **Platform**: Android (Native, single-activity architecture)
- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose (BOM 2024.02.01, Material3)
- **Architecture**: MVVM with Hilt (DI), ViewModels, Room (local DB), DataStore (preferences)
- **Build**: Gradle with KSP, R8 minification + resource shrinking enabled
- **Java Version**: 17 (source, target, JVM)
- **Key Libraries**:
  - `CameraX 1.4.1` & `ML Kit` (Play Services variants): Barcode scanning and image labeling for challenges.
  - `Google Play Billing 7.1.1`: Premium subscription model.
  - `DataStore 1.1.1`: User preferences.
  - `Navigation-Compose 2.7.7`: Single-activity navigation.
  - `Accompanist 0.34.0`: Runtime permissions.
  - `Hilt 2.50` / `Room 2.6.1`: DI and local database.
  - `Compose Compiler Extension`: 1.5.8

## Project Structure
```text
app/src/main/java/com/loud/alarm/
├── MainActivity.kt            # Single activity, hosts NavHost
├── LoudAlarmApp.kt            # Hilt Application class
├── billing/
│   ├── BillingManager.kt      # Google Play Billing integration (~17KB)
│   └── BillingViewModel.kt    # Billing state management
├── data/
│   ├── Alarm.kt               # Room entity + ChallengeType/MathDifficulty enums
│   ├── AlarmDao.kt            # Room DAO
│   ├── AlarmDatabase.kt       # Room database definition
│   ├── AlarmRepository.kt     # Data access layer
│   ├── SettingsRepository.kt  # DataStore-backed settings
│   └── VibrationPattern.kt    # 6 vibration patterns (1 free, 5 premium)
├── di/
│   ├── BillingModule.kt       # Billing Hilt module
│   ├── DatabaseModule.kt      # Room + repository providers
│   └── ServiceModule.kt       # Service-layer DI
├── service/
│   ├── AlarmReceiver.kt       # BroadcastReceiver for alarm triggers
│   ├── AlarmScheduler.kt      # Scheduler interface
│   ├── AlarmSchedulerImpl.kt  # AlarmManager-based scheduling
│   ├── AlarmService.kt        # Foreground service for ringing (~16KB)
│   ├── BootReceiver.kt        # Re-schedules alarms on boot
│   ├── RescheduleReceiver.kt  # Re-schedules on timezone/time change
│   ├── WakeUpCheckReceiver.kt # "Are you awake?" follow-up alarm
│   └── WakeUpCheckConfirmReceiver.kt
└── ui/
    ├── alarm/
    │   ├── AlarmActivity.kt        # Full-screen alarm ringing UI (~26KB)
    │   └── AlarmActiveViewModel.kt
    ├── challenge/                  # 8 Solve-to-Wake challenge screens
    │   ├── BarcodeChallengeScreen.kt
    │   ├── MathChallengeScreen.kt
    │   ├── MathProblemGenerator.kt
    │   ├── MazeChallengeScreen.kt
    │   ├── MemoryChallengeScreen.kt
    │   ├── RewriteChallengeScreen.kt
    │   ├── ScanChallengeScreen.kt  # ML Kit image labeling
    │   └── StepChallengeScreen.kt
    ├── components/
    │   └── SkeuomorphicSwitch.kt   # Custom toggle component
    ├── editor/
    │   ├── AlarmEditorScreen.kt    # Alarm config UI (~108KB, largest file)
    │   └── AlarmEditorViewModel.kt
    ├── home/
    │   ├── HomeScreen.kt           # Main alarm list
    │   └── HomeViewModel.kt
    ├── onboarding/
    │   ├── OnboardingScreen.kt     # First-run experience (~43KB)
    │   └── OnboardingViewModel.kt
    ├── permissions/
    │   └── PermissionSetupScreen.kt # Runtime permission flow
    ├── settings/
    │   ├── SettingsScreen.kt       # App settings (~39KB)
    │   ├── SettingsViewModel.kt
    │   └── AlarmReliabilityScreen.kt # Battery/OEM optimization guide
    ├── subscription/
    │   └── SubscriptionScreen.kt   # Premium features & billing UI
    └── theme/
        ├── Color.kt               # Color tokens
        ├── Theme.kt               # Material3 theme with dark mode
        └── Type.kt                # Typography definitions
```

## Navigation Routes
| Route | Screen | Description |
|-------|--------|-------------|
| `bootstrap` | Loading | Checks onboarding state, redirects |
| `onboarding` | OnboardingScreen | First-run walkthrough |
| `home` | HomeScreen | Main alarm list |
| `editor?alarmId={id}` | AlarmEditorScreen | Create/edit alarm (id=-1 = new) |
| `settings` | SettingsScreen | App settings |
| `alarm_reliability` | AlarmReliabilityScreen | Battery optimization guide |
| `permission_setup` | PermissionSetupScreen | Runtime permissions |
| `subscription` | SubscriptionScreen | Premium purchase flow |

## Challenge Types (12 total)
`NONE`, `MATH` (4 difficulties), `QR_CODE`, `REWRITE`, `STEP`, `MAZE`, `MEMORY`, `SHAKE`, `SPELL_BEE`, `PUZZLE`, `SCAN_SINK`, `SCAN_OBJECT`

## Vibration Patterns (6 total)
- **Free**: Device Default
- **Premium**: Breeze, Pulse, Heartbeat, SOS, Blast

## Premium Features
- **Power Vibrations**: 5 custom vibration patterns (Breeze, Pulse, Heartbeat, SOS, Blast).
- **Wake Up Check**: Follow-up alarm after 1–30 minutes to confirm user stayed awake.
- **7+ Challenges**: Extended challenge types beyond free tier.
- **Ad-Free Experience**: No advertisements.

## Alarm Entity Key Fields
`id`, `hour`, `minute`, `enabled`, `daysOfWeek`, `label`, `soundUri`, `challengeTypes` (multi-select), `mathDifficulty`, `barcodeValue`, `isVolumeBoostEnabled`, `wakeUpCheckMinutes`, `rewriteText`, `stepCount`, `sinkImageUri`, `scanObjectLabel`

## Design & UI
- **Theme**: Dark mode with glassmorphism aesthetic.
- **Background**: Full-screen image (`R.drawable.menu`) with 40% black overlay, transparent surfaces.
- **Typography & Colors**: Defined in `theme/` package (Color.kt, Type.kt, Theme.kt).
- **Material3**: Using Material Design 3 components throughout.

## Recent Development History
- **Java Compatibility**: Updated `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_17`.
- **Subscription UI**: Integrated "Power Vibrations" and "Wake Up Check" into the subscription model. Redesigned with a premium glassmorphism aesthetic. Added 3 additional premium features to incentivize upgrades.
- **Onboarding**: Completely redesigned for uniqueness. Fixed a crash in step 4/5.
- **App Size Optimization**: Transitioned ML Kit dependencies from bundled to Play Services variants (saving ~30-35 MB).
- **Google Stitch Integration**: Connected the project to Google Stitch for UI/Design synchronization.
- **AlarmEditorScreen**: Currently the largest and most actively edited file (~108KB). Primary working file.

## Current Focus & Objectives
1. **AlarmEditorScreen Development**: Actively iterating on the alarm editor UI and functionality (currently open file, cursor at line 1363).
2. **Premium Features Implementation**: Ensuring premium features (Power Vibrations, Wake Up Check, etc.) are fully functional across the app.
3. **Stability**: Monitoring recent onboarding and subscription UI changes for bugs.
4. **UI Refinement**: Continuing to polish the premium feel using glassmorphism and modern typography.

## Key Files (by importance)
| File | Size | Purpose |
|------|------|---------|
| `AlarmEditorScreen.kt` | ~108KB | Alarm creation/editing UI – **actively being worked on** |
| `OnboardingScreen.kt` | ~43KB | First-run experience |
| `SettingsScreen.kt` | ~39KB | App settings |
| `AlarmActivity.kt` | ~26KB | Full-screen alarm ringing |
| `SubscriptionScreen.kt` | ~23KB | Premium flow & feature showcase |
| `PermissionSetupScreen.kt` | ~22KB | Permission request flow |
| `HomeScreen.kt` | ~21KB | Main alarm list |
| `BillingManager.kt` | ~17KB | Google Play Billing logic |
| `AlarmService.kt` | ~16KB | Foreground alarm ringing service |
| `app/build.gradle.kts` | ~4KB | Dependency and SDK configuration |
