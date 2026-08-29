# Physical Test Log — Multi-Space Android Launcher

This document contains the authoritative history of physical on-device functional testing performed by human testers.

> **CRITICAL RULE:** Only human testing on a physical device may issue a `PASS` rating. Build success or simulated tool checks must never be recorded as a physical device pass.

---

## Test Records

### TEST-000: Initial Foundation & Lifecycle Verification
* **Date:** 2026-08-28
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN (Initial repository setup)
* **Device:** UNKNOWN (Physical test device not yet connected in this session)
* **Android Version:** UNKNOWN
* **API Level:** UNKNOWN
* **Feature:** Phase 0 — Project Foundation
* **Related Task:** Phase 0 — Project Foundation Initialization

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android device with USB debugging enabled.

#### Test Steps
1. Install debug APK via ADB (`adb install -r app-debug.apk`).
2. Launch application from the app drawer / home screen.
3. Verify that the "Phase 0 — Project Foundation" status screen renders with Material 3 styling.
4. Verify Logcat output emits `MSLauncher:LIFECYCLE` `MainActivity onCreate`.
5. Press Home/Back to exit application.
6. Reopen application from recents or app drawer.
7. Confirm clean relaunch without crash or visual glitch.

#### Expected Result
* Application installs cleanly, launches immediately, renders Phase 0 verification UI, logs lifecycle events, and exits/reopens reliably.

#### Actual Result
* `NOT PERFORMED` (Physical device test has not yet been executed by human developer).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Build succeeded in container environment; waiting for human tester execution on physical hardware.
* **Tested By:** UNKNOWN

---

### TEST-001: Gate 1 — Default Launcher & Home Key Interception Physical Verification
* **Date:** 2026-08-28
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Phase 1 — Launcher Viability Spike
* **Related Task:** Phase 1 — Launcher Viability Spike

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android phone with developer options and USB debugging enabled.

