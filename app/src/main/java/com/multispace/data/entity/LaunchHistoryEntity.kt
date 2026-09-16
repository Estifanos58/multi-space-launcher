package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.multispace.domain.model.AppIdentity

/**
 * Room entity representing a recorded application launch in the launcher's persistent history.
 *
 * Identity is canonically defined by (packageName + componentName + userHandleId), ensuring
 * Personal and Work profile instances and distinct activities within the same package remain separate.
 */
@Entity(
  tableName = "launch_history",
  indices = [
    Index(value = ["package_name", "component_name", "user_handle_id"], unique = true),
    Index(value = ["last_launched_at"])
  ]
)
data class LaunchHistoryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "package_name")
  val packageName: String,
  @ColumnInfo(name = "component_name")
  val componentName: String = "",
  @ColumnInfo(name = "user_handle_id")
  val userHandleId: Long = 0L,
  @ColumnInfo(name = "last_launched_at")
  val lastLaunchedAt: Long = System.currentTimeMillis()
) {
  val appIdentity: AppIdentity
    get() = AppIdentity(
      packageName = packageName,
      componentName = componentName,
      userHandleId = userHandleId
    )
}
