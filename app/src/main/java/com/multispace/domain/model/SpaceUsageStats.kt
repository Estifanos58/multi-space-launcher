package com.multispace.domain.model

/**
 * Represents the launch count and details for an app within a Space.
 */
data class AppLaunchCount(
  val app: DiscoveredApp,
  val launchCount: Int,
  val lastLaunched: Long = 0L
)

/**
 * Represents the launch count for a specific calendar day.
 */
data class DailyUsage(
  val dayLabel: String,
  val dateMillis: Long,
  val count: Int,
  val isToday: Boolean = false
)

/**
 * Aggregated usage statistics for an entire Space.
 */
data class SpaceUsageStats(
  val spaceId: String,
  val totalLaunches: Int,
  val uniqueAppsCount: Int,
  val mostRecentlyLaunchedApp: DiscoveredApp?,
  val mostUsedApp: DiscoveredApp?,
  val launchesToday: Int,
  val launchesLast7Days: Int,
  val launchesLast30Days: Int,
  val topApps: List<AppLaunchCount> = emptyList(),
  val weeklyLaunches: List<DailyUsage> = emptyList()
)

/**
 * Specific launch and usage statistics for an individual AppIdentity within a Space.
 */
data class AppUsageStats(
  val spaceId: String,
  val appIdentity: AppIdentity,
  val app: DiscoveredApp?,
  val launchCount: Int,
  val firstLaunchTimestamp: Long?,
  val lastLaunchTimestamp: Long?,
  val launchesToday: Int,
  val launchesLast7Days: Int,
  val launchesLast30Days: Int
)
