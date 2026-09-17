package com.multispace.domain.model

import com.multispace.diagnostics.AppLogger

/**
 * Resolved immutable placement layout computed deterministically from active placements,
 * installed apps, and space grid dimensions.
 */
data class ResolvedPlacementLayout(
  val effectivePlacements: List<SpaceItemPlacement>,
  val placementsByPage: Map<Int, List<SpaceItemPlacement>>,
  val widgetSlotsByPage: Map<Int, Set<Int>>,
  val occupiedSlotsByPage: Map<Int, Set<Int>>,
  val maxPageIndex: Int
) {
  fun placementsForPage(page: Int): List<SpaceItemPlacement> =
    placementsByPage[page] ?: emptyList()

  fun isSlotCoveredByWidget(page: Int, slot: Int): Boolean =
    widgetSlotsByPage[page]?.contains(slot) == true

  fun isSlotOccupied(page: Int, slot: Int): Boolean =
    occupiedSlotsByPage[page]?.contains(slot) == true

  fun isCandidateOverWidget(
    targetPage: Int,
    candidateSlot: Int,
    spanX: Int = 1,
    spanY: Int = 1,
    cols: Int,
    gridRows: Int
  ): Boolean {
    val widgetSlots = widgetSlotsByPage[targetPage] ?: return false
    val targetR = candidateSlot / cols
    val targetC = candidateSlot % cols
    for (dr in 0 until spanY) {
      for (dc in 0 until spanX) {
        val r = targetR + dr
        val c = targetC + dc
        if (r in 0 until gridRows && c in 0 until cols) {
          if (widgetSlots.contains(r * cols + c)) return true
        }
      }
    }
    return false
  }

  companion object {
    val EMPTY = ResolvedPlacementLayout(
      effectivePlacements = emptyList(),
      placementsByPage = emptyMap(),
      widgetSlotsByPage = emptyMap(),
      occupiedSlotsByPage = emptyMap(),
      maxPageIndex = 0
    )
  }
}

/**
 * Pure, deterministic placement layout resolver for Layer 1.
 *
 * Guarantees:
 * 1. Honors existing curated placements in Room (widgets, folders, curated apps)
 * 2. Deduplicates app entries with identical AppIdentity
 * 3. Provides clean fallback layout if Room contains no placements (bottom row on Page 0, overflow to Page 1+)
 * 4. Critical invariant: NO APP MAY EVER BE LAID OVER A WIDGET
 * 5. Deterministically relocates colliding apps away from widgets
 * 6. Validates placement integrity with [PlacementValidator]
 * 7. Precomputes indexed lookups by page and slot for O(1) Compose layout queries
 */
object PlacementLayoutResolver {

