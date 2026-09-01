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
* **Durable (Survives process death/reboot):** Space definitions, memberships, layouts, preferences, active Space selection. (Owned by Room / DataStore).
* **Ephemeral (Reconstructed at runtime):** Compose UI hierarchy, focus, active transitions, in-memory icon caches, temporary error banners. (Owned by ViewModel / StateFlow).

### Boundary 3: Launcher Presentation vs OS-Level Security
* **Launcher Protection:** A PIN gates display of the launcher UI for a protected Space.
* **Not OS Sandbox:** Does not prevent app access via system settings, notifications, direct intents, or third-party surfaces.

---

## 3. Presentation Pipeline

```text
Space persistence (Room / DataStore)
      ↓
Active Space (`active_space_id` via DataStore)
      ↓
Persisted Space Memberships (`SpaceMembershipDao` sorted by order_index ASC, added_at ASC)
      +
Android Launchable Catalog (`AppDiscoveryManager` multi-profile LauncherApps query)
      ↓
Derived Space Presentation (Matching component/package identity, preserving stored order, omitting unavailable apps)
      ↓
Launcher Home UI (`LauncherHomeScreen` 4-column clean grid)
```

---

## 4. Package Structure (Single-Module `app`)

```text
com.multispace/
├── MainActivity.kt               # Dedicated Android CATEGORY_HOME Launcher Entry Point (Home Surface)
├── ConfigurationActivity.kt      # Dedicated Android CATEGORY_LAUNCHER Entry Point (Space Configuration)
├── presentation/                 # UI Layer (Jetpack Compose, Themes, Screens, ViewModels)
│   ├── LauncherHomeScreen.kt     # Primary 4-column Launcher Home Surface (Active Space projection)
│   ├── LauncherConfigurationScreen.kt # Dedicated Space Management & Membership Surface
│   ├── CreateSpaceScreen.kt      # Dedicated Space Creation & App Assignment Screen
│   ├── AppCatalogScreen.kt       # Application Discovery & Management Surface
│   ├── SpaceManagementComponents.kt # Space Creation, Rename, Delete & Membership UI
│   ├── FoundationOverviewScreen.kt # Architecture & Phase Status Overview Screen
│   ├── LauncherDiagnosticsScreen.kt # Diagnostic Logcat & Intent Stream Inspector
│   ├── AppDiscoveryViewModel.kt  # Discovery & App Launch State Holder
│   ├── SpaceViewModel.kt         # Space Domain & Membership State Holder
│   └── ui/theme/                 # Material 3 Color, Type, Theme definitions
├── domain/                       # Core Space models & repository contracts
│   ├── model/
│   │   ├── Space.kt              # Space domain entity (stable ID, mutable name, timestamps)
│   │   ├── SpaceMembership.kt    # Space-App association domain entity
│   │   └── DiscoveredApp.kt      # Discovered launcher activity model
│   ├── repository/
│   │   └── SpaceRepository.kt    # Domain contract for Space persistence & operations
│   └── DomainContract.kt         # Structural boundary contract
├── data/                         # Persistence implementation & local storage
│   ├── database/
│   │   └── LauncherDatabase.kt   # Room Database (v1) with Space & Membership DAOs
│   ├── entity/
│   │   ├── SpaceEntity.kt        # Room table 'spaces' definition
│   │   └── SpaceMembershipEntity.kt # Room table 'space_memberships' definition
│   ├── dao/
│   │   ├── SpaceDao.kt           # SQLite queries for Spaces
│   │   └── SpaceMembershipDao.kt # SQLite queries for Space Memberships
│   ├── preferences/
│   │   └── LauncherPreferences.kt# Jetpack DataStore preference for single active_space_id
│   ├── repository/
│   │   └── RoomSpaceRepository.kt# SpaceRepository implementation backed by Room & DataStore
│   └── DataContract.kt           # Structural boundary contract
├── platform/                     # Android OS & Launcher API adapters
│   ├── HomePlatformManager.kt    # ROLE_HOME eligibility & system intent dispatching
│   ├── AppDiscoveryManager.kt    # Multi-profile LauncherApps discovery & package callbacks
│   ├── AppLaunchManager.kt       # LauncherApps.startMainActivity invocation & fallback
│   └── PlatformContract.kt       # Structural boundary contract
└── diagnostics/                  # Logging and development telemetry
    └── AppLogger.kt              # Standardized Logcat diagnostics with category tags
```

