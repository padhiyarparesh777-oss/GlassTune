package com.tittu.glasstune.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.tittu.glasstune.data.LrcParser

private enum class Panel { ART, QUEUE, LYRICS }

@Composable
fun MiniPlayer(vm: MusicViewModel, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val song = vm.currentSong ?: return
    val onBg = MaterialTheme.colorScheme.onBackground
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtBox(song, Modifier.size(46.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = onBg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = onBg.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { vm.playPause() }) {
            Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = accent, modifier = Modifier.size(32.dp))
        }
        IconButton(onClick = { vm.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = onBg) }
    }
}

@Composable
fun NowPlayingScreen(vm: MusicViewModel, accent: Color, dynamicWallpaper: Boolean = true, onClose: () -> Unit, onOpenEq: () -> Unit) {
    BackHandler(onBack = onClose)
    val ctx = LocalContext.current
    val song = vm.currentSong
    val fav = song != null && song.id in vm.favorites
    var panel by remember { mutableStateOf(Panel.ART) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var menu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color(0xFF050609))) {
        if (dynamicWallpaper && song != null) {
            AsyncImage(
                model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(42.dp).alpha(0.48f)
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101116).copy(alpha = 0.68f), Color(0xFF050609).copy(alpha = 0.84f), Color.Black.copy(alpha = 0.94f)))))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Header: collapse | NOW PLAYING | menu ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close", onClose, size = 44.dp, tint = Color.White)
                Spacer(Modifier.width(16.dp))
                Text(
                    "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                Box {
                    GlassIconButton(Icons.Rounded.MoreVert, "Menu", { menu = true }, size = 44.dp, tint = Color.White)
                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false },
                        modifier = Modifier.background(Color(0xFF24262C))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Equalizer") },
                            leadingIcon = { Icon(Icons.Rounded.Equalizer, null) },
                            onClick = { menu = false; onOpenEq() }
                        )
                        DropdownMenuItem(
                            text = { Text("Playlist / Queue") },
                            leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                            onClick = { menu = false; panel = if (panel == Panel.QUEUE) Panel.ART else Panel.QUEUE }
                        )
                        DropdownMenuItem(
                            text = { Text("Lyrics") },
                            leadingIcon = { Icon(Icons.Rounded.Subject, null) },
                            onClick = { menu = false; panel = if (panel == Panel.LYRICS) Panel.ART else Panel.LYRICS }
                        )
                        DropdownMenuItem(
                            text = { Text("Song info") },
                            leadingIcon = { Icon(Icons.Rounded.Info, null) },
                            enabled = song != null,
                            onClick = { menu = false; showInfo = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Share file") },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                            enabled = song != null,
                            onClick = {
                                menu = false
                                if (song != null) {
                                    try {
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(Intent.EXTRA_STREAM, song.uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        ctx.startActivity(Intent.createChooser(send, "Share song"))
                                    } catch (e: Exception) { /* no app can handle it */ }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (vm.sleepMinutes > 0) "Sleep timer (${vm.sleepMinutes}m)" else "Sleep timer") },
                            leadingIcon = { Icon(Icons.Rounded.Timer, null) },
                            onClick = { menu = false; showSleep = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (fav) "Remove from favorite" else "Add to favorite") },
                            leadingIcon = { Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                            enabled = song != null,
                            onClick = { menu = false; song?.let { vm.toggleFavorite(it.id) } }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // ---- Panel: artwork / queue / lyrics ----
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (panel) {
                    Panel.QUEUE -> LazyColumn(Modifier.fillMaxSize().glass(RoundedCornerShape(24.dp)), contentPadding = PaddingValues(12.dp)) {
                        itemsIndexed(vm.queue) { i, q ->
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.skipTo(i) }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(q.title, color = if (i == vm.currentIndex) accent else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    Text(q.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { vm.removeFromQueue(i) }) { Icon(Icons.Rounded.Close, "Remove", tint = Color.White.copy(alpha = 0.7f)) }
                            }
                        }
                    }
                    Panel.LYRICS -> LyricsPanel(vm, accent)
                    Panel.ART -> Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).glass(RoundedCornerShape(32.dp)).padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(72.dp))
                        if (song != null) {
                            AsyncImage(
                                model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Title + artist | favorite | shuffle (no overlap) ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song?.title ?: "", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(song?.artist ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(16.dp))
                GlassIconButton(
                    if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite",
                    { song?.let { vm.toggleFavorite(it.id) } },
                    size = 48.dp, tint = if (fav) accent else Color.White
                )
                Spacer(Modifier.width(12.dp))
                GlassIconButton(
                    Icons.Rounded.Shuffle, "Shuffle", { vm.toggleShuffle() },
                    size = 48.dp, tint = if (vm.shuffle) accent else Color.White
                )
            }

            // ---- Seek bar ----
            val dur = vm.durationMs.coerceAtLeast(1L).toFloat()
            val pos = if (dragging) dragValue else vm.positionMs.toFloat().coerceIn(0f, dur)
            Spacer(Modifier.height(8.dp))
            Slider(
                value = pos,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = { vm.seekTo(dragValue.toLong()); dragging = false },
                valueRange = 0f..dur,
                colors = SliderDefaults.colors(
                    thumbColor = accent, activeTrackColor = accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(pos.toLong()), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(formatTime(vm.durationMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            // ---- Main controls ----
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.SkipPrevious, "Previous", { vm.previous() }, size = 60.dp, tint = Color.White)
                Box(
                    Modifier.size(84.dp).clip(CircleShape).background(accent).clickable { vm.playPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = Color.Black, modifier = Modifier.size(44.dp))
                }
                GlassIconButton(Icons.Rounded.SkipNext, "Next", { vm.next() }, size = 60.dp, tint = Color.White)
            }

            // ---- Secondary controls ----
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) Color.White else accent
                    )
                }
                TextButton(onClick = { vm.cycleSpeed() }) {
                    Icon(Icons.Rounded.Speed, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("${vm.speed}x", color = Color.White)
                }
                TextButton(onClick = { showSleep = true }) {
                    Icon(Icons.Rounded.Timer, null, tint = if (vm.sleepMinutes > 0) accent else Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(if (vm.sleepMinutes == 0) "Sleep" else "${vm.sleepMinutes}m", color = Color.White)
                }
            }
        }
    }

    // ---- Song info dialog ----
    if (showInfo && song != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Song info") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoLine("Title", song.title)
                    InfoLine("Artist", song.artist)
                    InfoLine("Album", song.album)
                    InfoLine("Duration", formatTime(song.durationMs))
                    InfoLine("Folder", song.folder)
                    InfoLine("Path", song.path.ifEmpty { "Unknown" })
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close") } }
        )
    }

    // ---- Sleep timer dialog ----
    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    listOf(0, 15, 30, 45, 60).forEach { m ->
                        Text(
                            (if (m == 0) "Off" else "$m minutes") + if (vm.sleepMinutes == m) "  ✓" else "",
                            color = if (vm.sleepMinutes == m) accent else Color.Unspecified,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.setSleepTimer(m); showSleep = false }
                                .padding(vertical = 12.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Music fades out during the last 10 seconds.", fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value)
    }
}

