package com.example.hocuspocusfocus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Purple = Color(0xFF5B3A8C)
private val PurpleLight = Color(0xFFCDB8F0)
private val Amber = Color(0xFFFFD54F)

private val LightColors = lightColorScheme(primary = Purple, secondary = Amber)
private val DarkColors = darkColorScheme(primary = PurpleLight, secondary = Amber)

/** Material 3 theme, using the wallpaper-based dynamic colors where available (Android 12+). */
@Composable
fun HocusPocusFocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
