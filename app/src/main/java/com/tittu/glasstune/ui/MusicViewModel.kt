package com.tittu.glasstune.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
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
import com.tittu.glasstune.data.MusicRepository
import com.tittu.glasstune.data.Song
import com.tittu.glasstune.service.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder(val label: String) {
    TITLE("Title"), ARTIST("Artist"), ALBUM("Album"), DURATION("Duration"), ADDED("Recently added")
}

data class QueueEntry(val id: String, val title: String, val artist: String)

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private val prefs = app.getSharedPreferences("glasstune", Context.MODE_PRIVATE)
    private var controller: MediaController? = null
    private var byId: Map<Long, Song> = emptyMap()
    private var sleepJob: Job? = null

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
    var volumeLevel by mutableStateOf(0.85f); private set

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

    // ---- Library ----
    fun loadLibrary() {
        viewModelScope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) { repo.loadSongs() }
            songs = result
            byId = result.associateBy { it.id }
            loading = false
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

    // ---- Appearance ----
    fun cycleTheme() {
        themeMode = ThemeMode.entries[(themeMode.ordinal + 1) % ThemeMode.entries.size]
        prefs.edit().putInt("theme", themeMode.ordinal).apply()
    }

    fun cycleAccent() {
        accentIndex = (accentIndex + 1) % AccentColors.size
        prefs.edit().putInt("accent", accentIndex).apply()
    }

    // ---- Favorites ----
    fun toggleFavorite(id: Long) {
        val n = if (id in favorites) favorites - id else favorites + id
        favorites = n
        prefs.edit().putStringSet("fav", n.map { it.toString() }.toSet()).apply()
    }

    // ---- Playback ----
    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.setMediaItems(list.map { it.toMediaItem() }, index.coerceIn(0, list.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun setVolume(value: Float) {
        volumeLevel = value.coerceIn(0f, 1f)
        controller?.volume = volumeLevel
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

    // ---- Sleep timer (with 10s fade-out) ----
    fun cycleSleepTimer() {
        val next = when (sleepMinutes) { 0 -> 15; 15 -> 30; 30 -> 60; else -> 0 }
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

    override fun onCleared() {
        sleepJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
