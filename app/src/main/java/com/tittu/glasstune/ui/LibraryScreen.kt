package com.tittu.glasstune.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tittu.glasstune.data.Song
import com.tittu.glasstune.data.db.PlaylistInfo
import java.util.Calendar

private val tabs = listOf("Songs", "Albums", "Artists", "Folders", "Favorites", "Recent", "Playlists")

@Composable
fun LibraryScreen(
    vm: MusicViewModel,
    accent: Color,
    onOpenEq: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var group by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    var openPlaylistId by remember { mutableStateOf<Long?>(null) }
    var addTarget by remember { mutableStateOf<Song?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var createFor by remember { mutableStateOf<Song?>(null) }

    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val openPlaylist = playlists.firstOrNull { it.id == openPlaylistId }

    BackHandler(enabled = openPlaylist != null) { openPlaylistId = null }
    BackHandler(enabled = group != null && openPlaylist == null) { group = null }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importM3u(uri)
    }

    val base = remember(vm.songs, vm.query, vm.sort) { vm.filteredSongs() }
    val onBg = MaterialTheme.colorScheme.onBackground

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        if (openPlaylist != null) {
            PlaylistDetail(vm, accent, openPlaylist) { openPlaylistId = null }
        } else {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), color = onBg.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text("GlassTune", color = onBg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                GlassIconButton(Icons.Rounded.Refresh, "Refresh library", { vm.loadLibrary() })
                Spacer(Modifier.width(8.dp))
                GlassIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
            }

            // Search
            TextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                singleLine = true,
                placeholder = { Text("Search songs, artists, albums") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (vm.query.isNotEmpty()) {
                        Icon(Icons.Rounded.Close, "Clear", Modifier.clickable { vm.query = "" })
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).glass(RoundedCornerShape(26.dp))
            )

            val g = group
            if (g == null) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs.size) { i -> PillChip(tabs[i], vm.tab == i, accent) { vm.tab = i } }
                    item { PillChip("Sort: ${vm.sort.label}", false, accent) { vm.cycleSort() } }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(Icons.Rounded.ArrowBack, "Back", { group = null }, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(g.first, color = onBg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val onAdd: (Song) -> Unit = { addTarget = it }
                when {
                    g != null -> SongList(vm, accent, g.second, onAddToPlaylist = onAdd)
                    vm.tab == 6 -> PlaylistsTab(
                        playlists, accent,
                        onOpen = { openPlaylistId = it },
                        onNew = { createFor = null; showCreate = true },
                        onImport = { importLauncher.launch(arrayOf("*/*")) }
                    )
                    vm.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = accent)
                    vm.songs.isEmpty() -> Message("No music found on this device.\nAdd audio files and tap refresh.")
                    else -> when (vm.tab) {
                        0 -> SongList(vm, accent, base, onAddToPlaylist = onAdd)
                        1 -> GroupList(base.groupBy { it.album }) { k, v -> group = k to v }
                        2 -> GroupList(base.groupBy { it.artist }) { k, v -> group = k to v }
                        3 -> GroupList(base.groupBy { it.folder }) { k, v -> group = k to v }
                        4 -> SongList(vm, accent, base.filter { it.id in vm.favorites }, "No favorites yet.\nTap the heart on a song.", onAdd)
                        else -> SongList(vm, accent, base.sortedByDescending { it.dateAdded }.take(50), onAddToPlaylist = onAdd)
                    }
                }
            }
        }
    }

    // Add-to-playlist dialog
    addTarget?.let { song ->
        AlertDialog(
            onDismissRequest = { addTarget = null },
            title = { Text("Add to playlist") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (playlists.isEmpty()) Text("You have no playlists yet.")
                    playlists.forEach { p ->
                        Text(
                            "${p.name}  (${p.songCount})",
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.addToPlaylist(p.id, song); addTarget = null }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { createFor = song; addTarget = null; showCreate = true }) { Text("New playlist") }
            },
            dismissButton = { TextButton(onClick = { addTarget = null }) { Text("Close") } }
        )
    }

    if (showCreate) {
        TextInputDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { vm.createPlaylist(it, createFor); showCreate = false; createFor = null },
            onDismiss = { showCreate = false; createFor = null }
        )
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 16.sp)
    }
}

