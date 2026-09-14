package com.multispace.presentation

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.multispace.R
import com.multispace.domain.model.*
import com.multispace.ui.components.ModernEmptyState
import com.multispace.ui.components.ModernLoadingState
import com.multispace.ui.components.ModernStatusBadge
import com.multispace.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherHomeScreen(
  modifier: Modifier = Modifier,
  discoveryViewModel: AppDiscoveryViewModel,
  spaceViewModel: SpaceViewModel,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onOpenConfiguration: () -> Unit
) {
  val discoveryUiState by discoveryViewModel.uiState.collectAsStateWithLifecycle()
  val activeSpace by spaceViewModel.activeSpace.collectAsStateWithLifecycle()
  val activeMemberships by spaceViewModel.activeMemberships.collectAsStateWithLifecycle()
  val allSpaces by spaceViewModel.allSpaces.collectAsStateWithLifecycle()
  val unlockedSpaceIds by spaceViewModel.unlockedSpaceIds.collectAsStateWithLifecycle()

  val activeLayerIndex by spaceViewModel.activeLayerIndex.collectAsStateWithLifecycle()
  val activePlacements by spaceViewModel.activePlacements.collectAsStateWithLifecycle()
  val activeFolders by spaceViewModel.activeFolders.collectAsStateWithLifecycle()
  val activeDockItems by spaceViewModel.activeDockItems.collectAsStateWithLifecycle()

  var showSpaceSwitcherMenu by remember { mutableStateOf(false) }
  var spaceToUnlockForSwitch by remember { mutableStateOf<Space?>(null) }
  var showUnlockForActiveSpace by remember { mutableStateOf(false) }
  var showDesktopCustomizationSheet by remember { mutableStateOf(false) }
  var activeDesktopPage by remember { mutableIntStateOf(0) }
  var showImportDialog by remember { mutableStateOf(false) }
  var importReport by remember { mutableStateOf<ImportReport?>(null) }
  var isImporting by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var activeFolderInDialog by remember { mutableStateOf<SpaceFolder?>(null) }

  // Continuous interactive gesture transition state between Layer 1 and Layer 2
  var layerTransitionProgress by remember { mutableFloatStateOf(if (activeLayerIndex == 2) 1.0f else 0.0f) }
  var isGestureActive by remember { mutableStateOf(false) }
  var settleJob by remember { mutableStateOf<Job?>(null) }
  val layer2GridState = rememberLazyGridState()
  val layer2SectionListState = rememberLazyListState()
  var isSectionedAlphabeticalView by rememberSaveable { mutableStateOf(false) }

  // Synchronize transition progress when activeLayerIndex changes externally
  LaunchedEffect(activeLayerIndex) {
    val target = if (activeLayerIndex == 2) 1.0f else 0.0f
    if (!isGestureActive && settleJob?.isActive != true && layerTransitionProgress != target) {
      layerTransitionProgress = target
    }
  }

  // Smooth programmatic transition (dock button, back arrow, system back key)
  fun animateToLayer(targetLayer: Int) {
    settleJob?.cancel()
    settleJob = coroutineScope.launch {
      isGestureActive = false
      val targetValue = if (targetLayer == 2) 1.0f else 0.0f
      val animatable = Animatable(layerTransitionProgress)
      animatable.animateTo(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
      ) {
        layerTransitionProgress = value
      }
      spaceViewModel.setLayer(targetLayer)
    }
  }

  val isLayer2OpenOrOpening by remember {
    derivedStateOf { activeLayerIndex == 2 || layerTransitionProgress > 0f }
  }

  // Handle Android back button: close Layer 2 smoothly if open or transitioning
  BackHandler(enabled = isLayer2OpenOrOpening) {
    animateToLayer(1)
  }

  val isCurrentSpaceUnlocked = remember(activeSpace, unlockedSpaceIds) {
    spaceViewModel.isSpaceUnlocked(activeSpace)
  }

  // Automatic first-install configuration: ensure Default Space is configured as current Home page
  LaunchedEffect(discoveryUiState.allApps.isNotEmpty(), activeSpace?.id, activePlacements.isEmpty(), activeDockItems.isEmpty()) {
    if (discoveryUiState.allApps.isNotEmpty() &&
      (activeSpace?.id == Space.DEFAULT_SPACE_ID || activeSpace == null) &&
      activePlacements.isEmpty() &&
      activeDockItems.isEmpty()
    ) {
      val targetSpaceId = activeSpace?.id ?: Space.DEFAULT_SPACE_ID
      spaceViewModel.importCurrentHomeLayout(targetSpaceId, discoveryUiState.allApps)
    }
  }

  // Determine dynamic background styling and contrast
  val currentBgType = activeSpace?.homeWallpaperType ?: activeSpace?.backgroundType ?: Space.BACKGROUND_DEFAULT
  val currentBgColor = activeSpace?.homeWallpaperColor ?: activeSpace?.backgroundColor
  val currentBgImageUri = activeSpace?.homeWallpaperImageUri ?: activeSpace?.backgroundImageUri
  val currentScaleMode = activeSpace?.homeWallpaperScaleMode ?: "crop"
  val currentZoomLevel = activeSpace?.homeWallpaperZoomLevel ?: 1.0f
  val currentDimLevel = activeSpace?.homeWallpaperDimLevel ?: 0.20f
  val currentOffsetX = activeSpace?.homeWallpaperOffsetX ?: 0.0f
  val currentOffsetY = activeSpace?.homeWallpaperOffsetY ?: 0.0f

  val isDarkThemeBackground = remember(currentBgType, currentBgColor, currentBgImageUri) {
    when (currentBgType) {
      Space.BACKGROUND_COLOR -> {
        if (currentBgColor != null) {
          Color(currentBgColor).luminance() < 0.45f
        } else {
          false
        }
      }
      Space.BACKGROUND_IMAGE -> !currentBgImageUri.isNullOrEmpty()
      else -> false
    }
  }

  val headerContentColor = if (isDarkThemeBackground) Color.White else MaterialTheme.colorScheme.onSurface
  val pillSurfaceColor = if (isDarkThemeBackground) {
    Color(0xCC090B10)
  } else {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
  }
  val pillBorderColor = if (isDarkThemeBackground) {
    Color(0x33A78BFA)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
  }

  // Resolve Space presentation: Project active Space's persisted memberships against current Android LauncherApps catalog
  val spaceScopedApps = remember(discoveryUiState.allApps, activeMemberships, isCurrentSpaceUnlocked, activeSpace) {
    if (!isCurrentSpaceUnlocked || discoveryUiState.allApps.isEmpty()) {
      emptyList()
    } else if (activeMemberships.isEmpty() && (activeSpace?.id == Space.DEFAULT_SPACE_ID || activeSpace == null)) {
      discoveryUiState.allApps
    } else if (activeMemberships.isEmpty()) {
      emptyList()
    } else {
      val appsByComponent = discoveryUiState.allApps.associateBy { "${it.packageName}/${it.activityName}" }
      val appsByPackage = discoveryUiState.allApps.associateBy { it.packageName }

      val result = mutableListOf<DiscoveredApp>()
      val includedKeys = mutableSetOf<String>()

      for (membership in activeMemberships) {
        val matchedApp = appsByComponent["${membership.packageName}/${membership.componentName}"]
          ?: appsByPackage[membership.packageName]

        if (matchedApp != null) {
          val appKey = "${matchedApp.packageName}/${matchedApp.activityName}/${matchedApp.userHandleId}"
          if (includedKeys.add(appKey)) {
            result.add(matchedApp)
          }
        }
      }
      result
    }
  }

  // Unified drag state orchestrating Layer 1 Desktop and DockBar cross-component drag-and-drop
  val unifiedDragState = remember { UnifiedDragState() }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize()
  ) {
    val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

    // Settle transition to 0f or 1f using progress and release velocity
    fun settleTransition(currentProgress: Float, velocityY: Float) {
      settleJob?.cancel()
      settleJob = coroutineScope.launch {
        isGestureActive = false
        val flingThresholdPx = 500f
        val targetValue = when {
          velocityY < -flingThresholdPx && currentProgress > 0.02f -> 1.0f
          velocityY > flingThresholdPx && currentProgress < 0.98f -> 0.0f
          currentProgress >= 0.60f -> 1.0f
          else -> 0.0f
        }

        val progressVelocity = if (screenHeightPx > 0f) {
          -velocityY / screenHeightPx
        } else 0f

        val animatable = Animatable(currentProgress)
        animatable.animateTo(
          targetValue = targetValue,
          initialVelocity = progressVelocity.coerceIn(-15f, 15f),
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
          )
        ) {
          layerTransitionProgress = value
        }

        val targetLayer = if (targetValue == 1.0f) 2 else 1
        spaceViewModel.setLayer(targetLayer)
      }
    }

    // Gesture detector for Layer 1 upward / downward continuous drag
    val layer1VelocityTracker = remember { VelocityTracker() }
    val isAnyDragActive = unifiedDragState.isDragging || unifiedDragState.lifecycleState != DragLifecycleState.IDLE
    val useLayer2 = activeSpace?.useLayer2 ?: true
    val isSwipeAllowed = useLayer2 && activeSpace?.layer2AccessMode == Space.ACCESS_MODE_SWIPE_UP

    val layer1DragModifier = Modifier.pointerInput(screenHeightPx, isAnyDragActive, isSwipeAllowed) {
      if (isAnyDragActive || !isSwipeAllowed) return@pointerInput
      detectVerticalDragGestures(
        onDragStart = {
          settleJob?.cancel()
          layer1VelocityTracker.resetTracking()
          isGestureActive = true
        },
        onDragEnd = {
          isGestureActive = false
          val velocityY = layer1VelocityTracker.calculateVelocity().y
          settleTransition(layerTransitionProgress, velocityY)
        },
        onDragCancel = {
          isGestureActive = false
          settleTransition(layerTransitionProgress, 0f)
        },
        onVerticalDrag = { change, dragAmount ->
          if (!isGestureActive) {
            settleJob?.cancel()
            layer1VelocityTracker.resetTracking()
            isGestureActive = true
          }
          layer1VelocityTracker.addPosition(change.uptimeMillis, change.position)
          val progressDelta = -dragAmount / screenHeightPx
          layerTransitionProgress = (layerTransitionProgress + progressDelta).coerceIn(0f, 1f)
          change.consume()
        }
      )
    }

    // Gesture detector for Layer 2 Header / Search Bar area downward drag
    val layer2HeaderVelocityTracker = remember { VelocityTracker() }
    val layer2HeaderDragModifier = Modifier.pointerInput(screenHeightPx) {
      detectVerticalDragGestures(
        onDragStart = {
          settleJob?.cancel()
          layer2HeaderVelocityTracker.resetTracking()
          isGestureActive = true
        },
        onDragEnd = {
          isGestureActive = false
          val velocityY = layer2HeaderVelocityTracker.calculateVelocity().y
          settleTransition(layerTransitionProgress, velocityY)
        },
        onDragCancel = {
          isGestureActive = false
          settleTransition(layerTransitionProgress, 0f)
        },
        onVerticalDrag = { change, dragAmount ->
          if (!isGestureActive) {
            settleJob?.cancel()
            layer2HeaderVelocityTracker.resetTracking()
            isGestureActive = true
          }
          layer2HeaderVelocityTracker.addPosition(change.uptimeMillis, change.position)
          val progressDelta = -dragAmount / screenHeightPx
          layerTransitionProgress = (layerTransitionProgress + progressDelta).coerceIn(0f, 1f)
          change.consume()
        }
      )
    }

    // Nested scroll coordinator ensuring Layer 2 internal LazyVerticalGrid scrolling coexists seamlessly
    val nestedScrollConnection = remember(screenHeightPx, isSectionedAlphabeticalView) {
      object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
          val deltaY = available.y
          val currentProgress = layerTransitionProgress

          // If transition is already in progress (0 < progress < 1), intercept all vertical drags
          if (currentProgress in 0.0001f..0.9999f) {
            settleJob?.cancel()
            isGestureActive = true
            val progressDelta = -deltaY / screenHeightPx
            layerTransitionProgress = (currentProgress + progressDelta).coerceIn(0f, 1f)
            return Offset(0f, deltaY)
          }

          // If Layer 2 is fully open and user drags DOWN while already at top of grid or section list:
          if (currentProgress >= 0.999f && deltaY > 0f) {
            val isGridAtTop = !layer2GridState.canScrollBackward ||
                (layer2GridState.firstVisibleItemIndex == 0 && layer2GridState.firstVisibleItemScrollOffset <= 0)
            val isListAtTop = !layer2SectionListState.canScrollBackward ||
                (layer2SectionListState.firstVisibleItemIndex == 0 && layer2SectionListState.firstVisibleItemScrollOffset <= 0)
            val isAtTop = if (isSectionedAlphabeticalView) isListAtTop else isGridAtTop
            if (isAtTop) {
              settleJob?.cancel()
              isGestureActive = true
              val progressDelta = -deltaY / screenHeightPx
              layerTransitionProgress = (currentProgress + progressDelta).coerceIn(0f, 1f)
              return Offset(0f, deltaY)
            }
          }

          return Offset.Zero
        }

        override fun onPostScroll(
          consumed: Offset,
          available: Offset,
          source: NestedScrollSource
        ): Offset {
          val deltaY = available.y
          val currentProgress = layerTransitionProgress
          // If grid reached top and still has downward scroll delta
          if (deltaY > 0f && currentProgress >= 0.999f) {
            settleJob?.cancel()
            isGestureActive = true
            val progressDelta = -deltaY / screenHeightPx
            layerTransitionProgress = (currentProgress + progressDelta).coerceIn(0f, 1f)
            return Offset(0f, deltaY)
          }
          return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
          val currentProgress = layerTransitionProgress
          if (isGestureActive || currentProgress in 0.0001f..0.9999f) {
            settleTransition(currentProgress, available.y)
            return available
          }
          return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
          val currentProgress = layerTransitionProgress
          if (isGestureActive || currentProgress in 0.0001f..0.9999f || (available.y > 0f && currentProgress >= 0.999f)) {
            settleTransition(currentProgress, available.y)
            return available
          }
          return Velocity.Zero
        }
      }
    }
    // 1. Wallpaper / Background Layer
    when (currentBgType) {
      Space.BACKGROUND_COLOR -> {
        if (currentBgColor != null) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(currentBgColor))
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.img_wallpaper_aurora),
            contentDescription = "Space Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.20f))
          )
        }
      }
      Space.BACKGROUND_IMAGE -> {
        val presetRes = WallpaperCatalog.resolveDrawableRes(currentBgImageUri)
        if (presetRes != null) {
          Image(
            painter = painterResource(id = presetRes),
            contentDescription = "Space Wallpaper",
            contentScale = if (currentScaleMode == "crop") ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                scaleX = currentZoomLevel
                scaleY = currentZoomLevel
                translationX = currentOffsetX
                translationY = currentOffsetY
              }
          )
          if (currentDimLevel > 0f) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = currentDimLevel))
            )
          }
        } else if (!currentBgImageUri.isNullOrEmpty()) {
          val ctx = LocalContext.current
          AsyncImage(
            model = ImageRequest.Builder(ctx)
              .data(currentBgImageUri)
              .crossfade(true)
              .build(),
            contentDescription = "Space Wallpaper",
            contentScale = if (currentScaleMode == "crop") ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                scaleX = currentZoomLevel
                scaleY = currentZoomLevel
                translationX = currentOffsetX
                translationY = currentOffsetY
              }
          )
          // Scrim overlay to maintain high icon/text readability over photos
          if (currentDimLevel > 0f) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = currentDimLevel))
            )
          }
        } else {
          Image(
            painter = painterResource(id = R.drawable.img_wallpaper_aurora),
            contentDescription = "Space Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.20f))
          )
        }
      }
      else -> {
        // Space.BACKGROUND_DEFAULT: Default beautiful wallpaper
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora),
          contentDescription = "Space Wallpaper",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.20f))
        )
      }
    }

    // 2. Foreground UI
    Box(
      modifier = Modifier.fillMaxSize()
    ) {
      when {
        !isCurrentSpaceUnlocked -> {
          // Protected Space Locked State
          Column(
            modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding()
              .navigationBarsPadding()
              .padding(AppDimens.Spacing32),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              shape = CircleShape,
              color = pillSurfaceColor,
              border = BorderStroke(AppDimens.BorderMedium, CrimsonNova.copy(alpha = 0.5f)),
              modifier = Modifier.size(76.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = CrimsonNova,
                  modifier = Modifier.size(AppDimens.IconHero)
                )
              }
            }
            Spacer(modifier = Modifier.height(AppDimens.Spacing16))
            Text(
              text = "${activeSpace?.name ?: "Space"} is Protected",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = headerContentColor
            )
            Spacer(modifier = Modifier.height(AppDimens.Spacing8))
            Text(
              text = "Enter your credential to access applications in this isolated workspace.",
              style = MaterialTheme.typography.bodyMedium,
              color = if (isDarkThemeBackground) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppDimens.Spacing24))
            Button(
              onClick = { showUnlockForActiveSpace = true },
              shape = ShapeRoundMd,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.height(AppDimens.ButtonHeight).testTag("btn_unlock_active_space")
            ) {
              Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconSm)
              )
              Spacer(modifier = Modifier.width(AppDimens.Spacing8))
              Text(
                "Enter PIN / Credential",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
        discoveryUiState.isLoading && discoveryUiState.allApps.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding()
              .navigationBarsPadding(),
            contentAlignment = Alignment.Center
          ) {
            ModernLoadingState(message = "Scanning installed applications...")
          }
        }
        discoveryUiState.errorMessage != null && spaceScopedApps.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding()
              .navigationBarsPadding(),
            contentAlignment = Alignment.Center
          ) {
            ModernEmptyState(
              icon = Icons.Default.ErrorOutline,
              title = "Unable to load Space apps",
              description = discoveryUiState.errorMessage ?: "Unknown error occurred during discovery",
              actionText = "Retry Scan",
              onActionClick = { discoveryViewModel.loadApps() }
            )
          }
        }
        spaceScopedApps.isEmpty() -> {
          // Empty Space State prompting to configure app memberships
          Column(
            modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding()
              .navigationBarsPadding()
              .padding(AppDimens.Spacing32),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            ModernEmptyState(
              icon = Icons.Default.Apps,
              title = "No apps in ${activeSpace?.name ?: "this Space"}",
              description = "Assign apps to this workspace or import your existing home layout to get started."
            )
            Spacer(modifier = Modifier.height(AppDimens.Spacing16))
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
              Button(
                onClick = onOpenConfiguration,
                shape = ShapeRoundMd,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(AppDimens.ButtonHeightSm).testTag("btn_empty_space_configure")
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(AppDimens.IconSm)
                )
                Spacer(modifier = Modifier.width(AppDimens.Spacing6))
                Text("Add Apps", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }

              OutlinedButton(
                onClick = { showImportDialog = true },
                shape = ShapeRoundMd,
                modifier = Modifier.height(AppDimens.ButtonHeightSm).testTag("btn_empty_space_import")
              ) {
                Icon(
                  imageVector = Icons.Default.FileDownload,
                  contentDescription = null,
                  modifier = Modifier.size(AppDimens.IconSm)
                )
                Spacer(modifier = Modifier.width(AppDimens.Spacing6))
                Text("Import Layout", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }
            }
          }
        }
        else -> {
          // Main 2-Layer Workspace with continuous finger-following transition
          val currentSpace = activeSpace ?: Space.createDefault()
          val shouldComposeLayer1 by remember {
            derivedStateOf { isGestureActive || layerTransitionProgress < 1f || activeLayerIndex == 1 }
          }
          val shouldComposeLayer2 by remember {
            derivedStateOf { isGestureActive || layerTransitionProgress > 0f || activeLayerIndex == 2 }
          }
          val canDragLayer1 by remember(isAnyDragActive, isSwipeAllowed) {
            derivedStateOf { layerTransitionProgress < 1f && !isAnyDragActive && isSwipeAllowed }
          }

          // Cache most used apps: avoid recomputing on every layerTransitionProgress frame
          val cachedMostUsedApps = remember(spaceScopedApps, currentSpace.gridColumns) {
            discoveryViewModel.getMostUsedApps(spaceScopedApps, limit = currentSpace.gridColumns)
          }

          // Cache Layer 2 catalog derived data so swiping between layers does not rebuild collections
          val layer2CachedCatalog = remember(spaceScopedApps) {
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

            Layer2CachedCatalog(
              sortedApps = sorted,
              groupedApps = grouped,
              letterToSectionIndex = letterToSection,
              letterToFirstIndex = letterToFirst,
              activeLetters = letterToFirst.keys
            )
          }

          Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Curated Workspace (Pages / Scrolling Grid & Folders + Dock Bar)
            if (shouldComposeLayer1) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .zIndex(1f)
                  .graphicsLayer {
                    val p = layerTransitionProgress
                    translationY = -screenHeightPx * 0.08f * p
                    alpha = (1f - p).coerceIn(0f, 1f)
                  }
                  .then(
                    if (canDragLayer1) layer1DragModifier
                    else Modifier
                  )
              ) {
                Scaffold(
                  modifier = Modifier.fillMaxSize(),
                  containerColor = Color.Transparent,
                  contentWindowInsets = WindowInsets(0, 0, 0, 0),
                  bottomBar = {
                    if (isCurrentSpaceUnlocked && activeSpace != null && !showDesktopCustomizationSheet) {
                      SpaceDockBar(
                        dockItems = activeDockItems,
                        allApps = discoveryUiState.allApps,
                        capacity = activeSpace?.dockCapacity ?: 5,
                        accessMode = activeSpace?.layer2AccessMode ?: Space.ACCESS_MODE_DOCK_BUTTON,
                        getBitmap = { discoveryViewModel.getAppIconBitmap(it) },
                        onLaunchApp = onLaunchApp,
                        onOpenLayer2 = { animateToLayer(2) },
                        onRemoveFromDock = { item ->
                          activeSpace?.let { spaceViewModel.removeAppFromDock(it.id, item.id) }
                        },
                        onReorderDock = { reordered ->
                          activeSpace?.let { spaceViewModel.reorderDockItems(it.id, reordered) }
                        },
                        onDropFromDockToDesktop = { dockItem, app, targetPage, targetPos ->
                          activeSpace?.let { space ->
                            val existing = activePlacements.firstOrNull {
                              it.pageIndex == targetPage && it.positionIndex == targetPos
                            }
                            if (existing != null && !existing.isWidget && !existing.isFolder) {
                              val targetApp = spaceScopedApps.firstOrNull { it.packageName == existing.packageName }
                              if (targetApp != null) {
                                spaceViewModel.createFolderFromApps(
                                  spaceId = space.id,
                                  pageIndex = targetPage,
                                  positionIndex = targetPos,
                                  folderName = "Folder",
                                  sourceApp = app,
                                  targetApp = targetApp,
                                  sourcePlacementId = null,
                                  targetPlacementId = existing.id
                                )
                                spaceViewModel.removeAppFromDock(space.id, dockItem.id)
                              } else {
                                spaceViewModel.moveAppFromDockToHome(
                                  spaceId = space.id,
                                  dockItemId = dockItem.id,
                                  app = app,
                                  targetPage = targetPage,
                                  targetPosition = targetPos
                                )
                              }
                            } else {
                              spaceViewModel.moveAppFromDockToHome(
                                spaceId = space.id,
                                dockItemId = dockItem.id,
                                app = app,
                                targetPage = targetPage,
                                targetPosition = targetPos
                              )
                            }
                          }
                        },
                        unifiedDragState = unifiedDragState,
                        useLayer2 = activeSpace?.useLayer2 ?: true,
                        appTheme = activeSpace?.appTheme ?: Space.THEME_DEFAULT,
                        modifier = Modifier.navigationBarsPadding()
                      )
                    }
                  }
                ) { paddingValues ->
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .statusBarsPadding()
                      .padding(bottom = paddingValues.calculateBottomPadding())
                  ) {
                    Layer1HomeScreen(
                      space = currentSpace,
                      placements = activePlacements,
                      folders = activeFolders,
                      allApps = spaceScopedApps,
                      getBitmap = { discoveryViewModel.getAppIconBitmap(it) },
                      onLaunchApp = onLaunchApp,
                      onOpenFolder = { folder -> activeFolderInDialog = folder },
                      onRemovePlacement = { placementId ->
                        spaceViewModel.removePlacement(placementId)
                      },
                      onCreateFolderFromApps = { src, tgt, srcId, tgtId, targetPage, targetPos ->
                        spaceViewModel.createFolderFromApps(
                          spaceId = currentSpace.id,
                          pageIndex = targetPage,
                          positionIndex = targetPos,
                          folderName = "Folder",
                          sourceApp = src,
                          targetApp = tgt,
                          sourcePlacementId = srcId,
                          targetPlacementId = tgtId
                        )
                      },
                      onAddAppToExistingFolder = { folderId, app, sourcePlacementId ->
                        spaceViewModel.addAppToFolder(folderId, app)
                        spaceViewModel.removePlacement(sourcePlacementId)
                      },
                      onAddAppToHome = { app, page ->
                        spaceViewModel.addAppToHome(currentSpace.id, app, page)
                      },
                      onMovePlacement = { placementId, targetPage, targetPos, pageSize ->
                        spaceViewModel.moveAppToPage(currentSpace.id, placementId, targetPage, targetPos, pageSize)
                      },
                      onResizeWidget = { placementId, spanX, spanY, pos ->
                        spaceViewModel.updateWidgetSpan(placementId, spanX, spanY, pos)
                      },
                      onOpenCustomization = { page ->
                        activeDesktopPage = page
                        showDesktopCustomizationSheet = true
                      },
                      onOpenAppInfo = { app ->
                        discoveryViewModel.openAppInfo(app)
                      },
                      onUninstallApp = { app ->
                        discoveryViewModel.uninstallApp(app)
                      },
                      onForceStopApp = { app ->
                        discoveryViewModel.forceStopApp(app)
                      },
                      unifiedDragState = unifiedDragState,
                      onDropItemToDock = { placement, app, targetDockIndex ->
                        activeSpace?.let { space ->
                          spaceViewModel.moveAppFromHomeToDock(
                            spaceId = space.id,
                            placementId = placement.id,
                            app = app,
                            targetDockIndex = targetDockIndex
                          )
                        }
                      }
                    )
                  }
                }
              }
            }

            // Layer 2: Space App Library (Takes the WHOLE SCREEN, physically rises from below)
            if (shouldComposeLayer2) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .zIndex(2f)
                  .graphicsLayer {
                    val p = layerTransitionProgress
                    translationY = screenHeightPx * (1f - p)
                    alpha = p.coerceIn(0f, 1f)
                  }
              ) {
                Layer2LibraryScreen(
                  space = currentSpace,
                  spaceApps = spaceScopedApps,
                  getBitmap = { discoveryViewModel.getAppIconBitmap(it) },
                  onLaunchApp = onLaunchApp,
                  onAddToHome = { app ->
                    spaceViewModel.addAppToHome(currentSpace.id, app)
                  },
                  onAddToDock = { app ->
                    spaceViewModel.addAppToDock(currentSpace.id, app)
                  },
                  onAppInfo = { app ->
                    discoveryViewModel.openAppInfo(app)
                  },
                  onUninstallApp = { app ->
                    discoveryViewModel.uninstallApp(app)
                  },
                  onForceStopApp = { app ->
                    discoveryViewModel.forceStopApp(app)
                  },
                  onCloseLayer2 = { animateToLayer(1) },
                  mostUsedApps = cachedMostUsedApps,
                  cachedCatalog = layer2CachedCatalog,
                  gridState = layer2GridState,
                  sectionListState = layer2SectionListState,
                  isSectionedAlphabeticalView = isSectionedAlphabeticalView,
                  onToggleSectionedAlphabeticalView = { isSectionedAlphabeticalView = !isSectionedAlphabeticalView },
                  topBarModifier = layer2HeaderDragModifier,
                  modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                )
              }
            }
          }
        }
      }
    }

    // Unified floating drag follow overlay (used when dragging an app out of the DockBar)
    if (unifiedDragState.isDragging && unifiedDragState.dragSource == DragSource.DOCK_BAR && unifiedDragState.draggedApp != null) {
      val app = unifiedDragState.draggedApp!!
      val bitmap = discoveryViewModel.getAppIconBitmap(app)
      val dragScale by animateFloatAsState(
        targetValue = if (unifiedDragState.isOverBin) 0.85f else 1.10f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "unifiedDragScale"
      )

      Box(
        modifier = Modifier
          .fillMaxSize()
          .zIndex(9999f)
      ) {
        Box(
          modifier = Modifier
            .offset {
              val rootPos = unifiedDragState.rootPointerPos
              val touchOffset = unifiedDragState.touchOffsetInItem
              IntOffset(
                x = (rootPos.x - touchOffset.x).roundToInt(),
                y = (rootPos.y - touchOffset.y).roundToInt()
              )
            }
            .scale(dragScale)
            .shadow(16.dp, CircleShape)
            .size(56.dp)
            .background(
              if (unifiedDragState.isOverBin) CrimsonNova.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          ThemedAppIcon(
            app = app,
            bitmap = bitmap,
            appTheme = activeSpace?.appTheme ?: Space.THEME_DEFAULT,
            modifier = Modifier.size(46.dp)
          )
        }
      }
    }
  }

  // Active space unlock dialog
  if (showUnlockForActiveSpace && activeSpace != null) {
    SpaceUnlockDialog(
      space = activeSpace!!,
      onDismiss = { showUnlockForActiveSpace = false },
      onUnlockSuccess = {
        showUnlockForActiveSpace = false
      },
      spaceViewModel = spaceViewModel
    )
  }

  // Target space unlock dialog for switching
  spaceToUnlockForSwitch?.let { space ->
    SpaceUnlockDialog(
      space = space,
      onDismiss = { spaceToUnlockForSwitch = null },
      onUnlockSuccess = {
        spaceViewModel.selectActiveSpace(space.id)
        spaceToUnlockForSwitch = null
      },
      spaceViewModel = spaceViewModel
    )
  }

  // Folder Dialog
  if (activeFolderInDialog != null) {
    val folder = activeFolderInDialog!!
    FolderDialog(
      folder = folder,
      allApps = discoveryUiState.allApps,
      getBitmap = { discoveryViewModel.getAppIconBitmap(it) },
      onLaunchApp = onLaunchApp,
      onRenameFolder = { newName ->
        spaceViewModel.renameFolder(folder.id, newName)
        activeFolderInDialog = folder.copy(name = newName)
      },
      onRemoveItem = { item ->
        spaceViewModel.removeAppFromFolder(folder.id, item.id)
        activeFolderInDialog = folder.copy(items = folder.items.filter { it.id != item.id })
      },
      onDeleteFolder = {
        spaceViewModel.deleteFolder(folder.id)
        activeFolderInDialog = null
      },
      onDismiss = { activeFolderInDialog = null }
    )
  }

  // Import Layout Dialog
  if (showImportDialog && activeSpace != null) {
    ImportLayoutDialog(
      report = importReport,
      isImporting = isImporting,
      onStartImport = {
        isImporting = true
        activeSpace?.let { space ->
          spaceViewModel.importCurrentHomeLayout(space.id, discoveryUiState.allApps) { report ->
            importReport = report
            isImporting = false
          }
        }
      },
      onDismiss = {
        showImportDialog = false
        importReport = null
        isImporting = false
      }
    )
  }

  // Desktop Customization Sheet (Triggered on long-pressing empty desktop)
  if (showDesktopCustomizationSheet && activeSpace != null) {
    val space = activeSpace!!
    DesktopCustomizationSheet(
      space = space,
      placements = activePlacements,
      currentPage = activeDesktopPage,
      totalPageCount = maxOf(space.pageCount, (activePlacements.maxOfOrNull { it.pageIndex } ?: 0) + 1),
      spaceApps = spaceScopedApps,
      onDismiss = { showDesktopCustomizationSheet = false },
      onSelectWallpaperColor = { color ->
        spaceViewModel.updateSpaceWallpaper(
          space.id,
          Space.BACKGROUND_COLOR,
          color,
          null
        )
      },
      onSelectWallpaperPreset = { presetUri ->
        spaceViewModel.updateSpaceWallpaper(
          space.id,
          Space.BACKGROUND_IMAGE,
          null,
          presetUri
        )
      },
      onSelectWallpaperUri = { uri ->
        spaceViewModel.updateSpaceWallpaper(
          space.id,
          Space.BACKGROUND_IMAGE,
          null,
          uri.toString()
        )
      },
      onOpenWallpaperEditor = {},
      onUpdateSpaceCustomization = { bgType, bgColor, bgUri, cols, size, showLabels ->
        spaceViewModel.updateSpaceCustomization(
          spaceId = space.id,
          backgroundType = bgType,
          backgroundColor = bgColor,
          backgroundImageUri = bgUri,
          gridColumns = cols,
          iconSize = size,
          labelVisibility = showLabels
        )
      },
      onReorderApp = { app, direction ->
        spaceViewModel.reorderSpaceApp(space.id, app, direction)
      },
      onSortAlphabetically = {
        spaceViewModel.sortSpaceAppsAlphabetically(space.id, spaceScopedApps)
      },
      onAddWidget = { pageIndex, widgetType, spanX, spanY, appWidgetId, pkg, comp ->
        showDesktopCustomizationSheet = false
        spaceViewModel.addWidgetToHome(
          spaceId = space.id,
          pageIndex = pageIndex,
          widgetType = widgetType,
          spanX = spanX,
          spanY = spanY,
          appWidgetId = appWidgetId,
          packageName = pkg,
          componentName = comp
        )
      },
      onUpdateTheme = { appTheme, cols, iconSize, showLabels ->
        spaceViewModel.updateSpaceTheme(
          spaceId = space.id,
          appTheme = appTheme,
          gridColumns = cols,
          iconSize = iconSize,
          labelVisibility = showLabels
        )
      },
      onAddPage = {
        spaceViewModel.addPage(space.id)
      },
      onDeletePage = { pageIndex ->
        spaceViewModel.deletePage(space.id, pageIndex)
      },
      onScrollToPage = { _ ->
        // Jump to page
      }
    )
  }
}
