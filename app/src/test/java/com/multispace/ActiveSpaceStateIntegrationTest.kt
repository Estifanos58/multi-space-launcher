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
import com.multispace.domain.model.ActiveSpaceState
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveSpaceStateIntegrationTest {

  private lateinit var database: LauncherDatabase
  private lateinit var spaceDao: SpaceDao
  private lateinit var membershipDao: SpaceMembershipDao
  private lateinit var layoutDao: SpaceLayoutDao
  private lateinit var preferences: LauncherPreferences
  private lateinit var repository: RoomSpaceRepository
  private lateinit var context: Context

  private val appA = DiscoveredApp(
    id = "com.alpha.app/.Main#0",
    packageName = "com.alpha.app",
    activityName = ".Main",
    label = "Alpha App",
    userHandleId = 0L
  )

  private val appB = DiscoveredApp(
    id = "com.beta.app/.Main#0",
    packageName = "com.beta.app",
    activityName = ".Main",
    label = "Beta App",
    userHandleId = 0L
  )

  private val appX = DiscoveredApp(
    id = "com.omega.app/.Main#0",
    packageName = "com.omega.app",
    activityName = ".Main",
    label = "Omega App",
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

  @Test
  fun testActiveSpaceState_SwitchingSpacesProvidesCoherentSnapshotWithoutLeakage() = runBlocking {
    // 1. Initialize Space 1
    val space1Res = repository.createSpace("Work Space", "GRID_4")
    val space1Id = space1Res.getOrThrow().id

    // Setup Space 1 items
    repository.addAppToSpace(space1Id, appA)
    repository.addAppToSpace(space1Id, appB)
    repository.addPlacement(
      SpaceItemPlacement(
        id = "p_appA",
        spaceId = space1Id,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 0,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = appA.packageName,
        componentName = appA.activityName,
        userHandleId = appA.userHandleId
      )
    )
    val folderRes = repository.createFolderFromApps(
      spaceId = space1Id,
      pageIndex = 0,
      positionIndex = 1,
      folderName = "Work Tools",
      sourceApp = appA,
      targetApp = appB,
      sourcePlacementId = null,
      targetPlacementId = null
    )
    val folderId = folderRes.getOrThrow().id
    repository.addAppToDock(space1Id, appA, 0)

    // 2. Initialize Space 2
    val space2Res = repository.createSpace("Personal Space", "GRID_5")
    val space2Id = space2Res.getOrThrow().id

    // Setup Space 2 items
    repository.addAppToSpace(space2Id, appX)
    repository.addPlacement(
      SpaceItemPlacement(
        id = "p_appX",
        spaceId = space2Id,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 2,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = appX.packageName,
        componentName = appX.activityName,
        userHandleId = appX.userHandleId
      )
    )
    repository.addAppToDock(space2Id, appX, 0)

    // 3. Set Active Space to Space 1
    repository.setActiveSpaceId(space1Id)
    val state1 = repository.activeSpaceStateFlow.first()

    assertNotNull("Active space state should have non-null space", state1.space)
    assertEquals(space1Id, state1.space?.id)
    assertEquals("Work Space", state1.space?.name)

    // Check Space 1 memberships
    assertEquals(2, state1.memberships.size)
    assertTrue(state1.memberships.any { it.packageName == appA.packageName })
    assertTrue(state1.memberships.any { it.packageName == appB.packageName })

    // Check Space 1 placements
    assertTrue(state1.placements.any { it.id == "p_appA" })
    assertTrue(state1.placements.any { it.folderId == folderId })

    // Check Space 1 folders & contents
    assertEquals(1, state1.folders.size)
    assertEquals("Work Tools", state1.folders[0].name)
    assertEquals(2, state1.folders[0].items.size)

    // Check Space 1 dock
    assertEquals(1, state1.dockItems.size)
    assertEquals(appA.packageName, state1.dockItems[0].packageName)

    // 4. Switch Active Space to Space 2
    repository.setActiveSpaceId(space2Id)
    val state2 = repository.activeSpaceStateFlow.first()

    assertEquals(space2Id, state2.space?.id)
    assertEquals("Personal Space", state2.space?.name)

    // Verify ZERO state leakage from Space 1 to Space 2:
    assertEquals(1, state2.memberships.size)
    assertEquals(appX.packageName, state2.memberships[0].packageName)

    assertEquals(1, state2.placements.size)
    assertEquals("p_appX", state2.placements[0].id)
    assertEquals(appX.packageName, state2.placements[0].packageName)

    assertTrue("Space 2 should have no folders from Space 1", state2.folders.isEmpty())

    assertEquals(1, state2.dockItems.size)
    assertEquals(appX.packageName, state2.dockItems[0].packageName)
  }

  @Test
  fun testProcessRecreation_ReloadsActiveSpaceStateCleanly() = runBlocking {
    // 1. Initialize Space in repository instance 1
    val spaceRes = repository.createSpace("Persistent Space", "GRID_4")
    val spaceId = spaceRes.getOrThrow().id
    repository.setActiveSpaceId(spaceId)
    repository.addAppToSpace(spaceId, appA)
    repository.addPlacement(
      SpaceItemPlacement(
        id = "p_recreate",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 3,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = appA.packageName,
        componentName = appA.activityName,
        userHandleId = appA.userHandleId
      )
    )

    // 2. Simulate process recreation: re-instantiate repository pointing to same database and preferences
    val recreatedMembershipRepo = RoomSpaceMembershipRepository(membershipDao)
    val recreatedPlacementRepo = RoomPlacementRepository(spaceDao, layoutDao, membershipDao, context = null)
    val recreatedFolderRepo = RoomFolderRepository(layoutDao)
    val recreatedDockRepo = RoomDockRepository(spaceDao, layoutDao, recreatedPlacementRepo)

    val recreatedRepository = RoomSpaceRepository(
      spaceDao = spaceDao,
      membershipDao = membershipDao,
      layoutDao = layoutDao,
      preferences = preferences,
      context = null,
      membershipRepository = recreatedMembershipRepo,
      placementRepository = recreatedPlacementRepo,
      folderRepository = recreatedFolderRepo,
      dockRepository = recreatedDockRepo
    )

    // 3. Verify recreated repository loads the identical active space state snapshot
    val state = recreatedRepository.activeSpaceStateFlow.first()
    assertNotNull(state.space)
    assertEquals(spaceId, state.space?.id)
    assertEquals("Persistent Space", state.space?.name)
    assertEquals(1, state.memberships.size)
    assertEquals(appA.packageName, state.memberships[0].packageName)
    assertEquals(1, state.placements.size)
    assertEquals("p_recreate", state.placements[0].id)
  }
}
