package com.multispace.domain.model

/**
 * Single coherent state model representing the currently active Space and all of its
 * associated items, placements, folders, and dock items.
 */
data class ActiveSpaceState(
  val space: Space?,
  val memberships: List<SpaceMembership> = emptyList(),
  val placements: List<SpaceItemPlacement> = emptyList(),
  val folders: List<SpaceFolder> = emptyList(),
  val dockItems: List<SpaceDockItem> = emptyList(),
  val layer: Int = 1,
  val unlocked: Boolean = true
)
