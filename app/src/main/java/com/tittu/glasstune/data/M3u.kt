package com.tittu.glasstune.data

data class M3uEntry(val path: String, val title: String?)

object M3u {
    fun parse(text: String): List<M3uEntry> {
        val out = mutableListOf<M3uEntry>()
        var title: String? = null
        for (raw in text.lineSequence()) {
            val l = raw.trim().removePrefix("\uFEFF")
            if (l.isEmpty()) continue
            if (l.startsWith("#EXTINF")) {
                title = l.substringAfter(",", "").trim().ifEmpty { null }
            } else if (!l.startsWith("#")) {
                out += M3uEntry(l, title)
                title = null
            }
        }
        return out
    }

    fun build(songs: List<Song>): String = buildString {
        append("#EXTM3U\n")
        for (s in songs) {
            append("#EXTINF:").append(s.durationMs / 1000).append(',')
                .append(s.artist).append(" - ").append(s.title).append('\n')
            append(s.path.ifEmpty { s.title }).append('\n')
        }
    }

    fun match(entries: List<M3uEntry>, songs: List<Song>): List<Song> {
        val byPath = songs.associateBy { it.path }
        val byName = songs.groupBy { it.path.substringAfterLast('/').lowercase() }
        val byTitle = songs.groupBy { it.title.lowercase() }
        return entries.mapNotNull { e ->
            val fileName = e.path.substringAfterLast('/').substringAfterLast('\\').lowercase()
            byPath[e.path] ?: byName[fileName]
                ?.firstOrNull() ?: e.title?.let { t -> byTitle[t.substringAfter(" - ").lowercase()]?.firstOrNull() }
        }
    }
}