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

---

### DECISION-015: Stable Space Identifier Strategy & Default Space Rule
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to assign Space identifiers that remain stable across renames while guaranteeing the launcher always has a valid destination on first run or recovery.
* **Chosen Approach:**
  - Assign an immutable string identifier `space_` + 12-character truncated random UUID for user-created Spaces.
  - Define a well-known constant `space_default` with display name "Default" for initial system creation.
  - If database is queried and `getSpaceCount() == 0`, immediately create, insert, and activate `space_default`.
* **Reason:** Guarantees that renaming a Space never alters its database identity or membership relationships, and guarantees the launcher never starts in an unrecoverable 0-Space state.

---

### DECISION-016: Room Schema & Composite Key for Membership Persistence
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to persist Space-app relationships without duplicating the full Android application catalog or raw bitmap icons into SQLite.
* **Chosen Approach:**
  - Define `SpaceMembershipEntity` in table `space_memberships`.
  - Composite primary key: `(space_id, package_name, component_name, user_handle_id)`.
  - Foreign key on `space_id` referencing `spaces(id)` with `CASCADE` deletion.
  - Store only lightweight identity coordinates and integer `order_index`.
* **Reason:** Enforces domain uniqueness (an application cannot be added twice to the same Space), cleans up orphaned memberships automatically on Space deletion, and keeps SQLite storage minimal and fast.

---

### DECISION-017: Single Authoritative Location for Active Space State
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Whether to store the active Space ID in Room or Jetpack DataStore, avoiding duplicated or diverging state.
* **Chosen Approach:** Store `active_space_id` exclusively in Jetpack DataStore via `LauncherPreferences`.
* **Reason:** DataStore provides non-blocking, reactive `Flow<String?>` preference storage with atomic asynchronous writes. Keeping it in DataStore treats active Space selection as a fast global preference while Room stores structured relational data.
* **Rejected Alternatives:** Dual storage in both Room and DataStore. Rejected to prevent split-brain consistency bugs.

---

### DECISION-018: Deterministic Safe Deletion & Recovery Policy
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to handle deletion of a Space, especially when deleting the currently active Space or the only remaining Space.
* **Chosen Approach:**
  - If `getSpaceCount() <= 1`, reject deletion with `IllegalStateException("Cannot delete the only remaining Space")`.
  - If deleting the currently active Space, query remaining Spaces, atomically switch `active_space_id` in DataStore to the first remaining valid Space, then execute the Room delete.
* **Reason:** Guarantees deterministic, crash-proof behavior and prevents invalid launcher states.

---

### DECISION-019: Removal of QUERY_ALL_PACKAGES & Transition to Permanent Component Naming
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Adhering to Google Play package visibility policies and removing temporary development phase numbers from production component names.
* **Chosen Approach:**
  - Remove `QUERY_ALL_PACKAGES` permission from `AndroidManifest.xml`. Discovery relies exclusively on `<queries>` launcher intent declaration and standard `LauncherApps` APIs.
  - Adopt responsibility-based permanent names (e.g. `AppCatalogScreen`, `LauncherDiagnosticsScreen`, `FoundationOverviewScreen`) while preserving historical phase logs in `/docs/`.
* **Reason:** Follows platform security and policy best practices, ensuring clean production architecture without development artifacts.

---

### DECISION-020: Per-Space Presentation Projection & Persisted Ordering
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to derive the applications displayed on the launcher Home screen from active Space membership while preserving deterministic user ordering and avoiding global catalog leakage.
* **Chosen Approach:**
  - The Home presentation is dynamically derived by projecting the active Space's persisted Room memberships (queried in `order_index ASC, added_at ASC`) against the live `LauncherApps` catalog in `AppDiscoveryViewModel`.
  - Application matching prioritizes explicit component key `(packageName/activityName)` with fallback to `packageName`.
  - The resulting presentation order strictly mirrors the stored database order rather than imposing alphabetical or arbitrary sorting.
* **Reason:** Enforces strict boundary between Android platform authority (what apps exist and can launch) and Space domain ownership (which apps belong in which Space and in what order).

---

### DECISION-022: Local Space PIN Cryptography with PBKDF2 and Per-Space Salt
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to store and verify numeric Space PINs securely on device without risk of plaintext exposure or timing attacks.
* **Chosen Approach:**
  - Implement `PinSecurityManager` using standard `PBKDF2WithHmacSHA256` key derivation.
  - Generate a distinct 16-byte cryptographically secure random salt (`SecureRandom`) for every protected Space.
  - Compute 256-bit hashes using 10,000 iterations and verify incoming attempts using constant-time `MessageDigest.isEqual`.
  - Store `auth_policy = "PIN"`, `pin_salt`, and `pin_hash` in Room `spaces` table. Plaintext PINs are never stored, transmitted, or logged.
* **Reason:** Ensures strong offline cryptographic protection resistant to dictionary and timing attacks, while remaining lightweight and self-contained without external services.

---

