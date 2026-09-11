package com.multispace.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.LayoutPreset
import com.multispace.domain.model.PresetStrategy
import com.multispace.domain.model.Space
import com.multispace.ui.theme.PrimaryPurple

/**
 * Graphical Smartphone Picture Preview for a Layout Preset.
 * Renders an authentic miniature phone screen illustrating the layout paradigm visually.
 */
@Composable
fun LayoutPresetPhonePreview(
  preset: LayoutPreset,
  modifier: Modifier = Modifier,
  phoneWidth: Dp = 140.dp,
  phoneHeight: Dp = 240.dp,
  isSelected: Boolean = false,
  gridColumns: Int = preset.gridColumns
) {
  val borderColor by animateColorAsState(
    if (isSelected) PrimaryPurple else Color(0xFF475569).copy(alpha = 0.5f),
    label = "phone_border_color"
  )
  val elevation by animateDpAsState(
    if (isSelected) 8.dp else 2.dp,
    label = "phone_elevation"
  )

  Surface(
    modifier = modifier
      .width(phoneWidth)
      .height(phoneHeight)
      .shadow(elevation, RoundedCornerShape(22.dp))
      .clip(RoundedCornerShape(22.dp))
      .border(
        BorderStroke(if (isSelected) 2.5.dp else 1.5.dp, borderColor),
        RoundedCornerShape(22.dp)
      ),
    color = Color(0xFF0F172A)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Screen Wallpaper & Content Canvas
      PresetScreenCanvas(preset = preset, columns = gridColumns)

      // Top Notch & Status Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "10:08",
          fontSize = 8.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White.copy(alpha = 0.9f)
        )
        // Camera Hole-punch / Speaker slit
        Box(
          modifier = Modifier
            .width(18.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black.copy(alpha = 0.75f))
        )
        // Battery / Wifi dots
        Row(
          horizontalArrangement = Arrangement.spacedBy(2.5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
          Box(modifier = Modifier.width(7.dp).height(3.5.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = 0.8f)))
        }
      }

      // Checkmark overlay when selected
      if (isSelected) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(PrimaryPurple),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PresetScreenCanvas(preset: LayoutPreset, columns: Int) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    when (preset.strategy) {
      PresetStrategy.GALAXY_CURATED -> drawGalaxyCuratedPreset(w, h, columns)
      PresetStrategy.PIXEL_GLANCEABLE -> drawPixelPreset(w, h, columns)
      PresetStrategy.CLASSIC_GRID -> drawClassicAndroidPreset(w, h, columns)
      PresetStrategy.MINIMAL_SPARSE -> drawMinimalPreset(w, h, columns)
      PresetStrategy.PRODUCTIVITY_DASHBOARD -> drawProductivityPreset(w, h, columns)
      PresetStrategy.COMPACT_DENSITY -> drawCompactPreset(w, h, columns)
    }
  }
}

