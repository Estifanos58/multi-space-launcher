package com.multispace.presentation.widget

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopWidgetView(
  placement: SpaceItemPlacement,
  space: Space,
  onRemove: (() -> Unit)? = null,
  appWidgetHost: AppWidgetHost? = null,
  isResizeMode: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  onResizeChange: ((newSpanX: Int, newSpanY: Int) -> Unit)? = null,
  onFinishResize: (() -> Unit)? = null,
  maxSpanX: Int = 4,
  maxSpanY: Int = 5,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Drag accumulation for edge handles
  var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
  var dragAccumulatorY by remember { mutableFloatStateOf(0f) }
  val dragThresholdPx = 45f

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(if (isResizeMode) 4.dp else 2.dp)
  ) {
    Card(
      shape = ShapeRoundLg,
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isResizeMode) 0.95f else 0.88f)
      ),
      border = if (isResizeMode) {
        androidx.compose.foundation.BorderStroke(2.5.dp, Color.White)
      } else {
        androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
      },
      modifier = Modifier
        .fillMaxSize()
        .then(
          if (isResizeMode) {
            Modifier.shadow(elevation = 16.dp, shape = ShapeRoundLg, spotColor = Color.White)
          } else {
            Modifier
          }
        )
        .then(
          if (isResizeMode) {
            Modifier.clickable { onFinishResize?.invoke() }
          } else if (onLongClick != null) {
            Modifier.combinedClickable(
              onClick = {},
              onLongClick = onLongClick
            )
          } else {
            Modifier
          }
        )
        .testTag("widget_card_${placement.id}")
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        when {
          placement.appWidgetId != -1 && appWidgetHost != null -> {
            AndroidSystemWidget(
              appWidgetId = placement.appWidgetId,
              appWidgetHost = appWidgetHost,
              modifier = Modifier.fillMaxSize()
            )
          }
          placement.customWidgetType == SpaceItemPlacement.WIDGET_QUICK_SEARCH -> {
            QuickSearchWidget(context = context)
          }
          placement.customWidgetType == SpaceItemPlacement.WIDGET_CALENDAR -> {
            CalendarWidget()
          }
          placement.customWidgetType == SpaceItemPlacement.WIDGET_BATTERY_STATUS -> {
            BatteryStatusWidget(context = context)
          }
          placement.customWidgetType == SpaceItemPlacement.WIDGET_QUICK_NOTES -> {
            QuickNotesWidget()
          }
          else -> {
            // Default Clock & Date widget
            ClockDateWidget()
          }
        }

        if (onRemove != null && !isResizeMode) {
          IconButton(
            onClick = onRemove,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(28.dp)
              .testTag("btn_remove_widget_${placement.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remove Widget",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // --- Interactive Resize Mode Overlay & Handles ---
    if (isResizeMode) {
      // White Handle Dot: Top Edge (Height resize)
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .size(26.dp)
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { dragAccumulatorY = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                dragAccumulatorY += dragAmount.y
                if (dragAccumulatorY <= -dragThresholdPx) {
                  // Dragged upward -> expand height
                  val next = (placement.spanY + 1).coerceAtMost(maxSpanY)
                  if (next != placement.spanY) onResizeChange?.invoke(placement.spanX, next)
                  dragAccumulatorY = 0f
                } else if (dragAccumulatorY >= dragThresholdPx) {
                  // Dragged downward -> shrink height
                  val next = (placement.spanY - 1).coerceAtLeast(1)
                  if (next != placement.spanY) onResizeChange?.invoke(placement.spanX, next)
                  dragAccumulatorY = 0f
                }
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFF1E293B), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2563EB)))
        }
      }

      // White Handle Dot: Bottom Edge (Height resize)
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .size(26.dp)
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { dragAccumulatorY = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                dragAccumulatorY += dragAmount.y
                if (dragAccumulatorY >= dragThresholdPx) {
                  // Dragged downward -> expand height
                  val next = (placement.spanY + 1).coerceAtMost(maxSpanY)
                  if (next != placement.spanY) onResizeChange?.invoke(placement.spanX, next)
                  dragAccumulatorY = 0f
                } else if (dragAccumulatorY <= -dragThresholdPx) {
                  // Dragged upward -> shrink height
                  val next = (placement.spanY - 1).coerceAtLeast(1)
                  if (next != placement.spanY) onResizeChange?.invoke(placement.spanX, next)
                  dragAccumulatorY = 0f
                }
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFF1E293B), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2563EB)))
        }
      }

      // White Handle Dot: Left Edge (Width resize)
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .size(26.dp)
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { dragAccumulatorX = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                dragAccumulatorX += dragAmount.x
                if (dragAccumulatorX <= -dragThresholdPx) {
                  // Dragged left -> expand width
                  val next = (placement.spanX + 1).coerceAtMost(maxSpanX)
                  if (next != placement.spanX) onResizeChange?.invoke(next, placement.spanY)
                  dragAccumulatorX = 0f
                } else if (dragAccumulatorX >= dragThresholdPx) {
                  // Dragged right -> shrink width
                  val next = (placement.spanX - 1).coerceAtLeast(1)
                  if (next != placement.spanX) onResizeChange?.invoke(next, placement.spanY)
                  dragAccumulatorX = 0f
                }
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFF1E293B), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2563EB)))
        }
      }

      // White Handle Dot: Right Edge (Width resize)
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .size(26.dp)
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { dragAccumulatorX = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                dragAccumulatorX += dragAmount.x
                if (dragAccumulatorX >= dragThresholdPx) {
                  // Dragged right -> expand width
                  val next = (placement.spanX + 1).coerceAtMost(maxSpanX)
                  if (next != placement.spanX) onResizeChange?.invoke(next, placement.spanY)
                  dragAccumulatorX = 0f
                } else if (dragAccumulatorX <= -dragThresholdPx) {
                  // Dragged left -> shrink width
                  val next = (placement.spanX - 1).coerceAtLeast(1)
                  if (next != placement.spanX) onResizeChange?.invoke(next, placement.spanY)
                  dragAccumulatorX = 0f
                }
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFF1E293B), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2563EB)))
        }
      }

      // Floating Interactive Resize HUD / Toolbar
      Surface(
        shape = ShapeRoundMd,
        color = Color(0xFF0F172A).copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 6.dp)
          .zIndex(100f)
          .shadow(8.dp, ShapeRoundMd)
          .testTag("widget_resize_hud_${placement.id}")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Current Size Badge
          Text(
            text = "${placement.spanX} × ${placement.spanY}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp)
          )

          // Width Stepper
          IconButton(
            onClick = {
              val next = (placement.spanX - 1).coerceAtLeast(1)
              if (next != placement.spanX) onResizeChange?.invoke(next, placement.spanY)
            },
            modifier = Modifier.size(24.dp).testTag("btn_resize_dec_width")
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "Decrease Width",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
          }

          Text(
            text = "W",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF94A3B8)
          )

          IconButton(
            onClick = {
              val next = (placement.spanX + 1).coerceAtMost(maxSpanX)
              if (next != placement.spanX) onResizeChange?.invoke(next, placement.spanY)
            },
            modifier = Modifier.size(24.dp).testTag("btn_resize_inc_width")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Increase Width",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
          }

          // Height Stepper
          IconButton(
            onClick = {
              val next = (placement.spanY - 1).coerceAtLeast(1)
              if (next != placement.spanY) onResizeChange?.invoke(placement.spanX, next)
            },
            modifier = Modifier.size(24.dp).testTag("btn_resize_dec_height")
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "Decrease Height",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
          }

          Text(
            text = "H",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF94A3B8)
          )

          IconButton(
            onClick = {
              val next = (placement.spanY + 1).coerceAtMost(maxSpanY)
              if (next != placement.spanY) onResizeChange?.invoke(placement.spanX, next)
            },
            modifier = Modifier.size(24.dp).testTag("btn_resize_inc_height")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Increase Height",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
          }

          // Done confirmation button
          IconButton(
            onClick = { onFinishResize?.invoke() },
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(Color(0xFF22C55E))
              .testTag("btn_resize_done_${placement.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Done Resizing",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ClockDateWidget(modifier: Modifier = Modifier) {
  var currentTime by remember { mutableStateOf("") }
  var currentDate by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    while (true) {
      val now = Date()
      currentTime = timeFormat.format(now)
      currentDate = dateFormat.format(now)
      delay(1000L)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
      Icon(
        imageVector = Icons.Default.Schedule,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
      )
      Text(
        text = currentTime.ifEmpty { "12:00 PM" },
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp
      )
    }
    Spacer(modifier = Modifier.height(AppDimens.Spacing4))
    Text(
      text = currentDate.ifEmpty { "Today" },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun QuickSearchWidget(
  context: Context,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .clickable {
          try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
              putExtra(SearchManager.QUERY, "")
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
          } catch (e: Exception) {
            // Fallback
          }
        }
        .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing10)
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "Search web or apps...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Voice Search",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
fun CalendarWidget(modifier: Modifier = Modifier) {
  val cal = remember { Calendar.getInstance() }
  val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
  val dayOfWeekName = remember { SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time) }
  val monthYear = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }

  Row(
    modifier = modifier
      .fillMaxSize()
      .padding(AppDimens.Spacing16),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
  ) {
    Box(
      modifier = Modifier
        .size(60.dp)
        .clip(ShapeRoundMd)
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.primary,
              MaterialTheme.colorScheme.primaryContainer
            )
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time).uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "$dayOfMonth",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onPrimary,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 22.sp
        )
      }
    }

    Column(verticalArrangement = Arrangement.Center) {
      Text(
        text = dayOfWeekName,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(AppDimens.Spacing2))
      Text(
        text = monthYear,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun BatteryStatusWidget(
  context: Context,
  modifier: Modifier = Modifier
) {
  var batteryPct by remember { mutableIntStateOf(100) }
  var isCharging by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    try {
      val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryStatus = context.registerReceiver(null, ifilter)
      val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
      if (level >= 0 && scale > 0) {
        batteryPct = ((level / scale.toFloat()) * 100).toInt()
      }
      val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
      isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } catch (e: Exception) {
      // Default to 100%
    }
  }

  Row(
    modifier = modifier
      .fillMaxSize()
      .padding(AppDimens.Spacing16),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(
          if (isCharging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
        contentDescription = "Battery",
        tint = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(28.dp)
      )
    }

    Column(verticalArrangement = Arrangement.Center) {
      Text(
        text = "$batteryPct% Battery",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(AppDimens.Spacing2))
      Text(
        text = if (isCharging) "Charging" else "On Battery",
        style = MaterialTheme.typography.bodySmall,
        color = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun QuickNotesWidget(modifier: Modifier = Modifier) {
  var noteText by remember { mutableStateOf("Tap to add a quick reminder or note...") }
  var isEditing by remember { mutableStateOf(false) }
  var tempText by remember { mutableStateOf(noteText) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(AppDimens.Spacing16),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing6)
      ) {
        Icon(
          imageVector = Icons.Default.Notes,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "Quick Notes",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
      }
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit note",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .size(16.dp)
          .clickable { isEditing = !isEditing }
      )
    }

    if (isEditing) {
      OutlinedTextField(
        value = tempText,
        onValueChange = { tempText = it },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3,
        textStyle = MaterialTheme.typography.bodySmall
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        IconButton(onClick = {
          noteText = tempText
          isEditing = false
        }) {
          Text("Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
      }
    } else {
      Text(
        text = noteText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isEditing = true }
          .padding(vertical = AppDimens.Spacing8)
      )
    }
  }
}

@Composable
fun AndroidSystemWidget(
  appWidgetId: Int,
  appWidgetHost: AppWidgetHost,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
  val appWidgetInfo = remember(appWidgetId) {
    try {
      appWidgetManager.getAppWidgetInfo(appWidgetId)
    } catch (e: Exception) {
      null
    }
  }

  if (appWidgetInfo != null) {
    AndroidView(
      factory = { ctx ->
        try {
          appWidgetHost.createView(ctx, appWidgetId, appWidgetInfo).apply {
            setAppWidget(appWidgetId, appWidgetInfo)
          }
        } catch (e: Exception) {
          android.widget.TextView(ctx).apply {
            text = "Widget unavailable"
            gravity = android.view.Gravity.CENTER
          }
        }
      },
      modifier = modifier
    )
  } else {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text(
        text = "Widget #$appWidgetId",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
