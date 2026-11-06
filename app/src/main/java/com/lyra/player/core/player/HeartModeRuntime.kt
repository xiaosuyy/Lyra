package com.lyra.player.core.player

import com.lyra.player.data.model.SongItem

/**
 * 心动模式标记：只有当前队列仍与启动心动模式时下发的一致才算生效。
 * 换播其它队列后标记自然失效，不需要在每个播放入口手动清理。
 */
object HeartModeRuntime {
    @Volatile
    private var heartQueueKeys: List<String> = emptyList()

    fun markQueue(songs: List<SongItem>) {
        heartQueueKeys = songs.map(::keyOf).sorted()
    }

    fun matches(queue: List<SongItem>): Boolean =
        heartQueueKeys.isNotEmpty() && queue.map(::keyOf).sorted() == heartQueueKeys

    private fun keyOf(song: SongItem): String =
        "${song.channelId}:${song.id}:${song.audioId.orEmpty()}"
}
