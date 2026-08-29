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

---

### TEST-005: Gate 5 — Space Domain & Persistence Physical Verification
* **Date:** 2026-08-29
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Phase 5 — Space Domain & Persistence
* **Related Task:** Phase 5 — Space Domain & Persistence

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android phone with Multi-Space Launcher installed.

#### Physical Test Matrix (Tests A through H)

##### Test A: Default Space Initialization
1. Fresh installation or cleared app data.
2. Launch Multi-Space Launcher.
3. Open Configuration modal sheet from top bar.
4. **Verify:** "Default" Space exists with `space_default` ID and is marked as Active Space.
5. **Verify:** SQLite database table `spaces` contains 1 record with name "Default".

##### Test B: Create Space
1. Open Space Management inside Configuration modal sheet.
2. Tap "Create", enter name "Work", and confirm.
3. **Verify:** "Work" Space appears in the list with a new stable ID (e.g. `space_a1b2c3d4e5f6`).
4. Close the launcher completely and remove from Recents.
5. Reopen launcher -> Open Configuration.
6. **Verify:** Both "Default" and "Work" Spaces exist.

##### Test C: Rename Space
1. Tap Edit (pencil icon) next to "Work".
2. Change name to "Office & Productivity" and confirm.
3. **Verify:** List updates immediately to "Office & Productivity".
4. Kill launcher process via ADB (`adb shell am force-stop com.aistudio.multispace.fndn`) or App Info.
5. Reopen launcher.
6. **Verify:** The Space retains the name "Office & Productivity" with the same stable ID.

##### Test D: Multiple Spaces Creation
1. Create Space "Personal", Space "Games", and Space "Focus".
2. **Verify:** All 5 Spaces appear in the list ordered deterministically.
3. Reboot the physical device.
4. Unlock phone and open launcher.
5. **Verify:** All 5 Spaces remain persisted in Room database.

##### Test E: Safe Deletion & Active Space Fallback
1. Create a temporary Space "Temp".
2. Select "Temp" as the Active Space (radio button).
3. Tap Delete on "Temp" and confirm.
4. **Verify:** "Temp" is removed from the database, and the Active Space automatically and safely falls back to "Default" (or another remaining Space).
5. Attempt to delete all Spaces until only 1 remains.
6. **Verify:** Deletion of the final remaining Space is disabled/prevented with appropriate user feedback ("Cannot delete the only remaining Space").

##### Test F: Application Membership Persistence
1. Tap "Memberships" (folder icon) next to "Office & Productivity".
2. Check/select 3 applications (e.g. Chrome, Gmail, Calculator).
3. Close the modal sheet.
4. Force stop launcher process or restart phone.
5. Reopen launcher -> Memberships for "Office & Productivity".
6. **Verify:** All 3 applications remain checked/associated in Room `space_memberships`.

##### Test G: Duplicate Membership Prevention
1. Attempt to associate an already-linked application to the same Space.
2. **Verify:** Room composite primary key constraint `(space_id, package_name, component_name, user_handle_id)` prevents duplicate records. The membership count matches exactly.

##### Test H: Complete Process Recreation Recovery
1. Assign specific apps to "Default" and "Personal".
2. Switch active Space.
3. Simulate background process death using `adb shell am kill com.aistudio.multispace.fndn` or Developer Options "Don't keep activities".
4. Return to Home.
5. **Verify:** Active Space, all Space entities, and all membership records are completely reconstructed from Room and DataStore without data loss.

#### Expected Result
* Multi-Space Launcher successfully creates, persists, renames, deletes, and recovers Spaces and application memberships in Room SQLite database, maintaining active Space state in DataStore and recovering completely after process death and reboots.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Phase 5 Space Domain & Persistence implemented, verified with compile_applet, and ready for human on-device verification.
* **Tested By:** UNKNOWN

---

### TEST-006: Per-Space Application Presentation Physical Verification
* **Date:** 2026-08-29
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Per-Space Application Presentation
* **Related Task:** Per-Space Application Presentation

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android phone with Multi-Space Launcher installed.
* At least 5 installed third-party/system applications.

#### Physical Test Matrix (Tests A through J)

##### Test A: Configuration vs Home Separation
1. Launch Multi-Space Launcher normally from the application drawer / home icon.
2. **Verify:** Directly opens "Multi-Space Configuration" management screen with Space list, Default Home status, and App memberships access.
3. Set Multi-Space Launcher as Default Home.
4. Press the hardware or gesture Home button.
5. **Verify:** Opens the clean Launcher Home screen showing the active Space name and the 4-column application grid. No configuration dashboard, telemetry, or role controls appear on Home.

