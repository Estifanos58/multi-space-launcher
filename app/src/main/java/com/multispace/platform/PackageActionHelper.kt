package com.multispace.platform

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp

/**
 * Helper responsible for evaluating application package capabilities (e.g. whether
 * an app can be uninstalled) and executing direct package lifecycle actions such as
 * launching package uninstall confirmation and force-stopping applications.
 */
object PackageActionHelper {

  /**
   * Determines whether the package is uninstallable on Android before rendering the action menu.
   *
   * Checks runtime package manager flags:
   * - Standard user-installed apps (non-system) can be uninstalled.
   * - System apps that have been updated (FLAG_UPDATED_SYSTEM_APP) can be uninstalled (reverts to system image).
   * - System apps without updates cannot be uninstalled.
   * - DevicePolicyManager restrictions are respected (isUninstallBlocked).
   *
   * If PackageManager lookup fails (e.g. in synthetic or test environments), falls back to [DiscoveredApp.isUninstallable].
   */
  fun isPackageUninstallable(context: Context, app: DiscoveredApp): Boolean {
    return isPackageUninstallable(context, app.packageName, app.isUninstallable)
  }

  fun isPackageUninstallable(
    context: Context,
    packageName: String,
    fallbackUninstallable: Boolean = true
  ): Boolean {
    return try {
      val pm = context.packageManager
      val appInfo = pm.getApplicationInfo(packageName, 0)
      val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
      val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
      val isBlocked = try {
        dpm?.isUninstallBlocked(null, packageName) ?: false
      } catch (e: Exception) {
        false
      }

      if (isBlocked) {
        AppLogger.d(AppLogger.Category.LAUNCHER, "Package $packageName uninstall is blocked by DevicePolicy")
        return false
      }

      // Non-system apps or updated system apps can be uninstalled
      val canUninstall = !isSystem || isUpdatedSystem
      AppLogger.d(
        AppLogger.Category.LAUNCHER,
        "Package $packageName uninstallability: $canUninstall (isSystem=$isSystem, isUpdatedSystem=$isUpdatedSystem)"
      )
      canUninstall
    } catch (e: Exception) {
      // In testing or when package cannot be found in PM, fallback to the model's hint
      AppLogger.d(
        AppLogger.Category.LAUNCHER,
        "Package $packageName not found in PackageManager, using fallback: $fallbackUninstallable"
      )
      fallbackUninstallable
    }
  }

  /**
   * Creates direct package uninstall intent (ACTION_DELETE with package: URI).
   */
  fun createUninstallIntent(packageName: String): Intent {
    return Intent(Intent.ACTION_DELETE).apply {
      data = Uri.parse("package:$packageName")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  /**
   * Launches direct Android package uninstall confirmation using ACTION_DELETE.
   * Does NOT navigate to App Info / Application Details settings.
   */
  fun launchUninstallConfirmation(context: Context, packageName: String): Boolean {
    return try {
      val intent = createUninstallIntent(packageName)
      context.startActivity(intent)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Launched direct uninstall confirmation for $packageName")
      true
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to launch direct uninstall confirmation for $packageName", e)
      false
    }
  }

  /**
   * Directly force-stops that package without navigating to App Info settings.
   * Tries ActivityManager.forceStopPackage via reflection first (for privileged/system execution),
   * falling back to ActivityManager.killBackgroundProcesses. Handles failure gracefully.
   */
  fun forceStopPackage(context: Context, packageName: String): Boolean {
    return try {
      val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
      if (am == null) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "ActivityManager unavailable for force-stopping $packageName")
        return false
      }

      var stopped = false
      try {
        val forceStopMethod = am.javaClass.getMethod("forceStopPackage", String::class.java)
        forceStopMethod.isAccessible = true
        forceStopMethod.invoke(am, packageName)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Successfully invoked forceStopPackage for $packageName")
        stopped = true
      } catch (e: Exception) {
        AppLogger.d(AppLogger.Category.LAUNCHER, "forceStopPackage not directly accessible, falling back to killBackgroundProcesses for $packageName")
      }

      try {
        am.killBackgroundProcesses(packageName)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Invoked killBackgroundProcesses for $packageName")
        stopped = true
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Failed killBackgroundProcesses for $packageName", e)
      }

      stopped
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to force-stop package $packageName", e)
      false
    }
  }
}
