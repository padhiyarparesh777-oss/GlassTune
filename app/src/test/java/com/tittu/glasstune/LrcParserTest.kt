package com.tittu.glasstune

import com.tittu.glasstune.data.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parses basic timestamps`() {
        val lines = LrcParser.parse("[00:12.50]Hello world\n[01:02.00]Second line")
        assertEquals(2, lines.size)
        assertEquals(12500L, lines[0].timeMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals(62000L, lines[1].timeMs)
        assertEquals("Second line", lines[1].text)
    }

    @Test
    fun `sorts out-of-order timestamps`() {
        val lines = LrcParser.parse("[01:00.00]later\n[00:30.00]earlier")
        assertEquals("earlier", lines[0].text)
        assertEquals("later", lines[1].text)
    }

    @Test
    fun `handles multiple timestamps on one line`() {
        val lines = LrcParser.parse("[00:10.00][00:20.00]chorus")
        assertEquals(2, lines.size)
        assertEquals("chorus", lines[0].text)
        assertEquals("chorus", lines[1].text)
        assertEquals(10000L, lines[0].timeMs)
        assertEquals(20000L, lines[1].timeMs)
    }

    @Test
    fun `ignores non-timestamp lines`() {
        val lines = LrcParser.parse("[ti:Song title]\n[ar:Artist]\njust plain text")
        assertEquals(0, lines.size)
    }

    @Test
    fun `parses fractional variants`() {
        val lines = LrcParser.parse("[00:01.5]one\n[00:02.25]two\n[00:03.123]three")
        assertEquals(1500L, lines[0].timeMs)
        assertEquals(2250L, lines[1].timeMs)
        assertEquals(3123L, lines[2].timeMs)
    }

    @Test
    fun `empty text becomes empty string`() {
        val lines = LrcParser.parse("[00:05.00]")
        assertEquals(1, lines.size)
        assertEquals("", lines[0].text)
    }
}
