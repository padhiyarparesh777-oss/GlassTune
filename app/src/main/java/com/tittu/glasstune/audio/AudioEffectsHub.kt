package com.tittu.glasstune.audio

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class BandInfo(val freqHz: Int, val minMb: Int, val maxMb: Int)
data class EqUiState(
    val attached: Boolean = false, val enabled: Boolean = false,
    val eqSupported: Boolean = false, val bassSupported: Boolean = false,
    val virtSupported: Boolean = false, val loudSupported: Boolean = false,
    val bands: List<BandInfo> = emptyList(), val levels: List<Int> = emptyList(),
    val systemPresets: List<String> = emptyList(), val activePreset: String? = null,
    val bass: Int = 0, val virt: Int = 0, val loud: Int = 0,
    // Software FX section
    val softEqEnabled: Boolean = false,
    val softEqGains: List<Float> = List(10) { 0f },
    val softEqActivePreset: String? = null,
    val balance: Int = 0,
    val reverbPreset: String = "None",
    val reverbSupported: Boolean = false,
    val normalize: Boolean = false,
    val visualizerOn: Boolean = false,
    val visualizerAvailable: Boolean = false
)

object AudioEffectsHub {
    private const val PREFS = "glasstune_fx"
    private var appCtx: Context? = null
    private var session = 0
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null
    private var reverb: PresetReverb? = null
    private var visualizer: Visualizer? = null
    private var activePreset: String? = null
    var ui by mutableStateOf(EqUiState())
        private set
    val fxProcessor = SoftwareFxProcessor()
    private fun prefs() = appCtx!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Equalizer settings are remembered per device model (device-specific profiles). */
    private fun profileKey(base: String) = "$base.${android.os.Build.MODEL.take(24)}"

