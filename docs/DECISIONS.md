# Decision Log — Multi-Space Android Launcher

## Record Format
* **Decision ID:** Unique identifier
* **Date:** Timestamp of decision
* **Type:** `DECISION` / `HYPOTHESIS` / `EXPERIMENT` / `UNKNOWN`
* **Status:** `ACTIVE` / `SUPERSEDED` / `REJECTED` / `UNRESOLVED`

---

### DECISION-001: Single-Module Application Architecture
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to structure the Android project without introducing multi-module build overhead or overengineering for a solo developer.
* **Chosen Approach:** Use a single Android application module (`app`) containing clearly partitioned packages (`presentation`, `domain`, `data`, `platform`, `diagnostics`).
* **Reason:** Keeps Gradle sync times minimal, avoids multi-module configuration drift, and maintains simple dependency injection by constructor without enterprise DI frameworks.
* **Rejected Alternatives:** Multi-module architecture (e.g. `:core`, `:feature-spaces`, `:core-data`). Rejected as premature complexity for V1.

---

### DECISION-002: Adoption of Jetpack Compose for UI
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Selecting the UI toolkit for the launcher presentation layer.
* **Chosen Approach:** Jetpack Compose with Material 3.
* **Reason:** Compose's declarative model (`UI = f(State)`) natively aligns with a launcher where presentation is directly derived from the currently active Space and discovered app state.
* **Rejected Alternatives:** Traditional XML Views. Rejected because state-driven dynamic layouts and Space switching transitions are significantly more verbose in imperative Views.

---

### DECISION-003: API Target and Compatibility Strategy
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Defining compileSdk, targetSdk, and minSdk.
* **Chosen Approach:** `compileSdk = 36`, `targetSdk = 36`, `minSdk = 28`.
* **Reason:** Target SDK 36 aligns with Google Play distribution mandates for 2026+. MinSdk 28 provides modern Android runtime APIs (`RoleManager` available from API 29, modern `LauncherApps` callbacks) while covering Android 9.0+.
* **Consequences:** Devices below Android 9.0 are unsupported; avoids legacy compatibility shims.

---

### DECISION-004: Package Structure & Responsibility Separation
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Ensuring future phases can be implemented without major structural refactoring.
* **Chosen Approach:** Establish package organization reflecting the Technical Blueprint:
  - `presentation`: Compose UI, themes, navigation, view models.
  - `domain`: Space entities, invariants, membership rules.
  - `data`: Room persistence, DataStore, secure credentials.
  - `platform`: Android `LauncherApps`, `RoleManager`, `PackageManager` adapters.
  - `diagnostics`: Structured Logcat logging.
* **Reason:** Prevents platform code from polluting domain logic and guarantees clear separation of concerns from day one.

---

### DECISION-005: Dependency Discipline in Phase 0
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Avoiding dependency bloat and long compilation times before features are needed.
* **Chosen Approach:** Keep only Compose, Activity, Lifecycle, and Coroutines active. Comment out Room, DataStore, Retrofit, Firebase, and DI libraries until their scheduled phases.
* **Reason:** Adheres strictly to the Phase 0 scope boundary and speeds up build verification.

---

### DECISION-006: Exclusion of Automated Application Test Suites
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Determining the validation strategy for device-dependent launcher behavior.
* **Chosen Approach:** Rely on Gradle compilation builds (`BUILDS`) and human-conducted physical device tests (`PHYSICAL_TEST_LOG.md`). Exclude Robolectric, Espresso, and UI automated suites.
* **Reason:** Emulators and automated test harnesses cannot accurately validate OEM-specific launcher role acquisition, gesture interactions, or physical process death behavior.

---

### DECISION-007: Home Intent Filter and SingleTask Launch Mode Architecture
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to configure the launcher Activity in `AndroidManifest.xml` to receive Home button presses, prevent duplicate Activity stack instances, and reset tasks on launch.
* **Chosen Approach:**
  - Add intent filters: `android.intent.action.MAIN` with categories `android.intent.category.HOME`, `android.intent.category.DEFAULT`, and `android.intent.category.LAUNCHER`.
  - Set `android:launchMode="singleTask"`.
  - Set `android:stateNotNeeded="true"` and `android:clearTaskOnLaunch="true"`.
  - Handle `onNewIntent(intent: Intent)` in `MainActivity` to process Home key presses when already active.
* **Reason:** Ensures standard Android Home lifecycle semantics. When the user presses Home from an external app or from within the launcher, Android routes to the existing `MainActivity` task and triggers `onNewIntent`, preventing memory leaks and duplicate backstacks.

---

### DECISION-008: Default Home Role Request and Detection Strategy
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to check default Home status and prompt the user to set Multi-Space Launcher as default across Android versions.
* **Chosen Approach:**
  - Encapsulate logic in `com.example.platform.HomePlatformManager`.
  - On Android 10+ (API 29+): Use `RoleManager.isRoleHeld(RoleManager.ROLE_HOME)` and `RoleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)`.
  - On fallback: Use `PackageManager.resolveActivity` for `CATEGORY_HOME` and launch `Settings.ACTION_HOME_SETTINGS`.
