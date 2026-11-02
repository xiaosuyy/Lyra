package com.lyra.player.core.startup.logging

import android.content.Context
import com.lyra.player.core.logging.LyraLogger

internal object StartupLogInitializer {
    fun shouldEnableFileLogging(
        devModeEnabled: Boolean,
        alwaysRecordLogsEnabled: Boolean
    ): Boolean {
        return devModeEnabled || alwaysRecordLogsEnabled
    }

    fun sync(
        context: Context,
        devModeEnabled: Boolean,
        alwaysRecordLogsEnabled: Boolean
    ) {
        LyraLogger.init(
            context = context,
            enableFileLogging = shouldEnableFileLogging(
                devModeEnabled = devModeEnabled,
                alwaysRecordLogsEnabled = alwaysRecordLogsEnabled
            )
        )
    }
}
