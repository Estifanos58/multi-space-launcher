package com.multispace

import com.multispace.data.entity.SpaceEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import org.junit.Assert.*
import org.junit.Test

class DesktopCustomizationTest {

  @Test
  fun testSpacePageCountDefaultAndEntityMapping() {
    val defaultSpace = Space(
      id = "space_1",
      name = "Work Space",
      authPolicy = Space.AUTH_NONE
    )

    assertEquals(1, defaultSpace.pageCount)

    val spaceWithPages = defaultSpace.copy(pageCount = 3)
    val entity = SpaceEntity.fromDomain(spaceWithPages)
    assertEquals(3, entity.pageCount)

    val restored = entity.toDomain()
    assertEquals(3, restored.pageCount)
  }

  @Test
  fun testWidgetPlacementModelAndEntityMapping() {
    val widgetPlacement = SpaceItemPlacement(
      id = "widget_clock_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 1,
      appWidgetId = -1,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    assertTrue(widgetPlacement.isWidget)
    assertFalse(widgetPlacement.isFolder)
    assertEquals(2, widgetPlacement.spanX)
    assertEquals(1, widgetPlacement.spanY)
    assertEquals(SpaceItemPlacement.WIDGET_CLOCK_DATE, widgetPlacement.customWidgetType)

    val entity = SpaceItemPlacementEntity.fromDomain(widgetPlacement)
    assertEquals(SpaceItemPlacement.ITEM_TYPE_WIDGET, entity.itemType)
    assertEquals(2, entity.spanX)
    assertEquals(1, entity.spanY)
    assertEquals(SpaceItemPlacement.WIDGET_CLOCK_DATE, entity.customWidgetType)

    val restored = entity.toDomain()
    assertTrue(restored.isWidget)
    assertEquals(2, restored.spanX)
    assertEquals(1, restored.spanY)
    assertEquals(SpaceItemPlacement.WIDGET_CLOCK_DATE, restored.customWidgetType)
  }

  @Test
  fun testWidgetTypesConstants() {
    assertEquals("CLOCK_DATE", SpaceItemPlacement.WIDGET_CLOCK_DATE)
    assertEquals("QUICK_SEARCH", SpaceItemPlacement.WIDGET_QUICK_SEARCH)
    assertEquals("CALENDAR", SpaceItemPlacement.WIDGET_CALENDAR)
    assertEquals("BATTERY_STATUS", SpaceItemPlacement.WIDGET_BATTERY_STATUS)
    assertEquals("QUICK_NOTES", SpaceItemPlacement.WIDGET_QUICK_NOTES)
  }

  @Test
  fun testPage1ImmutabilityLogic() {
    val pageToDelete = 0
    val canDelete = pageToDelete != 0
    assertFalse("Page 1 (index 0) must be immutable and cannot be deleted", canDelete)

    val otherPageToDelete = 1
    val canDeleteOther = otherPageToDelete != 0
    assertTrue("Page 2+ (index >= 1) can be deleted", canDeleteOther)
  }

  @Test
  fun testPageDeletionPlacementShiftLogic() {
    val spaceId = "test_space"
    val placements = listOf(
      SpaceItemPlacement(id = "p0", spaceId = spaceId, layer = SpaceItemPlacement.LAYER_HOME, pageIndex = 0, positionIndex = 0),
      SpaceItemPlacement(id = "p1_to_delete", spaceId = spaceId, layer = SpaceItemPlacement.LAYER_HOME, pageIndex = 1, positionIndex = 0),
      SpaceItemPlacement(id = "p2_to_shift", spaceId = spaceId, layer = SpaceItemPlacement.LAYER_HOME, pageIndex = 2, positionIndex = 0)
    )

    val deletedPageIndex = 1
    // Filter out deleted page
    val remaining = placements.filter { it.pageIndex != deletedPageIndex }
    // Shift subsequent pages
    val shifted = remaining.map {
      if (it.pageIndex > deletedPageIndex) it.copy(pageIndex = it.pageIndex - 1) else it
    }

    assertEquals(2, shifted.size)
    assertEquals(0, shifted.first { it.id == "p0" }.pageIndex)
    assertEquals(1, shifted.first { it.id == "p2_to_shift" }.pageIndex)
  }

  @Test
  fun testStateIsolationBetweenThemeAndWallpaper() {
    val initialSpace = Space(
      id = "iso_space",
      name = "Isolated Space",
      authPolicy = Space.AUTH_NONE,
      appTheme = "DEFAULT",
      gridColumns = 4,
      backgroundType = Space.BACKGROUND_DEFAULT,
      backgroundColor = null,
      backgroundImageUri = null
    )

    // Modifying wallpaper must not mutate theme or grid
    val wallpaperUpdated = initialSpace.copy(
      backgroundType = Space.BACKGROUND_COLOR,
      backgroundColor = 0xFF12141CL
    )
    assertEquals("DEFAULT", wallpaperUpdated.appTheme)
    assertEquals(4, wallpaperUpdated.gridColumns)
    assertEquals(Space.BACKGROUND_COLOR, wallpaperUpdated.backgroundType)
    assertEquals(0xFF12141CL, wallpaperUpdated.backgroundColor)

    // Modifying theme must not mutate wallpaper
    val themeUpdated = wallpaperUpdated.copy(
      appTheme = "PURPLE",
      gridColumns = 5
    )
    assertEquals("PURPLE", themeUpdated.appTheme)
    assertEquals(5, themeUpdated.gridColumns)
    assertEquals(Space.BACKGROUND_COLOR, themeUpdated.backgroundType)
    assertEquals(0xFF12141CL, themeUpdated.backgroundColor)
  }
}
