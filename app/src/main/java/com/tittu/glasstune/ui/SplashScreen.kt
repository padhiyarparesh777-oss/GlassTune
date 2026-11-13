package com.tittu.glasstune.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Opening animation: "Reet Feel Music" fades in as 3D text, grows until it fills the screen,
 * then disappears and reveals the app. Tap anywhere to skip. Pure Compose, no image assets.
 */
@Composable
fun SplashScreen(accent: Color, onFinish: () -> Unit) {
    GlassTuneTheme(ThemeMode.DARK, accent) {
        val intro = remember { Animatable(0f) }
        val grow = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            intro.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            delay(450)
            grow.animateTo(1f, tween(950, easing = FastOutLinearInEasing))
            onFinish()
        }

        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFinish() }
        ) {
            // Background fades away while the text grows, revealing the app underneath
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - ((grow.value - 0.35f) / 0.65f).coerceIn(0f, 1f) }) {
                GlassBackground(accent) { }
            }

            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val s = (0.8f + 0.2f * intro.value) * (1f + 9f * grow.value)
                    scaleX = s
                    scaleY = s
                    rotationY = -25f * (1f - intro.value)
                    rotationX = 12f * (1f - intro.value)
                    cameraDistance = 14f * density
                    alpha = intro.value * (1f - ((grow.value - 0.5f) / 0.5f).coerceIn(0f, 1f))
                },
                contentAlignment = Alignment.Center
            ) {
                ThreeDTitle(accent)
            }
        }
    }
}

/** Fake extrusion: a stack of offset, progressively darker copies behind a glossy gradient face. */
@Composable
private fun ThreeDTitle(accent: Color) {
    val base = TextStyle(
        fontSize = 46.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center
    )
    val deep = lerp(accent, Color.Black, 0.75f)
    val mid = lerp(accent, Color.Black, 0.45f)
    val label = "Reet Feel\nMusic"

    Box(contentAlignment = Alignment.Center) {
        Text(
            label,
            style = base.copy(color = Color.Black.copy(alpha = 0.35f)),
            modifier = Modifier.offset(x = 16.dp, y = 22.dp)
        )
        for (i in 12 downTo 1) {
            Text(
                label,
                style = base.copy(color = lerp(mid, deep, i / 12f)),
                modifier = Modifier.offset(x = (i * 1.1f).dp, y = (i * 1.5f).dp)
            )
        }
        Text(
            label,
            style = base.copy(
                brush = Brush.verticalGradient(
                    listOf(Color.White, lerp(accent, Color.White, 0.35f), accent)
                ),
                shadow = Shadow(accent.copy(alpha = 0.7f), Offset.Zero, 36f)
            )
        )
    }
}