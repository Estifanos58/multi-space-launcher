package com.multispace.platform

import android.content.Context
import android.content.SharedPreferences
import com.multispace.domain.model.DiscoveredApp

/**
 * Tracks and persists application launch counts and timestamps to provide
 * accurate usage frequency ordering for launcher components like the
 * Layer 2 Most Used Apps section.
 */
class AppUsageTracker private constructor(context: Context) {

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("multispace_app_usage", Context.MODE_PRIVATE)

  companion object {
    @Volatile
    private var INSTANCE: AppUsageTracker? = null

    fun getInstance(context: Context): AppUsageTracker {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: AppUsageTracker(context).also { INSTANCE = it }
      }
    }
  }

  /**
   * Increments the launch count for the specified application and records the current timestamp.
   */
  fun recordLaunch(app: DiscoveredApp) {
    val key = app.packageName
    val currentCount = prefs.getInt("launch_count_$key", 0)
    prefs.edit()
      .putInt("launch_count_$key", currentCount + 1)
      .putLong("last_launch_$key", System.currentTimeMillis())
      .apply()
  }

  /**
   * Returns the recorded launch count for the specified application.
   */
  fun getLaunchCount(app: DiscoveredApp): Int {
    return prefs.getInt("launch_count_${app.packageName}", 0)
  }

  /**
   * Returns the epoch timestamp in milliseconds when the application was last launched.
   */
  fun getLastLaunchTime(app: DiscoveredApp): Long {
    return prefs.getLong("last_launch_${app.packageName}", 0L)
  }

  /**
   * Returns the given list of apps ordered by usage frequency:
   * 1. Launch count (descending)
   * 2. Last launch timestamp (descending)
   * 3. Last update time / install time (descending)
   * 4. App label (alphabetical case-insensitive)
   */
  fun getMostUsedApps(apps: List<DiscoveredApp>, limit: Int = 8): List<DiscoveredApp> {
    if (apps.isEmpty()) return emptyList()

    return apps.sortedWith(
      compareByDescending<DiscoveredApp> { getLaunchCount(it) }
        .thenByDescending { getLastLaunchTime(it) }
        .thenByDescending { it.lastUpdateTimeMillis }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    ).take(limit)
  }
}
