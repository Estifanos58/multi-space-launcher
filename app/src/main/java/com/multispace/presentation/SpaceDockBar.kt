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
  onDropFromDockToDesktop: (dockItem: SpaceDockItem, app: DiscoveredApp, targetPage: Int, targetPos: Int) -> Unit = { _, _, _, _ -> },
  unifiedDragState: UnifiedDragState = remember { UnifiedDragState() },
  modifier: Modifier = Modifier,
  useLayer2: Boolean = true,
  appTheme: String = Space.THEME_DEFAULT
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

  val isReceivingDrop = unifiedDragState.isDragging &&
      unifiedDragState.dragSource == DragSource.LAYER1_DESKTOP &&
      unifiedDragState.currentTargetZone == DragTargetZone.DOCK_BAR

  val isDockAtCapacity = displayedDockItems.size >= maxAppSlots

  // Update target dock slot when an app is dragged from Layer 1
  LaunchedEffect(isReceivingDrop, unifiedDragState.rootPointerPos) {
    if (isReceivingDrop) {
      val coords = dockBarBoxCoordinates
      if (coords != null && coords.isAttached) {
        val dockLocal = coords.rootToLocal(unifiedDragState.rootPointerPos)
        val sorted = slotBounds.entries.sortedBy { it.value.center.x }
        if (sorted.isNotEmpty()) {
          val idx = sorted.indexOfFirst { dockLocal.x < it.value.right }
          unifiedDragState.targetDockIndex = if (idx == -1) sorted.size else idx
        } else {
          unifiedDragState.targetDockIndex = 0
        }
      }
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Dynamic Drop Target for Removing from Dock during dock item drag
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

    // Dynamic Visual Feedback when dragging an app from Layer 1 over the DockBar
    AnimatedVisibility(
      visible = isReceivingDrop,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
      Surface(
        shape = CircleShape,
        color = if (isDockAtCapacity) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
          1.dp,
          if (isDockAtCapacity) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .padding(bottom = AppDimens.Spacing6)
          .testTag("dock_drop_feedback_pill")
      ) {
        Text(
          text = if (isDockAtCapacity) "Dock Full: Will Swap App" else "Release to Add to Dock",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (isDockAtCapacity) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coordinates ->
          dockBarBoxCoordinates = coordinates
          unifiedDragState.dockCoordinates = coordinates
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

                val key = "${touchedItem.packageName}/${touchedItem.componentName}"
                val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == touchedItem.packageName }

                val rect = slotBounds[touchedItem.id]
                touchOffsetInSlot = if (rect != null) {
                  Offset(rootOffset.x - rect.left, rootOffset.y - rect.top)
                } else {
                  Offset(25.dp.toPx(), 25.dp.toPx())
                }

                unifiedDragState.isDragging = true
                unifiedDragState.dragSource = DragSource.DOCK_BAR
                unifiedDragState.draggedDockItem = touchedItem
                unifiedDragState.draggedApp = app
                unifiedDragState.touchOffsetInItem = touchOffsetInSlot
                unifiedDragState.rootPointerPos = dockBarBoxCoordinates?.localToRoot(rootOffset) ?: rootOffset
                unifiedDragState.currentTargetZone = DragTargetZone.DOCK_BAR

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
                val rootPos = dockBarBoxCoordinates?.localToRoot(change.position) ?: change.position
                unifiedDragState.rootPointerPos = rootPos

                val overRemovePill = removeZoneBounds?.contains(change.position) == true
                val isMovedUpwards = change.position.y < -15f || unifiedDragState.isPointerOverDesktop(rootPos)

                if (overRemovePill) {
                  isOverRemoveZone = true
                  unifiedDragState.currentTargetZone = DragTargetZone.REMOVE_BIN
                } else if (isMovedUpwards) {
                  isOverRemoveZone = false
                  unifiedDragState.currentTargetZone = DragTargetZone.DESKTOP
                } else {
                  isOverRemoveZone = false
                  unifiedDragState.currentTargetZone = DragTargetZone.DOCK_BAR

                  if (hasInitiatedDrag && previewItems.isNotEmpty()) {
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
              }
            },
            onDragEnd = {
              if (draggedItem != null) {
                val item = draggedItem!!
                val key = "${item.packageName}/${item.componentName}"
                val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }

                when (unifiedDragState.currentTargetZone) {
                  DragTargetZone.REMOVE_BIN -> {
                    onRemoveFromDock(item)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  }
                  DragTargetZone.DESKTOP -> {
                    if (app != null) {
                      val targetPage = unifiedDragState.targetDesktopPage
                      val targetPos = unifiedDragState.targetDesktopPosition.coerceAtLeast(0)
                      onDropFromDockToDesktop(item, app, targetPage, targetPos)
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                  }
                  DragTargetZone.DOCK_BAR, DragTargetZone.NONE -> {
                    if (hasInitiatedDrag) {
                      if (previewItems != displayedDockItems && previewItems.isNotEmpty()) {
                        onReorderDock(previewItems)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      }
                    } else {
                      // Held without dragging: show remove/action dialog
                      itemForAction = item
                    }
                  }
                }
              }
              isDragging = false
              draggedItem = null
              hasInitiatedDrag = false
              accumulatedDragDistance = 0f
              isOverRemoveZone = false
              unifiedDragState.reset()
            },
            onDragCancel = {
              isDragging = false
              draggedItem = null
              hasInitiatedDrag = false
              accumulatedDragDistance = 0f
              isOverRemoveZone = false
              unifiedDragState.reset()
            }
          )
        }
    ) {
      val dockBorder = if (isReceivingDrop) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
      } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      }

      ModernGlassCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing6)
          .testTag("space_dock_bar"),
        shape = ShapeRoundLg,
        border = dockBorder
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
              val isItemGhost = (isDragging && item.id == draggedItem?.id) ||
                  (unifiedDragState.isDragging && unifiedDragState.draggedDockItem?.id == item.id)
              DockAppSlot(
                item = item,
                allApps = allApps,
                appLookup = appLookup,
                isGhost = isItemGhost,
                getBitmap = getBitmap,
                onLaunchApp = onLaunchApp,
                onPositioned = { rect -> slotBounds[item.id] = rect },
                parentCoordinates = dockBarBoxCoordinates,
                appTheme = appTheme
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
              val isItemGhost = (isDragging && item.id == draggedItem?.id) ||
                  (unifiedDragState.isDragging && unifiedDragState.draggedDockItem?.id == item.id)
              DockAppSlot(
                item = item,
                allApps = allApps,
                appLookup = appLookup,
                isGhost = isItemGhost,
                getBitmap = getBitmap,
                onLaunchApp = onLaunchApp,
                onPositioned = { rect -> slotBounds[item.id] = rect },
                parentCoordinates = dockBarBoxCoordinates,
                appTheme = appTheme
              )
            }
          } else {
            // No drawer button in dock (Swipe-up access mode)
            itemsToRender.forEach { item ->
              val isItemGhost = (isDragging && item.id == draggedItem?.id) ||
                  (unifiedDragState.isDragging && unifiedDragState.draggedDockItem?.id == item.id)
              DockAppSlot(
                item = item,
                allApps = allApps,
                appLookup = appLookup,
                isGhost = isItemGhost,
                getBitmap = getBitmap,
                onLaunchApp = onLaunchApp,
                onPositioned = { rect -> slotBounds[item.id] = rect },
                parentCoordinates = dockBarBoxCoordinates,
                appTheme = appTheme
              )
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
  parentCoordinates: LayoutCoordinates? = null,
  modifier: Modifier = Modifier,
  appTheme: String = Space.THEME_DEFAULT
) {
  val key = "${item.packageName}/${item.componentName}"
  val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }

  Box(
    modifier = modifier
      .size(50.dp)
      .onGloballyPositioned { coords ->
        val parent = parentCoordinates
        if (parent != null && parent.isAttached && coords.isAttached) {
          val localOffset = parent.localPositionOf(coords, Offset.Zero)
          onPositioned(Rect(localOffset, coords.size.toSize()))
        } else {
          onPositioned(Rect(Offset.Zero, coords.size.toSize()))
        }
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
      ThemedAppIcon(
        app = app,
        bitmap = bitmap,
        appTheme = appTheme,
        modifier = Modifier.fillMaxSize(),
        fallbackText = app?.label?.take(1) ?: item.packageName.take(1).uppercase()
      )
    }
  }
}
