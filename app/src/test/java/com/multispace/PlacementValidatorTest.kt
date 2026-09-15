package com.multispace

import com.multispace.domain.model.PlacementValidator
import com.multispace.domain.model.SpaceItemPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementValidatorTest {

  private val validApp1 = SpaceItemPlacement(
    id = "place_app1",
    spaceId = "space_1",
    layer = SpaceItemPlacement.LAYER_HOME,
    pageIndex = 0,
    positionIndex = 0,
    itemType = SpaceItemPlacement.ITEM_TYPE_APP,
    packageName = "com.example.app1",
    componentName = "com.example.app1.MainActivity"
  )

  private val validApp2 = SpaceItemPlacement(
    id = "place_app2",
    spaceId = "space_1",
    layer = SpaceItemPlacement.LAYER_HOME,
    pageIndex = 0,
    positionIndex = 1,
    itemType = SpaceItemPlacement.ITEM_TYPE_APP,
    packageName = "com.example.app2",
    componentName = "com.example.app2.MainActivity"
  )

  @Test
  fun testValidPlacementsPassValidation() {
    val placements = listOf(validApp1, validApp2)
    val result = PlacementValidator.validatePlacements(placements, cols = 4, rows = 5)

    assertTrue("Valid placements must pass validation", result.isValid)
    assertEquals("Should have zero issues", 0, result.issues.size)
  }

  @Test
  fun testDetectsInvalidPageIndex() {
    val invalidPlacement = validApp1.copy(pageIndex = -1)
    val result = PlacementValidator.validatePlacements(listOf(invalidPlacement), cols = 4, rows = 5)

    assertFalse("Negative page index must fail validation", result.isValid)
    assertTrue("Must flag InvalidPageIndex", result.issues.any { it.type is PlacementValidator.IssueType.InvalidPageIndex })
    assertEquals(1, result.invalidPageIndexCount)
  }

  @Test
  fun testDetectsNegativePositionIndex() {
    val invalidPlacement = validApp1.copy(positionIndex = -5)
    val result = PlacementValidator.validatePlacements(listOf(invalidPlacement), cols = 4, rows = 5)

    assertFalse("Negative position index must fail validation", result.isValid)
    assertTrue("Must flag OutOfBounds", result.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds })
  }

  @Test
  fun testDetectsPositionExceedingGridCapacity() {
    // In a 4x5 grid, max slot index is 19
    val invalidPlacement = validApp1.copy(positionIndex = 20)
    val result = PlacementValidator.validatePlacements(listOf(invalidPlacement), cols = 4, rows = 5)

    assertFalse("Position >= 20 in 4x5 grid must fail validation", result.isValid)
    assertTrue("Must flag OutOfBounds", result.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds })
  }

  @Test
  fun testDetectsWidgetSpanOutOfBounds() {
    // In a 4x5 grid, placing a 3x1 widget at column 2 (index 2) overflows 2 + 3 > 4
    val overflowWidget = SpaceItemPlacement(
      id = "place_widget",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2, // col 2, row 0
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      appWidgetId = 101,
      spanX = 3,
      spanY = 1
    )
    val result = PlacementValidator.validatePlacements(listOf(overflowWidget), cols = 4, rows = 5)

    assertFalse("Widget span overflowing grid edge must fail validation", result.isValid)
    assertTrue("Must flag OutOfBounds", result.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds })
  }

  @Test
  fun testDetectsDuplicateOccupiedSlot() {
    // Both apps occupy page 0, slot 3
    val item1 = validApp1.copy(id = "p1", positionIndex = 3, packageName = "com.example.one")
    val item2 = validApp2.copy(id = "p2", positionIndex = 3, packageName = "com.example.two")
    val result = PlacementValidator.validatePlacements(listOf(item1, item2), cols = 4, rows = 5)

    assertFalse("Overlapping items in same slot must fail validation", result.isValid)
    assertTrue("Must flag DuplicateOccupiedSlot", result.issues.any { it.type is PlacementValidator.IssueType.DuplicateOccupiedSlot })
    assertEquals(1, result.duplicateOccupiedSlotCount)
  }

  @Test
  fun testDetectsInvalidRecordMissingPackageOrId() {
    val blankId = validApp1.copy(id = "")
    val blankPkg = validApp2.copy(packageName = null)

    val result = PlacementValidator.validatePlacements(listOf(blankId, blankPkg), cols = 4, rows = 5)
    assertFalse("Blank ID and null packageName must fail validation", result.isValid)
    assertTrue(result.issues.any { it.description.contains("blank or empty ID") })
    assertTrue(result.issues.any { it.description.contains("missing required packageName") })
  }

  @Test
  fun testDetectsDuplicatePlacementId() {
    val p1 = validApp1.copy(id = "dup_id", positionIndex = 0, packageName = "com.example.one")
    val p2 = validApp2.copy(id = "dup_id", positionIndex = 1, packageName = "com.example.two")

    val result = PlacementValidator.validatePlacements(listOf(p1, p2), cols = 4, rows = 5)
    assertFalse("Duplicate placement IDs must fail validation", result.isValid)
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.DuplicateRecord })
  }
}
