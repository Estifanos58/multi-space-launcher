package com.multispace.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.LayoutPreset
import com.multispace.domain.model.Space
import com.multispace.ui.theme.PrimaryPurple
import com.multispace.ui.theme.PrimaryPurpleDark

/**
 * Graphical Smartphone Picture Preview for a Layout Preset.
 * Renders an authentic miniature phone screen illustrating the layout paradigm visually.
 */
@Composable
fun LayoutPresetPhonePreview(
  preset: LayoutPreset,
  modifier: Modifier = Modifier,
  phoneWidth: Dp = 130.dp,
  phoneHeight: Dp = 230.dp,
  isSelected: Boolean = false
) {
  val borderColor by animateColorAsState(
    if (isSelected) PrimaryPurple else Color(0xFFD1D5DB),
    label = "phone_border_color"
  )
  val elevation by animateDpAsState(
    if (isSelected) 8.dp else 3.dp,
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
    color = Color(0xFF1E293B)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Screen Wallpaper Background Canvas
      PresetScreenCanvas(preset = preset)

      // Top Notch & Status Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "9:41",
          fontSize = 7.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White.copy(alpha = 0.9f)
        )
        // Camera Hole-punch / Speaker slit
        Box(
          modifier = Modifier
            .width(18.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black.copy(alpha = 0.7f))
        )
        // Battery / Wifi dots
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
          Box(modifier = Modifier.width(6.dp).height(3.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = 0.8f)))
        }
      }

      // Checkmark overlay when selected
      if (isSelected) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(PrimaryPurple),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PresetScreenCanvas(preset: LayoutPreset) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    when (preset.id) {
      Space.PRESET_ONE_UI -> drawOneUiPreset(w, h)
      Space.PRESET_APPLE -> drawApplePreset(w, h)
      Space.PRESET_PIXEL -> drawPixelPreset(w, h)
      Space.PRESET_CLASSIC -> drawClassicAndroidPreset(w, h)
      Space.PRESET_MINIMAL -> drawMinimalPreset(w, h)
      Space.PRESET_COMPACT -> drawCompactPreset(w, h)
      Space.PRESET_LARGE_ICONS -> drawLargeIconsPreset(w, h)
      Space.PRESET_PRODUCTIVITY -> drawProductivityPreset(w, h)
      Space.PRESET_GAMING -> drawGamingPreset(w, h)
      else -> drawDefaultMultiSpacePreset(w, h)
    }
  }
}

// 1. Samsung One UI Preview Drawing
private fun DrawScope.drawOneUiPreset(w: Float, h: Float) {
  // Soft Gradient Wallpaper
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF2E1065), Color(0xFF4C1D95), Color(0xFF6D28D9))
    )
  )

  // Top Clock Widget Block
  val widgetTop = h * 0.12f
  val widgetH = h * 0.14f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.15f),
    topLeft = Offset(w * 0.1f, widgetTop),
    size = Size(w * 0.8f, widgetH),
    cornerRadius = CornerRadius(10f, 10f)
  )
  // Clock bar inside widget
  drawRoundRect(
    color = Color.White.copy(alpha = 0.85f),
    topLeft = Offset(w * 0.18f, widgetTop + widgetH * 0.25f),
    size = Size(w * 0.35f, 6f),
    cornerRadius = CornerRadius(3f, 3f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.5f),
    topLeft = Offset(w * 0.18f, widgetTop + widgetH * 0.55f),
    size = Size(w * 0.25f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 4x3 Squircle App Grid
  val gridTop = h * 0.30f
  val iconCols = 4
  val iconRows = 3
  val iconSize = w * 0.13f
  val colSpacing = (w * 0.84f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.035f

  val appColors = listOf(
    Color(0xFF38BDF8), Color(0xFFFB7185), Color(0xFF34D399), Color(0xFFFBBF24),
    Color(0xFFA78BFA), Color(0xFFF472B6), Color(0xFF60A5FA), Color(0xFF4ADE80),
    Color(0xFFF87171), Color(0xFF818CF8), Color(0xFF2DD4BF), Color(0xFFFCD34D)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.08f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = appColors[idx % appColors.size]
      idx++
      // One UI Squircle
      drawRoundRect(
        color = col,
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(iconSize * 0.35f, iconSize * 0.35f)
      )
      // Label line
      drawRoundRect(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = Offset(x + iconSize * 0.1f, y + iconSize + 3f),
        size = Size(iconSize * 0.8f, 2.5f),
        cornerRadius = CornerRadius(1f, 1f)
      )
    }
  }

  // Page indicator dots
  val dotsY = h * 0.77f
  drawCircle(Color.White, radius = 2.5f, center = Offset(w * 0.44f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.4f), radius = 2f, center = Offset(w * 0.50f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.4f), radius = 2f, center = Offset(w * 0.56f, dotsY))

  // One UI Dock (5 slots)
  val dockY = h * 0.83f
  val dockIconSize = w * 0.12f
  val dockSpacing = (w * 0.86f - (5 * dockIconSize)) / 4
  val dockColors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFF97316), Color(0xFFA855F7), Color(0xFFEF4444))

  for (i in 0 until 5) {
    val dx = w * 0.07f + i * (dockIconSize + dockSpacing)
    drawRoundRect(
      color = dockColors[i],
      topLeft = Offset(dx, dockY),
      size = Size(dockIconSize, dockIconSize),
      cornerRadius = CornerRadius(dockIconSize * 0.35f, dockIconSize * 0.35f)
    )
  }

  // Bottom Gesture Navigation Bar
  drawRoundRect(
    color = Color.White.copy(alpha = 0.7f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )
}

