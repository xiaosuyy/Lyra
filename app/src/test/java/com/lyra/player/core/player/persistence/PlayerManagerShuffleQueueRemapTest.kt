package com.lyra.player.core.player.persistence

import org.junit.Assert.assertEquals
import org.junit.Test
import com.lyra.player.core.player.model.insertIntoShuffleRestorePlaylist
import com.lyra.player.core.player.model.removeFromShuffleRestorePlaylist
import com.lyra.player.core.player.model.replaceInShuffleRestorePlaylist
import com.lyra.player.core.player.model.resolvePlayerQueueDisplayIndices
import com.lyra.player.core.player.model.resolvePlayerQueueRestoreOrder
import com.lyra.player.core.player.model.resolvePlayerRepeatAllShuffleOrder
import com.lyra.player.core.player.model.resolvePlayerSequentialShuffleOrder
import com.lyra.player.data.model.SongItem

class PlayerManagerQueueOrderTest {

    @Test
    fun `queue display follows the current playlist order`() {
        val displayIndices = resolvePlayerQueueDisplayIndices(
            queueSize = 4
        )

        assertEquals(listOf(0, 1, 2, 3), displayIndices)
    }

    @Test
    fun `queue display is empty for invalid queue size`() {
        val displayIndices = resolvePlayerQueueDisplayIndices(
            queueSize = 0
        )

        assertEquals(emptyList<Int>(), displayIndices)
    }

    @Test
    fun `sequential shuffle keeps current song first then applies shuffled order`() {
        val order = resolvePlayerSequentialShuffleOrder(
            queueSize = 5,
            currentIndex = 2,
            shuffleRemaining = { remaining -> remaining.reverse() }
        )

        assertEquals(listOf(2, 4, 3, 1, 0), order.queueIndices)
        assertEquals(0, order.currentIndex)
    }

    @Test
    fun `sequential shuffle falls back to first song when current index is invalid`() {
        val order = resolvePlayerSequentialShuffleOrder(
            queueSize = 5,
            currentIndex = 8,
            shuffleRemaining = { remaining -> remaining.reverse() }
        )

        assertEquals(listOf(0, 4, 3, 2, 1), order.queueIndices)
        assertEquals(0, order.currentIndex)
    }

    @Test
    fun `sequential shuffle returns missing current index for empty queue`() {
        val order = resolvePlayerSequentialShuffleOrder(
            queueSize = 0,
            currentIndex = 0
        )

        assertEquals(emptyList<Int>(), order.queueIndices)
        assertEquals(-1, order.currentIndex)
    }

    @Test
    fun `repeat all shuffle starts a fresh order without replaying the completed song`() {
        val order = resolvePlayerRepeatAllShuffleOrder(
            queueSize = 4,
            completedIndex = 3,
            shuffleQueue = { queue -> queue.reverse() }
        )

        assertEquals(listOf(2, 3, 1, 0), order.queueIndices)
        assertEquals(0, order.currentIndex)
    }

    @Test
    fun `repeat all shuffle changes an otherwise unchanged order when possible`() {
        val order = resolvePlayerRepeatAllShuffleOrder(
            queueSize = 3,
            completedIndex = 2,
            shuffleQueue = {}
        )

        assertEquals(listOf(0, 2, 1), order.queueIndices)
        assertEquals(0, order.currentIndex)
    }

    @Test
    fun `repeat all shuffle returns missing current index for an empty queue`() {
        val order = resolvePlayerRepeatAllShuffleOrder(
            queueSize = 0,
            completedIndex = 0
        )

        assertEquals(emptyList<Int>(), order.queueIndices)
        assertEquals(-1, order.currentIndex)
    }

    @Test
    fun `shuffle restore returns the original queue order and current song position`() {
        val first = testSong(id = 1L, name = "First")
        val second = testSong(id = 2L, name = "Second")
        val third = testSong(id = 3L, name = "Third")
        val restoreOrder = resolvePlayerQueueRestoreOrder(
            restorePlaylist = listOf(first, second, third),
            currentSong = third,
            fallbackIndex = 0
        )

        assertEquals(listOf(first, second, third), restoreOrder?.playlist)
        assertEquals(2, restoreOrder?.currentIndex)
    }

