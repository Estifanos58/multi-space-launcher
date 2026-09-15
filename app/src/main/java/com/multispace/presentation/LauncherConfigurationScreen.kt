package com.multispace.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppIdentityLookup
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.appIdentity
import com.multispace.ui.components.ModernCard
import com.multispace.ui.components.ModernSectionHeader
import com.multispace.ui.components.ModernStatusBadge
import com.multispace.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Configuration / Management Surface.
 * Houses Space Management, Space Creation, Renaming, Deletion, Membership assignment,
 * Launcher status, Catalog metrics, and Diagnostics navigation with modern obsidian/glassmorphic aesthetics.
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
  onOpenHomeSurface: () -> Unit,
  onOpenCreateSpace: (() -> Unit)? = null,
  onOpenEditSpace: ((Space) -> Unit)? = null
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
  var spaceToAuthForEdit by remember { mutableStateOf<Space?>(null) }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    spaceViewModel.userFeedback.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Multi-Space Control",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Workspace Isolation & App Partitioning",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          IconButton(
            onClick = { spaceViewModel.lockPhone() },
            modifier = Modifier.testTag("btn_config_lock_phone")
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Lock Device",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(AppDimens.IconSm)
            )
          }
          FilledTonalButton(
            onClick = onOpenHomeSurface,
            shape = ShapeRoundMd,
            contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing6),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.padding(end = AppDimens.Spacing8).testTag("btn_switch_to_home")
          ) {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = "Go to Launcher Home",
              modifier = Modifier.size(AppDimens.IconSm)
            )
            Spacer(modifier = Modifier.width(AppDimens.Spacing6))
            Text("Home View", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = AppDimens.Spacing12),
      verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
      contentPadding = PaddingValues(vertical = AppDimens.Spacing12)
    ) {
      // 1. Configured Spaces Management Section (with prominent Create New Space button)
      item {
        ModernCard(
          modifier = Modifier.fillMaxWidth(),
          shape = ShapeRoundLg
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                ModernSectionHeader(
                  title = "Configured Spaces",
                  subtitle = "${spaces.size} space${if (spaces.size != 1) "s" else ""} • Active: ${activeSpace?.name ?: "Default"}"
                )
              }
              ModernStatusBadge(
                text = "${spaces.size} SPACES",
                color = QuantumViolet
              )
            }

            // Prominent Create New Space Primary Button
            Button(
              onClick = {
                if (onOpenCreateSpace != null) {
                  onOpenCreateSpace()
                } else {
                  showCreateDialog = true
                }
              },
              shape = ShapeRoundMd,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ButtonHeight)
                .testTag("btn_create_space")
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Space",
                modifier = Modifier.size(AppDimens.IconSm)
              )
              Spacer(modifier = Modifier.width(AppDimens.Spacing8))
              Text(
                text = "Create New Space",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
              spaces.forEach { space ->
                SpaceRowItem(
                  space = space,
                  isActive = space.id == activeSpace?.id,
                  onSelectActive = {
                    if (space.isProtected) {
                      spaceToUnlockForSwitch = space
                    } else {
                      spaceViewModel.selectActiveSpace(space.id)
                    }
                  },
                  onEdit = {
                    if (space.isProtected && !spaceViewModel.isSpaceUnlocked(space)) {
                      spaceToAuthForEdit = space
                    } else {
                      if (onOpenEditSpace != null) {
                        onOpenEditSpace(space)
                      } else {
                        renameTargetSpace = space
                      }
                    }
                  },
                  onDelete = { deleteTargetSpace = space }
                )
              }
            }
          }
        }
      }

      // 2. Launcher Status & Diagnostics Navigation Card
      item {
        ModernCard(
          modifier = Modifier.fillMaxWidth(),
          shape = ShapeRoundLg
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing10)
          ) {
            ModernSectionHeader(
              title = "Launcher Environment",
              subtitle = "System integration and OS default handler status"
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.width(AppDimens.Spacing8))
                  ModernStatusBadge(
                    text = if (isDefaultHome) "ACTIVE DEFAULT" else "NOT DEFAULT",
                    color = if (isDefaultHome) EmeraldCore else AmberPulse
                  )
                }
                Spacer(modifier = Modifier.height(AppDimens.Spacing2))
                Text(
                  text = if (isDefaultHome)
                    "Home button opens active Space."
                  else
                    "Set as default Home so pressing Home opens your active Space.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.width(AppDimens.Spacing8))
              Button(
                onClick = onRequestSetDefaultHome,
                shape = ShapeRoundMd,
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDefaultHome) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                  contentColor = if (isDefaultHome) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing6)
              ) {
                Text(
                  text = if (isDefaultHome) "Change" else "Set Default",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Discovered Applications",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${discoveryUiState.allApps.size} apps (${discoveryUiState.userAppCount} user, ${discoveryUiState.systemAppCount} system)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              IconButton(
                onClick = { discoveryViewModel.loadApps() },
                enabled = !discoveryUiState.isLoading
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh Catalog",
                  tint = QuantumViolet
                )
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedButton(
              onClick = onOpenDiagnostics,
              shape = ShapeRoundMd,
              modifier = Modifier.fillMaxWidth().testTag("btn_open_diagnostics")
            ) {
              Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Diagnostics",
                modifier = Modifier.size(AppDimens.IconSm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(AppDimens.Spacing8))
              Text(
                "Open Diagnostics & Telemetry",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
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
    if (spaces.size <= 1) {
      DeleteSpaceDialog(
        spaceName = space.name,
        isOnlySpace = true,
        onDismiss = { deleteTargetSpace = null },
        onConfirm = { deleteTargetSpace = null }
      )
    } else if (space.isProtected) {
      DeleteSpaceCredentialDialog(
        space = space,
        onDismiss = { deleteTargetSpace = null },
        onConfirmDelete = {
          spaceViewModel.deleteSpace(space.id)
          deleteTargetSpace = null
        },
        spaceViewModel = spaceViewModel
      )
    } else {
      DeleteSpaceDialog(
        spaceName = space.name,
        isOnlySpace = false,
        onDismiss = { deleteTargetSpace = null },
        onConfirm = {
          spaceViewModel.deleteSpace(space.id)
          deleteTargetSpace = null
        }
      )
    }
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
      val lookup = AppIdentityLookup(discoveryUiState.allApps)
      val result = mutableListOf<DiscoveredApp>()
      val included = mutableSetOf<AppIdentity>()
      for (m in memberships) {
        val app = lookup[m.appIdentity]
        if (app != null && included.add(app.appIdentity)) {
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

  spaceToAuthForEdit?.let { space ->
    EditSpaceCredentialDialog(
      space = space,
      onDismiss = { spaceToAuthForEdit = null },
      onAuthSuccess = {
        val target = space
        spaceToAuthForEdit = null
        if (onOpenEditSpace != null) {
          onOpenEditSpace(target)
        } else {
          renameTargetSpace = target
        }
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
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val formattedDate = remember(space.createdAt) {
    SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(space.createdAt))
  }

  Surface(
    shape = ShapeRoundMd,
    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(
      AppDimens.BorderThin,
      if (isActive) QuantumViolet.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("config_space_item_${space.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(AppDimens.Spacing16),
      verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing10)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelectActive() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing10)
      ) {
        Icon(
          imageVector = if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
          contentDescription = if (isActive) "Active Space" else "Inactive Space",
          tint = if (isActive) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(AppDimens.IconMd)
        )
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            Text(
              text = space.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            if (isActive) {
              ModernStatusBadge(
                text = "ACTIVE",
                color = QuantumViolet
              )
            }
            if (space.isProtected) {
              val isPattern = space.isPatternProtected || space.authPolicy == Space.AUTH_PATTERN
              ModernStatusBadge(
                text = if (isPattern) "PATTERN" else "PIN",
                color = EmeraldCore
              )
            }
          }
          Text(
            text = "ID: ${space.id} · Created: $formattedDate",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Exactly two action buttons: Edit Space and Delete Space
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing10),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onEdit,
          shape = ShapeRoundMd,
          contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
          modifier = Modifier
            .weight(1f)
            .testTag("config_space_edit_${space.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Space",
            tint = QuantumViolet,
            modifier = Modifier.size(AppDimens.IconSm)
          )
          Spacer(modifier = Modifier.width(AppDimens.Spacing6))
          Text("Edit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = QuantumViolet)
        }

        OutlinedButton(
          onClick = onDelete,
          shape = ShapeRoundMd,
          contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = CrimsonNova
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("config_space_delete_${space.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Space",
            tint = CrimsonNova,
            modifier = Modifier.size(AppDimens.IconSm)
          )
          Spacer(modifier = Modifier.width(AppDimens.Spacing6))
          Text("Delete", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = CrimsonNova)
        }
      }
    }
  }
}
