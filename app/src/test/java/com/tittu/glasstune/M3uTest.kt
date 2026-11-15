package com.tittu.glasstune

import com.tittu.glasstune.data.M3u
import com.tittu.glasstune.data.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uTest {

    private fun song(id: Long, title: String, artist: String, path: String) = Song(
        id = id, title = title, artist = artist, album = "Album", albumId = 1,
        durationMs = 180_000, uri = "content://media/audio/$id",
        artUri = "content://media/albumart/1", dateAdded = 0,
        folder = "Music", path = path
    )

    @Test
    fun `parses extm3u with titles`() {
        val entries = M3u.parse("#EXTM3U\n#EXTINF:180,Artist - Song A\n/sd/a.mp3\n\n#EXTINF:200,Artist - Song B\n/sd/b.mp3")
        assertEquals(2, entries.size)
        assertEquals("/sd/a.mp3", entries[0].path)
        assertEquals("Artist - Song A", entries[0].title)
        assertEquals("/sd/b.mp3", entries[1].path)
    }

    @Test
    fun `build round trips through parse`() {
        val songs = listOf(
            song(1, "Alpha", "X", "/sd/alpha.mp3"),
            song(2, "Beta", "Y", "/sd/beta.mp3")
        )
        val text = M3u.build(songs)
        val entries = M3u.parse(text)
        assertEquals(2, entries.size)
        assertEquals("/sd/alpha.mp3", entries[0].path)
    }

    @Test
    fun `match by exact path`() {
        val lib = listOf(song(1, "Alpha", "X", "/sd/alpha.mp3"))
        val entries = M3u.parse("/sd/alpha.mp3")
        assertEquals(listOf("Alpha"), M3u.match(entries, lib).map { it.title })
    }

    @Test
    fun `match by filename fallback`() {
        val lib = listOf(song(1, "Alpha", "X", "/other/dir/alpha.mp3"))
        val entries = M3u.parse("/sd/alpha.mp3")
        assertEquals(listOf("Alpha"), M3u.match(entries, lib).map { it.title })
    }

    @Test
    fun `match by extinf title fallback`() {
        val lib = listOf(song(1, "Alpha", "X", "/sd/renamed.mp3"))
        val entries = M3u.parse("#EXTINF:180,X - Alpha\n/missing.mp3")
        assertEquals(listOf("Alpha"), M3u.match(entries, lib).map { it.title })
    }

    @Test
    fun `unknown entries are dropped`() {
        val lib = listOf(song(1, "Alpha", "X", "/sd/alpha.mp3"))
        val entries = M3u.parse("/sd/nope.mp3")
        assertEquals(0, M3u.match(entries, lib).size)
    }

    @Test
    fun `bom and windows separators tolerated`() {
        val text = "\uFEFF#EXTM3U\r\nC:\\Music\\song.mp3\r\n"
        val entries = M3u.parse(text)
        assertEquals(1, entries.size)
        assertEquals("C:\\Music\\song.mp3", entries[0].path)
    }
}
