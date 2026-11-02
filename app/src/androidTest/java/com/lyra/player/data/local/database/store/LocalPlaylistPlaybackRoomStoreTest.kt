package com.lyra.player.data.local.database.store

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import com.lyra.player.data.local.database.LyraUserDataDatabase
import com.lyra.player.data.playlist.usage.LocalPlaylistPlayBucket
import com.lyra.player.data.playlist.usage.LocalPlaylistPlaybackStat
import com.lyra.player.data.sync.model.SyncPlaybackCounterShard
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalPlaylistPlaybackRoomStoreTest {
    @Test
    fun playbackStatsAndDailyBucketsRoundTrip() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(
            context,
            LyraUserDataDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val stat = LocalPlaylistPlaybackStat(
                playlistId = 42L,
                totalPlayCount = 5L,
                firstPlayedAt = 100L,
                lastPlayedAt = 200L,
                counterBasePlayCount = 1L,
                counterShards = listOf(
                    SyncPlaybackCounterShard(
                        deviceId = "device-a",
                        playCount = 4,
                        firstPlayedAt = 100L,
                        lastPlayedAt = 200L
                    )
                ),
                dailyPlayBuckets = listOf(
                    LocalPlaylistPlayBucket(
                        dayStartAt = 86_400_000L,
                        playCount = 5L,
                        firstPlayedAt = 100L,
                        lastPlayedAt = 200L,
                        counterShards = listOf(
                            SyncPlaybackCounterShard(
                                deviceId = "device-a",
                                playCount = 4,
                                firstPlayedAt = 100L,
                                lastPlayedAt = 200L
                            )
                        )
                    )
                )
            )
            val store = LocalPlaylistPlaybackRoomStore(database)
            store.importLegacyAndPromote(listOf(stat))

            assertEquals(listOf(stat), store.readIfRoomPrimary())
            assertEquals(
                2,
                database.localPlaylistPlaybackDao().getCounterShards().size
            )
        } finally {
            database.close()
        }
    }
}
