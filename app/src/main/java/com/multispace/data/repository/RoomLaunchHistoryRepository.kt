package com.multispace.data.repository

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.multispace.data.dao.LaunchHistoryDao
import com.multispace.data.database.LaunchHistoryDatabase
import com.multispace.data.entity.LaunchHistoryEntity
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppIdentityLookup
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.LaunchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Room-backed implementation of [LaunchHistoryRepository].
 *
 * Enforces a bounded history of up to [maxCapacity] (default 20) entries,
 * strictly identifies apps by (packageName, componentName, userHandleId),
 * moves re-launched apps to the front without duplication, and automatically
 * prunes the oldest entries.
 */
class RoomLaunchHistoryRepository(
  private val dao: LaunchHistoryDao,
  private val maxCapacity: Int = 20
) : LaunchHistoryRepository {

  private val mutex = Mutex()

  companion object {
    @Volatile
    private var INSTANCE: RoomLaunchHistoryRepository? = null

    fun getInstance(context: Context, maxCapacity: Int = 20): RoomLaunchHistoryRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: RoomLaunchHistoryRepository(
          dao = LaunchHistoryDatabase.getInstance(context).launchHistoryDao(),
          maxCapacity = maxCapacity
        ).also { INSTANCE = it }
      }
    }

    @VisibleForTesting
    fun resetInstance() {
      synchronized(this) {
        INSTANCE = null
      }
    }
  }

  override suspend fun recordLaunch(identity: AppIdentity, timestamp: Long) {
    if (identity.packageName.isBlank()) return

    mutex.withLock {
      val existing = dao.findEntry(
        packageName = identity.packageName,
        componentName = identity.componentName,
        userHandleId = identity.userHandleId
      )

      if (existing != null) {
        dao.updateLaunchTimestamp(existing.id, timestamp)
      } else {
        dao.insertOrUpdate(
          LaunchHistoryEntity(
            packageName = identity.packageName,
            componentName = identity.componentName,
            userHandleId = identity.userHandleId,
            lastLaunchedAt = timestamp
          )
        )
      }

      // Automatically prune oldest entries beyond maxCapacity
      val ids = dao.getAllIdsOrdered()
      if (ids.size > maxCapacity) {
        dao.deleteByIds(ids.drop(maxCapacity))
      }
    }
  }

  override fun getRecentIdentities(limit: Int): Flow<List<AppIdentity>> {
    val boundedLimit = limit.coerceIn(1, maxCapacity)
    return dao.getRecentLaunchesFlow(boundedLimit).map { list ->
      list.map { it.appIdentity }
    }
  }

  override suspend fun getRecentIdentitiesSync(limit: Int): List<AppIdentity> {
    val boundedLimit = limit.coerceIn(1, maxCapacity)
    return dao.getRecentLaunchesSync(boundedLimit).map { it.appIdentity }
  }

  override fun getRecentApps(
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): Flow<List<DiscoveredApp>> {
    val boundedLimit = limit.coerceIn(1, maxCapacity)
    return getRecentIdentities(boundedLimit).map { identities ->
      resolveIdentities(identities, installedApps, boundedLimit)
    }
  }

  override fun getRecentAppsFlow(
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int
  ): Flow<List<DiscoveredApp>> {
    val boundedLimit = limit.coerceIn(1, maxCapacity)
    return combine(getRecentIdentities(boundedLimit), installedAppsFlow) { identities, installedApps ->
      resolveIdentities(identities, installedApps, boundedLimit)
    }
  }

  override suspend fun resolveRecentApps(
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): List<DiscoveredApp> {
    val boundedLimit = limit.coerceIn(1, maxCapacity)
    val identities = getRecentIdentitiesSync(boundedLimit)
    return resolveIdentities(identities, installedApps, boundedLimit)
  }

  private fun resolveIdentities(
    identities: List<AppIdentity>,
    installedApps: List<DiscoveredApp>,
    limit: Int
  ): List<DiscoveredApp> {
    if (identities.isEmpty() || installedApps.isEmpty()) return emptyList()

    val lookup = AppIdentityLookup(installedApps)
    val seen = mutableSetOf<AppIdentity>()
    val result = ArrayList<DiscoveredApp>(identities.size)

    for (identity in identities) {
      val resolvedApp = lookup[identity]
      if (resolvedApp != null && seen.add(resolvedApp.appIdentity)) {
        result.add(resolvedApp)
        if (result.size >= limit) break
      }
    }
    return result
  }

  override suspend fun clearHistory() {
    mutex.withLock {
      dao.clearAll()
    }
  }
}
