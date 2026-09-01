package com.multispace.presentation

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.multispace.domain.model.*
import com.multispace.platform.RecentsController
import com.multispace.platform.RecentsInvocationResult
import com.multispace.ui.theme.*
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
  var showCustomizationDialogForActiveSpace by remember { mutableStateOf(false) }
  var showImportDialog by remember { mutableStateOf(false) }
  var importReport by remember { mutableStateOf<ImportReport?>(null) }
  var isImporting by remember { mutableStateOf(false) }
  var showRecentsDisclosureDialog by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var activeFolderInDialog by remember { mutableStateOf<SpaceFolder?>(null) }

  val isCurrentSpaceUnlocked = remember(activeSpace, unlockedSpaceIds) {
    spaceViewModel.isSpaceUnlocked(activeSpace)
  }

  // Handle Android back button: close Layer 2 if open
  BackHandler(enabled = activeLayerIndex == 2) {
    spaceViewModel.setLayer(1)
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

  val headerContentColor = if (isDarkThemeBackground) Color.White else TextPrimary
  val chipBackgroundColor = if (isDarkThemeBackground) {
    Color.Black.copy(alpha = 0.5f)
  } else {
    PrimaryContainerLight
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

  // Draggable gesture state for swipe-up into Layer 2
  var swipeOffsetY by remember { mutableStateOf(0f) }
  val draggableState = rememberDraggableState { delta ->
    swipeOffsetY += delta
    if (swipeOffsetY < -100f && activeLayerIndex == 1) {
      spaceViewModel.setLayer(2)
      swipeOffsetY = 0f
    } else if (swipeOffsetY > 100f && activeLayerIndex == 2) {
      spaceViewModel.setLayer(1)
      swipeOffsetY = 0f
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .draggable(
        state = draggableState,
        orientation = Orientation.Vertical,
        onDragStopped = { swipeOffsetY = 0f }
      )
  ) {
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
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(LightBackground)
          )
        }
      }
      Space.BACKGROUND_IMAGE -> {
        if (!currentBgImageUri.isNullOrEmpty()) {
          val context = LocalContext.current
          AsyncImage(
            model = ImageRequest.Builder(context)
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
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(LightBackground)
          )
        }
      }
      else -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
        )
      }
    }

    // 2. Foreground UI
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      topBar = {
        // Minimal Home header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Space Title & Quick Switcher Chip
          Box {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = chipBackgroundColor,
              modifier = Modifier
                .clickable { showSpaceSwitcherMenu = true }
                .testTag("home_space_indicator")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                if (activeSpace?.isProtected == true) {
                  Icon(
                    imageVector = if (isCurrentSpaceUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = if (isCurrentSpaceUnlocked) "Unlocked" else "Locked",
                    tint = if (isCurrentSpaceUnlocked) Color(0xFF2E7D32) else if (isDarkThemeBackground) Color(0xFF81C784) else PrimaryPurpleDark,
                    modifier = Modifier.size(14.dp)
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(if (isDarkThemeBackground) Color.White else PrimaryPurpleDark)
                  )
                }
                Text(
                  text = activeSpace?.name ?: "Default",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = headerContentColor
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Switch Space",
                  tint = if (isDarkThemeBackground) Color.White.copy(alpha = 0.8f) else TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            // Space Switcher Dropdown Menu
            DropdownMenu(
              expanded = showSpaceSwitcherMenu,
              onDismissRequest = { showSpaceSwitcherMenu = false },
              modifier = Modifier.background(LightBackground)
            ) {
              Text(
                text = "SWITCH SPACE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 10.sp
              )
              allSpaces.forEach { space ->
                val isCurrent = space.id == activeSpace?.id
                DropdownMenuItem(
                  text = {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(
                        imageVector = if (isCurrent) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCurrent) PrimaryPurpleDark else TextMuted,
                        modifier = Modifier.size(18.dp)
                      )
                      Text(
                        text = space.name,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimary
                      )
                      if (space.isProtected) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                          imageVector = Icons.Default.Lock,
                          contentDescription = "Protected",
                          tint = TextSecondary,
                          modifier = Modifier.size(14.dp)
                        )
                      }
                    }
                  },
                  onClick = {
                    showSpaceSwitcherMenu = false
                    if (space.isProtected) {
                      spaceToUnlockForSwitch = space
                    } else {
                      spaceViewModel.selectActiveSpace(space.id)
                    }
                  },
                  modifier = Modifier.testTag("menu_switch_space_${space.id}")
                )
              }
              HorizontalDivider(color = LightSurfaceContainerHigh)

              DropdownMenuItem(
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.FileDownload,
                      contentDescription = null,
                      tint = PrimaryPurpleDark,
                      modifier = Modifier.size(18.dp)
                    )
                    Text("Import Android Layout...", color = PrimaryPurpleDark, fontWeight = FontWeight.Medium)
                  }
                },
                onClick = {
                  showSpaceSwitcherMenu = false
                  showImportDialog = true
                },
                modifier = Modifier.testTag("menu_import_layout")
              )

              DropdownMenuItem(
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Palette,
                      contentDescription = null,
                      tint = PrimaryPurpleDark,
                      modifier = Modifier.size(18.dp)
                    )
                    Text("Customize Space...", color = PrimaryPurpleDark, fontWeight = FontWeight.Medium)
                  }
                },
                onClick = {
                  showSpaceSwitcherMenu = false
                  showCustomizationDialogForActiveSpace = true
                },
                modifier = Modifier.testTag("menu_customize_active_space")
              )

              DropdownMenuItem(
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.GridView,
                      contentDescription = null,
                      tint = PrimaryPurpleDark,
                      modifier = Modifier.size(18.dp)
                    )
                    Text("System Recent Apps...", color = PrimaryPurpleDark, fontWeight = FontWeight.Medium)
                  }
                },
                onClick = {
                  showSpaceSwitcherMenu = false
                  val result = RecentsController.invokeNativeRecents(context)
                  when (result) {
                    RecentsInvocationResult.SUCCESS -> {}
                    RecentsInvocationResult.SERVICE_DISABLED -> {
                      showRecentsDisclosureDialog = true
                    }
                    RecentsInvocationResult.ACTION_FAILED -> {
                      spaceViewModel.postFeedback("System Recents action failed to execute")
                    }
                    RecentsInvocationResult.ACTION_UNAVAILABLE -> {
                      spaceViewModel.postFeedback("Global Recents action unavailable on device")
                    }
                  }
                },
                modifier = Modifier.testTag("menu_system_recent_apps")
              )

              DropdownMenuItem(
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Settings,
                      contentDescription = null,
                      tint = TextSecondary,
                      modifier = Modifier.size(18.dp)
                    )
                    Text("Manage Spaces...", color = TextPrimary, fontWeight = FontWeight.Medium)
                  }
                },
                onClick = {
                  showSpaceSwitcherMenu = false
                  onOpenConfiguration()
                }
              )
            }
          }

          // Top Right Action Buttons: Native Recents, Lock, Settings
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            IconButton(
              onClick = {
                val result = RecentsController.invokeNativeRecents(context)
                when (result) {
                  RecentsInvocationResult.SUCCESS -> {}
                  RecentsInvocationResult.SERVICE_DISABLED -> {
                    showRecentsDisclosureDialog = true
                  }
                  RecentsInvocationResult.ACTION_FAILED -> {
                    spaceViewModel.postFeedback("System Recents action failed to execute")
                  }
                  RecentsInvocationResult.ACTION_UNAVAILABLE -> {
                    spaceViewModel.postFeedback("Global Recents action unavailable on device")
                  }
                }
              },
              modifier = Modifier
                .size(36.dp)
                .testTag("btn_trigger_native_recents")
            ) {
              Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Native System Recents",
                tint = if (isDarkThemeBackground) Color.White.copy(alpha = 0.9f) else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = { spaceViewModel.lockPhone() },
              modifier = Modifier
                .size(36.dp)
                .testTag("btn_lock_phone")
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock Multi-Space",
                tint = if (isDarkThemeBackground) Color.White.copy(alpha = 0.9f) else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = onOpenConfiguration,
              modifier = Modifier
                .size(36.dp)
                .testTag("btn_open_config")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Launcher Settings",
                tint = if (isDarkThemeBackground) Color.White.copy(alpha = 0.9f) else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      },
      bottomBar = {
        // Space Dock Bar (Persistent on Home / Layer 1)
        if (isCurrentSpaceUnlocked && activeSpace != null && activeLayerIndex == 1) {
          SpaceDockBar(
            dockItems = activeDockItems,
            allApps = discoveryUiState.allApps,
            capacity = activeSpace?.dockCapacity ?: 5,
            accessMode = activeSpace?.layer2AccessMode ?: Space.ACCESS_MODE_DOCK_BUTTON,
            getBitmap = { discoveryViewModel.getAppIconBitmap(it) },
            onLaunchApp = onLaunchApp,
            onOpenLayer2 = { spaceViewModel.setLayer(2) },
            onRemoveFromDock = { item ->
              activeSpace?.let { spaceViewModel.removeAppFromDock(it.id, item.id) }
            },
            useLayer2 = activeSpace?.useLayer2 ?: true,
            modifier = Modifier.navigationBarsPadding()
          )
        }
      }
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        when {
          !isCurrentSpaceUnlocked -> {
            // Protected Space Locked State
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Surface(
                shape = CircleShape,
                color = if (isDarkThemeBackground) Color.Black.copy(alpha = 0.6f) else PrimaryContainerLight,
                modifier = Modifier.size(72.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isDarkThemeBackground) Color.White else PrimaryPurpleDark,
                    modifier = Modifier.size(36.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "${activeSpace?.name ?: "Space"} is Protected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = headerContentColor
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Enter your PIN to access applications in this Space.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkThemeBackground) Color.White.copy(alpha = 0.8f) else TextSecondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(20.dp))
              Button(
                onClick = { showUnlockForActiveSpace = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                modifier = Modifier.testTag("btn_unlock_active_space")
              ) {
                Icon(
                  imageVector = Icons.Default.Key,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enter PIN")
              }
            }
          }
          discoveryUiState.isLoading && discoveryUiState.allApps.isEmpty() -> {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(color = if (isDarkThemeBackground) Color.White else PrimaryPurpleDark)
            }
          }
          discoveryUiState.errorMessage != null && spaceScopedApps.isEmpty() -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Unable to load Space apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = headerContentColor
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = discoveryUiState.errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkThemeBackground) Color.White.copy(alpha = 0.8f) else TextSecondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(16.dp))
              Button(
                onClick = { discoveryViewModel.loadApps() },
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Retry")
              }
            }
          }
          spaceScopedApps.isEmpty() -> {
            // Empty Space State prompting to configure app memberships
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Surface(
                shape = CircleShape,
                color = if (isDarkThemeBackground) Color.Black.copy(alpha = 0.6f) else PrimaryContainerLight,
                modifier = Modifier.size(72.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = if (isDarkThemeBackground) Color.White else PrimaryPurpleDark,
                    modifier = Modifier.size(36.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "No apps in ${activeSpace?.name ?: "this Space"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = headerContentColor
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Assign apps to this Space or import your existing layout.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkThemeBackground) Color.White.copy(alpha = 0.8f) else TextSecondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(20.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = onOpenConfiguration,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.testTag("btn_empty_space_configure")
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Add Apps")
                }

                OutlinedButton(
                  onClick = { showImportDialog = true },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.testTag("btn_empty_space_import")
                ) {
                  Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Import Layout")
                }
              }
            }
          }
          else -> {
            // Main 2-Layer Workspace with animated transition
            val currentSpace = activeSpace ?: Space.createDefault()

            AnimatedContent(
              targetState = activeLayerIndex,
              transitionSpec = {
                if (targetState == 2) {
                  (slideInVertically(animationSpec = tween(300)) { it } + fadeIn()).togetherWith(
                    slideOutVertically(animationSpec = tween(300)) { -it / 3 } + fadeOut()
                  )
                } else {
                  (slideInVertically(animationSpec = tween(300)) { -it / 3 } + fadeIn()).togetherWith(
                    slideOutVertically(animationSpec = tween(300)) { it } + fadeOut()
                  )
                }
              },
              label = "layer_transition"
            ) { layer ->
              if (layer == 2) {
                // Layer 2: Space App Library (All Space Apps with Search)
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
                  onCloseLayer2 = { spaceViewModel.setLayer(1) }
                )
              } else {
                // Layer 1: Curated Workspace (Pages / Scrolling Grid & Folders)
                Layer1HomeScreen(
                  space = currentSpace,
                  placements = activePlacements,
                  folders = activeFolders,
                  allApps = discoveryUiState.allApps,
                  getBitmap = { discoveryViewModel.getAppIconBitmap(it) },
                  onLaunchApp = onLaunchApp,
                  onOpenFolder = { folder -> activeFolderInDialog = folder },
                  onRemovePlacement = { placementId ->
                    spaceViewModel.removePlacement(placementId)
                  },
                  onCreateFolderFromApps = { src, tgt, srcId, tgtId ->
                    spaceViewModel.createFolderFromApps(
                      spaceId = currentSpace.id,
                      pageIndex = 0,
                      positionIndex = 0,
                      folderName = "New Folder",
                      sourceApp = src,
                      targetApp = tgt,
                      sourcePlacementId = srcId,
                      targetPlacementId = tgtId
                    )
                  },
                  onAddAppToHome = { app, page ->
                    spaceViewModel.addAppToHome(currentSpace.id, app, page)
                  },
                  onMovePlacement = { placementId, targetPage, targetPos ->
                    spaceViewModel.moveAppToPage(currentSpace.id, placementId, targetPage, targetPos)
                  }
                )
              }
            }
          }
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

  // Live Space Customization Dialog on Home
  if (showCustomizationDialogForActiveSpace && activeSpace != null) {
    SpaceCustomizationDialog(
      space = activeSpace!!,
      spaceApps = spaceScopedApps,
      onDismiss = { showCustomizationDialogForActiveSpace = false },
      onSave = { bgType, bgColor, bgUri, cols, size, showLabels ->
        spaceViewModel.updateSpaceCustomization(
          spaceId = activeSpace!!.id,
          backgroundType = bgType,
          backgroundColor = bgColor,
          backgroundImageUri = bgUri,
          gridColumns = cols,
          iconSize = size,
          labelVisibility = showLabels
        )
      },
      onReorderApp = { app, direction ->
        spaceViewModel.reorderSpaceApp(activeSpace!!.id, app, direction)
      },
      onSortAlphabetically = {
        spaceViewModel.sortSpaceAppsAlphabetically(activeSpace!!.id, spaceScopedApps)
      }
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

  // Native Recents Disclosure Dialog
  if (showRecentsDisclosureDialog) {
    NativeRecentsDisclosureDialog(
      onDismiss = { showRecentsDisclosureDialog = false },
      onAcceptAndOpenSettings = {
        showRecentsDisclosureDialog = false
        context.startActivity(RecentsController.createAccessibilitySettingsIntent())
      }
    )
  }
}


