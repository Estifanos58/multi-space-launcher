package com.multispace.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.PageTurnEffect
import com.multispace.domain.model.Space
import com.multispace.ui.components.ModernCard
import com.multispace.ui.theme.AmberPulse
import com.multispace.ui.theme.CyberCyan
import com.multispace.ui.theme.CosmicIndigo
import com.multispace.ui.theme.EmeraldCore
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm
import kotlinx.coroutines.launch

/**
 * Configurable Page Turn Effect selector component for Create Space / Edit Space (Basic Tab).
 * Features:
 * - Compact initial display
 * - Single-choice selector for: Normal, Cube, Windmill, Crossfade, Zoom
 * - Live interactive preview of the selected effect
 * - Collapsible Advanced Settings panel (duration, intensity, reset)
 */
@Composable
fun PageTurnEffectConfigSection(
  selectedEffect: String,
  onEffectChange: (String) -> Unit,
  durationMs: Int,
  onDurationChange: (Int) -> Unit,
  intensity: Float,
  onIntensityChange: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by rememberSaveable { mutableStateOf(false) }
  var isAdvancedExpanded by rememberSaveable { mutableStateOf(false) }

  val currentEffect: PageTurnEffect = remember(selectedEffect) {
    PageTurnEffect.fromString(selectedEffect)
  }

  ModernCard(
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Header & Compact Summary Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded }
          .testTag("row_page_turn_effect_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = QuantumViolet.copy(alpha = 0.14f),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.ViewCarousel,
                contentDescription = null,
                tint = QuantumViolet,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Column {
            Text(
              text = "Page Turn Effect",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (isExpanded) "Select horizontal page transition" else currentEffect.displayName,
              style = MaterialTheme.typography.bodySmall,
              color = if (isExpanded) MaterialTheme.colorScheme.onSurfaceVariant else QuantumViolet,
              fontWeight = if (isExpanded) FontWeight.Normal else FontWeight.SemiBold
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = ShapeRoundSm,
            color = QuantumViolet.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, QuantumViolet.copy(alpha = 0.25f))
          ) {
            Text(
              text = currentEffect.displayName,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = QuantumViolet,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 2. Expanded Options & Live Preview & Advanced Settings
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // List of effects (single-choice list)
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            PageTurnEffect.entries.forEach { effect ->
              val isSelected = currentEffect == effect
              Surface(
                shape = ShapeRoundMd,
                color = if (isSelected) QuantumViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(ShapeRoundMd)
                  .clickable { onEffectChange(effect.name) }
                  .testTag("effect_option_${effect.name}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = effect.displayName,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 14.sp,
                      color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = effect.description,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  if (isSelected) {
                    Surface(
                      shape = CircleShape,
                      color = QuantumViolet,
                      modifier = Modifier.size(24.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = "Selected",
                          tint = Color.White,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // 3. Live Preview Card
          LivePageTurnPreview(
            effect = currentEffect,
            intensity = intensity,
            durationMs = durationMs
          )

          // 4. Advanced Settings Section
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(ShapeRoundSm)
              .clickable { isAdvancedExpanded = !isAdvancedExpanded }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Advanced Settings",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Icon(
              imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          AnimatedVisibility(
            visible = isAdvancedExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                  shape = ShapeRoundMd
                )
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              // Duration Setting
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Transition Duration",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "${durationMs} ms",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                Slider(
                  value = durationMs.toFloat(),
                  onValueChange = { onDurationChange(it.toInt()) },
                  valueRange = 150f..600f,
                  steps = 17, // step by 25ms
                  colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                  ),
                  modifier = Modifier.testTag("slider_page_turn_duration")
                )
              }

              // Intensity Setting (applies to Cube, Windmill, Zoom)
              if (currentEffect != PageTurnEffect.NORMAL && currentEffect != PageTurnEffect.CROSSFADE) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  val intensityLabel = when (currentEffect) {
                    PageTurnEffect.CUBE -> "3D Perspective / Max Angle: ${(intensity * 90).toInt()}°"
                    PageTurnEffect.WINDMILL -> "Pivot Rotation Angle: ${(intensity * 45).toInt()}°"
                    PageTurnEffect.ZOOM -> "Scale Depth: ${((1f - 0.30f * intensity) * 100).toInt()}%"
                    else -> "Effect Intensity"
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Effect Intensity",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "${String.format("%.1f", intensity)}x",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                  Text(
                    text = intensityLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Slider(
                    value = intensity,
                    onValueChange = { onIntensityChange(it) },
                    valueRange = 0.6f..1.4f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                      thumbColor = MaterialTheme.colorScheme.primary,
                      activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("slider_page_turn_intensity")
                  )
                }
              }

              // Reset to defaults
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(
                  onClick = {
                    onDurationChange(Space.DEFAULT_PAGE_TURN_DURATION_MS)
                    onIntensityChange(Space.DEFAULT_PAGE_TURN_INTENSITY)
                  },
                  modifier = Modifier.testTag("btn_reset_page_turn_defaults")
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Reset Defaults", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Live interactive preview demonstrating the exact page turn effect using two representative pages.
 */
@Composable
private fun LivePageTurnPreview(
  effect: PageTurnEffect,
  intensity: Float,
  durationMs: Int,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current.density
  val animProgress = remember { Animatable(0f) }
  var isAutoPlaying by rememberSaveable { mutableStateOf(true) }
  var manualProgress by remember { mutableFloatStateOf(0f) }
  val coroutineScope = rememberCoroutineScope()

  // Run continuous back-and-forth demo when auto-playing
  LaunchedEffect(isAutoPlaying, effect, durationMs) {
    if (isAutoPlaying) {
      while (true) {
        animProgress.animateTo(
          targetValue = 1f,
          animationSpec = tween(
            durationMillis = durationMs.coerceAtLeast(200),
            easing = FastOutSlowInEasing
          )
        )
        kotlinx.coroutines.delay(400)
        animProgress.animateTo(
          targetValue = 0f,
          animationSpec = tween(
            durationMillis = durationMs.coerceAtLeast(200),
            easing = FastOutSlowInEasing
          )
        )
        kotlinx.coroutines.delay(400)
      }
    }
  }

  val activeProgress = if (isAutoPlaying) animProgress.value else manualProgress

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF0F1117), ShapeRoundMd)
      .border(BorderStroke(1.dp, Color(0xFF262A36)), ShapeRoundMd)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Bar of Preview
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = EmeraldCore.copy(alpha = 0.15f),
          modifier = Modifier.size(8.dp)
        ) {}
        Text(
          text = "Live Preview: ${effect.displayName}",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFE2E8F0)
        )
      }

      TextButton(
        onClick = {
          isAutoPlaying = !isAutoPlaying
          if (isAutoPlaying) {
            coroutineScope.launch { animProgress.snapTo(0f) }
          }
        },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text(
          text = if (isAutoPlaying) "Pause" else "Auto Play",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = QuantumViolet
        )
      }
    }

    // Viewport with stationary wallpaper background
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF161922))
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { isAutoPlaying = false },
            onDrag = { _, dragAmount ->
              val delta = dragAmount.x / 400f
              manualProgress = (manualProgress - delta).coerceIn(0f, 1f)
            }
          )
        }
        .testTag("preview_viewport_page_turn"),
      contentAlignment = Alignment.Center
    ) {
      // Stationary subtle grid / wallpaper lines
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0x15FFFFFF))
      )

      // Page 0 (Current outgoing page as activeProgress goes 0 -> 1)
      // Its offset is: (0 - activeProgress) = -activeProgress
      val page0Offset = -activeProgress
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            PageTurnEffectHelper.applyPageTurnEffect(
              scope = this,
              pageOffset = page0Offset,
              effect = effect,
              intensity = intensity,
              density = density
            )
          }
      ) {
        MiniPageContent(
          pageLabel = "Page 1",
          iconColors = listOf(QuantumViolet, CosmicIndigo, EmeraldCore, AmberPulse)
        )
      }

      // Page 1 (Incoming page as activeProgress goes 0 -> 1)
      // Its offset is: (1 - activeProgress)
      val page1Offset = 1f - activeProgress
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            PageTurnEffectHelper.applyPageTurnEffect(
              scope = this,
              pageOffset = page1Offset,
              effect = effect,
              intensity = intensity,
              density = density
            )
          }
      ) {
        MiniPageContent(
          pageLabel = "Page 2",
          iconColors = listOf(CyberCyan, Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF8B5CF6))
        )
      }
    }

    // Scrub slider for interactive testing
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "Drag to scrub:",
        fontSize = 11.sp,
        color = Color(0xFF94A3B8)
      )
      Slider(
        value = activeProgress,
        onValueChange = {
          isAutoPlaying = false
          manualProgress = it
        },
        valueRange = 0f..1f,
        colors = SliderDefaults.colors(
          thumbColor = QuantumViolet,
          activeTrackColor = QuantumViolet
        ),
        modifier = Modifier
          .weight(1f)
          .testTag("slider_scrub_preview")
      )
      Text(
        text = "${(activeProgress * 100).toInt()}%",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFE2E8F0),
        modifier = Modifier.width(36.dp)
      )
    }
  }
}

/**
 * Mock representation of a launcher page with 4 mini app icons.
 */
@Composable
private fun MiniPageContent(
  pageLabel: String,
  iconColors: List<Color>
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(10.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = pageLabel,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White.copy(alpha = 0.7f)
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      iconColors.forEach { color ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = color,
            modifier = Modifier.size(26.dp)
          ) {}
          Box(
            modifier = Modifier
              .width(18.dp)
              .height(3.dp)
              .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
          )
        }
      }
    }

    Box(modifier = Modifier.height(2.dp))
  }
}
