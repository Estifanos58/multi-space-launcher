package com.multispace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.CrimsonNova
import com.multispace.ui.theme.CyberCyan
import com.multispace.ui.theme.EmeraldCore
import com.multispace.ui.theme.GlassDarkBorder
import com.multispace.ui.theme.GlassDarkSurface
import com.multispace.ui.theme.GlassLightBorder
import com.multispace.ui.theme.GlassLightSurface
import com.multispace.ui.theme.ObsidianBorder
import com.multispace.ui.theme.ObsidianBorderSubtle
import com.multispace.ui.theme.ObsidianSurfaceElevated
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.QuantumVioletLight
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm

/**
 * Premium Modern Card Surface with subtle border and tonal hierarchy.
 */
@Composable
fun ModernCard(
  modifier: Modifier = Modifier,
  shape: Shape = ShapeRoundMd,
  backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  borderWidth: Dp = AppDimens.BorderThin,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit
) {
  val isDark = isSystemInDarkTheme()
  val computedBorder = if (borderColor == MaterialTheme.colorScheme.outlineVariant) {
    if (isDark) ObsidianBorderSubtle else MaterialTheme.colorScheme.outlineVariant
  } else {
    borderColor
  }

  Surface(
    modifier = modifier
      .then(
        if (onClick != null) {
          Modifier.clip(shape).clickable(role = Role.Button, onClick = onClick)
        } else {
          Modifier
        }
      ),
    shape = shape,
    color = backgroundColor,
    border = BorderStroke(borderWidth, computedBorder),
    tonalElevation = AppDimens.ElevationLow
  ) {
    content()
  }
}

/**
 * Translucent Glass Surface for overlay bars, docks, and floating panels.
 */
@Composable
fun ModernGlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = ShapeRoundLg,
  content: @Composable () -> Unit
) {
  val isDark = isSystemInDarkTheme()
  val bg = if (isDark) GlassDarkSurface else GlassLightSurface
  val border = if (isDark) GlassDarkBorder else GlassLightBorder

  Surface(
    modifier = modifier,
    shape = shape,
    color = bg,
    border = BorderStroke(AppDimens.BorderThin, border),
    tonalElevation = AppDimens.ElevationMed
  ) {
    content()
  }
}

/**
 * Standardized Modern Section Header with title, optional badge, subtitle, and action button.
 */
@Composable
fun ModernSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  badgeText: String? = null,
  badgeColor: Color = MaterialTheme.colorScheme.primary,
  icon: ImageVector? = null,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = AppDimens.Spacing8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f, fill = false)
    ) {
      if (icon != null) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(ShapeRoundSm)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppDimens.IconSm)
          )
        }
        Spacer(modifier = Modifier.width(AppDimens.Spacing10))
      }

      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          if (badgeText != null) {
            Spacer(modifier = Modifier.width(AppDimens.Spacing8))
            ModernStatusBadge(text = badgeText, color = badgeColor)
          }
        }
        if (subtitle != null) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    if (actionText != null && onActionClick != null) {
      TextButton(
        onClick = onActionClick,
        modifier = Modifier.height(AppDimens.ButtonHeightSm)
      ) {
        Text(
          text = actionText,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

/**
 * Standardized Status Badge Chip with pulse indicator dot.
 */
@Composable
fun ModernStatusBadge(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary
) {
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = color.copy(alpha = 0.14f),
    border = BorderStroke(AppDimens.BorderThin, color.copy(alpha = 0.35f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(color)
      )
      Spacer(modifier = Modifier.width(AppDimens.Spacing6))
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1
      )
    }
  }
}

/**
 * Standard Modern Search Field with clear action and glowing container.
 */
@Composable
fun ModernSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector = Icons.Default.Search,
  onClear: (() -> Unit)? = null
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = {
      Text(
        text = placeholder,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    },
    leadingIcon = {
      Icon(
        imageVector = leadingIcon,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(AppDimens.IconMd)
      )
    },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(
          onClick = {
            onQueryChange("")
            onClear?.invoke()
          }
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppDimens.IconSm)
          )
        }
      }
    },
    singleLine = true,
    shape = ShapeRoundLg,
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    ),
    textStyle = MaterialTheme.typography.bodyMedium,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
  )
}

/**
 * Standardized Modal Dialog Container ensuring consistent padding, shape, title, and buttons.
 */
@Composable
fun ModernDialogContainer(
  title: String,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: ImageVector? = null,
  iconTint: Color = MaterialTheme.colorScheme.primary,
  confirmButtonText: String? = "Confirm",
  onConfirm: (() -> Unit)? = null,
  confirmButtonColor: Color = MaterialTheme.colorScheme.primary,
  dismissButtonText: String? = "Cancel",
  isConfirmEnabled: Boolean = true,
  content: @Composable () -> Unit
) {
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = modifier
        .widthIn(min = 280.dp, max = 440.dp)
        .fillMaxWidth(0.92f)
        .clip(ShapeRoundLg),
      shape = ShapeRoundLg,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
      tonalElevation = AppDimens.ElevationHigh
    ) {
      Column(
        modifier = Modifier.padding(AppDimens.Spacing24)
      ) {
        // Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (icon != null) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(ShapeRoundMd)
                .background(iconTint.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(AppDimens.IconMd)
              )
            }
            Spacer(modifier = Modifier.width(AppDimens.Spacing16))
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
              Spacer(modifier = Modifier.height(AppDimens.Spacing4))
              Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(AppDimens.Spacing20))

        // Body Content
        Box(modifier = Modifier.weight(1f, fill = false)) {
          content()
        }

        Spacer(modifier = Modifier.height(AppDimens.Spacing24))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (dismissButtonText != null) {
            TextButton(
              onClick = onDismissRequest,
              shape = ShapeRoundMd,
              modifier = Modifier.height(AppDimens.ButtonHeight)
            ) {
              Text(
                text = dismissButtonText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(AppDimens.Spacing12))
          }

          if (confirmButtonText != null && onConfirm != null) {
            Button(
              onClick = onConfirm,
              enabled = isConfirmEnabled,
              shape = ShapeRoundMd,
              colors = ButtonDefaults.buttonColors(
                containerColor = confirmButtonColor,
                contentColor = Color.White
              ),
              modifier = Modifier.height(AppDimens.ButtonHeight)
            ) {
              Text(
                text = confirmButtonText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Premium Modern Empty State with glowing icon container.
 */
@Composable
fun ModernEmptyState(
  icon: ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(AppDimens.Spacing32),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        .border(AppDimens.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(AppDimens.IconLg)
      )
    }

    Spacer(modifier = Modifier.height(AppDimens.Spacing16))

    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(AppDimens.Spacing8))

    Text(
      text = description,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 280.dp)
    )

    if (actionText != null && onActionClick != null) {
      Spacer(modifier = Modifier.height(AppDimens.Spacing20))
      Button(
        onClick = onActionClick,
        shape = ShapeRoundMd,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.height(AppDimens.ButtonHeightSm)
      ) {
        Text(
          text = actionText,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

/**
 * Standard Loading State with pulsing indicator.
 */
@Composable
fun ModernLoadingState(
  modifier: Modifier = Modifier,
  message: String? = "Loading..."
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(AppDimens.Spacing32),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    CircularProgressIndicator(
      color = MaterialTheme.colorScheme.primary,
      strokeWidth = 3.dp,
      modifier = Modifier.size(40.dp)
    )
    if (message != null) {
      Spacer(modifier = Modifier.height(AppDimens.Spacing16))
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
