package com.multispace.data.repository

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.multispace.data.dao.SpaceDao
import com.multispace.data.dao.SpaceLayoutDao
import com.multispace.data.dao.SpaceMembershipDao
import com.multispace.data.entity.SpaceDockItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.PlacementCascadeHelper
import com.multispace.domain.model.PlacementValidator
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.PlacementRepository
import com.multispace.platform.AppDiscoveryManager
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPlacementRepository(
  private val spaceDao: SpaceDao,
  private val layoutDao: SpaceLayoutDao,
  private val membershipDao: SpaceMembershipDao? = null,
  private val context: Context? = null,
  private val database: RoomDatabase? = null
) : PlacementRepository {

  private suspend fun <T> runInTransaction(block: suspend () -> T): T {
    return if (database != null) {
      database.withTransaction { block() }
    } else {
      block()
    }
  }

  override fun getPlacementsForSpaceLayerFlow(spaceId: String, layer: Int): Flow<List<SpaceItemPlacement>> {
    return layoutDao.getPlacementsForSpaceLayerFlow(spaceId, layer).map { list ->
      val domainList = list.map { it.toDomain() }
      val report = PlacementValidator.validatePlacements(domainList)
      if (report.hasIssues) {
        AppLogger.w(
          AppLogger.Category.LAUNCHER,
          "Placement validation detected ${report.issues.size} issues for Space '$spaceId' (layer $layer): ${report.issues.take(3)}"
        )
      }
      domainList
    }
  }

  override suspend fun getPlacementsForSpaceLayer(spaceId: String, layer: Int): List<SpaceItemPlacement> {
    val existing = layoutDao.getPlacementsForSpaceLayer(spaceId, layer).map { it.toDomain() }
    if (existing.isNotEmpty() || layer != SpaceItemPlacement.LAYER_HOME) {
      val report = PlacementValidator.validatePlacements(existing)
      if (report.hasIssues) {
        AppLogger.w(
          AppLogger.Category.LAUNCHER,
          "Placement validation detected ${report.issues.size} issues in loaded placements for Space '$spaceId' (layer $layer): ${report.issues.take(3)}"
        )
      }
      return existing
    }

    // Auto-bootstrap Layer 1 placements from memberships if empty
    if (membershipDao == null) {
      return emptyList()
    }
    val memberships = membershipDao.getMembershipsForSpace(spaceId)
    val distinctMemberships = memberships.distinctBy { it.appIdentity }
    if (distinctMemberships.isEmpty()) {
      return emptyList()
    }

    val space = spaceDao.getSpaceById(spaceId)
    val cols = space?.gridColumns ?: Space.DEFAULT_GRID_COLUMNS
    val pageSize = cols * 5 // standard rows per page

    val lastRow = 4
    val page0Count = minOf(cols, distinctMemberships.size)
    val newPlacements = mutableListOf<SpaceItemPlacementEntity>()
    for (i in 0 until page0Count) {
      val m = distinctMemberships[i]
      newPlacements.add(
        SpaceItemPlacementEntity(
          id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = 0,
          positionIndex = lastRow * cols + i,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = m.packageName,
          componentName = m.componentName,
          userHandleId = m.userHandleId
        )
      )
    }
    for (i in page0Count until distinctMemberships.size) {
      val m = distinctMemberships[i]
      val rem = i - page0Count
      newPlacements.add(
        SpaceItemPlacementEntity(
          id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = 1 + (rem / pageSize),
          positionIndex = rem % pageSize,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = m.packageName,
          componentName = m.componentName,
          userHandleId = m.userHandleId
        )
      )
    }

    runInTransaction {
      layoutDao.insertPlacements(newPlacements)

      // Auto-bootstrap Dock if empty
      val dockItems = layoutDao.getDockItemsForSpace(spaceId)
      if (dockItems.isEmpty()) {
        val dockCap = space?.dockCapacity ?: Space.DEFAULT_DOCK_CAPACITY
        val newDock = distinctMemberships.take(dockCap).mapIndexed { idx, m ->
          SpaceDockItemEntity(
            id = "dock_" + UUID.randomUUID().toString().replace("-", "").take(10),
            spaceId = spaceId,
            orderIndex = idx,
            packageName = m.packageName,
            componentName = m.componentName,
            userHandleId = m.userHandleId
          )
        }
        layoutDao.insertDockItems(newDock)
      }
    }

    return newPlacements.map { it.toDomain() }
  }

  override suspend fun addPlacement(placement: SpaceItemPlacement): Result<Unit> {
    return try {
      runInTransaction {
        // Invariant 1: If an app placement, remove any previous placement of this app on this layer/space to prevent duplicates
        if (placement.itemType == SpaceItemPlacement.ITEM_TYPE_APP) {
          val targetIdentity = placement.appIdentity
          if (targetIdentity != null) {
            val existing = layoutDao.getPlacementsForSpaceLayer(placement.spaceId, placement.layer)
            for (p in existing) {
              if (p.itemType == SpaceItemPlacement.ITEM_TYPE_APP && p.id != placement.id) {
                if (p.appIdentity?.matches(targetIdentity) == true) {
                  layoutDao.deletePlacementById(p.id)
                }
              }
            }
          }
        }
        // Invariant 2: If a folder placement, verify that folder exists to prevent orphans
        if (placement.itemType == SpaceItemPlacement.ITEM_TYPE_FOLDER && !placement.folderId.isNullOrBlank()) {
          val folder = layoutDao.getFolderById(placement.folderId)
          if (folder == null) {
            AppLogger.w(AppLogger.Category.LAUNCHER, "Cannot place folder '${placement.folderId}': folder entity does not exist")
            throw IllegalStateException("Folder '${placement.folderId}' does not exist")
          }
        }
        layoutDao.insertPlacement(SpaceItemPlacementEntity.fromDomain(placement))
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add placement: ${placement.id}", e)
      Result.failure(e)
    }
  }

  override suspend fun removePlacement(placementId: String): Result<Unit> {
    return try {
      runInTransaction {
        if (placementId.startsWith("virtual:") || placementId.startsWith("virtual_") || placementId.startsWith("fallback:")) {
          val targetIdentity = AppIdentity.parseFromIdentifier(placementId)
            ?: throw IllegalArgumentException("Cannot safely remove virtual placement without complete profile identity: $placementId")
          if (targetIdentity.componentName.isNotBlank()) {
            layoutDao.deletePlacementsForIdentity(targetIdentity.packageName, targetIdentity.componentName, targetIdentity.userHandleId)
          } else {
            layoutDao.deletePlacementsForPackage(targetIdentity.packageName, targetIdentity.userHandleId)
          }
        } else {
          layoutDao.deletePlacementById(placementId)
        }
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to remove placement: $placementId", e)
      Result.failure(e)
    }
  }

  override suspend fun removePlacementByIdentity(spaceId: String, appIdentity: AppIdentity): Result<Unit> {
    return try {
      runInTransaction {
        val existing = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
        val targets = existing.filter { it.itemType == SpaceItemPlacement.ITEM_TYPE_APP && it.appIdentity?.matches(appIdentity) == true }
        for (t in targets) {
          layoutDao.deletePlacementById(t.id)
        }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to remove placement by identity: $appIdentity", e)
      Result.failure(e)
    }
  }

  override suspend fun updatePlacements(placements: List<SpaceItemPlacement>): Result<Unit> {
    return try {
      runInTransaction {
        layoutDao.insertPlacements(placements.map { SpaceItemPlacementEntity.fromDomain(it) })
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update placements", e)
      Result.failure(e)
    }
  }

  override suspend fun moveAppToPage(
    spaceId: String,
    placementId: String,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int?,
    appIdentity: AppIdentity?
  ): Result<Unit> {
    return try {
      runInTransaction {
        val space = spaceDao.getSpaceById(spaceId)
        val cols = space?.gridColumns ?: Space.DEFAULT_GRID_COLUMNS
        val effectivePageSize = if (pageSize != null && pageSize > 0) {
          pageSize
        } else {
          maxOf(cols * 10, targetPosition + 1)
        }
        val targetPosClamped = targetPosition.coerceIn(0, effectivePageSize - 1)

        var allHome = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME).toMutableList()

        // 1. Ensure all memberships have persistent placements
        val memberships = membershipDao?.getMembershipsForSpace(spaceId)?.distinctBy { it.appIdentity } ?: emptyList()
        val placedIdentities = allHome.mapNotNull { it.appIdentity }.toSet()
        val missingMemberships = memberships.filter { mem -> placedIdentities.none { it.matches(mem.appIdentity) } }

        if (missingMemberships.isNotEmpty()) {
          val occupiedPerPage = mutableMapOf<Int, MutableSet<Int>>()
          for (p in allHome) {
            val sX = if (p.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) p.spanX.coerceIn(1, cols) else 1
            val sY = if (p.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) p.spanY.coerceIn(1, 5) else 1
            val r = p.positionIndex / cols
            val c = p.positionIndex % cols
            for (dr in 0 until sY) {
              for (dc in 0 until sX) {
                occupiedPerPage.getOrPut(p.pageIndex) { mutableSetOf() }.add((r + dr) * cols + (c + dc))
              }
            }
          }
          var curPage = 1
          var curPos = 0
          val bootstrapped = mutableListOf<SpaceItemPlacementEntity>()
          for (m in missingMemberships) {
            var occupied = occupiedPerPage.getOrPut(curPage) { mutableSetOf() }
            while (occupied.contains(curPos) && curPos < effectivePageSize) {
              curPos++
            }
            if (curPos >= effectivePageSize) {
              curPage++
              curPos = 0
              occupied = occupiedPerPage.getOrPut(curPage) { mutableSetOf() }
              while (occupied.contains(curPos) && curPos < effectivePageSize) {
                curPos++
              }
            }
            val entity = SpaceItemPlacementEntity(
              id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
              spaceId = spaceId,
              layer = SpaceItemPlacement.LAYER_HOME,
              pageIndex = curPage,
              positionIndex = curPos,
              itemType = SpaceItemPlacement.ITEM_TYPE_APP,
              packageName = m.packageName,
              componentName = m.componentName,
              userHandleId = m.userHandleId
            )
            bootstrapped.add(entity)
            allHome.add(entity)
            occupied.add(curPos)
            curPos++
          }
          layoutDao.insertPlacements(bootstrapped)
        }

        // 2. Resolve the target item to move strictly using canonical AppIdentity
        val targetIdentity = appIdentity ?: AppIdentity.parseFromIdentifier(placementId)

        var itemIndex = allHome.indexOfFirst { it.id == placementId }
        if (itemIndex == -1 && targetIdentity != null) {
          itemIndex = allHome.indexOfFirst {
            it.itemType == SpaceItemPlacement.ITEM_TYPE_APP && it.appIdentity?.matches(targetIdentity) == true
          }
        }

        val itemToMoveRaw = if (itemIndex != -1) {
          allHome.removeAt(itemIndex)
        } else if (targetIdentity != null) {
          val matchedMember = memberships.firstOrNull { it.appIdentity.matches(targetIdentity) }
          val discoveredApp = if (matchedMember == null && context != null) {
            try {
              AppDiscoveryManager(context).loadInstalledApps().firstOrNull { it.appIdentity.matches(targetIdentity) }
            } catch (e: Exception) {
              null
            }
          } else null

          val compName = when {
            targetIdentity.componentName.isNotEmpty() -> targetIdentity.componentName
            matchedMember != null && matchedMember.componentName.isNotEmpty() -> matchedMember.componentName
            discoveredApp != null && discoveredApp.activityName.isNotEmpty() -> discoveredApp.activityName
            else -> ""
          }

          SpaceItemPlacementEntity(
            id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
            spaceId = spaceId,
            layer = SpaceItemPlacement.LAYER_HOME,
            pageIndex = targetPage,
            positionIndex = targetPosClamped,
            itemType = SpaceItemPlacement.ITEM_TYPE_APP,
            packageName = targetIdentity.packageName,
            componentName = compName,
            userHandleId = targetIdentity.userHandleId
          )
        } else {
          AppLogger.w(
            AppLogger.Category.LAUNCHER,
            "Cannot resolve placement to move: placementId='$placementId' is not present in Space '$spaceId' and cannot be parsed to an AppIdentity"
          )
          throw IllegalArgumentException("Cannot resolve placement for ID: $placementId")
        }

        val itemToMove = if (itemToMoveRaw.id.startsWith("virtual") || itemToMoveRaw.id.startsWith("fallback")) {
          itemToMoveRaw.copy(id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10))
        } else {
          itemToMoveRaw
        }

        // CRITICAL: Prevent duplicate apps - purge any existing placements for the exact same app identity
        if (itemToMove.itemType == SpaceItemPlacement.ITEM_TYPE_APP) {
          val targetAppIdentity = itemToMove.appIdentity
          if (targetAppIdentity != null) {
            val duplicatePlacements = allHome.filter {
              it.itemType == SpaceItemPlacement.ITEM_TYPE_APP && it.appIdentity?.matches(targetAppIdentity) == true
            }
            if (duplicatePlacements.isNotEmpty()) {
              allHome.removeAll(duplicatePlacements)
              for (dup in duplicatePlacements) {
                layoutDao.deletePlacementById(dup.id)
              }
            }
          }
        }

        val sourcePage = itemToMove.pageIndex
        val sourcePos = itemToMove.positionIndex

        // 3. Resolve collisions and cascade-shift occupying items across pages until an empty slot is reached
        val toInsert = PlacementCascadeHelper.cascadeInsertGeneric(
          existingItems = allHome,
          itemToInsert = itemToMove,
          getId = { it.id },
          getPage = { it.pageIndex },
          getPosition = { it.positionIndex },
          copyItem = { entity, page, pos -> entity.copy(pageIndex = page, positionIndex = pos) },
          isSameItem = { a, b ->
            a.id == b.id || (
              a.itemType == SpaceItemPlacement.ITEM_TYPE_APP &&
              b.itemType == SpaceItemPlacement.ITEM_TYPE_APP &&
              a.appIdentity != null && b.appIdentity != null &&
              a.appIdentity!!.matches(b.appIdentity!!)
            )
          },
          getSpanX = { if (it.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) it.spanX else 1 },
          getSpanY = { if (it.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) it.spanY else 1 },
          targetPage = targetPage,
          targetPosition = targetPosClamped,
          pageSize = effectivePageSize,
          cols = cols
        )

        // Deduplicate toInsert before persistence using AppIdentity
        val deduplicatedToInsert = mutableListOf<SpaceItemPlacementEntity>()
        val seenIdentities = mutableListOf<AppIdentity>()
        val seenIds = mutableSetOf<String>()

        val finalItem = toInsert.firstOrNull { it.id == itemToMove.id } ?: itemToMove
        deduplicatedToInsert.add(finalItem)
        seenIds.add(finalItem.id)
        finalItem.appIdentity?.let { seenIdentities.add(it) }

        for (item in toInsert) {
          if (seenIds.contains(item.id)) continue
          val itemIdentity = item.appIdentity
          if (item.itemType == SpaceItemPlacement.ITEM_TYPE_APP && itemIdentity != null) {
            if (seenIdentities.any { it.matches(itemIdentity) }) continue
            seenIdentities.add(itemIdentity)
          }
          seenIds.add(item.id)
          deduplicatedToInsert.add(item)
        }

        layoutDao.insertPlacements(deduplicatedToInsert)
        val persistedItem = deduplicatedToInsert.firstOrNull { it.id == itemToMove.id }
        AppLogger.i(
          AppLogger.Category.LAUNCHER,
          "PERSISTED_PLACEMENT: id=${persistedItem?.id} pkg=${persistedItem?.packageName} user=${persistedItem?.userHandleId} targetPage=$targetPage targetPos=$targetPosClamped gridRows=${effectivePageSize / cols} pageSize=$effectivePageSize persistedPage=${persistedItem?.pageIndex} persistedPos=${persistedItem?.positionIndex} from=($sourcePage, $sourcePos) shiftedCount=${toInsert.size - 1}"
        )
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to move app to page $targetPage", e)
      Result.failure(e)
    }
  }

  override suspend fun addPage(spaceId: String): Result<Int> {
    return try {
      runInTransaction {
        val existing = spaceDao.getSpaceById(spaceId)
          ?: throw IllegalArgumentException("Space with id '$spaceId' not found")

        val currentPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
        val maxPlacementPage = currentPlacements.maxOfOrNull { it.pageIndex } ?: 0
        val currentPages = maxOf(existing.pageCount, maxPlacementPage + 1)

        if (currentPages >= Space.MAX_PAGES) {
          throw IllegalStateException("Maximum of ${Space.MAX_PAGES} pages allowed.")
        }

        val newPageCount = currentPages + 1
        val updated = existing.copy(
          pageCount = newPageCount,
          updatedAt = System.currentTimeMillis()
        )
        spaceDao.updateSpace(updated)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Created Layer 1 Page index ${newPageCount - 1} for Space '${existing.name}' ($spaceId). Total pages: $newPageCount")
        Result.success(newPageCount - 1)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to create page for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun deletePage(spaceId: String, pageIndex: Int): Result<Unit> {
    if (pageIndex == 0) {
      return Result.failure(IllegalArgumentException("Page 1 cannot be deleted"))
    }
    return try {
      runInTransaction {
        val existing = spaceDao.getSpaceById(spaceId)
          ?: throw IllegalArgumentException("Space with id '$spaceId' not found")

        // 1. Remove placements specifically on target page from Layer 1
        layoutDao.deletePlacementsForPage(spaceId, SpaceItemPlacement.LAYER_HOME, pageIndex)

        // 2. Decrement page indices of any placements beyond target page
        layoutDao.decrementPageIndicesAbove(spaceId, SpaceItemPlacement.LAYER_HOME, pageIndex)

        // 3. Decrement pageCount in space
        val newPageCount = maxOf(1, existing.pageCount - 1)
        val updated = existing.copy(
          pageCount = newPageCount,
          updatedAt = System.currentTimeMillis()
        )
        spaceDao.updateSpace(updated)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Deleted Layer 1 Page index $pageIndex for Space '${existing.name}' ($spaceId). New page count: $newPageCount")
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete page $pageIndex for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun addWidgetPlacement(
    spaceId: String,
    pageIndex: Int,
    widgetType: String,
    spanX: Int,
    spanY: Int,
    appWidgetId: Int,
    packageName: String?,
    componentName: String?
  ): Result<SpaceItemPlacement> {
    return try {
      runInTransaction {
        val space = spaceDao.getSpaceById(spaceId)
          ?: throw IllegalArgumentException("Space with id '$spaceId' not found")
        val cols = space.gridColumns.coerceAtLeast(1)
        val effectivePageSize = cols * 5
        val existingPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
          .map { it.toDomain() }

        val placementResult = PlacementCascadeHelper.findEmptySlotForWidget(
          existingPlacements = existingPlacements,
          preferredPage = pageIndex,
          spanX = spanX,
          spanY = spanY,
          cols = cols,
          pageSize = effectivePageSize,
          existingPageCount = space.pageCount
        )

        if (placementResult.isNewPage) {
          val newPageCount = placementResult.pageIndex + 1
          val updatedSpace = space.copy(
            pageCount = maxOf(space.pageCount, newPageCount),
            updatedAt = System.currentTimeMillis()
          )
          spaceDao.updateSpace(updatedSpace)
          AppLogger.i(
            AppLogger.Category.LAUNCHER,
            "Created new page ${placementResult.pageIndex} for Space $spaceId, new total pageCount: $newPageCount"
          )
        }

        val widgetPlacement = SpaceItemPlacement(
          id = "widget_" + UUID.randomUUID().toString().replace("-", "").take(10),
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = placementResult.pageIndex,
          positionIndex = placementResult.positionIndex,
          itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
          packageName = packageName,
          componentName = componentName,
          spanX = spanX,
          spanY = spanY,
          appWidgetId = appWidgetId,
          customWidgetType = widgetType
        )

        layoutDao.insertPlacement(SpaceItemPlacementEntity.fromDomain(widgetPlacement))
        AppLogger.i(
          AppLogger.Category.LAUNCHER,
          "Added widget placement ${widgetPlacement.id} to Space $spaceId on page ${widgetPlacement.pageIndex} at pos ${widgetPlacement.positionIndex} (isNewPage=${placementResult.isNewPage}, span=${spanX}x${spanY})"
        )
        Result.success(widgetPlacement)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add widget to Space $spaceId", e)
      Result.failure(e)
    }
  }

  override suspend fun updateWidgetSpan(
    placementId: String,
    spanX: Int,
    spanY: Int,
    positionIndex: Int?
  ): Result<Unit> {
    return try {
      runInTransaction {
        val entity = layoutDao.getPlacementById(placementId)
          ?: throw IllegalArgumentException("Placement with id '$placementId' not found")

        val updated = entity.copy(
          spanX = spanX.coerceAtLeast(1),
          spanY = spanY.coerceAtLeast(1),
          positionIndex = positionIndex ?: entity.positionIndex
        )
        layoutDao.updatePlacement(updated)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Updated widget placement ($placementId) span to ${spanX}x${spanY}")
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update widget span for placement $placementId", e)
      Result.failure(e)
    }
  }

  override suspend fun reconcilePlacements(spaceId: String, cols: Int, rows: Int): Result<Int> {
    return try {
      runInTransaction {
        val placements = layoutDao.getAllPlacementsForSpace(spaceId).map { it.toDomain() }
        val report = PlacementValidator.validatePlacements(placements, cols = cols, rows = rows)
        if (!report.hasIssues) {
          return@runInTransaction Result.success(0)
        }
        var fixedCount = 0
        val byLayerAndPage = placements.groupBy { Pair(it.layer, it.pageIndex.coerceAtLeast(0)) }
        val maxSlot = (cols * rows).coerceAtLeast(1)
        val updatedPlacements = mutableListOf<SpaceItemPlacement>()

        for ((_, pageItems) in byLayerAndPage) {
          val occupied = mutableSetOf<Int>()
          for (item in pageItems) {
            var pos = item.positionIndex
            val safePage = item.pageIndex.coerceAtLeast(0)
            if (pos < 0 || occupied.contains(pos)) {
              var candidate = 0
              while (occupied.contains(candidate)) {
                candidate++
              }
              pos = candidate
              fixedCount++
            }
            occupied.add(pos)
            val updated = item.copy(
              pageIndex = if (pos >= maxSlot && item.layer == SpaceItemPlacement.LAYER_HOME) safePage + (pos / maxSlot) else safePage,
              positionIndex = if (pos >= maxSlot && item.layer == SpaceItemPlacement.LAYER_HOME) pos % maxSlot else pos
            )
            updatedPlacements.add(updated)
          }
        }
        layoutDao.insertPlacements(updatedPlacements.map { SpaceItemPlacementEntity.fromDomain(it) })
        layoutDao.pruneDuplicatePlacements()
        layoutDao.pruneDuplicatePositions()
        AppLogger.i(AppLogger.Category.LAUNCHER, "Reconciled placements for space $spaceId ($fixedCount adjusted)")
        Result.success(fixedCount)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to reconcile placements for space $spaceId", e)
      Result.failure(e)
    }
  }
}
