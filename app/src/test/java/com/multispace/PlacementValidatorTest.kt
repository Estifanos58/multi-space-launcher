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

  @Test
  fun testValidPlacementAccepted() {
    val realisticLayout = listOf(
      validApp1.copy(id = "p_app1", pageIndex = 0, positionIndex = 0),
      validApp2.copy(id = "p_app2", pageIndex = 0, positionIndex = 1),
      SpaceItemPlacement(
        id = "p_folder",
        spaceId = "space_1",
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 2,
        itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
        folderId = "folder_1"
      ),
      SpaceItemPlacement(
        id = "p_widget",
        spaceId = "space_1",
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 1,
        positionIndex = 0, // col 0, row 0
        itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
        appWidgetId = 42,
        spanX = 2,
        spanY = 2
      )
    )

    val result = PlacementValidator.validatePlacements(realisticLayout, cols = 4, rows = 5)
    assertTrue("Realistic valid layout must pass validation", result.isValid)
    assertTrue("No issues should be reported", result.issues.isEmpty())
  }

  @Test
  fun testDuplicateSlotDetected() {
    val firstItem = validApp1.copy(id = "p_item_a", pageIndex = 1, positionIndex = 6)
    val collidingItem = validApp2.copy(id = "p_item_b", pageIndex = 1, positionIndex = 6)

    val result = PlacementValidator.validatePlacements(listOf(firstItem, collidingItem), cols = 4, rows = 5)
    assertFalse("Colliding slots must be flagged as invalid", result.isValid)
    assertEquals(1, result.duplicateOccupiedSlotCount)
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.DuplicateOccupiedSlot })
  }

  @Test
  fun testNegativePageDetected() {
    val itemWithNegativePage = validApp1.copy(id = "p_neg_page", pageIndex = -2)
    val result = PlacementValidator.validatePlacements(listOf(itemWithNegativePage), cols = 4, rows = 5)

    assertFalse("Negative page must fail validation", result.isValid)
    assertEquals(1, result.invalidPageIndexCount)
    assertTrue(result.issues.any { it.type is PlacementValidator.IssueType.InvalidPageIndex })
  }

  @Test
  fun testInvalidRowColumnDetected() {
    // 1. Negative position index
    val negPos = validApp1.copy(id = "p_neg_pos", positionIndex = -1)
    val res1 = PlacementValidator.validatePlacements(listOf(negPos), cols = 4, rows = 5)
    assertFalse("Negative position index must fail", res1.isValid)
    assertTrue(res1.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds })

    // 2. Position exceeding grid capacity (in 4x5, max index is 19)
    val overflowPos = validApp1.copy(id = "p_overflow_pos", positionIndex = 22)
    val res2 = PlacementValidator.validatePlacements(listOf(overflowPos), cols = 4, rows = 5)
    assertFalse("Position exceeding grid capacity must fail", res2.isValid)
    assertTrue(res2.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds })

    // 3. Widget span overflowing columns (col 3 + spanX 2 > 4)
    val colOverflowWidget = SpaceItemPlacement(
      id = "p_col_overflow",
      spaceId = "space_1",
      pageIndex = 0,
      positionIndex = 3, // col 3, row 0
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      appWidgetId = 99,
      spanX = 2,
      spanY = 1
    )
    val res3 = PlacementValidator.validatePlacements(listOf(colOverflowWidget), cols = 4, rows = 5)
    assertFalse("Widget overflowing right edge must fail", res3.isValid)
    assertTrue(res3.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds })

    // 4. Widget span overflowing rows (row 4 + spanY 2 > 5)
    val rowOverflowWidget = SpaceItemPlacement(
      id = "p_row_overflow",
      spaceId = "space_1",
      pageIndex = 0,
      positionIndex = 16, // col 0, row 4
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      appWidgetId = 100,
      spanX = 1,
      spanY = 2
    )
    val res4 = PlacementValidator.validatePlacements(listOf(rowOverflowWidget), cols = 4, rows = 5)
    assertFalse("Widget overflowing bottom edge must fail", res4.isValid)
    assertTrue(res4.issues.any { it.type is PlacementValidator.IssueType.OutOfBounds })
  }

  @Test
  fun testMissingOrCorruptPlacementDetected() {
    // Blank ID
    val corruptId = validApp1.copy(id = "   ")
    val res1 = PlacementValidator.validatePlacements(listOf(corruptId), cols = 4, rows = 5)
    assertFalse("Blank ID must fail", res1.isValid)
    assertTrue(res1.issues.any { it.type is PlacementValidator.IssueType.InvalidRecord })

    // Blank spaceId
    val corruptSpaceId = validApp1.copy(spaceId = "")
    val res2 = PlacementValidator.validatePlacements(listOf(corruptSpaceId), cols = 4, rows = 5)
    assertFalse("Blank spaceId must fail", res2.isValid)
    assertTrue(res2.issues.any { it.type is PlacementValidator.IssueType.InvalidRecord })

    // App missing packageName
    val corruptAppPackage = validApp1.copy(packageName = null)
    val res3 = PlacementValidator.validatePlacements(listOf(corruptAppPackage), cols = 4, rows = 5)
    assertFalse("App with missing packageName must fail", res3.isValid)
    assertTrue(res3.issues.any { it.type is PlacementValidator.IssueType.InvalidRecord })

    // Folder missing folderId
    val corruptFolder = SpaceItemPlacement(
      id = "p_corrupt_folder",
      spaceId = "space_1",
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = null
    )
    val res4 = PlacementValidator.validatePlacements(listOf(corruptFolder), cols = 4, rows = 5)
    assertFalse("Folder with missing folderId must fail", res4.isValid)
    assertTrue(res4.issues.any { it.type is PlacementValidator.IssueType.InvalidRecord })
  }

  @Test
  fun testNormalValidPlacementRemainsUnchanged() {
    val originalPlacements = listOf(
      validApp1.copy(id = "app_1", pageIndex = 0, positionIndex = 0),
      validApp2.copy(id = "app_2", pageIndex = 0, positionIndex = 1),
      SpaceItemPlacement(
        id = "folder_1",
        spaceId = "space_1",
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = 4,
        itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
        folderId = "f_1"
      )
    )

    // Run validator
    val result = PlacementValidator.validatePlacements(originalPlacements, cols = 4, rows = 5)
    assertTrue(result.isValid)

    // Invariant: validator is non-destructive and must not alter or reposition any placement
    assertEquals(3, originalPlacements.size)
    assertEquals("app_1", originalPlacements[0].id)
    assertEquals(0, originalPlacements[0].pageIndex)
    assertEquals(0, originalPlacements[0].positionIndex)

    assertEquals("app_2", originalPlacements[1].id)
    assertEquals(0, originalPlacements[1].pageIndex)
    assertEquals(1, originalPlacements[1].positionIndex)

    assertEquals("folder_1", originalPlacements[2].id)
    assertEquals(0, originalPlacements[2].pageIndex)
    assertEquals(4, originalPlacements[2].positionIndex)
  }
}