### DECISION-023: Transient In-Memory Unlock Session State & Presentation Gating
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** How to manage unlocked space sessions at runtime, gate Home presentation, and prevent cross-space leakage during space switching.
* **Chosen Approach:**
  - `SpaceViewModel` tracks unlocked state via a transient in-memory `StateFlow<Set<String>>` (`unlockedSpaceIds`).
  - If a Space is protected and not in `unlockedSpaceIds`:
    - `LauncherHomeScreen` returns an empty application projection list, completely concealing assigned applications.
    - A locked UI placeholder with lock badge and PIN prompt is rendered.
    - Attempting to switch to a protected Space via the dropdown or configuration intercepts the action with `SpaceUnlockDialog`.
  - Unlocked states reset automatically upon process restart or explicit lock.
* **Reason:** Prevents any transient exposure of protected applications in memory or UI without requiring complex background daemon persistence.

---

### DECISION-024: Space Customization, Wallpapers, and In-Space App Ordering
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Enabling user-defined aesthetics, wallpaper backgrounds, flexible grid layouts (3-6 columns), icon sizes, label visibility, and in-space app ordering without destabilizing performance or database integrity.
* **Chosen Approach:**
  - Persist visual customization fields (`background_type`, `background_color`, `background_image_uri`, `grid_columns`, `icon_size`, `label_visibility`) in Room `spaces` table with safe fallback bounds.
  - Implement system Photo Picker integration with `takePersistableUriPermission` and Coil `AsyncImage` with a dynamic contrast scrim overlay.
  - Support in-space reordering via interactive move controls and A-Z sorting, updating `order_index` in `space_memberships`.
* **Reason:** Gives users full control over Space personality and visual density while ensuring fast, deterministic rendering directly from SQLite.

---

### DECISION-025: Lifecycle, Package Change, and Recovery Hardening
* **Date:** 2026-08-29
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Ensuring the launcher survives realistic Android lifecycle changes, process recreation, Activity recreation, device reboots, and dynamic app package modifications without state corruption or crash loops.
* **Chosen Approach:**
  - `RoomSpaceRepository` implements self-healing active Space resolution (`ensureDefaultSpaceInitialized` creates default Space if DB is empty; heals invalid active Space pointers to the first valid Space in SQLite).
  - `MainActivity` invokes silent catalog and state refresh on `onStart` and `onResume`.
  - Dual package monitoring via `LauncherApps.Callback` and `BroadcastReceiver` (`ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_REPLACED`, `ACTION_PACKAGE_CHANGED`).
  - Stale/uninstalled memberships are gracefully filtered on the Home view without deleting durable database rows in Room, allowing apps to automatically restore in their exact custom sequence upon reinstallation.
  - Transient PIN unlock state resets upon process recreation or reboot, ensuring protected Spaces remain locked.
* **Reason:** Satisfies the core launcher mandate: deterministic state reconstruction and zero crash risk under any Android lifecycle or package modification scenario.

---

### DECISION-026: Separation of Home Launcher Task and Configuration Entry Point
* **Date:** 2026-09-01
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** When registered as the default Android HOME launcher, clicking the app icon from an external launcher or app drawer should open the Space Configuration screen, while pressing the device Home button or selecting the app in Android Recents/Overview should navigate to the active Home launcher surface. Previously, mixing both intents in a single activity or task caused Android Overview / Recents preview to display the Configuration UI rather than the Home surface.
* **Chosen Approach:**
  - `ConfigurationActivity` is registered as the standalone `CATEGORY_LAUNCHER` entry point with `android:taskAffinity="com.multispace.configuration"` and `android:launchMode="singleTask"`.
  - `MainActivity` is registered as the dedicated `CATEGORY_HOME` entry point with `android:launchMode="singleTask"` and `android:stateNotNeeded="true"`.
  - When navigating between `MainActivity` and `ConfigurationActivity`, `Intent.FLAG_ACTIVITY_NEW_TASK` is used to maintain strict task separation.
  - The Android native Recents/Overview button accurately previews the active Space Home surface under the Home task, while the Configuration activity resides cleanly in its own management task.
* **Reason:** Aligns with standard Android Home launcher OS architecture, satisfying strict task separation and clean user experience across native Recents/Overview and app drawer launches.

---

### DECISION-028: Seamless Continuous Cross-Page Dragging with Edge Auto-Transition and Dynamic Page Extension
* **Date:** 2026-09-02
* **Type:** `DECISION`
* **Status:** `ACTIVE`
* **Problem:** Launcher users need to organize apps across multiple pages fluidly. A drag operation must not be interrupted when transitioning pages, moving past the last page must dynamically create new pages, the floating icon must remain coordinate-stable across page animations, and the user must be able to continue dragging across multiple pages in a single uninterrupted gesture.
* **Chosen Approach:**
  - Track pointer position continuously in root coordinates (`currentPointerPos`), rendering the floating dragged item in a root-level overlay independent of page scrolling.
  - Disable horizontal pager gestures while dragging (`userScrollEnabled = !isDragging`) to avoid gesture contention.
  - Implement edge detection zones (~80dp / max 22% viewport width) with a 300ms dwell timer and haptic feedback.
  - Support continuous multi-page dragging: upon page transition completion, if the finger remains in the edge zone, automatically schedule the next transition.
  - Right edge dwell on the last page dynamically increments `extraPagesCount`, animating to the newly created page immediately.
  - Persist final placement in Room SQLite only on drop (`handleEndDrag`), with `moveAppToPage` re-indexing both source and target pages to avoid gaps.
* **Reason:** Delivers a native, fluid Android launcher experience with zero gesture fragmentation, seamless page transitions, and rock-solid persistence integrity.










