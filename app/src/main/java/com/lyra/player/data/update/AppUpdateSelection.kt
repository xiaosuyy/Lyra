package com.lyra.player.data.update

internal data class RemoteApkFile(
    val name: String,
    val url: String,
    val modifiedAt: Long,
    val sizeBytes: Long
)

data class AppUpdateInfo(
    val versionLabel: String,
    val fileName: String,
    val remoteUrl: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val changelog: String = ""
)

internal object AppUpdateSelection {
    private val semverRegex = Regex(
        """(?<![\d.])(\d{1,4}\.\d{1,4}(?:\.\d{1,4})?)(?![\d.])"""
    )
    private val abiAliases = mapOf(
        "arm64-v8a" to listOf("arm64-v8a", "arm64"),
        "armeabi-v7a" to listOf("armeabi-v7a", "armv7", "v7a"),
        "armeabi" to listOf("armeabi"),
        "x86_64" to listOf("x86_64", "x64"),
        "x86" to listOf("x86")
    )

    /**
     * 选出云盘里最新的 APK。文件名带 ABI 标记时只接受与本机 ABI 匹配的包；
     * 没有 ABI 标记的通用包在所有标记都不匹配时兜底。
     */
    fun selectLatestApk(
        files: List<RemoteApkFile>,
        supportedAbis: List<String>
    ): RemoteApkFile? {
        val apks = files.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) return null

        val abiMatched = supportedAbis.firstNotNullOfOrNull { abi ->
            val aliases = abiAliases[abi] ?: listOf(abi)
            apks.filter { apk ->
                aliases.any { alias -> containsAbiToken(apk.name, alias) }
            }.takeIf { it.isNotEmpty() }
        }
        val candidates = abiMatched ?: apks.filterNot { apk -> hasKnownAbiTag(apk.name) }
        return candidates.maxByOrNull { it.modifiedAt }
    }

    /** 文件名里带 0.7.2 这类版本号时直接展示；否则退化为去掉 .apk 的文件名。 */
    fun versionLabel(fileName: String): String {
        val baseName = fileName
            .substringAfterLast('/')
            .replace(Regex("""\.apk$""", RegexOption.IGNORE_CASE), "")
        semverRegex.find(baseName)?.groupValues?.get(1)?.let { return it }
        return baseName.ifBlank { fileName }
    }

    private fun hasKnownAbiTag(fileName: String): Boolean =
        abiAliases.values.flatten().any { alias -> containsAbiToken(fileName, alias) }

    private fun containsAbiToken(fileName: String, token: String): Boolean {
        val pattern = Regex(
            """(?<![A-Za-z0-9_])${Regex.escape(token)}(?![A-Za-z0-9_])""",
            RegexOption.IGNORE_CASE
        )
        return pattern.containsMatchIn(fileName)
    }
}
