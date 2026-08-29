package com.example.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.domain.model.SpaceMembership

@Entity(
  tableName = "space_memberships",
  primaryKeys = ["space_id", "package_name", "component_name", "user_handle_id"],
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
data class SpaceMembershipEntity(
  @ColumnInfo(name = "space_id")
  val spaceId: String,

  @ColumnInfo(name = "package_name")
  val packageName: String,

  @ColumnInfo(name = "component_name")
  val componentName: String,

  @ColumnInfo(name = "user_handle_id")
  val userHandleId: Long = 0L,

  @ColumnInfo(name = "order_index")
  val orderIndex: Int = 0,

  @ColumnInfo(name = "added_at")
  val addedAt: Long = System.currentTimeMillis()
) {
  fun toDomain(): SpaceMembership = SpaceMembership(
    spaceId = spaceId,
    packageName = packageName,
    componentName = componentName,
    userHandleId = userHandleId,
    orderIndex = orderIndex,
    addedAt = addedAt
  )

  companion object {
    fun fromDomain(domain: SpaceMembership): SpaceMembershipEntity = SpaceMembershipEntity(
      spaceId = domain.spaceId,
      packageName = domain.packageName,
      componentName = domain.componentName,
      userHandleId = domain.userHandleId,
      orderIndex = domain.orderIndex,
      addedAt = domain.addedAt
    )
  }
}