// 2. Apple iOS Preview Drawing
private fun DrawScope.drawApplePreset(w: Float, h: Float) {
  // Vibrant iOS Gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFF6366F1))
    )
  )

  // Dual Top Widgets (Square cards)
  val widgetTop = h * 0.11f
  val widgetSize = w * 0.38f
  // Left Calendar Widget
  drawRoundRect(
    color = Color.White.copy(alpha = 0.22f),
    topLeft = Offset(w * 0.08f, widgetTop),
    size = Size(widgetSize, widgetSize),
    cornerRadius = CornerRadius(14f, 14f)
  )
  drawCircle(Color(0xFFEF4444), radius = 5f, center = Offset(w * 0.16f, widgetTop + widgetSize * 0.3f))
  drawRoundRect(
    color = Color.White.copy(alpha = 0.9f),
    topLeft = Offset(w * 0.13f, widgetTop + widgetSize * 0.55f),
    size = Size(widgetSize * 0.55f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // Right Weather Widget
  drawRoundRect(
    color = Color.White.copy(alpha = 0.22f),
    topLeft = Offset(w * 0.54f, widgetTop),
    size = Size(widgetSize, widgetSize),
    cornerRadius = CornerRadius(14f, 14f)
  )
  drawCircle(Color(0xFFFBBF24), radius = 6f, center = Offset(w * 0.73f, widgetTop + widgetSize * 0.4f))
  drawRoundRect(
    color = Color.White.copy(alpha = 0.9f),
    topLeft = Offset(w * 0.62f, widgetTop + widgetSize * 0.68f),
    size = Size(widgetSize * 0.45f, 3.5f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 4x2 Rounded Square App Grid
  val gridTop = h * 0.43f
  val iconCols = 4
  val iconRows = 2
  val iconSize = w * 0.14f
  val colSpacing = (w * 0.84f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.038f

  val appColors = listOf(
    Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFFF59E0B),
    Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFFE11D48), Color(0xFF84CC16)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.08f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = appColors[idx % appColors.size]
      idx++
      // iOS Rounded Square Icon
      drawRoundRect(
        color = col,
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(iconSize * 0.28f, iconSize * 0.28f)
      )
      // Label line
      drawRoundRect(
        color = Color.White.copy(alpha = 0.65f),
        topLeft = Offset(x + iconSize * 0.1f, y + iconSize + 3f),
        size = Size(iconSize * 0.8f, 2.5f),
        cornerRadius = CornerRadius(1f, 1f)
      )
    }
  }

  // iOS Page Dots Indicator
  val dotsY = h * 0.76f
  drawCircle(Color.White, radius = 2.5f, center = Offset(w * 0.46f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.4f), radius = 2f, center = Offset(w * 0.50f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.4f), radius = 2f, center = Offset(w * 0.54f, dotsY))

  // Frosted Glass iOS Dock Shelf (4 slots)
  val dockShelfTop = h * 0.80f
  val dockShelfH = h * 0.15f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.25f),
    topLeft = Offset(w * 0.05f, dockShelfTop),
    size = Size(w * 0.9f, dockShelfH),
    cornerRadius = CornerRadius(18f, 18f)
  )

  val dockIconSize = w * 0.14f
  val dockSpacing = (w * 0.80f - (4 * dockIconSize)) / 3
  val dockColors = listOf(Color(0xFF22C55E), Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFFE11D48))

  for (i in 0 until 4) {
    val dx = w * 0.10f + i * (dockIconSize + dockSpacing)
    val dy = dockShelfTop + (dockShelfH - dockIconSize) / 2
    drawRoundRect(
      color = dockColors[i],
      topLeft = Offset(dx, dy),
      size = Size(dockIconSize, dockIconSize),
      cornerRadius = CornerRadius(dockIconSize * 0.28f, dockIconSize * 0.28f)
    )
  }

  // Home Bar
  drawRoundRect(
    color = Color.White.copy(alpha = 0.8f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )
}

