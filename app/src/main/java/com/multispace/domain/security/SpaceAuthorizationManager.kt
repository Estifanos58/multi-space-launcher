package com.multispace.domain.security

import com.multispace.domain.model.Space

/**
 * Enforces authorization policies for sensitive Space management operations.
 */
class SpaceAuthorizationManager(
  private val isSpaceUnlockedProvider: (String) -> Boolean,
  private val isLauncherLockedProvider: () -> Boolean
) {

  /**
   * Determines whether the given [operation] can be performed on the specified [space]
   * under current transient session and lock states.
   */
  fun canPerform(space: Space, operation: SensitiveOperation): Boolean {
    // If launcher itself is locked, no sensitive operations are permitted without unlocking launcher first
    if (isLauncherLockedProvider() && operation != SensitiveOperation.UNLOCK_SPACE) {
      return false
    }

    if (!space.isProtected) {
      return when (operation) {
        SensitiveOperation.DISABLE_AUTHENTICATION -> false // Cannot disable protection that isn't enabled
        else -> true
      }
    }

    // Protected Space rules
    val isSessionUnlocked = isSpaceUnlockedProvider(space.id)

    return when (operation) {
      SensitiveOperation.UNLOCK_SPACE -> true // Always allow attempting unlock
      SensitiveOperation.EDIT_SPACE -> isSessionUnlocked
      SensitiveOperation.MANAGE_MEMBERSHIPS -> isSessionUnlocked
      SensitiveOperation.DELETE_SPACE -> isSessionUnlocked
      SensitiveOperation.CHANGE_AUTHENTICATION -> isSessionUnlocked
      SensitiveOperation.DISABLE_AUTHENTICATION -> isSessionUnlocked
      SensitiveOperation.CHANGE_SECURITY_SETTINGS -> isSessionUnlocked
    }
  }

  /**
   * Checks whether the space requires re-authentication for this operation.
   */
  fun requiresExplicitAuthentication(space: Space, operation: SensitiveOperation): Boolean {
    if (!space.isProtected) return false
    return when (operation) {
      SensitiveOperation.DELETE_SPACE,
      SensitiveOperation.CHANGE_AUTHENTICATION,
      SensitiveOperation.DISABLE_AUTHENTICATION,
      SensitiveOperation.CHANGE_SECURITY_SETTINGS -> true
      SensitiveOperation.EDIT_SPACE,
      SensitiveOperation.MANAGE_MEMBERSHIPS -> !isSpaceUnlockedProvider(space.id)
      SensitiveOperation.UNLOCK_SPACE -> !isSpaceUnlockedProvider(space.id)
    }
  }
}
