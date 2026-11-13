package com.tittu.glasstune.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.media3.common.Player
import com.tittu.glasstune.MainActivity
import com.tittu.glasstune.R
import com.tittu.glasstune.service.PlaybackService

/** Home-screen widget with track information and transport controls. */
class GlassWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = build(context)
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.tittu.glasstune.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.tittu.glasstune.widget.NEXT"
        const val ACTION_PREV = "com.tittu.glasstune.widget.PREV"
        const val ACTION_FAVORITE = "com.tittu.glasstune.widget.FAVORITE"
        private const val PREFS = "glasstune_widget"

        fun update(context: Context, player: Player) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", player.mediaMetadata.title?.toString() ?: "Nothing playing")
                .putString("artist", player.mediaMetadata.artist?.toString() ?: "Open GlassTune")
                .putString("art_uri", player.mediaMetadata.artworkUri?.toString())
                .putString("media_id", player.currentMediaItem?.mediaId)
                .putBoolean("playing", player.isPlaying)
                .apply()
            refresh(context)
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GlassWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun build(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val views = RemoteViews(context.packageName, R.layout.widget_glass)
            views.setTextViewText(R.id.widget_title, prefs.getString("title", "Nothing playing"))
            views.setTextViewText(R.id.widget_artist, prefs.getString("artist", "Open GlassTune"))
            val artUri = prefs.getString("art_uri", null)
            if (!artUri.isNullOrBlank()) views.setImageViewUri(R.id.widget_art, Uri.parse(artUri))
            else views.setImageViewResource(R.id.widget_art, R.drawable.ic_widget_art)
            val mediaId = prefs.getString("media_id", null)
            val favoriteIds = context.getSharedPreferences("glasstune", Context.MODE_PRIVATE)
                .getStringSet("fav", emptySet()) ?: emptySet()
            val favorite = mediaId != null && mediaId in favoriteIds
            views.setImageViewResource(R.id.widget_favorite, if (favorite) R.drawable.ic_widget_heart_on else R.drawable.ic_widget_heart)
            views.setImageViewResource(
                R.id.widget_play,
                if (prefs.getBoolean("playing", false)) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            views.setOnClickPendingIntent(R.id.widget_prev, service(context, ACTION_PREV, 1))
            views.setOnClickPendingIntent(R.id.widget_play, service(context, ACTION_PLAY_PAUSE, 2))
            views.setOnClickPendingIntent(R.id.widget_next, service(context, ACTION_NEXT, 3))
            views.setOnClickPendingIntent(R.id.widget_favorite, service(context, ACTION_FAVORITE, 5))
            val open = PendingIntent.getActivity(
                context, 4, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }

        private fun service(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getService(
                context, requestCode,
                Intent(context, PlaybackService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}
