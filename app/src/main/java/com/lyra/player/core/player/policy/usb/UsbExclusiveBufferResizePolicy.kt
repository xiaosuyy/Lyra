package com.lyra.player.core.player.policy.usb

import com.lyra.player.data.settings.normalizeUsbExclusiveBackgroundBufferMs
import com.lyra.player.data.settings.normalizeUsbExclusiveForegroundBufferMs

internal fun shouldApplyActiveUsbBufferResize(
    streaming: Boolean,
    currentBufferMs: Int,
    targetBufferMs: Int
): Boolean {
    if (streaming) return false
    return targetBufferMs != currentBufferMs
}

internal fun usbExclusiveTransferWindowDurationMs(
    bufferDurationMs: Int,
    appInForeground: Boolean
): Int {
    return if (appInForeground) {
        normalizeUsbExclusiveForegroundBufferMs(bufferDurationMs)
    } else {
        normalizeUsbExclusiveBackgroundBufferMs(bufferDurationMs)
    }
}
