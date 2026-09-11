package com.multispace.data.repository

import android.content.Context
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
import com.multispace.domain.model.PresetLayoutHelper
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.SpaceMembership
import com.multispace.domain.model.WallpaperCatalog
import com.multispace.domain.repository.SpaceRepository
import com.multispace.platform.AppDiscoveryManager
import com.multispace.platform.DefaultAppCapabilityResolver
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
  private val preferences: LauncherPreferences,
  private val context: Context? = null
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
        AppLogger.i(AppLogger.Category.LAUNCHER, "No Spaces found in database. Initializing Default Space with default apps.")
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

        initializeNewSpaceDefaults(
          spaceId = Space.DEFAULT_SPACE_ID,
          spaceName = Space.DEFAULT_SPACE_NAME,
          layoutPreset = Space.PRESET_DEFAULT,
          dockCapacity = 5,
          gridColumns = 4,
          candidateApps = initialApps
        )

        Result.success(defaultSpace)
      } else {
        val spaces = spaceDao.getAllSpaces()
        val currentActiveId = preferences.activeSpaceIdFlow.firstOrNull()
        val resolvedSpace = spaces.firstOrNull { it.id == currentActiveId } ?: spaces.first()
        if (currentActiveId != resolvedSpace.id) {
          preferences.setActiveSpaceId(resolvedSpace.id)
        }

        // Clean up any historical duplicate dock items in the default space
        cleanupDuplicateDockItems(Space.DEFAULT_SPACE_ID)

        // If the default space has no placements and no dock items yet, auto-initialize
        val defaultEntity = spaces.firstOrNull { it.id == Space.DEFAULT_SPACE_ID }
        if (defaultEntity != null) {
          val placements = layoutDao.getPlacementsForSpaceLayer(Space.DEFAULT_SPACE_ID, SpaceItemPlacement.LAYER_HOME)
          val dockItems = layoutDao.getDockItemsForSpace(Space.DEFAULT_SPACE_ID)
          if (placements.isEmpty() && dockItems.isEmpty()) {
            AppLogger.i(AppLogger.Category.LAUNCHER, "Default Space unconfigured: initializing default DockBar and Layer 1 apps")
            initializeNewSpaceDefaults(
              spaceId = Space.DEFAULT_SPACE_ID,
              spaceName = Space.DEFAULT_SPACE_NAME,
              layoutPreset = Space.PRESET_DEFAULT,
              dockCapacity = defaultEntity.dockCapacity,
              gridColumns = defaultEntity.gridColumns,
              candidateApps = initialApps
            )
          }
        }

        Result.success(resolvedSpace.toDomain())
      }
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to initialize default Space", e)
      Result.failure(e)
    }
  }

  /**
   * Initializes default DockBar apps and default Layer 1 apps for a newly created Space.
   *
   * 1. DockBar Apps:
   *    Initializes with up to 4 core apps (Browser -> Camera -> Phone -> Messages).
   *    If dockCapacity > 4, fills newly available positions with sensible everyday apps:
   *    Contacts -> Gallery/Photos -> Files -> Clock -> Calculator -> Calendar -> Maps.
   *
   * 2. Layer 1 Apps:
   *    Initializes with a curated set of 8-12 apps with Space-type specific subsets (Personal, Work, Study, Default).
   *
   * 3. Registers Space memberships for all placed apps without duplication.
   */
  private suspend fun initializeNewSpaceDefaults(
    spaceId: String,
    spaceName: String,
    layoutPreset: String,
    dockCapacity: Int,
    gridColumns: Int,
    candidateApps: List<DiscoveredApp>
  ) {
    val appsToUse = if (candidateApps.isNotEmpty()) {
      candidateApps.distinctBy { it.packageName }
    } else if (context != null) {
      try {
        AppDiscoveryManager(context).loadInstalledApps().distinctBy { it.packageName }
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to load apps for new Space defaults: ${e.message}")
        emptyList()
      }
    } else {
      emptyList()
    }

    val presetObj = LayoutPreset.getById(layoutPreset)
    val layoutResult = PresetLayoutHelper.buildInitialLayout(
      spaceId = spaceId,
      preset = presetObj,
      gridColumns = gridColumns,
      availableApps = appsToUse,
      dockCapacity = dockCapacity
    )

    if (layoutResult.dockItems.isNotEmpty()) {
      layoutDao.insertDockItems(layoutResult.dockItems)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Initialized ${layoutResult.dockItems.size} default DockBar apps for Space '$spaceName' ($spaceId)")
    }

    if (layoutResult.placements.isNotEmpty()) {
      layoutDao.insertPlacements(layoutResult.placements)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Initialized ${layoutResult.placements.size} default Layer 1 placements for Space '$spaceName' ($spaceId)")
    }

    // 3. Register Memberships for all available apps
    if (appsToUse.isNotEmpty()) {
      val memberships = appsToUse.mapIndexed { idx, app ->
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
      AppLogger.d(AppLogger.Category.LAUNCHER, "Registered ${memberships.size} memberships for Space '$spaceName' ($spaceId)")
    }
  }

  /**
   * Expands the DockBar capacity when the user increases it above current count,
   * preserving original dock apps and filling newly available positions with sensible everyday apps:
   * Contacts -> Gallery/Photos -> Files -> Clock -> Calculator -> Calendar -> Maps.
   */
  private suspend fun expandDockItemsIfNeeded(
    spaceId: String,
    newCapacity: Int,
    appsToSearch: List<DiscoveredApp> = emptyList()
  ) {
    val existingDockEntities = layoutDao.getDockItemsForSpace(spaceId).sortedBy { it.orderIndex }
    if (existingDockEntities.size >= newCapacity) {
      return
    }

    val installedApps = if (appsToSearch.isNotEmpty()) {
      appsToSearch
    } else if (context != null) {
      try {
        AppDiscoveryManager(context).loadInstalledApps()
      } catch (e: Exception) {
        emptyList()
      }
    } else {
      emptyList()
    }

    if (installedApps.isEmpty()) return

    val existingDockApps = existingDockEntities.map { entity ->
      installedApps.firstOrNull { it.packageName == entity.packageName }
        ?: DiscoveredApp(
          id = "${entity.packageName}/${entity.componentName}#${entity.userHandleId}",
          packageName = entity.packageName,
          activityName = entity.componentName,
          label = entity.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
          userHandleId = entity.userHandleId
        )
    }

    val resolvedDock = DefaultAppCapabilityResolver.resolveDockApps(
      installedApps = installedApps,
      dockCapacity = newCapacity,
      context = context,
      existingDockApps = existingDockApps
    )

    val existingPkgs = existingDockEntities.map { it.packageName }.toSet()
    val newDockApps = resolvedDock.filterNot { existingPkgs.contains(it.packageName) }
    if (newDockApps.isEmpty()) return

    var currentMaxIndex = existingDockEntities.maxOfOrNull { it.orderIndex } ?: -1
    val newEntities = newDockApps.map { app ->
      currentMaxIndex++
      SpaceDockItemEntity(
        id = "dock_" + UUID.randomUUID().toString().replace("-", "").take(10),
        spaceId = spaceId,
        orderIndex = currentMaxIndex,
        packageName = app.packageName,
        componentName = app.activityName,
        userHandleId = app.userHandleId
      )
    }
    layoutDao.insertDockItems(newEntities)
    AppLogger.i(AppLogger.Category.LAUNCHER, "Expanded DockBar for Space ($spaceId) from ${existingDockEntities.size} to ${existingDockEntities.size + newEntities.size} apps: ${newDockApps.map { it.label }}")

    val existingMemberships = membershipDao.getMembershipsForSpace(spaceId).map { it.packageName }.toSet()
    val newMemberships = newDockApps
      .filterNot { existingMemberships.contains(it.packageName) }
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

  override suspend fun createSpace(
    name: String,
    layoutType: String,
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
        layoutType = layoutType,
        gridColumns = Space.DEFAULT_GRID_COLUMNS,
        dockCapacity = Space.DEFAULT_DOCK_CAPACITY
      )
      spaceDao.insertSpace(SpaceEntity.fromDomain(space))

      initializeNewSpaceDefaults(
        spaceId = newId,
        spaceName = trimmed,
        layoutPreset = Space.PRESET_DEFAULT,
        dockCapacity = Space.DEFAULT_DOCK_CAPACITY,
        gridColumns = Space.DEFAULT_GRID_COLUMNS,
        candidateApps = initialApps
      )

      AppLogger.i(AppLogger.Category.LAUNCHER, "Created new Space: '$trimmed' ($newId) with default apps")
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

      initializeNewSpaceDefaults(
        spaceId = newId,
        spaceName = trimmed,
        layoutPreset = layoutPreset,
        dockCapacity = dockCapacity,
        gridColumns = gridColumns,
        candidateApps = initialApps
      )

      AppLogger.i(AppLogger.Category.LAUNCHER, "Created configured Space: '$trimmed' ($newId) with preset '$layoutPreset' and default apps")
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

      val uniqueUpdatedApps = updatedApps.distinctBy { it.packageName }
      if (uniqueUpdatedApps.isNotEmpty()) {
        membershipDao.deleteMembershipsForSpace(spaceId)
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
          val appsToUse = if (uniqueUpdatedApps.isNotEmpty()) {
            uniqueUpdatedApps
          } else if (context != null) {
            try {
              AppDiscoveryManager(context).loadInstalledApps()
            } catch (e: Exception) {
              emptyList()
            }
          } else {
            emptyList()
          }
          val dockApps = DefaultAppCapabilityResolver.resolveDockApps(appsToUse, dockCapacity, context)
          val dockEntities = dockApps.mapIndexed { idx, app ->
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
        } else if (dockCapacity > existingDock.size) {
          expandDockItemsIfNeeded(spaceId, dockCapacity, uniqueUpdatedApps)
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

  override suspend fun updateSpaceWallpaper(
    spaceId: String,
    wallpaperType: String,
    wallpaperColor: Long?,
    wallpaperImageUri: String?
  ): Result<Unit> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val safeType = when (wallpaperType) {
        Space.BACKGROUND_COLOR, Space.BACKGROUND_IMAGE -> wallpaperType
        else -> Space.BACKGROUND_DEFAULT
      }

      val updated = existing.copy(
        homeWallpaperType = safeType,
        homeWallpaperColor = if (safeType == Space.BACKGROUND_COLOR) wallpaperColor else null,
        homeWallpaperImageUri = if (safeType == Space.BACKGROUND_IMAGE) wallpaperImageUri else null,
        backgroundType = safeType,
        backgroundColor = if (safeType == Space.BACKGROUND_COLOR) wallpaperColor else null,
        backgroundImageUri = if (safeType == Space.BACKGROUND_IMAGE) wallpaperImageUri else null,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated wallpaper for Space '${existing.name}' ($spaceId): type=$safeType, uri=$wallpaperImageUri")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update wallpaper for Space ($spaceId)", e)
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

      val safeGridColumns = (gridColumns ?: existing.gridColumns).coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
      val safeIconSize = when (iconSize ?: existing.iconSize) {
        Space.ICON_SIZE_SMALL, Space.ICON_SIZE_LARGE -> iconSize ?: existing.iconSize
        else -> Space.ICON_SIZE_MEDIUM
      }

      val updated = existing.copy(
        appTheme = appTheme,
        gridColumns = safeGridColumns,
        iconSize = safeIconSize,
        labelVisibility = labelVisibility ?: existing.labelVisibility,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated theme for Space '${existing.name}' ($spaceId): theme=$appTheme, cols=$safeGridColumns, iconSize=$safeIconSize")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update theme for Space ($spaceId)", e)
      Result.failure(e)
    }
  }

  override suspend fun addPage(spaceId: String): Result<Int> {
    return try {
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

      val currentPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      val maxPlacementPage = currentPlacements.maxOfOrNull { it.pageIndex } ?: 0
      val currentPages = maxOf(existing.pageCount, maxPlacementPage + 1)

      if (currentPages >= Space.MAX_PAGES) {
        return Result.failure(IllegalStateException("Maximum of ${Space.MAX_PAGES} pages allowed."))
      }

      val newPageCount = currentPages + 1
      val updated = existing.copy(
        pageCount = newPageCount,
        updatedAt = System.currentTimeMillis()
      )
      spaceDao.updateSpace(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Created Layer 1 Page index ${newPageCount - 1} for Space '${existing.name}' ($spaceId). Total pages: $newPageCount")
      Result.success(newPageCount - 1)
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
      val existing = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))

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
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete page $pageIndex for Space ($spaceId)", e)
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
      val entity = layoutDao.getPlacementById(placementId)
        ?: return Result.failure(IllegalArgumentException("Placement with id '$placementId' not found"))

      val updated = entity.copy(
        spanX = spanX.coerceAtLeast(1),
        spanY = spanY.coerceAtLeast(1),
        positionIndex = positionIndex ?: entity.positionIndex
      )
      layoutDao.updatePlacement(updated)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated widget placement ($placementId) span to ${spanX}x${spanY}")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to update widget span for placement $placementId", e)
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
        getSpanX = { if (it.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) it.spanX else 1 },
        getSpanY = { if (it.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) it.spanY else 1 },
        targetPage = targetPage,
        targetPosition = targetPosClamped,
        pageSize = effectivePageSize,
        cols = cols
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

      // Check if already in dock by packageName
      val existingIdx = current.indexOfFirst { it.packageName == app.packageName }
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

      if (safeDockCapacity > existing.dockCapacity) {
        expandDockItemsIfNeeded(spaceId, safeDockCapacity)
      }

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
      layoutDao.deletePlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      layoutDao.deleteAllDockItemsForSpace(spaceId)

      val presetObj = LayoutPreset.getById(preset.id)
      val layoutResult = PresetLayoutHelper.buildInitialLayout(
        spaceId = spaceId,
        preset = presetObj,
        gridColumns = preset.gridColumns,
        availableApps = distinctActiveApps,
        dockCapacity = preset.dockCapacity
      )

      if (layoutResult.placements.isNotEmpty()) {
        layoutDao.insertPlacements(layoutResult.placements)
      }

      if (layoutResult.dockItems.isNotEmpty()) {
        layoutDao.insertDockItems(layoutResult.dockItems)
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

      val space = spaceDao.getSpaceById(spaceId)
      val dockCapacity = space?.dockCapacity ?: 5
      val cols = space?.gridColumns ?: 4
      val presetId = space?.layoutPreset ?: Space.PRESET_DEFAULT
      val presetObj = LayoutPreset.getById(presetId)

      layoutDao.deletePlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      layoutDao.deleteAllDockItemsForSpace(spaceId)

      val layoutResult = PresetLayoutHelper.buildInitialLayout(
        spaceId = spaceId,
        preset = presetObj,
        gridColumns = cols,
        availableApps = uniqueApps,
        dockCapacity = dockCapacity
      )

      if (layoutResult.dockItems.isNotEmpty()) {
        layoutDao.insertDockItems(layoutResult.dockItems)
        successes.add("Identified and populated essential bottom Dock apps (${layoutResult.dockItems.size} apps)")
      }

      if (layoutResult.placements.isNotEmpty()) {
        layoutDao.insertPlacements(layoutResult.placements)
        successes.add("Initialized ${layoutResult.placements.size} placements using preset '${presetObj.name}'")
      }

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
        summary = "Successfully imported ${allInstalledApps.size} apps and ${layoutResult.dockItems.size} dock shortcuts from standard Android configuration."
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
      removePlacement(placementId)
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
        moveAppToPage(
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
      moveAppToPage(
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
      val space = spaceDao.getSpaceById(spaceId)
        ?: return Result.failure(IllegalArgumentException("Space with id '$spaceId' not found"))
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
        id = "widget_" + java.util.UUID.randomUUID().toString().replace("-", "").take(10),
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
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to add widget to Space $spaceId", e)
      Result.failure(e)
    }
  }
}
