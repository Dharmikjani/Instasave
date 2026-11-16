package com.instasaver.feature.gallery.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val AmoledBlack = Color(0xFF090C10)
val DarkSurface = Color(0xFF131822)
val DarkSurfaceElevated = Color(0xFF1C2333)
val DarkSurfaceBorder = Color(0xFF2B3447)

val TextPrimary = Color(0xFFF0F3F8)
val TextSecondary = Color(0xFF8B98AD)
val TextMuted = Color(0xFF5E6B80)

val IgPurple = Color(0xFF833AB4)
val IgRed = Color(0xFFFD1D1D)
val IgOrange = Color(0xFFFCB045)
val IgPink = Color(0xFFE1306C)

val SuccessGreen = Color(0xFF10B981)
val InfoBlue = Color(0xFF3B82F6)

val InstagramGradient = Brush.horizontalGradient(
    colors = listOf(IgPurple, IgPink, IgOrange)
)

val StoryRingGradient = Brush.sweepGradient(
    colors = listOf(IgPurple, IgPink, IgOrange, IgPurple)
)
