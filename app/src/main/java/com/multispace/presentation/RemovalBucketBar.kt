package com.multispace.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemovalBucketBar(
  isVisible: Boolean,
  onRemoveClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    modifier = modifier
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .height(52.dp)
        .clip(RoundedCornerShape(16.dp))
        .testTag("removal_bucket_bar"),
      color = MaterialTheme.colorScheme.errorContainer,
      tonalElevation = 6.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.RemoveCircleOutline,
          contentDescription = "Remove from Home",
          tint = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Remove from Home",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.width(12.dp))
        Button(
          onClick = onRemoveClicked,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp).testTag("confirm_removal_button")
        ) {
          Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
