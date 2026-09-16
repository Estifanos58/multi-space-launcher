package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.multispace.domain.model.AppIdentity

/**
 * Entity representing an individual application launch event within a specific Space.
 *
 * Space-specific usage identity: Space ID + AppIdentity (packageName + componentName + userHandleId).
 * Supports both recency and frequency analytics per Space.
 */
@Entity(
  tableName = "launch_events",
  indices = [
    Index(value = ["space_id", "package_name", "component_name", "user_handle_id"]),
    Index(value = ["space_id", "timestamp"]),
    Index(value = ["timestamp"])
  ]
)
data class LaunchEventEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "space_id")
  val spaceId: String,
  @ColumnInfo(name = "package_name")
  val packageName: String,
  @ColumnInfo(name = "component_name")
  val componentName: String = "",
  @ColumnInfo(name = "user_handle_id")
  val userHandleId: Long = 0L,
  @ColumnInfo(name = "timestamp")
  val timestamp: Long = System.currentTimeMillis()
) {
  val appIdentity: AppIdentity
    get() = AppIdentity(
      packageName = packageName,
      componentName = componentName,
      userHandleId = userHandleId
    )
}

/**
 * Aggregated summary projection returned from launch event queries.
 */
data class AppLaunchSummary(
  @ColumnInfo(name = "package_name") val packageName: String,
  @ColumnInfo(name = "component_name") val componentName: String,
  @ColumnInfo(name = "user_handle_id") val userHandleId: Long,
  @ColumnInfo(name = "launch_count") val launchCount: Int,
  @ColumnInfo(name = "last_launched") val lastLaunched: Long,
  @ColumnInfo(name = "first_launched") val firstLaunched: Long
) {
  val appIdentity: AppIdentity
    get() = AppIdentity(
      packageName = packageName,
      componentName = componentName,
      userHandleId = userHandleId
    )
}
