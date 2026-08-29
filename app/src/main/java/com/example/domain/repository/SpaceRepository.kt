package com.example.domain.repository

import com.example.domain.model.DiscoveredApp
import com.example.domain.model.Space
import com.example.domain.model.SpaceMembership
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
}
