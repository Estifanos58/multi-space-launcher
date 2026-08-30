package com.multispace.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PageIndicatorDots(
  pageCount: Int,
  currentPage: Int,
  onDotClick: ((Int) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  if (pageCount <= 1) return

  Row(
    modifier = modifier
      .wrapContentSize()
      .padding(vertical = 6.dp)
      .testTag("page_indicator_dots"),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0 until pageCount) {
      val isSelected = i == currentPage

      val width by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 6.dp,
        animationSpec = tween(durationMillis = 250),
        label = "dot_width"
      )

      val color by animateColorAsState(
        targetValue = if (isSelected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "dot_color"
      )

      Box(
        modifier = Modifier
          .height(6.dp)
          .width(width)
          .clip(CircleShape)
          .background(color)
          .then(
            if (onDotClick != null) {
              Modifier.clickable { onDotClick(i) }
            } else {
              Modifier
            }
          )
      )
    }
  }
}
