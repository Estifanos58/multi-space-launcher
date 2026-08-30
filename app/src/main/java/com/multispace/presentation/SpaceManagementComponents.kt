package com.multispace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceMembership
import com.multispace.ui.theme.DarkTerminalAccent
import com.multispace.ui.theme.DarkTerminalSurface
import com.multispace.ui.theme.DarkTerminalText
import com.multispace.ui.theme.LightBackground
import com.multispace.ui.theme.LightSurfaceContainer
import com.multispace.ui.theme.LightSurfaceContainerHigh
import com.multispace.ui.theme.LightSurfaceContainerLow
import com.multispace.ui.theme.PrimaryContainerBadge
import com.multispace.ui.theme.PrimaryContainerLight
import com.multispace.ui.theme.PrimaryPurple
import com.multispace.ui.theme.PrimaryPurpleDark
import com.multispace.ui.theme.StatusGreen
import com.multispace.ui.theme.TextMuted
import com.multispace.ui.theme.TextPrimary
import com.multispace.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpaceManagementSection(
  spaceViewModel: SpaceViewModel,
  allApps: List<DiscoveredApp>,
  modifier: Modifier = Modifier
) {
  val spaces by spaceViewModel.allSpaces.collectAsState()
  val activeSpace by spaceViewModel.activeSpace.collectAsState()

  var showCreateDialog by remember { mutableStateOf(false) }
  var spaceToRename by remember { mutableStateOf<Space?>(null) }
  var spaceToDelete by remember { mutableStateOf<Space?>(null) }
  var spaceForMemberships by remember { mutableStateOf<Space?>(null) }
  var spaceForCustomization by remember { mutableStateOf<Space?>(null) }
  var spaceForSetPin by remember { mutableStateOf<Space?>(null) }
  var spaceForChangePin by remember { mutableStateOf<Space?>(null) }
  var spaceForDisablePin by remember { mutableStateOf<Space?>(null) }
  var spaceToUnlockForSwitch by remember { mutableStateOf<Space?>(null) }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = LightSurfaceContainer,
    modifier = modifier.fillMaxWidth()
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
            text = "SPACE DOMAIN & PERSISTENCE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryPurpleDark
          )
          Text(
            text = "Room SQLite Database · ${spaces.size} Spaces Persisted",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }

        Button(
          onClick = { showCreateDialog = true },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("create_space_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create Space",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Create", fontSize = 12.sp)
        }
      }

      HorizontalDivider(color = LightSurfaceContainerHigh)

      // Spaces List
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        spaces.forEach { space ->
          val isActive = space.id == activeSpace?.id
          SpaceItemCard(
            space = space,
            isActive = isActive,
            onSelectActive = {
              if (space.isProtected) {
                spaceToUnlockForSwitch = space
              } else {
                spaceViewModel.selectActiveSpace(space.id)
              }
            },
            onCustomize = { spaceForCustomization = space },
            onRename = { spaceToRename = space },
            onDelete = { spaceToDelete = space },
            onManageMemberships = { spaceForMemberships = space },
            onManagePin = {
              if (space.isProtected) {
                spaceForChangePin = space
              } else {
                spaceForSetPin = space
              }
            },
            onDisablePin = { spaceForDisablePin = space }
          )
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateSpaceDialog(
      onDismiss = { showCreateDialog = false },
      onConfirm = { name ->
        spaceViewModel.createSpace(name)
        showCreateDialog = false
      }
    )
  }

  spaceToRename?.let { space ->
    RenameSpaceDialog(
      initialName = space.name,
      onDismiss = { spaceToRename = null },
      onConfirm = { newName ->
        spaceViewModel.renameSpace(space.id, newName)
        spaceToRename = null
      }
    )
  }

  spaceToDelete?.let { space ->
    DeleteSpaceDialog(
      spaceName = space.name,
      isOnlySpace = spaces.size <= 1,
      onDismiss = { spaceToDelete = null },
      onConfirm = {
        spaceViewModel.deleteSpace(space.id)
        spaceToDelete = null
      }
    )
  }

  spaceForMemberships?.let { space ->
    ManageMembershipsDialog(
      space = space,
      allApps = allApps,
      spaceViewModel = spaceViewModel,
      onDismiss = { spaceForMemberships = null }
    )
  }

  spaceForSetPin?.let { space ->
    SetSpacePinDialog(
      space = space,
      onDismiss = { spaceForSetPin = null },
      onPinSet = { pin ->
        spaceViewModel.setSpacePin(space.id, pin)
        spaceForSetPin = null
      }
    )
  }

  spaceForChangePin?.let { space ->
    ChangeSpacePinDialog(
      space = space,
      onDismiss = { spaceForChangePin = null },
      onPinChanged = { currentPin, newPin ->
        spaceViewModel.changeSpacePin(space.id, currentPin, newPin)
        spaceForChangePin = null
      }
    )
  }

  spaceForDisablePin?.let { space ->
    DisableSpacePinDialog(
      space = space,
      onDismiss = { spaceForDisablePin = null },
      onPinDisabled = { currentPin ->
        spaceViewModel.disableSpacePin(space.id, currentPin)
        spaceForDisablePin = null
      }
    )
  }

  spaceForCustomization?.let { space ->
    val membershipsFlow = remember(space.id) {
      spaceViewModel.getMembershipsFlowForSpace(space.id)
    }
    val memberships by membershipsFlow.collectAsState(initial = emptyList())
    val spaceApps = remember(allApps, memberships) {
      val appsByComponent = allApps.associateBy { "${it.packageName}/${it.activityName}" }
      val appsByPackage = allApps.associateBy { it.packageName }
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
      onDismiss = { spaceForCustomization = null },
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
private fun SpaceItemCard(
  space: Space,
  isActive: Boolean,
  onSelectActive: () -> Unit,
  onCustomize: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
  onManageMemberships: () -> Unit,
  onManagePin: () -> Unit,
  onDisablePin: () -> Unit
) {
  val formattedDate = remember(space.createdAt) {
    SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(space.createdAt))
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isActive) PrimaryContainerLight else LightSurfaceContainerLow,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("space_item_${space.id}")
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
            .testTag("space_memberships_${space.id}")
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
            .testTag("space_customize_${space.id}")
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
            .testTag("space_pin_${space.id}")
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
            modifier = Modifier.testTag("space_disable_pin_${space.id}")
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
            .testTag("space_rename_${space.id}")
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
            .testTag("space_delete_${space.id}")
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

@Composable
fun CreateSpaceDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create New Space") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "Enter a name for the new Space. A stable identifier will be generated and persisted in Room SQLite.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            isError = false
          },
          label = { Text("Space Name") },
          placeholder = { Text("e.g. Work, Personal, Media") },
          isError = isError,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("create_space_input")
        )
        if (isError) {
          Text(
            "Name cannot be empty",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.trim().isEmpty()) {
            isError = true
          } else {
            onConfirm(name.trim())
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        modifier = Modifier.testTag("confirm_create_space_button")
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun RenameSpaceDialog(
  initialName: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var name by remember { mutableStateOf(initialName) }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Rename Space") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "Update display name while preserving the stable Space ID and all memberships in Room database.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            isError = false
          },
          label = { Text("New Space Name") },
          isError = isError,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("rename_space_input")
        )
        if (isError) {
          Text(
            "Name cannot be empty",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.trim().isEmpty()) {
            isError = true
          } else {
            onConfirm(name.trim())
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        modifier = Modifier.testTag("confirm_rename_space_button")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun DeleteSpaceDialog(
  spaceName: String,
  isOnlySpace: Boolean,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Delete Space") },
    text = {
      if (isOnlySpace) {
        Text(
          "Cannot delete '$spaceName' because it is the only remaining Space. The launcher requires at least one valid Space at all times.",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium
        )
      } else {
        Text(
          "Are you sure you want to delete '$spaceName'? All associated membership records in Room will be removed.",
          style = MaterialTheme.typography.bodyMedium
        )
      }
    },
    confirmButton = {
      if (!isOnlySpace) {
        Button(
          onClick = onConfirm,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
          modifier = Modifier.testTag("confirm_delete_space_button")
        ) {
          Text("Delete")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isOnlySpace) "OK" else "Cancel")
      }
    }
  )
}

@Composable
fun ManageMembershipsDialog(
  space: Space,
  allApps: List<DiscoveredApp>,
  spaceViewModel: SpaceViewModel,
  onDismiss: () -> Unit
) {
  val membershipsFlow = remember(space.id) {
    spaceViewModel.getMembershipsFlowForSpace(space.id)
  }
  val memberships by membershipsFlow.collectAsState(initial = emptyList())
  var searchQuery by remember { mutableStateOf("") }

  val memberPackageSet = remember(memberships) {
    memberships.map { it.packageName }.toSet()
  }
  val memberComponentSet = remember(memberships) {
    memberships.map { "${it.packageName}/${it.componentName}" }.toSet()
  }

  val filteredApps = remember(allApps, searchQuery) {
    if (searchQuery.isBlank()) {
      allApps
    } else {
      val q = searchQuery.trim().lowercase()
      allApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = LightBackground,
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.9f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Space Memberships",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "${space.name} · ${memberships.size} of ${allApps.size} apps assigned",
              style = MaterialTheme.typography.bodySmall,
              color = PrimaryPurpleDark,
              fontWeight = FontWeight.SemiBold
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search text field
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search apps...", fontSize = 13.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick batch actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              filteredApps.forEach { app ->
                val isMember = memberComponentSet.contains("${app.packageName}/${app.activityName}") || memberPackageSet.contains(app.packageName)
                if (!isMember) {
                  spaceViewModel.addAppToSpace(space.id, app)
                }
              }
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Select All (${filteredApps.size})", fontSize = 11.sp)
          }

          OutlinedButton(
            onClick = {
              filteredApps.forEach { app ->
                val isMember = memberComponentSet.contains("${app.packageName}/${app.activityName}") || memberPackageSet.contains(app.packageName)
                if (isMember) {
                  spaceViewModel.removeAppFromSpace(space.id, app)
                }
              }
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Clear All", fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = LightSurfaceContainerHigh)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(
            items = filteredApps,
            key = { it.id },
            contentType = { "membership_item" }
          ) { app ->
            val isMember = memberComponentSet.contains("${app.packageName}/${app.activityName}") ||
              memberPackageSet.contains(app.packageName)

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isMember) PrimaryContainerLight else LightSurfaceContainer,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  spaceViewModel.setAppMembership(space.id, app, !isMember)
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isMember) FontWeight.Bold else FontWeight.Normal,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                Checkbox(
                  checked = isMember,
                  onCheckedChange = { isChecked ->
                    spaceViewModel.setAppMembership(space.id, app, isChecked)
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryPurpleDark,
                    checkmarkColor = Color.White
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleDark),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