##### Test B: Initial Space Presentation
1. Open Launcher Home.
2. **Verify:** Active Space name ("Default") is shown in the top header chip.
3. **Verify:** Only applications assigned to "Default" appear in the grid. If no apps are assigned, the empty space state displays cleanly with "No apps in Default".

##### Test C: Real-Time Membership Changes
1. Open Configuration -> Tap "Apps" next to "Default".
2. Select 2 new applications (e.g. Chrome, Calculator).
3. Return to Launcher Home.
4. **Verify:** Chrome and Calculator appear immediately in the Home grid without requiring an app restart or manual reload.
5. Return to Configuration -> Deselect Chrome.
6. Return to Launcher Home.
7. **Verify:** Chrome is removed from the Home grid immediately upon confirmation of the write.

##### Test D: Two Spaces App Isolation
1. In Configuration, create Space "Work".
2. Assign "Gmail", "Slack" (or two work apps) to "Work".
3. Assign "Gallery", "YouTube" (or two personal apps) to "Personal".
4. **Verify:** "Personal" contains only its assigned apps; "Work" contains only its assigned apps.

##### Test E: Space Switching
1. On Launcher Home, tap the Space switcher chip on top.
2. Select "Work".
3. **Verify:** Home title updates to "Work" and the grid immediately displays only "Gmail" and "Slack".
4. Switch back to "Personal".
5. **Verify:** Grid immediately updates to "Gallery" and "YouTube" with zero stale apps or cross-Space leakage.

##### Test F: Persisted Ordering
1. In Configuration, assign apps to a Space in a specific sequence (App 1, App 2, App 3).
2. Open Launcher Home.
3. **Verify:** Applications appear on the grid in the deterministic persisted order (`order_index ASC, added_at ASC`) without alphabetical reordering.

##### Test G: Restart Persistence
1. With configured Spaces and assigned apps, force stop the launcher process (`adb shell am force-stop com.aistudio.multispace.fndn`) or reboot the device.
2. Reopen launcher / press Home.
3. **Verify:** Active Space, membership associations, and exact application grid ordering are completely restored from Room SQLite and DataStore.

##### Test H: Unavailable Application Handling
1. Assign an application to the active Space.
2. Uninstall or disable that application via Android system settings or ADB.
3. Return to Launcher Home.
4. **Verify (Zero Crash):** Launcher Home renders cleanly. The uninstalled app is omitted from the visible grid. The Space remains fully functional.
5. In Configuration, inspect memberships for that Space -> **Verify:** The durable membership record in Room remains intact.

##### Test I: Reinstallation Recovery
1. Reinstall the previously uninstalled application from Test H.
2. Allow discovery to detect package addition.
3. Return to Launcher Home.
4. **Verify:** The reinstalled application reappears in the active Space grid at its persisted order position without requiring manual re-assignment.

##### Test J: Full Regression
1. Verify Home role capture, tap-to-launch external applications, return-to-home via Home button, Space creation, rename, safe deletion with active fallback, and catalog discovery remain fully operational.

#### Expected Result
* Multi-Space Launcher Home functions as a clean, deterministic projection of the active Space, displaying only that Space's available applications in persisted order, updating reactively on membership changes, switching Spaces without leakage, safely handling unavailable apps, and remaining separate from the configuration screen.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Per-Space Application Presentation implemented, verified with compile_applet, and ready for human on-device verification.
* **Tested By:** UNKNOWN

---

### TEST-007: Space PIN Security & Local Authentication Physical Verification
* **Date:** 2026-08-29
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Space PIN Security & Local Authentication
* **Related Task:** Space PIN Security & Local Authentication

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android device running Android 9.0+ (API 28+).
* At least two Spaces configured ("Default", "Confidential").

#### Physical Test Matrix (Tests A through H)

##### Test A: Enable PIN Protection on Space
1. Open Multi-Space Configuration.
2. In the Spaces list, locate "Confidential" Space.
3. Tap "+PIN" action button.
4. Enter numeric PIN `1234`, confirm `1234`, and tap "Enable PIN".
5. **Verify:** Green "PIN" badge appears on "Confidential" card. Action button updates to "PIN" and an orange Remove PIN button appears.

