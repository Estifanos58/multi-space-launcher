package com.example.data.repository

import com.example.data.dao.SpaceDao
import com.example.data.dao.SpaceMembershipDao
import com.example.data.entity.SpaceEntity
import com.example.data.entity.SpaceMembershipEntity
import com.example.data.preferences.LauncherPreferences
import com.example.diagnostics.AppLogger
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.Space
import com.example.domain.model.SpaceMembership
import com.example.domain.repository.SpaceRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class RoomSpaceRepository(
  private val spaceDao: SpaceDao,
  private val membershipDao: SpaceMembershipDao,
  private val preferences: LauncherPreferences
) : SpaceRepository {

  override val allSpacesFlow: Flow<List<Space>> = spaceDao.getAllSpacesFlow().map { entities ->
    entities.map { it.toDomain() }
  }

  override val activeSpaceIdFlow: Flow<String?> = preferences.activeSpaceIdFlow

  override val activeSpaceFlow: Flow<Space?> = combine(
    allSpacesFlow,
    activeSpaceIdFlow
  ) { spaces, activeId ->
    if (spaces.isEmpty()) {
      null
    } else {
      spaces.firstOrNull { it.id == activeId } ?: spaces.first()
    }
  }

  override suspend fun ensureDefaultSpaceInitialized(): Result<Space> {
    return try {
      val count = spaceDao.getSpaceCount()
      if (count == 0) {
        AppLogger.i(AppLogger.Category.LAUNCHER, "No Spaces found in database. Initializing Default Space.")
        val defaultSpace = Space(
          id = Space.DEFAULT_SPACE_ID,
          name = Space.DEFAULT_SPACE_NAME,
          orderIndex = 0,
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis()
        )
        spaceDao.insertSpace(SpaceEntity.fromDomain(defaultSpace))
        preferences.setActiveSpaceId(Space.DEFAULT_SPACE_ID)
        Result.success(defaultSpace)
      } else {
        val spaces = spaceDao.getAllSpaces()
        val currentActiveId = preferences.activeSpaceIdFlow.firstOrNull()
        val resolvedSpace = spaces.firstOrNull { it.id == currentActiveId } ?: spaces.first()
        if (currentActiveId != resolvedSpace.id) {
          preferences.setActiveSpaceId(resolvedSpace.id)
        }
        Result.success(resolvedSpace.toDomain())
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to initialize default Space", e)
      Result.failure(e)
    }
  }

  override suspend fun getSpaceById(spaceId: String): Space? {
    return try {
      spaceDao.getSpaceById(spaceId)?.toDomain()
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Error fetching space by id: $spaceId", e)
      null
    }
  }

  override suspend fun setActiveSpaceId(spaceId: String): Result<Unit> {
    return try {
      val space = spaceDao.getSpaceById(spaceId)
      if (space == null) {
        Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))
      } else {
        preferences.setActiveSpaceId(spaceId)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Active Space updated to '${space.name}' ($spaceId)")
        Result.success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to set active Space id: $spaceId", e)
      Result.failure(e)
    }
  }

  override suspend fun createSpace(name: String, layoutType: String): Result<Space> {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) {
      return Result.failure(IllegalArgumentException("Space name cannot be empty"))
    }
    return try {
      val newId = "space_" + UUID.randomUUID().toString().replace("-", "").take(12)
      val orderIndex = spaceDao.getSpaceCount()
      val space = Space(
        id = newId,
        name = trimmed,
        orderIndex = orderIndex,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        layoutType = layoutType
      )
      spaceDao.insertSpace(SpaceEntity.fromDomain(space))
      AppLogger.i(AppLogger.Category.LAUNCHER, "Created new Space: '$trimmed' ($newId)")
      Result.success(space)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to create Space: '$name'", e)
      Result.failure(e)
    }
  }

  override suspend fun renameSpace(spaceId: String, newName: String): Result<Unit> {
    val trimmed = newName.trim()
    if (trimmed.isEmpty()) {
      return Result.failure(IllegalArgumentException("Space name cannot be empty"))
    }
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val updated = existing.copy(
        name = trimmed,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Renamed Space ($spaceId) from '${existing.name}' to '$trimmed'")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to rename Space ($spaceId) to '$newName'", e)
      Result.failure(e)
    }
  }

  override suspend fun deleteSpace(spaceId: String): Result<Unit> {
    return try {
      val count = spaceDao.getSpaceCount()
      if (count <= 1) {
        return Result.failure(IllegalStateException("Cannot delete the only remaining Space"))
      }

      val allSpaces = spaceDao.getAllSpaces()
      val target = allSpaces.firstOrNull { it.id == spaceId }
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val currentActiveId = preferences.activeSpaceIdFlow.firstOrNull()
      if (currentActiveId == spaceId) {
        // Fall back active space to another valid space before deleting
        val fallback = allSpaces.first { it.id != spaceId }
        preferences.setActiveSpaceId(fallback.id)
        AppLogger.i(AppLogger.Category.LAUNCHER, "Active Space fallback to '${fallback.name}' prior to deleting '$spaceId'")
      }

      spaceDao.deleteSpaceById(spaceId)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Deleted Space '${target.name}' ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete Space ($spaceId)", e)
      Result.failure(e)
    }
  }

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
      val deletedCount = membershipDao.deleteMembership(
        spaceId = spaceId,
        packageName = app.packageName,
        componentName = app.activityName,
        userHandleId = app.userHandleId
      )
      if (deletedCount == 0) {
        membershipDao.deleteMembershipByPackage(spaceId = spaceId, packageName = app.packageName)
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
      memberships.any {
        it.packageName == app.packageName &&
          (it.componentName == app.activityName || it.componentName.isEmpty() || app.activityName.isEmpty())
      }
    } catch (e: Exception) {
      false
    }
  }

  override suspend fun setSpacePin(spaceId: String, pin: String): Result<Unit> {
    if (!com.example.platform.PinSecurityManager.isValidPinFormat(pin)) {
      return Result.failure(IllegalArgumentException("PIN must be 4 to 8 numeric digits"))
    }
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val salt = com.example.platform.PinSecurityManager.generateSalt()
      val hash = com.example.platform.PinSecurityManager.hashPin(pin, salt)

      val updated = existing.copy(
        authPolicy = "PIN",
        pinSalt = salt,
        pinHash = hash,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "PIN protection enabled for Space '${existing.name}' ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to enable PIN for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun changeSpacePin(spaceId: String, currentPin: String, newPin: String): Result<Unit> {
    if (!com.example.platform.PinSecurityManager.isValidPinFormat(newPin)) {
      return Result.failure(IllegalArgumentException("New PIN must be 4 to 8 numeric digits"))
    }
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val isCurrentValid = com.example.platform.PinSecurityManager.verifyPin(
        currentPin,
        existing.pinSalt,
        existing.pinHash
      )
      if (!isCurrentValid) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "PIN change failed: incorrect current PIN for Space ($spaceId)")
        return Result.failure(IllegalArgumentException("Incorrect current PIN"))
      }

      val newSalt = com.example.platform.PinSecurityManager.generateSalt()
      val newHash = com.example.platform.PinSecurityManager.hashPin(newPin, newSalt)

      val updated = existing.copy(
        authPolicy = "PIN",
        pinSalt = newSalt,
        pinHash = newHash,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "PIN changed for Space '${existing.name}' ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to change PIN for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun disableSpacePin(spaceId: String, currentPin: String): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val isCurrentValid = com.example.platform.PinSecurityManager.verifyPin(
        currentPin,
        existing.pinSalt,
        existing.pinHash
      )
      if (!isCurrentValid) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "PIN disable failed: incorrect current PIN for Space ($spaceId)")
        return Result.failure(IllegalArgumentException("Incorrect current PIN"))
      }

      val updated = existing.copy(
        authPolicy = "NONE",
        pinSalt = null,
        pinHash = null,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "PIN protection disabled for Space '${existing.name}' ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to disable PIN for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun verifySpacePin(spaceId: String, pin: String): Boolean {
    return try {
      val existing = spaceDao.getSpaceById(spaceId) ?: return false
      if (existing.authPolicy != "PIN" || existing.pinHash.isNullOrEmpty()) {
        return true
      }
      val isValid = com.example.platform.PinSecurityManager.verifyPin(
        pin,
        existing.pinSalt,
        existing.pinHash
      )
      if (isValid) {
        AppLogger.i(AppLogger.Category.LAUNCHER, "Space PIN authentication succeeded for Space ($spaceId)")
      } else {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Space PIN authentication failed for Space ($spaceId)")
      }
      isValid
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Error during PIN verification for Space ($spaceId)", e)
      false
    }
  }

  override suspend fun updateSpaceCustomization(
    spaceId: String,
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean
  ): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val safeGridColumns = gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
      val safeIconSize = when (iconSize) {
        Space.ICON_SIZE_SMALL, Space.ICON_SIZE_LARGE -> iconSize
        else -> Space.ICON_SIZE_MEDIUM
      }
      val safeBgType = when (backgroundType) {
        Space.BACKGROUND_COLOR, Space.BACKGROUND_IMAGE -> backgroundType
        else -> Space.BACKGROUND_DEFAULT
      }

      val updated = existing.copy(
        backgroundType = safeBgType,
        backgroundColor = backgroundColor,
        backgroundImageUri = backgroundImageUri,
        gridColumns = safeGridColumns,
        iconSize = safeIconSize,
        labelVisibility = labelVisibility,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated customization for Space '${existing.name}' ($spaceId): bg=$safeBgType, cols=$safeGridColumns, iconSize=$safeIconSize")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update customization for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun reorderSpaceApp(
    spaceId: String,
    app: DiscoveredApp,
    direction: Int
  ): Result<Unit> {
    return try {
      val memberships = membershipDao.getMembershipsForSpace(spaceId).toMutableList()
      val index = memberships.indexOfFirst {
        it.packageName == app.packageName &&
          (it.componentName == app.activityName || it.componentName.isEmpty() || app.activityName.isEmpty())
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
      val membershipMap = memberships.associateBy { "${it.packageName}/${it.componentName}" }

      orderedApps.forEachIndexed { index, app ->
        val key = "${app.packageName}/${app.activityName}"
        val membership = membershipMap[key] ?: memberships.firstOrNull { it.packageName == app.packageName }
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
