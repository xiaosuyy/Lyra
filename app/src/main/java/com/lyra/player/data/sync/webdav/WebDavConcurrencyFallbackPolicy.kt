package com.lyra.player.data.sync.webdav

internal fun shouldAllowUnconditionalWebDavWrite(
    expectedFingerprint: String?,
    currentFingerprint: String?
): Boolean {
    return !expectedFingerprint.isNullOrBlank() &&
        expectedFingerprint == currentFingerprint
}
