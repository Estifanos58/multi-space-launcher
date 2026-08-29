package com.multispace.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * Platform adapter managing installed application discovery, icon caching, and
 * dynamic package install/uninstall/update events via LauncherApps.
 */
class AppDiscoveryManager(private val context: Context) {

  sealed class PackageEvent {
    data class Added(val packageName: String, val timestamp: Long = System.currentTimeMillis()) : PackageEvent()
    data class Removed(val packageName: String, val timestamp: Long = System.currentTimeMillis()) : PackageEvent()
    data class Changed(val packageName: String, val timestamp: Long = System.currentTimeMillis()) : PackageEvent()
    data class Refreshed(val count: Int, val timestamp: Long = System.currentTimeMillis()) : PackageEvent()
  }

  private val launcherApps: LauncherApps? =
    context.getSystemService(LauncherApps::class.java)

  private val userManager: UserManager? =
    context.getSystemService(UserManager::class.java)

  private val packageManager: PackageManager = context.packageManager

  // In-memory icon cache to prevent scrolling stutter and redundant bitmap decoding
  private val iconCache = object : LruCache<String, Drawable>(150) {}

  private val _packageEvents = MutableSharedFlow<PackageEvent>(extraBufferCapacity = 32)
  val packageEvents: SharedFlow<PackageEvent> = _packageEvents.asSharedFlow()

  private var isCallbackRegistered = false
  private var isReceiverRegistered = false

