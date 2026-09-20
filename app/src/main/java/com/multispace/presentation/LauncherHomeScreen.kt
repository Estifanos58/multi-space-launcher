package com.multispace.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multispace.domain.model.*
import com.multispace.presentation.components.LauncherDialogsHost
import com.multispace.presentation.components.LauncherEmptySpaceState
import com.multispace.presentation.components.LauncherFloatingDragOverlay
import com.multispace.presentation.components.LauncherLockedState
import com.multispace.presentation.components.LauncherWallpaperLayer
import com.multispace.ui.components.ModernEmptyState
import com.multispace.ui.components.ModernLoadingState

/**
 * Clean architectural presentation entry point for MultiSpace Launcher.
 * Delegates state aggregation, gesture arbitration, layer transitions,
 * desktop interactions, and dock interactions to dedicated controllers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherHomeScreen(
  modifier: Modifier = Modifier,
  discoveryViewModel: AppDiscoveryViewModel,
  spaceViewModel: SpaceViewModel,
  onLaunchApp: (DiscoveredApp) -> Unit = {},
  onLaunchAppInSpace: ((DiscoveredApp, String) -> Unit)? = null,
  onOpenConfiguration: () -> Unit
) {
  val uiState = rememberLauncherUiState(
    discoveryViewModel = discoveryViewModel,
    spaceViewModel = spaceViewModel
  )

  val transitionController = rememberLayerTransitionController(
    activeLayerIndex = uiState.activeLayerIndex,
    onSetLayer = { spaceViewModel.setLayer(it) }
  )

  val unifiedDragState = remember { UnifiedDragState() }

  val gestureCoordinator = rememberLauncherGestureCoordinator(
    unifiedDragState = unifiedDragState,
    transitionController = transitionController
  )

  val desktopController = rememberDesktopInteractionController(
    spaceViewModel = spaceViewModel,
    discoveryViewModel = discoveryViewModel
  )

  val dockController = rememberDockInteractionController(
    spaceViewModel = spaceViewModel,
    spaceScopedApps = uiState.spaceScopedApps,
    activePlacements = uiState.activePlacements
  )

  val iconBitmaps by discoveryViewModel.iconBitmaps.collectAsStateWithLifecycle()
  val getAppBitmap: (DiscoveredApp) -> android.graphics.Bitmap? = { app ->
    iconBitmaps[app.id] ?: discoveryViewModel.getAppIconBitmap(app)
  }

  val handleAppLaunch: (DiscoveredApp) -> Unit = { app ->
    val spaceId = uiState.activeSpace?.id ?: Space.DEFAULT_SPACE_ID
    if (onLaunchAppInSpace != null) {
      onLaunchAppInSpace(app, spaceId)
    } else {
      discoveryViewModel.launchApp(app, spaceId)
    }
  }

  // Handle Android back button: close Layer 2 smoothly if open or transitioning
  BackHandler(enabled = transitionController.isLayer2OpenOrOpening) {
    transitionController.animateToLayer(1)
  }

  val homeResetCounter by spaceViewModel.homeResetCounter.collectAsState()

  LaunchedEffect(spaceViewModel) {
    spaceViewModel.launcherCommands.collect { command ->
      when (command) {
        is SpaceViewModel.LauncherCommand.NavigateHome -> {
          if (transitionController.isLayer2OpenOrOpening) {
            transitionController.animateToLayer(1)
          } else {
            spaceViewModel.setLayer(1)
          }
          desktopController.dismissAllModals()
          unifiedDragState.reset()
        }
        is SpaceViewModel.LauncherCommand.ResetTransientState -> {
          unifiedDragState.reset()
        }
      }
    }
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize()
  ) {
    val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
    val emptySwipeCallbacks = remember(screenHeightPx) {
      transitionController.createEmptySpaceSwipeCallbacks(screenHeightPx)
    }

    // 1. Background / Wallpaper Layer
    LauncherWallpaperLayer(wallpaperStyle = uiState.wallpaperStyle)

    // 2. Foreground Workspace UI
    Box(modifier = Modifier.fillMaxSize()) {
      when {
        !uiState.isCurrentSpaceUnlocked -> {
          LauncherLockedState(
            spaceName = uiState.activeSpace?.name ?: "Space",
            wallpaperStyle = uiState.wallpaperStyle,
            onUnlockClick = { desktopController.showUnlockForActiveSpace = true }
          )
        }
        uiState.isLoading && uiState.allApps.isEmpty() -> {
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
        uiState.errorMessage != null && uiState.spaceScopedApps.isEmpty() -> {
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
              description = uiState.errorMessage ?: "Unknown error occurred during discovery",
              actionText = "Retry Scan",
              onActionClick = { discoveryViewModel.loadApps() }
            )
          }
        }
        uiState.spaceScopedApps.isEmpty() -> {
          LauncherEmptySpaceState(
            spaceName = uiState.activeSpace?.name ?: "this Space",
            onConfigureClick = onOpenConfiguration,
            onImportClick = { desktopController.showImportDialog = true }
          )
        }
        else -> {
          val currentSpace = uiState.currentSpace
          val useLayer2 = currentSpace.useLayer2
          val canDragLayer1 = gestureCoordinator.canDragLayer1(useLayer2)

          Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Curated Workspace (Pages / Scrolling Grid & Folders + Dock Bar)
            if (transitionController.shouldComposeLayer1(uiState.activeLayerIndex)) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .zIndex(1f)
                  .graphicsLayer {
                    val p = transitionController.layerTransitionProgress
                    translationY = -screenHeightPx * 0.08f * p
                    alpha = (1f - p).coerceIn(0f, 1f)
                  }
              ) {
                Scaffold(
                  modifier = Modifier.fillMaxSize(),
                  containerColor = Color.Transparent,
                  contentWindowInsets = WindowInsets(0, 0, 0, 0),
                  bottomBar = {
                    if (uiState.isCurrentSpaceUnlocked && uiState.activeSpace != null && !desktopController.showDesktopCustomizationSheet) {
                      SpaceDockBar(
                        dockItems = uiState.activeDockItems,
                        allApps = uiState.allApps,
                        capacity = currentSpace.dockCapacity,
                        accessMode = currentSpace.layer2AccessMode,
                        getBitmap = getAppBitmap,
                        onLaunchApp = handleAppLaunch,
                        onOpenLayer2 = { transitionController.animateToLayer(2) },
                        onRemoveFromDock = { item ->
                          dockController.removeAppFromDock(currentSpace.id, item.id)
                        },
                        onReorderDock = { reordered ->
                          dockController.reorderDockItems(currentSpace.id, reordered)
                        },
                        onDropFromDockToDesktop = { dockItem, app, targetPage, targetPos ->
                          dockController.onDropFromDockToDesktop(currentSpace, dockItem, app, targetPage, targetPos)
                        },
                        unifiedDragState = unifiedDragState,
                        useLayer2 = currentSpace.useLayer2,
                        appTheme = currentSpace.appTheme,
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
                      placements = uiState.activePlacements,
                      folders = uiState.resolvedActiveFolders,
                      allApps = uiState.spaceScopedApps,
                      getBitmap = getAppBitmap,
                      onLaunchApp = handleAppLaunch,
                      onOpenFolder = { folder ->
                        desktopController.openFolder(folder, uiState.resolvedActiveFolders)
                      },
                      onRemovePlacement = { placementId ->
                        desktopController.removePlacement(placementId)
                      },
                      onCreateFolderFromApps = { src, tgt, srcId, tgtId, targetPage, targetPos ->
                        desktopController.createFolderFromApps(currentSpace.id, targetPage, targetPos, src, tgt, srcId, tgtId)
                      },
                      onAddAppToExistingFolder = { folderId, app, sourcePlacementId ->
                        desktopController.addAppToExistingFolder(folderId, app, sourcePlacementId)
                      },
                      onAddAppToHome = { app, page ->
                        desktopController.addAppToHome(currentSpace.id, app, page)
                      },
                      onMovePlacement = { placementId, targetPage, targetPos, pageSize, appIdentity ->
                        desktopController.movePlacement(currentSpace.id, placementId, targetPage, targetPos, pageSize, appIdentity)
                      },
                      onResizeWidget = { placementId, spanX, spanY, pos ->
                        desktopController.resizeWidget(placementId, spanX, spanY, pos)
                      },
                      onOpenCustomization = { page ->
                        desktopController.openCustomization(page)
                      },
                      onOpenAppInfo = { app -> desktopController.openAppInfo(app) },
                      onUninstallApp = { app -> desktopController.uninstallApp(app) },
                      onForceStopApp = { app -> desktopController.forceStopApp(app) },
                      unifiedDragState = unifiedDragState,
                      onDropItemToDock = { placement, app, targetDockIndex ->
                        dockController.onDropItemToDock(currentSpace, placement, app, targetDockIndex)
                      },
                      isSwipeAllowed = canDragLayer1,
                      onEmptySpaceSwipeStart = emptySwipeCallbacks.onStart,
                      onEmptySpaceSwipeMove = emptySwipeCallbacks.onMove,
                      onEmptySpaceSwipeEnd = emptySwipeCallbacks.onEnd,
                      onEmptySpaceSwipeCancel = emptySwipeCallbacks.onCancel,
                      usageStats = uiState.spaceUsageStats,
                      homeResetTrigger = homeResetCounter
                    )
                  }
                }
              }
            }

            // Layer 2: Space App Library (Physically rises from below)
            if (transitionController.shouldComposeLayer2(uiState.activeLayerIndex)) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .zIndex(2f)
                  .graphicsLayer {
                    val p = transitionController.layerTransitionProgress
                    translationY = screenHeightPx * (1f - p)
                    alpha = p.coerceIn(0f, 1f)
                  }
              ) {
                Layer2LibraryScreen(
                  space = currentSpace,
                  spaceApps = uiState.spaceScopedApps,
                  getBitmap = getAppBitmap,
                  onLaunchApp = handleAppLaunch,
                  onAddToHome = { app -> desktopController.addAppToHome(currentSpace.id, app) },
                  onAddToDock = { app -> spaceViewModel.addAppToDock(currentSpace.id, app) },
                  onAppInfo = { app -> desktopController.openAppInfo(app) },
                  onUninstallApp = { app -> desktopController.uninstallApp(app) },
                  onForceStopApp = { app -> desktopController.forceStopApp(app) },
                  onCloseLayer2 = { transitionController.animateToLayer(1) },
                  recentApps = uiState.spaceScopedRecentApps,
                  mostUsedApps = uiState.spaceScopedMostUsedApps,
                  discoveryViewModel = discoveryViewModel,
                  cachedCatalog = uiState.layer2CachedCatalog,
                  gridState = transitionController.layer2GridState,
                  sectionListState = transitionController.layer2SectionListState,
                  isSectionedAlphabeticalView = transitionController.isSectionedAlphabeticalView,
                  onToggleSectionedAlphabeticalView = {
                    transitionController.isSectionedAlphabeticalView = !transitionController.isSectionedAlphabeticalView
                  },
                  topBarModifier = transitionController.createHeaderDragModifier(screenHeightPx),
                  modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(transitionController.createNestedScrollConnection(screenHeightPx))
                )
              }
            }
          }
        }
      }
    }

    // Floating drag overlay for cross-component drag operations
    LauncherFloatingDragOverlay(
      unifiedDragState = unifiedDragState,
      appTheme = uiState.currentSpace.appTheme,
      getBitmap = getAppBitmap
    )
  }

  // Dialogs and modal sheets
  LauncherDialogsHost(
    desktopController = desktopController,
    uiState = uiState,
    spaceViewModel = spaceViewModel,
    getBitmap = getAppBitmap,
    onLaunchApp = handleAppLaunch
  )
}
