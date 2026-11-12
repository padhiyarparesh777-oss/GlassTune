package com.tittu.glasstune.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.tittu.glasstune.data.Song

@Composable
fun LibraryScreen(vm: MusicViewModel, accent: Color, modifier: Modifier = Modifier, onSettings: () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(0) }
    var showThemes by remember { mutableStateOf(false) }
    val songs = vm.filteredSongs()
    val shown = when (tab) {
        1 -> songs.filter { it.id in vm.favorites }
        2 -> songs.sortedByDescending { it.dateAdded }.take(50)
        else -> songs
    }
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your music", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
                Text("GlassTune", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            GlassIconButton(Icons.Rounded.Refresh, "Refresh", vm::loadLibrary, size = 42.dp)
            Spacer(Modifier.width(8.dp))
            GlassIconButton(Icons.Rounded.Palette, "Colors", { showThemes = true }, size = 42.dp)
            Spacer(Modifier.width(8.dp))
            GlassIconButton(Icons.Rounded.Settings, "Settings", onSettings, size = 42.dp)
        }
        TextField(
            value = vm.query, onValueChange = { vm.query = it }, singleLine = true,
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).glass(RoundedCornerShape(24.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Songs", "Favorites", "Recent").forEachIndexed { i, label ->
                LibraryTab(label, selected = tab == i, modifier = Modifier.weight(if (i == 1) 1.28f else 1f)) { tab = i }
            }
            LibraryTab(vm.sort.label, selected = false, modifier = Modifier.weight(0.92f), compact = true) { vm.cycleSort() }
        }
        when {
            vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (vm.songs.isEmpty()) "No music found. Add audio files and refresh." else "Nothing here yet.", color = MaterialTheme.colorScheme.onBackground)
            }
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(shown, key = { _, s -> s.id }) { index, song ->
                    Row(Modifier.fillMaxWidth().glass().rubberClickable { vm.play(shown, index) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtBox(song, Modifier.size(52.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = if (vm.currentSong?.id == song.id) accent else MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(song.artist + " • " + formatTime(song.durationMs), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), fontSize = 12.sp)
                        }
                        val favoriteInteraction = remember(song.id) { MutableInteractionSource() }
                        IconButton(onClick = { vm.toggleFavorite(song.id) }, interactionSource = favoriteInteraction, modifier = Modifier.rubberPress(favoriteInteraction)) {
                            Icon(if (song.id in vm.favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = accent)
                        }
                        val nextInteraction = remember(song.id) { MutableInteractionSource() }
                        IconButton(onClick = { vm.playNext(song) }, interactionSource = nextInteraction, modifier = Modifier.rubberPress(nextInteraction)) { Icon(Icons.Rounded.PlaylistAdd, "Play next") }
                    }
                }
            }
        }
    }
    if (showThemes) {
        AlertDialog(
            onDismissRequest = { showThemes = false },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Water Glass Themes", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose your bubble-glass color", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AccentNames.forEachIndexed { index, name ->
                        Row(
                            Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp))
                                .rubberClickable { vm.setAccent(index); showThemes = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(30.dp).background(AccentColors[index], CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = .65f), CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(name, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            if (vm.accentIndex == index) Icon(Icons.Rounded.CheckCircle, "Selected", tint = Color.White)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemes = false }) { Text("Close", color = MaterialTheme.colorScheme.onSurface) } }
        )
    }
}

@Composable
private fun LibraryTab(label: String, selected: Boolean, modifier: Modifier = Modifier, compact: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.height(54.dp)
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.075f), shape)
            .border(1.dp, Color.White.copy(alpha = if (selected) 0.62f else 0.28f), shape)
            .rubberPress(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = if (compact) 13.sp else 15.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun ArtBox(song: Song, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).glass(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f))
        AsyncImage(model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}
