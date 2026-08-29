# Architecture Reference — Multi-Space Android Launcher

## 1. Architectural Overview

The Multi-Space Android Launcher is a single-module, local-first Android home-screen application. It enables users to organize their applications into distinct presentation contexts ("Spaces") on a single physical device without modifying the underlying Android OS, user profiles, or installed application storage.

```text
                    ANDROID OS
                        │
              ┌─────────┴─────────┐
              │                   │
         Home / Launcher     Installed Apps
              │                   │
              ▼                   ▼
       ┌─────────────────────────────────┐
       │      Multi-Space Launcher        │
       │                                 │
       │  Presentation (Compose)         │
       │       │                         │
       │       ▼                         │
       │  Domain / Space State           │
       │       │                         │
       │       ├──────────────┐          │
       │       ▼              ▼          │
       │   Persistence   Android APIs    │
       │   (Room/DS)     (LauncherApps)  │
       └───────┼──────────────┼───────────┘
               │              │
               ▼              ▼
          App-Private    Android Package
            Storage         Manager
```

---

## 2. Core Architectural Boundaries

### Boundary 1: Android Platform Authority vs Space Ownership
* **Android OS owns:** Installed application packages, system metadata, launchable activities, live icons, and runtime lifecycle.
* **Launcher owns:** Spaces, Space membership associations, arrangement/ordering, presentation preferences, and local PIN configuration.
* **Design rule:** The launcher stores references to stable component identities (`package/activity`), never authoritative copies of app binaries or OS permissions.

### Boundary 2: Durable vs Ephemeral State
* **Durable (Survives process death/reboot):** Space definitions, memberships, layouts, preferences, active Space selection. (Owned by Room / DataStore in future phases).
* **Ephemeral (Reconstructed at runtime):** Compose UI hierarchy, focus, active transitions, in-memory icon caches, temporary error banners. (Owned by ViewModel / StateFlow).

### Boundary 3: Launcher Presentation vs OS-Level Security
* **Launcher Protection:** A PIN gates display of the launcher UI for a protected Space.
* **Not OS Sandbox:** Does not prevent app access via system settings, notifications, direct intents, or third-party surfaces.

---

## 3. Package Structure (Single-Module `app`)

```text
com.example/
├── MainActivity.kt               # Standard Android Application Entry Point
├── presentation/                 # UI Layer (Jetpack Compose, Themes, Screens)
│   ├── Phase0FoundationScreen.kt # Foundation Status Verification Composable
│   └── theme/                    # Material 3 Color, Type, Theme
├── domain/                       # Core Space models & business invariants
│   └── DomainContract.kt         # Structural boundary placeholder
├── data/                         # Persistence contracts & local storage
│   └── DataContract.kt           # Structural boundary placeholder
├── platform/                     # Android OS & Launcher API adapters
│   └── PlatformContract.kt       # Structural boundary placeholder
└── diagnostics/                  # Logging and development telemetry
    └── AppLogger.kt              # Standardized Logcat diagnostics
```

---

## 4. Current Implementation vs Architectural Intent

| Subsystem | Architectural Intent (V1 Target) | Current Phase 0 Implementation |
| :--- | :--- | :--- |
| **Module Structure** | Single application module | Single `app` module configured |
| **UI Toolkit** | Jetpack Compose (Material 3) | Compose configured and active (`Phase0FoundationScreen`) |
| **Android Entry** | Home application (`ROLE_HOME`) | Standard launcher activity (Home role deferred to Phase 1) |
| **Diagnostics** | Structured Logcat categories | `AppLogger` with `MSLauncher` tag categories |
| **App Discovery** | `LauncherApps.getActivityList()` | Placeholder boundary (`PlatformContract`) |
| **App Launching** | `LauncherApps.startMainActivity()` | Placeholder boundary (`PlatformContract`) |
| **Space Domain** | Space entity, invariants, active state | Placeholder boundary (`DomainContract`) |
| **Persistence** | Room (Spaces/Membership) + DataStore | Placeholder boundary (`DataContract`) |
| **Authentication** | Local PIN with Android Keystore | Planned for Phase 8 |

---

## 5. Technology Baseline
* **Language:** Kotlin 2.2.10
* **UI Toolkit:** Jetpack Compose with Material 3 (Compose BOM 2024.09.00)
* **Target SDK / Compile SDK:** 36 (Android 16 baseline)
* **Minimum SDK:** 28 (Android 9.0 baseline)
* **Build System:** Gradle with Kotlin DSL
