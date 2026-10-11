package moe.ouom.neriplayer.ui.viewmodel.tab.home

import kotlin.random.Random
import moe.ouom.neriplayer.data.settings.HomeSectionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.home/HomeRandomizationTest
 * Created: 2026/9/28
 */

class HomeRandomizationTest {

    private val pool = (1..30).map { "song$it" }

    @Test
    fun batchHasNoDuplicatesAndRequestedSize() {
        val batch = selectRandomizedBatch(
            items = pool,
            count = 3,
            idOf = { it },
            excludedIds = emptySet(),
            random = Random(1)
        )
        assertEquals(3, batch.size)
        assertEquals(batch.size, batch.distinct().size)
    }

    @Test
    fun recentlyShownItemsAreExcludedWhenPoolIsLargeEnough() {
        val recent = setOf("song1", "song2", "song3", "song4", "song5")
        val batch = selectRandomizedBatch(
            items = pool,
            count = 3,
            idOf = { it },
            excludedIds = recent,
            random = Random(2)
        )
        assertEquals(3, batch.size)
        assertTrue(batch.none { it in recent })
    }

    @Test
    fun fallsBackToRecentItemsWhenPoolIsTooSmallButNeverDuplicatesOrLoops() {
        val smallPool = listOf("a", "b", "c")
        val recent = setOf("a", "b", "c")
        val batch = selectRandomizedBatch(
            items = smallPool,
            count = 3,
            idOf = { it },
            excludedIds = recent,
            random = Random(3)
        )
        assertEquals(3, batch.size)
        assertEquals(3, batch.distinct().size)
    }

    @Test
    fun emptyPoolReturnsEmpty() {
        assertTrue(
            selectRandomizedBatch(
                items = emptyList<String>(),
                count = 3,
                idOf = { it },
                excludedIds = emptySet(),
                random = Random(4)
            ).isEmpty()
        )
    }

    @Test
    fun sameSeedAndGenerationAreStable() {
        val seedA = homeBatchSeed(baseSeed = 42L, generation = 3, groupKey = "netease:trending:TOP_HOT")
        val seedB = homeBatchSeed(baseSeed = 42L, generation = 3, groupKey = "netease:trending:TOP_HOT")
        assertEquals(seedA, seedB)

        val batchA = selectRandomizedBatch(pool, 3, { it }, emptySet(), Random(seedA))
        val batchB = selectRandomizedBatch(pool, 3, { it }, emptySet(), Random(seedB))
        assertEquals(batchA, batchB)
    }

    @Test
    fun differentGenerationGivesDifferentSeed() {
        val seedGen1 = homeBatchSeed(baseSeed = 42L, generation = 1, groupKey = "g")
        val seedGen2 = homeBatchSeed(baseSeed = 42L, generation = 2, groupKey = "g")
        assertFalse(seedGen1 == seedGen2)
    }

    @Test
    fun continueIsNotRandomizedButRadarIs() {
        assertTrue(HomeSectionId.RADAR in HomeRandomizationDefaults.RandomizedSections)
        assertTrue(HomeSectionId.RECOMMENDED in HomeRandomizationDefaults.RandomizedSections)
        assertTrue(HomeSectionId.TRENDING in HomeRandomizationDefaults.RandomizedSections)
        assertFalse(HomeSectionId.CONTINUE in HomeRandomizationDefaults.RandomizedSections)
    }

    @Test
    fun recentlyShownStoreKeepsWindowBounded() {
        val store = RecentlyShownStore(windowSize = 5)
        (1..10).forEach { store.record(listOf("id$it")) }
        assertEquals(5, store.size())
        assertTrue("id10" in store.snapshot())
        assertFalse("id1" in store.snapshot())
    }
}
