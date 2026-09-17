package com.multispace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.multispace.data.dao.SpaceDao
import com.multispace.data.dao.SpaceLayoutDao
import com.multispace.data.dao.SpaceMembershipDao
import com.multispace.data.database.LauncherDatabase
import com.multispace.data.entity.SpaceFolderEntity
import com.multispace.data.entity.SpaceFolderItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.data.repository.RoomDockRepository
import com.multispace.data.repository.RoomFolderRepository
import com.multispace.data.repository.RoomPlacementRepository
import com.multispace.data.repository.RoomSpaceMembershipRepository
import com.multispace.data.repository.RoomSpaceRepository
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceItemPlacement
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.sql.SQLException

@RunWith(RobolectricTestRunner::class)
class LayoutTransactionSafetyTest {

  private lateinit var database: LauncherDatabase
  private lateinit var context: Context
  private lateinit var preferences: LauncherPreferences
  private lateinit var realLayoutDao: SpaceLayoutDao
  private lateinit var faultInjectingLayoutDao: FaultInjectingLayoutDao
  private lateinit var faultInjectingMembershipDao: FaultInjectingMembershipDao
  private lateinit var spaceDao: SpaceDao

  private lateinit var spaceRepo: RoomSpaceRepository
  private lateinit var placementRepo: RoomPlacementRepository
  private lateinit var folderRepo: RoomFolderRepository
  private lateinit var dockRepo: RoomDockRepository
  private lateinit var membershipRepo: RoomSpaceMembershipRepository

  private val appA = DiscoveredApp(
    id = "com.test.appA/.MainActivity#0",
    packageName = "com.test.appA",
    activityName = ".MainActivity",
    label = "App A",
    userHandleId = 0L
  )

  private val appB = DiscoveredApp(
    id = "com.test.appB/.MainActivity#0",
    packageName = "com.test.appB",
    activityName = ".MainActivity",
    label = "App B",
    userHandleId = 0L
  )

  private val appC = DiscoveredApp(
    id = "com.test.appC/.MainActivity#0",
    packageName = "com.test.appC",
    activityName = ".MainActivity",
    label = "App C",
    userHandleId = 0L
  )

  class FaultInjectingLayoutDao(private val delegate: SpaceLayoutDao) : SpaceLayoutDao by delegate {
    var shouldFailInsertFolderItem = false
    var shouldFailInsertFolderItems = false
    var shouldFailInsertPlacements = false
    var shouldFailDeletePlacementById = false

    override suspend fun insertFolderItem(item: SpaceFolderItemEntity) {
      if (shouldFailInsertFolderItem) {
        throw SQLException("Simulated database failure during insertFolderItem")
      }
      delegate.insertFolderItem(item)
    }

    override suspend fun insertFolderItems(items: List<SpaceFolderItemEntity>) {
      if (shouldFailInsertFolderItems) {
        throw SQLException("Simulated database failure during insertFolderItems")
      }
      delegate.insertFolderItems(items)
    }

    override suspend fun insertPlacements(placements: List<SpaceItemPlacementEntity>) {
      if (shouldFailInsertPlacements) {
        throw SQLException("Simulated database failure during insertPlacements")
      }
      delegate.insertPlacements(placements)
    }

    override suspend fun deletePlacementById(placementId: String) {
      if (shouldFailDeletePlacementById) {
        throw SQLException("Simulated database failure during deletePlacementById")
      }
      delegate.deletePlacementById(placementId)
    }
  }

  class FaultInjectingMembershipDao(private val delegate: SpaceMembershipDao) : SpaceMembershipDao by delegate {
    var shouldFailInsertMemberships = false
    var shouldFailUpdateMembershipOrder = false

    override suspend fun insertMemberships(memberships: List<com.multispace.data.entity.SpaceMembershipEntity>) {
      if (shouldFailInsertMemberships) {
        throw SQLException("Simulated database failure during insertMemberships")
      }
      delegate.insertMemberships(memberships)
    }

