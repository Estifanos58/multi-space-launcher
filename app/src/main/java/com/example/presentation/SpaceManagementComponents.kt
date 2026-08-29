package com.example.presentation

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.Space
import com.example.domain.model.SpaceMembership
import com.example.ui.theme.DarkTerminalAccent
import com.example.ui.theme.DarkTerminalSurface
import com.example.ui.theme.DarkTerminalText
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.LightSurfaceContainerHigh
import com.example.ui.theme.LightSurfaceContainerLow
import com.example.ui.theme.PrimaryContainerBadge
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleDark
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
            onSelectActive = { spaceViewModel.selectActiveSpace(space.id) },
            onRename = { spaceToRename = space },
            onDelete = { spaceToDelete = space },
            onManageMemberships = { spaceForMemberships = space }
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
}

@Composable
private fun SpaceItemCard(
  space: Space,
  isActive: Boolean,
  onSelectActive: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
  onManageMemberships: () -> Unit
) {
  val formattedDate = remember(space.createdAt) {
    SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(space.createdAt))
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (isActive) PrimaryContainerLight else LightSurfaceContainerLow,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("space_item_${space.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.clickable { onSelectActive() }
        ) {
          Icon(
            imageVector = if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isActive) "Active Space" else "Inactive Space",
            tint = if (isActive) PrimaryPurpleDark else TextMuted,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = space.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              if (isActive) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = PrimaryPurpleDark
                ) {
                  Text(
                    text = "ACTIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
            }
            Text(
              text = "ID: ${space.id} · Created: $formattedDate",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = TextSecondary,
              fontSize = 10.sp
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(
            onClick = onManageMemberships,
            modifier = Modifier.size(32.dp).testTag("space_memberships_${space.id}")
          ) {
            Icon(
              imageVector = Icons.Default.List,
              contentDescription = "Manage Memberships",
              tint = PrimaryPurpleDark,
              modifier = Modifier.size(18.dp)
            )
          }
          IconButton(
            onClick = onRename,
            modifier = Modifier.size(32.dp).testTag("space_rename_${space.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Rename Space",
              tint = TextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp).testTag("space_delete_${space.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Space",
              tint = Color(0xFFC62828),
              modifier = Modifier.size(18.dp)
            )
          }
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
    spaceViewModel.spaceRepository.getMembershipsForSpaceFlow(space.id)
  }
  val memberships by membershipsFlow.collectAsState(initial = emptyList())

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = LightBackground,
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.85f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Space Memberships",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "${space.name} · ${memberships.size} of ${allApps.size} apps assigned",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Check, contentDescription = "Done")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = LightSurfaceContainerHigh)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items(allApps, key = { it.id }) { app ->
            val isMember = memberships.any {
              it.packageName == app.packageName &&
                it.componentName == app.activityName &&
                it.userHandleId == app.userHandleId
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isMember) PrimaryContainerLight else LightSurfaceContainer,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  spaceViewModel.toggleAppMembership(space.id, app)
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
                  onCheckedChange = {
                    spaceViewModel.toggleAppMembership(space.id, app)
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryPurple,
                    checkmarkColor = Color.White
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Done")
        }
      }
    }
  }
}