@Composable
private fun LyricsPanel(vm: MusicViewModel, accent: Color) {
    val text = vm.lyricsText
    var editing by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importLrc(uri)
    }
    val lines = remember(text) { if (text == null) emptyList() else LrcParser.parse(text) }

    Column(Modifier.fillMaxSize().glass(RoundedCornerShape(24.dp)).padding(12.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (text == null) {
                Text(
                    "No lyrics found for this song.\nAdd or paste lyrics, or import a .lrc file.",
                    color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (lines.isNotEmpty()) {
                val listState = rememberLazyListState()
                val current = lines.indexOfLast { it.timeMs <= vm.positionMs }
                LaunchedEffect(current) {
                    if (current >= 0) listState.animateScrollToItem((current - 2).coerceAtLeast(0))
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    itemsIndexed(lines) { i, l ->
                        Text(
                            l.text.ifEmpty { "♪" },
                            color = if (i == current) accent else Color.White.copy(alpha = 0.6f),
                            fontSize = if (i == current) 20.sp else 16.sp,
                            fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp).clickable { vm.seekTo(l.timeMs) }
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(text.orEmpty(), color = Color.White, fontSize = 16.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { editing = true }) { Text(if (text == null) "Add lyrics" else "Edit") }
            TextButton(onClick = { importer.launch(arrayOf("*/*")) }) { Text("Import .lrc") }
        }
    }

    if (editing) {
        var value by remember { mutableStateOf(text.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Lyrics") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("Paste plain lyrics or LRC with [mm:ss.xx] timestamps") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 320.dp)
                )
            },
            confirmButton = { TextButton(onClick = { vm.saveLyrics(value); editing = false }) { Text("Save") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.saveLyrics(""); editing = false }) { Text("Delete") }
                    TextButton(onClick = { editing = false }) { Text("Cancel") }
                }
            }
        )
    }
}