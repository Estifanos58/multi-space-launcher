package com.multispace.presentation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.multispace.domain.model.PageTurnEffect
import kotlin.math.abs

/**
 * Calculated transformation parameters for a single page.
 */
data class PageTransformation(
  val translationX: Float = 0f,
  val scaleX: Float = 1f,
  val scaleY: Float = 1f,
  val rotationY: Float = 0f,
  val rotationZ: Float = 0f,
  val transformOriginX: Float = 0.5f,
  val transformOriginY: Float = 0.5f,
  val cameraDistanceMultiplier: Float = 8000f,
  val alpha: Float = 1f
)

/**
 * Computes deterministic transformation metrics for a given effect, page offset, and configuration.
 *
 * System B: Purely visual presentation layer. Has zero knowledge or authority over app placement,
 * drag state, or layout ordering.
 *
 * @param effect The selected [PageTurnEffect]
 * @param pageOffset The offset of the page relative to the current position (0.0 = centered)
 * @param pageWidth Width of the page in pixels
 * @param intensity Effect intensity multiplier (bounded between 0.5 and 2.0)
 */
fun calculatePageTransformation(
  effect: PageTurnEffect,
  pageOffset: Float,
  pageWidth: Float = 1080f,
  intensity: Float = 1.0f
): PageTransformation {
  if (effect == PageTurnEffect.NORMAL) {
    return PageTransformation()
  }

  val clampedOffset = pageOffset.coerceIn(-1f, 1f)
  val absOffset = abs(clampedOffset)

  if (absOffset < 0.0001f) {
    return PageTransformation()
  }

  val safeIntensity = intensity.coerceIn(0.5f, 2.0f)

  return when (effect) {
    PageTurnEffect.NORMAL -> PageTransformation()

    PageTurnEffect.CUBE -> {
      // 3D Cube effect:
      // When pageOffset > 0, page is moving to the left: pivot on right edge (1.0f)
      // When pageOffset < 0, page is moving to the right: pivot on left edge (0.0f)
      val pivotX = if (clampedOffset > 0f) 1.0f else 0.0f
      // Clamped to (-89.9f, 89.9f) to avoid cos(90 deg) = 0 singular projection matrix
      val rawAngle = -90f * clampedOffset * safeIntensity
      val safeAngle = rawAngle.coerceIn(-89.9f, 89.9f)
      PageTransformation(
        rotationY = safeAngle,
        transformOriginX = pivotX,
        transformOriginY = 0.5f,
        cameraDistanceMultiplier = 8000f,
        alpha = (1f - 0.25f * absOffset).coerceIn(0f, 1f)
      )
    }

    PageTurnEffect.WINDMILL -> {
      // Windmill effect:
      // Rotates around bottom center pivot
      val angle = -90f * clampedOffset * safeIntensity
      val scale = (1f - 0.25f * absOffset).coerceIn(0.6f, 1f)
      PageTransformation(
        rotationZ = angle.coerceIn(-90f, 90f),
        transformOriginX = 0.5f,
        transformOriginY = 1.0f,
        scaleX = scale,
        scaleY = scale,
        cameraDistanceMultiplier = 8000f,
        alpha = (1f - 0.4f * absOffset).coerceIn(0f, 1f)
      )
    }

    PageTurnEffect.CROSSFADE -> {
      // Crossfade effect:
      // Dissolves smoothly while counteracting default translation so pages crossfade gracefully
      val counterTranslation = clampedOffset * pageWidth * 0.85f
      val scale = (1f - 0.08f * absOffset).coerceIn(0.9f, 1f)
      PageTransformation(
        translationX = counterTranslation,
        scaleX = scale,
        scaleY = scale,
        cameraDistanceMultiplier = 8000f,
        alpha = (1f - absOffset).coerceIn(0f, 1f)
      )
    }

    PageTurnEffect.ZOOM -> {
      // Zoom effect:
      // Scale-in/scale-out depth transition centered in the viewport
      val minScale = 0.72f
      val scale = (1f - (1f - minScale) * absOffset * safeIntensity).coerceIn(0.5f, 1f)
      PageTransformation(
        scaleX = scale,
        scaleY = scale,
        transformOriginX = 0.5f,
        transformOriginY = 0.5f,
        cameraDistanceMultiplier = 8000f,
        alpha = (1f - 0.45f * absOffset).coerceIn(0f, 1f)
      )
    }
  }
}

/**
 * Modifier extension that applies the visual [PageTurnEffect] to a page item in a [PagerState].
 * Operates purely as a visual transformation layer, independent of gesture ownership and data placement.
 */
fun Modifier.pageTurnEffect(
  pagerState: PagerState,
  page: Int,
  effect: PageTurnEffect,
  intensity: Float = 1.0f
): Modifier = graphicsLayer {
  if (effect == PageTurnEffect.NORMAL) {
    return@graphicsLayer
  }

  val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
  val absOffset = abs(pageOffset.coerceIn(-1f, 1f))

  if (absOffset < 0.0001f) {
    return@graphicsLayer
  }

  val transform = calculatePageTransformation(
    effect = effect,
    pageOffset = pageOffset,
    pageWidth = size.width,
    intensity = intensity
  )

  this.translationX = transform.translationX
  this.scaleX = transform.scaleX
  this.scaleY = transform.scaleY
  this.rotationY = transform.rotationY
  this.rotationZ = transform.rotationZ
  this.transformOrigin = TransformOrigin(transform.transformOriginX, transform.transformOriginY)
  this.cameraDistance = transform.cameraDistanceMultiplier
  this.alpha = transform.alpha
}
