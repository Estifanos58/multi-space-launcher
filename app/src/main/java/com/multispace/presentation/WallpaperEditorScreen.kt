package com.multispace.presentation

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.ui.components.ModernCard
import com.multispace.ui.components.ModernSectionHeader
import com.multispace.ui.theme.*

/**
 * Dedicated Wallpaper Editor and Live Launcher Preview Screen.
 * Modernized with obsidian glassmorphic visuals, phone preview frame, and tactile sliders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperEditorScreen(
  targetTitle: String,
  initialImageUri: String?,
  apps: List<DiscoveredApp>,
  gridColumns: Int,
  spaceName: String,
  dockCapacity: Int,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onPickNewImage: () -> Unit,
  initialScaleMode: String = "crop",
  initialZoomLevel: Float = 1.0f,
  initialDimLevel: Float = 0.20f,
  initialOffsetX: Float = 0.0f,
  initialOffsetY: Float = 0.0f,
  onApply: (imageUri: String, scaleMode: String, zoomLevel: Float, dimLevel: Float, offsetX: Float, offsetY: Float) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var currentImageUri by remember { mutableStateOf(initialImageUri) }
  var scaleMode by remember { mutableStateOf(initialScaleMode) } // "crop" or "fit"
  var zoomLevel by remember { mutableFloatStateOf(initialZoomLevel.coerceIn(1.0f, 3.0f)) }
  var dimLevel by remember { mutableFloatStateOf(initialDimLevel.coerceIn(0.0f, 0.8f)) }
  var offsetX by remember { mutableFloatStateOf(initialOffsetX) }
  var offsetY by remember { mutableFloatStateOf(initialOffsetY) }
  var showLiveLauncherOverlay by remember { mutableStateOf(true) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Wallpaper Studio",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = targetTitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onCancel,
            modifier = Modifier.testTag("btn_wallpaper_editor_back")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Cancel",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          FilledTonalButton(
            onClick = onPickNewImage,
            shape = ShapeRoundMd,
            contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing6),
            modifier = Modifier.padding(end = AppDimens.Spacing8).testTag("btn_change_photo_in_editor")
          ) {
            Icon(
              imageVector = Icons.Default.PhotoLibrary,
              contentDescription = null,
              modifier = Modifier.size(AppDimens.IconSm)
            )
            Spacer(modifier = Modifier.width(AppDimens.Spacing6))
            Text("Select Photo", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.Spacing16),
          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onCancel,
            shape = ShapeRoundMd,
            modifier = Modifier
              .weight(1f)
              .height(AppDimens.ButtonHeight)
              .testTag("btn_cancel_wallpaper_editor")
          ) {
            Text("Discard", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
          }

          Button(
            onClick = {
              if (currentImageUri != null) {
                onApply(currentImageUri!!, scaleMode, zoomLevel, dimLevel, offsetX, offsetY)
              } else {
                onCancel()
              }
            },
            shape = ShapeRoundMd,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .weight(1.5f)
              .height(AppDimens.ButtonHeight)
              .testTag("btn_apply_wallpaper_editor")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(AppDimens.IconSm)
            )
            Spacer(modifier = Modifier.width(AppDimens.Spacing6))
            Text("Apply Wallpaper", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(bottom = AppDimens.Spacing24)
    ) {
      // 1. Phone Canvas Live Preview with interactive pinch-to-zoom and pan
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(390.dp)
          .padding(AppDimens.Spacing16),
        contentAlignment = Alignment.Center
      ) {
        // Phone Bezel Frame
        Surface(
          modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .clip(ShapeRoundLg)
            .border(3.dp, QuantumViolet.copy(alpha = 0.6f), ShapeRoundLg),
          color = Color.Black
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                  zoomLevel = (zoomLevel * zoom).coerceIn(1.0f, 3.0f)
                  offsetX = (offsetX + pan.x).coerceIn(-400f, 400f)
                  offsetY = (offsetY + pan.y).coerceIn(-400f, 400f)
                }
              }
          ) {
            // Underneath Wallpaper
            if (currentImageUri != null) {
              AsyncImage(
                model = ImageRequest.Builder(context)
                  .data(Uri.parse(currentImageUri))
                  .crossfade(true)
                  .build(),
                contentDescription = "Wallpaper Preview",
                contentScale = if (scaleMode == "crop") ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer {
                    scaleX = zoomLevel
                    scaleY = zoomLevel
                    translationX = offsetX
                    translationY = offsetY
                  }
              )
            } else {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "No Image Selected",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp
                )
              }
            }

            // Darkening Scrim
            if (dimLevel > 0f) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color.Black.copy(alpha = dimLevel))
              )
            }

            // Live Launcher Overlay
            if (showLiveLauncherOverlay) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing10),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                // Top Status & Header
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "09:41",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
                      Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                      )
                      Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(AppDimens.Spacing8))

                  // Space Title Pill
                  Surface(
                    shape = ShapeRoundSm,
                    color = Color.Black.copy(alpha = 0.5f)
                  ) {
                    Text(
                      text = spaceName.ifBlank { "Home Space" },
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing2)
                    )
                  }
                }

                // Grid of Sample / Real Apps
                val previewApps = apps.take(gridColumns * 3)
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing6)
                ) {
                  val rows = previewApps.chunked(gridColumns)
                  rows.forEach { rowApps ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                      rowApps.forEach { app ->
                        Column(
                          horizontalAlignment = Alignment.CenterHorizontally,
                          modifier = Modifier.width(36.dp)
                        ) {
                          val bmp = getBitmap(app)
                          if (bmp != null) {
                            Image(
                              bitmap = bmp.asImageBitmap(),
                              contentDescription = null,
                              modifier = Modifier
                                .size(24.dp)
                                .clip(ShapeRoundXs)
                            )
                          } else {
                            Box(
                              modifier = Modifier
                                .size(24.dp)
                                .clip(ShapeRoundXs)
                                .background(Color.White.copy(alpha = 0.8f)),
                              contentAlignment = Alignment.Center
                            ) {
                              Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = QuantumViolet,
                                modifier = Modifier.size(14.dp)
                              )
                            }
                          }
                          Text(
                            text = app.label,
                            fontSize = 7.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                          )
                        }
                      }
                    }
                  }
                }

                // Bottom Dock Mockup
                Surface(
                  shape = ShapeRoundMd,
                  color = Color.Black.copy(alpha = 0.5f),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing4, vertical = AppDimens.Spacing2)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = AppDimens.Spacing4, horizontal = AppDimens.Spacing6),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    val dockApps = apps.take(dockCapacity.coerceAtMost(5))
                    dockApps.forEach { app ->
                      val bmp = getBitmap(app)
                      if (bmp != null) {
                        Image(
                          bitmap = bmp.asImageBitmap(),
                          contentDescription = null,
                          modifier = Modifier
                            .size(20.dp)
                            .clip(ShapeRoundXs)
                        )
                      } else {
                        Box(
                          modifier = Modifier
                            .size(20.dp)
                            .clip(ShapeRoundXs)
                            .background(Color.White.copy(alpha = 0.8f))
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 2. Editor Controls Section
      ModernCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = AppDimens.Spacing16),
        shape = ShapeRoundLg
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.Spacing16),
          verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            ModernSectionHeader(
              title = "Frame & Presentation",
              subtitle = "Configure viewport scale and fit mode"
            )

            TextButton(
              onClick = {
                zoomLevel = 1.0f
                dimLevel = 0.20f
                offsetX = 0f
                offsetY = 0f
                scaleMode = "crop"
              }
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = QuantumViolet, modifier = Modifier.size(AppDimens.IconSm))
              Spacer(modifier = Modifier.width(AppDimens.Spacing4))
              Text("Reset", color = QuantumViolet, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
          }

          // Scaling Mode Choice: Crop/Fill vs Fit
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            val modes = listOf(
              "crop" to ("Crop / Fill" to Icons.Default.CropFree),
              "fit" to ("Fit to Screen" to Icons.Default.FitScreen)
            )

            modes.forEach { (modeKey, meta) ->
              val isSelected = scaleMode == modeKey
              Surface(
                shape = ShapeRoundMd,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                  if (isSelected) AppDimens.BorderThick else AppDimens.BorderThin,
                  if (isSelected) QuantumViolet else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                  .weight(1f)
                  .clickable { scaleMode = modeKey }
                  .testTag("btn_wallpaper_mode_$modeKey")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.Spacing10, horizontal = AppDimens.Spacing8),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = meta.second,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(AppDimens.IconSm)
                  )
                  Spacer(modifier = Modifier.width(AppDimens.Spacing6))
                  Text(
                    text = meta.first,
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          Text(
            text = "Tip: Pinch or drag the wallpaper preview above to freely position and frame your image.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // Zoom / Scale Slider
          Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Zoom Level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
              Text("${(zoomLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = QuantumViolet)
            }
            Slider(
              value = zoomLevel,
              onValueChange = { zoomLevel = it },
              valueRange = 1.0f..3.0f,
              colors = SliderDefaults.colors(
                thumbColor = QuantumViolet,
                activeTrackColor = QuantumViolet,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier.testTag("slider_wallpaper_zoom")
            )
          }

          // Darkness / Scrim Slider
          Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Contrast Scrim (Dimming)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
              Text("${(dimLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = QuantumViolet)
            }
            Slider(
              value = dimLevel,
              onValueChange = { dimLevel = it },
              valueRange = 0.0f..0.8f,
              colors = SliderDefaults.colors(
                thumbColor = QuantumViolet,
                activeTrackColor = QuantumViolet,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier.testTag("slider_wallpaper_dim")
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // Toggle Live Launcher Preview Overlay
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Show Live Launcher UI",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Overlay mock apps, status bar, and dock to verify icon readability.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = showLiveLauncherOverlay,
              onCheckedChange = { showLiveLauncherOverlay = it },
              modifier = Modifier.testTag("switch_live_launcher_overlay")
            )
          }
        }
      }
    }
  }
}
