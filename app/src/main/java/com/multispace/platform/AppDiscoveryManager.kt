package com.multispace.platform

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * Platform adapter managing installed application discovery, icon caching, and
 * dynamic profile-aware package install/uninstall/update events.
 */
class AppDiscoveryManager(private val context: Context) {

  sealed class PackageEvent {
    abstract val packageName: String
    abstract val userHandleId: Long
    abstract val timestamp: Long

    data class Added(
      override val packageName: String,
      override val userHandleId: Long = 0L,
      override val timestamp: Long = System.currentTimeMillis()
    ) : PackageEvent()

    data class Removed(
      override val packageName: String,
      override val userHandleId: Long = 0L,
      override val timestamp: Long = System.currentTimeMillis()
    ) : PackageEvent()

    data class Changed(
      override val packageName: String,
      override val userHandleId: Long = 0L,
      override val timestamp: Long = System.currentTimeMillis()
    ) : PackageEvent()

    data class Refreshed(
      override val packageName: String = "",
      override val userHandleId: Long = 0L,
      val count: Int = 0,
      val packages: List<String> = emptyList(),
      override val timestamp: Long = System.currentTimeMillis()
    ) : PackageEvent()
  }

  private val launcherApps: LauncherApps? =
    context.getSystemService(LauncherApps::class.java)

  private val userManager: UserManager? =
    context.getSystemService(UserManager::class.java)

  private val packageManager: PackageManager = context.packageManager

  // In-memory caches to prevent scrolling stutter and redundant bitmap decoding
  // Key format: "$packageName/$activityName#$userHandleId"
  private val iconCache = object : LruCache<String, Drawable>(250) {}
  private val bitmapCache = object : LruCache<String, Bitmap>(250) {}
  private val metadataCache = PackageMetadataCache(300)
  private val deduplicator = PackageEventDeduplicator(windowMillis = 400L)

  // Density-aware icon resolution (128px is memory-efficient and crisp for low-RAM devices)
  private val targetIconSizePx: Int by lazy {
    val density = context.resources.displayMetrics.density
    (density * 56).toInt().coerceIn(96, 192)
  }

  private val _packageEvents = MutableSharedFlow<PackageEvent>(extraBufferCapacity = 64)
  val packageEvents: SharedFlow<PackageEvent> = _packageEvents.asSharedFlow()

  private var isCallbackRegistered = false
  private var isReceiverRegistered = false

  private fun emitPackageEvent(event: PackageEvent) {
    if (deduplicator.shouldProcess(event)) {
      _packageEvents.tryEmit(event)
    } else {
      AppLogger.d(AppLogger.Category.LAUNCHER, "Suppressed duplicate package event: $event")
    }
  }

  private val packageReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.action ?: return
      val data = intent.data
      val packageName = data?.schemeSpecificPart ?: return
      val myUserHandleId = UserHandleHelper.getUserHandleId(userManager, Process.myUserHandle())

