package com.multispace.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import com.multispace.ui.components.ModernDialogContainer
import com.multispace.ui.components.ModernGlassCard
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.CrimsonNova
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import kotlin.math.roundToInt

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
  onReorderDock: (List<SpaceDockItem>) -> Unit = {},
  modifier: Modifier = Modifier,
  useLayer2: Boolean = true
) {
  val deduplicatedDockItems = remember(dockItems) {
    dockItems.distinctBy { it.packageName }
  }
  val isCenterDrawerButton = useLayer2 && accessMode == Space.ACCESS_MODE_DOCK_BUTTON
  val maxAppSlots = if (isCenterDrawerButton) (capacity - 1).coerceAtLeast(1) else capacity
  val displayedDockItems = remember(deduplicatedDockItems, maxAppSlots) {
    deduplicatedDockItems.take(maxAppSlots)
  }

  var draggedItem by remember { mutableStateOf<SpaceDockItem?>(null) }
  var isDragging by remember { mutableStateOf(false) }
  var hasInitiatedDrag by remember { mutableStateOf(false) }
  var currentPointerPos by remember { mutableStateOf(Offset.Zero) }
  var touchOffsetInSlot by remember { mutableStateOf(Offset.Zero) }
  var previewItems by remember { mutableStateOf<List<SpaceDockItem>>(emptyList()) }
  var accumulatedDragDistance by remember { mutableFloatStateOf(0f) }
  var isOverRemoveZone by remember { mutableStateOf(false) }
  var itemForAction by remember { mutableStateOf<SpaceDockItem?>(null) }

  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val dragSlopPx = with(density) { 8.dp.toPx() }

  var dockBarBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var removeZoneBounds by remember { mutableStateOf<Rect?>(null) }
  val slotBounds = remember { mutableStateMapOf<String, Rect>() }

  val appLookup = remember(allApps) {
    allApps.associateBy { "${it.packageName}/${it.activityName}" }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Dynamic Drop Target for Removing from Dock during drag
    AnimatedVisibility(
      visible = isDragging && draggedItem != null,
      enter = fadeIn() + slideInVertically { -it / 2 },
      exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
      Surface(
        shape = CircleShape,
        color = if (isOverRemoveZone) CrimsonNova else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
          1.dp,
          if (isOverRemoveZone) CrimsonNova else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
          .padding(bottom = AppDimens.Spacing6)
          .onGloballyPositioned { coords ->
            dockBarBoxCoordinates?.let { root ->
              if (coords.isAttached && root.isAttached) {
                val localOffset = root.localPositionOf(coords, Offset.Zero)
                removeZoneBounds = Rect(localOffset, coords.size.toSize())
              }
            }
          }
          .testTag("dock_remove_drop_target")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Remove from Dock",
            tint = if (isOverRemoveZone) Color.White else CrimsonNova,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = if (isOverRemoveZone) "Release to Remove" else "Drag here to Remove",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isOverRemoveZone) Color.White else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coordinates ->
          dockBarBoxCoordinates = coordinates
        }
        .pointerInput(displayedDockItems) {
          detectDragGesturesAfterLongPress(
            onDragStart = { rootOffset ->
              val touchedItem = displayedDockItems.firstOrNull { item ->
                slotBounds[item.id]?.contains(rootOffset) == true
              }
              if (touchedItem != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                draggedItem = touchedItem
                isDragging = true
                hasInitiatedDrag = false
                accumulatedDragDistance = 0f
                currentPointerPos = rootOffset
                val rect = slotBounds[touchedItem.id]
                touchOffsetInSlot = if (rect != null) {
                  Offset(rootOffset.x - rect.left, rootOffset.y - rect.top)
                } else {
                  Offset(25.dp.toPx(), 25.dp.toPx())
                }
                previewItems = displayedDockItems.toMutableList()
              }
            },
            onDrag = { change, dragAmount ->
              if (draggedItem != null) {
                change.consume()
                accumulatedDragDistance += dragAmount.getDistance()
                if (accumulatedDragDistance >= dragSlopPx) {
                  hasInitiatedDrag = true
                }
                currentPointerPos = change.position

                // Check if hovering over remove zone or dragged upward past dock
                val isAboveDock = change.position.y < -5f
                val overZone = removeZoneBounds?.contains(change.position) == true || isAboveDock
                isOverRemoveZone = overZone

                if (!overZone && hasInitiatedDrag && previewItems.isNotEmpty()) {
                  val curItem = draggedItem!!
                  val sortedSlots = slotBounds.entries
                    .filter { entry -> previewItems.any { itm -> itm.id == entry.key } }
                    .sortedBy { it.value.center.x }

                  if (sortedSlots.isNotEmpty()) {
                    val targetIndex = sortedSlots.indexOfFirst { change.position.x < it.value.right }
                      .let { if (it == -1) sortedSlots.size - 1 else it }
                      .coerceIn(0, previewItems.size - 1)

                    val curIndex = previewItems.indexOfFirst { it.id == curItem.id }
                    if (curIndex != -1 && targetIndex != curIndex) {
                      val updated = previewItems.toMutableList()
                      val moved = updated.removeAt(curIndex)
                      updated.add(targetIndex, moved)
                      previewItems = updated
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                  }
                }
              }
            },
            onDragEnd = {
              if (draggedItem != null) {
                val item = draggedItem!!
                if (isOverRemoveZone) {
                  onRemoveFromDock(item)
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else if (hasInitiatedDrag) {
                  if (previewItems != displayedDockItems && previewItems.isNotEmpty()) {
                    onReorderDock(previewItems)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  }
                } else {
                  // Held without dragging: show remove/action dialog
                  itemForAction = item
                }
              }
              isDragging = false
              draggedItem = null
              hasInitiatedDrag = false
              accumulatedDragDistance = 0f
              isOverRemoveZone = false
            },
            onDragCancel = {
              isDragging = false
              draggedItem = null
              hasInitiatedDrag = false
              accumulatedDragDistance = 0f
              isOverRemoveZone = false
            }
          )
        }
    ) {
      ModernGlassCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing6)
          .testTag("space_dock_bar"),
        shape = ShapeRoundLg
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val itemsToRender = if (isDragging) previewItems else displayedDockItems

          if (isCenterDrawerButton) {
            val splitIndex = itemsToRender.size / 2
            val leftItems = itemsToRender.take(splitIndex)
            val rightItems = itemsToRender.drop(splitIndex)

            // Left apps
            leftItems.forEach { item ->
              DockAppSlot(
                item = item,
                allApps = allApps,
                appLookup = appLookup,
                isGhost = isDragging && item.id == draggedItem?.id,
                getBitmap = getBitmap,
                onLaunchApp = onLaunchApp,
                onPositioned = { rect -> slotBounds[item.id] = rect }
              )
            }

            // Center Futuristic All-Apps Drawer Button
            IconButton(
              onClick = onOpenLayer2,
              modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                  Brush.radialGradient(
                    colors = listOf(
                      QuantumViolet,
                      MaterialTheme.colorScheme.primaryContainer
                    )
                  )
                )
                .border(AppDimens.BorderThin, QuantumViolet.copy(alpha = 0.5f), CircleShape)
                .testTag("dock_layer2_drawer_button")
            ) {
              Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "All Apps Library",
                tint = Color.White,
                modifier = Modifier.size(AppDimens.IconMd)
              )
            }

            // Right apps
            rightItems.forEach { item ->
              DockAppSlot(
                item = item,
                allApps = allApps,
                appLookup = appLookup,
                isGhost = isDragging && item.id == draggedItem?.id,
                getBitmap = getBitmap,
                onLaunchApp = onLaunchApp,
                onPositioned = { rect -> slotBounds[item.id] = rect }
              )
            }
          } else {
            // No drawer button in dock (Swipe-up access mode)
            itemsToRender.forEach { item ->
              DockAppSlot(
                item = item,
                allApps = allApps,
                appLookup = appLookup,
                isGhost = isDragging && item.id == draggedItem?.id,
                getBitmap = getBitmap,
                onLaunchApp = onLaunchApp,
                onPositioned = { rect -> slotBounds[item.id] = rect }
              )
            }
          }
        }
      }

      // Floating dragged icon preview following user touch
      if (isDragging && draggedItem != null) {
        val item = draggedItem!!
        val key = "${item.packageName}/${item.componentName}"
        val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }
        val bitmap = app?.let { getBitmap(it) }

        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                (currentPointerPos.x - touchOffsetInSlot.x).roundToInt(),
                (currentPointerPos.y - touchOffsetInSlot.y).roundToInt()
              )
            }
            .size(52.dp)
            .graphicsLayer {
              scaleX = 1.15f
              scaleY = 1.15f
              shadowElevation = 16.dp.toPx()
            }
            .zIndex(999f)
            .testTag("dock_floating_dragged_item"),
          contentAlignment = Alignment.Center
        ) {
          if (bitmap != null) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = app?.label,
              modifier = Modifier
                .fillMaxSize()
                .clip(ShapeRoundMd)
            )
          } else {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = ShapeRoundMd,
              border = BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.fillMaxSize()
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = app?.label?.take(1) ?: item.packageName.take(1).uppercase(),
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  fontSize = 20.sp
                )
              }
            }
          }
        }
      }
    }
  }

  // Modernized Remove from Dock Confirmation Dialog
  if (itemForAction != null) {
    val target = itemForAction!!
    val key = "${target.packageName}/${target.componentName}"
    val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == target.packageName }

    ModernDialogContainer(
      title = "Dock Shortcut",
      subtitle = "Manage persistent dock placement",
      icon = Icons.Default.DeleteOutline,
      iconTint = CrimsonNova,
      confirmButtonText = "Remove",
      confirmButtonColor = CrimsonNova,
      onConfirm = {
        onRemoveFromDock(target)
        itemForAction = null
      },
      onDismissRequest = { itemForAction = null }
    ) {
      Text(
        text = "Remove '${app?.label ?: target.packageName}' from this Space's dock?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun DockAppSlot(
  item: SpaceDockItem,
  allApps: List<DiscoveredApp>,
  appLookup: Map<String, DiscoveredApp>,
  isGhost: Boolean,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onPositioned: (Rect) -> Unit,
  modifier: Modifier = Modifier
) {
  val key = "${item.packageName}/${item.componentName}"
  val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }

  Box(
    modifier = modifier
      .size(50.dp)
      .onGloballyPositioned { coords ->
        onPositioned(Rect(Offset.Zero, coords.size.toSize()))
      }
      .clip(ShapeRoundMd)
      .then(
        if (!isGhost) {
          Modifier.clickable {
            if (app != null) onLaunchApp(app)
          }
        } else {
          Modifier
        }
      )
      .testTag("dock_item_${item.packageName}"),
    contentAlignment = Alignment.Center
  ) {
    if (isGhost) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(ShapeRoundMd)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
          .border(
            width = 1.5.dp,
            color = QuantumViolet.copy(alpha = 0.60f),
            shape = ShapeRoundMd
          ),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(QuantumViolet.copy(alpha = 0.5f))
        )
      }
    } else {
      val bitmap = app?.let { getBitmap(it) }
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = app?.label,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = ShapeRoundMd,
          border = BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
          ),
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
}
