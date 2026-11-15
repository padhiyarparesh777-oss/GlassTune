package com.tittu.glasstune.ui.visualizer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class Side { LEFT, RIGHT }

data class VizColors(val primary: Color, val secondary: Color)

/**
 * Ten distinct audio-reactive styles. Every function draws inside a strip of
 * `size.width x size.height` anchored at the LEFT edge; RIGHT side strips are
 * mirrored horizontally so effects hug both edges of the artwork symmetrically.
 *
 * All state comes from real FFT analysis ([VisualizerData]) — bass drives big
 * pulses, mids drive wave motion, highs drive sparkle, beats trigger glow bursts.
 */
object VisualizerStyles {
    val LABELS = listOf(
        "Neon Spectrum", "Liquid Wave", "Cosmic Particles", "Laser Flow", "Aurora Energy",
        "Digital Equalizer", "Electric Pulse", "Glass Wave", "Dual Side Bars", "Minimal Waveform"
    )

    fun draw(
        style: Int,
        s: DrawScope,
        side: Side,
        bands: FloatArray,
        bass: Float,
        mid: Float,
        treble: Float,
        beat: Float,
        phase: Float,
        colors: VizColors,
        intensity: Float,
        lowFx: Boolean
    ) {
        val body = {
            when (((style % LABELS.size) + LABELS.size) % LABELS.size) {
                0 -> neonSpectrum(s, bands, beat, colors, intensity)
                1 -> liquidWave(s, bands, phase, colors, intensity)
                2 -> cosmicParticles(s, phase, treble, bass, beat, colors, intensity, lowFx)
                3 -> laserFlow(s, bands, beat, phase, colors, intensity)
                4 -> aurora(s, bands, phase, beat, colors, intensity)
                5 -> digitalEq(s, bands, colors, intensity, lowFx)
                6 -> electricPulse(s, mid, beat, phase, colors, intensity)
                7 -> glassWave(s, bands, phase, colors, intensity)
                8 -> dualSideBars(s, bands, beat, colors, intensity)
                else -> minimalWave(s, bands, phase, colors, intensity)
            }
        }
        if (side == Side.RIGHT) {
            s.withTransform({ scale(-1f, 1f, pivot = Offset(s.size.width / 2f, s.size.height / 2f)) }) { body() }
        } else {
            body()
        }
    }

    /** 1. Neon Spectrum — glowing bars with a white-hot core and floor reflection. */
    private fun neonSpectrum(s: DrawScope, bands: FloatArray, beat: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        val bw = w / bands.size
        for (i in bands.indices) {
            val lvl = (bands[i] * intensity).coerceIn(0f, 1.5f)
            val bh = max(bw * 0.4f, lvl * h * 0.5f)
            val x = i * bw
            s.drawRect(c.primary.copy(alpha = 0.10f + 0.12f * beat), Offset(x, h - bh), Size(bw * 0.9f, bh))
            s.drawRect(c.primary.copy(alpha = 0.40f), Offset(x, h - bh), Size(bw * 0.55f, bh))
            s.drawRect(Color.White.copy(alpha = 0.55f * lvl), Offset(x, h - bh), Size(bw * 0.22f, bh))
            s.drawRect(c.secondary.copy(alpha = 0.07f), Offset(x, h), Size(bw * 0.55f, bh * 0.25f))
        }
    }

