package com.multispace.domain.repository

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceDockItem
import kotlinx.coroutines.flow.Flow

/**
 * Focused repository interface for managing Space persistent dock items.
 */
interface DockRepository {
  fun getDockItemsForSpaceFlow(spaceId: String): Flow<List<SpaceDockItem>>
  suspend fun getDockItemsForSpace(spaceId: String): List<SpaceDockItem>
  suspend fun addAppToDock(spaceId: String, app: DiscoveredApp, orderIndex: Int = -1): Result<Unit>
  suspend fun removeAppFromDock(spaceId: String, dockItemId: String): Result<Unit>
  suspend fun reorderDockItems(spaceId: String, dockItems: List<SpaceDockItem>): Result<Unit>
  suspend fun cleanupDuplicateDockItems(spaceId: String): Result<Unit>
  suspend fun moveAppFromHomeToDock(spaceId: String, placementId: String, app: DiscoveredApp, targetDockIndex: Int = -1): Result<Unit>
  suspend fun moveAppFromDockToHome(spaceId: String, dockItemId: String, app: DiscoveredApp, targetPage: Int, targetPosition: Int, pageSize: Int? = null): Result<Unit>
}
