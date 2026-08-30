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
)
