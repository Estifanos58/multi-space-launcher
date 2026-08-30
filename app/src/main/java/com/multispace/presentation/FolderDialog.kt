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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
        .fillMaxWidth(0.92f)
        .wrapContentHeight()
        .clip(RoundedCornerShape(24.dp))
        .testTag("folder_dialog"),
      color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
      tonalElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
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
              textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.weight(1f).testTag("folder_rename_input"),
              trailingIcon = {
                IconButton(onClick = {
                  if (folderNameInput.isNotBlank()) {
                    onRenameFolder(folderNameInput)
                    isRenaming = false
                  }
                }) {
                  Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
              }
            )
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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
                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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

        Spacer(modifier = Modifier.height(16.dp))

        if (folder.items.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Empty folder",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
          ) {
            items(folder.items, key = { it.id }) { item ->
              val key = "${item.packageName}/${item.componentName}"
              val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
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
                  .padding(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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

                Spacer(modifier = Modifier.height(4.dp))
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

        Spacer(modifier = Modifier.height(12.dp))
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

    AlertDialog(
      onDismissRequest = { selectedItemForAction = null },
      title = { Text(app?.label ?: "App Item") },
      text = { Text("Remove '${app?.label ?: targetItem.packageName}' from '${folder.name}'?") },
      confirmButton = {
        Button(
          onClick = {
            onRemoveItem(targetItem)
            selectedItemForAction = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Remove from Folder")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedItemForAction = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
