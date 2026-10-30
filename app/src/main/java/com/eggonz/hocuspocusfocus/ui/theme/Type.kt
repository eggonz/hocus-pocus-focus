package com.eggonz.hocuspocusfocus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.eggonz.hocuspocusfocus.R

// Both fonts are bundled variable fonts (licenses in licenses/), so each weight is an instance of
// the same file
private fun variableFont(resId: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { Font(resId, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight))) },
)

/** Space Grotesk: START, the countdown and headings. */
val DisplayFontFamily = variableFont(
    R.font.space_grotesk,
    FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold,
)

/** DM Sans: body text and labels. */
val BodyFontFamily = variableFont(R.font.dm_sans, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold)

private fun TextStyle.display() = copy(fontFamily = DisplayFontFamily)
private fun TextStyle.body() = copy(fontFamily = BodyFontFamily)

val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.display(),
        displayMedium = displayMedium.display(),
        displaySmall = displaySmall.display(),
        headlineLarge = headlineLarge.display(),
        headlineMedium = headlineMedium.display(),
        headlineSmall = headlineSmall.display(),
        titleLarge = titleLarge.display(),
        titleMedium = titleMedium.body(),
        titleSmall = titleSmall.body(),
        bodyLarge = bodyLarge.body(),
        bodyMedium = bodyMedium.body(),
        bodySmall = bodySmall.body(),
        labelLarge = labelLarge.body(),
        labelMedium = labelMedium.body(),
        labelSmall = labelSmall.body(),
    )
}
