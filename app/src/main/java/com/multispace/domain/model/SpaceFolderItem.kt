package com.multispace.domain.model

/**
 * Domain entity representing an app contained within a folder.
 */
data class SpaceFolderItem(
  val id: String,
  val folderId: String,
  val packageName: String,
  val componentName: String,
  val userHandleId: Long = 0L,
  val orderIndex: Int = 0
)
