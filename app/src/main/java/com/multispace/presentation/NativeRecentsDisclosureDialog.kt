package com.multispace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.multispace.ui.theme.*

/**
 * Google Play Store Compliant In-App Disclosure for the Optional Native Recents Accessibility Service.
 *
 * Requirements met:
 * 1. Prominently shown inside the app before opening Android Accessibility Settings.
 * 2. Explicitly explains why the service is requested (invoking Android's native Recents/Overview action).
 * 3. Explicitly details privacy scope (Zero screen content reading, zero keystroke capture, zero data collection).
 * 4. Requires affirmative user consent action ("Open Accessibility Settings") or dismiss ("Not Now").
 */
@Composable
fun NativeRecentsDisclosureDialog(
  onDismiss: () -> Unit,
  onAcceptAndOpenSettings: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 24.dp)
        .testTag("dialog_native_recents_disclosure"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = LightSurfaceContainerLow)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Header Icon
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(PrimaryPurpleLight),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.GridView,
            contentDescription = "Native Recents",
            tint = PrimaryPurpleDark,
            modifier = Modifier.size(32.dp)
          )
        }

        // Title
        Text(
          text = "Native Recent Apps Bridge",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          textAlign = TextAlign.Center
        )

        // Primary Explanation
        Text(
          text = "Multi-Space Launcher can optionally use an Accessibility Service to trigger Android's native System Recent Apps / Overview screen (task cards and thumbnails).",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )

        // Privacy & Usage Highlights Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            DisclosureBulletItem(
              icon = Icons.Default.TaskAlt,
              title = "Single Purpose",
              description = "Used solely to request Android\'s system GLOBAL_ACTION_RECENTS overview."
            )
            DisclosureBulletItem(
              icon = Icons.Default.VisibilityOff,
              title = "Zero Screen Inspection",
              description = "Does not read, inspect, or interact with your screen contents or other apps."
            )
            DisclosureBulletItem(
              icon = Icons.Default.Lock,
              title = "Zero Data Collection",
              description = "No personal data, keystrokes, or accessibility events are collected or transmitted."
            )
          }
        }

        Text(
          text = "To enable this optional bridge, tap below to open Android Accessibility Settings and toggle on 'Multi-Space Launcher'. You can disable it at any time in system settings.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          fontSize = 12.sp
        )

        // Actions
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onAcceptAndOpenSettings,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_accept_disclosure_open_settings"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleDark)
          ) {
            Text(
              text = "Open Accessibility Settings",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }

          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_dismiss_disclosure"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = "Not Now",
              color = TextSecondary,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DisclosureBulletItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = PrimaryPurpleDark,
      modifier = Modifier
        .size(20.dp)
        .padding(top = 2.dp)
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        fontSize = 11.sp
      )
    }
  }
}
