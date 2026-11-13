package com.tittu.glasstune.data

data class LrcLine(val timeMs: Long, val text: String)

/** Parses standard LRC ([mm:ss.xx] text). Returns an empty list for plain (unsynced) text. */
object LrcParser {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    fun parse(raw: String): List<LrcLine> {
        val out = mutableListOf<LrcLine>()
        for (line in raw.lines()) {
            val matches = tag.findAll(line).toList()
            if (matches.isEmpty()) continue
            val text = line.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                out += LrcLine(min * 60_000 + sec * 1000 + ms, text)
            }
        }
        return out.sortedBy { it.timeMs }
    }
}