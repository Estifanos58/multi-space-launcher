package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.multispace.domain.model.Space

@Entity(tableName = "spaces")
data class SpaceEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "name")
  val name: String,

  @ColumnInfo(name = "order_index")
  val orderIndex: Int = 0,

  @ColumnInfo(name = "created_at")
  val createdAt: Long = System.currentTimeMillis(),

  @ColumnInfo(name = "updated_at")
  val updatedAt: Long = System.currentTimeMillis(),

  @ColumnInfo(name = "auth_policy")
  val authPolicy: String = "NONE",

  @ColumnInfo(name = "pin_salt")
  val pinSalt: String? = null,

  @ColumnInfo(name = "pin_hash")
  val pinHash: String? = null,

  @ColumnInfo(name = "layout_type")
  val layoutType: String = "GRID_4",

  @ColumnInfo(name = "background_type")
  val backgroundType: String = "DEFAULT",

  @ColumnInfo(name = "background_color")
  val backgroundColor: Long? = null,

  @ColumnInfo(name = "background_image_uri")
  val backgroundImageUri: String? = null,

  @ColumnInfo(name = "grid_columns")
  val gridColumns: Int = 4,

  @ColumnInfo(name = "icon_size")
  val iconSize: String = "MEDIUM",

  @ColumnInfo(name = "label_visibility")
  val labelVisibility: Boolean = true
) {
  fun toDomain(): Space = Space(
    id = id,
    name = name,
    orderIndex = orderIndex,
    createdAt = createdAt,
    updatedAt = updatedAt,
    authPolicy = authPolicy,
    pinSalt = pinSalt,
    pinHash = pinHash,
    layoutType = layoutType,
    backgroundType = backgroundType,
    backgroundColor = backgroundColor,
    backgroundImageUri = backgroundImageUri,
    gridColumns = gridColumns,
    iconSize = iconSize,
    labelVisibility = labelVisibility
  )

  companion object {
    fun fromDomain(domain: Space): SpaceEntity = SpaceEntity(
      id = domain.id,
      name = domain.name,
      orderIndex = domain.orderIndex,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      authPolicy = domain.authPolicy,
      pinSalt = domain.pinSalt,
      pinHash = domain.pinHash,
      layoutType = domain.layoutType,
      backgroundType = domain.backgroundType,
      backgroundColor = domain.backgroundColor,
      backgroundImageUri = domain.backgroundImageUri,
      gridColumns = domain.gridColumns,
      iconSize = domain.iconSize,
      labelVisibility = domain.labelVisibility
    )
  }
}
