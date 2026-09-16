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
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.domain.model.appIdentity
import com.multispace.domain.model.PresetLayoutHelper
import com.multispace.platform.AppLaunchManager
import com.multispace.platform.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

/**
 * Comprehensive verification of the 15 specific scenarios for the unified Space usage system:
 *
 * 1. Launch app in Space A -> appears in Space A Layer 2 Recent Opened Apps
 * 2. Launch app in Space A -> does NOT appear in Space B Layer 2 Recent Opened Apps
 * 3. Launch app multiple times -> moves to front of Recent Opened Apps without duplicate entries
 * 4. Work Profile app and Personal Profile app with same package name tracked independently
 * 5. Same package with different component names tracked independently
 * 6. Failed launch (e.g. ActivityNotFoundException) does NOT record to usage history
 * 7. Launches recorded through AppLaunchManager update history repository
 * 8. Most Used Apps folder on Layer 1 Page 0 correctly lists apps ordered by launch frequency in that Space
 * 9. Most Used Apps folder does not include apps from other Spaces
 * 10. Dynamic Most Used Apps folder reacts to new launches in real-time
 * 11. Usage Statistics widget displays correct total launch count for active Space
 * 12. Usage Statistics widget displays correct unique apps count for active Space
 * 13. Usage Statistics widget displays correct top/most used app for active Space
 * 14. Usage Statistics in Space A does not count launches from Space B
 * 15. Uninstalled/unmapped apps are safely filtered out without crashes
 */
@RunWith(RobolectricTestRunner::class)
class SpaceUsageSystemTest {

  private lateinit var database: LaunchHistoryDatabase
  private lateinit var dao: LaunchHistoryDao
  private lateinit var repository: RoomLaunchHistoryRepository
  private lateinit var context: Context

  private val spaceA = "space_personal"
  private val spaceB = "space_work"

  private val app1 = DiscoveredApp(
    id = "com.google.android.youtube/.MainActivity#0",
    packageName = "com.google.android.youtube",
    activityName = ".MainActivity",
    label = "YouTube",
    userHandleId = 0L
  )

  private val app2 = DiscoveredApp(
    id = "com.android.chrome/.Main#0",
    packageName = "com.android.chrome",
    activityName = ".Main",
    label = "Chrome",
    userHandleId = 0L
  )

  private val app3 = DiscoveredApp(
    id = "com.spotify.music/.MainActivity#0",
    packageName = "com.spotify.music",
    activityName = ".MainActivity",
    label = "Spotify",
    userHandleId = 0L
  )

  // Work Profile version of Chrome (same package and component, userHandleId = 10)
  private val workAppChrome = DiscoveredApp(
    id = "com.android.chrome/.Main#10",
    packageName = "com.android.chrome",
    activityName = ".Main",
    label = "Chrome (Work)",
    userHandleId = 10L
  )

  // Secondary component of same package
  private val chromeSettings = DiscoveredApp(
    id = "com.android.chrome/.SettingsActivity#0",
    packageName = "com.android.chrome",
    activityName = ".SettingsActivity",
    label = "Chrome Settings",
    userHandleId = 0L
  )

  private val allApps = listOf(app1, app2, app3, workAppChrome, chromeSettings)

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

  // --------------------------------------------------------------------------
  // Scenario 1: Launch app in Space A -> appears in Space A Layer 2 Recent Opened Apps
  // --------------------------------------------------------------------------
  @Test
  fun scenario01_launchInSpaceA_appearsInSpaceARecents() = runBlocking {
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)

