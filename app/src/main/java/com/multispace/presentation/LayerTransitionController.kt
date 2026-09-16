package com.multispace.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import com.multispace.presentation.gesture.LayerTransitionGestureHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class Layer1SwipeCallbacks(
  val onStart: () -> Unit,
  val onMove: (dragAmount: Float, change: PointerInputChange) -> Unit,
  val onEnd: () -> Unit,
  val onCancel: () -> Unit
)

class LayerTransitionController(
  val coroutineScope: CoroutineScope,
  val onSetLayer: (Int) -> Unit,
  val layer2GridState: LazyGridState,
  val layer2SectionListState: LazyListState,
  initialProgress: Float = 0f
) {
  var layerTransitionProgress by mutableFloatStateOf(initialProgress)
  var isGestureActive by mutableStateOf(false)
  private var settleJob: Job? = null

  var isSectionedAlphabeticalView by mutableStateOf(false)

  val layer1VelocityTracker = VelocityTracker()
  val layer2HeaderVelocityTracker = VelocityTracker()

  val isLayer2OpenOrOpening: Boolean
    get() = layerTransitionProgress > 0f

  fun shouldComposeLayer1(activeLayerIndex: Int): Boolean =
    isGestureActive || layerTransitionProgress < 1f || activeLayerIndex == 1

  fun shouldComposeLayer2(activeLayerIndex: Int): Boolean =
    isGestureActive || layerTransitionProgress > 0f || activeLayerIndex == 2

  fun canDragLayer1(useLayer2: Boolean): Boolean =
    layerTransitionProgress < 1f && useLayer2

  fun animateToLayer(targetLayer: Int) {
    settleJob?.cancel()
    settleJob = coroutineScope.launch {
      isGestureActive = false
      val targetValue = if (targetLayer == 2) 1.0f else 0.0f
      val animatable = Animatable(layerTransitionProgress)
      animatable.animateTo(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
      ) {
        layerTransitionProgress = value
      }
      onSetLayer(targetLayer)
    }
  }

  fun settleTransition(currentProgress: Float, velocityY: Float, screenHeightPx: Float) {
    settleJob?.cancel()
    settleJob = coroutineScope.launch {
      isGestureActive = false
      val targetValue = LayerTransitionGestureHelper.calculateSettleTarget(currentProgress, velocityY)
      val progressVelocity = LayerTransitionGestureHelper.calculateProgressVelocity(velocityY, screenHeightPx)

      val animatable = Animatable(currentProgress)
      animatable.animateTo(
        targetValue = targetValue,
        initialVelocity = progressVelocity.coerceIn(-15f, 15f),
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow
        )
      ) {
        layerTransitionProgress = value
      }

      val targetLayer = if (targetValue == 1.0f) 2 else 1
      onSetLayer(targetLayer)
    }
  }

  fun syncWithExternalLayer(activeLayerIndex: Int) {
    val target = if (activeLayerIndex == 2) 1.0f else 0.0f
    if (!isGestureActive && settleJob?.isActive != true && layerTransitionProgress != target) {
      layerTransitionProgress = target
    }
  }

  fun createHeaderDragModifier(screenHeightPx: Float): Modifier {
    return Modifier.pointerInput(screenHeightPx) {
      detectVerticalDragGestures(
        onDragStart = {
          settleJob?.cancel()
          layer2HeaderVelocityTracker.resetTracking()
          isGestureActive = true
        },
        onDragEnd = {
          isGestureActive = false
          val velocityY = layer2HeaderVelocityTracker.calculateVelocity().y
          settleTransition(layerTransitionProgress, velocityY, screenHeightPx)
        },
        onDragCancel = {
          isGestureActive = false
          settleTransition(layerTransitionProgress, 0f, screenHeightPx)
        },
        onVerticalDrag = { change, dragAmount ->
          if (!isGestureActive) {
            settleJob?.cancel()
            layer2HeaderVelocityTracker.resetTracking()
            isGestureActive = true
          }
          layer2HeaderVelocityTracker.addPosition(change.uptimeMillis, change.position)
          val progressDelta = -dragAmount / screenHeightPx
          layerTransitionProgress = (layerTransitionProgress + progressDelta).coerceIn(0f, 1f)
          change.consume()
        }
      )
    }
  }

  fun createNestedScrollConnection(screenHeightPx: Float): NestedScrollConnection {
    return object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val deltaY = available.y
        val currentProgress = layerTransitionProgress

        // If transition is already in progress (0 < progress < 1), intercept all vertical drags
        if (currentProgress in 0.0001f..0.9999f) {
          settleJob?.cancel()
          isGestureActive = true
          val progressDelta = -deltaY / screenHeightPx
          layerTransitionProgress = (currentProgress + progressDelta).coerceIn(0f, 1f)
          return Offset(0f, deltaY)
        }

        // If Layer 2 is fully open and user drags DOWN while already at top of grid or section list:
        if (currentProgress >= 0.999f && deltaY > 0f) {
          val isGridAtTop = !layer2GridState.canScrollBackward ||
              (layer2GridState.firstVisibleItemIndex == 0 && layer2GridState.firstVisibleItemScrollOffset <= 0)
          val isListAtTop = !layer2SectionListState.canScrollBackward ||
              (layer2SectionListState.firstVisibleItemIndex == 0 && layer2SectionListState.firstVisibleItemScrollOffset <= 0)
          val isAtTop = if (isSectionedAlphabeticalView) isListAtTop else isGridAtTop
          if (isAtTop) {
            settleJob?.cancel()
            isGestureActive = true
            val progressDelta = -deltaY / screenHeightPx
            layerTransitionProgress = (currentProgress + progressDelta).coerceIn(0f, 1f)
            return Offset(0f, deltaY)
          }
        }

        return Offset.Zero
      }

      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
      ): Offset {
        val deltaY = available.y
        val currentProgress = layerTransitionProgress
        if (deltaY > 0f && currentProgress >= 0.999f) {
          settleJob?.cancel()
          isGestureActive = true
          val progressDelta = -deltaY / screenHeightPx
          layerTransitionProgress = (currentProgress + progressDelta).coerceIn(0f, 1f)
          return Offset(0f, deltaY)
        }
        return Offset.Zero
      }

      override suspend fun onPreFling(available: Velocity): Velocity {
        val currentProgress = layerTransitionProgress
        if (isGestureActive || currentProgress in 0.0001f..0.9999f) {
          settleTransition(currentProgress, available.y, screenHeightPx)
          return available
        }
        return Velocity.Zero
      }

      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val currentProgress = layerTransitionProgress
        if (isGestureActive || currentProgress in 0.0001f..0.9999f || (available.y > 0f && currentProgress >= 0.999f)) {
          settleTransition(currentProgress, available.y, screenHeightPx)
          return available
        }
        return Velocity.Zero
      }
    }
  }

  fun createEmptySpaceSwipeCallbacks(screenHeightPx: Float): Layer1SwipeCallbacks {
    return Layer1SwipeCallbacks(
      onStart = {
        settleJob?.cancel()
        layer1VelocityTracker.resetTracking()
        isGestureActive = true
      },
      onMove = { dragAmount, change ->
        if (!isGestureActive) {
          settleJob?.cancel()
          layer1VelocityTracker.resetTracking()
          isGestureActive = true
        }
        layer1VelocityTracker.addPosition(change.uptimeMillis, change.position)
        val progressDelta = LayerTransitionGestureHelper.calculateProgressDelta(dragAmount, screenHeightPx)
        layerTransitionProgress = (layerTransitionProgress + progressDelta).coerceIn(0f, 1f)
      },
      onEnd = {
        isGestureActive = false
        val velocityY = layer1VelocityTracker.calculateVelocity().y
        settleTransition(layerTransitionProgress, velocityY, screenHeightPx)
      },
      onCancel = {
        isGestureActive = false
        settleTransition(layerTransitionProgress, 0f, screenHeightPx)
      }
    )
  }
}

@Composable
fun rememberLayerTransitionController(
  activeLayerIndex: Int,
  onSetLayer: (Int) -> Unit
): LayerTransitionController {
  val coroutineScope = rememberCoroutineScope()
  val layer2GridState = rememberLazyGridState()
  val layer2SectionListState = rememberLazyListState()
  val isSectioned = rememberSaveable { mutableStateOf(false) }

  val controller = remember {
    LayerTransitionController(
      coroutineScope = coroutineScope,
      onSetLayer = onSetLayer,
      layer2GridState = layer2GridState,
      layer2SectionListState = layer2SectionListState,
      initialProgress = if (activeLayerIndex == 2) 1.0f else 0.0f
    )
  }

  controller.isSectionedAlphabeticalView = isSectioned.value

  LaunchedEffect(activeLayerIndex) {
    controller.syncWithExternalLayer(activeLayerIndex)
  }

  return controller
}
