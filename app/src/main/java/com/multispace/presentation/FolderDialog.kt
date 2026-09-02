package com.multispace.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem
import com.multispace.ui.components.ModernCard
import com.multispace.ui.components.ModernDialogContainer
import com.multispace.ui.components.ModernEmptyState
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.CrimsonNova
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderDialog(
  folder: SpaceFolder,
  allApps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onRenameFolder: (String) -> Unit,
  onRemoveItem: (SpaceFolderItem) -> Unit,
  onDeleteFolder: () -> Unit,
  onDismiss: () -> Unit
) {
  var isRenaming by remember { mutableStateOf(false) }
  var folderNameInput by remember(folder.name) { mutableStateOf(folder.name) }
  var showOptionsMenu by remember { mutableStateOf(false) }
  var selectedItemForAction by remember { mutableStateOf<SpaceFolderItem?>(null) }

  val appLookup = remember(allApps) {
    allApps.associateBy { "${it.packageName}/${it.activityName}" }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .wrapContentHeight()
        .clip(ShapeRoundLg)
        .testTag("folder_dialog"),
      shape = ShapeRoundLg,
      color = MaterialTheme.colorScheme.surface,
      border = androidx.compose.foundation.BorderStroke(
        AppDimens.BorderThin,
        MaterialTheme.colorScheme.outlineVariant
      ),
      tonalElevation = AppDimens.ElevationHigh
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(AppDimens.Spacing20),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          if (isRenaming) {
            OutlinedTextField(
              value = folderNameInput,
              onValueChange = { folderNameInput = it },
              singleLine = true,
              shape = ShapeRoundMd,
              textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.weight(1f).testTag("folder_rename_input"),
              trailingIcon = {
                IconButton(onClick = {
                  if (folderNameInput.isNotBlank()) {
                    onRenameFolder(folderNameInput)
                    isRenaming = false
                  }
                }) {
                  Text(
                    "Save",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                  )
                }
              }
            )
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(ShapeRoundSm)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Folder,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(AppDimens.IconSm)
                )
              }
              Spacer(modifier = Modifier.width(AppDimens.Spacing12))
              Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              IconButton(onClick = { isRenaming = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Rename folder",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
              IconButton(onClick = { showOptionsMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "Folder options",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = { showOptionsMenu = false }
              ) {
                DropdownMenuItem(
                  text = { Text("Rename Folder") },
                  leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                  onClick = {
                    showOptionsMenu = false
                    isRenaming = true
                  }
                )
                DropdownMenuItem(
                  text = { Text("Delete Folder", color = CrimsonNova) },
                  leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CrimsonNova) },
                  onClick = {
                    showOptionsMenu = false
                    onDeleteFolder()
                    onDismiss()
                  }
                )
              }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(AppDimens.Spacing16))

        if (folder.items.isEmpty()) {
          ModernEmptyState(
            icon = Icons.Default.Folder,
            title = "Empty Folder",
            description = "Drag and drop apps onto this folder on your Home screen to group them together."
          )
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16),
            contentPadding = PaddingValues(vertical = AppDimens.Spacing8)
          ) {
            items(folder.items, key = { it.id }) { item ->
              val key = "${item.packageName}/${item.componentName}"
              val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(ShapeRoundMd)
                  .combinedClickable(
                    onClick = {
                      if (app != null) {
                        onLaunchApp(app)
                        onDismiss()
                      }
                    },
                    onLongClick = {
                      selectedItemForAction = item
                    }
                  )
                  .padding(AppDimens.Spacing6)
              ) {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(ShapeRoundMd)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                  contentAlignment = Alignment.Center
                ) {
                  val bitmap = app?.let { getBitmap(it) }
                  if (bitmap != null) {
                    androidx.compose.foundation.Image(
                      bitmap = bitmap.asImageBitmap(),
                      contentDescription = app.label,
                      modifier = Modifier.fillMaxSize()
                    )
                  } else {
                    Text(
                      text = app?.label?.take(1) ?: item.packageName.take(1).uppercase(),
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 20.sp
                    )
                  }
                }

                Spacer(modifier = Modifier.height(AppDimens.Spacing6))
                Text(
                  text = app?.label ?: item.packageName.substringAfterLast('.'),
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(AppDimens.Spacing12))
        Text(
          text = "Tip: Long-press an app to remove it from this folder",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
      }
    }
  }

  // Action Dialog for selected folder item
  if (selectedItemForAction != null) {
    val targetItem = selectedItemForAction!!
    val key = "${targetItem.packageName}/${targetItem.componentName}"
    val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == targetItem.packageName }

    ModernDialogContainer(
      title = "Folder Item",
      subtitle = "Manage folder contents",
      icon = Icons.Default.DeleteOutline,
      iconTint = CrimsonNova,
      confirmButtonText = "Remove",
      confirmButtonColor = CrimsonNova,
      onConfirm = {
        onRemoveItem(targetItem)
        selectedItemForAction = null
      },
      onDismissRequest = { selectedItemForAction = null }
    ) {
      Text(
        text = "Remove '${app?.label ?: targetItem.packageName}' from '${folder.name}'?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