// 3. Google Pixel Preview Drawing
private fun DrawScope.drawPixelPreset(w: Float, h: Float) {
  // Vibrant Nature Pixel Wallpaper
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981))
    )
  )

  // Top "At a Glance" Widget
  val glanceTop = h * 0.11f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.9f),
    topLeft = Offset(w * 0.1f, glanceTop),
    size = Size(w * 0.45f, 5f),
    cornerRadius = CornerRadius(2.5f, 2.5f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.6f),
    topLeft = Offset(w * 0.1f, glanceTop + 8f),
    size = Size(w * 0.3f, 3.5f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 5x3 Circular App Grid
  val gridTop = h * 0.26f
  val iconCols = 5
  val iconRows = 3
  val iconSize = w * 0.11f
  val colSpacing = (w * 0.86f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.038f

  val appColors = listOf(
    Color(0xFF38BDF8), Color(0xFFF43F5E), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFFA855F7),
    Color(0xFFEC4899), Color(0xFF3B82F6), Color(0xFF22C55E), Color(0xFFEAB308), Color(0xFF6366F1),
    Color(0xFF14B8A6), Color(0xFFFB923C), Color(0xFF06B6D4), Color(0xFFA3E635), Color(0xFFF472B6)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.07f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = appColors[idx % appColors.size]
      idx++
      // Pixel Circle Icon
      drawCircle(
        color = col,
        radius = iconSize / 2,
        center = Offset(x + iconSize / 2, y + iconSize / 2)
      )
      // Label line
      drawRoundRect(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = Offset(x + iconSize * 0.1f, y + iconSize + 3f),
        size = Size(iconSize * 0.8f, 2.5f),
        cornerRadius = CornerRadius(1f, 1f)
      )
    }
  }

  // Google Search Pill Bar
  val searchTop = h * 0.72f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.28f),
    topLeft = Offset(w * 0.08f, searchTop),
    size = Size(w * 0.84f, h * 0.06f),
    cornerRadius = CornerRadius(16f, 16f)
  )
  // Google 'G' pill dot
  drawCircle(Color(0xFF38BDF8), radius = 4f, center = Offset(w * 0.16f, searchTop + h * 0.03f))
  drawRoundRect(
    color = Color.White.copy(alpha = 0.75f),
    topLeft = Offset(w * 0.24f, searchTop + h * 0.024f),
    size = Size(w * 0.35f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // Pixel 5-item Dock
  val dockY = h * 0.83f
  val dockIconSize = w * 0.11f
  val dockSpacing = (w * 0.86f - (5 * dockIconSize)) / 4
  val dockColors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFF97316), Color(0xFFA855F7), Color(0xFFEF4444))

  for (i in 0 until 5) {
    val dx = w * 0.07f + i * (dockIconSize + dockSpacing)
    drawCircle(
      color = dockColors[i],
      radius = dockIconSize / 2,
      center = Offset(dx + dockIconSize / 2, dockY + dockIconSize / 2)
    )
  }

  // Navigation Bar
  drawRoundRect(
    color = Color.White.copy(alpha = 0.8f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )
}

