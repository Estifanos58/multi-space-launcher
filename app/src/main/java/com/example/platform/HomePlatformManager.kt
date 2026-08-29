package com.example.platform

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.diagnostics.AppLogger

/**
 * Android platform integration adapter for default Home role eligibility and verification.
 * Adheres to Phase 1 — Launcher Viability Spike requirements.
 */
object HomePlatformManager {

  enum class HomeRoleState {
    DEFAULT_HOME,
    NOT_DEFAULT_HOME,
    UNKNOWN
  }

  fun checkHomeStatus(context: Context): HomeRoleState {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
          val isHeld = roleManager.isRoleHeld(RoleManager.ROLE_HOME)
          AppLogger.d(AppLogger.Category.LAUNCHER, "RoleManager isRoleHeld(ROLE_HOME): $isHeld")
          if (isHeld) HomeRoleState.DEFAULT_HOME else HomeRoleState.NOT_DEFAULT_HOME
        } else {
          checkDefaultHomeViaPackageManager(context)
        }
      } else {
        checkDefaultHomeViaPackageManager(context)
      }
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to check Home role status", e)
      HomeRoleState.UNKNOWN
    }
  }

  private fun checkDefaultHomeViaPackageManager(context: Context): HomeRoleState {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    val currentPackage = resolveInfo?.activityInfo?.packageName
    AppLogger.d(AppLogger.Category.LAUNCHER, "PackageManager resolved default HOME package: $currentPackage")
    return if (currentPackage == context.packageName) {
      HomeRoleState.DEFAULT_HOME
    } else {
      HomeRoleState.NOT_DEFAULT_HOME
    }
  }

  fun createRequestDefaultHomeIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = context.getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        AppLogger.i(AppLogger.Category.LAUNCHER, "Creating RoleManager request intent for ROLE_HOME")
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
      }
    }
    AppLogger.i(AppLogger.Category.LAUNCHER, "Creating Settings fallback intent for Home role selection")
    return Intent(Settings.ACTION_HOME_SETTINGS)
  }

  fun createTestExternalAppIntent(): Intent {
    AppLogger.i(AppLogger.Category.LAUNCH, "Creating external test app launch intent (Settings)")
    return Intent(Settings.ACTION_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }
}
