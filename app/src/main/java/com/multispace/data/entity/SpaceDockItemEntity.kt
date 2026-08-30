package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.multispace.domain.model.SpaceDockItem

@Entity(
  tableName = "space_dock_items",
  foreignKeys = [
    ForeignKey(
      entity = SpaceEntity::class,
      parentColumns = ["id"],
      childColumns = ["space_id"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["space_id"]),
    Index(value = ["space_id", "order_index"])
  ]
)
data class SpaceDockItemEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "space_id")
  val spaceId: String,

  @ColumnInfo(name = "order_index")
  val orderIndex: Int = 0,

  @ColumnInfo(name = "package_name")
  val packageName: String,

  @ColumnInfo(name = "component_name")
  val componentName: String,

  @ColumnInfo(name = "user_handle_id")
  val userHandleId: Long = 0L
) {
  fun toDomain(): SpaceDockItem = SpaceDockItem(
    id = id,
    spaceId = spaceId,
    orderIndex = orderIndex,
    packageName = packageName,
    componentName = componentName,
    userHandleId = userHandleId
  )

  companion object {
    fun fromDomain(domain: SpaceDockItem): SpaceDockItemEntity = SpaceDockItemEntity(
      id = domain.id,
      spaceId = domain.spaceId,
      orderIndex = domain.orderIndex,
      packageName = domain.packageName,
      componentName = domain.componentName,
      userHandleId = domain.userHandleId
    )
  }
}
