package com.multispace.domain.repository

import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing persistent launcher-owned application launch history.
 *
 * Core principles:
 * - Identifies apps strictly by AppIdentity (packageName + componentName + userHandleId).
 * - Most recently launched apps appear first.
 * - Duplicate launches move the existing entry to the newest position without duplication.
 * - Bounded to a maximum number of recent entries (default 20).
 * - Profile isolation: Personal and Work copies of the same package remain separate.
 * - Resolves safely against discovered apps, excluding stale/uninstalled apps without crashing.
 */
interface LaunchHistoryRepository {

  /**
   * Records a successful application launch.
   * If the [identity] already exists, moves it to the newest position with updated timestamp.
   * Automatically prunes older entries beyond max capacity.
   */
  suspend fun recordLaunch(identity: AppIdentity, timestamp: Long = System.currentTimeMillis())

  /**
   * Returns a reactive flow of recent [AppIdentity] items ordered newest to oldest.
   */
  fun getRecentIdentities(limit: Int = 20): Flow<List<AppIdentity>>

  /**
   * Returns a synchronous list of recent [AppIdentity] items ordered newest to oldest.
   */
  suspend fun getRecentIdentitiesSync(limit: Int = 20): List<AppIdentity>

  /**
   * Resolves recent app identities against a static collection of discovered apps.
   * - Ordered most recently launched -> oldest.
   * - Stale/uninstalled apps are safely excluded without crashing.
   * - Strictly preserves profile boundaries (never resolves Work to Personal or vice-versa).
   */
  fun getRecentApps(
    installedApps: List<DiscoveredApp>,
    limit: Int = 20
  ): Flow<List<DiscoveredApp>>

  /**
   * Resolves recent app identities reactively against a dynamic flow of installed apps.
   */
  fun getRecentAppsFlow(
    installedAppsFlow: Flow<List<DiscoveredApp>>,
    limit: Int = 20
  ): Flow<List<DiscoveredApp>>

  /**
   * Synchronous / suspend helper to resolve recent identities against installed apps.
   */
  suspend fun resolveRecentApps(
    installedApps: List<DiscoveredApp>,
    limit: Int = 20
  ): List<DiscoveredApp>

  /**
   * Clears all launch history records.
   */
  suspend fun clearHistory()
}
