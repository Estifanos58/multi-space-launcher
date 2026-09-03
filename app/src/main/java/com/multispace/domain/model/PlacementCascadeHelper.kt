package com.multispace.domain.model

/**
 * Helper object providing cascading ripple shifting logic for space item placements.
 *
 * When an app is placed at an occupied slot (targetPage, targetPosition), the occupying
 * app moves to the next position. If that position is also occupied, the collision
 * cascades to subsequent positions. When the page runs out of space (position >= pageSize),
 * the last app moves to position 0 on the next page, iterating continuously until an
 * empty slot is reached.
 */
object PlacementCascadeHelper {

  /**
   * Performs cascading shift when inserting [itemToInsert] at ([targetPage], [targetPosition]).
   *
   * @param existingPlacements List of all current placements (excluding or including [itemToInsert]).
   * @param itemToInsert The placement being moved or inserted.
   * @param targetPage Target page index (0-based).
   * @param targetPosition Target slot index on the target page (0 until pageSize).
   * @param pageSize Total capacity of a single page (columns * rows).
   * @return A list containing only the updated/shifted placements (including [itemToInsert]).
   */
  fun cascadeInsert(
    existingPlacements: List<SpaceItemPlacement>,
    itemToInsert: SpaceItemPlacement,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int
  ): List<SpaceItemPlacement> {
    val safePageSize = pageSize.coerceAtLeast(1)
    val targetPosClamped = targetPosition.coerceIn(0, safePageSize - 1)
    val targetSlot = targetPage * safePageSize + targetPosClamped

    val slotMap = mutableMapOf<Int, SpaceItemPlacement>()
    for (p in existingPlacements) {
      if (p.id != itemToInsert.id) {
        val slot = p.pageIndex * safePageSize + p.positionIndex
        slotMap[slot] = p
      }
    }

    val updatedItems = mutableListOf<SpaceItemPlacement>()

    if (slotMap.containsKey(targetSlot)) {
      var checkSlot = targetSlot
      val chain = mutableListOf<SpaceItemPlacement>()
      val maxIterations = slotMap.size + 10
      var iterations = 0

      while (slotMap.containsKey(checkSlot) && iterations < maxIterations) {
        chain.add(slotMap[checkSlot]!!)
        checkSlot++
        iterations++
      }

      // Shift chain items forward in reverse order
      for (i in chain.indices.reversed()) {
        val item = chain[i]
        val oldSlot = targetSlot + i
        val newSlot = oldSlot + 1
        val newPage = newSlot / safePageSize
        val newPos = newSlot % safePageSize
        updatedItems.add(item.copy(pageIndex = newPage, positionIndex = newPos))
      }
    }

    val placedItem = itemToInsert.copy(pageIndex = targetPage, positionIndex = targetPosClamped)
    updatedItems.add(placedItem)
    return updatedItems
  }

  /**
   * Computes the complete updated placement list after dropping [itemToInsert] at ([targetPage], [targetPosition]).
   */
  fun computeFullPlacementsAfterDrop(
    allCurrentPlacements: List<SpaceItemPlacement>,
    itemToInsert: SpaceItemPlacement,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int
  ): List<SpaceItemPlacement> {
    val updated = cascadeInsert(allCurrentPlacements, itemToInsert, targetPage, targetPosition, pageSize)
    val updatedMap = updated.associateBy { it.id }
    val result = mutableListOf<SpaceItemPlacement>()

    for (p in allCurrentPlacements) {
      if (p.id == itemToInsert.id) continue
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
    return result
  }

  /**
   * Generic version of cascade shift for database entities like SpaceItemPlacementEntity.
   */
  fun <T> cascadeInsertGeneric(
    existingItems: List<T>,
    itemToInsert: T,
    getId: (T) -> String,
    getPage: (T) -> Int,
    getPosition: (T) -> Int,
    copyItem: (T, Int, Int) -> T,
    targetPage: Int,
    targetPosition: Int,
    pageSize: Int
  ): List<T> {
    val safePageSize = pageSize.coerceAtLeast(1)
    val targetPosClamped = targetPosition.coerceIn(0, safePageSize - 1)
    val targetSlot = targetPage * safePageSize + targetPosClamped
    val insertId = getId(itemToInsert)

    val slotMap = mutableMapOf<Int, T>()
    for (item in existingItems) {
      if (getId(item) != insertId) {
        val slot = getPage(item) * safePageSize + getPosition(item)
        slotMap[slot] = item
      }
    }

    val updatedItems = mutableListOf<T>()

    if (slotMap.containsKey(targetSlot)) {
      var checkSlot = targetSlot
      val chain = mutableListOf<T>()
      val maxIterations = slotMap.size + 10
      var iterations = 0

      while (slotMap.containsKey(checkSlot) && iterations < maxIterations) {
        chain.add(slotMap[checkSlot]!!)
        checkSlot++
        iterations++
      }

      for (i in chain.indices.reversed()) {
        val item = chain[i]
        val oldSlot = targetSlot + i
        val newSlot = oldSlot + 1
        val newPage = newSlot / safePageSize
        val newPos = newSlot % safePageSize
        updatedItems.add(copyItem(item, newPage, newPos))
      }
    }

    updatedItems.add(copyItem(itemToInsert, targetPage, targetPosClamped))
    return updatedItems
  }
}
