package com.tittu.glasstune.ui.visualizer

import android.os.SystemClock
import androidx.compose.runtime.mutableIntStateOf
import java.util.Arrays
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pure mapping from a Visualizer FFT buffer to log-spaced frequency bands.
 * Kept free of Android dependencies so it can be unit tested.
 */
object FftAnalyzer {
    const val BANDS = 24

    /**
     * Visualizer FFT bytes are interleaved magnitude pairs: fft[2k] = real, fft[2k+1] = imaginary.
     * Bin k maps to roughly (k * sampleRate / captureSize) Hz. Bins are grouped with a growing
     * (log-like) spacing so bass gets fine resolution and treble spans the whole spectrum.
     */
    fun computeBands(fft: ByteArray, out: FloatArray) {
        for (b in 0 until BANDS) out[b] = 0f
        val bins = fft.size / 2
        if (bins < 4) return
        var pos = 1f
        for (b in 0 until BANDS) {
            val start = pos.toInt().coerceIn(1, bins - 1)
            pos *= 1.28f
            val end = pos.toInt().coerceIn(start + 1, bins)
            var peak = 0f
            for (k in start until end) {
                val re = fft[2 * k].toFloat()
                val im = fft[2 * k + 1].toFloat()
                peak = max(peak, sqrt(re * re + im * im))
            }
            // Slight high-band gain compensates for the natural energy roll-off of music.
            out[b] = (peak / 128f).coerceIn(0f, 1.6f) * (0.85f + 0.35f * b / BANDS)
        }
    }
}

/** Pure beat-decision rule (unit-testable). */
object BeatDetector {
    /** A beat is a bass spike well above the rolling average, with a short cooldown. */
    fun update(bass: Float, rollingAvg: Float, lastBeatMs: Long, nowMs: Long): Boolean =
        bass > 0.5f && bass > rollingAvg * 1.30f && nowMs - lastBeatMs > 220L
}

/**
 * Shared live audio data for every visualizer surface.
 *
 * `onFft` is called from the Visualizer binder thread (fast path, no allocation),
 * `animate` is called once per frame from the Compose animation loop on the main
 * thread, where it advances attack/release envelopes, the shared animation phase
 * and the beat decay. [tick] is a snapshot state so Canvas draw scopes and
 * graphicsLayer blocks can subscribe to per-frame updates cheaply.
 */
object VisualizerData {
    val BANDS = FftAnalyzer.BANDS

    @Volatile
    var target: FloatArray = FloatArray(BANDS)
        private set

    /** Smoothed band levels (attack fast, release slow) read by the draw code. */
    val display = FloatArray(BANDS)

    @Volatile var bass = 0f
        private set
    @Volatile var mid = 0f
        private set
    @Volatile var treble = 0f
        private set

    /** Draw-thread only: decaying 0..1 beat pulse and shared animation phase. */
    var beat = 0f
    var phase = 0f

    /** Snapshot state bumped once per animated frame. */
    val tick = mutableIntStateOf(0)
        private set

    @Volatile private var lastFftAt = 0L
    private var rollingBass = 0f
    @Volatile private var lastBeatMs = 0L

    fun onFft(fft: ByteArray) {
        FftAnalyzer.computeBands(fft, target)
        var b = 0f
        var m = 0f
        var t = 0f
        for (i in 0 until BANDS) {
            val v = target[i]
            when {
                i < 5 -> b += v
                i < 13 -> m += v
                else -> t += v
            }
        }
        bass = (b / 5f).coerceIn(0f, 2f)
        mid = (m / 8f).coerceIn(0f, 2f)
        treble = (t / (BANDS - 13)).coerceIn(0f, 2f)
        val now = SystemClock.elapsedRealtime()
        lastFftAt = now
        if (BeatDetector.update(bass, rollingBass, lastBeatMs, now)) {
            beat = 1f
            lastBeatMs = now
        }
        rollingBass += (bass - rollingBass) * 0.18f
    }

    /** True while FFT data has been arriving recently. */
    fun isFresh(): Boolean = SystemClock.elapsedRealtime() - lastFftAt < 2500L

    /** Advance envelopes + phase; called once per frame from the animation loop. */
    fun animate(dt: Float, speed: Float) {
        val s = speed.coerceIn(0.2f, 3f)
        phase = (phase + dt * s) % 100_000f
        for (i in 0 until BANDS) {
            val to = target[i]
            val cur = display[i]
            val rate = if (to > cur) 14f * s else 4.5f * s
            display[i] = cur + (to - cur) * (1f - exp(-(rate * dt))).coerceIn(0f, 1f)
        }
        beat = max(0f, beat - dt * 2.6f)
        tick.intValue++
    }

    fun reset() {
        Arrays.fill(target, 0f)
        Arrays.fill(display, 0f)
        bass = 0f
        mid = 0f
        treble = 0f
        beat = 0f
        lastFftAt = 0L
    }
}