#### Test Steps
1. Install debug APK (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
2. Launch Multi-Space Launcher from the current app drawer.
3. Verify that the "Phase 1: Launcher Spike" UI renders with Clean Utility styling and displays "HOME INTENT CONFIGURED" status.
4. Tap the "Set as Default Home Launcher" button.
5. In the Android system role chooser dialog / Settings screen, select "Multi-Space Launcher" as the default Home app.
6. Tap "Check Role" on the spike screen -> verify status turns green with "DEFAULT LAUNCHER ACTIVE".
7. Press the physical/gesture Home button while in the launcher -> verify live telemetry logs `onNewIntent: Home key captured (singleTask active)` and no duplicate Activity is spawned.
8. Tap "Test App Launch" to open device Settings.
9. While inside Settings, press the hardware/gesture Home button -> verify device immediately and smoothly returns to Multi-Space Launcher.
10. Open Recents / Overview screen -> swipe away or return -> verify launcher stability.
11. Reboot the physical phone -> verify that Multi-Space Launcher starts or activates upon unlocking without crashing or bootlooping.

#### Expected Result
* Multi-Space Launcher is accepted by Android as the default Home app, smoothly catches Home key/gesture presses, returns seamlessly from external apps, and persists across reboots.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Phase 1 codebase builds green and is ready for physical hardware verification.
* **Tested By:** UNKNOWN

---

### TEST-002: Phase 2 — Application Discovery & Dynamic Package Tracking Physical Verification
* **Date:** 2026-08-28
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Phase 2 — Application Discovery & LauncherApps Integration
* **Related Task:** Phase 2 — Application Discovery & LauncherApps Integration

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android phone with 20+ installed applications and optional Work profile.

#### Test Steps
1. Install debug APK (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
2. Launch Multi-Space Launcher -> confirm App Catalog loads and displays total app count.
3. Verify that all standard installed user applications and launchable system applications appear with clear icons and titles.
4. Scroll through the grid view rapidly -> verify smooth 60fps rendering without jank or icon flicker.
5. Tap the view toggle button -> verify switch to detailed List view mode with package names and version tags.
6. Type in the search field (e.g. "Play" or "Settings") -> verify instant search filtering.
7. Tap the filter chips ("User Apps", "System") -> verify item list updates to match category filters.
8. Sideload or install a test APK (or install any app from Google Play) while Multi-Space Launcher is running -> verify telemetry reports `Package Added` and list updates automatically.
9. Uninstall a test app -> verify telemetry reports `Package Removed` and icon cache is purged.

#### Expected Result
* Application discovery indexes all launchable activities across user profiles accurately, renders icons with high performance via LruCache, filters/searches in real time, and reacts instantly to package install/uninstall events.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Phase 2 codebase compiles cleanly with 0 errors and is ready for physical hardware verification.
* **Tested By:** UNKNOWN

---

### TEST-003: Phase 3 — Application Launching Viability Spike Physical Verification
* **Date:** 2026-08-28
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Phase 3 — Application Launching
* **Related Task:** Phase 3 — Application Launching Viability Spike

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android device with 10+ installed third-party apps, system apps, and active developer options.

#### Physical Test Matrix (Tests A through F)

##### Test A: Standard Application Launch
1. Open Multi-Space Launcher on physical hardware.
2. Tap a standard installed user application (e.g. Calculator, Chrome, YouTube).
3. **Verify:** Application opens immediately into the foreground without delay or black frame.
4. **Logcat Verify:** `MSLauncher:LAUNCH` logs `LAUNCH_REQUESTED`, `LAUNCH_RESOLUTION_STARTED`, `LAUNCH_RESOLUTION_SUCCESS`, `LAUNCH_ATTEMPTED`, and `LAUNCH_SUCCESS`.

##### Test B: System Application Launch
1. Switch filter to "System" or search for a system app (e.g. "Settings" or "Camera").
2. Tap the system application item.
3. **Verify:** System application launches smoothly via `LauncherApps.startMainActivity`.

##### Test C: Clean Return to Launcher via Home Button
1. While inside the external application launched in Test A or B, press the physical or gesture Home button.
2. **Verify:** Android returns directly and immediately to Multi-Space Launcher.
3. **Lifecycle Verify:** `MainActivity` receives `onNewIntent` (if already active) or `onRestart/onStart/onResume` smoothly without state corruption.

##### Test D: Stale / Unavailable Component Handling
1. Using ADB or system settings, force-disable or uninstall a test application while keeping the launcher open without an immediate manual refresh.
2. Tap the disabled/uninstalled application entry.
3. **Verify (Zero Crash Mandate):** Multi-Space Launcher DOES NOT crash.
4. **Verify Feedback:** A clean, concise user feedback Snackbar appears: *"Unable to open application: Application is unavailable."*
5. **Logcat Verify:** `MSLauncher:LAUNCH` logs `LAUNCH_UNAVAILABLE` or `LAUNCH_FAILED`.

##### Test E: Component Redirection / Fallback
1. Tap an application whose activity declaration has changed or whose primary entry was updated.
2. **Verify:** Launch engine attempts fallback to alternative launcher activity in package or `PackageManager.getLaunchIntentForPackage`.
3. **Logcat Verify:** `LAUNCH_FALLBACK_USED` is emitted.

##### Test F: Live Telemetry & Log History
1. Navigate to the "Telemetry" tab in bottom navigation.
2. **Verify:** The "LAUNCH_DIAGNOSTICS" terminal card displays the most recent launch attempt and formatted history of the last 5 launches.

#### Expected Result
* All launchable apps open reliably, Home button returns seamlessly, stale/unavailable components fail gracefully without crashing, and launch diagnostics record all transitions accurately.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Phase 3 launch subsystem implemented, verified with compile_applet, and ready for human on-device verification.
* **Tested By:** UNKNOWN

---

### TEST-004: Phase 4 — Minimal Usable Launcher Physical Verification
* **Date:** 2026-08-28
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Phase 4 — Minimal Usable Launcher Home Surface
* **Related Task:** Phase 4 — Minimal Usable Launcher

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android phone with Multi-Space Launcher set as default Home application.

#### Physical Test Matrix (Tests A through G)

##### Test A: Clean Home Startup
1. Set Multi-Space Launcher as the default Home app.
2. Press the hardware/gesture Home button or reboot the phone.
3. **Verify:** Launcher opens directly to `LauncherHomeScreen` showing the 4-column application grid, clear top bar with available app count, and search input.
4. **Verify:** Loading state shows briefly then resolves cleanly without visual flashing or layout jumps.

##### Test B: Grid Layout & Smooth Scrolling
1. Scroll vertically through the 4-column grid.
2. **Verify:** Icons and labels are sharp, well-aligned, and centered.
3. **Verify:** Scrolling remains fluid (60/120 fps) without stutter, dropped frames, or icon re-rendering glitches.

##### Test C: Immediate Tap-to-Launch
1. Tap any application on the Home grid (e.g. Settings, Chrome, Clock).
2. **Verify:** The application opens immediately into the foreground.
3. **Verify:** No launcher lag, black screen, or UI freeze before launch.

##### Test D: Return to Home
1. While inside the external application, press the physical or gesture Home button.
2. **Verify:** Android returns directly and immediately to `LauncherHomeScreen`.
3. **Verify:** The grid retains scroll position and state smoothly.

##### Test E: Repeated Launch and Return Loop
1. Launch app A -> press Home -> Launch app B -> press Home -> Launch app C -> press Home.
2. **Verify:** Repeated cycle runs without memory leaks, process crashes, or launcher slowdowns.

##### Test F: Real-time Catalog Refresh
1. Install or update an application from Google Play / ADB while Multi-Space Launcher is running.
2. **Verify:** The new application appears automatically in the Home grid without requiring manual restart.
3. Uninstall a third-party app -> **Verify:** The item vanishes from the Home grid instantly.

##### Test G: Empty & Error State Handling
1. Type a non-existent search query (e.g. `xyz123nonsense`) into the search bar.
2. **Verify:** The empty state card appears cleanly with "No matches found" and a "Clear Search" button.
3. Tap "Clear Search" -> **Verify:** Full application grid is instantly restored.
4. Open the Settings/Info modal sheet from top bar -> **Verify:** Role status ("Active Default Home") and catalog statistics are displayed accurately, with optional link to engineering diagnostics.

#### Expected Result
* Multi-Space Launcher operates as an everyday usable Android Home surface, reliably launching applications, catching Home keys, reacting to package events in real time, and maintaining clean separation between Home usage and engineering telemetry.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Phase 4 Minimal Usable Launcher implemented, verified with compile_applet, and ready for human on-device verification.
* **Tested By:** UNKNOWN


