package com.multispace.domain.model

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
  val launchesLast30Days: Int
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