    @Test
    fun `shuffle restore falls back to the captured index when current song is missing`() {
        val first = testSong(id = 1L, name = "First")
        val second = testSong(id = 2L, name = "Second")
        val restoreOrder = resolvePlayerQueueRestoreOrder(
            restorePlaylist = listOf(first, second),
            currentSong = testSong(id = 99L, name = "Missing"),
            fallbackIndex = 1
        )

        assertEquals(listOf(first, second), restoreOrder?.playlist)
        assertEquals(1, restoreOrder?.currentIndex)
    }

    @Test
    fun `shuffle restore returns null for an empty restore queue`() {
        val restoreOrder = resolvePlayerQueueRestoreOrder(
            restorePlaylist = emptyList(),
            currentSong = testSong(id = 1L, name = "Song"),
            fallbackIndex = 0
        )

        assertEquals(null, restoreOrder)
    }

    @Test
    fun `queue current index follows dragged current item`() {
        val index = resolveQueueCurrentIndexAfterMove(
            currentIndex = 1,
            fromIndex = 1,
            toIndex = 3,
            queueSize = 5
        )

        assertEquals(3, index)
    }

    @Test
    fun `queue current index shifts when earlier item moves after it`() {
        val index = resolveQueueCurrentIndexAfterMove(
            currentIndex = 2,
            fromIndex = 0,
            toIndex = 3,
            queueSize = 5
        )

        assertEquals(1, index)
    }

    @Test
    fun `queue current index shifts when later item moves before it`() {
        val index = resolveQueueCurrentIndexAfterMove(
            currentIndex = 2,
            fromIndex = 4,
            toIndex = 1,
            queueSize = 5
        )

        assertEquals(3, index)
    }

    @Test
    fun `queue current index survives invalid move`() {
        val index = resolveQueueCurrentIndexAfterMove(
            currentIndex = 2,
            fromIndex = -1,
            toIndex = 1,
            queueSize = 5
        )

        assertEquals(2, index)
    }

    @Test
    fun `queue current index returns missing for empty queue`() {
        val index = resolveQueueCurrentIndexAfterMove(
            currentIndex = 0,
            fromIndex = 0,
            toIndex = 0,
            queueSize = 0
        )

        assertEquals(-1, index)
    }

    @Test
    fun `queue current index stays on next item when current row is removed`() {
        val index = resolveQueueCurrentIndexAfterRemoval(
            currentIndex = 2,
            removedIndex = 2,
            queueSize = 5
        )

        assertEquals(2, index)
    }

    @Test
    fun `queue current index moves to previous item when last current row is removed`() {
        val index = resolveQueueCurrentIndexAfterRemoval(
            currentIndex = 4,
            removedIndex = 4,
            queueSize = 5
        )

        assertEquals(3, index)
    }

    @Test
    fun `queue current index shifts left when an earlier row is removed`() {
        val index = resolveQueueCurrentIndexAfterRemoval(
            currentIndex = 3,
            removedIndex = 1,
            queueSize = 5
        )

        assertEquals(2, index)
    }

    @Test
    fun `queue current index survives invalid removal`() {
        val index = resolveQueueCurrentIndexAfterRemoval(
            currentIndex = 2,
            removedIndex = -1,
            queueSize = 5
        )

        assertEquals(2, index)
    }

    @Test
    fun `queue current index returns missing after removing the only row`() {
        val index = resolveQueueCurrentIndexAfterRemoval(
            currentIndex = 0,
            removedIndex = 0,
            queueSize = 1
        )

        assertEquals(-1, index)
    }

    @Test
    fun `reorder resolves current song when submitted index is stale`() {
        val current = testSong(2L, "Current")
        val queue = listOf(
            testSong(1L, "First"),
            testSong(3L, "Third"),
            current
        )

        val index = resolveQueueCurrentIndexAfterReorder(
            queue = queue,
            currentSong = current,
            submittedCurrentIndex = 1,
            fallbackCurrentIndex = 0
        )

        assertEquals(2, index)
    }

