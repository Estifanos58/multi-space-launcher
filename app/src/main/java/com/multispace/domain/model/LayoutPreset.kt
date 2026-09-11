package com.multispace.domain.model

/**
 * Strategy defining the desktop spatial composition of widgets and apps on Layer 1.
 */
enum class PresetStrategy {
  /**
   * Galaxy / One UI inspired: Prominent Weather + Clock (rows 0-1), Search widget (row 2),
   * 4-5 curated apps (row 3), spacious row 4, and persistent dock.
   */
  GALAXY_CURATED,

  /**
   * Pixel / Google inspired: Glanceable At-a-Glance date widget at top (row 0),
   * balanced 2-row app grid (rows 1-2), Search widget at bottom (row 3), and 5-item dock.
   */
  PIXEL_GLANCEABLE,

  /**
   * Classic Android inspired: Digital clock at top (row 0), conventional app-first grid
   * (rows 1-3), and 5-slot dock with center App Drawer button.
   */
  CLASSIC_GRID,

  /**
   * Minimal distraction-free: Typographical clock at top (row 0), wide breathing room
   * (rows 1-2), 3 discrete unlabelled apps (row 3), and 3-item dock.
   */
  MINIMAL_SPARSE,

  /**
   * Productivity dashboard: Dual information widgets (Calendar card + Clock) at top (rows 0-1),
   * Quick Notes notepad (row 2), curated work apps (row 3), and 5-slot dock.
   */
  PRODUCTIVITY_DASHBOARD,

  /**
   * Compact density: High-density 6-column grid with top Search pill (row 0) and
   * dense app grid (rows 1-4) filling the screen with small icons.
   */
  COMPACT_DENSITY
}

/**
 * Encapsulates a cohesive, inspired launcher layout paradigm.
 *
 * @property id Unique preset identifier.
 * @property name User-facing display title.
 * @property description Detailed description of layout characteristics.
 * @property inspiration Tag/reference to the design paradigm.
 * @property gridColumns Default column density.
 * @property layer1DisplayMode Page or Scroll mode for Layer 1.
 * @property layer2DisplayMode Page or Scroll mode for Layer 2.
 * @property layer2AccessMode Dock Button or Swipe Up.
 * @property dockCapacity Number of slots in the persistent Dock.
 * @property iconSize Icon scaling preference (SMALL, MEDIUM, LARGE).
 * @property labelVisibility Whether app labels are visible.
 * @property appTheme Preferred theme styling.
 * @property useLayer2 Whether Layer 2 is enabled for this Space.
 * @property strategy Desktop spatial placement strategy for initial Layer 1 layout.
 */
data class LayoutPreset(
  val id: String,
  val name: String,
  val description: String,
  val inspiration: String,
  val gridColumns: Int = 4,
  val layer1DisplayMode: String = Space.DISPLAY_MODE_PAGE,
  val layer2DisplayMode: String = Space.DISPLAY_MODE_SCROLL,
  val layer2AccessMode: String = Space.ACCESS_MODE_DOCK_BUTTON,
  val dockCapacity: Int = 5,
  val iconSize: String = Space.ICON_SIZE_MEDIUM,
  val labelVisibility: Boolean = true,
  val appTheme: String = Space.THEME_DEFAULT,
  val useLayer2: Boolean = true,
  val strategy: PresetStrategy = PresetStrategy.GALAXY_CURATED
) {
  companion object {
    val ALL_PRESETS: List<LayoutPreset> = listOf(
      LayoutPreset(
        id = Space.PRESET_DEFAULT,
        name = "Galaxy / Modern Curated",
        description = "Glanceable Weather & Clock widget, Quick Search pill, 4–5 priority apps, and a 5-item dock with generous breathing room.",
        inspiration = "Samsung One UI / Galaxy",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_DEFAULT,
        useLayer2 = true,
        strategy = PresetStrategy.GALAXY_CURATED
      ),
      LayoutPreset(
        id = Space.PRESET_PIXEL,
        name = "Pixel / Google-inspired",
        description = "Clean At-a-Glance date widget, balanced 2-row app grid, and bottom Search bar directly above a 5-item dock.",
        inspiration = "Google Pixel",
        gridColumns = 5,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_EMERALD,
        useLayer2 = true,
        strategy = PresetStrategy.PIXEL_GLANCEABLE
      ),
      LayoutPreset(
        id = Space.PRESET_CLASSIC,
        name = "Classic Android",
        description = "Traditional app-first desktop grid with digital clock and dedicated center Dock button to access the scrolling App Drawer.",
        inspiration = "Stock Android",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_DOCK_BUTTON,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_DEFAULT,
        useLayer2 = true,
        strategy = PresetStrategy.CLASSIC_GRID
      ),
      LayoutPreset(
        id = Space.PRESET_MINIMAL,
        name = "Minimal Distraction-Free",
        description = "Ultra-clean typographical time display with expansive negative space, 3 essential unlabelled apps, and a compact 3-slot dock.",
        inspiration = "Minimalist Launcher",
        gridColumns = 3,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 3,
        iconSize = Space.ICON_SIZE_SMALL,
        labelVisibility = false,
        appTheme = Space.THEME_MINIMAL,
        useLayer2 = true,
        strategy = PresetStrategy.MINIMAL_SPARSE
      ),
      LayoutPreset(
        id = Space.PRESET_PRODUCTIVITY,
        name = "Productivity Dashboard",
        description = "Information-dense layout featuring Calendar card, Clock, Quick Notes notepad, and curated task-oriented apps.",
        inspiration = "Workplace & Focus",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_DOCK_BUTTON,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_OCEAN,
        useLayer2 = true,
        strategy = PresetStrategy.PRODUCTIVITY_DASHBOARD
      ),
      LayoutPreset(
        id = Space.PRESET_COMPACT,
        name = "Compact Density",
        description = "High-density 6-column grid with top Search pill and maximum one-tap app access for power users with many applications.",
        inspiration = "Power User Density",
        gridColumns = 6,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 6,
        iconSize = Space.ICON_SIZE_SMALL,
        labelVisibility = true,
        appTheme = Space.THEME_DARK,
        useLayer2 = true,
        strategy = PresetStrategy.COMPACT_DENSITY
      )
    )

    fun getById(id: String): LayoutPreset {
      return ALL_PRESETS.firstOrNull { it.id == id }
        ?: when (id) {
          Space.PRESET_ONE_UI -> ALL_PRESETS.first { it.id == Space.PRESET_DEFAULT }
          Space.PRESET_APPLE -> ALL_PRESETS.first { it.id == Space.PRESET_DEFAULT }
          Space.PRESET_LARGE_ICONS -> ALL_PRESETS.first { it.id == Space.PRESET_CLASSIC }
          Space.PRESET_GAMING -> ALL_PRESETS.first { it.id == Space.PRESET_COMPACT }
          else -> ALL_PRESETS.first { it.id == Space.PRESET_DEFAULT }
        }
    }
  }
}

