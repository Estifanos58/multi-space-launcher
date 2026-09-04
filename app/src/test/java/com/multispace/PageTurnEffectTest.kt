package com.multispace

import com.multispace.data.entity.SpaceEntity
import com.multispace.domain.model.PageTurnEffect
import com.multispace.domain.model.PlacementCascadeHelper
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.presentation.calculatePageTransformation
import org.junit.Assert.*
import org.junit.Test

class PageTurnEffectTest {

  @Test
  fun testPageTurnEffectEnumValuesAndExclusions() {
    val effects = PageTurnEffect.entries
    assertEquals(5, effects.size)

    val effectNames = effects.map { it.name }
    assertTrue(effectNames.contains("NORMAL"))
    assertTrue(effectNames.contains("CUBE"))
    assertTrue(effectNames.contains("WINDMILL"))
    assertTrue(effectNames.contains("CROSSFADE"))
    assertTrue(effectNames.contains("ZOOM"))

    // Ensure CLASSICAL is excluded
    assertFalse(effectNames.contains("CLASSICAL"))
  }

  @Test
  fun testPageTurnEffectFromString() {
    assertEquals(PageTurnEffect.NORMAL, PageTurnEffect.fromString("NORMAL"))
    assertEquals(PageTurnEffect.NORMAL, PageTurnEffect.fromString("normal"))
    assertEquals(PageTurnEffect.CUBE, PageTurnEffect.fromString("cube"))
    assertEquals(PageTurnEffect.WINDMILL, PageTurnEffect.fromString("WINDMILL"))
    assertEquals(PageTurnEffect.CROSSFADE, PageTurnEffect.fromString("crossfade"))
    assertEquals(PageTurnEffect.ZOOM, PageTurnEffect.fromString("Zoom"))

    // Unknown or invalid strings (including CLASSICAL) must fall back to NORMAL
    assertEquals(PageTurnEffect.NORMAL, PageTurnEffect.fromString("CLASSICAL"))
    assertEquals(PageTurnEffect.NORMAL, PageTurnEffect.fromString("unknown_effect"))
    assertEquals(PageTurnEffect.NORMAL, PageTurnEffect.fromString(null))
  }

  @Test
  fun testSpaceDefaults() {
    val defaultSpace = Space(
      id = "test_space",
      name = "Test Space",
      authPolicy = Space.AUTH_NONE
    )

    assertEquals(PageTurnEffect.NORMAL, defaultSpace.pageTurnEffect)
    assertEquals(Space.DEFAULT_PAGE_TURN_DURATION_MS, defaultSpace.pageTurnDurationMs)
    assertEquals(300, defaultSpace.pageTurnDurationMs)
    assertEquals(Space.DEFAULT_PAGE_TURN_INTENSITY, defaultSpace.pageTurnIntensity, 0.001f)
    assertEquals(1.0f, defaultSpace.pageTurnIntensity, 0.001f)
  }

  @Test
  fun testCubeEffectTransformation() {
    // Page exiting to the left (offset > 0): pivot should be at right edge (1.0f)
    val leftTransform = calculatePageTransformation(
      effect = PageTurnEffect.CUBE,
      pageOffset = 0.5f,
      pageWidth = 1080f,
      intensity = 1.0f
    )
    assertEquals(1.0f, leftTransform.transformOriginX, 0.001f)
    assertEquals(-45f, leftTransform.rotationY, 0.001f)
    assertEquals(8000f, leftTransform.cameraDistanceMultiplier, 0.001f)
    assertTrue("Alpha should slightly diminish as page turns", leftTransform.alpha < 1.0f)

    // Page entering from right (offset < 0): pivot should be at left edge (0.0f)
    val rightTransform = calculatePageTransformation(
      effect = PageTurnEffect.CUBE,
      pageOffset = -0.5f,
      pageWidth = 1080f,
      intensity = 1.0f
    )
    assertEquals(0.0f, rightTransform.transformOriginX, 0.001f)
    assertEquals(45f, rightTransform.rotationY, 0.001f)
    assertEquals(8000f, rightTransform.cameraDistanceMultiplier, 0.001f)
  }

