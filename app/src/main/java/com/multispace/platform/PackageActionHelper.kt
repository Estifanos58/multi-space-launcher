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

  sealed class ForceStopResult {
    data object PrivilegedSuccess : ForceStopResult()
    data object BackgroundProcessesKilled : ForceStopResult()
    data class Failure(val errorMessage: String) : ForceStopResult()

    val isSuccessOrHandled: Boolean
      get() = this is PrivilegedSuccess || this is BackgroundProcessesKilled
  }

  /**
   * Directly force-stops that package without navigating to App Info settings.
   * Legitimate Android mechanisms:
   * 1. Direct ActivityManager.forceStopPackage via reflection if running in a privileged/system/platform context.
   * 2. Legitimate public fallback: ActivityManager.killBackgroundProcesses.
   * Returns a [ForceStopResult] representing the outcome, without pretending unprivileged apps
   * can force-stop arbitrary packages on Android, and strictly never navigates to App Info.
   */
  fun forceStopPackage(context: Context, packageName: String): ForceStopResult {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
      ?: return ForceStopResult.Failure("ActivityManager is not available")

    // 1. Check for privileged / system access to direct forceStopPackage
    try {
      val forceStopMethod = am.javaClass.getMethod("forceStopPackage", String::class.java)
      forceStopMethod.isAccessible = true
      forceStopMethod.invoke(am, packageName)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Privileged forceStopPackage succeeded for $packageName")
      return ForceStopResult.PrivilegedSuccess
    } catch (e: SecurityException) {
      AppLogger.d(AppLogger.Category.LAUNCHER, "forceStopPackage requires privileged permission on standard Android for $packageName")
    } catch (e: Exception) {
      AppLogger.d(AppLogger.Category.LAUNCHER, "forceStopPackage not directly invokable for $packageName: ${e.message}")
    }

    // 2. Legitimate standard Android mechanism: killBackgroundProcesses
    return try {
      am.killBackgroundProcesses(packageName)
      AppLogger.i(AppLogger.Category.LAUNCHER, "killBackgroundProcesses invoked for $packageName (standard Android limitation: full force-stop requires privileged/system permissions)")
      ForceStopResult.BackgroundProcessesKilled
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to kill background processes for $packageName", e)
      ForceStopResult.Failure(e.localizedMessage ?: "Failed to stop package processes")
    }
  }
}
