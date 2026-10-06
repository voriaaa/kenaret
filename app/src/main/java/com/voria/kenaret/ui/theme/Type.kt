package com.voria.kenaret.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.voria.kenaret.R

/** Vazirmatn (SIL Open Font License) is bundled locally; it covers Persian and Latin text. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val Base = Typography()

private fun TextStyle.vazir(weight: FontWeight? = null) =
    copy(fontFamily = Vazirmatn, fontWeight = weight ?: fontWeight)

val KenaretTypography = Typography(
    displayLarge = Base.displayLarge.vazir(FontWeight.SemiBold),
    displayMedium = Base.displayMedium.vazir(FontWeight.SemiBold),
    displaySmall = Base.displaySmall.vazir(FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.vazir(FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.vazir(FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.vazir(FontWeight.SemiBold),
    titleLarge = Base.titleLarge.vazir(FontWeight.SemiBold),
    titleMedium = Base.titleMedium.vazir(FontWeight.SemiBold),
    titleSmall = Base.titleSmall.vazir(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.vazir(),
    bodyMedium = Base.bodyMedium.vazir(),
    bodySmall = Base.bodySmall.vazir(),
    labelLarge = Base.labelLarge.vazir(FontWeight.Medium),
    labelMedium = Base.labelMedium.vazir(FontWeight.Medium),
    labelSmall = Base.labelSmall.vazir(FontWeight.Medium),
)
