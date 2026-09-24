# 😴 Sleep Controller

### Enforced Sleep Schedule with App Blocking & Morning Verification

A native Android app that enforces bedtime discipline by blocking distracting apps during sleep hours and requiring photo-verified morning tasks (with ML Kit face detection) before unlocking.

---

## ✨ Features

- **App Blocking** — AccessibilityService detects blocked apps on launch, sends back action + full-screen overlay blocker
- **Sleep Session Lifecycle** — State machine: Schedule → Wind-Down → Sleep Mode → Morning Mode → Session End
- **Emergency Unlock** — 5 uses/session; uses 1–4 grant 20-min temporary access (AlarmManager auto-revert), use 5 fully disables
- **Morning Task Verification** — Configurable tasks with photo proof (brush teeth, exercise, etc.)
- **Anti-Cheat** — ML Kit face detection + EXIF validation (rejects screenshots, old photos, faceless images)
- **Per-Day Scheduling** — Set different bedtime/wake-up per day of week
- **Boot Persistence** — BootReceiver re-registers all alarms after restart
- **Sleep History & Streaks** — Room-backed history with streak counter
- **Material 3 UI** — Jetpack Compose with dynamic theming

---

## 🏗️ Architecture

```
Clean Architecture:
  UI Layer:     Compose Screens → ViewModels → UseCases
  Domain Layer: UseCases, Repository interfaces, SessionState model
  Data Layer:   Room DAOs/Entities, DataStore Preferences, Repositories

Services:
  AccessibilityService → OverlayBlockerService → SleepSessionManager

Scheduling:
  AlarmReceiver, WindDownReceiver, SleepModeReceiver, BootReceiver

DI: Hilt modules + ServiceEntryPoint for AccessibilityService injection
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose, Material 3 |
| **Architecture** | Clean Architecture (data/domain/ui) |
| **DI** | Hilt (Dagger) |
| **Database** | Room + KSP |
| **Preferences** | DataStore |
| **Background** | WorkManager, AlarmManager |
| **ML** | ML Kit Face Detection (Play Services) |
| **Services** | AccessibilityService, Overlay (TYPE_APPLICATION_OVERLAY) |
| **Navigation** | Compose Navigation + Kotlin Serialization |
| **Build** | Gradle KTS, compileSdk 36, minSdk 26, ProGuard (R8) |

---

## 📝 License

This is a private project. © 2025 Aman (Hawks Hydra). All rights reserved.