  fun resolveEffectivePlacements(
    placements: List<SpaceItemPlacement>,
    allApps: List<DiscoveredApp>,
    spaceId: String,
    spaceName: String = "",
    cols: Int,
    gridRows: Int,
    pageSize: Int = (cols * gridRows).coerceAtLeast(1)
  ): ResolvedPlacementLayout {
    val basePlacements = if (placements.isNotEmpty()) {
      val seenAppIdentities = mutableSetOf<AppIdentity>()
      val deduplicated = mutableListOf<SpaceItemPlacement>()
      for (p in placements) {
        if (p.isFolder || p.isWidget) {
          deduplicated.add(p)
        } else {
          val identity = p.appIdentity
          if (identity != null) {
            if (seenAppIdentities.add(identity)) {
              deduplicated.add(p)
            }
          } else {
            deduplicated.add(p)
          }
        }
      }
      deduplicated
    } else {
      // Fallback only if there are absolutely NO placements in Room yet.
      // Default layout: exactly cols apps on Page 0 at the bottom row (lastRow * cols + i)
      val lastRow = (gridRows - 1).coerceAtLeast(0)
      val distinctApps = allApps.distinctBy { it.appIdentity }
      val page0Count = minOf(cols, distinctApps.size)
      val fallbackList = mutableListOf<SpaceItemPlacement>()
      for (i in 0 until page0Count) {
        val app = distinctApps[i]
        fallbackList.add(
          SpaceItemPlacement(
            id = "fallback:${app.id}",
            spaceId = spaceId,
            layer = SpaceItemPlacement.LAYER_HOME,
            pageIndex = 0,
            positionIndex = lastRow * cols + i,
            itemType = SpaceItemPlacement.ITEM_TYPE_APP,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
        )
      }
      for (i in page0Count until distinctApps.size) {
        val app = distinctApps[i]
        val rem = i - page0Count
        fallbackList.add(
          SpaceItemPlacement(
            id = "fallback:${app.id}",
            spaceId = spaceId,
            layer = SpaceItemPlacement.LAYER_HOME,
            pageIndex = 1 + (rem / pageSize),
            positionIndex = rem % pageSize,
            itemType = SpaceItemPlacement.ITEM_TYPE_APP,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
        )
      }
      fallbackList
    }

    // CRITICAL GUARANTEE: NO APP MAY EVER BE LAID ON A WIDGET!
    val widgetSlotsByPage = mutableMapOf<Int, MutableSet<Int>>()
    for (p in basePlacements) {
      if (p.isWidget) {
        val r = (p.positionIndex / cols).coerceIn(0, gridRows - 1)
        val c = (p.positionIndex % cols).coerceIn(0, cols - 1)
        val sX = p.spanX.coerceIn(1, cols - c)
        val sY = p.spanY.coerceIn(1, gridRows - r)
        for (dr in 0 until sY) {
          for (dc in 0 until sX) {
            widgetSlotsByPage.getOrPut(p.pageIndex) { mutableSetOf() }.add((r + dr) * cols + (c + dc))
          }
        }
      }
    }

    val lastRow = (gridRows - 1).coerceAtLeast(0)
    val resolvedList = mutableListOf<SpaceItemPlacement>()
    val occupiedSlotsByPage = mutableMapOf<Int, MutableSet<Int>>()

    // 1. Keep all widgets and folders intact
    for (p in basePlacements) {
      if (p.isWidget || p.isFolder) {
        resolvedList.add(p)
        val r = (p.positionIndex / cols).coerceIn(0, gridRows - 1)
        val c = (p.positionIndex % cols).coerceIn(0, cols - 1)
        val sX = if (p.isWidget) p.spanX.coerceIn(1, cols - c) else 1
        val sY = if (p.isWidget) p.spanY.coerceIn(1, gridRows - r) else 1
        for (dr in 0 until sY) {
          for (dc in 0 until sX) {
            occupiedSlotsByPage.getOrPut(p.pageIndex) { mutableSetOf() }.add((r + dr) * cols + (c + dc))
          }
        }
      }
    }

    // 2. Validate and place apps: never allow an app on a widget
    val apps = basePlacements.filter { !it.isWidget && !it.isFolder }
    for (app in apps) {
      val page = app.pageIndex
      val pos = app.positionIndex
      val isCoveredByWidget = widgetSlotsByPage[page]?.contains(pos) == true
      val isSlotTaken = occupiedSlotsByPage[page]?.contains(pos) == true

      if (!isCoveredByWidget && !isSlotTaken) {
        // Valid placement: can be anywhere the user placed it, as long as it is not on a widget!
        resolvedList.add(app)
        occupiedSlotsByPage.getOrPut(page) { mutableSetOf() }.add(pos)
      } else {
        // Relocate app away from widget or collision
        var placed = false
        if (page == 0) {
          // If on Page 0: default to the bottom row first
          for (c in 0 until cols) {
            val candidatePos = lastRow * cols + c
            val onWidget = widgetSlotsByPage[0]?.contains(candidatePos) == true
            val taken = occupiedSlotsByPage[0]?.contains(candidatePos) == true
            if (!onWidget && !taken) {
              val relocated = app.copy(pageIndex = 0, positionIndex = candidatePos)
              resolvedList.add(relocated)
              occupiedSlotsByPage.getOrPut(0) { mutableSetOf() }.add(candidatePos)
              placed = true
              break
            }
          }
        }
        if (!placed) {
          // Find first available non-widget slot on Page 1 or beyond
          var searchPage = maxOf(1, page)
          var searchPos = 0
          while (!placed) {
            val onWidget = widgetSlotsByPage[searchPage]?.contains(searchPos) == true
            val taken = occupiedSlotsByPage[searchPage]?.contains(searchPos) == true
            if (!onWidget && !taken) {
              val relocated = app.copy(pageIndex = searchPage, positionIndex = searchPos)
              resolvedList.add(relocated)
              occupiedSlotsByPage.getOrPut(searchPage) { mutableSetOf() }.add(searchPos)
              placed = true
            } else {
              searchPos++
              if (searchPos >= pageSize) {
                searchPage++
                searchPos = 0
              }
            }
          }
        }
      }
    }

    val validationReport = PlacementValidator.validatePlacements(resolvedList, cols = cols, rows = gridRows)
    if (validationReport.hasIssues) {
      AppLogger.w(
        AppLogger.Category.LAUNCHER,
        "Placement validation detected ${validationReport.issues.size} issues on desktop layout for space '$spaceName': ${validationReport.issues.take(3)}"
      )
    }

    val placementsByPage = resolvedList.groupBy { it.pageIndex }
    val maxPageIndex = resolvedList.maxOfOrNull { it.pageIndex } ?: 0

    return ResolvedPlacementLayout(
      effectivePlacements = resolvedList,
      placementsByPage = placementsByPage,
      widgetSlotsByPage = widgetSlotsByPage,
      occupiedSlotsByPage = occupiedSlotsByPage,
      maxPageIndex = maxPageIndex
    )
  }
}
