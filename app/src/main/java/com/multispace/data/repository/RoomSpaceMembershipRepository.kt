package com.multispace.data.repository

import com.multispace.data.dao.SpaceMembershipDao
import com.multispace.data.entity.SpaceMembershipEntity
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceMembership
import com.multispace.domain.model.appIdentity
import com.multispace.domain.repository.SpaceMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSpaceMembershipRepository(
  private val membershipDao: SpaceMembershipDao
) : SpaceMembershipRepository {

  override fun getMembershipsForSpaceFlow(spaceId: String): Flow<List<SpaceMembership>> {
    return membershipDao.getMembershipsForSpaceFlow(spaceId).map { entities ->
      entities.map { it.toDomain() }
    }
  }

  override suspend fun getMembershipsForSpace(spaceId: String): List<SpaceMembership> {
    return try {
      membershipDao.getMembershipsForSpace(spaceId).map { it.toDomain() }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Error fetching memberships for space: $spaceId", e)
      emptyList()
    }
  }

  override suspend fun addAppToSpace(spaceId: String, app: DiscoveredApp): Result<Unit> {
    return try {
      if (isAppInSpace(spaceId, app)) {
        AppLogger.d(AppLogger.Category.LAUNCHER, "App '${app.label}' is already in Space ($spaceId), skipping duplicate insertion")
        return Result.success(Unit)
      }
      val existingCount = membershipDao.getMembershipCountForSpace(spaceId)
      val membership = SpaceMembership(
        spaceId = spaceId,
        packageName = app.packageName,
        componentName = app.activityName,
        userHandleId = app.userHandleId,
        orderIndex = existingCount,
        addedAt = System.currentTimeMillis()
      )
      membershipDao.insertMembership(SpaceMembershipEntity.fromDomain(membership))
      AppLogger.i(AppLogger.Category.LAUNCHER, "Added app '${app.label}' to Space ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add app '${app.label}' to Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun removeAppFromSpace(spaceId: String, app: DiscoveredApp): Result<Unit> {
    return try {
      val memberships = membershipDao.getMembershipsForSpace(spaceId)
      val targetIdentity = app.appIdentity
      val matching = memberships.filter { it.toDomain().appIdentity.matches(targetIdentity) }
      if (matching.isNotEmpty()) {
        for (m in matching) {
          membershipDao.deleteMembership(
            spaceId = spaceId,
            packageName = m.packageName,
            componentName = m.componentName,
            userHandleId = m.userHandleId
          )
        }
      } else {
        membershipDao.deleteMembership(
          spaceId = spaceId,
          packageName = app.packageName,
          componentName = app.activityName,
          userHandleId = app.userHandleId
        )
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Removed app '${app.label}' from Space ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to remove app '${app.label}' from Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun isAppInSpace(spaceId: String, app: DiscoveredApp): Boolean {
    return try {
      val memberships = membershipDao.getMembershipsForSpace(spaceId)
      val targetIdentity = app.appIdentity
      memberships.any { it.toDomain().appIdentity.matches(targetIdentity) }
    } catch (e: Exception) {
      false
    }
  }

  override suspend fun reorderSpaceApp(
    spaceId: String,
    app: DiscoveredApp,
    direction: Int
  ): Result<Unit> {
    return try {
      val memberships = membershipDao.getMembershipsForSpace(spaceId).toMutableList()
      val targetIdentity = app.appIdentity
      val index = memberships.indexOfFirst {
        it.toDomain().appIdentity.matches(targetIdentity)
      }
      if (index == -1) {
        return Result.failure(IllegalArgumentException("App not found in Space memberships"))
      }
      val targetIndex = index + direction
      if (targetIndex < 0 || targetIndex >= memberships.size) {
        return Result.success(Unit) // Already at boundary
      }

      // Swap
      val item = memberships.removeAt(index)
      memberships.add(targetIndex, item)

      // Update indices
      memberships.forEachIndexed { i, m ->
        membershipDao.updateMembershipOrder(
          spaceId = spaceId,
          packageName = m.packageName,
          componentName = m.componentName,
          userHandleId = m.userHandleId,
          newOrderIndex = i
        )
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Reordered app '${app.label}' in Space ($spaceId) to index $targetIndex")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to reorder app in Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun reorderSpaceApps(
    spaceId: String,
    orderedApps: List<DiscoveredApp>
  ): Result<Unit> {
    return try {
      val memberships = membershipDao.getMembershipsForSpace(spaceId)
      val membershipByIdentity = memberships.associateBy { it.appIdentity }

      orderedApps.forEachIndexed { index, app ->
        val membership = membershipByIdentity[app.appIdentity]
          ?: memberships.firstOrNull { it.appIdentity.matches(app.appIdentity) }
          ?: memberships.firstOrNull { it.packageName == app.packageName }
        if (membership != null) {
          membershipDao.updateMembershipOrder(
            spaceId = spaceId,
            packageName = membership.packageName,
            componentName = membership.componentName,
            userHandleId = membership.userHandleId,
            newOrderIndex = index
          )
        }
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Reordered all ${orderedApps.size} apps in Space ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to reorder apps in Space ($spaceId)", e)
      Result.failure(e)
    }
  }
}
