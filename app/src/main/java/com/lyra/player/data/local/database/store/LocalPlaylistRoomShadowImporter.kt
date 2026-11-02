package com.lyra.player.data.local.database.store

import android.content.Context
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.local.database.LyraUserDataDatabase
import com.lyra.player.data.local.playlist.LocalPlaylistRepository

internal class LocalPlaylistRoomShadowImporter(
    context: Context,
    private val repository: LocalPlaylistRepository
) {
    private val appContext = context.applicationContext

    suspend fun importIfNeeded() {
        if (!repository.awaitInitialized()) {
            LyraLogger.w(TAG, "Skip Room shadow import because playlist load failed")
            return
        }

        val playlists = repository.playlists.value
        val sourceDigest = LocalPlaylistRoomStore.sourceDigest(playlists)
        val store = LocalPlaylistRoomStore(
            database = LyraUserDataDatabase.getInstance(appContext)
        )
        if (store.isRoomPrimary()) {
            LyraLogger.d(TAG, "Skip legacy shadow import because Room is already primary")
            return
        }
        val result = runCatching {
            store.importShadowSnapshotIfChanged(
                playlists = playlists,
                sourceDigest = sourceDigest
            )
        }.getOrElse { error ->
            LyraLogger.e(TAG, "Room shadow import failed; JSON remains authoritative", error)
            return
        }

        when (result.status) {
            LocalPlaylistRoomShadowImportStatus.IMPORTED -> {
                LyraLogger.i(
                    TAG,
                    "Room shadow import completed: playlists=${result.playlistCount}, " +
                        "members=${result.memberCount}"
                )
            }

            LocalPlaylistRoomShadowImportStatus.SKIPPED_UNCHANGED -> {
                LyraLogger.d(TAG, "Room shadow import skipped because source digest is unchanged")
            }

            LocalPlaylistRoomShadowImportStatus.SKIPPED_NOT_EQUIVALENT -> {
                LyraLogger.w(
                    TAG,
                    "Room shadow import skipped because mapper is not equivalent: " +
                        result.firstMismatch
                )
            }
        }
    }

    private companion object {
        const val TAG = "LocalPlaylistRoomShadow"
    }
}
