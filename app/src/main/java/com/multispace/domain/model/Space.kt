package com.multispace.domain.model

/**
 * Represents an isolated user-defined launcher workspace.
 *
 * @property id Stable, immutable identifier independent of the display name.
 * @property name User-visible, mutable display name.
 * @property orderIndex Position for ordering spaces.
 * @property createdAt Timestamp when space was created.
 * @property updatedAt Timestamp when space was last modified.
 * @property authPolicy Extensible authentication configuration reference (e.g., "NONE", "PIN").
 * @property layoutType Extensible layout configuration descriptor (e.g., "GRID_4").
 */
data class Space(
  val id: String,
  val name: String,
  val orderIndex: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val authPolicy: String = AUTH_NONE,
  val pinSalt: String? = null,
  val pinHash: String? = null,
  val layoutType: String = "GRID_4",
  val patternRows: Int = DEFAULT_PATTERN_ROWS,
  val patternCols: Int = DEFAULT_PATTERN_COLS,
  val backgroundType: String = BACKGROUND_DEFAULT,
  val backgroundColor: Long? = null,
  val backgroundImageUri: String? = null,
  val homeWallpaperType: String = BACKGROUND_DEFAULT,
  val homeWallpaperColor: Long? = null,
  val homeWallpaperImageUri: String? = null,
  val phoneLockWallpaperType: String = BACKGROUND_DEFAULT,
  val phoneLockWallpaperColor: Long? = null,
  val phoneLockWallpaperImageUri: String? = null,
  val spaceLockWallpaperType: String = BACKGROUND_DEFAULT,
  val spaceLockWallpaperColor: Long? = null,
  val spaceLockWallpaperImageUri: String? = null,
  val appTheme: String = THEME_DEFAULT,
  val gridColumns: Int = DEFAULT_GRID_COLUMNS,
  val iconSize: String = ICON_SIZE_MEDIUM,
  val labelVisibility: Boolean = true,
  val layer1DisplayMode: String = DISPLAY_MODE_PAGE,
  val layer2DisplayMode: String = DISPLAY_MODE_SCROLL,
  val layer2AccessMode: String = ACCESS_MODE_DOCK_BUTTON,
  val dockCapacity: Int = DEFAULT_DOCK_CAPACITY,
  val layoutPreset: String = PRESET_DEFAULT,
  val useLayer2: Boolean = true,
  val homeWallpaperScaleMode: String = "crop",
  val homeWallpaperZoomLevel: Float = 1.0f,
  val homeWallpaperDimLevel: Float = 0.20f,
  val homeWallpaperOffsetX: Float = 0.0f,
  val homeWallpaperOffsetY: Float = 0.0f,
  val phoneLockWallpaperScaleMode: String = "crop",
  val phoneLockWallpaperZoomLevel: Float = 1.0f,
  val phoneLockWallpaperDimLevel: Float = 0.20f,
  val phoneLockWallpaperOffsetX: Float = 0.0f,
  val phoneLockWallpaperOffsetY: Float = 0.0f,
  val spaceLockWallpaperScaleMode: String = "crop",
  val spaceLockWallpaperZoomLevel: Float = 1.0f,
  val spaceLockWallpaperDimLevel: Float = 0.20f,
  val spaceLockWallpaperOffsetX: Float = 0.0f,
  val spaceLockWallpaperOffsetY: Float = 0.0f
) {
  val isProtected: Boolean
    get() = if (authPolicy == AUTH_BIOMETRIC) true else ((authPolicy == AUTH_PIN || authPolicy == AUTH_PATTERN) && !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty())

  val isBiometricProtected: Boolean
    get() = authPolicy == AUTH_BIOMETRIC

  val isPatternProtected: Boolean
    get() = authPolicy == AUTH_PATTERN && !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()

  val isPinProtected: Boolean
    get() = authPolicy == AUTH_PIN && !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()

  companion object {
    const val DEFAULT_SPACE_ID = "space_default"
    const val DEFAULT_SPACE_NAME = "Default"

    const val AUTH_NONE = "NONE"
    const val AUTH_PIN = "PIN"
    const val AUTH_PATTERN = "PATTERN"
    const val AUTH_BIOMETRIC = "BIOMETRIC"

    const val DEFAULT_PATTERN_ROWS = 3
    const val DEFAULT_PATTERN_COLS = 3

    const val BACKGROUND_DEFAULT = "DEFAULT"
    const val BACKGROUND_COLOR = "COLOR"
    const val BACKGROUND_IMAGE = "IMAGE"

    const val THEME_DEFAULT = "DEFAULT"
    const val THEME_PURPLE = "PURPLE"
    const val THEME_DARK = "DARK"
    const val THEME_NEON = "NEON"
    const val THEME_MINIMAL = "MINIMAL"
    const val THEME_EMERALD = "EMERALD"
    const val THEME_SUNSET = "SUNSET"
    const val THEME_OCEAN = "OCEAN"

    const val DEFAULT_GRID_COLUMNS = 4
    const val MIN_GRID_COLUMNS = 2
    const val MAX_GRID_COLUMNS = 8

    const val ICON_SIZE_SMALL = "SMALL"
    const val ICON_SIZE_MEDIUM = "MEDIUM"
    const val ICON_SIZE_LARGE = "LARGE"

    // Layer Display Modes
    const val DISPLAY_MODE_PAGE = "PAGE"
    const val DISPLAY_MODE_SCROLL = "SCROLL"

    // Layer 2 Access Modes
    const val ACCESS_MODE_DOCK_BUTTON = "DOCK_BUTTON"
    const val ACCESS_MODE_SWIPE_UP = "SWIPE_UP"

    // Dock bounds
    const val DEFAULT_DOCK_CAPACITY = 5
    const val MIN_DOCK_CAPACITY = 3
    const val MAX_DOCK_CAPACITY = 7

    // Layout Presets
    const val PRESET_DEFAULT = "DEFAULT"
    const val PRESET_APPLE = "APPLE_INSPIRED"
    const val PRESET_ONE_UI = "SAMSUNG_ONEUI"
    const val PRESET_PIXEL = "PIXEL_INSPIRED"
    const val PRESET_CLASSIC = "CLASSIC_ANDROID"
    const val PRESET_MINIMAL = "MINIMAL"
    const val PRESET_COMPACT = "COMPACT"
    const val PRESET_LARGE_ICONS = "LARGE_ICONS"
    const val PRESET_PRODUCTIVITY = "PRODUCTIVITY"
    const val PRESET_GAMING = "GAMING"

    fun createDefault(
      id: String = DEFAULT_SPACE_ID,
      name: String = DEFAULT_SPACE_NAME
    ): Space = Space(
      id = id,
      name = name,
      backgroundType = BACKGROUND_IMAGE,
      backgroundImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI,
      homeWallpaperType = BACKGROUND_IMAGE,
      homeWallpaperImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI,
      phoneLockWallpaperType = BACKGROUND_IMAGE,
      phoneLockWallpaperImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI,
      spaceLockWallpaperType = BACKGROUND_IMAGE,
      spaceLockWallpaperImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI
    )
  }
}
