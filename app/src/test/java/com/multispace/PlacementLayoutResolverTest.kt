package com.multispace

import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.PlacementLayoutResolver
import com.multispace.domain.model.SpaceItemPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementLayoutResolverTest {

  private val testSpaceId = "space_work"
  private val cols = 4
  private val gridRows = 5
  private val pageSize = cols * gridRows

  private fun createApp(id: String, pkg: String, label: String = pkg): DiscoveredApp {
    return DiscoveredApp(
      id = id,
      packageName = pkg,
      activityName = "$pkg.MainActivity",
      label = label,
      userHandleId = 0
    )
  }

  @Test
  fun testFallbackPlacement_whenNoPlacementsInRoom_placesColsOnPage0BottomRowAndOverflowOnPage1() {
    val apps = (0 until 10).map { i -> createApp("app_$i", "com.test.app$i") }

    val layout = PlacementLayoutResolver.resolveEffectivePlacements(
      placements = emptyList(),
      allApps = apps,
      spaceId = testSpaceId,
      cols = cols,
      gridRows = gridRows,
      pageSize = pageSize
    )

    assertEquals(10, layout.effectivePlacements.size)

    // Page 0 should have exactly `cols` (4) apps on the bottom row (row 4 -> slots 16, 17, 18, 19)
    val page0Placements = layout.placementsForPage(0)
    assertEquals(cols, page0Placements.size)
    val expectedPage0Slots = setOf(16, 17, 18, 19)
    assertEquals(expectedPage0Slots, page0Placements.map { it.positionIndex }.toSet())

    // Page 1 should contain the remaining 6 apps starting from slot 0
    val page1Placements = layout.placementsForPage(1)
    assertEquals(6, page1Placements.size)
    assertEquals((0 until 6).toList(), page1Placements.map { it.positionIndex }.sorted())

    assertEquals(1, layout.maxPageIndex)
  }

  @Test
  fun testDeduplication_removesDuplicateAppsByAppIdentity() {
    val p1 = SpaceItemPlacement(
      id = "p1",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.test.app1",
      componentName = "com.test.app1.MainActivity",
      userHandleId = 0
    )
    val p2Duplicate = SpaceItemPlacement(
      id = "p2",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.test.app1", // Same identity
      componentName = "com.test.app1.MainActivity",
      userHandleId = 0
    )
    val p3Unique = SpaceItemPlacement(
      id = "p3",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.test.app2",
      componentName = "com.test.app2.MainActivity",
      userHandleId = 0
    )

    val layout = PlacementLayoutResolver.resolveEffectivePlacements(
      placements = listOf(p1, p2Duplicate, p3Unique),
      allApps = emptyList(),
      spaceId = testSpaceId,
      cols = cols,
      gridRows = gridRows,
      pageSize = pageSize
    )

    assertEquals(2, layout.effectivePlacements.size)
    assertEquals(listOf("p1", "p3"), layout.effectivePlacements.map { it.id })
  }

  @Test
  fun testPreservesWidgetsAndFoldersIntact() {
    val widget = SpaceItemPlacement(
      id = "w1",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      appWidgetId = 42
    )
    val folder = SpaceItemPlacement(
      id = "f1",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 3,
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = "folder_1"
    )

    val layout = PlacementLayoutResolver.resolveEffectivePlacements(
      placements = listOf(widget, folder),
      allApps = emptyList(),
      spaceId = testSpaceId,
      cols = cols,
      gridRows = gridRows,
      pageSize = pageSize
    )

    assertEquals(2, layout.effectivePlacements.size)
    assertTrue(layout.effectivePlacements.any { it.id == "w1" && it.isWidget })
    assertTrue(layout.effectivePlacements.any { it.id == "f1" && it.isFolder })

    // Widget at slot 0 (2x2) occupies slots 0, 1, 4, 5
    assertTrue(layout.isSlotCoveredByWidget(0, 0))
    assertTrue(layout.isSlotCoveredByWidget(0, 1))
    assertTrue(layout.isSlotCoveredByWidget(0, 4))
    assertTrue(layout.isSlotCoveredByWidget(0, 5))
    assertFalse(layout.isSlotCoveredByWidget(0, 2))
    assertFalse(layout.isSlotCoveredByWidget(0, 3))
  }

  @Test
  fun testRelocatesApp_whenCollidingWithWidget() {
    // 2x2 widget at slot 0 (covers 0, 1, 4, 5)
    val widget = SpaceItemPlacement(
      id = "w1",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      appWidgetId = 100
    )
    // App attempting to sit on slot 1 (covered by the widget!)
    val appOnWidget = SpaceItemPlacement(
      id = "p_colliding",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.test.colliding",
      userHandleId = 0
    )

    val layout = PlacementLayoutResolver.resolveEffectivePlacements(
      placements = listOf(widget, appOnWidget),
      allApps = emptyList(),
      spaceId = testSpaceId,
      cols = cols,
      gridRows = gridRows,
      pageSize = pageSize
    )

    val resolvedApp = layout.effectivePlacements.first { it.id == "p_colliding" }

    // CRITICAL: Must not sit on any widget footprint slots (0, 1, 4, 5)
    assertFalse(setOf(0, 1, 4, 5).contains(resolvedApp.positionIndex))
    // On Page 0, fallback relocation prefers bottom row (slots 16..19)
    assertTrue("Should be placed on page 0 bottom row", resolvedApp.pageIndex == 0 && resolvedApp.positionIndex in 16..19)
  }

  @Test
  fun testIsCandidateOverWidget_evaluatesFootprint() {
    val widget = SpaceItemPlacement(
      id = "w1",
      spaceId = testSpaceId,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 5, // row 1, col 1
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2
    )

    val layout = PlacementLayoutResolver.resolveEffectivePlacements(
      placements = listOf(widget),
      allApps = emptyList(),
      spaceId = testSpaceId,
      cols = cols,
      gridRows = gridRows,
      pageSize = pageSize
    )

    // A 1x1 app at slot 5, 6, 9, 10 touches the widget
    assertTrue(layout.isCandidateOverWidget(targetPage = 0, candidateSlot = 5, spanX = 1, spanY = 1, cols = cols, gridRows = gridRows))
    assertTrue(layout.isCandidateOverWidget(targetPage = 0, candidateSlot = 6, spanX = 1, spanY = 1, cols = cols, gridRows = gridRows))
    assertTrue(layout.isCandidateOverWidget(targetPage = 0, candidateSlot = 9, spanX = 1, spanY = 1, cols = cols, gridRows = gridRows))
    assertTrue(layout.isCandidateOverWidget(targetPage = 0, candidateSlot = 10, spanX = 1, spanY = 1, cols = cols, gridRows = gridRows))

    // Slot 0 is completely clear
    assertFalse(layout.isCandidateOverWidget(targetPage = 0, candidateSlot = 0, spanX = 1, spanY = 1, cols = cols, gridRows = gridRows))
    // Another page is clear
    assertFalse(layout.isCandidateOverWidget(targetPage = 1, candidateSlot = 5, spanX = 1, spanY = 1, cols = cols, gridRows = gridRows))
  }
}
