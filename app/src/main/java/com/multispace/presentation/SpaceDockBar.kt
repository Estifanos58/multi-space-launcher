package com.multispace.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
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
import com.multispace.domain.model.SpaceDockItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpaceDockBar(
  dockItems: List<SpaceDockItem>,
  allApps: List<DiscoveredApp>,
  capacity: Int,
  accessMode: String,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onOpenLayer2: () -> Unit,
  onRemoveFromDock: (SpaceDockItem) -> Unit,
  modifier: Modifier = Modifier,
  useLayer2: Boolean = true
) {
  var itemForAction by remember { mutableStateOf<SpaceDockItem?>(null) }
  val appLookup = remember(allApps) {
    allApps.associateBy { "${it.packageName}/${it.activityName}" }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(28.dp))
      .testTag("space_dock_bar"),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    tonalElevation = 6.dp,
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val isCenterDrawerButton = useLayer2 && accessMode == Space.ACCESS_MODE_DOCK_BUTTON
      val maxAppSlots = if (isCenterDrawerButton) (capacity - 1).coerceAtLeast(1) else capacity
      val displayedDockItems = dockItems.take(maxAppSlots)

      if (isCenterDrawerButton) {
        val splitIndex = displayedDockItems.size / 2
        val leftItems = displayedDockItems.take(splitIndex)
        val rightItems = displayedDockItems.drop(splitIndex)

        // Left apps
        leftItems.forEach { item ->
          DockAppSlot(
            item = item,
            allApps = allApps,
            appLookup = appLookup,
            getBitmap = getBitmap,
            onLaunchApp = onLaunchApp,
            onLongPress = { itemForAction = item }
          )
        }

        // Center All-Apps Drawer Button
        IconButton(
          onClick = onOpenLayer2,
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .testTag("dock_layer2_drawer_button")
        ) {
          Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = "All Apps Library",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(26.dp)
          )
        }

        // Right apps
        rightItems.forEach { item ->
          DockAppSlot(
            item = item,
            allApps = allApps,
            appLookup = appLookup,
            getBitmap = getBitmap,
            onLaunchApp = onLaunchApp,
            onLongPress = { itemForAction = item }
          )
        }
      } else {
        // No drawer button in dock (Swipe-up access mode)
        displayedDockItems.forEach { item ->
          DockAppSlot(
            item = item,
            allApps = allApps,
            appLookup = appLookup,
            getBitmap = getBitmap,
            onLaunchApp = onLaunchApp,
            onLongPress = { itemForAction = item }
          )
        }
      }
    }
  }

  // Remove from Dock Confirmation Dialog
  if (itemForAction != null) {
    val target = itemForAction!!
    val key = "${target.packageName}/${target.componentName}"
    val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == target.packageName }

    AlertDialog(
      onDismissRequest = { itemForAction = null },
      title = { Text("Dock Item") },
      text = { Text("Remove '${app?.label ?: target.packageName}' from Dock?") },
      confirmButton = {
        Button(
          onClick = {
            onRemoveFromDock(target)
            itemForAction = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Remove from Dock")
        }
      },
      dismissButton = {
        TextButton(onClick = { itemForAction = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppSlot(
  item: SpaceDockItem,
  allApps: List<DiscoveredApp>,
  appLookup: Map<String, DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onLongPress: () -> Unit
) {
  val key = "${item.packageName}/${item.componentName}"
  val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }

  Box(
    modifier = Modifier
      .size(52.dp)
      .clip(RoundedCornerShape(14.dp))
      .combinedClickable(
        onClick = { if (app != null) onLaunchApp(app) },
        onLongClick = onLongPress
      )
      .testTag("dock_item_${item.packageName}"),
    contentAlignment = Alignment.Center
  ) {
    val bitmap = app?.let { getBitmap(it) }
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = app.label,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = app?.label?.take(1) ?: item.packageName.take(1).uppercase(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 18.sp
          )
        }
      }
    }
  }
}
