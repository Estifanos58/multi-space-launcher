package com.multispace.presentation

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.ui.theme.*

/**
 * Android Home / Launcher Surface (Phase 6 & 7 Clean Home with Customizations).
 * Displays:
 * 1. Current Space Name (with quick Space Switcher & Live Customization dialog)
 * 2. Space-specific custom background (solid color, wallpaper image, or default)
 * 3. Applications belonging strictly to the active Space with custom columns (3-6),
 *    custom icon sizes (SMALL/MEDIUM/LARGE), and toggleable app labels.
 */
@Composable
private fun HomeAppGridItem(
  app: DiscoveredApp,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  containerSize: Dp,
  iconSize: Dp,
  showLabel: Boolean,
  labelColor: Color,
  onLaunch: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onLaunch() }
      .padding(vertical = 4.dp, horizontal = 2.dp)
      .testTag("home_app_item_${app.packageName}")
  ) {
    Box(
      modifier = Modifier
        .size(containerSize)
        .clip(RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center
    ) {
      AsyncAppIcon(
        app = app,
        getBitmap = getBitmap,
        contentDescription = app.label,
        modifier = Modifier.size(iconSize)
      )
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = app.label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontSize = if (containerSize < 48.dp) 10.sp else 11.sp
      )
    }
  }
}

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

  var showSpaceSwitcherMenu by remember { mutableStateOf(false) }
  var spaceToUnlockForSwitch by remember { mutableStateOf<Space?>(null) }
  var showUnlockForActiveSpace by remember { mutableStateOf(false) }
  var showCustomizationDialogForActiveSpace by remember { mutableStateOf(false) }

  val isCurrentSpaceUnlocked = remember(activeSpace, unlockedSpaceIds) {
    spaceViewModel.isSpaceUnlocked(activeSpace)
  }

  // Determine dynamic background styling and contrast
  val currentBgType = activeSpace?.backgroundType ?: Space.BACKGROUND_DEFAULT
  val currentBgColor = activeSpace?.backgroundColor
  val currentBgImageUri = activeSpace?.backgroundImageUri

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
  val appLabelColor = if (isDarkThemeBackground) Color.White else TextPrimary
  val chipBackgroundColor = if (isDarkThemeBackground) {
    Color.Black.copy(alpha = 0.5f)
  } else {
    PrimaryContainerLight
  }

  // Resolve layout customization parameters
  val gridColumns = remember(activeSpace?.gridColumns) {
    (activeSpace?.gridColumns ?: 4).coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
  }

  val (iconContainerDp, iconDrawableDp) = remember(activeSpace?.iconSize) {
    when (activeSpace?.iconSize) {
      Space.ICON_SIZE_SMALL -> 44.dp to 38.dp
      Space.ICON_SIZE_LARGE -> 64.dp to 56.dp
      else -> 52.dp to 48.dp
    }
  }

  val labelVisibility = activeSpace?.labelVisibility ?: true

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

  Box(modifier = modifier.fillMaxSize()) {
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
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          // Scrim overlay to maintain high icon/text readability over photos
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.35f))
          )
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

    // 2. Foreground UI & Applications
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      topBar = {
        // Minimal Home header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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

          // Quick lock button & Settings button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
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
                text = "Assign apps to this Space to display them on your Home screen.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkThemeBackground) Color.White.copy(alpha = 0.8f) else TextSecondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(20.dp))
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
                Text("Add Apps to Space")
              }
            }
          }
          else -> {
            // Custom Column Configurable Grid
            LazyVerticalGrid(
              columns = GridCells.Fixed(gridColumns),
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .testTag("home_apps_grid"),
              contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(
                items = spaceScopedApps,
                key = { it.id },
                contentType = { "home_app_item" }
              ) { app ->
                HomeAppGridItem(
                  app = app,
                  getBitmap = { discoveryViewModel.getAppIconBitmap(it) },
                  containerSize = iconContainerDp,
                  iconSize = iconDrawableDp,
                  showLabel = labelVisibility,
                  labelColor = appLabelColor,
                  onLaunch = { onLaunchApp(app) }
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
}

