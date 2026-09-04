package com.multispace.domain.model

/**
 * Helper object providing size-aware cascading ripple shifting logic for space item placements.
 *
 * Widgets and apps participate in a shared desktop spatial system. Multi-cell widgets reserve
 * a physical footprint (`spanX × spanY`) on the desktop grid. When a widget or app is dropped
 * onto an area occupied by one or more items, all items intersecting the incoming item's
 * physical footprint are displaced. Displaced items ripple forward into subsequent available
 * slots where their own footprints fit without collision, cascading across pages if needed.
 */
object PlacementCascadeHelper {

  /**
   * Spatial footprint descriptor of an item on a grid page.
   */
  data class Footprint(
    val pageIndex: Int,
    val row: Int,
    val col: Int,
    val spanX: Int,
    val spanY: Int,
    val cols: Int,
    val pageSize: Int
  ) {
    val globalSlots: Set<Int> by lazy {
      val slots = mutableSetOf<Int>()
      for (dr in 0 until spanY) {
        for (dc in 0 until spanX) {
          val r = row + dr
          val c = col + dc
          slots.add(pageIndex * pageSize + (r * cols + c))
        }
      }
      slots
    }

    fun intersects(other: Footprint): Boolean {
      if (pageIndex != other.pageIndex) return false
      val overlapX = maxOf(col, other.col) < minOf(col + spanX, other.col + other.spanX)
      val overlapY = maxOf(row, other.row) < minOf(row + spanY, other.row + other.spanY)
      return overlapX && overlapY
    }
  }

  fun getFootprint(
    pageIndex: Int,
    positionIndex: Int,
    spanX: Int,
    spanY: Int,
    cols: Int,
    pageSize: Int
  ): Footprint {
    val safeCols = cols.coerceAtLeast(1)
    val safePageSize = pageSize.coerceAtLeast(safeCols)
    val rows = maxOf(1, safePageSize / safeCols)
    val sX = spanX.coerceIn(1, safeCols)
    val sY = spanY.coerceIn(1, rows)
    val clampedPos = positionIndex.coerceAtLeast(0)
    val rawR = clampedPos / safeCols
    val rawC = clampedPos % safeCols
    val clampedC = rawC.coerceIn(0, maxOf(0, safeCols - sX))
    val clampedR = rawR.coerceIn(0, maxOf(0, rows - sY))
    return Footprint(
      pageIndex = pageIndex.coerceAtLeast(0),
      row = clampedR,
      col = clampedC,
      spanX = sX,
      spanY = sY,
      cols = safeCols,
      pageSize = safePageSize
    )
  }

  /**
   * Performs cascading shift when inserting [itemToInsert] at ([targetPage], [targetPosition]).
   *
   * @param existingPlacements List of all current placements (excluding or including [itemToInsert]).
   * @param itemToInsert The placement being moved or inserted.
   * @param targetPage Target page index (0-based).
   * @param targetPosition Target slot index on the target page (0 until pageSize).
   * @param pageSize Total capacity of a single page (columns * rows).
   * @param cols Number of columns in the grid.
   * @return A list containing only the updated/shifted placements (including [itemToInsert]).
   */
  fun cascadeInsert(
    existingPlacements: List<SpaceItemPlacement>,
    itemToInsert: SpaceItemPlacement,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int,
    cols: Int = 4
  ): List<SpaceItemPlacement> {
    return cascadeInsertGeneric(
      existingItems = existingPlacements,
      itemToInsert = itemToInsert,
      getId = { it.id },
      getPage = { it.pageIndex },
      getPosition = { it.positionIndex },
      copyItem = { placement, page, pos -> placement.copy(pageIndex = page, positionIndex = pos) },
      isSameItem = { a, b ->
        a.id == b.id || (
          !a.isFolder && !a.isWidget && !b.isFolder && !b.isWidget &&
          a.packageName != null && a.packageName == b.packageName
        )
      },
      getSpanX = { if (it.isWidget) it.spanX else 1 },
      getSpanY = { if (it.isWidget) it.spanY else 1 },
      targetPage = targetPage,
      targetPosition = targetPosition,
      pageSize = pageSize,
      cols = cols
    )
  }