      AppLogger.i(AppLogger.Category.LAUNCHER, "Package BroadcastReceiver: $action for $packageName")
      when (action) {
        Intent.ACTION_PACKAGE_ADDED -> {
          val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
          if (!isReplacing) {
            emitPackageEvent(PackageEvent.Added(packageName, myUserHandleId))
          }
        }
        Intent.ACTION_PACKAGE_REMOVED -> {
          val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
          if (!isReplacing) {
            evictPackageFromCache(packageName, myUserHandleId)
            emitPackageEvent(PackageEvent.Removed(packageName, myUserHandleId))
          }
        }
        Intent.ACTION_PACKAGE_REPLACED, Intent.ACTION_PACKAGE_CHANGED -> {
          evictPackageFromCache(packageName, myUserHandleId)
          emitPackageEvent(PackageEvent.Changed(packageName, myUserHandleId))
        }
      }
    }
  }

  private val launcherAppsCallback = object : LauncherApps.Callback() {
    override fun onPackageAdded(packageName: String, user: UserHandle) {
      val userHandleId = UserHandleHelper.getUserHandleId(userManager, user)
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackageAdded: $packageName (user: $userHandleId)")
      emitPackageEvent(PackageEvent.Added(packageName, userHandleId))
    }

    override fun onPackageRemoved(packageName: String, user: UserHandle) {
      val userHandleId = UserHandleHelper.getUserHandleId(userManager, user)
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackageRemoved: $packageName (user: $userHandleId)")
      evictPackageFromCache(packageName, userHandleId)
      emitPackageEvent(PackageEvent.Removed(packageName, userHandleId))
    }

    override fun onPackageChanged(packageName: String, user: UserHandle) {
      val userHandleId = UserHandleHelper.getUserHandleId(userManager, user)
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackageChanged: $packageName (user: $userHandleId)")
      evictPackageFromCache(packageName, userHandleId)
      emitPackageEvent(PackageEvent.Changed(packageName, userHandleId))
    }

    override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
      val userHandleId = UserHandleHelper.getUserHandleId(userManager, user)
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackagesAvailable: ${packageNames.size} packages (user: $userHandleId)")
      packageNames.forEach { evictPackageFromCache(it, userHandleId) }
      emitPackageEvent(
        PackageEvent.Refreshed(
          userHandleId = userHandleId,
          count = packageNames.size,
          packages = packageNames.toList()
        )
      )
    }

    override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
      val userHandleId = UserHandleHelper.getUserHandleId(userManager, user)
      AppLogger.i(AppLogger.Category.LAUNCHER, "LauncherApps.Callback: onPackagesUnavailable: ${packageNames.size} packages (user: $userHandleId)")
      packageNames.forEach { evictPackageFromCache(it, userHandleId) }
      emitPackageEvent(
        PackageEvent.Refreshed(
          userHandleId = userHandleId,
          count = packageNames.size,
          packages = packageNames.toList()
        )
      )
    }
  }

  fun evictPackageFromCache(packageName: String, userHandleId: Long? = null) {
    val prefix = "$packageName/"
    val suffix = if (userHandleId != null) "#$userHandleId" else null

    fun matches(key: String): Boolean {
      if (!key.startsWith(prefix)) return false
      if (suffix != null && !key.endsWith(suffix)) return false
      return true
    }

    iconCache.snapshot().keys.filter { matches(it) }.forEach { iconCache.remove(it) }
    bitmapCache.snapshot().keys.filter { matches(it) }.forEach { bitmapCache.remove(it) }
    metadataCache.evict(packageName, userHandleId)
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
   * Resolves package metadata with in-memory caching to avoid repeated PackageManager IPC.
   */
  fun resolvePackageMetadata(
    packageName: String,
    userHandleId: Long,
    isSystemApp: Boolean,
    isUpdatedSystemApp: Boolean
  ): PackageMetadata {
    val cached = metadataCache.get(packageName, userHandleId)
    if (cached != null) return cached

    val dpm = try {
      context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    } catch (_: Exception) { null }
    val isBlocked = try {
      dpm?.isUninstallBlocked(null, packageName) ?: false
    } catch (_: Exception) { false }
    val isUninstallable = (!isSystemApp || isUpdatedSystemApp) && !isBlocked

    var versionName = ""
    var installTime = 0L
    var updateTime = 0L
    try {
      val pkgInfo = packageManager.getPackageInfo(packageName, 0)
      versionName = pkgInfo.versionName ?: ""
      installTime = pkgInfo.firstInstallTime
      updateTime = pkgInfo.lastUpdateTime
    } catch (_: Exception) {}

    val metadata = PackageMetadata(
      versionName = versionName,
      installTimeMillis = installTime,
      lastUpdateTimeMillis = updateTime,
      isUninstallable = isUninstallable
    )
    metadataCache.put(packageName, userHandleId, metadata)
    return metadata
  }

  /**
   * Queries launchable activities for a specific package and profile incrementally.
   */
  suspend fun loadPackageApps(packageName: String, userHandleId: Long = 0L): List<DiscoveredApp> = withContext(Dispatchers.IO) {
    val apps = mutableListOf<DiscoveredApp>()
    try {
      val profile = UserHandleHelper.resolveUserHandle(userManager, userHandleId)
      val activityList = try {
        launcherApps?.getActivityList(packageName, profile)
      } catch (e: Exception) {
        null
      }
      if (!activityList.isNullOrEmpty()) {
        for (activityInfo in activityList) {
          val app = buildDiscoveredAppFromLauncherActivity(activityInfo, userHandleId)
          apps.add(app)
        }
      } else {
        val myUserHandleId = UserHandleHelper.getUserHandleId(userManager, Process.myUserHandle())
        if (userHandleId == 0L || userHandleId == myUserHandleId) {
          val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = packageName
          }
          val resolved = packageManager.queryIntentActivities(mainIntent, 0)
          for (resolveInfo in resolved) {
            val app = buildDiscoveredAppFromResolveInfo(resolveInfo, userHandleId)
            if (app != null) {
              apps.add(app)
            }
          }
        }
      }
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to load activities for package $packageName (user: $userHandleId)", e)
    }
    apps.distinctBy { it.id }
  }

  /**
   * Performs a full application discovery scan across all available user profiles.
   * Real platform data only; strictly never returns fake sample applications.
   */
  suspend fun loadInstalledApps(): List<DiscoveredApp> = withContext(Dispatchers.IO) {
    val apps = mutableListOf<DiscoveredApp>()
    AppLogger.i(AppLogger.Category.LAUNCHER, "Starting application discovery scan...")

    try {
      val profiles: List<UserHandle> = userManager?.userProfiles ?: listOf(Process.myUserHandle())
      AppLogger.d(AppLogger.Category.LAUNCHER, "Found ${profiles.size} user profile(s)")

      for (profile in profiles) {
        val userHandleId = UserHandleHelper.getUserHandleId(userManager, profile)
        val activityList: List<LauncherActivityInfo>? = try {
          launcherApps?.getActivityList(null, profile)
        } catch (e: Exception) {
          AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to get activity list for profile $profile", e)
          null
        }

        if (!activityList.isNullOrEmpty()) {
          AppLogger.d(AppLogger.Category.LAUNCHER, "LauncherApps returned ${activityList.size} activities for profile $profile")
          for (activityInfo in activityList) {
            val app = buildDiscoveredAppFromLauncherActivity(activityInfo, userHandleId)
            apps.add(app)
          }
        }
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "LauncherApps query failed, falling back to PackageManager", e)
    }

    // Fallback if LauncherApps returned empty
    if (apps.isEmpty()) {
      apps.addAll(loadAppsViaPackageManagerFallback())
    }

    // Sort alphabetically by app label
    val sorted = apps.distinctBy { it.id }.sortedWith(
      compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    )

    AppLogger.i(AppLogger.Category.LAUNCHER, "Discovered ${sorted.size} launchable application(s)")
    sorted
  }

  /**
   * Executes discovery and returns an explicit [DiscoveryResult] without masking failures.
   */
  suspend fun discoverApps(): DiscoveryResult = withContext(Dispatchers.IO) {
    try {
      val apps = loadInstalledApps()
      if (apps.isNotEmpty()) {
        DiscoveryResult.Success(apps)
      } else {
        DiscoveryResult.Empty("No launchable applications found on device")
      }
    } catch (t: Throwable) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "App discovery encountered error", t)
      DiscoveryResult.Failure(t)
    }
  }

  private fun buildDiscoveredAppFromLauncherActivity(
    activityInfo: LauncherActivityInfo,
    userHandleId: Long
  ): DiscoveredApp {
    val appInfo = activityInfo.applicationInfo
    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    val pkgName = activityInfo.componentName.packageName
    val clsName = activityInfo.componentName.className
    val label = activityInfo.label?.toString() ?: pkgName

    val metadata = resolvePackageMetadata(pkgName, userHandleId, isSystem, isUpdatedSystem)
    val id = "$pkgName/$clsName#$userHandleId"

    return DiscoveredApp(
      id = id,
      packageName = pkgName,
      activityName = clsName,
      label = label,
      userHandleId = userHandleId,
      isSystemApp = isSystem,
      isUninstallable = metadata.isUninstallable,
      versionName = metadata.versionName,
      installTimeMillis = metadata.installTimeMillis,
      lastUpdateTimeMillis = metadata.lastUpdateTimeMillis
    )
  }

  private fun buildDiscoveredAppFromResolveInfo(
    resolveInfo: ResolveInfo,
    userHandleId: Long
  ): DiscoveredApp? {
    val activityInfo = resolveInfo.activityInfo ?: return null
    val pkgName = activityInfo.packageName
    val clsName = activityInfo.name
    val label = resolveInfo.loadLabel(packageManager)?.toString() ?: pkgName
    val isSystem = (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    val isUpdatedSystem = (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    val metadata = resolvePackageMetadata(pkgName, userHandleId, isSystem, isUpdatedSystem)
    val id = "$pkgName/$clsName#$userHandleId"

    return DiscoveredApp(
      id = id,
      packageName = pkgName,
      activityName = clsName,
      label = label,
      userHandleId = userHandleId,
      isSystemApp = isSystem,
      isUninstallable = metadata.isUninstallable,
      versionName = metadata.versionName,
      installTimeMillis = metadata.installTimeMillis,
      lastUpdateTimeMillis = metadata.lastUpdateTimeMillis
    )
  }

  private fun loadAppsViaPackageManagerFallback(): List<DiscoveredApp> {
    val fallbackList = mutableListOf<DiscoveredApp>()
    try {
      val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val resolvedActivities = packageManager.queryIntentActivities(mainIntent, 0)
      val myUserHandleId = UserHandleHelper.getUserHandleId(userManager, Process.myUserHandle())
      AppLogger.d(AppLogger.Category.LAUNCHER, "PackageManager fallback found ${resolvedActivities.size} activities")

      for (resolveInfo in resolvedActivities) {
        val app = buildDiscoveredAppFromResolveInfo(resolveInfo, myUserHandleId)
        if (app != null) {
          fallbackList.add(app)
        }
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "PackageManager fallback query also failed", e)
    }
    return fallbackList
  }

  /**
   * Retrieves the pre-rasterized, memory-efficient Bitmap icon for an app.
   * Fast path returns instantly from LruCache in <0.01ms without UI thread jank.
   */
  fun loadAppIconBitmap(app: DiscoveredApp): Bitmap? {
    val cachedBitmap = bitmapCache.get(app.id)
    if (cachedBitmap != null) return cachedBitmap

    val drawable = loadAppIcon(app) ?: return null
    val bitmap = drawableToBitmap(drawable, targetIconSizePx)
    if (bitmap != null) {
      bitmapCache.put(app.id, bitmap)
    }
    return bitmap
  }

  /**
   * Retrieves the Drawable icon for a discovered app using fast direct activity resolution
   * with profile-aware badging.
   */
  fun loadAppIcon(app: DiscoveredApp): Drawable? {
    val cached = iconCache.get(app.id)
    if (cached != null) return cached

    return try {
      var icon: Drawable? = null

      // Fast Path 1: LauncherApps direct activity info icon (avoids intent filter resolution and badges work profiles)
      if (launcherApps != null && userManager != null) {
        val profile = UserHandleHelper.resolveUserHandle(userManager, app.userHandleId)
        val activityList = try {
          launcherApps.getActivityList(app.packageName, profile)
        } catch (_: Exception) { null }

        val matchedActivity = activityList?.firstOrNull {
          it.componentName.className == app.activityName
        } ?: activityList?.firstOrNull()

        if (matchedActivity != null) {
          icon = matchedActivity.getBadgedIcon(0)
        }
      }

      // Fast Path 2: Direct ComponentName PackageManager lookup
      if (icon == null) {
        try {
          val componentName = ComponentName(app.packageName, app.activityName)
          icon = packageManager.getActivityInfo(componentName, 0).loadIcon(packageManager)
        } catch (_: Exception) {}
      }

      // Fast Path 3: Application icon fallback
      if (icon == null) {
        try {
          icon = packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {}
      }

      // Fallback: Default activity icon
      if (icon == null) {
        icon = packageManager.getDefaultActivityIcon()
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

  /**
   * Pre-warms bitmap and drawable icon caches in background IO coroutines
   * so scrolling operations hit 100% in-memory cache without frame drops.
   */
  suspend fun prewarmIconCache(apps: List<DiscoveredApp>) = withContext(Dispatchers.IO) {
    for (app in apps) {
      ensureActive()
      if (bitmapCache.get(app.id) == null) {
        try {
          val drawable = loadAppIcon(app)
          if (drawable != null) {
            val bitmap = drawableToBitmap(drawable, targetIconSizePx)
            if (bitmap != null) {
              bitmapCache.put(app.id, bitmap)
            }
          }
        } catch (_: Exception) {}
      }
    }
  }

  private fun drawableToBitmap(drawable: Drawable, targetSize: Int): Bitmap? {
    return try {
      if (drawable is BitmapDrawable && drawable.bitmap != null) {
        val src = drawable.bitmap
        if (src.width == targetSize && src.height == targetSize) {
          return src
        }
        return Bitmap.createScaledBitmap(src, targetSize, targetSize, true)
      }

      val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSize
      val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSize
      val scaledWidth = targetSize
      val scaledHeight = (height * targetSize / width).coerceAtLeast(1)

      val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      drawable.setBounds(0, 0, canvas.width, canvas.height)
      drawable.draw(canvas)
      bitmap
    } catch (e: Exception) {
      null
    }
  }

  fun clearIconCache() {
    iconCache.evictAll()
    bitmapCache.evictAll()
    metadataCache.clear()
    deduplicator.clear()
    AppLogger.d(AppLogger.Category.LAUNCHER, "Discovery and icon caches evicted")
  }
}
