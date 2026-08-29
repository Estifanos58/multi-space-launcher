package com.multispace.domain.repository

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceMembership
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for Space management, Space persistence, and Space membership.
 */
interface SpaceRepository {
  val allSpacesFlow: Flow<List<Space>>
  val activeSpaceIdFlow: Flow<String?>
  val activeSpaceFlow: Flow<Space?>

  suspend fun ensureDefaultSpaceInitialized(): Result<Space>
  suspend fun getSpaceById(spaceId: String): Space?
  suspend fun setActiveSpaceId(spaceId: String): Result<Unit>
  suspend fun createSpace(name: String, layoutType: String = "GRID_4"): Result<Space>
  suspend fun renameSpace(spaceId: String, newName: String): Result<Unit>
  suspend fun deleteSpace(spaceId: String): Result<Unit>

  fun getMembershipsForSpaceFlow(spaceId: String): Flow<List<SpaceMembership>>
  suspend fun getMembershipsForSpace(spaceId: String): List<SpaceMembership>
  suspend fun addAppToSpace(spaceId: String, app: DiscoveredApp): Result<Unit>
  suspend fun removeAppFromSpace(spaceId: String, app: DiscoveredApp): Result<Unit>
  suspend fun isAppInSpace(spaceId: String, app: DiscoveredApp): Boolean

  suspend fun setSpacePin(spaceId: String, pin: String): Result<Unit>
  suspend fun changeSpacePin(spaceId: String, currentPin: String, newPin: String): Result<Unit>
  suspend fun disableSpacePin(spaceId: String, currentPin: String): Result<Unit>
  suspend fun verifySpacePin(spaceId: String, pin: String): Boolean

  suspend fun updateSpaceCustomization(
    spaceId: String,
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean
  ): Result<Unit>

  suspend fun reorderSpaceApp(
    spaceId: String,
    app: DiscoveredApp,
    direction: Int
  ): Result<Unit>

  suspend fun reorderSpaceApps(
    spaceId: String,
    orderedApps: List<DiscoveredApp>
  ): Result<Unit>
}
