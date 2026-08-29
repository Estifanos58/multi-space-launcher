# Project State

## Last Updated
2026-08-29

## Current State
Space PIN Security & Local Authentication (Implemented & Build Verified)

## Current Milestone
Milestone: Space PIN Security & Local Authentication

---

## State Breakdown

| Subsystem / Capability | Status | Evidence / Notes |
| :--- | :--- | :--- |
| Project Foundation & Gradle Build | `BUILDS` | Clean build with targetSdk 36, minSdk 28, Compose UI |
| Formalized Package Layout | `IMPLEMENTED` | `presentation`, `domain`, `data`, `platform`, `diagnostics` established |
| Lifecycle Diagnostics Logging | `IMPLEMENTED` | `AppLogger` utility integrated with categories `LIFECYCLE`, `LAUNCH`, `DISCOVERY`, `LAUNCHER` |
| Continuity Documentation System | `IMPLEMENTED` | Canonical `/docs/` structure maintained with core documents |
| Launcher / Home Role (`ROLE_HOME`) | `IMPLEMENTED` | `HOME` and `DEFAULT` intent categories in manifest, `singleTask` launchMode, `HomePlatformManager` |
| Home Interception & Return | `IMPLEMENTED` | `onNewIntent` handler in `MainActivity`, restores primary Home surface |
| App Discovery (`LauncherApps`) | `IMPLEMENTED` | Multi-profile `LauncherApps` query, `UserManager`, `queries` manifest declaration, PackageManager fallback; `QUERY_ALL_PACKAGES` removed |
| In-Memory Icon Caching | `IMPLEMENTED` | `LruCache` icon loading to guarantee jank-free scrolling |
| Dynamic Package Monitoring | `IMPLEMENTED` | `LauncherApps.Callback` monitoring installs, uninstalls, and package replacements |
| App Catalog UI & Filtering | `IMPLEMENTED` | Search, filter (All / User / System), sort (A-Z / Z-A / Recent), and Grid / List views |
| App Launching Integration | `IMPLEMENTED` | `AppLaunchManager` with `LauncherApps.startMainActivity`, multi-profile resolution, component verification, PackageManager fallback, zero-crash exception handling |
| Launcher Home Surface | `IMPLEMENTED` | Clean `LauncherHomeScreen` with active Space chip, 4-column application grid, tap-to-launch, empty state |
| Space Domain & Room Persistence | `BUILDS` | Room Database (`LauncherDatabase` v1), `SpaceDao`, `SpaceMembershipDao`, `SpaceEntity`, `SpaceMembershipEntity`, `RoomSpaceRepository`, default Space initialization, safe deletion, duplicate prevention |
| Active Space State via DataStore | `BUILDS` | `LauncherPreferences` managing single authoritative `active_space_id` |
| Multi-Space Configuration Surface | `IMPLEMENTED` | Dedicated `LauncherConfigurationScreen` for Space creation, rename, safe deletion, and per-app membership assignment |
| Space Switching Engine | `IMPLEMENTED` | Instantaneous Space switcher popover on Home header and radio selection in Configuration |
| Per-Space Application Presentation | `BUILDS` | Home surface projects strictly active Space memberships against dynamic `LauncherApps` catalog with persisted ordering and graceful unavailable app filtering |
| Space PIN Security | `BUILDS` | `PinSecurityManager` with PBKDF2WithHmacSHA256, cryptographically secure salts, constant-time verification, session unlock state flow, locked Home state, switcher gating, and PIN lifecycle dialogs |
| Wallpaper & Layout Customization | `PLANNED` | Scheduled for Layout Customization milestone |
| Lifecycle & Reboot Hardening | `PLANNED` | Scheduled for Hardening milestone |

---

## Product & Implementation State Summary
* **Product State:** Space PIN Security & Local Authentication Integrated. Users can configure independent PINs for individual Spaces. Protected Spaces require numeric PIN verification when opening or switching to them on the Launcher Home screen. Applications in locked Spaces are completely concealed until authentication succeeds. Cryptographic operations use standard PBKDF2WithHmacSHA256 with per-space salts. Plaintext PINs are never stored or logged.
* **Build State:** `BUILDS` (Debug APK compiles cleanly with 0 errors via Gradle KSP and Compose).
* **Physical Verification State:** `PENDING HUMAN TEST` (Physical test TEST-007 prepared for human verification).

---

## Completed in Current Task
* Created `PinSecurityManager.kt` with PBKDF2WithHmacSHA256 key derivation, 16-byte random salt generation, and constant-time verification.
* Updated `Space.kt` domain model and `SpaceEntity.kt` with `pinSalt`, `pinHash`, and `isProtected` helpers.
* Implemented `setSpacePin`, `changeSpacePin`, `disableSpacePin`, and `verifySpacePin` in `SpaceRepository` and `RoomSpaceRepository`.
* Added transient runtime session unlock tracking (`unlockedSpaceIds`) and PIN action methods to `SpaceViewModel`.
* Built modular Compose PIN dialogs in `SpacePinDialogs.kt` (`SetSpacePinDialog`, `ChangeSpacePinDialog`, `DisableSpacePinDialog`, `SpaceUnlockDialog`).
* Integrated locked presentation state and gated Space switcher dropdown in `LauncherHomeScreen.kt`.
* Added PIN security management controls, lock status badges, and dialog integration to `SpaceManagementComponents.kt` and `LauncherConfigurationScreen.kt`.
* Verified clean build via `compile_applet`.
