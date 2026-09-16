package com.multispace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.multispace.data.dao.LaunchHistoryDao
import com.multispace.data.database.LaunchHistoryDatabase
import com.multispace.data.repository.RoomLaunchHistoryRepository
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.appIdentity
import com.multispace.platform.AppLaunchManager
import com.multispace.platform.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherRecentAppsTest {

  private lateinit var database: LaunchHistoryDatabase
  private lateinit var dao: LaunchHistoryDao
  private lateinit var repository: RoomLaunchHistoryRepository
  private lateinit var context: Context

  private val appA = DiscoveredApp(
    id = "com.example.appA/.MainActivity#0",
    packageName = "com.example.appA",
    activityName = ".MainActivity",
    label = "App A",
    userHandleId = 0L
  )

  private val appB = DiscoveredApp(
    id = "com.example.appB/.MainActivity#0",
    packageName = "com.example.appB",
    activityName = ".MainActivity",
    label = "App B",
    userHandleId = 0L
  )

  private val appC = DiscoveredApp(
    id = "com.example.appC/.MainActivity#0",
    packageName = "com.example.appC",
    activityName = ".MainActivity",
    label = "App C",
    userHandleId = 0L
  )

  // Work Profile version of App A (same package and component, userHandleId = 10)
  private val workAppA = DiscoveredApp(
    id = "com.example.appA/.MainActivity#10",
    packageName = "com.example.appA",
    activityName = ".MainActivity",
    label = "App A (Work)",
    userHandleId = 10L
  )

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, LaunchHistoryDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = database.launchHistoryDao()
    repository = RoomLaunchHistoryRepository(dao = dao, maxCapacity = 5)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testRecordLaunch_PersistsAndOrdersNewestFirst() = runBlocking {
    repository.recordLaunch(appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(appB.appIdentity, timestamp = 2000L)
    repository.recordLaunch(appC.appIdentity, timestamp = 3000L)

    val recents = repository.getRecentIdentitiesSync(limit = 10)
    assertEquals(3, recents.size)
    // Most recent (appC at 3000L) should be first
    assertEquals(appC.appIdentity, recents[0])
    assertEquals(appB.appIdentity, recents[1])
    assertEquals(appA.appIdentity, recents[2])
  }

  @Test
  fun testRelaunchExistingApp_UpdatesPositionWithoutDuplication() = runBlocking {
    repository.recordLaunch(appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(appB.appIdentity, timestamp = 2000L)
    repository.recordLaunch(appC.appIdentity, timestamp = 3000L)

    // Re-launch App A at a newer timestamp
    repository.recordLaunch(appA.appIdentity, timestamp = 4000L)

    val recents = repository.getRecentIdentitiesSync(limit = 10)
    assertEquals(3, recents.size) // No duplicate!
    assertEquals(appA.appIdentity, recents[0]) // Moved to newest
    assertEquals(appC.appIdentity, recents[1])
    assertEquals(appB.appIdentity, recents[2])
  }

  @Test
  fun testHistoryBoundedToMaxCapacity_AutoPrunesOldest() = runBlocking {
    val repoWithCapacity3 = RoomLaunchHistoryRepository(dao = dao, maxCapacity = 3)

    repoWithCapacity3.recordLaunch(AppIdentity("com.pkg1", ".Act", 0L), timestamp = 100L)
    repoWithCapacity3.recordLaunch(AppIdentity("com.pkg2", ".Act", 0L), timestamp = 200L)
    repoWithCapacity3.recordLaunch(AppIdentity("com.pkg3", ".Act", 0L), timestamp = 300L)
    repoWithCapacity3.recordLaunch(AppIdentity("com.pkg4", ".Act", 0L), timestamp = 400L) // should evict pkg1

    val recents = repoWithCapacity3.getRecentIdentitiesSync(limit = 10)
    assertEquals(3, recents.size)
    assertEquals("com.pkg4", recents[0].packageName)
    assertEquals("com.pkg3", recents[1].packageName)
    assertEquals("com.pkg2", recents[2].packageName)
    assertFalse(recents.any { it.packageName == "com.pkg1" })
  }

  @Test
  fun testProfileIsolation_PersonalAndWorkTrackedSeparately() = runBlocking {
    // Both share same package name and component name, but different userHandleId
    repository.recordLaunch(appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(workAppA.appIdentity, timestamp = 2000L)

    val recents = repository.getRecentIdentitiesSync(limit = 10)
    assertEquals(2, recents.size)
    assertEquals(workAppA.appIdentity, recents[0])
    assertEquals(appA.appIdentity, recents[1])

    assertEquals(10L, recents[0].userHandleId)
    assertEquals(0L, recents[1].userHandleId)
  }

  @Test
  fun testSafeResolution_ExcludesUninstalledAppsWithoutCrashing() = runBlocking {
    repository.recordLaunch(appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(appB.appIdentity, timestamp = 2000L) // Uninstalled / missing from installed list
    repository.recordLaunch(appC.appIdentity, timestamp = 3000L)

    // Installed apps only contains A and C, B has been uninstalled
    val installedApps = listOf(appA, appC)

    val resolved = repository.resolveRecentApps(installedApps, limit = 10)
    assertEquals(2, resolved.size)
    assertEquals(appC, resolved[0])
    assertEquals(appA, resolved[1])
  }

  @Test
  fun testSafeResolution_RespectsProfileBoundaries() = runBlocking {
    // Only personal appA is installed, workAppA was launched earlier
    repository.recordLaunch(workAppA.appIdentity, timestamp = 1000L)

    val installedApps = listOf(appA) // personal only

    val resolved = repository.resolveRecentApps(installedApps, limit = 10)
    // Must NOT mistakenly resolve personal appA for workAppA's launch entry
    assertTrue(resolved.isEmpty())
  }

  @Test
  fun testFailedLaunch_DoesNotRecordToHistory() = runBlocking {
    val launchManager = AppLaunchManager(
      context = context,
      historyRepository = repository,
      ioDispatcher = Dispatchers.Unconfined
    )

    // Attempting to launch an app that is not installed on the system will fail or be unavailable
    val uninstalledApp = DiscoveredApp(
      id = "com.nonexistent.app/.NoActivity#0",
      packageName = "com.nonexistent.app",
      activityName = ".NoActivity",
      label = "Nonexistent",
      userHandleId = 0L
    )

    val result = launchManager.launchApp(uninstalledApp)
    assertTrue(result !is LaunchResult.Success)

    // Verify nothing was recorded in launch history
    val recents = repository.getRecentIdentitiesSync(limit = 10)
    assertTrue("Failed launch must not record to history", recents.isEmpty())
  }

  @Test
  fun testRecentAppsFlow_ReactsToNewLaunches() = runBlocking {
    val installed = listOf(appA, appB, appC)
    repository.recordLaunch(appA.appIdentity, timestamp = 1000L)

    val initial = repository.getRecentApps(installed, limit = 5).first()
    assertEquals(1, initial.size)
    assertEquals(appA, initial[0])

    repository.recordLaunch(appB.appIdentity, timestamp = 2000L)
    val updated = repository.getRecentApps(installed, limit = 5).first()
    assertEquals(2, updated.size)
    assertEquals(appB, updated[0])
    assertEquals(appA, updated[1])
  }
}
