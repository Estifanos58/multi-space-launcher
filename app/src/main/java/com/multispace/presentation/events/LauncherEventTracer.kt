package com.multispace.presentation.events

import com.multispace.diagnostics.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Lightweight, structured event/state tracing mechanism that captures launcher interaction
 * transitions for diagnostics, telemetry, and automated verification.
 */
interface LauncherEventTracer {
  val events: StateFlow<List<LauncherEvent>>
  val latestEvent: StateFlow<LauncherEvent?>
  fun record(event: LauncherEvent)
  fun clear()
}

/**
 * Default bounded, observable implementation of [LauncherEventTracer].
 */
class DefaultLauncherEventTracer(
  private val maxHistory: Int = 100
) : LauncherEventTracer {

  private val _events = MutableStateFlow<List<LauncherEvent>>(emptyList())
  override val events: StateFlow<List<LauncherEvent>> = _events.asStateFlow()

  private val _latestEvent = MutableStateFlow<LauncherEvent?>(null)
  override val latestEvent: StateFlow<LauncherEvent?> = _latestEvent.asStateFlow()

  override fun record(event: LauncherEvent) {
    _latestEvent.value = event
    _events.update { current ->
      (current + event).takeLast(maxHistory)
    }
    AppLogger.d(AppLogger.Category.LAUNCHER, "INTERACTION_EVENT: $event")
  }

  override fun clear() {
    _latestEvent.value = null
    _events.value = emptyList()
  }

  companion object {
    val Global: LauncherEventTracer = DefaultLauncherEventTracer()
  }
}
