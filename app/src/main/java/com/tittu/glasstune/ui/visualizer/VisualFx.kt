package com.tittu.glasstune.ui.visualizer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** A manual color theme for the visualizer; `primary == null` means "follow the artwork accent". */
data class VizPreset(val label: String, val primary: Color?, val secondary: Color?)

/**
 * User-tunable visual effects settings, persisted in SharedPreferences.
 * Modeled on the FxState/AudioEffectsHub pattern: an object with Compose state,
 * so any screen can read the values directly.
 */
object VisualFx {
    private const val PREFS = "glasstune_vfx"
    private var prefs: SharedPreferences? = null

    val PRESETS = listOf(
        VizPreset("Auto (artwork)", null, null),
        VizPreset("Neon Cyan", Color(0xFF22D3EE), Color(0xFFA78BFA)),
        VizPreset("Sunset", Color(0xFFFB923C), Color(0xFFF43F5E)),
        VizPreset("Purple Haze", Color(0xFF8B5CF6), Color(0xFFEC4899)),
        VizPreset("Emerald", Color(0xFF10B981), Color(0xFF22D3EE)),
        VizPreset("Crimson", Color(0xFFEF4444), Color(0xFFFBBF24)),
        VizPreset("Ice", Color(0xFF38BDF8), Color(0xFFE0F2FE)),
        VizPreset("Silver", Color(0xFFE5E7EB), Color(0xFF9CA3AF))
    )

    var masterEnabled by mutableStateOf(true); private set
    var leftEnabled by mutableStateOf(true); private set
    var rightEnabled by mutableStateOf(true); private set
    var style by mutableStateOf(0); private set
    var intensity by mutableStateOf(1f); private set
    var speed by mutableStateOf(1f); private set
    var colorPreset by mutableStateOf(0); private set
    var artworkGlow by mutableStateOf(true); private set
    var beatPulse by mutableStateOf(true); private set
    var perfMode by mutableStateOf(false); private set
    var startFullscreen by mutableStateOf(false); private set

    /** Remembered so the audio hub can restart capture after audio-session changes. */
    var micPermission by mutableStateOf(false); private set

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        masterEnabled = p.getBoolean("master", true)
        leftEnabled = p.getBoolean("left", true)
        rightEnabled = p.getBoolean("right", true)
        style = p.getInt("style", 0).coerceIn(0, VisualizerStyles.LABELS.size - 1)
        intensity = p.getInt("intensity", 100).coerceIn(30, 160) / 100f
        speed = p.getInt("speed", 100).coerceIn(50, 200) / 100f
        colorPreset = p.getInt("color", 0).coerceIn(0, PRESETS.size - 1)
        artworkGlow = p.getBoolean("glow", true)
        beatPulse = p.getBoolean("pulse", true)
        perfMode = p.getBoolean("perf", false)
        startFullscreen = p.getBoolean("fullscreen", false)
        micPermission = p.getBoolean("mic", false)
    }

    private fun edit(block: (SharedPreferences.Editor) -> Unit) {
        prefs?.edit()?.let { e -> block(e); e.apply() }
    }

    fun setMasterEnabled(v: Boolean) { masterEnabled = v; edit { it.putBoolean("master", v) } }
    fun setLeftEnabled(v: Boolean) { leftEnabled = v; edit { it.putBoolean("left", v) } }
    fun setRightEnabled(v: Boolean) { rightEnabled = v; edit { it.putBoolean("right", v) } }
    fun setStyle(v: Int) { style = v.coerceIn(0, VisualizerStyles.LABELS.size - 1); edit { it.putInt("style", style) } }
    fun setIntensity(v: Float) { intensity = v.coerceIn(0.3f, 1.6f); edit { it.putInt("intensity", (intensity * 100).toInt()) } }
    fun setSpeed(v: Float) { speed = v.coerceIn(0.5f, 2f); edit { it.putInt("speed", (speed * 100).toInt()) } }
    fun setColorPreset(v: Int) { colorPreset = v.coerceIn(0, PRESETS.size - 1); edit { it.putInt("color", colorPreset) } }
    fun setArtworkGlow(v: Boolean) { artworkGlow = v; edit { it.putBoolean("glow", v) } }
    fun setBeatPulse(v: Boolean) { beatPulse = v; edit { it.putBoolean("pulse", v) } }
    fun setPerfMode(v: Boolean) { perfMode = v; edit { it.putBoolean("perf", v) } }
    fun setStartFullscreen(v: Boolean) { startFullscreen = v; edit { it.putBoolean("fullscreen", v) } }
    fun setMicPermission(v: Boolean) { micPermission = v; edit { it.putBoolean("mic", v) } }
}
