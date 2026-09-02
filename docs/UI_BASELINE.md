# Multi-Space Launcher — Complete UI/UX Baseline Audit

**Audit Date:** September 2026  
**Document Version:** 1.0.0-BASELINE  
**Status:** Complete UI/UX & Component Architecture Inventory  
**Target Output Path:** `/docs/UI_BASELINE.md`  

---

## 1. Executive Summary & Audit Overview

### 1.1 Purpose
This document provides an exhaustive, forensic UI/UX baseline audit of the **Multi-Space Launcher** Android application. It captures the exact current state of the application's visual architecture, Jetpack Compose layouts, Material 3 design system implementations, dialog flows, touch gesture systems, wallpaper rendering engines, and styling patterns.

This audit serves as the definitive reference for upcoming modernization, refactoring, and UI/UX redesign initiatives.

### 1.2 Application Essence & Philosophy
Multi-Space Launcher is a specialized Android launcher featuring a **two-tier spatial paradigm**:
1. **Space Isolation Engine:** Multiple isolated user workspaces (e.g., Default, Work, Personal, Media, Gaming, Privacy), backed by Room SQLite persistence. Each Space possesses independent app memberships, layout presets, grid geometry, icon sizing, and dedicated PIN/Pattern security gates.
2. **2-Layer Hierarchy Per Space:**
   - **Layer 1 (Curated Home Surface):** Paged or vertical workspace with draggable app placements, folder creation, quick action bar, page indicator dots, and a persistent customizable dock.
   - **Layer 2 (Space Library / Drawer):** Full alphabetical or category-filtered catalog of all apps assigned to the active Space, with quick search and contextual management.
3. **Dual-Activity Architecture:**
   - `MainActivity`: Serves as the high-priority `CATEGORY_HOME` surface and phone lock container.
   - `ConfigurationActivity`: Runs in an independent task affinity (`com.multispace.configuration`) for dedicated management, space creation, live wallpaper calibration, app discovery diagnostics, and system role acquisition.

---

## 2. System Information & Entry Points

### 2.1 Manifest & Intent Architecture
| Component | Class | Intent Filters / Categories | Launch Mode / Affinity | Key Responsibilities |
|---|---|---|---|---|
| **Home Activity** | `com.multispace.MainActivity` | `ACTION_MAIN`<br>`CATEGORY_HOME`<br>`CATEGORY_DEFAULT` | `singleTask`<br>`clearTaskOnLaunch=true`<br>`stateNotNeeded=true` | Primary launcher surface, hardware Home button capture, screen-off lock receiver, lock screen presentation (`MultiSpaceLockScreen`), and `LauncherHomeScreen` container. |
| **Config Activity** | `com.multispace.ConfigurationActivity` | `ACTION_MAIN`<br>`CATEGORY_LAUNCHER` | `singleTask`<br>`taskAffinity="com.multispace.configuration"` | Space CRUD, Space membership assignment, Wallpaper Live Editor, App Discovery Inspector, System Diagnostics, Home Role acquisition. |
| **Accessibility Bridge** | `com.multispace.platform.MultiSpaceAccessibilityService` | `android.accessibilityservice` | System Bound Service | Non-intrusive invocation of Android's `GLOBAL_ACTION_RECENTS` (Native Recent Apps Overview) without screen scraping. |

### 2.2 Core State & ViewModel Architecture
- **`SpaceViewModel`:** Controls active space selection, active layer index (1 vs 2), room database flows (`allSpaces`, `activeSpace`, `activeMemberships`, `activePlacements`, `activeFolders`, `activeDockItems`, `unlockedSpaceIds`), phone lock state (`isPhoneLocked`), PIN/Pattern verification, and layout preset transitions.
- **`AppDiscoveryViewModel`:** Manages asynchronous package discovery via `LauncherApps` and `PackageManager`, real-time icon extraction into memory bitmaps, category grouping, search querying, and application intent launching.

---

## 3. Color System & Palettes (Current State)

### 3.1 Color Definitions (`com.multispace.ui.theme.Color.kt`)
The current color palette mixes custom branded tokens with Material 3 standard tokens:

