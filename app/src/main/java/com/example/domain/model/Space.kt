package com.example.domain.model

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
  val authPolicy: String = "NONE",
  val pinSalt: String? = null,
  val pinHash: String? = null,
  val layoutType: String = "GRID_4",
  val backgroundType: String = BACKGROUND_DEFAULT,
  val backgroundColor: Long? = null,
  val backgroundImageUri: String? = null,
  val gridColumns: Int = DEFAULT_GRID_COLUMNS,
  val iconSize: String = ICON_SIZE_MEDIUM,
  val labelVisibility: Boolean = true
) {
  val isProtected: Boolean
    get() = authPolicy == "PIN" && !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()

  companion object {
    const val DEFAULT_SPACE_ID = "space_default"
    const val DEFAULT_SPACE_NAME = "Default"

    const val BACKGROUND_DEFAULT = "DEFAULT"
    const val BACKGROUND_COLOR = "COLOR"
    const val BACKGROUND_IMAGE = "IMAGE"

    const val DEFAULT_GRID_COLUMNS = 4
    const val MIN_GRID_COLUMNS = 3
    const val MAX_GRID_COLUMNS = 6

    const val ICON_SIZE_SMALL = "SMALL"
    const val ICON_SIZE_MEDIUM = "MEDIUM"
    const val ICON_SIZE_LARGE = "LARGE"
  }
}
