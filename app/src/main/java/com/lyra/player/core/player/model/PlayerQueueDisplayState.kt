package com.lyra.player.core.player.model

import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.sameIdentityAs

data class PlayerQueueDisplayItem(
    val queueIndex: Int,
    val song: SongItem
)

data class PlayerQueueDisplayState(
    val items: List<PlayerQueueDisplayItem>,
    val currentDisplayIndex: Int
) {
    companion object {
        val EMPTY = PlayerQueueDisplayState(
            items = emptyList(),
            currentDisplayIndex = -1
        )
    }
}

internal data class PlayerQueueShuffleOrder(
    val queueIndices: List<Int>,
    val currentIndex: Int
)

internal data class PlayerQueueRestoreOrder(
    val playlist: List<SongItem>,
    val currentIndex: Int
)

internal fun buildPlayerQueueDisplayState(
    playlist: List<SongItem>,
    currentIndex: Int
): PlayerQueueDisplayState {
    if (playlist.isEmpty()) return PlayerQueueDisplayState.EMPTY
    val displayIndices = resolvePlayerQueueDisplayIndices(
        queueSize = playlist.size
    )
    return PlayerQueueDisplayState(
        items = displayIndices.map { index ->
            PlayerQueueDisplayItem(
                queueIndex = index,
                song = playlist[index]
            )
        },
        currentDisplayIndex = displayIndices.indexOf(currentIndex)
    )
}

internal fun resolvePlayerQueueDisplayIndices(
    queueSize: Int
): List<Int> {
    if (queueSize <= 0) return emptyList()
    return List(queueSize) { it }
}

internal fun resolvePlayerSequentialShuffleOrder(
    queueSize: Int,
    currentIndex: Int,
    shuffleRemaining: (MutableList<Int>) -> Unit = { it.shuffle() }
): PlayerQueueShuffleOrder {
    if (queueSize <= 0) {
        return PlayerQueueShuffleOrder(
            queueIndices = emptyList(),
            currentIndex = -1
        )
    }
    val resolvedCurrentIndex = currentIndex.takeIf { it in 0 until queueSize } ?: 0
    val remainingIndices = MutableList(queueSize) { it }
    remainingIndices.remove(resolvedCurrentIndex)
    shuffleRemaining(remainingIndices)
    return PlayerQueueShuffleOrder(
        queueIndices = listOf(resolvedCurrentIndex) + remainingIndices,
        currentIndex = 0
    )
}

internal fun resolvePlayerRepeatAllShuffleOrder(
    queueSize: Int,
    completedIndex: Int,
    shuffleQueue: (MutableList<Int>) -> Unit = { it.shuffle() }
): PlayerQueueShuffleOrder {
    if (queueSize <= 0) {
        return PlayerQueueShuffleOrder(
            queueIndices = emptyList(),
            currentIndex = -1
        )
    }
    val resolvedCompletedIndex = completedIndex.takeIf { it in 0 until queueSize }
        ?: (queueSize - 1)
    val nextCycleIndices = MutableList(queueSize) { it }
    shuffleQueue(nextCycleIndices)
    if (nextCycleIndices.firstOrNull() == resolvedCompletedIndex && nextCycleIndices.size > 1) {
        nextCycleIndices[0] = nextCycleIndices[1]
        nextCycleIndices[1] = resolvedCompletedIndex
    }
    if (
        nextCycleIndices == List(queueSize) { it } &&
        nextCycleIndices.size > 2
    ) {
        val second = nextCycleIndices[1]
        nextCycleIndices[1] = nextCycleIndices[2]
        nextCycleIndices[2] = second
    }
    return PlayerQueueShuffleOrder(
        queueIndices = nextCycleIndices,
        currentIndex = 0
    )
}

internal fun insertIntoShuffleRestorePlaylist(
    restorePlaylist: List<SongItem>,
    song: SongItem,
    currentSong: SongItem?,
    afterCurrent: Boolean
): List<SongItem> {
    val updated = restorePlaylist.toMutableList()
    val existingIndex = updated.indexOfFirst { it.sameIdentityAs(song) }
    if (existingIndex >= 0) {
        updated.removeAt(existingIndex)
    }
    val insertIndex = if (afterCurrent) {
        val anchorIndex = currentSong
            ?.let { current -> updated.indexOfFirst { it.sameIdentityAs(current) } }
            ?: -1
        if (anchorIndex >= 0) anchorIndex + 1 else updated.size
    } else {
        updated.size
    }
    updated.add(insertIndex.coerceIn(0, updated.size), song)
    return updated
}

internal fun removeFromShuffleRestorePlaylist(
    restorePlaylist: List<SongItem>,
    song: SongItem
): List<SongItem> {
    val removedIndex = restorePlaylist.indexOfFirst { it.sameIdentityAs(song) }
    if (removedIndex < 0) return restorePlaylist
    return restorePlaylist.toMutableList().apply { removeAt(removedIndex) }
}

internal fun replaceInShuffleRestorePlaylist(
    restorePlaylist: List<SongItem>,
    replacedSong: SongItem?,
    song: SongItem
): List<SongItem> {
    if (replacedSong != null && replacedSong.sameIdentityAs(song)) {
        val index = restorePlaylist.indexOfFirst { it.sameIdentityAs(song) }
        if (index < 0) return restorePlaylist
        return restorePlaylist.toMutableList().apply { set(index, song) }
    }
    val updated = restorePlaylist.toMutableList()
    val existingIndex = updated.indexOfFirst { it.sameIdentityAs(song) }
    if (existingIndex >= 0) {
        updated.removeAt(existingIndex)
    }
    val replacedIndex = replacedSong
        ?.let { replaced -> updated.indexOfFirst { it.sameIdentityAs(replaced) } }
        ?: -1
    if (replacedIndex >= 0) {
        updated[replacedIndex] = song
    } else {
        updated.add(song)
    }
    return updated
}

internal fun resolvePlayerQueueRestoreOrder(
    restorePlaylist: List<SongItem>?,
    currentSong: SongItem?,
    fallbackIndex: Int
): PlayerQueueRestoreOrder? {
    if (restorePlaylist.isNullOrEmpty()) return null
    val resolvedCurrentIndex = currentSong?.let { song ->
        restorePlaylist.indexOfFirst { it.sameIdentityAs(song) }
            .takeIf { it >= 0 }
    } ?: fallbackIndex.takeIf { it in restorePlaylist.indices }
        ?: 0
    return PlayerQueueRestoreOrder(
        playlist = restorePlaylist.toList(),
        currentIndex = resolvedCurrentIndex
    )
}