  /**
   * Computes the complete updated placement list after dropping [itemToInsert] at ([targetPage], [targetPosition]).
   */
  fun computeFullPlacementsAfterDrop(
    allCurrentPlacements: List<SpaceItemPlacement>,
    itemToInsert: SpaceItemPlacement,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int,
    cols: Int = 4
  ): List<SpaceItemPlacement> {
    val updated = cascadeInsert(allCurrentPlacements, itemToInsert, targetPage, targetPosition, pageSize, cols)
    val updatedMap = updated.associateBy { it.id }
    val result = mutableListOf<SpaceItemPlacement>()

    for (p in allCurrentPlacements) {
      val isSameItem = p.id == itemToInsert.id ||
        (!p.isFolder && !p.isWidget && !itemToInsert.isFolder && !itemToInsert.isWidget &&
          p.packageName != null && p.packageName == itemToInsert.packageName)
      if (isSameItem) continue
      val shifted = updatedMap[p.id]
      if (shifted != null) {
        result.add(shifted)
      } else {
        result.add(p)
      }
    }

    val finalPlaced = updatedMap[itemToInsert.id]
      ?: itemToInsert.copy(pageIndex = targetPage, positionIndex = targetPosition)
    result.add(finalPlaced)

    // Strict deduplication: ensure each app package only appears once, preserving finalPlaced at target
    val deduplicated = mutableListOf<SpaceItemPlacement>()
    val seenPackages = mutableSetOf<String>()
    val seenIds = mutableSetOf<String>()

    // finalPlaced takes precedence to guarantee placement at target
    deduplicated.add(finalPlaced)
    seenIds.add(finalPlaced.id)
    if (!finalPlaced.isFolder && !finalPlaced.isWidget && finalPlaced.packageName != null) {
      seenPackages.add(finalPlaced.packageName!!)
    }

    for (p in result) {
      if (seenIds.contains(p.id)) continue
      if (!p.isFolder && !p.isWidget && p.packageName != null) {
        if (seenPackages.contains(p.packageName)) continue
        seenPackages.add(p.packageName!!)
      }
      seenIds.add(p.id)
      deduplicated.add(p)
    }

    return deduplicated
  }

  /**
   * Generic version of size-aware cascade shift for database entities and domain models.
   */
  fun <T> cascadeInsertGeneric(
    existingItems: List<T>,
    itemToInsert: T,
    getId: (T) -> String,
    getPage: (T) -> Int,
    getPosition: (T) -> Int,
    copyItem: (T, Int, Int) -> T,
    isSameItem: (T, T) -> Boolean = { a, b -> getId(a) == getId(b) },
    getSpanX: (T) -> Int = { 1 },
    getSpanY: (T) -> Int = { 1 },
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int,
    cols: Int = 4
  ): List<T> {
    val safeCols = cols.coerceAtLeast(1)
    val safePageSize = pageSize.coerceAtLeast(safeCols)
    val rows = maxOf(1, safePageSize / safeCols)

    val targetSpanX = getSpanX(itemToInsert).coerceIn(1, safeCols)
    val targetSpanY = getSpanY(itemToInsert).coerceIn(1, rows)
    val targetFp = getFootprint(
      pageIndex = targetPage,
      positionIndex = targetPosition,
      spanX = targetSpanX,
      spanY = targetSpanY,
      cols = safeCols,
      pageSize = safePageSize
    )
    val finalTargetPos = targetFp.row * safeCols + targetFp.col
    val targetGlobalSlots = targetFp.globalSlots

    // Filter out itemToInsert or matching duplicates
    val otherItems = existingItems.filter { !isSameItem(it, itemToInsert) }

    data class PlacedEntry(
      val item: T,
      val page: Int,
      val pos: Int,
      val footprint: Footprint
    )

    val currentPlacements = mutableMapOf<String, PlacedEntry>()
    for (item in otherItems) {
      val p = getPage(item)
      val pos = getPosition(item)
      val fp = getFootprint(p, pos, getSpanX(item), getSpanY(item), safeCols, safePageSize)
      currentPlacements[getId(item)] = PlacedEntry(item, p, pos, fp)
    }

    // Identify colliding items with targetFootprint
    val colliding = mutableListOf<PlacedEntry>()
    for ((_, entry) in currentPlacements) {
      if (entry.footprint.intersects(targetFp) || entry.footprint.globalSlots.any { targetGlobalSlots.contains(it) }) {
        colliding.add(entry)
      }
    }

    val updatedItems = mutableListOf<T>()
    val placedItem = copyItem(itemToInsert, targetPage, finalTargetPos)
    updatedItems.add(placedItem)

    // If no colliding items, return placedItem directly
    if (colliding.isEmpty()) {
      return updatedItems
    }

    // Slots reserved by items placed or locked in this cascade
    val lockedRippleSlots = mutableSetOf<Int>()
    lockedRippleSlots.addAll(targetGlobalSlots)

    // Evict all directly colliding items from current placements and enqueue them
    val queue = ArrayDeque<T>()
    colliding.sortBy { getPage(it.item) * safePageSize + getPosition(it.item) }
    for (entry in colliding) {
      currentPlacements.remove(getId(entry.item))
      queue.add(entry.item)
    }

    var safetyCount = 0
    val maxSafetySteps = (otherItems.size + 15) * safePageSize + 500

    while (queue.isNotEmpty() && safetyCount < maxSafetySteps) {
      safetyCount++
      val item = queue.removeFirst()
      val spanX = getSpanX(item).coerceIn(1, safeCols)
      val spanY = getSpanY(item).coerceIn(1, rows)
      val origGlobalSlot = getPage(item) * safePageSize + getPosition(item)

      // Start looking for the next available slot at or after origGlobalSlot + 1
      var candidateGlobalSlot = origGlobalSlot + 1
      var placed = false

      while (!placed && safetyCount < maxSafetySteps) {
        safetyCount++
        val candPage = candidateGlobalSlot / safePageSize
        val candPos = candidateGlobalSlot % safePageSize
        val candR = candPos / safeCols
        val candC = candPos % safeCols

        // Grid boundary check: entire footprint must fit within page boundaries
        if (candC + spanX <= safeCols && candR + spanY <= rows) {
          val candFp = Footprint(
            pageIndex = candPage,
            row = candR,
            col = candC,
            spanX = spanX,
            spanY = spanY,
            cols = safeCols,
            pageSize = safePageSize
          )
          val candSlots = candFp.globalSlots

          // Must not overlap with itemToInsert or previously locked ripple items
          val collidesWithLocked = candSlots.any { lockedRippleSlots.contains(it) }

          if (!collidesWithLocked) {
            // Check if it overlaps with any unmoved existing items in currentPlacements
            val displacedExisting = mutableListOf<PlacedEntry>()
            for ((_, entry) in currentPlacements) {
              if (entry.footprint.intersects(candFp) || entry.footprint.globalSlots.any { candSlots.contains(it) }) {
                displacedExisting.add(entry)
              }
            }

            // Evict any displaced unmoved items into the queue to ripple forward
            for (d in displacedExisting) {
              currentPlacements.remove(getId(d.item))
              queue.add(d.item)
            }

            // Place the item at candidate position
            val finalPos = candR * safeCols + candC
            val shifted = copyItem(item, candPage, finalPos)
            updatedItems.add(shifted)
            val newEntry = PlacedEntry(shifted, candPage, finalPos, candFp)
            currentPlacements[getId(item)] = newEntry
            lockedRippleSlots.addAll(candSlots)
            placed = true
          }
        }
        candidateGlobalSlot++
      }
    }

    return updatedItems
  }

