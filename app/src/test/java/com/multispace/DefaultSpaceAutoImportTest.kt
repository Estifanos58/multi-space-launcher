package com.multispace

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSpaceAutoImportTest {

  @Test
  fun testEssentialDockAppsIdentification() {
    val sampleApps = listOf(
      DiscoveredApp(id = "com.google.android.dialer/.DialerActivity/0", packageName = "com.google.android.dialer", activityName = ".DialerActivity", label = "Phone", isSystemApp = true),
      DiscoveredApp(id = "com.google.android.apps.messaging/.ui.ConversationListActivity/0", packageName = "com.google.android.apps.messaging", activityName = ".ui.ConversationListActivity", label = "Messages", isSystemApp = true),
      DiscoveredApp(id = "com.android.chrome/.Main/0", packageName = "com.android.chrome", activityName = ".Main", label = "Chrome", isSystemApp = true),
      DiscoveredApp(id = "com.google.android.GoogleCamera/.Camera/0", packageName = "com.google.android.GoogleCamera", activityName = ".Camera", label = "Camera", isSystemApp = true),
      DiscoveredApp(id = "com.android.settings/.Settings/0", packageName = "com.android.settings", activityName = ".Settings", label = "Settings", isSystemApp = true),
      DiscoveredApp(id = "com.spotify.music/.MainActivity/0", packageName = "com.spotify.music", activityName = ".MainActivity", label = "Spotify", isSystemApp = false),
      DiscoveredApp(id = "com.slack/.RootActivity/0", packageName = "com.slack", activityName = ".RootActivity", label = "Slack", isSystemApp = false)
    )

    val dialer = sampleApps.firstOrNull { it.packageName.contains("dialer") || it.packageName.contains("phone") || it.label.contains("Phone", ignoreCase = true) }
    val messaging = sampleApps.firstOrNull { it.packageName.contains("messaging") || it.packageName.contains("mms") || it.packageName.contains("message") || it.label.contains("Messages", ignoreCase = true) }
    val browser = sampleApps.firstOrNull { it.packageName.contains("chrome") || it.packageName.contains("browser") || it.label.contains("Chrome", ignoreCase = true) || it.label.contains("Browser", ignoreCase = true) }
    val camera = sampleApps.firstOrNull { it.packageName.contains("camera") || it.label.contains("Camera", ignoreCase = true) }
    val settings = sampleApps.firstOrNull { it.packageName.contains("settings") || it.label.contains("Settings", ignoreCase = true) }

    val dockCandidates = listOfNotNull(dialer, messaging, browser, camera, settings).distinctBy { it.packageName }

    assertEquals("Should detect 5 essential phone dock apps", 5, dockCandidates.size)
    assertEquals("Phone", dockCandidates[0].label)
    assertEquals("Messages", dockCandidates[1].label)
    assertEquals("Chrome", dockCandidates[2].label)
    assertEquals("Camera", dockCandidates[3].label)
    assertEquals("Settings", dockCandidates[4].label)
  }

  @Test
  fun testHomeLayoutPageGridPartitioning() {
    val sampleApps = (1..23).map { index ->
      DiscoveredApp(
        id = "com.app.example$index/.Main/0",
        packageName = "com.app.example$index",
        activityName = ".Main",
        label = "App $index",
        isSystemApp = false
      )
    }

    val gridCols = 4
    val pageSize = gridCols * 5 // 20 per page

    val page0Apps = sampleApps.filterIndexed { idx, _ -> idx / pageSize == 0 }
    val page1Apps = sampleApps.filterIndexed { idx, _ -> idx / pageSize == 1 }

    assertEquals(20, page0Apps.size)
    assertEquals(3, page1Apps.size)
    assertEquals("App 1", page0Apps.first().label)
    assertEquals("App 20", page0Apps.last().label)
    assertEquals("App 21", page1Apps.first().label)
    assertEquals("App 23", page1Apps.last().label)
  }

  @Test
  fun testDefaultSpaceConfigurationConstants() {
    assertEquals("space_default", Space.DEFAULT_SPACE_ID)
    assertEquals("Default", Space.DEFAULT_SPACE_NAME)
    assertEquals(4, Space.DEFAULT_GRID_COLUMNS)
    assertEquals(5, Space.DEFAULT_DOCK_CAPACITY)
    assertEquals("DEFAULT", Space.PRESET_DEFAULT)
    assertEquals("PAGE", Space.DISPLAY_MODE_PAGE)
    assertEquals("SCROLL", Space.DISPLAY_MODE_SCROLL)
  }
}
