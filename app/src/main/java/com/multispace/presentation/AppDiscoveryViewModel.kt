package com.multispace.presentation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multispace.MultiSpaceApplication
import com.multispace.data.database.LauncherDatabase
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.data.repository.RoomLaunchHistoryRepository
import com.multispace.data.repository.RoomSpaceRepository
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppUsageStats
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.domain.repository.LaunchHistoryRepository
import com.multispace.domain.repository.SpaceRepository
import com.multispace.platform.AppCatalogUpdater
import com.multispace.platform.AppDiscoveryManager
import com.multispace.platform.AppLaunchManager
import com.multispace.platform.DiscoveryResult
import com.multispace.platform.LauncherSessionManager
import com.multispace.platform.LaunchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppFilter {
  ALL,
  USER_ONLY,
  SYSTEM_ONLY
}

enum class AppViewMode {
  GRID,
  LIST
}

enum class AppSortMode {
  NAME_ASC,
  NAME_DESC,
  RECENTLY_UPDATED
}

data class AppDiscoveryUiState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val allApps: List<DiscoveredApp> = emptyList(),
  val filteredApps: List<DiscoveredApp> = emptyList(),
  val searchQuery: String = "",
  val activeFilter: AppFilter = AppFilter.ALL,
  val viewMode: AppViewMode = AppViewMode.GRID,
  val sortMode: AppSortMode = AppSortMode.NAME_ASC,
  val totalAppCount: Int = 0,
  val userAppCount: Int = 0,
  val systemAppCount: Int = 0,
  val lastScannedTime: Long = 0L,
  val recentPackageEvent: String = "No package events yet",
  val recentLaunchLog: String = "No launch attempts yet",
  val launchHistory: List<String> = emptyList(),
  val discoveryResult: DiscoveryResult? = null
)

