package com.multispace

import android.app.Activity
import android.content.Intent
import com.multispace.domain.model.AppIdentity
import com.multispace.presentation.DragOrigin
import com.multispace.presentation.UnifiedDragState
import com.multispace.presentation.events.*
import com.multispace.presentation.lifecycle.LauncherLifecycleCoordinator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherLifecycleCoordinatorTest {

  private lateinit var activity: Activity
  private lateinit var eventTracer: DefaultLauncherEventTracer

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    eventTracer = DefaultLauncherEventTracer(maxHistory = 50)
  }

  @Test
  fun testIsHomeIntentEvaluation() {
    assertFalse(LauncherLifecycleCoordinator.isHomeIntent(null))
    assertFalse(LauncherLifecycleCoordinator.isHomeIntent(Intent(Intent.ACTION_VIEW)))
    assertFalse(LauncherLifecycleCoordinator.isHomeIntent(Intent(Intent.ACTION_MAIN)))

    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
    }
    assertTrue(LauncherLifecycleCoordinator.isHomeIntent(homeIntent))

    val categoryOnly = Intent().apply {
      addCategory(Intent.CATEGORY_HOME)
    }
    assertTrue(LauncherLifecycleCoordinator.isHomeIntent(categoryOnly))
  }

  @Test
  fun testColdStartLifecycleAndHomeIntentDelivery() {
    var homeIntentReceivedCount = 0
    var homeTriggerSource: HomeTriggerSource? = null

    val coordinator = LauncherLifecycleCoordinator(
      eventTracer = eventTracer,
      onHomeIntent = { source ->
        homeIntentReceivedCount++
        homeTriggerSource = source
      }
    )

    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
    }

    coordinator.onCreate(activity, homeIntent)
    assertTrue(coordinator.isActivityCreated)
    assertTrue(coordinator.pendingHomeIntent)
    assertEquals(1, homeIntentReceivedCount)
    assertEquals(HomeTriggerSource.COLD_START, homeTriggerSource)

    coordinator.onStart(activity, homeIntent)
    assertTrue(coordinator.isActivityStarted)

    var restoredReason: RestorationReason? = null
    val restoringCoordinator = LauncherLifecycleCoordinator(
      eventTracer = eventTracer,
      onRestored = { reason -> restoredReason = reason }
    )
    restoringCoordinator.onCreate(activity, homeIntent)
    restoringCoordinator.onStart(activity, homeIntent)
    restoringCoordinator.onResume(activity, homeIntent)

    assertTrue(restoringCoordinator.isActivityResumed)
    assertFalse(restoringCoordinator.pendingHomeIntent)
    assertEquals(RestorationReason.COLD_START, restoredReason)
  }

  @Test
  fun testAppLaunchDispatchedAndBackgroundTransition() {
    var transientResetCount = 0
    val coordinator = LauncherLifecycleCoordinator(
      eventTracer = eventTracer,
      onResetTransientState = { transientResetCount++ }
    )

    coordinator.onCreate(activity, null)
    coordinator.onStart(activity, null)
    coordinator.onResume(activity, null)

    // Simulate launching an external app
    coordinator.recordAppLaunch("com.example.camera", ".CameraActivity", "default_space")
    assertNotNull(coordinator.lastDispatchedAppLaunch)
    assertEquals("com.example.camera", coordinator.lastDispatchedAppLaunch?.packageName)
    assertEquals(1, transientResetCount)

    // Launcher transitions to background
    coordinator.onPause(activity, null)
    assertFalse(coordinator.isActivityResumed)
    assertEquals(2, transientResetCount)

    // Verify Backgrounded event tagged with APP_LAUNCH
    val backgroundEvent = eventTracer.events.value.filterIsInstance<LauncherEvent.Backgrounded>().lastOrNull()
    assertNotNull(backgroundEvent)
    assertEquals(BackgroundReason.APP_LAUNCH, backgroundEvent?.reason)

    coordinator.onStop(activity, null)
    assertFalse(coordinator.isActivityStarted)
    assertTrue(coordinator.hasBeenBackgrounded)
    assertEquals(3, transientResetCount)
  }

  @Test
  fun testRestorationViaHomeIntentFromBackground() {
    var homeIntentDispatched = false
    var sourceReceived: HomeTriggerSource? = null
    var restoredReason: RestorationReason? = null

    val coordinator = LauncherLifecycleCoordinator(
      eventTracer = eventTracer,
      onHomeIntent = { source ->
        homeIntentDispatched = true
        sourceReceived = source
      },
      onRestored = { reason ->
        restoredReason = reason
      }
    )

    coordinator.onCreate(activity, null)
    coordinator.onStart(activity, null)
    coordinator.onResume(activity, null)
    coordinator.onPause(activity, null)
    coordinator.onStop(activity, null)
    assertTrue(coordinator.hasBeenBackgrounded)

    // User presses HOME while in background: Android delivers onNewIntent before onResume
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
    }
    coordinator.onNewIntent(activity, homeIntent)

    assertTrue(homeIntentDispatched)
    assertEquals(HomeTriggerSource.NEW_INTENT_BACKGROUND, sourceReceived)
    assertTrue(coordinator.pendingHomeIntent)

    // Now activity resumes
    coordinator.onStart(activity, null)
    coordinator.onResume(activity, null)

    assertFalse(coordinator.hasBeenBackgrounded)
    assertFalse(coordinator.pendingHomeIntent)
    assertEquals(RestorationReason.HOME_INTENT, restoredReason)
  }

  @Test
  fun testRestorationViaRecentsFromBackground() {
    var homeIntentDispatched = false
    var restoredReason: RestorationReason? = null

    val coordinator = LauncherLifecycleCoordinator(
      eventTracer = eventTracer,
      onHomeIntent = { _ -> homeIntentDispatched = true },
      onRestored = { reason -> restoredReason = reason }
    )

    coordinator.onCreate(activity, null)
    coordinator.onStart(activity, null)
    coordinator.onResume(activity, null)
    coordinator.onPause(activity, null)
    coordinator.onStop(activity, null)
    assertTrue(coordinator.hasBeenBackgrounded)

    // User switches back via Recents overview -> onNewIntent is NOT called
    coordinator.onStart(activity, null)
    coordinator.onResume(activity, null)

    assertFalse(homeIntentDispatched)
    assertFalse(coordinator.hasBeenBackgrounded)
    assertEquals(RestorationReason.RECENTS_OR_TASK_SWITCH, restoredReason)
  }

  @Test
  fun testHomeKeyDispatchedWhileInForeground() {
    var sourceReceived: HomeTriggerSource? = null

    val coordinator = LauncherLifecycleCoordinator(
      eventTracer = eventTracer,
      onHomeIntent = { source -> sourceReceived = source }
    )

    coordinator.onCreate(activity, null)
    coordinator.onStart(activity, null)
    coordinator.onResume(activity, null)
    assertTrue(coordinator.isActivityResumed)
    assertFalse(coordinator.hasBeenBackgrounded)

    // User taps HOME while already on the launcher surface
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
    }
    coordinator.onNewIntent(activity, homeIntent)

    assertEquals(HomeTriggerSource.NEW_INTENT_FOREGROUND, sourceReceived)
  }

  @Test
  fun testTransientStateResetOnPauseProtectsDragState() {
    val dragState = UnifiedDragState(eventTracer = eventTracer)

    val coordinator = LauncherLifecycleCoordinator(
      eventTracer = eventTracer,
      onResetTransientState = {
        dragState.reset()
      }
    )

    coordinator.onCreate(activity, null)
    coordinator.onStart(activity, null)
    coordinator.onResume(activity, null)

    // Simulate an ongoing drag interaction
    val testApp = com.multispace.domain.model.DiscoveredApp(
      id = "test.pkg/.MainActivity/0",
      packageName = "test.pkg",
      activityName = ".MainActivity",
      label = "Test App",
      isSystemApp = false
    )
    val testPlacement = com.multispace.domain.model.SpaceItemPlacement(
      id = "placement_1",
      spaceId = "default",
      pageIndex = 0,
      positionIndex = 2,
      packageName = "test.pkg",
      componentName = ".MainActivity"
    )

    dragState.startDesktopDrag(
      placement = testPlacement,
      app = testApp,
      pointerPos = androidx.compose.ui.geometry.Offset(100f, 100f)
    )
    assertTrue(dragState.isDragging)
    assertEquals(com.multispace.presentation.DragLifecycleState.DRAGGING, dragState.lifecycleState)

    // Activity is paused (e.g. backgrounded or obscured)
    coordinator.onPause(activity, null)

    // Verify drag state was cleanly cancelled and restored to IDLE
    assertFalse(dragState.isDragging)
    assertEquals(com.multispace.presentation.DragLifecycleState.IDLE, dragState.lifecycleState)
    assertNull(dragState.draggedPlacement)
    assertNull(dragState.activeActionPlacement)
  }
}
