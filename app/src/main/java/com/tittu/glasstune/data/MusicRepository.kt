package com.tittu.glasstune.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/** Reads the device music library from MediaStore. */
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
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val artBase = Uri.parse("content://media/external/audio/albumart")

        try {
            context.contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val iId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDuration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iData = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(iId)
                    val albumId = cursor.getLong(iAlbumId)
                    val artist = cursor.getString(iArtist)
                        ?.takeIf { it != "<unknown>" } ?: "Unknown artist"
                    val path = cursor.getString(iData) ?: ""
                    out += Song(
                        id = id,
                        title = cursor.getString(iTitle) ?: "Unknown title",
                        artist = artist,
                        album = cursor.getString(iAlbum) ?: "Unknown album",
                        albumId = albumId,
                        durationMs = cursor.getLong(iDuration),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        artUri = ContentUris.withAppendedId(artBase, albumId),
                        dateAdded = cursor.getLong(iAdded),
                        folder = File(path).parentFile?.name ?: "Unknown folder",
                        path = path
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission was revoked; the library remains empty until access is granted again.
        } catch (_: Exception) {
            // MediaStore provider errors must not crash the UI.
        }
        return out
    }
}
