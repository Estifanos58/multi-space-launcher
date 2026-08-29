package com.multispace.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp

/**
 * Result of an application launch attempt.
 */
sealed class LaunchResult {
  data class Success(
    val packageName: String,
    val activityName: String,
    val method: String,
    val timestamp: Long = System.currentTimeMillis()
  ) : LaunchResult()

  data class Unavailable(
    val packageName: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
  ) : LaunchResult()

  data class Failed(
    val packageName: String,
    val errorMessage: String,
    val exception: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
  ) : LaunchResult()
}

/**
 * Platform integration manager responsible for launcher-aware application launching,
 * launch-time component verification, profile resolution, and graceful failure handling.
 */
class AppLaunchManager(private val context: Context) {

  private val launcherApps: LauncherApps? =
    context.getSystemService(LauncherApps::class.java)

  private val userManager: UserManager? =
    context.getSystemService(UserManager::class.java)

  private val packageManager: PackageManager = context.packageManager

  /**
   * Resolves the UserHandle corresponding to the discovered app's user profile.
   */
  private fun resolveUserHandle(userHandleId: Long): UserHandle {
    val profiles = userManager?.userProfiles ?: emptyList()
    for (profile in profiles) {
      if (profile.hashCode().toLong() == userHandleId) {
        return profile
      }
    }
    return Process.myUserHandle()
  }

  /**
   * Attempts to launch an application using its discovered launcher identity.
   *
   * Flow:
   * 1. Resolve UserHandle.
   * 2. Verify current component availability via LauncherApps at launch time.
   * 3. Launch via LauncherApps.startMainActivity if available.
   * 4. If component is stale but package is present, attempt controlled recovery via PackageManager fallback.
   * 5. If application is uninstalled/disabled or launch fails, handle gracefully without crashing.
   */
  fun launchApp(app: DiscoveredApp, sourceBounds: Rect? = null): LaunchResult {
    val targetComponent = ComponentName(app.packageName, app.activityName)
    val userHandle = resolveUserHandle(app.userHandleId)

    AppLogger.i(
      AppLogger.Category.LAUNCH,
      "LAUNCH_REQUESTED: ${app.label} [${app.packageName}/${app.activityName}] (profile: $userHandle)"
    )

    AppLogger.d(
      AppLogger.Category.LAUNCH,
      "LAUNCH_RESOLUTION_STARTED: Verifying current availability for ${app.packageName}"
    )

    // Step 1: Launch-time resolution against current Android LauncherApps state
    if (launcherApps != null) {
      try {
        val activities: List<LauncherActivityInfo>? =
          launcherApps.getActivityList(app.packageName, userHandle)

        val matchingActivity = activities?.firstOrNull {
          it.componentName.className == app.activityName
        }

        if (matchingActivity != null) {
          // Direct component verified
          AppLogger.i(
            AppLogger.Category.LAUNCH,
            "LAUNCH_RESOLUTION_SUCCESS: Component verified: ${matchingActivity.componentName.flattenToShortString()}"
          )
          AppLogger.d(
            AppLogger.Category.LAUNCH,
            "LAUNCH_ATTEMPTED: Invoking LauncherApps.startMainActivity"
          )

          launcherApps.startMainActivity(
            matchingActivity.componentName,
            userHandle,
            sourceBounds,
            null
          )

          AppLogger.i(
            AppLogger.Category.LAUNCH,
            "LAUNCH_SUCCESS: ${app.label} launched successfully via LauncherApps"
          )
          return LaunchResult.Success(
            packageName = app.packageName,
            activityName = matchingActivity.componentName.className,
            method = "LauncherApps.startMainActivity"
          )
        } else if (!activities.isNullOrEmpty()) {
          // Stale component name, but alternative launcher activity exists in package
          val fallbackActivity = activities.first()
          AppLogger.w(
            AppLogger.Category.LAUNCH,
            "LAUNCH_FALLBACK_USED: Stale activity '${app.activityName}', resolving to '${fallbackActivity.componentName.className}'"
          )
          AppLogger.d(
            AppLogger.Category.LAUNCH,
            "LAUNCH_ATTEMPTED: Invoking LauncherApps.startMainActivity for fallback activity"
          )

          launcherApps.startMainActivity(
            fallbackActivity.componentName,
            userHandle,
            sourceBounds,
            null
          )

          AppLogger.i(
            AppLogger.Category.LAUNCH,
            "LAUNCH_SUCCESS: ${app.label} launched via fallback activity ${fallbackActivity.componentName.flattenToShortString()}"
          )
          return LaunchResult.Success(
            packageName = app.packageName,
            activityName = fallbackActivity.componentName.className,
            method = "LauncherApps.startMainActivity (Resolved Fallback)"
          )
        }
      } catch (e: SecurityException) {
        AppLogger.e(AppLogger.Category.LAUNCH, "LAUNCH_FAILED: SecurityException launching ${app.packageName}", e)
        return LaunchResult.Failed(
          packageName = app.packageName,
          errorMessage = "Permission denied while launching application.",
          exception = e
        )
      } catch (e: Exception) {
        AppLogger.w(
          AppLogger.Category.LAUNCH,
          "LAUNCH_FAILED: LauncherApps invocation failed, attempting PackageManager fallback",
          e
        )
      }
    }

    // Step 2: Fallback to PackageManager launch intent if LauncherApps failed or component was not found
    try {
      val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
      if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (sourceBounds != null) {
          launchIntent.sourceBounds = sourceBounds
        }
        AppLogger.w(
          AppLogger.Category.LAUNCH,
          "LAUNCH_FALLBACK_USED: Launching via PackageManager.getLaunchIntentForPackage for ${app.packageName}"
        )
        context.startActivity(launchIntent)
        AppLogger.i(
          AppLogger.Category.LAUNCH,
          "LAUNCH_SUCCESS: ${app.label} launched successfully via PackageManager fallback"
        )
        return LaunchResult.Success(
          packageName = app.packageName,
          activityName = launchIntent.component?.className ?: app.activityName,
          method = "PackageManager.getLaunchIntentForPackage"
        )
      }
    } catch (e: Exception) {
      AppLogger.e(
        AppLogger.Category.LAUNCH,
        "LAUNCH_FAILED: PackageManager fallback launch failed for ${app.packageName}",
        e
      )
      return LaunchResult.Failed(
        packageName = app.packageName,
        errorMessage = "Failed to launch application: ${e.localizedMessage ?: "Unknown error"}",
        exception = e
      )
    }

    // Step 3: Application is uninstalled, disabled, or no launchable activity was found
    AppLogger.w(
      AppLogger.Category.LAUNCH,
      "LAUNCH_UNAVAILABLE: Application ${app.packageName} is unavailable or disabled"
    )
    return LaunchResult.Unavailable(
      packageName = app.packageName,
      reason = "Application is uninstalled, disabled, or no launchable activity was found."
    )
  }
}
