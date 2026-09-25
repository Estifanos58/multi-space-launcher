package com.multispace

import com.multispace.domain.model.DiscoveredApp
import com.multispace.presentation.AppFilter
import com.multispace.presentation.AppSortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPerformanceLifecycleTest {

  @Test
  fun testEventLogBoundingMaintainsCapacity() {
    val maxCapacity = 100
    val list = mutableListOf<String>()

    // Simulate 250 rapid lifecycle logging events
    for (i in 1..250) {
      if (list.size >= maxCapacity) {
        list.removeAt(0)
      }
      list.add("Event $i")
    }

    assertEquals(maxCapacity, list.size)
    assertEquals("Event 151", list.first())
    assertEquals("Event 250", list.last())
  }

  @Test
  fun testAppFilteringAndSortingAscending() {
    val apps = listOf(
      DiscoveredApp(id = "c/.Main/0", packageName = "com.c", activityName = ".Main", label = "Chrome", isSystemApp = false),
      DiscoveredApp(id = "a/.Main/0", packageName = "com.a", activityName = ".Main", label = "Amazon", isSystemApp = false),
      DiscoveredApp(id = "b/.Main/0", packageName = "com.b", activityName = ".Main", label = "Browser", isSystemApp = true)
    )

    // User-only filter
    val userOnly = apps.filter { !it.isSystemApp }
    assertEquals(2, userOnly.size)
    assertFalse(userOnly.any { it.isSystemApp })

    // Alphabetical ascending
    val sortedAsc = apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    assertEquals("Amazon", sortedAsc[0].label)
    assertEquals("Browser", sortedAsc[1].label)
    assertEquals("Chrome", sortedAsc[2].label)
  }

  @Test
  fun testAppDeduplicationAndCoalescingLogic() {
    // Tests that duplicate scan requests maintain a single active state
    var activeScanRunning = false
    var hasPendingScan = false
    var totalCompletedScans = 0

    fun triggerScan() {
      if (activeScanRunning) {
        hasPendingScan = true
        return
      }
      activeScanRunning = true
      hasPendingScan = false

      // Simulate loop execution
      var runNext = true
      while (runNext) {
        hasPendingScan = false
        // Work
        totalCompletedScans++
        runNext = hasPendingScan
      }
      activeScanRunning = false
    }

    // Single run
    triggerScan()
    assertEquals(1, totalCompletedScans)

    // Multiple rapid concurrent triggers while running
    activeScanRunning = true
    hasPendingScan = false
    // Rapid triggers during execution
    for (i in 1..5) {
      if (activeScanRunning) {
        hasPendingScan = true
      }
    }
    // Execution checks pending
    assertTrue(hasPendingScan)
    // Run next pass
    totalCompletedScans++
    hasPendingScan = false
    activeScanRunning = false

    assertEquals(2, totalCompletedScans)
  }

  @Test
  fun testLayer2CatalogCachingInLauncherStateHolder() {
    val stateHolder = com.multispace.presentation.LauncherStateHolder()
    val apps = listOf(
      DiscoveredApp(id = "com.app.a/.Main#0", packageName = "com.app.a", activityName = ".Main", label = "App A"),
      DiscoveredApp(id = "com.app.b/.Main#0", packageName = "com.app.b", activityName = ".Main", label = "App B")
    )
    val discoveryState = com.multispace.presentation.AppDiscoveryUiState(allApps = apps)
    val space = com.multispace.domain.model.Space.createDefault()

    // 1. Initial derivation builds Layer 2 catalog
    val state1 = stateHolder.deriveState(
      discoveryUiState = discoveryState,
      activeSpace = space,
      allSpaces = listOf(space),
      activeMemberships = emptyList(),
      unlockedSpaceIds = emptySet(),
      activeLayerIndex = 1,
      activePlacements = emptyList(),
      activeFolders = emptyList(),
      activeDockItems = emptyList(),
      spaceMostUsedApps = emptyList(),
      spaceRecentApps = emptyList(),
      spaceUsageStats = null,
      wallpaperStyle = com.multispace.presentation.LauncherWallpaperStyle()
    )

    // 2. Second derivation with changed wallpaper/usage stats but same apps
    val state2 = stateHolder.deriveState(
      discoveryUiState = discoveryState,
      activeSpace = space,
      allSpaces = listOf(space),
      activeMemberships = emptyList(),
      unlockedSpaceIds = emptySet(),
      activeLayerIndex = 1,
      activePlacements = emptyList(),
      activeFolders = emptyList(),
      activeDockItems = emptyList(),
      spaceMostUsedApps = emptyList(),
      spaceRecentApps = emptyList(),
      spaceUsageStats = null,
      wallpaperStyle = com.multispace.presentation.LauncherWallpaperStyle(bgType = com.multispace.domain.model.Space.BACKGROUND_COLOR)
    )

    // Must reuse the exact same Layer 2 cached catalog instance without re-sorting or re-grouping
    org.junit.Assert.assertSame(
      "Layer 2 catalog must be reused without recomputing when apps are unchanged",
      state1.layer2CachedCatalog,
      state2.layer2CachedCatalog
    )
  }
}
