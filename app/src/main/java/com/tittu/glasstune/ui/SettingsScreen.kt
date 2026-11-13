package com.tittu.glasstune.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(vm: MusicViewModel, accent: Color, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val onBg = MaterialTheme.colorScheme.onBackground
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.backupTo(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restoreFrom(uri)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back", onClose, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text("Settings", color = onBg, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        GlassSection("Appearance") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemeMode.entries.toList()) { m -> PillChip(m.label, vm.themeMode == m, accent) { vm.setTheme(m) } }
            }
            Spacer(Modifier.height(14.dp))
            Text("Accent color", color = onBg.copy(alpha = 0.75f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentColors.forEachIndexed { i, c ->
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c).border(if (vm.accentIndex == i) 3.dp else 0.dp, onBg, CircleShape).clickable { vm.setAccent(i) })
                }
            }
        }
        GlassSection("Library") {
            Text("${vm.songs.size} songs found on this device", color = onBg.copy(alpha = 0.8f))
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.loadLibrary() }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) { Text("Rescan library") }
        }
        GlassSection("Backup & restore") {
            Text("Saves playlists, favorites, equalizer presets and settings, lyrics you added, and theme to a JSON file. Songs are matched by file path when restoring.", color = onBg.copy(alpha = 0.75f), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { backupLauncher.launch("glasstune-backup.json") }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) { Text("Backup") }
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("*/*")) }) { Text("Restore") }
            }
        }
        GlassSection("About") {
            Text("GlassTune 1.0", color = onBg, fontWeight = FontWeight.Medium)
            Text("Offline music player with a frosted-glass interface.", color = onBg.copy(alpha = 0.75f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}