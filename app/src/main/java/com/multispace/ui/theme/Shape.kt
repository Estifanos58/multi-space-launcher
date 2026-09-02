package com.multispace.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==========================================
// MULTI-SPACE LAUNCHER — SHAPE SYSTEM
// Standardized Geometric Radii Hierarchy
// ==========================================

val ShapeRoundXs = RoundedCornerShape(4.dp)
val ShapeRoundSm = RoundedCornerShape(8.dp)
val ShapeRoundMd = RoundedCornerShape(16.dp)
val ShapeRoundLg = RoundedCornerShape(24.dp)
val ShapeRoundXl = RoundedCornerShape(32.dp)
val ShapePill = CircleShape

val AppShapes = Shapes(
  extraSmall = ShapeRoundXs,
  small = ShapeRoundSm,
  medium = ShapeRoundMd,
  large = ShapeRoundLg,
  extraLarge = ShapeRoundXl
)
