package com.tittu.glasstune.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppDao {
    @Query("SELECT p.id AS id, p.name AS name, (SELECT COUNT(*) FROM playlist_songs s WHERE s.playlistId = p.id) AS songCount FROM playlists p ORDER BY p.createdAt DESC")
    abstract fun observePlaylists(): Flow<List<PlaylistInfo>>
    @Query("SELECT * FROM playlists")
    abstract suspend fun allPlaylists(): List<PlaylistEntity>
    @Insert
    abstract suspend fun insertPlaylist(p: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :id ORDER BY position ASC")
    abstract fun observePlaylistSongs(id: Long): Flow<List<PlaylistSongEntity>>
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :id ORDER BY position ASC")
    abstract suspend fun getPlaylistSongs(id: Long): List<PlaylistSongEntity>
    @Insert
    abstract suspend fun insertSongs(items: List<PlaylistSongEntity>)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    abstract suspend fun clearPlaylist(id: Long)
    @Query("DELETE FROM playlist_songs WHERE id = :rowId")
    abstract suspend fun deleteRow(rowId: Long)
    @Transaction
    open suspend fun replaceSongs(id: Long, items: List<PlaylistSongEntity>) {
        clearPlaylist(id)
        insertSongs(items)
    }
    @Query("SELECT * FROM eq_presets ORDER BY name")
    abstract fun observePresets(): Flow<List<EqPresetEntity>>
    @Query("SELECT * FROM eq_presets")
    abstract suspend fun allPresets(): List<EqPresetEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun savePreset(p: EqPresetEntity)
    @Query("DELETE FROM eq_presets WHERE name = :name")
    abstract suspend fun deletePreset(name: String)
    @Query("SELECT text FROM lyrics WHERE songPath = :path")
    abstract suspend fun getLyrics(path: String): String?
    @Query("SELECT * FROM lyrics")
    abstract suspend fun allLyrics(): List<LyricsEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveLyrics(l: LyricsEntity)
    @Query("DELETE FROM lyrics WHERE songPath = :path")
    abstract suspend fun deleteLyrics(path: String)
}