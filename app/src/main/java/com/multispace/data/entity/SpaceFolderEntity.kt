package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem

@Entity(
  tableName = "space_folders",
  foreignKeys = [
    ForeignKey(
      entity = SpaceEntity::class,
      parentColumns = ["id"],
      childColumns = ["space_id"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["space_id"])]
)
data class SpaceFolderEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "space_id")
  val spaceId: String,

  @ColumnInfo(name = "name")
  val name: String,

  @ColumnInfo(name = "created_at")
  val createdAt: Long = System.currentTimeMillis(),

  @ColumnInfo(name = "updated_at")
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun toDomain(items: List<SpaceFolderItem> = emptyList()): SpaceFolder = SpaceFolder(
    id = id,
    spaceId = spaceId,
    name = name,
    items = items,
    createdAt = createdAt,
    updatedAt = updatedAt
  )

  companion object {
    fun fromDomain(domain: SpaceFolder): SpaceFolderEntity = SpaceFolderEntity(
      id = domain.id,
      spaceId = domain.spaceId,
      name = domain.name,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt
    )
  }
}
