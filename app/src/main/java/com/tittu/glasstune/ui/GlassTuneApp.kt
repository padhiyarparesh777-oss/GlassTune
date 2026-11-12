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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
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
    var showQuick by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsStartPage by remember { mutableStateOf("HOME") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAudio()
        denied = !granted
    }
    LaunchedEffect(granted) { if (granted) vm.loadLibrary() }

    val accent = AccentColors[vm.accentIndex]

    GlassTuneTheme(vm.themeMode, accent) {
        GlassBackground(accent) {
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
                val overlayVisible = showNow || showQuick || showSettings
                Box(Modifier.fillMaxSize().then(if (overlayVisible) Modifier.blur(18.dp) else Modifier)) {
                    LibraryScreen(vm, accent, Modifier.fillMaxSize(), onSettings = { showQuick = true })
                    if (vm.currentSong != null) {
                        MiniPlayer(vm, accent, onClick = { showNow = true }, modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
                if (overlayVisible) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().background(Color(0xFF1D2730).copy(alpha = 0.46f))
                    )
                }
                AnimatedVisibility(
                    visible = showNow && vm.currentSong != null && !showQuick && !showSettings,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NowPlayingScreen(vm, accent, onClose = { showNow = false })
                }
                if (showQuick) {
                    GlassTuneQuickOptions(
                        vm = vm,
                        onSettings = { showQuick = false; settingsStartPage = "HOME"; showSettings = true },
                        onClose = { showQuick = false },
                        onEqualizer = {
                            try { ctx.startActivity(Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, ctx.packageName)
                                putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, 0)
                            }) } catch (_: Exception) { }
                        },
                        onSleep = { vm.cycleSleepTimer() },
                        onStorage = { showQuick = false; settingsStartPage = "STORAGE"; showSettings = true }
                    )
                }
                if (showSettings) {
                    GlassTuneSettings(vm, onClose = { showSettings = false }, startPage = settingsStartPage)
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
            val allowInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onAllow,
                interactionSource = allowInteraction,
                modifier = Modifier.glass(RoundedCornerShape(20.dp)).rubberPress(allowInteraction),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White)
            ) { Text("Allow access") }
            if (denied) {
                Spacer(Modifier.height(8.dp))
                val settingsInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = onSettings,
                    interactionSource = settingsInteraction,
                    modifier = Modifier.glass(RoundedCornerShape(20.dp)).rubberPress(settingsInteraction),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = onBg)
                ) { Text("Open app settings") }
            }
        }
    }
}
