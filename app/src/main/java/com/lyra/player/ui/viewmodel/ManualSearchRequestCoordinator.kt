package com.lyra.player.ui.viewmodel

import com.lyra.player.core.api.search.MusicPlatform

internal data class ManualSearchRequest(
    val keyword: String,
    val platform: MusicPlatform
)

internal data class ManualSearchRequestToken(
    val request: ManualSearchRequest,
    val generation: Long
)

internal class ManualSearchRequestCoordinator {
    private var nextGeneration = 0L
    private var activeToken: ManualSearchRequestToken? = null

    @Synchronized
    fun begin(request: ManualSearchRequest): ManualSearchRequestToken? {
        if (activeToken?.request == request) return null
        return ManualSearchRequestToken(
            request = request,
            generation = ++nextGeneration
        ).also { token ->
            activeToken = token
        }
    }

    @Synchronized
    fun isLatest(token: ManualSearchRequestToken): Boolean {
        return activeToken == token
    }

    @Synchronized
    fun complete(token: ManualSearchRequestToken) {
        if (activeToken == token) {
            activeToken = null
        }
    }

    @Synchronized
    fun invalidate() {
        activeToken = null
    }
}
