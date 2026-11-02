package moe.ouom.neriplayer.core.player.service

internal fun shouldSchedulePlaybackServiceIdleShutdown(
    playerInitialized: Boolean,
    hasPlaybackSurfaceContent: Boolean,
    transportActive: Boolean,
    transportBuffering: Boolean,
    usbSessionActiveOrTransitioning: Boolean,
    sleepTimerActive: Boolean,
): Boolean {
    return playerInitialized &&
        hasPlaybackSurfaceContent &&
        !transportActive &&
        !transportBuffering &&
        !usbSessionActiveOrTransitioning &&
        !sleepTimerActive
}
