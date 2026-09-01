package com.multispace.presentation

import android.net.Uri
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
import com.multispace.ui.theme.*

/**
 * Dedicated Wallpaper Editor and Live Launcher Preview Screen.
 *
 * Workflow:
 * 1. Select / inspect image
 * 2. Configure display mode: Crop/Fill (ContentScale.Crop) vs Fit (ContentScale.Fit)
 * 3. Adjust Zoom, Position Offset (Pan & Pinch), and Scrim / Darkness overlay
 * 4. Toggle Live Launcher Preview overlay with apps, dock, and search/time widget
 * 5. Confirm to persist or Cancel to discard changes
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
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Wallpaper Editor",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = targetTitle,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.8f)
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
              tint = Color.White
            )
          }
        },
        actions = {
          TextButton(
            onClick = onPickNewImage,
            modifier = Modifier.testTag("btn_change_photo_in_editor")
          ) {
            Icon(
              imageVector = Icons.Default.PhotoLibrary,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Change Photo", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1B4B))
      )
    },
    bottomBar = {
      Surface(
        color = Color(0xFF1E1B4B),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_cancel_wallpaper_editor")
          ) {
            Text("Cancel")
          }

          Button(
            onClick = {
              if (currentImageUri != null) {
                onApply(currentImageUri!!, scaleMode, zoomLevel, dimLevel, offsetX, offsetY)
              } else {
                onCancel()
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            modifier = Modifier
              .weight(1.5f)
              .testTag("btn_apply_wallpaper_editor")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Wallpaper", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F172A))
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
    ) {
      // 1. Phone Canvas Live Preview with interactive pinch-to-zoom and pan
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(390.dp)
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        // Phone Bezel Frame
        Surface(
          modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .border(3.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
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
                  .background(PrimaryPurpleDark),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "No Image Selected",
                  color = Color.White.copy(alpha = 0.7f),
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
                  .padding(horizontal = 8.dp, vertical = 10.dp),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

                  Spacer(modifier = Modifier.height(8.dp))

                  // Space Title Pill
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                  ) {
                    Text(
                      text = spaceName.ifBlank { "Home Space" },
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }

                // Grid of Sample / Real Apps
                val previewApps = apps.take(gridColumns * 3)
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                .clip(RoundedCornerShape(6.dp))
                            )
                          } else {
                            Box(
                              modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.8f)),
                              contentAlignment = Alignment.Center
                            ) {
                              Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = Color(0xFF1E1B4B),
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
                  shape = RoundedCornerShape(14.dp),
                  color = Color.Black.copy(alpha = 0.5f),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp, horizontal = 6.dp),
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
                            .clip(RoundedCornerShape(5.dp))
                        )
                      } else {
                        Box(
                          modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
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
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Scaling & Presentation Mode",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White
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
              Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryPurpleLight, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reset", color = PrimaryPurpleLight, fontSize = 12.sp)
            }
          }

          // Scaling Mode Choice: Crop/Fill vs Fit
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val modes = listOf(
              "crop" to ("Crop / Fill" to Icons.Default.CropFree),
              "fit" to ("Fit to Screen" to Icons.Default.FitScreen)
            )

            modes.forEach { (modeKey, meta) ->
              val isSelected = scaleMode == modeKey
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) PrimaryPurple else Color.White.copy(alpha = 0.1f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
                modifier = Modifier
                  .weight(1f)
                  .clickable { scaleMode = modeKey }
                  .testTag("btn_wallpaper_mode_$modeKey")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = meta.second,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = meta.first,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White
                  )
                }
              }
            }
          }

          Text(
            text = "Tip: Pinch or drag the wallpaper preview above to freely position and frame your image.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f)
          )

          Divider(color = Color.White.copy(alpha = 0.15f))

          // Zoom / Scale Slider
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Zoom Level", fontSize = 12.sp, color = Color.White)
              Text("${(zoomLevel * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Slider(
              value = zoomLevel,
              onValueChange = { zoomLevel = it },
              valueRange = 1.0f..3.0f,
              colors = SliderDefaults.colors(
                thumbColor = PrimaryPurple,
                activeTrackColor = PrimaryPurpleLight,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
              ),
              modifier = Modifier.testTag("slider_wallpaper_zoom")
            )
          }

          // Darkness / Scrim Slider
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Contrast Scrim (Dimming)", fontSize = 12.sp, color = Color.White)
              Text("${(dimLevel * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Slider(
              value = dimLevel,
              onValueChange = { dimLevel = it },
              valueRange = 0.0f..0.8f,
              colors = SliderDefaults.colors(
                thumbColor = PrimaryPurple,
                activeTrackColor = PrimaryPurpleLight,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
              ),
              modifier = Modifier.testTag("slider_wallpaper_dim")
            )
          }

          Divider(color = Color.White.copy(alpha = 0.15f))

          // Toggle Live Launcher Preview Overlay
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Show Live Launcher UI",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
              )
              Text(
                text = "Overlay mock apps, status bar, and dock to verify icon readability.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f)
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