class AppDiscoveryViewModel @JvmOverloads constructor(
  application: Application,
  private val spaceRepository: SpaceRepository = (application as? MultiSpaceApplication)?.container?.spaceRepository ?: run {
    val db = LauncherDatabase.getInstance(application.applicationContext)
    val session = (application as? MultiSpaceApplication)?.container?.sessionManager
      ?: LauncherSessionManager(application.applicationContext)
    RoomSpaceRepository(
      spaceDao = db.spaceDao(),
      membershipDao = db.spaceMembershipDao(),
      layoutDao = db.spaceLayoutDao(),
      preferences = LauncherPreferences.getInstance(application.applicationContext),
      context = application.applicationContext,
      database = db,
      sessionManager = session
    )
  },
  private val discoveryManager: AppDiscoveryManager = (application as? MultiSpaceApplication)?.container?.discoveryManager
    ?: AppDiscoveryManager(application.applicationContext),
  private val launchManager: AppLaunchManager = (application as? MultiSpaceApplication)?.container?.appLaunchManager
    ?: AppLaunchManager(
      context = application.applicationContext,
      spaceRepository = spaceRepository,
      coroutineScope = null
    )
) : AndroidViewModel(application) {

  val launchHistoryRepository: LaunchHistoryRepository = launchManager.historyRepository

  private val _uiState = MutableStateFlow(AppDiscoveryUiState())
  val uiState: StateFlow<AppDiscoveryUiState> = _uiState.asStateFlow()

  private val _iconBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
  val iconBitmaps: StateFlow<Map<String, Bitmap>> = _iconBitmaps.asStateFlow()

  val recentApps: StateFlow<List<DiscoveredApp>> = launchHistoryRepository
    .getRecentAppsFlow(Space.DEFAULT_SPACE_ID, _uiState.map { it.allApps })
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  fun getRecentAppsFlow(spaceId: String, limit: Int = 20): Flow<List<DiscoveredApp>> {
    return launchHistoryRepository.getRecentAppsFlow(spaceId, _uiState.map { it.allApps }, limit)
  }

  fun getMostUsedAppsFlow(spaceId: String, limit: Int = 20): Flow<List<DiscoveredApp>> {
    return launchHistoryRepository.getMostUsedAppsFlow(spaceId, _uiState.map { it.allApps }, limit)
  }

  fun getTopAppsWithCountFlow(spaceId: String, limit: Int = 3): Flow<List<com.multispace.domain.model.AppLaunchCount>> {
    return launchHistoryRepository.getTopMostUsedAppsWithCountFlow(spaceId, _uiState.map { it.allApps }, limit)
  }

  fun getWeeklyUsageFlow(spaceId: String): Flow<List<com.multispace.domain.model.DailyUsage>> {
    return launchHistoryRepository.getDailyLaunchCountsLast7DaysFlow(spaceId)
  }

  suspend fun getSpaceUsageStats(spaceId: String, now: Long = System.currentTimeMillis()): SpaceUsageStats {
    return launchHistoryRepository.getSpaceUsageStats(spaceId, _uiState.value.allApps, now)
  }

  fun getSpaceUsageStatsFlow(spaceId: String): Flow<SpaceUsageStats> {
    return launchHistoryRepository.getSpaceUsageStatsFlow(spaceId, _uiState.map { it.allApps })
  }

  suspend fun getAppUsageStats(
    spaceId: String,
    identity: AppIdentity,
    app: DiscoveredApp? = null,
    now: Long = System.currentTimeMillis()
  ): AppUsageStats {
    return launchHistoryRepository.getAppUsageStats(spaceId, identity, app, now)
  }

  private val _userFeedback = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

  private var activeScanJob: Job? = null
  private var hasPendingScan: Boolean = false
  private var pendingScanSilent: Boolean = true
  private var prewarmJob: Job? = null

  init {
    AppLogger.i(AppLogger.Category.LAUNCHER, "AppDiscoveryViewModel initialized")
    discoveryManager.startMonitoring()
    observePackageEvents()
    loadApps()
  }

  private fun observePackageEvents() {
    viewModelScope.launch {
      discoveryManager.packageEvents.collect { event ->
        val eventDescription = when (event) {
          is AppDiscoveryManager.PackageEvent.Added -> "Package Added: ${event.packageName} (profile: ${event.userHandleId})"
          is AppDiscoveryManager.PackageEvent.Removed -> "Package Removed: ${event.packageName} (profile: ${event.userHandleId})"
          is AppDiscoveryManager.PackageEvent.Changed -> "Package Changed: ${event.packageName} (profile: ${event.userHandleId})"
          is AppDiscoveryManager.PackageEvent.Refreshed -> "Packages Refreshed: ${event.count} packages"
        }
        _uiState.update { it.copy(recentPackageEvent = eventDescription) }

        // Incremental catalog update without rebuilding entire catalog
        handlePackageEvent(event)
      }
    }
  }

  private fun handlePackageEvent(event: AppDiscoveryManager.PackageEvent) {
    viewModelScope.launch {
      when (event) {
        is AppDiscoveryManager.PackageEvent.Added -> {
          val newApps = discoveryManager.loadPackageApps(event.packageName, event.userHandleId)
          applyCatalogMutation { currentCatalog ->
            AppCatalogUpdater.applyPackageUpsert(currentCatalog, newApps, event.packageName, event.userHandleId)
          }
          if (newApps.isNotEmpty()) {
            discoveryManager.prewarmIconCache(newApps)
            val prewarmed = newApps.mapNotNull { a ->
              discoveryManager.getCachedAppIconBitmap(a)?.let { a.id to it }
            }.toMap()
            if (prewarmed.isNotEmpty()) {
              _iconBitmaps.update { it + prewarmed }
            }
          }
        }
        is AppDiscoveryManager.PackageEvent.Changed -> {
          val updatedApps = discoveryManager.loadPackageApps(event.packageName, event.userHandleId)
          applyCatalogMutation { currentCatalog ->
            AppCatalogUpdater.applyPackageUpsert(currentCatalog, updatedApps, event.packageName, event.userHandleId)
          }
          if (updatedApps.isNotEmpty()) {
            discoveryManager.prewarmIconCache(updatedApps)
            val prewarmed = updatedApps.mapNotNull { a ->
              discoveryManager.getCachedAppIconBitmap(a)?.let { a.id to it }
            }.toMap()
            if (prewarmed.isNotEmpty()) {
              _iconBitmaps.update { it + prewarmed }
            }
          }
        }
        is AppDiscoveryManager.PackageEvent.Removed -> {
          applyCatalogMutation { currentCatalog ->
            AppCatalogUpdater.applyPackageRemoval(currentCatalog, event.packageName, event.userHandleId)
          }
          _iconBitmaps.update { current ->
            current.filterKeys { !it.startsWith("${event.packageName}/") }
          }
          // Profile-safe persistence reconciliation:
          // Placements, dock items, folders, and memberships for this uninstalled package and userHandleId
          // are cleaned up transactionally without affecting any other profile.
          try {
            spaceRepository.cleanupUninstalledApp(event.packageName, event.userHandleId)
          } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to reconcile persistence for uninstalled package ${event.packageName} (profile: ${event.userHandleId})", e)
          }
        }
        is AppDiscoveryManager.PackageEvent.Refreshed -> {
          if (event.packages.isNotEmpty()) {
            val batchApps = mutableListOf<DiscoveredApp>()
            for (pkg in event.packages) {
              batchApps.addAll(discoveryManager.loadPackageApps(pkg, event.userHandleId))
            }
            applyCatalogMutation { currentCatalog ->
              AppCatalogUpdater.applyBatchUpsert(currentCatalog, batchApps, event.packages.toSet(), event.userHandleId)
            }
            if (batchApps.isNotEmpty()) {
              discoveryManager.prewarmIconCache(batchApps)
              val prewarmed = batchApps.mapNotNull { a ->
                discoveryManager.getCachedAppIconBitmap(a)?.let { a.id to it }
              }.toMap()
              if (prewarmed.isNotEmpty()) {
                _iconBitmaps.update { it + prewarmed }
              }
            }
          } else {
            loadApps(isSilent = true, forceRefresh = true)
          }
        }
      }
    }
  }

  private fun applyCatalogMutation(mutation: (List<DiscoveredApp>) -> List<DiscoveredApp>) {
    _uiState.update { current ->
      val updatedAllApps = mutation(current.allApps)
      val userApps = updatedAllApps.count { !it.isSystemApp }
      val systemApps = updatedAllApps.count { it.isSystemApp }
      val filtered = applyFiltersAndSort(updatedAllApps, current.searchQuery, current.activeFilter, current.sortMode)
      current.copy(
        allApps = updatedAllApps,
        filteredApps = filtered,
        totalAppCount = updatedAllApps.size,
        userAppCount = userApps,
        systemAppCount = systemApps,
        lastScannedTime = System.currentTimeMillis(),
        discoveryResult = if (updatedAllApps.isNotEmpty()) DiscoveryResult.Success(updatedAllApps) else DiscoveryResult.Empty()
      )
    }
  }

  /**
   * Discovers installed applications via full discovery scan.
   * Concurrently invoked requests are coalesced so at most one active scan runs at a time.
   * If a scan is already in progress, subsequent requests flag a pending refresh and return immediately.
   * Redundant silent/lifecycle requests when apps are already discovered are skipped.
   */
  fun loadApps(isSilent: Boolean = false, forceRefresh: Boolean = false) {
    if (!forceRefresh && isSilent && _uiState.value.allApps.isNotEmpty()) {
      AppLogger.d(AppLogger.Category.LAUNCHER, "App discovery already populated (${_uiState.value.allApps.size} apps), skipping redundant scan")
      return
    }

    if (!isSilent) {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    if (activeScanJob?.isActive == true) {
      hasPendingScan = true
      if (!isSilent) {
        pendingScanSilent = false
      }
      return
    }

    pendingScanSilent = isSilent
    hasPendingScan = false

    activeScanJob = viewModelScope.launch {
      try {
        var runNext = true
        while (runNext) {
          val silent = pendingScanSilent
          hasPendingScan = false
          pendingScanSilent = true

          if (!silent && !_uiState.value.isLoading) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
          }

          val result = discoveryManager.discoverApps()
          val apps = when (result) {
            is DiscoveryResult.Success -> result.apps
            is DiscoveryResult.Empty -> emptyList()
            is DiscoveryResult.Failure -> throw result.error
          }

          val userApps = apps.count { !it.isSystemApp }
          val systemApps = apps.count { it.isSystemApp }

          _uiState.update { current ->
            val filtered = applyFiltersAndSort(apps, current.searchQuery, current.activeFilter, current.sortMode)
            current.copy(
              isLoading = false,
              errorMessage = null,
              allApps = apps,
              filteredApps = filtered,
              totalAppCount = apps.size,
              userAppCount = userApps,
              systemAppCount = systemApps,
              lastScannedTime = System.currentTimeMillis(),
              discoveryResult = result
            )
          }

          // Cancel any previous prewarm job to prevent accumulation
          prewarmJob?.cancel()
          if (apps.isNotEmpty()) {
            // Prioritize initial visible apps (e.g. top 24), then stream remaining apps in background without blocking startup
            val priority = apps.take(24)
            val remaining = apps.drop(24)
            prioritizeIconPrewarm(priority, remaining)
          }

          runNext = hasPendingScan
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        AppLogger.e(AppLogger.Category.LAUNCHER, "Error scanning apps in ViewModel", e)
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = "Unable to discover installed applications. Tap to retry.",
            discoveryResult = DiscoveryResult.Failure(e)
          )
        }
      }
    }
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update { current ->
      val filtered = applyFiltersAndSort(current.allApps, query, current.activeFilter, current.sortMode)
      current.copy(searchQuery = query, filteredApps = filtered)
    }
  }

  fun onFilterChanged(filter: AppFilter) {
    _uiState.update { current ->
      val filtered = applyFiltersAndSort(current.allApps, current.searchQuery, filter, current.sortMode)
      current.copy(activeFilter = filter, filteredApps = filtered)
    }
  }

  fun onViewModeChanged(viewMode: AppViewMode) {
    _uiState.update { it.copy(viewMode = viewMode) }
  }

  fun onSortModeChanged(sortMode: AppSortMode) {
    _uiState.update { current ->
      val filtered = applyFiltersAndSort(current.allApps, current.searchQuery, current.activeFilter, sortMode)
      current.copy(sortMode = sortMode, filteredApps = filtered)
    }
  }

  fun getAppIcon(app: DiscoveredApp): Drawable? {
    return discoveryManager.loadAppIcon(app)
  }

  fun getCachedAppIconBitmap(app: DiscoveredApp): Bitmap? {
    return discoveryManager.getCachedAppIconBitmap(app)
  }

  suspend fun loadAppIconBitmapAsync(app: DiscoveredApp): Bitmap? {
    val cached = discoveryManager.getCachedAppIconBitmap(app)
    if (cached != null) return cached
    val loaded = discoveryManager.loadAppIconBitmapAsync(app)
    if (loaded != null) {
      _iconBitmaps.update { it + (app.id to loaded) }
    }
    return loaded
  }

  suspend fun getAppIconBitmapAsync(app: DiscoveredApp): Bitmap? {
    return loadAppIconBitmapAsync(app)
  }

  /**
   * Flow that yields the cached bitmap immediately if present, and updates once loaded
   * without requiring launcher-wide map recomposition.
   */
  fun getAppIconFlow(app: DiscoveredApp): kotlinx.coroutines.flow.Flow<Bitmap?> = kotlinx.coroutines.flow.flow {
    val cached = discoveryManager.getCachedAppIconBitmap(app)
    if (cached != null) {
      emit(cached)
    } else {
      emit(null)
      val loaded = loadAppIconBitmapAsync(app)
      emit(loaded)
    }
  }

  fun getAppIconBitmap(app: DiscoveredApp): Bitmap? {
    val stateBitmap = _iconBitmaps.value[app.id]
    if (stateBitmap != null) return stateBitmap

    val cached = discoveryManager.getCachedAppIconBitmap(app)
    if (cached != null) {
      _iconBitmaps.update { it + (app.id to cached) }
      return cached
    }
    viewModelScope.launch(Dispatchers.IO) {
      val loaded = discoveryManager.loadAppIconBitmapAsync(app)
      if (loaded != null) {
        _iconBitmaps.update { it + (app.id to loaded) }
      }
    }
    return null
  }

  /**
   * Prioritized icon prewarming: decodes high-priority apps (desktop, dock, recents, visible)
   * immediately, then yields to background coroutines to process the remaining apps.
   */
  fun prioritizeIconPrewarm(
    priorityApps: List<DiscoveredApp>,
    remainingApps: List<DiscoveredApp> = emptyList()
  ) {
    prewarmJob?.cancel()
    prewarmJob = viewModelScope.launch(Dispatchers.IO) {
      if (priorityApps.isNotEmpty()) {
        discoveryManager.prewarmIconCache(priorityApps)
        val prewarmed = priorityApps.mapNotNull { a ->
          discoveryManager.getCachedAppIconBitmap(a)?.let { a.id to it }
        }.toMap()
        if (prewarmed.isNotEmpty()) {
          _iconBitmaps.update { it + prewarmed }
        }
      }

      if (remainingApps.isNotEmpty()) {
        val nonPriority = remainingApps.filterNot { rem -> priorityApps.any { it.id == rem.id } }
        for (batch in nonPriority.chunked(12)) {
          ensureActive()
          discoveryManager.prewarmIconCache(batch)
          val batchBitmaps = batch.mapNotNull { a ->
            discoveryManager.getCachedAppIconBitmap(a)?.let { a.id to it }
          }.toMap()
          if (batchBitmaps.isNotEmpty()) {
            _iconBitmaps.update { it + batchBitmaps }
          }
          kotlinx.coroutines.yield()
        }
      }
    }
  }

  /**
   * Primary suspending launch implementation that dispatches application launch using launcher-aware platform APIs
   * off the UI thread within the caller lifecycle scope, handles failure gracefully, and records launch telemetry.
   */
  suspend fun launchAppSuspending(
    app: DiscoveredApp,
    spaceId: String = Space.DEFAULT_SPACE_ID,
    sourceBounds: Rect? = null
  ): LaunchResult {
    val result = launchManager.launchAppSuspending(app, spaceId, sourceBounds)

    val logEntry: String
    val feedbackMessage: String?

    when (result) {
      is LaunchResult.Success -> {
        logEntry = "SUCCESS: Launched ${app.label} (${result.packageName}) in space '$spaceId' via ${result.method}"
        feedbackMessage = null // Normal launch transition
      }
      is LaunchResult.Unavailable -> {
        logEntry = "UNAVAILABLE: ${app.label} (${result.packageName}) - ${result.reason}"
        feedbackMessage = "Unable to open ${app.label}: Application is unavailable."
      }
      is LaunchResult.Failed -> {
        logEntry = "FAILED: ${app.label} (${result.packageName}) - ${result.errorMessage}"
        feedbackMessage = "Unable to open ${app.label}."
      }
    }

    _uiState.update { current ->
      val updatedHistory = listOf(logEntry) + current.launchHistory.take(20)
      current.copy(
        recentLaunchLog = logEntry,
        launchHistory = updatedHistory
      )
    }

    if (feedbackMessage != null) {
      _userFeedback.tryEmit(feedbackMessage)
    }
    return result
  }

  /**
   * Dispatches application launch using launcher-aware platform APIs,
   * delegating to the lifecycle-owned suspending launch path.
   */
  fun launchApp(
    app: DiscoveredApp,
    spaceId: String = Space.DEFAULT_SPACE_ID,
    sourceBounds: Rect? = null
  ) {
    viewModelScope.launch {
      launchAppSuspending(app, spaceId, sourceBounds)
    }
  }

  suspend fun getMostUsedApps(
    spaceId: String = Space.DEFAULT_SPACE_ID,
    apps: List<DiscoveredApp>,
    limit: Int = 8
  ): List<DiscoveredApp> {
    return launchHistoryRepository.resolveMostUsedApps(spaceId, apps, limit)
  }

  fun openAppInfo(app: DiscoveredApp) {
    try {
      val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", app.packageName, null)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      getApplication<Application>().startActivity(intent)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to open App Info for ${app.packageName}", e)
    }
  }

  fun isUninstallable(app: DiscoveredApp): Boolean {
    return com.multispace.platform.PackageActionHelper.isPackageUninstallable(getApplication(), app)
  }

  fun uninstallApp(app: DiscoveredApp): Boolean {
    return com.multispace.platform.PackageActionHelper.launchUninstallConfirmation(getApplication(), app.packageName)
  }

  fun forceStopApp(app: DiscoveredApp): Boolean {
    val result = com.multispace.platform.PackageActionHelper.forceStopPackage(getApplication(), app.packageName)
    when (result) {
      is com.multispace.platform.PackageActionHelper.ForceStopResult.PrivilegedSuccess -> {
        _userFeedback.tryEmit("Force stopped ${app.label}")
      }
      is com.multispace.platform.PackageActionHelper.ForceStopResult.BackgroundProcessesKilled -> {
        _userFeedback.tryEmit("Killed background processes for ${app.label}. Full force-stop requires system/privileged access on Android.")
      }
      is com.multispace.platform.PackageActionHelper.ForceStopResult.Failure -> {
        _userFeedback.tryEmit("Unable to stop ${app.label}: ${result.errorMessage}")
      }
    }
    return result.isSuccessOrHandled
  }

  private fun applyFiltersAndSort(
    apps: List<DiscoveredApp>,
    query: String,
    filter: AppFilter,
    sortMode: AppSortMode
  ): List<DiscoveredApp> {
    var result = apps

    // Apply Filter
    result = when (filter) {
      AppFilter.ALL -> result
      AppFilter.USER_ONLY -> result.filter { !it.isSystemApp }
      AppFilter.SYSTEM_ONLY -> result.filter { it.isSystemApp }
    }

    // Apply Search
    if (query.isNotBlank()) {
      val trimmed = query.trim()
      result = result.filter {
        it.label.contains(trimmed, ignoreCase = true) ||
          it.packageName.contains(trimmed, ignoreCase = true)
      }
    }

    // Apply Sort
    result = when (sortMode) {
      AppSortMode.NAME_ASC -> result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
      AppSortMode.NAME_DESC -> result.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.label })
      AppSortMode.RECENTLY_UPDATED -> result.sortedByDescending { it.lastUpdateTimeMillis }
    }

    return result
  }

  override fun onCleared() {
    super.onCleared()
    activeScanJob?.cancel()
    prewarmJob?.cancel()
    discoveryManager.stopMonitoring()
    AppLogger.d(AppLogger.Category.LAUNCHER, "AppDiscoveryViewModel cleared")
  }
}