##### Test B: Locked Presentation State on Home
1. From Configuration, select "Confidential" as active Space (or switch to it).
2. Lock the space by restarting the launcher or forcing stop.
3. Open Launcher Home.
4. **Verify:** "Confidential is Protected" locked card displays with lock icon and "Enter PIN" button.
5. **Verify (Zero App Leak):** No applications assigned to "Confidential" are rendered on the Home grid while locked.

##### Test C: PIN Verification & Unlocking
1. On the locked Home screen, tap "Enter PIN".
2. Enter an incorrect PIN `0000` -> **Verify:** "Incorrect PIN. Please try again." error message is displayed. Space remains locked.
3. Enter correct PIN `1234` -> tap "Unlock".
4. **Verify:** Dialog dismisses, Space unlocks in memory, and the 4-column application grid renders all assigned member applications.

##### Test D: Gated Space Switching
1. From Home screen, tap the Space switcher dropdown.
2. Observe that "Confidential" has a lock icon next to its name.
3. Switch to "Default" (unprotected) -> **Verify:** Home switches immediately to "Default" and shows "Default" apps.
4. From the switcher dropdown, tap "Confidential" (locked) -> **Verify:** PIN unlock dialog appears before switching.
5. Enter correct PIN -> **Verify:** Space switches to "Confidential" and displays its applications.

##### Test E: Change PIN
1. In Configuration, tap "PIN" button on "Confidential" card.
2. Enter wrong current PIN -> verify error.
3. Enter correct current PIN `1234`, new PIN `5678`, confirm `5678` -> tap "Update PIN".
4. **Verify:** Snackbar shows "PIN changed successfully."
5. Test unlocking with old PIN `1234` (fails) and new PIN `5678` (succeeds).

##### Test F: Disable PIN Protection
1. In Configuration, tap the orange Remove PIN button on "Confidential" card.
2. Enter current PIN `5678` -> tap "Disable PIN".
3. **Verify:** "PIN" badge disappears. Card button reverts to "+PIN". Space becomes freely accessible without authentication prompts.

##### Test G: Restart Session Cleansing
1. Enable PIN on a Space and unlock it in memory.
2. Force stop launcher process (`adb shell am force-stop com.aistudio.multispace.fndn`) or reboot.
3. Reopen launcher.
4. **Verify:** In-memory unlock state is reset. Protected Space requires PIN entry again before displaying applications.

##### Test H: Plaintext Security & Diagnostics Audit
1. Inspect Logcat output during PIN setup, change, unlock, and failure.
2. **Verify (Zero Plaintext Rule):** No plaintext PINs, raw salts, or hashes are output to Logcat. Only high-level non-sensitive operational records are logged under `MSLauncher:LAUNCHER`.

#### Expected Result
* Space PIN security protects confidential Spaces using robust PBKDF2 hashing, conceals member apps until authenticated, gates Space switching smoothly, provides intuitive management dialogs, and preserves complete security without logging sensitive credentials.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Space PIN Security implemented, build verified with compile_applet, and ready for human on-device verification.
* **Tested By:** UNKNOWN

---

### TEST-008: Space Customization, Theming & Layout Physical Verification
* **Date:** 2026-08-29
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Space Customization, Wallpapers, Grid Sizing & App Ordering
* **Related Task:** Space Customization, Wallpapers, Grid Sizing & App Ordering

#### Preconditions
* Debug APK built successfully from repository (`app-debug.apk`).
* Physical Android device running Android 9.0+ (API 28+).
* At least two Spaces configured ("Work", "Personal") with assigned apps.

#### Physical Test Matrix (Tests A through G)

##### Test A: Space Solid Background Color Customization
1. Open Launcher Home or Configuration.
2. Tap "Customize Space..." from the Space switcher dropdown or tap "Style" on the Space card in Configuration.
3. Select "Solid Color" background preset (e.g. "Midnight Navy" or "Forest Pine").
4. Tap "Apply Customization".
5. **Verify:** Active Space Home background immediately updates to the selected solid color with high-contrast text and icons.

##### Test B: Space Custom Wallpaper Image Customization
1. Open Space Customization dialog.
2. Tap "Custom Image / Photo" -> tap "Choose Wallpaper Image".
3. Select an image from device gallery via the system Photo Picker.
4. Tap "Apply Customization".
5. **Verify:** Active Space displays the custom wallpaper photo with a subtle dimming scrim for optimal icon readability and white header/label text.

