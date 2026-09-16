package com.multispace.data.repository

import android.content.Context
import com.multispace.data.dao.LaunchHistoryDao
import com.multispace.data.database.LaunchHistoryDatabase
import com.multispace.data.entity.LaunchEventEntity
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppIdentityLookup
import com.multispace.domain.model.AppUsageStats
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.LaunchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Room-backed implementation of [LaunchHistoryRepository] tracking Space-specific launch events.
 *
 * All operations are strictly scoped to (spaceId + AppIdentity).
 */
class RoomLaunchHistoryRepository(
  private val launchHistoryDao: LaunchHistoryDao,
  private val maxEventsPerSpace: Int = 5000
) : LaunchHistoryRepository {

  override suspend fun recordLaunch(
    spaceId: String,
    identity: AppIdentity,
    timestamp: Long
  ) {
    val event = LaunchEventEntity(
      spaceId = spaceId,
      packageName = identity.packageName,
      componentName = identity.componentName,
      userHandleId = identity.userHandleId,
      timestamp = timestamp
    )
    launchHistoryDao.insertEvent(event)
    // Keep space events bounded
    launchHistoryDao.pruneOldEvents(spaceId, maxEventsPerSpace)
  }

  override fun getRecentIdentities(spaceId: String, limit: Int): Flow<List<AppIdentity>> {
    return launchHistoryDao.getRecentLaunchesFlow(spaceId, limit)
      .map { summaries -> summaries.map { it.appIdentity } }
      .distinctUntilChanged()
  }

  override suspend fun getRecentIdentitiesSync(spaceId: String, limit: Int): List<AppIdentity> {
    return launchHistoryDao.getRecentLaunchesSync(spaceId, limit).map { it.appIdentity }
  }

  override fun getRecentApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): Flow<List<DiscoveredApp>> {
    return getRecentAppsFlow(spaceId, flowOf(installedApps), limit)
  }

  override fun getRecentAppsFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int
  ): Flow<List<DiscoveredApp>> {
    return combine(
      launchHistoryDao.getRecentLaunchesFlow(spaceId, limit * 2),
      installedAppsFlow
    ) { summaries, installed ->
      val lookup = AppIdentityLookup(installed)
      val seen = mutableSetOf<AppIdentity>()
      val resolved = mutableListOf<DiscoveredApp>()

      for (summary in summaries) {
        val app = lookup[summary.appIdentity]
        if (app != null && seen.add(app.appIdentity)) {
          resolved.add(app)
          if (resolved.size >= limit) break
        }
      }
      resolved
    }.distinctUntilChanged()
  }

  override suspend fun resolveRecentApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): List<DiscoveredApp> {
    val summaries = launchHistoryDao.getRecentLaunchesSync(spaceId, limit * 2)
    val lookup = AppIdentityLookup(installedApps)
    val seen = mutableSetOf<AppIdentity>()
    val resolved = mutableListOf<DiscoveredApp>()

    for (summary in summaries) {
      val app = lookup[summary.appIdentity]
      if (app != null && seen.add(app.appIdentity)) {
        resolved.add(app)
        if (resolved.size >= limit) break
      }
    }
    return resolved
  }

  override fun getMostUsedApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): Flow<List<DiscoveredApp>> {
    return getMostUsedAppsFlow(spaceId, flowOf(installedApps), limit)
  }

  override fun getMostUsedAppsFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int
  ): Flow<List<DiscoveredApp>> {
    return combine(
      launchHistoryDao.getMostUsedLaunchesFlow(spaceId, limit * 3),
      installedAppsFlow
    ) { summaries, installed ->
      val lookup = AppIdentityLookup(installed)
      val seen = mutableSetOf<AppIdentity>()
      val resolvedWithStats = mutableListOf<Triple<DiscoveredApp, Int, Long>>()

      for (summary in summaries) {
        val app = lookup[summary.appIdentity]
        if (app != null && seen.add(app.appIdentity)) {
          resolvedWithStats.add(Triple(app, summary.launchCount, summary.lastLaunched))
        }
      }

      // Ordering:
      // 1. Launch count descending
      // 2. Last launch timestamp descending
      // 3. Label alphabetical (case-insensitive) fallback
      resolvedWithStats.sortedWith(
        compareByDescending<Triple<DiscoveredApp, Int, Long>> { it.second }
          .thenByDescending { it.third }
          .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first.label }
      ).map { it.first }.take(limit)
    }.distinctUntilChanged()
  }

  override suspend fun resolveMostUsedApps(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): List<DiscoveredApp> {
    val summaries = launchHistoryDao.getMostUsedLaunchesSync(spaceId, limit * 3)
    val lookup = AppIdentityLookup(installedApps)
    val seen = mutableSetOf<AppIdentity>()
    val resolvedWithStats = mutableListOf<Triple<DiscoveredApp, Int, Long>>()

    for (summary in summaries) {
      val app = lookup[summary.appIdentity]
      if (app != null && seen.add(app.appIdentity)) {
        resolvedWithStats.add(Triple(app, summary.launchCount, summary.lastLaunched))
      }
    }

    return resolvedWithStats.sortedWith(
      compareByDescending<Triple<DiscoveredApp, Int, Long>> { it.second }
        .thenByDescending { it.third }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first.label }
    ).map { it.first }.take(limit)
  }

  override suspend fun getSpaceUsageStats(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    now: Long
  ): SpaceUsageStats {
    val totalLaunches = launchHistoryDao.getTotalLaunchCount(spaceId)
    val uniqueAppsCount = launchHistoryDao.getUniqueAppsCount(spaceId)

    val todayStart = calculateDayStart(now)
    val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000L)
    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000L)

    val launchesToday = launchHistoryDao.getLaunchCountSince(spaceId, todayStart)
    val launchesLast7Days = launchHistoryDao.getLaunchCountSince(spaceId, sevenDaysAgo)
    val launchesLast30Days = launchHistoryDao.getLaunchCountSince(spaceId, thirtyDaysAgo)

    val mostRecent = resolveRecentApps(spaceId, installedApps, 1).firstOrNull()
    val mostUsed = resolveMostUsedApps(spaceId, installedApps, 1).firstOrNull()

    val installedIdentities = installedApps.map { it.appIdentity }.toSet()
    val effectiveUniqueApps = if (installedApps.isNotEmpty()) {
      val summaries = launchHistoryDao.getMostUsedLaunchesSync(spaceId, 1000)
      summaries.count { it.appIdentity in installedIdentities }
    } else {
      uniqueAppsCount
    }

    return SpaceUsageStats(
      spaceId = spaceId,
      totalLaunches = totalLaunches,
      uniqueAppsCount = effectiveUniqueApps,
      mostRecentlyLaunchedApp = mostRecent,
      mostUsedApp = mostUsed,
      launchesToday = launchesToday,
      launchesLast7Days = launchesLast7Days,
      launchesLast30Days = launchesLast30Days
    )
  }

  override fun getSpaceUsageStatsFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    nowProvider: () -> Long
  ): Flow<SpaceUsageStats> {
    return combine(
      launchHistoryDao.getTotalLaunchCountFlow(spaceId),
      launchHistoryDao.getMostUsedLaunchesFlow(spaceId, 100),
      installedAppsFlow
    ) { totalLaunches, summaries, installed ->
      val now = nowProvider()
      val todayStart = calculateDayStart(now)
      val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000L)
      val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000L)

      val installedIdentities = installed.map { it.appIdentity }.toSet()
      val activeInstalledSummaries = summaries.filter { it.appIdentity in installedIdentities }
      val effectiveUnique = if (installed.isNotEmpty()) activeInstalledSummaries.size else launchHistoryDao.getUniqueAppsCount(spaceId)

      val launchesToday = launchHistoryDao.getLaunchCountSince(spaceId, todayStart)
      val launchesLast7Days = launchHistoryDao.getLaunchCountSince(spaceId, sevenDaysAgo)
      val launchesLast30Days = launchHistoryDao.getLaunchCountSince(spaceId, thirtyDaysAgo)

      val mostRecent = resolveRecentApps(spaceId, installed, 1).firstOrNull()
      val mostUsed = resolveMostUsedApps(spaceId, installed, 1).firstOrNull()

      SpaceUsageStats(
        spaceId = spaceId,
        totalLaunches = totalLaunches,
        uniqueAppsCount = effectiveUnique,
        mostRecentlyLaunchedApp = mostRecent,
        mostUsedApp = mostUsed,
        launchesToday = launchesToday,
        launchesLast7Days = launchesLast7Days,
        launchesLast30Days = launchesLast30Days
      )
    }.distinctUntilChanged()
  }

  override suspend fun getAppUsageStats(
    spaceId: String,
    identity: AppIdentity,
    app: DiscoveredApp?,
    now: Long
  ): AppUsageStats {
    val launchCount = launchHistoryDao.getAppLaunchCount(
      spaceId = spaceId,
      packageName = identity.packageName,
      componentName = identity.componentName,
      userHandleId = identity.userHandleId
    )
    val firstLaunch = launchHistoryDao.getAppFirstLaunch(
      spaceId = spaceId,
      packageName = identity.packageName,
      componentName = identity.componentName,
      userHandleId = identity.userHandleId
    )
    val lastLaunch = launchHistoryDao.getAppLastLaunch(
      spaceId = spaceId,
      packageName = identity.packageName,
      componentName = identity.componentName,
      userHandleId = identity.userHandleId
    )

    val todayStart = calculateDayStart(now)
    val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000L)
    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000L)

    val launchesToday = launchHistoryDao.getAppLaunchCountSince(
      spaceId = spaceId,
      packageName = identity.packageName,
      componentName = identity.componentName,
      userHandleId = identity.userHandleId,
      sinceTimestamp = todayStart
    )
    val launchesLast7Days = launchHistoryDao.getAppLaunchCountSince(
      spaceId = spaceId,
      packageName = identity.packageName,
      componentName = identity.componentName,
      userHandleId = identity.userHandleId,
      sinceTimestamp = sevenDaysAgo
    )
    val launchesLast30Days = launchHistoryDao.getAppLaunchCountSince(
      spaceId = spaceId,
      packageName = identity.packageName,
      componentName = identity.componentName,
      userHandleId = identity.userHandleId,
      sinceTimestamp = thirtyDaysAgo
    )

    return AppUsageStats(
      spaceId = spaceId,
      appIdentity = identity,
      app = app,
      launchCount = launchCount,
      firstLaunchTimestamp = firstLaunch,
      lastLaunchTimestamp = lastLaunch,
      launchesToday = launchesToday,
      launchesLast7Days = launchesLast7Days,
      launchesLast30Days = launchesLast30Days
    )
  }

  override suspend fun clearHistoryForSpace(spaceId: String) {
    launchHistoryDao.clearSpaceHistory(spaceId)
  }

  override suspend fun clearAll() {
    launchHistoryDao.clearAll()
  }

  private fun calculateDayStart(timeMs: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = timeMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  companion object {
    @Volatile
    private var INSTANCE: RoomLaunchHistoryRepository? = null

    fun getInstance(context: Context): RoomLaunchHistoryRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: RoomLaunchHistoryRepository(
          LaunchHistoryDatabase.getInstance(context).launchHistoryDao()
        ).also { INSTANCE = it }
      }
    }
  }
}