* **Reason:** Provides the native system dialog on modern Android without forcing users to manually navigate through deep Settings hierarchies, while maintaining a robust fallback for OEM customizations.

---

### DECISION-009: Multi-Profile Application Discovery via LauncherApps
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to discover all installed, launchable applications on modern Android across standard, work, and secondary user profiles without missing apps or crashing on managed devices.
* **Chosen Approach:**
  - Use `LauncherApps.getActivityList(null, profile)` iterating through all profiles from `UserManager.userProfiles`.
  - Include `<queries>` intent filter declaration in `AndroidManifest.xml` to ensure package visibility on Android 11+ (API 30+).
  - Implement a `PackageManager.queryIntentActivities` fallback in case `LauncherApps` returns empty.
* **Reason:** `LauncherApps` is the official, modern Android API designed specifically for custom home launchers, correctly respecting multi-profile work profiles, app restrictions, and managed user spaces.

---

### DECISION-010: In-Memory LruCache Icon Loading and Dynamic Package Change Monitoring
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to render dozens or hundreds of high-resolution application icons in Compose Grid/List views at 60fps without causing memory leaks, frame drops, or stale data when apps are installed/uninstalled.
* **Chosen Approach:**
  - Implement an in-memory `LruCache<String, Drawable>` in `AppDiscoveryManager`.
  - Register `LauncherApps.Callback` to listen to `onPackageAdded`, `onPackageRemoved`, and `onPackageChanged` events.
  - Automatically evict stale cached icons and trigger reactive flow updates when package events occur.
* **Reason:** Eliminates heavy IPC and bitmap decoding on every scroll event, maintaining a smooth 60fps framerate while keeping the app catalog synchronized in real-time with device package state.

---

### DECISION-011: Launcher-Aware Application Launching via LauncherApps.startMainActivity
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Selecting the proper Android platform mechanism to launch discovered applications across standard and managed user profiles without violating Android launcher identity.
* **Chosen Approach:**
  - Encapsulate application launch dispatch in `com.example.platform.AppLaunchManager`.
  - Use `LauncherApps.startMainActivity(ComponentName, UserHandle, Rect, Bundle)` as the primary launch engine.
  - Dynamically resolve the corresponding `UserHandle` at launch time from `UserManager.userProfiles`.
* **Reason:** `LauncherApps.startMainActivity` is Android's designated launcher API, automatically managing cross-profile transitions, security constraints, and window animation source bounds without manual `FLAG_ACTIVITY_NEW_TASK` plumbing.
* **Rejected Alternatives:** Generic `Context.startActivity(Intent)`. Rejected because it does not support multi-profile user handle routing natively.

---

### DECISION-012: Launch-Time Availability Validation and Graceful Error Handling
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to handle situations where an application was uninstalled, disabled, or had its launcher activity renamed while Multi-Space Launcher was in the foreground or suspended.
* **Chosen Approach:**
  - Before invoking the launch API, verify the target component against current `LauncherApps.getActivityList(packageName, userHandle)`.
  - If the exact activity has changed but the package remains, attempt recovery using an alternative launcher activity in the package or `PackageManager.getLaunchIntentForPackage`.
  - Catch all exceptions (`ActivityNotFoundException`, `SecurityException`, `IllegalArgumentException`, etc.) within `AppLaunchManager` and return a structured `LaunchResult`.
  - Display non-intrusive `Snackbar` user feedback on failure.
* **Reason:** Satisfies the foundational launcher mandate: a launch failure must NEVER crash, freeze, or terminate the launcher process.

---

### DECISION-013: Clean Separation of Launcher Home Surface and Diagnostics
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to present a minimal, everyday usable Home screen while retaining full access to engineering diagnostics and spike telemetry for developers and testers.
* **Chosen Approach:**
  - Establish `LauncherHomeScreen` as the primary, default Home surface in `MainActivity`.
  - Encapsulate configuration and system status within a clean modal bottom sheet (`LauncherConfigSheetContent`).
  - Route to `Phase2AppDiscoveryScreen` and `Phase1LauncherSpikeScreen` as dedicated diagnostics views accessible on demand.
* **Reason:** Ensures the launcher delivers a real, unencumbered user experience while maintaining zero loss of developer diagnostics.

---

### DECISION-014: Adaptive 4-Column Minimal Grid with Ellipsized Two-Line Labels
* **Date:** 2026-08-28
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Defining standard density, icon sizing, and label layout for the Phase 4 minimal Home surface.
* **Chosen Approach:**
  - Use `LazyVerticalGrid` with 4 fixed columns (phone standard) and vertical scrolling.
  - Render 56dp icon containers with 46dp bitmap icons inside rounded containers (16dp shape).
  - Use centered 12sp typography with 2-line maximum and ellipsis truncation.
* **Reason:** Adheres strictly to the Material 3 Clean Utility / Minimal design guidelines, providing spacious, readable, and touch-target compliant (48dp+) launcher interactions.