// 4. Classic Stock Android Preview Drawing
private fun DrawScope.drawClassicAndroidPreset(w: Float, h: Float) {
  // Dark Navy Wallpaper
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    )
  )

  // Classic Google Search Bar at Top
  val searchTop = h * 0.10f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.2f),
    topLeft = Offset(w * 0.08f, searchTop),
    size = Size(w * 0.84f, h * 0.055f),
    cornerRadius = CornerRadius(12f, 12f)
  )
  drawCircle(Color(0xFFEF4444), radius = 3.5f, center = Offset(w * 0.16f, searchTop + h * 0.0275f))
  drawRoundRect(
    color = Color.White.copy(alpha = 0.7f),
    topLeft = Offset(w * 0.23f, searchTop + h * 0.022f),
    size = Size(w * 0.4f, 3.5f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 4x3 App Grid
  val gridTop = h * 0.25f
  val iconCols = 4
  val iconRows = 3
  val iconSize = w * 0.13f
  val colSpacing = (w * 0.84f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.04f

  val appColors = listOf(
    Color(0xFF0284C7), Color(0xFF16A34A), Color(0xFFDC2626), Color(0xFFD97706),
    Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFFDB2777), Color(0xFF4F46E5),
    Color(0xFFEA580C), Color(0xFF059669), Color(0xFF2563EB), Color(0xFFCA8A04)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.08f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = appColors[idx % appColors.size]
      idx++
      drawRoundRect(
        color = col,
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(6f, 6f)
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = Offset(x + iconSize * 0.1f, y + iconSize + 3f),
        size = Size(iconSize * 0.8f, 2.5f),
        cornerRadius = CornerRadius(1f, 1f)
      )
    }
  }

  // Classic Dock with Center App Drawer Button
  val dockY = h * 0.82f
  val dockIconSize = w * 0.12f
  val dockSpacing = (w * 0.86f - (5 * dockIconSize)) / 4

  // Left 2 Apps: Phone, Messages
  drawRoundRect(
    color = Color(0xFF22C55E),
    topLeft = Offset(w * 0.07f, dockY),
    size = Size(dockIconSize, dockIconSize),
    cornerRadius = CornerRadius(6f, 6f)
  )
  drawRoundRect(
    color = Color(0xFF3B82F6),
    topLeft = Offset(w * 0.07f + (dockIconSize + dockSpacing), dockY),
    size = Size(dockIconSize, dockIconSize),
    cornerRadius = CornerRadius(6f, 6f)
  )

  // Center 6-Dots App Drawer Button!
  val centerDx = w * 0.07f + 2 * (dockIconSize + dockSpacing)
  drawCircle(
    color = Color.White.copy(alpha = 0.35f),
    radius = dockIconSize / 2,
    center = Offset(centerDx + dockIconSize / 2, dockY + dockIconSize / 2)
  )
  // 6 dots inside center button
  for (dr in 0..1) {
    for (dc in 0..2) {
      drawCircle(
        color = Color.White,
        radius = 1.5f,
        center = Offset(centerDx + dockIconSize * (0.3f + dc * 0.2f), dockY + dockIconSize * (0.35f + dr * 0.3f))
      )
    }
  }

  // Right 2 Apps: Browser, Camera
  drawRoundRect(
    color = Color(0xFFF97316),
    topLeft = Offset(w * 0.07f + 3 * (dockIconSize + dockSpacing), dockY),
    size = Size(dockIconSize, dockIconSize),
    cornerRadius = CornerRadius(6f, 6f)
  )
  drawRoundRect(
    color = Color(0xFFEF4444),
    topLeft = Offset(w * 0.07f + 4 * (dockIconSize + dockSpacing), dockY),
    size = Size(dockIconSize, dockIconSize),
    cornerRadius = CornerRadius(6f, 6f)
  )

  // 3-Button Navigation Bar at bottom (Back, Home, Recents)
  val navY = h * 0.96f
  drawCircle(Color.White.copy(alpha = 0.7f), radius = 3f, center = Offset(w * 0.5f, navY))
  drawRect(Color.White.copy(alpha = 0.7f), topLeft = Offset(w * 0.72f, navY - 3f), size = Size(6f, 6f))
  drawCircle(Color.White.copy(alpha = 0.7f), radius = 3f, center = Offset(w * 0.28f, navY))
}

