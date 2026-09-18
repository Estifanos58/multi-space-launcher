package com.multispace.domain.repository

import com.multispace.domain.model.SpaceItemPlacement
import kotlinx.coroutines.flow.Flow

/**
 * Focused repository interface for managing Space item placements, desktop pages, and widgets.
 */
interface PlacementRepository {
  fun getPlacementsForSpaceLayerFlow(spaceId: String, layer: Int): Flow<List<SpaceItemPlacement>>
  suspend fun getPlacementsForSpaceLayer(spaceId: String, layer: Int): List<SpaceItemPlacement>
  suspend fun addPlacement(placement: SpaceItemPlacement): Result<Unit>
  suspend fun removePlacement(placementId: String): Result<Unit>
  suspend fun removePlacementByIdentity(spaceId: String, appIdentity: com.multispace.domain.model.AppIdentity): Result<Unit>
  suspend fun updatePlacements(placements: List<SpaceItemPlacement>): Result<Unit>
  suspend fun moveAppToPage(
    spaceId: String,
    placementId: String,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int? = null,
    appIdentity: com.multispace.domain.model.AppIdentity? = null
  ): Result<Unit>
  suspend fun addPage(spaceId: String): Result<Int>
  suspend fun deletePage(spaceId: String, pageIndex: Int): Result<Unit>
  suspend fun addWidgetPlacement(
    spaceId: String,
    pageIndex: Int,
    widgetType: String,
    spanX: Int = 1,
    spanY: Int = 1,
    appWidgetId: Int = -1,
    packageName: String? = null,
    componentName: String? = null
  ): Result<SpaceItemPlacement>
  suspend fun updateWidgetSpan(
    placementId: String,
    spanX: Int,
    spanY: Int,
    positionIndex: Int? = null
  ): Result<Unit>
}
