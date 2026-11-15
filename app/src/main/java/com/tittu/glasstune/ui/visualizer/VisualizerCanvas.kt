package com.tittu.glasstune.ui.visualizer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tittu.glasstune.ui.GlassIconButton
import com.tittu.glasstune.ui.MusicViewModel
import com.tittu.glasstune.ui.formatTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Compose the visualizer palette: AUTO follows the artwork accent, else a preset. */
@Composable
fun rememberVizColors(accent: Color): VizColors {
    val preset = VisualFx.PRESETS[VisualFx.colorPreset.coerceIn(0, VisualFx.PRESETS.size - 1)]
    return remember(preset, accent) {
        if (preset.primary == null) {
            VizColors(accent, lerp(accent, Color.White, 0.45f))
        } else {
            VizColors(preset.primary, preset.secondary ?: preset.primary)
        }
    }
}

/**
 * A live audio-reactive strip drawn on one side of the screen.
 * Renders nothing when there is no fresh FFT data — no fake motion.
 */
@Composable
fun VisualizerStrip(
    accent: Color,
    side: Side,
    modifier: Modifier,
    fullscreen: Boolean = false,
    lowFx: Boolean = false
) {
    val colors = rememberVizColors(accent)
    Canvas(modifier.alpha(if (fullscreen) 1f else 0.95f)) {
        VisualizerData.tick.intValue // subscribe to per-frame updates
        if (!VisualizerData.isFresh()) return@Canvas
        VisualizerStyles.draw(
            style = VisualFx.style,
            s = this,
            side = side,
            bands = VisualizerData.display,
            bass = VisualizerData.bass,
            mid = VisualizerData.mid,
            treble = VisualizerData.treble,
            beat = VisualizerData.beat,
            phase = VisualizerData.phase,
            colors = colors,
            intensity = VisualFx.intensity,
            lowFx = lowFx
        )
    }
}

/** Beat-reactive glow + rotating energy ring drawn behind the album artwork. */
@Composable
fun ArtworkEnergyRing(accent: Color, modifier: Modifier) {
    val colors = rememberVizColors(accent)
    Canvas(modifier) {
        VisualizerData.tick.intValue
        if (!VisualizerData.isFresh()) return@Canvas
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        val r = min(w, h) / 2f
        val bass = VisualizerData.bass
        val beat = VisualizerData.beat
        val phase = VisualizerData.phase
        drawCircle(
            Brush.radialGradient(
                listOf(colors.primary.copy(alpha = 0.30f + 0.25f * beat + 0.20f * bass), Color.Transparent),
                center = c, radius = r * 1.25f
            ),
            radius = r * 1.25f,
            center = c
        )
        val sweep = 110f + 90f * bass
        drawArc(
            Brush.sweepGradient(
                listOf(
                    Color.Transparent,
                    colors.primary.copy(alpha = 0.8f),
                    colors.secondary.copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = c
            ),
            startAngle = phase * 60f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(c.x - r * 1.02f, c.y - r * 1.02f),
            size = Size(r * 2.04f, r * 2.04f),
            style = Stroke(width = 4.dp.toPx())
        )
        val orbA = phase * 3.1f
        drawCircle(
            Color.White.copy(alpha = 0.6f),
            r * 0.035f,
            Offset(c.x + cos(orbA) * r * 1.02f, c.y + sin(orbA) * r * 1.02f)
        )
    }
}

/** Immersive full-screen visualizer mode with floating playback controls. */
@Composable
fun FullscreenVisualizer(vm: MusicViewModel, accent: Color, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    val song = vm.currentSong
    Box(Modifier.fillMaxSize().background(Color(0xFF050609))) {
        Row(Modifier.fillMaxSize()) {
            VisualizerStrip(accent, Side.LEFT, Modifier.weight(1f).fillMaxHeight(), fullscreen = true)
            VisualizerStrip(accent, Side.RIGHT, Modifier.weight(1f).fillMaxHeight(), fullscreen = true)
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Exit fullscreen", onExit, size = 44.dp, tint = Color.White)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song?.title ?: "Nothing playing",
                    color = Color.White, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    song?.artist ?: "",
                    color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dur = vm.durationMs.coerceAtLeast(1L).toFloat()
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((vm.positionMs.coerceAtLeast(0L).toFloat() / dur).coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(accent)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(34.dp)
            ) {
                GlassIconButton(Icons.Rounded.SkipPrevious, "Previous", { vm.previous() }, size = 52.dp, tint = Color.White)
                Box(
                    Modifier.size(68.dp).clip(CircleShape).background(accent).clickable { vm.playPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        "Play/Pause", tint = Color.Black, modifier = Modifier.size(36.dp)
                    )
                }
                GlassIconButton(Icons.Rounded.SkipNext, "Next", { vm.next() }, size = 52.dp, tint = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${formatTime(vm.positionMs)} / ${formatTime(vm.durationMs)}",
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
            )
        }
    }
}
