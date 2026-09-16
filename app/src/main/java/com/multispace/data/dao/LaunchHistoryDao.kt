package com.multispace.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.multispace.data.entity.LaunchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for launcher-owned application launch history.
 */
@Dao
interface LaunchHistoryDao {

  @Query("SELECT * FROM launch_history ORDER BY last_launched_at DESC, id DESC LIMIT :limit")
  fun getRecentLaunchesFlow(limit: Int = 20): Flow<List<LaunchHistoryEntity>>

  @Query("SELECT * FROM launch_history ORDER BY last_launched_at DESC, id DESC LIMIT :limit")
  suspend fun getRecentLaunchesSync(limit: Int = 20): List<LaunchHistoryEntity>

  @Query("""
    SELECT * FROM launch_history 
    WHERE package_name = :packageName 
      AND component_name = :componentName 
      AND user_handle_id = :userHandleId 
    LIMIT 1
  """)
  suspend fun findEntry(
    packageName: String,
    componentName: String,
    userHandleId: Long
  ): LaunchHistoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(entry: LaunchHistoryEntity): Long

  @Query("UPDATE launch_history SET last_launched_at = :timestamp WHERE id = :id")
  suspend fun updateLaunchTimestamp(id: Long, timestamp: Long)

  @Query("SELECT id FROM launch_history ORDER BY last_launched_at DESC, id DESC")
  suspend fun getAllIdsOrdered(): List<Long>

  @Query("DELETE FROM launch_history WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<Long>)

  @Query("DELETE FROM launch_history WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM launch_history")
  suspend fun clearAll()

  @Query("SELECT COUNT(*) FROM launch_history")
  suspend fun getCount(): Int
}
