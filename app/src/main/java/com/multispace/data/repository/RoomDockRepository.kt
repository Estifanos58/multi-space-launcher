package com.multispace.data.repository

import com.multispace.data.dao.SpaceDao
import com.multispace.data.dao.SpaceLayoutDao
import com.multispace.data.entity.SpaceDockItemEntity
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.DockRepository
import com.multispace.domain.repository.PlacementRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDockRepository(
  private val spaceDao: SpaceDao,
  private val layoutDao: SpaceLayoutDao,
  private var placementRepository: PlacementRepository? = null
) : DockRepository {

  fun setPlacementRepository(repository: PlacementRepository) {
    this.placementRepository = repository
  }

  override fun getDockItemsForSpaceFlow(spaceId: String): Flow<List<SpaceDockItem>> {
    return layoutDao.getDockItemsForSpaceFlow(spaceId).map { list ->
      list.map { it.toDomain() }.distinctBy { it.packageName }
    }
  }

  override suspend fun getDockItemsForSpace(spaceId: String): List<SpaceDockItem> {
    return layoutDao.getDockItemsForSpace(spaceId).map { it.toDomain() }.distinctBy { it.packageName }
  }

  override suspend fun addAppToDock(spaceId: String, app: DiscoveredApp, orderIndex: Int): Result<Unit> {
    return try {
      val space = spaceDao.getSpaceById(spaceId)
      val capacity = space?.dockCapacity ?: Space.DEFAULT_DOCK_CAPACITY
      val current = layoutDao.getDockItemsForSpace(spaceId).distinctBy { it.packageName }.toMutableList()

      // Check if already in dock by packageName using AppIdentity
      val targetIdentity = app.appIdentity
      val existingIdx = current.indexOfFirst { it.toDomain().appIdentity.matches(targetIdentity) }
      if (existingIdx != -1) {
        if (orderIndex != -1 && orderIndex != existingIdx) {
          val item = current.removeAt(existingIdx)
          val targetIdx = orderIndex.coerceIn(0, current.size)
          current.add(targetIdx, item)
          val reindexed = current.mapIndexed { idx, itm -> itm.copy(orderIndex = idx) }
          layoutDao.deleteAllDockItemsForSpace(spaceId)
          layoutDao.insertDockItems(reindexed)
        }
        return Result.success(Unit)
      }

      if (current.size >= capacity) {
        val removed = current.removeAt(current.lastIndex)
        layoutDao.deleteDockItemById(removed.id)
      }

      val targetIdx = if (orderIndex in 0..current.size) orderIndex else current.size
      val newItem = SpaceDockItemEntity(
        id = "dock_" + UUID.randomUUID().toString().replace("-", "").take(10),
        spaceId = spaceId,
        orderIndex = targetIdx,
        packageName = app.packageName,
        componentName = app.activityName,
        userHandleId = app.userHandleId
      )
      current.add(targetIdx, newItem)

      val reindexed = current.distinctBy { it.packageName }.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
      layoutDao.deleteAllDockItemsForSpace(spaceId)
      layoutDao.insertDockItems(reindexed)
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add app to dock", e)
      Result.failure(e)
    }
  }

  override suspend fun removeAppFromDock(spaceId: String, dockItemId: String): Result<Unit> {
    return try {
      val allItems = layoutDao.getDockItemsForSpace(spaceId)
      val target = allItems.firstOrNull { it.id == dockItemId }
      if (target != null) {
        layoutDao.deleteDockItemsForPackage(target.packageName)
      } else {
        layoutDao.deleteDockItemById(dockItemId)
      }
      val remaining = layoutDao.getDockItemsForSpace(spaceId).distinctBy { it.packageName }
      layoutDao.deleteAllDockItemsForSpace(spaceId)
      val reindexed = remaining.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
      layoutDao.insertDockItems(reindexed)
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to remove app from dock", e)
      Result.failure(e)
    }
  }

  override suspend fun reorderDockItems(spaceId: String, dockItems: List<SpaceDockItem>): Result<Unit> {
    return try {
      val distinctItems = dockItems.distinctBy { it.packageName }
      layoutDao.deleteAllDockItemsForSpace(spaceId)
      val entities = distinctItems.mapIndexed { idx, item ->
        SpaceDockItemEntity(
          id = item.id,
          spaceId = spaceId,
          orderIndex = idx,
          packageName = item.packageName,
          componentName = item.componentName,
          userHandleId = item.userHandleId
        )
      }
      layoutDao.insertDockItems(entities)
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to reorder dock items", e)
      Result.failure(e)
    }
  }

  override suspend fun cleanupDuplicateDockItems(spaceId: String): Result<Unit> {
    return try {
      val items = layoutDao.getDockItemsForSpace(spaceId)
      val seen = mutableSetOf<String>()
      val toDelete = mutableListOf<String>()
      for (item in items) {
        if (!seen.add(item.packageName)) {
          toDelete.add(item.id)
        }
      }
      for (id in toDelete) {
        layoutDao.deleteDockItemById(id)
      }
      if (toDelete.isNotEmpty()) {
        val remaining = layoutDao.getDockItemsForSpace(spaceId)
        val reindexed = remaining.mapIndexed { idx, itm -> itm.copy(orderIndex = idx) }
        layoutDao.deleteAllDockItemsForSpace(spaceId)
        layoutDao.insertDockItems(reindexed)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Cleaned up ${toDelete.size} duplicate dock items for space $spaceId")
      }
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to clean duplicate dock items for $spaceId", e)
      Result.failure(e)
    }
  }

  override suspend fun moveAppFromHomeToDock(
    spaceId: String,
    placementId: String,
    app: DiscoveredApp,
    targetDockIndex: Int
  ): Result<Unit> {
    return try {
      val space = spaceDao.getSpaceById(spaceId)
      val capacity = space?.dockCapacity ?: Space.DEFAULT_DOCK_CAPACITY
      val currentDock = layoutDao.getDockItemsForSpace(spaceId).distinctBy { it.packageName }.toMutableList()

      // Find original placement details
      val originalPlacement = layoutDao.getPlacementById(placementId)
      val originalPage = originalPlacement?.pageIndex ?: 0
      val originalPos = originalPlacement?.positionIndex ?: 0

      // If dock is full and we're adding a new item, find which dock item will be displaced
      var displacedDockItem: SpaceDockItemEntity? = null
      if (currentDock.none { it.packageName == app.packageName } && currentDock.size >= capacity) {
        val removeIdx = if (targetDockIndex in 0 until currentDock.size) targetDockIndex else currentDock.lastIndex
        displacedDockItem = currentDock.getOrNull(removeIdx)
      }

      // 1. Remove placement from home desktop
      placementRepository?.removePlacement(placementId) ?: layoutDao.deletePlacementById(placementId)
      // Also purge any duplicate placement for this package from home
      val homePlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      val duplicates = homePlacements.filter { it.packageName == app.packageName }
      for (dup in duplicates) {
        layoutDao.deletePlacementById(dup.id)
      }

      // 2. Add app to dock
      addAppToDock(spaceId, app, targetDockIndex)

      // 3. If a dock item was displaced, place it on the desktop at the original spot
      if (displacedDockItem != null) {
        val displacedPkg = displacedDockItem.packageName
        val virtualId = "virtual:$displacedPkg"
        val cols = space?.gridColumns ?: Space.DEFAULT_GRID_COLUMNS
        val effectivePageSize = cols * 5
        placementRepository?.moveAppToPage(
          spaceId = spaceId,
          placementId = virtualId,
          targetPage = originalPage,
          targetPosition = originalPos,
          pageSize = effectivePageSize
        )
        AppLogger.i(AppLogger.Category.LAUNCHER, "Swapped displaced dock item '$displacedPkg' to desktop at page $originalPage, pos $originalPos")
      }

      AppLogger.i(AppLogger.Category.LAUNCHER, "Moved app '${app.label}' from Home ($placementId) to Dock at index $targetDockIndex")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to move app from Home to Dock", e)
      Result.failure(e)
    }
  }

  override suspend fun moveAppFromDockToHome(
    spaceId: String,
    dockItemId: String,
    app: DiscoveredApp,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int?
  ): Result<Unit> {
    return try {
      // 1. Remove from dock
      removeAppFromDock(spaceId, dockItemId)

      // 2. Insert into home placements with cascade
      val virtualId = "virtual:${app.packageName}"
      placementRepository?.moveAppToPage(
        spaceId = spaceId,
        placementId = virtualId,
        targetPage = targetPage,
        targetPosition = targetPosition,
        pageSize = pageSize
      )
      AppLogger.i(AppLogger.Category.LAUNCHER, "Moved app '${app.label}' from Dock ($dockItemId) to Home page $targetPage, pos $targetPosition")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to move app from Dock to Home", e)
      Result.failure(e)
    }
  }
}