  /**
   * Result of finding a non-disturbing placement for an added widget.
   */
  data class EmptySlotResult(
    val pageIndex: Int,
    val positionIndex: Int,
    val isNewPage: Boolean
  )

  /**
   * Finds an empty space of size [spanX] x [spanY] on the desktop starting from [preferredPage].
   * If an empty slot of the requested dimensions is found on [preferredPage] or any existing page,
   * returns that slot without disturbing any existing items.
   * If no existing page has sufficient empty space, creates a new page at [currentTotalPages]
   * and returns slot 0 on that new page.
   */
  fun findEmptySlotForWidget(
    existingPlacements: List<SpaceItemPlacement>,
    preferredPage: Int,
    spanX: Int,
    spanY: Int,
    cols: Int,
    pageSize: Int,
    existingPageCount: Int = 1
  ): EmptySlotResult {
    val safeCols = cols.coerceAtLeast(1)
    val safePageSize = pageSize.coerceAtLeast(safeCols)
    val rows = maxOf(1, safePageSize / safeCols)
    val sX = spanX.coerceIn(1, safeCols)
    val sY = spanY.coerceIn(1, rows)

    val maxExistingPageIndex = existingPlacements.maxOfOrNull { it.pageIndex } ?: -1
    val totalPages = maxOf(existingPageCount, maxExistingPageIndex + 1).coerceAtLeast(1)
    val safePreferredPage = preferredPage.coerceIn(0, maxOf(0, totalPages - 1))

    fun findOnPage(page: Int): Int? {
      val onPage = existingPlacements.filter { it.pageIndex == page }
      val occupiedSlots = mutableSetOf<Int>()
      for (item in onPage) {
        val pSpanX = if (item.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) item.spanX else 1
        val pSpanY = if (item.itemType == SpaceItemPlacement.ITEM_TYPE_WIDGET) item.spanY else 1
        val startR = (item.positionIndex / safeCols).coerceAtLeast(0)
        val startC = (item.positionIndex % safeCols).coerceAtLeast(0)
        for (r in startR until minOf(rows, startR + pSpanY)) {
          for (c in startC until minOf(safeCols, startC + pSpanX)) {
            occupiedSlots.add(r * safeCols + c)
          }
        }
      }

      for (r in 0..(rows - sY)) {
        for (c in 0..(safeCols - sX)) {
          var fits = true
          for (dr in 0 until sY) {
            for (dc in 0 until sX) {
              val slot = (r + dr) * safeCols + (c + dc)
              if (occupiedSlots.contains(slot)) {
                fits = false
                break
              }
            }
            if (!fits) break
          }
          if (fits) {
            return r * safeCols + c
          }
        }
      }
      return null
    }

    // 1. Check preferred page first
    val slotOnPreferred = findOnPage(safePreferredPage)
    if (slotOnPreferred != null) {
      return EmptySlotResult(
        pageIndex = safePreferredPage,
        positionIndex = slotOnPreferred,
        isNewPage = false
      )
    }

    // 2. Check remaining existing pages in order (subsequent pages first, then preceding pages)
    val searchOrder = (safePreferredPage + 1 until totalPages) + (0 until safePreferredPage)
    for (page in searchOrder) {
      val slot = findOnPage(page)
      if (slot != null) {
        return EmptySlotResult(
          pageIndex = page,
          positionIndex = slot,
          isNewPage = false
        )
      }
    }

    // 3. Not found on any existing page -> create a new page
    val newPageIndex = totalPages
    return EmptySlotResult(
      pageIndex = newPageIndex,
      positionIndex = 0,
      isNewPage = true
    )
  }
}
