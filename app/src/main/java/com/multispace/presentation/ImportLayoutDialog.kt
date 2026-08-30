package com.multispace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.multispace.domain.model.ImportReport

@Composable
fun ImportLayoutDialog(
  report: ImportReport?,
  isImporting: Boolean,
  onStartImport: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.82f)
        .clip(RoundedCornerShape(24.dp))
        .testTag("import_layout_dialog"),
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
              text = "Import Android Layout",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Detect and migrate home configuration",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isImporting) {
          Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator()
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Scanning installed applications & launcher configuration...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else if (report == null) {
          // Pre-import explanation
          Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              modifier = Modifier.size(64.dp),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Automatic System Migration",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Multi-Space will detect your default apps (Dialer, Messaging, Browser, Camera, Settings), automatically populate the persistent bottom Dock, and organize all launchable applications onto Home pages.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
              onClick = onStartImport,
              modifier = Modifier.fillMaxWidth(0.8f).testTag("start_import_button")
            ) {
              Text("Start Layout Import")
            }
          }
        } else {
          // Post-import Report View
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            item {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "Import Summary",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = report.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }
            }

            // Success items
            if (report.successItems.isNotEmpty()) {
              item {
                SectionHeader(title = "Successfully Configured", count = report.successItems.size)
              }
              items(report.successItems) { item ->
                ReportItemRow(
                  icon = Icons.Default.CheckCircle,
                  iconTint = Color(0xFF2E7D32),
                  text = item
                )
              }
            }

            // Partially imported
            if (report.partiallyImportedItems.isNotEmpty()) {
              item {
                SectionHeader(title = "Approximated Layout Elements", count = report.partiallyImportedItems.size)
              }
              items(report.partiallyImportedItems) { item ->
                ReportItemRow(
                  icon = Icons.Default.Info,
                  iconTint = Color(0xFF0288D1),
                  text = item
                )
              }
            }

            // Android Sandbox / Restricted items
            if (report.restrictedItems.isNotEmpty()) {
              item {
                SectionHeader(title = "OS Sandbox Boundaries", count = report.restrictedItems.size)
              }
              items(report.restrictedItems) { item ->
                ReportItemRow(
                  icon = Icons.Default.Warning,
                  iconTint = Color(0xFFED6C02),
                  text = item
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(onClick = onDismiss, modifier = Modifier.testTag("dismiss_import_report_button")) {
              Text("Done")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(4.dp)
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
      )
    }
  }
}

@Composable
private fun ReportItemRow(
  icon: ImageVector,
  iconTint: Color,
  text: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
      .padding(8.dp),
    verticalAlignment = Alignment.Top
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.size(18.dp).padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      lineHeight = 16.sp
    )
  }
}