// -------------------------------------------------------------
// 1. Galaxy / Modern Curated (Default)
// -------------------------------------------------------------
private fun DrawScope.drawGalaxyCuratedPreset(w: Float, h: Float, columns: Int) {
  // Rich Indigo-to-Violet Twilight Gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF1E1B4B), Color(0xFF2E1065), Color(0xFF4C1D95))
    )
  )

  val marginX = w * 0.08f
  val contentW = w - (marginX * 2)

  // 1. Top Prominent Weather + Clock Widget (Rows 0-1)
  val weatherTop = h * 0.12f
  val weatherH = h * 0.18f
  drawMiniWeatherClockWidget(
    x = marginX,
    y = weatherTop,
    w = contentW,
    h = weatherH,
    prominent = true
  )

  // 2. Middle Search Bar Widget (Row 2)
  val searchTop = weatherTop + weatherH + h * 0.025f
  val searchH = h * 0.065f
  drawMiniSearchBarWidget(
    x = marginX,
    y = searchTop,
    w = contentW,
    h = searchH
  )

  // 3. Lower Area: Curated Apps (Row 3) - Only 4-5 curated apps!
  val appsTop = searchTop + searchH + h * 0.05f
  val curatedAppCount = when (columns) {
    3 -> 3
    4 -> 4
    5 -> 5
    else -> 5
  }

  val iconSize = (contentW / columns) * 0.65f
  val slotW = contentW / curatedAppCount

  val curatedColors = listOf(
    Color(0xFF38BDF8), // Phone / Light Blue
    Color(0xFF34D399), // Messages / Mint Green
    Color(0xFFFB923C), // Browser / Orange
    Color(0xFFA78BFA), // Camera / Purple
    Color(0xFFF472B6)  // Gallery / Pink
  )

  for (i in 0 until curatedAppCount) {
    val cx = marginX + i * slotW + (slotW / 2)
    val cy = appsTop + (iconSize / 2)
    val color = curatedColors[i % curatedColors.size]
    drawMiniAppIcon(
      cx = cx,
      cy = cy,
      size = iconSize,
      color = color,
      isSquircle = true,
      hasLabel = true
    )
  }

  // Row 4 is left empty for generous negative space and visual hierarchy!

  // Subtle Page Indicator Dots
  val dotsY = h * 0.77f
  drawCircle(Color.White, radius = 2.5f, center = Offset(w * 0.44f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.35f), radius = 2f, center = Offset(w * 0.50f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.35f), radius = 2f, center = Offset(w * 0.56f, dotsY))

  // 4. Persistent Dock (5 Slots)
  drawMiniDock(
    w = w,
    h = h,
    capacity = 5,
    accessMode = Space.ACCESS_MODE_SWIPE_UP
  )
}

// -------------------------------------------------------------
// 2. Pixel / Google-inspired
// -------------------------------------------------------------
private fun DrawScope.drawPixelPreset(w: Float, h: Float, columns: Int) {
  // Emerald / Earthy Dynamic Gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF047857))
    )
  )

  val marginX = w * 0.08f
  val contentW = w - (marginX * 2)

  // 1. Top "At a Glance" Date + Weather Widget (Row 0)
  val glanceTop = h * 0.11f
  val glanceH = h * 0.09f
  drawMiniAtAGlanceWidget(
    x = marginX,
    y = glanceTop,
    w = contentW,
    h = glanceH
  )

  // 2. Middle: Clean 2-Row App Grid (Rows 1 & 2)
  val gridCols = columns.coerceIn(4, 5)
  val gridTop = glanceTop + glanceH + h * 0.035f
  val slotW = contentW / gridCols
  val iconSize = slotW * 0.65f
  val rowGap = h * 0.045f

  val appColors = listOf(
    Color(0xFF60A5FA), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFFF87171), Color(0xFFA78BFA),
    Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFFB923C), Color(0xFFEC4899), Color(0xFF818CF8)
  )

  var idx = 0
  for (r in 0 until 2) {
    for (c in 0 until gridCols) {
      val cx = marginX + c * slotW + (slotW / 2)
      val cy = gridTop + r * (iconSize + rowGap) + (iconSize / 2)
      drawMiniAppIcon(
        cx = cx,
        cy = cy,
        size = iconSize,
        color = appColors[idx % appColors.size],
        isSquircle = false, // Circular Pixel style
        hasLabel = true
      )
      idx++
    }
  }

  // 3. Lower: Pixel Quick Search Bar directly above dock (Row 3)
  val searchH = h * 0.06f
  val searchTop = h * 0.75f
  drawMiniSearchBarWidget(
    x = marginX,
    y = searchTop,
    w = contentW,
    h = searchH,
    isPixelStyle = true
  )

  // 4. Pixel Dock (5 Slots)
  drawMiniDock(
    w = w,
    h = h,
    capacity = 5,
    accessMode = Space.ACCESS_MODE_SWIPE_UP,
    isPixelStyle = true
  )
}

