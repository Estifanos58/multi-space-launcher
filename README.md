# Multi-Space Launcher

[![Android CI](https://img.shields.io/badge/Platform-Android_10+-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVI%20%2F%20Room-009688?style=flat)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

An open-source, context-driven Android Launcher built with **Jetpack Compose**, **Kotlin Coroutines**, and **Room Database**. Multi-Space Launcher allows users to organize installed applications into dedicated, isolated workspaces ("Spaces") with per-space visual customizations, app ordering, and PIN-secured privacy.

---

## Key Features

- **Contextual Spaces**: Create custom workspaces (e.g., *Personal*, *Work*, *Focus*, *Reading*) with isolated application subsets and fast switching.
- **Customizable Home Surface**:
  - Grid density configuration (3 to 6 columns).
  - Icon size scaling (Small, Medium, Large).
  - Toggleable application labels.
  - Per-space backgrounds (Solid color presets or custom wallpaper images via SAF).
  - Custom app ordering and one-tap alphabetical sorting.
- **PIN-Secured Spaces**: Protect sensitive spaces using salted SHA-256 PIN authentication with session-based memory caching.
- **Clean Architecture & Strict Separation of Concerns**:
  - **Domain Layer**: Pure business logic with immutable models and repository interfaces.
  - **Data Layer**: Offline-first Room persistence and Android Jetpack DataStore preferences.
  - **Platform Layer**: Hardware/OS adapters for `LauncherApps`, `UserManager`, and `RoleManager` default Home integration.
  - **Presentation Layer**: Material 3 Jetpack Compose UI with reactive `StateFlow` streams.
- **System Diagnostics & Intent Verification**:
  - Real-time logging console for lifecycle events.
  - Role manager status verification (`ROLE_HOME`).
  - Intent resolution validation.

---

## Architecture Overview

Multi-Space Launcher adheres strictly to Clean Architecture and Modern Android Architecture guidelines:

```
                  ┌──────────────────────────────┐
                  │      Presentation Layer      │
                  │ (Compose, ViewModels, Theme) │
                  └──────────────┬───────────────┘
                                 │
                  ┌──────────────▼───────────────┐
                  │         Domain Layer         │
                  │ (Models, Repository API)     │
                  └──────────────┬───────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         │                                               │
┌────────▼──────────────┐                     ┌──────────▼──────────┐
│      Data Layer       │                     │    Platform Layer   │
│  (Room DB, DataStore, │                     │  (LauncherApps,     │
│   Entity Mappers)     │                     │   RoleManager,      │
└───────────────────────┘                     │   PinSecurity)      │
                                              └─────────────────────┘
```

---

## Project Structure

```
com.multispace/
├── MainActivity.kt                      # Root launcher activity & window insets configuration
├── data/
│   ├── DataContract.kt                  # Data layer contract definitions
│   ├── dao/
│   │   ├── SpaceDao.kt                  # Room DAO for space entities
│   │   └── SpaceMembershipDao.kt        # Room DAO for app space memberships
│   ├── database/
│   │   └── LauncherDatabase.kt          # Room SQLite database configuration
│   ├── entity/
│   │   ├── SpaceEntity.kt               # Room database space table schema
│   │   └── SpaceMembershipEntity.kt     # Room database membership table schema
│   ├── preferences/
│   │   └── LauncherPreferences.kt       # DataStore preference store
│   └── repository/
│       └── RoomSpaceRepository.kt       # Repository implementation combining Room & DataStore
├── diagnostics/
│   └── AppLogger.kt                     # Unified diagnostic logger
├── domain/
│   ├── DomainContract.kt                # Domain responsibility boundary definition
│   ├── model/
│   │   ├── DiscoveredApp.kt             # Domain entity for installed applications
│   │   ├── Space.kt                     # Domain workspace entity
│   │   └── SpaceMembership.kt           # Domain membership association
│   └── repository/
│       └── SpaceRepository.kt           # Domain repository interface
├── platform/
│   ├── AppDiscoveryManager.kt           # LauncherApps package discovery & broadcast receiver
│   ├── AppLaunchManager.kt              # App launch handler with fallback intent support
│   ├── HomePlatformManager.kt           # Default Home role & settings bridge
│   ├── PinSecurityManager.kt            # Salted SHA-256 hashing & verification
│   └── PlatformContract.kt              # Platform contract definition
├── presentation/
│   ├── AppCatalogScreen.kt              # Full application catalog & space assignment UI
│   ├── AppDiscoveryViewModel.kt         # ViewModel for app discovery & launch
│   ├── FoundationOverviewScreen.kt      # Architectural showcase & capability overview
│   ├── LauncherConfigurationScreen.kt   # Space management & settings UI
│   ├── LauncherDiagnosticsScreen.kt     # Diagnostic logs & intent verification UI
│   ├── LauncherHomeScreen.kt            # Clean primary launcher desktop surface
│   ├── SpaceCustomizationDialog.kt      # Space visual editor (background, grid, ordering)
│   ├── SpaceManagementComponents.kt     # Space creation & app association modals
│   ├── SpacePinDialogs.kt               # PIN lock, unlock, and modification dialogs
│   └── SpaceViewModel.kt                # ViewModel for space orchestration & persistence
└── ui/
    └── theme/
        ├── Color.kt                     # Material Design 3 color palette
        ├── Theme.kt                     # Dynamic / Dark / Light theme definitions
        └── Type.kt                      # Typography scale definitions
```

---

## Tech Stack & Libraries

| Category | Technology |
|---|---|
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Local Database** | Android Jetpack Room (SQLite) |
| **Preferences** | Android Jetpack DataStore |
| **Async / Streams** | Kotlin Coroutines & `Flow` / `StateFlow` |
| **Image Loading** | Coil Compose |
| **Target SDK** | Android 15 (API Level 35) |
| **Min SDK** | Android 8.0 (API Level 26) |

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+) or higher
- JDK 17+
- Android SDK with API level 35 installed

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/multispace/multispace-launcher.git
   cd multispace-launcher
   ```
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Run on a connected physical device or emulator running Android 8.0+.

```bash
# Build Debug APK via Gradle
./gradlew assembleDebug
```

---

## Security & Privacy

- **On-Device Only**: All space configurations, membership associations, and app metadata remain 100% on-device. No telemetry or external cloud sync is required.
- **PIN Protection**: PINs are never stored in plaintext. They are salted using `java.security.SecureRandom` and hashed using SHA-256 before storage in Room.

---

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
