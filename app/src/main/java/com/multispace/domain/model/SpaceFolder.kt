package com.multispace.domain.model

/**
 * Domain entity representing an app folder in a Space.
 *
 * @property id Unique stable identifier for the folder.
 * @property spaceId The Space to which this folder belongs.
 * @property name User-visible folder title.
 * @property items Apps contained within this folder.
 * @property createdAt Creation timestamp.
 * @property updatedAt Last modification timestamp.
 */
data class SpaceFolder(
  val id: String,
  val spaceId: String,
  val name: String,
  val items: List<SpaceFolderItem> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
) {
  val isMostUsedFolder: Boolean
    get() = name == MOST_USED_FOLDER_NAME || id.startsWith(MOST_USED_FOLDER_PREFIX)

  companion object {
    const val MOST_USED_FOLDER_NAME = "Most Used Apps"
    const val MOST_USED_FOLDER_PREFIX = "folder_most_used"

    fun getMostUsedFolderId(spaceId: String): String = "${MOST_USED_FOLDER_PREFIX}_$spaceId"
    fun getMostUsedPlacementId(spaceId: String): String = "placement_most_used_$spaceId"
  }
}
