package com.multispace.domain.model

/**
 * Domain entity representing an application placed in a Space's persistent bottom Dock.
 */
data class SpaceDockItem(
  val id: String,
  val spaceId: String,
  val orderIndex: Int = 0,
  val packageName: String,
  val componentName: String,
  val userHandleId: Long = 0L
)
