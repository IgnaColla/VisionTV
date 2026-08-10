// ui/theme/Theme.kt
package com.visiontv.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Equivalente a tus variables CSS / Tailwind dark colors
private val VisionTVDarkColors = darkColorScheme(
    background  = Color(0xFF0A0A0A),   // bg-neutral-950
    surface     = Color(0xFF171717),   // bg-neutral-900
    primary     = Color(0xFF6366F1),   // indigo-500 (color del splash)
    onBackground = Color.White,
    onSurface   = Color.White,
    outline     = Color(0x1AFFFFFF),   // white/10
)

@Composable
fun VisionTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VisionTVDarkColors,
        content = content
    )
}
