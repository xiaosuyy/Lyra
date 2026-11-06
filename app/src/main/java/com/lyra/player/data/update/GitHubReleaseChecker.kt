package com.lyra.player.data.update

import android.content.Context
import android.os.Build
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * 从 GitHub Releases 检查更新，公开仓库无需任何登录。
 * 取最新正式 Release，按本机 ABI 选择 APK 资产，Release 正文作为更新内容。
 */
internal class GitHubReleaseChecker(
    context: Context,
    repository: String
) : UpdateChecker {
    private val appContext = context.applicationContext
    private val client: OkHttpClient = AppContainer.sharedOkHttpClient
    private val repoPath = repository.trim().trim('/')

    override fun isConfigured(): Boolean = isValidRepo(repoPath)

    override suspend fun findLatestUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val release = fetchLatestRelease()
            val update = GitHubReleaseParser.parse(
                release = release,
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
                installedAtMillis = installedAtMillis(appContext)
            )
            LyraLogger.d(TAG, "Checked $repoPath: ${update?.versionLabel ?: "up to date"}")
            update
        }.onFailure { LyraLogger.w(TAG, "GitHub release check failed", it) }
    }

    override suspend fun downloadApk(
        update: AppUpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(update.remoteUrl)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
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
        }.onFailure { LyraLogger.w(TAG, "GitHub release download failed", it) }
    }

    /** latest 接口在只有预发布版本时返回 404，此时退回最近的 Release 列表。 */
    private fun fetchLatestRelease(): JsonObject {
        requestJson("$API_BASE/repos/$repoPath/releases/latest")?.let { return it }

        val releases = requestJsonArray("$API_BASE/repos/$repoPath/releases?per_page=10")
            .mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
            .filterNot { it.boolean("draft") }
        return releases.firstOrNull { !it.boolean("prerelease") }
            ?: releases.firstOrNull()
            ?: throw IOException("No GitHub release found for $repoPath")
    }

    private fun requestJson(url: String): JsonObject? {
        execute(url).use { response ->
            if (response.code == 404) return null
            ensureSuccess(response, url)
            return JsonParser.parseString(response.body.string()).asJsonObject
        }
    }

    private fun requestJsonArray(url: String): JsonArray {
        execute(url).use { response ->
            ensureSuccess(response, url)
            return JsonParser.parseString(response.body.string()).asJsonArray
        }
    }

    private fun execute(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return client.newCall(request).execute()
    }

    private fun ensureSuccess(response: Response, url: String) {
        if (response.isSuccessful) return
        val rateLimited = (response.code == 403 || response.code == 429) &&
            response.header("X-RateLimit-Remaining") == "0"
        throw IOException(
            if (rateLimited) "GitHub API rate limit reached"
            else "GitHub API failed: ${response.code} for $url"
        )
    }

    companion object {
        private const val TAG = "GitHubReleaseChecker"
        private const val API_BASE = "https://api.github.com"
        private const val USER_AGENT = "LyraPlayer"
        private val REPO_REGEX = Regex("""^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$""")

        fun isValidRepo(repo: String): Boolean = REPO_REGEX.matches(repo.trim().trim('/'))
    }
}

internal object GitHubReleaseParser {
    private const val MAX_CHANGELOG_CHARS = 8_000

    fun parse(
        release: JsonObject,
        supportedAbis: List<String>,
        installedAtMillis: Long
    ): AppUpdateInfo? {
        val fallbackModifiedAt = release.string("published_at")?.let(::parseInstantMillis) ?: 0L
        val assets = release.getAsJsonArray("assets") ?: return null
        val files = assets.mapNotNull { element ->
            val asset = element.takeIf(JsonElement::isJsonObject)?.asJsonObject
                ?: return@mapNotNull null
            val name = asset.string("name") ?: return@mapNotNull null
            val url = asset.string("browser_download_url") ?: return@mapNotNull null
            RemoteApkFile(
                name = name,
                url = url,
                modifiedAt = asset.string("updated_at")?.let(::parseInstantMillis)
                    ?: fallbackModifiedAt,
                sizeBytes = asset.long("size")
            )
        }
        val latest = AppUpdateSelection.selectLatestApk(
            files = files,
            supportedAbis = supportedAbis
        ) ?: return null
        if (latest.modifiedAt <= installedAtMillis) return null

        return AppUpdateInfo(
            versionLabel = versionLabel(
                tag = release.string("tag_name").orEmpty(),
                assetName = latest.name
            ),
            fileName = latest.name,
            remoteUrl = latest.url,
            sizeBytes = latest.sizeBytes,
            modifiedAt = latest.modifiedAt,
            changelog = release.string("body").orEmpty().trim().take(MAX_CHANGELOG_CHARS)
        )
    }

    /** 优先用 Release 标签去掉品牌前缀后的版本；标签无版本号时退回 APK 文件名。 */
    internal fun versionLabel(tag: String, assetName: String): String {
        val label = AppUpdateSelection.versionLabel(tag)
            .replace(Regex("""^(?:Lyra|NeriPlayer|MeloX)[-_]""", RegexOption.IGNORE_CASE), "")
        return label.ifBlank { AppUpdateSelection.versionLabel(assetName) }
    }

    private fun parseInstantMillis(value: String): Long = runCatching {
        Instant.parse(value).toEpochMilli()
    }.getOrDefault(0L)
}

private fun JsonObject.string(key: String): String? {
    val primitive = get(key)?.takeIf { it.isJsonPrimitive } ?: return null
    return runCatching { primitive.asString }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun JsonObject.long(key: String): Long {
    val primitive = get(key)?.takeIf { it.isJsonPrimitive } ?: return 0L
    return runCatching { primitive.asLong }.getOrDefault(0L)
}

private fun JsonObject.boolean(key: String): Boolean {
    val primitive = get(key)?.takeIf { it.isJsonPrimitive } ?: return false
    return runCatching { primitive.asBoolean }.getOrDefault(false)
}
