package com.multispace.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.PageTurnEffect
import com.multispace.domain.model.Space
import com.multispace.presentation.pageTurnEffect
import com.multispace.ui.components.ModernCard
import com.multispace.ui.components.ModernSectionHeader
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Configuration and isolated live preview component for Layer 1 Page Turn Effects.
 */
@Composable
fun PageTurnEffectSection(
  selectedEffect: PageTurnEffect,
  onEffectSelected: (PageTurnEffect) -> Unit,
  durationMs: Int,
  onDurationChange: (Int) -> Unit,
  intensity: Float,
  onIntensityChange: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var isDropdownExpanded by remember { mutableStateOf(false) }
  var showAdvancedSettings by remember { mutableStateOf(false) }
  val previewPagerState = rememberPagerState { 2 }
  val coroutineScope = rememberCoroutineScope()

  ModernCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("section_page_turn_effect")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      ModernSectionHeader(
        title = "Page Turn Effect",
        subtitle = "Horizontal page transition style for this Space"
      )

      // 1. Selector Bar (Compact Dropdown Toggle)
      Surface(
        onClick = { isDropdownExpanded = !isDropdownExpanded },
        shape = ShapeRoundMd,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
          width = 1.dp,
          color = if (isDropdownExpanded) QuantumViolet else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("toggle_page_turn_dropdown")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(QuantumViolet.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = getEffectIcon(selectedEffect),
                contentDescription = null,
                tint = QuantumViolet,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = selectedEffect.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = selectedEffect.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Icon(
            imageVector = if (isDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isDropdownExpanded) "Collapse" else "Expand",
            tint = QuantumViolet
          )
        }
      }

      // Compact Selection List (Animated Collapse / Expand)
      AnimatedVisibility(
        visible = isDropdownExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRoundMd)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), ShapeRoundMd)
            .padding(vertical = 4.dp)
            .testTag("list_page_turn_options")
        ) {
          PageTurnEffect.entries.forEach { effect ->
            val isSelected = effect == selectedEffect
            Surface(
              onClick = {
                onEffectSelected(effect)
                isDropdownExpanded = false
              },
              color = if (isSelected) QuantumViolet.copy(alpha = 0.12f) else Color.Transparent,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("option_page_turn_${effect.name.lowercase(Locale.US)}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Icon(
                    imageVector = getEffectIcon(effect),
                    contentDescription = null,
                    tint = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                  Column {
                    Text(
                      text = effect.displayName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = effect.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                  }
                }

                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = QuantumViolet,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }

      // 2. Isolated Live Preview
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(ShapeRoundMd)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeRoundMd)
          .padding(12.dp)
          .testTag("preview_page_turn_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = null,
              tint = QuantumViolet,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Live Preview",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          FilledTonalButton(
            onClick = {
              coroutineScope.launch {
                val nextPage = if (previewPagerState.currentPage == 0) 1 else 0
                previewPagerState.animateScrollToPage(
                  page = nextPage,
                  animationSpec = tween(durationMs)
                )
              }
            },
            shape = ShapeRoundSm,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
              .height(32.dp)
              .testTag("btn_preview_transition")
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (previewPagerState.currentPage == 0) "Turn Page" else "Turn Back",
              style = MaterialTheme.typography.labelSmall
            )
          }
        }

        // Mini Viewport Box with HorizontalPager applying the effect
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, QuantumViolet.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          HorizontalPager(
            state = previewPagerState,
            userScrollEnabled = true,
            modifier = Modifier
              .fillMaxSize()
              .testTag("preview_horizontal_pager")
          ) { page ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .pageTurnEffect(
                  pagerState = previewPagerState,
                  page = page,
                  effect = selectedEffect,
                  intensity = intensity
                )
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              MiniPageMockContent(pageIndex = page)
            }
          }

          // Mini Page Dots
          Row(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            repeat(2) { idx ->
              val active = idx == previewPagerState.currentPage
              Box(
                modifier = Modifier
                  .size(if (active) 6.dp else 4.dp)
                  .clip(CircleShape)
                  .background(if (active) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
              )
            }
          }
        }
      }

      // 3. Advanced Settings (Expandable)
      Surface(
        onClick = { showAdvancedSettings = !showAdvancedSettings },
        shape = ShapeRoundSm,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = QuantumViolet,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Advanced Transition Settings",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Icon(
            imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      AnimatedVisibility(
        visible = showAdvancedSettings,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRoundMd)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp)
            .testTag("section_advanced_page_turn_settings"),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Duration Slider
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Transition Speed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${durationMs} ms",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = QuantumViolet
              )
            }
            Slider(
              value = durationMs.toFloat(),
              onValueChange = { onDurationChange(it.toInt()) },
              valueRange = Space.MIN_PAGE_TURN_DURATION_MS.toFloat()..Space.MAX_PAGE_TURN_DURATION_MS.toFloat(),
              steps = 12,
              colors = SliderDefaults.colors(
                thumbColor = QuantumViolet,
                activeTrackColor = QuantumViolet
              ),
              modifier = Modifier.testTag("slider_page_turn_duration")
            )
          }

          // Intensity Slider
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Effect Intensity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = String.format(Locale.US, "%.1fx", intensity),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = QuantumViolet
              )
            }
            Slider(
              value = intensity,
              onValueChange = onIntensityChange,
              valueRange = Space.MIN_PAGE_TURN_INTENSITY..Space.MAX_PAGE_TURN_INTENSITY,
              steps = 14,
              colors = SliderDefaults.colors(
                thumbColor = QuantumViolet,
                activeTrackColor = QuantumViolet
              ),
              modifier = Modifier.testTag("slider_page_turn_intensity")
            )
          }

          // Reset Defaults Button
          TextButton(
            onClick = {
              onDurationChange(Space.DEFAULT_PAGE_TURN_DURATION_MS)
              onIntensityChange(Space.DEFAULT_PAGE_TURN_INTENSITY)
            },
            modifier = Modifier.align(Alignment.End)
          ) {
            Icon(
              imageVector = Icons.Default.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset to Defaults", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}

/**
 * Mock representation of mini app grid for live preview.
 */
@Composable
private fun MiniPageMockContent(pageIndex: Int) {
  val mockApps = if (pageIndex == 0) {
    listOf(
      "Mail" to Color(0xFFE53935),
      "Camera" to Color(0xFF1E88E5),
      "Music" to Color(0xFF43A047),
      "Chat" to Color(0xFFFB8C00)
    )
  } else {
    listOf(
      "Web" to Color(0xFF8E24AA),
      "Notes" to Color(0xFF00ACC1),
      "Photos" to Color(0xFFF4511E),
      "Settings" to Color(0xFF546E7A)
    )
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    mockApps.forEach { (name, color) ->
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.75f)))),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = name.take(1),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
        Text(
          text = name,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

private fun getEffectIcon(effect: PageTurnEffect): ImageVector = when (effect) {
  PageTurnEffect.NORMAL -> Icons.Default.Swipe
  PageTurnEffect.CUBE -> Icons.Default.ViewInAr
  PageTurnEffect.WINDMILL -> Icons.Default.RotateRight
  PageTurnEffect.CROSSFADE -> Icons.Default.BlurOn
  PageTurnEffect.ZOOM -> Icons.Default.ZoomIn
}
