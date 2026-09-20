# Multi-Space Launcher

[![Android Platform](https://img.shields.io/badge/Platform-Android_9+_(API_28+)-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/Target_SDK-API_36-009688?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM%20%2F%20Room-009688?style=flat)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

An open-source, context-driven Android Launcher built with **Jetpack Compose**, **Kotlin Coroutines**, and **Room Database**. Multi-Space Launcher allows users to organize installed applications into dedicated, isolated workspaces ("Spaces") with per-space visual customizations, app ordering, and PIN/Pattern-secured privacy.

---

## Key Features

- **Contextual Spaces**: Create custom workspaces (e.g., *Personal*, *Work*, *Focus*, *Reading*) with isolated application subsets and fast switching.
- **Customizable Desktop Surface (Layer 1)**:
  - Grid density configuration (3 to 6 columns).
  - Icon size scaling (Small, Medium, Large).
  - Toggleable application labels and page-turn physics.
  - Per-space backgrounds (Solid color presets or custom wallpaper images via SAF).
  - Custom app ordering, desktop folders, drag-and-drop management, and one-tap alphabetical sorting.
  - Native Android system widgets via application-scoped `AppWidgetHost`.
- **All-Apps Library Drawer (Layer 2)**:
  - Asynchronous package cataloging via `LauncherApps` and `UserManager`.
  - Non-blocking, reactive icon bitmap streaming (`StateFlow`) to prevent UI jank.
  - Search, categorization, and space assignment.
- **Launcher & Space Lock**:
  - Protect the entire launcher home surface and individual sensitive spaces using PBKDF2WithHmacSHA256 (120,000 iterations) with per-space cryptographically secure salts.
  - Biometric authentication integration.
- **Clean Architecture & Strict Separation of Concerns**:
  - **AppContainer**: Lightweight application-scoped service locator hosting singletons (repositories, discovery manager, widget host, security manager).
  - **Domain Layer**: Immutable data contracts, placement validation, and domain repositories.
  - **Data Layer**: Dual Room databases with active schema export and migrations.
  - **Platform Layer**: Hardware/OS adapters for `LauncherApps`, `UserManager`, `AppWidgetHost`, and `RoleManager` default Home integration.
  - **Presentation Layer**: Material 3 Jetpack Compose UI with reactive `StateFlow` streams.

---

## Architecture & Project Structure

Multi-Space Launcher adheres to Clean Architecture and Modern Android Architecture Guidelines:

```
com.multispace/
├── AppContainer.kt                      # Lightweight dependency locator (repositories, platform managers)
├── MultiSpaceApplication.kt             # Application class initializing AppContainer & widget host
├── MainActivity.kt                      # Root home activity (CATEGORY_HOME), window insets & lifecycle handling
├── ConfigurationActivity.kt             # Dedicated settings & diagnostics activity (CATEGORY_LAUNCHER)
│
├── data/
│   ├── DataContract.kt                  # Data layer contract definitions
│   ├── dao/
│   │   ├── SpaceDao.kt                  # Room DAO for space configurations
│   │   ├── SpaceMembershipDao.kt        # Room DAO for app-to-space memberships
│   │   ├── SpaceLayoutDao.kt            # Room DAO for desktop placements, folders, and dock items
│   │   └── LaunchHistoryDao.kt          # Room DAO for application launch telemetry
│   ├── database/
│   │   ├── LauncherDatabase.kt          # Primary Room Database (v11, schemas exported)
│   │   └── LaunchHistoryDatabase.kt     # Telemetry Room Database (v2, schemas exported)
│   ├── entity/                          # Room entities (SpaceEntity, SpaceMembershipEntity, etc.)
│   ├── preferences/
│   │   └── LauncherPreferences.kt       # Jetpack DataStore preferences (active space, lock flags)
│   └── repository/
│       ├── RoomSpaceRepository.kt       # Space & layout repository (atomic initialization, Room + DataStore)
│       └── RoomLaunchHistoryRepository.kt# Launch history and usage analytics repository
│
├── domain/
│   ├── DomainContract.kt                # Domain layer specifications
│   ├── model/
│   │   ├── DiscoveredApp.kt             # Installed application domain model
│   │   ├── Space.kt                     # Workspace domain model
│   │   ├── SpaceLayoutModels.kt         # Placements, Folders, and Dock items
│   │   └── PlacementValidator.kt        # Desktop layout validation
│   └── repository/
│       ├── SpaceRepository.kt           # Domain repository interface for spaces & layouts
│       └── LaunchHistoryRepository.kt   # Domain repository interface for launch telemetry
│
├── platform/
│   ├── AppDiscoveryManager.kt           # LauncherApps package scanning, icon decoding & cache prewarming
│   ├── AppLaunchManager.kt              # Suspending, lifecycle-safe launcher with fallback resolution
│   ├── HomePlatformManager.kt           # Android RoleManager (ROLE_HOME) & default launcher intents
│   ├── PinSecurityManager.kt            # PBKDF2 (120,000 iterations) key derivation & constant-time check
│   └── MultiSpaceAppWidgetHost.kt       # Application-scoped AppWidgetHost for home widgets
│
├── presentation/
│   ├── LauncherHomeScreen.kt            # Root Compose launcher UI (Layer 1 desktop + Layer 2 drawer)
│   ├── Layer1HomeScreen.kt              # Desktop grid, widgets, folders, dock, and drag-and-drop
│   ├── Layer2LibraryScreen.kt           # All-apps library drawer with search & filter
│   ├── SpaceViewModel.kt                # ViewModel managing spaces, layouts, folders, and lock states
│   ├── AppDiscoveryViewModel.kt         # ViewModel managing app catalog, iconBitmaps StateFlow, and launches
│   ├── MultiSpaceLockScreen.kt          # PIN, Pattern, and Biometric unlock surface
│   └── AppCatalogScreen.kt              # Application management and diagnostics inspector
│
└── ui/
    └── theme/                           # Material 3 ColorScheme, Typography, and Shapes
```

---

## Technical Specifications

| Component | Specification | Description |
|---|---|---|
| **Language** | Kotlin 2.2.10 | Modern Kotlin with coroutines and Flow |
| **Android Gradle Plugin** | AGP 9.1.1 | Standard AGP build toolchain |
| **Compile SDK** | API 36 (Android 16) | Latest Android compilation targets |
| **Target SDK** | API 36 (Android 16) | Full modern platform behavior compliance |
| **Min SDK** | API 28 (Android 9.0) | Broad modern device compatibility |
| **UI Framework** | Jetpack Compose (BOM 2024.09.00) | Declarative Material 3 design system |
| **Room Database** | Room 2.7.0 (KSP) | Schema export enabled (`$projectDir/schemas`) |
| **Preferences** | Jetpack DataStore 1.1.7 | Coroutine-first preferences storage |
| **Widget Host** | `AppWidgetHost` (Host ID: 20241) | Lifecycle-managed native widget rendering |
| **Security / KDF** | PBKDF2WithHmacSHA256 | 120,000 iterations, 16-byte cryptographically secure salt |

---

## Database Architecture & Migrations

Multi-Space Launcher utilizes two dedicated Room databases with SQLite WAL mode and verified schema export (`exportSchema = true`):

1. **`LauncherDatabase` (Version 11)**:
   - Tables: `spaces`, `space_memberships`, `space_dock_items`, `space_item_placements`, `space_folders`, `space_folder_items`.
   - **Migration History (v1 → v11)**:
     - `MIGRATION_1_2` through `MIGRATION_8_9`: Incremental additions for dock items, item placements, grid coordinates, page turn configuration, and folders.
     - `MIGRATION_9_10`: Deduplicated `space_item_placements` for application items by canonical package, component, and user handle identity.
     - `MIGRATION_10_11`: Deduplicated and added canonical identity indices on `space_dock_items`, `space_folder_items`, and `space_item_placements`, and resolved position index collisions between distinct desktop placements without deleting records.
   - **Atomic Initialization**: Space initialization executes atomically inside Room SQLite transactions. The existence check verifies actual database presence within the transaction, and default layout seeding is fully decoupled from DataStore operations to guarantee consistent state across process restarts.

2. **`LaunchHistoryDatabase` (Version 2)**:
   - Table: `launch_events` (indexed by space ID, canonical app identity, and timestamp).
   - **Migration History (v1 → v2)**: `MIGRATION_1_2` safely migrates legacy global launch history from `launch_history` table into space-isolated `launch_events` scoped to the system default space, and drops the legacy un-scoped table.
   - Records space ID, package name, activity name, user profile, and timestamp for telemetry and per-space most-used app resolution.

---

## App Launching & Icon Loading Architecture

- **Primary Suspending Launch Path**: `AppLaunchManager.launchAppSuspending` is the designated primary suspending launch API, executing platform activity resolution on `Dispatchers.IO`, respecting `LauncherApps.startMainActivity`, handling same-profile activity fallback, recording launch history, and persisting repaired component identities. The legacy non-suspending `launchApp` is deprecated and performs real synchronous launch checks without returning misleading speculative success states.
- **Asynchronous Reactive Icon Loading**: Icons are prewarmed and loaded asynchronously in background coroutines. The `AppDiscoveryViewModel` exposes an observable `iconBitmaps: StateFlow<Map<String, Bitmap>>`. UI composables collect this state reactively, eliminating UI thread stalls and preventing permanent `null` caching in `remember` blocks.

---

## Security & Cryptography

- **PBKDF2 Key Derivation**: PIN and pattern credentials use `PBKDF2WithHmacSHA256` with **120,000 iterations** (upgraded from 10,000 to comply with current NIST SP 800-63B and OWASP guidelines) and a 256-bit derived key.
- **Salt Generation**: Salts are generated per-space using `java.security.SecureRandom` (16 bytes).
- **Constant-Time Verification**: Hash comparison uses `MessageDigest.isEqual` to prevent timing attacks.
- **Lock State Management**: Launcher lock state is tracked via `isLauncherLocked` and individual space lock status in `SpaceViewModel`.

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+) or higher
- JDK 17+
- Android SDK with API level 36 installed

### Build via Gradle
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit & Robolectric Tests
./gradlew testDebugUnitTest
```

---

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
