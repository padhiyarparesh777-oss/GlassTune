package com.tittu.glasstune.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tittu.glasstune.audio.AudioEffectsHub
import com.tittu.glasstune.data.BackupManager
import com.tittu.glasstune.data.M3u
import com.tittu.glasstune.data.MusicRepository
import com.tittu.glasstune.data.Song
import com.tittu.glasstune.data.db.AppDatabase
import com.tittu.glasstune.data.db.EqPresetEntity
import com.tittu.glasstune.data.db.LyricsEntity
import com.tittu.glasstune.data.db.PlaylistEntity
import com.tittu.glasstune.data.db.PlaylistInfo
import com.tittu.glasstune.data.db.PlaylistSongEntity
import com.tittu.glasstune.service.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SortOrder(val label: String) {
    TITLE("Title"), ARTIST("Artist"), ALBUM("Album"), DURATION("Duration"), ADDED("Recently added")
}

data class QueueEntry(val id: String, val title: String, val artist: String)
data class PlaylistItem(val rowId: Long, val song: Song)

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private val prefs = app.getSharedPreferences("glasstune", Context.MODE_PRIVATE)
    private val dao = AppDatabase.get(app).dao()
    private var controller: MediaController? = null
    private var byId: Map<Long, Song> = emptyMap()
    private val songsFlow = MutableStateFlow<List<Song>>(emptyList())
    private var sleepJob: Job? = null
    private var lyricsFor: Long = -1L

    // Library
    var songs by mutableStateOf<List<Song>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var query by mutableStateOf("")
    var sort by mutableStateOf(SortOrder.TITLE)
    var tab by mutableStateOf(0)

    // Appearance
    var themeMode by mutableStateOf(ThemeMode.entries[prefs.getInt("theme", 0).coerceIn(0, 2)]); private set
    var accentIndex by mutableStateOf(prefs.getInt("accent", 0).coerceIn(0, AccentColors.size - 1)); private set

    // Favorites
    var favorites by mutableStateOf(
        (prefs.getStringSet("fav", emptySet()) ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
    ); private set

    // Playback state
    var currentSong by mutableStateOf<Song?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var positionMs by mutableStateOf(0L); private set
    var durationMs by mutableStateOf(0L); private set
    var shuffle by mutableStateOf(false); private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF); private set
    var speed by mutableStateOf(1f); private set
    var queue by mutableStateOf<List<QueueEntry>>(emptyList()); private set
    var currentIndex by mutableStateOf(0); private set
    var sleepMinutes by mutableStateOf(0); private set

    // Lyrics of the current song (null = none found)
    var lyricsText by mutableStateOf<String?>(null); private set

    // Database-backed state
    val playlists: StateFlow<List<PlaylistInfo>> =
        dao.observePlaylists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customPresets: StateFlow<List<EqPresetEntity>> =
        dao.observePresets().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState(events.contains(Player.EVENT_TIMELINE_CHANGED))
        }
    }

    init {
        connect()
        viewModelScope.launch {
            while (true) {
                if (isPlaying) controller?.let { positionMs = it.currentPosition.coerceAtLeast(0L) }
                delay(500)
            }
        }
    }

    private fun toast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(getApplication<Application>(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun connect() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            try {
                controller = future.get().also { it.addListener(listener) }
                syncState(true)
            } catch (e: Exception) {
                controller = null
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun syncState(rebuildQueue: Boolean = true) {
        val c = controller ?: return
        isPlaying = c.isPlaying
        shuffle = c.shuffleModeEnabled
        repeatMode = c.repeatMode
        speed = c.playbackParameters.speed
        val d = c.duration
        durationMs = if (d == C.TIME_UNSET || d < 0) 0L else d
        positionMs = c.currentPosition.coerceAtLeast(0L)
        currentIndex = c.currentMediaItemIndex
        val id = c.currentMediaItem?.mediaId?.toLongOrNull()
        currentSong = if (id == null) null else byId[id]
        if ((currentSong?.id ?: -1L) != lyricsFor) {
            lyricsFor = currentSong?.id ?: -1L
            loadLyrics(currentSong)
        }
        if (rebuildQueue) {
            queue = List(c.mediaItemCount) { i ->
                val m = c.getMediaItemAt(i)
                QueueEntry(
                    m.mediaId,
                    m.mediaMetadata.title?.toString() ?: "",
                    m.mediaMetadata.artist?.toString() ?: ""
                )
            }
        }
    }

    // ---------------- Library ----------------
    fun loadLibrary() {
        viewModelScope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) { repo.loadSongs() }
            songs = result
            songsFlow.value = result
            byId = result.associateBy { it.id }
            loading = false
            lyricsFor = -2L
            syncState(true)
        }
    }

    fun filteredSongs(): List<Song> {
        val q = query.trim()
        val list = if (q.isEmpty()) songs else songs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
        return when (sort) {
            SortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM -> list.sortedBy { it.album.lowercase() }
            SortOrder.DURATION -> list.sortedByDescending { it.durationMs }
            SortOrder.ADDED -> list.sortedByDescending { it.dateAdded }
        }
    }

    fun cycleSort() {
        sort = SortOrder.entries[(sort.ordinal + 1) % SortOrder.entries.size]
    }

    // ---------------- Appearance ----------------
    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putInt("theme", mode.ordinal).apply()
    }

    fun setAccent(index: Int) {
        accentIndex = index.coerceIn(0, AccentColors.size - 1)
        prefs.edit().putInt("accent", accentIndex).apply()
    }

    // ---------------- Favorites ----------------
    fun toggleFavorite(id: Long) {
        persistFavorites(if (id in favorites) favorites - id else favorites + id)
    }

    private fun persistFavorites(n: Set<Long>) {
        favorites = n
        prefs.edit().putStringSet("fav", n.map { it.toString() }.toSet()).apply()
    }

    // ---------------- Playback ----------------
    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.setMediaItems(list.map { it.toMediaItem() }, index.coerceIn(0, list.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun skipTo(index: Int) {
        controller?.let { it.seekTo(index, 0L); it.play() }
    }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    fun cycleSpeed() {
        val c = controller ?: return
        val i = speeds.indexOfFirst { it == c.playbackParameters.speed }
        c.setPlaybackSpeed(speeds[(i + 1) % speeds.size])
    }

    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(song), 0) else c.addMediaItem(song.toMediaItem())
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(song), 0)
        else c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
    }

    fun removeFromQueue(index: Int) { controller?.removeMediaItem(index) }

    // ---------------- Sleep timer (10 s fade-out) ----------------
    fun cycleSleepTimer() {
        setSleepTimer(when (sleepMinutes) { 0 -> 15; 15 -> 30; 30 -> 60; else -> 0 })
    }

    fun setSleepTimer(next: Int) {
        sleepJob?.cancel()
        controller?.volume = 1f
        sleepMinutes = next
        if (next == 0) return
        sleepJob = viewModelScope.launch {
            delay((next * 60_000L - 10_000L).coerceAtLeast(0L))
            for (i in 10 downTo 1) {
                controller?.volume = i / 10f
                delay(1000)
            }
            controller?.pause()
            controller?.volume = 1f
            sleepMinutes = 0
        }
    }

    // ---------------- Playlists ----------------
    fun playlistItems(id: Long): Flow<List<PlaylistItem>> =
        dao.observePlaylistSongs(id).combine(songsFlow) { rows, all ->
            val ids = all.associateBy { it.id }
            val paths = all.associateBy { it.path }
            rows.mapNotNull { r -> (ids[r.songId] ?: paths[r.path])?.let { PlaylistItem(r.id, it) } }
        }

    fun createPlaylist(name: String, song: Song? = null) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = dao.insertPlaylist(PlaylistEntity(name = n))
            if (song != null) {
                dao.insertSongs(listOf(PlaylistSongEntity(playlistId = id, songId = song.id, path = song.path, position = 0)))
                toast("Added to \"$n\"")
            }
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { dao.renamePlaylist(id, n) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { dao.deletePlaylist(id) }
    }

    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val rows = dao.getPlaylistSongs(playlistId)
            if (rows.any { it.songId == song.id || it.path == song.path }) {
                toast("Already in this playlist")
                return@launch
            }
            dao.insertSongs(
                listOf(PlaylistSongEntity(playlistId = playlistId, songId = song.id, path = song.path, position = rows.size))
            )
            toast("Added to playlist")
        }
    }

    fun removeFromPlaylist(rowId: Long) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteRow(rowId) }
    }

    fun movePlaylistItem(playlistId: Long, rowId: Long, delta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val rows = dao.getPlaylistSongs(playlistId).toMutableList()
            val i = rows.indexOfFirst { it.id == rowId }
            val j = i + delta
            if (i < 0 || j !in rows.indices) return@launch
            val moved = rows.removeAt(i)
            rows.add(j, moved)
            dao.replaceSongs(playlistId, rows.mapIndexed { idx, r -> r.copy(id = 0, position = idx) })
        }
    }

    fun exportPlaylistM3u(playlistId: Long, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val all = songsFlow.value
                val ids = all.associateBy { it.id }
                val paths = all.associateBy { it.path }
                val list = dao.getPlaylistSongs(playlistId).mapNotNull { ids[it.songId] ?: paths[it.path] }
                writeText(uri, M3u.build(list))
                toast("Exported ${list.size} songs")
            } catch (e: Exception) {
                toast("Export failed")
            }
        }
    }

    fun importM3u(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = readText(uri) ?: throw IllegalStateException("empty")
                val entries = M3u.parse(text)
                val matched = M3u.match(entries, songsFlow.value)
                if (matched.isEmpty()) {
                    toast("No songs from this playlist were found in your library")
                    return@launch
                }
                val name = displayName(uri).substringBeforeLast('.').ifBlank { "Imported playlist" }
                val id = dao.insertPlaylist(PlaylistEntity(name = name))
                dao.insertSongs(matched.mapIndexed { i, s ->
                    PlaylistSongEntity(playlistId = id, songId = s.id, path = s.path, position = i)
                })
                toast("Imported ${matched.size} of ${entries.size} songs")
            } catch (e: Exception) {
                toast("Could not import playlist")
            }
        }
    }

    // ---------------- Lyrics ----------------
    private fun loadLyrics(song: Song?) {
        if (song == null) {
            lyricsText = null
            return
        }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { dao.getLyrics(song.lyricsKey) ?: readSidecarLrc(song.path) }
            if (currentSong?.id == song.id) lyricsText = text
        }
    }

    /** Best effort: works where the app may read files next to the song (older Android versions). */
    private fun readSidecarLrc(path: String): String? = try {
        if (path.isEmpty()) null else {
            val f = File(path.substringBeforeLast('.') + ".lrc")
            if (f.exists() && f.canRead()) f.readText() else null
        }
    } catch (e: Exception) {
        null
    }

    fun saveLyrics(text: String) {
        val s = currentSong ?: return
        val t = text.trim()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (t.isEmpty()) dao.deleteLyrics(s.lyricsKey) else dao.saveLyrics(LyricsEntity(s.lyricsKey, t))
            }
            if (currentSong?.id == s.id) lyricsText = t.ifEmpty { null }
        }
    }

    fun importLrc(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { readText(uri) }
            if (text.isNullOrBlank()) toast("Empty or unreadable file") else saveLyrics(text)
        }
    }

    // ---------------- Equalizer presets ----------------
    fun saveCustomPreset(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        val s = AudioEffectsHub.ui
        viewModelScope.launch(Dispatchers.IO) {
            dao.savePreset(EqPresetEntity(n, s.levels.joinToString(","), s.bass, s.virt))
        }
    }

    fun applyCustomPreset(p: EqPresetEntity) {
        AudioEffectsHub.applyCustom(p.name, p.levels.split(",").mapNotNull { it.toIntOrNull() }, p.bass, p.virtualizer)
    }

    fun deleteCustomPreset(name: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.deletePreset(name) }
    }

    // ---------------- Backup / restore ----------------
    fun backupTo(uri: Uri) {
        val songList = songsFlow.value
        val favs = favorites
        val theme = themeMode.ordinal
        val accent = accentIndex
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = BackupManager.export(getApplication(), dao, songList, favs, theme, accent)
                writeText(uri, json)
                toast("Backup saved")
            } catch (e: Exception) {
                toast("Backup failed")
            }
        }
    }

    fun restoreFrom(uri: Uri) {
        val songList = songsFlow.value
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val text = readText(uri) ?: return@withContext null
                    BackupManager.restore(getApplication(), dao, songList, text)
                } catch (e: Exception) {
                    null
                }
            }
            if (result == null) {
                toast("Restore failed: not a valid GlassTune backup")
                return@launch
            }
            result.theme?.let { setTheme(ThemeMode.entries[it.coerceIn(0, 2)]) }
            result.accent?.let { setAccent(it) }
            val byPath = songList.associateBy { it.path }
            persistFavorites(favorites + result.favoritePaths.mapNotNull { byPath[it]?.id })
            AudioEffectsHub.reapply()
            toast("Restored ${result.playlists} playlists, favorites and settings")
        }
    }

    // ---------------- IO helpers ----------------
    private fun readText(uri: Uri): String? =
        getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

    private fun writeText(uri: Uri, text: String) {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
    }

    private fun displayName(uri: Uri): String =
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Imported playlist"

    override fun onCleared() {
        sleepJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}