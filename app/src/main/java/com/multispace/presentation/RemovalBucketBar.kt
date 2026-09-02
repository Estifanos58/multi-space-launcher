package com.multispace.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.CrimsonNova

@Composable
fun RemovalBucketBar(
  isVisible: Boolean,
  isHovered: Boolean = false,
  onPositioned: (Rect) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scale by animateFloatAsState(
    targetValue = if (isHovered) 1.25f else 1.0f,
    animationSpec = spring(),
    label = "bin_scale"
  )

  AnimatedVisibility(
    visible = isVisible,
    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .padding(top = AppDimens.Spacing8)
        .onGloballyPositioned { coordinates ->
          onPositioned(coordinates.boundsInRoot())
        }
        .scale(scale)
        .size(54.dp)
        .clip(CircleShape)
        .background(
          if (isHovered) CrimsonNova else MaterialTheme.colorScheme.errorContainer
        )
        .border(
          AppDimens.BorderMedium,
          if (isHovered) Color.White.copy(alpha = 0.8f) else CrimsonNova.copy(alpha = 0.4f),
          CircleShape
        )
        .testTag("removal_bucket_bar"),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (isHovered) Icons.Filled.Delete else Icons.Outlined.Delete,
        contentDescription = "Remove from Home",
        tint = if (isHovered) Color.White else MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
          .size(AppDimens.IconLg)
          .testTag("removal_bin_icon")
      )
    }
  }
}
