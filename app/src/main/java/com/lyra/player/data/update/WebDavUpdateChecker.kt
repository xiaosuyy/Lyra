package com.lyra.player.data.update

import android.content.Context
import android.os.Build
import com.lyra.player.data.sync.webdav.RemoteDirectoryEntry
import com.lyra.player.data.sync.webdav.WebDavApiClient
import com.lyra.player.data.sync.webdav.WebDavStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 用已配置的 WebDAV 账号检查云端目录里是否有比本机安装更新的 APK。
 * 判断依据是 APK 文件的修改时间与系统记录的安装时间。
 */
class WebDavUpdateChecker(context: Context) : UpdateChecker {
    private val appContext = context.applicationContext
    private val storage = WebDavStorage(appContext)

    override fun isConfigured(): Boolean = storage.isConfigured()

    override suspend fun findLatestUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val serverUrl = storage.getServerUrl()?.takeIf { it.isNotBlank() }
            val username = storage.getUsername()?.takeIf { it.isNotBlank() }
            val password = storage.getPassword()?.takeIf { it.isNotBlank() }
            if (serverUrl == null || username == null || password == null) {
                throw IOException("WebDAV is not configured")
            }

            val apiClient = WebDavApiClient(appContext, username, password)
            val directoryUrl = WebDavApiClient.buildRemoteDirectoryUrl(
                serverUrl = serverUrl,
                basePath = storage.getBasePath()
            )
            val entries = apiClient.listDirectory(directoryUrl).getOrThrow()
            val apks = entries
                .filterNot { it.isDirectory }
                .map { entry ->
                    RemoteApkFile(
                        name = entry.name,
                        url = entry.url,
                        modifiedAt = entry.lastModifiedAt,
                        sizeBytes = entry.sizeBytes
                    )
                }
            val latest = AppUpdateSelection.selectLatestApk(
                files = apks,
                supportedAbis = Build.SUPPORTED_ABIS.toList()
            ) ?: return@runCatching null

            if (latest.modifiedAt <= installedAtMillis(appContext)) return@runCatching null

            val changelog = findChangelogEntry(entries, latest.name)?.let { entry ->
                // 更新说明缺失或读取失败时不影响更新本身
                apiClient.fetchTextFile(entry.url).getOrNull()
                    ?.trim()
                    .orEmpty()
            }.orEmpty()

            AppUpdateInfo(
                versionLabel = AppUpdateSelection.versionLabel(latest.name),
                fileName = latest.name,
                remoteUrl = latest.url,
                sizeBytes = latest.sizeBytes,
                modifiedAt = latest.modifiedAt,
                changelog = changelog.take(MAX_CHANGELOG_CHARS)
            )
        }
    }

    /**
     * 更新说明文件优先级：与 APK 同名的 .txt/.md，其次目录内约定的
     * update.txt / update.md / changelog.txt / changelog.md。
     */
    private fun findChangelogEntry(
        entries: List<RemoteDirectoryEntry>,
        apkName: String
    ): RemoteDirectoryEntry? {
        val files = entries.filterNot { it.isDirectory }
        val baseName = apkName.substringBeforeLast('.', apkName)
        val candidates = listOf(
            "$baseName.txt",
            "$baseName.md"
        ) + CONVENTIONAL_CHANGELOG_NAMES
        return candidates.firstNotNullOfOrNull { name ->
            files.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
    }

    override suspend fun downloadApk(
        update: AppUpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val username = storage.getUsername()?.takeIf { it.isNotBlank() }
            val password = storage.getPassword()?.takeIf { it.isNotBlank() }
            if (username == null || password == null) {
                throw IOException("WebDAV is not configured")
            }

            val updateDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
            updateDir.listFiles()?.forEach { it.delete() }
            val safeName = update.fileName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "update.apk" }
            val target = File(updateDir, safeName)

            WebDavApiClient(appContext, username, password)
                .downloadToFile(
                    remoteUrl = update.remoteUrl,
                    targetFile = target
                ) { downloaded, total ->
                    if (total > 0L) {
                        onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
                .getOrThrow()
        }
    }

    private companion object {
        private const val MAX_CHANGELOG_CHARS = 8_000
        private val CONVENTIONAL_CHANGELOG_NAMES = listOf(
            "update.txt",
            "update.md",
            "changelog.txt",
            "changelog.md"
        )
    }
}