```kotlin
// Primary Brand Palette
val PrimaryPurple         = Color(0xFF6200EE)
val PrimaryPurpleLight    = Color(0xFFBB86FC)
val PrimaryPurpleDark     = Color(0xFF3700B3)
val PrimaryPurpleMuted    = Color(0xFF7C4DFF)
val PrimaryContainerBadge = Color(0xFFEDE7F6)
val PrimaryContainerLight = Color(0xFFF3E5F5)

// Functional Neutral Palette (Light Surface)
val LightBackground           = Color(0xFFF8F9FA)
val LightSurfaceContainer     = Color(0xFFFFFFFF)
val LightSurfaceContainerLow  = Color(0xFFF1F3F4)
val LightSurfaceContainerHigh = Color(0xFFE8EAED)

// Terminal / Diagnostic Surface (Dark Monospace)
val DarkTerminalBackground = Color(0xFF1E1E1E)
val DarkTerminalSurface    = Color(0xFF252526)
val DarkTerminalText       = Color(0xFFD4D4D4)
val DarkTerminalAccent     = Color(0xFF4EC9B0)
val DarkTerminalWarning    = Color(0xFFCE9178)
val DarkTerminalError      = Color(0xFFF44747)

// Status & Semantic Tokens
val StatusGreen  = Color(0xFF4CAF50)
val StatusOrange = Color(0xFFFF9800)
val StatusRed    = Color(0xFFF44336)
val StatusBlue   = Color(0xFF2196F3)

// Text Tokens
val TextPrimary   = Color(0xFF1C1B1F)
val TextSecondary = Color(0xFF49454F)
val TextMuted     = Color(0xFF79747E)
```

