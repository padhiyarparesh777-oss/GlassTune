package com.tittu.glasstune.ui

import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private enum class SettingsPage { HOME, APPEARANCE, PLAYBACK, LIBRARY, NOTIFICATIONS, PRIVACY, ABOUT, EQUALIZER, SLEEP, STORAGE }

@Composable
fun GlassTuneSettings(vm: MusicViewModel, onClose: () -> Unit) {
    var page by remember { mutableStateOf(SettingsPage.HOME) }
    BackHandler(page != SettingsPage.HOME) { page = SettingsPage.HOME }
    BackHandler(page == SettingsPage.HOME) { onClose() }
    val context = LocalContext.current
    val fg = MaterialTheme.colorScheme.onBackground
    val muted = fg.copy(alpha = .68f)

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back", { if (page == SettingsPage.HOME) onClose() else page = SettingsPage.HOME }, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (page == SettingsPage.HOME) "Tittu" else page.name.lowercase().replaceFirstChar { it.uppercase() }, color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("GLASSTUNE SETTINGS", color = muted, fontSize = 9.sp, letterSpacing = 2.sp)
            }
            GlassIconButton(Icons.Rounded.Close, "Close settings", onClose, size = 40.dp)
        }
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            when (page) {
                SettingsPage.HOME -> {
                    item { SettingsHeading("Personalize your player") }
                    item { SettingsRow(Icons.Rounded.Palette, "Appearance", "Themes, colors and visual style") { page = SettingsPage.APPEARANCE } }
                    item { SettingsRow(Icons.Rounded.PlayCircle, "Playback", "Playback speed, shuffle and repeat") { page = SettingsPage.PLAYBACK } }
                    item { SettingsRow(Icons.Rounded.LibraryMusic, "Music library", "Scan, sorting and your collection") { page = SettingsPage.LIBRARY } }
                    item { SettingsRow(Icons.Rounded.Notifications, "Notifications", "Android notification permission and player controls") { page = SettingsPage.NOTIFICATIONS } }
                    item { SettingsRow(Icons.Rounded.Shield, "Privacy & storage", "Local music access and storage details") { page = SettingsPage.PRIVACY } }
                    item { SettingsRow(Icons.Rounded.Info, "About GlassTune", "App information") { page = SettingsPage.ABOUT } }
                }
                SettingsPage.APPEARANCE -> {
                    item { SettingsHeading("Water-glass color themes") }
                    items(AccentNames.size) { i ->
                        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).rubberClickable { vm.setAccent(i) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).background(AccentColors[i], CircleShape))
                            Spacer(Modifier.width(14.dp))
                            Text(AccentNames[i], color = fg, modifier = Modifier.weight(1f))
                            if (vm.accentIndex == i) Icon(Icons.Rounded.CheckCircle, null, tint = fg)
                        }
                    }
                    item { SettingsRow(Icons.Rounded.Brightness6, "Surface mode", vm.themeMode.label) { vm.cycleTheme() } }
                    item { Text("Theme changes are saved automatically.", color = muted) }
                }
                SettingsPage.PLAYBACK -> {
                    item { SettingsHeading("Playback controls") }
                    item { SettingsRow(Icons.Rounded.Speed, "Playback speed", "${vm.speed}× · Tap to cycle") { vm.cycleSpeed() } }
                    item { SettingsRow(Icons.Rounded.Shuffle, "Shuffle", if (vm.shuffle) "On" else "Off") { vm.toggleShuffle() } }
                    item { SettingsRow(Icons.Rounded.Repeat, "Repeat mode", when(vm.repeatMode) { 1 -> "All"; 2 -> "One"; else -> "Off" }) { vm.cycleRepeat() } }
                    item { SettingsRow(Icons.Rounded.Bedtime, "Sleep timer", if(vm.sleepMinutes == 0) "Off · Tap to cycle 15 / 30 / 60 min" else "${vm.sleepMinutes} minutes · Tap to cycle") { vm.cycleSleepTimer() } }
                }
                SettingsPage.LIBRARY -> {
                    item { SettingsHeading("Music library") }
                    item { SettingsRow(Icons.Rounded.Sort, "Sort order", vm.sort.label + " · Tap to change") { vm.cycleSort() } }
                    item { SettingsRow(Icons.Rounded.Refresh, "Rescan music", "${vm.songs.size} tracks found · Tap to scan") { vm.loadLibrary() } }
                    item { Text("GlassTune reads audio files allowed by Android's media permission.", color = muted) }
                }
                SettingsPage.NOTIFICATIONS -> {
                    item { SettingsHeading("Android notification access") }
                    item { Text("Playback notification controls are managed by Android. Notification permission status is shown below.", color = muted) }
                    item {
                        val granted = if (android.os.Build.VERSION.SDK_INT >= 33) androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED else true
                        SettingsRow(Icons.Rounded.NotificationsActive, "Notification permission", if (granted) "Allowed" else "Not granted · Enable in Android App settings") {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null)))
                        }
                    }
                }
                SettingsPage.PRIVACY -> {
                    item { SettingsHeading("Device storage") }
                    item {
                        val stat = StatFs(context.filesDir.absolutePath)
                        val total = stat.totalBytes / (1024L * 1024L)
                        val free = stat.availableBytes / (1024L * 1024L)
                        SettingsRow(Icons.Rounded.Storage, "Internal storage", "${free} MB free of ${total} MB")
                    }
                    item { SettingsRow(Icons.Rounded.Folder, "App data", context.filesDir.absolutePath) }
                    item { Text("Your music library and favorites are stored locally. No account is required.", color = muted) }
                }
                SettingsPage.ABOUT -> {
                    item { SettingsHeading("About") }
                    item { SettingsRow(Icons.Rounded.MusicNote, "Application", "GlassTune · Tittu Edition") }
                    item { SettingsRow(Icons.Rounded.Code, "Version", "Debug build · Android") }
                }
                SettingsPage.EQUALIZER -> {
                    item { SettingsHeading("Equalizer") }
                    item { Text("Equalizer is provided by Android audio effects when supported by your device. Use the Equalizer shortcut from Quick Options to open the device panel.", color = muted) }
                }
                SettingsPage.SLEEP -> {
                    item { SettingsHeading("Sleep timer") }
                    item { SettingsRow(Icons.Rounded.Timer, "Timer", if (vm.sleepMinutes == 0) "Off · Tap to start" else "${vm.sleepMinutes} minutes remaining") { vm.cycleSleepTimer() } }
                    item { Text("Timer fades volume over the final 10 seconds, then pauses playback.", color = muted) }
                }
                SettingsPage.STORAGE -> {
                    item { SettingsHeading("Storage information") }
                    item {
                        val stat = StatFs(context.filesDir.absolutePath)
                        SettingsRow(Icons.Rounded.Storage, "Device storage", "${stat.availableBytes / (1024L*1024L)} MB available")
                    }
                    item { SettingsRow(Icons.Rounded.MusicNote, "Indexed songs", "${vm.songs.size} tracks") }
                }
            }
        }
    }
}

@Composable
fun GlassTuneQuickOptions(vm: MusicViewModel, onSettings: () -> Unit, onClose: () -> Unit, onEqualizer: () -> Unit, onSleep: () -> Unit, onStorage: () -> Unit) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quick Options", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GlassIconButton(Icons.Rounded.Close, "Close", onClose)
        }
        Spacer(Modifier.height(20.dp))
        SettingsRow(Icons.Rounded.Equalizer, "Equalizer", "Device audio effects") { onEqualizer() }
        SettingsRow(Icons.Rounded.Timer, "Sleep timer", if (vm.sleepMinutes == 0) "Off" else "${vm.sleepMinutes} minutes") { onSleep() }
        SettingsRow(Icons.Rounded.Storage, "Storage", "${vm.songs.size} songs indexed") { onStorage() }
        SettingsRow(Icons.Rounded.Settings, "Settings", "All GlassTune preferences") { onSettings() }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.rubberClickable(onClick) else Modifier)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .66f), fontSize = 11.sp)
        }
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
    }
}
