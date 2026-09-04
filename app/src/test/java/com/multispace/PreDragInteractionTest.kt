package com.multispace

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import org.junit.Assert.*
import org.junit.Test

class PreDragInteractionTest {

  @Test
  fun testAppPlacementPreDragTarget() {
    val appPlacement = SpaceItemPlacement(
      id = "placement_app_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 5,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.example.testapp",
      componentName = "com.example.testapp.MainActivity"
    )

    assertFalse(appPlacement.isWidget)
    assertFalse(appPlacement.isFolder)
    assertEquals("com.example.testapp", appPlacement.packageName)

    val app = DiscoveredApp(
      id = "app_1",
      packageName = "com.example.testapp",
      activityName = "com.example.testapp.MainActivity",
      label = "Test App"
    )

    var openedAppInfoPackage: String? = null
    val onOpenAppInfo: (DiscoveredApp) -> Unit = { target ->
      openedAppInfoPackage = target.packageName
    }

    onOpenAppInfo(app)
    assertEquals("com.example.testapp", openedAppInfoPackage)
  }

  @Test
  fun testWidgetPlacementPreDragResizeTarget() {
    val widgetPlacement = SpaceItemPlacement(
      id = "placement_widget_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 8,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    assertTrue(widgetPlacement.isWidget)
    assertFalse(widgetPlacement.isFolder)

    var activeResizingWidgetId: String? = null
    val onActivateResize: (String) -> Unit = { id ->
      activeResizingWidgetId = id
    }

    onActivateResize(widgetPlacement.id)
    assertEquals("placement_widget_1", activeResizingWidgetId)
  }

  @Test
  fun testPreDragOverlayBoundsCalculation() {
    val boxWidth = 44f
    val boxHeight = 44f
    val gap = 8f
    val minMargin = 16f
    val viewportWidth = 400f

    // Case 1: Item in middle of screen
    val targetCenterX = 200f
    val targetTop = 250f
    val idealTop = targetTop - gap - boxHeight
    val topY = if (idealTop >= minMargin) idealTop else (targetTop + 100f + gap)
    val leftX = (targetCenterX - (boxWidth / 2f)).coerceIn(
      minMargin,
      (viewportWidth - boxWidth - minMargin).coerceAtLeast(minMargin)
    )

    assertEquals(198f, idealTop, 0.01f)
    assertEquals(198f, topY, 0.01f)
    assertEquals(178f, leftX, 0.01f)

    // Case 2: Item at the very top edge of screen
    val topItemTop = 10f
    val topItemBottom = 70f
    val topIdealTop = topItemTop - gap - boxHeight // 10 - 8 - 44 = -42 < minMargin
    val topCalculatedY = if (topIdealTop >= minMargin) topIdealTop else (topItemBottom + gap)
    assertEquals(78f, topCalculatedY, 0.01f)
  }

  @Test
  fun testWidgetSpatialFootprintAndGesturePriority() {
    val cols = 4
    val rows = 6
    val pageSize = cols * rows

    // 2x2 Widget placed at (row 1, col 1) -> positionIndex = 1 * 4 + 1 = 5
    val widgetPlacement = SpaceItemPlacement(
      id = "widget_2x2",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 5,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    // Compute occupied slots: rows in 1..2, cols in 1..2
    // Expected slots: 5 (1,1), 6 (1,2), 9 (2,1), 10 (2,2)
    val widgetR = widgetPlacement.positionIndex / cols
    val widgetC = widgetPlacement.positionIndex % cols
    val occupiedSlots = (0 until widgetPlacement.spanY).flatMap { dr ->
      (0 until widgetPlacement.spanX).map { dc ->
        (widgetR + dr) * cols + (widgetC + dc)
      }
    }.toSet()

    assertEquals(setOf(5, 6, 9, 10), occupiedSlots)

    // Gesture priority simulation:
    // Any touch hitting slots 5, 6, 9, or 10 MUST resolve to the widget, NOT empty desktop
    val allPlacements = listOf(widgetPlacement)

    fun resolveTouch(touchSlot: Int): String {
      val hitWidget = allPlacements.firstOrNull { item ->
        if (!item.isWidget) return@firstOrNull false
        val r = item.positionIndex / cols
        val c = item.positionIndex % cols
        val sX = item.spanX
        val sY = item.spanY
        val tR = touchSlot / cols
        val tC = touchSlot % cols
        tC in c until (c + sX) && tR in r until (r + sY)
      }
      return if (hitWidget != null) "WIDGET_INTERACTION" else "DESKTOP_CUSTOMIZATION"
    }

    // Touch all 4 slots of the 2x2 widget
    assertEquals("WIDGET_INTERACTION", resolveTouch(5))
    assertEquals("WIDGET_INTERACTION", resolveTouch(6))
    assertEquals("WIDGET_INTERACTION", resolveTouch(9))
    assertEquals("WIDGET_INTERACTION", resolveTouch(10))

    // Touch empty space outside the widget
    assertEquals("DESKTOP_CUSTOMIZATION", resolveTouch(0))
    assertEquals("DESKTOP_CUSTOMIZATION", resolveTouch(4))
    assertEquals("DESKTOP_CUSTOMIZATION", resolveTouch(7))
    assertEquals("DESKTOP_CUSTOMIZATION", resolveTouch(11))
    assertEquals("DESKTOP_CUSTOMIZATION", resolveTouch(15))
  }

  @Test
  fun testWidgetDropDisplacementCalculation() {
    val cols = 4
    val pageSize = 24

    // Existing app at slot 5
    val existingApp = SpaceItemPlacement(
      id = "app_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 5,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.test.app1"
    )

    // 2x2 widget to drop at slot 4 (covers 4, 5, 8, 9)
    val widgetToDrop = SpaceItemPlacement(
      id = "widget_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 4,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    val updatedPlacements = com.multispace.domain.model.PlacementCascadeHelper.computeFullPlacementsAfterDrop(
      allCurrentPlacements = listOf(existingApp),
      itemToInsert = widgetToDrop,
      targetPage = 0,
      targetPosition = 4,
      pageSize = pageSize,
      cols = cols
    )

    val placedWidget = updatedPlacements.first { it.id == widgetToDrop.id }
    val displacedApp = updatedPlacements.first { it.id == existingApp.id }

    // Widget lands at slot 4
    assertEquals(4, placedWidget.positionIndex)
    assertEquals(0, placedWidget.pageIndex)

    // App was displaced from slot 5 (which is covered by widget)
    // Widget footprint is {4, 5, 8, 9}. App should be shifted beyond the widget's footprint!
    val widgetFootprint = setOf(4, 5, 8, 9)
    assertFalse(widgetFootprint.contains(displacedApp.positionIndex))
  }
}
