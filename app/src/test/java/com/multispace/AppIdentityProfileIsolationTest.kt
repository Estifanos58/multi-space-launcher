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
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.platform.LauncherSessionManager
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
class AppIdentityProfileIsolationTest {

  private lateinit var database: LauncherDatabase
  private lateinit var spaceDao: SpaceDao
  private lateinit var membershipDao: SpaceMembershipDao
  private lateinit var layoutDao: SpaceLayoutDao
  private lateinit var preferences: LauncherPreferences
  private lateinit var repository: RoomSpaceRepository
  private lateinit var placementRepository: RoomPlacementRepository
  private lateinit var dockRepository: RoomDockRepository
  private lateinit var context: Context

  // Personal Profile (userHandleId = 0) vs Work Profile (userHandleId = 10)
  private val personalTeams = DiscoveredApp(
    id = "com.microsoft.teams/.MainActivity#0",
    packageName = "com.microsoft.teams",
    activityName = ".MainActivity",
    label = "Teams (Personal)",
    userHandleId = 0L
  )

  private val workTeams = DiscoveredApp(
    id = "com.microsoft.teams/.MainActivity#10",
    packageName = "com.microsoft.teams",
    activityName = ".MainActivity",
    label = "Teams (Work)",
    userHandleId = 10L
  )

  // Multiple Activities within the same package
  private val dialerMainActivity = DiscoveredApp(
    id = "com.google.android.dialer/.DialtactsActivity#0",
    packageName = "com.google.android.dialer",
    activityName = ".DialtactsActivity",
    label = "Phone",
    userHandleId = 0L
  )

