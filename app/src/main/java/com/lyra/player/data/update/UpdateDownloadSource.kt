package com.lyra.player.data.update

import android.content.Context
import com.lyra.player.data.settings.SettingsKeys
import com.lyra.player.data.settings.dataStore
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Locale

/**
 * APK 下载访问源。
 *
 * GitHub API 检查更新仍直连；gh-proxy 只代理 GitHub Release 资产下载地址。
 */
enum class UpdateDownloadSource(
    val storageValue: String,
    private val proxyPrefix: String?
) {
    GitHub("github", null),
    GhProxy("ghproxy", "https://gh-proxy.com/");

    fun routedDownloadUrl(url: String): String {
        val prefix = proxyPrefix ?: return url
        val parsed = runCatching { url.toHttpUrl() }.getOrNull() ?: return url
        if (!parsed.isHttps || parsed.host !in GITHUB_DOWNLOAD_HOSTS) return url
        return prefix + url
    }

    private companion object {
        val GITHUB_DOWNLOAD_HOSTS = setOf("github.com", "objects.githubusercontent.com")
    }
}

const val DEFAULT_UPDATE_DOWNLOAD_SOURCE = "github"

object UpdateDownloadSourcePolicy {
    fun normalize(value: String): String = fromStorage(value).storageValue

    fun fromStorage(value: String?): UpdateDownloadSource {
        return when (value?.trim()?.lowercase(Locale.ROOT)) {
            UpdateDownloadSource.GhProxy.storageValue,
            "gh_proxy", "gh-proxy" -> UpdateDownloadSource.GhProxy
            else -> UpdateDownloadSource.GitHub
        }
    }
}

internal suspend fun Context.resolveUpdateDownloadSource(): UpdateDownloadSource {
    return runCatching {
        dataStore.data.first()[SettingsKeys.UPDATE_DOWNLOAD_SOURCE]
    }.getOrNull().let(UpdateDownloadSourcePolicy::fromStorage)
}
