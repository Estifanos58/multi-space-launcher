package com.multispace.platform

import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/**
 * Standard Android platform boundary for converting between Android [UserHandle]
 * and the canonical [Long] identity (`userHandleId`).
 *
 * Uses Android's standard [UserManager.getSerialNumberForUser] and [UserManager.getUserForSerialNumber],
 * which provide stable serial numbers across process lifetimes, avoiding volatile JVM hash codes.
 */
object UserHandleHelper {

  /**
   * Converts an Android [UserHandle] to a canonical [Long] identity using [Context].
   * Returns null if Context or UserManager is unavailable, or fails to return a valid non-negative serial.
   */
  fun getUserHandleId(context: Context?, userHandle: UserHandle): Long? {
    if (context != null) {
      try {
        val userManager = context.getSystemService(UserManager::class.java)
        return getUserHandleId(userManager, userHandle)
      } catch (_: Throwable) {}
    }
    return null
  }

  /**
   * Converts an Android [UserHandle] to a canonical [Long] identity using [UserManager].
   * Returns null if [userManager] is null or [UserManager.getSerialNumberForUser] returns < 0.
   */
  fun getUserHandleId(userManager: UserManager?, userHandle: UserHandle): Long? {
    if (userManager != null) {
      try {
        val serial = userManager.getSerialNumberForUser(userHandle)
        if (serial >= 0L) {
          return serial
        }
      } catch (_: Throwable) {}
    }
    return null
  }

  /**
   * Resolves a [UserHandle] from a canonical [userHandleId].
   * Returns null if Context or UserManager is unavailable, or if the profile cannot be found.
   * Strictly never falls back to [Process.myUserHandle()] or user 0 for an unresolvable profile.
   */
  fun resolveUserHandle(context: Context?, userHandleId: Long): UserHandle? {
    if (context != null) {
      try {
        val userManager = context.getSystemService(UserManager::class.java)
        return resolveUserHandle(userManager, userHandleId)
      } catch (_: Throwable) {}
    }
    return null
  }

  /**
   * Resolves a [UserHandle] from a canonical [userHandleId] using [UserManager].
   * Returns null if [userManager] is null or if the profile cannot be found.
   */
  fun resolveUserHandle(userManager: UserManager?, userHandleId: Long): UserHandle? {
    if (userManager != null) {
      try {
        val handle = userManager.getUserForSerialNumber(userHandleId)
        if (handle != null) {
          return handle
        }
      } catch (_: Throwable) {}

      // Fallback: check all currently available user profiles by computed serial ID
      try {
        val profiles = userManager.userProfiles ?: emptyList()
        for (profile in profiles) {
          if (getUserHandleId(userManager, profile) == userHandleId) {
            return profile
          }
        }
      } catch (_: Throwable) {}
    }

    return null
  }
}
