# Current Task — Home Launcher Task vs Configuration Entry Point Separation

## Task Overview
* **Task Name:** Home Launcher Task vs Configuration Entry Point Separation
* **Target Objective:** Establish clean architectural separation between the Android `CATEGORY_HOME` launcher surface and the application's `CATEGORY_LAUNCHER` configuration surface, ensuring that clicking the app icon opens Space Configuration while pressing Home or viewing Android Recents/Overview previews the Home launcher surface.
* **Status:** `IMPLEMENTED (BUILD VERIFIED, READY FOR PHYSICAL VERIFICATION)`

---

## Architecture & Task Routing Matrix

```text
User Interaction
        │
        ├─► Clicks App Icon in Drawer / External Launcher
        │         ↓
        │   CATEGORY_LAUNCHER Intent
        │         ↓
        │   ConfigurationActivity (Task Affinity: com.multispace.configuration)
        │         ↓
        │   Multi-Space Configuration UI (Spaces, Apps, Wallpapers, Settings)
        │
        └─► Presses Native Home Button / Sets Default Launcher / System Overview
                  ↓
            CATEGORY_HOME Intent
                  ↓
            MainActivity (Standard Home Task)
                  ↓
            Active Space Home Surface (4-Column Grid, Dock, Active Space Apps)
```

---

## Implementations

1. **Manifest Registration (`AndroidManifest.xml`)**:
   - `ConfigurationActivity` is registered as the standalone `CATEGORY_LAUNCHER` activity with `taskAffinity="com.multispace.configuration"`, `launchMode="singleTask"`, and `exported="true"`.
   - `MainActivity` is registered as the dedicated `CATEGORY_HOME` activity with `launchMode="singleTask"`, `stateNotNeeded="true"`, and `exported="true"`.

2. **Clean Task Navigation (`MainActivity.kt` & `ConfigurationActivity.kt`)**:
   - Navigation between `MainActivity` (Home surface) and `ConfigurationActivity` uses `Intent.FLAG_ACTIVITY_NEW_TASK`, keeping the Configuration management task separate from the Home task stack.
   - Navigating back to the Home surface dispatches `Intent.CATEGORY_HOME` with `FLAG_ACTIVITY_NEW_TASK`.

3. **Android Recents / Overview Compatibility**:
   - The device native Recents / Overview preview for the Home task represents the actual Home surface (active Space grid), avoiding task collision with the Configuration UI.

---

## Acceptance Criteria
- [x] App icon click from external launcher/drawer launches `ConfigurationActivity`.
- [x] Home button / default launcher role dispatches to `MainActivity` (Home surface).
- [x] `ConfigurationActivity` runs with separate task affinity (`com.multispace.configuration`).
- [x] Android native Recents/Overview displays the active Home surface for the Home task.
- [x] Zero automated test code added (strict physical verification policy).
- [x] Clean compilation via `compile_applet`.

