package com.multispace.platform

import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.Space
import com.multispace.domain.security.AuthenticatedSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Application-scoped holder for transient launcher authentication, session expiration,
 * and rate-limiting / brute-force throttling state.
 *
 * Shared across MainActivity, ConfigurationActivity, and ViewModels.
 */
class LauncherSessionManager(
  private val context: android.content.Context? = null,
  private val timeProvider: () -> Long = {
    try {
      android.os.SystemClock.elapsedRealtime()
    } catch (_: Throwable) {
      System.currentTimeMillis()
    }
  }
) {

  companion object {
    const val DEFAULT_SESSION_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    const val THROTTLE_ATTEMPT_THRESHOLD = 5
    private const val PREFS_NAME = "multispace_session_lockout"
    private const val PREF_PREFIX_ATTEMPTS = "attempts_"
    private const val PREF_PREFIX_LOCKOUT_MS = "lockout_ms_"
  }

  // Launcher lock starts locked to prevent unauthorized access at launch
  private val _isLauncherLocked = MutableStateFlow(true)
  val isLauncherLocked: StateFlow<Boolean> = _isLauncherLocked.asStateFlow()

  @Deprecated("Renamed to isLauncherLocked to reflect launcher-level scope", ReplaceWith("isLauncherLocked"))
  val isPhoneLocked: StateFlow<Boolean> get() = isLauncherLocked

  // Active authenticated sessions per Space ID mapped to expiration time
  private val _activeSessions = MutableStateFlow<Map<String, AuthenticatedSession>>(emptyMap())
  private val _unlockedSpaceIds = MutableStateFlow<Set<String>>(emptySet())
  val unlockedSpaceIds: StateFlow<Set<String>> = _unlockedSpaceIds.asStateFlow()

  // Brute-force rate limiting per Space ID
  private val failedAttempts = mutableMapOf<String, Int>()
  private val lockoutUntilWallClockMs = mutableMapOf<String, Long>()

  private val prefs by lazy {
    context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
  }

  init {
    loadPersistedLockoutState()
  }

  @Synchronized
  private fun loadPersistedLockoutState() {
    val sp = prefs ?: return
    try {
      val now = System.currentTimeMillis()
      sp.all.forEach { (key, value) ->
        if (key.startsWith(PREF_PREFIX_ATTEMPTS) && value is Int) {
          val spaceId = key.removePrefix(PREF_PREFIX_ATTEMPTS)
          failedAttempts[spaceId] = value
        } else if (key.startsWith(PREF_PREFIX_LOCKOUT_MS) && value is Long) {
          val spaceId = key.removePrefix(PREF_PREFIX_LOCKOUT_MS)
          if (value > now) {
            lockoutUntilWallClockMs[spaceId] = value
          }
        }
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.AUTH, "Failed loading persisted lockout state", e)
    }
  }

  /**
   * Checks whether authentication for this space is currently throttled due to repeated failures.
   * Returns remaining cooldown in seconds, or null if not throttled.
   */
  @Synchronized
  fun getRemainingCooldownSeconds(spaceId: String): Long? {
    val now = System.currentTimeMillis()
    val lockedUntil = lockoutUntilWallClockMs[spaceId] ?: return null
    val remainingMs = lockedUntil - now
    return if (remainingMs > 0) {
      (remainingMs + 999) / 1000
    } else {
      lockoutUntilWallClockMs.remove(spaceId)
      prefs?.edit()?.remove(PREF_PREFIX_LOCKOUT_MS + spaceId)?.apply()
      null
    }
  }

  @Synchronized
  fun isLockedOut(spaceId: String): Boolean {
    val remaining = getRemainingCooldownSeconds(spaceId)
    return remaining != null && remaining > 0
  }

  /**
   * Records a failed authentication attempt and calculates backoff cooldown if threshold reached.
   * Returns remaining cooldown in seconds if throttled.
   */
  @Synchronized
  fun recordFailedAttempt(spaceId: String): Long? {
    val count = (failedAttempts[spaceId] ?: 0) + 1
    failedAttempts[spaceId] = count
    prefs?.edit()?.putInt(PREF_PREFIX_ATTEMPTS + spaceId, count)?.apply()

    val cooldownSeconds: Long = when {
      count >= 10 -> 300L // 5 min lockout
      count >= 8 -> 60L   // 1 min lockout
      count >= 6 -> 30L   // 30s lockout
      count >= 5 -> 10L   // 10s lockout
      else -> 0L
    }

    return if (cooldownSeconds > 0) {
      val now = System.currentTimeMillis()
      val lockoutEnd = now + (cooldownSeconds * 1000L)
      lockoutUntilWallClockMs[spaceId] = lockoutEnd
      prefs?.edit()?.putLong(PREF_PREFIX_LOCKOUT_MS + spaceId, lockoutEnd)?.apply()
      AppLogger.w(AppLogger.Category.AUTH, "Space ($spaceId) throttled for ${cooldownSeconds}s after $count failed attempts")
      cooldownSeconds
    } else {
      null
    }
  }

  /**
   * Clears failure count and lockout after successful authentication.
   */
  @Synchronized
  fun recordSuccessfulAttempt(spaceId: String) {
    failedAttempts.remove(spaceId)
    lockoutUntilWallClockMs.remove(spaceId)
    prefs?.edit()
      ?.remove(PREF_PREFIX_ATTEMPTS + spaceId)
      ?.remove(PREF_PREFIX_LOCKOUT_MS + spaceId)
      ?.apply()
  }

  /**
   * Checks whether a space is currently unlocked and has not expired.
   */
  fun isSpaceUnlocked(space: Space?): Boolean {
    if (space == null) return false
    if (_isLauncherLocked.value) return false
    if (!space.isProtected) return true
    return isSpaceUnlocked(space.id)
  }

  fun isSpaceUnlocked(spaceId: String): Boolean {
    if (_isLauncherLocked.value) return false
    val now = timeProvider()
    val session = _activeSessions.value[spaceId] ?: return false
    if (session.isExpired(now)) {
      lockSpace(spaceId)
      return false
    }
    return true
  }

  /**
   * Unlocks a space for the duration of the session.
   */
  fun unlockSpace(spaceId: String, durationMs: Long = DEFAULT_SESSION_TIMEOUT_MS) {
    val now = timeProvider()
    val session = AuthenticatedSession(
      spaceId = spaceId,
      authenticatedAtElapsedMs = now,
      expiresAtElapsedMs = now + durationMs
    )
    _activeSessions.update { it + (spaceId to session) }
    _unlockedSpaceIds.update { it + spaceId }
    recordSuccessfulAttempt(spaceId)
    AppLogger.d(AppLogger.Category.AUTH, "LauncherSessionManager: Space '$spaceId' unlocked until ${session.expiresAtElapsedMs}")
  }

  fun lockSpace(spaceId: String) {
    _activeSessions.update { it - spaceId }
    _unlockedSpaceIds.update { it - spaceId }
    AppLogger.d(AppLogger.Category.AUTH, "LauncherSessionManager: Space '$spaceId' locked")
  }

  fun lockAllProtectedSpaces() {
    _activeSessions.value = emptyMap()
    _unlockedSpaceIds.value = emptySet()
    AppLogger.d(AppLogger.Category.AUTH, "LauncherSessionManager: All protected spaces locked")
  }

  fun lockLauncher() {
    _isLauncherLocked.value = true
    lockAllProtectedSpaces()
    AppLogger.i(AppLogger.Category.AUTH, "Launcher locked (session reset)")
  }

  @Deprecated("Renamed to lockLauncher()", ReplaceWith("lockLauncher()"))
  fun lockPhone() = lockLauncher()

  fun unlockLauncher() {
    _isLauncherLocked.value = false
    AppLogger.i(AppLogger.Category.AUTH, "Launcher unlocked")
  }

  @Deprecated("Renamed to unlockLauncher()", ReplaceWith("unlockLauncher()"))
  fun unlockPhone() = unlockLauncher()

  // Explicit re-authorization grants for sensitive operations (e.g. delete space, change security)
  private val explicitAuthGrants = mutableMapOf<String, Long>()

  @Synchronized
  fun grantExplicitAuthorization(spaceId: String, durationMs: Long = 60_000L) {
    explicitAuthGrants[spaceId] = System.currentTimeMillis() + durationMs
    AppLogger.d(AppLogger.Category.AUTH, "Granted explicit authorization for Space '$spaceId' for ${durationMs}ms")
  }

  @Synchronized
  fun hasExplicitAuthorization(spaceId: String): Boolean {
    val expiry = explicitAuthGrants[spaceId] ?: return false
    val now = System.currentTimeMillis()
    return if (now <= expiry) {
      true
    } else {
      explicitAuthGrants.remove(spaceId)
      false
    }
  }

  @Synchronized
  fun consumeExplicitAuthorization(spaceId: String): Boolean {
    val hasAuth = hasExplicitAuthorization(spaceId)
    if (hasAuth) {
      explicitAuthGrants.remove(spaceId)
    }
    return hasAuth
  }
}