// 5. Minimalist Preview Drawing
private fun DrawScope.drawMinimalPreset(w: Float, h: Float) {
  // Pure AMOLED Dark Slate
  drawRect(color = Color(0xFF09090B))

  // Ultra-Clean Minimal Time
  val timeY = h * 0.20f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.95f),
    topLeft = Offset(w * 0.12f, timeY),
    size = Size(w * 0.45f, 10f),
    cornerRadius = CornerRadius(3f, 3f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.4f),
    topLeft = Offset(w * 0.12f, timeY + 16f),
    size = Size(w * 0.25f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // Clean Vertical App List (3 columns, subtle icon tiles, no clutter)
  val listTop = h * 0.42f
  val items = 4
  val itemH = h * 0.07f

  for (i in 0 until items) {
    val y = listTop + i * itemH
    // Left minimal monochrome dot
    drawCircle(
      color = Color.White.copy(alpha = 0.8f),
      radius = 4.5f,
      center = Offset(w * 0.18f, y + itemH * 0.4f)
    )
    // Clean text bar
    drawRoundRect(
      color = Color.White.copy(alpha = 0.85f),
      topLeft = Offset(w * 0.28f, y + itemH * 0.32f),
      size = Size(w * (0.35f + (i % 3) * 0.1f), 5f),
      cornerRadius = CornerRadius(2f, 2f)
    )
  }

  // Thin separator
  drawLine(
    color = Color.White.copy(alpha = 0.15f),
    start = Offset(w * 0.12f, h * 0.78f),
    end = Offset(w * 0.88f, h * 0.78f),
    strokeWidth = 1f
  )

  // 3-slot minimal dock
  val dockY = h * 0.84f
  val dockSlots = 3
  val dockSlotSize = w * 0.12f
  val dockSpacing = (w * 0.6f - (dockSlots * dockSlotSize)) / (dockSlots - 1)

  for (i in 0 until dockSlots) {
    val dx = w * 0.2f + i * (dockSlotSize + dockSpacing)
    drawRoundRect(
      color = Color.White.copy(alpha = 0.3f),
      topLeft = Offset(dx, dockY),
      size = Size(dockSlotSize, dockSlotSize),
      cornerRadius = CornerRadius(dockSlotSize * 0.3f, dockSlotSize * 0.3f)
    )
  }

  // Bottom line
  drawRoundRect(
    color = Color.White.copy(alpha = 0.6f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2f),
    cornerRadius = CornerRadius(1f, 1f)
  )
}

// 6. Compact Density Preview Drawing
private fun DrawScope.drawCompactPreset(w: Float, h: Float) {
  // Dense Dark Tech Wallpaper
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF3F3F46))
    )
  )

  // 6x5 Dense Grid of mini apps
  val gridTop = h * 0.12f
  val iconCols = 6
  val iconRows = 5
  val iconSize = w * 0.10f
  val colSpacing = (w * 0.88f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.024f

  val denseColors = listOf(
    Color(0xFF38BDF8), Color(0xFFF43F5E), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFFA855F7), Color(0xFFEC4899),
    Color(0xFF3B82F6), Color(0xFF22C55E), Color(0xFFEAB308), Color(0xFF6366F1), Color(0xFF14B8A6), Color(0xFFFB923C),
    Color(0xFF06B6D4), Color(0xFFA3E635), Color(0xFFF472B6), Color(0xFF818CF8), Color(0xFF2DD4BF), Color(0xFFFCD34D)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.06f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = denseColors[idx % denseColors.size]
      idx++
      drawRoundRect(
        color = col,
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(iconSize * 0.25f, iconSize * 0.25f)
      )
    }
  }

  // 6-app mini dock
  val dockY = h * 0.85f
  val dockIconSize = w * 0.10f
  val dockSpacing = (w * 0.88f - (6 * dockIconSize)) / 5

  for (i in 0 until 6) {
    val dx = w * 0.06f + i * (dockIconSize + dockSpacing)
    val col = denseColors[(i + 4) % denseColors.size]
    drawRoundRect(
      color = col,
      topLeft = Offset(dx, dockY),
      size = Size(dockIconSize, dockIconSize),
      cornerRadius = CornerRadius(dockIconSize * 0.25f, dockIconSize * 0.25f)
    )
  }

  drawRoundRect(
    color = Color.White.copy(alpha = 0.7f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2f),
    cornerRadius = CornerRadius(1f, 1f)
  )
}

