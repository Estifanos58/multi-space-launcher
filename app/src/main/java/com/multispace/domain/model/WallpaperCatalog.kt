package com.multispace.domain.model

import androidx.annotation.DrawableRes
import com.multispace.R

/**
 * Curated high-definition launcher wallpaper asset model.
 */
data class WallpaperImagePreset(
  val id: String,
  val name: String,
  val category: String,
  @DrawableRes val drawableRes: Int,
  val uriString: String,
  val previewDescription: String
)

/**
 * Central catalog of built-in premium launcher wallpapers.
 */
object WallpaperCatalog {
  const val DEFAULT_WALLPAPER_URI = "android.resource://drawable/img_wallpaper_aurora"
  const val DEFAULT_WALLPAPER_ID = "wp_aurora"

  val PRESET_WALLPAPERS = listOf(
    WallpaperImagePreset(
      id = "wp_aurora",
      name = "Cosmic Aurora",
      category = "Cosmic & Deep Sky",
      drawableRes = R.drawable.img_wallpaper_aurora,
      uriString = "android.resource://drawable/img_wallpaper_aurora",
      previewDescription = "Iridescent cosmic aurora glowing with starry violet & magenta hues"
    ),
    WallpaperImagePreset(
      id = "wp_mountain",
      name = "Alpine Sunrise",
      category = "Serene Landscapes",
      drawableRes = R.drawable.img_wallpaper_mountain,
      uriString = "android.resource://drawable/img_wallpaper_mountain",
      previewDescription = "Majestic snow-capped mountain peaks bathed in golden morning light"
    ),
    WallpaperImagePreset(
      id = "wp_cyber",
      name = "Quantum Neon",
      category = "Futuristic 3D",
      drawableRes = R.drawable.img_wallpaper_cyber,
      uriString = "android.resource://drawable/img_wallpaper_cyber",
      previewDescription = "Futuristic glowing ribbons with smooth neon curves and glass depth"
    ),
    WallpaperImagePreset(
      id = "wp_nature",
      name = "Emerald Forest",
      category = "Serene Landscapes",
      drawableRes = R.drawable.img_wallpaper_nature,
      uriString = "android.resource://drawable/img_wallpaper_nature",
      previewDescription = "Lush emerald pine forest with warm ethereal morning sunbeams"
    )
  )

  /**
   * Resolves a stored wallpaper URI or identifier to a built-in drawable resource if available.
   */
  fun resolveDrawableRes(uriOrId: String?): Int? {
    if (uriOrId.isNullOrEmpty()) return R.drawable.img_wallpaper_aurora
    return when {
      uriOrId.contains("aurora") || uriOrId == "wp_aurora" -> R.drawable.img_wallpaper_aurora
      uriOrId.contains("mountain") || uriOrId == "wp_mountain" -> R.drawable.img_wallpaper_mountain
      uriOrId.contains("cyber") || uriOrId == "wp_cyber" -> R.drawable.img_wallpaper_cyber
      uriOrId.contains("nature") || uriOrId == "wp_nature" -> R.drawable.img_wallpaper_nature
      else -> null
    }
  }

  fun getPresetById(id: String?): WallpaperImagePreset {
    return PRESET_WALLPAPERS.firstOrNull { it.id == id }
      ?: PRESET_WALLPAPERS.firstOrNull { it.uriString == id }
      ?: PRESET_WALLPAPERS.first()
  }
}