// -------------------------------------------------------------
// 3. Classic Android
// -------------------------------------------------------------
private fun DrawScope.drawClassicAndroidPreset(w: Float, h: Float, columns: Int) {
  // Classic Dark Slate Gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    )
  )

  val marginX = w * 0.08f
  val contentW = w - (marginX * 2)

  // 1. Digital Clock Widget (Row 0)
  val clockTop = h * 0.11f
  val clockH = h * 0.08f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.12f),
    topLeft = Offset(marginX, clockTop),
    size = Size(contentW, clockH),
    cornerRadius = CornerRadius(8f, 8f)
  )
  // Large digital clock bars
  drawRoundRect(
    color = Color.White.copy(alpha = 0.9f),
    topLeft = Offset(marginX + contentW * 0.25f, clockTop + clockH * 0.28f),
    size = Size(contentW * 0.5f, 7f),
    cornerRadius = CornerRadius(3.5f, 3.5f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.55f),
    topLeft = Offset(marginX + contentW * 0.35f, clockTop + clockH * 0.62f),
    size = Size(contentW * 0.3f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 2. Full 4x3 Conventional App Grid (Rows 1-3)
  val gridCols = columns.coerceIn(3, 4)
  val gridRows = 3
  val gridTop = clockTop + clockH + h * 0.035f
  val slotW = contentW / gridCols
  val iconSize = slotW * 0.62f
  val rowGap = h * 0.04f

  val appColors = listOf(
    Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
    Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFF84CC16), Color(0xFFF97316),
    Color(0xFF6366F1), Color(0xFF14B8A6), Color(0xFFEC4899), Color(0xFFEAB308)
  )

  var idx = 0
  for (r in 0 until gridRows) {
    for (c in 0 until gridCols) {
      val cx = marginX + c * slotW + (slotW / 2)
      val cy = gridTop + r * (iconSize + rowGap) + (iconSize / 2)
      drawMiniAppIcon(
        cx = cx,
        cy = cy,
        size = iconSize,
        color = appColors[idx % appColors.size],
        isSquircle = true,
        hasLabel = true
      )
      idx++
    }
  }

  // 3. Classic Dock with Center App Drawer Button
  drawMiniDock(
    w = w,
    h = h,
    capacity = 5,
    accessMode = Space.ACCESS_MODE_DOCK_BUTTON
  )
}

