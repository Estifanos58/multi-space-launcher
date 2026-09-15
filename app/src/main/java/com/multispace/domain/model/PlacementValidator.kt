package com.multispace.domain.model

/**
 * Centralized reusable placement validation component.
 *
 * Validates desktop/layer placements for structural integrity and flags invalid states:
 * 1. Invalid page indices (e.g. pageIndex < 0)
 * 2. Invalid row/column (e.g. positionIndex < 0, positionIndex exceeding grid capacity, or widget span overflowing page boundary)
 * 3. Duplicate occupied slots (multiple items occupying the exact same slot/footprint on a single page)
 * 4. Invalid or conflicting placement records (missing ID, missing spaceId, missing packageName for apps,
 *    missing folderId for folders, or duplicate placement IDs / duplicate app packages on the same layer)
 *
 * Strictly non-destructive: does NOT automatically mutate, redesign, or reposition layouts.
 */
object PlacementValidator {

  /**
   * Specific category of placement validation issue.
   */
  sealed class IssueType {
    object InvalidPageIndex : IssueType()
    object OutOfBounds : IssueType()
    object InvalidSpan : IssueType()
    object DuplicateOccupiedSlot : IssueType()
    object InvalidRecord : IssueType()
    object DuplicateRecord : IssueType()

    override fun toString(): String = this::class.simpleName ?: "UnknownIssue"
  }

  /**
   * Describes a single issue found during placement validation.
   *
   * @property placementId ID of the placement having the issue.
   * @property type Categorized issue type.
   * @property description Human-readable explanation of the issue.
   * @property pageIndex Page index where the issue occurred.
   * @property positionIndex Slot position index where the issue occurred.
   */
  data class PlacementIssue(
    val placementId: String,
    val type: IssueType,
    val description: String,
    val pageIndex: Int = 0,
    val positionIndex: Int = 0
  )

  /**
   * Complete validation report for a set of placements.
   */
  data class ValidationResult(
    val isValid: Boolean,
    val issues: List<PlacementIssue> = emptyList()
  ) {
    val hasIssues: Boolean get() = issues.isNotEmpty()

    val invalidPageIndexCount: Int get() = issues.count { it.type is IssueType.InvalidPageIndex }
    val outOfBoundsCount: Int get() = issues.count { it.type is IssueType.OutOfBounds }
    val duplicateOccupiedSlotCount: Int get() = issues.count { it.type is IssueType.DuplicateOccupiedSlot }
    val invalidRecordCount: Int get() = issues.count { it.type is IssueType.InvalidRecord || it.type is IssueType.DuplicateRecord }
  }

