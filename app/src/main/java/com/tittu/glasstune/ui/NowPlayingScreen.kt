package com.tittu.glasstune.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.media3.common.Player

@Composable
fun MiniPlayer(vm: MusicViewModel, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val song = vm.currentSong ?: return
    Row(
        modifier.navigationBarsPadding().padding(14.dp).fillMaxWidth()
            .glass(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtBox(song, Modifier.size(44.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(song.artist, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f), fontSize = 12.sp)
        }
        GlassIconButton(Icons.Rounded.PlayArrow.takeIf { !vm.isPlaying } ?: Icons.Rounded.Pause, "Play/Pause", vm::playPause, size = 42.dp)
        Spacer(Modifier.width(6.dp))
        GlassIconButton(Icons.Rounded.SkipNext, "Next", vm::next, size = 42.dp)
    }
}

@Composable
fun NowPlayingScreen(vm: MusicViewModel, accent: Color, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val song = vm.currentSong
    val fg = MaterialTheme.colorScheme.onBackground
    var showQueue by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close", onClose, size = 42.dp)
            Text("Now Playing", color = fg, modifier = Modifier.weight(1f).padding(start = 14.dp), fontWeight = FontWeight.SemiBold)
            GlassIconButton(Icons.Rounded.QueueMusic, "Queue", { showQueue = !showQueue }, size = 42.dp)
        }
        Spacer(Modifier.height(18.dp))
        if (showQueue) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(8.dp)) {
                items(vm.queue.size) { i ->
                    val q = vm.queue[i]
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(RoundedCornerShape(16.dp))
                            .clickable { vm.skipTo(i) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(q.title, color = fg)
                            Text(q.artist, color = fg.copy(alpha = .65f), fontSize = 12.sp)
                        }
                        GlassIconButton(Icons.Rounded.Close, "Remove", { vm.removeFromQueue(i) }, size = 36.dp)
                    }
                }
            }
        } else {
            Box(
                Modifier.weight(1f).fillMaxWidth().aspectRatio(1f)
                    .glass(RoundedCornerShape(30.dp)).padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (song != null) {
                    AsyncImage(
                        model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(23.dp))
                    )
                } else {
                    Icon(Icons.Rounded.MusicNote, null, tint = fg, modifier = Modifier.size(80.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(song?.title ?: "No song selected", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song?.artist ?: "", color = fg.copy(alpha = .72f))
        Spacer(Modifier.height(12.dp))
        Slider(
            value = vm.positionMs.toFloat().coerceIn(0f, vm.durationMs.coerceAtLeast(1L).toFloat()),
            onValueChange = { vm.seekTo(it.toLong()) },
            valueRange = 0f..vm.durationMs.coerceAtLeast(1L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFF2F5F7), activeTrackColor = Color(0xFFE0E7EB),
                inactiveTrackColor = Color(0xFF46545D).copy(alpha = .55f)
            )
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(vm.positionMs), color = fg.copy(alpha = .7f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(formatTime(vm.durationMs), color = fg.copy(alpha = .7f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.Shuffle, "Shuffle", vm::toggleShuffle, size = 44.dp, tint = if (vm.shuffle) Color.White else fg)
            GlassIconButton(Icons.Rounded.SkipPrevious, "Previous", vm::previous, size = 52.dp)
            Box(
                Modifier.size(72.dp).glass(CircleShape).clickable { vm.playPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            GlassIconButton(Icons.Rounded.SkipNext, "Next", vm::next, size = 52.dp)
            GlassIconButton(
                if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Repeat", vm::cycleRepeat, size = 44.dp,
                tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) fg else Color.White
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::cycleSleepTimer) { Text(if (vm.sleepMinutes == 0) "Sleep timer" else "${vm.sleepMinutes}m", color = fg) }
            TextButton(onClick = vm::cycleSpeed) { Text("${vm.speed}x", color = fg) }
            GlassIconButton(
                if (song != null && song.id in vm.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Favorite", { song?.let { vm.toggleFavorite(it.id) } }, size = 42.dp
            )
        }
    }
}
