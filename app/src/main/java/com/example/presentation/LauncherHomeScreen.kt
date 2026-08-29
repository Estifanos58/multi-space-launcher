package com.example.presentation

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.Space
import com.example.ui.theme.*

/**
 * Android Home / Launcher Surface (Phase 6 Clean Home).
 * Displays ONLY:
 * 1. Current Space Name (with a clean, quick Space Switcher popover)
 * 2. Applications belonging strictly to the active Space
 *
 * Excludes all administrative controls, telemetry, diagnostics, and giant toolbars.
 */
@Composable
private fun HomeAppGridItem(
  app: DiscoveredApp,
  getIcon: (DiscoveredApp) -> Drawable?,
  onLaunch: () -> Unit
) {
  val icon = remember(app.id) { getIcon(app) }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onLaunch() }
      .padding(vertical = 6.dp, horizontal = 2.dp)
      .testTag("home_app_item_${app.packageName}")
  ) {
    Box(
      modifier = Modifier
        .size(52.dp)
        .clip(RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center
    ) {
      AppIconImage(
        drawable = icon,
        contentDescription = app.label,
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = app.label,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      color = TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      fontSize = 11.sp
    )
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

  val isCurrentSpaceUnlocked = remember(activeSpace, unlockedSpaceIds) {
    spaceViewModel.isSpaceUnlocked(activeSpace)
  }

  // Resolve Space presentation: Project active Space's persisted memberships against current Android LauncherApps catalog
  // - Respects persisted ordering (order_index ASC, added_at ASC from Room)
  // - Excludes unavailable/uninstalled applications while preserving their durable membership
  // - Dynamically includes reinstalled applications when discovery updates
  // - If Space is protected and NOT unlocked in runtime, returns emptyList() to strictly prevent app leak
  // - If active Space is Default and no custom memberships are set yet, displays all discovered apps out-of-the-box
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

      // activeMemberships is already ordered by order_index ASC, added_at ASC from Room DAO
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

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = LightBackground,
    topBar = {
      // Extremely minimal Home header: Space indicator with quick switcher + subtle config access
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
            color = PrimaryContainerLight,
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
                  tint = if (isCurrentSpaceUnlocked) Color(0xFF2E7D32) else PrimaryPurpleDark,
                  modifier = Modifier.size(14.dp)
                )
              } else {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurpleDark)
                )
              }
              Text(
                text = activeSpace?.name ?: "Default",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Switch Space",
                tint = TextSecondary,
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
                  if (space.isProtected && !spaceViewModel.isSpaceUnlocked(space)) {
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
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = PrimaryPurpleDark,
                    modifier = Modifier.size(18.dp)
                  )
                  Text("Manage Spaces...", color = PrimaryPurpleDark, fontWeight = FontWeight.Medium)
                }
              },
              onClick = {
                showSpaceSwitcherMenu = false
                onOpenConfiguration()
              }
            )
          }
        }

        // Discreet button to enter full Configuration & App Manager
        IconButton(
          onClick = onOpenConfiguration,
          modifier = Modifier
            .size(36.dp)
            .testTag("btn_open_config")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Launcher Settings",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
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
              color = PrimaryContainerLight,
              modifier = Modifier.size(72.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = PrimaryPurpleDark,
                  modifier = Modifier.size(36.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "${activeSpace?.name ?: "Space"} is Protected",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Enter your PIN to access applications in this Space.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
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
            CircularProgressIndicator(color = PrimaryPurpleDark)
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
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = discoveryUiState.errorMessage ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
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
              color = PrimaryContainerLight,
              modifier = Modifier.size(72.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Apps,
                  contentDescription = null,
                  tint = PrimaryPurpleDark,
                  modifier = Modifier.size(36.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "No apps in ${activeSpace?.name ?: "this Space"}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Assign apps to this Space to display them on your Home screen.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
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
          // 4-Column Clean Application Grid
          LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 8.dp)
              .testTag("home_apps_grid"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(
              count = spaceScopedApps.size,
              key = { index ->
                val app = spaceScopedApps[index]
                "${app.packageName}/${app.activityName}/${app.userHandleId}"
              }
            ) { index ->
              val app = spaceScopedApps[index]
              HomeAppGridItem(
                app = app,
                getIcon = { discoveryViewModel.getAppIcon(it) },
                onLaunch = { onLaunchApp(app) }
              )
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
}
