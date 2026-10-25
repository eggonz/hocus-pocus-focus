package com.example.hocuspocusfocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The night sky behind the screen; also the window background in themes.xml. */
val Night = Color(0xFF140B22)

/** The violet glow at the center of the screen. */
val Glow = Color(0xFF3B2266)

private val Amber = Color(0xFFFFC94D)
private val Lavender = Color(0xFFCDB8F0)

private val Colors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2E1A47),
    secondary = Lavender,
    onSecondary = Color(0xFF2E1A47),
    background = Night,
    onBackground = Color(0xFFF3EDFF),
    surface = Night,
    onSurface = Color(0xFFF3EDFF),
    surfaceVariant = Color(0xFF2A1D40),
    onSurfaceVariant = Color(0xFFB9AACF),
    surfaceContainerHigh = Color(0xFF241835),
    outline = Color(0xFF5E4B7D),
    outlineVariant = Color(0xFF3A2C52),
)

/**
 * Material 3 theme with the app's own dark palette (violet night, amber accents) in both system
 * themes, so the screen stays dim during a session.
 */
@Composable
fun HocusPocusFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
