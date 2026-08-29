package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryContainerBadge,
    onPrimary = PrimaryPurpleDark,
    primaryContainer = PrimaryPurple,
    onPrimaryContainer = PrimaryContainerLight,
    secondary = LightSurfaceContainerHigh,
    onSecondary = TextPrimary,
    background = DarkTerminalSurface,
    surface = DarkTerminalSurface,
    onSurface = DarkTerminalText,
    surfaceContainer = Color(0xFF2B2930),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = PrimaryPurpleDark,
    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = PrimaryContainerBadge,
    onSecondaryContainer = PrimaryPurpleDark,
    background = LightBackground,
    surface = LightSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
