package com.multispace.presentation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp
import com.multispace.platform.AppDiscoveryManager
import com.multispace.platform.AppLaunchManager
import com.multispace.platform.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val launchManager = AppLaunchManager(application.applicationContext)

  private val _uiState = MutableStateFlow(AppDiscoveryUiState())
  val uiState: StateFlow<AppDiscoveryUiState> = _uiState.asStateFlow()

  private val _userFeedback = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

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
          is AppDiscoveryManager.PackageEvent.Added -> "Package Added: ${event.packageName}"
          is AppDiscoveryManager.PackageEvent.Removed -> "Package Removed: ${event.packageName}"
          is AppDiscoveryManager.PackageEvent.Changed -> "Package Changed: ${event.packageName}"
          is AppDiscoveryManager.PackageEvent.Refreshed -> "Packages Refreshed: ${event.count} packages"
        }
        _uiState.update { it.copy(recentPackageEvent = eventDescription) }
        // Auto-refresh list upon package changes
        loadApps(isSilent = true)
      }
    }
  }

  fun loadApps(isSilent: Boolean = false) {
    viewModelScope.launch {
      if (!isSilent) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      }
      try {
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

        // Asynchronously pre-warm in-memory bitmap cache on IO dispatcher
        // so scrolling in Home and App Catalog hits RAM cache with zero UI-thread latency
        viewModelScope.launch(Dispatchers.IO) {
          discoveryManager.prewarmIconCache(apps)
        }
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
  fun launchApp(app: DiscoveredApp, sourceBounds: Rect? = null) {
    val result = launchManager.launchApp(app, sourceBounds)

    val logEntry: String
    val feedbackMessage: String?

    when (result) {
      is LaunchResult.Success -> {
        logEntry = "SUCCESS: Launched ${app.label} (${result.packageName}) via ${result.method}"
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
    discoveryManager.stopMonitoring()
    AppLogger.d(AppLogger.Category.LAUNCHER, "AppDiscoveryViewModel cleared")
  }
}

