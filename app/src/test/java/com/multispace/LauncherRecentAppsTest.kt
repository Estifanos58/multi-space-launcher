package com.multispace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.multispace.data.dao.LaunchHistoryDao
import com.multispace.data.database.LaunchHistoryDatabase
import com.multispace.data.repository.RoomLaunchHistoryRepository
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
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

  private val defaultSpace = Space.DEFAULT_SPACE_ID
  private val workSpace = "space_work"
  private val secretSpace = "space_secret"

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
    repository = RoomLaunchHistoryRepository(launchHistoryDao = dao, maxEventsPerSpace = 50)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testRecordLaunch_PersistsAndOrdersNewestFirst() = runBlocking {
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2000L)
    repository.recordLaunch(defaultSpace, appC.appIdentity, timestamp = 3000L)

    val recents = repository.getRecentIdentitiesSync(defaultSpace, limit = 10)
    assertEquals(3, recents.size)
    // Most recent (appC at 3000L) should be first
    assertEquals(appC.appIdentity, recents[0])
    assertEquals(appB.appIdentity, recents[1])
    assertEquals(appA.appIdentity, recents[2])
  }

  @Test
  fun testRelaunchExistingApp_UpdatesPositionWithoutDuplication() = runBlocking {
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2000L)
    repository.recordLaunch(defaultSpace, appC.appIdentity, timestamp = 3000L)

    // Re-launch App A at a newer timestamp
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 4000L)

    val recents = repository.getRecentIdentitiesSync(defaultSpace, limit = 10)
    assertEquals(3, recents.size) // No duplicate!
    assertEquals(appA.appIdentity, recents[0]) // Moved to newest
    assertEquals(appC.appIdentity, recents[1])
    assertEquals(appB.appIdentity, recents[2])
  }

  @Test
  fun testSpaceIsolation_UsageInOneSpaceNeverAffectsAnotherSpace() = runBlocking {
    // Launch appA and appB in defaultSpace
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2000L)

    // Launch appC in workSpace
    repository.recordLaunch(workSpace, appC.appIdentity, timestamp = 3000L)

    // Verify defaultSpace contains appB and appA, but NOT appC
    val defaultRecents = repository.getRecentIdentitiesSync(defaultSpace, limit = 10)
    assertEquals(2, defaultRecents.size)
    assertEquals(appB.appIdentity, defaultRecents[0])
    assertEquals(appA.appIdentity, defaultRecents[1])

    // Verify workSpace contains appC only
    val workRecents = repository.getRecentIdentitiesSync(workSpace, limit = 10)
    assertEquals(1, workRecents.size)
    assertEquals(appC.appIdentity, workRecents[0])

    // Verify secretSpace has no history at all
    val secretRecents = repository.getRecentIdentitiesSync(secretSpace, limit = 10)
    assertTrue("Unused space should have empty history", secretRecents.isEmpty())
  }

  @Test
  fun testProfileIsolation_PersonalAndWorkTrackedSeparately() = runBlocking {
    // Both share same package name and component name, but different userHandleId
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(defaultSpace, workAppA.appIdentity, timestamp = 2000L)

    val recents = repository.getRecentIdentitiesSync(defaultSpace, limit = 10)
    assertEquals(2, recents.size)
    assertEquals(workAppA.appIdentity, recents[0])
    assertEquals(appA.appIdentity, recents[1])

    assertEquals(10L, recents[0].userHandleId)
    assertEquals(0L, recents[1].userHandleId)
  }

  @Test
  fun testMostUsedApps_OrdersByLaunchCountDescending() = runBlocking {
    // Launch App A 1 time
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)

    // Launch App B 3 times
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2000L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2100L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2200L)

    // Launch App C 2 times
    repository.recordLaunch(defaultSpace, appC.appIdentity, timestamp = 3000L)
    repository.recordLaunch(defaultSpace, appC.appIdentity, timestamp = 3100L)

    val installed = listOf(appA, appB, appC)
    val mostUsed = repository.resolveMostUsedApps(defaultSpace, installed, limit = 10)

    assertEquals(3, mostUsed.size)
    assertEquals(appB, mostUsed[0]) // 3 launches
    assertEquals(appC, mostUsed[1]) // 2 launches
    assertEquals(appA, mostUsed[2]) // 1 launch
  }

  @Test
  fun testSpaceUsageStats_CalculatesTotalsAndUniqueAppsAccurately() = runBlocking {
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2000L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2500L)

    val installed = listOf(appA, appB, appC)
    val stats = repository.getSpaceUsageStats(defaultSpace, installed)

    assertEquals(defaultSpace, stats.spaceId)
    assertEquals(3, stats.totalLaunches)
    assertEquals(2, stats.uniqueAppsCount)
    assertEquals(appB, stats.mostRecentlyLaunchedApp)
    assertEquals(appB, stats.mostUsedApp)

    val bAppStats = repository.getAppUsageStats(defaultSpace, appB.appIdentity, appB)
    val aAppStats = repository.getAppUsageStats(defaultSpace, appA.appIdentity, appA)
    assertEquals(2, bAppStats.launchCount)
    assertEquals(1, aAppStats.launchCount)
  }

  @Test
  fun testSafeResolution_ExcludesUninstalledAppsWithoutCrashing() = runBlocking {
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2000L) // Uninstalled / missing from installed list
    repository.recordLaunch(defaultSpace, appC.appIdentity, timestamp = 3000L)

    // Installed apps only contains A and C, B has been uninstalled
    val installedApps = listOf(appA, appC)

    val resolved = repository.resolveRecentApps(defaultSpace, installedApps, limit = 10)
    assertEquals(2, resolved.size)
    assertEquals(appC, resolved[0])
    assertEquals(appA, resolved[1])
  }

  @Test
  fun testSafeResolution_RespectsProfileBoundaries() = runBlocking {
    // Only personal appA is installed, workAppA was launched earlier
    repository.recordLaunch(defaultSpace, workAppA.appIdentity, timestamp = 1000L)

    val installedApps = listOf(appA) // personal only

    val resolved = repository.resolveRecentApps(defaultSpace, installedApps, limit = 10)
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

    val result = launchManager.launchAppSuspending(uninstalledApp, spaceId = defaultSpace)
    assertTrue(result !is LaunchResult.Success)

    // Verify nothing was recorded in launch history
    val recents = repository.getRecentIdentitiesSync(defaultSpace, limit = 10)
    assertTrue("Failed launch must not record to history", recents.isEmpty())
  }

  @Test
  fun testRecentAppsFlow_ReactsToNewLaunchesInSpace() = runBlocking {
    val installed = listOf(appA, appB, appC)
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)

    val initial = repository.getRecentApps(defaultSpace, installed, limit = 5).first()
    assertEquals(1, initial.size)
    assertEquals(appA, initial[0])

    repository.recordLaunch(defaultSpace, appB.appIdentity, timestamp = 2000L)
    val updated = repository.getRecentApps(defaultSpace, installed, limit = 5).first()
    assertEquals(2, updated.size)
    assertEquals(appB, updated[0])
    assertEquals(appA, updated[1])
  }

  @Test
  fun testClearSpaceHistory_OnlyClearsTargetSpace() = runBlocking {
    repository.recordLaunch(defaultSpace, appA.appIdentity, timestamp = 1000L)
    repository.recordLaunch(workSpace, appB.appIdentity, timestamp = 2000L)

    repository.clearHistoryForSpace(defaultSpace)

    val defaultRecents = repository.getRecentIdentitiesSync(defaultSpace, limit = 10)
    val workRecents = repository.getRecentIdentitiesSync(workSpace, limit = 10)

    assertTrue("defaultSpace history must be cleared", defaultRecents.isEmpty())
    assertEquals("workSpace history must remain intact", 1, workRecents.size)
    assertEquals(appB.appIdentity, workRecents[0])
  }
}
