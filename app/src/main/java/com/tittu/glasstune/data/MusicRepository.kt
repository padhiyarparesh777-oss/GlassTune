package com.tittu.glasstune.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

class MusicRepository(private val context: Context) {
    @Suppress("DEPRECATION")
    fun loadSongs(): List<Song> {
        val out = mutableListOf<Song>()
        val collection: Uri = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val artBase = Uri.parse("content://media/external/audio/albumart")
        try {
            context.contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val albumId = c.getLong(iAlbumId)
                    val artist = c.getString(iArtist)?.takeIf { it != "<unknown>" } ?: "Unknown artist"
                    val path = c.getString(iData) ?: ""
                    out += Song(
                        id = id,
                        title = c.getString(iTitle) ?: "Unknown title",
                        artist = artist,
                        album = c.getString(iAlbum) ?: "Unknown album",
                        albumId = albumId,
                        durationMs = c.getLong(iDur),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        artUri = ContentUris.withAppendedId(artBase, albumId),
                        dateAdded = c.getLong(iAdded),
                        folder = File(path).parentFile?.name ?: "Unknown folder"
                    )
                }
            }
        } catch (_: Exception) {
        }
        return out
    }
}