    override suspend fun updateMembershipOrder(
      spaceId: String,
      packageName: String,
      componentName: String,
      userHandleId: Long,
      newOrderIndex: Int
    ): Int {
      if (shouldFailUpdateMembershipOrder) {
        throw SQLException("Simulated database failure during updateMembershipOrder")
      }
      return delegate.updateMembershipOrder(spaceId, packageName, componentName, userHandleId, newOrderIndex)
    }
  }

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    spaceDao = database.spaceDao()
    realLayoutDao = database.spaceLayoutDao()
    faultInjectingLayoutDao = FaultInjectingLayoutDao(realLayoutDao)
    faultInjectingMembershipDao = FaultInjectingMembershipDao(database.spaceMembershipDao())
    preferences = LauncherPreferences(context)

    placementRepo = RoomPlacementRepository(
      spaceDao = spaceDao,
      layoutDao = faultInjectingLayoutDao,
      membershipDao = faultInjectingMembershipDao,
      context = null,
      database = database
    )
    folderRepo = RoomFolderRepository(
      layoutDao = faultInjectingLayoutDao,
      database = database
    )
    dockRepo = RoomDockRepository(
      spaceDao = spaceDao,
      layoutDao = faultInjectingLayoutDao,
      placementRepository = placementRepo,
      database = database
    )
    membershipRepo = RoomSpaceMembershipRepository(
      membershipDao = faultInjectingMembershipDao,
      database = database
    )

    spaceRepo = RoomSpaceRepository(
      spaceDao = spaceDao,
      membershipDao = faultInjectingMembershipDao,
      layoutDao = faultInjectingLayoutDao,
      preferences = preferences,
      context = null,
      database = database,
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
  fun testCreateFolderFromPlacements_RollsBackOnMidwayFailure() = runBlocking {
    val space = spaceRepo.createSpace("Folder Rollback Space", "GRID_4").getOrThrow()
    val spaceId = space.id

    // Setup 2 initial app placements
    val p1 = SpaceItemPlacement(
      id = "p1",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = appA.packageName,
      componentName = appA.activityName,
      userHandleId = appA.userHandleId
    )
    val p2 = SpaceItemPlacement(
      id = "p2",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = appB.packageName,
      componentName = appB.activityName,
      userHandleId = appB.userHandleId
    )
    assertTrue(placementRepo.addPlacement(p1).isSuccess)
    assertTrue(placementRepo.addPlacement(p2).isSuccess)

    // Snapshot pre-failure state
    val initialPlacements = realLayoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    val initialFolders = realLayoutDao.getFoldersForSpace(spaceId)
    assertEquals(2, initialPlacements.size)
    assertEquals(0, initialFolders.size)

    // Trigger failure during folder items insertion
    faultInjectingLayoutDao.shouldFailInsertFolderItems = true

    val result = folderRepo.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = 0,
      positionIndex = 1,
      folderName = "Utilities",
      sourceApp = appA,
      targetApp = appB,
      sourcePlacementId = p1.id,
      targetPlacementId = p2.id
    )

    assertTrue("Operation must fail when DB throws mid-transaction", result.isFailure)

    // Turn off failure to verify DB state
    faultInjectingLayoutDao.shouldFailInsertFolderItems = false

    // Assert complete rollback:
    // 1. No folders were persisted
    val afterFolders = realLayoutDao.getFoldersForSpace(spaceId)
    assertEquals("Folders must be completely rolled back", 0, afterFolders.size)

    // 2. Both original placements remain intact at their original positions
    val afterPlacements = realLayoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals("Placements must not be partially deleted", 2, afterPlacements.size)
    val afterP1 = afterPlacements.firstOrNull { it.id == "p1" }
    val afterP2 = afterPlacements.firstOrNull { it.id == "p2" }
    assertNotNull("p1 must still exist", afterP1)
    assertNotNull("p2 must still exist", afterP2)
    assertEquals(0, afterP1?.positionIndex)
    assertEquals(1, afterP2?.positionIndex)

    // 3. Invariants are maintained
    DatabaseInvariantAssertions.assertAllLayoutInvariants(database, spaceId)
  }

