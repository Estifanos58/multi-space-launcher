package com.multispace.presentation

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import com.multispace.domain.model.PageTurnEffect
import kotlin.math.absoluteValue

/**
 * Helper object providing calculations and GPU graphicsLayer transformations
 * for gesture-driven page turn effects.
 */
object PageTurnEffectHelper {

  /**
   * Calculates signed page offset from the current scroll position.
   * - 0f: page is exactly centered in viewport
   * - negative (< 0f): page is towards the left
   * - positive (> 0f): page is towards the right
   */
  fun calculatePageOffset(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    page: Int
  ): Float {
    return (page - currentPage) - currentPageOffsetFraction
  }

  /**
   * Applies the configured page turn effect onto a Composable's GraphicsLayerScope.
   */
  fun applyPageTurnEffect(
    scope: GraphicsLayerScope,
    pageOffset: Float,
    effect: PageTurnEffect,
    intensity: Float = 1.0f,
    density: Float = 1.0f
  ) {
    val clampedIntensity = intensity.coerceIn(0.5f, 1.5f)
    val absOffset = pageOffset.absoluteValue

    when (effect) {
      PageTurnEffect.NORMAL -> {
        // Standard horizontal slide - identity transform
        scope.translationX = 0f
        scope.translationY = 0f
        scope.rotationX = 0f
        scope.rotationY = 0f
        scope.rotationZ = 0f
        scope.scaleX = 1f
        scope.scaleY = 1f
        scope.alpha = 1f
        scope.transformOrigin = TransformOrigin.Center
      }

      PageTurnEffect.CUBE -> {
        val maxAngle = (90f * clampedIntensity).coerceIn(60f, 120f)
        scope.cameraDistance = (8f / clampedIntensity.coerceAtLeast(0.5f)) * density

        if (pageOffset < 0f) {
          // Outgoing page to left or returning from left: pivot on right edge
          scope.transformOrigin = TransformOrigin(1f, 0.5f)
          scope.rotationY = (maxAngle * pageOffset).coerceIn(-maxAngle, 0f)
          scope.alpha = if (pageOffset < -1f) 0f else (1f - 0.2f * absOffset).coerceIn(0f, 1f)
        } else if (pageOffset > 0f) {
          // Incoming page from right or returning to right: pivot on left edge
          scope.transformOrigin = TransformOrigin(0f, 0.5f)
          scope.rotationY = (maxAngle * pageOffset).coerceIn(0f, maxAngle)
          scope.alpha = if (pageOffset > 1f) 0f else (1f - 0.2f * absOffset).coerceIn(0f, 1f)
        } else {
          scope.transformOrigin = TransformOrigin(0.5f, 0.5f)
          scope.rotationY = 0f
          scope.alpha = 1f
        }
        scope.scaleX = 1f
        scope.scaleY = 1f
        scope.rotationZ = 0f
      }

      PageTurnEffect.WINDMILL -> {
        // Fan-style rotation around bottom center pivot
        val maxAngle = (45f * clampedIntensity).coerceIn(25f, 75f)
        scope.transformOrigin = TransformOrigin(0.5f, 1.0f)
        scope.rotationZ = (maxAngle * pageOffset).coerceIn(-maxAngle, maxAngle)
        scope.alpha = if (absOffset > 1f) 0f else (1f - 0.35f * absOffset).coerceIn(0f, 1f)
        scope.rotationY = 0f
        scope.scaleX = 1f
        scope.scaleY = 1f
      }

      PageTurnEffect.CROSSFADE -> {
        // Smooth in-place crossfade by counteracting default horizontal translation
        scope.translationX = -pageOffset * scope.size.width
        scope.alpha = if (absOffset >= 1f) 0f else (1f - absOffset).coerceIn(0f, 1f)
        scope.transformOrigin = TransformOrigin.Center
        scope.rotationY = 0f
        scope.rotationZ = 0f
        scope.scaleX = 1f
        scope.scaleY = 1f
      }

      PageTurnEffect.ZOOM -> {
        // Depth-based zoom with smooth scaling around center
        val minScale = (1f - 0.30f * clampedIntensity).coerceIn(0.50f, 0.85f)
        val scale = if (absOffset >= 1f) minScale else (1f - (1f - minScale) * absOffset).coerceIn(minScale, 1.0f)
        scope.transformOrigin = TransformOrigin.Center
        scope.scaleX = scale
        scope.scaleY = scale
        scope.alpha = if (absOffset >= 1f) 0f else (1f - 0.35f * absOffset).coerceIn(0f, 1f)
        scope.rotationY = 0f
        scope.rotationZ = 0f
      }
    }
  }
}