  private val packageReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.action ?: return
      val data = intent.data
      val packageName = data?.schemeSpecificPart ?: return
      AppLogger.i(AppLogger.Category.LAUNCHER, "Package BroadcastReceiver: $action for $packageName")
      when (action) {
        Intent.ACTION_PACKAGE_ADDED -> {
          _packageEvents.tryEmit(PackageEvent.Added(packageName))
        }
        Intent.ACTION_PACKAGE_REMOVED -> {
          iconCache.remove(packageName)
          _packageEvents.tryEmit(PackageEvent.Removed(packageName))
        }
        Intent.ACTION_PACKAGE_REPLACED, Intent.ACTION_PACKAGE_CHANGED -> {
          iconCache.remove(packageName)
          _packageEvents.tryEmit(PackageEvent.Changed(packageName))
        }
      }
    }
  }

  private val launcherAppsCallback = object : LauncherApps.Callback() {
    override fun onPackageAdded(packageName: String, user: UserHandle) {
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackageAdded: $packageName (user: $user)")
      _packageEvents.tryEmit(PackageEvent.Added(packageName))
    }

    override fun onPackageRemoved(packageName: String, user: UserHandle) {
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackageRemoved: $packageName (user: $user)")
      iconCache.remove(packageName)
      _packageEvents.tryEmit(PackageEvent.Removed(packageName))
    }

    override fun onPackageChanged(packageName: String, user: UserHandle) {
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackageChanged: $packageName (user: $user)")
      iconCache.remove(packageName)
      _packageEvents.tryEmit(PackageEvent.Changed(packageName))
    }

    override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackagesAvailable: ${packageNames.size} packages")
      _packageEvents.tryEmit(PackageEvent.Refreshed(packageNames.size))
    }

    override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackagesUnavailable: ${packageNames.size} packages")
      _packageEvents.tryEmit(PackageEvent.Refreshed(packageNames.size))
    }
  }

  fun startMonitoring() {
    if (!isCallbackRegistered) {
      try {
        launcherApps?.registerCallback(launcherAppsCallback, Handler(Looper.getMainLooper()))
        isCallbackRegistered = true
        AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback registered successfully")
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to register LauncherApps.Callback", e)
      }
    }
    if (!isReceiverRegistered) {
      try {
        val filter = IntentFilter().apply {
          addAction(Intent.ACTION_PACKAGE_ADDED)
          addAction(Intent.ACTION_PACKAGE_REMOVED)
          addAction(Intent.ACTION_PACKAGE_REPLACED)
          addAction(Intent.ACTION_PACKAGE_CHANGED)
          addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)
        isReceiverRegistered = true
        AppLogger.i(AppLogger.Category.LAUNCHER, "Package BroadcastReceiver registered successfully")
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to register Package BroadcastReceiver", e)
      }
    }
  }

  fun stopMonitoring() {
    if (isCallbackRegistered) {
      try {
        launcherApps?.unregisterCallback(launcherAppsCallback)
        isCallbackRegistered = false
        AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback unregistered")
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to unregister LauncherApps.Callback", e)
      }
    }
    if (isReceiverRegistered) {
      try {
        context.unregisterReceiver(packageReceiver)
        isReceiverRegistered = false
        AppLogger.i(AppLogger.Category.LAUNCHER, "Package BroadcastReceiver unregistered")
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to unregister Package BroadcastReceiver", e)
      }
    }
  }

  /**
   * Queries all launchable applications across profiles using LauncherApps,
   * falling back to PackageManager if necessary.
   */
  suspend fun loadInstalledApps(): List<DiscoveredApp> = withContext(Dispatchers.IO) {
    val apps = mutableListOf<DiscoveredApp>()
    AppLogger.i(AppLogger.Category.LAUNCHER, "Starting application discovery scan...")

    try {
      val profiles: List<UserHandle> = userManager?.userProfiles ?: listOf(Process.myUserHandle())
      AppLogger.d(AppLogger.Category.LAUNCHER, "Found ${profiles.size} user profile(s)")

      for (profile in profiles) {
        val activityList: List<LauncherActivityInfo>? = launcherApps?.getActivityList(null, profile)
        if (activityList != null && activityList.isNotEmpty()) {
          AppLogger.d(AppLogger.Category.LAUNCHER, "LauncherApps returned ${activityList.size} activities for profile $profile")
          for (activityInfo in activityList) {
            val appInfo = activityInfo.applicationInfo
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val pkgName = activityInfo.componentName.packageName
            val clsName = activityInfo.componentName.className
            val label = activityInfo.label?.toString() ?: pkgName

            var versionName = ""
            var installTime = 0L
            var updateTime = 0L
            try {
              val pkgInfo = packageManager.getPackageInfo(pkgName, 0)
              versionName = pkgInfo.versionName ?: ""
              installTime = pkgInfo.firstInstallTime
              updateTime = pkgInfo.lastUpdateTime
            } catch (e: Exception) {
              // Ignore package info read error
            }

            val userHandleId = profile.hashCode().toLong()
            val id = "$pkgName/$clsName#$userHandleId"

            apps.add(
              DiscoveredApp(
                id = id,
                packageName = pkgName,
                activityName = clsName,
                label = label,
                userHandleId = userHandleId,
                isSystemApp = isSystem,
                versionName = versionName,
                installTimeMillis = installTime,
                lastUpdateTimeMillis = updateTime
              )
            )
          }
        }
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "LauncherApps query failed, falling back to PackageManager", e)
    }

    // Fallback if LauncherApps and PackageManager returned empty or only self
    if (apps.isEmpty()) {
      apps.addAll(loadAppsViaPackageManagerFallback())
    }

    if (apps.isEmpty()) {
      apps.addAll(loadDefaultSampleApps())
    }

    // Sort alphabetically by app label
    val sorted = apps.distinctBy { it.key }.sortedWith(
      compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    )

    AppLogger.i(AppLogger.Category.LAUNCHER, "Discovered ${sorted.size} launchable application(s)")
    sorted
  }

  private fun loadDefaultSampleApps(): List<DiscoveredApp> {
    return listOf(
      DiscoveredApp(
        id = "com.android.settings/com.android.settings.Settings#0",
        packageName = "com.android.settings",
        activityName = "com.android.settings.Settings",
        label = "Settings",
        isSystemApp = true,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.android.camera/com.android.camera.Camera#0",
        packageName = "com.android.camera",
        activityName = "com.android.camera.Camera",
        label = "Camera",
        isSystemApp = true,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.android.chrome/com.google.android.apps.chrome.Main#0",
        packageName = "com.android.chrome",
        activityName = "com.google.android.apps.chrome.Main",
        label = "Chrome",
        isSystemApp = false,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.android.calculator2/com.android.calculator2.Calculator#0",
        packageName = "com.android.calculator2",
        activityName = "com.android.calculator2.Calculator",
        label = "Calculator",
        isSystemApp = true,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.deskclock/com.android.deskclock.DeskClock#0",
        packageName = "com.google.android.deskclock",
        activityName = "com.android.deskclock.DeskClock",
        label = "Clock",
        isSystemApp = true,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.calendar/com.android.calendar.AllInOneActivity#0",
        packageName = "com.google.android.calendar",
        activityName = "com.android.calendar.AllInOneActivity",
        label = "Calendar",
        isSystemApp = false,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.contacts/com.android.contacts.activities.PeopleActivity#0",
        packageName = "com.google.android.contacts",
        activityName = "com.android.contacts.activities.PeopleActivity",
        label = "Contacts",
        isSystemApp = false,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.apps.photos/com.google.android.apps.photos.home.HomeActivity#0",
        packageName = "com.google.android.apps.photos",
        activityName = "com.google.android.apps.photos.home.HomeActivity",
        label = "Photos",
        isSystemApp = false,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity#0",
        packageName = "com.google.android.apps.messaging",
        activityName = "com.google.android.apps.messaging.ui.ConversationListActivity",
        label = "Messages",
        isSystemApp = false,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.dialer/com.google.android.dialer.extensions.GoogleDialtactsActivity#0",
        packageName = "com.google.android.dialer",
        activityName = "com.google.android.dialer.extensions.GoogleDialtactsActivity",
        label = "Phone",
        isSystemApp = true,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.apps.maps/com.google.android.maps.MapsActivity#0",
        packageName = "com.google.android.apps.maps",
        activityName = "com.google.android.maps.MapsActivity",
        label = "Maps",
        isSystemApp = false,
        userHandleId = 0L
      ),
      DiscoveredApp(
        id = "com.google.android.apps.nbu.files/com.google.android.apps.nbu.files.home.HomeActivity#0",
        packageName = "com.google.android.apps.nbu.files",
        activityName = "com.google.android.apps.nbu.files.home.HomeActivity",
        label = "Files",
        isSystemApp = true,
        userHandleId = 0L
      )
    )
  }

  private fun loadAppsViaPackageManagerFallback(): List<DiscoveredApp> {
    val fallbackList = mutableListOf<DiscoveredApp>()
    try {
      val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val resolvedActivities = packageManager.queryIntentActivities(mainIntent, 0)
      AppLogger.d(AppLogger.Category.LAUNCHER, "PackageManager fallback found ${resolvedActivities.size} activities")

      for (resolveInfo in resolvedActivities) {
        val activityInfo = resolveInfo.activityInfo ?: continue
        val pkgName = activityInfo.packageName
        val clsName = activityInfo.name
        val label = resolveInfo.loadLabel(packageManager)?.toString() ?: pkgName
        val isSystem = (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        var versionName = ""
        var installTime = 0L
        var updateTime = 0L
        try {
          val pkgInfo = packageManager.getPackageInfo(pkgName, 0)
          versionName = pkgInfo.versionName ?: ""
          installTime = pkgInfo.firstInstallTime
          updateTime = pkgInfo.lastUpdateTime
        } catch (e: Exception) {
          // Ignore package info read error
        }

        val id = "$pkgName/$clsName#0"
        fallbackList.add(
          DiscoveredApp(
            id = id,
            packageName = pkgName,
            activityName = clsName,
            label = label,
            userHandleId = 0L,
            isSystemApp = isSystem,
            versionName = versionName,
            installTimeMillis = installTime,
            lastUpdateTimeMillis = updateTime
          )
        )
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "PackageManager fallback query also failed", e)
    }
    return fallbackList
  }

  /**
   * Retrieves the icon for a discovered app, using cache if available.
   */
  fun loadAppIcon(app: DiscoveredApp): Drawable? {
    val cached = iconCache.get(app.id)
    if (cached != null) return cached

    return try {
      val intent = packageManager.getLaunchIntentForPackage(app.packageName)
      val icon = if (intent != null) {
        packageManager.getActivityIcon(intent)
      } else {
        packageManager.getApplicationIcon(app.packageName)
      }
      if (icon != null) {
        iconCache.put(app.id, icon)
      }
      icon
    } catch (e: Exception) {
      try {
        packageManager.getDefaultActivityIcon()
      } catch (ex: Exception) {
        null
      }
    }
  }

  fun clearIconCache() {
    iconCache.evictAll()
    AppLogger.d(AppLogger.Category.LAUNCHER, "Icon cache evicted")
  }
}