---

## 5. Subsystem Implementation Status

| Subsystem | Target Architecture | Current Implementation |
| :--- | :--- | :--- |
| **Module Structure** | Single application module | Single `app` module configured (`com.multispace`) |
| **UI Toolkit** | Jetpack Compose (Material 3) | Compose configured and active (`LauncherHomeScreen`, `LauncherConfigurationScreen`) |
| **Android Entry Points** | Separated Home vs Configuration Tasks | `MainActivity` (`CATEGORY_HOME`) and `ConfigurationActivity` (`CATEGORY_LAUNCHER`, affinity `com.multispace.configuration`) |
| **Diagnostics** | Structured Logcat categories | `AppLogger` with `MSLauncher` tag categories |
| **App Discovery** | `LauncherApps.getActivityList()` | Implemented via `AppDiscoveryManager` (zero `QUERY_ALL_PACKAGES`) |
| **App Launching** | `LauncherApps.startMainActivity()` | Implemented via `AppLaunchManager` with fallback |
| **Space Domain** | Space entity, invariants, active state | Implemented via `Space`, `SpaceMembership`, `RoomSpaceRepository` |
| **Persistence** | Room (Spaces/Membership) + DataStore | Implemented via `LauncherDatabase` (Room v1) and `LauncherPreferences` (DataStore) |
| **Presentation** | Active Space projection & filtering | Implemented in `LauncherHomeScreen` with persisted ordering & unavailable handling |
| **Authentication** | Local PIN with PBKDF2WithHmacSHA256 | Implemented in `PinSecurityManager`, `SpacePinDialogs`, and `SpaceViewModel` |
| **Customization** | Wallpapers, Grid, Icons, Ordering | Implemented in `SpaceCustomizationDialog` and `LauncherHomeScreen` |
| **Lifecycle & Recovery** | Self-healing state & catalog reconciliation | Implemented in `MainActivity`, `AppDiscoveryManager`, `RoomSpaceRepository` |

---

## 6. Lifecycle & Recovery Architecture

The integrated launcher is hardened against process recreation, activity destruction, device reboots, and package modifications:

```text
1. Persistent Storage (Authoritative Source)
   - Room SQLite Database: 'spaces' and 'space_memberships' tables.
   - Jetpack DataStore: 'active_space_id' string key.

2. State Reconstruction on Startup / Resume
   - MainActivity calls ensureDefaultSpaceInitialized() on onStart/onResume.
   - If SQLite has zero spaces, creates 'default_space' ("Default").
   - If active_space_id is missing or points to a deleted Space, heals to the first available Space.

3. Package Change Reconciliation
   - Dual listener: LauncherApps.Callback + BroadcastReceiver for package added/removed/replaced/changed.
   - Automatic LRU icon cache eviction.
   - Dynamic reconciliation: uninstalled apps disappear from Home view without deleting durable membership rows in Room; reinstalled apps automatically reappear with original order preserved.

4. Transient Authentication Security
   - Unlocked session state is in-memory only (unlockedSpaceIds).
   - Process recreation / reboot guarantees protected Spaces immediately return to locked state requiring PIN.
```

---

## 7. Technology Baseline
* **Language:** Kotlin 2.2.10
* **UI Toolkit:** Jetpack Compose with Material 3 (Compose BOM 2024.09.00)
* **Image Loading:** Coil Compose 2.6.0
* **Target SDK / Compile SDK:** 36 (Android 16 baseline)
* **Minimum SDK:** 28 (Android 9.0 baseline)
* **Build System:** Gradle with Kotlin DSL
