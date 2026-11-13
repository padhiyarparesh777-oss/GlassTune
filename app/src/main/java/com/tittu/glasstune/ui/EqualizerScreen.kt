package com.tittu.glasstune.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tittu.glasstune.audio.AudioEffectsHub

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
