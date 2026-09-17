package com.multispace.platform

/**
 * Deduplicates rapid, redundant package events emitted by concurrent platform channels
 * (e.g. LauncherApps.Callback and BroadcastReceiver firing for the same package change).
 */
class PackageEventDeduplicator(private val windowMillis: Long = 400L) {

  private val recentEvents = mutableMapOf<String, Long>()

  @Synchronized
  fun shouldProcess(
    event: AppDiscoveryManager.PackageEvent,
    now: Long = System.currentTimeMillis()
  ): Boolean {
    // Evict expired entries older than 2x the debounce window
    val expiryCutoff = now - (windowMillis * 2)
    recentEvents.entries.removeAll { it.value < expiryCutoff }

    val eventCategory = when (event) {
      is AppDiscoveryManager.PackageEvent.Added,
      is AppDiscoveryManager.PackageEvent.Changed -> "UPSERT"
      is AppDiscoveryManager.PackageEvent.Removed -> "REMOVAL"
      is AppDiscoveryManager.PackageEvent.Refreshed -> "REFRESH"
    }

    val key = when (event) {
      is AppDiscoveryManager.PackageEvent.Refreshed -> {
        if (event.packageName.isNotEmpty()) {
          "$eventCategory:${event.packageName}:${event.userHandleId}"
        } else {
          "$eventCategory:all:${event.userHandleId}:${event.count}"
        }
      }
      else -> "$eventCategory:${event.packageName}:${event.userHandleId}"
    }

    val lastRecorded = recentEvents[key]
    if (lastRecorded != null && (now - lastRecorded) < windowMillis) {
      return false
    }

    recentEvents[key] = now
    return true
  }

  @Synchronized
  fun clear() {
    recentEvents.clear()
  }
}
