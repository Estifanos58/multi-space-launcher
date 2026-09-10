package com.multispace.presentation.customization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CustomizationSection {
  NONE,
  WALLPAPER,
  WIDGETS,
  THEME,
  PAGE_CONTROL
}

@Composable
fun DesktopCustomizationBar(
  visible: Boolean,
  onSelectSection: (CustomizationSection) -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically { it } + fadeIn(),
    exit = slideOutVertically { it } + fadeOut(),
    modifier = modifier
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
      tonalElevation = 8.dp,
      shadowElevation = 12.dp,
      modifier = Modifier
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(24.dp)
        )
        .testTag("desktop_customization_bar")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        CustomizationBarItem(
          icon = Icons.Default.Wallpaper,
          label = "Wallpaper",
          testTag = "customization_action_wallpaper",
          onClick = { onSelectSection(CustomizationSection.WALLPAPER) }
        )
        CustomizationBarItem(
          icon = Icons.Default.Widgets,
          label = "Widgets",
          testTag = "customization_action_widgets",
          onClick = { onSelectSection(CustomizationSection.WIDGETS) }
        )
        CustomizationBarItem(
          icon = Icons.Default.Palette,
          label = "Theme",
          testTag = "customization_action_theme",
          onClick = { onSelectSection(CustomizationSection.THEME) }
        )
        CustomizationBarItem(
          icon = Icons.Default.Layers,
          label = "Page Control",
          testTag = "customization_action_page_control",
          onClick = { onSelectSection(CustomizationSection.PAGE_CONTROL) }
        )
      }
    }
  }
}

@Composable
private fun CustomizationBarItem(
  icon: ImageVector,
  label: String,
  testTag: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(24.dp)
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )
  }
}
