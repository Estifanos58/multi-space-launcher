package com.multispace

import com.multispace.domain.model.PlacementCascadeHelper
import com.multispace.domain.model.SpaceItemPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementCascadeTest {

  private val pageSize = 4 // 4 items per page for clean test verification

  @Test
  fun testDragStartRemovesAppFromOriginalPosition() {
    // 3 apps on page 0
    val placements = listOf(
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app1"),
      SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app2"),
      SpaceItemPlacement(id = "app3", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.app3")
    )

    val draggedItem = placements[1] // app2 at position 1
    // While dragging, otherPlacements excludes the dragged item
    val remainingPlacements = placements.filter { it.id != draggedItem.id }

    assertEquals(2, remainingPlacements.size)
    assertFalse(remainingPlacements.any { it.id == "app2" })
    assertFalse(remainingPlacements.any { it.positionIndex == 1 }) // original position is empty!
  }

  @Test
  fun testDropOnEmptySlotDoesNotShiftOtherApps() {
    val existing = listOf(
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app1"),
      SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app2")
    )
    val itemToInsert = SpaceItemPlacement(id = "app3", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app3")

    // Drop at empty slot (pos = 3)
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = itemToInsert,
      targetPage = 0,
      targetPosition = 3,
      pageSize = pageSize
    )

    val map = result.associateBy { it.id }
    assertEquals(0, map["app1"]?.positionIndex)
    assertEquals(1, map["app2"]?.positionIndex)
    assertEquals(3, map["app3"]?.positionIndex)
  }

  @Test
  fun testDropOnOccupiedSlotShiftsOccupyingAppToNextPosition() {
    val existing = listOf(
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app1"),
      SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app2")
    )
    val dragged = SpaceItemPlacement(id = "dragged", spaceId = "s1", pageIndex = 0, positionIndex = 3, packageName = "com.dragged")

    // Drop dragged item at pos = 1 (where app2 is)
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = dragged,
      targetPage = 0,
      targetPosition = 1,
      pageSize = pageSize
    )

    val map = result.associateBy { it.id }
    assertEquals(0, map["app1"]?.positionIndex) // untouched
    assertEquals(1, map["dragged"]?.positionIndex) // placed at target slot
    assertEquals(2, map["app2"]?.positionIndex) // app2 shifted 1 -> 2
  }

  @Test
  fun testCascadingShiftThroughConsecutiveOccupiedSlots() {
    // Page 0: pos 0=app0, pos 1=app1, pos 2=app2, pos 3 is empty
    val existing = listOf(
      SpaceItemPlacement(id = "app0", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.0"),
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.1"),
      SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.2")
    )
    val newItem = SpaceItemPlacement(id = "new", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.new")

    // Insert at pos 1: app1 moves to 2, app2 moves to 3 (which is empty)
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = newItem,
      targetPage = 0,
      targetPosition = 1,
      pageSize = pageSize
    )

    val map = result.associateBy { it.id }
    assertEquals(0, map["app0"]?.positionIndex)
    assertEquals(1, map["new"]?.positionIndex)
    assertEquals(2, map["app1"]?.positionIndex) // shifted 1 -> 2
    assertEquals(3, map["app2"]?.positionIndex) // shifted 2 -> 3
  }

  @Test
  fun testPageOverflowCascadesToNextPagePositionZero() {
    // Page 0 is full: slots 0, 1, 2, 3 are occupied (pageSize = 4)
    // Page 1 is empty
    val existing = listOf(
      SpaceItemPlacement(id = "app0", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.0"),
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.1"),
      SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.2"),
      SpaceItemPlacement(id = "app3", spaceId = "s1", pageIndex = 0, positionIndex = 3, packageName = "com.3")
    )
    val newItem = SpaceItemPlacement(id = "new", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.new")

    // Insert at Page 0, Pos 1
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = newItem,
      targetPage = 0,
      targetPosition = 1,
      pageSize = pageSize
    )

    val map = result.associateBy { it.id }
    assertEquals(0, map["app0"]?.pageIndex)
    assertEquals(0, map["app0"]?.positionIndex)

    assertEquals(0, map["new"]?.pageIndex)
    assertEquals(1, map["new"]?.positionIndex)

    assertEquals(0, map["app1"]?.pageIndex)
    assertEquals(2, map["app1"]?.positionIndex)

    assertEquals(0, map["app2"]?.pageIndex)
    assertEquals(3, map["app2"]?.positionIndex)

    // app3 overflows Page 0 (pos 4 >= 4) -> cascades to Page 1, position 0!
    assertEquals(1, map["app3"]?.pageIndex)
    assertEquals(0, map["app3"]?.positionIndex)
  }

  @Test
  fun testMultiPageCascadeIteratesUntilEmptySpaceIsFound() {
    // Page 0 is full (slots 0..3)
    // Page 1 has slot 0 occupied (pos 0) and slot 1 empty
    val existing = listOf(
      SpaceItemPlacement(id = "p0_0", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.00"),
      SpaceItemPlacement(id = "p0_1", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.01"),
      SpaceItemPlacement(id = "p0_2", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.02"),
      SpaceItemPlacement(id = "p0_3", spaceId = "s1", pageIndex = 0, positionIndex = 3, packageName = "com.03"),
      SpaceItemPlacement(id = "p1_0", spaceId = "s1", pageIndex = 1, positionIndex = 0, packageName = "com.10")
    )
    val newItem = SpaceItemPlacement(id = "new", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.new")

    // Insert at Page 0, Pos 2
    // p0_2 -> Page 0, Pos 3
    // p0_3 -> overflows to Page 1, Pos 0!
    // p1_0 at Page 1, Pos 0 -> cascades to Page 1, Pos 1 (which is empty)!
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = newItem,
      targetPage = 0,
      targetPosition = 2,
      pageSize = pageSize
    )

    val map = result.associateBy { it.id }
    assertEquals(0, map["p0_0"]?.pageIndex)
    assertEquals(0, map["p0_0"]?.positionIndex)

    assertEquals(0, map["p0_1"]?.pageIndex)
    assertEquals(1, map["p0_1"]?.positionIndex)

    assertEquals(0, map["new"]?.pageIndex)
    assertEquals(2, map["new"]?.positionIndex)

    assertEquals(0, map["p0_2"]?.pageIndex)
    assertEquals(3, map["p0_2"]?.positionIndex)

    // p0_3 shifted into Page 1, Pos 0
    assertEquals(1, map["p0_3"]?.pageIndex)
    assertEquals(0, map["p0_3"]?.positionIndex)

    // p1_0 shifted into Page 1, Pos 1
    assertEquals(1, map["p1_0"]?.pageIndex)
    assertEquals(1, map["p1_0"]?.positionIndex)
  }

  @Test
  fun testDropBackOntoOwnSlotDoesNotShift() {
    val existing = listOf(
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.1"),
      SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.2"),
      SpaceItemPlacement(id = "app3", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.3")
    )
    val dragged = existing[1] // app2 at pos 1

    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = dragged,
      targetPage = 0,
      targetPosition = 1,
      pageSize = pageSize
    )

    val map = result.associateBy { it.id }
    assertEquals(0, map["app1"]?.positionIndex)
    assertEquals(1, map["app2"]?.positionIndex)
    assertEquals(2, map["app3"]?.positionIndex)
  }

  @Test
  fun testNoDuplicateAppCreatedOnDropWhenPackageMatches() {
    // Existing list has app2 at position 1
    val existing = listOf(
      SpaceItemPlacement(id = "p1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.test.app1"),
      SpaceItemPlacement(id = "p2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.test.app2"),
      SpaceItemPlacement(id = "p3", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.test.app3")
    )
    // Virtual or dragged item for com.test.app2 with different ID
    val dragged = SpaceItemPlacement(
      id = "virtual:com.test.app2",
      spaceId = "s1",
      pageIndex = 0,
      positionIndex = 1,
      packageName = "com.test.app2"
    )

    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = dragged,
      targetPage = 0,
      targetPosition = 3,
      pageSize = pageSize
    )

    // Result must have exactly ONE item with packageName "com.test.app2"
    val app2Placements = result.filter { it.packageName == "com.test.app2" }
    assertEquals(1, app2Placements.size)
    // Placed at target position 3
    assertEquals(3, app2Placements.first().positionIndex)
    // Initial position 1 must not have app2
    assertFalse(result.any { it.positionIndex == 1 && it.packageName == "com.test.app2" })
  }

  @Test
  fun testDragFilteringRemovesBothMatchingIdAndMatchingPackage() {
    val placements = listOf(
      SpaceItemPlacement(id = "p1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.pkg1"),
      SpaceItemPlacement(id = "p2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.pkg2"),
      SpaceItemPlacement(id = "p2_dup", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.pkg2")
    )
    val dragged = SpaceItemPlacement(id = "p2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.pkg2")

    val remaining = placements.filter { p ->
      p.id != dragged.id && (dragged.packageName == null || p.packageName != dragged.packageName)
    }

    assertEquals(1, remaining.size)
    assertEquals("com.pkg1", remaining.first().packageName)
  }

  @Test
  fun testWidgetFootprintDisplacesMultipleApps() {
    // 4-column grid, page size 16 (4 rows x 4 cols)
    val gridCols = 4
    val testPageSize = 16
    val existing = listOf(
      SpaceItemPlacement(id = "app0", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app0"),
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app1"),
      SpaceItemPlacement(id = "app4", spaceId = "s1", pageIndex = 0, positionIndex = 4, packageName = "com.app4"),
      SpaceItemPlacement(id = "app5", spaceId = "s1", pageIndex = 0, positionIndex = 5, packageName = "com.app5")
    )
    val widget2x2 = SpaceItemPlacement(
      id = "widget_clock",
      spaceId = "s1",
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    // Drop 2x2 widget at position 0 (footprint: row 0 cols 0..1, row 1 cols 0..1 -> slots 0, 1, 4, 5)
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = widget2x2,
      targetPage = 0,
      targetPosition = 0,
      pageSize = testPageSize,
      cols = gridCols
    )

    val map = result.associateBy { it.id }
    val placedWidget = map["widget_clock"]
    assertTrue("Widget must be placed", placedWidget != null)
    assertEquals(0, placedWidget?.pageIndex)
    assertEquals(0, placedWidget?.positionIndex)
    assertEquals(2, placedWidget?.spanX)
    assertEquals(2, placedWidget?.spanY)

    // The widget reserves slots {0, 1, 4, 5}. None of the displaced apps should remain in these slots!
    val widgetReservedSlots = setOf(0, 1, 4, 5)
    val appIds = listOf("app0", "app1", "app4", "app5")
    for (appId in appIds) {
      val app = map[appId]
      assertTrue("App $appId should still exist", app != null)
      assertFalse(
        "App $appId should be displaced out of widget footprint",
        widgetReservedSlots.contains(app!!.positionIndex)
      )
    }
  }

  @Test
  fun testAppDropOntoMultiCellWidgetDisplacesWidget() {
    val gridCols = 4
    val testPageSize = 16
    // 2x2 widget at position 0 (occupies slots 0, 1, 4, 5)
    val widget2x2 = SpaceItemPlacement(
      id = "widget_clock",
      spaceId = "s1",
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )
    val incomingApp = SpaceItemPlacement(
      id = "incoming_app",
      spaceId = "s1",
      pageIndex = 0,
      positionIndex = 0,
      packageName = "com.test.incoming"
    )

    // Dropping app onto slot 1 (inside widget footprint {0, 1, 4, 5})
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = listOf(widget2x2),
      itemToInsert = incomingApp,
      targetPage = 0,
      targetPosition = 1,
      pageSize = testPageSize,
      cols = gridCols
    )

    val map = result.associateBy { it.id }
    val placedApp = map["incoming_app"]
    val displacedWidget = map["widget_clock"]

    assertTrue(placedApp != null)
    assertEquals(0, placedApp?.pageIndex)
    assertEquals(1, placedApp?.positionIndex)

    assertTrue(displacedWidget != null)
    // Widget must not overlap slot 1
    val widgetFp = PlacementCascadeHelper.getFootprint(
      pageIndex = displacedWidget!!.pageIndex,
      positionIndex = displacedWidget.positionIndex,
      spanX = displacedWidget.spanX,
      spanY = displacedWidget.spanY,
      cols = gridCols,
      pageSize = testPageSize
    )
    val appGlobalSlot = placedApp!!.pageIndex * testPageSize + placedApp.positionIndex
    assertFalse(
      "Widget footprint should not overlap with app slot",
      widgetFp.globalSlots.contains(appGlobalSlot)
    )
  }

  @Test
  fun testMultiCellWidgetClampsAtGridBoundary() {
    val gridCols = 4
    val testPageSize = 16
    val widget2x2 = SpaceItemPlacement(
      id = "widget_clock",
      spaceId = "s1",
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    // Dropping 2x2 widget at position 3 (row 0, col 3) -> should clamp to col 2 (position 2)
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = emptyList(),
      itemToInsert = widget2x2,
      targetPage = 0,
      targetPosition = 3,
      pageSize = testPageSize,
      cols = gridCols
    )

    val placed = result.firstOrNull { it.id == "widget_clock" }
    assertTrue(placed != null)
    assertEquals(2, placed?.positionIndex) // clamped to col 2 so spanX=2 fits in 4 cols
  }

  @Test
  fun testMultiCellWidgetCascadeOverflowsToNextPage() {
    val gridCols = 4
    val testPageSize = 4 // 1 row of 4 cols per page
    val existing = listOf(
      SpaceItemPlacement(id = "app0", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app0"),
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app1"),
      SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "com.app2"),
      SpaceItemPlacement(id = "app3", spaceId = "s1", pageIndex = 0, positionIndex = 3, packageName = "com.app3")
    )
    val widget2x1 = SpaceItemPlacement(
      id = "widget_search",
      spaceId = "s1",
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 1,
      customWidgetType = SpaceItemPlacement.WIDGET_QUICK_SEARCH
    )

    // Drop 2x1 widget at pos 0 on full page 0
    val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = existing,
      itemToInsert = widget2x1,
      targetPage = 0,
      targetPosition = 0,
      pageSize = testPageSize,
      cols = gridCols
    )

    val map = result.associateBy { it.id }
    assertEquals(0, map["widget_search"]?.pageIndex)
    assertEquals(0, map["widget_search"]?.positionIndex)

    // Some apps must have cascaded to page 1
    val onPage1 = result.filter { it.pageIndex == 1 }
    assertTrue("Trailing apps should cascade to page 1", onPage1.isNotEmpty())
  }

  @Test
  fun testFindEmptySlotOnPreferredPageWhenAvailable() {
    val existing = listOf(
      SpaceItemPlacement(id = "app0", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app0"),
      SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app1")
    )
    val result = PlacementCascadeHelper.findEmptySlotForWidget(
      existingPlacements = existing,
      preferredPage = 0,
      spanX = 2,
      spanY = 2,
      cols = 4,
      pageSize = 20,
      existingPageCount = 1
    )

    assertEquals(0, result.pageIndex)
    assertEquals(2, result.positionIndex)
    assertFalse(result.isNewPage)
  }

  @Test
  fun testFindEmptySlotWhenPreferredPageFullChecksNextPage() {
    // Fill page 0 completely (20 items in 4 cols x 5 rows)
    val page0Apps = (0 until 20).map { i ->
      SpaceItemPlacement(id = "app$i", spaceId = "s1", pageIndex = 0, positionIndex = i, packageName = "com.app$i")
    }
    // Page 1 has 4 items in row 0
    val page1Apps = (0 until 4).map { i ->
      SpaceItemPlacement(id = "p1_app$i", spaceId = "s1", pageIndex = 1, positionIndex = i, packageName = "com.p1_app$i")
    }
    val existing = page0Apps + page1Apps

    val result = PlacementCascadeHelper.findEmptySlotForWidget(
      existingPlacements = existing,
      preferredPage = 0,
      spanX = 2,
      spanY = 2,
      cols = 4,
      pageSize = 20,
      existingPageCount = 2
    )

    assertEquals(1, result.pageIndex)
    assertEquals(4, result.positionIndex) // Starts at row 1, col 0
    assertFalse(result.isNewPage)
  }

  @Test
  fun testFindEmptySlotWhenAllPagesFullCreatesNewPage() {
    // Fill page 0 completely (20 items)
    val page0Apps = (0 until 20).map { i ->
      SpaceItemPlacement(id = "app$i", spaceId = "s1", pageIndex = 0, positionIndex = i, packageName = "com.app$i")
    }

    val result = PlacementCascadeHelper.findEmptySlotForWidget(
      existingPlacements = page0Apps,
      preferredPage = 0,
      spanX = 2,
      spanY = 2,
      cols = 4,
      pageSize = 20,
      existingPageCount = 1
    )

    assertEquals(1, result.pageIndex) // Created page 1
    assertEquals(0, result.positionIndex) // Placed at top-left
    assertTrue(result.isNewPage)
  }

  @Test
  fun testFindEmptySlotRespectsMultiCellWidgetOccupancy() {
    // Page 0 has a 4x1 search bar widget at pos 0 (occupies slots 0, 1, 2, 3)
    val widget4x1 = SpaceItemPlacement(
      id = "widget_search",
      spaceId = "s1",
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 4,
      spanY = 1,
      customWidgetType = SpaceItemPlacement.WIDGET_QUICK_SEARCH
    )
    // and apps at slot 4 and 5
    val app4 = SpaceItemPlacement(id = "app4", spaceId = "s1", pageIndex = 0, positionIndex = 4, packageName = "com.app4")
    val app5 = SpaceItemPlacement(id = "app5", spaceId = "s1", pageIndex = 0, positionIndex = 5, packageName = "com.app5")

    val result = PlacementCascadeHelper.findEmptySlotForWidget(
      existingPlacements = listOf(widget4x1, app4, app5),
      preferredPage = 0,
      spanX = 2,
      spanY = 1,
      cols = 4,
      pageSize = 20,
      existingPageCount = 1
    )

    assertEquals(0, result.pageIndex)
    assertEquals(6, result.positionIndex) // slots 0..3 and 4..5 are taken, next available 2x1 is pos 6 (row 1, col 2)
    assertFalse(result.isNewPage)
  }
}
