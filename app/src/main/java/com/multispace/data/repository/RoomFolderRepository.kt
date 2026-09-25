package com.multispace.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.multispace.data.dao.SpaceLayoutDao
import com.multispace.data.entity.SpaceFolderEntity
import com.multispace.data.entity.SpaceFolderItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.FolderRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomFolderRepository(
  private val layoutDao: SpaceLayoutDao,
  private val database: RoomDatabase? = null
) : FolderRepository {

  private suspend fun <T> runInTransaction(block: suspend () -> T): T {
    return if (database != null) {
      database.withTransaction { block() }
    } else {
      block()
    }
  }

  override fun getFoldersForSpaceFlow(spaceId: String): Flow<List<SpaceFolder>> {
    return combine(
      layoutDao.getFoldersForSpaceFlow(spaceId),
      layoutDao.getAllFolderItemsForSpaceFlow(spaceId)
    ) { folders, items ->
      val itemsByFolder = items.groupBy { it.folderId }
      folders.map { f ->
        val folderItems = itemsByFolder[f.id]?.map { it.toDomain() } ?: emptyList()
        f.toDomain(folderItems)
      }
    }
  }

  override suspend fun getFoldersForSpace(spaceId: String): List<SpaceFolder> {
    val folders = layoutDao.getFoldersForSpace(spaceId)
    return folders.map { f ->
      val items = layoutDao.getFolderItems(f.id).map { it.toDomain() }
      f.toDomain(items)
    }
  }

  override suspend fun renameFolder(folderId: String, newName: String): Result<Unit> {
    return try {
      runInTransaction {
        val existing = layoutDao.getFolderById(folderId)
          ?: throw IllegalArgumentException("Folder not found: $folderId")
        val updated = existing.copy(name = newName.trim().ifBlank { "Folder" }, updatedAt = System.currentTimeMillis())
        layoutDao.updateFolder(updated)
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to rename folder $folderId", e)
      Result.failure(e)
    }
  }

  override suspend fun addAppToFolder(folderId: String, app: DiscoveredApp, sourcePlacementId: String?): Result<Unit> {
    return try {
      runInTransaction {
        val folder = layoutDao.getFolderById(folderId)
          ?: throw IllegalArgumentException("Folder $folderId does not exist")
        if (folder.name == SpaceFolder.MOST_USED_FOLDER_NAME || folder.id.startsWith(SpaceFolder.MOST_USED_FOLDER_PREFIX)) {
          // Dynamic Most Used Apps folder: manual additions are not allowed and source placement must remain intact
          return@runInTransaction Result.success(Unit)
        }
        val items = layoutDao.getFolderItems(folderId)
        val targetIdentity = app.appIdentity
        val exists = items.any { it.toDomain().appIdentity.matches(targetIdentity) }
        if (!exists) {
          val newItem = SpaceFolderItemEntity(
            id = "fitem_" + UUID.randomUUID().toString().replace("-", "").take(10),
            folderId = folderId,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId,
            orderIndex = items.size
          )
          layoutDao.insertFolderItem(newItem)
        }
        if (!sourcePlacementId.isNullOrEmpty()) {
          layoutDao.deletePlacementById(sourcePlacementId)
        }
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add app to folder $folderId", e)
      Result.failure(e)
    }
  }

  override suspend fun removeAppFromFolder(folderId: String, folderItemId: String): Result<Unit> {
    return try {
      runInTransaction {
        layoutDao.deleteFolderItemById(folderItemId)
        val remaining = layoutDao.getFolderItems(folderId)
        if (remaining.isEmpty()) {
          layoutDao.deleteFolderById(folderId)
          layoutDao.deletePlacementByFolderId(folderId)
        }
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to remove app from folder $folderId", e)
      Result.failure(e)
    }
  }

  override suspend fun deleteFolder(folderId: String): Result<Unit> {
    return try {
      runInTransaction {
        layoutDao.deleteFolderItemsForFolder(folderId)
        layoutDao.deleteFolderById(folderId)
        layoutDao.deletePlacementByFolderId(folderId)
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete folder $folderId", e)
      Result.failure(e)
    }
  }

  override suspend fun createFolderFromApps(
    spaceId: String,
    pageIndex: Int,
    positionIndex: Int,
    folderName: String,
    sourceApp: DiscoveredApp,
    targetApp: DiscoveredApp,
    sourcePlacementId: String?,
    targetPlacementId: String?,
    sourceDockItemId: String?
  ): Result<SpaceFolder> {
    return try {
      runInTransaction {
        val folderId = "folder_" + UUID.randomUUID().toString().replace("-", "").take(10)
        val folderEntity = SpaceFolderEntity(
          id = folderId,
          spaceId = spaceId,
          name = folderName.ifBlank { "Folder" },
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis()
        )
        layoutDao.insertFolder(folderEntity)

        val item1 = SpaceFolderItemEntity(
          id = "fitem_" + UUID.randomUUID().toString().replace("-", "").take(10),
          folderId = folderId,
          packageName = targetApp.packageName,
          componentName = targetApp.activityName,
          userHandleId = targetApp.userHandleId,
          orderIndex = 0
        )
        val item2 = SpaceFolderItemEntity(
          id = "fitem_" + UUID.randomUUID().toString().replace("-", "").take(10),
          folderId = folderId,
          packageName = sourceApp.packageName,
          componentName = sourceApp.activityName,
          userHandleId = sourceApp.userHandleId,
          orderIndex = 1
        )
        val itemsToInsert = if (targetApp.appIdentity.matches(sourceApp.appIdentity)) {
          listOf(item1)
        } else {
          listOf(item1, item2)
        }
        layoutDao.insertFolderItems(itemsToInsert)

        // Remove the original standalone placements if any
        if (!sourcePlacementId.isNullOrEmpty()) {
          layoutDao.deletePlacementById(sourcePlacementId)
        }
        if (!targetPlacementId.isNullOrEmpty()) {
          layoutDao.deletePlacementById(targetPlacementId)
        }

        // Remove the source dock item if dragged from dock
        if (!sourceDockItemId.isNullOrEmpty()) {
          layoutDao.deleteDockItemById(sourceDockItemId)
          val remainingDock = layoutDao.getDockItemsForSpace(spaceId).distinctBy { it.appIdentity }
          layoutDao.deleteAllDockItemsForSpace(spaceId)
          val reindexed = remainingDock.mapIndexed { idx, itm -> itm.copy(orderIndex = idx) }
          layoutDao.insertDockItems(reindexed)
        }

        // Add the folder placement
        val placementEntity = SpaceItemPlacementEntity(
          id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = pageIndex,
          positionIndex = positionIndex,
          itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
          folderId = folderId
        )
        layoutDao.insertPlacement(placementEntity)

        val domainFolder = folderEntity.toDomain(listOf(item1.toDomain(), item2.toDomain()))
        AppLogger.i(AppLogger.Category.LAUNCHER, "Created folder '${folderEntity.name}' ($folderId) with 2 apps atomically")
        Result.success(domainFolder)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to create folder", e)
      Result.failure(e)
    }
  }
}
