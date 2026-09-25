package com.multispace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.multispace.data.database.LauncherDatabase
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.data.repository.RoomSpaceRepository
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.platform.LauncherSessionManager
import java.io.File
import java.util.UUID
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
class RoomPersistenceRecreationTest {

  private lateinit var context: Context
  private lateinit var dbFile: File
  private var activeDb: LauncherDatabase? = null

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val tempDir = context.cacheDir
    dbFile = File(tempDir, "test_persistence_${UUID.randomUUID()}.db")
  }

  @After
  fun tearDown() {
    activeDb?.close()
    activeDb = null
    if (dbFile.exists()) {
      dbFile.delete()
    }
    val walFile = File(dbFile.path + "-wal")
    if (walFile.exists()) walFile.delete()
    val shmFile = File(dbFile.path + "-shm")
    if (shmFile.exists()) shmFile.delete()
  }

  private fun openDatabase(file: File): LauncherDatabase {
    val db = Room.databaseBuilder(
      context,
      LauncherDatabase::class.java,
      file.absolutePath
    )
      .fallbackToDestructiveMigration(true)
      .allowMainThreadQueries()
      .build()
    activeDb = db
    return db
  }

  @Test
  fun testSpacePersistenceRecreationLifecycle() = runBlocking {
    val notesApp = DiscoveredApp(
      id = "com.notes/.MainActivity/0",
      packageName = "com.notes",
      activityName = ".MainActivity",
      label = "Notes"
    )
    val browserApp = DiscoveredApp(
      id = "com.browser/.MainActivity/0",
      packageName = "com.browser",
      activityName = ".MainActivity",
      label = "Browser"
    )
    val musicApp = DiscoveredApp(
      id = "com.music/.MainActivity/0",
      packageName = "com.music",
      activityName = ".MainActivity",
      label = "Music"
    )
    val appList = listOf(notesApp, browserApp, musicApp)

    // 1. Initial Launch: open database and instantiate repositories
    val db1 = openDatabase(dbFile)
    val preferences1 = LauncherPreferences(context)
    val spaceRepo1 = RoomSpaceRepository(
      spaceDao = db1.spaceDao(),
      membershipDao = db1.spaceMembershipDao(),
      layoutDao = db1.spaceLayoutDao(),
      preferences = preferences1,
      context = context,
      sessionManager = LauncherSessionManager(context)
    )

    // 2. Create a Space
    val initialSpaceResult = spaceRepo1.createSpace(
      name = "Project Alpha",
      layoutType = "GRID_4",
      initialApps = appList
    )
    assertTrue("Space creation failed: ${initialSpaceResult.exceptionOrNull()?.message}", initialSpaceResult.isSuccess)
    val createdSpace = initialSpaceResult.getOrThrow()
    val spaceId = createdSpace.id

    // 3. Add Placements (Widget and additional app placement)
    val notesPlacement = SpaceItemPlacement(
      id = "placement_notes_custom",
      spaceId = spaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = notesApp.packageName,
      componentName = notesApp.activityName
    )
    spaceRepo1.addPlacement(notesPlacement)

    val widgetResult = spaceRepo1.addWidgetPlacement(
      spaceId = spaceId,
      pageIndex = 0,
      widgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE,
      spanX = 2,
      spanY = 2
    )
    assertTrue("Widget placement should succeed", widgetResult.isSuccess)
    val addedWidget = widgetResult.getOrThrow()

    // 4. Add Folder with Folder Items
    val folderResult = spaceRepo1.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = 0,
      positionIndex = 8,
      folderName = "Utilities",
      sourceApp = notesApp,
      targetApp = browserApp,
      sourcePlacementId = null,
      targetPlacementId = null
    )
    assertTrue("Folder creation should succeed", folderResult.isSuccess)
    val createdFolder = folderResult.getOrThrow()

    // 5. Add Dock Item
    val dockResult = spaceRepo1.addAppToDock(
      spaceId = spaceId,
      app = musicApp,
      orderIndex = 0
    )
    assertTrue("Dock addition should succeed", dockResult.isSuccess)

    // 6. Persist Active Space
    spaceRepo1.setActiveSpaceId(spaceId)

    // 7. Capture expected state
    val stateBeforeClose = spaceRepo1.activeSpaceStateFlow.first()
    assertNotNull(stateBeforeClose.space)
    assertEquals("Project Alpha", stateBeforeClose.space?.name)
    val expectedMemberships = spaceRepo1.getMembershipsForSpace(spaceId)
    val expectedPlacements = spaceRepo1.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    val expectedFolders = spaceRepo1.getFoldersForSpace(spaceId)
    val expectedDockItems = spaceRepo1.getDockItemsForSpace(spaceId)

    assertTrue("Expected memberships not empty", expectedMemberships.isNotEmpty())
    assertTrue("Expected placements not empty", expectedPlacements.isNotEmpty())
    assertTrue("Expected folders not empty", expectedFolders.isNotEmpty())
    assertTrue("Expected dock items not empty", expectedDockItems.isNotEmpty())

    // 8. Close the database
    db1.close()
    activeDb = null

    // 9. Reopen database from the exact same file and recreate repositories
    val db2 = openDatabase(dbFile)
    val preferences2 = LauncherPreferences(context)
    val spaceRepo2 = RoomSpaceRepository(
      spaceDao = db2.spaceDao(),
      membershipDao = db2.spaceMembershipDao(),
      layoutDao = db2.spaceLayoutDao(),
      preferences = preferences2,
      context = context,
      sessionManager = LauncherSessionManager(context)
    )

    // 10. Verify identical state after complete recreation
    val reloadedSpace = spaceRepo2.getSpaceById(spaceId)
    assertNotNull("Reloaded space must exist", reloadedSpace)
    assertEquals(createdSpace.id, reloadedSpace?.id)
    assertEquals(createdSpace.name, reloadedSpace?.name)
    assertEquals(createdSpace.layoutType, reloadedSpace?.layoutType)
    assertEquals(createdSpace.gridColumns, reloadedSpace?.gridColumns)

    val reloadedActiveState = spaceRepo2.activeSpaceStateFlow.first()
    assertEquals(spaceId, reloadedActiveState.space?.id)
    assertEquals("Project Alpha", reloadedActiveState.space?.name)

    // Verify memberships
    val reloadedMemberships = spaceRepo2.getMembershipsForSpace(spaceId)
    assertEquals(expectedMemberships.size, reloadedMemberships.size)
    val expectedPkgSet = expectedMemberships.map { it.packageName }.toSet()
    val reloadedPkgSet = reloadedMemberships.map { it.packageName }.toSet()
    assertEquals(expectedPkgSet, reloadedPkgSet)

    // Verify placements
    val reloadedPlacements = spaceRepo2.getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    assertEquals(expectedPlacements.size, reloadedPlacements.size)
    val expectedPlacementIds = expectedPlacements.map { it.id }.toSet()
    val reloadedPlacementIds = reloadedPlacements.map { it.id }.toSet()
    assertEquals(expectedPlacementIds, reloadedPlacementIds)

    // Verify widget placement properties survived
    val reloadedWidget = reloadedPlacements.firstOrNull { it.id == addedWidget.id }
    assertNotNull("Widget placement must survive reload", reloadedWidget)
    assertEquals(2, reloadedWidget?.spanX)
    assertEquals(2, reloadedWidget?.spanY)

    // Verify folder and folder items survived
    val reloadedFolders = spaceRepo2.getFoldersForSpace(spaceId)
    assertEquals(expectedFolders.size, reloadedFolders.size)
    val reloadedFolder = reloadedFolders.first { it.id == createdFolder.id }
    assertEquals(createdFolder.id, reloadedFolder.id)
    assertEquals("Utilities", reloadedFolder.name)
    assertEquals(2, reloadedFolder.items.size)

    // Verify dock items survived
    val reloadedDockItems = spaceRepo2.getDockItemsForSpace(spaceId)
    assertEquals(expectedDockItems.size, reloadedDockItems.size)
    val reloadedDockItem = reloadedDockItems.first()
    assertEquals("com.music", reloadedDockItem.packageName)

    // Clean close
    db2.close()
    activeDb = null
  }

  @Test
  fun testMigration9To10PrunesDuplicatePlacements() = runBlocking {
    val db = openDatabase(dbFile)
    val sqliteDb = db.openHelper.writableDatabase

    // Insert space via DAO to ensure all entity columns match schema
    val space = Space(
      id = "space_mig_test",
      name = "Mig Test"
    )
    db.spaceDao().insertSpace(com.multispace.data.entity.SpaceEntity.fromDomain(space))

    // Insert duplicate placements for the same app package in the same space/layer
    val dup1 = com.multispace.data.entity.SpaceItemPlacementEntity(
      id = "dup_place_1",
      spaceId = "space_mig_test",
      layer = 1,
      pageIndex = 0,
      positionIndex = 0,
      itemType = "APP",
      packageName = "com.duplicate.app"
    )
    val dup2 = com.multispace.data.entity.SpaceItemPlacementEntity(
      id = "dup_place_2",
      spaceId = "space_mig_test",
      layer = 1,
      pageIndex = 0,
      positionIndex = 1,
      itemType = "APP",
      packageName = "com.duplicate.app"
    )
    val distinct = com.multispace.data.entity.SpaceItemPlacementEntity(
      id = "distinct_place",
      spaceId = "space_mig_test",
      layer = 1,
      pageIndex = 0,
      positionIndex = 2,
      itemType = "APP",
      packageName = "com.distinct.app"
    )
    db.spaceLayoutDao().insertPlacements(listOf(dup1, dup2, distinct))

    // Run MIGRATION_9_10
    LauncherDatabase.MIGRATION_9_10.migrate(sqliteDb)

    // Verify duplicate was pruned while distinct remains
    val cursor = sqliteDb.query("SELECT id, package_name FROM space_item_placements WHERE space_id = 'space_mig_test'")
    val remainingPlacements = mutableListOf<Pair<String, String>>()
    while (cursor.moveToNext()) {
      remainingPlacements.add(cursor.getString(0) to cursor.getString(1))
    }
    cursor.close()

    assertEquals("Should only retain 2 placements (1 distinct + 1 deduplicated)", 2, remainingPlacements.size)
    val remainingPackages = remainingPlacements.map { it.second }.toSet()
    assertTrue(remainingPackages.contains("com.duplicate.app"))
    assertTrue(remainingPackages.contains("com.distinct.app"))

    db.close()
    activeDb = null
  }
}
