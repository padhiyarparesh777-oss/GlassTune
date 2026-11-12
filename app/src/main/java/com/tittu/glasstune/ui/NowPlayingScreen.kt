package com.tittu.glasstune.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    Row(modifier.navigationBarsPadding().padding(14.dp).fillMaxWidth().glass(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ArtBox(song, Modifier.size(44.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(song.artist, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), fontSize = 12.sp)
        }
        IconButton(onClick = vm::playPause) { Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = accent) }
        IconButton(onClick = vm::next) { Icon(Icons.Rounded.SkipNext, "Next") }
    }
}

@Composable
fun NowPlayingScreen(vm: MusicViewModel, accent: Color, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val song = vm.currentSong
    var showQueue by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0B1020)).statusBarsPadding().navigationBarsPadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close", tint = Color.White) }
            Text("Now Playing", color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = { showQueue = !showQueue }) { Icon(Icons.Rounded.QueueMusic, "Queue", tint = Color.White) }
        }
        if (showQueue) {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f).fillMaxWidth().glass()) {
                items(vm.queue.size) { i ->
                    val q = vm.queue[i]
                    Row(Modifier.fillMaxWidth().clickable { vm.skipTo(i) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(q.title, color = Color.White)
                            Text(q.artist, color = Color.White.copy(alpha = .6f), fontSize = 12.sp)
                        }
                        IconButton(onClick = { vm.removeFromQueue(i) }) { Icon(Icons.Rounded.Close, "Remove", tint = Color.White) }
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth().aspectRatio(1f).glass(RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                if (song != null) AsyncImage(model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)))
                else Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(80.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(song?.title ?: "No song selected", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song?.artist ?: "", color = Color.White.copy(alpha = .7f))
        Slider(value = vm.positionMs.toFloat().coerceIn(0f, vm.durationMs.coerceAtLeast(1L).toFloat()), onValueChange = { vm.seekTo(it.toLong()) }, valueRange = 0f..vm.durationMs.coerceAtLeast(1L).toFloat(), colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(vm.positionMs), color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(formatTime(vm.durationMs), color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::toggleShuffle) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (vm.shuffle) accent else Color.White) }
            IconButton(onClick = vm::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(34.dp)) }
            Box(Modifier.size(68.dp).clip(CircleShape).background(accent).clickable { vm.playPause() }, contentAlignment = Alignment.Center) {
                Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = Color.Black, modifier = Modifier.size(38.dp))
            }
            IconButton(onClick = vm::next) { Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(34.dp)) }
            IconButton(onClick = vm::cycleRepeat) { Icon(if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) Color.White else accent) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = vm::cycleSleepTimer) { Text(if (vm.sleepMinutes == 0) "Sleep timer" else "${vm.sleepMinutes}m", color = Color.White) }
            TextButton(onClick = vm::cycleSpeed) { Text("${vm.speed}x", color = Color.White) }
            IconButton(onClick = { song?.let { vm.toggleFavorite(it.id) } }) { Icon(if (song != null && song.id in vm.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = accent) }
        }
    }
}
