package com.multispace.domain.model

/**
 * Visual page turn transitions for Layer 1 horizontal page navigation.
 */
enum class PageTurnEffect(val displayName: String, val description: String) {
  NORMAL("Normal", "Classic smooth horizontal slide"),
  CUBE("Cube", "3D rotating cube transition"),
  WINDMILL("Windmill", "Rotating pinwheel pivot transition"),
  CROSSFADE("Crossfade", "Seamless fade and depth dissolve"),
  ZOOM("Zoom", "Modern scale and depth zoom");

  companion object {
    fun fromString(name: String?): PageTurnEffect {
      if (name == null) return NORMAL
      return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
    }
  }
}
