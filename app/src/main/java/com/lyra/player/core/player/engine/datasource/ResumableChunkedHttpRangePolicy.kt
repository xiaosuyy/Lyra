@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.lyra.player.core.player.engine.datasource

import android.net.Uri
import androidx.media3.datasource.DataSpec
import com.lyra.player.core.player.resolver.netease.normalizeNeteaseFlacResponseContentType
import com.lyra.player.core.player.resolver.netease.shouldUseNeteaseFlacResumableRange
import com.lyra.player.core.player.resolver.netease.validateNeteaseFlacRangeResponse
import com.lyra.player.core.player.resolver.youtube.YouTubeGoogleVideoRangeSupport

internal data class ResumableChunkedHttpRangePolicy(
    val shouldUse: (DataSpec) -> Boolean,
    val normalizeResponseHeaders: (
        uri: Uri,
        responseHeaders: Map<String, List<String>>
    ) -> Map<String, List<String>> = { _, responseHeaders -> responseHeaders },
    val validateResponse: (
        uri: Uri,
        responseCode: Int,
        responseHeaders: Map<String, List<String>>,
        requestedStartPosition: Long
    ) -> Unit = { _, _, _, _ -> }
)

internal fun shouldUseResumableChunkedHttpRange(dataSpec: DataSpec): Boolean {
    return dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET &&
        !ResumableHttpRangeSupport.hasExplicitRangeHeader(dataSpec.httpRequestHeaders) &&
        (
            YouTubeGoogleVideoRangeSupport.shouldUseChunkedRange(dataSpec.uri) ||
                shouldUseNeteaseFlacResumableRange(dataSpec.uri)
            )
}

internal val defaultResumableChunkedHttpRangePolicy = ResumableChunkedHttpRangePolicy(
    shouldUse = ::shouldUseResumableChunkedHttpRange,
    normalizeResponseHeaders = ::normalizeNeteaseFlacResponseContentType,
    validateResponse = ::validateNeteaseFlacRangeResponse
)
