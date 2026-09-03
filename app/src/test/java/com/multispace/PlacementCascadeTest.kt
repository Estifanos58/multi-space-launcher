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
}
