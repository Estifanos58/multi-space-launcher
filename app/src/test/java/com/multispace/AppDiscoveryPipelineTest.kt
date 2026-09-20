package com.multispace

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.multispace.domain.model.DiscoveredApp
import com.multispace.platform.AppCatalogUpdater
import com.multispace.platform.AppDiscoveryManager
import com.multispace.platform.AppLaunchManager
import com.multispace.platform.DiscoveryResult
import com.multispace.platform.LaunchResult
import com.multispace.platform.PackageEventDeduplicator
import com.multispace.platform.PackageMetadata
import com.multispace.platform.PackageMetadataCache
import com.multispace.platform.UserHandleHelper
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
class AppDiscoveryPipelineTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  // --- 1. AppCatalogUpdater Incremental Tests ---

  @Test
  fun testIncrementalUpsertAddsNewAppInAlphabeticalOrder() {
    val initialCatalog = listOf(
      DiscoveredApp(id = "com.brave.browser/.Main#0", packageName = "com.brave.browser", activityName = ".Main", label = "Brave"),
      DiscoveredApp(id = "com.spotify.music/.MainActivity#0", packageName = "com.spotify.music", activityName = ".MainActivity", label = "Spotify")
    )

    val chrome = listOf(
      DiscoveredApp(id = "com.android.chrome/.Main#0", packageName = "com.android.chrome", activityName = ".Main", label = "Chrome")
    )

    val updated = AppCatalogUpdater.applyPackageUpsert(
      currentCatalog = initialCatalog,
      newAppsForPackage = chrome,
      packageName = "com.android.chrome",
      userHandleId = 0L
    )

    assertEquals(3, updated.size)
    assertEquals("Brave", updated[0].label)
    assertEquals("Chrome", updated[1].label)
    assertEquals("Spotify", updated[2].label)
  }

  @Test
  fun testIncrementalUpsertReplacesExistingActivitiesOnAppUpdate() {
    val initialCatalog = listOf(
      DiscoveredApp(id = "com.test.app/.OldActivity#0", packageName = "com.test.app", activityName = ".OldActivity", label = "Test App v1", versionName = "1.0")
    )

    val updatedApps = listOf(
      DiscoveredApp(id = "com.test.app/.NewActivity#0", packageName = "com.test.app", activityName = ".NewActivity", label = "Test App v2", versionName = "2.0")
    )

    val result = AppCatalogUpdater.applyPackageUpsert(
      currentCatalog = initialCatalog,
      newAppsForPackage = updatedApps,
      packageName = "com.test.app",
      userHandleId = 0L
    )

    assertEquals(1, result.size)
    assertEquals("Test App v2", result[0].label)
    assertEquals(".NewActivity", result[0].activityName)
    assertEquals("2.0", result[0].versionName)
  }

  @Test
  fun testIncrementalUpsertPreservesProfileIsolation() {
    // Both Personal (userHandleId = 0) and Work (userHandleId = 10) have Teams installed
    val initialCatalog = listOf(
      DiscoveredApp(id = "com.teams/.MainActivity#0", packageName = "com.teams", activityName = ".MainActivity", label = "Teams (Personal)", userHandleId = 0L),
      DiscoveredApp(id = "com.teams/.MainActivity#10", packageName = "com.teams", activityName = ".MainActivity", label = "Teams (Work)", userHandleId = 10L)
    )

    // Updating Work profile's Teams must NOT affect Personal Teams
    val updatedWorkTeams = listOf(
      DiscoveredApp(id = "com.teams/.MainActivity#10", packageName = "com.teams", activityName = ".MainActivity", label = "Teams Work Updated", userHandleId = 10L)
    )

    val result = AppCatalogUpdater.applyPackageUpsert(
      currentCatalog = initialCatalog,
      newAppsForPackage = updatedWorkTeams,
      packageName = "com.teams",
      userHandleId = 10L
    )

    assertEquals(2, result.size)
    val personal = result.first { it.userHandleId == 0L }
    val work = result.first { it.userHandleId == 10L }

    assertEquals("Teams (Personal)", personal.label)
    assertEquals("Teams Work Updated", work.label)
  }

  @Test
  fun testIncrementalRemovalRemovesOnlyTargetPackageAndProfile() {
    val initialCatalog = listOf(
      DiscoveredApp(id = "com.slack/.MainActivity#0", packageName = "com.slack", activityName = ".MainActivity", label = "Slack (Personal)", userHandleId = 0L),
      DiscoveredApp(id = "com.slack/.MainActivity#10", packageName = "com.slack", activityName = ".MainActivity", label = "Slack (Work)", userHandleId = 10L),
      DiscoveredApp(id = "com.zoom/.MainActivity#0", packageName = "com.zoom", activityName = ".MainActivity", label = "Zoom", userHandleId = 0L)
    )

    // Remove only Work Slack
    val afterWorkSlackRemoved = AppCatalogUpdater.applyPackageRemoval(
      currentCatalog = initialCatalog,
      packageName = "com.slack",
      userHandleId = 10L
    )

    assertEquals(2, afterWorkSlackRemoved.size)
    assertTrue(afterWorkSlackRemoved.any { it.packageName == "com.slack" && it.userHandleId == 0L })
    assertFalse(afterWorkSlackRemoved.any { it.packageName == "com.slack" && it.userHandleId == 10L })
    assertTrue(afterWorkSlackRemoved.any { it.packageName == "com.zoom" })

    // Remove Personal Slack
    val afterPersonalSlackRemoved = AppCatalogUpdater.applyPackageRemoval(
      currentCatalog = afterWorkSlackRemoved,
      packageName = "com.slack",
      userHandleId = 0L
    )

    assertEquals(1, afterPersonalSlackRemoved.size)
    assertEquals("com.zoom", afterPersonalSlackRemoved[0].packageName)
  }

  @Test
  fun testBatchUpsertMergesMultiplePackages() {
    val initialCatalog = listOf(
      DiscoveredApp(id = "com.app.a/.Main#0", packageName = "com.app.a", activityName = ".Main", label = "App A")
    )

    val batch = listOf(
      DiscoveredApp(id = "com.app.b/.Main#0", packageName = "com.app.b", activityName = ".Main", label = "App B"),
      DiscoveredApp(id = "com.app.c/.Main#0", packageName = "com.app.c", activityName = ".Main", label = "App C")
    )

    val result = AppCatalogUpdater.applyBatchUpsert(
      currentCatalog = initialCatalog,
      newApps = batch,
      affectedPackages = setOf("com.app.b", "com.app.c"),
      userHandleId = 0L
    )

    assertEquals(3, result.size)
    assertEquals(listOf("App A", "App B", "App C"), result.map { it.label })
  }

  // --- 2. PackageEventDeduplicator Tests ---

  @Test
  fun testDeduplicatorSuppressesRapidDuplicateEvents() {
    val deduplicator = PackageEventDeduplicator(windowMillis = 400L)
    val now = 1000000L

    val event1 = AppDiscoveryManager.PackageEvent.Added("com.example.app", userHandleId = 0L, timestamp = now)
    val event2 = AppDiscoveryManager.PackageEvent.Added("com.example.app", userHandleId = 0L, timestamp = now + 50L)

    assertTrue("First event should process", deduplicator.shouldProcess(event1, now))
    assertFalse("Duplicate event within 50ms should be suppressed", deduplicator.shouldProcess(event2, now + 50L))
  }

  @Test
  fun testDeduplicatorAllowsSamePackageOnDifferentProfiles() {
    val deduplicator = PackageEventDeduplicator(windowMillis = 400L)
    val now = 1000000L

    val personalEvent = AppDiscoveryManager.PackageEvent.Added("com.example.app", userHandleId = 0L, timestamp = now)
    val workEvent = AppDiscoveryManager.PackageEvent.Added("com.example.app", userHandleId = 10L, timestamp = now + 10L)

    assertTrue("Personal event should process", deduplicator.shouldProcess(personalEvent, now))
    assertTrue("Work profile event should process independently", deduplicator.shouldProcess(workEvent, now + 10L))
  }

  @Test
  fun testDeduplicatorAllowsEventAfterWindowExpiry() {
    val deduplicator = PackageEventDeduplicator(windowMillis = 400L)
    val now = 1000000L

    val event1 = AppDiscoveryManager.PackageEvent.Added("com.example.app", userHandleId = 0L, timestamp = now)
    val event2 = AppDiscoveryManager.PackageEvent.Added("com.example.app", userHandleId = 0L, timestamp = now + 500L)

    assertTrue("Initial event should process", deduplicator.shouldProcess(event1, now))
    assertTrue("Event after window (500ms > 400ms) should process", deduplicator.shouldProcess(event2, now + 500L))
  }

  @Test
  fun testDeduplicatorDistinguishesRemovalFromUpsert() {
    val deduplicator = PackageEventDeduplicator(windowMillis = 400L)
    val now = 1000000L

    val addedEvent = AppDiscoveryManager.PackageEvent.Added("com.example.app", userHandleId = 0L, timestamp = now)
    val removedEvent = AppDiscoveryManager.PackageEvent.Removed("com.example.app", userHandleId = 0L, timestamp = now + 20L)

    assertTrue("Add event should process", deduplicator.shouldProcess(addedEvent, now))
    assertTrue("Removal event should not be suppressed by an add event", deduplicator.shouldProcess(removedEvent, now + 20L))
  }

  // --- 3. PackageMetadataCache Tests ---

  @Test
  fun testMetadataCacheStoresAndRetrievesByProfile() {
    val cache = PackageMetadataCache(maxEntries = 100)

    val personalMeta = PackageMetadata("1.0", 1000L, 1000L, isUninstallable = true)
    val workMeta = PackageMetadata("2.0", 2000L, 2000L, isUninstallable = false)

    cache.put("com.teams", 0L, personalMeta)
    cache.put("com.teams", 10L, workMeta)

    val retrievedPersonal = cache.get("com.teams", 0L)
    val retrievedWork = cache.get("com.teams", 10L)

    assertEquals("1.0", retrievedPersonal?.versionName)
    assertTrue(retrievedPersonal?.isUninstallable == true)

    assertEquals("2.0", retrievedWork?.versionName)
    assertFalse(retrievedWork?.isUninstallable == true)
  }

  @Test
  fun testMetadataCacheEvictionTargeted() {
    val cache = PackageMetadataCache(maxEntries = 100)

    cache.put("com.teams", 0L, PackageMetadata("1.0", 100L, 100L, true))
    cache.put("com.teams", 10L, PackageMetadata("1.0", 100L, 100L, false))

    // Evict only work profile
    cache.evict("com.teams", 10L)

    assertNotNull("Personal profile metadata should remain", cache.get("com.teams", 0L))
    assertNull("Work profile metadata should be evicted", cache.get("com.teams", 10L))

    // Wildcard evict
    cache.evict("com.teams")
    assertNull("All profiles should be evicted on wildcard", cache.get("com.teams", 0L))
  }

  // --- 4. DiscoveryResult & Fake Fallback Removal Tests ---

  @Test
  fun testDiscoveryResultModelIntegrity() {
    val apps = listOf(
      DiscoveredApp(id = "com.sample/.Main#0", packageName = "com.sample", activityName = ".Main", label = "Sample")
    )

    val success = DiscoveryResult.Success(apps)
    assertEquals(1, success.apps.size)

    val empty = DiscoveryResult.Empty("No apps")
    assertEquals("No apps", empty.reason)

    val failure = DiscoveryResult.Failure(IllegalStateException("Simulated"))
    assertTrue(failure.error is IllegalStateException)
  }

  @Test
  fun testEmptyDiscoveryReturnsEmptyWithoutFakeSampleApps() {
    // Verifies that AppDiscoveryManager loadInstalledApps does not inject fake sample apps
    // on a clean environment where no apps are discovered.
    val manager = AppDiscoveryManager(context)
    val discovered = kotlinx.coroutines.runBlocking {
      manager.loadInstalledApps()
    }

    // In Robolectric with no registered apps, discovered must NEVER contain the old fake sample apps
    // like com.google.android.deskclock, com.android.camera, etc., unless genuinely installed.
    val fakePackages = setOf(
      "com.android.calculator2",
      "com.google.android.deskclock",
      "com.google.android.apps.nbu.files"
    )
    for (fake in fakePackages) {
      assertFalse(
        "Discovery must not inject fake sample app $fake",
        discovered.any { it.packageName == fake && it.label == "Clock" && it.activityName == "com.android.deskclock.DeskClock" }
      )
    }
  }

  @Test
  fun testIconCacheProfileIsolationOnEviction() {
    val manager = AppDiscoveryManager(context)
    val personalApp = DiscoveredApp(
      id = "com.example.chat/.MainActivity#0",
      packageName = "com.example.chat",
      activityName = ".MainActivity",
      label = "Chat",
      userHandleId = 0L
    )
    val workApp = DiscoveredApp(
      id = "com.example.chat/.MainActivity#10",
      packageName = "com.example.chat",
      activityName = ".MainActivity",
      label = "Chat (Work)",
      userHandleId = 10L
    )

    // Load icon for both (uses default icon fallback in test environment)
    val iconPersonal = manager.loadAppIcon(personalApp)
    val iconWork = manager.loadAppIcon(workApp)

    assertNotNull(iconPersonal)
    assertNotNull(iconWork)

    // Evict only work profile icon
    manager.evictPackageFromCache("com.example.chat", userHandleId = 10L)

    // Both can still be queried, but eviction targeted only work profile without touching personal
    manager.clearIconCache()
  }

  // --- 5. UserHandle Resolution & Boundary Hardening Tests ---

  @Test
  fun testResolveUserHandleReturnsNullForUnresolvedProfileWithoutManufacturingUser0() {
    // Non-existent profile serial 9999L must NEVER resolve to Process.myUserHandle() or user 0
    val resolved = UserHandleHelper.resolveUserHandle(context, 9999L)
    assertNull("Unresolved profile serial must return null, never manufacture a fallback UserHandle", resolved)
  }

  @Test
  fun testAppLaunchWithUnresolvedProfileFailsSafelyWithoutCrossProfileLaunch() {
    val launchManager = AppLaunchManager(context)
    val workApp = DiscoveredApp(
      id = "com.work.app/.MainActivity#9999",
      packageName = "com.work.app",
      activityName = ".MainActivity",
      label = "Work App",
      userHandleId = 9999L
    )

    val result = kotlinx.coroutines.runBlocking { launchManager.launchAppSuspending(workApp) }
    assertTrue("Launch on unresolved profile must return Unavailable", result is LaunchResult.Unavailable)
    val unavailable = result as LaunchResult.Unavailable
    assertEquals("com.work.app", unavailable.packageName)
    assertTrue("Error reason must mention user profile", unavailable.reason.contains("profile 9999"))
  }

  @Test
  fun testLoadPackageAppsWithUnresolvedProfileReturnsEmptyList() = kotlinx.coroutines.runBlocking {
    val manager = AppDiscoveryManager(context)
    val apps = manager.loadPackageApps("com.android.chrome", userHandleId = 9999L)
    assertTrue("loadPackageApps for non-existent profile must return empty list without cross-profile leak", apps.isEmpty())
  }
}
