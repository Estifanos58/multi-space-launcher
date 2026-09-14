package com.multispace

import com.multispace.domain.model.DiscoveredApp
import com.multispace.platform.DefaultAppCapabilityResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAppInitializationTest {

  private val installedApps = listOf(
    DiscoveredApp(id = "com.brave.browser/.Main/0", packageName = "com.brave.browser", activityName = ".Main", label = "Brave Browser"),
    DiscoveredApp(id = "org.codeaurora.snapcam/.Camera/0", packageName = "org.codeaurora.snapcam", activityName = ".Camera", label = "Camera Pro"),
    DiscoveredApp(id = "com.android.dialer/.DialerActivity/0", packageName = "com.android.dialer", activityName = ".DialerActivity", label = "Phone"),
    DiscoveredApp(id = "org.thoughtcrime.securesms/.ConversationListActivity/0", packageName = "org.thoughtcrime.securesms", activityName = ".ConversationListActivity", label = "Messages"),
    DiscoveredApp(id = "com.android.contacts/.ContactsActivity/0", packageName = "com.android.contacts", activityName = ".ContactsActivity", label = "Contacts"),
    DiscoveredApp(id = "com.simplemobiletools.gallery/.MainActivity/0", packageName = "com.simplemobiletools.gallery", activityName = ".MainActivity", label = "Photos"),
    DiscoveredApp(id = "com.google.android.apps.nbu.files/.MainActivity/0", packageName = "com.google.android.apps.nbu.files", activityName = ".MainActivity", label = "Files"),
    DiscoveredApp(id = "com.google.android.deskclock/.DeskClock/0", packageName = "com.google.android.deskclock", activityName = ".DeskClock", label = "Clock"),
    DiscoveredApp(id = "com.google.android.calculator/.Calculator/0", packageName = "com.google.android.calculator", activityName = ".Calculator", label = "Calculator"),
    DiscoveredApp(id = "com.google.android.calendar/.AllInOneActivity/0", packageName = "com.google.android.calendar", activityName = ".AllInOneActivity", label = "Calendar"),
    DiscoveredApp(id = "com.google.android.apps.maps/.MapsActivity/0", packageName = "com.google.android.apps.maps", activityName = ".MapsActivity", label = "Maps"),
    DiscoveredApp(id = "com.spotify.music/.MainActivity/0", packageName = "com.spotify.music", activityName = ".MainActivity", label = "Spotify Music"),
    DiscoveredApp(id = "com.google.android.gm/.ConversationListActivityGmail/0", packageName = "com.google.android.gm", activityName = ".ConversationListActivityGmail", label = "Gmail"),
    DiscoveredApp(id = "com.google.android.keep/.activities.BrowseActivity/0", packageName = "com.google.android.keep", activityName = ".activities.BrowseActivity", label = "Notes Keep"),
    DiscoveredApp(id = "com.google.android.apps.docs/.MainActivity/0", packageName = "com.google.android.apps.docs", activityName = ".MainActivity", label = "Docs")
  )

  @Test
  fun testDockAppsOrderFirstFour() {
    // Dock initialized with up to 4 core apps: Browser -> Camera -> Phone -> Messages
    val dockApps = DefaultAppCapabilityResolver.resolveDockApps(
      installedApps = installedApps,
      dockCapacity = 4,
      context = null
    )

    assertEquals(4, dockApps.size)
    assertEquals("com.brave.browser", dockApps[0].packageName)
    assertEquals("org.codeaurora.snapcam", dockApps[1].packageName)
    assertEquals("com.android.dialer", dockApps[2].packageName)
    assertEquals("org.thoughtcrime.securesms", dockApps[3].packageName)
  }

  @Test
  fun testDockAppsSkipMissingCapability() {
    // If Phone is not installed, it should skip Phone and proceed to Messages, then next available
    val appsWithoutPhone = installedApps.filterNot { it.packageName == "com.android.dialer" }
    val dockApps = DefaultAppCapabilityResolver.resolveDockApps(
      installedApps = appsWithoutPhone,
      dockCapacity = 4,
      context = null
    )

    assertEquals(4, dockApps.size)
    assertEquals("com.brave.browser", dockApps[0].packageName)
    assertEquals("org.codeaurora.snapcam", dockApps[1].packageName)
    assertEquals("org.thoughtcrime.securesms", dockApps[2].packageName)
    // Next available candidate fills the 4th slot (Contacts)
    assertEquals("com.android.contacts", dockApps[3].packageName)
  }

  @Test
  fun testDockAppsExpandedCapacity() {
    // If capacity > 4 (e.g. 7), first 4 are core, followed by:
    // Contacts -> Gallery/Photos -> Files -> Clock -> Calculator -> Calendar -> Maps
    val dockApps = DefaultAppCapabilityResolver.resolveDockApps(
      installedApps = installedApps,
      dockCapacity = 7,
      context = null
    )

    assertEquals(7, dockApps.size)
    assertEquals("com.brave.browser", dockApps[0].packageName)
    assertEquals("org.codeaurora.snapcam", dockApps[1].packageName)
    assertEquals("com.android.dialer", dockApps[2].packageName)
    assertEquals("org.thoughtcrime.securesms", dockApps[3].packageName)
    assertEquals("com.android.contacts", dockApps[4].packageName)
    assertEquals("com.simplemobiletools.gallery", dockApps[5].packageName)
    assertEquals("com.google.android.apps.nbu.files", dockApps[6].packageName)
  }

  @Test
  fun testDockAppsPreservesExistingWhenCapacityIncreased() {
    // When user already has custom apps in dock, increasing capacity preserves them first
    val existingDock = listOf(
      DiscoveredApp(id = "app1", packageName = "com.spotify.music", activityName = ".Main", label = "Spotify"),
      DiscoveredApp(id = "app2", packageName = "com.brave.browser", activityName = ".Main", label = "Brave")
    )

    val expandedDock = DefaultAppCapabilityResolver.resolveDockApps(
      installedApps = installedApps,
      dockCapacity = 5,
      context = null,
      existingDockApps = existingDock
    )

    assertEquals(5, expandedDock.size)
    assertEquals("com.spotify.music", expandedDock[0].packageName)
    assertEquals("com.brave.browser", expandedDock[1].packageName)
    // Remaining slots filled with unselected candidates
    assertFalse(expandedDock.drop(2).any { it.packageName == "com.spotify.music" || it.packageName == "com.brave.browser" })
  }

  @Test
  fun testLayer1AppsCuratedSizeBetween8And12() {
    val personalApps = DefaultAppCapabilityResolver.resolveLayer1Apps(
      installedApps = installedApps,
      spaceName = "Personal Life",
      layoutPreset = "PERSONAL",
      context = null
    )
    assertTrue("Personal space Layer 1 apps count should be 8-12, actual=${personalApps.size}", personalApps.size in 8..12)

    val workApps = DefaultAppCapabilityResolver.resolveLayer1Apps(
      installedApps = installedApps,
      spaceName = "Office Space",
      layoutPreset = "WORK",
      context = null
    )
    assertTrue("Work space Layer 1 apps count should be 8-12, actual=${workApps.size}", workApps.size in 8..12)

    val studyApps = DefaultAppCapabilityResolver.resolveLayer1Apps(
      installedApps = installedApps,
      spaceName = "College Study",
      layoutPreset = "STUDY",
      context = null
    )
    assertTrue("Study space Layer 1 apps count should be 8-12, actual=${studyApps.size}", studyApps.size in 8..12)
  }

  @Test
  fun testWorkSpaceCuratesWorkRelatedApps() {
    val workApps = DefaultAppCapabilityResolver.resolveLayer1Apps(
      installedApps = installedApps,
      spaceName = "Work",
      layoutPreset = "WORK",
      context = null
    )
    val packages = workApps.map { it.packageName }
    assertTrue("Work space should prioritize Email", packages.contains("com.google.android.gm"))
    assertTrue("Work space should prioritize Calendar", packages.contains("com.google.android.calendar"))
    assertTrue("Work space should prioritize Docs/Files", packages.contains("com.google.android.apps.docs") || packages.contains("com.google.android.apps.nbu.files"))
  }

  @Test
  fun testStudySpaceCuratesStudyRelatedApps() {
    val studyApps = DefaultAppCapabilityResolver.resolveLayer1Apps(
      installedApps = installedApps,
      spaceName = "Study",
      layoutPreset = "STUDY",
      context = null
    )
    val packages = studyApps.map { it.packageName }
    assertTrue("Study space should prioritize Notes", packages.contains("com.google.android.keep"))
    assertTrue("Study space should prioritize Calculator", packages.contains("com.google.android.calculator"))
    assertTrue("Study space should prioritize Calendar", packages.contains("com.google.android.calendar"))
  }

  @Test
  fun testPersonalSpaceCuratesPersonalApps() {
    val personalApps = DefaultAppCapabilityResolver.resolveLayer1Apps(
      installedApps = installedApps,
      spaceName = "Personal",
      layoutPreset = "PERSONAL",
      context = null
    )
    val packages = personalApps.map { it.packageName }
    assertTrue("Personal space should prioritize Photos/Gallery", packages.contains("com.simplemobiletools.gallery"))
    assertTrue("Personal space should prioritize Music/Media", packages.contains("com.spotify.music"))
  }

  @Test
  fun testNoDuplicateAppsInCuratedLayer1() {
    val layer1 = DefaultAppCapabilityResolver.resolveLayer1Apps(
      installedApps = installedApps,
      spaceName = "Daily",
      layoutPreset = "DEFAULT",
      context = null
    )
    val distinctCount = layer1.distinctBy { it.packageName }.size
    assertEquals("Layer 1 apps should contain no duplicates", distinctCount, layer1.size)
  }

  @Test
  fun testGalaxyCuratedPlacesAppsAtBottomRow() {
    val preset = com.multispace.domain.model.LayoutPreset.getById(com.multispace.domain.model.Space.PRESET_DEFAULT)
    val result = com.multispace.domain.model.PresetLayoutHelper.buildInitialLayout(
      spaceId = "test_space",
      preset = preset,
      gridColumns = 4,
      availableApps = installedApps,
      dockCapacity = 4
    )

    val page0Apps = result.placements.filter { it.pageIndex == 0 && it.itemType == com.multispace.domain.model.SpaceItemPlacement.ITEM_TYPE_APP }
    val page0Widgets = result.placements.filter { it.pageIndex == 0 && it.itemType == com.multispace.domain.model.SpaceItemPlacement.ITEM_TYPE_WIDGET }

    assertEquals("Page 0 should have exactly cols apps by default", 4, page0Apps.size)
    page0Apps.forEach { appPlacement ->
      val row = appPlacement.positionIndex / 4
      assertEquals("Page 0 apps in Galaxy Curated must be placed at the bottom row (row 4)", 4, row)
    }

    // Verify no app is laid on a widget
    val widgetSlots = mutableSetOf<Int>()
    page0Widgets.forEach { w ->
      val r = w.positionIndex / 4
      val c = w.positionIndex % 4
      for (dr in 0 until w.spanY) {
        for (dc in 0 until w.spanX) {
          widgetSlots.add((r + dr) * 4 + (c + dc))
        }
      }
    }
    page0Apps.forEach { app ->
      assertFalse("No app may be placed on a widget slot", widgetSlots.contains(app.positionIndex))
    }
  }

  @Test
  fun testClassicGridPlacesAppsToBottomRows() {
    val preset = com.multispace.domain.model.LayoutPreset.getById(com.multispace.domain.model.Space.PRESET_CLASSIC)
    val result = com.multispace.domain.model.PresetLayoutHelper.buildInitialLayout(
      spaceId = "test_space",
      preset = preset,
      gridColumns = 4,
      availableApps = installedApps,
      dockCapacity = 4
    )

    val page0Apps = result.placements.filter { it.pageIndex == 0 && it.itemType == com.multispace.domain.model.SpaceItemPlacement.ITEM_TYPE_APP }
    assertEquals("Page 0 should have exactly cols apps by default", 4, page0Apps.size)
    page0Apps.forEach { appPlacement ->
      val row = appPlacement.positionIndex / 4
      assertEquals("All Page 0 apps in Classic Grid must be placed on the last row (row 4)", 4, row)
    }
  }

  @Test
  fun testAllPresetsDefaultPage0AppsEqualToColumnsAndNeverOverlapsWidgets() {
    val colsList = listOf(3, 4, 5)
    val allPresets = listOf(
      com.multispace.domain.model.Space.PRESET_DEFAULT,
      com.multispace.domain.model.Space.PRESET_PIXEL,
      com.multispace.domain.model.Space.PRESET_CLASSIC,
      com.multispace.domain.model.Space.PRESET_MINIMAL,
      com.multispace.domain.model.Space.PRESET_PRODUCTIVITY,
      com.multispace.domain.model.Space.PRESET_COMPACT
    )

    for (presetId in allPresets) {
      val preset = com.multispace.domain.model.LayoutPreset.getById(presetId)
      for (cols in colsList) {
        val result = com.multispace.domain.model.PresetLayoutHelper.buildInitialLayout(
          spaceId = "test_space",
          preset = preset,
          gridColumns = cols,
          availableApps = installedApps,
          dockCapacity = cols
        )

        val page0Apps = result.placements.filter { it.pageIndex == 0 && it.itemType == com.multispace.domain.model.SpaceItemPlacement.ITEM_TYPE_APP }
        val page0Widgets = result.placements.filter { it.pageIndex == 0 && it.itemType == com.multispace.domain.model.SpaceItemPlacement.ITEM_TYPE_WIDGET }

        assertEquals("Preset $presetId with $cols cols must have $cols apps on Page 0", cols, page0Apps.size)
        page0Apps.forEach { appPlacement ->
          val row = appPlacement.positionIndex / cols
          assertEquals("Preset $presetId apps on Page 0 must be placed at the bottom row (row 4)", 4, row)
        }

        val widgetSlots = mutableSetOf<Int>()
        page0Widgets.forEach { w ->
          val r = w.positionIndex / cols
          val c = w.positionIndex % cols
          for (dr in 0 until w.spanY) {
            for (dc in 0 until w.spanX) {
              widgetSlots.add((r + dr) * cols + (c + dc))
            }
          }
        }
        page0Apps.forEach { app ->
          assertFalse("Preset $presetId: no app may be placed on a widget slot", widgetSlots.contains(app.positionIndex))
        }
      }
    }
  }
}
