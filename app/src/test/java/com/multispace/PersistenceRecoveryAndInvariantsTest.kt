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
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppIdentityLookup
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.PlacementValidator
import com.multispace.platform.LauncherSessionManager
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.appIdentity
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
class PersistenceRecoveryAndInvariantsTest {

  private lateinit var database: LauncherDatabase
  private lateinit var spaceDao: SpaceDao
  private lateinit var membershipDao: SpaceMembershipDao
  private lateinit var layoutDao: SpaceLayoutDao
  private lateinit var preferences: LauncherPreferences
  private lateinit var repository: RoomSpaceRepository
  private lateinit var context: Context

  private val installedApp = DiscoveredApp(
    id = "com.valid.app/.MainActivity#0",
    packageName = "com.valid.app",
    activityName = ".MainActivity",
    label = "Valid App",
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
      dockRepository = dockRepo,
      sessionManager = LauncherSessionManager(context)
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  // ==========================================
  // 1. STALE MEMBERSHIPS AND MISSING APPS
  // ==========================================

  @Test
  fun testStaleMembershipAndMissingAppResolution() = runBlocking {
    val spaceRes = repository.createSpace("Recovery Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // App that is installed
    repository.addAppToSpace(spaceId, installedApp)

    // App that was previously installed but subsequently uninstalled
    val uninstalledApp = DiscoveredApp(
      id = "com.stale.uninstalled/.MainActivity#0",
      packageName = "com.stale.uninstalled",
      activityName = ".MainActivity",
      label = "Ghost App",
      userHandleId = 0L
    )
    repository.addAppToSpace(spaceId, uninstalledApp)

    // Desktop placement for both installed and uninstalled apps
    repository.addPlacement(
      SpaceItemPlacement(
        id = "p_installed",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 0,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = installedApp.packageName,
        componentName = installedApp.activityName,
        userHandleId = installedApp.userHandleId
      )
    )
    repository.addPlacement(
      SpaceItemPlacement(
        id = "p_uninstalled",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 1,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = uninstalledApp.packageName,
        componentName = uninstalledApp.activityName,
        userHandleId = uninstalledApp.userHandleId
      )
    )

    // Verify memberships in database
    val memberships = repository.getMembershipsForSpace(spaceId)
    assertEquals(2, memberships.size)

    // Installed apps catalog only has installedApp (uninstalledApp is absent)
    val currentInstalledApps = listOf(installedApp)
    val lookup = AppIdentityLookup(currentInstalledApps)

    // 1. Installed app resolves safely
    val resolvedInstalled = lookup[memberships.first { it.packageName == installedApp.packageName }.appIdentity]
    assertNotNull(resolvedInstalled)
    assertEquals("com.valid.app", resolvedInstalled?.packageName)

    // 2. Uninstalled app safely resolves to null without throwing
    val resolvedStale = lookup[memberships.first { it.packageName == uninstalledApp.packageName }.appIdentity]
    assertNull("Stale membership app lookup must resolve safely to null", resolvedStale)

    // 3. Desktop placement lookup for uninstalled app safely resolves to null
    val placements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    val stalePlacement = placements.first { it.packageName == uninstalledApp.packageName }
    val resolvedPlacementApp = lookup[stalePlacement.appIdentity]
    assertNull("Stale placement app lookup must resolve safely to null", resolvedPlacementApp)
  }

  // ==========================================
  // 2. PLACEMENT VALIDATOR INVARIANTS
  // ==========================================

  @Test
  fun testPlacementValidator_DetectsInvalidAndDuplicatePlacements() {
    val cols = 4
    val rows = 5

    val validPlacement = SpaceItemPlacement(
      id = "p_valid",
      spaceId = "space_1",
      layer = 1,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.valid.app",
      componentName = ".Main"
    )

    val negativePage = SpaceItemPlacement(
      id = "p_neg_page",
      spaceId = "space_1",
      layer = 1,
      pageIndex = -1,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.app.a",
      componentName = ".Main"
    )

    val outOfBoundsPosition = SpaceItemPlacement(
      id = "p_out_of_bounds",
      spaceId = "space_1",
      layer = 1,
      pageIndex = 0,
      positionIndex = 25, // 25 >= cols * rows (20)
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.app.b",
      componentName = ".Main"
    )

    val widgetOverflow = SpaceItemPlacement(
      id = "p_widget_overflow",
      spaceId = "space_1",
      layer = 1,
      pageIndex = 0,
      positionIndex = 3, // col 3 in 4-column grid, spanX = 2 overflows page width
      spanX = 2,
      spanY = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET
    )

    val invalidSpan = SpaceItemPlacement(
      id = "p_invalid_span",
      spaceId = "space_1",
      layer = 1,
      pageIndex = 0,
      positionIndex = 4,
      spanX = 0, // Invalid: span < 1
      spanY = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET
    )

    val duplicateSlot = SpaceItemPlacement(
      id = "p_dup_slot",
      spaceId = "space_1",
      layer = 1,
      pageIndex = 0,
      positionIndex = 0, // Collides with validPlacement at page 0, pos 0
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.app.c",
      componentName = ".Main"
    )

    val duplicateId = SpaceItemPlacement(
      id = "p_valid", // Duplicate ID matching validPlacement
      spaceId = "space_1",
      layer = 1,
      pageIndex = 1,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.app.d",
      componentName = ".Main"
    )

    val missingFolderId = SpaceItemPlacement(
      id = "p_empty_folder",
      spaceId = "space_1",
      layer = 1,
      pageIndex = 0,
      positionIndex = 10,
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = null // Invalid: missing folderId
    )

    val testList = listOf(
      validPlacement,
      negativePage,
      outOfBoundsPosition,
      widgetOverflow,
      invalidSpan,
      duplicateSlot,
      duplicateId,
      missingFolderId
    )

    val result = PlacementValidator.validatePlacements(testList, cols = cols, rows = rows)

    assertFalse("Validation should report issues for corrupted placements", result.isValid)
    assertTrue(result.hasIssues)

    // Check specific issue categories
    assertTrue(result.invalidPageIndexCount >= 1)
    assertTrue(result.outOfBoundsCount >= 1)
    assertTrue(result.duplicateOccupiedSlotCount >= 1)
    assertTrue(result.invalidRecordCount >= 2) // duplicateId and missingFolderId

    // Verify exact issue types
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.InvalidPageIndex && it.placementId == "p_neg_page" })
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds && it.placementId == "p_out_of_bounds" })
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds && it.placementId == "p_widget_overflow" })
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.InvalidSpan && it.placementId == "p_invalid_span" })
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.DuplicateOccupiedSlot && it.placementId == "p_dup_slot" })
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.DuplicateRecord && it.placementId == "p_valid" })
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.InvalidRecord && it.placementId == "p_empty_folder" })
  }

  // ==========================================
  // 3. ORPHAN FOLDER INVARIANTS
  // ==========================================

  @Test
  fun testOrphanFolderInvariants() = runBlocking {
    val spaceRes = repository.createSpace("Orphan Check Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // Attempt to add a FOLDER placement pointing to non-existent folderId
    val nonExistentFolderId = "folder_does_not_exist"
    val orphanFolderPlacement = SpaceItemPlacement(
      id = "p_orphan_folder",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = nonExistentFolderId
    )

    val addRes = repository.addPlacement(orphanFolderPlacement)
    assertFalse("Adding a folder placement when folder entity does not exist must be rejected", addRes.isSuccess)
  }

  // ==========================================
  // 4. PLACEMENT DEDUPLICATION INVARIANT
  // ==========================================

  @Test
  fun testPlacementRepository_ReplacesOlderPlacementOnSameLayer() = runBlocking {
    val spaceRes = repository.createSpace("Deduplication Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Place app at page 0, pos 2
    val firstPlacement = SpaceItemPlacement(
      id = "p_first",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = installedApp.packageName,
      componentName = installedApp.activityName,
      userHandleId = installedApp.userHandleId
    )
    val res1 = repository.addPlacement(firstPlacement)
    assertTrue(res1.isSuccess)

    // 2. Move or re-place same app identity at page 1, pos 5 with new placement ID
    val secondPlacement = SpaceItemPlacement(
      id = "p_second",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 1,
      positionIndex = 5,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = installedApp.packageName,
      componentName = installedApp.activityName,
      userHandleId = installedApp.userHandleId
    )
    val res2 = repository.addPlacement(secondPlacement)
    assertTrue(res2.isSuccess)

    // 3. Verify older placement is removed and only the newest placement exists
    val placements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(1, placements.size)
    assertEquals("p_second", placements[0].id)
    assertEquals(1, placements[0].pageIndex)
    assertEquals(5, placements[0].positionIndex)
  }
}
