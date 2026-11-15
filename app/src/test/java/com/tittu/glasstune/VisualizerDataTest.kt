package com.tittu.glasstune

import com.tittu.glasstune.ui.visualizer.BeatDetector
import com.tittu.glasstune.ui.visualizer.FftAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerDataTest {

    @Test
    fun `silent fft yields zero bands`() {
        val out = FloatArray(FftAnalyzer.BANDS)
        FftAnalyzer.computeBands(ByteArray(128), out)
        for (v in out) assertEquals(0f, v, 1e-6f)
    }

    @Test
    fun `loud bass bins excite the low bands only`() {
        // Bin 1 magnitude: sqrt(127^2 + 127^2) ~= 180 -> ~1.4 normalized.
        val fft = ByteArray(128)
        fft[2] = 127
        fft[3] = 127
        val out = FloatArray(FftAnalyzer.BANDS)
        FftAnalyzer.computeBands(fft, out)
        assertTrue(out[0] > 0.9f)
        assertTrue(out[FftAnalyzer.BANDS - 1] == 0f)
    }

    @Test
    fun `loud high bins excite the high bands`() {
        val fft = ByteArray(128)
        // Fill the last quarter of bins.
        for (k in 48 until 64) {
            fft[2 * k] = 100
            fft[2 * k + 1] = 40
        }
        val out = FloatArray(FftAnalyzer.BANDS)
        FftAnalyzer.computeBands(fft, out)
        assertTrue("top band should be loud", out[FftAnalyzer.BANDS - 1] > 0.5f)
        assertTrue("bass band should stay quiet", out[0] < out[FftAnalyzer.BANDS - 1])
    }

    @Test
    fun `bands never exceed the clamp`() {
        val fft = ByteArray(128)
        for (k in 1 until 64) {
            fft[2 * k] = 127
            fft[2 * k + 1] = 127
        }
        val out = FloatArray(FftAnalyzer.BANDS)
        FftAnalyzer.computeBands(fft, out)
        for (v in out) assertTrue(v <= 1.6f * 1.2f)
    }

    @Test
    fun `beat requires threshold and rolling-average gap`() {
        assertTrue(BeatDetector.update(0.8f, 0.2f, 0L, 1000L))
        assertFalse(BeatDetector.update(0.3f, 0.1f, 0L, 1000L))
        assertFalse(BeatDetector.update(0.6f, 0.55f, 0L, 1000L))
    }

    @Test
    fun `beat has a cooldown`() {
        assertTrue(BeatDetector.update(0.8f, 0.2f, 0L, 1000L))
        assertFalse(BeatDetector.update(0.9f, 0.2f, 1000L, 1100L))
        assertTrue(BeatDetector.update(0.9f, 0.2f, 1000L, 1400L))
    }
}
