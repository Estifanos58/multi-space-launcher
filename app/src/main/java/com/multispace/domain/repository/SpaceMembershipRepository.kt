package com.multispace.domain.repository

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceMembership
import kotlinx.coroutines.flow.Flow

/**
 * Focused repository interface for managing Space app memberships and ordering.
 */
interface SpaceMembershipRepository {
  fun getMembershipsForSpaceFlow(spaceId: String): Flow<List<SpaceMembership>>
  suspend fun getMembershipsForSpace(spaceId: String): List<SpaceMembership>
  suspend fun addAppToSpace(spaceId: String, app: DiscoveredApp): Result<Unit>
  suspend fun removeAppFromSpace(spaceId: String, app: DiscoveredApp): Result<Unit>
  suspend fun isAppInSpace(spaceId: String, app: DiscoveredApp): Boolean
  suspend fun reorderSpaceApp(spaceId: String, app: DiscoveredApp, direction: Int): Result<Unit>
  suspend fun reorderSpaceApps(spaceId: String, orderedApps: List<DiscoveredApp>): Result<Unit>
}
