package com.lyra.player.data.sync.webdav

import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URLDecoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class RemoteDirectoryEntry(
    val name: String,
    val url: String,
    val lastModifiedAt: Long,
    val sizeBytes: Long,
    val isDirectory: Boolean
)

/** 解析 WebDAV PROPFIND 的 207 Multistatus 响应。 */
internal object WebDavMultistatusParser {
    private val responseRegex = Regex(
        "<(?:[\\w.-]+:)?response\\b[^>]*>(.*?)</(?:[\\w.-]+:)?response>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val hrefRegex = Regex(
        "<(?:[\\w.-]+:)?href\\b[^>]*>(.*?)</(?:[\\w.-]+:)?href>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val lastModifiedRegex = Regex(
        "<(?:[\\w.-]+:)?getlastmodified\\b[^>]*>(.*?)</",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val contentLengthRegex = Regex(
        "<(?:[\\w.-]+:)?getcontentlength\\b[^>]*>(.*?)</",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val collectionRegex = Regex(
        "<(?:[\\w.-]+:)?collection\\b",
        RegexOption.IGNORE_CASE
    )

    fun parse(xml: String, directoryUrl: String): List<RemoteDirectoryEntry> {
        val directory = directoryUrl.toHttpUrl()
        return responseRegex.findAll(xml).mapNotNull { responseMatch ->
            val block = responseMatch.groupValues[1]
            val rawHref = hrefRegex.find(block)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val resolvedUrl = directory.resolve(rawHref)?.toString() ?: return@mapNotNull null
            val isDirectory = collectionRegex.containsMatchIn(block) || rawHref.endsWith("/")
            RemoteDirectoryEntry(
                name = decodeName(rawHref),
                url = resolvedUrl,
                lastModifiedAt = parseLastModified(lastModifiedRegex.find(block)?.groupValues?.get(1)),
                sizeBytes = contentLengthRegex.find(block)?.groupValues?.get(1)
                    ?.trim()
                    ?.toLongOrNull()
                    ?.coerceAtLeast(0L)
                    ?: 0L,
                isDirectory = isDirectory
            )
        }.toList()
    }

    private fun decodeName(rawHref: String): String {
        val path = rawHref.substringBefore('?').trimEnd('/')
        val segment = path.substringAfterLast('/')
        return runCatching {
            // 先转义加号，避免 URLDecoder 把文件名里的 '+' 变成空格
            URLDecoder.decode(segment.replace("+", "%2B"), "UTF-8")
        }.getOrDefault(segment)
    }

    private fun parseLastModified(rawValue: String?): Long {
        val value = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return 0L
        return runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(0L)
    }
}
