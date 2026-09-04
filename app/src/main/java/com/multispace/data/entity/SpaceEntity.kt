package com.multispace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.multispace.domain.model.PageTurnEffect
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

  @ColumnInfo(name = "pattern_rows")
  val patternRows: Int = 3,

  @ColumnInfo(name = "pattern_cols")
  val patternCols: Int = 3,

  @ColumnInfo(name = "background_type")
  val backgroundType: String = "DEFAULT",

  @ColumnInfo(name = "background_color")
  val backgroundColor: Long? = null,

  @ColumnInfo(name = "background_image_uri")
  val backgroundImageUri: String? = null,

  @ColumnInfo(name = "home_wallpaper_type")
  val homeWallpaperType: String = "DEFAULT",

  @ColumnInfo(name = "home_wallpaper_color")
  val homeWallpaperColor: Long? = null,

  @ColumnInfo(name = "home_wallpaper_image_uri")
  val homeWallpaperImageUri: String? = null,

  @ColumnInfo(name = "phone_lock_wallpaper_type")
  val phoneLockWallpaperType: String = "DEFAULT",

  @ColumnInfo(name = "phone_lock_wallpaper_color")
  val phoneLockWallpaperColor: Long? = null,

  @ColumnInfo(name = "phone_lock_wallpaper_image_uri")
  val phoneLockWallpaperImageUri: String? = null,

  @ColumnInfo(name = "space_lock_wallpaper_type")
  val spaceLockWallpaperType: String = "DEFAULT",

  @ColumnInfo(name = "space_lock_wallpaper_color")
  val spaceLockWallpaperColor: Long? = null,

  @ColumnInfo(name = "space_lock_wallpaper_image_uri")
  val spaceLockWallpaperImageUri: String? = null,

  @ColumnInfo(name = "app_theme")
  val appTheme: String = "DEFAULT",

  @ColumnInfo(name = "grid_columns")
  val gridColumns: Int = 4,

  @ColumnInfo(name = "icon_size")
  val iconSize: String = "MEDIUM",

  @ColumnInfo(name = "label_visibility")
  val labelVisibility: Boolean = true,

  @ColumnInfo(name = "layer1_display_mode")
  val layer1DisplayMode: String = "PAGE",

  @ColumnInfo(name = "layer2_display_mode")
  val layer2DisplayMode: String = "SCROLL",

  @ColumnInfo(name = "layer2_access_mode")
  val layer2AccessMode: String = "DOCK_BUTTON",

  @ColumnInfo(name = "dock_capacity")
  val dockCapacity: Int = 5,

  @ColumnInfo(name = "layout_preset")
  val layoutPreset: String = "DEFAULT",

  @ColumnInfo(name = "use_layer2")
  val useLayer2: Boolean = true,

  @ColumnInfo(name = "home_wallpaper_scale_mode")
  val homeWallpaperScaleMode: String = "crop",

  @ColumnInfo(name = "home_wallpaper_zoom_level")
  val homeWallpaperZoomLevel: Float = 1.0f,

  @ColumnInfo(name = "home_wallpaper_dim_level")
  val homeWallpaperDimLevel: Float = 0.20f,

  @ColumnInfo(name = "home_wallpaper_offset_x")
  val homeWallpaperOffsetX: Float = 0.0f,

  @ColumnInfo(name = "home_wallpaper_offset_y")
  val homeWallpaperOffsetY: Float = 0.0f,

  @ColumnInfo(name = "phone_lock_wallpaper_scale_mode")
  val phoneLockWallpaperScaleMode: String = "crop",

  @ColumnInfo(name = "phone_lock_wallpaper_zoom_level")
  val phoneLockWallpaperZoomLevel: Float = 1.0f,

  @ColumnInfo(name = "phone_lock_wallpaper_dim_level")
  val phoneLockWallpaperDimLevel: Float = 0.20f,

  @ColumnInfo(name = "phone_lock_wallpaper_offset_x")
  val phoneLockWallpaperOffsetX: Float = 0.0f,

  @ColumnInfo(name = "phone_lock_wallpaper_offset_y")
  val phoneLockWallpaperOffsetY: Float = 0.0f,

  @ColumnInfo(name = "space_lock_wallpaper_scale_mode")
  val spaceLockWallpaperScaleMode: String = "crop",

  @ColumnInfo(name = "space_lock_wallpaper_zoom_level")
  val spaceLockWallpaperZoomLevel: Float = 1.0f,

  @ColumnInfo(name = "space_lock_wallpaper_dim_level")
  val spaceLockWallpaperDimLevel: Float = 0.20f,

  @ColumnInfo(name = "space_lock_wallpaper_offset_x")
  val spaceLockWallpaperOffsetX: Float = 0.0f,

  @ColumnInfo(name = "space_lock_wallpaper_offset_y")
  val spaceLockWallpaperOffsetY: Float = 0.0f,

  @ColumnInfo(name = "page_turn_effect")
  val pageTurnEffect: String = "NORMAL",

  @ColumnInfo(name = "page_turn_duration_ms")
  val pageTurnDurationMs: Int = 300,

  @ColumnInfo(name = "page_turn_intensity")
  val pageTurnIntensity: Float = 1.0f
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
    patternRows = patternRows,
    patternCols = patternCols,
    backgroundType = backgroundType,
    backgroundColor = backgroundColor,
    backgroundImageUri = backgroundImageUri,
    homeWallpaperType = homeWallpaperType,
    homeWallpaperColor = homeWallpaperColor,
    homeWallpaperImageUri = homeWallpaperImageUri,
    phoneLockWallpaperType = phoneLockWallpaperType,
    phoneLockWallpaperColor = phoneLockWallpaperColor,
    phoneLockWallpaperImageUri = phoneLockWallpaperImageUri,
    spaceLockWallpaperType = spaceLockWallpaperType,
    spaceLockWallpaperColor = spaceLockWallpaperColor,
    spaceLockWallpaperImageUri = spaceLockWallpaperImageUri,
    appTheme = appTheme,
    gridColumns = gridColumns,
    iconSize = iconSize,
    labelVisibility = labelVisibility,
    layer1DisplayMode = layer1DisplayMode,
    layer2DisplayMode = layer2DisplayMode,
    layer2AccessMode = layer2AccessMode,
    dockCapacity = dockCapacity,
    layoutPreset = layoutPreset,
    useLayer2 = useLayer2,
    homeWallpaperScaleMode = homeWallpaperScaleMode,
    homeWallpaperZoomLevel = homeWallpaperZoomLevel,
    homeWallpaperDimLevel = homeWallpaperDimLevel,
    homeWallpaperOffsetX = homeWallpaperOffsetX,
    homeWallpaperOffsetY = homeWallpaperOffsetY,
    phoneLockWallpaperScaleMode = phoneLockWallpaperScaleMode,
    phoneLockWallpaperZoomLevel = phoneLockWallpaperZoomLevel,
    phoneLockWallpaperDimLevel = phoneLockWallpaperDimLevel,
    phoneLockWallpaperOffsetX = phoneLockWallpaperOffsetX,
    phoneLockWallpaperOffsetY = phoneLockWallpaperOffsetY,
    spaceLockWallpaperScaleMode = spaceLockWallpaperScaleMode,
    spaceLockWallpaperZoomLevel = spaceLockWallpaperZoomLevel,
    spaceLockWallpaperDimLevel = spaceLockWallpaperDimLevel,
    spaceLockWallpaperOffsetX = spaceLockWallpaperOffsetX,
    spaceLockWallpaperOffsetY = spaceLockWallpaperOffsetY,
    pageTurnEffect = PageTurnEffect.fromString(pageTurnEffect),
    pageTurnDurationMs = pageTurnDurationMs,
    pageTurnIntensity = pageTurnIntensity
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
      patternRows = domain.patternRows,
      patternCols = domain.patternCols,
      backgroundType = domain.backgroundType,
      backgroundColor = domain.backgroundColor,
      backgroundImageUri = domain.backgroundImageUri,
      homeWallpaperType = domain.homeWallpaperType,
      homeWallpaperColor = domain.homeWallpaperColor,
      homeWallpaperImageUri = domain.homeWallpaperImageUri,
      phoneLockWallpaperType = domain.phoneLockWallpaperType,
      phoneLockWallpaperColor = domain.phoneLockWallpaperColor,
      phoneLockWallpaperImageUri = domain.phoneLockWallpaperImageUri,
      spaceLockWallpaperType = domain.spaceLockWallpaperType,
      spaceLockWallpaperColor = domain.spaceLockWallpaperColor,
      spaceLockWallpaperImageUri = domain.spaceLockWallpaperImageUri,
      appTheme = domain.appTheme,
      gridColumns = domain.gridColumns,
      iconSize = domain.iconSize,
      labelVisibility = domain.labelVisibility,
      layer1DisplayMode = domain.layer1DisplayMode,
      layer2DisplayMode = domain.layer2DisplayMode,
      layer2AccessMode = domain.layer2AccessMode,
      dockCapacity = domain.dockCapacity,
      layoutPreset = domain.layoutPreset,
      useLayer2 = domain.useLayer2,
      homeWallpaperScaleMode = domain.homeWallpaperScaleMode,
      homeWallpaperZoomLevel = domain.homeWallpaperZoomLevel,
      homeWallpaperDimLevel = domain.homeWallpaperDimLevel,
      homeWallpaperOffsetX = domain.homeWallpaperOffsetX,
      homeWallpaperOffsetY = domain.homeWallpaperOffsetY,
      phoneLockWallpaperScaleMode = domain.phoneLockWallpaperScaleMode,
      phoneLockWallpaperZoomLevel = domain.phoneLockWallpaperZoomLevel,
      phoneLockWallpaperDimLevel = domain.phoneLockWallpaperDimLevel,
      phoneLockWallpaperOffsetX = domain.phoneLockWallpaperOffsetX,
      phoneLockWallpaperOffsetY = domain.phoneLockWallpaperOffsetY,
      spaceLockWallpaperScaleMode = domain.spaceLockWallpaperScaleMode,
      spaceLockWallpaperZoomLevel = domain.spaceLockWallpaperZoomLevel,
      spaceLockWallpaperDimLevel = domain.spaceLockWallpaperDimLevel,
      spaceLockWallpaperOffsetX = domain.spaceLockWallpaperOffsetX,
      spaceLockWallpaperOffsetY = domain.spaceLockWallpaperOffsetY,
      pageTurnEffect = domain.pageTurnEffect.name,
      pageTurnDurationMs = domain.pageTurnDurationMs,
      pageTurnIntensity = domain.pageTurnIntensity
    )
  }
}