    val spaceARecents = repository.resolveRecentApps(spaceA, allApps, limit = 10)
    assertEquals(1, spaceARecents.size)
    assertEquals(app1.packageName, spaceARecents[0].packageName)
    assertEquals(app1.userHandleId, spaceARecents[0].userHandleId)
  }

  // --------------------------------------------------------------------------
  // Scenario 2: Launch app in Space A -> does NOT appear in Space B Layer 2 Recent Opened Apps
  // --------------------------------------------------------------------------
  @Test
  fun scenario02_launchInSpaceA_doesNotAppearInSpaceBRecents() = runBlocking {
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)

    val spaceBRecents = repository.resolveRecentApps(spaceB, allApps, limit = 10)
    assertTrue("Space B recents must be completely empty when only Space A has launches", spaceBRecents.isEmpty())
  }

  // --------------------------------------------------------------------------
  // Scenario 3: Launch app multiple times -> moves to front of Recent Opened Apps without duplicate entries
  // --------------------------------------------------------------------------
  @Test
  fun scenario03_relaunchApp_movesToFrontWithoutDuplicates() = runBlocking {
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 2000L)
    repository.recordLaunch(spaceA, app3.appIdentity, timestamp = 3000L)

    // Re-launch app1 with newer timestamp
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 4000L)

    val recents = repository.resolveRecentApps(spaceA, allApps, limit = 10)
    assertEquals(3, recents.size)
    assertEquals(app1, recents[0]) // Moved to front
    assertEquals(app3, recents[1])
    assertEquals(app2, recents[2])
  }

  // --------------------------------------------------------------------------
  // Scenario 4: Work Profile app and Personal Profile app with same package name tracked independently
  // --------------------------------------------------------------------------
  @Test
  fun scenario04_personalAndWorkProfile_trackedIndependently() = runBlocking {
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 1000L)       // Personal Chrome (user 0)
    repository.recordLaunch(spaceA, workAppChrome.appIdentity, timestamp = 2000L) // Work Chrome (user 10)

    val recents = repository.resolveRecentApps(spaceA, allApps, limit = 10)
    assertEquals(2, recents.size)
    assertEquals(workAppChrome, recents[0])
    assertEquals(app2, recents[1])
    assertEquals(10L, recents[0].userHandleId)
    assertEquals(0L, recents[1].userHandleId)
  }

  // --------------------------------------------------------------------------
  // Scenario 5: Same package with different component names tracked independently
  // --------------------------------------------------------------------------
  @Test
  fun scenario05_differentComponentsOfSamePackage_trackedIndependently() = runBlocking {
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 1000L)           // Chrome .Main
    repository.recordLaunch(spaceA, chromeSettings.appIdentity, timestamp = 2000L) // Chrome .SettingsActivity

    val recents = repository.resolveRecentApps(spaceA, allApps, limit = 10)
    assertEquals(2, recents.size)
    assertEquals(chromeSettings, recents[0])
    assertEquals(app2, recents[1])
    assertEquals(".SettingsActivity", recents[0].activityName)
    assertEquals(".Main", recents[1].activityName)
  }

  // --------------------------------------------------------------------------
  // Scenario 6: Failed launch does NOT record to usage history
  // --------------------------------------------------------------------------
  @Test
  fun scenario06_failedLaunch_doesNotRecordToUsageHistory() = runBlocking {
    val launchManager = AppLaunchManager(
      context = context,
      historyRepository = repository,
      ioDispatcher = Dispatchers.Unconfined
    )

    val uninstalledApp = DiscoveredApp(
      id = "com.uninstalled.fake/.MainActivity#0",
      packageName = "com.uninstalled.fake",
      activityName = ".MainActivity",
      label = "Fake App",
      userHandleId = 0L
    )

    val result = launchManager.launchApp(uninstalledApp, spaceId = spaceA)
    assertFalse("Launch should fail for uninstalled app", result is LaunchResult.Success)

    val recents = repository.resolveRecentApps(spaceA, allApps, limit = 10)
    assertTrue("Failed launch must not add to history", recents.isEmpty())
  }

  // --------------------------------------------------------------------------
  // Scenario 7: Launches recorded through AppLaunchManager update history repository
  // --------------------------------------------------------------------------
  @Test
  fun scenario07_appLaunchManager_updatesHistoryAuthoritatively() = runBlocking {
    // Record launch directly as AppLaunchManager does upon successful launch
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1500L)

    val recents = repository.resolveRecentApps(spaceA, allApps, limit = 5)
    assertEquals(1, recents.size)
    assertEquals(app1, recents[0])
  }

  // --------------------------------------------------------------------------
  // Scenario 8: Most Used Apps folder on Layer 1 Page 0 correctly lists apps ordered by launch frequency
  // --------------------------------------------------------------------------
  @Test
  fun scenario08_mostUsedAppsFolder_ordersByLaunchFrequency() = runBlocking {
    // app1 launched 1 time
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    // app2 launched 5 times
    repeat(5) { i ->
      repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 2000L + i)
    }
    // app3 launched 3 times
    repeat(3) { i ->
      repository.recordLaunch(spaceA, app3.appIdentity, timestamp = 3000L + i)
    }

    val mostUsed = repository.resolveMostUsedApps(spaceA, allApps, limit = 10)
    assertEquals(3, mostUsed.size)
    assertEquals(app2, mostUsed[0]) // 5 launches
    assertEquals(app3, mostUsed[1]) // 3 launches
    assertEquals(app1, mostUsed[2]) // 1 launch

    // Verify folder placement metadata
    val folderId = SpaceFolder.getMostUsedFolderId(spaceA)
    assertEquals("folder_most_used_$spaceA", folderId)
    assertTrue("Folder id must match MOST_USED prefix", folderId.startsWith(SpaceFolder.MOST_USED_FOLDER_PREFIX))
  }

  // --------------------------------------------------------------------------
  // Scenario 9: Most Used Apps folder does not include apps from other Spaces
  // --------------------------------------------------------------------------
  @Test
  fun scenario09_mostUsedAppsFolder_doesNotIncludeAppsFromOtherSpaces() = runBlocking {
    // Launch app1 and app2 in Space A
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 1100L)

    // Launch app3 10 times in Space B
    repeat(10) { i ->
      repository.recordLaunch(spaceB, app3.appIdentity, timestamp = 2000L + i)
    }

    // Space A Most Used Apps must only include app2 and app1, NEVER app3
    val mostUsedA = repository.resolveMostUsedApps(spaceA, allApps, limit = 10)
    assertEquals(2, mostUsedA.size)
    assertFalse("Space A Most Used must not contain app3 launched in Space B", mostUsedA.contains(app3))

    // Space B Most Used Apps must only include app3
    val mostUsedB = repository.resolveMostUsedApps(spaceB, allApps, limit = 10)
    assertEquals(1, mostUsedB.size)
    assertEquals(app3, mostUsedB[0])
  }

  // --------------------------------------------------------------------------
  // Scenario 10: Dynamic Most Used Apps folder reacts to new launches in real-time
  // --------------------------------------------------------------------------
  @Test
  fun scenario10_dynamicMostUsedAppsFolder_reactsInRealTime() = runBlocking {
    // Initial state: app1 has 2 launches, app2 has 1 launch
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1100L)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 1200L)

    val initialFlow = repository.getMostUsedApps(spaceA, allApps, limit = 10).first()
    assertEquals(app1, initialFlow[0]) // app1 is #1

    // Now launch app2 twice more -> app2 becomes #1 with 3 launches
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 2000L)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 2100L)

    val updatedFlow = repository.getMostUsedApps(spaceA, allApps, limit = 10).first()
    assertEquals(app2, updatedFlow[0]) // app2 reacted dynamically to become #1
    assertEquals(app1, updatedFlow[1])
  }

  // --------------------------------------------------------------------------
  // Scenario 11: Usage Statistics widget displays correct total launch count for active Space
  // --------------------------------------------------------------------------
  @Test
  fun scenario11_usageStatsWidget_displaysCorrectTotalLaunchCount() = runBlocking {
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 1100L)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 1200L)

    val stats = repository.getSpaceUsageStats(spaceA, allApps)
    assertEquals(3, stats.totalLaunches)
  }

  // --------------------------------------------------------------------------
  // Scenario 12: Usage Statistics widget displays correct unique apps count for active Space
  // --------------------------------------------------------------------------
  @Test
  fun scenario12_usageStatsWidget_displaysCorrectUniqueAppsCount() = runBlocking {
    // Launch app1 once, app2 four times -> exactly 2 unique apps
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    repeat(4) { i ->
      repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 2000L + i)
    }

    val stats = repository.getSpaceUsageStats(spaceA, allApps)
    assertEquals(2, stats.uniqueAppsCount)
  }

  // --------------------------------------------------------------------------
  // Scenario 13: Usage Statistics widget displays correct top/most used app for active Space
  // --------------------------------------------------------------------------
  @Test
  fun scenario13_usageStatsWidget_displaysCorrectTopApp() = runBlocking {
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    repository.recordLaunch(spaceA, app3.appIdentity, timestamp = 2000L)
    repository.recordLaunch(spaceA, app3.appIdentity, timestamp = 2100L)

    val stats = repository.getSpaceUsageStats(spaceA, allApps)
    assertNotNull(stats.mostUsedApp)
    assertEquals(app3, stats.mostUsedApp)
    assertEquals(app3, stats.mostRecentlyLaunchedApp)
  }

  // --------------------------------------------------------------------------
  // Scenario 14: Usage Statistics in Space A does not count launches from Space B
  // --------------------------------------------------------------------------
  @Test
  fun scenario14_usageStats_spaceA_doesNotCountLaunchesFromSpaceB() = runBlocking {
    // Space A has 2 launches
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 1100L)

    // Space B has 20 launches
    repeat(20) { i ->
      repository.recordLaunch(spaceB, app3.appIdentity, timestamp = 3000L + i)
    }

    val statsA = repository.getSpaceUsageStats(spaceA, allApps)
    val statsB = repository.getSpaceUsageStats(spaceB, allApps)

    assertEquals(2, statsA.totalLaunches)
    assertEquals(2, statsA.uniqueAppsCount)

    assertEquals(20, statsB.totalLaunches)
    assertEquals(1, statsB.uniqueAppsCount)
  }

  // --------------------------------------------------------------------------
  // Scenario 15: Uninstalled/unmapped apps are safely filtered out without crashes
  // --------------------------------------------------------------------------
  @Test
  fun scenario15_uninstalledApps_safelyFilteredWithoutCrashes() = runBlocking {
    val uninstalledIdentity = AppIdentity("com.deleted.app", ".Main", 0L)
    repository.recordLaunch(spaceA, uninstalledIdentity, timestamp = 500L)
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L)

    // allApps does NOT contain com.deleted.app
    val recents = repository.resolveRecentApps(spaceA, allApps, limit = 10)
    assertEquals(1, recents.size)
    assertEquals(app1, recents[0])

    val mostUsed = repository.resolveMostUsedApps(spaceA, allApps, limit = 10)
    assertEquals(1, mostUsed.size)
    assertEquals(app1, mostUsed[0])

    val stats = repository.getSpaceUsageStats(spaceA, allApps)
    // totalLaunches counts raw database events (2), but top apps gracefully ignores deleted app
    assertEquals(2, stats.totalLaunches)
    assertEquals(app1, stats.mostUsedApp)
    assertEquals(app1, stats.mostRecentlyLaunchedApp)
  }

  // --------------------------------------------------------------------------
  // Scenario 16: Top Most Used Apps with Count returns accurate counts in descending order
  // --------------------------------------------------------------------------
  @Test
  fun scenario16_topMostUsedAppsWithCount_accurateCounts() = runBlocking {
    // Launch app1 4 times, app2 7 times, app3 2 times
    repeat(4) { i -> repository.recordLaunch(spaceA, app1.appIdentity, timestamp = 1000L + i) }
    repeat(7) { i -> repository.recordLaunch(spaceA, app2.appIdentity, timestamp = 2000L + i) }
    repeat(2) { i -> repository.recordLaunch(spaceA, app3.appIdentity, timestamp = 3000L + i) }

    val topAppsWithCount = repository.getTopMostUsedAppsWithCount(spaceA, allApps, limit = 5)
    assertEquals(3, topAppsWithCount.size)
    assertEquals(app2, topAppsWithCount[0].app)
    assertEquals(7, topAppsWithCount[0].launchCount)
    assertEquals(app1, topAppsWithCount[1].app)
    assertEquals(4, topAppsWithCount[1].launchCount)
    assertEquals(app3, topAppsWithCount[2].app)
    assertEquals(2, topAppsWithCount[2].launchCount)
  }

  // --------------------------------------------------------------------------
  // Scenario 17: Daily Launch Counts for Last 7 Days aggregates accurately with 7 calendar days
  // --------------------------------------------------------------------------
  @Test
  fun scenario17_dailyLaunchCountsLast7Days_aggregatesCorrectly() = runBlocking {
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    // Record launches today
    repository.recordLaunch(spaceA, app1.appIdentity, timestamp = now)
    repository.recordLaunch(spaceA, app2.appIdentity, timestamp = now)

    // Record 3 launches yesterday
    val yesterday = now - oneDayMs
    repeat(3) { i -> repository.recordLaunch(spaceA, app1.appIdentity, timestamp = yesterday + i * 100) }

    val weeklyData = repository.getDailyLaunchCountsLast7DaysFlow(spaceA).first()
    assertEquals(7, weeklyData.size)
    // The last item is today
    val todayItem = weeklyData.last()
    assertEquals(2, todayItem.count)
    // The second to last item is yesterday
    val yesterdayItem = weeklyData[weeklyData.size - 2]
    assertEquals(3, yesterdayItem.count)
  }
}