    /** 2. Liquid Wave — a smooth flowing ribbon filled with a vertical gradient. */
    private fun liquidWave(s: DrawScope, bands: FloatArray, phase: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        val steps = 28
        val fill = Path()
        val line = Path()
        var prevX = 0f
        var prevY = 0f
        var started = false
        for (j in 0..steps) {
            val t = j / steps.toFloat()
            val b = bands[(t * (bands.size - 1)).toInt().coerceIn(0, bands.size - 1)] * intensity
            val wob = sin(t * 12.566f + phase * 2.2f) * h * 0.02f
            val x = (b * w * 0.72f + wob).coerceAtLeast(w * 0.015f)
            val y = t * h
            if (!started) {
                fill.moveTo(x, y)
                line.moveTo(x, y)
                started = true
            } else {
                val mx = (prevX + x) / 2f
                val my = (prevY + y) / 2f
                fill.quadraticBezierTo(prevX, prevY, mx, my)
                line.quadraticBezierTo(prevX, prevY, mx, my)
            }
            prevX = x
            prevY = y
        }
        line.lineTo(prevX, prevY)
        fill.lineTo(0f, h)
        fill.lineTo(0f, 0f)
        fill.close()
        s.drawPath(fill, Brush.verticalGradient(listOf(c.primary.copy(alpha = 0.02f), c.primary.copy(alpha = 0.30f)), startY = 0f, endY = h))
        s.drawPath(line, Brush.verticalGradient(listOf(c.secondary, c.primary), startY = 0f, endY = h), style = Stroke(width = 3f))
        s.drawPath(line, Color.White.copy(alpha = 0.25f), style = Stroke(width = 1.2f))
    }

    // Particle pool, preallocated once and advanced per frame (no allocations in the loop).
    private val rnd = Random(11)
    private val pX = FloatArray(64) { rnd.nextFloat() }
    private val pY = FloatArray(64) { rnd.nextFloat() }
    private val pSeed = FloatArray(64) { rnd.nextFloat() }
    private val pPhase = FloatArray(64) { rnd.nextFloat() }

    /** 3. Cosmic Particles — energy dust rising with the highs, swelled by the bass. */
    private fun cosmicParticles(
        s: DrawScope, phase: Float, treble: Float, bass: Float, beat: Float,
        c: VizColors, intensity: Float, lowFx: Boolean
    ) {
        val w = s.size.width
        val h = s.size.height
        val count = if (lowFx) 28 else 64
        val energy = 0.25f + bass * 0.75f
        for (i in 0 until count) {
            val y = ((pY[i] - phase * 0.12f * pSeed[i] * intensity) % 1f + 1f) % 1f
            val drift = sin(phase * 1.7f + pPhase[i] * 6.283f) * 0.12f * pSeed[i]
            val x = (pX[i] + drift) * w
            val yy = y * h
            val edge = (1f - abs(y - 0.5f) * 2f).coerceIn(0f, 1f)
            val alpha = edge * energy * 0.85f
            val r = (1.5f + 3.5f * pSeed[i] * energy) * (w / 90f).coerceIn(0.8f, 2.2f)
            val col = when {
                i % 7 == 0 -> Color.White.copy(alpha = alpha * (0.5f + treble * 0.8f))
                i % 2 == 0 -> c.primary.copy(alpha = alpha)
                else -> c.secondary.copy(alpha = alpha * 0.9f)
            }
            s.drawCircle(col, r, Offset(x, yy))
            if (!lowFx && i % 4 == 0) s.drawCircle(col.copy(alpha = alpha * 0.25f), r * 2.6f, Offset(x, yy))
        }
        if (beat > 0.05f) {
            s.drawCircle(c.primary.copy(alpha = 0.10f * beat), w * (0.4f + beat * 0.5f), Offset(w * 0.5f, h * 0.5f))
        }
    }

    /** 4. Laser Flow — beams sweeping from the edge, their length pulled by the spectrum. */
    private fun laserFlow(s: DrawScope, bands: FloatArray, beat: Float, phase: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        val origin = Offset(0f, h * 0.5f)
        val beams = 6
        for (b in 0 until beams) {
            val base = -0.9f + (1.8f * b / (beams - 1))
            val ang = base + sin(phase * 0.9f + b) * 0.15f
            val e = bands[(b * 4) % bands.size] * intensity
            val len = h * (0.35f + 0.65f * e) + beat * h * 0.1f
            val end = Offset(origin.x + cos(ang) * len, origin.y + sin(ang) * len)
            val col = if (b % 2 == 0) c.primary else c.secondary
            s.drawLine(
                Brush.linearGradient(
                    listOf(col.copy(alpha = 0f), col.copy(alpha = 0.55f + 0.25f * beat), col.copy(alpha = 0f)),
                    start = origin, end = end
                ),
                origin, end, strokeWidth = 6f
            )
            s.drawLine(Color.White.copy(alpha = 0.45f * e.coerceIn(0f, 1f)), origin, end, strokeWidth = 1.4f)
            s.drawCircle(col.copy(alpha = 0.7f * e.coerceIn(0f, 1f)), 2.5f, end)
        }
    }

