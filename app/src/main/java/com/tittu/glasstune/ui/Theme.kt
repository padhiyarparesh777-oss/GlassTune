package com.tittu.glasstune.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    DARK("Dark Glass"), AMOLED("AMOLED Black"), LIGHT("Light Glass")
}

val AccentColors = listOf(
    Color(0xFF7C9CFF), Color(0xFFFF7CC8), Color(0xFF5EEAD4), Color(0xFFFFB86B), Color(0xFFB794F6)
)

@Composable
fun GlassTuneTheme(mode: ThemeMode, accent: Color, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        ThemeMode.DARK -> darkColorScheme(
            primary = accent, onPrimary = Color.Black,
            background = Color(0xFF0B1020), onBackground = Color.White,
            surface = Color(0xFF141A2E), onSurface = Color.White
        )
        ThemeMode.AMOLED -> darkColorScheme(
            primary = accent, onPrimary = Color.Black,
            background = Color.Black, onBackground = Color.White,
            surface = Color(0xFF0A0A0A), onSurface = Color.White
        )
        ThemeMode.LIGHT -> lightColorScheme(
            primary = accent, onPrimary = Color.White,
            background = Color(0xFFE9EEFF), onBackground = Color(0xFF111426),
            surface = Color.White, onSurface = Color(0xFF111426)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun GlassBackground(accent: Color, content: @Composable BoxScope.() -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind {
                if (bg != Color.Black) {
                    val c1 = Offset(size.width * 0.15f, size.height * 0.12f)
                    val c2 = Offset(size.width * 0.9f, size.height * 0.85f)
                    val r = size.width * 0.9f
                    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), center = c1, radius = r), radius = r, center = c1)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF8A5CFF).copy(alpha = 0.28f), Color.Transparent), center = c2, radius = r), radius = r, center = c2)
                }
            },
        content = content
    )
}
