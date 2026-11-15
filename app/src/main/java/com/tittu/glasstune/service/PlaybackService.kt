package com.tittu.glasstune.service

import android.app.PendingIntent
import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.tittu.glasstune.MainActivity
import com.tittu.glasstune.audio.AudioEffectsHub
import com.tittu.glasstune.widget.GlassWidgetProvider

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Route player PCM through the software FX chain (graphic EQ, balance, normalization).
        val fxProcessor = AudioEffectsHub.fxProcessor
        val renderersFactory = object : DefaultRenderersFactory(this) {
            @OptIn(UnstableApi::class)
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(fxProcessor))
                    .build()
            }
        }
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioEffectsHub.attach(this@PlaybackService, audioSessionId)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
                    events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                ) {
                    GlassWidgetProvider.update(this@PlaybackService, player)
                }
            }
        })
        AudioEffectsHub.attach(this, player.audioSessionId)

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(VoiceSearchCallback(this))
            .build()
    }

    /** Offline voice/search support: resolves assistant queries ("play <title>") against MediaStore. */
    @OptIn(UnstableApi::class)
    private class VoiceSearchCallback(private val context: Context) : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            return Futures.immediateFuture(mediaItems.mapNotNull { resolve(it) })
        }

        private fun resolve(item: MediaItem): MediaItem? {
            // Already-resolvable items (e.g. from the app itself) pass straight through.
            if (item.localConfiguration != null) return item
            val query = item.requestMetadata.searchQuery
                ?: item.mediaMetadata.title?.toString()
                ?: return null
            if (query.isBlank()) return null
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
            )
            val artBase = Uri.parse("content://media/external/audio/albumart")
            try {
                context.contentResolver.query(
                    collection, projection,
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.TITLE} LIKE ?",
                    arrayOf("%$query%"), null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.getLong(0)
                        val metadata = MediaMetadata.Builder()
                            .setTitle(c.getString(1))
                            .setArtist(c.getString(2)?.takeIf { it != "<unknown>" } ?: "Unknown artist")
                            .setAlbumTitle(c.getString(3))
                            .setArtworkUri(ContentUris.withAppendedId(artBase, c.getLong(4)))
                            .build()
                        return MediaItem.Builder()
                            .setMediaId(id.toString())
                            .setUri(ContentUris.withAppendedId(collection, id))
                            .setMediaMetadata(metadata)
                            .build()
                    }
                }
            } catch (_: Exception) { }
            return null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null && action.startsWith("com.tittu.glasstune.widget.")) {
            val player = session?.player
            if (player == null || player.mediaItemCount == 0) {
                stopSelf()
            } else {
                when (action) {
                    GlassWidgetProvider.ACTION_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
                    GlassWidgetProvider.ACTION_NEXT -> player.seekToNextMediaItem()
                    GlassWidgetProvider.ACTION_PREV -> {
                        if (player.currentPosition > 3000L) player.seekTo(0L)
                        else player.seekToPreviousMediaItem()
                    }
                    GlassWidgetProvider.ACTION_FAVORITE -> {
                        val mediaId = player.currentMediaItem?.mediaId
                        val id = mediaId?.toLongOrNull()
                        if (id != null) {
                            val prefs = getSharedPreferences("glasstune", MODE_PRIVATE)
                            val current = (prefs.getStringSet("fav", emptySet()) ?: emptySet()).toMutableSet()
                            if (!current.add(id.toString())) current.remove(id.toString())
                            prefs.edit().putStringSet("fav", current).apply()
                            GlassWidgetProvider.refresh(this)
                        }
                    }
                }
            }
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        AudioEffectsHub.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
