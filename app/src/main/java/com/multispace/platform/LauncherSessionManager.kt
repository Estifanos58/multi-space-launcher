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
  private val lockoutUntilMs = mutableMapOf<String, Long>()

  /**
   * Checks whether authentication for this space is currently throttled due to repeated failures.
   * Returns remaining cooldown in seconds, or null if not throttled.
   */
  @Synchronized
  fun getRemainingCooldownSeconds(spaceId: String): Long? {
    val now = timeProvider()
    val lockedUntil = lockoutUntilMs[spaceId] ?: return null
    val remainingMs = lockedUntil - now
    return if (remainingMs > 0) {
      (remainingMs + 999) / 1000
    } else {
      lockoutUntilMs.remove(spaceId)
      null
    }
  }

  /**
   * Records a failed authentication attempt and calculates backoff cooldown if threshold reached.
   * Returns remaining cooldown in seconds if throttled.
   */
  @Synchronized
  fun recordFailedAttempt(spaceId: String): Long? {
    val count = (failedAttempts[spaceId] ?: 0) + 1
    failedAttempts[spaceId] = count
    val now = timeProvider()

    val cooldownSeconds: Long = when {
      count >= 8 -> 60L
      count == 7 -> 30L
      count == 6 -> 10L
      count == 5 -> 5L
      else -> 0L
    }

    return if (cooldownSeconds > 0) {
      val lockoutEnd = now + (cooldownSeconds * 1000L)
      lockoutUntilMs[spaceId] = lockoutEnd
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
    lockoutUntilMs.remove(spaceId)
  }

  /**
   * Checks whether a space is currently unlocked and has not expired.
   */
  fun isSpaceUnlocked(space: Space?): Boolean {
    if (space == null) return false
    if (!space.isProtected) return true
    return isSpaceUnlocked(space.id)
  }

  fun isSpaceUnlocked(spaceId: String): Boolean {
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
}
