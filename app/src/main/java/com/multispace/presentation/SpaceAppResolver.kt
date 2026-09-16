package com.multispace.presentation

import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppIdentityLookup
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem
import com.multispace.domain.model.SpaceMembership
import com.multispace.domain.model.appIdentity

/**
 * Domain-aware resolver that extracts business and projection logic out of UI Composables.
 * Resolves space-scoped apps, recent and most-used rankings, dynamic folder contents,
 * and Layer 2 alphabetical catalog structures deterministically and testably.
 */
object SpaceAppResolver {

  /**
   * Resolves the list of [DiscoveredApp]s available within the given space based on
   * current lock status, active memberships, and installed application catalog.
   */
  fun resolveSpaceScopedApps(
    allApps: List<DiscoveredApp>,
    activeMemberships: List<SpaceMembership>,
    isCurrentSpaceUnlocked: Boolean,
    activeSpace: Space?
  ): List<DiscoveredApp> {
    if (!isCurrentSpaceUnlocked || allApps.isEmpty()) {
      return emptyList()
    }
    if (activeMemberships.isEmpty() && (activeSpace?.id == Space.DEFAULT_SPACE_ID || activeSpace == null)) {
      return allApps
    }
    if (activeMemberships.isEmpty()) {
      return emptyList()
    }

    val lookup = AppIdentityLookup(allApps)
    val result = mutableListOf<DiscoveredApp>()
    val includedIdentities = mutableSetOf<AppIdentity>()

    for (membership in activeMemberships) {
      val matchedApp = lookup[membership.appIdentity]
      if (matchedApp != null && includedIdentities.add(matchedApp.appIdentity)) {
        result.add(matchedApp)
      }
    }
    return result
  }

  /**
   * Resolves all space-scoped most used apps matching the space's installed and visible catalog.
   */
  fun resolveSpaceScopedMostUsedAppsFull(
    spaceMostUsedApps: List<DiscoveredApp>,
    spaceScopedApps: List<DiscoveredApp>
  ): List<DiscoveredApp> {
    val spaceLookup = AppIdentityLookup(spaceScopedApps)
    val seen = mutableSetOf<AppIdentity>()
    val resolved = mutableListOf<DiscoveredApp>()
    for (app in spaceMostUsedApps) {
      val inSpace = spaceLookup[app.appIdentity]
      if (inSpace != null && seen.add(inSpace.appIdentity)) {
        resolved.add(inSpace)
      }
    }
    return resolved
  }

  /**
   * Resolves space-scoped recent apps bounded by [maxCount].
   */
  fun resolveSpaceScopedRecentApps(
    spaceRecentApps: List<DiscoveredApp>,
    spaceScopedApps: List<DiscoveredApp>,
    maxCount: Int
  ): List<DiscoveredApp> {
    val spaceLookup = AppIdentityLookup(spaceScopedApps)
    val seen = mutableSetOf<AppIdentity>()
    val resolved = mutableListOf<DiscoveredApp>()
    for (app in spaceRecentApps) {
      val inSpace = spaceLookup[app.appIdentity]
      if (inSpace != null && seen.add(inSpace.appIdentity)) {
        resolved.add(inSpace)
        if (resolved.size >= maxCount) break
      }
    }
    return resolved
  }

  /**
   * Resolves active folders, injecting dynamic items into virtual folders like [SpaceFolder.MOST_USED_FOLDER_NAME].
   */
  fun resolveActiveFolders(
    activeFolders: List<SpaceFolder>,
    dynamicMostUsedApps: List<DiscoveredApp>,
    spaceId: String
  ): List<SpaceFolder> {
    return activeFolders.map { folder ->
      if (folder.isMostUsedFolder) {
        val dynamicItems = dynamicMostUsedApps.mapIndexed { index, app ->
          SpaceFolderItem(
            id = "most_used_${spaceId}_${app.packageName}_${app.userHandleId}",
            folderId = folder.id,
            packageName = app.packageName,
            componentName = app.activityName ?: "${app.packageName}.MainActivity",
            userHandleId = app.userHandleId,
            orderIndex = index
          )
        }
        folder.copy(name = SpaceFolder.MOST_USED_FOLDER_NAME, items = dynamicItems)
      } else {
        folder
      }
    }
  }

  /**
   * Builds the pre-indexed alphabetical catalog for Layer 2.
   */
  fun buildLayer2Catalog(spaceScopedApps: List<DiscoveredApp>): Layer2CachedCatalog {
    val sorted = spaceScopedApps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    val grouped = linkedMapOf<Char, MutableList<DiscoveredApp>>()
    val letterToFirst = mutableMapOf<Char, Int>()

    sorted.forEachIndexed { index, app ->
      val cleanLabel = app.label.trim().trim('"', '\'', '(', '[', '{')
      val firstChar = cleanLabel.firstOrNull()?.uppercaseChar() ?: '#'
      val groupKey = if (firstChar in 'A'..'Z') firstChar else '#'
      grouped.getOrPut(groupKey) { mutableListOf() }.add(app)
      if (firstChar in 'A'..'Z' && !letterToFirst.containsKey(firstChar)) {
        letterToFirst[firstChar] = index
      }
    }

    val letterToSection = mutableMapOf<Char, Int>()
    grouped.keys.forEachIndexed { index, char ->
      letterToSection[char] = index
    }

    return Layer2CachedCatalog(
      sortedApps = sorted,
      groupedApps = grouped,
      letterToSectionIndex = letterToSection,
      letterToFirstIndex = letterToFirst,
      activeLetters = letterToFirst.keys
    )
  }
}
