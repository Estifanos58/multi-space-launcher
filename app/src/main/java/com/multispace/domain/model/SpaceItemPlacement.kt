package com.multispace.domain.model

/**
 * Represents the persistent placement of an application or a folder on a specific layer and page of a Space.
 *
 * @property id Unique identifier for this placement.
 * @property spaceId Stable identifier of the associated Space.
 * @property layer Target layer: 1 for Curated Home Layer, 2 for Space App Library.
 * @property pageIndex 0-based page index within the layer.
 * @property positionIndex 0-based position order on that page.
 * @property itemType Type of placed item: [ITEM_TYPE_APP] or [ITEM_TYPE_FOLDER].
 * @property packageName Package name if itemType is [ITEM_TYPE_APP].
 * @property componentName Explicit activity component name if itemType is [ITEM_TYPE_APP].
 * @property userHandleId Android UserProfile identifier.
 * @property folderId Unique identifier of the folder if itemType is [ITEM_TYPE_FOLDER].
 */
data class SpaceItemPlacement(
  val id: String,
  val spaceId: String,
  val layer: Int = LAYER_HOME,
  val pageIndex: Int = 0,
  val positionIndex: Int = 0,
  val itemType: String = ITEM_TYPE_APP,
  val packageName: String? = null,
  val componentName: String? = null,
  val userHandleId: Long = 0L,
  val folderId: String? = null
) {
  val isFolder: Boolean
    get() = itemType == ITEM_TYPE_FOLDER && !folderId.isNullOrEmpty()

  val isApp: Boolean
    get() = itemType == ITEM_TYPE_APP && !packageName.isNullOrEmpty()

  companion object {
    const val LAYER_HOME = 1
    const val LAYER_LIBRARY = 2

    const val ITEM_TYPE_APP = "APP"
    const val ITEM_TYPE_FOLDER = "FOLDER"
  }
}
