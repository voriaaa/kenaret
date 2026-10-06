package com.voria.kenaret.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Calm "dusk" palette: slate blue, warm clay and sage. Deliberately not the usual all-pink look.
val LightColors = lightColorScheme(
    primary = Color(0xFF3D5A80),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E2F3),
    onPrimaryContainer = Color(0xFF0E1D33),
    secondary = Color(0xFFA0604B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF6DDD3),
    onSecondaryContainer = Color(0xFF3A170C),
    tertiary = Color(0xFF4F7A63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2EADB),
    onTertiaryContainer = Color(0xFF0F2A1C),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7F5F2),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFF7F5F2),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFE7E2DC),
    onSurfaceVariant = Color(0xFF4A4744),
    outline = Color(0xFF7B7671),
    outlineVariant = Color(0xFFCFC9C2),
    surfaceTint = Color(0xFF3D5A80),
    inverseSurface = Color(0xFF31302E),
    inverseOnSurface = Color(0xFFF3F0EC),
    inversePrimary = Color(0xFFA9C3E8),
    surfaceBright = Color(0xFFFCFAF8),
    surfaceDim = Color(0xFFE2DEDA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2EFEB),
    surfaceContainer = Color(0xFFEDE9E4),
    surfaceContainerHigh = Color(0xFFE8E3DE),
    surfaceContainerHighest = Color(0xFFE2DDD7),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C3E8),
    onPrimary = Color(0xFF10243F),
    primaryContainer = Color(0xFF2A4262),
    onPrimaryContainer = Color(0xFFD6E2F3),
    secondary = Color(0xFFEDB59F),
    onSecondary = Color(0xFF4D2416),
    secondaryContainer = Color(0xFF6B3A2A),
    onSecondaryContainer = Color(0xFFF6DDD3),
    tertiary = Color(0xFFA6D0B6),
    onTertiary = Color(0xFF133524),
    tertiaryContainer = Color(0xFF2F5240),
    onTertiaryContainer = Color(0xFFD2EADB),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121416),
    onBackground = Color(0xFFE5E2DE),
    surface = Color(0xFF121416),
    onSurface = Color(0xFFE5E2DE),
    surfaceVariant = Color(0xFF3A3D42),
    onSurfaceVariant = Color(0xFFC4C1BC),
    outline = Color(0xFF8E8B87),
    outlineVariant = Color(0xFF45484C),
    surfaceTint = Color(0xFFA9C3E8),
    inverseSurface = Color(0xFFE5E2DE),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF3D5A80),
    surfaceBright = Color(0xFF383A3E),
    surfaceDim = Color(0xFF121416),
    surfaceContainerLowest = Color(0xFF0D0F11),
    surfaceContainerLow = Color(0xFF1A1C1F),
    surfaceContainer = Color(0xFF1E2024),
    surfaceContainerHigh = Color(0xFF282B2F),
    surfaceContainerHighest = Color(0xFF33363A),
)

/** Colors used to mark cycle phases in the calendar and the cycle ring. */
data class PhasePalette(
    val menstrual: Color,
    val follicular: Color,
    val ovulation: Color,
    val luteal: Color,
)

val LightPhasePalette = PhasePalette(
    menstrual = Color(0xFFC8664F),
    follicular = Color(0xFF4F8A6B),
    ovulation = Color(0xFFC9962E),
    luteal = Color(0xFF6E6AA8),
)

val DarkPhasePalette = PhasePalette(
    menstrual = Color(0xFFE8957F),
    follicular = Color(0xFF8CC7A6),
    ovulation = Color(0xFFE6C06A),
    luteal = Color(0xFFA9A5DD),
)
