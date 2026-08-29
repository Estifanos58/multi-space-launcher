# Current Task — Lifecycle, Package Changes, and Recovery Hardening

## Task Overview
* **Task Name:** Lifecycle, Package Changes, and Recovery Hardening
* **Target Objective:** Ensure the integrated multi-space launcher remains completely resilient and reconstructs correct state after process recreation, Activity recreation, launcher restart, device reboot, application installation/uninstallation/disablement, stale application references, and missing/malformed persisted state.
* **Status:** `IMPLEMENTED (BUILD VERIFIED, READY FOR PHYSICAL VERIFICATION)`

---

## Hardening Matrix & Architecture

```text
Android Event / Lifecycle Change
        ↓
Process / Activity Recreation / Reboot
        ↓
Room Database + DataStore (Durable State)
        ↓
Self-Healing Active Space Resolution (Fallback to default if invalid/empty)
        ↓
Persistent Space Customizations & PIN Hash
        ↓
Dynamic Application Discovery & Package Change Reconciliation (LauncherApps + BroadcastReceiver)
        ↓
Space Membership Projection (Omit unavailable/uninstalled; preserve durable identity and order)
        ↓
Derived High-Contrast Launcher Home Presentation
```

---

## Hardening Implementations

1. **Activity & Process Lifecycle Hardening (`MainActivity.kt`)**:
   - `onCreate`, `onStart`, and `onResume` lifecycle handlers invoke `spaceViewModel.ensureDefaultSpaceInitialized()` and `discoveryViewModel.loadApps(isSilent = true)`.
   - Distinguishes system `Intent.CATEGORY_HOME` vs standard app launcher entry points, maintaining strict Home / Configuration surface separation across lifecycle events.
   - Preserves state across configuration changes, screen rotations, and low-memory Activity recreation.

2. **Dual-Layer Package Change Monitoring (`AppDiscoveryManager.kt`)**:
   - Registered `LauncherApps.Callback` on Main Looper for `onPackageAdded`, `onPackageRemoved`, `onPackageChanged`, `onPackagesAvailable`, and `onPackagesUnavailable`.
   - Added secondary `BroadcastReceiver` fallback listening for `ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_REPLACED`, `ACTION_PACKAGE_CHANGED` with `package` data scheme.
   - Automatic LRU icon cache eviction for modified/removed packages.
   - Dynamic reconciliation against persisted Room memberships without rewriting or dropping durable user configuration.

3. **Stale Membership & Unavailable Application Handling (`LauncherHomeScreen.kt`)**:
   - Projects active Space memberships against dynamic `LauncherApps` catalog.
   - Gracefully omits uninstalled or disabled applications from Home screen without modifying or deleting their stored Room memberships.
   - When an application is reinstalled, its matching component/package automatically reappears on the active Space with its original custom sequence (`order_index`).

4. **Self-Healing Active Space & State Recovery (`RoomSpaceRepository.kt`)**:
   - `ensureDefaultSpaceInitialized()` checks for zero-space or invalid active Space states.
   - If no Spaces exist, initializes `default_space` ("Default") and persists active pointer.
   - If DataStore points to a non-existent or deleted Space ID, automatically falls back to the first valid Space in SQLite and updates the DataStore pointer.
   - Customization fields (grid columns 3-6, icon size, label visibility, background type/color/URI) have defensive fallbacks against malformed values.

5. **Security & PIN Protection Lifecycle**:
   - PBKDF2 salt/hash credentials reside securely in SQLite.
   - Transient unlock session state (`unlockedSpaceIds`) is in-memory only in `SpaceViewModel`.
   - Process recreation or device reboot safely resets unlocked state, guaranteeing protected Spaces require PIN authentication on fresh start.

---

## Acceptance Criteria
- [x] Activity recreation and configuration changes preserve active Space and Home state.
- [x] Process restart and device reboot reconstruct state cleanly from Room and DataStore.
- [x] Package install/uninstall/update events refresh catalog and reconcile Space memberships.
- [x] Uninstalled apps disappear from Home while preserving durable membership records for reinstall recovery.
- [x] Invalid active Space reference automatically heals to a valid Space.
- [x] Protected Spaces require PIN verification after process recreation or reboot.
- [x] Zero automated test code added (strict physical verification policy).
- [x] Clean compilation via `compile_applet`.