  @Test
  fun testAddAppToFolderWithSourcePlacement_RollsBackOnMidwayFailure() = runBlocking {
    val space = spaceRepo.createSpace("Folder Add Rollback", "GRID_4").getOrThrow()
    val spaceId = space.id

    // Create a valid folder first from appA and appB
    val folderRes = folderRepo.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = 0,
      positionIndex = 0,
      folderName = "Tools",
      sourceApp = appA,
      targetApp = appB
    )
    assertTrue(folderRes.isSuccess)
    val folder = folderRes.getOrThrow()

    // Add an app placement to page 0, pos 1
    val pApp = SpaceItemPlacement(
      id = "place_app_c",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = appC.packageName,
      componentName = appC.activityName,
      userHandleId = appC.userHandleId
    )
    assertTrue(placementRepo.addPlacement(pApp).isSuccess)

    // Snapshot pre-failure state
    val folderItemsBefore = realLayoutDao.getFolderItems(folder.id)
    val placementBefore = realLayoutDao.getPlacementById(pApp.id)
    assertEquals(2, folderItemsBefore.size)
    assertNotNull(placementBefore)

    // Enable failure when deleting source placement
    faultInjectingLayoutDao.shouldFailDeletePlacementById = true

    val result = folderRepo.addAppToFolder(folder.id, appC, sourcePlacementId = pApp.id)
    assertTrue("Operation must fail when placement deletion fails", result.isFailure)

    faultInjectingLayoutDao.shouldFailDeletePlacementById = false

    // Assert atomic rollback:
    // 1. The item was NOT added to the folder
    val folderItemsAfter = realLayoutDao.getFolderItems(folder.id)
    assertEquals("Folder item count must be unchanged after rollback", 2, folderItemsAfter.size)

    // 2. The source placement still exists
    val placementAfter = realLayoutDao.getPlacementById(pApp.id)
    assertNotNull("Source placement must not be deleted if transaction failed", placementAfter)
    assertEquals(1, placementAfter?.positionIndex)