### 3.2 Theme Configuration (`com.multispace.ui.theme.Theme.kt`)
- **Dynamic Color:** Enabled on Android 12+ (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`) via `dynamicLightColorScheme` and `dynamicDarkColorScheme`.
- **Static Light Fallback:**
  - `primary = PrimaryPurple` (`#6200EE`)
  - `secondary = PrimaryPurpleLight` (`#BB86FC`)
  - `tertiary = PrimaryPurpleDark` (`#3700B3`)
  - `background = LightBackground` (`#F8F9FA`)
  - `surface = LightSurfaceContainer` (`#FFFFFF`)
- **Static Dark Fallback:**
  - `primary = PrimaryPurpleLight` (`#BB86FC`)
  - `secondary = PrimaryPurpleMuted` (`#7C4DFF`)
  - `tertiary = PrimaryPurple` (`#6200EE`)
  - `background = DarkTerminalBackground` (`#1E1E1E`)
  - `surface = DarkTerminalSurface` (`#252526`)

### 3.3 Hardcoded Color Usages (Identified Visual Inconsistencies)
Across presentation composables, several hardcoded color hex values bypass theme tokens:
1. **Security & PIN UI (`PatternLockCanvas.kt`, `SpaceManagementComponents.kt`):**
   - Green Lock badge: `Color(0xFF2E7D32)`
   - Orange Warning / Disable: `Color(0xFFE65100)`
   - Red Destructive / Delete: `Color(0xFFC62828)`, `Color(0xFFD32F2F)`, `Color(0xFFEF5350)`
   - Dark nodes: `Color(0xFF4A4458)`, `Color(0xFFD0BCFF)`
2. **Lock Screen Surface (`MultiSpaceLockScreen.kt`):**
   - Gradient Slate Dark: `Color(0xFF0F172A)` to `Color(0xFF1E293B)`
   - Error container: `Color(0xFFEF4444).copy(alpha = 0.2f)`
3. **Wallpaper Presets Canvas (`LayoutPresetVisualPreview.kt`):**
   - Over 40 distinct gradient hex stops (e.g. `Color(0xFF2E1065)`, `Color(0xFF1E1B4B)`, `Color(0xFF064E3B)`, `Color(0xFF09090B)`, `Color(0xFF18181B)`).

---

## 4. Typography & Text Hierarchy

### 4.1 Typography Definition (`com.multispace.ui.theme.Type.kt`)
Jetpack Compose Material 3 standard type scale:
- `bodyLarge`: `FontFamily.Default`, `FontWeight.Normal`, `16.sp`, `24.sp` line height
- `titleLarge`: `FontFamily.Default`, `FontWeight.Normal`, `22.sp`, `28.sp` line height
- `labelSmall`: `FontFamily.Default`, `FontWeight.Medium`, `11.sp`, `16.sp` line height

### 4.2 Observed Typography Usage
| Level | Font Size / Weight | Family | Typical UI Usage |
|---|---|---|---|
| **Hero Digital Clock** | `68.sp` – `76.sp`, `FontWeight.Bold` / `Light` | Sans-Serif | `MultiSpaceLockScreen` Lock Clock Display |
| **Screen Headlines** | `20.sp` – `24.sp`, `FontWeight.Bold` | Default / Sans-Serif | TopAppBars, Header Titles |
| **Section Labels** | `10.sp` – `12.sp`, `FontWeight.Bold` | Default | Badges, Uppercase Category Headers |
| **Diagnostic Terminal** | `10.sp` – `12.sp`, `FontWeight.Normal` | `FontFamily.Monospace` | Log stream in `LauncherDiagnosticsScreen`, Space IDs |
| **App Icon Labels** | `11.sp` – `12.sp`, `FontWeight.Medium` | Default | Grid and Dock labels with single-line truncation |

---

## 5. Elevation, Shapes & Corner Radii Inventory

### 5.1 Corner Radii Inventory
The codebase exhibits several distinct corner radii patterns:
- **Small Badges / Chips:** `4.dp`, `6.dp` (`RoundedCornerShape(6.dp)`)
- **Buttons & Action Pills:** `8.dp`, `10.dp`, `12.dp`
- **Cards & List Items:** `14.dp`, `16.dp`
- **Dialog Containers:** `20.dp`, `24.dp`
- **Preset Preview Phone Frames:** `22.dp`
- **Dock Bar Container:** `24.dp`
- **Floating Badges / Quick Floating Buttons:** `CircleShape` (`50%` / 999.dp)

### 5.2 Elevation & Shadow Patterns
- Standard Cards: `1.dp` default tonal elevation, `4.dp` when active/selected
- Preset Phone Preview: `3.dp` unselected, `8.dp` selected shadow
- Dock Bar: Tonal elevation `3.dp` via `Surface`
- Removal Bin / Bucket Bar: Dynamic spring scale `1.0f` to `1.2f` when dragged items hover

---

## 6. Layouts & Window Insets Management

### 6.1 Edge-to-Edge Compliance
- Both `MainActivity` and `ConfigurationActivity` invoke `enableEdgeToEdge()` in `onCreate()`.
- Top Bars utilize `.statusBarsPadding()` or `WindowInsets.statusBars`.
- Dock and bottom components utilize `.navigationBarsPadding()`.

### 6.2 Contrast & Wallpaper Scrimming
In `LauncherHomeScreen.kt`:
- Evaluates dynamic luminance of the active space's background color (`Color(currentBgColor).luminance() < 0.45f`).
- Automatically switches top bar icon tinting and text color between `Color.White` (dark backgrounds/photos) and `TextPrimary` (light backgrounds).
- Applies a customizable dark scrim overlay (`Color.Black.copy(alpha = currentDimLevel)`) over custom photographic wallpapers to maintain legibility.

---

## 7. Navigation Architecture & Screen Flow

### 7.1 Architecture Diagram
```
[Android OS Boot / Home Press]
              │
              ▼
   ┌──────────────────────┐
   │     MainActivity     │
   └──────────┬───────────┘
              │
      ┌───────┴──────────────────┐
      ▼                          ▼
[Phone Locked]            [Phone Unlocked]
MultiSpaceLockScreen      LauncherHomeScreen
                                 │
                 ┌───────────────┴───────────────┐
                 ▼                               ▼
       [Layer 1: Home Grid]            [Layer 2: Space Library]
       - Paged/Vertical Workspace      - Full Alphabetical List/Grid
       - Folders & Placements          - Instant App Search
       - Space Dock Bar                - Context Actions (Pin/Dock)
```

```
[App Drawer / Settings Action]
              │
              ▼
   ┌───────────────────────────┐
   │   ConfigurationActivity   │
   │ (taskAffinity = config)   │
   └─────────────┬─────────────┘
                 │
  ┌──────────────┼──────────────┬──────────────┬──────────────┐
  ▼              ▼              ▼              ▼              ▼
Launcher-      CreateSpace-   AppCatalog-    Launcher-      Wallpaper-
Configuration  Screen         Screen         Diagnostics    EditorScreen
Screen         (Edit/Create)  (Discovery)    Screen (Logs)  (Live Preview)
```

---

## 8. Comprehensive Screen-by-Screen Inventory

### 8.1 `LauncherHomeScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/LauncherHomeScreen.kt`
- **Visual Structure:**
  - Full-screen wallpaper canvas (Color / Image Uri / Dim overlay / Offset translation).
  - Status-bar-padded Header Row with Space Switcher Pill (`Surface`, dropdown arrow, lock status).
  - Top Action Icons: Native Recents (`Icons.Default.GridView`), Quick Lock (`Icons.Default.Lock`), Settings (`Icons.Default.Settings`).
  - Animated Layer Switcher (`AnimatedContent` with vertical slide and fade transitions).
  - Bottom Bar: Floating `SpaceDockBar` in Layer 1.
- **States:**
  - `Protected Space Locked`: Centered lock icon, message, and "Enter PIN" button.
  - `Loading Catalog`: Centered circular progress indicator.
  - `Empty Space`: Explanatory message with "Add Apps" and "Import Layout" actions.
  - `Active Space Layer 1 / Layer 2`: Main working surfaces.

### 8.2 `Layer1HomeScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/Layer1HomeScreen.kt`
- **Visual Structure:**
  - Multi-page `HorizontalPager` or Single Scroll Grid depending on preset.
  - Page indicator dots (`PageIndicatorDots`) positioned below workspace grid.
  - Drag-and-drop workspace supporting app icon movement, reordering, folder creation via app-on-app drop, and removal via top `RemovalBucketBar`.
  - Grid cell layout: Icon with customizable size (36dp to 64dp) and optional single-line label.

### 8.3 `Layer2LibraryScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/Layer2LibraryScreen.kt`
- **Visual Structure:**
  - Frosted / elevated surface overlay with swipe-down dismiss gesture and close button.
  - Search Header with instant text filtering.
  - Category / Letter fast scroller.
  - Grid or List view of all apps belonging to the active Space.
  - Contextual Long-Press Sheet: Add to Home Page, Pin to Dock, App Info, Remove from Space.

### 8.4 `MultiSpaceLockScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/MultiSpaceLockScreen.kt`
- **Visual Structure:**
  - Full-screen dark gradient slate background (`#0F172A` to `#1E293B`).
  - Centered Time Display (`68.sp`, Bold) and Date Display.
  - Authentication Mode Switcher (PIN Numpad vs Gesture Pattern Canvas).
  - PIN Input: Visual masked dots with shake animation on error.
  - Numeric Keypad: 3x4 grid with circular keys, ripple feedback, biometric / delete actions.
  - Pattern Input: Full interactive `PatternLockCanvas` with gesture connecting lines.

### 8.5 `LauncherConfigurationScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/LauncherConfigurationScreen.kt`
- **Visual Structure:**
  - TopAppBar with title "Multi-Space Launcher" and subtitle.
  - System Home Role Banner: Green verified badge if Default Home, amber warning button if Not Default.
  - Space Management Section: List of all spaces with active indicator, Space ID, app count, and action buttons (`Apps`, `Style`, `PIN`, `Rename`, `Delete`).
  - Layout & Behavior Settings Cards: Grid density, dock capacity, gesture actions, system recents bridge.
  - Navigation links to `Diagnostics` and `App Catalog`.

### 8.6 `CreateSpaceScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/CreateSpaceScreen.kt`
- **Visual Structure:**
  - 3-Step Wizard:
    1. **Basics:** Space name, icon, preset selector.
    2. **App Assignment:** Multi-selection catalog with search, "Select All", and category filters.
    3. **Customization & Security:** Grid columns, dock slots, wallpaper color/image, PIN protection toggle.
  - Sticky bottom action bar with "Cancel" and "Save Space".

### 8.7 `CustomGridScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/CustomGridScreen.kt`
- **Visual Structure:**
  - Interactive grid calibration sliders (Columns: 2 to 7, Rows: 3 to 8, Icon Size: 36dp to 72dp).
  - Real-time miniature preview canvas showing changes dynamically.

### 8.8 `WallpaperEditorScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/WallpaperEditorScreen.kt`
- **Visual Structure:**
  - Interactive live viewport with dual-finger pinch-to-zoom, pan, and rotate gestures.
  - Control Drawer: Solid Color Picker palette, Image URI selector (Photo Picker), Dimming level slider (0% to 80%), Scale Mode toggle (Crop vs Fit).
  - Live Launcher Preview overlay with mockup icons to check readability before saving.

### 8.9 `LauncherDiagnosticsScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/LauncherDiagnosticsScreen.kt`
- **Visual Structure:**
  - Monospaced dark terminal theme (`#1E1E1E`).
  - Real-time rolling log output with category color tags (`LIFECYCLE`, `LAUNCHER`, `DATABASE`, `SECURITY`).
  - Quick action toolbar: Clear Logs, Share/Export Logs, Trigger System Checks, Re-scan LauncherApps.

### 8.10 `AppCatalogScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/AppCatalogScreen.kt`
- **Visual Structure:**
  - Inspector tool for all discovered Android packages on device.
  - Displays Package Name, Main Activity Component, User Handle (Work Profile / Personal), Icon Load State, and App Flags.

### 8.11 `FoundationOverviewScreen.kt`
- **Location:** `/app/src/main/java/com/multispace/presentation/FoundationOverviewScreen.kt`
- **Visual Structure:**
  - Architecture documentation viewer rendering key platform guarantees, persistence rules, and security scopes.

---

## 9. Reusable Component Inventory

| Component | File Path | Key Props / Parameters | Description & Visual Characteristics |
|---|---|---|---|
| **`SpaceDockBar`** | `SpaceDockBar.kt` | `dockItems`, `allApps`, `capacity`, `accessMode`, `getBitmap`, `onLaunchApp`, `onOpenLayer2`, `onRemoveFromDock` | Persistent floating dock with rounded pill container (`24.dp`), dynamic slot count (3 to 6 apps), and optional center App Drawer / Layer 2 button. |
| **`FolderDialog`** | `FolderDialog.kt` | `folder`, `allApps`, `getBitmap`, `onLaunchApp`, `onRenameFolder`, `onRemoveItem`, `onDeleteFolder` | Full modal dialog displaying apps contained in a home folder, supporting folder title inline renaming, item launching, and item removal. |
| **`PatternLockCanvas`** | `PatternLockCanvas.kt` | `rows`, `cols`, `isError`, `enabled`, `clearTrigger`, `onPatternStart`, `onPatternComplete` | Custom Canvas rendering N x M touch grid nodes, path lines with rounded caps/joins, active outer glow discs, and haptic vibration feedback. |
| **`LayoutPresetVisualCard`** | `LayoutPresetVisualPreview.kt` | `preset`, `isSelected`, `onSelect` | Card containing miniature smartphone mockup canvas (`LayoutPresetPhonePreview`) showcasing preset wallpaper, widgets, app grid, and dock structure. |
| **`PageIndicatorDots`** | `PageIndicatorDots.kt` | `pageCount`, `currentPage`, `onDotClick` | Horizontal animated pill indicator expanding active page width from 6.dp to 20.dp with color tweening. |
| **`RemovalBucketBar`** | `RemovalBucketBar.kt` | `isVisible`, `isHovered`, `onPositioned` | Floating top circle icon (`54.dp`) with animated slide-in and spring scale (1.2x) when dragging apps toward removal zone. |

---

## 10. Modal & Dialog Systems Inventory

### 10.1 Dialog Directory
1. **`SetSpacePinDialog`** (`SpacePinDialogs.kt`): PIN setup with confirmation match validation.
2. **`ChangeSpacePinDialog`** (`SpacePinDialogs.kt`): Current PIN validation before entering new PIN.
3. **`DisableSpacePinDialog`** (`SpacePinDialogs.kt`): Security confirmation to remove PIN protection.
4. **`SpaceCredentialVerificationDialog`** (`SpacePinDialogs.kt`): Generic challenge dialog for secured actions.
5. **`SpaceUnlockDialog`** (`SpacePinDialogs.kt`): Modal dialog to unlock a space during switcher selection.
6. **`SpaceCustomizationDialog`** (`SpaceCustomizationDialog.kt`): Tabbed modal for live wallpaper, grid columns (2–6), icon sizing (Small/Medium/Large), and label visibility.
7. **`CreateSpaceDialog`** (`SpaceManagementComponents.kt`): Simple text prompt for new Space naming.
8. **`RenameSpaceDialog`** (`SpaceManagementComponents.kt`): Inline text dialog to rename existing Space.
9. **`DeleteSpaceDialog`** (`SpaceManagementComponents.kt`): Safety confirmation alert preventing deletion of the sole remaining Space.
10. **`ManageMembershipsDialog`** (`SpaceManagementComponents.kt`): Full-height 90% screen dialog with search bar, "Select All", and checkbox list to assign apps to spaces.
11. **`ImportLayoutDialog`** (`ImportLayoutDialog.kt`): Migration assistant with progress state and post-import summary report.
12. **`PresetSelectionDialog`** (`PresetSelectionDialog.kt`): Modal preset picker showcasing visual smartphone previews.
13. **`NativeRecentsDisclosureDialog`** (`NativeRecentsDisclosureDialog.kt`): Google Play-compliant privacy disclosure before opening Android Accessibility Settings.

---

## 11. Gestures, Touch Interactions & Drag-and-Drop

### 11.1 Gesture Inventory
- **Swipe-Up on Home:** Transitions active workspace from Layer 1 (Curated Home) to Layer 2 (Space Library).
- **Swipe-Down on Layer 2:** Collapses Layer 2 and returns to Layer 1.
- **Long-Press on App Icon (Layer 1):** Initiates drag-and-drop mode, displaying `RemovalBucketBar` at top.
- **Drag App Over Another App (Layer 1):** Triggers folder creation prompt and groups both items into `SpaceFolder`.
- **Drag App to Top Bucket:** Removes placement from active Space Home.
- **Long-Press on App Icon (Layer 2):** Opens context menu to pin app to Home or Dock.
- **Dual-Finger Gestures (Wallpaper Editor):** PointerInput detecting multi-touch zoom, pan offset, and bounds clamping.
- **Continuous Drag (Pattern Lock):** Real-time node hit testing with distance threshold (`36.dp` hit radius) and haptic ticks.

---

## 12. Animation, Motion & Transitions

- **Layer Transitions:** `AnimatedContent` utilizing `slideInVertically(tween(300))` combined with `fadeIn()` / `fadeOut()`.
- **Page Indicators:** `animateDpAsState` (6dp -> 20dp) and `animateColorAsState` with duration 250ms.
- **Lock Screen Shake:** Spring animation triggering horizontal translation on invalid credential entry.
- **Removal Bucket Spring:** `animateFloatAsState` targeting scale `1.2f` using `spring()` animation spec.
- **Selection Borders:** `animateColorAsState` and `animateDpAsState` on preset cards.

---

## 13. Dynamic Theming, Wallpaper & Contrast Handling

- **Wallpaper Rendering Modes:**
  - Solid Color: Full background surface fill.
  - Image Uri: Coil `AsyncImage` with dynamic pan translation (`translationX`, `translationY`), zoom scaling (`scaleX`, `scaleY`), and scale mode (`Crop` vs `Fit`).
- **Luminance Auto-Detection:** Automatically inspects wallpaper color/photo brightness to adjust TopBar and status icon tinting (`Color.White` vs `TextPrimary`).
- **Dimming Scrim:** User-controlled black scrim overlay (`0.0f` to `0.80f`) guaranteeing WCAG 2.1 AA text contrast against vibrant photographic wallpapers.

---

## 14. Accessibility & Touch Target Audit

- **Touch Targets:** Key action buttons, dock icons, and grid items maintain minimum `48.dp x 48.dp` interactive areas.
- **Test Tags:** Primary components feature descriptive test tags (e.g. `home_space_indicator`, `btn_lock_phone`, `btn_open_config`, `pattern_lock_canvas`, `space_item_{id}`).
- **Content Descriptions:** Configured across all IconButtons, navigation controls, and app icons.
- **Disclosure Transparency:** `NativeRecentsDisclosureDialog` explicitly details single-purpose usage, zero screen inspection, and zero data collection.

---

## 15. Iconography & Visual Assets Inventory

- **Standard Material Icons Utilized:**
  - Navigation & Space: `Icons.Default.GridView`, `Icons.Default.Apps`, `Icons.Default.Settings`, `Icons.Default.ArrowDropDown`, `Icons.Default.Add`
  - Security & Locks: `Icons.Default.Lock`, `Icons.Default.LockOpen`, `Icons.Default.Key`, `Icons.Default.LockReset`, `Icons.Default.VisibilityOff`
  - Management & Edit: `Icons.Default.Edit`, `Icons.Default.Delete`, `Icons.Default.Palette`, `Icons.Default.List`, `Icons.Default.Search`, `Icons.Default.Check`
  - System & Status: `Icons.Default.FileDownload`, `Icons.Default.Info`, `Icons.Default.Warning`, `Icons.Default.CheckCircle`, `Icons.Default.TaskAlt`
- **Application Icons:** Rendered via Coil `AsyncImage` and cached bitmaps from Android's `LauncherApps` / `PackageManager`.

---

## 16. Layout Density & Scalability

- **Grid Geometry:** Supports 2 to 7 columns dynamically. Standard presets use 4 columns on phone form factors.
- **Dock Capacity:** Configurable from 3 to 6 slots.
- **Adaptive Canvas:** `BoxWithConstraints` utilized in `PatternLockCanvas` and `LayoutPresetVisualCard` to dynamically scale based on available container bounds.

---

## 17. Inconsistencies & Visual Debt Matrix

| Area | Current State / Observation | Modernization Opportunity |
|---|---|---|
| **Color Tokens** | Mixed usage of Material 3 `colorScheme` tokens and hardcoded static hex values (e.g., `#2E7D32`, `#C62828`, `#6200EE`). | Unify all semantic states under centralized M3 theme tokens with dynamic color compliance. |
| **Corner Radii** | Divergent radii (`6.dp`, `8.dp`, `10.dp`, `14.dp`, `16.dp`, `20.dp`, `22.dp`, `24.dp`) across cards and buttons. | Establish a standardized 4-tier shape scale: Small (8dp), Medium (16dp), Large (24dp), Full (Circle). |
| **Header Styling** | Top app bars and headers across screens use varied padding and container elevations. | Implement a unified, consistent Header / TopAppBar component. |
| **Dialog Architecture** | Multiple dialog patterns (standard `AlertDialog`, custom `Dialog` with `Surface`, custom bottom sheets). | Standardize modal dialog containers with unified padding, corner radii, and action button placements. |
| **Monospace / Terminal vs Consumer UI** | `LauncherDiagnosticsScreen` uses specialized dark terminal tokens while management screens use standard light container tokens. | Preserve terminal styling for diagnostics, but harmonize contrast and font scales with the broader design system. |

---

## 18. Hardcoded Values & Anti-Patterns Audit

1. **Hardcoded Text Dimensions:** Minor instances of fixed `11.sp` or `13.sp` in auxiliary labels; should strictly follow M3 Typography styles (`labelSmall`, `bodySmall`).
2. **Preset Canvas Color Arrays:** Static hex definitions in `LayoutPresetVisualPreview.kt` are contained within preview drawing scope, but should be centralized if reused across live themes.
3. **Card Border Widths:** Varies between `0.5.dp`, `1.dp`, `1.5.dp`, `2.dp`, `2.5.dp` across components.

---

## 19. Performance & Rendering Characteristics

- **Bitmap Caching:** `AppDiscoveryViewModel` caches extracted app icon bitmaps in memory to prevent re-extraction lag during grid scroll.
- **Lazy Layouts:** `LazyColumn` and `LazyVerticalGrid` utilize stable item keys (`it.id`, `it.packageName`) and explicit `contentType` declarations for optimal recomposition.
- **Gesture Canvas:** `PatternLockCanvas` and `WallpaperEditorScreen` perform calculations inside DrawScope or pointer input without triggering unnecessary recompositions.

---

## 20. Security & Privacy UI States

- **Locked Space Privacy Shield:** When a protected space is locked, application items and placements are completely withheld from the UI composition tree (`emptyList()`), preventing memory inspection or accessibility leakage.
- **Masked PIN Entry:** Numeric PIN keypad uses masked dot indicators with dynamic feedback.
- **Non-Intrusive Accessibility:** Clear, Google Play-compliant disclosures explaining that accessibility permissions are solely used for system overview actions.

---

## 21. Baseline Summary & Readiness for Modernization Plan

The Multi-Space Launcher codebase has a robust, fully implemented functional foundation:
- Complete dual-layer space isolation architecture.
- Full Room SQLite persistence for spaces, memberships, dock slots, folders, and placements.
- Native system integration for home role management, recent apps bridge, and screen-off locking.
- Comprehensive suite of configuration dialogs, wallpaper live tuning, and gesture canvases.

**Conclusion:** The codebase is in a verified, stable baseline state. This document provides the complete UI/UX blueprint required to craft an intentional, cohesive, and premium modernization plan in the next phase.
