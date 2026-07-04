package com.appblocker.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian Space Dark Palette
val DarkPrimary = Color(0xFF9E82FF)      // Electric Violet
val DarkSecondary = Color(0xFF00E5FF)    // Neon Cyan
val DarkTertiary = Color(0xFFFF3D71)     // Neon Pink / Rose
val DarkBackground = Color(0xFF0C0E14)   // Obsidian Deep Black
val DarkSurface = Color(0xFF161A26)      // Slate Card Background
val DarkSurfaceVariant = Color(0xFF222738)// Slate Hover/Selected Card
val DarkOnBackground = Color(0xFFF1F3F9) // Warm Off-White Text
val DarkOnSurface = Color(0xFFE4E7EB)
val DarkOutline = Color(0xFF384358)

// Lavender Pearl Light Palette
val LightPrimary = Color(0xFF6200EE)     // Rich Deep Violet
val LightSecondary = Color(0xFF00ACC1)   // Cool Teal
val LightTertiary = Color(0xFFD81B60)    // Vibrant Pink
val LightBackground = Color(0xFFF8F9FE)  // Soft Off-White
val LightSurface = Color(0xFFFFFFFF)     // Clean White Card
val LightSurfaceVariant = Color(0xFFEFEFF7)
val LightOnBackground = Color(0xFF1C1B1F)// Near Black Text
val LightOnSurface = Color(0xFF1C1B1F)
val LightOutline = Color(0xFFCAC4D0)

object AppBlockerBrushes {
    val DarkBackdrop = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0C0E14),
            Color(0xFF16112C), // Hint of deep violet
            Color(0xFF0C0E14)
        )
    )

    val LightBackdrop = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8F9FE),
            Color(0xFFEDE9FE),
            Color(0xFFF8F9FE)
        )
    )

    val PremiumShieldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF9E82FF)
        )
    )

    val WarningShieldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFB300),
            Color(0xFFFF7043)
        )
    )

    val BlockedShieldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF3D71),
            Color(0xFF7B001C)
        )
    )
    
    val CardGradientDark = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1C2131),
            Color(0xFF161A26)
        )
    )
}
