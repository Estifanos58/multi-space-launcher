package com.multispace.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space

/**
 * Palette specification for space application icon theming.
 */
data class AppThemePalette(
  val id: String,
  val name: String,
  val description: String,
  val primaryColor: Color,
  val secondaryColor: Color,
  val iconBackgroundColor: Color,
  val labelColor: Color = Color.White
)

object AppThemeHelper {
  val PALETTES = listOf(
    AppThemePalette(
      id = Space.THEME_DEFAULT,
      name = "Default Material",
      description = "Standard colorful Android app icons",
      primaryColor = Color(0xFF6750A4),
      secondaryColor = Color(0xFF625B71),
      iconBackgroundColor = Color(0xFFE8DEF8)
    ),
    AppThemePalette(
      id = Space.THEME_PURPLE,
      name = "Royal Purple",
      description = "Rich violet and deep indigo themed icons",
      primaryColor = Color(0xFF7C4DFF),
      secondaryColor = Color(0xFFB388FF),
      iconBackgroundColor = Color(0xFF26184A)
    ),
    AppThemePalette(
      id = Space.THEME_DARK,
      name = "Midnight AMOLED",
      description = "High-contrast dark monochrome styling",
      primaryColor = Color(0xFFF8FAFC),
      secondaryColor = Color(0xFF94A3B8),
      iconBackgroundColor = Color(0xFF18181B)
    ),
    AppThemePalette(
      id = Space.THEME_NEON,
      name = "Cyber Neon",
      description = "Vibrant electric pink and cyan glow",
      primaryColor = Color(0xFF00F0FF),
      secondaryColor = Color(0xFFFF007F),
      iconBackgroundColor = Color(0xFF240A34)
    ),
    AppThemePalette(
      id = Space.THEME_MINIMAL,
      name = "Slate Minimal",
      description = "Clean muted slate and silver aesthetic",
      primaryColor = Color(0xFF38BDF8),
      secondaryColor = Color(0xFF94A3B8),
      iconBackgroundColor = Color(0xFF1E293B)
    ),
    AppThemePalette(
      id = Space.THEME_EMERALD,
      name = "Forest Emerald",
      description = "Calming organic lush emerald green accents",
      primaryColor = Color(0xFF10B981),
      secondaryColor = Color(0xFF34D399),
      iconBackgroundColor = Color(0xFF064E3B)
    ),
    AppThemePalette(
      id = Space.THEME_SUNSET,
      name = "Sunset Amber",
      description = "Warm golden dusk and coral amber",
      primaryColor = Color(0xFFF59E0B),
      secondaryColor = Color(0xFFFB923C),
      iconBackgroundColor = Color(0xFF451A03)
    ),
    AppThemePalette(
      id = Space.THEME_OCEAN,
      name = "Oceanic Teal",
      description = "Deep coastal blue and aquamarine styling",
      primaryColor = Color(0xFF38BDF8),
      secondaryColor = Color(0xFF0284C7),
      iconBackgroundColor = Color(0xFF082F49)
    )
  )

  fun getPalette(themeId: String?): AppThemePalette {
    return PALETTES.firstOrNull { it.id.equals(themeId, ignoreCase = true) } ?: PALETTES[0]
  }

  fun getThemedColorFilter(themeId: String?): ColorFilter? {
    if (themeId.isNullOrEmpty() || themeId.equals(Space.THEME_DEFAULT, ignoreCase = true)) {
      return null
    }
    val palette = getPalette(themeId)
    val r = palette.primaryColor.red
    val g = palette.primaryColor.green
    val b = palette.primaryColor.blue

    val matrix = ColorMatrix(
      floatArrayOf(
        r * 0.299f, r * 0.587f, r * 0.114f, 0f, 0f,
        g * 0.299f, g * 0.587f, g * 0.114f, 0f, 0f,
        b * 0.299f, b * 0.587f, b * 0.114f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
      )
    )
    return ColorFilter.colorMatrix(matrix)
  }
}

/**
 * Dynamic theming app icon composable that applies the space's theme palette
 * to app icons across desktop, dock, and app drawer.
 */
@Composable
fun ThemedAppIcon(
  app: DiscoveredApp?,
  bitmap: Bitmap?,
  appTheme: String,
  modifier: Modifier = Modifier,
  fallbackText: String? = null
) {
  val isThemed = !appTheme.equals(Space.THEME_DEFAULT, ignoreCase = true)
  val palette = AppThemeHelper.getPalette(appTheme)
  val colorFilter = remember(appTheme) { AppThemeHelper.getThemedColorFilter(appTheme) }
  val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

  if (isThemed) {
    Box(
      modifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(palette.iconBackgroundColor)
        .border(
          width = 1.5.dp,
          color = palette.primaryColor.copy(alpha = 0.55f),
          shape = RoundedCornerShape(16.dp)
        ),
      contentAlignment = Alignment.Center
    ) {
      if (imageBitmap != null) {
        Image(
          bitmap = imageBitmap,
          contentDescription = app?.label,
          colorFilter = colorFilter,
          modifier = Modifier
            .fillMaxSize()
            .padding(7.dp)
        )
      } else {
        Text(
          text = fallbackText ?: app?.label?.take(1) ?: "?",
          fontWeight = FontWeight.Bold,
          color = palette.primaryColor,
          fontSize = 18.sp
        )
      }
    }
  } else {
    // Default Material Theme: Original colorful icon
    if (imageBitmap != null) {
      Image(
        bitmap = imageBitmap,
        contentDescription = app?.label,
        modifier = modifier
      )
    } else {
      Box(
        modifier = modifier
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = fallbackText ?: app?.label?.take(1) ?: "?",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          fontSize = 18.sp
        )
      }
    }
  }
}

/**
 * Miniature themed app icon for folder previews.
 */
@Composable
fun ThemedMiniAppIcon(
  app: DiscoveredApp?,
  bitmap: Bitmap?,
  appTheme: String,
  modifier: Modifier = Modifier.size(16.dp),
  fallbackText: String? = null
) {
  val isThemed = !appTheme.equals(Space.THEME_DEFAULT, ignoreCase = true)
  val palette = AppThemeHelper.getPalette(appTheme)
  val colorFilter = remember(appTheme) { AppThemeHelper.getThemedColorFilter(appTheme) }

  if (isThemed) {
    Box(
      modifier = modifier
        .clip(RoundedCornerShape(4.dp))
        .background(palette.iconBackgroundColor)
        .border(0.5.dp, palette.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
      contentAlignment = Alignment.Center
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = app?.label,
          colorFilter = colorFilter,
          modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
        )
      } else {
        Text(
          text = fallbackText ?: app?.label?.take(1) ?: "?",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = palette.primaryColor
        )
      }
    }
  } else {
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = app?.label,
        modifier = modifier.clip(RoundedCornerShape(4.dp))
      )
    } else {
      Box(
        modifier = modifier
          .clip(RoundedCornerShape(4.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = fallbackText ?: app?.label?.take(1) ?: "?",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }
  }
}
