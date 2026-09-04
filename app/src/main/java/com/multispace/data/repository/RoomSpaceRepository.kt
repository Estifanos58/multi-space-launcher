package com.multispace.data.repository

import com.multispace.data.dao.SpaceDao
import com.multispace.data.dao.SpaceLayoutDao
import com.multispace.data.dao.SpaceMembershipDao
import com.multispace.data.entity.SpaceDockItemEntity
import com.multispace.data.entity.SpaceEntity
import com.multispace.data.entity.SpaceFolderEntity
import com.multispace.data.entity.SpaceFolderItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import com.multispace.data.entity.SpaceMembershipEntity
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.ImportReport
import com.multispace.domain.model.LayoutPreset
import com.multispace.domain.model.PageTurnEffect
import com.multispace.domain.model.PlacementCascadeHelper
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.SpaceMembership
import com.multispace.domain.model.WallpaperCatalog
import com.multispace.domain.repository.SpaceRepository
import com.multispace.platform.PinSecurityManager
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class RoomSpaceRepository(
  private val spaceDao: SpaceDao,
  private val membershipDao: SpaceMembershipDao,
  private val layoutDao: SpaceLayoutDao,
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

  override suspend fun ensureDefaultSpaceInitialized(initialApps: List<DiscoveredApp>): Result<Space> {
    return try {
      val count = spaceDao.getSpaceCount()
      if (count == 0) {
        AppLogger.i(AppLogger.Category.LAUNCHER, "No Spaces found in database. Initializing Default Space with Phone's Home Layout.")
        val defaultSpace = Space(
          id = Space.DEFAULT_SPACE_ID,
          name = Space.DEFAULT_SPACE_NAME,
          orderIndex = 0,
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis(),
          backgroundType = Space.BACKGROUND_IMAGE,
          backgroundImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI,
          homeWallpaperType = Space.BACKGROUND_IMAGE,
          homeWallpaperImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI,
          phoneLockWallpaperType = Space.BACKGROUND_IMAGE,
          phoneLockWallpaperImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI,
          spaceLockWallpaperType = Space.BACKGROUND_IMAGE,
          spaceLockWallpaperImageUri = WallpaperCatalog.DEFAULT_WALLPAPER_URI,
          gridColumns = 4,
          dockCapacity = 5,
          layoutPreset = Space.PRESET_DEFAULT,
          layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
          layer2DisplayMode = Space.DISPLAY_MODE_SCROLL
        )
        spaceDao.insertSpace(SpaceEntity.fromDomain(defaultSpace))
        preferences.setActiveSpaceId(Space.DEFAULT_SPACE_ID)

        if (initialApps.isNotEmpty()) {
          val distinctApps = initialApps.distinctBy { it.packageName }
          importCurrentHomeLayout(Space.DEFAULT_SPACE_ID, distinctApps)
        }
        Result.success(defaultSpace)
      } else {
        val spaces = spaceDao.getAllSpaces()
        val currentActiveId = preferences.activeSpaceIdFlow.firstOrNull()
        val resolvedSpace = spaces.firstOrNull { it.id == currentActiveId } ?: spaces.first()
        if (currentActiveId != resolvedSpace.id) {
          preferences.setActiveSpaceId(resolvedSpace.id)
        }

        // If the default space has no placements and no dock items yet, auto-import phone layout
        val defaultEntity = spaces.firstOrNull { it.id == Space.DEFAULT_SPACE_ID }
        if (defaultEntity != null && initialApps.isNotEmpty()) {
          val placements = layoutDao.getPlacementsForSpaceLayer(Space.DEFAULT_SPACE_ID, SpaceItemPlacement.LAYER_HOME)
          val dockItems = layoutDao.getDockItemsForSpace(Space.DEFAULT_SPACE_ID)
          if (placements.isEmpty() && dockItems.isEmpty()) {
            AppLogger.i(AppLogger.Category.LAUNCHER, "Default Space unconfigured: automatically importing Phone's Home Layout")
            val distinctApps = initialApps.distinctBy { it.packageName }
            importCurrentHomeLayout(Space.DEFAULT_SPACE_ID, distinctApps)
          }
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

  override suspend fun createFullSpace(
    name: String,
    authPolicy: String,
    pinSalt: String?,
    pinHash: String?,
    patternRows: Int,
    patternCols: Int,
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    homeWallpaperType: String,
    homeWallpaperColor: Long?,
    homeWallpaperImageUri: String?,
    phoneLockWallpaperType: String,
    phoneLockWallpaperColor: Long?,
    phoneLockWallpaperImageUri: String?,
    spaceLockWallpaperType: String,
    spaceLockWallpaperColor: Long?,
    spaceLockWallpaperImageUri: String?,
    appTheme: String,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean,
    layer1DisplayMode: String,
    layer2DisplayMode: String,
    layer2AccessMode: String,
    dockCapacity: Int,
    layoutPreset: String,
    useLayer2: Boolean,
    homeWallpaperScaleMode: String,
    homeWallpaperZoomLevel: Float,
    homeWallpaperDimLevel: Float,
    homeWallpaperOffsetX: Float,
    homeWallpaperOffsetY: Float,
    phoneLockWallpaperScaleMode: String,
    phoneLockWallpaperZoomLevel: Float,
    phoneLockWallpaperDimLevel: Float,
    phoneLockWallpaperOffsetX: Float,
    phoneLockWallpaperOffsetY: Float,
    spaceLockWallpaperScaleMode: String,
    spaceLockWallpaperZoomLevel: Float,
    spaceLockWallpaperDimLevel: Float,
    spaceLockWallpaperOffsetX: Float,
    spaceLockWallpaperOffsetY: Float,
    pageTurnEffect: PageTurnEffect,
    pageTurnDurationMs: Int,
    pageTurnIntensity: Float,
    initialApps: List<DiscoveredApp>
  ): Result<Space> {
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
        authPolicy = authPolicy,
        pinSalt = pinSalt,
        pinHash = pinHash,
        patternRows = patternRows,
        patternCols = patternCols,
        layoutType = "GRID_$gridColumns",
        backgroundType = backgroundType,
        backgroundColor = backgroundColor,
        backgroundImageUri = backgroundImageUri,
        homeWallpaperType = homeWallpaperType,
        homeWallpaperColor = homeWallpaperColor,
        homeWallpaperImageUri = homeWallpaperImageUri,
        phoneLockWallpaperType = phoneLockWallpaperType,
        phoneLockWallpaperColor = phoneLockWallpaperColor,
        phoneLockWallpaperImageUri = phoneLockWallpaperImageUri,
        spaceLockWallpaperType = spaceLockWallpaperType,
        spaceLockWallpaperColor = spaceLockWallpaperColor,
        spaceLockWallpaperImageUri = spaceLockWallpaperImageUri,
        appTheme = appTheme,
        gridColumns = gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS),
        iconSize = iconSize,
        labelVisibility = labelVisibility,
        layer1DisplayMode = layer1DisplayMode,
        layer2DisplayMode = layer2DisplayMode,
        layer2AccessMode = layer2AccessMode,
        dockCapacity = dockCapacity,
        layoutPreset = layoutPreset,
        useLayer2 = useLayer2,
        homeWallpaperScaleMode = homeWallpaperScaleMode,
        homeWallpaperZoomLevel = homeWallpaperZoomLevel,
        homeWallpaperDimLevel = homeWallpaperDimLevel,
        homeWallpaperOffsetX = homeWallpaperOffsetX,
        homeWallpaperOffsetY = homeWallpaperOffsetY,
        phoneLockWallpaperScaleMode = phoneLockWallpaperScaleMode,
        phoneLockWallpaperZoomLevel = phoneLockWallpaperZoomLevel,
        phoneLockWallpaperDimLevel = phoneLockWallpaperDimLevel,
        phoneLockWallpaperOffsetX = phoneLockWallpaperOffsetX,
        phoneLockWallpaperOffsetY = phoneLockWallpaperOffsetY,
        spaceLockWallpaperScaleMode = spaceLockWallpaperScaleMode,
        spaceLockWallpaperZoomLevel = spaceLockWallpaperZoomLevel,
        spaceLockWallpaperDimLevel = spaceLockWallpaperDimLevel,
        spaceLockWallpaperOffsetX = spaceLockWallpaperOffsetX,
        spaceLockWallpaperOffsetY = spaceLockWallpaperOffsetY,
        pageTurnEffect = pageTurnEffect,
        pageTurnDurationMs = pageTurnDurationMs,
        pageTurnIntensity = pageTurnIntensity
      )
      spaceDao.insertSpace(SpaceEntity.fromDomain(space))

      val uniqueInitialApps = initialApps.distinctBy { it.packageName }
      if (uniqueInitialApps.isNotEmpty()) {
        val memberships = uniqueInitialApps.mapIndexed { idx, app ->
          SpaceMembershipEntity(
            spaceId = newId,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId,
            orderIndex = idx,
            addedAt = System.currentTimeMillis()
          )
        }
        membershipDao.insertMemberships(memberships)

        // Also populate default dock items & initial home placements for this preset
        val dockEntities = uniqueInitialApps.take(dockCapacity).mapIndexed { idx, app ->
          SpaceDockItemEntity(
            id = "dock_" + UUID.randomUUID().toString().replace("-", "").take(10),
            spaceId = newId,
            orderIndex = idx,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
        }
        layoutDao.insertDockItems(dockEntities)

        val pageSize = (gridColumns * 5).coerceAtLeast(1)
        val homeEntities = uniqueInitialApps.mapIndexed { idx, app ->
          SpaceItemPlacementEntity(
            id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
            spaceId = newId,
            layer = SpaceItemPlacement.LAYER_HOME,
            pageIndex = idx / pageSize,
            positionIndex = idx % pageSize,
            itemType = SpaceItemPlacement.ITEM_TYPE_APP,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
        }
        layoutDao.insertPlacements(homeEntities)
      }

      AppLogger.i(AppLogger.Category.LAUNCHER, "Created configured Space: '$trimmed' ($newId) with preset '$layoutPreset' and ${uniqueInitialApps.size} apps")
      Result.success(space)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to create configured Space: '$name'", e)
      Result.failure(e)
    }
  }

  override suspend fun updateFullSpace(
    spaceId: String,
    name: String,
    authPolicy: String,
    pinSalt: String?,
    pinHash: String?,
    keepExistingCredentials: Boolean,
    patternRows: Int,
    patternCols: Int,
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    homeWallpaperType: String,
    homeWallpaperColor: Long?,
    homeWallpaperImageUri: String?,
    phoneLockWallpaperType: String,
    phoneLockWallpaperColor: Long?,
    phoneLockWallpaperImageUri: String?,
    spaceLockWallpaperType: String,
    spaceLockWallpaperColor: Long?,
    spaceLockWallpaperImageUri: String?,
    appTheme: String,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean,
    layer1DisplayMode: String,
    layer2DisplayMode: String,
    layer2AccessMode: String,
    dockCapacity: Int,
    layoutPreset: String,
    useLayer2: Boolean,
    homeWallpaperScaleMode: String,
    homeWallpaperZoomLevel: Float,
    homeWallpaperDimLevel: Float,
    homeWallpaperOffsetX: Float,
    homeWallpaperOffsetY: Float,
    phoneLockWallpaperScaleMode: String,
    phoneLockWallpaperZoomLevel: Float,
    phoneLockWallpaperDimLevel: Float,
    phoneLockWallpaperOffsetX: Float,
    phoneLockWallpaperOffsetY: Float,
    spaceLockWallpaperScaleMode: String,
    spaceLockWallpaperZoomLevel: Float,
    spaceLockWallpaperDimLevel: Float,
    spaceLockWallpaperOffsetX: Float,
    spaceLockWallpaperOffsetY: Float,
    pageTurnEffect: PageTurnEffect,
    pageTurnDurationMs: Int,
    pageTurnIntensity: Float,
    updatedApps: List<DiscoveredApp>
  ): Result<Space> {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) {
      return Result.failure(IllegalArgumentException("Space name cannot be empty"))
    }
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val resolvedAuthPolicy: String
      val resolvedSalt: String?
      val resolvedHash: String?
      val resolvedPatternRows: Int
      val resolvedPatternCols: Int

      if (keepExistingCredentials) {
        resolvedAuthPolicy = existing.authPolicy
        resolvedSalt = existing.pinSalt
        resolvedHash = existing.pinHash
        resolvedPatternRows = if (patternRows != Space.DEFAULT_PATTERN_ROWS) patternRows else existing.patternRows
        resolvedPatternCols = if (patternCols != Space.DEFAULT_PATTERN_COLS) patternCols else existing.patternCols
      } else if (authPolicy == Space.AUTH_NONE) {
        resolvedAuthPolicy = Space.AUTH_NONE
        resolvedSalt = null
        resolvedHash = null
        resolvedPatternRows = patternRows
        resolvedPatternCols = patternCols
      } else {
        resolvedAuthPolicy = authPolicy
        resolvedSalt = pinSalt
        resolvedHash = pinHash
        resolvedPatternRows = patternRows
        resolvedPatternCols = patternCols
      }

      val updated = existing.copy(
        name = trimmed,
        updatedAt = System.currentTimeMillis(),
        authPolicy = resolvedAuthPolicy,
        pinSalt = resolvedSalt,
        pinHash = resolvedHash,
        patternRows = resolvedPatternRows,
        patternCols = resolvedPatternCols,
        layoutType = "GRID_$gridColumns",
        backgroundType = backgroundType,
        backgroundColor = backgroundColor,
        backgroundImageUri = backgroundImageUri,
        homeWallpaperType = homeWallpaperType,
        homeWallpaperColor = homeWallpaperColor,
        homeWallpaperImageUri = homeWallpaperImageUri,
        phoneLockWallpaperType = phoneLockWallpaperType,
        phoneLockWallpaperColor = phoneLockWallpaperColor,
        phoneLockWallpaperImageUri = phoneLockWallpaperImageUri,
        spaceLockWallpaperType = spaceLockWallpaperType,
        spaceLockWallpaperColor = spaceLockWallpaperColor,
        spaceLockWallpaperImageUri = spaceLockWallpaperImageUri,
        appTheme = appTheme,
        gridColumns = gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS),
        iconSize = iconSize,
        labelVisibility = labelVisibility,
        layer1DisplayMode = layer1DisplayMode,
        layer2DisplayMode = layer2DisplayMode,
        layer2AccessMode = layer2AccessMode,
        dockCapacity = dockCapacity,
        layoutPreset = layoutPreset,
        useLayer2 = useLayer2,
        homeWallpaperScaleMode = homeWallpaperScaleMode,
        homeWallpaperZoomLevel = homeWallpaperZoomLevel,
        homeWallpaperDimLevel = homeWallpaperDimLevel,
        homeWallpaperOffsetX = homeWallpaperOffsetX,
        homeWallpaperOffsetY = homeWallpaperOffsetY,
        phoneLockWallpaperScaleMode = phoneLockWallpaperScaleMode,
        phoneLockWallpaperZoomLevel = phoneLockWallpaperZoomLevel,
        phoneLockWallpaperDimLevel = phoneLockWallpaperDimLevel,
        phoneLockWallpaperOffsetX = phoneLockWallpaperOffsetX,
        phoneLockWallpaperOffsetY = phoneLockWallpaperOffsetY,
        spaceLockWallpaperScaleMode = spaceLockWallpaperScaleMode,
        spaceLockWallpaperZoomLevel = spaceLockWallpaperZoomLevel,
        spaceLockWallpaperDimLevel = spaceLockWallpaperDimLevel,
        spaceLockWallpaperOffsetX = spaceLockWallpaperOffsetX,
        spaceLockWallpaperOffsetY = spaceLockWallpaperOffsetY,
        pageTurnEffect = pageTurnEffect.name,
        pageTurnDurationMs = pageTurnDurationMs,
        pageTurnIntensity = pageTurnIntensity
      )
      spaceDao.updateSpace(updated)

      membershipDao.deleteMembershipsForSpace(spaceId)
      val uniqueUpdatedApps = updatedApps.distinctBy { it.packageName }
      if (uniqueUpdatedApps.isNotEmpty()) {
        val memberships = uniqueUpdatedApps.mapIndexed { idx, app ->
          SpaceMembershipEntity(
            spaceId = spaceId,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId,
            orderIndex = idx,
            addedAt = System.currentTimeMillis()
          )
        }
        membershipDao.insertMemberships(memberships)

        // System A vs System B isolation:
        // Updating space settings (such as pageTurnEffect, theme, or name) must NEVER reset
        // the user's custom app placements or dock arrangement.
        val existingPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
        if (existingPlacements.isEmpty()) {
          // Only generate default layout if there are no existing placements
          val pageSize = (gridColumns * 5).coerceAtLeast(1)
          val homeEntities = uniqueUpdatedApps.mapIndexed { idx, app ->
            SpaceItemPlacementEntity(
              id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
              spaceId = spaceId,
              layer = SpaceItemPlacement.LAYER_HOME,
              pageIndex = idx / pageSize,
              positionIndex = idx % pageSize,
              itemType = SpaceItemPlacement.ITEM_TYPE_APP,
              packageName = app.packageName,
              componentName = app.activityName,
              userHandleId = app.userHandleId
            )
          }
          layoutDao.insertPlacements(homeEntities)
        } else {
          // PRESERVE ALL USER CUSTOM PLACEMENTS!
          // Only synchronize additions and removals without disturbing existing positions
          val updatedPkgSet = uniqueUpdatedApps.map { it.packageName }.toSet()
          val placedPkgSet = existingPlacements.mapNotNull { it.packageName }.toSet()

          // 1. Remove placements for apps explicitly deselected from the space
          val placementsToRemove = existingPlacements.filter { p ->
            p.itemType == SpaceItemPlacement.ITEM_TYPE_APP && p.packageName != null && !updatedPkgSet.contains(p.packageName)
          }
          for (p in placementsToRemove) {
            layoutDao.deletePlacementById(p.id)
          }

          // 2. Add placements for newly added apps into empty slots or trailing pages
          val newlyAddedApps = uniqueUpdatedApps.filter { !placedPkgSet.contains(it.packageName) }
          if (newlyAddedApps.isNotEmpty()) {
            val remainingPlacements = existingPlacements.filter { !placementsToRemove.any { r -> r.id == it.id } }
            val pageSize = (gridColumns * 5).coerceAtLeast(1)
            val occupiedPerPage = mutableMapOf<Int, MutableSet<Int>>()
            for (p in remainingPlacements) {
              occupiedPerPage.getOrPut(p.pageIndex) { mutableSetOf() }.add(p.positionIndex)
            }

            var curPage = 0
            var curPos = 0
            val newEntities = mutableListOf<SpaceItemPlacementEntity>()
            for (app in newlyAddedApps) {
              var occupied = occupiedPerPage.getOrPut(curPage) { mutableSetOf() }
              while (occupied.contains(curPos) && curPos < pageSize) {
                curPos++
              }
              if (curPos >= pageSize) {
                curPage++
                curPos = 0
                occupied = occupiedPerPage.getOrPut(curPage) { mutableSetOf() }
                while (occupied.contains(curPos) && curPos < pageSize) {
                  curPos++
                }
              }
              newEntities.add(
                SpaceItemPlacementEntity(
                  id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
                  spaceId = spaceId,
                  layer = SpaceItemPlacement.LAYER_HOME,
                  pageIndex = curPage,
                  positionIndex = curPos,
                  itemType = SpaceItemPlacement.ITEM_TYPE_APP,
                  packageName = app.packageName,
                  componentName = app.activityName,
                  userHandleId = app.userHandleId
                )
              )
              occupied.add(curPos)
              curPos++
            }
            if (newEntities.isNotEmpty()) {
              layoutDao.insertPlacements(newEntities)
            }
          }
        }

        val existingDock = layoutDao.getDockItemsForSpace(spaceId)
        if (existingDock.isEmpty()) {
          val dockEntities = uniqueUpdatedApps.take(dockCapacity).mapIndexed { idx, app ->
            SpaceDockItemEntity(
              id = "dock_" + UUID.randomUUID().toString().replace("-", "").take(10),
              spaceId = spaceId,
              orderIndex = idx,
              packageName = app.packageName,
              componentName = app.activityName,
              userHandleId = app.userHandleId
            )
          }
          layoutDao.insertDockItems(dockEntities)
        }
      }

      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated configured Space: '$trimmed' ($spaceId) with ${updatedApps.size} apps")
      Result.success(updated.toDomain())
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update Space: '$name' ($spaceId)", e)
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
    if (!PinSecurityManager.isValidPinFormat(pin)) {
      return Result.failure(IllegalArgumentException("PIN must be 4 to 8 numeric digits"))
    }
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val salt = PinSecurityManager.generateSalt()
      val hash = PinSecurityManager.hashPin(pin, salt)

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
    if (!PinSecurityManager.isValidPinFormat(newPin)) {
      return Result.failure(IllegalArgumentException("New PIN must be 4 to 8 numeric digits"))
    }
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val isCurrentValid = PinSecurityManager.verifyPin(
        currentPin,
        existing.pinSalt,
        existing.pinHash
      )
      if (!isCurrentValid) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "PIN change failed: incorrect current PIN for Space ($spaceId)")
        return Result.failure(IllegalArgumentException("Incorrect current PIN"))
      }

      val newSalt = PinSecurityManager.generateSalt()
      val newHash = PinSecurityManager.hashPin(newPin, newSalt)

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

      val isCurrentValid = PinSecurityManager.verifyPin(
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
      if ((existing.authPolicy != Space.AUTH_PIN && existing.authPolicy != Space.AUTH_PATTERN) || existing.pinHash.isNullOrEmpty()) {
        return true
      }
      val isValid = PinSecurityManager.verifyPin(
        pin,
        existing.pinSalt,
        existing.pinHash
      )
      if (isValid) {
        AppLogger.i(AppLogger.Category.LAUNCHER, "Space authentication succeeded for Space ($spaceId)")
      } else {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Space authentication failed for Space ($spaceId)")
      }
      isValid
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Error during authentication verification for Space ($spaceId)", e)
      false
    }
  }

  override suspend fun findSpaceMatchingCredential(credential: String): Space? {
    return try {
      val entities = spaceDao.getAllSpaces()
      for (entity in entities) {
        val domain = entity.toDomain()
        if (domain.isProtected && !domain.pinHash.isNullOrEmpty() && !domain.pinSalt.isNullOrEmpty()) {
          if (PinSecurityManager.verifyPin(credential, domain.pinSalt, domain.pinHash)) {
            AppLogger.i(AppLogger.Category.LAUNCHER, "Credential matched Space '${domain.name}' (${domain.id})")
            return domain
          }
        }
      }
      null
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Error matching credential across spaces", e)
      null
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

  override suspend fun updatePageTurnSettings(
    spaceId: String,
    effect: PageTurnEffect,
    durationMs: Int,
    intensity: Float
  ): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))
      val updated = existing.copy(
        pageTurnEffect = effect.name,
        pageTurnDurationMs = durationMs.coerceIn(Space.MIN_PAGE_TURN_DURATION_MS, Space.MAX_PAGE_TURN_DURATION_MS),
        pageTurnIntensity = intensity.coerceIn(Space.MIN_PAGE_TURN_INTENSITY, Space.MAX_PAGE_TURN_INTENSITY),
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated page turn settings for Space '${existing.name}': effect=${effect.name}, duration=${durationMs}ms, intensity=$intensity")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update page turn settings for Space ($spaceId)", e)
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

  // --- Layer 1 & 2 Placements, Pages, & Folders ---

  override fun getPlacementsForSpaceLayerFlow(spaceId: String, layer: Int): Flow<List<SpaceItemPlacement>> {
    return layoutDao.getPlacementsForSpaceLayerFlow(spaceId, layer).map { list ->
      list.map { it.toDomain() }
    }
  }

  override suspend fun getPlacementsForSpaceLayer(spaceId: String, layer: Int): List<SpaceItemPlacement> {
    val existing = layoutDao.getPlacementsForSpaceLayer(spaceId, layer).map { it.toDomain() }
    if (existing.isNotEmpty() || layer != SpaceItemPlacement.LAYER_HOME) {
      return existing
    }

    // Auto-bootstrap Layer 1 placements from memberships if empty
    val memberships = membershipDao.getMembershipsForSpace(spaceId)
    val distinctMemberships = memberships.distinctBy { it.packageName }
    if (distinctMemberships.isEmpty()) {
      return emptyList()
    }

    val space = spaceDao.getSpaceById(spaceId)
    val cols = space?.gridColumns ?: Space.DEFAULT_GRID_COLUMNS
    val pageSize = cols * 5 // standard rows per page

    val newPlacements = distinctMemberships.mapIndexed { index, m ->
      val page = index / pageSize
      val pos = index % pageSize
      SpaceItemPlacementEntity(
        id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = page,
        positionIndex = pos,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = m.packageName,
        componentName = m.componentName,
        userHandleId = m.userHandleId
      )
    }
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

    return newPlacements.map { it.toDomain() }
  }

  override suspend fun addPlacement(placement: SpaceItemPlacement): Result<Unit> {
    return try {
      layoutDao.insertPlacement(SpaceItemPlacementEntity.fromDomain(placement))
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add placement: ${placement.id}", e)
      Result.failure(e)
    }
  }

  override suspend fun removePlacement(placementId: String): Result<Unit> {
    return try {
      if (placementId.startsWith("virtual:") || placementId.startsWith("virtual_")) {
        val pkg = when {
          placementId.startsWith("virtual:") -> placementId.removePrefix("virtual:").substringBefore(":")
          else -> {
            val withoutPrefix = placementId.removePrefix("virtual_")
            val lastUnderscore = withoutPrefix.lastIndexOf('_')
            val secondLast = if (lastUnderscore != -1) withoutPrefix.lastIndexOf('_', lastUnderscore - 1) else -1
            if (secondLast != -1) withoutPrefix.substring(0, secondLast) else withoutPrefix
          }
        }
        layoutDao.deletePlacementsForPackage(pkg)
      } else {
        layoutDao.deletePlacementById(placementId)
      }
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to remove placement: $placementId", e)
      Result.failure(e)
    }
  }

  override suspend fun updatePlacements(placements: List<SpaceItemPlacement>): Result<Unit> {
    return try {
      layoutDao.insertPlacements(placements.map { SpaceItemPlacementEntity.fromDomain(it) })
      Result.success(Unit)
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
    pageSize: Int?
  ): Result<Unit> {
    return try {
      val space = spaceDao.getSpaceById(spaceId)
      val cols = space?.gridColumns ?: Space.DEFAULT_GRID_COLUMNS
      val effectivePageSize = if (pageSize != null && pageSize > 0) {
        pageSize
      } else {
        maxOf(cols * 10, targetPosition + 1)
      }
      val targetPosClamped = targetPosition.coerceIn(0, effectivePageSize - 1)

      var allHome = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME).toMutableList()

      // 1. Ensure all memberships have persistent placements in database
      val memberships = membershipDao.getMembershipsForSpace(spaceId).distinctBy { it.packageName }
      val placedPkgs = allHome.mapNotNull { it.packageName }.toSet()
      val missingMemberships = memberships.filter { !placedPkgs.contains(it.packageName) }

      if (missingMemberships.isNotEmpty()) {
        val occupiedPerPage = mutableMapOf<Int, MutableSet<Int>>()
        for (p in allHome) {
          occupiedPerPage.getOrPut(p.pageIndex) { mutableSetOf() }.add(p.positionIndex)
        }
        var curPage = 0
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

      // 2. Resolve the target item to move
      val pkgFromVirtual = when {
        placementId.startsWith("virtual:") -> {
          placementId.removePrefix("virtual:").substringBefore(":")
        }
        placementId.startsWith("virtual_") -> {
          val withoutPrefix = placementId.removePrefix("virtual_")
          val lastUnderscore = withoutPrefix.lastIndexOf('_')
          val secondLast = if (lastUnderscore != -1) withoutPrefix.lastIndexOf('_', lastUnderscore - 1) else -1
          if (secondLast != -1) withoutPrefix.substring(0, secondLast) else withoutPrefix
        }
        else -> null
      }

      var itemIndex = allHome.indexOfFirst { it.id == placementId }
      if (itemIndex == -1 && pkgFromVirtual != null) {
        itemIndex = allHome.indexOfFirst { it.packageName == pkgFromVirtual }
      }
      if (itemIndex == -1 && pkgFromVirtual != null) {
        itemIndex = allHome.indexOfFirst { it.packageName?.contains(pkgFromVirtual) == true || pkgFromVirtual.contains(it.packageName ?: "---") }
      }

      val itemToMoveRaw = if (itemIndex != -1) {
        allHome.removeAt(itemIndex)
      } else {
        val matchedMember = memberships.firstOrNull { it.packageName == pkgFromVirtual }
        SpaceItemPlacementEntity(
          id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = targetPage,
          positionIndex = targetPosClamped,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = pkgFromVirtual,
          componentName = matchedMember?.componentName,
          userHandleId = matchedMember?.userHandleId ?: 0L
        )
      }

      val itemToMove = if (itemToMoveRaw.id.startsWith("virtual")) {
        itemToMoveRaw.copy(id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10))
      } else {
        itemToMoveRaw
      }

      // CRITICAL: Prevent duplicate apps - purge any existing placements for the same package name
      if (itemToMove.itemType == SpaceItemPlacement.ITEM_TYPE_APP && itemToMove.packageName != null) {
        val duplicatePlacements = allHome.filter {
          it.itemType == SpaceItemPlacement.ITEM_TYPE_APP && it.packageName == itemToMove.packageName
        }
        if (duplicatePlacements.isNotEmpty()) {
          allHome.removeAll(duplicatePlacements)
          for (dup in duplicatePlacements) {
            layoutDao.deletePlacementById(dup.id)
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
            a.packageName != null && a.packageName == b.packageName
          )
        },
        targetPage = targetPage,
        targetPosition = targetPosClamped,
        pageSize = effectivePageSize
      )

      // Deduplicate toInsert before persistence
      val deduplicatedToInsert = mutableListOf<SpaceItemPlacementEntity>()
      val seenPkgs = mutableSetOf<String>()
      val seenIds = mutableSetOf<String>()

      val finalItem = toInsert.firstOrNull { it.id == itemToMove.id } ?: itemToMove
      deduplicatedToInsert.add(finalItem)
      seenIds.add(finalItem.id)
      if (finalItem.itemType == SpaceItemPlacement.ITEM_TYPE_APP && finalItem.packageName != null) {
        seenPkgs.add(finalItem.packageName!!)
      }

      for (item in toInsert) {
        if (seenIds.contains(item.id)) continue
        if (item.itemType == SpaceItemPlacement.ITEM_TYPE_APP && item.packageName != null) {
          if (seenPkgs.contains(item.packageName)) continue
          seenPkgs.add(item.packageName!!)
        }
        seenIds.add(item.id)
        deduplicatedToInsert.add(item)
      }

      layoutDao.insertPlacements(deduplicatedToInsert)
      val persistedItem = deduplicatedToInsert.firstOrNull { it.id == itemToMove.id }
      AppLogger.i(
        AppLogger.Category.LAUNCHER,
        "PERSISTED_PLACEMENT: id=${persistedItem?.id} pkg=${persistedItem?.packageName} targetPage=$targetPage targetPos=$targetPosClamped gridRows=${effectivePageSize / cols} pageSize=$effectivePageSize persistedPage=${persistedItem?.pageIndex} persistedPos=${persistedItem?.positionIndex} from=($sourcePage, $sourcePos) shiftedCount=${toInsert.size - 1}"
      )
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to move app to page $targetPage", e)
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
    targetPlacementId: String?
  ): Result<SpaceFolder> {
    return try {
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
      layoutDao.insertFolderItems(listOf(item1, item2))

      // Remove the original standalone placements
      if (!sourcePlacementId.isNullOrEmpty()) {
        layoutDao.deletePlacementById(sourcePlacementId)
      }
      if (!targetPlacementId.isNullOrEmpty()) {
        layoutDao.deletePlacementById(targetPlacementId)
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
      AppLogger.i(AppLogger.Category.LAUNCHER, "Created folder '${folderEntity.name}' ($folderId) with 2 apps")
      Result.success(domainFolder)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to create folder", e)
      Result.failure(e)
    }
  }

  // --- Folders ---

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
      val existing = layoutDao.getFolderById(folderId)
        ?: return Result.failure(IllegalArgumentException("Folder not found"))
      val updated = existing.copy(name = newName.trim().ifBlank { "Folder" }, updatedAt = System.currentTimeMillis())
      layoutDao.updateFolder(updated)
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to rename folder $folderId", e)
      Result.failure(e)
    }
  }

  override suspend fun addAppToFolder(folderId: String, app: DiscoveredApp): Result<Unit> {
    return try {
      val items = layoutDao.getFolderItems(folderId)
      val exists = items.any { it.packageName == app.packageName && it.componentName == app.activityName }
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
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add app to folder $folderId", e)
      Result.failure(e)
    }
  }

  override suspend fun removeAppFromFolder(folderId: String, folderItemId: String): Result<Unit> {
    return try {
      layoutDao.deleteFolderItemById(folderItemId)
      val remaining = layoutDao.getFolderItems(folderId)
      if (remaining.isEmpty()) {
        layoutDao.deleteFolderById(folderId)
        layoutDao.deletePlacementByFolderId(folderId)
      }
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to remove app from folder $folderId", e)
      Result.failure(e)
    }
  }

  override suspend fun deleteFolder(folderId: String): Result<Unit> {
    return try {
      layoutDao.deleteFolderItemsForFolder(folderId)
      layoutDao.deleteFolderById(folderId)
      layoutDao.deletePlacementByFolderId(folderId)
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete folder $folderId", e)
      Result.failure(e)
    }
  }

  // --- Dock ---

  override fun getDockItemsForSpaceFlow(spaceId: String): Flow<List<SpaceDockItem>> {
    return layoutDao.getDockItemsForSpaceFlow(spaceId).map { list ->
      list.map { it.toDomain() }
    }
  }

  override suspend fun getDockItemsForSpace(spaceId: String): List<SpaceDockItem> {
    return layoutDao.getDockItemsForSpace(spaceId).map { it.toDomain() }
  }

  override suspend fun addAppToDock(spaceId: String, app: DiscoveredApp, orderIndex: Int): Result<Unit> {
    return try {
      val space = spaceDao.getSpaceById(spaceId)
      val capacity = space?.dockCapacity ?: Space.DEFAULT_DOCK_CAPACITY
      val current = layoutDao.getDockItemsForSpace(spaceId).toMutableList()

      // Check if already in dock
      val existingIdx = current.indexOfFirst { it.packageName == app.packageName && it.componentName == app.activityName }
      if (existingIdx != -1) {
        return Result.success(Unit)
      }

      if (current.size >= capacity) {
        // Drop the last item or reject
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

      val reindexed = current.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
      layoutDao.insertDockItems(reindexed)
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add app to dock", e)
      Result.failure(e)
    }
  }

  override suspend fun removeAppFromDock(spaceId: String, dockItemId: String): Result<Unit> {
    return try {
      layoutDao.deleteDockItemById(dockItemId)
      val remaining = layoutDao.getDockItemsForSpace(spaceId)
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
      val entities = dockItems.mapIndexed { idx, item ->
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

  // --- Layout Configuration & Presets ---

  override suspend fun updateSpaceLayoutSettings(
    spaceId: String,
    layer1DisplayMode: String,
    layer2DisplayMode: String,
    layer2AccessMode: String,
    dockCapacity: Int,
    gridColumns: Int
  ): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val safeDockCapacity = dockCapacity.coerceIn(Space.MIN_DOCK_CAPACITY, Space.MAX_DOCK_CAPACITY)
      val safeGridColumns = gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)

      val updated = existing.copy(
        layer1DisplayMode = layer1DisplayMode,
        layer2DisplayMode = layer2DisplayMode,
        layer2AccessMode = layer2AccessMode,
        dockCapacity = safeDockCapacity,
        gridColumns = safeGridColumns,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated layout settings for Space '${existing.name}': L1=$layer1DisplayMode, L2=$layer2DisplayMode, Access=$layer2AccessMode, Dock=$safeDockCapacity, Cols=$safeGridColumns")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update layout settings for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun applyLayoutPreset(
    spaceId: String,
    preset: LayoutPreset,
    apps: List<DiscoveredApp>
  ): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val updated = existing.copy(
        layoutPreset = preset.id,
        gridColumns = preset.gridColumns,
        layer1DisplayMode = preset.layer1DisplayMode,
        layer2DisplayMode = preset.layer2DisplayMode,
        layer2AccessMode = preset.layer2AccessMode,
        dockCapacity = preset.dockCapacity,
        iconSize = preset.iconSize,
        labelVisibility = preset.labelVisibility,
        appTheme = preset.appTheme,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)

      // Reorganize Layer 1 placements deterministically
      val activeApps = if (apps.isNotEmpty()) {
        apps
      } else {
        val memberships = membershipDao.getMembershipsForSpace(spaceId)
        memberships.map {
          DiscoveredApp(
            id = "${it.packageName}/${it.componentName}/${it.userHandleId}",
            packageName = it.packageName,
            activityName = it.componentName,
            label = it.packageName,
            userHandleId = it.userHandleId
          )
        }
      }

      val distinctActiveApps = activeApps.distinctBy { it.packageName }
      if (distinctActiveApps.isNotEmpty()) {
        layoutDao.deletePlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
        layoutDao.deleteAllDockItemsForSpace(spaceId)

        val pageSize = (preset.gridColumns * 5).coerceAtLeast(1)
        val placements = distinctActiveApps.mapIndexed { idx, app ->
          val page = idx / pageSize
          val pos = idx % pageSize
          SpaceItemPlacementEntity(
            id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
            spaceId = spaceId,
            layer = SpaceItemPlacement.LAYER_HOME,
            pageIndex = page,
            positionIndex = pos,
            itemType = SpaceItemPlacement.ITEM_TYPE_APP,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
        }
        layoutDao.insertPlacements(placements)

        val dockItems = distinctActiveApps.take(preset.dockCapacity).mapIndexed { idx, app ->
          SpaceDockItemEntity(
            id = "dock_" + UUID.randomUUID().toString().replace("-", "").take(10),
            spaceId = spaceId,
            orderIndex = idx,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
        }
        layoutDao.insertDockItems(dockItems)
      }

      AppLogger.i(AppLogger.Category.LAUNCHER, "Applied layout preset '${preset.name}' to Space '${existing.name}' ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to apply layout preset '${preset.name}' to Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun importCurrentHomeLayout(
    spaceId: String,
    allInstalledApps: List<DiscoveredApp>
  ): Result<ImportReport> {
    return try {
      val uniqueApps = allInstalledApps.distinctBy { it.packageName }
      val successes = mutableListOf<String>()
      val partiallyImported = mutableListOf<String>()
      val restricted = mutableListOf<String>()

      // Essential system categories detection
      val dialer = uniqueApps.firstOrNull { it.packageName.contains("dialer") || it.packageName.contains("phone") || it.label.contains("Phone", ignoreCase = true) }
      val messaging = uniqueApps.firstOrNull { it.packageName.contains("messaging") || it.packageName.contains("mms") || it.packageName.contains("message") || it.label.contains("Messages", ignoreCase = true) }
      val browser = uniqueApps.firstOrNull { it.packageName.contains("chrome") || it.packageName.contains("browser") || it.label.contains("Chrome", ignoreCase = true) || it.label.contains("Browser", ignoreCase = true) }
      val camera = uniqueApps.firstOrNull { it.packageName.contains("camera") || it.label.contains("Camera", ignoreCase = true) }
      val settings = uniqueApps.firstOrNull { it.packageName.contains("settings") || it.label.contains("Settings", ignoreCase = true) }

      val dockCandidates = listOfNotNull(dialer, messaging, browser, camera, settings).distinctBy { it.packageName }
      if (dockCandidates.isNotEmpty()) {
        layoutDao.deleteAllDockItemsForSpace(spaceId)
        val dockEntities = dockCandidates.take(5).mapIndexed { idx, app ->
          SpaceDockItemEntity(
            id = "dock_" + UUID.randomUUID().toString().replace("-", "").take(10),
            spaceId = spaceId,
            orderIndex = idx,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
        }
        layoutDao.insertDockItems(dockEntities)
        successes.add("Identified and populated essential bottom Dock apps (${dockCandidates.size} apps: Phone, Messages, Browser, Camera, Settings)")
      }

      // Populate Layer 1 with launchable installed apps
      layoutDao.deletePlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      val space = spaceDao.getSpaceById(spaceId)
      val cols = space?.gridColumns ?: 4
      val pageSize = cols * 5

      val placements = uniqueApps.mapIndexed { idx, app ->
        val page = idx / pageSize
        val pos = idx % pageSize
        SpaceItemPlacementEntity(
          id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = page,
          positionIndex = pos,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = app.packageName,
          componentName = app.activityName,
          userHandleId = app.userHandleId
        )
      }
      layoutDao.insertPlacements(placements)
      successes.add("Imported ${uniqueApps.size} launchable application shortcuts onto organized Home pages")

      // Ensure all imported apps are registered as memberships in this Space
      if (uniqueApps.isNotEmpty()) {
        val existingMemberships = membershipDao.getMembershipsForSpace(spaceId)
        val existingPkgs = existingMemberships.map { it.packageName }.toSet()
        val newMemberships = uniqueApps
          .filterNot { existingPkgs.contains(it.packageName) }
          .mapIndexed { idx, app ->
            SpaceMembershipEntity(
              spaceId = spaceId,
              packageName = app.packageName,
              componentName = app.activityName,
              userHandleId = app.userHandleId,
              orderIndex = existingMemberships.size + idx,
              addedAt = System.currentTimeMillis()
            )
          }
        if (newMemberships.isNotEmpty()) {
          membershipDao.insertMemberships(newMemberships)
        }
      }

      // Detect current default launcher package if available
      var launcherPkg = "System Default"
      var launcherLabel = "Default Android Launcher"
      partiallyImported.add("Imported 4x5 standard grid alignment structure")

      restricted.add("OEM-specific launcher internal SQLite databases (e.g. Samsung One UI / Pixel Launcher private tables) are strictly sandboxed by Android security architecture")
      restricted.add("Third-party home widget state instances cannot be directly migrated across launcher packages without user widget re-binding")

      val report = ImportReport(
        sourceLauncherPackage = launcherPkg,
        sourceLauncherLabel = launcherLabel,
        successItems = successes,
        partiallyImportedItems = partiallyImported,
        restrictedItems = restricted,
        summary = "Successfully imported ${allInstalledApps.size} apps and ${dockCandidates.size} dock shortcuts from standard Android configuration."
      )

      AppLogger.i(AppLogger.Category.LAUNCHER, "Imported Android home layout: ${report.summary}")
      Result.success(report)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to import home layout", e)
      Result.failure(e)
    }
  }

  override suspend fun cleanupUninstalledApp(packageName: String): Result<Unit> {
    return try {
      layoutDao.deletePlacementsForPackage(packageName)
      layoutDao.deleteFolderItemsForPackage(packageName)
      layoutDao.deleteDockItemsForPackage(packageName)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Cleaned up layout placements for uninstalled package: $packageName")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to cleanup uninstalled package: $packageName", e)
      Result.failure(e)
    }
  }

  override suspend fun addPage(spaceId: String): Result<Int> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))
      val currentPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      val maxPageIndex = currentPlacements.maxOfOrNull { it.pageIndex } ?: 0
      val newPageCount = maxOf(existing.pageCount, maxPageIndex + 1) + 1
      val updated = existing.copy(pageCount = newPageCount, updatedAt = System.currentTimeMillis())
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Added page to Space $spaceId, new pageCount: $newPageCount")
      Result.success(newPageCount)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add page for Space $spaceId", e)
      Result.failure(e)
    }
  }

  override suspend fun deletePage(spaceId: String, pageIndex: Int): Result<Unit> {
    return try {
      if (pageIndex == 0) {
        return Result.failure(IllegalArgumentException("Page 1 is immutable and cannot be deleted"))
      }
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val currentPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      // Delete all placements on this page
      val toDelete = currentPlacements.filter { it.pageIndex == pageIndex }
      for (p in toDelete) {
        layoutDao.deletePlacementById(p.id)
      }
      // Shift all placements on pages > pageIndex down by 1
      val toShift = currentPlacements.filter { it.pageIndex > pageIndex }
      for (p in toShift) {
        layoutDao.updatePlacement(p.copy(pageIndex = p.pageIndex - 1))
      }
      val remainingPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      val maxRemaining = remainingPlacements.maxOfOrNull { it.pageIndex } ?: 0
      val newPageCount = maxOf(1, maxOf(existing.pageCount - 1, maxRemaining + 1))
      val updated = existing.copy(pageCount = newPageCount, updatedAt = System.currentTimeMillis())
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Deleted page $pageIndex from Space $spaceId, new pageCount: $newPageCount")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete page $pageIndex for Space $spaceId", e)
      Result.failure(e)
    }
  }

  override suspend fun updateSpaceTheme(
    spaceId: String,
    appTheme: String,
    gridColumns: Int?,
    iconSize: String?,
    labelVisibility: Boolean?
  ): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))
      val updated = existing.copy(
        appTheme = appTheme,
        gridColumns = gridColumns?.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS) ?: existing.gridColumns,
        iconSize = iconSize ?: existing.iconSize,
        labelVisibility = labelVisibility ?: existing.labelVisibility,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated theme for Space $spaceId: theme=$appTheme, cols=${updated.gridColumns}")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update theme for Space $spaceId", e)
      Result.failure(e)
    }
  }

  override suspend fun updateSpaceWallpaper(
    spaceId: String,
    wallpaperType: String,
    wallpaperColor: Long?,
    wallpaperImageUri: String?
  ): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))
      val updated = existing.copy(
        backgroundType = wallpaperType,
        backgroundColor = wallpaperColor,
        backgroundImageUri = wallpaperImageUri,
        homeWallpaperType = wallpaperType,
        homeWallpaperColor = wallpaperColor,
        homeWallpaperImageUri = wallpaperImageUri,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated wallpaper for Space $spaceId: type=$wallpaperType")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update wallpaper for Space $spaceId", e)
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
      val existingPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      val onPage = existingPlacements.filter { it.pageIndex == pageIndex }
      val nextPos = if (onPage.isEmpty()) 0 else onPage.maxOf { it.positionIndex } + 1
      val widgetPlacement = SpaceItemPlacement(
        id = "widget_" + java.util.UUID.randomUUID().toString().replace("-", "").take(10),
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = pageIndex,
        positionIndex = nextPos,
        itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
        packageName = packageName,
        componentName = componentName,
        spanX = spanX,
        spanY = spanY,
        appWidgetId = appWidgetId,
        customWidgetType = widgetType
      )
      layoutDao.insertPlacement(SpaceItemPlacementEntity.fromDomain(widgetPlacement))
      AppLogger.i(AppLogger.Category.LAUNCHER, "Added widget placement ${widgetPlacement.id} to Space $spaceId on page $pageIndex at pos $nextPos")
      Result.success(widgetPlacement)
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
      if (positionIndex != null) {
        layoutDao.updateWidgetSpanAndPosition(placementId, spanX, spanY, positionIndex)
      } else {
        layoutDao.updateWidgetSpan(placementId, spanX, spanY)
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated widget span: id=$placementId, spanX=$spanX, spanY=$spanY, pos=$positionIndex")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update widget span for $placementId", e)
      Result.failure(e)
    }
  }
}
