package com.multispace.data.repository

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.withTransaction
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
import com.multispace.domain.model.ActiveSpaceState
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.appIdentity
import com.multispace.domain.model.findMatching
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.ImportReport
import com.multispace.domain.model.LayoutPreset
import com.multispace.domain.model.PageTurnEffect
import com.multispace.domain.model.PlacementCascadeHelper
import com.multispace.domain.model.PlacementValidator
import com.multispace.domain.model.PresetLayoutHelper
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.SpaceMembership
import com.multispace.domain.model.WallpaperCatalog
import com.multispace.domain.repository.DockRepository
import com.multispace.domain.repository.FolderRepository
import com.multispace.domain.repository.PlacementRepository
import com.multispace.domain.repository.SpaceMembershipRepository
import com.multispace.domain.repository.SpaceRepository
import com.multispace.platform.AppDiscoveryManager
import com.multispace.platform.DefaultAppCapabilityResolver
import com.multispace.platform.PinSecurityManager
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomSpaceRepository(
  private val spaceDao: SpaceDao,
  private val membershipDao: SpaceMembershipDao,
  private val layoutDao: SpaceLayoutDao,
  private val preferences: LauncherPreferences,
  private val context: Context? = null,
  private val database: RoomDatabase? = null,
  private val membershipRepository: SpaceMembershipRepository = RoomSpaceMembershipRepository(membershipDao, database),
  private val placementRepository: PlacementRepository = RoomPlacementRepository(spaceDao, layoutDao, membershipDao, context, database),
  private val folderRepository: FolderRepository = RoomFolderRepository(layoutDao, database),
  private val dockRepository: DockRepository = RoomDockRepository(spaceDao, layoutDao, placementRepository, database)
) : SpaceRepository,
    SpaceMembershipRepository by membershipRepository,
    PlacementRepository by placementRepository,
    FolderRepository by folderRepository,
    DockRepository by dockRepository {

  private val initMutex = Mutex()

  private suspend fun <T> runInTransaction(block: suspend () -> T): T {
    return if (database != null) {
      database.withTransaction { block() }
    } else {
      block()
    }
  }

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

  override val activeSpaceStateFlow: Flow<ActiveSpaceState> = activeSpaceFlow.flatMapLatest { space ->
    if (space != null) {
      combine(
        membershipRepository.getMembershipsForSpaceFlow(space.id),
        placementRepository.getPlacementsForSpaceLayerFlow(space.id, SpaceItemPlacement.LAYER_HOME),
        folderRepository.getFoldersForSpaceFlow(space.id),
        dockRepository.getDockItemsForSpaceFlow(space.id)
      ) { memberships, placements, folders, dockItems ->
        ActiveSpaceState(
          space = space,
          memberships = memberships,
          placements = placements,
          folders = folders,
          dockItems = dockItems
        )
      }
    } else {
      flowOf(ActiveSpaceState(space = null))
    }
  }

  override suspend fun ensureDefaultSpaceInitialized(initialApps: List<DiscoveredApp>): Result<Space> = initMutex.withLock {
    return@withLock try {
      val count = spaceDao.getSpaceCount()
      if (count == 0) {
        AppLogger.i(AppLogger.Category.LAUNCHER, "No Spaces found in database. Initializing Default Space with default apps.")
        val defaultSpace = runInTransaction {
          val space = Space(
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
          spaceDao.insertSpace(SpaceEntity.fromDomain(space))
          preferences.setActiveSpaceId(Space.DEFAULT_SPACE_ID)

          initializeNewSpaceDefaults(
            spaceId = Space.DEFAULT_SPACE_ID,
            spaceName = Space.DEFAULT_SPACE_NAME,
            layoutPreset = Space.PRESET_DEFAULT,
            dockCapacity = 5,
            gridColumns = 4,
            candidateApps = initialApps
          )
          preferences.markSpaceInitialized(Space.DEFAULT_SPACE_ID)
          space
        }

        Result.success(defaultSpace)
      } else {
        val spaces = spaceDao.getAllSpaces()
        val currentActiveId = preferences.activeSpaceIdFlow.firstOrNull()
        val resolvedSpace = spaces.firstOrNull { it.id == currentActiveId } ?: spaces.first()
        if (currentActiveId != resolvedSpace.id) {
          preferences.setActiveSpaceId(resolvedSpace.id)
        }

        // Clean up any historical duplicate dock items and redundant placements in the default space
        cleanupDuplicateDockItems(Space.DEFAULT_SPACE_ID)
        try {
          val pruned = layoutDao.pruneDuplicatePlacements()
          val prunedPositions = layoutDao.pruneDuplicatePositions()
          if (pruned > 0 || prunedPositions > 0) {
            AppLogger.i(AppLogger.Category.LAUNCHER, "Pruned duplicate placements ($pruned apps, $prunedPositions positions) from database")
          }
        } catch (e: Exception) {
          AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to prune duplicate placements", e)
        }

        if (context != null) {
          // Ensure every existing Space has exactly one Most Used Apps folder on Page 0
          for (s in spaces) {
            ensureMostUsedFolderExists(s.id)
          }
        }

        // Check explicit persistent initialization state rather than placements.isEmpty()
        val defaultEntity = spaces.firstOrNull { it.id == Space.DEFAULT_SPACE_ID }
        if (defaultEntity != null) {
          val isDefaultInitialized = preferences.isSpaceInitialized(Space.DEFAULT_SPACE_ID)
          if (!isDefaultInitialized) {
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
            preferences.markSpaceInitialized(Space.DEFAULT_SPACE_ID)
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
      candidateApps.distinctBy { it.appIdentity }
    } else if (context != null) {
      try {
        AppDiscoveryManager(context).loadInstalledApps().distinctBy { it.appIdentity }
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to load apps for new Space defaults: ${e.message}")
        emptyList()
      }
    } else {
      emptyList()
    }

    if (appsToUse.isEmpty()) {
      return
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

    if (layoutResult.folders.isNotEmpty()) {
      layoutDao.insertFolders(layoutResult.folders)
      AppLogger.i(AppLogger.Category.LAUNCHER, "Initialized ${layoutResult.folders.size} default folders for Space '$spaceName' ($spaceId)")
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

    ensureMostUsedFolderExists(spaceId)
    preferences.markSpaceInitialized(spaceId)
  }

  /**
   * Ensures the dynamic "Most Used Apps" folder exists on Page 0 for the specified Space.
   * Repairs existing Spaces, ensures a valid Page 0 placement without widget collisions,
   * and prunes duplicate folders/placements.
   */
  override suspend fun ensureMostUsedFolderExists(spaceId: String) {
    try {
      val existingFolders = layoutDao.getFoldersForSpace(spaceId)
      val matchingFolders = existingFolders.filter {
        it.name == SpaceFolder.MOST_USED_FOLDER_NAME || it.id.startsWith(SpaceFolder.MOST_USED_FOLDER_PREFIX)
      }

      val primaryFolder: SpaceFolderEntity = if (matchingFolders.isEmpty()) {
        val newFolderId = SpaceFolder.getMostUsedFolderId(spaceId)
        val folderEntity = SpaceFolderEntity(
          id = newFolderId,
          spaceId = spaceId,
          name = SpaceFolder.MOST_USED_FOLDER_NAME,
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis()
        )
        layoutDao.insertFolder(folderEntity)
        folderEntity
      } else {
        val preferred = matchingFolders.firstOrNull { it.id == SpaceFolder.getMostUsedFolderId(spaceId) } ?: matchingFolders.first()
        if (preferred.name != SpaceFolder.MOST_USED_FOLDER_NAME) {
          layoutDao.updateFolder(preferred.copy(name = SpaceFolder.MOST_USED_FOLDER_NAME, updatedAt = System.currentTimeMillis()))
        }
        val duplicates = matchingFolders.filter { it.id != preferred.id }
        for (dup in duplicates) {
          layoutDao.deleteFolderById(dup.id)
          layoutDao.deletePlacementByFolderId(dup.id)
          layoutDao.deleteFolderItemsForFolder(dup.id)
        }
        preferred
      }

      val targetFolderId = primaryFolder.id

      // Placements check on Layer 1 (Home)
      val currentPlacements = layoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
      val matchingPlacements = currentPlacements.filter {
        it.itemType == SpaceItemPlacement.ITEM_TYPE_FOLDER &&
          (it.folderId == targetFolderId || (it.folderId != null && it.folderId.startsWith(SpaceFolder.MOST_USED_FOLDER_PREFIX)))
      }

      val primaryPlacement = matchingPlacements.firstOrNull()
      if (matchingPlacements.size > 1) {
        matchingPlacements.drop(1).forEach {
          layoutDao.deletePlacementById(it.id)
        }
      }

      val space = spaceDao.getSpaceById(spaceId)
      val gridCols = space?.gridColumns ?: 4
      val maxRows = 6
      val pageSize = gridCols * maxRows

      // Calculate widget occupied slots on Page 0
      val page0Placements = currentPlacements.filter { it.pageIndex == 0 && it.id != primaryPlacement?.id }
      val widgetOccupiedSlots = mutableSetOf<Int>()
      for (p in page0Placements) {
        if (p.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) {
          val r = (p.positionIndex / gridCols).coerceIn(0, maxRows - 1)
          val c = (p.positionIndex % gridCols).coerceIn(0, gridCols - 1)
          val sX = p.spanX.coerceIn(1, gridCols - c)
          val sY = p.spanY.coerceIn(1, maxRows - r)
          for (dr in 0 until sY) {
            for (dc in 0 until sX) {
              widgetOccupiedSlots.add((r + dr) * gridCols + (c + dc))
            }
          }
        }
      }

      val isAlreadyOnPage0Valid = primaryPlacement != null &&
        primaryPlacement.pageIndex == 0 &&
        !widgetOccupiedSlots.contains(primaryPlacement.positionIndex)

      if (!isAlreadyOnPage0Valid) {
        val allOccupiedOnPage0 = widgetOccupiedSlots.toMutableSet()
        for (p in page0Placements) {
          allOccupiedOnPage0.add(p.positionIndex)
        }

        // Check preferred preset slot: Row 3, Col 0
        val preferredPos = 3 * gridCols
        val targetPos = if (preferredPos < pageSize && !allOccupiedOnPage0.contains(preferredPos)) {
          preferredPos
        } else {
          // First unoccupied slot on Page 0
          (0 until pageSize).firstOrNull { !allOccupiedOnPage0.contains(it) } ?: run {
            // Page 0 is full: find first normal APP to displace to Page 1
            val appToDisplace = page0Placements.firstOrNull { it.itemType == SpaceItemPlacement.ITEM_TYPE_APP }
            if (appToDisplace != null) {
              val page1Placements = currentPlacements.filter { it.pageIndex == 1 }
              val page1Occupied = page1Placements.map { it.positionIndex }.toSet()
              val page1Slot = (0 until pageSize).firstOrNull { !page1Occupied.contains(it) } ?: 0
              layoutDao.updatePlacement(
                appToDisplace.copy(
                  pageIndex = 1,
                  positionIndex = page1Slot
                )
              )
              appToDisplace.positionIndex
            } else {
              0
            }
          }
        }

        if (primaryPlacement != null) {
          layoutDao.updatePlacement(
            primaryPlacement.copy(
              pageIndex = 0,
              positionIndex = targetPos,
              itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
              folderId = targetFolderId,
              spanX = 1,
              spanY = 1
            )
          )
        } else {
          val newPlacement = SpaceItemPlacementEntity(
            id = SpaceFolder.getMostUsedPlacementId(spaceId),
            spaceId = spaceId,
            layer = SpaceItemPlacement.LAYER_HOME,
            pageIndex = 0,
            positionIndex = targetPos,
            itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
            folderId = targetFolderId,
            spanX = 1,
            spanY = 1
          )
          layoutDao.insertPlacement(newPlacement)
        }
        AppLogger.i(AppLogger.Category.LAUNCHER, "Ensured/Repaired Most Used Apps folder on Page 0 at pos $targetPos for Space $spaceId")
      }
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to ensure Most Used Apps folder for Space $spaceId", e)
    }
  }

  /**
   * Expands the DockBar capacity when the user increases it above current count,
   * preserving original dock apps and filling newly available positions with sensible everyday apps:
   * Contacts -> Gallery/Photos -> Files -> Clock -> Calculator -> Calendar -> Maps.
   */
  @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
  internal suspend fun expandDockItemsIfNeeded(
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
      val targetIdentity = entity.appIdentity
      installedApps.findMatching(targetIdentity)
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

    val existingDockIdentities = existingDockEntities.map { it.appIdentity }
    val newDockApps = resolvedDock.filterNot { dockApp ->
      existingDockIdentities.any { it.matches(dockApp.appIdentity) }
    }
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

    val existingMemberships = membershipDao.getMembershipsForSpace(spaceId)
    val existingMembershipIdentities = existingMemberships.map { it.appIdentity }
    val newMemberships = newDockApps
      .filterNot { dockApp -> existingMembershipIdentities.any { it.matches(dockApp.appIdentity) } }
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
        if (context != null) {
          ensureMostUsedFolderExists(spaceId)
        }
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
      val space = runInTransaction {
        val newId = "space_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val orderIndex = spaceDao.getSpaceCount()
        val s = Space(
          id = newId,
          name = trimmed,
          orderIndex = orderIndex,
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis(),
          layoutType = layoutType,
          gridColumns = Space.DEFAULT_GRID_COLUMNS,
          dockCapacity = Space.DEFAULT_DOCK_CAPACITY
        )
        spaceDao.insertSpace(SpaceEntity.fromDomain(s))

        initializeNewSpaceDefaults(
          spaceId = newId,
          spaceName = trimmed,
          layoutPreset = Space.PRESET_DEFAULT,
          dockCapacity = Space.DEFAULT_DOCK_CAPACITY,
          gridColumns = Space.DEFAULT_GRID_COLUMNS,
          candidateApps = initialApps
        )
        s
      }

      AppLogger.i(AppLogger.Category.LAUNCHER, "Created new Space: '$trimmed' (${space.id}) with default apps")
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
      val space = runInTransaction {
        val newId = "space_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val orderIndex = spaceDao.getSpaceCount()
        val s = Space(
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
        spaceDao.insertSpace(SpaceEntity.fromDomain(s))

        initializeNewSpaceDefaults(
          spaceId = newId,
          spaceName = trimmed,
          layoutPreset = layoutPreset,
          dockCapacity = dockCapacity,
          gridColumns = gridColumns,
          candidateApps = initialApps
        )
        s
      }

      AppLogger.i(AppLogger.Category.LAUNCHER, "Created configured Space: '$trimmed' (${space.id}) with preset '$layoutPreset' and default apps")
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
      val updatedDomain = runInTransaction {
        spaceDao.updateSpace(updated)

      val uniqueUpdatedApps = updatedApps.distinctBy { it.appIdentity }
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
          // Only generate default layout if there are no existing placements.
          // Page 0 strictly gets cols apps on the bottom row; remaining apps on Page 1+.
          val pageSize = (gridColumns * 5).coerceAtLeast(1)
          val lastRow = 4
          val page0Count = minOf(gridColumns, uniqueUpdatedApps.size)
          val homeEntities = mutableListOf<SpaceItemPlacementEntity>()
          for (i in 0 until page0Count) {
            val app = uniqueUpdatedApps[i]
            homeEntities.add(
              SpaceItemPlacementEntity(
                id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
                spaceId = spaceId,
                layer = SpaceItemPlacement.LAYER_HOME,
                pageIndex = 0,
                positionIndex = lastRow * gridColumns + i,
                itemType = SpaceItemPlacement.ITEM_TYPE_APP,
                packageName = app.packageName,
                componentName = app.activityName,
                userHandleId = app.userHandleId
              )
            )
          }
          for (i in page0Count until uniqueUpdatedApps.size) {
            val app = uniqueUpdatedApps[i]
            val rem = i - page0Count
            homeEntities.add(
              SpaceItemPlacementEntity(
                id = "place_" + UUID.randomUUID().toString().replace("-", "").take(10),
                spaceId = spaceId,
                layer = SpaceItemPlacement.LAYER_HOME,
                pageIndex = 1 + (rem / pageSize),
                positionIndex = rem % pageSize,
                itemType = SpaceItemPlacement.ITEM_TYPE_APP,
                packageName = app.packageName,
                componentName = app.activityName,
                userHandleId = app.userHandleId
              )
            )
          }
          layoutDao.insertPlacements(homeEntities)
        } else {
          // PRESERVE ALL USER CUSTOM PLACEMENTS!
          // Only synchronize additions and removals without disturbing existing positions
          val updatedIdentitySet = uniqueUpdatedApps.map { it.appIdentity }.toSet()
          val placedIdentitySet = existingPlacements.mapNotNull { it.appIdentity }.toSet()

          // 1. Remove placements for apps explicitly deselected from the space
          val placementsToRemove = existingPlacements.filter { p ->
            p.itemType == SpaceItemPlacement.ITEM_TYPE_APP && p.appIdentity != null &&
              updatedIdentitySet.none { it.matches(p.appIdentity!!) }
          }
          for (p in placementsToRemove) {
            layoutDao.deletePlacementById(p.id)
          }

          // 2. Add placements for newly added apps into empty slots on trailing pages (Page 1+)
          val newlyAddedApps = uniqueUpdatedApps.filter { app ->
            placedIdentitySet.none { it.matches(app.appIdentity) }
          }
          if (newlyAddedApps.isNotEmpty()) {
            val remainingPlacements = existingPlacements.filter { !placementsToRemove.any { r -> r.id == it.id } }
            val pageSize = (gridColumns * 5).coerceAtLeast(1)
            val occupiedPerPage = mutableMapOf<Int, MutableSet<Int>>()
            for (p in remainingPlacements) {
              val sX = if (p.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) p.spanX.coerceIn(1, gridColumns) else 1
              val sY = if (p.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) p.spanY.coerceIn(1, 5) else 1
              val r = p.positionIndex / gridColumns
              val c = p.positionIndex % gridColumns
              for (dr in 0 until sY) {
                for (dc in 0 until sX) {
                  occupiedPerPage.getOrPut(p.pageIndex) { mutableSetOf() }.add((r + dr) * gridColumns + (c + dc))
                }
              }
            }

            var curPage = 1
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
      updated.toDomain()
    }

      AppLogger.i(AppLogger.Category.LAUNCHER, "Updated configured Space: '$trimmed' ($spaceId) with ${updatedApps.size} apps")
      Result.success(updatedDomain)
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

      runInTransaction {
        val currentActiveId = preferences.activeSpaceIdFlow.firstOrNull()
        if (currentActiveId == spaceId) {
          // Fall back active space to another valid space before deleting
          val fallback = allSpaces.first { it.id != spaceId }
          preferences.setActiveSpaceId(fallback.id)
          AppLogger.i(AppLogger.Category.LAUNCHER, "Active Space fallback to '${fallback.name}' prior to deleting '$spaceId'")
        }

        spaceDao.deleteSpaceById(spaceId)
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Deleted Space '${target.name}' ($spaceId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete Space ($spaceId)", e)
      Result.failure(e)
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

      runInTransaction {
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

        val distinctActiveApps = activeApps.distinctBy { it.appIdentity }
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
      val uniqueApps = allInstalledApps.distinctBy { it.appIdentity }
      val successes = mutableListOf<String>()
      val partiallyImported = mutableListOf<String>()
      val restricted = mutableListOf<String>()

      val space = spaceDao.getSpaceById(spaceId)
      val dockCapacity = space?.dockCapacity ?: 5
      val cols = space?.gridColumns ?: 4
      val presetId = space?.layoutPreset ?: Space.PRESET_DEFAULT
      val presetObj = LayoutPreset.getById(presetId)

      val (dockCount, placementCount) = runInTransaction {
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
        }

        if (layoutResult.placements.isNotEmpty()) {
          layoutDao.insertPlacements(layoutResult.placements)
        }

        // Ensure all imported apps are registered as memberships in this Space
        if (uniqueApps.isNotEmpty()) {
          val existingMemberships = membershipDao.getMembershipsForSpace(spaceId)
          val existingMembershipIdentities = existingMemberships.map { it.appIdentity }
          val newMemberships = uniqueApps
            .filterNot { app -> existingMembershipIdentities.any { it.matches(app.appIdentity) } }
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

        Pair(layoutResult.dockItems.size, layoutResult.placements.size)
      }

      if (dockCount > 0) {
        successes.add("Identified and populated essential bottom Dock apps ($dockCount apps)")
      }
      if (placementCount > 0) {
        successes.add("Initialized $placementCount placements using preset '${presetObj.name}'")
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
        summary = "Successfully imported ${allInstalledApps.size} apps and $dockCount dock shortcuts from standard Android configuration."
      )

      AppLogger.i(AppLogger.Category.LAUNCHER, "Imported Android home layout: ${report.summary}")
      Result.success(report)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to import home layout", e)
      Result.failure(e)
    }
  }

  override suspend fun cleanupUninstalledApp(packageName: String, userHandleId: Long): Result<Unit> {
    return try {
      runInTransaction {
        layoutDao.deletePlacementsForPackage(packageName, userHandleId)
        layoutDao.deleteFolderItemsForPackage(packageName, userHandleId)
        layoutDao.deleteDockItemsForPackage(packageName, userHandleId)
        membershipDao.deleteAllMembershipsForPackage(packageName, userHandleId)
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Cleaned up layout placements for uninstalled package: $packageName (userHandleId=$userHandleId)")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to cleanup uninstalled package: $packageName", e)
      Result.failure(e)
    }
  }

  override suspend fun cleanupUninstalledAppForAllProfiles(packageName: String): Result<Unit> {
    return try {
      runInTransaction {
        layoutDao.deletePlacementsForAllProfiles(packageName)
        layoutDao.deleteFolderItemsForAllProfiles(packageName)
        layoutDao.deleteDockItemsForAllProfiles(packageName)
        membershipDao.deleteAllMembershipsForAllProfiles(packageName)
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Cleaned up layout placements for uninstalled package across all profiles: $packageName")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to cleanup uninstalled package across all profiles: $packageName", e)
      Result.failure(e)
    }
  }

  override suspend fun repairAppIdentity(
    oldIdentity: AppIdentity,
    newIdentity: AppIdentity
  ): Result<Unit> {
    return try {
      runInTransaction {
        layoutDao.updatePlacementComponent(
          packageName = oldIdentity.packageName,
          oldComponent = oldIdentity.componentName,
          newComponent = newIdentity.componentName,
          userHandleId = oldIdentity.userHandleId
        )
        layoutDao.updateDockItemComponent(
          packageName = oldIdentity.packageName,
          oldComponent = oldIdentity.componentName,
          newComponent = newIdentity.componentName,
          userHandleId = oldIdentity.userHandleId
        )
        layoutDao.updateFolderItemComponent(
          packageName = oldIdentity.packageName,
          oldComponent = oldIdentity.componentName,
          newComponent = newIdentity.componentName,
          userHandleId = oldIdentity.userHandleId
        )
        membershipDao.updateMembershipComponent(
          packageName = oldIdentity.packageName,
          oldComponent = oldIdentity.componentName,
          newComponent = newIdentity.componentName,
          userHandleId = oldIdentity.userHandleId
        )
      }
      AppLogger.i(AppLogger.Category.LAUNCHER, "Repaired app identity from $oldIdentity to $newIdentity")
      Result.success(Unit)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to repair app identity", e)
      Result.failure(e)
    }
  }
}
