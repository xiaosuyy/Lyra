package com.lyra.player.activity.sync

import android.content.Context
import com.lyra.player.core.startup.sync.StartupSyncWarningRepository
import com.lyra.player.core.startup.sync.StartupSyncWarningState

@Deprecated("use StartupSyncWarningRepository")
internal class GitHubSyncWarningRepository(
    context: Context
) {
    private val delegate = StartupSyncWarningRepository(context)

    suspend fun loadState(): StartupSyncWarningState = delegate.loadState()

    suspend fun setDismissed(dismissed: Boolean) {
        delegate.setDismissed(dismissed)
    }
}