##### Test C: Grid Columns Adjustment (3 to 6 columns)
1. Open Customization dialog -> switch to "Grid & Icons" tab.
2. Select 3 columns -> apply -> **Verify:** Home grid displays 3 spacious columns.
3. Switch to 5 or 6 columns -> apply -> **Verify:** Home grid adapts dynamically to compact 5/6 column layout without clipping or horizontal scroll.

##### Test D: Icon Sizing & Label Visibility
1. Open Customization dialog -> switch to "Grid & Icons" tab.
2. Select "Large (64dp)" icon size and toggle "Show Application Labels" to OFF.
3. Tap "Apply Customization".
4. **Verify:** Home grid displays enlarged icons without text labels beneath them for a clean, minimalist aesthetic.
5. Toggle "Small (44dp)" icon size and enable labels -> **Verify:** Icons and labels adjust cleanly.

##### Test E: In-Space App Reordering
1. Open Customization dialog -> switch to "App Order" tab.
2. Tap the Up/Down arrow buttons next to an app to move its relative sequence.
3. Tap "Sort A-Z" button -> **Verify:** Apps list sorts alphabetically from A to Z.
4. Tap "Apply Customization".
5. **Verify:** Home grid immediately renders the applications in the updated custom sequence.

##### Test F: Per-Space Isolation of Customizations
1. Configure "Work" Space with "Deep Teal" background and 5 columns.
2. Configure "Personal" Space with "Midnight Navy" background and 4 columns.
3. Switch back and forth between "Work" and "Personal" on Home.
4. **Verify:** Each Space preserves and instantly applies its independent visual theme and grid preferences without cross-space contamination.

##### Test G: Persistence Across Reboots
1. Force stop the launcher (`adb shell am force-stop com.aistudio.multispace.fndn`) or reboot the device.
2. Reopen Multi-Space Launcher.
3. **Verify:** All visual customization settings (background type/color/URI, grid columns, icon size, label visibility, and app order) persist perfectly from Room SQLite database.

#### Expected Result
* Every Space can be independently styled with solid colors, wallpaper photos, flexible grid columns (3-6), customizable icon sizes, toggleable labels, and custom app ordering, persisting cleanly in Room across app restarts and reboots.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Space Customization & Theming fully implemented and verified via compile_applet; ready for human on-device verification.
* **Tested By:** UNKNOWN

---

### TEST-009: Lifecycle, Package Changes, and Recovery Hardening Physical Verification
* **Date:** 2026-08-29
* **Build Version:** 1.0 (versionCode 1)
* **Git Commit:** UNKNOWN
* **Device:** UNKNOWN
* **Android Version:** UNKNOWN (Targeting Android 9.0+ / API 28+)
* **API Level:** UNKNOWN
* **Feature:** Lifecycle, Package Changes, and Recovery Hardening
* **Related Task:** Lifecycle, Package Changes, and Recovery Hardening

#### Preconditions
* Debug APK built successfully (`app-debug.apk`).
* Physical Android device running Android 9.0+ (API 28+).
* Multi-Space Launcher set as default Home launcher.
* At least three distinct Spaces configured ("Personal", "Work", "Study") with different apps, PINs, and customizations.

#### Physical Test Matrix (Tests A through O)

##### Test A: Activity Recreation & Configuration Change
1. Open Launcher Home on active Space "Work".
2. Rotate device between Portrait and Landscape or trigger a dark/light mode toggle.
3. **Verify:** Active Space "Work", application grid layout, wallpaper, and icon order reconstruct instantly without blank screens or resetting to Default Space.

##### Test B: Process Recreation / Process Kill
1. While on Launcher Home in Space "Work", invoke Home or background the launcher.
2. Kill the launcher process via ADB (`adb shell am kill com.aistudio.multispace.fndn`) or trigger low-memory termination.
3. Tap Home or reopen the launcher.
4. **Verify:** Process restarts, loads SQLite database and DataStore, and reconstructs active Space "Work" and its applications cleanly.

##### Test C: Launcher Restart
1. Force stop the launcher (`adb shell am force-stop com.aistudio.multispace.fndn`).
2. Relaunch Multi-Space Launcher.
3. **Verify:** Active Space, membership projection, grid dimensions, and styling recover reliably from Room and DataStore.

##### Test D: Complete Device Reboot
1. Configure custom background and grid on "Personal" and "Work" Spaces.
2. Reboot the Android device (`adb reboot` or physical power cycle).
3. Unlock device to system Home.
4. **Verify:** Multi-Space Launcher starts cleanly upon boot completion, reads persistent state, and renders the correct active Space without requiring manual setup or background services.

