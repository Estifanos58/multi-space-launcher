package com.multispace.data.repository

import android.content.Context
import com.multispace.data.dao.LaunchHistoryDao
import com.multispace.data.database.LaunchHistoryDatabase
import com.multispace.data.entity.LaunchEventEntity
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppIdentityLookup
import com.multispace.domain.model.AppLaunchCount
import com.multispace.domain.model.AppUsageStats
import com.multispace.domain.model.DailyUsage
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.LaunchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

  override fun getTopMostUsedAppsWithCountFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int
  ): Flow<List<AppLaunchCount>> {
    return combine(
      launchHistoryDao.getMostUsedLaunchesFlow(spaceId, limit * 3),
      installedAppsFlow
    ) { summaries, installed ->
      val lookup = AppIdentityLookup(installed)
      val seen = mutableSetOf<AppIdentity>()
      val resolved = mutableListOf<AppLaunchCount>()
      for (summary in summaries) {
        val app = lookup[summary.appIdentity]
        if (app != null && seen.add(app.appIdentity)) {
          resolved.add(
            AppLaunchCount(
              app = app,
              launchCount = summary.launchCount,
              lastLaunched = summary.lastLaunched
            )
          )
        }
      }
      resolved.sortedWith(
        compareByDescending<AppLaunchCount> { it.launchCount }
          .thenByDescending { it.lastLaunched }
          .thenBy(String.CASE_INSENSITIVE_ORDER) { it.app.label }
      ).take(limit)
    }.distinctUntilChanged()
  }

  override suspend fun getTopMostUsedAppsWithCount(
    spaceId: String,
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): List<AppLaunchCount> {
    val summaries = launchHistoryDao.getMostUsedLaunchesSync(spaceId, limit * 3)
    val lookup = AppIdentityLookup(installedApps)
    val seen = mutableSetOf<AppIdentity>()
    val resolved = mutableListOf<AppLaunchCount>()
    for (summary in summaries) {
      val app = lookup[summary.appIdentity]
      if (app != null && seen.add(app.appIdentity)) {
        resolved.add(
          AppLaunchCount(
            app = app,
            launchCount = summary.launchCount,
            lastLaunched = summary.lastLaunched
          )
        )
      }
    }
    return resolved.sortedWith(
      compareByDescending<AppLaunchCount> { it.launchCount }
        .thenByDescending { it.lastLaunched }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.app.label }
    ).take(limit)
  }

  override fun getDailyLaunchCountsLast7DaysFlow(
    spaceId: String,
    nowProvider: () -> Long
  ): Flow<List<DailyUsage>> {
    val now = nowProvider()
    val sevenDaysAgoStart = calculateStartOfDaysAgo(now, 6)
    return launchHistoryDao.getLaunchTimestampsSinceFlow(spaceId, sevenDaysAgoStart)
      .map { timestamps ->
        calculate7CalendarDaysUsage(timestamps, nowProvider())
      }.distinctUntilChanged()
  }

  override suspend fun getDailyLaunchCountsLast7Days(
    spaceId: String,
    now: Long
  ): List<DailyUsage> {
    val sevenDaysAgoStart = calculateStartOfDaysAgo(now, 6)
    val timestamps = launchHistoryDao.getLaunchTimestampsSinceSync(spaceId, sevenDaysAgoStart)
    return calculate7CalendarDaysUsage(timestamps, now)
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
    val topApps = getTopMostUsedAppsWithCount(spaceId, installedApps, 3)
    val weeklyLaunches = getDailyLaunchCountsLast7Days(spaceId, now)

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
      launchesLast30Days = launchesLast30Days,
      topApps = topApps,
      weeklyLaunches = weeklyLaunches
    )
  }

  override fun getSpaceUsageStatsFlow(
    spaceId: String,
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    nowProvider: () -> Long
  ): Flow<SpaceUsageStats> {
    val now = nowProvider()
    val sevenDaysAgoStart = calculateStartOfDaysAgo(now, 6)
    return combine(
      launchHistoryDao.getTotalLaunchCountFlow(spaceId),
      launchHistoryDao.getMostUsedLaunchesFlow(spaceId, 100),
      launchHistoryDao.getLaunchTimestampsSinceFlow(spaceId, sevenDaysAgoStart),
      installedAppsFlow
    ) { totalLaunches, summaries, timestamps7Days, installed ->
      val currentNow = nowProvider()
      val todayStart = calculateDayStart(currentNow)
      val sevenDaysAgo = currentNow - (7L * 24 * 60 * 60 * 1000L)
      val thirtyDaysAgo = currentNow - (30L * 24 * 60 * 60 * 1000L)

      val lookup = AppIdentityLookup(installed)
      val installedIdentities = installed.map { it.appIdentity }.toSet()
      val activeInstalledSummaries = summaries.filter { it.appIdentity in installedIdentities }
      val effectiveUnique = if (installed.isNotEmpty()) activeInstalledSummaries.size else launchHistoryDao.getUniqueAppsCount(spaceId)

      val launchesToday = launchHistoryDao.getLaunchCountSince(spaceId, todayStart)
      val launchesLast7Days = launchHistoryDao.getLaunchCountSince(spaceId, sevenDaysAgo)
      val launchesLast30Days = launchHistoryDao.getLaunchCountSince(spaceId, thirtyDaysAgo)

      val mostRecent = resolveRecentApps(spaceId, installed, 1).firstOrNull()
      val mostUsed = resolveMostUsedApps(spaceId, installed, 1).firstOrNull()

      val seen = mutableSetOf<AppIdentity>()
      val resolvedTop = mutableListOf<AppLaunchCount>()
      for (summary in summaries) {
        val app = lookup[summary.appIdentity]
        if (app != null && seen.add(app.appIdentity)) {
          resolvedTop.add(
            AppLaunchCount(
              app = app,
              launchCount = summary.launchCount,
              lastLaunched = summary.lastLaunched
            )
          )
        }
      }
      val top3 = resolvedTop.sortedWith(
        compareByDescending<AppLaunchCount> { it.launchCount }
          .thenByDescending { it.lastLaunched }
          .thenBy(String.CASE_INSENSITIVE_ORDER) { it.app.label }
      ).take(3)

      val weekly = calculate7CalendarDaysUsage(timestamps7Days, currentNow)

      SpaceUsageStats(
        spaceId = spaceId,
        totalLaunches = totalLaunches,
        uniqueAppsCount = effectiveUnique,
        mostRecentlyLaunchedApp = mostRecent,
        mostUsedApp = mostUsed,
        launchesToday = launchesToday,
        launchesLast7Days = launchesLast7Days,
        launchesLast30Days = launchesLast30Days,
        topApps = top3,
        weeklyLaunches = weekly
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

  private fun calculateStartOfDaysAgo(now: Long, daysAgo: Int): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.DAY_OF_YEAR, -daysAgo)
    }
    return cal.timeInMillis
  }

  private fun calculate7CalendarDaysUsage(timestamps: List<Long>, now: Long): List<DailyUsage> {
    val cal = Calendar.getInstance().apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val todayStart = cal.timeInMillis
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    val result = mutableListOf<DailyUsage>()
    // Start from 6 days ago up to today (i = 0 is 6 days ago, i = 6 is today)
    cal.add(Calendar.DAY_OF_YEAR, -6)
    for (i in 0 until 7) {
      val dayStart = cal.timeInMillis
      val label = dayFormat.format(cal.time)
      cal.add(Calendar.DAY_OF_YEAR, 1)
      val dayEnd = cal.timeInMillis
      val count = timestamps.count { it in dayStart until dayEnd }
      result.add(
        DailyUsage(
          dayLabel = label,
          dateMillis = dayStart,
          count = count,
          isToday = (dayStart == todayStart)
        )
      )
    }
    return result
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