// -------------------------------------------------------------
// 4. Minimal Distraction-Free
// -------------------------------------------------------------
private fun DrawScope.drawMinimalPreset(w: Float, h: Float, columns: Int) {
  // Pure Pitch Black / Charcoal Canvas
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF09090B), Color(0xFF121214), Color(0xFF18181B))
    )
  )

  val marginX = w * 0.12f
  val contentW = w - (marginX * 2)

  // 1. Typographical Time Header
  val clockTop = h * 0.16f
  // Time hour & minute line
  drawRoundRect(
    color = Color.White.copy(alpha = 0.95f),
    topLeft = Offset(marginX, clockTop),
    size = Size(contentW * 0.55f, 9f),
    cornerRadius = CornerRadius(4.5f, 4.5f)
  )
  // Date subtitle line
  drawRoundRect(
    color = Color.White.copy(alpha = 0.45f),
    topLeft = Offset(marginX, clockTop + 16f),
    size = Size(contentW * 0.35f, 4.5f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // Rows 1 and 2: Expansive breathing room (empty)

  // 2. Row 3: Only 3 discrete minimalist icons, unlabelled
  val appsTop = h * 0.65f
  val appCount = 3
  val slotW = contentW / appCount
  val iconSize = w * 0.11f

  val minimalColors = listOf(
    Color(0xFFE2E8F0),
    Color(0xFFCBD5E1),
    Color(0xFF94A3B8)
  )

  for (i in 0 until appCount) {
    val cx = marginX + i * slotW + (slotW / 2)
    val cy = appsTop + (iconSize / 2)
    drawMiniAppIcon(
      cx = cx,
      cy = cy,
      size = iconSize,
      color = minimalColors[i],
      isSquircle = false,
      hasLabel = false
    )
  }

  // 3. Compact 3-Slot Minimal Dock
  val dockY = h * 0.85f
  val dockSlotW = contentW / 3
  for (i in 0 until 3) {
    val cx = marginX + i * dockSlotW + (dockSlotW / 2)
    drawCircle(
      color = Color.White.copy(alpha = 0.6f),
      radius = iconSize * 0.35f,
      center = Offset(cx, dockY + iconSize * 0.4f)
    )
  }

  // Bottom Gesture Bar
  drawRoundRect(
    color = Color.White.copy(alpha = 0.4f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2f),
    cornerRadius = CornerRadius(1f, 1f)
  )
}

// -------------------------------------------------------------
// 5. Productivity Dashboard
// -------------------------------------------------------------
private fun DrawScope.drawProductivityPreset(w: Float, h: Float, columns: Int) {
  // Deep Oceanic Focus Gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF0C4A6E), Color(0xFF075985), Color(0xFF0369A1))
    )
  )

  val marginX = w * 0.08f
  val contentW = w - (marginX * 2)

  // 1. Dual Widgets: Calendar Card + Clock (Rows 0-1)
  val topWidgetsY = h * 0.11f
  val topWidgetsH = h * 0.16f
  val cardGap = w * 0.03f
  val cardW = (contentW - cardGap) / 2

  // Left: Calendar Card Widget
  drawRoundRect(
    color = Color.White.copy(alpha = 0.16f),
    topLeft = Offset(marginX, topWidgetsY),
    size = Size(cardW, topWidgetsH),
    cornerRadius = CornerRadius(10f, 10f)
  )
  // Calendar red top banner
  drawRoundRect(
    color = Color(0xFFEF4444).copy(alpha = 0.85f),
    topLeft = Offset(marginX, topWidgetsY),
    size = Size(cardW, topWidgetsH * 0.28f),
    cornerRadius = CornerRadius(10f, 10f)
  )
  // Calendar day number bar
  drawRoundRect(
    color = Color.White.copy(alpha = 0.9f),
    topLeft = Offset(marginX + cardW * 0.3f, topWidgetsY + topWidgetsH * 0.42f),
    size = Size(cardW * 0.4f, 8f),
    cornerRadius = CornerRadius(4f, 4f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.6f),
    topLeft = Offset(marginX + cardW * 0.2f, topWidgetsY + topWidgetsH * 0.72f),
    size = Size(cardW * 0.6f, 3.5f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // Right: Clock & Weather Card Widget
  val rightCardX = marginX + cardW + cardGap
  drawRoundRect(
    color = Color.White.copy(alpha = 0.16f),
    topLeft = Offset(rightCardX, topWidgetsY),
    size = Size(cardW, topWidgetsH),
    cornerRadius = CornerRadius(10f, 10f)
  )
  drawCircle(
    color = Color(0xFFFBBF24),
    radius = 5f,
    center = Offset(rightCardX + cardW * 0.3f, topWidgetsY + topWidgetsH * 0.38f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.9f),
    topLeft = Offset(rightCardX + cardW * 0.45f, topWidgetsY + topWidgetsH * 0.33f),
    size = Size(cardW * 0.42f, 6.5f),
    cornerRadius = CornerRadius(3f, 3f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.55f),
    topLeft = Offset(rightCardX + cardW * 0.2f, topWidgetsY + topWidgetsH * 0.68f),
    size = Size(cardW * 0.6f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 2. Middle: Quick Notes Notepad Card (Row 2)
  val notesY = topWidgetsY + topWidgetsH + h * 0.02f
  val notesH = h * 0.12f
  drawRoundRect(
    color = Color(0xFFFEF3C7).copy(alpha = 0.22f),
    topLeft = Offset(marginX, notesY),
    size = Size(contentW, notesH),
    cornerRadius = CornerRadius(10f, 10f)
  )
  // Notepad header
  drawRoundRect(
    color = Color(0xFFFDE68A).copy(alpha = 0.75f),
    topLeft = Offset(marginX + contentW * 0.08f, notesY + notesH * 0.22f),
    size = Size(contentW * 0.35f, 5f),
    cornerRadius = CornerRadius(2.5f, 2.5f)
  )
  // Notepad bullet lines
  for (line in 1..2) {
    drawCircle(
      color = Color(0xFFFDE68A).copy(alpha = 0.75f),
      radius = 2.5f,
      center = Offset(marginX + contentW * 0.1f, notesY + notesH * (0.28f + line * 0.22f))
    )
    drawRoundRect(
      color = Color.White.copy(alpha = 0.7f),
      topLeft = Offset(marginX + contentW * 0.16f, notesY + notesH * (0.25f + line * 0.22f)),
      size = Size(contentW * 0.72f, 3.5f),
      cornerRadius = CornerRadius(2f, 2f)
    )
  }

  // 3. Lower: Curated Work Apps (Row 3)
  val appsTop = notesY + notesH + h * 0.035f
  val workAppsCount = 4
  val slotW = contentW / workAppsCount
  val iconSize = slotW * 0.65f

  val workColors = listOf(
    Color(0xFF38BDF8), // Email
    Color(0xFF34D399), // Sheets/Docs
    Color(0xFF818CF8), // Tasks/Calendar
    Color(0xFFF472B6)  // Notes
  )

  for (i in 0 until workAppsCount) {
    val cx = marginX + i * slotW + (slotW / 2)
    val cy = appsTop + (iconSize / 2)
    drawMiniAppIcon(
      cx = cx,
      cy = cy,
      size = iconSize,
      color = workColors[i],
      isSquircle = true,
      hasLabel = true
    )
  }

  // 4. Productivity Dock
  drawMiniDock(
    w = w,
    h = h,
    capacity = 5,
    accessMode = Space.ACCESS_MODE_DOCK_BUTTON
  )
}

// -------------------------------------------------------------
// 6. Compact Density
// -------------------------------------------------------------
private fun DrawScope.drawCompactPreset(w: Float, h: Float, columns: Int) {
  // Dark Obsidian / Carbon Gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF3F3F46))
    )
  )

  val marginX = w * 0.06f
  val contentW = w - (marginX * 2)

  // 1. Compact Top Search Pill (Row 0)
  val searchTop = h * 0.10f
  val searchH = h * 0.05f
  drawMiniSearchBarWidget(
    x = marginX,
    y = searchTop,
    w = contentW,
    h = searchH
  )

  // 2. High-Density 6-Column x 4-Row App Grid (Rows 1-4)
  val gridCols = columns.coerceIn(5, 6)
  val gridRows = 4
  val gridTop = searchTop + searchH + h * 0.02f
  val slotW = contentW / gridCols
  val iconSize = slotW * 0.65f
  val rowGap = h * 0.025f

  val denseColors = listOf(
    Color(0xFF38BDF8), Color(0xFFFB7185), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFFA78BFA), Color(0xFFF472B6),
    Color(0xFF60A5FA), Color(0xFF4ADE80), Color(0xFFF87171), Color(0xFF818CF8), Color(0xFF2DD4BF), Color(0xFFFCD34D),
    Color(0xFFC084FC), Color(0xFFFB923C), Color(0xFF4E9F3D), Color(0xFFE11D48), Color(0xFF0284C7), Color(0xFF6366F1),
    Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFFF97316), Color(0xFFEC4899)
  )

  var idx = 0
  for (r in 0 until gridRows) {
    for (c in 0 until gridCols) {
      val cx = marginX + c * slotW + (slotW / 2)
      val cy = gridTop + r * (iconSize + rowGap) + (iconSize / 2)
      drawMiniAppIcon(
        cx = cx,
        cy = cy,
        size = iconSize,
        color = denseColors[idx % denseColors.size],
        isSquircle = true,
        hasLabel = false // dense grid omits labels in preview for clarity
      )
      idx++
    }
  }

  // 3. Compact 6-Item Dock
  drawMiniDock(
    w = w,
    h = h,
    capacity = 6,
    accessMode = Space.ACCESS_MODE_SWIPE_UP
  )
}

