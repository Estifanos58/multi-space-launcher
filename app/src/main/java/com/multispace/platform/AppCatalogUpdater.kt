package com.multispace.platform

import com.multispace.domain.model.DiscoveredApp

/**
 * Functional updater performing deterministic, profile-safe incremental mutations
 * to the launcher's installed applications catalog.
 */
object AppCatalogUpdater {

  /**
   * Applies an incremental addition or update for a specific package and profile to [currentCatalog].
   * Existing entries matching [packageName] and [userHandleId] are replaced by [newAppsForPackage].
   * If [newAppsForPackage] is empty (e.g. package was disabled), the entries are removed.
   */
  fun applyPackageUpsert(
    currentCatalog: List<DiscoveredApp>,
    newAppsForPackage: List<DiscoveredApp>,
    packageName: String,
    userHandleId: Long? = null
  ): List<DiscoveredApp> {
    val remaining = currentCatalog.filterNot { app ->
      app.packageName == packageName && (userHandleId == null || app.userHandleId == userHandleId)
    }
    val combined = remaining + newAppsForPackage
    return combined.distinctBy { it.id }.sortedWith(
      compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    )
  }

  /**
   * Removes all entries matching [packageName] (and optionally [userHandleId]) from [currentCatalog].
   */
  fun applyPackageRemoval(
    currentCatalog: List<DiscoveredApp>,
    packageName: String,
    userHandleId: Long? = null
  ): List<DiscoveredApp> {
    return currentCatalog.filterNot { app ->
      app.packageName == packageName && (userHandleId == null || app.userHandleId == userHandleId)
    }
  }

  /**
   * Batch upserts multiple packages into the catalog (e.g. onPackagesAvailable).
   */
  fun applyBatchUpsert(
    currentCatalog: List<DiscoveredApp>,
    newApps: List<DiscoveredApp>,
    affectedPackages: Set<String>,
    userHandleId: Long? = null
  ): List<DiscoveredApp> {
    val remaining = currentCatalog.filterNot { app ->
      affectedPackages.contains(app.packageName) && (userHandleId == null || app.userHandleId == userHandleId)
    }
    val combined = remaining + newApps
    return combined.distinctBy { it.id }.sortedWith(
      compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    )
  }
}
