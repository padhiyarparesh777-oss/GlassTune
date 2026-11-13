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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri

@Composable
fun SettingsScreen(vm: MusicViewModel, accent: Color, onClose: () -> Unit, onOpenEq: () -> Unit) {
    BackHandler(onBack = onClose)
    val onBg = MaterialTheme.colorScheme.onBackground
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.backupTo(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restoreFrom(uri)
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.setSelectedFolder(uri)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back", onClose, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text("Settings", color = onBg, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        GlassSection("Appearance") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Song-based dynamic theme", color = onBg, fontWeight = FontWeight.Medium)
                    Text("Wallpaper and accent follow the playing song", color = onBg.copy(alpha = 0.65f), fontSize = 12.sp)
                }
                Switch(checked = vm.dynamicThemeEnabled, onCheckedChange = { vm.updateDynamicThemeEnabled(it) }, colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black))
            }
            Spacer(Modifier.height(12.dp))
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
        GlassSection("Marvel wallpapers") {
            Text("32 wallpapers • Same AUTO / FIXED switching as Reet Music Player", color = onBg.copy(alpha = 0.68f), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("AUTO", vm.wallpaperMode == "AUTO", accent) { vm.updateWallpaperMode("AUTO") }
                PillChip("FIXED", vm.wallpaperMode == "FIXED", accent) { vm.setWallpaperMode("FIXED") }
            }
            Spacer(Modifier.height(10.dp))
            Text("Current: ${vm.currentWallpaper?.substringBeforeLast('.')?.replace('_', ' ') ?: "No wallpaper"}", color = onBg, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.stepWallpaper(-1) }) { Text("Previous") }
                OutlinedButton(onClick = { vm.stepWallpaper(1) }) { Text("Next") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.marvelWallpapers) { name ->
                    PillChip(name.substringBeforeLast('.').replace('_', ' '), vm.currentWallpaper == name, accent) {
                        vm.chooseFixedWallpaper(name)
                        vm.setWallpaperMode("FIXED")
                    }
                }
            }
        }
        GlassSection("Playback & library") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Skip audio under 60 seconds", color = onBg, fontWeight = FontWeight.Medium)
                    Text("Short clips and notification sounds stay hidden", color = onBg.copy(alpha = 0.65f), fontSize = 12.sp)
                }
                Switch(checked = vm.hideShortAudio, onCheckedChange = vm::updateHideShortAudio, colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black))
            }
            Spacer(Modifier.height(14.dp))
            Text("Music folder", color = onBg, fontWeight = FontWeight.Medium)
            Text("Selected: ${vm.selectedFolderLabel}", color = onBg.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { folderLauncher.launch(null) }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) { Text("Choose folder") }
                OutlinedButton(onClick = { vm.clearSelectedFolder() }) { Text("All folders") }
            }
            Spacer(Modifier.height(14.dp))
            Text("${vm.filteredSongs().size} songs visible • ${vm.songs.size} total indexed", color = onBg.copy(alpha = 0.8f))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { vm.loadLibrary() }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) { Text("Rescan library") }
                OutlinedButton(onClick = onOpenEq) {
                    Icon(Icons.Rounded.Tune, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Equalizer")
                }
            }
        }
        GlassSection("Advanced library filters") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Skip short audio", color = onBg, fontWeight = FontWeight.Medium)
                    Text("Hide clips shorter than the selected duration", color = onBg.copy(alpha = 0.65f), fontSize = 12.sp)
                }
                Switch(checked = vm.skipShort, onCheckedChange = vm::updateSkipShort, colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black))
            }
            if (vm.skipShort) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(30, 60, 90, 120, 180, 300)) { sec ->
                        PillChip(if (sec >= 60) "${sec / 60} min" else "${sec} sec", vm.skipShortSeconds == sec, accent) { vm.updateSkipShortSeconds(sec) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show selected folders only", color = onBg, fontWeight = FontWeight.Medium)
                    Text("Choose exactly which music folders appear", color = onBg.copy(alpha = 0.65f), fontSize = 12.sp)
                }
                Switch(checked = vm.folderFilter, onCheckedChange = vm::updateFolderFilter, colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black))
            }
            if (vm.folderFilter) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::selectAllFolders) { Text("Select all") }
                    OutlinedButton(onClick = vm::clearFolders) { Text("Clear") }
                }
                Text(if (vm.selectedFolders.isEmpty()) "No folders selected — showing all folders." else "${vm.selectedFolders.size} selected", color = onBg.copy(alpha = 0.7f), fontSize = 12.sp)
                Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    vm.folders.forEach { folder ->
                        Row(Modifier.fillMaxWidth().clickable { vm.toggleFolder(folder.path) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = folder.path in vm.selectedFolders, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(folder.name, color = onBg, maxLines = 1)
                                Text(folder.displayPath, color = onBg.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                            }
                            Text("${folder.count}", color = onBg.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Showing ${vm.songs.size} of ${vm.totalSongCount} indexed songs", color = onBg.copy(alpha = 0.75f), fontSize = 12.sp)
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