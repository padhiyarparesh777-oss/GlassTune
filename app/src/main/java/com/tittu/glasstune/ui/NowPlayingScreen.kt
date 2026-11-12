package com.tittu.glasstune.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.media3.common.Player
import com.tittu.glasstune.data.Song

private val SilverText = Color(0xFFF3F6F8)
private val MutedSilver = Color(0xFFCBD3D8)

@Composable
fun MiniPlayer(vm: MusicViewModel, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val song = vm.currentSong ?: return
    Row(
        modifier.navigationBarsPadding().padding(14.dp).fillMaxWidth()
            .glass(RoundedCornerShape(24.dp)).rubberClickable(onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtBox(song, Modifier.size(44.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = SilverText, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(song.artist, color = MutedSilver, fontSize = 12.sp)
        }
        GlassIconButton(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", vm::playPause, size = 42.dp)
        Spacer(Modifier.width(6.dp))
        GlassIconButton(Icons.Rounded.SkipNext, "Next", vm::next, size = 42.dp)
    }
}

@Composable
fun NowPlayingScreen(vm: MusicViewModel, accent: Color, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val song = vm.currentSong
    var menuExpanded by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    val fg = SilverText
    val cardColor = androidx.compose.ui.graphics.lerp(Color(0xFF4A5862), accent, 0.15f)

    BoxWithConstraints(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        val cardWidth = minOf(maxWidth, 370.dp)
        Column(
            Modifier.width(cardWidth).fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(36.dp))
                .background(cardColor)
                .glass(RoundedCornerShape(36.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top identity row, matching the reference player's compact proportions.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close player", onClose, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tittu", color = fg, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("NOW PLAYING", color = MutedSilver, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                Box {
                    GlassIconButton(Icons.Rounded.MoreVert, "More options", { menuExpanded = !menuExpanded }, size = 42.dp)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = menuExpanded,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.94f),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.96f),
                        modifier = Modifier.align(Alignment.TopEnd).offset(y = 50.dp)
                    ) {
                        Column(
                            Modifier.width(224.dp).glass(RoundedCornerShape(26.dp)).padding(vertical = 7.dp, horizontal = 6.dp)
                        ) {
                            PlayerMenuItem(Icons.Rounded.Equalizer, "Equalizer") { menuExpanded = false; showEqualizer = true }
                            PlayerMenuItem(Icons.Rounded.QueueMusic, "Playlist / Queue") { menuExpanded = false; showQueue = true }
                            PlayerMenuItem(Icons.Rounded.Info, "Song info") { menuExpanded = false; showInfo = true }
                            PlayerMenuItem(Icons.Rounded.Share, "Share file") {
                                menuExpanded = false
                                song?.let {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, it.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Share song"))
                                }
                            }
                            PlayerMenuItem(Icons.Rounded.Bedtime, if (vm.sleepMinutes == 0) "Sleep timer" else "Sleep timer · ${vm.sleepMinutes}m") {
                                menuExpanded = false; vm.cycleSleepTimer()
                            }
                            PlayerMenuItem(
                                if (song != null && song.id in vm.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                if (song != null && song.id in vm.favorites) "Remove favorite" else "Add to favorite"
                            ) {
                                menuExpanded = false; song?.let { vm.toggleFavorite(it.id) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Album-art window uses translucent glass instead of a hard black inset.
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.07f)).glass(RoundedCornerShape(26.dp)).padding(7.dp),
                contentAlignment = Alignment.Center
            ) {
                if (song != null) {
                    AsyncImage(
                        model = song.artUri,
                        contentDescription = "Album artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp))
                    )
                } else {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)).background(Color.White.copy(alpha = 0.045f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.42f), modifier = Modifier.size(72.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song?.title ?: "No song selected", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song?.artist ?: "Choose a song from your library", color = MutedSilver, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                GlassIconButton(
                    if (song != null && song.id in vm.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Favorite", { song?.let { vm.toggleFavorite(it.id) } }, size = 40.dp,
                    tint = if (song != null && song.id in vm.favorites) Color(0xFFFFE0E5) else fg
                )
                GlassIconButton(Icons.Rounded.Shuffle, "Shuffle", vm::toggleShuffle, size = 40.dp, tint = if (vm.shuffle) Color.White else MutedSilver)
            }

            Spacer(Modifier.height(8.dp))
            Slider(
                value = vm.positionMs.toFloat().coerceIn(0f, vm.durationMs.coerceAtLeast(1L).toFloat()),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange = 0f..vm.durationMs.coerceAtLeast(1L).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color(0xFFF0F4F6),
                    inactiveTrackColor = Color(0xFF3D4A52).copy(alpha = .72f)
                ),
                modifier = Modifier.height(28.dp)
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(vm.positionMs), color = MutedSilver, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("-${formatTime((vm.durationMs - vm.positionMs).coerceAtLeast(0L))}", color = MutedSilver, fontSize = 11.sp)
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.SkipPrevious, "Previous", vm::previous, size = 48.dp)
                Box(Modifier.size(66.dp).glass(CircleShape).rubberClickable(vm::playPause), contentAlignment = Alignment.Center) {
                    Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                GlassIconButton(Icons.Rounded.SkipNext, "Next", vm::next, size = 48.dp)
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeDown, null, tint = MutedSilver, modifier = Modifier.size(20.dp))
                Slider(
                    value = vm.volumeLevel,
                    onValueChange = vm::setVolume,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(28.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White, activeTrackColor = Color(0xFFE8EEF1),
                        inactiveTrackColor = Color(0xFF3D4A52)
                    )
                )
                Icon(Icons.Rounded.VolumeUp, null, tint = MutedSilver, modifier = Modifier.size(22.dp))
            }
        }
    }

    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            containerColor = cardColor,
            shape = RoundedCornerShape(30.dp),
            title = { Text("Playlist / Queue", color = fg) },
            text = {
                if (vm.queue.isEmpty()) Text("Queue is empty", color = MutedSilver)
                else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    itemsIndexed(vm.queue) { index, item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(RoundedCornerShape(14.dp))
                                .rubberClickable { vm.skipTo(index) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.MusicNote, null, tint = MutedSilver)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.artist, color = MutedSilver, fontSize = 11.sp)
                            }
                            if (index == vm.currentIndex) Icon(Icons.Rounded.GraphicEq, null, tint = fg)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQueue = false }) { Text("Done", color = fg) } }
        )
    }

    if (showInfo && song != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = Color(0xFF66747D),
            title = { Text("Song information", color = fg) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoLine("Title", song.title)
                    InfoLine("Artist", song.artist)
                    InfoLine("Album", song.album)
                    InfoLine("Duration", formatTime(song.durationMs))
                    InfoLine("Folder", song.folder)
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close", color = fg) } }
        )
    }

    if (showEqualizer) {
        AlertDialog(
            onDismissRequest = { showEqualizer = false },
            containerColor = Color(0xFF66747D),
            title = { Text("Equalizer", color = fg) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Open your phone's supported audio effects panel.", color = MutedSilver)
                    Text("Equalizer availability depends on the device.", color = MutedSilver, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEqualizer = false
                    try {
                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                }) { Text("Open equalizer", color = fg) }
            },
            dismissButton = { TextButton(onClick = { showEqualizer = false }) { Text("Close", color = fg) } }
        )
    }
}

@Composable
private fun PlayerMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MutedSilver, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, color = SilverText, fontSize = 13.sp)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = MutedSilver, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value.ifBlank { "Unknown" }, color = SilverText, fontSize = 13.sp)
    }
}
