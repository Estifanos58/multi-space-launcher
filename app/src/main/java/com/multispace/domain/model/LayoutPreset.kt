package com.multispace.domain.model

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
  val appTheme: String = Space.THEME_DEFAULT
) {
  companion object {
    val ALL_PRESETS: List<LayoutPreset> = listOf(
      LayoutPreset(
        id = Space.PRESET_ONE_UI,
        name = "Samsung / One UI-inspired",
        description = "Paged curated Home with swipe-up Apps library, 4 columns, 5-app dock with optional drawer button, and balanced spacing.",
        inspiration = "Samsung One UI",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_DEFAULT
      ),
      LayoutPreset(
        id = Space.PRESET_APPLE,
        name = "Apple-inspired",
        description = "Paged Home screens with prominent page indicators, 4-app curated dock, smooth swipe to comprehensive App Library, and rounded icons.",
        inspiration = "Apple iOS",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 4,
        iconSize = Space.ICON_SIZE_LARGE,
        labelVisibility = true,
        appTheme = Space.THEME_PURPLE
      ),
      LayoutPreset(
        id = Space.PRESET_PIXEL,
        name = "Pixel-inspired",
        description = "5-column grid with swipe-up All-Apps library, dynamic theme accents, and a 5-item persistent dock.",
        inspiration = "Google Pixel",
        gridColumns = 5,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_EMERALD
      ),
      LayoutPreset(
        id = Space.PRESET_CLASSIC,
        name = "Classic Android-inspired",
        description = "Traditional 4-column paged desktop with dedicated center Dock button to access the vertically scrolling App Drawer.",
        inspiration = "Stock Android",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_DOCK_BUTTON,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_DEFAULT
      ),
      LayoutPreset(
        id = Space.PRESET_MINIMAL,
        name = "Minimal",
        description = "Distraction-free vertical flow without text labels or heavy docks, featuring clean monochrome styling.",
        inspiration = "Minimalist Design",
        gridColumns = 3,
        layer1DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 3,
        iconSize = Space.ICON_SIZE_SMALL,
        labelVisibility = false,
        appTheme = Space.THEME_MINIMAL
      ),
      LayoutPreset(
        id = Space.PRESET_COMPACT,
        name = "Compact Density",
        description = "High-density 6-column grid with 6-item dock for power users with many applications.",
        inspiration = "Power User",
        gridColumns = 6,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 6,
        iconSize = Space.ICON_SIZE_SMALL,
        labelVisibility = true,
        appTheme = Space.THEME_DARK
      ),
      LayoutPreset(
        id = Space.PRESET_LARGE_ICONS,
        name = "Large Icons",
        description = "Spacious 3-column layout with extra-large icons and high-contrast text for high accessibility.",
        inspiration = "Accessibility / Large Display",
        gridColumns = 3,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2AccessMode = Space.ACCESS_MODE_DOCK_BUTTON,
        dockCapacity = 4,
        iconSize = Space.ICON_SIZE_LARGE,
        labelVisibility = true,
        appTheme = Space.THEME_DEFAULT
      ),
      LayoutPreset(
        id = Space.PRESET_PRODUCTIVITY,
        name = "Productivity",
        description = "Curated multi-page workspace organized for focused task execution with 5-slot dock and crisp contrast.",
        inspiration = "Workplace & Study",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_DOCK_BUTTON,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_OCEAN
      ),
      LayoutPreset(
        id = Space.PRESET_GAMING,
        name = "Gaming",
        description = "4-column dark neon theme with quick swipe-up library access and streamlined dock.",
        inspiration = "Gaming Aesthetics",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP,
        dockCapacity = 4,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_NEON
      ),
      LayoutPreset(
        id = Space.PRESET_DEFAULT,
        name = "Standard Multi-Space",
        description = "Default balanced launcher configuration with paged Home, scrollable App Library, and Dock button.",
        inspiration = "Multi-Space Standard",
        gridColumns = 4,
        layer1DisplayMode = Space.DISPLAY_MODE_PAGE,
        layer2DisplayMode = Space.DISPLAY_MODE_SCROLL,
        layer2AccessMode = Space.ACCESS_MODE_DOCK_BUTTON,
        dockCapacity = 5,
        iconSize = Space.ICON_SIZE_MEDIUM,
        labelVisibility = true,
        appTheme = Space.THEME_DEFAULT
      )
    )

    fun getById(id: String): LayoutPreset {
      return ALL_PRESETS.firstOrNull { it.id == id } ?: ALL_PRESETS.first { it.id == Space.PRESET_DEFAULT }
    }
  }
}
