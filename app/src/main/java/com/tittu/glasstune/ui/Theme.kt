package com.tittu.glasstune.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    DARK("Silver Glass"), AMOLED("Deep Silver"), LIGHT("Soft Silver")
}

val AccentColors = listOf(
    Color(0xFFE5EBEF), Color(0xFFD9E1E6), Color(0xFFC8D2D9),
    Color(0xFFF0F3F5), Color(0xFFB9C6CE)
)

@Composable
fun GlassTuneTheme(mode: ThemeMode, accent: Color, content: @Composable () -> Unit) {
    // One unified cool-silver palette keeps every screen visually consistent.
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF53616A),
        secondary = Color(0xFFD7E0E5),
        onSecondary = Color(0xFF53616A),
        background = Color(0xFF69767E),
        onBackground = Color(0xFFF1F4F6),
        surface = Color(0xFF707D85),
        onSurface = Color(0xFFF1F4F6),
        surfaceVariant = Color(0xFF626F77),
        onSurfaceVariant = Color(0xFFE1E7EA),
        outline = Color.White.copy(alpha = 0.38f)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun GlassBackground(accent: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF7B878F), Color(0xFF68757D), Color(0xFF59666E))
                )
            )
            .drawBehind {
                val r = size.width * 0.95f
                val c1 = Offset(size.width * 0.10f, size.height * 0.08f)
                val c2 = Offset(size.width * 0.92f, size.height * 0.88f)
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), center = c1, radius = r),
                    radius = r, center = c1
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF35434B).copy(alpha = 0.20f), Color.Transparent), center = c2, radius = r),
                    radius = r, center = c2
                )
            },
        content = content
    )
}
