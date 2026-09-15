package com.multispace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.multispace.data.dao.SpaceDao
import com.multispace.data.dao.SpaceLayoutDao
import com.multispace.data.dao.SpaceMembershipDao
import com.multispace.data.database.LauncherDatabase
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.data.repository.RoomDockRepository
import com.multispace.data.repository.RoomFolderRepository
import com.multispace.data.repository.RoomPlacementRepository
import com.multispace.data.repository.RoomSpaceMembershipRepository
import com.multispace.data.repository.RoomSpaceRepository
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.PageTurnEffect
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.SpaceMembership
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpaceRepositoryIntegrationTest {

  private lateinit var database: LauncherDatabase
  private lateinit var spaceDao: SpaceDao
  private lateinit var membershipDao: SpaceMembershipDao
  private lateinit var layoutDao: SpaceLayoutDao
  private lateinit var preferences: LauncherPreferences
  private lateinit var repository: RoomSpaceRepository
  private lateinit var context: Context

  private val appA = DiscoveredApp(
    id = "com.test.appA/.Main#0",
    packageName = "com.test.appA",
    activityName = ".Main",
    label = "App A",
    userHandleId = 0L
  )

  private val appB = DiscoveredApp(
    id = "com.test.appB/.Main#0",
    packageName = "com.test.appB",
    activityName = ".Main",
    label = "App B",
    userHandleId = 0L
  )

  private val appC = DiscoveredApp(
    id = "com.test.appC/.Main#0",
    packageName = "com.test.appC",
    activityName = ".Main",
    label = "App C",
    userHandleId = 0L
  )

  private val appD = DiscoveredApp(
    id = "com.test.appD/.Main#0",
    packageName = "com.test.appD",
    activityName = ".Main",
    label = "App D",
    userHandleId = 0L
  )

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    spaceDao = database.spaceDao()
    membershipDao = database.spaceMembershipDao()
    layoutDao = database.spaceLayoutDao()
    preferences = LauncherPreferences(context)

    val membershipRepo = RoomSpaceMembershipRepository(membershipDao)
    val placementRepo = RoomPlacementRepository(spaceDao, layoutDao, membershipDao, context = null)
    val folderRepo = RoomFolderRepository(layoutDao)
    val dockRepo = RoomDockRepository(spaceDao, layoutDao, placementRepo)

    repository = RoomSpaceRepository(
      spaceDao = spaceDao,
      membershipDao = membershipDao,
      layoutDao = layoutDao,
      preferences = preferences,
      context = null,
      membershipRepository = membershipRepo,
      placementRepository = placementRepo,
      folderRepository = folderRepo,
      dockRepository = dockRepo
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  // ==========================================
  // 1. SPACE LIFECYCLE
  // ==========================================

  @Test
  fun testSpaceLifecycle_CreateReadUpdateDelete() = runBlocking {
    // 1. Create default space & a custom space
    val defaultSpaceRes = repository.ensureDefaultSpaceInitialized(listOf(appA, appB))
    assertTrue("Default space initialization should succeed", defaultSpaceRes.isSuccess)
    val defaultSpace = defaultSpaceRes.getOrThrow()
    assertEquals(Space.DEFAULT_SPACE_ID, defaultSpace.id)

    val createRes = repository.createSpace(name = "Productivity Space", layoutType = "GRID_4")
    assertTrue("Custom space creation should succeed", createRes.isSuccess)
    val createdSpace = createRes.getOrThrow()
    val spaceId = createdSpace.id

    // 2. Read it back
    val fetched = repository.getSpaceById(spaceId)
    assertNotNull("Created space must be readable by ID", fetched)
    assertEquals("Productivity Space", fetched?.name)
    assertEquals(4, fetched?.gridColumns)

    val allSpaces = repository.allSpacesFlow.first()
    assertEquals(2, allSpaces.size)
    assertTrue("All spaces should include created space", allSpaces.any { it.id == spaceId })

    // 3. Update space settings
    val updateRes = repository.updateFullSpace(
      spaceId = spaceId,
      name = "Deep Work",
      keepExistingCredentials = true,
      authPolicy = Space.AUTH_NONE,
      pinSalt = null,
      pinHash = null,
      patternRows = 3,
      patternCols = 3,
      backgroundType = Space.BACKGROUND_COLOR,
      backgroundColor = 0xFF123456,
      backgroundImageUri = null,
      homeWallpaperType = Space.BACKGROUND_COLOR,
      homeWallpaperColor = 0xFF123456,
      homeWallpaperImageUri = null,
      phoneLockWallpaperType = Space.BACKGROUND_DEFAULT,
      phoneLockWallpaperColor = null,
      phoneLockWallpaperImageUri = null,
      spaceLockWallpaperType = Space.BACKGROUND_DEFAULT,
      spaceLockWallpaperColor = null,
      spaceLockWallpaperImageUri = null,
      appTheme = Space.THEME_DARK,
      gridColumns = 5,
      iconSize = Space.ICON_SIZE_LARGE,
      labelVisibility = false,
      layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
      layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
      layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
      dockCapacity = 6,
      layoutPreset = Space.PRESET_DEFAULT,
      useLayer2 = true,
      homeWallpaperScaleMode = "crop",
      homeWallpaperZoomLevel = 1.0f,
      homeWallpaperDimLevel = 0.2f,
      homeWallpaperOffsetX = 0f,
      homeWallpaperOffsetY = 0f,
      phoneLockWallpaperScaleMode = "crop",
      phoneLockWallpaperZoomLevel = 1.0f,
      phoneLockWallpaperDimLevel = 0.2f,
      phoneLockWallpaperOffsetX = 0f,
      phoneLockWallpaperOffsetY = 0f,
      spaceLockWallpaperScaleMode = "crop",
      spaceLockWallpaperZoomLevel = 1.0f,
      spaceLockWallpaperDimLevel = 0.2f,
      spaceLockWallpaperOffsetX = 0f,
      spaceLockWallpaperOffsetY = 0f,
      pageTurnEffect = PageTurnEffect.CUBE,
      pageTurnDurationMs = 350,
      pageTurnIntensity = 1.0f,
      updatedApps = listOf(appA, appB, appC)
    )
    assertTrue("Space update should succeed", updateRes.isSuccess)
    val updated = repository.getSpaceById(spaceId)
    assertEquals("Deep Work", updated?.name)
    assertEquals(5, updated?.gridColumns)
    assertEquals(6, updated?.dockCapacity)
    assertEquals(Space.THEME_DARK, updated?.appTheme)
    assertEquals(false, updated?.labelVisibility)
    assertEquals(PageTurnEffect.CUBE, updated?.pageTurnEffect)

    // 4. Rename space
    val renameRes = repository.renameSpace(spaceId, "Flow State")
    assertTrue(renameRes.isSuccess)
    assertEquals("Flow State", repository.getSpaceById(spaceId)?.name)

    // 5. Delete Space (Space 2)
    val deleteRes = repository.deleteSpace(spaceId)
    assertTrue("Deleting non-only space should succeed", deleteRes.isSuccess)
    assertNull("Deleted space should no longer exist", repository.getSpaceById(spaceId))

    // 6. Deleting the last remaining space must fail
    val deleteLastRes = repository.deleteSpace(Space.DEFAULT_SPACE_ID)
    assertFalse("Deleting the last remaining space must be rejected", deleteLastRes.isSuccess)
  }

  // ==========================================
  // 2. MEMBERSHIP LIFECYCLE
  // ==========================================

  @Test
  fun testMembershipLifecycle_AddRemoveReorderQuery() = runBlocking {
    val spaceRes = repository.createSpace(name = "Membership Test Space", layoutType = "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Add apps
    val addARes = repository.addAppToSpace(spaceId, appA)
    val addBRes = repository.addAppToSpace(spaceId, appB)
    val addCRes = repository.addAppToSpace(spaceId, appC)
    assertTrue(addARes.isSuccess && addBRes.isSuccess && addCRes.isSuccess)

    // 2. Query consistency
    val list = repository.getMembershipsForSpace(spaceId)
    val flowList = repository.getMembershipsForSpaceFlow(spaceId).first()
    assertEquals(3, list.size)
    assertEquals(list, flowList)
    assertEquals("com.test.appA", list[0].packageName)
    assertEquals("com.test.appB", list[1].packageName)
    assertEquals("com.test.appC", list[2].packageName)

    assertTrue(repository.isAppInSpace(spaceId, appA))
    assertTrue(repository.isAppInSpace(spaceId, appB))
    assertFalse(repository.isAppInSpace(spaceId, appD))

    // 3. Prevent duplicate insertion
    val addADupRes = repository.addAppToSpace(spaceId, appA)
    assertTrue(addADupRes.isSuccess)
    assertEquals(3, repository.getMembershipsForSpace(spaceId).size)

    // 4. Reorder single app: move appC up (direction = -1) -> becomes index 1
    val reorderRes = repository.reorderSpaceApp(spaceId, appC, -1)
    assertTrue(reorderRes.isSuccess)
    val reordered = repository.getMembershipsForSpace(spaceId)
    assertEquals("com.test.appC", reordered[1].packageName)
    assertEquals("com.test.appB", reordered[2].packageName)

    // 5. Reorder multiple apps
    val reorderBatchRes = repository.reorderSpaceApps(spaceId, listOf(appB, appA, appC))
    assertTrue(reorderBatchRes.isSuccess)
    val reorderedBatch = repository.getMembershipsForSpace(spaceId)
    assertEquals("com.test.appB", reorderedBatch[0].packageName)
    assertEquals("com.test.appA", reorderedBatch[1].packageName)
    assertEquals("com.test.appC", reorderedBatch[2].packageName)

    // 6. Remove app
    val removeRes = repository.removeAppFromSpace(spaceId, appA)
    assertTrue(removeRes.isSuccess)
    assertFalse(repository.isAppInSpace(spaceId, appA))
    assertEquals(2, repository.getMembershipsForSpace(spaceId).size)
  }

  // ==========================================
  // 3. PLACEMENT LIFECYCLE
  // ==========================================

  @Test
  fun testPlacementLifecycle_AddUpdateRemoveQuery() = runBlocking {
    val spaceRes = repository.createSpace(name = "Placement Space", layoutType = "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Add app placement
    val appPlacement = SpaceItemPlacement(
      id = "place_app_1",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 4,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = appA.packageName,
      componentName = appA.activityName,
      userHandleId = appA.userHandleId
    )
    val addRes = repository.addPlacement(appPlacement)
    assertTrue(addRes.isSuccess)

    // 2. Add widget placement
    val widgetRes = repository.addWidgetPlacement(
      spaceId = spaceId,
      pageIndex = 0,
      widgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE,
      spanX = 2,
      spanY = 1
    )
    assertTrue(widgetRes.isSuccess)
    val widget = widgetRes.getOrThrow()

    // 3. Query placements by space & layer
    val homePlacements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(2, homePlacements.size)
    assertTrue(homePlacements.any { it.id == "place_app_1" })
    assertTrue(homePlacements.any { it.id == widget.id })

    // 4. Update widget span & position
    val spanUpdateRes = repository.updateWidgetSpan(
      placementId = widget.id,
      spanX = 4,
      spanY = 2,
      positionIndex = 8
    )
    assertTrue(spanUpdateRes.isSuccess)
    val updatedWidget = layoutDao.getPlacementById(widget.id)
    assertEquals(4, updatedWidget?.spanX)
    assertEquals(2, updatedWidget?.spanY)
    assertEquals(8, updatedWidget?.positionIndex)

    // 5. Move app to target page & position
    val moveRes = repository.moveAppToPage(
      spaceId = spaceId,
      placementId = "place_app_1",
      targetPage = 1,
      targetPosition = 0
    )
    assertTrue(moveRes.isSuccess)
    val movedApp = layoutDao.getPlacementById("place_app_1")
    assertEquals(1, movedApp?.pageIndex)
    assertEquals(0, movedApp?.positionIndex)

    // 6. Remove placement
    val removeRes = repository.removePlacement("place_app_1")
    assertTrue(removeRes.isSuccess)
    assertNull(layoutDao.getPlacementById("place_app_1"))
  }

  // ==========================================
  // 4. FOLDER LIFECYCLE
  // ==========================================

  @Test
  fun testFolderLifecycle_CreateAddRemoveAutoCleanupAndCascade() = runBlocking {
    val spaceRes = repository.createSpace(name = "Folder Space", layoutType = "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Create folder from two apps
    val createFolderRes = repository.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = 0,
      positionIndex = 5,
      folderName = "Social",
      sourceApp = appA,
      targetApp = appB,
      sourcePlacementId = null,
      targetPlacementId = null
    )
    assertTrue("Folder creation should succeed", createFolderRes.isSuccess)
    val folder = createFolderRes.getOrThrow()
    val folderId = folder.id

    // Verify folder entity & items
    val persistedFolder = layoutDao.getFolderById(folderId)
    assertNotNull(persistedFolder)
    assertEquals("Social", persistedFolder?.name)

    val folderItems = layoutDao.getFolderItems(folderId)
    assertEquals(2, folderItems.size)
    val folderPkgs = folderItems.map { it.packageName }.toSet()
    assertTrue(folderPkgs.contains(appA.packageName))
    assertTrue(folderPkgs.contains(appB.packageName))

    // Verify desktop placement created for the folder
    val placements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    val folderPlacement = placements.firstOrNull { it.folderId == folderId }
    assertNotNull("Folder placement on home screen should exist", folderPlacement)
    assertEquals(SpaceItemPlacement.ITEM_TYPE_FOLDER, folderPlacement?.itemType)

    // 2. Add app to folder
    val addAppRes = repository.addAppToFolder(folderId, appC)
    assertTrue(addAppRes.isSuccess)
    assertEquals(3, layoutDao.getFolderItems(folderId).size)

    // Adding duplicate app to folder is ignored
    val addDupRes = repository.addAppToFolder(folderId, appC)
    assertTrue(addDupRes.isSuccess)
    assertEquals(3, layoutDao.getFolderItems(folderId).size)

    // 3. Rename folder
    val renameRes = repository.renameFolder(folderId, "Work Tools")
    assertTrue(renameRes.isSuccess)
    assertEquals("Work Tools", layoutDao.getFolderById(folderId)?.name)

    // 4. Remove one item from folder
    val itemToRemove = layoutDao.getFolderItems(folderId).first { it.packageName == appC.packageName }
    val removeOneRes = repository.removeAppFromFolder(folderId, itemToRemove.id)
    assertTrue(removeOneRes.isSuccess)
    assertEquals(2, layoutDao.getFolderItems(folderId).size)

    // 5. Test explicit Delete Folder (cascades folder items, folder entity, and desktop placement)
    val deleteFolderRes = repository.deleteFolder(folderId)
    assertTrue(deleteFolderRes.isSuccess)
    assertNull("Folder entity should be deleted", layoutDao.getFolderById(folderId))
    assertTrue("Folder items should be deleted", layoutDao.getFolderItems(folderId).isEmpty())
    val remainingPlacements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertFalse("Folder placement should be removed", remainingPlacements.any { it.folderId == folderId })

    // 6. Test Auto-Cleanup: When last item is removed, folder and its placement are automatically deleted
    val secondFolderRes = repository.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = 0,
      positionIndex = 6,
      folderName = "Temporary",
      sourceApp = appA,
      targetApp = appB,
      sourcePlacementId = null,
      targetPlacementId = null
    )
    val secondFolderId = secondFolderRes.getOrThrow().id
    val items = layoutDao.getFolderItems(secondFolderId)
    assertEquals(2, items.size)

    // Remove first item
    repository.removeAppFromFolder(secondFolderId, items[0].id)
    assertNotNull("Folder still exists with 1 item", layoutDao.getFolderById(secondFolderId))

    // Remove second (last) item -> auto cleanup
    repository.removeAppFromFolder(secondFolderId, items[1].id)
    assertNull("Folder should be deleted when empty", layoutDao.getFolderById(secondFolderId))
    val postCleanupPlacements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertFalse("Folder placement should be deleted when folder becomes empty", postCleanupPlacements.any { it.folderId == secondFolderId })
  }

  // ==========================================
  // 5. DOCK LIFECYCLE
  // ==========================================

  @Test
  fun testDockLifecycle_AddReorderRemoveCapacityAndIsolation() = runBlocking {
    val space1Res = repository.createSpace(name = "Dock Space 1", layoutType = "GRID_4")
    val space1Id = space1Res.getOrThrow().id

    val space2Res = repository.createSpace(name = "Dock Space 2", layoutType = "GRID_4")
    val space2Id = space2Res.getOrThrow().id

    // 1. Add apps to Dock in Space 1
    val addA = repository.addAppToDock(space1Id, appA, orderIndex = 0)
    val addB = repository.addAppToDock(space1Id, appB, orderIndex = 1)
    val addC = repository.addAppToDock(space1Id, appC, orderIndex = 2)
    assertTrue(addA.isSuccess && addB.isSuccess && addC.isSuccess)

    val dockSpace1 = repository.getDockItemsForSpace(space1Id)
    assertEquals(3, dockSpace1.size)
    assertEquals("com.test.appA", dockSpace1[0].packageName)
    assertEquals("com.test.appB", dockSpace1[1].packageName)
    assertEquals("com.test.appC", dockSpace1[2].packageName)

    // 2. Per-space dock scoping: Space 2 dock is independent and empty
    val dockSpace2 = repository.getDockItemsForSpace(space2Id)
    assertTrue("Space 2 dock should initially have no custom items", dockSpace2.isEmpty())

    val addDToSpace2 = repository.addAppToDock(space2Id, appD, orderIndex = 0)
    assertTrue(addDToSpace2.isSuccess)
    assertEquals(1, repository.getDockItemsForSpace(space2Id).size)
    assertEquals(3, repository.getDockItemsForSpace(space1Id).size) // Space 1 dock intact

    // 3. Reorder dock items
    val reordered = listOf(dockSpace1[2], dockSpace1[0], dockSpace1[1])
    val reorderRes = repository.reorderDockItems(space1Id, reordered)
    assertTrue(reorderRes.isSuccess)
    val newDockSpace1 = repository.getDockItemsForSpace(space1Id)
    assertEquals("com.test.appC", newDockSpace1[0].packageName)
    assertEquals("com.test.appA", newDockSpace1[1].packageName)
    assertEquals("com.test.appB", newDockSpace1[2].packageName)

    // 4. Remove app from dock
    val removeRes = repository.removeAppFromDock(space1Id, newDockSpace1[0].id)
    assertTrue(removeRes.isSuccess)
    val postRemoveDock = repository.getDockItemsForSpace(space1Id)
    assertEquals(2, postRemoveDock.size)
    assertEquals("com.test.appA", postRemoveDock[0].packageName)
    assertEquals(0, postRemoveDock[0].orderIndex)
    assertEquals("com.test.appB", postRemoveDock[1].packageName)
    assertEquals(1, postRemoveDock[1].orderIndex)

    // 5. Deduplication cleanup
    repository.cleanupDuplicateDockItems(space1Id)
    assertEquals(2, repository.getDockItemsForSpace(space1Id).size)
  }
}
