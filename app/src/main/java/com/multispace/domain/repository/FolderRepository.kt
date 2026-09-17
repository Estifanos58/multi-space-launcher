package com.multispace.domain.repository

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceFolder
import kotlinx.coroutines.flow.Flow

/**
 * Focused repository interface for managing Space folders and folder items.
 */
interface FolderRepository {
  fun getFoldersForSpaceFlow(spaceId: String): Flow<List<SpaceFolder>>
  suspend fun getFoldersForSpace(spaceId: String): List<SpaceFolder>
  suspend fun renameFolder(folderId: String, newName: String): Result<Unit>
  suspend fun addAppToFolder(folderId: String, app: DiscoveredApp, sourcePlacementId: String? = null): Result<Unit>
  suspend fun removeAppFromFolder(folderId: String, folderItemId: String): Result<Unit>
  suspend fun deleteFolder(folderId: String): Result<Unit>
  suspend fun createFolderFromApps(
    spaceId: String,
    pageIndex: Int,
    positionIndex: Int,
    folderName: String,
    sourceApp: DiscoveredApp,
    targetApp: DiscoveredApp,
    sourcePlacementId: String? = null,
    targetPlacementId: String? = null,
    sourceDockItemId: String? = null
  ): Result<SpaceFolder>
}