// -------------------------------------------------------------
// Reusable Miniature Drawing Helpers
// -------------------------------------------------------------

/**
 * Draws a realistic Weather + Clock widget card with subtle frosted backdrop,
 * bold clock numeral bars, mini sun/weather glyph, and temperature/date pill.
 */
private fun DrawScope.drawMiniWeatherClockWidget(
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  prominent: Boolean = true
) {
  // Translucent rounded container
  drawRoundRect(
    color = Color.White.copy(alpha = 0.15f),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(12f, 12f)
  )

  // Inner subtle border
  drawRoundRect(
    color = Color.White.copy(alpha = 0.25f),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(12f, 12f),
    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
  )

  // Weather Sun icon glyph
  val sunRadius = if (prominent) 7f else 5f
  val sunCenter = Offset(x + w * 0.18f, y + h * 0.42f)
  drawCircle(
    color = Color(0xFFFBBF24),
    radius = sunRadius,
    center = sunCenter
  )
  // Weather Cloud puff
  drawCircle(
    color = Color.White.copy(alpha = 0.85f),
    radius = sunRadius * 0.75f,
    center = Offset(sunCenter.x + sunRadius * 0.7f, sunCenter.y + sunRadius * 0.3f)
  )

  // Prominent Clock Digits Bars
  val clockBarW = w * 0.45f
  val clockBarH = if (prominent) 9f else 7f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.95f),
    topLeft = Offset(x + w * 0.42f, y + h * 0.28f),
    size = Size(clockBarW, clockBarH),
    cornerRadius = CornerRadius(clockBarH / 2, clockBarH / 2)
  )

  // Weather string bar: "72° Sunny • Tue, Sep 15"
  drawRoundRect(
    color = Color.White.copy(alpha = 0.65f),
    topLeft = Offset(x + w * 0.42f, y + h * 0.58f),
    size = Size(clockBarW * 0.8f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )
}