  /**
   * Validates a collection of placements against grid boundaries and collision rules.
   *
   * @param placements List of placements to validate.
   * @param cols Number of columns in the grid layout (default 4).
   * @param rows Number of rows in the grid layout (default 5).
   * @return [ValidationResult] with detected issues if any.
   */
  fun validatePlacements(
    placements: List<SpaceItemPlacement>,
    cols: Int = Space.DEFAULT_GRID_COLUMNS,
    rows: Int = 5
  ): ValidationResult {
    val safeCols = cols.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val issues = mutableListOf<PlacementIssue>()

    val seenIds = mutableSetOf<String>()
    val seenPackagesByLayer = mutableMapOf<Int, MutableSet<String>>()
    val occupiedSlotsByPageAndLayer = mutableMapOf<Pair<Int, Int>, MutableMap<Int, String>>()

    for (p in placements) {
      // 1. Invalid or conflicting record checks
      if (p.id.isBlank()) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.InvalidRecord,
            description = "Placement record has blank or empty ID",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      } else if (!seenIds.add(p.id)) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.DuplicateRecord,
            description = "Duplicate placement ID found: '${p.id}'",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      if (p.spaceId.isBlank()) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.InvalidRecord,
            description = "Placement '${p.id}' has blank or empty spaceId",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      if (p.itemType == SpaceItemPlacement.ITEM_TYPE_APP && p.packageName.isNullOrBlank()) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.InvalidRecord,
            description = "App placement '${p.id}' is missing required packageName",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      if (p.itemType == SpaceItemPlacement.ITEM_TYPE_FOLDER && p.folderId.isNullOrBlank()) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.InvalidRecord,
            description = "Folder placement '${p.id}' is missing required folderId",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      // Check duplicate app package on the same layer
      if (p.itemType == SpaceItemPlacement.ITEM_TYPE_APP && !p.packageName.isNullOrBlank()) {
        val layerSeen = seenPackagesByLayer.getOrPut(p.layer) { mutableSetOf() }
        if (!layerSeen.add(p.packageName)) {
          issues.add(
            PlacementIssue(
              placementId = p.id,
              type = IssueType.DuplicateRecord,
              description = "Duplicate app package on layer ${p.layer}: '${p.packageName}'",
              pageIndex = p.pageIndex,
              positionIndex = p.positionIndex
            )
          )
        }
      }

      // 2. Invalid page index
      if (p.pageIndex < 0) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.InvalidPageIndex,
            description = "Invalid negative page index: ${p.pageIndex}",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      // 3. Invalid row / column / bounds
      if (p.positionIndex < 0) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.OutOfBounds,
            description = "Invalid negative position index: ${p.positionIndex}",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      val r = if (p.positionIndex >= 0) p.positionIndex / safeCols else -1
      val c = if (p.positionIndex >= 0) p.positionIndex % safeCols else -1

      val sX = if (p.isWidget) p.spanX else 1
      val sY = if (p.isWidget) p.spanY else 1

      if (sX < 1 || sY < 1) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.InvalidSpan,
            description = "Invalid span dimensions: ${sX}x$sY (minimum is 1x1)",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      if (r in 0 until safeRows && c in 0 until safeCols) {
        if (c + sX > safeCols || r + sY > safeRows) {
          issues.add(
            PlacementIssue(
              placementId = p.id,
              type = IssueType.OutOfBounds,
              description = "Footprint exceeds grid bounds: col $c + spanX $sX > $safeCols or row $r + spanY $sY > $safeRows",
              pageIndex = p.pageIndex,
              positionIndex = p.positionIndex
            )
          )
        }
      } else if (p.positionIndex >= 0) {
        issues.add(
          PlacementIssue(
            placementId = p.id,
            type = IssueType.OutOfBounds,
            description = "Position index ${p.positionIndex} exceeds grid capacity (${safeCols}x$safeRows = ${safeCols * safeRows})",
            pageIndex = p.pageIndex,
            positionIndex = p.positionIndex
          )
        )
      }

      // 4. Duplicate occupied slots
      if (p.pageIndex >= 0 && p.positionIndex >= 0 && r in 0 until safeRows && c in 0 until safeCols) {
        val pageOccupancy = occupiedSlotsByPageAndLayer.getOrPut(Pair(p.layer, p.pageIndex)) { mutableMapOf() }
        val effectiveSpanX = sX.coerceIn(1, maxOf(1, safeCols - c))
        val effectiveSpanY = sY.coerceIn(1, maxOf(1, safeRows - r))

        for (dr in 0 until effectiveSpanY) {
          for (dc in 0 until effectiveSpanX) {
            val slot = (r + dr) * safeCols + (c + dc)
            val existingOccupantId = pageOccupancy[slot]
            if (existingOccupantId != null) {
              issues.add(
                PlacementIssue(
                  placementId = p.id,
                  type = IssueType.DuplicateOccupiedSlot,
                  description = "Slot $slot on page ${p.pageIndex} (layer ${p.layer}) is already occupied by '$existingOccupantId'",
                  pageIndex = p.pageIndex,
                  positionIndex = slot
                )
              )
            } else {
              pageOccupancy[slot] = p.id
            }
          }
        }
      }
    }

    return ValidationResult(
      isValid = issues.isEmpty(),
      issues = issues
    )
  }
}
