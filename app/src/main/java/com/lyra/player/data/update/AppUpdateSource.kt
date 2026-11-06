package com.lyra.player.data.update

/**
 * 更新源配置。
 *
 * - [GITHUB_RELEASES_REPO]：GitHub 仓库，如 `"owner/repo"`。优先使用，公开仓库无需登录，
 *   取最新正式 Release 的 APK 资产，Release 正文即更新内容。
 * - [PUBLIC_UPDATE_URL]：备用公开地址，支持 `.apk` 直链、`.json` 清单或以 `/` 结尾的目录。
 * - 两者都留空时，退回已保存的 WebDAV 账号。
 */
internal object AppUpdateSource {
    const val GITHUB_RELEASES_REPO = ""
    const val PUBLIC_UPDATE_URL = ""
}
