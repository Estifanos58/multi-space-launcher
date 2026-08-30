package com.multispace.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Layer2LibraryScreen(
  space: Space,
  spaceApps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onAddToHome: (DiscoveredApp) -> Unit,
  onAddToDock: (DiscoveredApp) -> Unit,
  onAppInfo: (DiscoveredApp) -> Unit,
  onCloseLayer2: () -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedAppForMenu by remember { mutableStateOf<DiscoveredApp?>(null) }

  val filteredApps = remember(spaceApps, searchQuery) {
    if (searchQuery.isBlank()) {
      spaceApps
    } else {
      val q = searchQuery.trim().lowercase()
      spaceApps.filter {
        it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
      }
    }
  }

  val iconSizeModifier = when (space.iconSize) {
    Space.ICON_SIZE_SMALL -> Modifier.size(44.dp)
    Space.ICON_SIZE_LARGE -> Modifier.size(62.dp)
    else -> Modifier.size(54.dp)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
      .padding(top = 8.dp)
      .testTag("layer2_library_screen")
  ) {
    // Top Bar with Back Arrow and Search Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onCloseLayer2, modifier = Modifier.size(40.dp).testTag("layer2_back_button")) {
        Icon(
          imageVector = Icons.Default.ArrowBack,
          contentDescription = "Back to Home",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search Space apps...", fontSize = 14.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("layer2_search_input")
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Apps Grid
    if (filteredApps.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = if (searchQuery.isBlank()) "No apps in this Space" else "No matching apps found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { searchQuery = "" }) {
              Text("Clear Search")
            }
          }
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(space.gridColumns),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .testTag("layer2_apps_grid"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(filteredApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { selectedAppForMenu = app }
              )
              .padding(4.dp)
              .testTag("layer2_app_${app.packageName}")
          ) {
            Box(
              modifier = iconSizeModifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              val bitmap = getBitmap(app)
              if (bitmap != null) {
                Image(
                  bitmap = bitmap.asImageBitmap(),
                  contentDescription = app.label,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Text(
                  text = app.label.take(1).uppercase(),
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 18.sp
                )
              }
            }

            if (space.labelVisibility) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = app.label,
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
    }
  }

  // App Action Menu Bottom Sheet / Dialog
  if (selectedAppForMenu != null) {
    val app = selectedAppForMenu!!
    AlertDialog(
      onDismissRequest = { selectedAppForMenu = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          val bitmap = getBitmap(app)
          if (bitmap != null) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = app.label,
              modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
          }
          Text(text = app.label, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = app.packageName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))

          TextButton(
            onClick = {
              onLaunchApp(app)
              selectedAppForMenu = null
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Launch App")
            }
          }

          TextButton(
            onClick = {
              onAddToHome(app)
              selectedAppForMenu = null
            },
            modifier = Modifier.fillMaxWidth().testTag("menu_add_to_home")
          ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Home, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Add to Home Screen")
            }
          }

          TextButton(
            onClick = {
              onAddToDock(app)
              selectedAppForMenu = null
            },
            modifier = Modifier.fillMaxWidth().testTag("menu_add_to_dock")
          ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Dock, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Add to Dock")
            }
          }

          TextButton(
            onClick = {
              onAppInfo(app)
              selectedAppForMenu = null
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("App Information")
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { selectedAppForMenu = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
