package com.multispace.domain.repository

import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppUsageStats
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceUsageStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository managing Space-specific application launch events, recency ordering,
 * frequency ordering (most used), and usage statistics.
 *
 * Core principles:
 * - Identity is strictly defined by Space ID + AppIdentity (packageName + componentName + userHandleId).
 * - Usage in one Space NEVER affects another Space.
 * - Profile isolation: Personal and Work profile instances are tracked separately.
 * - Re-launches in the same Space move the app to the front without duplication in recency.
 * - Most used apps are ordered by:
 *   1. Launch count descending
 *   2. Last launch timestamp descending
 *   3. Label alphabetical fallback
 * - Safe resolution: Stale/uninstalled apps are safely excluded.
 */
interface LaunchHistoryRepository {

  /**
   * Records a successful application launch event in [spaceId].
   */
  suspend fun recordLaunch(
    spaceId: String,
    identity: AppIdentity,
    timestamp: Long = System.currentTimeMillis()
  )

  /**
   * Returns a reactive flow of recent [AppIdentity] items for [spaceId], ordered newest to oldest.
   */
  fun getRecentIdentities(spaceId: String, limit: Int = 20): Flow<List<AppIdentity>>

  /**
   * Synchronous list of recent [AppIdentity] items for [spaceId], ordered newest to oldest.
   */
  suspend fun getRecentIdentitiesSync(spaceId: String, limit: Int = 20): List<AppIdentity>

  /**
   * Resolves recent app identities for [spaceId] against a static collection of installed apps.
   */
  fun getRecentApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int = 20
  ): Flow<List<DiscoveredApp>>

  /**
   * Resolves recent app identities for [spaceId] reactively against an installed apps flow.
   */
  fun getRecentAppsFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int = 20
  ): Flow<List<DiscoveredApp>>

  /**
   * Suspend helper to resolve recent apps in [spaceId].
   */
  suspend fun resolveRecentApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int = 20
  ): List<DiscoveredApp>

  /**
   * Returns a reactive flow of most used apps in [spaceId].
   * Ordered by: launch count desc, last launch desc, alphabetical label.
   */
  fun getMostUsedApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int = 20
  ): Flow<List<DiscoveredApp>>

  /**
   * Reactive flow of most used apps in [spaceId] against an installed apps flow.
   */
  fun getMostUsedAppsFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int = 20
  ): Flow<List<DiscoveredApp>>

  /**
   * Suspend helper to resolve most used apps in [spaceId].
   */
  suspend fun resolveMostUsedApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int = 20
  ): List<DiscoveredApp>

  /**
   * Returns a reactive flow of top most used apps with their launch counts for [spaceId].
   */
  fun getTopMostUsedAppsWithCountFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int = 3
  ): Flow<List<com.multispace.domain.model.AppLaunchCount>>

  /**
   * Suspend helper to get top most used apps with their launch counts for [spaceId].
   */
  suspend fun getTopMostUsedAppsWithCount(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int = 3
  ): List<com.multispace.domain.model.AppLaunchCount>

  /**
   * Returns a reactive flow of daily launch counts for the last 7 days ending today.
   */
  fun getDailyLaunchCountsLast7DaysFlow(
    spaceId: String,
    nowProvider: () -> Long = { System.currentTimeMillis() }
  ): Flow<List<com.multispace.domain.model.DailyUsage>>

  /**
   * Suspend helper to get daily launch counts for the last 7 days ending today.
   */
  suspend fun getDailyLaunchCountsLast7Days(
    spaceId: String,
    now: Long = System.currentTimeMillis()
  ): List<com.multispace.domain.model.DailyUsage>

  /**
   * Computes overall usage statistics for [spaceId].
   */
  suspend fun getSpaceUsageStats(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    now: Long = System.currentTimeMillis()
  ): SpaceUsageStats

  /**
   * Returns a reactive flow of overall usage statistics for [spaceId].
   */
  fun getSpaceUsageStatsFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    nowProvider: () -> Long = { System.currentTimeMillis() }
  ): Flow<SpaceUsageStats>

  /**
   * Computes detailed usage statistics for an individual [identity] within [spaceId].
   */
  suspend fun getAppUsageStats(
    spaceId: String,
    identity: AppIdentity,
    app: DiscoveredApp? = null,
    now: Long = System.currentTimeMillis()
  ): AppUsageStats

  /**
   * Clears usage events for a specific Space.
   */
  suspend fun clearHistoryForSpace(spaceId: String)

  /**
   * Clears all recorded usage events across all spaces.
   */
  suspend fun clearAll()
}