    DatabaseInvariantAssertions.assertAllLayoutInvariants(database, spaceId)
  }

  @Test
  fun testMoveAppToPage_RollsBackOnMidwayFailure() = runBlocking {
    val space = spaceRepo.createSpace("Move App Rollback", "GRID_4").getOrThrow()
    val spaceId = space.id

    val p1 = SpaceItemPlacement(
      id = "p1",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = appA.packageName,
      componentName = appA.activityName,
      userHandleId = appA.userHandleId
    )
    val p2 = SpaceItemPlacement(
      id = "p2",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = appB.packageName,
      componentName = appB.activityName,
      userHandleId = appB.userHandleId
    )
    placementRepo.addPlacement(p1)
    placementRepo.addPlacement(p2)

    val prePlacements = realLayoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(2, prePlacements.size)

    // Inject failure on insertPlacements (which writes the cascading/updated placements)
    faultInjectingLayoutDao.shouldFailInsertPlacements = true

    val result = placementRepo.moveAppToPage(spaceId, placementId = "p1", targetPage = 1, targetPosition = 0)
    assertTrue("moveAppToPage must fail when DB operation fails", result.isFailure)

    faultInjectingLayoutDao.shouldFailInsertPlacements = false

    // Assert rollback:
    // Placements must remain on Page 0 at original positions
    val postPlacements = realLayoutDao.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(2, postPlacements.size)
    val restoredP1 = postPlacements.firstOrNull { it.id == "p1" }
    assertEquals(0, restoredP1?.pageIndex)
    assertEquals(0, restoredP1?.positionIndex)

    DatabaseInvariantAssertions.assertAllLayoutInvariants(database, spaceId)
  }

  @Test
  fun testReorderSpaceApps_RollsBackOnMidwayFailure() = runBlocking {
    val space = spaceRepo.createSpace("Reorder Rollback", "GRID_4").getOrThrow()
    val spaceId = space.id

    membershipRepo.addAppToSpace(spaceId, appA)
    membershipRepo.addAppToSpace(spaceId, appB)
    membershipRepo.addAppToSpace(spaceId, appC)

    val preMemberships = database.spaceMembershipDao().getMembershipsForSpace(spaceId)
    assertEquals(3, preMemberships.size)

    // Inject failure during update
    faultInjectingMembershipDao.shouldFailUpdateMembershipOrder = true

    val reversedApps = listOf(appC, appB, appA)
    val result = membershipRepo.reorderSpaceApps(spaceId, reversedApps)
    assertTrue("reorderSpaceApps must fail when DB throws", result.isFailure)

    faultInjectingMembershipDao.shouldFailUpdateMembershipOrder = false

    // Assert rollback: memberships were not left deleted or half-written!
    val postMemberships = database.spaceMembershipDao().getMembershipsForSpace(spaceId)
    assertEquals(3, postMemberships.size)
    assertEquals(appA.packageName, postMemberships.first { it.orderIndex == 0 }.packageName)
    assertEquals(appB.packageName, postMemberships.first { it.orderIndex == 1 }.packageName)
    assertEquals(appC.packageName, postMemberships.first { it.orderIndex == 2 }.packageName)

    DatabaseInvariantAssertions.assertValidMembershipOrdering(database, spaceId)
  }

  @Test
  fun testSequentialSuccessfulMutations_PreserveAllInvariants() = runBlocking {
    val space = spaceRepo.createSpace("Invariants Preserved Space", "GRID_4").getOrThrow()
    val spaceId = space.id

    // 1. Add 3 apps
    placementRepo.addPlacement(
      SpaceItemPlacement(
        id = "p_a",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 0,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = appA.packageName,
        componentName = appA.activityName,
        userHandleId = appA.userHandleId
      )
    )
    placementRepo.addPlacement(
      SpaceItemPlacement(
        id = "p_b",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 1,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = appB.packageName,
        componentName = appB.activityName,
        userHandleId = appB.userHandleId
      )
    )
    placementRepo.addPlacement(
      SpaceItemPlacement(
        id = "p_c",
        spaceId = spaceId,
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 2,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = appC.packageName,
        componentName = appC.activityName,
        userHandleId = appC.userHandleId
      )
    )
    DatabaseInvariantAssertions.assertAllLayoutInvariants(database, spaceId)

    // 2. Move app to Page 1
    assertTrue(placementRepo.moveAppToPage(spaceId, "p_c", targetPage = 1, targetPosition = 0).isSuccess)
    DatabaseInvariantAssertions.assertAllLayoutInvariants(database, spaceId)

    // 3. Create folder from p_a and p_b
    val folderRes = folderRepo.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = 0,
      positionIndex = 1,
      folderName = "Work Folder",
      sourceApp = appA,
      targetApp = appB,
      sourcePlacementId = "p_a",
      targetPlacementId = "p_b"
    )
    assertTrue(folderRes.isSuccess)
    DatabaseInvariantAssertions.assertAllLayoutInvariants(database, spaceId)

    // 4. Add app C into that folder
    val folder = folderRes.getOrThrow()
    assertTrue(folderRepo.addAppToFolder(folder.id, appC, sourcePlacementId = "p_c").isSuccess)
    DatabaseInvariantAssertions.assertAllLayoutInvariants(database, spaceId)

    // 5. Add dock item
    assertTrue(dockRepo.addAppToDock(spaceId, appA, orderIndex = 0).isSuccess)
    DatabaseInvariantAssertions.assertDockPositionsSequential(database, spaceId)
  }
}