##### Test E: New Application Installation
1. Install a new APK or app from Play Store/ADB (`adb install test_app.apk`).
2. **Verify:** Dynamic package listener (`LauncherApps.Callback` / `BroadcastReceiver`) refreshes catalog.
3. Open Configuration -> "Manage Apps" -> verify the new app appears in the available catalog.
4. Return to Launcher Home -> verify the new app is NOT automatically added to every Space (respecting explicit user membership).

##### Test F: Application Uninstallation
1. Assign an app (e.g. "Test App") to Space "Work".
2. Uninstall the application from device (`adb uninstall test_package`).
3. Return to Launcher Home on Space "Work".
4. **Verify:** Uninstalled application is gracefully omitted from the Home grid without crash or layout corruption.
5. Open Configuration -> Space "Work" -> verify membership entry is preserved in database for potential reinstall.

##### Test G: Application Reinstallation Recovery
1. Reinstall the previously uninstalled application (`adb install test_app.apk`).
2. Return to Launcher Home on Space "Work".
3. **Verify:** Application automatically reappears on the Home grid in its exact original custom `order_index` sequence.

##### Test H: Application Disablement
1. Disable a system or user app assigned to a Space (`adb shell pm disable-user <package>`).
2. Return to Launcher Home.
3. **Verify:** Disabled application is safely omitted from presentation without error. Re-enabling the app restores it immediately.

##### Test I: Stale Membership / Changed Activity Component
1. If an application updates its manifest and changes or removes its launcher Activity component name:
2. Open Launcher Home.
3. **Verify:** Component fallback resolves via package name or safely omits the stale item without crashing the launcher process.

##### Test J: Invalid Active Space Reference Self-Healing
1. Simulate corrupted DataStore active Space pointer pointing to a non-existent ID (`"deleted_space_xyz"`).
2. Launch or resume Multi-Space Launcher.
3. **Verify:** `RoomSpaceRepository.ensureDefaultSpaceInitialized()` detects missing Space, automatically heals pointer to the first valid Space in SQLite, and persists the corrected active pointer.

##### Test K: Customization Recovery Across Lifecycle
1. Configure "Personal" with 3 columns, small icons, and photo wallpaper; configure "Work" with 6 columns, large icons, hidden labels, and solid color.
2. Force-kill and restart launcher.
3. **Verify:** Both Spaces completely retain their independent visual styling and grid layout configurations without corruption.

##### Test L: Protected Space PIN Security Recovery
1. Enable PIN protection on "Work" Space and unlock it during active session.
2. Kill the app process or reboot device.
3. Reopen launcher and switch to "Work" Space.
4. **Verify:** Transient unlock state has reset; "Work" Space is strictly locked and requires PIN entry before displaying application icons.

##### Test M: Multi-Space Consistency Stress Test
1. Set up three Spaces ("Personal", "Work", "Study").
2. Switch rapidly between them: Personal -> Work (enter PIN) -> Study -> Personal.
3. **Verify:** Zero cross-Space contamination; app membership, grid columns, wallpapers, and labels match each Space definition.

##### Test N: Combined Stress Cycle
1. Execute a comprehensive stress cycle: Create Space -> Add Apps -> Set Wallpaper -> Set PIN -> Launch App -> Return Home -> Uninstall App -> Reboot -> Authenticate -> Reinstall App -> Return Home.
2. **Verify:** System remains responsive, stable, deterministic, and crash-free throughout.

##### Test O: Full Subsystem Regression Verification
1. Verify all previous capabilities remain functional:
   - System Home role & Return interception
   - App discovery & icon caching
   - Safe app launching
   - Space CRUD (Create, Rename, Delete)
   - Space membership management
   - Space switching popover & radio selection
   - PIN setup, change, disable, and unlock
   - Solid colors, custom photo wallpapers, grid sizing, label toggling, and app reordering
   - Home vs Configuration surface separation
2. **Verify:** All subsystems are 100% operational with zero regressions.

#### Expected Result
* Multi-Space Launcher reconstructs all active Space state, memberships, customization settings, and PIN protection after process recreation, configuration changes, reboots, and package lifecycle events without data corruption or crash.

#### Actual Result
* `NOT PERFORMED` (Awaiting human tester execution on physical test hardware).

#### Result
* **Status:** `NOT PERFORMED`
* **Observations:** Lifecycle, Package Changes, and Recovery Hardening implemented and verified via compile_applet; ready for human on-device verification.
* **Tested By:** UNKNOWN





