package com.voria.kenaret.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.domain.ThemeMode

val LocalPhasePalette = staticCompositionLocalOf { LightPhasePalette }

@Composable
fun KenaretTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalPhasePalette provides if (dark) DarkPhasePalette else LightPhasePalette) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = KenaretTypography,
            content = content,
        )
    }
}

@Composable
fun phaseColor(phase: CyclePhase) = LocalPhasePalette.current.let {
    when (phase) {
        CyclePhase.MENSTRUAL -> it.menstrual
        CyclePhase.FOLLICULAR -> it.follicular
        CyclePhase.OVULATION -> it.ovulation
        CyclePhase.LUTEAL -> it.luteal
    }
}