// 7. Large Icons Preview Drawing
private fun DrawScope.drawLargeIconsPreset(w: Float, h: Float) {
  // Clean High-Contrast Background
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    )
  )

  // 3x3 Large Accessible Tiles
  val gridTop = h * 0.13f
  val iconCols = 3
  val iconRows = 3
  val iconSize = w * 0.21f
  val colSpacing = (w * 0.82f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.045f

  val largeColors = listOf(
    Color(0xFF0284C7), Color(0xFF16A34A), Color(0xFFDC2626),
    Color(0xFFD97706), Color(0xFF7C3AED), Color(0xFF0D9488),
    Color(0xFFDB2777), Color(0xFF4F46E5), Color(0xFFEA580C)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.09f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = largeColors[idx % largeColors.size]
      idx++
      // Large Rounded Square Tile
      drawRoundRect(
        color = col,
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(iconSize * 0.28f, iconSize * 0.28f)
      )
      // High-contrast bold label
      drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(x + iconSize * 0.15f, y + iconSize + 4f),
        size = Size(iconSize * 0.7f, 3.5f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
      )
    }
  }

  // 3 Large Dock Icons
  val dockY = h * 0.82f
  val dockIconSize = w * 0.20f
  val dockSpacing = (w * 0.80f - (3 * dockIconSize)) / 2
  val dockColors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFEF4444))

  for (i in 0 until 3) {
    val dx = w * 0.10f + i * (dockIconSize + dockSpacing)
    drawRoundRect(
      color = dockColors[i],
      topLeft = Offset(dx, dockY),
      size = Size(dockIconSize, dockIconSize),
      cornerRadius = CornerRadius(dockIconSize * 0.28f, dockIconSize * 0.28f)
    )
  }

  drawRoundRect(
    color = Color.White.copy(alpha = 0.8f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )
}

// 8. Productivity Preview Drawing
private fun DrawScope.drawProductivityPreset(w: Float, h: Float) {
  // Deep Abyssal Ocean Wallpaper
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF082F49), Color(0xFF0369A1), Color(0xFF0284C7))
    )
  )

  // Top Productivity Schedule Widget
  val widgetTop = h * 0.11f
  val widgetH = h * 0.16f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.2f),
    topLeft = Offset(w * 0.08f, widgetTop),
    size = Size(w * 0.84f, widgetH),
    cornerRadius = CornerRadius(12f, 12f)
  )
  drawCircle(Color(0xFF38BDF8), radius = 4.5f, center = Offset(w * 0.18f, widgetTop + widgetH * 0.32f))
  drawRoundRect(
    color = Color.White.copy(alpha = 0.9f),
    topLeft = Offset(w * 0.26f, widgetTop + widgetH * 0.25f),
    size = Size(w * 0.48f, 5f),
    cornerRadius = CornerRadius(2.5f, 2.5f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.6f),
    topLeft = Offset(w * 0.26f, widgetTop + widgetH * 0.58f),
    size = Size(w * 0.32f, 3.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )

  // 4x3 Work & Study Grid
  val gridTop = h * 0.32f
  val iconCols = 4
  val iconRows = 3
  val iconSize = w * 0.13f
  val colSpacing = (w * 0.84f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.038f

  val prodColors = listOf(
    Color(0xFF38BDF8), Color(0xFF60A5FA), Color(0xFF818CF8), Color(0xFFA78BFA),
    Color(0xFF34D399), Color(0xFF2DD4BF), Color(0xFF06B6D4), Color(0xFF0284C7),
    Color(0xFFFBBF24), Color(0xFFF87171), Color(0xFFFB923C), Color(0xFFF472B6)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.08f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = prodColors[idx % prodColors.size]
      idx++
      drawRoundRect(
        color = col,
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(iconSize * 0.3f, iconSize * 0.3f)
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = Offset(x + iconSize * 0.1f, y + iconSize + 3f),
        size = Size(iconSize * 0.8f, 2.5f),
        cornerRadius = CornerRadius(1f, 1f)
      )
    }
  }

  // 5-slot Productivity Dock
  val dockY = h * 0.83f
  val dockIconSize = w * 0.12f
  val dockSpacing = (w * 0.86f - (5 * dockIconSize)) / 4
  val dockColors = listOf(Color(0xFF38BDF8), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B))

  for (i in 0 until 5) {
    val dx = w * 0.07f + i * (dockIconSize + dockSpacing)
    drawRoundRect(
      color = dockColors[i],
      topLeft = Offset(dx, dockY),
      size = Size(dockIconSize, dockIconSize),
      cornerRadius = CornerRadius(dockIconSize * 0.3f, dockIconSize * 0.3f)
    )
  }

  drawRoundRect(
    color = Color.White.copy(alpha = 0.8f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )
}

