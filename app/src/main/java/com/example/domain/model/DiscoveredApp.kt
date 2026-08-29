package com.example.domain.model

/**
 * Domain entity representing an installed, launchable application discovered via LauncherApps.
 */
data class DiscoveredApp(
  val id: String,
  val packageName: String,
  val activityName: String,
  val label: String,
  val userHandleId: Long = 0L,
  val isSystemApp: Boolean = false,
  val versionName: String = "",
  val installTimeMillis: Long = 0L,
  val lastUpdateTimeMillis: Long = 0L
) {
  val key: String get() = "$packageName/$activityName"
}
