package com.lyra.player.data.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortPreferencesTest {

    @Test
    fun `sort letter uses pinyin initial for Chinese names`() {
        assertEquals('Q', librarySortLetter("晴天"))
        assertEquals('A', librarySortLetter("爱情"))
        assertEquals('X', librarySortLetter("许嵩"))
    }

    @Test
    fun `sort letter uppercases latin names`() {
        assertEquals('L', librarySortLetter("love story"))
        assertEquals('T', librarySortLetter("Taylor Swift"))
    }

    @Test
    fun `sort letter puts non latin names into other group`() {
        assertEquals(LIBRARY_SORT_OTHER_GROUP, librarySortLetter("123"))
        assertEquals(LIBRARY_SORT_OTHER_GROUP, librarySortLetter("【纯音乐】"))
        assertEquals(LIBRARY_SORT_OTHER_GROUP, librarySortLetter(""))
        assertEquals(LIBRARY_SORT_OTHER_GROUP, librarySortLetter("   "))
    }

    @Test
    fun `sort key converts Chinese to full pinyin`() {
        assertEquals("qingtian", librarySortKey("晴天"))
        assertEquals("love story", librarySortKey("Love Story"))
    }

    @Test
    fun `groups are ordered by letter with other group last`() {
        val names = listOf("周杰伦", "Adele", "123", "陈奕迅", "love story")
        val groups = groupByLibrarySortLetter(names) { it }
        assertEquals(listOf('A', 'C', 'L', 'Z', '#'), groups.map { it.letter })
        assertEquals(listOf("Adele"), groups[0].items)
        assertEquals(listOf("123"), groups.last().items)
    }

    @Test
    fun `items inside a group are sorted by pinyin`() {
        val names = listOf("爱情", "阿杜", "安静")
        val groups = groupByLibrarySortLetter(names) { it }
        assertEquals(1, groups.size)
        assertEquals(listOf("阿杜", "爱情", "安静"), groups.single().items)
    }

    @Test
    fun `stored sort mode resolves with added time as default`() {
        assertEquals(
            LibrarySortMode.ADDED_TIME,
            LibrarySortPreferences.resolveSortMode(null)
        )
        assertEquals(
            LibrarySortMode.ADDED_TIME,
            LibrarySortPreferences.resolveSortMode("unknown")
        )
        assertEquals(
            LibrarySortMode.ALPHABETICAL,
            LibrarySortPreferences.resolveSortMode("ALPHABETICAL")
        )
    }
}
