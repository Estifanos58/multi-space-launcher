package com.multispace.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.multispace.data.entity.AppLaunchSummary
import com.multispace.data.entity.LaunchEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Space-specific application launch events and analytics.
 */
@Dao
interface LaunchHistoryDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: LaunchEventEntity): Long

  /**
   * Returns recent unique apps launched in [spaceId], ordered by most recently launched.
   */
  @Query("""
    SELECT package_name, component_name, user_handle_id,
           COUNT(*) AS launch_count,
           MAX(timestamp) AS last_launched,
           MIN(timestamp) AS first_launched
    FROM launch_events
    WHERE space_id = :spaceId
    GROUP BY package_name, component_name, user_handle_id
    ORDER BY last_launched DESC
    LIMIT :limit
  """)
  fun getRecentLaunchesFlow(spaceId: String, limit: Int = 20): Flow<List<AppLaunchSummary>>

  @Query("""
    SELECT package_name, component_name, user_handle_id,
           COUNT(*) AS launch_count,
           MAX(timestamp) AS last_launched,
           MIN(timestamp) AS first_launched
    FROM launch_events
    WHERE space_id = :spaceId
    GROUP BY package_name, component_name, user_handle_id
    ORDER BY last_launched DESC
    LIMIT :limit
  """)
  suspend fun getRecentLaunchesSync(spaceId: String, limit: Int = 20): List<AppLaunchSummary>

  /**
   * Returns most frequently launched apps in [spaceId], ordered by launch count descending,
   * then last launch timestamp descending.
   */
  @Query("""
    SELECT package_name, component_name, user_handle_id,
           COUNT(*) AS launch_count,
           MAX(timestamp) AS last_launched,
           MIN(timestamp) AS first_launched
    FROM launch_events
    WHERE space_id = :spaceId
    GROUP BY package_name, component_name, user_handle_id
    ORDER BY launch_count DESC, last_launched DESC
    LIMIT :limit
  """)
  fun getMostUsedLaunchesFlow(spaceId: String, limit: Int = 50): Flow<List<AppLaunchSummary>>

  @Query("""
    SELECT package_name, component_name, user_handle_id,
           COUNT(*) AS launch_count,
           MAX(timestamp) AS last_launched,
           MIN(timestamp) AS first_launched
    FROM launch_events
    WHERE space_id = :spaceId
    GROUP BY package_name, component_name, user_handle_id
    ORDER BY launch_count DESC, last_launched DESC
    LIMIT :limit
  """)
  suspend fun getMostUsedLaunchesSync(spaceId: String, limit: Int = 50): List<AppLaunchSummary>

  @Query("SELECT COUNT(*) FROM launch_events WHERE space_id = :spaceId")
  suspend fun getTotalLaunchCount(spaceId: String): Int

  @Query("SELECT COUNT(*) FROM launch_events WHERE space_id = :spaceId AND timestamp >= :sinceTimestamp")
  suspend fun getLaunchCountSince(spaceId: String, sinceTimestamp: Long): Int

  @Query("""
    SELECT COUNT(DISTINCT package_name || '/' || component_name || '#' || user_handle_id)
    FROM launch_events
    WHERE space_id = :spaceId
  """)
  suspend fun getUniqueAppsCount(spaceId: String): Int

  @Query("""
    SELECT COUNT(*) FROM launch_events
    WHERE space_id = :spaceId
      AND package_name = :packageName
      AND component_name = :componentName
      AND user_handle_id = :userHandleId
  """)
  suspend fun getAppLaunchCount(
    spaceId: String,
    packageName: String,
    componentName: String,
    userHandleId: Long
  ): Int

  @Query("""
    SELECT COUNT(*) FROM launch_events
    WHERE space_id = :spaceId
      AND package_name = :packageName
      AND component_name = :componentName
      AND user_handle_id = :userHandleId
      AND timestamp >= :sinceTimestamp
  """)
  suspend fun getAppLaunchCountSince(
    spaceId: String,
    packageName: String,
    componentName: String,
    userHandleId: Long,
    sinceTimestamp: Long
  ): Int

  @Query("""
    SELECT MIN(timestamp) FROM launch_events
    WHERE space_id = :spaceId
      AND package_name = :packageName
      AND component_name = :componentName
      AND user_handle_id = :userHandleId
  """)
  suspend fun getAppFirstLaunch(
    spaceId: String,
    packageName: String,
    componentName: String,
    userHandleId: Long
  ): Long?

  @Query("""
    SELECT MAX(timestamp) FROM launch_events
    WHERE space_id = :spaceId
      AND package_name = :packageName
      AND component_name = :componentName
      AND user_handle_id = :userHandleId
  """)
  suspend fun getAppLastLaunch(
    spaceId: String,
    packageName: String,
    componentName: String,
    userHandleId: Long
  ): Long?

  @Query("""
    DELETE FROM launch_events
    WHERE id IN (
      SELECT id FROM launch_events
      WHERE space_id = :spaceId
      ORDER BY timestamp DESC
      LIMIT -1 OFFSET :keepLimit
    )
  """)
  suspend fun pruneOldEvents(spaceId: String, keepLimit: Int)

  @Query("DELETE FROM launch_events WHERE space_id = :spaceId")
  suspend fun clearSpaceHistory(spaceId: String)

  @Query("DELETE FROM launch_events")
  suspend fun clearAll()
}