// 9. Gaming Preview Drawing
private fun DrawScope.drawGamingPreset(w: Float, h: Float) {
  // Cyberpunk Neon Dark Wallpaper
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF1A0B2E), Color(0xFF2D0A4E), Color(0xFF4C0519))
    )
  )

  // Cyber Neon Glow Accents
  drawCircle(Color(0xFFEC4899).copy(alpha = 0.25f), radius = w * 0.4f, center = Offset(w * 0.8f, h * 0.2f))
  drawCircle(Color(0xFF06B6D4).copy(alpha = 0.2f), radius = w * 0.45f, center = Offset(w * 0.2f, h * 0.7f))

  // 4x3 Neon App Tiles
  val gridTop = h * 0.18f
  val iconCols = 4
  val iconRows = 3
  val iconSize = w * 0.14f
  val colSpacing = (w * 0.84f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.045f

  val neonColors = listOf(
    Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFFA855F7), Color(0xFF10B981),
    Color(0xFFEAB308), Color(0xFFEC4899), Color(0xFF3B82F6), Color(0xFFF97316),
    Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFF472B6), Color(0xFF6366F1)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.08f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = neonColors[idx % neonColors.size]
      idx++
      // Dark Tile with Neon Border
      drawRoundRect(
        color = Color(0xFF0F071D),
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(8f, 8f)
      )
      drawRoundRect(
        color = col,
        topLeft = Offset(x + 2f, y + 2f),
        size = Size(iconSize - 4f, iconSize - 4f),
        cornerRadius = CornerRadius(6f, 6f)
      )
    }
  }

  // Swipe Up Accent Arrow
  val arrowY = h * 0.76f
  drawLine(
    color = Color(0xFF06B6D4),
    start = Offset(w * 0.45f, arrowY + 4f),
    end = Offset(w * 0.5f, arrowY),
    strokeWidth = 2.5f
  )
  drawLine(
    color = Color(0xFF06B6D4),
    start = Offset(w * 0.5f, arrowY),
    end = Offset(w * 0.55f, arrowY + 4f),
    strokeWidth = 2.5f
  )

  // 4-slot Gaming Dock
  val dockY = h * 0.83f
  val dockIconSize = w * 0.13f
  val dockSpacing = (w * 0.82f - (4 * dockIconSize)) / 3
  val dockColors = listOf(Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFFA855F7), Color(0xFF10B981))

  for (i in 0 until 4) {
    val dx = w * 0.09f + i * (dockIconSize + dockSpacing)
    drawRoundRect(
      color = dockColors[i],
      topLeft = Offset(dx, dockY),
      size = Size(dockIconSize, dockIconSize),
      cornerRadius = CornerRadius(8f, 8f)
    )
  }

  drawRoundRect(
    color = Color(0xFFEC4899),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )
}

