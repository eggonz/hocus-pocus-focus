package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hocuspocusfocus.R

/** An app the user can launch, as shown in a routine's allowed apps. */
data class InstalledApp(val packageName: String, val label: String, val icon: Painter)

/** The apps of [installedApps] in [packageNames], in that order; uninstalled ones are left out. */
internal fun appsIn(packageNames: List<String>, installedApps: List<InstalledApp>): List<InstalledApp> =
    packageNames.mapNotNull { packageName -> installedApps.find { it.packageName == packageName } }

/** An app's launcher icon, described by its label, faded to grey unless [isEnabled]. */
@Composable
fun AppIcon(app: InstalledApp, modifier: Modifier = Modifier, isEnabled: Boolean = true) {
    Image(
        app.icon,
        contentDescription = app.label,
        modifier = modifier.size(APP_ICON_SIZE),
        alpha = if (isEnabled) 1f else DISABLED_APP_ICON_ALPHA,
        colorFilter = if (isEnabled) null else GREYSCALE,
    )
}

private const val DISABLED_APP_ICON_ALPHA = 0.38f

private val GREYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

internal val APP_ICON_SIZE = 48.dp

/** Stand-in apps for previews, which can't load real launcher icons. */
@Composable
internal fun previewApps(): List<InstalledApp> = listOf(
    InstalledApp("com.example.clock", "Clock", painterResource(R.drawable.ic_launcher_foreground)),
    InstalledApp("com.example.reader", "Reader", painterResource(R.drawable.ic_pool_book)),
    InstalledApp("com.example.weather", "Weather", painterResource(R.drawable.ic_pool_sun)),
)
