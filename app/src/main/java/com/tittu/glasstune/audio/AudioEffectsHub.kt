package com.tittu.glasstune.audio

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
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
    val bass: Int = 0, val virt: Int = 0, val loud: Int = 0
)

object AudioEffectsHub {
    private const val PREFS = "glasstune_fx"
    private var appCtx: Context? = null
    private var session = 0
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null
    private var activePreset: String? = null
    var ui by mutableStateOf(EqUiState())
        private set
    private fun prefs() = appCtx!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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
        eq?.let { e ->
            try {
                val n = e.numberOfBands.toInt(); val range = e.bandLevelRange
                val saved = (p.getString("levels", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }
                if (saved.size == n) for (i in 0 until n) e.setBandLevel(i.toShort(), saved[i].coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            } catch (_: Exception) { }
        }
        try { bass?.setStrength(p.getInt("bass", 0).toShort()) } catch (_: Exception) { }
        try { virt?.setStrength(p.getInt("virt", 0).toShort()) } catch (_: Exception) { }
        try { loud?.setTargetGain(p.getInt("loud", 0)) } catch (_: Exception) { }
        enableAll(enabled); refresh(enabled)
    }
    fun reapply() { appCtx?.let { attach(it, session) } }
    fun release() { releaseEffects(); ui = EqUiState() }
    private fun releaseEffects() {
        listOf<AudioEffect?>(eq, bass, virt, loud).forEach { try { it?.release() } catch (_: Exception) { } }
        eq = null; bass = null; virt = null; loud = null
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
        ui = EqUiState(true, enabled, e != null && bands.isNotEmpty(), bass != null, virt != null, loud != null,
            bands, levels, presets, activePreset, p.getInt("bass", 0), p.getInt("virt", 0), p.getInt("loud", 0))
    }
    private fun persistLevels() {
        val e = eq ?: return
        try { prefs().edit().putString("levels", (0 until e.numberOfBands.toInt()).joinToString(",") { e.getBandLevel(it.toShort()).toString() }).apply() } catch (_: Exception) { }
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
}