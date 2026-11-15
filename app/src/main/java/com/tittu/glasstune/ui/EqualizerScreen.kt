package com.tittu.glasstune.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tittu.glasstune.audio.AudioEffectsHub

private val softBandLabels = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

@Composable
fun EqualizerScreen(vm: MusicViewModel, accent: Color, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val s = AudioEffectsHub.ui
    val custom by vm.customPresets.collectAsStateWithLifecycle()
    var showSave by remember { mutableStateOf(false) }
    val onBg = MaterialTheme.colorScheme.onBackground
    val sliderColors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = onBg.copy(alpha = 0.2f))
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back", onClose, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text("Equalizer", color = onBg, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(checked = s.enabled, onCheckedChange = { AudioEffectsHub.setEnabled(it) }, enabled = s.attached,
                colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (!s.attached) GlassSection("Audio engine not ready") {
                Text("Audio effects attach to the player. Play any song, then reopen this screen.", color = onBg.copy(alpha = 0.75f))
            }
            GlassSection("Graphic EQ (10-band software)") {
                Text("Works on every device, independent of the hardware equalizer.", color = onBg.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = s.softEqEnabled, onCheckedChange = { AudioEffectsHub.setSoftEqEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black))
                    Spacer(Modifier.width(10.dp))
                    Text(if (s.softEqEnabled) "On" else "Off", color = onBg.copy(alpha = 0.8f))
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AudioEffectsHub.SOFT_PRESETS.keys.toList()) { name ->
                        PillChip(name, s.softEqActivePreset == name, accent) {
                            AudioEffectsHub.useSoftEqPreset(name, AudioEffectsHub.SOFT_PRESETS[name]!!)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                s.softEqGains.forEachIndexed { i, gain ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(softBandLabels[i], color = onBg.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.width(40.dp))
                        Slider(value = gain, onValueChange = { AudioEffectsHub.setSoftEqBand(i, it) },
                            valueRange = -12f..12f, enabled = s.softEqEnabled, colors = sliderColors, modifier = Modifier.weight(1f))
                        Text("%+.1f".format(gain), color = onBg.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
                    }
                }
            }
            GlassSection("Visualizer") {
                val granted = hasRecordAudio()
                val permLauncher = rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { ok -> if (ok) AudioEffectsHub.startVisualizerCapture(true) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Waveform visualizer", color = onBg, fontWeight = FontWeight.Medium)
                        Text("Needs microphone permission (used only for audio capture)", color = onBg.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    if (!granted) {
                        TextButton(onClick = { permLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) { Text("Allow") }
                    } else {
                        Switch(
                            checked = s.visualizerOn,
                            onCheckedChange = { on ->
                                if (on) AudioEffectsHub.startVisualizerCapture(true) else AudioEffectsHub.stopVisualizerCapture()
                            },
                            enabled = s.attached,
                            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black)
                        )
                    }
                }
                val w = AudioEffectsHub.waveform
                if (s.visualizerOn && w != null) {
                    Spacer(Modifier.height(10.dp))
                    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
                        val step = size.width / w.size
                        val path = Path()
                        for (i in w.indices) {
                            val x = i * step + step / 2
                            val y = size.height - w[i] * size.height
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, accent, style = Stroke(width = 3.dp.toPx()))
                    }
                }
            }
            GlassSection("Balance & normalization") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L", color = onBg.copy(alpha = 0.8f), fontSize = 13.sp)
                    Slider(value = s.balance.toFloat(), onValueChange = { AudioEffectsHub.setBalance(it.toInt()) },
                        valueRange = -100f..100f, colors = sliderColors, modifier = Modifier.weight(1f))
                    Text("R", color = onBg.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (s.balance == 0) "Center" else "${if (s.balance > 0) "R" else "L"} +${kotlin.math.abs(s.balance)}%",
                        color = onBg.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.width(64.dp), textAlign = TextAlign.End
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Volume normalization", color = onBg, fontWeight = FontWeight.Medium)
                        Text("Approximate: evens out loud/quiet tracks", color = onBg.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Switch(checked = s.normalize, onCheckedChange = { AudioEffectsHub.setNormalization(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black))
                }
            }
            GlassSection("Reverb") {
                if (!s.reverbSupported) Text("Reverb is not supported on this device.", color = onBg.copy(alpha = 0.75f))
                else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AudioEffectsHub.REVERB_PRESETS.map { it.first }) { name ->
                        PillChip(name, s.reverbPreset == name, accent) { AudioEffectsHub.setReverbPreset(name) }
                    }
                }
            }
            GlassSection("Equalizer") {
                if (!s.eqSupported) Text("This device does not provide a hardware equalizer.", color = onBg.copy(alpha = 0.75f))
                else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.systemPresets.size) { i -> val name = s.systemPresets[i]; PillChip(name, s.activePreset == name, accent) { AudioEffectsHub.usePreset(i) } }
                        items(custom, key = { it.name }) { p -> PillChip("★ ${p.name}", s.activePreset == p.name, accent) { vm.applyCustomPreset(p) } }
                    }
                    Spacer(Modifier.height(8.dp))
                    s.bands.forEachIndexed { i, b ->
                        val level = s.levels.getOrElse(i) { 0 }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(freqLabel(b.freqHz), color = onBg.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.width(64.dp))
                            Slider(value = level.toFloat(), onValueChange = { AudioEffectsHub.setBandLevel(i, it.toInt()) },
                                valueRange = b.minMb.toFloat()..b.maxMb.toFloat(), enabled = s.enabled, colors = sliderColors, modifier = Modifier.weight(1f))
                            Text("%+.1f dB".format(level / 100f), color = onBg.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                        }
                    }
                    TextButton(onClick = { showSave = true }, enabled = s.enabled) { Text("Save as custom preset") }
                }
            }
            if (custom.isNotEmpty()) GlassSection("My presets") {
                custom.forEach { p -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, color = onBg, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteCustomPreset(p.name) }) { Icon(Icons.Rounded.Close, "Delete preset", tint = onBg.copy(alpha = 0.7f)) }
                } }
            }
            GlassSection("Bass Boost") { EffectSlider(s.bassSupported, s.enabled, s.bass, 1000f, accent, "${s.bass / 10}%", "Bass Boost is not supported on this device.") { AudioEffectsHub.setBass(it) } }
            GlassSection("Virtualizer") { EffectSlider(s.virtSupported, s.enabled, s.virt, 1000f, accent, "${s.virt / 10}%", "Virtualizer is not supported on this device.") { AudioEffectsHub.setVirtualizer(it) } }
            GlassSection("Loudness Enhancer") { EffectSlider(s.loudSupported, s.enabled, s.loud, 1000f, accent, "+%.1f dB".format(s.loud / 100f), "Loudness Enhancer is not supported on this device.") { AudioEffectsHub.setLoudness(it) } }
            Text(
                "EQ settings are remembered separately for ${android.os.Build.MODEL}.",
                color = onBg.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showSave) TextInputDialog(title = "Preset name", onConfirm = { vm.saveCustomPreset(it); showSave = false }, onDismiss = { showSave = false })
}
@Composable
private fun EffectSlider(supported: Boolean, enabled: Boolean, value: Int, max: Float, accent: Color, label: String, unsupported: String, onChange: (Int) -> Unit) {
    val onBg = MaterialTheme.colorScheme.onBackground
    if (!supported) { Text(unsupported, color = onBg.copy(alpha = 0.75f)); return }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..max, enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = onBg.copy(alpha = 0.2f)), modifier = Modifier.weight(1f))
        Text(label, color = onBg.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
    }
}
private fun freqLabel(hz: Int): String = if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else "$hz Hz"

@Composable
fun hasRecordAudio(): Boolean {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return androidx.core.content.ContextCompat.checkSelfPermission(
        ctx, android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
