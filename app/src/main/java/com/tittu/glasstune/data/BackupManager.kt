package com.tittu.glasstune.data

import android.content.Context
import com.tittu.glasstune.data.db.*
import org.json.JSONArray
import org.json.JSONObject

data class RestoreResult(val playlists: Int, val favoritePaths: List<String>, val theme: Int?, val accent: Int?)

object BackupManager {
    private const val FX_PREFS = "glasstune_fx"
    suspend fun export(ctx: Context, dao: AppDao, songs: List<Song>, favorites: Set<Long>, theme: Int, accent: Int): String {
        val byId = songs.associateBy { it.id }
        val root = JSONObject().put("app", "GlassTune").put("version", 1).put("theme", theme).put("accent", accent)
        root.put("favorites", JSONArray(favorites.mapNotNull { byId[it]?.path }))
        val pls = JSONArray()
        for (p in dao.allPlaylists()) {
            val arr = JSONArray()
            for (r in dao.getPlaylistSongs(p.id)) arr.put(JSONObject().put("path", r.path).put("title", byId[r.songId]?.title ?: ""))
            pls.put(JSONObject().put("name", p.name).put("songs", arr))
        }
        root.put("playlists", pls)
        val presets = JSONArray()
        for (e in dao.allPresets()) presets.put(JSONObject().put("name", e.name).put("levels", e.levels).put("bass", e.bass).put("virt", e.virtualizer))
        root.put("presets", presets)
        val lyrics = JSONArray()
        for (l in dao.allLyrics()) lyrics.put(JSONObject().put("path", l.songPath).put("text", l.text))
        root.put("lyrics", lyrics)
        val fx = ctx.getSharedPreferences(FX_PREFS, Context.MODE_PRIVATE)
        root.put("fx", JSONObject().put("enabled", fx.getBoolean("enabled", false)).put("levels", fx.getString("levels", "") ?: "")
            .put("bass", fx.getInt("bass", 0)).put("virt", fx.getInt("virt", 0)).put("loud", fx.getInt("loud", 0)))
        return root.toString(2)
    }
    suspend fun restore(ctx: Context, dao: AppDao, songs: List<Song>, text: String): RestoreResult {
        val root = JSONObject(text)
        require(root.optString("app") == "GlassTune") { "Not a GlassTune backup" }
        val byPath = songs.associateBy { it.path }
        var count = 0
        val existing = dao.allPlaylists().map { it.name }.toMutableSet()
        val pls = root.optJSONArray("playlists") ?: JSONArray()
        for (i in 0 until pls.length()) {
            val p = pls.getJSONObject(i); val name = p.getString("name")
            if (name in existing) continue
            val id = dao.insertPlaylist(PlaylistEntity(name = name))
            val arr = p.optJSONArray("songs") ?: JSONArray()
            val rows = mutableListOf<PlaylistSongEntity>()
            for (j in 0 until arr.length()) {
                val path = arr.getJSONObject(j).getString("path")
                rows += PlaylistSongEntity(playlistId = id, songId = byPath[path]?.id ?: -1L, path = path, position = j)
            }
            dao.insertSongs(rows); existing += name; count++
        }
        val presets = root.optJSONArray("presets") ?: JSONArray()
        for (i in 0 until presets.length()) {
            val e = presets.getJSONObject(i)
            dao.savePreset(EqPresetEntity(e.getString("name"), e.getString("levels"), e.optInt("bass"), e.optInt("virt")))
        }
        val lyrics = root.optJSONArray("lyrics") ?: JSONArray()
        for (i in 0 until lyrics.length()) { val l = lyrics.getJSONObject(i); dao.saveLyrics(LyricsEntity(l.getString("path"), l.getString("text"))) }
        root.optJSONObject("fx")?.let { fx ->
            ctx.getSharedPreferences(FX_PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", fx.optBoolean("enabled"))
                .putString("levels", fx.optString("levels")).putInt("bass", fx.optInt("bass")).putInt("virt", fx.optInt("virt"))
                .putInt("loud", fx.optInt("loud")).apply()
        }
        val favs = root.optJSONArray("favorites") ?: JSONArray()
        val favPaths = List(favs.length()) { favs.getString(it) }
        return RestoreResult(count, favPaths, if (root.has("theme")) root.getInt("theme") else null, if (root.has("accent")) root.getInt("accent") else null)
    }
}