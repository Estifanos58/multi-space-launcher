package com.multispace.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.ImportReport
import com.multispace.domain.model.LayoutPreset
import com.multispace.domain.model.Space
import com.multispace.platform.PinSecurityManager
import androidx.compose.foundation.BorderStroke
import com.multispace.ui.components.*
import com.multispace.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CreateSpaceSubPage {
  MAIN_TABS,
  PATTERN_GRID_CHOICE,
  PATTERN_DRAWING,
  CUSTOM_GRID,
  WALLPAPER_EDITOR
}

enum class CredentialOption {
  NONE,
  PIN,
  PATTERN,
  BIOMETRIC
}

data class GradientWallpaperPreset(
  val id: String,
  val name: String,
  val colors: List<Color>,
  val representativeColor: Long
)

val PRESET_GRADIENTS = listOf(
  GradientWallpaperPreset(
    id = "grad_midnight",
    name = "Midnight Indigo",
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81)),
    representativeColor = 0xFF0F172AL
  ),
  GradientWallpaperPreset(
    id = "grad_aurora",
    name = "Aurora Borealis",
    colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981)),
    representativeColor = 0xFF064E3BL
  ),
  GradientWallpaperPreset(
    id = "grad_cyberpunk",
    name = "Cyber Neon",
    colors = listOf(Color(0xFF4C0519), Color(0xFF831843), Color(0xFFBE185D)),
    representativeColor = 0xFF4C0519L
  ),
  GradientWallpaperPreset(
    id = "grad_sunset",
    name = "Sunset Horizon",
    colors = listOf(Color(0xFF451A03), Color(0xFF78350F), Color(0xFFB45309)),
    representativeColor = 0xFF451A03L
  ),
  GradientWallpaperPreset(
    id = "grad_nebula",
    name = "Deep Nebula",
    colors = listOf(Color(0xFF1E1B4B), Color(0xFF3B0764), Color(0xFF581C87)),
    representativeColor = 0xFF1E1B4BL
  ),
  GradientWallpaperPreset(
    id = "grad_ocean",
    name = "Abyssal Ocean",
    colors = listOf(Color(0xFF082F49), Color(0xFF0369A1), Color(0xFF0284C7)),
    representativeColor = 0xFF082F49L
  ),
  GradientWallpaperPreset(
    id = "grad_charcoal",
    name = "Minimal Steel",
    colors = listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF3F3F46)),
    representativeColor = 0xFF18181BL
  ),
  GradientWallpaperPreset(
    id = "grad_emerald",
    name = "Forest Emerald",
    colors = listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF065F46)),
    representativeColor = 0xFF022C22L
  )
)

data class AppThemePreset(
  val id: String,
  val name: String,
  val description: String,
  val primaryColor: Color,
  val secondaryColor: Color,
  val iconBackgroundColor: Color
)

val PRESET_APP_THEMES = listOf(
  AppThemePreset(
    id = Space.THEME_DEFAULT,
    name = "Default Material",
    description = "Standard dynamic Android styling",
    primaryColor = Color(0xFF6750A4),
    secondaryColor = Color(0xFF625B71),
    iconBackgroundColor = Color(0xFFE8DEF8)
  ),
  AppThemePreset(
    id = Space.THEME_PURPLE,
    name = "Royal Purple",
    description = "Rich violet and deep indigo accents",
    primaryColor = Color(0xFF7C4DFF),
    secondaryColor = Color(0xFF651FFF),
    iconBackgroundColor = Color(0xFFEDE7F6)
  ),
  AppThemePreset(
    id = Space.THEME_DARK,
    name = "Midnight AMOLED",
    description = "High-contrast dark monochrome",
    primaryColor = Color(0xFFE0E0E0),
    secondaryColor = Color(0xFF757575),
    iconBackgroundColor = Color(0xFF212121)
  ),
  AppThemePreset(
    id = Space.THEME_NEON,
    name = "Cyber Neon",
    description = "Vibrant electric pink and cyan glow",
    primaryColor = Color(0xFFFF007F),
    secondaryColor = Color(0xFF00F0FF),
    iconBackgroundColor = Color(0xFF2A0845)
  ),
  AppThemePreset(
    id = Space.THEME_MINIMAL,
    name = "Slate Minimal",
    description = "Subtle clean grey and slate tones",
    primaryColor = Color(0xFF475569),
    secondaryColor = Color(0xFF64748B),
    iconBackgroundColor = Color(0xFFF1F5F9)
  ),
  AppThemePreset(
    id = Space.THEME_EMERALD,
    name = "Forest Emerald",
    description = "Calming organic lush green accents",
    primaryColor = Color(0xFF10B981),
    secondaryColor = Color(0xFF059669),
    iconBackgroundColor = Color(0xFFD1FAE5)
  ),
  AppThemePreset(
    id = Space.THEME_SUNSET,
    name = "Sunset Amber",
    description = "Warm coral and golden dusk tones",
    primaryColor = Color(0xFFF59E0B),
    secondaryColor = Color(0xFFD97706),
    iconBackgroundColor = Color(0xFFFEF3C7)
  ),
  AppThemePreset(
    id = Space.THEME_OCEAN,
    name = "Oceanic Teal",
    description = "Deep coastal blue and aquamarine",
    primaryColor = Color(0xFF0284C7),
    secondaryColor = Color(0xFF0369A1),
    iconBackgroundColor = Color(0xFFE0F2FE)
  )
)

