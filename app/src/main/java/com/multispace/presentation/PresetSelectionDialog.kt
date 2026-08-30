package com.multispace.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.multispace.domain.model.LayoutPreset
import com.multispace.domain.model.Space

@Composable
fun PresetSelectionDialog(
  currentPresetId: String,
  onPresetSelected: (LayoutPreset) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedPreset by remember {
    mutableStateOf(LayoutPreset.getById(currentPresetId))
  }
  var showConfirmDialog by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.85f)
        .clip(RoundedCornerShape(24.dp))
        .testTag("preset_selection_dialog"),
      color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Layout Presets",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Select a design paradigm for this Space",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset List
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(LayoutPreset.ALL_PRESETS, key = { it.id }) { preset ->
            val isSelected = selectedPreset.id == preset.id
            LayoutPresetVisualCard(
              preset = preset,
              isSelected = isSelected,
              onSelect = { selectedPreset = preset }
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.testTag("apply_preset_button")
          ) {
            Text("Apply Preset")
          }
        }
      }
    }
  }

  // Confirmation Alert Dialog
  if (showConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Warning",
          tint = MaterialTheme.colorScheme.primary
        )
      },
      title = { Text("Apply '${selectedPreset.name}'?") },
      text = {
        Text("Applying this preset will reorganize Home pages and Dock slots according to the ${selectedPreset.name} paradigm. Space memberships and applications will remain intact.")
      },
      confirmButton = {
        Button(
          onClick = {
            showConfirmDialog = false
            onPresetSelected(selectedPreset)
            onDismiss()
          }
        ) {
          Text("Confirm & Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun AttributeTag(label: String, isSelected: Boolean) {
  Surface(
    color = if (isSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(4.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontSize = 10.sp,
      color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}
