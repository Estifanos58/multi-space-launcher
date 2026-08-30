package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.multispace.domain.model.SpaceFolderItem

@Entity(
  tableName = "space_folder_items",
  foreignKeys = [
    ForeignKey(
      entity = SpaceFolderEntity::class,
      parentColumns = ["id"],
      childColumns = ["folder_id"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["folder_id"]),
    Index(value = ["folder_id", "order_index"])
  ]
)
data class SpaceFolderItemEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "folder_id")
  val folderId: String,

  @ColumnInfo(name = "package_name")
  val packageName: String,

  @ColumnInfo(name = "component_name")
  val componentName: String,

  @ColumnInfo(name = "user_handle_id")
  val userHandleId: Long = 0L,

  @ColumnInfo(name = "order_index")
  val orderIndex: Int = 0
) {
  fun toDomain(): SpaceFolderItem = SpaceFolderItem(
    id = id,
    folderId = folderId,
    packageName = packageName,
    componentName = componentName,
    userHandleId = userHandleId,
    orderIndex = orderIndex
  )

  companion object {
    fun fromDomain(domain: SpaceFolderItem): SpaceFolderItemEntity = SpaceFolderItemEntity(
      id = domain.id,
      folderId = domain.folderId,
      packageName = domain.packageName,
      componentName = domain.componentName,
      userHandleId = domain.userHandleId,
      orderIndex = domain.orderIndex
    )
  }
}
