package com.multispace.presentation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multispace.data.repository.RoomLaunchHistoryRepository
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppUsageStats
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.domain.repository.LaunchHistoryRepository
import com.multispace.platform.AppDiscoveryManager
import com.multispace.platform.AppLaunchManager
import com.multispace.platform.LaunchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
  val launchHistory: List<String> = emptyList()
)

class AppDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

  private val discoveryManager = AppDiscoveryManager(application.applicationContext)
  private val launchManager = AppLaunchManager(
    context = application.applicationContext,
    coroutineScope = viewModelScope
  )
  val launchHistoryRepository: LaunchHistoryRepository = launchManager.historyRepository

  private val _uiState = MutableStateFlow(AppDiscoveryUiState())
  val uiState: StateFlow<AppDiscoveryUiState> = _uiState.asStateFlow()

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

  @OptIn(FlowPreview::class)
  private fun observePackageEvents() {
    // 1. Immediate UI feedback for telemetry / diagnostic view
    viewModelScope.launch {
      discoveryManager.packageEvents.collect { event ->
        val eventDescription = when (event) {
          is AppDiscoveryManager.PackageEvent.Added -> "Package Added: ${event.packageName}"
          is AppDiscoveryManager.PackageEvent.Removed -> "Package Removed: ${event.packageName}"
          is AppDiscoveryManager.PackageEvent.Changed -> "Package Changed: ${event.packageName}"
          is AppDiscoveryManager.PackageEvent.Refreshed -> "Packages Refreshed: ${event.count} packages"
        }
        _uiState.update { it.copy(recentPackageEvent = eventDescription) }
      }
    }

    // 2. Debounced scan trigger so rapid package callbacks coalesce into a single refresh
    viewModelScope.launch {
      discoveryManager.packageEvents
        .debounce(400L)
        .collect {
          loadApps(isSilent = true)
        }
    }
  }

  /**
   * Discovers installed applications.
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

          val apps = discoveryManager.loadInstalledApps()
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
              lastScannedTime = System.currentTimeMillis()
            )
          }

          // Cancel any previous prewarm job to prevent accumulation
          prewarmJob?.cancel()
          prewarmJob = viewModelScope.launch(Dispatchers.IO) {
            discoveryManager.prewarmIconCache(apps)
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
            errorMessage = "Unable to discover installed applications. Tap to retry."
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

  fun getAppIconBitmap(app: DiscoveredApp): Bitmap? {
    return discoveryManager.loadAppIconBitmap(app)
  }

  /**
   * Dispatches application launch using launcher-aware platform APIs,
   * handles failure gracefully, and records launch telemetry.
   */
  fun launchApp(
    app: DiscoveredApp,
    spaceId: String = Space.DEFAULT_SPACE_ID,
    sourceBounds: Rect? = null
  ) {
    val result = launchManager.launchApp(app, spaceId, sourceBounds, callerScope = viewModelScope)

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

