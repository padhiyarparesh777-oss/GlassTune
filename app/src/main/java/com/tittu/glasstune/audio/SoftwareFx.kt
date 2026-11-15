package com.tittu.glasstune.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Software audio-effects pipeline applied to the player's PCM stream before output:
 * 10-band graphic EQ (peaking biquads), stereo balance and approximate volume
 * normalization. Works on every device because it does not rely on vendor audiofx.
 */
@UnstableApi
object FxState {
    val BAND_FREQS = floatArrayOf(31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    @Volatile var eqEnabled: Boolean = false
    @Volatile var eqGainsDb: FloatArray = FloatArray(10)
    @Volatile var balance: Float = 0f        // -1 (left) .. +1 (right)
    @Volatile var normalize: Boolean = false

    fun eqActive(): Boolean = eqEnabled && eqGainsDb.any { abs(it) > 0.4f }
    fun anyActive(): Boolean = eqActive() || abs(balance) > 0.01f || normalize
}

@UnstableApi
class SoftwareFxProcessor : AudioProcessor {

    private var inputFormat: AudioProcessor.AudioFormat? = null
    private var outputFormat: AudioProcessor.AudioFormat? = null
    private var active = false
    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    private var inputEnded = false

    // Per-channel biquad state [band][channel][x1,x2,y1,y2]
    private var coeffs: Array<FloatArray> = emptyArray()
    private var state: Array<FloatArray> = emptyArray()
    private var channels = 0

    // Normalization envelope
    private var normGain = 1f

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        outputFormat = inputAudioFormat
        channels = inputAudioFormat.channelCount
        rebuildCoefficients(inputAudioFormat.sampleRate)
        updateActive()
        return inputAudioFormat
    }

    private fun rebuildCoefficients(sampleRate: Int) {
        val gains = FxState.eqGainsDb
        val newCoeffs = Array(10) { FloatArray(5) }
        for (b in 0 until 10) {
            val f0 = FxState.BAND_FREQS[b].coerceAtMost(sampleRate * 0.45f)
            val gain = if (FxState.eqEnabled) gains[b] else 0f
            val A = 10.0.pow(gain / 40.0).toFloat()
            val w0 = (2.0 * Math.PI * f0 / sampleRate)
            val alpha = (sin(w0) / (2.0 * 1.1)).toFloat()
            val cw = cos(w0).toFloat()
            val a0 = 1f + alpha / A
            newCoeffs[b][0] = (1f + alpha * A) / a0          // b0
            newCoeffs[b][1] = (-2f * cw) / a0                // b1
            newCoeffs[b][2] = (1f - alpha * A) / a0          // b2
            newCoeffs[b][3] = (-2f * cw) / a0                // a1
            newCoeffs[b][4] = (1f - alpha / A) / a0          // a2
        }
        coeffs = newCoeffs
        state = Array(10) { FloatArray(channels * 4) }
    }

    fun onParamsChanged() {
        inputFormat?.let { rebuildCoefficients(it.sampleRate) }
        updateActive()
    }

    private fun updateActive() {
        active = inputFormat != null && FxState.anyActive()
    }

    override fun isActive(): Boolean = active

    override fun queueInput(inputBuffer: ByteBuffer) {
        val fmt = inputFormat ?: return
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
        when (fmt.encoding) {
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer, remaining)
            else -> processPcm16(inputBuffer, remaining)
        }
        buffer.flip()
        inputBuffer.position(inputBuffer.limit())
    }

    private fun processPcm16(src: ByteBuffer, byteCount: Int) {
        val samples = byteCount / 2
        for (i in 0 until samples) {
            val raw = src.short
            var v = raw / 32768f
            v = processSample(v, i % channels)
            buffer.putShort((v.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        }
    }

    private fun processFloat(src: ByteBuffer, byteCount: Int) {
        val samples = byteCount / 4
        for (i in 0 until samples) {
            var v = src.getFloat()
            v = processSample(v, i % channels)
            buffer.putFloat(v)
        }
    }

    private fun processSample(input: Float, channel: Int): Float {
        var v = input
        if (FxState.eqActive()) {
            for (b in 0 until 10) {
                val c = coeffs[b]
                val s = state[b]
                val x = v
                val y = c[0] * x + c[1] * s[channel * 4] + c[2] * s[channel * 4 + 1] - c[3] * s[channel * 4 + 2] - c[4] * s[channel * 4 + 3]
                s[channel * 4 + 1] = s[channel * 4]
                s[channel * 4] = x
                s[channel * 4 + 3] = s[channel * 4 + 2]
                s[channel * 4 + 2] = y
                v = y
            }
        }
        val bal = FxState.balance
        if (abs(bal) > 0.01f) {
            v *= if (channel == 0) 1f - bal.coerceIn(0f, 1f) else 1f + bal.coerceIn(-1f, 0f)
        }
        if (FxState.normalize) {
            // Slow rolling RMS -> gain compensation (approximate loudness normalization).
            val target = 0.10f
            val rms = abs(v)
            val desired = if (rms > 0.001f) (target / rms).coerceIn(0.5f, 4f) else normGain
            normGain += (desired - normGain) * 0.02f
            v *= normGain
        }
        return v
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = buffer

    override fun isEnded(): Boolean = inputEnded && !buffer.hasRemaining()

    override fun flush() {
        inputEnded = false
        buffer.clear()
        for (b in state) b.fill(0f)
        normGain = 1f
    }

    override fun reset() {
        flush()
        inputFormat = null
        outputFormat = null
        active = false
        coeffs = emptyArray()
        state = emptyArray()
        channels = 0
    }
}
