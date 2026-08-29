package com.multispace.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.multispace.ui.theme.PrimaryPurple
import com.multispace.ui.theme.PrimaryPurpleDark
import kotlin.math.sqrt

/**
 * High-performance, gesture-driven Pattern Lock Canvas Composable.
 * Supports arbitrary N x M grid layouts (e.g. 2x3, 3x3, 4x4, custom).
 */
@Composable
fun PatternLockCanvas(
  rows: Int = 3,
  cols: Int = 3,
  isError: Boolean = false,
  enabled: Boolean = true,
  clearTrigger: Any? = null,
  onPatternStart: () -> Unit = {},
  onPatternComplete: (selectedNodes: List<Int>, patternString: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  var selectedNodes by remember(rows, cols, clearTrigger) { mutableStateOf<List<Int>>(emptyList()) }
  var currentTouchOffset by remember(rows, cols, clearTrigger) { mutableStateOf<Offset?>(null) }
  var isDrawing by remember(rows, cols, clearTrigger) { mutableStateOf(false) }

  val normalDotColor = Color(0xFF4A4458)
  val normalRingColor = Color(0xFFD0BCFF).copy(alpha = 0.5f)
  val activeColor = if (isError) Color(0xFFD32F2F) else PrimaryPurpleDark
  val activeGlowColor = if (isError) Color(0x33D32F2F) else Color(0x336200EE)
  val lineColor = if (isError) Color(0xFFEF5350) else PrimaryPurpleDark

  fun encodePattern(nodes: List<Int>): String {
    return "PATTERN:${rows}x${cols}:" + nodes.joinToString("-")
  }

  fun getNodeOffset(index: Int, width: Float, height: Float): Offset {
    val col = index % cols
    val row = index / cols

    val cellWidth = width / (cols + 1)
    val cellHeight = height / (rows + 1)

    val x = cellWidth * (col + 1)
    val y = cellHeight * (row + 1)
    return Offset(x, y)
  }

  fun findClosestNode(touch: Offset, width: Float, height: Float, hitRadius: Float): Int? {
    val totalNodes = rows * cols
    for (i in 0 until totalNodes) {
      val center = getNodeOffset(i, width, height)
      val dx = touch.x - center.x
      val dy = touch.y - center.y
      val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
      if (dist <= hitRadius) {
        return i
      }
    }
    return null
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .padding(8.dp)
      .testTag("pattern_lock_canvas"),
    contentAlignment = Alignment.Center
  ) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val canvasWidth = with(density) { maxWidth.toPx() }
    val canvasHeight = with(density) { maxHeight.toPx().coerceAtLeast(canvasWidth * (rows.toFloat() / cols.toFloat())) }
    val hitRadius = maxOf(with(density) { 36.dp.toPx() }, (canvasWidth / (cols + 1)) * 0.48f)

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(with(density) {
          val calculatedHeight = maxWidth * (rows.toFloat() / cols.toFloat()).coerceIn(0.7f, 1.4f)
          calculatedHeight.coerceIn(240.dp, 320.dp)
        })
        .pointerInput(rows, cols, enabled, clearTrigger) {
          if (!enabled) return@pointerInput

          detectDragGestures(
            onDragStart = { offset ->
              isDrawing = true
              onPatternStart()
              val initialNode = findClosestNode(offset, size.width.toFloat(), size.height.toFloat(), hitRadius)
              if (initialNode != null) {
                selectedNodes = listOf(initialNode)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              } else {
                selectedNodes = emptyList()
              }
              currentTouchOffset = offset
            },
            onDragEnd = {
              isDrawing = false
              currentTouchOffset = null
              if (selectedNodes.isNotEmpty()) {
                onPatternComplete(selectedNodes, encodePattern(selectedNodes))
              }
            },
            onDragCancel = {
              isDrawing = false
              currentTouchOffset = null
              if (selectedNodes.isNotEmpty()) {
                onPatternComplete(selectedNodes, encodePattern(selectedNodes))
              }
            },
            onDrag = { change, _ ->
              change.consume()
              val touch = change.position
              currentTouchOffset = touch

              val nearest = findClosestNode(touch, size.width.toFloat(), size.height.toFloat(), hitRadius)
              if (nearest != null && !selectedNodes.contains(nearest)) {
                selectedNodes = selectedNodes + nearest
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
            }
          )
        }
    ) {
      val w = size.width
      val h = size.height
      val totalNodes = rows * cols
      val cellMinDimension = minOf(w / (cols + 1), h / (rows + 1))
      
      val dotRadius = (cellMinDimension * 0.16f).coerceIn(with(density) { 6.dp.toPx() }, with(density) { 12.dp.toPx() })
      val activeRingRadius = (cellMinDimension * 0.38f).coerceIn(with(density) { 18.dp.toPx() }, with(density) { 32.dp.toPx() })
      val normalRingRadius = (cellMinDimension * 0.32f).coerceIn(with(density) { 14.dp.toPx() }, with(density) { 26.dp.toPx() })
      val lineStrokeWidth = (cellMinDimension * 0.08f).coerceIn(with(density) { 4.dp.toPx() }, with(density) { 8.dp.toPx() })

      // 1. Draw Connecting Lines
      if (selectedNodes.size > 1) {
        val path = Path()
        val firstOffset = getNodeOffset(selectedNodes.first(), w, h)
        path.moveTo(firstOffset.x, firstOffset.y)

        for (i in 1 until selectedNodes.size) {
          val nextOffset = getNodeOffset(selectedNodes[i], w, h)
          path.lineTo(nextOffset.x, nextOffset.y)
        }

        drawPath(
          path = path,
          color = lineColor,
          style = Stroke(
            width = lineStrokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
          )
        )
      }

      // 2. Draw line from last selected node to finger touch position
      if (isDrawing && selectedNodes.isNotEmpty() && currentTouchOffset != null) {
        val lastOffset = getNodeOffset(selectedNodes.last(), w, h)
        drawLine(
          color = lineColor.copy(alpha = 0.75f),
          start = lastOffset,
          end = currentTouchOffset!!,
          strokeWidth = lineStrokeWidth * 0.85f,
          cap = StrokeCap.Round
        )
      }

      // 3. Draw Nodes (Dots & Rings)
      for (i in 0 until totalNodes) {
        val center = getNodeOffset(i, w, h)
        val isSelected = selectedNodes.contains(i)

        if (isSelected) {
          // Glow / outer accent disc
          drawCircle(
            color = activeGlowColor,
            radius = activeRingRadius * 1.3f,
            center = center
          )
          // Solid outer accent ring
          drawCircle(
            color = activeColor,
            radius = activeRingRadius,
            center = center,
            style = Stroke(width = lineStrokeWidth * 0.7f)
          )
          // Center selected filled dot
          drawCircle(
            color = activeColor,
            radius = dotRadius * 1.25f,
            center = center
          )
        } else {
          // Normal unselected ring
          drawCircle(
            color = normalRingColor,
            radius = normalRingRadius,
            center = center,
            style = Stroke(width = with(density) { 1.5.dp.toPx() })
          )
          // Normal center dot
          drawCircle(
            color = normalDotColor,
            radius = dotRadius,
            center = center
          )
        }
      }
    }
  }
}
