package com.multispace.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ModernDarkColorScheme =
  darkColorScheme(
    primary = QuantumVioletLight,
    onPrimary = QuantumVioletDark,
    primaryContainer = QuantumVioletDark,
    onPrimaryContainer = QuantumVioletGlow,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    secondaryContainer = CyberCyanDark,
    onSecondaryContainer = CyberCyanLight,
    tertiary = EmeraldCore,
    onTertiary = Color.Black,
    tertiaryContainer = EmeraldCoreDark,
    onTertiaryContainer = EmeraldCoreLight,
    background = ObsidianBase,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceHighlight,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
    error = CrimsonNova,
    onError = Color.White,
    errorContainer = CrimsonNovaDark,
    onErrorContainer = CrimsonNovaLight
  )

private val ModernLightColorScheme =
  lightColorScheme(
    primary = QuantumViolet,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerBadge,
    onPrimaryContainer = QuantumVioletDark,
    secondary = CyberCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = CyberCyanDark,
    tertiary = EmeraldCoreDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = EmeraldCoreDark,
    background = CanvasLightBase,
    onBackground = TextPrimaryLight,
    surface = CanvasLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CanvasLightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = CanvasLightSurfaceElevated,
    surfaceContainerLow = CanvasLightSurface,
    surfaceContainerHigh = CanvasLightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = CanvasLightBorder,
    outlineVariant = CanvasLightBorderSubtle,
    error = CrimsonNova,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = CrimsonNovaDark
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> ModernDarkColorScheme
      else -> ModernLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content
  )
}
