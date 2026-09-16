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
import com.multispace.data.repository.RoomLaunchHistoryRepository
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.LaunchHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
class AppLaunchManager(
  private val context: Context,
  val historyRepository: LaunchHistoryRepository = RoomLaunchHistoryRepository.getInstance(context),
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val coroutineScope: CoroutineScope? = null
) {

  private val launcherApps: LauncherApps? =
    context.getSystemService(LauncherApps::class.java)

  private val userManager: UserManager? =
    context.getSystemService(UserManager::class.java)

  private val packageManager: PackageManager = context.packageManager

  suspend fun recordSuccessfulLaunch(app: DiscoveredApp, spaceId: String) {
    try {
      historyRepository.recordLaunch(spaceId, app.appIdentity)
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.LAUNCH, "Failed to record launch history for ${app.label} in space $spaceId", e)
    }
  }

  private fun dispatchRecordLaunch(app: DiscoveredApp, spaceId: String, callerScope: CoroutineScope?) {
    val targetScope = callerScope ?: coroutineScope
    if (targetScope != null) {
      targetScope.launch(ioDispatcher) {
        recordSuccessfulLaunch(app, spaceId)
      }
    } else {
      CoroutineScope(Dispatchers.IO).launch {
        recordSuccessfulLaunch(app, spaceId)
      }
    }
  }

  /**
   * Resolves the UserHandle corresponding to the discovered app's user profile.
   */
  private fun resolveUserHandle(userHandleId: Long): UserHandle {
    return UserHandleHelper.resolveUserHandle(userManager, userHandleId)
  }

  /**
   * Attempts to launch an application using its discovered launcher identity within [spaceId].
   *
   * Flow:
   * 1. Resolve UserHandle.
   * 2. Verify current component availability via LauncherApps at launch time.
   * 3. Launch via LauncherApps.startMainActivity if available.
   * 4. If component is stale but package is present, attempt controlled recovery via PackageManager fallback.
   * 5. If application is uninstalled/disabled or launch fails, handle gracefully without crashing.
   */
  fun launchApp(
    app: DiscoveredApp,
    spaceId: String = Space.DEFAULT_SPACE_ID,
    sourceBounds: Rect? = null,
    callerScope: CoroutineScope? = null
  ): LaunchResult {
    val identity = app.appIdentity
    val targetComponent = identity.toComponentName()
    val userHandle = resolveUserHandle(identity.userHandleId)

    AppLogger.i(
      AppLogger.Category.LAUNCH,
      "LAUNCH_REQUESTED: ${app.label} [$identity] in space '$spaceId' (profile: $userHandle)"
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
          it.componentName == targetComponent || it.componentName.className == identity.componentName
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
          dispatchRecordLaunch(app, spaceId, callerScope)
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

          val launchedFallbackApp = app.copy(
            activityName = fallbackActivity.componentName.className
          )
          AppLogger.i(
            AppLogger.Category.LAUNCH,
            "LAUNCH_SUCCESS: ${app.label} launched via fallback activity ${fallbackActivity.componentName.flattenToShortString()}"
          )
          dispatchRecordLaunch(launchedFallbackApp, spaceId, callerScope)
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
        val launchedPkgApp = app.copy(
          activityName = launchIntent.component?.className ?: app.activityName
        )
        AppLogger.i(
          AppLogger.Category.LAUNCH,
          "LAUNCH_SUCCESS: ${app.label} launched successfully via PackageManager fallback"
        )
        dispatchRecordLaunch(launchedPkgApp, spaceId, callerScope)
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

  suspend fun launchAppSuspending(
    app: DiscoveredApp,
    spaceId: String = Space.DEFAULT_SPACE_ID,
    sourceBounds: Rect? = null
  ): LaunchResult {
    val result = launchApp(app, spaceId, sourceBounds, callerScope = null)
    if (result is LaunchResult.Success) {
      withContext(ioDispatcher) {
        recordSuccessfulLaunch(app, spaceId)
      }
    }
    return result
  }
}