@Composable
private fun SongList(
    vm: MusicViewModel,
    accent: Color,
    list: List<Song>,
    empty: String = "Nothing here.",
    onAddToPlaylist: (Song) -> Unit
) {
    if (list.isEmpty()) {
        Message(empty)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(list, key = { _, s -> s.id }) { index, song ->
            SongRow(
                song = song,
                current = vm.currentSong?.id == song.id,
                fav = song.id in vm.favorites,
                accent = accent,
                onClick = { vm.play(list, index) },
                onFav = { vm.toggleFavorite(song.id) },
                onPlayNext = { vm.playNext(song) },
                onAddQueue = { vm.addToQueue(song) },
                onAddPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song, current: Boolean, fav: Boolean, accent: Color,
    onClick: () -> Unit, onFav: () -> Unit, onPlayNext: () -> Unit,
    onAddQueue: () -> Unit, onAddPlaylist: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().glass().clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtBox(song, Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = if (current) accent else onBg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} • ${formatTime(song.durationMs)}", color = onBg.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFav) {
            Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (fav) accent else onBg.copy(alpha = 0.7f))
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = onBg.copy(alpha = 0.7f)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; onPlayNext() })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menu = false; onAddQueue() })
                DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menu = false; onAddPlaylist() })
            }
        }
    }
}

@Composable
private fun GroupList(groups: Map<String, List<Song>>, onOpen: (String, List<Song>) -> Unit) {
    if (groups.isEmpty()) {
        Message("Nothing here.")
        return
    }
    val onBg = MaterialTheme.colorScheme.onBackground
    val entries = remember(groups) { groups.entries.toList() }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(entries, key = { it.key }) { e ->
            Row(
                Modifier.fillMaxWidth().glass().clickable { onOpen(e.key, e.value) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtBox(e.value.first(), Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.key, color = onBg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${e.value.size} songs", color = onBg.copy(alpha = 0.65f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistInfo>,
    accent: Color,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNew,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("New playlist")
                }
                OutlinedButton(onClick = onImport) { Text("Import M3U") }
            }
        }
        if (playlists.isEmpty()) {
            item { Text("No playlists yet.", color = onBg.copy(alpha = 0.7f), modifier = Modifier.padding(8.dp)) }
        }
        items(playlists, key = { it.id }) { p ->
            Row(
                Modifier.fillMaxWidth().glass().clickable { onOpen(p.id) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.QueueMusic, null, tint = accent)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = onBg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${p.songCount} songs", color = onBg.copy(alpha = 0.65f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(vm: MusicViewModel, accent: Color, pl: PlaylistInfo, onBack: () -> Unit) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val flow = remember(pl.id) { vm.playlistItems(pl.id) }
    val items by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        if (uri != null) vm.exportPlaylistM3u(pl.id, uri)
    }
    val songs = items.map { it.song }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back", onBack, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pl.name, color = onBg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${songs.size} songs", color = onBg.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { PillChip("▶ Play all", true, accent) { vm.play(songs, 0) } }
            item { PillChip("Export M3U", false, accent) { exportLauncher.launch("${pl.name}.m3u") } }
            item { PillChip("Rename", false, accent) { rename = true } }
            item { PillChip("Delete", false, accent) { confirmDelete = true } }
        }
        if (items.isEmpty()) {
            Message("This playlist is empty.\nUse \"Add to playlist\" from a song's menu.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(items, key = { _, row -> row.rowId }) { index, item ->
                    Row(
                        Modifier.fillMaxWidth().glass().clickable { vm.play(songs, index) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtBox(item.song, Modifier.size(46.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.song.title,
                                color = if (vm.currentSong?.id == item.song.id) accent else onBg,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(item.song.artist, color = onBg.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { vm.movePlaylistItem(pl.id, item.rowId, -1) }) {
                            Icon(Icons.Rounded.KeyboardArrowUp, "Move up", tint = onBg.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { vm.movePlaylistItem(pl.id, item.rowId, 1) }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, "Move down", tint = onBg.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { vm.removeFromPlaylist(item.rowId) }) {
                            Icon(Icons.Rounded.Close, "Remove", tint = onBg.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }

    if (rename) {
        TextInputDialog(
            title = "Rename playlist",
            initial = pl.name,
            onConfirm = { vm.renamePlaylist(pl.id, it); rename = false },
            onDismiss = { rename = false }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("\"${pl.name}\" will be removed. Your songs are not deleted.") },
            confirmButton = { TextButton(onClick = { vm.deletePlaylist(pl.id); confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ArtBox(song: Song, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).glass(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        AsyncImage(
            model = song.artUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}