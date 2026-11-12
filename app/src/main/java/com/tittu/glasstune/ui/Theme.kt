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

val AccentNames = listOf("Silver Mist", "Ocean Blue", "Rose Quartz", "Lavender", "Mint Aqua", "Midnight")

val AccentColors = listOf(
    Color(0xFFE5EBEF), Color(0xFF7FA8C9), Color(0xFFCC96AE),
    Color(0xFFA99BCC), Color(0xFF87B8AD), Color(0xFF7886AD)
)

@Composable
fun GlassTuneTheme(mode: ThemeMode, accent: Color, content: @Composable () -> Unit) {
    val base = Color(0xFF69767E)
    val tintedBase = androidx.compose.ui.graphics.lerp(base, accent, 0.28f)
    // Each palette keeps the same frosted-silver surface and changes its tint.
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF53616A),
        secondary = Color(0xFFD7E0E5),
        onSecondary = Color(0xFF53616A),
        background = tintedBase,
        onBackground = Color(0xFFF1F4F6),
        surface = androidx.compose.ui.graphics.lerp(Color(0xFF707D85), accent, 0.22f),
        onSurface = Color(0xFFF1F4F6),
        surfaceVariant = androidx.compose.ui.graphics.lerp(Color(0xFF626F77), accent, 0.20f),
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
                    listOf(
                        androidx.compose.ui.graphics.lerp(Color(0xFF7B878F), accent, 0.33f),
                        androidx.compose.ui.graphics.lerp(Color(0xFF68757D), accent, 0.25f),
                        androidx.compose.ui.graphics.lerp(Color(0xFF59666E), accent, 0.20f)
                    )
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