/**
 * Comprehensive Dedicated Full Page for Creating a New Multi-Space Launcher Space.
 * Features tabs:
 * 1. Basics & Security: Space name, preset chips, credentials (None / PIN / Pattern with multi-dot choice).
 * 2. Wallpaper & Theme: Home Wallpaper, Phone Lock Screen Wallpaper, Space Lock Screen Wallpaper, Theme for Apps.
 * 3. Apps & Layout: App selection, grid presets (3, 4, 5, 6 cols), and custom grid format designer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceScreen(
  allApps: List<DiscoveredApp>,
  spaceViewModel: SpaceViewModel,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onNavigateBack: () -> Unit,
  onSpaceCreated: (spaceId: String) -> Unit,
  editingSpace: Space? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isEditMode = editingSpace != null

  // Top-level Navigation Sub-Page State
  var currentSubPage by rememberSaveable { mutableStateOf(CreateSpaceSubPage.MAIN_TABS) }
  var currentTab by rememberSaveable { mutableIntStateOf(0) } // 0: Basics, 1: Presets, 2: Wallpaper & Theme, 3: Apps & Layout

  // Tab 1: Basics & Credentials State
  var spaceName by rememberSaveable { mutableStateOf(editingSpace?.name ?: "") }
  var spaceNameError by rememberSaveable { mutableStateOf<String?>(null) }
  val initialCredentialOption = remember(editingSpace) {
    when {
      editingSpace?.isBiometricProtected == true || editingSpace?.authPolicy == Space.AUTH_BIOMETRIC -> CredentialOption.BIOMETRIC
      editingSpace?.isPatternProtected == true || editingSpace?.authPolicy == Space.AUTH_PATTERN -> CredentialOption.PATTERN
      editingSpace?.isPinProtected == true || editingSpace?.authPolicy == Space.AUTH_PIN -> CredentialOption.PIN
      else -> CredentialOption.NONE
    }
  }
  var credentialOption by rememberSaveable { mutableStateOf(initialCredentialOption) }

  // PIN state
  var pinValue by rememberSaveable { mutableStateOf("") }
  var confirmPinValue by rememberSaveable { mutableStateOf("") }
  var pinError by rememberSaveable { mutableStateOf<String?>(null) }
  var showPinText by rememberSaveable { mutableStateOf(false) }

  // Pattern state
  var patternRows by rememberSaveable { mutableIntStateOf(editingSpace?.patternRows ?: 3) }
  var patternCols by rememberSaveable { mutableIntStateOf(editingSpace?.patternCols ?: 3) }
  val initialPatternOption = remember(editingSpace) {
    if (editingSpace != null) {
      val dots = editingSpace.patternRows * editingSpace.patternCols
      if (dots == 6) "6dots" else if (dots == 9) "9dots" else "custom"
    } else {
      "9dots"
    }
  }
  var patternCustomOption by rememberSaveable { mutableStateOf(initialPatternOption) }
  var drawnFirstPattern by remember { mutableStateOf<String?>(null) }
  var confirmedPatternString by rememberSaveable { mutableStateOf<String?>(null) }
  var patternDrawingStep by rememberSaveable { mutableIntStateOf(1) } // 1: Record, 2: Confirm
  var patternCanvasError by remember { mutableStateOf(false) }
  var patternCanvasFeedback by remember { mutableStateOf<String?>(null) }
  var patternClearTrigger by remember { mutableIntStateOf(0) }

  // Tab 2: Layout Preset State
  var selectedLayoutPreset by rememberSaveable {
    mutableStateOf(editingSpace?.layoutPreset ?: Space.PRESET_DEFAULT)
  }
  var layer1DisplayMode by rememberSaveable {
    mutableStateOf(editingSpace?.layer1DisplayMode ?: Space.DISPLAY_MODE_PAGE)
  }
  var layer2DisplayMode by rememberSaveable {
    mutableStateOf(editingSpace?.layer2DisplayMode ?: Space.DISPLAY_MODE_SCROLL)
  }
  var layer2AccessMode by rememberSaveable {
    mutableStateOf(editingSpace?.layer2AccessMode ?: Space.ACCESS_MODE_DOCK_BUTTON)
  }
  var useLayer2 by rememberSaveable {
    mutableStateOf(editingSpace?.useLayer2 ?: true)
  }
  var dockCapacity by rememberSaveable {
    mutableIntStateOf(editingSpace?.dockCapacity ?: Space.DEFAULT_DOCK_CAPACITY)
  }
  var presetToConfirm by remember { mutableStateOf<LayoutPreset?>(null) }
  var showImportLayoutDialog by remember { mutableStateOf(false) }
  var importReport by remember { mutableStateOf<ImportReport?>(null) }
  var isImportingLayout by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()
  var wallpaperEditorTarget by rememberSaveable { mutableStateOf("home") }

  // Tab 2: Wallpaper & Theme State
  // Home Wallpaper
  val initialHomeCat = remember(editingSpace) {
    when (editingSpace?.homeWallpaperType ?: editingSpace?.backgroundType) {
      Space.BACKGROUND_IMAGE -> "photo"
      Space.BACKGROUND_COLOR -> if (PRESET_GRADIENTS.any { it.representativeColor == (editingSpace?.homeWallpaperColor ?: editingSpace?.backgroundColor) }) "gradients" else "colors"
      else -> "gradients"
    }
  }
  var homeWallpaperCategory by rememberSaveable { mutableStateOf(initialHomeCat) }
  var homeSelectedBgColor by rememberSaveable { mutableStateOf(editingSpace?.homeWallpaperColor ?: editingSpace?.backgroundColor ?: PRESET_BACKGROUND_COLORS.first().first) }
  var homeSelectedGradientId by rememberSaveable { mutableStateOf(PRESET_GRADIENTS.firstOrNull { it.representativeColor == (editingSpace?.homeWallpaperColor ?: editingSpace?.backgroundColor) }?.id ?: PRESET_GRADIENTS.first().id) }
  var homeCustomImageUri by rememberSaveable { mutableStateOf(editingSpace?.homeWallpaperImageUri ?: editingSpace?.backgroundImageUri) }
  var homeWallpaperScaleMode by rememberSaveable { mutableStateOf(editingSpace?.homeWallpaperScaleMode ?: "crop") }
  var homeWallpaperZoomLevel by rememberSaveable { mutableFloatStateOf(editingSpace?.homeWallpaperZoomLevel ?: 1.0f) }
  var homeWallpaperDimLevel by rememberSaveable { mutableFloatStateOf(editingSpace?.homeWallpaperDimLevel ?: 0.20f) }
  var homeWallpaperOffsetX by rememberSaveable { mutableFloatStateOf(editingSpace?.homeWallpaperOffsetX ?: 0.0f) }
  var homeWallpaperOffsetY by rememberSaveable { mutableFloatStateOf(editingSpace?.homeWallpaperOffsetY ?: 0.0f) }

  // Phone Lock Screen Wallpaper
  val initialPhoneLockCat = remember(editingSpace) {
    when (editingSpace?.phoneLockWallpaperType) {
      Space.BACKGROUND_IMAGE -> "photo"
      Space.BACKGROUND_COLOR -> if (PRESET_GRADIENTS.any { it.representativeColor == editingSpace.phoneLockWallpaperColor }) "gradients" else "colors"
      else -> "gradients"
    }
  }
  var phoneLockWallpaperCategory by rememberSaveable { mutableStateOf(initialPhoneLockCat) }
  var phoneLockSelectedBgColor by rememberSaveable { mutableStateOf(editingSpace?.phoneLockWallpaperColor ?: PRESET_BACKGROUND_COLORS[1].first) }
  var phoneLockSelectedGradientId by rememberSaveable { mutableStateOf(PRESET_GRADIENTS.firstOrNull { it.representativeColor == editingSpace?.phoneLockWallpaperColor }?.id ?: PRESET_GRADIENTS[1].id) }
  var phoneLockCustomImageUri by rememberSaveable { mutableStateOf(editingSpace?.phoneLockWallpaperImageUri) }
  var phoneLockWallpaperScaleMode by rememberSaveable { mutableStateOf(editingSpace?.phoneLockWallpaperScaleMode ?: "crop") }
  var phoneLockWallpaperZoomLevel by rememberSaveable { mutableFloatStateOf(editingSpace?.phoneLockWallpaperZoomLevel ?: 1.0f) }
  var phoneLockWallpaperDimLevel by rememberSaveable { mutableFloatStateOf(editingSpace?.phoneLockWallpaperDimLevel ?: 0.20f) }
  var phoneLockWallpaperOffsetX by rememberSaveable { mutableFloatStateOf(editingSpace?.phoneLockWallpaperOffsetX ?: 0.0f) }
  var phoneLockWallpaperOffsetY by rememberSaveable { mutableFloatStateOf(editingSpace?.phoneLockWallpaperOffsetY ?: 0.0f) }

  // Space Lock Screen Wallpaper
  val initialSpaceLockCat = remember(editingSpace) {
    when (editingSpace?.spaceLockWallpaperType) {
      Space.BACKGROUND_IMAGE -> "photo"
      Space.BACKGROUND_COLOR -> if (PRESET_GRADIENTS.any { it.representativeColor == editingSpace.spaceLockWallpaperColor }) "gradients" else "colors"
      else -> "gradients"
    }
  }
  var spaceLockWallpaperCategory by rememberSaveable { mutableStateOf(initialSpaceLockCat) }
  var spaceLockSelectedBgColor by rememberSaveable { mutableStateOf(editingSpace?.spaceLockWallpaperColor ?: PRESET_BACKGROUND_COLORS[4].first) }
  var spaceLockSelectedGradientId by rememberSaveable { mutableStateOf(PRESET_GRADIENTS.firstOrNull { it.representativeColor == editingSpace?.spaceLockWallpaperColor }?.id ?: PRESET_GRADIENTS[4].id) }
  var spaceLockCustomImageUri by rememberSaveable { mutableStateOf(editingSpace?.spaceLockWallpaperImageUri) }
  var spaceLockWallpaperScaleMode by rememberSaveable { mutableStateOf(editingSpace?.spaceLockWallpaperScaleMode ?: "crop") }
  var spaceLockWallpaperZoomLevel by rememberSaveable { mutableFloatStateOf(editingSpace?.spaceLockWallpaperZoomLevel ?: 1.0f) }
  var spaceLockWallpaperDimLevel by rememberSaveable { mutableFloatStateOf(editingSpace?.spaceLockWallpaperDimLevel ?: 0.20f) }
  var spaceLockWallpaperOffsetX by rememberSaveable { mutableFloatStateOf(editingSpace?.spaceLockWallpaperOffsetX ?: 0.0f) }
  var spaceLockWallpaperOffsetY by rememberSaveable { mutableFloatStateOf(editingSpace?.spaceLockWallpaperOffsetY ?: 0.0f) }

  // Theme for Apps
  var selectedAppTheme by rememberSaveable { mutableStateOf(editingSpace?.appTheme ?: Space.THEME_DEFAULT) }

  // Tab 3: Apps & Layout State
  var appSearchQuery by rememberSaveable { mutableStateOf("") }
  var selectedAppsSet by remember { mutableStateOf(setOf<String>()) }
  var gridColumns by rememberSaveable { mutableIntStateOf(editingSpace?.gridColumns ?: Space.DEFAULT_GRID_COLUMNS) }
  var iconSize by rememberSaveable { mutableStateOf(editingSpace?.iconSize ?: Space.ICON_SIZE_MEDIUM) }
  var showLabels by rememberSaveable { mutableStateOf(editingSpace?.labelVisibility ?: true) }

  var isCreating by remember { mutableStateOf(false) }

  // Pre-load apps for editing space
  LaunchedEffect(editingSpace?.id, allApps) {
    if (editingSpace != null) {
      val memberships = spaceViewModel.spaceRepository.getMembershipsForSpace(editingSpace.id)
      val memberIds = mutableSetOf<String>()
      for (m in memberships) {
        val matched = allApps.firstOrNull { it.packageName == m.packageName && (m.componentName.isBlank() || it.activityName == m.componentName) }
        if (matched != null) {
          memberIds.add(matched.id)
        } else {
          memberIds.add("${m.packageName}/${m.componentName}/0")
        }
      }
      selectedAppsSet = memberIds
    }
  }

  // Photo Wallpaper Picker Launchers
  val homePhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (_: Exception) {}
      homeCustomImageUri = uri.toString()
      homeWallpaperCategory = "photo"
    }
  }

  val phoneLockPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (_: Exception) {}
      phoneLockCustomImageUri = uri.toString()
      phoneLockWallpaperCategory = "photo"
    }
  }

  val spaceLockPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (_: Exception) {}
      spaceLockCustomImageUri = uri.toString()
      spaceLockWallpaperCategory = "photo"
    }
  }

  // Handle Android hardware/gesture back press
  BackHandler {
    when (currentSubPage) {
      CreateSpaceSubPage.PATTERN_DRAWING -> {
        currentSubPage = CreateSpaceSubPage.PATTERN_GRID_CHOICE
      }
      CreateSpaceSubPage.PATTERN_GRID_CHOICE -> {
        currentSubPage = CreateSpaceSubPage.MAIN_TABS
      }
      CreateSpaceSubPage.CUSTOM_GRID -> {
        currentSubPage = CreateSpaceSubPage.MAIN_TABS
      }
      CreateSpaceSubPage.WALLPAPER_EDITOR -> {
        currentSubPage = CreateSpaceSubPage.MAIN_TABS
      }
      CreateSpaceSubPage.MAIN_TABS -> {
        if (currentTab > 0) {
          currentTab -= 1
        } else {
          onNavigateBack()
        }
      }
    }
  }

  // Sub-flow screens routing
  when (currentSubPage) {
    CreateSpaceSubPage.PATTERN_GRID_CHOICE -> {
      PatternGridChoiceScreen(
        selectedOption = patternCustomOption,
        customRows = patternRows,
        customCols = patternCols,
        onOptionSelected = { opt, r, c ->
          patternCustomOption = opt
          patternRows = r
          patternCols = c
        },
        onNavigateBack = { currentSubPage = CreateSpaceSubPage.MAIN_TABS },
        onProceedToDraw = {
          drawnFirstPattern = null
          patternDrawingStep = 1
          patternCanvasError = false
          patternCanvasFeedback = null
          patternClearTrigger++
          currentSubPage = CreateSpaceSubPage.PATTERN_DRAWING
        }
      )
    }

    CreateSpaceSubPage.PATTERN_DRAWING -> {
      PatternDrawingScreen(
        rows = patternRows,
        cols = patternCols,
        step = patternDrawingStep,
        feedbackMessage = patternCanvasFeedback,
        isError = patternCanvasError,
        clearTrigger = patternClearTrigger,
        onPatternRecorded = { patternStr, nodeCount ->
          if (patternDrawingStep == 1) {
            if (nodeCount < 4) {
              patternCanvasError = true
              patternCanvasFeedback = "Connect at least 4 dots to secure your Space."
              patternClearTrigger++
            } else {
              drawnFirstPattern = patternStr
              patternDrawingStep = 2
              patternCanvasError = false
              patternCanvasFeedback = "Draw the pattern again to confirm."
              patternClearTrigger++
            }
          } else {
            // Confirmation step
            if (patternStr == drawnFirstPattern) {
              confirmedPatternString = patternStr
              patternCanvasError = false
              patternCanvasFeedback = "Pattern matched successfully!"
              currentSubPage = CreateSpaceSubPage.MAIN_TABS
            } else {
              patternCanvasError = true
              patternCanvasFeedback = "Patterns do not match. Try drawing again from Step 1."
              patternDrawingStep = 1
              drawnFirstPattern = null
              patternClearTrigger++
            }
          }
        },
        onReset = {
          drawnFirstPattern = null
          patternDrawingStep = 1
          patternCanvasError = false
          patternCanvasFeedback = null
          patternClearTrigger++
        },
        onNavigateBack = { currentSubPage = CreateSpaceSubPage.PATTERN_GRID_CHOICE }
      )
    }

    CreateSpaceSubPage.CUSTOM_GRID -> {
      CustomGridScreen(
        initialColumns = gridColumns,
        initialIconSize = iconSize,
        initialLabelVisibility = showLabels,
        onNavigateBack = { currentSubPage = CreateSpaceSubPage.MAIN_TABS },
        onApplyCustomGrid = { newCols, newSize, newLabels ->
          gridColumns = newCols
          iconSize = newSize
          showLabels = newLabels
          currentSubPage = CreateSpaceSubPage.MAIN_TABS
        }
      )
    }

    CreateSpaceSubPage.WALLPAPER_EDITOR -> {
      WallpaperEditorScreen(
        targetTitle = when (wallpaperEditorTarget) {
          "phone_lock" -> "Phone Lock Screen Wallpaper"
          "space_lock" -> "Space Lock Screen Wallpaper"
          else -> "Home Wallpaper"
        },
        initialImageUri = when (wallpaperEditorTarget) {
          "phone_lock" -> phoneLockCustomImageUri
          "space_lock" -> spaceLockCustomImageUri
          else -> homeCustomImageUri
        },
        apps = allApps,
        gridColumns = gridColumns,
        spaceName = spaceName,
        dockCapacity = dockCapacity,
        getBitmap = getBitmap,
        onPickNewImage = {
          when (wallpaperEditorTarget) {
            "phone_lock" -> phoneLockPhotoPickerLauncher.launch("image/*")
            "space_lock" -> spaceLockPhotoPickerLauncher.launch("image/*")
            else -> homePhotoPickerLauncher.launch("image/*")
          }
        },
        initialScaleMode = when (wallpaperEditorTarget) {
          "phone_lock" -> phoneLockWallpaperScaleMode
          "space_lock" -> spaceLockWallpaperScaleMode
          else -> homeWallpaperScaleMode
        },
        initialZoomLevel = when (wallpaperEditorTarget) {
          "phone_lock" -> phoneLockWallpaperZoomLevel
          "space_lock" -> spaceLockWallpaperZoomLevel
          else -> homeWallpaperZoomLevel
        },
        initialDimLevel = when (wallpaperEditorTarget) {
          "phone_lock" -> phoneLockWallpaperDimLevel
          "space_lock" -> spaceLockWallpaperDimLevel
          else -> homeWallpaperDimLevel
        },
        initialOffsetX = when (wallpaperEditorTarget) {
          "phone_lock" -> phoneLockWallpaperOffsetX
          "space_lock" -> spaceLockWallpaperOffsetX
          else -> homeWallpaperOffsetX
        },
        initialOffsetY = when (wallpaperEditorTarget) {
          "phone_lock" -> phoneLockWallpaperOffsetY
          "space_lock" -> spaceLockWallpaperOffsetY
          else -> homeWallpaperOffsetY
        },
        onApply = { newUri, scaleMode, zoomLevel, dimLevel, offsetX, offsetY ->
          when (wallpaperEditorTarget) {
            "phone_lock" -> {
              phoneLockCustomImageUri = newUri
              phoneLockWallpaperCategory = "photo"
              phoneLockWallpaperScaleMode = scaleMode
              phoneLockWallpaperZoomLevel = zoomLevel
              phoneLockWallpaperDimLevel = dimLevel
              phoneLockWallpaperOffsetX = offsetX
              phoneLockWallpaperOffsetY = offsetY
            }
            "space_lock" -> {
              spaceLockCustomImageUri = newUri
              spaceLockWallpaperCategory = "photo"
              spaceLockWallpaperScaleMode = scaleMode
              spaceLockWallpaperZoomLevel = zoomLevel
              spaceLockWallpaperDimLevel = dimLevel
              spaceLockWallpaperOffsetX = offsetX
              spaceLockWallpaperOffsetY = offsetY
            }
            else -> {
              homeCustomImageUri = newUri
              homeWallpaperCategory = "photo"
              homeWallpaperScaleMode = scaleMode
              homeWallpaperZoomLevel = zoomLevel
              homeWallpaperDimLevel = dimLevel
              homeWallpaperOffsetX = offsetX
              homeWallpaperOffsetY = offsetY
            }
          }
          currentSubPage = CreateSpaceSubPage.MAIN_TABS
        },
        onCancel = {
          currentSubPage = CreateSpaceSubPage.MAIN_TABS
        }
      )
    }

    CreateSpaceSubPage.MAIN_TABS -> {
      Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
          Column {
            TopAppBar(
              title = {
                Column {
                  Text(
                    text = if (isEditMode) "Edit Space" else "Create New Space",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = when (currentTab) {
                      0 -> "Space Identity & Security"
                      1 -> "Layout Preset & Paradigm"
                      2 -> "Wallpaper & App Theme"
                      else -> "Apps & Grid Layout"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              },
              navigationIcon = {
                IconButton(
                  onClick = {
                    if (currentTab > 0) currentTab -= 1 else onNavigateBack()
                  },
                  modifier = Modifier.testTag("btn_back_create_space")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                  )
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

            // Primary Four-Tab Switcher
            TabRow(
              selectedTabIndex = currentTab,
              containerColor = MaterialTheme.colorScheme.surfaceContainer,
              contentColor = QuantumViolet,
              modifier = Modifier.fillMaxWidth()
            ) {
              Tab(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                modifier = Modifier.testTag("tab_basics_security"),
                selectedContentColor = QuantumViolet,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Shield,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp)
                    )
                    Text("Basics", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                  }
                }
              )
              Tab(
                selected = currentTab == 1,
                onClick = {
                  if (validateTab1(spaceName, credentialOption, pinValue, confirmPinValue, confirmedPatternString, editingSpace, { spaceNameError = it }, { pinError = it })) {
                    currentTab = 1
                  }
                },
                modifier = Modifier.testTag("tab_presets"),
                selectedContentColor = QuantumViolet,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.DashboardCustomize,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp)
                    )
                    Text("Presets", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                  }
                }
              )
              Tab(
                selected = currentTab == 2,
                onClick = {
                  if (validateTab1(spaceName, credentialOption, pinValue, confirmPinValue, confirmedPatternString, editingSpace, { spaceNameError = it }, { pinError = it })) {
                    currentTab = 2
                  }
                },
                modifier = Modifier.testTag("tab_wallpaper_theme"),
                selectedContentColor = QuantumViolet,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Wallpaper,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp)
                    )
                    Text("Wallpaper", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                  }
                }
              )
              Tab(
                selected = currentTab == 3,
                onClick = {
                  if (validateTab1(spaceName, credentialOption, pinValue, confirmPinValue, confirmedPatternString, editingSpace, { spaceNameError = it }, { pinError = it })) {
                    currentTab = 3
                  }
                },
                modifier = Modifier.testTag("tab_apps_layout"),
                selectedContentColor = QuantumViolet,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.GridView,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp)
                    )
                    Text("Apps", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                  }
                }
              )
            }
          }
        },
        bottomBar = {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (currentTab == 0) {
                OutlinedButton(
                  onClick = onNavigateBack,
                  shape = ShapeRoundMd,
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Cancel")
                }
                Button(
                  onClick = {
                    if (validateTab1(spaceName, credentialOption, pinValue, confirmPinValue, confirmedPatternString, editingSpace, { spaceNameError = it }, { pinError = it })) {
                      currentTab = 1
                    }
                  },
                  shape = ShapeRoundMd,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier
                    .weight(1.5f)
                    .testTag("btn_next_to_presets")
                ) {
                  Text("Next: Presets", fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                }
              } else if (currentTab == 1) {
                OutlinedButton(
                  onClick = { currentTab = 0 },
                  shape = ShapeRoundMd,
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Basics")
                }
                Button(
                  onClick = { currentTab = 2 },
                  shape = ShapeRoundMd,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier
                    .weight(1.5f)
                    .testTag("btn_next_to_wallpaper")
                ) {
                  Text("Next: Wallpaper", fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                }
              } else if (currentTab == 2) {
                OutlinedButton(
                  onClick = { currentTab = 1 },
                  shape = ShapeRoundMd,
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Presets")
                }
                Button(
                  onClick = { currentTab = 3 },
                  shape = ShapeRoundMd,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier
                    .weight(1.5f)
                    .testTag("btn_next_to_apps")
                ) {
                  Text("Next: Apps & Grid", fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                }
              } else {
                OutlinedButton(
                  onClick = { currentTab = 2 },
                  shape = ShapeRoundMd,
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Wallpaper")
                }
                Button(
                  onClick = {
                    if (!isCreating && validateTab1(spaceName, credentialOption, pinValue, confirmPinValue, confirmedPatternString, editingSpace, { spaceNameError = it }, { pinError = it })) {
                      isCreating = true

                      // Process credential hashing
                      var authPolicy = Space.AUTH_NONE
                      var salt: String? = null
                      var hash: String? = null

                      if (isEditMode) {
                        authPolicy = when (credentialOption) {
                          CredentialOption.NONE -> Space.AUTH_NONE
                          CredentialOption.PIN -> Space.AUTH_PIN
                          CredentialOption.PATTERN -> Space.AUTH_PATTERN
                          CredentialOption.BIOMETRIC -> Space.AUTH_BIOMETRIC
                        }

                        if (credentialOption == CredentialOption.PIN) {
                          if (pinValue.isNotBlank()) {
                            salt = PinSecurityManager.generateSalt()
                            hash = PinSecurityManager.hashPin(pinValue, salt)
                          } else {
                            salt = editingSpace?.pinSalt
                            hash = editingSpace?.pinHash
                          }
                        } else if (credentialOption == CredentialOption.PATTERN) {
                          if (!confirmedPatternString.isNullOrEmpty()) {
                            salt = PinSecurityManager.generateSalt()
                            hash = PinSecurityManager.hashPin(confirmedPatternString!!, salt)
                          } else {
                            salt = editingSpace?.pinSalt
                            hash = editingSpace?.pinHash
                          }
                        }
                      } else {
                        if (credentialOption == CredentialOption.BIOMETRIC) {
                          authPolicy = Space.AUTH_BIOMETRIC
                        } else if (credentialOption == CredentialOption.PIN && pinValue.isNotBlank()) {
                          authPolicy = Space.AUTH_PIN
                          salt = PinSecurityManager.generateSalt()
                          hash = PinSecurityManager.hashPin(pinValue, salt)
                        } else if (credentialOption == CredentialOption.PATTERN && !confirmedPatternString.isNullOrEmpty()) {
                          authPolicy = Space.AUTH_PATTERN
                          salt = PinSecurityManager.generateSalt()
                          hash = PinSecurityManager.hashPin(confirmedPatternString!!, salt)
                        }
                      }

                      // Resolve Home Wallpaper
                      val homeBgType = when (homeWallpaperCategory) {
                        "photo" -> Space.BACKGROUND_IMAGE
                        "colors", "gradients" -> Space.BACKGROUND_COLOR
                        else -> Space.BACKGROUND_DEFAULT
                      }
                      val homeBgColor = when (homeWallpaperCategory) {
                        "gradients" -> PRESET_GRADIENTS.firstOrNull { it.id == homeSelectedGradientId }?.representativeColor ?: homeSelectedBgColor
                        "colors" -> homeSelectedBgColor
                        else -> null
                      }
                      val homeBgImageUri = if (homeWallpaperCategory == "photo") homeCustomImageUri else null

                      // Resolve Phone Lock Screen Wallpaper
                      val phoneLockBgType = when (phoneLockWallpaperCategory) {
                        "photo" -> Space.BACKGROUND_IMAGE
                        "colors", "gradients" -> Space.BACKGROUND_COLOR
                        else -> Space.BACKGROUND_DEFAULT
                      }
                      val phoneLockBgColor = when (phoneLockWallpaperCategory) {
                        "gradients" -> PRESET_GRADIENTS.firstOrNull { it.id == phoneLockSelectedGradientId }?.representativeColor ?: phoneLockSelectedBgColor
                        "colors" -> phoneLockSelectedBgColor
                        else -> null
                      }
                      val phoneLockBgImageUri = if (phoneLockWallpaperCategory == "photo") phoneLockCustomImageUri else null

                      // Resolve Space Lock Screen Wallpaper
                      val spaceLockBgType = when (spaceLockWallpaperCategory) {
                        "photo" -> Space.BACKGROUND_IMAGE
                        "colors", "gradients" -> Space.BACKGROUND_COLOR
                        else -> Space.BACKGROUND_DEFAULT
                      }
                      val spaceLockBgColor = when (spaceLockWallpaperCategory) {
                        "gradients" -> PRESET_GRADIENTS.firstOrNull { it.id == spaceLockSelectedGradientId }?.representativeColor ?: spaceLockSelectedBgColor
                        "colors" -> spaceLockSelectedBgColor
                        else -> null
                      }
                      val spaceLockBgImageUri = if (spaceLockWallpaperCategory == "photo") spaceLockCustomImageUri else null

                      val selectedAppObjects = allApps.filter { selectedAppsSet.contains(it.id) }

                      if (editingSpace != null) {
                        spaceViewModel.updateFullSpace(
                          spaceId = editingSpace.id,
                          name = spaceName.trim(),
                          authPolicy = authPolicy,
                          pinSalt = salt,
                          pinHash = hash,
                          patternRows = patternRows,
                          patternCols = patternCols,
                          backgroundType = homeBgType,
                          backgroundColor = homeBgColor,
                          backgroundImageUri = homeBgImageUri,
                          homeWallpaperType = homeBgType,
                          homeWallpaperColor = homeBgColor,
                          homeWallpaperImageUri = homeBgImageUri,
                          phoneLockWallpaperType = phoneLockBgType,
                          phoneLockWallpaperColor = phoneLockBgColor,
                          phoneLockWallpaperImageUri = phoneLockBgImageUri,
                          spaceLockWallpaperType = spaceLockBgType,
                          spaceLockWallpaperColor = spaceLockBgColor,
                          spaceLockWallpaperImageUri = spaceLockBgImageUri,
                          appTheme = selectedAppTheme,
                          gridColumns = gridColumns,
                          iconSize = iconSize,
                          labelVisibility = showLabels,
                          layer1DisplayMode = layer1DisplayMode,
                          layer2DisplayMode = layer2DisplayMode,
                          layer2AccessMode = layer2AccessMode,
                          useLayer2 = useLayer2,
                          dockCapacity = dockCapacity,
                          layoutPreset = selectedLayoutPreset,
                          homeWallpaperScaleMode = homeWallpaperScaleMode,
                          homeWallpaperZoomLevel = homeWallpaperZoomLevel,
                          homeWallpaperDimLevel = homeWallpaperDimLevel,
                          homeWallpaperOffsetX = homeWallpaperOffsetX,
                          homeWallpaperOffsetY = homeWallpaperOffsetY,
                          phoneLockWallpaperScaleMode = phoneLockWallpaperScaleMode,
                          phoneLockWallpaperZoomLevel = phoneLockWallpaperZoomLevel,
                          phoneLockWallpaperDimLevel = phoneLockWallpaperDimLevel,
                          phoneLockWallpaperOffsetX = phoneLockWallpaperOffsetX,
                          phoneLockWallpaperOffsetY = phoneLockWallpaperOffsetY,
                          spaceLockWallpaperScaleMode = spaceLockWallpaperScaleMode,
                          spaceLockWallpaperZoomLevel = spaceLockWallpaperZoomLevel,
                          spaceLockWallpaperDimLevel = spaceLockWallpaperDimLevel,
                          spaceLockWallpaperOffsetX = spaceLockWallpaperOffsetX,
                          spaceLockWallpaperOffsetY = spaceLockWallpaperOffsetY,
                          updatedApps = selectedAppObjects,
                          onResult = { success, _ ->
                            isCreating = false
                            if (success) {
                              onSpaceCreated(editingSpace.id)
                              onNavigateBack()
                            }
                          }
                        )
                      } else {
                        spaceViewModel.createFullSpace(
                          name = spaceName.trim(),
                          authPolicy = authPolicy,
                          pinSalt = salt,
                          pinHash = hash,
                          patternRows = patternRows,
                          patternCols = patternCols,
                          backgroundType = homeBgType,
                          backgroundColor = homeBgColor,
                          backgroundImageUri = homeBgImageUri,
                          homeWallpaperType = homeBgType,
                          homeWallpaperColor = homeBgColor,
                          homeWallpaperImageUri = homeBgImageUri,
                          phoneLockWallpaperType = phoneLockBgType,
                          phoneLockWallpaperColor = phoneLockBgColor,
                          phoneLockWallpaperImageUri = phoneLockBgImageUri,
                          spaceLockWallpaperType = spaceLockBgType,
                          spaceLockWallpaperColor = spaceLockBgColor,
                          spaceLockWallpaperImageUri = spaceLockBgImageUri,
                          appTheme = selectedAppTheme,
                          gridColumns = gridColumns,
                          iconSize = iconSize,
                          labelVisibility = showLabels,
                          layer1DisplayMode = layer1DisplayMode,
                          layer2DisplayMode = layer2DisplayMode,
                          layer2AccessMode = layer2AccessMode,
                          useLayer2 = useLayer2,
                          dockCapacity = dockCapacity,
                          layoutPreset = selectedLayoutPreset,
                          homeWallpaperScaleMode = homeWallpaperScaleMode,
                          homeWallpaperZoomLevel = homeWallpaperZoomLevel,
                          homeWallpaperDimLevel = homeWallpaperDimLevel,
                          homeWallpaperOffsetX = homeWallpaperOffsetX,
                          homeWallpaperOffsetY = homeWallpaperOffsetY,
                          phoneLockWallpaperScaleMode = phoneLockWallpaperScaleMode,
                          phoneLockWallpaperZoomLevel = phoneLockWallpaperZoomLevel,
                          phoneLockWallpaperDimLevel = phoneLockWallpaperDimLevel,
                          phoneLockWallpaperOffsetX = phoneLockWallpaperOffsetX,
                          phoneLockWallpaperOffsetY = phoneLockWallpaperOffsetY,
                          spaceLockWallpaperScaleMode = spaceLockWallpaperScaleMode,
                          spaceLockWallpaperZoomLevel = spaceLockWallpaperZoomLevel,
                          spaceLockWallpaperDimLevel = spaceLockWallpaperDimLevel,
                          spaceLockWallpaperOffsetX = spaceLockWallpaperOffsetX,
                          spaceLockWallpaperOffsetY = spaceLockWallpaperOffsetY,
                          initialApps = selectedAppObjects,
                          onResult = { success, newId ->
                            isCreating = false
                            if (success) {
                              onSpaceCreated(newId ?: "")
                              onNavigateBack()
                            }
                          }
                        )
                      }
                    }
                  },
                  enabled = !isCreating,
                  shape = ShapeRoundMd,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier
                    .weight(1.5f)
                    .testTag("btn_confirm_create_space")
                ) {
                  if (isCreating) {
                    CircularProgressIndicator(
                      color = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(20.dp),
                      strokeWidth = 2.dp
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditMode) "Save Changes" else "Create Space", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      ) { paddingValues ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
          when (currentTab) {
            0 -> {
              // TAB 1: Basics & Security (Space Name & Credentials: None, PIN, Pattern)
              Tab1BasicsAndSecurity(
                spaceName = spaceName,
                onSpaceNameChange = {
                  spaceName = it
                  spaceNameError = null
                },
                spaceNameError = spaceNameError,
                credentialOption = credentialOption,
                onCredentialOptionChange = {
                  credentialOption = it
                  pinError = null
                },
                pinValue = pinValue,
                onPinValueChange = {
                  if (it.all { ch -> ch.isDigit() } && it.length <= 8) {
                    pinValue = it
                    pinError = null
                  }
                },
                confirmPinValue = confirmPinValue,
                onConfirmPinValueChange = {
                  if (it.all { ch -> ch.isDigit() } && it.length <= 8) {
                    confirmPinValue = it
                    pinError = null
                  }
                },
                pinError = pinError,
                showPinText = showPinText,
                onToggleShowPin = { showPinText = !showPinText },
                confirmedPatternString = confirmedPatternString,
                patternRows = patternRows,
                patternCols = patternCols,
                editingSpace = editingSpace,
                onOpenPatternConfig = {
                  currentSubPage = CreateSpaceSubPage.PATTERN_GRID_CHOICE
                }
              )
            }
            1 -> {
              // TAB 2: Layout Presets with Graphical Smartphone Picture Previews & Advanced Behavioral Customization
              Tab2LayoutPresets(
                selectedPresetId = selectedLayoutPreset,
                layer1DisplayMode = layer1DisplayMode,
                onLayer1DisplayModeChange = { layer1DisplayMode = it },
                layer2DisplayMode = layer2DisplayMode,
                onLayer2DisplayModeChange = { layer2DisplayMode = it },
                layer2AccessMode = layer2AccessMode,
                onLayer2AccessModeChange = { layer2AccessMode = it },
                useLayer2 = useLayer2,
                onUseLayer2Change = { useLayer2 = it },
                dockCapacity = dockCapacity,
                onDockCapacityChange = { dockCapacity = it },
                gridColumns = gridColumns,
                isEditMode = isEditMode,
                onOpenImportLayout = {
                  importReport = null
                  isImportingLayout = false
                  showImportLayoutDialog = true
                },
                onSelectPreset = { preset ->
                  if (isEditMode || selectedLayoutPreset != preset.id) {
                    presetToConfirm = preset
                  } else {
                    selectedLayoutPreset = preset.id
                    gridColumns = preset.gridColumns
                    layer1DisplayMode = preset.layer1DisplayMode
                    layer2DisplayMode = preset.layer2DisplayMode
                    layer2AccessMode = preset.layer2AccessMode
                    useLayer2 = preset.useLayer2
                    dockCapacity = preset.dockCapacity
                  }
                }
              )
            }
            2 -> {
              // TAB 3: Wallpaper & Theme (Home, Phone Lock Screen, Space Lock Screen, App Theme)
              Tab2WallpaperAndTheme(
                homeWallpaperCategory = homeWallpaperCategory,
                onHomeWallpaperCategoryChange = { homeWallpaperCategory = it },
                homeSelectedBgColor = homeSelectedBgColor,
                onHomeSelectBgColor = { homeSelectedBgColor = it },
                homeSelectedGradientId = homeSelectedGradientId,
                onHomeSelectGradient = { homeSelectedGradientId = it },
                homeCustomImageUri = homeCustomImageUri,
                homeScaleMode = homeWallpaperScaleMode,
                homeZoomLevel = homeWallpaperZoomLevel,
                homeDimLevel = homeWallpaperDimLevel,
                homeOffsetX = homeWallpaperOffsetX,
                homeOffsetY = homeWallpaperOffsetY,
                onHomePickCustomPhoto = { homePhotoPickerLauncher.launch("image/*") },
                onHomeRemoveCustomPhoto = {
                  homeCustomImageUri = null
                  homeWallpaperCategory = "gradients"
                },
                onHomeOpenEditor = {
                  wallpaperEditorTarget = "home"
                  currentSubPage = CreateSpaceSubPage.WALLPAPER_EDITOR
                },
                phoneLockWallpaperCategory = phoneLockWallpaperCategory,
                onPhoneLockWallpaperCategoryChange = { phoneLockWallpaperCategory = it },
                phoneLockSelectedBgColor = phoneLockSelectedBgColor,
                onPhoneLockSelectBgColor = { phoneLockSelectedBgColor = it },
                phoneLockSelectedGradientId = phoneLockSelectedGradientId,
                onPhoneLockSelectGradient = { phoneLockSelectedGradientId = it },
                phoneLockCustomImageUri = phoneLockCustomImageUri,
                phoneLockScaleMode = phoneLockWallpaperScaleMode,
                phoneLockZoomLevel = phoneLockWallpaperZoomLevel,
                phoneLockDimLevel = phoneLockWallpaperDimLevel,
                phoneLockOffsetX = phoneLockWallpaperOffsetX,
                phoneLockOffsetY = phoneLockWallpaperOffsetY,
                onPhoneLockPickCustomPhoto = { phoneLockPhotoPickerLauncher.launch("image/*") },
                onPhoneLockRemoveCustomPhoto = {
                  phoneLockCustomImageUri = null
                  phoneLockWallpaperCategory = "gradients"
                },
                onPhoneLockOpenEditor = {
                  wallpaperEditorTarget = "phone_lock"
                  currentSubPage = CreateSpaceSubPage.WALLPAPER_EDITOR
                },
                spaceLockWallpaperCategory = spaceLockWallpaperCategory,
                onSpaceLockWallpaperCategoryChange = { spaceLockWallpaperCategory = it },
                spaceLockSelectedBgColor = spaceLockSelectedBgColor,
                onSpaceLockSelectBgColor = { spaceLockSelectedBgColor = it },
                spaceLockSelectedGradientId = spaceLockSelectedGradientId,
                onSpaceLockSelectGradient = { spaceLockSelectedGradientId = it },
                spaceLockCustomImageUri = spaceLockCustomImageUri,
                spaceLockScaleMode = spaceLockWallpaperScaleMode,
                spaceLockZoomLevel = spaceLockWallpaperZoomLevel,
                spaceLockDimLevel = spaceLockWallpaperDimLevel,
                spaceLockOffsetX = spaceLockWallpaperOffsetX,
                spaceLockOffsetY = spaceLockWallpaperOffsetY,
                onSpaceLockPickCustomPhoto = { spaceLockPhotoPickerLauncher.launch("image/*") },
                onSpaceLockRemoveCustomPhoto = {
                  spaceLockCustomImageUri = null
                  spaceLockWallpaperCategory = "gradients"
                },
                onSpaceLockOpenEditor = {
                  wallpaperEditorTarget = "space_lock"
                  currentSubPage = CreateSpaceSubPage.WALLPAPER_EDITOR
                },
                selectedAppTheme = selectedAppTheme,
                onSelectAppTheme = { selectedAppTheme = it }
              )
            }
            else -> {
              // TAB 4: Apps & Layout (App Selection, Grid Formats, Custom Grid Trigger)
              Tab3AppsAndLayout(
                allApps = allApps,
                searchQuery = appSearchQuery,
                onSearchQueryChange = { appSearchQuery = it },
                selectedApps = selectedAppsSet,
                onToggleApp = { appId ->
                  selectedAppsSet = if (selectedAppsSet.contains(appId)) {
                    selectedAppsSet - appId
                  } else {
                    selectedAppsSet + appId
                  }
                },
                onSelectAll = {
                  selectedAppsSet = if (appSearchQuery.isNotBlank()) {
                    val q = appSearchQuery.trim().lowercase()
                    val filtered = allApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
                    selectedAppsSet + filtered.map { it.id }.toSet()
                  } else {
                    allApps.map { it.id }.toSet()
                  }
                },
                onClearAll = {
                  selectedAppsSet = if (appSearchQuery.isNotBlank()) {
                    val q = appSearchQuery.trim().lowercase()
                    val filtered = allApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
                    selectedAppsSet - filtered.map { it.id }.toSet()
                  } else {
                    emptySet()
                  }
                },
                gridColumns = gridColumns,
                onSelectGridColumns = { gridColumns = it },
                iconSize = iconSize,
                showLabels = showLabels,
                onOpenCustomGrid = {
                  currentSubPage = CreateSpaceSubPage.CUSTOM_GRID
                },
                getBitmap = getBitmap
              )
            }
          }

          if (presetToConfirm != null) {
            val preset = presetToConfirm!!
            AlertDialog(
              onDismissRequest = { presetToConfirm = null },
              title = {
                Text(
                  text = "Apply ${preset.name} Preset?",
                  fontWeight = FontWeight.Bold
                )
              },
              text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "This will update your layout paradigm settings:",
                    style = MaterialTheme.typography.bodyMedium
                  )
                  Text(
                    text = "• Grid Columns: ${preset.gridColumns}\n" +
                      "• Dock Slots: ${preset.dockCapacity}\n" +
                      "• Workspace: ${if (preset.layer1DisplayMode == Space.DISPLAY_MODE_PAGE) "Paged" else "Vertical Scroll"}\n" +
                      "• Drawer Access: ${if (preset.layer2AccessMode == Space.ACCESS_MODE_DOCK_BUTTON) "Center Dock Button" else "Swipe Up Gesture"}\n" +
                      "• Layer 2 Enabled: ${if (preset.useLayer2) "Yes" else "No"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                  )
                  Text(
                    text = "Your existing app memberships, folders, passwords, and custom wallpapers will not be removed.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = SuccessGreen
                  )
                }
              },
              confirmButton = {
                Button(
                  onClick = {
                    selectedLayoutPreset = preset.id
                    gridColumns = preset.gridColumns
                    layer1DisplayMode = preset.layer1DisplayMode
                    layer2DisplayMode = preset.layer2DisplayMode
                    layer2AccessMode = preset.layer2AccessMode
                    useLayer2 = preset.useLayer2
                    dockCapacity = preset.dockCapacity
                    presetToConfirm = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                  Text("Apply Preset", fontWeight = FontWeight.Bold)
                }
              },
              dismissButton = {
                TextButton(onClick = { presetToConfirm = null }) {
                  Text("Cancel")
                }
              }
            )
          }

          if (showImportLayoutDialog) {
            ImportLayoutDialog(
              report = importReport,
              isImporting = isImportingLayout,
              onStartImport = {
                isImportingLayout = true
                if (isEditMode && editingSpace != null) {
                  spaceViewModel.importCurrentHomeLayout(editingSpace.id, allApps) { report ->
                    isImportingLayout = false
                    importReport = report
                    selectedAppsSet = allApps.map { it.id }.toSet()
                    dockCapacity = 5
                    gridColumns = 4
                    layer1DisplayMode = Space.DISPLAY_MODE_PAGE
                    layer2DisplayMode = Space.DISPLAY_MODE_SCROLL
                    selectedLayoutPreset = Space.PRESET_DEFAULT
                  }
                } else {
                  coroutineScope.launch {
                    delay(350)
                    val dialer = allApps.firstOrNull { it.packageName.contains("dialer") || it.packageName.contains("phone") || it.label.contains("Phone", ignoreCase = true) }
                    val messaging = allApps.firstOrNull { it.packageName.contains("messaging") || it.packageName.contains("mms") || it.packageName.contains("message") || it.label.contains("Messages", ignoreCase = true) }
                    val browser = allApps.firstOrNull { it.packageName.contains("chrome") || it.packageName.contains("browser") || it.label.contains("Chrome", ignoreCase = true) || it.label.contains("Browser", ignoreCase = true) }
                    val camera = allApps.firstOrNull { it.packageName.contains("camera") || it.label.contains("Camera", ignoreCase = true) }
                    val settings = allApps.firstOrNull { it.packageName.contains("settings") || it.label.contains("Settings", ignoreCase = true) }

                    val dockCandidates = listOfNotNull(dialer, messaging, browser, camera, settings).distinctBy { it.packageName }
                    val successes = mutableListOf<String>()
                    if (dockCandidates.isNotEmpty()) {
                      successes.add("Identified and populated essential bottom Dock apps (${dockCandidates.size} apps: Phone, Messages, Browser, Camera, Settings)")
                    }
                    successes.add("Imported ${allApps.size} launchable application shortcuts onto organized Home pages")

                    val report = ImportReport(
                      sourceLauncherPackage = "System Default",
                      sourceLauncherLabel = "Default Android Launcher",
                      successItems = successes,
                      partiallyImportedItems = listOf("Imported 4x5 standard grid alignment structure"),
                      restrictedItems = listOf(
                        "OEM-specific launcher internal SQLite databases are sandboxed by Android security",
                        "Third-party home widget state instances cannot be directly migrated across launcher packages"
                      ),
                      summary = "Successfully prepared ${allApps.size} apps and ${dockCandidates.size} dock shortcuts from standard Android configuration."
                    )
                    isImportingLayout = false
                    importReport = report
                    selectedAppsSet = allApps.map { it.id }.toSet()
                    dockCapacity = 5
                    gridColumns = 4
                    layer1DisplayMode = Space.DISPLAY_MODE_PAGE
                    layer2DisplayMode = Space.DISPLAY_MODE_SCROLL
                    selectedLayoutPreset = Space.PRESET_DEFAULT
                    spaceViewModel.postFeedback("Layout import prepared: ${report.summary}")
                  }
                }
              },
              onDismiss = {
                showImportLayoutDialog = false
                importReport = null
              }
            )
          }
        }
      }
    }
  }
}

private fun validateTab1(
  name: String,
  credential: CredentialOption,
  pin: String,
  confirmPin: String,
  pattern: String?,
  editingSpace: Space?,
  onErrorName: (String) -> Unit,
  onErrorPin: (String) -> Unit
): Boolean {
  if (name.trim().isBlank()) {
    onErrorName("Space name is required.")
    return false
  }
  if (credential == CredentialOption.PIN) {
    val keepExistingPin = editingSpace != null && editingSpace.isPinProtected && pin.isEmpty() && confirmPin.isEmpty()
    if (!keepExistingPin) {
      if (pin.length < 4) {
        onErrorPin("PIN must be at least 4 numeric digits.")
        return false
      }
      if (pin != confirmPin) {
        onErrorPin("PINs do not match. Please re-enter.")
        return false
      }
    }
  }
  if (credential == CredentialOption.PATTERN) {
    val keepExistingPattern = editingSpace != null && editingSpace.isPatternProtected && pattern.isNullOrEmpty()
    if (!keepExistingPattern) {
      if (pattern.isNullOrEmpty()) {
        onErrorPin("Please draw and confirm a pattern gesture.")
        return false
      }
    }
  }
  return true
}

// ---------------------------------------------------------------------------
// TAB 1: Basics & Security Content
// ---------------------------------------------------------------------------
@Composable
private fun Tab1BasicsAndSecurity(
  spaceName: String,
  onSpaceNameChange: (String) -> Unit,
  spaceNameError: String?,
  credentialOption: CredentialOption,
  onCredentialOptionChange: (CredentialOption) -> Unit,
  pinValue: String,
  onPinValueChange: (String) -> Unit,
  confirmPinValue: String,
  onConfirmPinValueChange: (String) -> Unit,
  pinError: String?,
  showPinText: Boolean,
  onToggleShowPin: () -> Unit,
  confirmedPatternString: String?,
  patternRows: Int,
  patternCols: Int,
  editingSpace: Space? = null,
  onOpenPatternConfig: () -> Unit
) {
  val presetNames = listOf("Personal", "Work", "Focus & Study", "Social", "Kids Zone", "Vault & Private", "Gaming")

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(vertical = 16.dp)
  ) {
    // 1. Space Name Section
    item {
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ModernSectionHeader(
            title = "Space Identity",
            subtitle = "Choose a descriptive name for your space"
          )

          OutlinedTextField(
            value = spaceName,
            onValueChange = onSpaceNameChange,
            label = { Text("Space Name") },
            placeholder = { Text("e.g. Work, Personal, Games") },
            singleLine = true,
            isError = spaceNameError != null,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
            shape = ShapeRoundMd,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              focusedBorderColor = QuantumViolet,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              focusedLabelColor = QuantumViolet,
              unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
              focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
              unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
              cursorColor = QuantumViolet
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_space_name"),
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Label,
                contentDescription = null,
                tint = QuantumViolet
              )
            }
          )

          if (spaceNameError != null) {
            Text(
              text = spaceNameError,
              color = CrimsonNova,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium
            )
          }

          // Quick Preset Name Suggestions
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            presetNames.forEach { preset ->
              SuggestionChip(
                onClick = { onSpaceNameChange(preset) },
                label = { Text(preset, fontSize = 12.sp) }
              )
            }
          }
        }
      }
    }

    // 2. Space Credentials & Security Section
    item {
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            ModernSectionHeader(
              title = "Space Security & Credentials",
              subtitle = "Protect this Space with a PIN or Pattern Lock"
            )
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = QuantumViolet
            )
          }

          // Credential Type Selector
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val options = listOf(
              CredentialOption.NONE to ("None" to Icons.Default.LockOpen),
              CredentialOption.PIN to ("PIN Code" to Icons.Default.Pin),
              CredentialOption.PATTERN to ("Pattern" to Icons.Default.Gesture),
              CredentialOption.BIOMETRIC to ("Biometric" to Icons.Default.Fingerprint)
            )

            options.forEach { (opt, meta) ->
              val isSelected = credentialOption == opt
              Surface(
                shape = ShapeRoundMd,
                color = if (isSelected) QuantumViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) BorderStroke(2.dp, QuantumViolet) else BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onCredentialOptionChange(opt) }
                  .testTag("chip_credential_${opt.name.lowercase()}")
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = meta.second,
                    contentDescription = null,
                    tint = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = meta.first,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          // Credential Detail Inputs
          when (credentialOption) {
            CredentialOption.NONE -> {
              Text(
                text = "No password required. Anyone can switch into this Space.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            CredentialOption.BIOMETRIC -> {
              Surface(
                shape = ShapeRoundMd,
                color = QuantumViolet.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, QuantumViolet.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("card_biometric_security_info")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = QuantumViolet,
                    modifier = Modifier.size(36.dp)
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = "Biometric Protection Enabled",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "This space is protected by your device's biometric sensor (fingerprint or face unlock).",
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }

            CredentialOption.PIN -> {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editingSpace != null && editingSpace.isPinProtected) {
                  Surface(
                    shape = ShapeRoundSm,
                    color = EmeraldCore.copy(alpha = 0.12f),
                    border = BorderStroke(AppDimens.BorderThin, EmeraldCore.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "🔒 Currently protected by PIN. Enter new digits below to change, or leave blank to keep your current PIN.",
                      fontSize = 12.sp,
                      color = EmeraldCore,
                      modifier = Modifier.padding(10.dp)
                    )
                  }
                }

                OutlinedTextField(
                  value = pinValue,
                  onPinValueChange,
                  label = { Text(if (editingSpace != null && editingSpace.isPinProtected) "New PIN (leave blank to keep)" else "PIN (4-8 digits)") },
                  visualTransformation = if (showPinText) VisualTransformation.None else PasswordVisualTransformation(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
                  shape = ShapeRoundMd,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = QuantumViolet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = QuantumViolet,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = QuantumViolet
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_space_pin"),
                  trailingIcon = {
                    IconButton(onClick = onToggleShowPin) {
                      Icon(
                        imageVector = if (showPinText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle PIN visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                )

                OutlinedTextField(
                  value = confirmPinValue,
                  onValueChange = onConfirmPinValueChange,
                  label = { Text(if (editingSpace != null && editingSpace.isPinProtected) "Confirm New PIN" else "Confirm PIN") },
                  visualTransformation = if (showPinText) VisualTransformation.None else PasswordVisualTransformation(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
                  shape = ShapeRoundMd,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = QuantumViolet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = QuantumViolet,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = QuantumViolet
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_space_confirm_pin")
                )

                if (pinError != null) {
                  Text(
                    text = pinError,
                    color = CrimsonNova,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }

            CredentialOption.PATTERN -> {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (confirmedPatternString != null) {
                  Surface(
                    shape = ShapeRoundMd,
                    color = EmeraldCore.copy(alpha = 0.12f),
                    border = BorderStroke(AppDimens.BorderThin, EmeraldCore.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.CheckCircle,
                          contentDescription = null,
                          tint = EmeraldCore,
                          modifier = Modifier.size(24.dp)
                        )
                        Column {
                          Text(
                            text = "New Pattern Configured",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldCore,
                            fontSize = 13.sp
                          )
                          Text(
                            text = "${patternRows}×${patternCols} grid (${patternRows * patternCols} dots) verified",
                            color = EmeraldCore.copy(alpha = 0.8f),
                            fontSize = 11.sp
                          )
                        }
                      }
                      TextButton(onClick = onOpenPatternConfig) {
                        Text("Change", fontWeight = FontWeight.Bold, color = EmeraldCore)
                      }
                    }
                  }
                } else if (editingSpace != null && editingSpace.isPatternProtected) {
                  Surface(
                    shape = ShapeRoundMd,
                    color = EmeraldCore.copy(alpha = 0.12f),
                    border = BorderStroke(AppDimens.BorderThin, EmeraldCore.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.CheckCircle,
                          contentDescription = null,
                          tint = EmeraldCore,
                          modifier = Modifier.size(24.dp)
                        )
                        Column {
                          Text(
                            text = "Existing Pattern Lock Active",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldCore,
                            fontSize = 13.sp
                          )
                          Text(
                            text = "${patternRows}×${patternCols} grid pattern active. Tap 'Change' to record a new pattern.",
                            color = EmeraldCore.copy(alpha = 0.8f),
                            fontSize = 11.sp
                          )
                        }
                      }
                      TextButton(onClick = onOpenPatternConfig) {
                        Text("Change", fontWeight = FontWeight.Bold, color = EmeraldCore)
                      }
                    }
                  }
                } else {
                  Button(
                    onClick = onOpenPatternConfig,
                    shape = ShapeRoundMd,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("btn_configure_pattern")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Gesture,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Format & Draw Pattern", fontWeight = FontWeight.Bold)
                  }
                }

                if (pinError != null) {
                  Text(
                    text = pinError,
                    color = CrimsonNova,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// TAB 2: Layout Presets Page (Visual Picture Previews of Screen Paradigms & Advanced Customization)
// ---------------------------------------------------------------------------
@Composable
private fun Tab2LayoutPresets(
  selectedPresetId: String,
  layer1DisplayMode: String,
  onLayer1DisplayModeChange: (String) -> Unit,
  layer2DisplayMode: String,
  onLayer2DisplayModeChange: (String) -> Unit,
  layer2AccessMode: String,
  onLayer2AccessModeChange: (String) -> Unit,
  useLayer2: Boolean,
  onUseLayer2Change: (Boolean) -> Unit,
  dockCapacity: Int,
  onDockCapacityChange: (Int) -> Unit,
  gridColumns: Int,
  isEditMode: Boolean = false,
  onOpenImportLayout: () -> Unit = {},
  onSelectPreset: (LayoutPreset) -> Unit
) {
  val presets = remember { LayoutPreset.ALL_PRESETS }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
  ) {
    item {
      // Explanatory Header Card
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(ShapeRoundSm)
              .background(QuantumViolet.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.DashboardCustomize,
              contentDescription = null,
              tint = QuantumViolet,
              modifier = Modifier.size(28.dp)
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Space Layout Paradigm",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Select how apps and drawers are organized in this space. Picture previews show the real layout behavior.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Import Android Layout Feature Card
    item {
      ModernCard(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_import_layout_presets")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(QuantumViolet),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.MoveToInbox,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Import Android Layout",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Auto-detect default system apps (Phone, Messages, Browser, Camera, Settings), dock setup, and organize apps into home workspace pages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Button(
            onClick = onOpenImportLayout,
            shape = ShapeRoundMd,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_import_layout_presets_tab")
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isEditMode) "Import System Layout to this Space" else "Import Android Layout Preset",
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Active Behavioral Settings & Independent Mode Customization Card
    item {
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            ModernSectionHeader(
              title = "Behavioral Properties",
              subtitle = "Customize workspace & drawer modes independently"
            )
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = QuantumViolet,
              modifier = Modifier.size(20.dp)
            )
          }

          // Active Summary Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            SuggestionChip(
              onClick = {},
              label = { Text("${gridColumns} Cols", fontSize = 11.sp) }
            )
            SuggestionChip(
              onClick = {},
              label = {
                Text(
                  if (layer1DisplayMode == Space.DISPLAY_MODE_PAGE) "Paged Workspace" else "Scroll Workspace",
                  fontSize = 11.sp
                )
              }
            )
            SuggestionChip(
              onClick = {},
              label = {
                Text(
                  if (useLayer2) "Layer 2 Enabled" else "No Drawer (Home Only)",
                  fontSize = 11.sp
                )
              }
            )
            if (useLayer2) {
              SuggestionChip(
                onClick = {},
                label = {
                  Text(
                    if (layer2AccessMode == Space.ACCESS_MODE_DOCK_BUTTON) "Dock Button" else "Swipe Up",
                    fontSize = 11.sp
                  )
                }
              )
            }
            SuggestionChip(
              onClick = {},
              label = { Text("${dockCapacity} Dock Slots", fontSize = 11.sp) }
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // 1. Layer 1 Display Mode (Workspace)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Layer 1 Workspace Display Mode",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = layer1DisplayMode == Space.DISPLAY_MODE_PAGE,
                onClick = { onLayer1DisplayModeChange(Space.DISPLAY_MODE_PAGE) },
                label = { Text("Paged Pages (Default)", fontSize = 12.sp) },
                leadingIcon = if (layer1DisplayMode == Space.DISPLAY_MODE_PAGE) {
                  { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
              )
              FilterChip(
                selected = layer1DisplayMode == Space.DISPLAY_MODE_SCROLL,
                onClick = { onLayer1DisplayModeChange(Space.DISPLAY_MODE_SCROLL) },
                label = { Text("Vertical Scroll", fontSize = 12.sp) },
                leadingIcon = if (layer1DisplayMode == Space.DISPLAY_MODE_SCROLL) {
                  { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
              )
            }
          }

          // 2. Use Layer 2 (App Drawer) Toggle
          Surface(
            shape = ShapeRoundSm,
            color = if (useLayer2) QuantumViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(AppDimens.BorderThin, if (useLayer2) QuantumViolet.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Enable Layer 2 (All-Apps Drawer)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (useLayer2) "Secondary app library drawer is active" else "Disabled: All space apps stay on Home workspace",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = useLayer2,
                onCheckedChange = onUseLayer2Change,
                modifier = Modifier.testTag("switch_use_layer2")
              )
            }
          }

          // 3. Layer 2 Display Mode (if enabled)
          if (useLayer2) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Layer 2 Drawer Display Mode",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FilterChip(
                  selected = layer2DisplayMode == Space.DISPLAY_MODE_SCROLL,
                  onClick = { onLayer2DisplayModeChange(Space.DISPLAY_MODE_SCROLL) },
                  label = { Text("Vertical Scroll (Default)", fontSize = 12.sp) },
                  leadingIcon = if (layer2DisplayMode == Space.DISPLAY_MODE_SCROLL) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                  } else null,
                  modifier = Modifier.weight(1f)
                )
                FilterChip(
                  selected = layer2DisplayMode == Space.DISPLAY_MODE_PAGE,
                  onClick = { onLayer2DisplayModeChange(Space.DISPLAY_MODE_PAGE) },
                  label = { Text("Horizontal Paged", fontSize = 12.sp) },
                  leadingIcon = if (layer2DisplayMode == Space.DISPLAY_MODE_PAGE) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                  } else null,
                  modifier = Modifier.weight(1f)
                )
              }
            }

            // 4. Layer 2 Access Mode
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Layer 2 Drawer Access Mode",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FilterChip(
                  selected = layer2AccessMode == Space.ACCESS_MODE_DOCK_BUTTON,
                  onClick = { onLayer2AccessModeChange(Space.ACCESS_MODE_DOCK_BUTTON) },
                  label = { Text("Center Dock Button", fontSize = 12.sp) },
                  leadingIcon = if (layer2AccessMode == Space.ACCESS_MODE_DOCK_BUTTON) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                  } else null,
                  modifier = Modifier.weight(1f)
                )
                FilterChip(
                  selected = layer2AccessMode == Space.ACCESS_MODE_SWIPE_UP,
                  onClick = { onLayer2AccessModeChange(Space.ACCESS_MODE_SWIPE_UP) },
                  label = { Text("Swipe Up Gesture", fontSize = 12.sp) },
                  leadingIcon = if (layer2AccessMode == Space.ACCESS_MODE_SWIPE_UP) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                  } else null,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }

          // 5. Dock Capacity
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Dock Bar Capacity ($dockCapacity apps)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              (3..7).forEach { slots ->
                val isSelected = dockCapacity == slots
                FilterChip(
                  selected = isSelected,
                  onClick = { onDockCapacityChange(slots) },
                  label = { Text("$slots", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Layout Presets (${presets.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
      )
    }

    items(presets, key = { it.id }) { preset ->
      LayoutPresetVisualCard(
        preset = preset,
        isSelected = preset.id == selectedPresetId,
        onSelect = { onSelectPreset(preset) },
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

// ---------------------------------------------------------------------------
// TAB 3: Wallpaper & Theme Content (4 Sections)
// ---------------------------------------------------------------------------
@Composable
private fun Tab2WallpaperAndTheme(
  homeWallpaperCategory: String,
  onHomeWallpaperCategoryChange: (String) -> Unit,
  homeSelectedBgColor: Long,
  onHomeSelectBgColor: (Long) -> Unit,
  homeSelectedGradientId: String,
  onHomeSelectGradient: (String) -> Unit,
  homeCustomImageUri: String?,
  homeScaleMode: String = "crop",
  homeZoomLevel: Float = 1.0f,
  homeDimLevel: Float = 0.20f,
  homeOffsetX: Float = 0.0f,
  homeOffsetY: Float = 0.0f,
  onHomePickCustomPhoto: () -> Unit,
  onHomeRemoveCustomPhoto: () -> Unit,
  onHomeOpenEditor: () -> Unit,

  phoneLockWallpaperCategory: String,
  onPhoneLockWallpaperCategoryChange: (String) -> Unit,
  phoneLockSelectedBgColor: Long,
  onPhoneLockSelectBgColor: (Long) -> Unit,
  phoneLockSelectedGradientId: String,
  onPhoneLockSelectGradient: (String) -> Unit,
  phoneLockCustomImageUri: String?,
  phoneLockScaleMode: String = "crop",
  phoneLockZoomLevel: Float = 1.0f,
  phoneLockDimLevel: Float = 0.20f,
  phoneLockOffsetX: Float = 0.0f,
  phoneLockOffsetY: Float = 0.0f,
  onPhoneLockPickCustomPhoto: () -> Unit,
  onPhoneLockRemoveCustomPhoto: () -> Unit,
  onPhoneLockOpenEditor: () -> Unit,

  spaceLockWallpaperCategory: String,
  onSpaceLockWallpaperCategoryChange: (String) -> Unit,
  spaceLockSelectedBgColor: Long,
  onSpaceLockSelectBgColor: (Long) -> Unit,
  spaceLockSelectedGradientId: String,
  onSpaceLockSelectGradient: (String) -> Unit,
  spaceLockCustomImageUri: String?,
  spaceLockScaleMode: String = "crop",
  spaceLockZoomLevel: Float = 1.0f,
  spaceLockDimLevel: Float = 0.20f,
  spaceLockOffsetX: Float = 0.0f,
  spaceLockOffsetY: Float = 0.0f,
  onSpaceLockPickCustomPhoto: () -> Unit,
  onSpaceLockRemoveCustomPhoto: () -> Unit,
  onSpaceLockOpenEditor: () -> Unit,

  selectedAppTheme: String,
  onSelectAppTheme: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(vertical = 16.dp)
  ) {
    // Home Wallpaper Section
    item {
      WallpaperSectionCard(
        title = "Home Wallpaper",
        description = "Background displayed on your Space launcher Home screen",
        icon = Icons.Default.Home,
        category = homeWallpaperCategory,
        onCategoryChange = onHomeWallpaperCategoryChange,
        selectedBgColor = homeSelectedBgColor,
        onSelectBgColor = onHomeSelectBgColor,
        selectedGradientId = homeSelectedGradientId,
        onSelectGradient = onHomeSelectGradient,
        customImageUri = homeCustomImageUri,
        scaleMode = homeScaleMode,
        zoomLevel = homeZoomLevel,
        dimLevel = homeDimLevel,
        offsetX = homeOffsetX,
        offsetY = homeOffsetY,
        onPickCustomPhoto = onHomePickCustomPhoto,
        onRemoveCustomPhoto = onHomeRemoveCustomPhoto,
        onOpenEditor = onHomeOpenEditor,
        testTagPrefix = "home_wallpaper"
      )
    }

    // Phone Lock Screen Wallpaper Section
    item {
      WallpaperSectionCard(
        title = "Phone Lock Screen Wallpaper",
        description = "Wallpaper applied to your device lock screen when this Space is active",
        icon = Icons.Default.Smartphone,
        category = phoneLockWallpaperCategory,
        onCategoryChange = onPhoneLockWallpaperCategoryChange,
        selectedBgColor = phoneLockSelectedBgColor,
        onSelectBgColor = onPhoneLockSelectBgColor,
        selectedGradientId = phoneLockSelectedGradientId,
        onSelectGradient = onPhoneLockSelectGradient,
        customImageUri = phoneLockCustomImageUri,
        scaleMode = phoneLockScaleMode,
        zoomLevel = phoneLockZoomLevel,
        dimLevel = phoneLockDimLevel,
        offsetX = phoneLockOffsetX,
        offsetY = phoneLockOffsetY,
        onPickCustomPhoto = onPhoneLockPickCustomPhoto,
        onRemoveCustomPhoto = onPhoneLockRemoveCustomPhoto,
        onOpenEditor = onPhoneLockOpenEditor,
        testTagPrefix = "phone_lock_wallpaper"
      )
    }

    // Space Lock Screen Wallpaper Section
    item {
      WallpaperSectionCard(
        title = "Space Lock Screen Wallpaper",
        description = "Background displayed when entering PIN or Pattern to unlock this Space",
        icon = Icons.Default.Lock,
        category = spaceLockWallpaperCategory,
        onCategoryChange = onSpaceLockWallpaperCategoryChange,
        selectedBgColor = spaceLockSelectedBgColor,
        onSelectBgColor = onSpaceLockSelectBgColor,
        selectedGradientId = spaceLockSelectedGradientId,
        onSelectGradient = onSpaceLockSelectGradient,
        customImageUri = spaceLockCustomImageUri,
        scaleMode = spaceLockScaleMode,
        zoomLevel = spaceLockZoomLevel,
        dimLevel = spaceLockDimLevel,
        offsetX = spaceLockOffsetX,
        offsetY = spaceLockOffsetY,
        onPickCustomPhoto = onSpaceLockPickCustomPhoto,
        onRemoveCustomPhoto = onSpaceLockRemoveCustomPhoto,
        onOpenEditor = onSpaceLockOpenEditor,
        testTagPrefix = "space_lock_wallpaper"
      )
    }

    // Theme for Apps Section
    item {
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            ModernSectionHeader(
              title = "Theme for Apps",
              subtitle = "Select visual color theme & palette for applications in this Space"
            )
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = null,
              tint = QuantumViolet
            )
          }

          // Theme options list
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_APP_THEMES.forEach { theme ->
              val isSelected = selectedAppTheme == theme.id
              Surface(
                shape = ShapeRoundMd,
                color = if (isSelected) QuantumViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) BorderStroke(2.dp, QuantumViolet) else BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectAppTheme(theme.id) }
                  .testTag("chip_theme_${theme.id.lowercase()}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  // Mini preview icon chip
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(ShapeRoundSm)
                      .background(theme.iconBackgroundColor)
                      .border(1.dp, theme.primaryColor.copy(alpha = 0.5f), ShapeRoundSm),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Apps,
                      contentDescription = null,
                      tint = theme.primaryColor,
                      modifier = Modifier.size(20.dp)
                    )
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = theme.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = theme.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  // Color accent dots
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                      modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor)
                    )
                    Box(
                      modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(theme.secondaryColor)
                    )
                  }

                  Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WallpaperSectionCard(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  category: String,
  onCategoryChange: (String) -> Unit,
  selectedBgColor: Long,
  onSelectBgColor: (Long) -> Unit,
  selectedGradientId: String,
  onSelectGradient: (String) -> Unit,
  customImageUri: String?,
  scaleMode: String = "crop",
  zoomLevel: Float = 1.0f,
  dimLevel: Float = 0.20f,
  offsetX: Float = 0.0f,
  offsetY: Float = 0.0f,
  onPickCustomPhoto: () -> Unit,
  onRemoveCustomPhoto: () -> Unit,
  onOpenEditor: (() -> Unit)? = null,
  testTagPrefix: String
) {
  val context = LocalContext.current

  ModernCard(
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        ModernSectionHeader(
          title = title,
          subtitle = description
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = QuantumViolet
        )
      }

      // Live Wallpaper Mockup Preview
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp)
          .clip(ShapeRoundMd)
          .background(
            when (category) {
              "gradients" -> {
                val grad = PRESET_GRADIENTS.firstOrNull { it.id == selectedGradientId } ?: PRESET_GRADIENTS.first()
                Brush.verticalGradient(grad.colors)
              }
              "photo" -> {
                if (customImageUri != null) Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                else Brush.verticalGradient(listOf(Color(selectedBgColor), Color(selectedBgColor)))
              }
              else -> Brush.verticalGradient(listOf(Color(selectedBgColor), Color(selectedBgColor)))
            }
          )
          .border(BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant), ShapeRoundMd)
          .clickable(enabled = category == "photo" && customImageUri != null && onOpenEditor != null) {
            onOpenEditor?.invoke()
          },
        contentAlignment = Alignment.Center
      ) {
        if (category == "photo" && customImageUri != null) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(customImageUri)
              .crossfade(true)
              .build(),
            contentDescription = "Custom Photo Wallpaper",
            contentScale = if (scaleMode == "crop") ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                scaleX = zoomLevel
                scaleY = zoomLevel
                translationX = offsetX
                translationY = offsetY
              }
          )
          if (dimLevel > 0f) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimLevel))
            )
          }
        }

        // Mock UI overlay
        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          repeat(4) { i ->
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(ShapeRoundSm)
                .background(Color.White.copy(alpha = 0.85f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (i) {
                  0 -> Icons.Default.Apps
                  1 -> Icons.Default.Call
                  2 -> Icons.Default.Camera
                  else -> Icons.Default.Folder
                },
                contentDescription = null,
                tint = QuantumViolet,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // Category Chips: Gradients, Solid Colors, Custom Photo
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val categories = listOf(
          "gradients" to "Gradients",
          "colors" to "Solid Colors",
          "photo" to "Custom Photo"
        )

        categories.forEach { (catKey, catLabel) ->
          val isSelected = category == catKey
          FilterChip(
            selected = isSelected,
            onClick = { onCategoryChange(catKey) },
            label = { Text(catLabel, fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Category Specific Content
      when (category) {
        "gradients" -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            PRESET_GRADIENTS.forEach { grad ->
              val isSelected = selectedGradientId == grad.id
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(ShapeRoundSm)
                  .clickable { onSelectGradient(grad.id) }
                  .padding(4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(width = 68.dp, height = 44.dp)
                    .clip(ShapeRoundSm)
                    .background(Brush.verticalGradient(grad.colors))
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color = if (isSelected) QuantumViolet else Color.White.copy(alpha = 0.2f),
                      shape = ShapeRoundSm
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Selected",
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = grad.name,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
            }
          }
        }

        "colors" -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            PRESET_BACKGROUND_COLORS.forEach { (colorVal, colorName) ->
              val isSelected = selectedBgColor == colorVal
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(ShapeRoundSm)
                  .clickable { onSelectBgColor(colorVal) }
                  .padding(4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(colorVal))
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.outlineVariant,
                      shape = CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Selected",
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = colorName,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
            }
          }
        }

        "photo" -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (customImageUri != null) {
              if (onOpenEditor != null) {
                Button(
                  onClick = onOpenEditor,
                  shape = ShapeRoundSm,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_edit_${testTagPrefix}_preview")
                ) {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Edit & Live Launcher Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = onPickCustomPhoto,
                  shape = ShapeRoundSm,
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Change Photo", fontSize = 12.sp)
                }
                OutlinedButton(
                  onClick = onRemoveCustomPhoto,
                  shape = ShapeRoundSm,
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonNova),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Remove Photo", fontSize = 12.sp)
                }
              }
            } else {
              Button(
                onClick = onPickCustomPhoto,
                shape = ShapeRoundMd,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_pick_${testTagPrefix}_photo")
              ) {
                Icon(
                  imageVector = Icons.Default.AddPhotoAlternate,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Photo from Gallery", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// TAB 3: Apps & Layout Content
// ---------------------------------------------------------------------------
@Composable
private fun Tab3AppsAndLayout(
  allApps: List<DiscoveredApp>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  selectedApps: Set<String>,
  onToggleApp: (String) -> Unit,
  onSelectAll: () -> Unit,
  onClearAll: () -> Unit,
  gridColumns: Int,
  onSelectGridColumns: (Int) -> Unit,
  iconSize: String,
  showLabels: Boolean,
  onOpenCustomGrid: () -> Unit,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?
) {
  val filteredApps = remember(allApps, searchQuery) {
    if (searchQuery.isBlank()) allApps
    else {
      val q = searchQuery.trim().lowercase()
      allApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(vertical = 12.dp)
  ) {
    // 1. Grid Format Selection Section
    item {
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Grid Layout Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "$gridColumns Columns · $iconSize Icons",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            TextButton(
              onClick = onOpenCustomGrid,
              modifier = Modifier.testTag("btn_open_custom_grid_page")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = QuantumViolet,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Custom...", color = QuantumViolet, fontWeight = FontWeight.Bold)
            }
          }

          // Preset Grid Formats
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val presets = listOf(
              3 to "3 Cols\n(Spacious)",
              4 to "4 Cols\n(Standard)",
              5 to "5 Cols\n(Compact)",
              6 to "6 Cols\n(Dense)"
            )

            presets.forEach { (cols, label) ->
              val isSelected = gridColumns == cols
              Surface(
                shape = ShapeRoundMd,
                color = if (isSelected) QuantumViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) BorderStroke(2.dp, QuantumViolet) else BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onSelectGridColumns(cols) }
                  .testTag("chip_grid_preset_$cols")
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = when (cols) {
                      3 -> Icons.Default.ViewWeek
                      4 -> Icons.Default.GridView
                      5 -> Icons.Default.Apps
                      else -> Icons.Default.Window
                    },
                    contentDescription = null,
                    tint = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = label,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          }
        }
      }
    }

    // 2. Search & Bulk Selection Header
    item {
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Applications Membership",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${selectedApps.size} of ${allApps.size} apps included",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Surface(
              shape = CircleShape,
              color = QuantumViolet.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, QuantumViolet.copy(alpha = 0.3f))
            ) {
              Text(
                text = "${selectedApps.size} Active",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = QuantumViolet,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          // Search Field
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search installed applications...") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            shape = ShapeRoundMd,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              focusedBorderColor = QuantumViolet,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
              unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
              cursorColor = QuantumViolet
            ),
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_search_space_apps")
          )

          // Prominent Bulk Selection Action Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onSelectAll,
              shape = ShapeRoundMd,
              border = BorderStroke(1.dp, QuantumViolet),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = QuantumViolet
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_select_all_apps")
            ) {
              Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Select All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = onClearAll,
              shape = ShapeRoundMd,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_unselect_all_apps")
            ) {
              Icon(
                imageVector = Icons.Default.ClearAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Unselect All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // 3. Applications List Container
    item {
      ModernCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          if (filteredApps.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No applications matching search query.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            filteredApps.forEachIndexed { index, app ->
              val isSelected = selectedApps.contains(app.id)
              Surface(
                color = if (isSelected) QuantumViolet.copy(alpha = 0.08f) else Color.Transparent,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onToggleApp(app.id) }
                  .testTag("app_toggle_row_${app.packageName}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  AsyncAppIcon(
                    app = app,
                    getBitmap = getBitmap,
                    contentDescription = app.label,
                    modifier = Modifier.size(38.dp)
                  )

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = app.label,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = app.packageName,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleApp(app.id) },
                    colors = CheckboxDefaults.colors(
                      checkedColor = QuantumViolet,
                      uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                  )
                }
              }

              if (index < filteredApps.size - 1) {
                HorizontalDivider(
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  thickness = 0.5.dp,
                  modifier = Modifier.padding(horizontal = 14.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// SUB-PAGE: Pattern Grid Choice Screen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternGridChoiceScreen(
  selectedOption: String, // "6dots", "9dots", "custom"
  customRows: Int,
  customCols: Int,
  onOptionSelected: (option: String, rows: Int, cols: Int) -> Unit,
  onNavigateBack: () -> Unit,
  onProceedToDraw: () -> Unit
) {
  var activeOption by remember { mutableStateOf(selectedOption) }
  var rows by remember { mutableIntStateOf(customRows) }
  var cols by remember { mutableIntStateOf(customCols) }

  Scaffold(
    containerColor = LightBackground,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Choose Pattern Grid",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Select number of gesture connection dots",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
      )
    },
    bottomBar = {
      Surface(
        color = LightSurfaceContainer,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Cancel")
          }
          Button(
            onClick = {
              val (r, c) = when (activeOption) {
                "6dots" -> 2 to 3
                "9dots" -> 3 to 3
                else -> rows to cols
              }
              onOptionSelected(activeOption, r, c)
              onProceedToDraw()
            },
            modifier = Modifier
              .weight(1.5f)
              .testTag("btn_proceed_pattern_draw"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
          ) {
            Text("Continue to Draw", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Six Dots Option
      PatternGridOptionCard(
        title = "6 Dots Grid (2 × 3)",
        subtitle = "Compact 6-point matrix for fast gesture unlocking",
        rows = 2,
        cols = 3,
        isSelected = activeOption == "6dots",
        onClick = {
          activeOption = "6dots"
          onOptionSelected("6dots", 2, 3)
        }
      )

      // 2. Nine Dots Option (Standard)
      PatternGridOptionCard(
        title = "9 Dots Grid (3 × 3 Standard)",
        subtitle = "Standard Android 9-point pattern matrix with high security",
        rows = 3,
        cols = 3,
        isSelected = activeOption == "9dots",
        badge = "POPULAR",
        onClick = {
          activeOption = "9dots"
          onOptionSelected("9dots", 3, 3)
        }
      )

      // 3. Custom Dots Option
      PatternGridOptionCard(
        title = "Custom Grid (${rows} × ${cols} = ${rows * cols} Dots)",
        subtitle = "Customize matrix dimensions from 2×2 up to 5×5",
        rows = rows,
        cols = cols,
        isSelected = activeOption == "custom",
        onClick = {
          activeOption = "custom"
          onOptionSelected("custom", rows, cols)
        }
      )

      // Fine controls for Custom Dot Matrix
      if (activeOption == "custom") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PrimaryContainerLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "Custom Dimensions",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = PrimaryPurpleDark
            )

            // Rows Slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Rows: $rows", fontWeight = FontWeight.SemiBold, color = TextPrimary)
              Slider(
                value = rows.toFloat(),
                onValueChange = {
                  rows = it.toInt()
                  onOptionSelected("custom", rows, cols)
                },
                valueRange = 2f..5f,
                steps = 2,
                modifier = Modifier.width(180.dp),
                colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurpleDark)
              )
            }

            // Cols Slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Columns: $cols", fontWeight = FontWeight.SemiBold, color = TextPrimary)
              Slider(
                value = cols.toFloat(),
                onValueChange = {
                  cols = it.toInt()
                  onOptionSelected("custom", rows, cols)
                },
                valueRange = 2f..5f,
                steps = 2,
                modifier = Modifier.width(180.dp),
                colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurpleDark)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PatternGridOptionCard(
  title: String,
  subtitle: String,
  rows: Int,
  cols: Int,
  isSelected: Boolean,
  badge: String? = null,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) PrimaryContainerLight else LightSurfaceContainerLow
    ),
    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryPurple) else null,
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Miniature visual dot grid diagram
      Box(
        modifier = Modifier
          .size(width = 68.dp, height = 58.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(if (isSelected) PrimaryPurple.copy(alpha = 0.15f) else LightSurfaceContainerHigh)
          .padding(6.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          verticalArrangement = Arrangement.SpaceEvenly,
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxSize()
        ) {
          repeat(rows) {
            Row(
              horizontalArrangement = Arrangement.SpaceEvenly,
              modifier = Modifier.fillMaxWidth()
            ) {
              repeat(cols) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryPurpleDark else TextSecondary)
                )
              }
            }
          }
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) PrimaryPurpleDark else TextPrimary
          )
          if (badge != null) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = PrimaryPurpleDark
            ) {
              Text(
                text = badge,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }

      Icon(
        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (isSelected) PrimaryPurple else TextSecondary
      )
    }
  }
}

// ---------------------------------------------------------------------------
// SUB-PAGE: Pattern Drawing & Confirmation Screen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternDrawingScreen(
  rows: Int,
  cols: Int,
  step: Int, // 1: Draw first, 2: Confirm
  feedbackMessage: String?,
  isError: Boolean,
  clearTrigger: Any?,
  onPatternRecorded: (patternStr: String, nodeCount: Int) -> Unit,
  onReset: () -> Unit,
  onNavigateBack: () -> Unit
) {
  Scaffold(
    containerColor = LightBackground,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = if (step == 1) "Draw Space Pattern" else "Confirm Space Pattern",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = if (step == 1) "Step 1 of 2: Record gesture (${rows}×${cols} grid)" else "Step 2 of 2: Re-draw pattern to verify",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
        },
        actions = {
          TextButton(onClick = onReset) {
            Text("Clear", color = PrimaryPurpleDark, fontWeight = FontWeight.SemiBold)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = if (isError) Color(0xFFFFEBEE) else PrimaryContainerLight,
          modifier = Modifier.size(54.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Gesture,
              contentDescription = null,
              tint = if (isError) Color(0xFFC62828) else PrimaryPurpleDark,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Text(
          text = if (step == 1) "Draw an unlock pattern" else "Draw pattern again to confirm",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = if (isError) Color(0xFFC62828) else TextPrimary,
          textAlign = TextAlign.Center
        )

        Text(
          text = feedbackMessage ?: "Connect at least 4 dots to ensure strong security.",
          style = MaterialTheme.typography.bodySmall,
          color = if (isError) Color(0xFFC62828) else TextSecondary,
          textAlign = TextAlign.Center
        )
      }

      // Pattern Lock Canvas inside prominent, elevated surface
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = LightSurfaceContainerLowest,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, LightSurfaceContainerHigh),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 12.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          PatternLockCanvas(
            rows = rows,
            cols = cols,
            isError = isError,
            clearTrigger = clearTrigger,
            onPatternComplete = { nodes, encoded ->
              onPatternRecorded(encoded, nodes.size)
            }
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        TextButton(onClick = onReset) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reset Pattern", fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// SUB-PAGE: Custom Grid Configuration Screen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomGridScreen(
  initialColumns: Int,
  initialIconSize: String,
  initialLabelVisibility: Boolean,
  onNavigateBack: () -> Unit,
  onApplyCustomGrid: (cols: Int, iconSize: String, showLabels: Boolean) -> Unit
) {
  var columns by remember { mutableIntStateOf(initialColumns) }
  var iconSize by remember { mutableStateOf(initialIconSize) }
  var showLabels by remember { mutableStateOf(initialLabelVisibility) }

  Scaffold(
    containerColor = LightBackground,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Custom Grid Designer",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Design a tailor-made launcher app layout",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
      )
    },
    bottomBar = {
      Surface(
        color = LightSurfaceContainer,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Cancel")
          }
          Button(
            onClick = { onApplyCustomGrid(columns, iconSize, showLabels) },
            modifier = Modifier
              .weight(1.5f)
              .testTag("btn_apply_custom_grid"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Grid Format", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Live Interactive Grid Mockup Preview
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Live Layout Preview ($columns Columns)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(LightSurfaceContainerHigh)
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            LazyVerticalGrid(
              columns = GridCells.Fixed(columns),
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(count = columns * 2) { idx ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Surface(
                    shape = RoundedCornerShape(
                      when (iconSize) {
                        Space.ICON_SIZE_SMALL -> 8.dp
                        Space.ICON_SIZE_LARGE -> 14.dp
                        else -> 10.dp
                      }
                    ),
                    color = PrimaryContainerLight,
                    modifier = Modifier.size(
                      when (iconSize) {
                        Space.ICON_SIZE_SMALL -> 30.dp
                        Space.ICON_SIZE_LARGE -> 46.dp
                        else -> 38.dp
                      }
                    )
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = PrimaryPurpleDark,
                        modifier = Modifier.size(
                          when (iconSize) {
                            Space.ICON_SIZE_SMALL -> 14.dp
                            Space.ICON_SIZE_LARGE -> 24.dp
                            else -> 18.dp
                          }
                        )
                      )
                    }
                  }
                  if (showLabels) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "App ${idx + 1}",
                      fontSize = 9.sp,
                      color = TextSecondary,
                      maxLines = 1
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. Custom Grid Column Controls
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Grid Columns: $columns", fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              IconButton(
                onClick = { if (columns > 2) columns-- },
                enabled = columns > 2,
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(LightSurfaceContainerHigh)
              ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Columns")
              }
              IconButton(
                onClick = { if (columns < 8) columns++ },
                enabled = columns < 8,
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(LightSurfaceContainerHigh)
              ) {
                Icon(Icons.Default.Add, contentDescription = "Increase Columns")
              }
            }
          }

          Slider(
            value = columns.toFloat(),
            onValueChange = { columns = it.toInt() },
            valueRange = 2f..8f,
            steps = 5,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurpleDark)
          )

          HorizontalDivider(color = LightSurfaceContainerHigh)

          // Icon Size selection
          Text("Icon Sizing", fontWeight = FontWeight.Bold, color = TextPrimary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val sizes = listOf(
              Space.ICON_SIZE_SMALL to "Small",
              Space.ICON_SIZE_MEDIUM to "Medium",
              Space.ICON_SIZE_LARGE to "Large"
            )
            sizes.forEach { (szKey, szLabel) ->
              val isSelected = iconSize == szKey
              FilterChip(
                selected = isSelected,
                onClick = { iconSize = szKey },
                label = { Text(szLabel) },
                modifier = Modifier.weight(1f)
              )
            }
          }

          HorizontalDivider(color = LightSurfaceContainerHigh)

          // App Label Visibility Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Show App Labels", fontWeight = FontWeight.Bold, color = TextPrimary)
              Text("Display application names below icons", fontSize = 12.sp, color = TextSecondary)
            }
            Switch(
              checked = showLabels,
              onCheckedChange = { showLabels = it },
              colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
            )
          }
        }
      }
    }
  }
}