    @Test
    fun `reorder keeps submitted index when it still points to current song`() {
        val current = testSong(2L, "Current")
        val queue = listOf(
            testSong(1L, "First"),
            current,
            testSong(3L, "Third")
        )

        val index = resolveQueueCurrentIndexAfterReorder(
            queue = queue,
            currentSong = current,
            submittedCurrentIndex = 1,
            fallbackCurrentIndex = 0
        )

        assertEquals(1, index)
    }

    @Test
    fun `shuffle restore inserts a queued-next song after the current song`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val third = testSong(3L, "Third")
        val added = testSong(4L, "Added")

        val updated = insertIntoShuffleRestorePlaylist(
            restorePlaylist = listOf(first, second, third),
            song = added,
            currentSong = second,
            afterCurrent = true
        )

        assertEquals(listOf(first, second, added, third), updated)
    }

    @Test
    fun `shuffle restore appends a queued song at the end`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val added = testSong(3L, "Added")

        val updated = insertIntoShuffleRestorePlaylist(
            restorePlaylist = listOf(first, second),
            song = added,
            currentSong = first,
            afterCurrent = false
        )

        assertEquals(listOf(first, second, added), updated)
    }

    @Test
    fun `shuffle restore moves an existing song when it is queued again`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val third = testSong(3L, "Third")

        val updated = insertIntoShuffleRestorePlaylist(
            restorePlaylist = listOf(first, second, third),
            song = first,
            currentSong = third,
            afterCurrent = true
        )

        assertEquals(listOf(second, third, first), updated)
    }

    @Test
    fun `shuffle restore appends the song when the current song is missing`() {
        val first = testSong(1L, "First")
        val added = testSong(2L, "Added")

        val updated = insertIntoShuffleRestorePlaylist(
            restorePlaylist = listOf(first),
            song = added,
            currentSong = testSong(99L, "Missing"),
            afterCurrent = true
        )

        assertEquals(listOf(first, added), updated)
    }

    @Test
    fun `shuffle restore removes the dequeued song`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val third = testSong(3L, "Third")

        val updated = removeFromShuffleRestorePlaylist(
            restorePlaylist = listOf(first, second, third),
            song = second
        )

        assertEquals(listOf(first, third), updated)
    }

    @Test
    fun `shuffle restore keeps the order when removing a missing song`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val restorePlaylist = listOf(first, second)

        val updated = removeFromShuffleRestorePlaylist(
            restorePlaylist = restorePlaylist,
            song = testSong(99L, "Missing")
        )

        assertEquals(restorePlaylist, updated)
    }

    @Test
    fun `shuffle restore replaces the current song in place`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val third = testSong(3L, "Third")
        val replacement = testSong(4L, "Replacement")

        val updated = replaceInShuffleRestorePlaylist(
            restorePlaylist = listOf(first, second, third),
            replacedSong = second,
            song = replacement
        )

        assertEquals(listOf(first, replacement, third), updated)
    }

    @Test
    fun `shuffle restore keeps the position for an identity-only metadata update`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val third = testSong(3L, "Third")
        val renamedSecond = second.copy(name = "Second Renamed")

        val updated = replaceInShuffleRestorePlaylist(
            restorePlaylist = listOf(first, second, third),
            replacedSong = second,
            song = renamedSecond
        )

        assertEquals(listOf(first, renamedSecond, third), updated)
    }

    @Test
    fun `shuffle restore drops the replaced song when the replacement already exists`() {
        val first = testSong(1L, "First")
        val second = testSong(2L, "Second")
        val third = testSong(3L, "Third")

        val updated = replaceInShuffleRestorePlaylist(
            restorePlaylist = listOf(first, second, third),
            replacedSong = second,
            song = third
        )

        assertEquals(listOf(first, third), updated)
    }

    private fun testSong(id: Long, name: String): SongItem {
        return SongItem(
            id = id,
            name = name,
            artist = "Artist",
            album = "Album",
            albumId = id,
            durationMs = 180_000L,
            coverUrl = null
        )
    }

}
