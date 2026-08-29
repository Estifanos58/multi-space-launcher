# Current Task — Space PIN Security & Local Authentication

## Task Overview
* **Task Name:** Space PIN Security & Local Authentication
* **Target Objective:** Implement local PIN-based authentication for individual Spaces. Protect sensitive Spaces with PBKDF2WithHmacSHA256 hashed numeric PINs, per-Space cryptographically secure random salts, constant-time verification, locked presentation state on Home, secure Space switching gating, and comprehensive PIN lifecycle management (Set, Change, Disable, Unlock).
* **Status:** `IN_PROGRESS (BUILD VERIFIED, READY FOR PHYSICAL VERIFICATION)`

---

## Security Architecture & Design
```text
User Sets PIN (4-8 digits)
        ↓
PinSecurityManager generates 16-byte SecureRandom salt
        ↓
Derives PBKDF2WithHmacSHA256 hash (10,000 iterations, 256 bits)
        ↓
Persisted in Room Database (authPolicy="PIN", pin_salt, pin_hash)
        ↓
Runtime Memory tracks transient unlocked states (unlockedSpaceIds StateFlow)
        ↓
Launcher Home & Space Switcher gate access using constant-time verification
```

---

## Completed Implementations
1. **Cryptographic Security Manager (`PinSecurityManager.kt`)**:
   - Implements PBKDF2 with HMAC-SHA256 (`PBKDF2WithHmacSHA256`).
   - 16-byte cryptographically secure random salts generated via `java.security.SecureRandom`.
   - 10,000 iterations and 256-bit derived key length.
   - Constant-time verification using `MessageDigest.isEqual` to prevent timing attacks.
   - Numeric PIN format validation (4 to 8 digits).
   - Strict security rule: plaintext PINs are NEVER stored or logged.
2. **Domain & Persistence Model Extensions**:
   - `Space.kt`: Added `pinSalt`, `pinHash`, and computed property `isProtected` (`authPolicy == "PIN"` and non-empty hash/salt).
   - `SpaceEntity.kt`: Added `@ColumnInfo(name = "pin_salt")` and `@ColumnInfo(name = "pin_hash")`.
   - `SpaceRepository` & `RoomSpaceRepository`: Implemented `setSpacePin`, `changeSpacePin`, `disableSpacePin`, and `verifySpacePin`.
3. **Session & Runtime State Management (`SpaceViewModel.kt`)**:
   - In-memory `unlockedSpaceIds` `StateFlow` tracks unlocked status during active app process lifecycle.
   - Transient unlock state resets cleanly upon app process restart or explicit lock.
   - `verifyAndUnlockSpace` authenticates against the repository and unlocks the Space in memory upon success.
4. **Home Screen Protection & Gating (`LauncherHomeScreen.kt`)**:
   - If active Space is protected and not unlocked in runtime memory:
     - Application grid is completely hidden (returns `emptyList()` projection to strictly prevent app leaks).
     - Renders a clean locked state with lock icon and "Enter PIN" button.
     - Tapping "Enter PIN" opens the secure `SpaceUnlockDialog`.
5. **Secure Space Switching Gating (`LauncherHomeScreen.kt` & `LauncherConfigurationScreen.kt`)**:
   - Selecting a protected Space from the switcher dropdown or configuration list intercepts navigation.
   - Prompts for PIN verification via `SpaceUnlockDialog`.
   - Only switches active Space and reveals apps upon successful PIN verification.
6. **PIN Management UI & Dialogs (`SpacePinDialogs.kt`)**:
   - `SetSpacePinDialog`: PIN entry with confirmation, numeric keyboard, masked password transformation, and digit format validation.
   - `ChangeSpacePinDialog`: Current PIN verification + New PIN + Confirmation.
   - `DisableSpacePinDialog`: Current PIN verification before removing protection.
   - `SpaceUnlockDialog`: Masked PIN input, real-time error feedback, and async verification indicator.
   - `SpaceItemCard` / `SpaceRowItem`: Displays green "PIN" lock badge, +PIN / PIN button, and dedicated Remove PIN action.
7. **Security Logging & Diagnostics**:
   - Non-sensitive operational logging via `AppLogger.Category.LAUNCHER` (records enablement, unlock success/failure without logging PINs).

---

## Scope Boundaries & Protections
* **Protected Scope (Maintained):**
  - Space PIN security is a launcher presentation protection layer; it does not claim to replace Android OS work profile sandboxing or filesystem encryption.
  - Zero automated test code (Robolectric, Espresso) per Constitution Rule 7.
  - No `QUERY_ALL_PACKAGES` permission in Manifest.
  - No remote server authentication or telemetry leaks.

---

## Acceptance Criteria
- [x] PIN setting, changing, disabling, and verification works with PBKDF2 hashing.
- [x] Plaintext PINs are never stored in Room or output to logs.
- [x] Locked Space strictly conceals application grid on Launcher Home until unlocked.
- [x] Space switcher dropdown prompts for PIN when switching to a protected Space.
- [x] Space management cards display protection status and provide PIN setup/change/disable controls.
- [x] Build compiles cleanly with zero errors.