/**
 * Draws a Google / search pill style widget.
 */
private fun DrawScope.drawMiniSearchBarWidget(
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  isPixelStyle: Boolean = false
) {
  val pillRadius = h / 2
  // Pill container
  drawRoundRect(
    color = if (isPixelStyle) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.16f),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(pillRadius, pillRadius)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.25f),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(pillRadius, pillRadius),
    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
  )

  // Search "G" or lens dot on left
  val leftDotX = x + h * 0.65f
  val leftDotY = y + h * 0.5f
  drawCircle(
    color = if (isPixelStyle) Color(0xFF4285F4) else Color.White.copy(alpha = 0.9f),
    radius = 3.5f,
    center = Offset(leftDotX, leftDotY)
  )

  // Center text hint bar
  drawRoundRect(
    color = Color.White.copy(alpha = 0.5f),
    topLeft = Offset(leftDotX + 8f, y + h * 0.42f),
    size = Size(w * 0.42f, 3f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )

  // Voice Mic / Lens dot on right
  val rightDotX = x + w - h * 0.65f
  drawCircle(
    color = if (isPixelStyle) Color(0xFFEA4335) else Color.White.copy(alpha = 0.8f),
    radius = 3f,
    center = Offset(rightDotX, leftDotY)
  )
}

/**
 * Draws a Pixel style "At a Glance" top widget.
 */
private fun DrawScope.drawMiniAtAGlanceWidget(
  x: Float,
  y: Float,
  w: Float,
  h: Float
) {
  // Day & Date string bar
  drawRoundRect(
    color = Color.White.copy(alpha = 0.95f),
    topLeft = Offset(x + 4f, y + h * 0.22f),
    size = Size(w * 0.48f, 6.5f),
    cornerRadius = CornerRadius(3.5f, 3.5f)
  )

  // Weather pill & temperature on right
  drawCircle(
    color = Color(0xFFFBBF24),
    radius = 4f,
    center = Offset(x + w * 0.72f, y + h * 0.35f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.75f),
    topLeft = Offset(x + w * 0.78f, y + h * 0.26f),
    size = Size(w * 0.18f, 5f),
    cornerRadius = CornerRadius(2.5f, 2.5f)
  )

  // Event subtitle line
  drawRoundRect(
    color = Color.White.copy(alpha = 0.5f),
    topLeft = Offset(x + 4f, y + h * 0.62f),
    size = Size(w * 0.32f, 3.5f),
    cornerRadius = CornerRadius(2f, 2f)
  )
}

/**
 * Draws an app icon (Squircle or Circle) with inner glyph and optional label line.
 */
