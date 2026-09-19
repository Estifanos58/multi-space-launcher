package com.multispace.domain.model

import com.multispace.data.entity.SpaceDockItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import java.util.UUID

data class PresetLayoutResult(
  val placements: List<SpaceItemPlacementEntity>,
  val dockItems: List<SpaceDockItemEntity>,
  val folders: List<com.multispace.data.entity.SpaceFolderEntity> = emptyList()
)

/**
 * Builds the initial spatial layout of widgets, curated apps, and dock items
 * for a space based on its [LayoutPreset] strategy and column density.
 */
object PresetLayoutHelper {

  fun buildInitialLayout(
    spaceId: String,
    preset: LayoutPreset,
    gridColumns: Int,
    availableApps: List<DiscoveredApp>,
    dockCapacity: Int = preset.dockCapacity
  ): PresetLayoutResult {
    val cols = gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
    val actualDockCap = dockCapacity.coerceIn(Space.MIN_DOCK_CAPACITY, Space.MAX_DOCK_CAPACITY)

    if (availableApps.isEmpty()) {
      return PresetLayoutResult(emptyList(), emptyList())
    }

    val distinctApps = availableApps.distinctBy { it.appIdentity }

    // 1. Curate Dock Apps
    val dockApps = distinctApps.take(actualDockCap)
    val dockEntities = dockApps.mapIndexed { idx, app ->
      SpaceDockItemEntity(
        id = "dock_${spaceId}_${idx}_${UUID.randomUUID().toString().take(6)}",
        spaceId = spaceId,
        orderIndex = idx,
        packageName = app.packageName,
        componentName = app.activityName ?: "${app.packageName}.MainActivity",
        userHandleId = app.userHandleId
      )
    }

    val dockIdentities = dockApps.map { it.appIdentity }.toSet()
    val desktopPool = distinctApps.filter { it.appIdentity !in dockIdentities }

    val placements = mutableListOf<SpaceItemPlacementEntity>()
    val folders = mutableListOf<com.multispace.data.entity.SpaceFolderEntity>()
    val mostUsedFolderId = SpaceFolder.getMostUsedFolderId(spaceId)
    folders.add(
      com.multispace.data.entity.SpaceFolderEntity(
        id = mostUsedFolderId,
        spaceId = spaceId,
        name = SpaceFolder.MOST_USED_FOLDER_NAME,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
      )
    )

    fun addFolder(
      page: Int,
      row: Int,
      col: Int,
      folderId: String
    ) {
      val pos = row * cols + col
      placements.add(
        SpaceItemPlacementEntity(
          id = "f_${spaceId}_p${page}_r${row}_c${col}_${UUID.randomUUID().toString().take(6)}",
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = page,
          positionIndex = pos,
          itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
          folderId = folderId,
          spanX = 1,
          spanY = 1
        )
      )
    }

    fun addWidget(
      page: Int,
      row: Int,
      col: Int,
      spanX: Int,
      spanY: Int,
      customWidgetType: String
    ) {
      val clampedSpanX = spanX.coerceIn(1, cols - col)
      val clampedSpanY = spanY.coerceIn(1, 5)
      val pos = row * cols + col
      placements.add(
        SpaceItemPlacementEntity(
          id = "w_${spaceId}_p${page}_r${row}_c${col}_${UUID.randomUUID().toString().take(6)}",
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = page,
          positionIndex = pos,
          itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
          spanX = clampedSpanX,
          spanY = clampedSpanY,
          customWidgetType = customWidgetType
        )
      )
    }

    fun addApp(
      page: Int,
      row: Int,
      col: Int,
      app: DiscoveredApp
    ) {
      val pos = row * cols + col
      placements.add(
        SpaceItemPlacementEntity(
          id = "app_${spaceId}_p${page}_r${row}_c${col}_${UUID.randomUUID().toString().take(6)}",
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = page,
          positionIndex = pos,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = app.packageName,
          componentName = app.activityName ?: "${app.packageName}.MainActivity",
          userHandleId = app.userHandleId,
          spanX = 1,
          spanY = 1
        )
      )
    }

    var appCursor = 0

    when (preset.strategy) {
      PresetStrategy.GALAXY_CURATED -> {
        // Page 0:
        // Rows 0-1: Weather + Clock (spanX = cols, spanY = 2)
        addWidget(
          page = 0,
          row = 0,
          col = 0,
          spanX = cols,
          spanY = 2,
          customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
        )
        // Row 2: Search Widget (spanX = cols, spanY = 1)
        addWidget(
          page = 0,
          row = 2,
          col = 0,
          spanX = cols,
          spanY = 1,
          customWidgetType = SpaceItemPlacement.WIDGET_QUICK_SEARCH
        )
        // Row 3: Most Used Apps Dynamic Folder
        addFolder(page = 0, row = 3, col = 0, folderId = mostUsedFolderId)
        // Row 4: Curated apps at the bottom (strictly cols count)
        for (c in 0 until cols) {
          if (appCursor < desktopPool.size) {
            addApp(page = 0, row = 4, col = c, app = desktopPool[appCursor++])
          }
        }
      }

      PresetStrategy.PIXEL_GLANCEABLE -> {
        // Page 0:
        // Row 0: At a Glance (Clock/Date)
        addWidget(
          page = 0,
          row = 0,
          col = 0,
          spanX = cols,
          spanY = 1,
          customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
        )
        // Row 2: Quick Search
        addWidget(
          page = 0,
          row = 2,
          col = 0,
          spanX = cols,
          spanY = 1,
          customWidgetType = SpaceItemPlacement.WIDGET_QUICK_SEARCH
        )
        // Row 3: Most Used Apps Dynamic Folder
        addFolder(page = 0, row = 3, col = 0, folderId = mostUsedFolderId)
        // Row 4: Apps placed strictly on the last row
        for (c in 0 until cols) {
          if (appCursor < desktopPool.size) {
            addApp(page = 0, row = 4, col = c, app = desktopPool[appCursor++])
          }
        }
      }

      PresetStrategy.CLASSIC_GRID -> {
        // Page 0:
        // Row 0: Digital Clock (spanX = cols, spanY = 1)
        addWidget(
          page = 0,
          row = 0,
          col = 0,
          spanX = cols,
          spanY = 1,
          customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
        )
        // Row 3: Most Used Apps Dynamic Folder
        addFolder(page = 0, row = 3, col = 0, folderId = mostUsedFolderId)
        // Row 4: Apps placed strictly on the last row
        for (c in 0 until cols) {
          if (appCursor < desktopPool.size) {
            addApp(page = 0, row = 4, col = c, app = desktopPool[appCursor++])
          }
        }
      }

      PresetStrategy.MINIMAL_SPARSE -> {
        // Page 0:
        // Row 0: Minimal Clock
        addWidget(
          page = 0,
          row = 0,
          col = 0,
          spanX = cols,
          spanY = 1,
          customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
        )
        // Rows 1, 2: empty breathing room
        // Row 3: Most Used Apps Dynamic Folder
        addFolder(page = 0, row = 3, col = 0, folderId = mostUsedFolderId)
        // Row 4: Apps placed strictly on the last row (cols count)
        for (c in 0 until cols) {
          if (appCursor < desktopPool.size) {
            addApp(page = 0, row = 4, col = c, app = desktopPool[appCursor++])
          }
        }
      }

      PresetStrategy.PRODUCTIVITY_DASHBOARD -> {
        // Page 0:
        if (cols >= 4) {
          // Calendar card (2x2) on left
          addWidget(
            page = 0,
            row = 0,
            col = 0,
            spanX = 2,
            spanY = 2,
            customWidgetType = SpaceItemPlacement.WIDGET_CALENDAR
          )
          // Clock & Date (cols - 2 x 2) on right
          addWidget(
            page = 0,
            row = 0,
            col = 2,
            spanX = cols - 2,
            spanY = 2,
            customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
          )
          // Row 2: Quick Notes notepad (spanX = cols, spanY = 1)
          addWidget(
            page = 0,
            row = 2,
            col = 0,
            spanX = cols,
            spanY = 1,
            customWidgetType = SpaceItemPlacement.WIDGET_QUICK_NOTES
          )
          // Row 3: Most Used Folder + Usage Stats Widget
          addFolder(page = 0, row = 3, col = 0, folderId = mostUsedFolderId)
          addWidget(
            page = 0,
            row = 3,
            col = 1,
            spanX = cols - 1,
            spanY = 1,
            customWidgetType = SpaceItemPlacement.WIDGET_USAGE_STATS
          )
          // Row 4: Curated task apps at the bottom
          for (c in 0 until cols) {
            if (appCursor < desktopPool.size) {
              addApp(page = 0, row = 4, col = c, app = desktopPool[appCursor++])
            }
          }
        } else {
          // 3 cols: stacked widgets
          addWidget(
            page = 0,
            row = 0,
            col = 0,
            spanX = 3,
            spanY = 1,
            customWidgetType = SpaceItemPlacement.WIDGET_CALENDAR
          )
          addWidget(
            page = 0,
            row = 1,
            col = 0,
            spanX = 3,
            spanY = 1,
            customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
          )
          addWidget(
            page = 0,
            row = 2,
            col = 0,
            spanX = 3,
            spanY = 1,
            customWidgetType = SpaceItemPlacement.WIDGET_QUICK_NOTES
          )
          addFolder(page = 0, row = 3, col = 0, folderId = mostUsedFolderId)
          // Row 4: Curated task apps at the bottom
          for (c in 0 until cols) {
            if (appCursor < desktopPool.size) {
              addApp(page = 0, row = 4, col = c, app = desktopPool[appCursor++])
            }
          }
        }
      }

      PresetStrategy.COMPACT_DENSITY -> {
        // Page 0:
        // Row 0: Quick Search
        addWidget(
          page = 0,
          row = 0,
          col = 0,
          spanX = cols,
          spanY = 1,
          customWidgetType = SpaceItemPlacement.WIDGET_QUICK_SEARCH
        )
        // Row 3: Most Used Apps Dynamic Folder
        addFolder(page = 0, row = 3, col = 0, folderId = mostUsedFolderId)
        // Row 4: Apps placed strictly on the last row
        for (c in 0 until cols) {
          if (appCursor < desktopPool.size) {
            addApp(page = 0, row = 4, col = c, app = desktopPool[appCursor++])
          }
        }
      }
    }

    // Remaining apps placed on Page 1 and beyond
    val pageSize = cols * 5
    var remainingIndex = 0
    while (appCursor < desktopPool.size) {
      val app = desktopPool[appCursor++]
      val page = 1 + (remainingIndex / pageSize)
      val pos = remainingIndex % pageSize
      placements.add(
        SpaceItemPlacementEntity(
          id = "app_${spaceId}_p${page}_rem${remainingIndex}_${UUID.randomUUID().toString().take(6)}",
          spaceId = spaceId,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = page,
          positionIndex = pos,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = app.packageName,
          componentName = app.activityName ?: "${app.packageName}.MainActivity",
          userHandleId = app.userHandleId,
          spanX = 1,
          spanY = 1
        )
      )
      remainingIndex++
    }

    return PresetLayoutResult(
      placements = placements,
      dockItems = dockEntities,
      folders = folders
    )
  }
}
