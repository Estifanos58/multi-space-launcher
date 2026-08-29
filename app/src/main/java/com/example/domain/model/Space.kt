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
  val layoutType: String = "GRID_4"
) {
  companion object {
    const val DEFAULT_SPACE_ID = "space_default"
    const val DEFAULT_SPACE_NAME = "Default"
  }
}
