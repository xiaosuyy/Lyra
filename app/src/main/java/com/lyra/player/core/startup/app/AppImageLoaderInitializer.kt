package com.lyra.player.core.startup.app

import android.app.Application
import coil.Coil
import coil.EventListener
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger

internal object AppImageLoaderInitializer {
    private const val TAG = "LYRA-Image"

    fun initialize(app: Application) {
        val imageLoader = ImageLoader.Builder(app)
            .okHttpClient { AppContainer.sharedOkHttpClient }
            .respectCacheHeaders(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(app.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(app)
                    .maxSizePercent(0.12)
                    .build()
            }
            .eventListener(object : EventListener {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    LyraLogger.w(
                        TAG,
                        "图片加载失败: data=${request.data}, " +
                            "error=${result.throwable.javaClass.simpleName}: ${result.throwable.message}"
                    )
                }
            })
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