  private val dialerContactsActivity = DiscoveredApp(
    id = "com.google.android.dialer/.ContactsActivity#0",
    packageName = "com.google.android.dialer",
    activityName = ".ContactsActivity",
    label = "Contacts",
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
    placementRepository = RoomPlacementRepository(spaceDao, layoutDao, membershipDao, context = null)
    val folderRepo = RoomFolderRepository(layoutDao)
    dockRepository = RoomDockRepository(spaceDao, layoutDao, placementRepository)

    repository = RoomSpaceRepository(
      spaceDao = spaceDao,
      membershipDao = membershipDao,
      layoutDao = layoutDao,
      preferences = preferences,
      context = null,
      membershipRepository = membershipRepo,
      placementRepository = placementRepository,
      folderRepository = folderRepo,
      dockRepository = dockRepository,
      sessionManager = LauncherSessionManager(context)
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testProfileIsolation_InMemberships() = runBlocking {
    val spaceRes = repository.createSpace("Profile Isolation Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Add both Personal and Work versions to the same space
    val addPersonal = repository.addAppToSpace(spaceId, personalTeams)
    val addWork = repository.addAppToSpace(spaceId, workTeams)
    assertTrue(addPersonal.isSuccess)
    assertTrue(addWork.isSuccess)

    // 2. Verify both exist simultaneously as distinct membership rows
    val memberships = repository.getMembershipsForSpace(spaceId)
    assertEquals(2, memberships.size)
    assertTrue(memberships.any { it.userHandleId == 0L && it.packageName == "com.microsoft.teams" })
    assertTrue(memberships.any { it.userHandleId == 10L && it.packageName == "com.microsoft.teams" })

    assertTrue(repository.isAppInSpace(spaceId, personalTeams))
    assertTrue(repository.isAppInSpace(spaceId, workTeams))

    // 3. Remove personal app only
    val removePersonal = repository.removeAppFromSpace(spaceId, personalTeams)
    assertTrue(removePersonal.isSuccess)

    // Work app must remain intact
    assertFalse(repository.isAppInSpace(spaceId, personalTeams))
    assertTrue("Work profile instance must still be in space", repository.isAppInSpace(spaceId, workTeams))
    assertEquals(1, repository.getMembershipsForSpace(spaceId).size)
  }

  @Test
  fun testProfileIsolation_InPlacements() = runBlocking {
    val spaceRes = repository.createSpace("Placement Isolation Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Place personal app on home screen
    val placePersonal = SpaceItemPlacement(
      id = "p_personal_teams",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = personalTeams.packageName,
      componentName = personalTeams.activityName,
      userHandleId = personalTeams.userHandleId
    )
    val res1 = repository.addPlacement(placePersonal)
    assertTrue(res1.isSuccess)

    // 2. Place work app on home screen
    val placeWork = SpaceItemPlacement(
      id = "p_work_teams",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = workTeams.packageName,
      componentName = workTeams.activityName,
      userHandleId = workTeams.userHandleId
    )
    val res2 = repository.addPlacement(placeWork)
    assertTrue(res2.isSuccess)

    // 3. Verify both placements co-exist without overwriting each other
    val placements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(2, placements.size)
    val personalItem = placements.firstOrNull { it.userHandleId == 0L }
    val workItem = placements.firstOrNull { it.userHandleId == 10L }

    assertNotNull("Personal app placement must exist", personalItem)
    assertNotNull("Work app placement must exist", workItem)
    assertEquals("p_personal_teams", personalItem?.id)
    assertEquals("p_work_teams", workItem?.id)
  }

  @Test
  fun testProfileIsolation_InDockAndFolders() = runBlocking {
    val spaceRes = repository.createSpace("Dock & Folder Isolation", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Dock Isolation
    val dockRes1 = repository.addAppToDock(spaceId, personalTeams, 0)
    val dockRes2 = repository.addAppToDock(spaceId, workTeams, 1)
    assertTrue(dockRes1.isSuccess && dockRes2.isSuccess)

    val dockItems = repository.getDockItemsForSpace(spaceId)
    assertEquals(2, dockItems.size)
    assertTrue(dockItems.any { it.userHandleId == 0L })
    assertTrue(dockItems.any { it.userHandleId == 10L })

    // Remove personal from dock -> work must remain
    val personalDockItem = dockItems.first { it.userHandleId == 0L }
    repository.removeAppFromDock(spaceId, personalDockItem.id)
    val remainingDock = repository.getDockItemsForSpace(spaceId)
    assertEquals(1, remainingDock.size)
    assertEquals(10L, remainingDock[0].userHandleId)

    // 2. Folder Isolation
    val folderRes = repository.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = 0,
      positionIndex = 4,
      folderName = "Collaboration",
      sourceApp = personalTeams,
      targetApp = workTeams,
      sourcePlacementId = null,
      targetPlacementId = null
    )
    assertTrue(folderRes.isSuccess)
    val folderId = folderRes.getOrThrow().id

    val folderItems = layoutDao.getFolderItems(folderId)
    assertEquals(2, folderItems.size)
    assertTrue(folderItems.any { it.userHandleId == 0L })
    assertTrue(folderItems.any { it.userHandleId == 10L })
  }

  @Test
  fun testDockExpansion_PreservesAndAllowsBothPersonalAndWorkProfiles() = runBlocking {
    val spaceRes = repository.createSpace("Dock Expansion Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Initial space has personal com.microsoft.teams in memberships and dock
    val addRes = repository.addAppToSpace(spaceId, personalTeams)
    assertTrue(addRes.isSuccess)
    val dockRes = repository.addAppToDock(spaceId, personalTeams, 0)
    assertTrue(dockRes.isSuccess)

    val initialDock = repository.getDockItemsForSpace(spaceId)
    assertEquals(1, initialDock.size)
    assertEquals(0L, initialDock[0].userHandleId)
    assertEquals("com.microsoft.teams", initialDock[0].packageName)

    // 2. Candidate apps available during expansion include both personal and work teams
    val expansionCandidates = listOf(personalTeams, workTeams)

    // Expand dock capacity from 1 to 2
    repository.expandDockItemsIfNeeded(
      spaceId = spaceId,
      newCapacity = 2,
      appsToSearch = expansionCandidates
    )

    // 3. Verify that the work profile was added and the personal profile was NOT suppressed/removed
    val expandedDock = repository.getDockItemsForSpace(spaceId)
    assertEquals(2, expandedDock.size)
    assertTrue("Personal profile copy must exist in dock", expandedDock.any { it.userHandleId == 0L && it.packageName == "com.microsoft.teams" })
    assertTrue("Work profile copy must exist in dock", expandedDock.any { it.userHandleId == 10L && it.packageName == "com.microsoft.teams" })

    // 4. Verify memberships also contain both profiles without suppression
    val memberships = repository.getMembershipsForSpace(spaceId)
    assertTrue("Personal profile copy must exist in memberships", memberships.any { it.userHandleId == 0L && it.packageName == "com.microsoft.teams" })
    assertTrue("Work profile copy must exist in memberships", memberships.any { it.userHandleId == 10L && it.packageName == "com.microsoft.teams" })
  }

  @Test
  fun testMultipleActivitiesWithinSamePackage_AreDistinct() = runBlocking {
    val spaceRes = repository.createSpace("Multi Activity Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id

    // 1. Add both activities to memberships
    repository.addAppToSpace(spaceId, dialerMainActivity)
    repository.addAppToSpace(spaceId, dialerContactsActivity)

    val memberships = repository.getMembershipsForSpace(spaceId)
    assertEquals(2, memberships.size)
    assertTrue(memberships.any { it.componentName == ".DialtactsActivity" })
    assertTrue(memberships.any { it.componentName == ".ContactsActivity" })

    // 2. Add both activities to home screen
    repository.addPlacement(
      SpaceItemPlacement(
        id = "p_dialer_main",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 0,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = dialerMainActivity.packageName,
        componentName = dialerMainActivity.activityName,
        userHandleId = dialerMainActivity.userHandleId
      )
    )
    repository.addPlacement(
      SpaceItemPlacement(
        id = "p_dialer_contacts",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 1,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = dialerContactsActivity.packageName,
        componentName = dialerContactsActivity.activityName,
        userHandleId = dialerContactsActivity.userHandleId
      )
    )

    val placements = repository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(2, placements.size)
    assertTrue(placements.any { it.componentName == ".DialtactsActivity" })
    assertTrue(placements.any { it.componentName == ".ContactsActivity" })
  }

  @Test
  fun testLegacyRecordsAndCrossProfileLookupSafety() {
    val installed = listOf(personalTeams, workTeams, dialerMainActivity)
    val lookup = AppIdentityLookup(installed)

    // 1. Strict exact lookup
    assertEquals(personalTeams.id, lookup[personalTeams.appIdentity]?.id)
    assertEquals(workTeams.id, lookup[workTeams.appIdentity]?.id)

    // 2. Legacy record: empty componentName with userHandleId = 0
    val legacyPersonal = AppIdentity(packageName = "com.microsoft.teams", componentName = "", userHandleId = 0L)
    val resolvedPersonal = lookup[legacyPersonal]
    assertNotNull(resolvedPersonal)
    assertEquals(0L, resolvedPersonal?.userHandleId)

    // 3. Legacy record: empty componentName with userHandleId = 10
    val legacyWork = AppIdentity(packageName = "com.microsoft.teams", componentName = "", userHandleId = 10L)
    val resolvedWork = lookup[legacyWork]
    assertNotNull(resolvedWork)
    assertEquals(10L, resolvedWork?.userHandleId)

    // 4. Cross-profile safety: Lookups must NEVER cross profiles
    // If an app only exists on work profile (userHandleId = 10), looking up with userHandleId = 0 returns null
    val workOnlyApp = DiscoveredApp(
      id = "com.intranet.secure/.Login#10",
      packageName = "com.intranet.secure",
      activityName = ".Login",
      label = "Intranet",
      userHandleId = 10L
    )
    val customLookup = AppIdentityLookup(listOf(personalTeams, workOnlyApp))

    val personalLookupForWorkOnly = customLookup[AppIdentity("com.intranet.secure", ".Login", 0L)]
    assertNull("Personal profile query must NOT return work profile instance", personalLookupForWorkOnly)
  }

  @Test
  fun testCrossProfilePlacements_IndependentCreationAndDeletion() = runBlocking {
    val space = repository.createSpace("Cross Profile Space", "GRID_4").getOrThrow()
    val spaceId = space.id

    // 1. Create two independent placements for the same package and component across profile 0 and 10
    val p0 = SpaceItemPlacement(
      id = "place_teams_p0",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = personalTeams.packageName,
      componentName = personalTeams.activityName,
      userHandleId = personalTeams.userHandleId
    )
    val p10 = SpaceItemPlacement(
      id = "place_teams_p10",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = workTeams.packageName,
      componentName = workTeams.activityName,
      userHandleId = workTeams.userHandleId
    )

    placementRepository.addPlacement(p0).getOrThrow()
    placementRepository.addPlacement(p10).getOrThrow()

    val initialPlacements = placementRepository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(2, initialPlacements.size)
    assertTrue(initialPlacements.any { it.userHandleId == 0L && it.packageName == "com.microsoft.teams" })
    assertTrue(initialPlacements.any { it.userHandleId == 10L && it.packageName == "com.microsoft.teams" })

    // 2. Deleting profile 10's placement leaves profile 0's placement untouched
    repository.cleanupUninstalledApp(workTeams.packageName, workTeams.userHandleId).getOrThrow()

    val remainingPlacements = placementRepository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(1, remainingPlacements.size)
    val remaining = remainingPlacements.first()
    assertEquals(0L, remaining.userHandleId)
    assertEquals("com.microsoft.teams", remaining.packageName)
  }

  @Test
  fun testDockIsolation_PreservesBothProfiles() = runBlocking {
    val space = repository.createSpace("Dock Space", "GRID_4").getOrThrow()
    val spaceId = space.id

    // 1. Docking an app under profile 0 and profile 10 preserves both dock entries
    dockRepository.addAppToDock(spaceId, personalTeams, 0).getOrThrow()
    dockRepository.addAppToDock(spaceId, workTeams, 1).getOrThrow()

    val dockItems = dockRepository.getDockItemsForSpace(spaceId)
    assertEquals(2, dockItems.size)
    assertTrue("Personal profile must be in dock", dockItems.any { it.userHandleId == 0L && it.packageName == "com.microsoft.teams" })
    assertTrue("Work profile must be in dock", dockItems.any { it.userHandleId == 10L && it.packageName == "com.microsoft.teams" })

    // 2. Removing work profile from dock leaves personal profile intact
    val workDockItem = dockItems.first { it.userHandleId == 10L }
    dockRepository.removeAppFromDock(spaceId, workDockItem.id).getOrThrow()

    val remainingDock = dockRepository.getDockItemsForSpace(spaceId)
    assertEquals(1, remainingDock.size)
    assertEquals(0L, remainingDock.first().userHandleId)
  }

  @Test
  fun testSafeRemoval_VirtualPlacementValidation() = runBlocking {
    val space = repository.createSpace("Safe Removal Space", "GRID_4").getOrThrow()
    val spaceId = space.id

    val p0 = SpaceItemPlacement(
      id = "place_teams_p0",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = personalTeams.packageName,
      componentName = personalTeams.activityName,
      userHandleId = 0L
    )
    val p10 = SpaceItemPlacement(
      id = "place_teams_p10",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = workTeams.packageName,
      componentName = workTeams.activityName,
      userHandleId = 10L
    )

    placementRepository.addPlacement(p0).getOrThrow()
    placementRepository.addPlacement(p10).getOrThrow()

    // 1. Ambiguous virtual identifier without explicit profile fails safely
    val removeAmbiguousRes = placementRepository.removePlacement("virtual:com.microsoft.teams")
    assertTrue("Removing ambiguous placement without profile must fail safely", removeAmbiguousRes.isFailure)

    // Verify neither placement was removed
    val afterAmbiguous = placementRepository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(2, afterAmbiguous.size)

    // 2. Exact profile-qualified virtual identifier only removes that specific profile
    val removeExactRes = placementRepository.removePlacement("virtual:com.microsoft.teams/.MainActivity#10")
    assertTrue("Removing exact profile placement should succeed", removeExactRes.isSuccess)

    val afterExact = placementRepository.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(1, afterExact.size)
    assertEquals(0L, afterExact.first().userHandleId)
  }

  @Test
  fun testMigration_10_11_PreservesMultipleProfilesAndResolvesCollisions() {
    val dbHelper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
      androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(null) // in-memory
        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(10) {
          override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
              CREATE TABLE IF NOT EXISTS `spaces` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `name` TEXT NOT NULL
              )
            """.trimIndent())
            db.execSQL("""
              CREATE TABLE IF NOT EXISTS `space_dock_items` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `space_id` TEXT NOT NULL,
                `order_index` INTEGER NOT NULL DEFAULT 0,
                `package_name` TEXT NOT NULL,
                `component_name` TEXT NOT NULL,
                `user_handle_id` INTEGER NOT NULL DEFAULT 0
              )
            """.trimIndent())
            db.execSQL("""
              CREATE TABLE IF NOT EXISTS `space_folder_items` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `folder_id` TEXT NOT NULL,
                `package_name` TEXT NOT NULL,
                `component_name` TEXT NOT NULL,
                `user_handle_id` INTEGER NOT NULL DEFAULT 0,
                `order_index` INTEGER NOT NULL DEFAULT 0
              )
            """.trimIndent())
            db.execSQL("""
              CREATE TABLE IF NOT EXISTS `space_item_placements` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `space_id` TEXT NOT NULL,
                `layer` INTEGER NOT NULL DEFAULT 1,
                `page_index` INTEGER NOT NULL DEFAULT 0,
                `position_index` INTEGER NOT NULL DEFAULT 0,
                `item_type` TEXT NOT NULL DEFAULT 'APP',
                `package_name` TEXT,
                `component_name` TEXT,
                `user_handle_id` INTEGER NOT NULL DEFAULT 0
              )
            """.trimIndent())
          }

          override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
        .build()
    )

    val db = dbHelper.writableDatabase
    try {
      db.execSQL("INSERT INTO spaces (id, name) VALUES ('s1', 'Main')")

      // Pre-migration state: Multiple profiles for same package
      db.execSQL("INSERT INTO space_dock_items (id, space_id, order_index, package_name, component_name, user_handle_id) VALUES ('d1', 's1', 0, 'com.microsoft.teams', '.MainActivity', 0)")
      db.execSQL("INSERT INTO space_dock_items (id, space_id, order_index, package_name, component_name, user_handle_id) VALUES ('d2', 's1', 1, 'com.microsoft.teams', '.MainActivity', 10)")

      db.execSQL("INSERT INTO space_item_placements (id, space_id, layer, page_index, position_index, item_type, package_name, component_name, user_handle_id) VALUES ('p1', 's1', 1, 0, 0, 'APP', 'com.microsoft.teams', '.MainActivity', 0)")
      db.execSQL("INSERT INTO space_item_placements (id, space_id, layer, page_index, position_index, item_type, package_name, component_name, user_handle_id) VALUES ('p2', 's1', 1, 0, 1, 'APP', 'com.microsoft.teams', '.MainActivity', 10)")

      // Distinct apps colliding on position_index = 2
      db.execSQL("INSERT INTO space_item_placements (id, space_id, layer, page_index, position_index, item_type, package_name, component_name, user_handle_id) VALUES ('p3', 's1', 1, 0, 2, 'APP', 'com.app.a', '.MainA', 0)")
      db.execSQL("INSERT INTO space_item_placements (id, space_id, layer, page_index, position_index, item_type, package_name, component_name, user_handle_id) VALUES ('p4', 's1', 1, 0, 2, 'APP', 'com.app.b', '.MainB', 0)")

      // Run MIGRATION_10_11
      LauncherDatabase.MIGRATION_10_11.migrate(db)

      // 1. Verify both dock items (profile 0 and 10) are preserved
      val dockCursor = db.query("SELECT user_handle_id FROM space_dock_items WHERE package_name = 'com.microsoft.teams'")
      val dockProfiles = mutableListOf<Long>()
      dockCursor.use {
        while (it.moveToNext()) dockProfiles.add(it.getLong(0))
      }
      assertEquals(2, dockProfiles.size)
      assertTrue(dockProfiles.contains(0L))
      assertTrue(dockProfiles.contains(10L))

      // 2. Verify both placements for com.microsoft.teams (profile 0 and 10) are preserved
      val placementCursor = db.query("SELECT user_handle_id FROM space_item_placements WHERE package_name = 'com.microsoft.teams'")
      val placementProfiles = mutableListOf<Long>()
      placementCursor.use {
        while (it.moveToNext()) placementProfiles.add(it.getLong(0))
      }
      assertEquals(2, placementProfiles.size)
      assertTrue(placementProfiles.contains(0L))
      assertTrue(placementProfiles.contains(10L))

      // 3. Verify position collision between p3 and p4 was resolved without deleting either app
      val collidingCursor = db.query("SELECT id, position_index FROM space_item_placements WHERE id IN ('p3', 'p4')")
      val positions = mutableMapOf<String, Int>()
      collidingCursor.use {
        while (it.moveToNext()) positions[it.getString(0)] = it.getInt(1)
      }
      assertEquals(2, positions.size)
      assertTrue(positions["p3"] != positions["p4"])
    } finally {
      db.close()
    }
  }
}