    /** 5. Aurora Energy — layered curtains of light breathing with the music. */
    private fun aurora(s: DrawScope, bands: FloatArray, phase: Float, beat: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        for (l in 0 until 3) {
            val b = bands[(l * 6 + 2) % bands.size] * intensity
            val cy = h * (0.2f + 0.3f * l) + sin(phase * 0.8f + l * 2.1f) * h * 0.08f
            val amp = h * (0.10f + 0.16f * b)
            val path = Path()
            var prevX = 0f
            var prevY = 0f
            var started = false
            val steps = 20
            for (j in 0..steps) {
                val t = j / steps.toFloat()
                val y = cy + sin(t * 6.283f * 1.5f + phase * (1.1f + l * 0.35f)) * amp
                val x = t * w
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.quadraticBezierTo(prevX, prevY, (prevX + x) / 2f, (prevY + y) / 2f)
                }
                prevX = x
                prevY = y
            }
            val col = when (l) {
                0 -> c.primary
                1 -> c.secondary
                else -> lerp(c.primary, Color.White, 0.4f)
            }
            s.drawPath(
                path,
                Brush.horizontalGradient(
                    listOf(col.copy(alpha = 0f), col.copy(alpha = 0.16f + 0.10f * b + 0.08f * beat), col.copy(alpha = 0f))
                )
            )
            s.drawPath(path, col.copy(alpha = 0.25f), style = Stroke(width = 2f))
        }
    }

    /** 6. Digital Equalizer — chunky LED blocks with a bright top segment. */
    private fun digitalEq(s: DrawScope, bands: FloatArray, c: VizColors, intensity: Float, lowFx: Boolean) {
        val w = s.size.width
        val h = s.size.height
        val bw = w / bands.size
        val segs = 26
        val segH = h / segs
        for (i in bands.indices) {
            val lit = ((bands[i] * intensity).coerceIn(0f, 1f) * segs).toInt().coerceIn(0, segs)
            val x = i * bw + bw * 0.15f
            val bwid = bw * 0.7f
            for (k in 0 until segs) {
                val yTop = h - (k + 1) * segH
                if (k < lit) {
                    val col = when {
                        k == lit - 1 -> Color.White.copy(alpha = 0.9f)
                        k >= 20 -> c.secondary
                        else -> c.primary
                    }
                    s.drawRect(col.copy(alpha = 0.85f), Offset(x, yTop), Size(bwid, segH * 0.7f))
                } else if (!lowFx) {
                    s.drawRect(c.primary.copy(alpha = 0.05f), Offset(x, yTop), Size(bwid, segH * 0.7f))
                }
            }
        }
    }

    /** 7. Electric Pulse — a crackling bolt plus expanding rings on every beat. */
    private fun electricPulse(s: DrawScope, mid: Float, beat: Float, phase: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        val segs = 12
        val path = Path()
        for (j in 0..segs) {
            val t = j / segs.toFloat()
            val jitter = sin(t * 40f + phase * 7f) * w * (0.10f + 0.28f * mid * intensity) * (1f - abs(t - 0.5f) * 0.6f)
            val x = w * 0.5f + jitter
            val y = h * t
            if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        s.drawPath(path, c.primary.copy(alpha = 0.10f + 0.25f * mid), style = Stroke(width = 9f))
        s.drawPath(path, Color.White.copy(alpha = 0.35f + 0.5f * mid), style = Stroke(width = 2f))
        if (beat > 0.02f) {
            val center = Offset(w / 2f, h / 2f)
            s.drawCircle(
                Brush.sweepGradient(
                    listOf(c.secondary.copy(alpha = 0.5f * beat), Color.Transparent),
                    center = center
                ),
                h * beat * 0.45f,
                center,
                style = Stroke(width = 3f)
            )
        }
    }

    /** 8. Glass Wave — translucent frosted ribbons with a crisp highlight edge. */
    private fun glassWave(s: DrawScope, bands: FloatArray, phase: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        for (layer in 0 until 2) {
            val fill = Path()
            var prevX = 0f
            var prevY = 0f
            var started = false
            val steps = 24
            for (j in 0..steps) {
                val t = j / steps.toFloat()
                val b = bands[(t * (bands.size - 1)).toInt().coerceIn(0, bands.size - 1)] * intensity
                val y = h * (0.35f + 0.3f * layer) + sin(t * 9.42f + phase * (1.4f + layer * 0.5f)) * h * (0.06f + 0.18f * b)
                val x = t * w
                if (!started) {
                    fill.moveTo(x, y)
                    started = true
                } else {
                    fill.quadraticBezierTo(prevX, prevY, (prevX + x) / 2f, (prevY + y) / 2f)
                }
                prevX = x
                prevY = y
            }
            fill.lineTo(w, h)
            fill.lineTo(0f, h)
            fill.close()
            val alpha = if (layer == 0) 0.14f else 0.09f
            s.drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(c.primary.copy(alpha = alpha + 0.05f), c.secondary.copy(alpha = alpha * 0.4f)),
                    startY = 0f, endY = h
                )
            )
            s.drawPath(fill, Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent)), style = Stroke(width = 1.5f))
        }
    }

    /** 9. Dual Side Bars — symmetric bars from both edges reaching toward the center. */
    private fun dualSideBars(s: DrawScope, bands: FloatArray, beat: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        val bw = (w * 0.82f) / bands.size
        for (i in bands.indices) {
            val lvl = (bands[i] * intensity).coerceIn(0f, 1.4f)
            val bh = max(bw * 0.5f, lvl * h * 0.5f)
            val col = c.primary.copy(alpha = 0.4f + 0.15f * beat)
            val tipH = min(bh, bw * 0.2f)
            s.drawRect(col, Offset(i * bw, h - bh), Size(bw * 0.6f, bh))
            s.drawRect(col, Offset(w - (i + 1) * bw, h - bh), Size(bw * 0.6f, bh))
            s.drawRect(Color.White.copy(alpha = 0.5f * lvl), Offset(i * bw, h - bh), Size(bw * 0.6f, tipH))
            s.drawRect(Color.White.copy(alpha = 0.5f * lvl), Offset(w - (i + 1) * bw, h - bh), Size(bw * 0.6f, tipH))
        }
    }

    /** 10. Minimal Waveform — one thin elegant line, nothing more. */
    private fun minimalWave(s: DrawScope, bands: FloatArray, phase: Float, c: VizColors, intensity: Float) {
        val w = s.size.width
        val h = s.size.height
        val steps = 30
        val path = Path()
        var prevX = 0f
        var prevY = 0f
        var started = false
        for (j in 0..steps) {
            val t = j / steps.toFloat()
            val b = bands[(t * (bands.size - 1)).toInt().coerceIn(0, bands.size - 1)]
            val y = h / 2f + sin(t * 18.85f + phase * 2f) * b * h * 0.28f * intensity
            val x = t * w
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.quadraticBezierTo(prevX, prevY, (prevX + x) / 2f, (prevY + y) / 2f)
            }
            prevX = x
            prevY = y
        }
        s.drawPath(path, c.primary.copy(alpha = 0.18f), style = Stroke(width = 7f))
        s.drawPath(path, c.primary.copy(alpha = 0.8f), style = Stroke(width = 2.5f))
    }
}
