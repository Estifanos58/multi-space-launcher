package com.multispace

import androidx.compose.ui.geometry.Offset
import com.multispace.domain.model.AppIdentity
import com.multispace.presentation.DragOrigin
import com.multispace.presentation.DragTargetZone
import com.multispace.presentation.DropTarget
import com.multispace.presentation.LauncherInteractionCoordinator
import com.multispace.presentation.LauncherInteractionState
import com.multispace.presentation.UnifiedDragState
import com.multispace.presentation.events.DefaultLauncherEventTracer
import com.multispace.presentation.events.LauncherEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherEventTracerTest {

  @Test
  fun testTracerRecordsStateChangedOnCoordinatorTransitions() {
    val tracer = DefaultLauncherEventTracer(maxHistory = 10)
    val coordinator = LauncherInteractionCoordinator(
      initialState = LauncherInteractionState.Idle,
      eventTracer = tracer
    )

    coordinator.toPressing(
      identity = AppIdentity("com.test.app", "Activity", 0),
      startPosition = Offset(100f, 200f)
    )

    assertEquals(1, tracer.events.value.size)
    val event = tracer.events.value[0]
    assertTrue(event is LauncherEvent.StateChanged)
    val stateEvent = event as LauncherEvent.StateChanged
    assertEquals(LauncherInteractionState.Idle, stateEvent.previousState)
    assertTrue(stateEvent.newState is LauncherInteractionState.Pressing)

    coordinator.toIdle()
    assertEquals(2, tracer.events.value.size)
    val secondEvent = tracer.events.value[1] as LauncherEvent.StateChanged
    assertTrue(secondEvent.previousState is LauncherInteractionState.Pressing)
    assertEquals(LauncherInteractionState.Idle, secondEvent.newState)
  }

  @Test
  fun testTracerRecordsDragLifecycleEventsInUnifiedDragState() {
    val tracer = DefaultLauncherEventTracer(maxHistory = 20)
    val dragState = UnifiedDragState(eventTracer = tracer)

    // Show context menu
    val identity = AppIdentity("com.pkg.app", "Main", 0)
    dragState.showContextMenu(identity, Offset(50f, 50f), "item_1")

    val menuEvent = tracer.latestEvent.value
    assertTrue(menuEvent is LauncherEvent.ActionMenuOpened)
    val actionEvent = menuEvent as LauncherEvent.ActionMenuOpened
    assertEquals(identity, actionEvent.identity)
    assertEquals("item_1", actionEvent.itemId)

    // Reset dismisses menu
    dragState.reset()
    val dismissEvent = tracer.latestEvent.value
    assertTrue(dismissEvent is LauncherEvent.ActionMenuDismissed)

    // Cancel drag event
    dragState.cancelDrag()
    val cancelEvent = tracer.latestEvent.value
    assertTrue(cancelEvent is LauncherEvent.DragCancelled)

    // Drop event
    val dropTarget = dragState.finishDrop()
    val dropEvent = tracer.latestEvent.value
    assertTrue(dropEvent is LauncherEvent.DragDropped)
    assertEquals(dropTarget, (dropEvent as LauncherEvent.DragDropped).target)
  }

  @Test
  fun testTracerMaintainsBoundedHistoryAndClear() {
    val tracer = DefaultLauncherEventTracer(maxHistory = 5)
    for (i in 1..10) {
      tracer.record(LauncherEvent.LayerTransitionProgress(i.toFloat()))
    }
    assertEquals(5, tracer.events.value.size)
    assertNotNull(tracer.latestEvent.value)

    tracer.clear()
    assertEquals(0, tracer.events.value.size)
    assertEquals(null, tracer.latestEvent.value)
  }
}