    fun attach(context: Context, sessionId: Int) {
        appCtx = context.applicationContext
        session = sessionId
        releaseEffects()
        if (sessionId <= 0) { ui = EqUiState(); return }
        val p = prefs()
        val enabled = p.getBoolean("enabled", false)
        eq = try { Equalizer(0, sessionId) } catch (_: Exception) { null }
        val b = try { BassBoost(0, sessionId) } catch (_: Exception) { null }
        bass = if (b != null && b.strengthSupported) b else { b?.release(); null }
        val v = try { Virtualizer(0, sessionId) } catch (_: Exception) { null }
        virt = if (v != null && v.strengthSupported) v else { v?.release(); null }
        loud = try { LoudnessEnhancer(sessionId) } catch (_: Exception) { null }
        reverb = try { PresetReverb(0, sessionId) } catch (_: Exception) { null }
        eq?.let { e ->
            try {
                val n = e.numberOfBands.toInt(); val range = e.bandLevelRange
                val saved = (p.getString(profileKey("levels"), "") ?: "").split(",").mapNotNull { it.toIntOrNull() }
                if (saved.size == n) for (i in 0 until n) e.setBandLevel(i.toShort(), saved[i].coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            } catch (_: Exception) { }
        }
        try { bass?.setStrength(p.getInt("bass", 0).toShort()) } catch (_: Exception) { }
        try { virt?.setStrength(p.getInt("virt", 0).toShort()) } catch (_: Exception) { }
        try { loud?.setTargetGain(p.getInt("loud", 0)) } catch (_: Exception) { }
        enableAll(enabled); refresh(enabled)
        restoreSoftFx(p)
        tryReverbEnabled()
    }
    fun reapply() { appCtx?.let { attach(it, session) } }
    fun release() { releaseEffects(); stopVisualizerCapture(); ui = EqUiState() }
    private fun releaseEffects() {
        listOf<AudioEffect?>(eq, bass, virt, loud, reverb).forEach { try { it?.release() } catch (_: Exception) { } }
        eq = null; bass = null; virt = null; loud = null; reverb = null
        try { visualizer?.release() } catch (_: Exception) { }
        visualizer = null
    }
    private fun enableAll(on: Boolean) {
        listOf<AudioEffect?>(eq, bass, virt, loud).forEach { try { it?.setEnabled(on) } catch (_: Exception) { } }
    }
    private fun refresh(enabled: Boolean = ui.enabled) {
        val e = eq; val bands = mutableListOf<BandInfo>(); val levels = mutableListOf<Int>(); val presets = mutableListOf<String>()
        if (e != null) try {
            val r = e.bandLevelRange
            for (i in 0 until e.numberOfBands.toInt()) {
                bands += BandInfo(e.getCenterFreq(i.toShort()) / 1000, r[0].toInt(), r[1].toInt())
                levels += e.getBandLevel(i.toShort()).toInt()
            }
            for (i in 0 until e.numberOfPresets.toInt()) presets += e.getPresetName(i.toShort())
        } catch (_: Exception) { }
        val p = prefs()
        val softEqOn = p.getBoolean("soft_eq_enabled", false)
        val gains = (p.getString(profileKey("soft_eq_gains"), "") ?: "")
            .split(",").mapNotNull { it.toFloatOrNull() }.let { if (it.size == 10) it else List(10) { 0f } }
        ui = EqUiState(true, enabled, e != null && bands.isNotEmpty(), bass != null, virt != null, loud != null,
            bands, levels, presets, activePreset, p.getInt("bass", 0), p.getInt("virt", 0), p.getInt("loud", 0),
            softEqEnabled = softEqOn,
            softEqGains = gains,
            softEqActivePreset = p.getString(profileKey("soft_eq_preset"), null),
            balance = p.getInt("balance", 0),
            reverbPreset = p.getString("reverb_preset", "None") ?: "None",
            reverbSupported = reverb != null,
            normalize = p.getBoolean("normalize", false),
            visualizerOn = p.getBoolean("visualizer_on", false),
            visualizerAvailable = visualizer != null || session > 0
        )
        pushToProcessor(p, gains, softEqOn)
    }

    private fun persistLevels() {
        val e = eq ?: return
        try { prefs().edit().putString(profileKey("levels"), (0 until e.numberOfBands.toInt()).joinToString(",") { e.getBandLevel(it.toShort()).toString() }).apply() } catch (_: Exception) { }
    }
    fun setEnabled(on: Boolean) { prefs().edit().putBoolean("enabled", on).apply(); enableAll(on); refresh(on) }
    fun setBandLevel(index: Int, mb: Int) {
        val e = eq ?: return; val b = ui.bands.getOrNull(index) ?: return
        try { e.setBandLevel(index.toShort(), mb.coerceIn(b.minMb, b.maxMb).toShort()) } catch (_: Exception) { return }
        activePreset = null; persistLevels(); refresh()
    }
    fun usePreset(index: Int) {
        val e = eq ?: return
        try { e.usePreset(index.toShort()) } catch (_: Exception) { return }
        activePreset = ui.systemPresets.getOrNull(index); persistLevels(); refresh()
    }
    fun applyCustom(name: String, levels: List<Int>, bassStrength: Int, virtStrength: Int) {
        val e = eq
        if (e != null) try {
            val range = e.bandLevelRange
            levels.forEachIndexed { i, lvl -> if (i < e.numberOfBands.toInt()) e.setBandLevel(i.toShort(), lvl.coerceIn(range[0].toInt(), range[1].toInt()).toShort()) }
        } catch (_: Exception) { }
        try { bass?.setStrength(bassStrength.coerceIn(0, 1000).toShort()) } catch (_: Exception) { }
        try { virt?.setStrength(virtStrength.coerceIn(0, 1000).toShort()) } catch (_: Exception) { }
        prefs().edit().putInt("bass", bassStrength).putInt("virt", virtStrength).apply()
        persistLevels(); activePreset = name; refresh()
    }
    fun setBass(value: Int) { try { bass?.setStrength(value.coerceIn(0, 1000).toShort()) } catch (_: Exception) { return }; prefs().edit().putInt("bass", value).apply(); refresh() }
    fun setVirtualizer(value: Int) { try { virt?.setStrength(value.coerceIn(0, 1000).toShort()) } catch (_: Exception) { return }; prefs().edit().putInt("virt", value).apply(); refresh() }
    fun setLoudness(mb: Int) { try { loud?.setTargetGain(mb.coerceIn(0, 1000)) } catch (_: Exception) { return }; prefs().edit().putInt("loud", mb).apply(); refresh() }

    // ---------------- Software FX (10-band EQ, balance, reverb, normalization) ----------------
    private fun restoreSoftFx(p: android.content.SharedPreferences) {
        val gains = (p.getString(profileKey("soft_eq_gains"), "") ?: "")
            .split(",").mapNotNull { it.toFloatOrNull() }.let { if (it.size == 10) it else List(10) { 0f } }
        pushToProcessor(p, gains, p.getBoolean("soft_eq_enabled", false))
    }

    private fun pushToProcessor(p: android.content.SharedPreferences, gains: List<Float>, softEqOn: Boolean) {
        FxState.eqEnabled = softEqOn
        FxState.eqGainsDb = gains.toFloatArray()
        FxState.balance = p.getInt("balance", 0) / 100f
        FxState.normalize = p.getBoolean("normalize", false)
        fxProcessor.onParamsChanged()
    }

    fun setSoftEqEnabled(on: Boolean) {
        prefs().edit().putBoolean("soft_eq_enabled", on).apply()
        refresh()
    }

    fun setSoftEqBand(index: Int, db: Float) {
        val gains = ui.softEqGains.toMutableList()
        if (index !in gains.indices) return
        gains[index] = db.coerceIn(-12f, 12f)
        prefs().edit().putString(profileKey("soft_eq_gains"), gains.joinToString(",") { "%.1f".format(it) })
            .putString(profileKey("soft_eq_preset"), null).apply()
        refresh()
    }

    fun useSoftEqPreset(name: String, gains: List<Float>) {
        prefs().edit().putString(profileKey("soft_eq_gains"), gains.joinToString(",") { "%.1f".format(it) })
            .putString(profileKey("soft_eq_preset"), name).apply()
        refresh()
    }

    fun setBalance(percent: Int) {
        prefs().edit().putInt("balance", percent.coerceIn(-100, 100)).apply()
        refresh()
    }

    fun setNormalization(on: Boolean) {
        prefs().edit().putBoolean("normalize", on).apply()
        refresh()
    }

    fun setReverbPreset(name: String) {
        prefs().edit().putString("reverb_preset", name).apply()
        tryReverbEnabled()
        refresh()
    }

    private fun tryReverbEnabled() {
        val r = reverb ?: return
        val name = prefs().getString("reverb_preset", "None") ?: "None"
        try {
            if (name == "None") { r.enabled = false; return }
            val idx = REVERB_PRESETS.indexOfFirst { it.first == name }
            if (idx >= 0) { r.preset = REVERB_PRESETS[idx].second.toShort(); r.enabled = true }
        } catch (_: Exception) { }
    }

    // ---------------- Visualizer (waveform capture) ----------------
    var waveform: FloatArray? by mutableStateOf(null)
        private set

    fun startVisualizerCapture(hasPermission: Boolean): Boolean {
        if (!hasPermission || session <= 0) return false
        try {
            if (visualizer == null) {
                val v = Visualizer(session)
                v.captureSize = Visualizer.getCaptureSizeRange()[0]
                v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {
                        if (data == null) return
                        val out = FloatArray(32)
                        val step = data.size / 32
                        for (i in 0 until 32) {
                            var acc = 0
                            for (j in 0 until step) acc += abs(data[i * step + j].toInt() - 128)
                            out[i] = (acc.toFloat() / step / 128f).coerceIn(0f, 1f)
                        }
                        waveform = out
                    }
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) { }
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                v.enabled = true
                visualizer = v
            }
            prefs().edit().putBoolean("visualizer_on", true).apply()
            refresh()
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun stopVisualizerCapture() {
        try { visualizer?.enabled = false } catch (_: Exception) { }
        try { visualizer?.release() } catch (_: Exception) { }
        visualizer = null
        waveform = null
        prefs().edit().putBoolean("visualizer_on", false).apply()
        refresh()
    }

    // -------------------- Shared constants --------------------
    /** Human name -> PresetReverb preset constant. */
    val REVERB_PRESETS = listOf(
        "None" to 0,
        "Small Room" to 1,
        "Medium Room" to 2,
        "Large Room" to 3,
        "Medium Hall" to 4,
        "Large Hall" to 5,
        "Plate" to 6
    )

    /** Built-in soft-EQ curves (10 ISO bands: 31..16k Hz). */
    val SOFT_PRESETS = mapOf(
        "Flat" to List(10) { 0f },
        "Bass Boost" to listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Rock" to listOf(5f, 4f, 2f, -1f, -2f, 0f, 2f, 4f, 5f, 4f),
        "Pop" to listOf(-1f, 2f, 4f, 4f, 2f, 0f, -1f, 1f, 3f, 2f),
        "Jazz" to listOf(4f, 3f, 1f, 2f, -1f, -1f, 0f, 2f, 3f, 4f),
        "Classical" to listOf(5f, 4f, 3f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
        "Vocal" to listOf(-3f, -2f, 0f, 3f, 4f, 4f, 3f, 1f, 0f, -1f),
        "Treble" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 3f, 5f, 6f, 6f)
    )
}