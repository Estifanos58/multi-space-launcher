package com.example.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.Space
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Configuration / Management Surface.
 * Opened when user launches the Multi-Space Launcher app normally from app drawer.
 * Houses Space Management, Space Creation, Renaming, Deletion, Membership assignment,
 * Launcher status, Catalog metrics, and Diagnostics navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherConfigurationScreen(
  modifier: Modifier = Modifier,
  spaceViewModel: SpaceViewModel,
  discoveryViewModel: AppDiscoveryViewModel,
  isDefaultHome: Boolean,
  onRequestSetDefaultHome: () -> Unit,
  onOpenDiagnostics: () -> Unit,
  onOpenHomeSurface: () -> Unit
) {
  val spaces by spaceViewModel.allSpaces.collectAsStateWithLifecycle()
  val activeSpace by spaceViewModel.activeSpace.collectAsStateWithLifecycle()
  val discoveryUiState by discoveryViewModel.uiState.collectAsStateWithLifecycle()

  var showCreateDialog by rememberSaveable { mutableStateOf(false) }
  var renameTargetSpace by remember { mutableStateOf<Space?>(null) }
  var deleteTargetSpace by remember { mutableStateOf<Space?>(null) }
  var membershipTargetSpace by remember { mutableStateOf<Space?>(null) }
  var customizeTargetSpace by remember { mutableStateOf<Space?>(null) }
  var setPinTargetSpace by remember { mutableStateOf<Space?>(null) }
  var changePinTargetSpace by remember { mutableStateOf<Space?>(null) }
  var disablePinTargetSpace by remember { mutableStateOf<Space?>(null) }
  var spaceToUnlockForSwitch by remember { mutableStateOf<Space?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    spaceViewModel.userFeedback.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = LightBackground,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Multi-Space Configuration",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Manage Spaces & App Memberships",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        actions = {
          FilledTonalButton(
            onClick = onOpenHomeSurface,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.padding(end = 8.dp).testTag("btn_switch_to_home")
          ) {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = "Go to Launcher Home",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Home View", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = LightBackground
        )
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // 1. Active Space Summary Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PrimaryContainerLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "CURRENT ACTIVE SPACE",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryPurpleDark,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = activeSpace?.name ?: "Default",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
              )
              Text(
                text = "ID: ${activeSpace?.id ?: "space_default"}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PrimaryPurpleDark),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Active",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
          }
        }
      }

      // 2. Spaces Management Section
      item {
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
              Column {
                Text(
                  text = "Your Spaces",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "${spaces.size} space${if (spaces.size != 1) "s" else ""} configured",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary
                )
              }
              Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_create_space")
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Create Space",
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Space", fontSize = 13.sp)
              }
            }

            HorizontalDivider(color = LightSurfaceContainerHigh)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              spaces.forEach { space ->
                SpaceRowItem(
                  space = space,
                  isActive = space.id == activeSpace?.id,
                  onSelectActive = {
                    if (space.isProtected && !spaceViewModel.isSpaceUnlocked(space)) {
                      spaceToUnlockForSwitch = space
                    } else {
                      spaceViewModel.selectActiveSpace(space.id)
                    }
                  },
                  onCustomize = { customizeTargetSpace = space },
                  onManageMemberships = { membershipTargetSpace = space },
                  onManagePin = {
                    if (space.isProtected) {
                      changePinTargetSpace = space
                    } else {
                      setPinTargetSpace = space
                    }
                  },
                  onDisablePin = { disablePinTargetSpace = space },
                  onRename = { renameTargetSpace = space },
                  onDelete = { deleteTargetSpace = space }
                )
              }
            }
          }
        }
      }

      // 3. Launcher Status & Diagnostics Navigation Card
      item {
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
              text = "Launcher Environment",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Default Home Launcher",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDefaultHome) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                  ) {
                    Text(
                      text = if (isDefaultHome) "ACTIVE DEFAULT" else "NOT DEFAULT",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isDefaultHome) Color(0xFF2E7D32) else Color(0xFFE65100),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      fontSize = 10.sp
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = if (isDefaultHome)
                    "Home button opens your active Space. Launching the app directly opens this Configuration page."
                  else
                    "Set as default Home so pressing Home opens your active Space.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                  fontSize = 12.sp
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = onRequestSetDefaultHome,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDefaultHome) LightSurfaceContainerHigh else PrimaryPurpleDark,
                  contentColor = if (isDefaultHome) TextPrimary else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = if (isDefaultHome) "Change" else "Set Default",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            HorizontalDivider(color = LightSurfaceContainerHigh)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Discovered Applications",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = TextPrimary
                )
                Text(
                  text = "${discoveryUiState.allApps.size} apps (${discoveryUiState.userAppCount} user, ${discoveryUiState.systemAppCount} system)",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary
                )
              }
              IconButton(
                onClick = { discoveryViewModel.loadApps() },
                enabled = !discoveryUiState.isLoading
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh Catalog",
                  tint = PrimaryPurpleDark
                )
              }
            }

            HorizontalDivider(color = LightSurfaceContainerHigh)

            OutlinedButton(
              onClick = onOpenDiagnostics,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_open_diagnostics")
            ) {
              Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Diagnostics",
                modifier = Modifier.size(16.dp),
                tint = TextSecondary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Open Diagnostics & Telemetry", color = TextPrimary, fontSize = 13.sp)
            }
          }
        }
      }
    }
  }

  // Dialogs
  if (showCreateDialog) {
    CreateSpaceDialog(
      onDismiss = { showCreateDialog = false },
      onConfirm = { name ->
        spaceViewModel.createSpace(name)
        showCreateDialog = false
      }
    )
  }

  renameTargetSpace?.let { space ->
    RenameSpaceDialog(
      initialName = space.name,
      onDismiss = { renameTargetSpace = null },
      onConfirm = { newName ->
        spaceViewModel.renameSpace(space.id, newName)
        renameTargetSpace = null
      }
    )
  }

  deleteTargetSpace?.let { space ->
    DeleteSpaceDialog(
      spaceName = space.name,
      isOnlySpace = spaces.size <= 1,
      onDismiss = { deleteTargetSpace = null },
      onConfirm = {
        spaceViewModel.deleteSpace(space.id)
        deleteTargetSpace = null
      }
    )
  }

  membershipTargetSpace?.let { space ->
    ManageMembershipsDialog(
      space = space,
      allApps = discoveryUiState.allApps,
      spaceViewModel = spaceViewModel,
      onDismiss = { membershipTargetSpace = null }
    )
  }

  setPinTargetSpace?.let { space ->
    SetSpacePinDialog(
      space = space,
      onDismiss = { setPinTargetSpace = null },
      onPinSet = { pin ->
        spaceViewModel.setSpacePin(space.id, pin)
        setPinTargetSpace = null
      }
    )
  }

  changePinTargetSpace?.let { space ->
    ChangeSpacePinDialog(
      space = space,
      onDismiss = { changePinTargetSpace = null },
      onPinChanged = { currentPin, newPin ->
        spaceViewModel.changeSpacePin(space.id, currentPin, newPin)
        changePinTargetSpace = null
      }
    )
  }

  disablePinTargetSpace?.let { space ->
    DisableSpacePinDialog(
      space = space,
      onDismiss = { disablePinTargetSpace = null },
      onPinDisabled = { currentPin ->
        spaceViewModel.disableSpacePin(space.id, currentPin)
        disablePinTargetSpace = null
      }
    )
  }

  customizeTargetSpace?.let { space ->
    val membershipsFlow = remember(space.id) {
      spaceViewModel.getMembershipsFlowForSpace(space.id)
    }
    val memberships by membershipsFlow.collectAsState(initial = emptyList())
    val spaceApps = remember(discoveryUiState.allApps, memberships) {
      val appsByComponent = discoveryUiState.allApps.associateBy { "${it.packageName}/${it.activityName}" }
      val appsByPackage = discoveryUiState.allApps.associateBy { it.packageName }
      val result = mutableListOf<DiscoveredApp>()
      val included = mutableSetOf<String>()
      for (m in memberships) {
        val app = appsByComponent["${m.packageName}/${m.componentName}"] ?: appsByPackage[m.packageName]
        if (app != null && included.add("${app.packageName}/${app.activityName}/${app.userHandleId}")) {
          result.add(app)
        }
      }
      result
    }

    SpaceCustomizationDialog(
      space = space,
      spaceApps = spaceApps,
      onDismiss = { customizeTargetSpace = null },
      onSave = { bgType, bgColor, bgUri, cols, size, showLabels ->
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
      onReorderApp = { app, dir ->
        spaceViewModel.reorderSpaceApp(space.id, app, dir)
      },
      onSortAlphabetically = {
        spaceViewModel.sortSpaceAppsAlphabetically(space.id, spaceApps)
      }
    )
  }

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

@Composable
private fun SpaceRowItem(
  space: Space,
  isActive: Boolean,
  onSelectActive: () -> Unit,
  onCustomize: () -> Unit,
  onManageMemberships: () -> Unit,
  onManagePin: () -> Unit,
  onDisablePin: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit
) {
  val formattedDate = remember(space.createdAt) {
    SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(space.createdAt))
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isActive) PrimaryContainerLight else LightSurfaceContainerLow,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("config_space_item_${space.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelectActive() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
          contentDescription = if (isActive) "Active Space" else "Inactive Space",
          tint = if (isActive) PrimaryPurpleDark else TextMuted,
          modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = space.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            if (isActive) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = PrimaryPurpleDark
              ) {
                Text(
                  text = "ACTIVE",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            if (space.isProtected) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF2E7D32)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Protected",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                  )
                  Text(
                    text = "PIN",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
          Text(
            text = "ID: ${space.id} · Created: $formattedDate",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp
          )
        }
      }

      HorizontalDivider(color = LightSurfaceContainerHigh)

      // Dedicated Action Buttons for Apps Membership, PIN Security, Rename, and Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onManageMemberships,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("config_space_memberships_${space.id}")
        ) {
          Icon(
            imageVector = Icons.Default.List,
            contentDescription = "Manage Memberships",
            tint = PrimaryPurpleDark,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Apps", fontSize = 11.sp, color = PrimaryPurpleDark)
        }

        OutlinedButton(
          onClick = onCustomize,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("config_space_customize_${space.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = "Customize Space",
            tint = PrimaryPurpleDark,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Style", fontSize = 11.sp, color = PrimaryPurpleDark)
        }

        OutlinedButton(
          onClick = onManagePin,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("config_space_pin_${space.id}")
        ) {
          Icon(
            imageVector = if (space.isProtected) Icons.Default.LockReset else Icons.Default.Lock,
            contentDescription = "PIN Security",
            tint = if (space.isProtected) Color(0xFF2E7D32) else TextSecondary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            if (space.isProtected) "PIN" else "+PIN",
            fontSize = 11.sp,
            color = if (space.isProtected) Color(0xFF2E7D32) else TextPrimary
          )
        }

        if (space.isProtected) {
          OutlinedButton(
            onClick = onDisablePin,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = Color(0xFFE65100)
            ),
            modifier = Modifier.testTag("config_space_disable_pin_${space.id}")
          ) {
            Icon(
              imageVector = Icons.Default.LockOpen,
              contentDescription = "Remove PIN",
              tint = Color(0xFFE65100),
              modifier = Modifier.size(14.dp)
            )
          }
        }

        OutlinedButton(
          onClick = onRename,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("config_space_rename_${space.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Rename Space",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Rename", fontSize = 11.sp, color = TextPrimary)
        }

        OutlinedButton(
          onClick = onDelete,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFC62828)
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("config_space_delete_${space.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Space",
            tint = Color(0xFFC62828),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Delete", fontSize = 11.sp, color = Color(0xFFC62828))
        }
      }
    }
  }
}
