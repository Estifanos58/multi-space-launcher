package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.multispace.domain.model.SpaceItemPlacement

@Entity(
  tableName = "space_item_placements",
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
    Index(value = ["space_id", "layer", "page_index"])
  ]
)
data class SpaceItemPlacementEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "space_id")
  val spaceId: String,

  @ColumnInfo(name = "layer")
  val layer: Int = 1,

  @ColumnInfo(name = "page_index")
  val pageIndex: Int = 0,

  @ColumnInfo(name = "position_index")
  val positionIndex: Int = 0,

  @ColumnInfo(name = "item_type")
  val itemType: String = "APP",

  @ColumnInfo(name = "package_name")
  val packageName: String? = null,

  @ColumnInfo(name = "component_name")
  val componentName: String? = null,

  @ColumnInfo(name = "user_handle_id")
  val userHandleId: Long = 0L,

  @ColumnInfo(name = "folder_id")
  val folderId: String? = null,

  @ColumnInfo(name = "span_x")
  val spanX: Int = 1,

  @ColumnInfo(name = "span_y")
  val spanY: Int = 1,

  @ColumnInfo(name = "app_widget_id")
  val appWidgetId: Int = -1,

  @ColumnInfo(name = "custom_widget_type")
  val customWidgetType: String? = null
) {
  fun toDomain(): SpaceItemPlacement = SpaceItemPlacement(
    id = id,
    spaceId = spaceId,
    layer = layer,
    pageIndex = pageIndex,
    positionIndex = positionIndex,
    itemType = itemType,
    packageName = packageName,
    componentName = componentName,
    userHandleId = userHandleId,
    folderId = folderId,
    spanX = spanX,
    spanY = spanY,
    appWidgetId = appWidgetId,
    customWidgetType = customWidgetType
  )

  companion object {
    fun fromDomain(domain: SpaceItemPlacement): SpaceItemPlacementEntity = SpaceItemPlacementEntity(
      id = domain.id,
      spaceId = domain.spaceId,
      layer = domain.layer,
      pageIndex = domain.pageIndex,
      positionIndex = domain.positionIndex,
      itemType = domain.itemType,
      packageName = domain.packageName,
      componentName = domain.componentName,
      userHandleId = domain.userHandleId,
      folderId = domain.folderId,
      spanX = domain.spanX,
      spanY = domain.spanY,
      appWidgetId = domain.appWidgetId,
      customWidgetType = domain.customWidgetType
    )
  }
}
