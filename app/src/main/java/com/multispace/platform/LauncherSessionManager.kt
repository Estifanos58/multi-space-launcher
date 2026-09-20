package com.multispace.platform

import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.Space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Lightweight application-scoped holder for transient launcher authentication and lock state.
 *
 * Shared across MainActivity, ConfigurationActivity, and ViewModels to prevent
 * disjoint lock/session states when switching tasks.
 */
class LauncherSessionManager {

  private val _isLauncherLocked = MutableStateFlow(false)
  val isLauncherLocked: StateFlow<Boolean> = _isLauncherLocked.asStateFlow()

  @Deprecated("Renamed to isLauncherLocked to reflect launcher-level scope", ReplaceWith("isLauncherLocked"))
  val isPhoneLocked: StateFlow<Boolean> get() = isLauncherLocked

  private val _unlockedSpaceIds = MutableStateFlow<Set<String>>(emptySet())
  val unlockedSpaceIds: StateFlow<Set<String>> = _unlockedSpaceIds.asStateFlow()

  fun isSpaceUnlocked(space: Space?): Boolean {
    if (space == null) return false
    if (!space.isProtected) return true
    return _unlockedSpaceIds.value.contains(space.id)
  }

  fun isSpaceUnlocked(spaceId: String): Boolean {
    return _unlockedSpaceIds.value.contains(spaceId)
  }

  fun unlockSpace(spaceId: String) {
    _unlockedSpaceIds.update { it + spaceId }
    AppLogger.d(AppLogger.Category.AUTH, "LauncherSessionManager: Space '$spaceId' transiently unlocked for session")
  }

  fun lockSpace(spaceId: String) {
    _unlockedSpaceIds.update { it - spaceId }
    AppLogger.d(AppLogger.Category.AUTH, "LauncherSessionManager: Space '$spaceId' locked")
  }

  fun lockAllProtectedSpaces() {
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
