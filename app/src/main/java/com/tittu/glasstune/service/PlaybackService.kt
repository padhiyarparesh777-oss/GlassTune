package com.tittu.glasstune.service

import android.app.PendingIntent
import android.media.AudioManager
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tittu.glasstune.MainActivity

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    companion object {
        @Volatile var audioSessionId: Int = 0
            private set
    }
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val generatedSessionId = (getSystemService(AUDIO_SERVICE) as AudioManager).generateAudioSessionId()
        if (generatedSessionId > 0) audioSessionId = generatedSessionId
        val player = ExoPlayer.Builder(this)
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
        if (audioSessionId > 0) player.setAudioSessionId(audioSessionId)

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player).setSessionActivity(openApp).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        audioSessionId = 0
        super.onDestroy()
    }
}
