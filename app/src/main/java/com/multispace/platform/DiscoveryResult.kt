package com.multispace.platform

import com.multispace.domain.model.DiscoveredApp

/**
 * Result model representing the outcome of application discovery scans.
 * Prevents silent fallback to fake production data and makes platform state explicit.
 */
sealed class DiscoveryResult {
  data class Success(
    val apps: List<DiscoveredApp>,
    val timestamp: Long = System.currentTimeMillis()
  ) : DiscoveryResult()

  data class Empty(
    val reason: String = "No launchable applications found",
    val timestamp: Long = System.currentTimeMillis()
  ) : DiscoveryResult()

  data class Failure(
    val error: Throwable,
    val timestamp: Long = System.currentTimeMillis()
  ) : DiscoveryResult()
}