// 10. Default Multi-Space Preview Drawing
private fun DrawScope.drawDefaultMultiSpacePreset(w: Float, h: Float) {
  // Royal Indigo Wallpaper
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))
    )
  )

  // Top Search Pill
  val searchTop = h * 0.10f
  drawRoundRect(
    color = Color.White.copy(alpha = 0.2f),
    topLeft = Offset(w * 0.08f, searchTop),
    size = Size(w * 0.84f, h * 0.055f),
    cornerRadius = CornerRadius(12f, 12f)
  )
  drawCircle(Color.White.copy(alpha = 0.7f), radius = 3.5f, center = Offset(w * 0.16f, searchTop + h * 0.0275f))
  drawRoundRect(
    color = Color.White.copy(alpha = 0.7f),
    topLeft = Offset(w * 0.24f, searchTop + h * 0.022f),
    size = Size(w * 0.4f, 3.5f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 4x3 App Grid
  val gridTop = h * 0.25f
  val iconCols = 4
  val iconRows = 3
  val iconSize = w * 0.13f
  val colSpacing = (w * 0.84f - (iconCols * iconSize)) / (iconCols - 1)
  val rowSpacing = h * 0.04f

  val appColors = listOf(
    Color(0xFF7C3AED), Color(0xFF2563EB), Color(0xFF059669), Color(0xFFEA580C),
    Color(0xFFDB2777), Color(0xFF0891B2), Color(0xFFD97706), Color(0xFF4F46E5),
    Color(0xFFDC2626), Color(0xFF16A34A), Color(0xFF0284C7), Color(0xFF9333EA)
  )

  var idx = 0
  for (r in 0 until iconRows) {
    for (c in 0 until iconCols) {
      val x = w * 0.08f + c * (iconSize + colSpacing)
      val y = gridTop + r * (iconSize + rowSpacing)
      val col = appColors[idx % appColors.size]
      idx++
      drawRoundRect(
        color = col,
        topLeft = Offset(x, y),
        size = Size(iconSize, iconSize),
        cornerRadius = CornerRadius(iconSize * 0.3f, iconSize * 0.3f)
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = Offset(x + iconSize * 0.1f, y + iconSize + 3f),
        size = Size(iconSize * 0.8f, 2.5f),
        cornerRadius = CornerRadius(1f, 1f)
      )
    }
  }

  // Page indicator dots
  val dotsY = h * 0.77f
  drawCircle(Color.White, radius = 2.5f, center = Offset(w * 0.46f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.4f), radius = 2f, center = Offset(w * 0.50f, dotsY))
  drawCircle(Color.White.copy(alpha = 0.4f), radius = 2f, center = Offset(w * 0.54f, dotsY))

  // 5-Slot Multi-Space Dock
  val dockY = h * 0.83f
  val dockIconSize = w * 0.12f
  val dockSpacing = (w * 0.86f - (5 * dockIconSize)) / 4
  val dockColors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFF97316), Color(0xFFA855F7), Color(0xFFEF4444))

  for (i in 0 until 5) {
    val dx = w * 0.07f + i * (dockIconSize + dockSpacing)
    drawRoundRect(
      color = dockColors[i],
      topLeft = Offset(dx, dockY),
      size = Size(dockIconSize, dockIconSize),
      cornerRadius = CornerRadius(dockIconSize * 0.3f, dockIconSize * 0.3f)
    )
  }

  drawRoundRect(
    color = Color.White.copy(alpha = 0.8f),
    topLeft = Offset(w * 0.35f, h * 0.97f),
    size = Size(w * 0.3f, 2.5f),
    cornerRadius = CornerRadius(1.5f, 1.5f)
  )
}

/**
 * Rich Visual Layout Preset Card featuring the phone picture preview on top,
 * along with title, inspiration badge, and key feature tags.
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
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ),
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top: Picture Preview of Smartphone Layout
      LayoutPresetPhonePreview(
        preset = preset,
        isSelected = isSelected,
        phoneWidth = 140.dp,
        phoneHeight = 240.dp
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Middle: Title & Inspiration Tag
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