  @Test
  fun testCubeEffectAngleClampingAvoidsSingularMatrix() {
    // At full offset 1.0 and intensity 2.0, angle would be -180 deg if unclamped.
    // To prevent singular/distorted projection matrices, it is clamped to [-89.9, 89.9].
    val extremeLeft = calculatePageTransformation(
      effect = PageTurnEffect.CUBE,
      pageOffset = 1.0f,
      pageWidth = 1080f,
      intensity = 2.0f
    )
    assertTrue(extremeLeft.rotationY >= -89.9f)
    assertTrue(extremeLeft.rotationY <= 89.9f)

    val extremeRight = calculatePageTransformation(
      effect = PageTurnEffect.CUBE,
      pageOffset = -1.0f,
      pageWidth = 1080f,
      intensity = 2.0f
    )
    assertTrue(extremeRight.rotationY >= -89.9f)
    assertTrue(extremeRight.rotationY <= 89.9f)
  }

  @Test
  fun testWindmillEffectTransformation() {
    val transform = calculatePageTransformation(
      effect = PageTurnEffect.WINDMILL,
      pageOffset = 0.5f,
      pageWidth = 1080f,
      intensity = 1.0f
    )
    assertEquals(0.5f, transform.transformOriginX, 0.001f)
    assertEquals(1.0f, transform.transformOriginY, 0.001f)
    assertEquals(-45f, transform.rotationZ, 0.001f)
    assertTrue("Scale should reduce slightly during windmill", transform.scaleX < 1.0f)
  }

  @Test
  fun testCrossfadeEffectTransformation() {
    val transform = calculatePageTransformation(
      effect = PageTurnEffect.CROSSFADE,
      pageOffset = 0.5f,
      pageWidth = 1000f,
      intensity = 1.0f
    )
    // Counter-translation to achieve smooth crossfade
    assertEquals(0.5f * 1000f * 0.85f, transform.translationX, 0.001f)
    assertEquals(0.5f, transform.alpha, 0.001f)
  }

  @Test
  fun testZoomEffectTransformation() {
    val transform = calculatePageTransformation(
      effect = PageTurnEffect.ZOOM,
      pageOffset = 0.5f,
      pageWidth = 1080f,
      intensity = 1.0f
    )
    assertEquals(0.5f, transform.transformOriginX, 0.001f)
    assertEquals(0.5f, transform.transformOriginY, 0.001f)
    assertTrue("Zoom scale should be less than 1.0", transform.scaleX < 1.0f)
    assertTrue("Alpha should diminish in zoom", transform.alpha < 1.0f)
  }

  @Test
  fun testPlacementCascadeHelperIndependenceFromPageEffects() {
    // PlacementCascadeHelper owns System A (app positions, ordering, drop destinations).
    // Verify that cascading behaves purely mathematically without any knowledge of page effects.
    val existing = listOf(
      com.multispace.domain.model.SpaceItemPlacement(
        id = "p1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "pkg1"
      ),
      com.multispace.domain.model.SpaceItemPlacement(
        id = "p2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "pkg2"
      )
    )
    val newItem = com.multispace.domain.model.SpaceItemPlacement(
      id = "p3", spaceId = "s1", pageIndex = 0, positionIndex = 2, packageName = "pkg3"
    )

    val result = PlacementCascadeHelper.cascadeInsert(
      existingPlacements = existing,
      itemToInsert = newItem,
      targetPage = 0,
      targetPosition = 0,
      pageSize = 20
    )

    // p3 at index 0, p1 shifted to 1, p2 shifted to 2
    val p3 = result.first { it.id == "p3" }
    val p1 = result.first { it.id == "p1" }
    val p2 = result.first { it.id == "p2" }

    assertEquals(0, p3.positionIndex)
    assertEquals(1, p1.positionIndex)
    assertEquals(2, p2.positionIndex)
  }

  @Test
  fun testSpaceEntityMappingPreservesPageTurnSettings() {
    val originalSpace = Space(
      id = "cube_space_1",
      name = "Cube Space",
      authPolicy = Space.AUTH_NONE,
      pageTurnEffect = PageTurnEffect.CUBE,
      pageTurnDurationMs = 450,
      pageTurnIntensity = 1.5f
    )

    val entity = SpaceEntity.fromDomain(originalSpace)
    assertEquals("CUBE", entity.pageTurnEffect)
    assertEquals(450, entity.pageTurnDurationMs)
    assertEquals(1.5f, entity.pageTurnIntensity, 0.001f)

    val restoredDomain = entity.toDomain()
    assertEquals(PageTurnEffect.CUBE, restoredDomain.pageTurnEffect)
    assertEquals(450, restoredDomain.pageTurnDurationMs)
    assertEquals(1.5f, restoredDomain.pageTurnIntensity, 0.001f)
  }
}
