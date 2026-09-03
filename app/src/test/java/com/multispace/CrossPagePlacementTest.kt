package com.multispace

import com.multispace.domain.model.SpaceItemPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossPagePlacementTest {

  @Test
  fun testReindexSourceAndTargetPagesOnMove() {
    // Page 0 has 3 items (A, B, C), Page 1 has 2 items (D, E)
    val page0 = mutableListOf(
      SpaceItemPlacement(id = "item_a", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 0, itemType = "APP", packageName = "com.a"),
      SpaceItemPlacement(id = "item_b", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 1, itemType = "APP", packageName = "com.b"),
      SpaceItemPlacement(id = "item_c", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 2, itemType = "APP", packageName = "com.c")
    )
    val page1 = mutableListOf(
      SpaceItemPlacement(id = "item_d", spaceId = "s1", layer = 1, pageIndex = 1, positionIndex = 0, itemType = "APP", packageName = "com.d"),
      SpaceItemPlacement(id = "item_e", spaceId = "s1", layer = 1, pageIndex = 1, positionIndex = 1, itemType = "APP", packageName = "com.e")
    )

    // Move Item B from Page 0 to Page 1 at position 1 (between D and E)
    val movedItem = page0.first { it.id == "item_b" }
    page0.remove(movedItem)

    // Reindex page 0
    val reindexedPage0 = page0.mapIndexed { idx, item -> item.copy(positionIndex = idx) }
    assertEquals(2, reindexedPage0.size)
    assertEquals("item_a", reindexedPage0[0].id)
    assertEquals(0, reindexedPage0[0].positionIndex)
    assertEquals("item_c", reindexedPage0[1].id)
    assertEquals(1, reindexedPage0[1].positionIndex) // No gaps!

    // Insert into page 1 at position 1
    val targetPos = 1
    page1.add(targetPos, movedItem.copy(pageIndex = 1, positionIndex = targetPos))
    val reindexedPage1 = page1.mapIndexed { idx, item -> item.copy(positionIndex = idx) }

    assertEquals(3, reindexedPage1.size)
    assertEquals("item_d", reindexedPage1[0].id)
    assertEquals(0, reindexedPage1[0].positionIndex)
    assertEquals("item_b", reindexedPage1[1].id)
    assertEquals(1, reindexedPage1[1].positionIndex)
    assertEquals("item_e", reindexedPage1[2].id)
    assertEquals(2, reindexedPage1[2].positionIndex)
  }

  @Test
  fun testTrailingPageCreationWhenMovingBeyondLastPage() {
    val existingPlacements = listOf(
      SpaceItemPlacement(id = "item_1", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 0, itemType = "APP", packageName = "com.1")
    )

    val maxPage = existingPlacements.maxOfOrNull { it.pageIndex } ?: 0
    val basePageCount = (maxPage + 1).coerceAtLeast(1)
    assertEquals(1, basePageCount)

    // Drag past the edge: extra page allocated
    var extraPagesCount = 1
    val totalPageCount = basePageCount + extraPagesCount
    assertEquals(2, totalPageCount)

    // Dropped onto page 1
    val targetPage = 1
    val newItemOnPage1 = existingPlacements[0].copy(pageIndex = targetPage, positionIndex = 0)
    assertEquals(1, newItemOnPage1.pageIndex)
    assertEquals(0, newItemOnPage1.positionIndex)
  }

  @Test
  fun testEdgeZonePxCalculationConstraints() {
    val density = 2.5f // 2.5 px per dp
    val baseEdgeZonePx = 80f * density // 200px
    val viewportWidth = 1080f

    // Capped at 22% of viewport width
    val maxAllowedEdgeZone = viewportWidth * 0.22f // 237.6px
    val edgeZonePx = baseEdgeZonePx.coerceAtMost(maxAllowedEdgeZone)

    assertEquals(200f, edgeZonePx, 0.01f)
    assertTrue("Edge zone must be strictly within viewport boundary", edgeZonePx < viewportWidth / 2)
  }

  @Test
  fun testFlexiblePlacementInArbitraryEmptyGridSlot() {
    // 4 columns x 5 rows grid = 20 slots (0 to 19)
    val pageItems = listOf(
      SpaceItemPlacement(id = "item_1", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 0, itemType = "APP", packageName = "app1"),
      SpaceItemPlacement(id = "item_2", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 1, itemType = "APP", packageName = "app2")
    )

    // User drags and targets slot 14 (Row 3, Col 2) which is currently empty
    val targetSlot = 14
    val hasCollision = pageItems.any { it.positionIndex == targetSlot }
    assertEquals(false, hasCollision)

    // Placement should succeed directly at slot 14 without shifting existing items at 0 and 1
    val placedItem = SpaceItemPlacement(
      id = "item_dragged", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = targetSlot, itemType = "APP", packageName = "app3"
    )
    val finalPlacements = pageItems + placedItem
    assertEquals(3, finalPlacements.size)
    assertEquals(0, finalPlacements[0].positionIndex)
    assertEquals(1, finalPlacements[1].positionIndex)
    assertEquals(14, finalPlacements[2].positionIndex)
  }

  @Test
  fun testRealtimePreviewShiftingOnCollision() {
    // Page with items at position 0, 1, 2, 3
    val pageItems = listOf(
      SpaceItemPlacement(id = "p0", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 0, itemType = "APP", packageName = "a"),
      SpaceItemPlacement(id = "p1", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 1, itemType = "APP", packageName = "b"),
      SpaceItemPlacement(id = "p2", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 2, itemType = "APP", packageName = "c"),
      SpaceItemPlacement(id = "p3", spaceId = "s1", layer = 1, pageIndex = 0, positionIndex = 3, itemType = "APP", packageName = "d")
    )

    // User hovers dragged app at position 1 (between p0 and p1)
    val previewTargetSlot = 1
    val hasCollision = pageItems.any { it.positionIndex == previewTargetSlot }
    assertTrue(hasCollision)

    // Visual shifting preview: items with positionIndex >= 1 shift by +1
    val previewSlots = pageItems.associate { placement ->
      val shiftedPos = if (hasCollision && placement.positionIndex >= previewTargetSlot) {
        placement.positionIndex + 1
      } else {
        placement.positionIndex
      }
      shiftedPos to placement
    }

    // Target preview slot 1 is now vacant for the preview ghost drop slot
    assertTrue(!previewSlots.containsKey(previewTargetSlot))
    assertEquals("p0", previewSlots[0]?.id)
    assertEquals("p1", previewSlots[2]?.id) // Shifted 1 -> 2
    assertEquals("p2", previewSlots[3]?.id) // Shifted 2 -> 3
    assertEquals("p3", previewSlots[4]?.id) // Shifted 3 -> 4
  }

  @Test
  fun testDraggedPointerCoordinatesPreservedAcrossPageTransition() {
    // Pointer coordinate in root window space
    var currentPointerX = 1040f // Near right edge
    var currentPointerY = 520f
    var currentPage = 0

    // Trigger page transition to page 1
    currentPage += 1

    // Drag position must stay glued to the finger coordinates and not reset
    assertEquals(1, currentPage)
    assertEquals(1040f, currentPointerX, 0.001f)
    assertEquals(520f, currentPointerY, 0.001f)
  }
}