private fun DrawScope.drawMiniAppIcon(
  cx: Float,
  cy: Float,
  size: Float,
  color: Color,
  isSquircle: Boolean = true,
  hasLabel: Boolean = true
) {
  val half = size / 2
  if (isSquircle) {
    drawRoundRect(
      color = color,
      topLeft = Offset(cx - half, cy - half),
      size = Size(size, size),
      cornerRadius = CornerRadius(size * 0.32f, size * 0.32f)
    )
  } else {
    drawCircle(
      color = color,
      radius = half,
      center = Offset(cx, cy)
    )
  }

  // Inner subtle icon highlight
  drawCircle(
    color = Color.White.copy(alpha = 0.85f),
    radius = size * 0.22f,
    center = Offset(cx, cy)
  )

  // Miniature app label bar below icon
  if (hasLabel) {
    drawRoundRect(
      color = Color.White.copy(alpha = 0.65f),
      topLeft = Offset(cx - half * 0.75f, cy + half + 2.5f),
      size = Size(size * 0.75f, 2.5f),
      cornerRadius = CornerRadius(1.25f, 1.25f)
    )
  }
}

/**
 * Draws the persistent Dock with the specified capacity and styling.
 */
private fun DrawScope.drawMiniDock(
  w: Float,
  h: Float,
  capacity: Int,
  accessMode: String,
  isPixelStyle: Boolean = false
) {
  val dockY = h * 0.84f
  val dockIconSize = (w * 0.84f / capacity) * 0.65f
  val marginX = w * 0.08f
  val contentW = w - (marginX * 2)
  val slotW = contentW / capacity

  val dockColors = listOf(
    Color(0xFF22C55E), // Phone
    Color(0xFF3B82F6), // Messages
    Color(0xFFF97316), // Chrome/Browser
    Color(0xFFA855F7), // Camera
    Color(0xFFEF4444), // Gallery
    Color(0xFF06B6D4), // Extra
    Color(0xFFEAB308)
  )

  for (i in 0 until capacity) {
    val cx = marginX + i * slotW + (slotW / 2)
    val cy = dockY + (dockIconSize / 2)

    // Classic Android Dock Button in center slot (index 2 of 5)
    if (accessMode == Space.ACCESS_MODE_DOCK_BUTTON && capacity == 5 && i == 2) {
      // White circle with 6 dots
      drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = dockIconSize * 0.5f,
        center = Offset(cx, cy)
      )
      // 6 tiny dots inside drawer button
      val dotRadius = 1.2f
      val dotSpacing = 3f
      for (dr in -1..1) {
        for (dc in -1..0) {
          drawCircle(
            color = Color(0xFF334155),
            radius = dotRadius,
            center = Offset(cx + (dc + 0.5f) * dotSpacing, cy + dr * dotSpacing)
          )
        }
      }
    } else {
      drawMiniAppIcon(
        cx = cx,
        cy = cy,
        size = dockIconSize,
        color = dockColors[i % dockColors.size],
        isSquircle = !isPixelStyle,
        hasLabel = false
      )
    }
  }

  // Bottom Navigation Gesture Pill
  drawRoundRect(
    color = Color.White.copy(alpha = 0.75f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.25f, 1.25f)
  )
}

/**
 * Rich Visual Layout Preset Card featuring the realistic phone picture preview on top,
 * along with title, inspiration badge, description, and key feature tags.
 */
@Composable
fun LayoutPresetVisualCard(
  preset: LayoutPreset,
  isSelected: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onSelect)
      .testTag("preset_card_${preset.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top: Miniature Realistic Phone Preview
      LayoutPresetPhonePreview(
        preset = preset,
        isSelected = isSelected,
        phoneWidth = 145.dp,
        phoneHeight = 250.dp,
        gridColumns = preset.gridColumns
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Title & Inspiration Tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = preset.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = preset.inspiration,
              style = MaterialTheme.typography.labelSmall,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        if (isSelected) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Selected",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Description text
      Text(
        text = preset.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Key Attribute Tags Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        AttributeChip(label = "${preset.gridColumns} Columns", isSelected = isSelected)
        AttributeChip(label = "${preset.dockCapacity} Dock", isSelected = isSelected)
        AttributeChip(
          label = if (preset.layer2AccessMode == Space.ACCESS_MODE_SWIPE_UP) "Swipe Up" else "Dock Button",
          isSelected = isSelected
        )
      }
    }
  }
}

@Composable
private fun AttributeChip(label: String, isSelected: Boolean) {
  Surface(
    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium
    )
  }
}
