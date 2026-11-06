package com.lyra.player.data.update

import android.content.Context
import android.os.Build
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 公开更新源，无需 WebDAV 账号避免登录。[sourceUrl] 支持三种形式：
 * - 以 `.apk` 结尾：直接指向 APK，同目录的 update.txt / changelog.txt 作为更新说明
 * - 以 `.json` 结尾：JSON 清单
 * - 以 `/` 结尾：读取该目录下的 `update.json`
 *
 * 清单格式（除 `url` 外均可选）：
 * ```
 * {
 *   "version": "0.7.3",
 *   "url": "https://example.com/Lyra-0.7.3-arm64-v8a.apk",
 *   "notes": "更新说明",
 *   "updatedAt": "2026-11-06T12:00:00Z",
 *   "size": 12345678
 * }
 * ```
 * 未提供 `updatedAt` 时使用 APK 的 Last-Modified 与安装时间比较。
 */
internal class PublicUpdateChecker(
    context: Context,
    private val sourceUrl: String
) : UpdateChecker {
    private val appContext = context.applicationContext
    private val client: OkHttpClient = AppContainer.sharedOkHttpClient

    override fun isConfigured(): Boolean = sourceUrl.isNotBlank()

    override suspend fun findLatestUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = sourceUrl.trim()
            if (url.isBlank()) throw IOException("Update source is not configured")
            when {
                url.substringBefore('?').endsWith(".json", ignoreCase = true) -> fromManifest(url)
                url.endsWith("/") -> fromManifest(url + MANIFEST_NAME)
                url.substringBefore('?').endsWith(".apk", ignoreCase = true) -> fromDirectApk(url)
                else -> throw IOException("Unsupported update source: $url")
            }
        }.onFailure { LyraLogger.w(TAG, "Public update check failed", it) }
    }

    private fun fromDirectApk(apkUrl: String): AppUpdateInfo? {
        val info = remoteFileInfo(apkUrl)
        val fileName = apkUrl.substringBefore('?').substringAfterLast('/')
        val apk = RemoteApkFile(
            name = fileName,
            url = apkUrl,
            modifiedAt = info.modifiedAt,
            sizeBytes = info.sizeBytes
        )
        val selected = AppUpdateSelection.selectLatestApk(
            files = listOf(apk),
            supportedAbis = Build.SUPPORTED_ABIS.toList()
        ) ?: return null
        if (selected.modifiedAt <= installedAtMillis(appContext)) return null

        val changelog = fetchFirstText(siblingUrls(apkUrl, CONVENTIONAL_CHANGELOG_NAMES)).orEmpty()
        return AppUpdateInfo(
            versionLabel = AppUpdateSelection.versionLabel(selected.name),
            fileName = selected.name.ifBlank { "update.apk" },
            remoteUrl = selected.url,
            sizeBytes = selected.sizeBytes,
            modifiedAt = selected.modifiedAt,
            changelog = changelog.take(MAX_CHANGELOG_CHARS)
        )
    }

    private fun fromManifest(manifestUrl: String): AppUpdateInfo? {
        val json = fetchJson(manifestUrl)
        val apkUrlRaw = json.string("url")
            ?: json.string("apkUrl")
            ?: json.string("downloadUrl")
            ?: throw IOException("Update manifest has no download url")
        val apkUrl = resolveUrl(manifestUrl, apkUrlRaw)
        val fileName = json.string("name")
            ?: json.string("fileName")
            ?: apkUrl.substringBefore('?').substringAfterLast('/')

        var modifiedAt = json.timeMillis("updatedAt").takeIf { it > 0L }
            ?: json.timeMillis("modifiedAt")
        var sizeBytes = json.long("size").takeIf { it > 0L }
            ?: json.long("sizeBytes")
        if (modifiedAt <= 0L) {
            val info = remoteFileInfo(apkUrl)
            modifiedAt = info.modifiedAt
            if (sizeBytes <= 0L) sizeBytes = info.sizeBytes
        }
        if (modifiedAt <= installedAtMillis(appContext)) return null

        val changelog = json.string("notes")
            ?: json.string("changelog")
            ?: json.string("description")
            ?: fetchFirstText(siblingUrls(apkUrl, CONVENTIONAL_CHANGELOG_NAMES)).orEmpty()

        return AppUpdateInfo(
            versionLabel = json.string("version")
                ?: AppUpdateSelection.versionLabel(fileName),
            fileName = fileName.ifBlank { "update.apk" },
            remoteUrl = apkUrl,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
            changelog = changelog.trim().take(MAX_CHANGELOG_CHARS)
        )
    }

    override suspend fun downloadApk(
        update: AppUpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(update.remoteUrl).get().build()
            val updateDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
            updateDir.listFiles()?.forEach { it.delete() }
            val safeName = update.fileName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "update.apk" }
            val target = File(updateDir, safeName)
            val partFile = File(updateDir, "$safeName.part")

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Download failed: ${response.code}")
                    }
                    val totalBytes = response.body.contentLength()
                    response.body.byteStream().use { input ->
                        partFile.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var downloaded = 0L
                            var lastReportAt = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                if (read == 0) continue
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                if (now - lastReportAt >= 200L) {
                                    lastReportAt = now
                                    if (totalBytes > 0L) {
                                        onProgress(
                                            (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                                        )
                                    }
                                }
                            }
                            if (totalBytes > 0L) onProgress(1f)
                        }
                    }
                }
                if (!partFile.renameTo(target)) {
                    partFile.copyTo(target, overwrite = true)
                    partFile.delete()
                }
                target
            } catch (error: Exception) {
                partFile.delete()
                throw error
            }
        }.onFailure { LyraLogger.w(TAG, "Public update download failed", it) }
    }

    private data class RemoteFileInfo(val modifiedAt: Long, val sizeBytes: Long)

    private fun remoteFileInfo(url: String): RemoteFileInfo {
        headRequest(url)?.let { return it }
        // 部分静态托管不支持 HEAD，用 Range GET 兜底取头部
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to inspect $url: ${response.code}")
            }
            RemoteFileInfo(
                modifiedAt = response.header("Last-Modified")?.let(::parseHttpDate) ?: 0L,
                sizeBytes = response.header("Content-Range")
                    ?.substringAfterLast('/')
                    ?.toLongOrNull()
                    ?: response.header("Content-Length")?.toLongOrNull()
                    ?: 0L
            )
        }
    }

    private fun headRequest(url: String): RemoteFileInfo? = runCatching {
        val request = Request.Builder().url(url).head().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val modifiedAt = response.header("Last-Modified")?.let(::parseHttpDate) ?: 0L
            if (modifiedAt <= 0L) return null
            RemoteFileInfo(
                modifiedAt = modifiedAt,
                sizeBytes = response.header("Content-Length")?.toLongOrNull() ?: 0L
            )
        }
    }.getOrNull()

    private fun fetchJson(url: String): JsonObject {
        val text = fetchText(url, MAX_MANIFEST_BYTES)
            ?: throw IOException("Failed to read update manifest: $url")
        return JsonParser.parseString(text).asJsonObject
    }

    private fun fetchFirstText(urls: List<String>): String? =
        urls.firstNotNullOfOrNull { url ->
            fetchText(url)?.trim()?.takeIf { it.isNotEmpty() }
        }

    private fun fetchText(url: String, maxBytes: Int = MAX_CHANGELOG_CHARS * 4): String? {
        return runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val output = ByteArrayOutputStream()
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (total < maxBytes) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        if (read == 0) continue
                        val keep = minOf(read, maxBytes - total)
                        output.write(buffer, 0, keep)
                        total += keep
                    }
                }
                String(output.toByteArray(), Charsets.UTF_8)
            }
        }.getOrNull()
    }

    private fun siblingUrls(apkUrl: String, names: List<String>): List<String> {
        val base = apkUrl.substringBefore('?').substringBeforeLast('/')
        return names.map { "$base/$it" }
    }

    private fun resolveUrl(baseUrl: String, url: String): String {
        if (url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
        ) {
            return url
        }
        return baseUrl.toHttpUrlOrNull()?.resolve(url)?.toString()
            ?: throw IOException("Invalid download url: $url")
    }

    private fun parseHttpDate(value: String): Long = runCatching {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant()
            .toEpochMilli()
    }.getOrDefault(0L)

    private fun JsonObject.string(key: String): String? {
        val primitive = get(key)?.takeIf { it.isJsonPrimitive } ?: return null
        return runCatching { primitive.asString }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.long(key: String): Long {
        val primitive = get(key)?.takeIf { it.isJsonPrimitive } ?: return 0L
        return runCatching { primitive.asLong }.getOrDefault(0L)
    }

    private fun JsonObject.timeMillis(key: String): Long {
        val primitive = get(key)?.takeIf { it.isJsonPrimitive } ?: return 0L
        if (primitive.asJsonPrimitive.isNumber) {
            val raw = primitive.asLong
            return if (raw in 1..SECONDS_UPPER_BOUND) raw * 1000 else raw
        }
        val text = runCatching { primitive.asString }.getOrNull()?.trim().orEmpty()
        if (text.isEmpty()) return 0L
        text.toLongOrNull()?.let { raw ->
            return if (raw in 1..SECONDS_UPPER_BOUND) raw * 1000 else raw
        }
        return runCatching { Instant.parse(text).toEpochMilli() }.getOrDefault(0L)
    }

    private companion object {
        private const val TAG = "PublicUpdateChecker"
        private const val MANIFEST_NAME = "update.json"
        private const val MAX_MANIFEST_BYTES = 256 * 1024
        private const val MAX_CHANGELOG_CHARS = 8_000
        private const val SECONDS_UPPER_BOUND = 999_999_999_999L
        private val CONVENTIONAL_CHANGELOG_NAMES = listOf(
            "update.txt",
            "update.md",
            "changelog.txt",
            "changelog.md"
        )
    }
}
