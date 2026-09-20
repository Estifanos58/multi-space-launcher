package com.multispace.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
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
  private val spaceRepository: com.multispace.domain.repository.SpaceRepository? = null,
  private val applicationScope: CoroutineScope? = null,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val coroutineScope: CoroutineScope? = null
) {

  private val launcherApps: LauncherApps? =
    context.getSystemService(LauncherApps::class.java)

  private val userManager: UserManager? =
    context.getSystemService(UserManager::class.java)

  suspend fun recordSuccessfulLaunch(app: DiscoveredApp, spaceId: String) {
    try {
      historyRepository.recordLaunch(spaceId, app.appIdentity)
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.LAUNCH, "Failed to record launch history for ${app.label} in space $spaceId", e)
    }
  }

  /**
   * Resolves the UserHandle corresponding to the discovered app's user profile.
   * Returns null if the profile cannot be resolved or is no longer present.
   */
  private fun resolveUserHandle(userHandleId: Long): UserHandle? {
    return UserHandleHelper.resolveUserHandle(userManager, userHandleId)
  }

  /**
   * Executes the launch operation via LauncherApps strictly for the profile associated
   * with [app]'s AppIdentity. Never falls back to cross-profile or package-only resolution.
   */
  private fun performLaunch(
    app: DiscoveredApp,
    sourceBounds: Rect?
  ): Pair<LaunchResult, DiscoveredApp?> {
    val identity = app.appIdentity
    val targetComponent = identity.toComponentName()
    val userHandle = resolveUserHandle(identity.userHandleId)

    AppLogger.i(
      AppLogger.Category.LAUNCH,
      "LAUNCH_REQUESTED: ${app.label} [$identity] (profile: $userHandle)"
    )

    if (userHandle == null) {
      AppLogger.w(
        AppLogger.Category.LAUNCH,
        "LAUNCH_UNAVAILABLE: User profile ${identity.userHandleId} could not be resolved for ${app.packageName}"
      )
      return Pair(
        LaunchResult.Unavailable(
          packageName = app.packageName,
          reason = "User profile ${identity.userHandleId} is unavailable or uninstalled."
        ),
        null
      )
    }

    if (launcherApps == null) {
      AppLogger.w(AppLogger.Category.LAUNCH, "LAUNCH_UNAVAILABLE: LauncherApps service is unavailable")
      return Pair(
        LaunchResult.Unavailable(
          packageName = app.packageName,
          reason = "LauncherApps service is unavailable."
        ),
        null
      )
    }

    try {
      val activities: List<LauncherActivityInfo>? =
        launcherApps.getActivityList(app.packageName, userHandle)

      val matchingActivity = activities?.firstOrNull {
        it.componentName == targetComponent || it.componentName.className == identity.componentName
      }

      if (matchingActivity != null) {
        AppLogger.i(
          AppLogger.Category.LAUNCH,
          "LAUNCH_RESOLUTION_SUCCESS: Component verified: ${matchingActivity.componentName.flattenToShortString()}"
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
        return Pair(
          LaunchResult.Success(
            packageName = app.packageName,
            activityName = matchingActivity.componentName.className,
            method = "LauncherApps.startMainActivity"
          ),
          app
        )
      } else if (!activities.isNullOrEmpty()) {
        // Stale component name, but alternative launcher activity exists in package for THIS EXACT user profile
        val fallbackActivity = activities.first()
        AppLogger.w(
          AppLogger.Category.LAUNCH,
          "LAUNCH_FALLBACK_USED: Stale activity '${app.activityName}', resolving to '${fallbackActivity.componentName.className}' for profile ${identity.userHandleId}"
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
          "LAUNCH_SUCCESS: ${app.label} launched via profile fallback activity ${fallbackActivity.componentName.flattenToShortString()}"
        )
        return Pair(
          LaunchResult.Success(
            packageName = app.packageName,
            activityName = fallbackActivity.componentName.className,
            method = "LauncherApps.startMainActivity (Resolved Profile Fallback)"
          ),
          launchedFallbackApp
        )
      }
    } catch (e: SecurityException) {
      AppLogger.e(AppLogger.Category.LAUNCH, "LAUNCH_FAILED: SecurityException launching ${app.packageName} for user ${identity.userHandleId}", e)
      return Pair(
        LaunchResult.Failed(
          packageName = app.packageName,
          errorMessage = "Permission denied while launching application.",
          exception = e
        ),
        null
      )
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCH, "LAUNCH_FAILED: Error launching ${app.packageName} for user ${identity.userHandleId}", e)
      return Pair(
        LaunchResult.Failed(
          packageName = app.packageName,
          errorMessage = "Failed to launch application: ${e.localizedMessage ?: "Unknown error"}",
          exception = e
        ),
        null
      )
    }

    // No activity found for the requested package and userHandleId
    AppLogger.w(
      AppLogger.Category.LAUNCH,
      "LAUNCH_UNAVAILABLE: Application ${app.packageName} is unavailable or disabled for profile ${identity.userHandleId}"
    )
    return Pair(
      LaunchResult.Unavailable(
        packageName = app.packageName,
        reason = "Application is uninstalled, disabled, or not available for profile ${identity.userHandleId}."
      ),
      null
    )
  }

  /**
   * Attempts to launch an application using its discovered launcher identity within [spaceId].
   *
   * Flow:
   * 1. Resolve exact UserHandle.
   * 2. Verify current component availability via LauncherApps for that exact UserHandle.
   * 3. Launch via LauncherApps.startMainActivity if available.
   * 4. If component is stale within the SAME profile, recover using alternative launcher activity for that profile.
   * 5. If application is unavailable or launch fails, return LaunchResult.Unavailable or LaunchResult.Failed without crashing.
   * 6. If caller or manager provides a CoroutineScope, asynchronously records successful launch to history.
   *    No unmanaged CoroutineScope is ever created.
   */
  /**
   * Suspending variant of [launchApp] where platform resolution and history recording are performed within
   * the calling coroutine context via [ioDispatcher], ensuring caller lifecycle ownership and avoiding
   * synchronous platform operations on the UI thread.
   */
  suspend fun launchAppSuspending(
    app: DiscoveredApp,
    spaceId: String = Space.DEFAULT_SPACE_ID,
    sourceBounds: Rect? = null
  ): LaunchResult = withContext(ioDispatcher) {
    val (result, launchedApp) = performLaunch(app, sourceBounds)
    if (result is LaunchResult.Success) {
      recordSuccessfulLaunch(launchedApp ?: app, spaceId)
      if (spaceRepository != null && launchedApp != null && launchedApp.activityName != app.activityName) {
        try {
          spaceRepository.repairAppIdentity(app.appIdentity, launchedApp.appIdentity)
          AppLogger.i(AppLogger.Category.LAUNCH, "Persisted repaired component for ${app.label}: ${app.activityName} -> ${launchedApp.activityName}")
        } catch (e: Exception) {
          AppLogger.w(AppLogger.Category.LAUNCH, "Failed to persist repaired component for ${app.label}", e)
        }
      }
    }
    result
  }

  /**
   * Attempts to launch an application using its discovered launcher identity within [spaceId].
   * Delegates to [launchAppSuspending] within the provided lifecycle scope.
   */
  fun launchApp(
    app: DiscoveredApp,
    spaceId: String = Space.DEFAULT_SPACE_ID,
    sourceBounds: Rect? = null,
    callerScope: CoroutineScope? = null
  ): LaunchResult {
    val targetScope = callerScope ?: applicationScope ?: coroutineScope
    if (targetScope != null) {
      targetScope.launch(ioDispatcher) {
        launchAppSuspending(app, spaceId, sourceBounds)
      }
      return LaunchResult.Success(
        packageName = app.packageName,
        activityName = app.activityName,
        method = "LauncherApps.startMainActivity"
      )
    }

    val (result, launchedApp) = performLaunch(app, sourceBounds)
    if (result is LaunchResult.Success) {
      AppLogger.d(
        AppLogger.Category.LAUNCH,
        "No coroutine scope provided to record launch history for ${app.label}. Use launchAppSuspending or supply a callerScope."
      )
    }
    return result
  }
}
