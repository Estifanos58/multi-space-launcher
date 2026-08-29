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
  val labelVisibility: Boolean = true
) {
  val isProtected: Boolean
    get() = (authPolicy == AUTH_PIN || authPolicy == AUTH_PATTERN) && !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()

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
  }
}
