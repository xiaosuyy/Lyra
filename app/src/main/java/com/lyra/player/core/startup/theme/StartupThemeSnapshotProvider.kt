package com.lyra.player.core.startup.theme

import android.content.Context
import com.lyra.player.data.settings.ThemePreferenceSnapshot
import com.lyra.player.data.settings.readThemePreferenceSnapshotSync

internal object StartupThemeSnapshotProvider {
    fun read(
        context: Context,
        safeModeActive: Boolean
    ): ThemePreferenceSnapshot {
        return if (safeModeActive) {
            ThemePreferenceSnapshot()
        } else {
            readThemePreferenceSnapshotSync(context)
        }
    }
}
