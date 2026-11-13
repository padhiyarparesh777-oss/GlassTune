package com.tittu.glasstune.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

enum class AppScreen { HOME, EQ, SETTINGS }

@Composable
fun GlassTuneApp(vm: MusicViewModel) {
    val ctx = LocalContext.current
    val audioPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    val perms = if (Build.VERSION.SDK_INT >= 33) arrayOf(audioPerm, Manifest.permission.POST_NOTIFICATIONS)
    else arrayOf(audioPerm)

    fun hasAudio() = ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasAudio()) }
    var denied by remember { mutableStateOf(false) }
    var showNow by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(AppScreen.HOME) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAudio()
        denied = !granted
    }
    LaunchedEffect(granted) { if (granted) vm.loadLibrary() }

    val accent = if (vm.dynamicThemeEnabled) vm.dynamicAccent ?: AccentColors[vm.accentIndex] else AccentColors[vm.accentIndex]
    val wallpaper = vm.currentWallpaper

    GlassTuneTheme(vm.themeMode, accent) {
        GlassBackground(accent, wallpaper, true) {
            if (!granted) {
                PermissionScreen(
                    denied = denied,
                    accent = accent,
                    onAllow = { launcher.launch(perms) },
                    onSettings = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                        )
                    }
                )
            } else {
                LibraryScreen(
                    vm, accent,
                    onOpenEq = { screen = AppScreen.EQ },
                    onOpenSettings = { screen = AppScreen.SETTINGS },
                    modifier = Modifier.fillMaxSize()
                )
                if (vm.currentSong != null) {
                    MiniPlayer(vm, accent, onClick = { showNow = true }, modifier = Modifier.align(Alignment.BottomCenter))
                }
                if (screen == AppScreen.EQ) {
                    GlassBackground(accent, wallpaper, true) { EqualizerScreen(vm, accent, onClose = { screen = AppScreen.HOME }) }
                }
                if (screen == AppScreen.SETTINGS) {
                    GlassBackground(accent, wallpaper, true) { SettingsScreen(vm, accent, onClose = { screen = AppScreen.HOME }, onOpenEq = { screen = AppScreen.EQ }) }
                }
                AnimatedVisibility(
                    visible = showNow && vm.currentSong != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    GlassTuneTheme(ThemeMode.DARK, accent) {
                        NowPlayingScreen(
                            vm, accent,
                            dynamicWallpaper = vm.dynamicThemeEnabled,
                            onClose = { showNow = false },
                            onOpenEq = { showNow = false; screen = AppScreen.EQ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(denied: Boolean, accent: Color, onAllow: () -> Unit, onSettings: () -> Unit) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp)).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Welcome to GlassTune", color = onBg, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                if (denied) "Permission was denied. GlassTune needs access to your audio files to show your music. You can enable it in app settings."
                else "Allow access to your music files so GlassTune can build your library. Everything stays on your device.",
                color = onBg.copy(alpha = 0.75f), textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAllow,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
            ) { Text("Allow access") }
            if (denied) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = onBg)
                ) { Text("Open app settings") }
            }
        }
    }
}