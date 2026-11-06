package com.lyra.player.data.sync.webdav

import android.content.Context
import com.lyra.player.R
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class WebDavAuthException(message: String) : IOException(message)

class WebDavFileNotFoundException(message: String) : IOException(message)

class WebDavSyncInProgressException(message: String) : IOException(message)

open class WebDavApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

class WebDavContentConflictException(
    statusCode: Int,
    message: String
) : WebDavApiException(statusCode, message)

class WebDavMissingConcurrencyTokenException(message: String) : IOException(message)

class WebDavApiClient(
    context: Context,
    username: String,
    password: String
) {
    private val appContext = context.applicationContext
    private val client: OkHttpClient = AppContainer.sharedOkHttpClient
    private val authorizationHeader = Credentials.basic(username, password)

    companion object {
        private const val TAG = "WebDavApiClient"
        private const val DEFAULT_REMOTE_FILE_NAME = "Lyra-sync.json"

        /** 旧版远端文件名，仅用于读取迁移，写入始终使用 [DEFAULT_REMOTE_FILE_NAME]。 */
        const val LEGACY_REMOTE_FILE_NAME = "neriplayer-sync.json"

        fun calculateFingerprint(content: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(content)
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun buildRemoteFileUrl(
            serverUrl: String,
            basePath: String,
            fileName: String = DEFAULT_REMOTE_FILE_NAME
        ): String {
            val normalizedServerUrl = serverUrl.trim().trimEnd('/')
            val normalizedBasePath = basePath.trim().trim('/')
            val urlBuilder = normalizedServerUrl.toHttpUrl().newBuilder()
            if (normalizedBasePath.isNotBlank()) {
                normalizedBasePath
                    .split('/')
                    .filter(String::isNotBlank)
                    .forEach(urlBuilder::addPathSegment)
            }
            urlBuilder.addPathSegment(fileName)
            return urlBuilder.build().toString()
        }

        /** 目录 URL 以 / 结尾，便于直接用作 PROPFIND 目标。 */
        fun buildRemoteDirectoryUrl(serverUrl: String, basePath: String): String {
            val normalizedServerUrl = serverUrl.trim().trimEnd('/')
            val normalizedBasePath = basePath.trim().trim('/')
            val urlBuilder = normalizedServerUrl.toHttpUrl().newBuilder()
            if (normalizedBasePath.isNotBlank()) {
                normalizedBasePath
                    .split('/')
                    .filter(String::isNotBlank)
                    .forEach(urlBuilder::addPathSegment)
            }
            return urlBuilder.addPathSegment("").build().toString()
        }
    }

    data class ConcurrencyToken(
        val etag: String? = null,
        val lastModified: String? = null
    ) {
        fun hasConditionToken(): Boolean {
            return !etag.isNullOrBlank() || !lastModified.isNullOrBlank()
        }
    }

    class RemoteFileSnapshot(
        val content: ByteArray,
        val fingerprint: String,
        val version: ConcurrencyToken
    )

    data class WriteResult(
        val fingerprint: String,
        val version: ConcurrencyToken
    )

    fun validateConnection(serverUrl: String, basePath: String): Result<Unit> {
        return runCatching {
            val remoteUrl = buildRemoteFileUrl(serverUrl, basePath)
            val request = Request.Builder()
                .url(remoteUrl)
                .header("Authorization", authorizationHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful || response.code == 404 -> Unit
                    response.code == 401 || response.code == 403 -> {
                        throw WebDavAuthException(
                            appContext.getString(R.string.webdav_auth_failed)
                        )
                    }

                    else -> {
                        val errorBody = response.body.string()
                        throw WebDavApiException(
                            response.code,
                            "WebDAV validate failed: ${response.code}${errorBody.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""}"
                        )
                    }
                }
            }
        }.onFailure {
            LyraLogger.e(TAG, "Validate WebDAV connection failed", it)
        }
    }

    fun getFileContentStrict(remoteUrl: String): Result<RemoteFileSnapshot> {
        return runCatching {
            val request = Request.Builder()
                .url(remoteUrl)
                .header("Authorization", authorizationHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> {
                        val body = response.body.bytes()
                        RemoteFileSnapshot(
                            content = body,
                            fingerprint = calculateFingerprint(body),
                            version = extractConcurrencyToken(response)
                        )
                    }

                    response.code == 401 || response.code == 403 -> {
                        throw WebDavAuthException(
                            appContext.getString(R.string.webdav_auth_failed)
                        )
                    }

                    response.code == 404 -> {
                        throw WebDavFileNotFoundException("Remote backup file not found: $remoteUrl")
                    }

                    else -> {
                        val errorBody = response.body.string()
                        throw WebDavApiException(
                            response.code,
                            "Failed to get file: ${response.code}${errorBody.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""}"
                        )
                    }
                }
            }
        }.onFailure {
            LyraLogger.e(TAG, "Get WebDAV file content failed", it)
        }
    }

    /** 列目录 (Depth: 1)，返回目录本身及其直接子项。 */
    fun listDirectory(directoryUrl: String): Result<List<RemoteDirectoryEntry>> {
        return runCatching {
            val body = """<?xml version="1.0" encoding="utf-8"?>
                <d:propfind xmlns:d="DAV:">
                  <d:prop>
                    <d:resourcetype/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                  </d:prop>
                </d:propfind>""".trimIndent()
            val request = Request.Builder()
                .url(directoryUrl)
                .header("Authorization", authorizationHeader)
                .header("Depth", "1")
                .header("Accept", "application/xml")
                .method("PROPFIND", body.toRequestBody("application/xml; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> WebDavMultistatusParser.parse(
                        xml = response.body.string(),
                        directoryUrl = directoryUrl
                    )

                    response.code == 401 || response.code == 403 -> {
                        throw WebDavAuthException(
                            appContext.getString(R.string.webdav_auth_failed)
                        )
                    }

                    response.code == 404 -> {
                        throw WebDavFileNotFoundException("Remote directory not found: $directoryUrl")
                    }

                    else -> {
                        throw WebDavApiException(
                            response.code,
                            "Failed to list directory: ${response.code}"
                        )
                    }
                }
            }
        }.onFailure {
            LyraLogger.e(TAG, "List WebDAV directory failed", it)
        }
    }

    fun downloadToFile(
        remoteUrl: String,
        targetFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> {
        return runCatching {
            val request = Request.Builder()
                .url(remoteUrl)
                .header("Authorization", authorizationHeader)
                .get()
                .build()
            val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
            targetFile.parentFile?.mkdirs()
            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> {
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
                                            onProgress(downloaded, totalBytes)
                                        }
                                    }
                                    onProgress(downloaded, totalBytes)
                                }
                            }
                            if (!partFile.renameTo(targetFile)) {
                                partFile.copyTo(targetFile, overwrite = true)
                                partFile.delete()
                            }
                            targetFile
                        }

                        response.code == 401 || response.code == 403 -> {
                            throw WebDavAuthException(
                                appContext.getString(R.string.webdav_auth_failed)
                            )
                        }

                        response.code == 404 -> {
                            throw WebDavFileNotFoundException("Remote file not found: $remoteUrl")
                        }

                        else -> {
                            throw WebDavApiException(
                                response.code,
                                "Failed to download file: ${response.code}"
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                partFile.delete()
                throw error
            }
        }.onFailure {
            LyraLogger.e(TAG, "Download WebDAV file failed", it)
        }
    }

    /** 读取纯文本文件（更新说明等），超出 [maxBytes] 的部分直接截断。 */
    fun fetchTextFile(remoteUrl: String, maxBytes: Int = 64 * 1024): Result<String> {
        return runCatching {
            val request = Request.Builder()
                .url(remoteUrl)
                .header("Authorization", authorizationHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> {
                        val output = java.io.ByteArrayOutputStream()
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

                    response.code == 401 || response.code == 403 -> {
                        throw WebDavAuthException(
                            appContext.getString(R.string.webdav_auth_failed)
                        )
                    }

                    response.code == 404 -> {
                        throw WebDavFileNotFoundException("Remote file not found: $remoteUrl")
                    }

                    else -> {
                        throw WebDavApiException(
                            response.code,
                            "Failed to read text file: ${response.code}"
                        )
                    }
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "Read WebDAV text file failed", it)
        }
    }

    fun updateFileContent(
        remoteUrl: String,
        content: ByteArray,
        mediaType: String = "application/octet-stream",
        expectedVersion: ConcurrencyToken? = null,
        createOnly: Boolean = false,
        allowUnconditionalWrite: Boolean = false
    ): Result<WriteResult> {
        return runCatching {
            if (
                !createOnly &&
                expectedVersion != null &&
                !expectedVersion.hasConditionToken() &&
                !allowUnconditionalWrite
            ) {
                throw WebDavMissingConcurrencyTokenException(
                    "WebDAV server does not expose ETag or Last-Modified for conditional sync"
                )
            }

            val requestBuilder = Request.Builder()
                .url(remoteUrl)
                .header("Authorization", authorizationHeader)

            if (createOnly) {
                requestBuilder.header("If-None-Match", "*")
            } else {
                expectedVersion?.etag?.let { requestBuilder.header("If-Match", it) }
                if (expectedVersion?.etag.isNullOrBlank()) {
                    expectedVersion?.lastModified?.let {
                        requestBuilder.header("If-Unmodified-Since", it)
                    }
                }
            }

            val request = Request.Builder()
                .url(remoteUrl)
                .headers(requestBuilder.build().headers)
                .put(content.toRequestBody(mediaType.toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> WriteResult(
                        fingerprint = calculateFingerprint(content),
                        version = extractConcurrencyToken(response)
                    )
                    response.code == 401 || response.code == 403 -> {
                        throw WebDavAuthException(
                            appContext.getString(R.string.webdav_auth_failed)
                        )
                    }

                    response.code == 409 ||
                        response.code == 412 ||
                        response.code == 423 -> {
                        val errorBody = response.body.string()
                        throw WebDavContentConflictException(
                            statusCode = response.code,
                            message = "Failed to update file: ${response.code}${errorBody.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""}"
                        )
                    }

                    else -> {
                        val errorBody = response.body.string()
                        throw WebDavApiException(
                            response.code,
                            "Failed to update file: ${response.code}${errorBody.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""}"
                        )
                    }
                }
            }
        }.onFailure {
            LyraLogger.e(TAG, "Update WebDAV file content failed", it)
        }
    }

    private fun extractConcurrencyToken(response: okhttp3.Response): ConcurrencyToken {
        val etag = response.header("ETag")
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.startsWith("W/") }
        val lastModified = response.header("Last-Modified")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return ConcurrencyToken(
            etag = etag,
            lastModified = lastModified
        )
    }
}
