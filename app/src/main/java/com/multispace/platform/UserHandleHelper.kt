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
   * Converts an Android [UserHandle] to a canonical [Long] identity.
   * Prefers [UserManager.getSerialNumberForUser] when available.
   * Falls back to deterministic handle conversion if UserManager is unavailable.
   */
  fun getUserHandleId(context: Context?, userHandle: UserHandle): Long {
    if (context != null) {
      try {
        val userManager = context.getSystemService(UserManager::class.java)
        return getUserHandleId(userManager, userHandle)
      } catch (_: Throwable) {}
    }
    return userHandle.hashCode().toLong()
  }

  /**
   * Converts an Android [UserHandle] to a canonical [Long] identity using [UserManager].
   */
  fun getUserHandleId(userManager: UserManager?, userHandle: UserHandle): Long {
    if (userManager != null) {
      try {
        val serial = userManager.getSerialNumberForUser(userHandle)
        if (serial >= 0L) {
          return serial
        }
      } catch (_: Throwable) {}
    }
    return userHandle.hashCode().toLong()
  }

  /**
   * Resolves a [UserHandle] from a canonical [userHandleId].
   * Prefers [UserManager.getUserForSerialNumber], with fallback to profile scanning and myUserHandle.
   */
  fun resolveUserHandle(context: Context?, userHandleId: Long): UserHandle {
    if (context != null) {
      try {
        val userManager = context.getSystemService(UserManager::class.java)
        return resolveUserHandle(userManager, userHandleId)
      } catch (_: Throwable) {}
    }
    return Process.myUserHandle()
  }

  /**
   * Resolves a [UserHandle] from a canonical [userHandleId] using [UserManager].
   */
  fun resolveUserHandle(userManager: UserManager?, userHandleId: Long): UserHandle {
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
        // Also support legacy hashCode IDs for backwards compatibility
        for (profile in profiles) {
          if (profile.hashCode().toLong() == userHandleId) {
            return profile
          }
        }
      } catch (_: Throwable) {}
    }

    return Process.myUserHandle()
  }
}
