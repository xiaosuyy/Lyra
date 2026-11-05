package com.lyra.player.ui.screen.tab.settings.page

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyra.player.R
import com.lyra.player.data.settings.generated.AutoSettingInfo
import com.lyra.player.data.settings.generated.AutoSettingsMetadata
import com.lyra.player.data.settings.generated.AutoSettingsSections
import com.lyra.player.ksp.annotations.SettingUiType
import com.lyra.player.util.search.SearchTextMatcher

internal data class SettingsSearchEntry(
    val id: String,
    val page: SettingsPage,
    val title: String,
    val description: String,
    val tokens: List<String>,
    val targetId: String,
    val order: Int
)

internal data class SettingsSearchScrollAnchor(
    val itemIndex: Int,
    val scrollOffset: Dp = 0.dp
)

internal fun buildSettingsSearchEntries(context: Context): List<SettingsSearchEntry> {
    val pageEntries = SettingsPage.entries.map { page ->
        val title = context.safeString(page.titleRes)
        val description = context.safeString(page.descriptionRes)
        SettingsSearchEntry(
            id = "page:${page.name}",
            page = page,
            title = title,
            description = description,
            tokens = listOf(title, page.name) + page.searchAliases(),
            targetId = "page:${page.name}",
            order = page.ordinal * 100
        )
    }

    val settingEntries = AutoSettingsMetadata.settings
        // YouTube 渠道已从 Lyra 移除，相关设置不再出现在搜索结果里
        .filter {
            it.ui != SettingUiType.None &&
                it.titleRes != 0 &&
                !it.isRemovedYouTubeSetting() &&
                !it.isRemovedLyraSetting()
        }
        .mapNotNull { setting ->
            val page = setting.settingsPage() ?: return@mapNotNull null
            val title = context.safeString(setting.titleRes)
            val description = context.safeString(setting.descriptionRes)
            val pageTitle = context.safeString(page.titleRes)
            SettingsSearchEntry(
                id = "setting:${setting.keyName}",
                page = page,
                title = title.ifBlank { setting.keyName },
                description = description.ifBlank { pageTitle },
                tokens = setting.searchTokens(title = title, description = description) +
                    listOf(pageTitle, page.name, setting.section) +
                    page.searchAliases(),
                targetId = setting.searchTargetId(),
                order = page.ordinal * 100 + setting.order
            )
        }

    return pageEntries + settingEntries + manualSettingsSearchEntries(context)
}

internal fun searchSettingsEntries(
    entries: List<SettingsSearchEntry>,
    query: String,
    limit: Int = 40
): List<SettingsSearchEntry> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return emptyList()

    return entries
        .mapNotNull { entry ->
            val score = SearchTextMatcher.score(normalizedQuery, entry.tokens)
                ?: return@mapNotNull null
            entry to score
        }
        .sortedWith(
            compareBy<Pair<SettingsSearchEntry, Int>> { it.second }
                .thenBy { it.first.order }
                .thenBy { it.first.title }
        )
        .map { it.first }
        .distinctBy { it.visibleDedupeKey() }
        .take(limit)
}

/** 已从 Lyra 移除的 YouTube 设置，不再参与设置搜索。 */
internal fun AutoSettingInfo.isRemovedYouTubeSetting(): Boolean =
    keyName.contains("youtube", ignoreCase = true)

/** 已从设置页移除的旧开关，仅保留数据键以兼容历史配置。 */
private val RemovedLyraSettingKeys = setOf(
    "dynamic_color",
    "home_card_continue",
    // 首页已改为固定 Melox 布局，旧首页栏目开关不再出现在设置搜索里。
    "home_card_trending",
    "home_card_radar",
    "home_card_recommended",
    "always_use_new_tab_style",
    // 已从设置页移除的开关与滑块，仅保留数据键以兼容历史配置。
    "haptic_feedback_enabled",
    "bili_skip_segment_prompt_enabled",
    "show_cover_source_badge",
    "nowplaying_show_title",
    "nowplaying_cover_lyrics_enabled",
    "nowplaying_progress_show_quality_switch",
    "nowplaying_toolbar_dock_enabled",
    "nowplaying_control_placement",
    "nowplaying_control_size",
    "bili_sponsor_block_enabled",
    "netease_local_source_fallback",
    "netease_auto_source_switch",
    "cloud_music_lyric_default_offset_ms",
    "qq_music_lyric_default_offset_ms",
    "nowplaying_cover_lyric_font_scale",
    "nowplaying_cover_translation_font_scale",
    "lyric_font_scale"
)

internal fun AutoSettingInfo.isRemovedLyraSetting(): Boolean =
    keyName in RemovedLyraSettingKeys

internal fun AutoSettingInfo.settingsPage(): SettingsPage? {
    if (keyName in LyricAppearanceSettingKeys) {
        return SettingsPage.Lyrics
    }
    if (
        keyName == "netease_auto_source_switch" ||
        keyName == "netease_local_source_fallback"
    ) {
        return SettingsPage.PlaybackSource
    }
    if (section == AutoSettingsSections.playback && keyName.startsWith("usb_exclusive")) {
        return SettingsPage.UsbExclusive
    }
    return settingsPageForSection(section)
}

internal fun settingsPageForSection(section: String): SettingsPage? {
    return when (section) {
        AutoSettingsSections.general -> SettingsPage.General
        AutoSettingsSections.theme -> SettingsPage.Theme
        // 音质设置页已整体移除
        AutoSettingsSections.audioQuality -> null
        AutoSettingsSections.personalization -> SettingsPage.Personalization
        AutoSettingsSections.display -> SettingsPage.Personalization
        AutoSettingsSections.motion -> SettingsPage.Motion
        AutoSettingsSections.lyrics -> SettingsPage.Lyrics
        AutoSettingsSections.network -> SettingsPage.Network
        AutoSettingsSections.download -> SettingsPage.Downloads
        AutoSettingsSections.trafficManagement -> SettingsPage.TrafficManagement
        AutoSettingsSections.storage -> SettingsPage.Storage
        AutoSettingsSections.backup -> SettingsPage.Backup
        AutoSettingsSections.playback -> SettingsPage.Playback
        else -> null
    }
}

private fun AutoSettingInfo.searchAliases(): List<String> {
    val keyTokens = keyName.split('_')
    val propertyTokens = propertyName.split(Regex("(?=[A-Z])"))
    return keyTokens + propertyTokens + SettingSearchAliases[keyName].orEmpty()
}

internal fun AutoSettingInfo.searchTokens(
    title: String,
    description: String
): List<String> {
    return listOf(
        title,
        description,
        keyName,
        propertyName
    ) + searchAliases()
}

internal fun AutoSettingInfo.searchTargetId(): String {
    val targetKey = when (keyName) {
        "playback_fade_in_duration_ms",
        "playback_fade_out_duration_ms" -> "playback_fade_in"
        "playback_crossfade_in_duration_ms",
        "playback_crossfade_out_duration_ms" -> "playback_crossfade_next"
        "enhanced_advanced_blur_radius_dp" -> "advanced_blur_enabled"
        "advanced_blur_quality" -> "advanced_blur_enabled"
        "nowplaying_cover_blur_amount",
        "nowplaying_cover_blur_darken" -> "nowplaying_cover_blur_background_enabled"
        "lyric_blur_amount" -> "lyric_blur_enabled"
        "standardized_lyric_embedding_enabled" -> "download_metadata_post_processing_enabled"
        else -> keyName
    }
    return "setting:$targetKey"
}

internal fun resolveSettingsSearchHighlightTarget(
    entry: SettingsSearchEntry,
    mobileDataFollowDefaultAudioQuality: Boolean = false,
    hasCustomBackground: Boolean = true
): String {
    return when {
        mobileDataFollowDefaultAudioQuality &&
            entry.targetId in MobileDataQualityHiddenSearchTargets -> {
            "setting:mobile_data_follow_default_audio_quality"
        }
        !hasCustomBackground && entry.targetId in BackgroundImageHiddenSearchTargets -> {
            "setting:background_image_uri"
        }
        else -> entry.targetId
    }
}

internal fun settingsSearchScrollAnchor(
    page: SettingsPage,
    targetId: String
): SettingsSearchScrollAnchor {
    if (targetId.startsWith("page:")) {
        return SettingsSearchScrollAnchor(itemIndex = 0)
    }

    return when (page) {
        SettingsPage.Theme -> when (targetId) {
            "manual:theme_mode" -> SettingsSearchScrollAnchor(itemIndex = 1)
            else -> SettingsSearchScrollAnchor(itemIndex = 2)
        }
        SettingsPage.Motion -> SettingsSearchScrollAnchor(
            itemIndex = 1 + motionCardIndex(targetId)
        )
        SettingsPage.Lyrics -> SettingsSearchScrollAnchor(
            itemIndex = 1 + lyricsCardIndex(targetId)
        )
        SettingsPage.Playback -> SettingsSearchScrollAnchor(
            itemIndex = 1 + playbackCardIndex(targetId)
        )
        SettingsPage.Storage -> SettingsSearchScrollAnchor(
            itemIndex = 1 + storageCardIndex(targetId)
        )
        SettingsPage.Backup -> SettingsSearchScrollAnchor(
            itemIndex = 1 + backupCardIndex(targetId)
        )
        SettingsPage.Personalization -> SettingsSearchScrollAnchor(
            itemIndex = 1 + personalizationCardIndex(targetId)
        )
        else -> SettingsSearchScrollAnchor(itemIndex = 1)
    }
}

private fun lyricsCardIndex(targetId: String): Int {
    return when (targetId) {
        "setting:floating_lyrics_enabled" -> 0
        in LyricAppearanceSearchTargets -> 2
        else -> 1
    }
}

private fun playbackCardIndex(targetId: String): Int {
    return when (targetId) {
        "setting:playback_high_resolution_output_enabled",
        "setting:playback_volume_normalization_enabled",
        "setting:playback_volume_balance",
        "setting:usb_exclusive_playback",
        "setting:allow_mixed_playback",
        "setting:preempt_audio_focus" -> 1
        "setting:playback_fade_in" -> 2
        "setting:playback_crossfade_next" -> 3
        else -> 0
    }
}

private fun storageCardIndex(targetId: String): Int {
    return when (targetId) {
        "setting:download_file_name_template" -> 1
        "setting:max_cache_size_bytes" -> 2
        "manual:clear_cache" -> 3
        else -> 0
    }
}

private fun backupCardIndex(targetId: String): Int {
    return when (targetId) {
        "manual:playlist_export",
        "manual:playlist_import",
        "manual:config_export",
        "manual:config_import" -> 0
        "manual:backup_history" -> 1
        "manual:github_sync",
        "manual:github_auto_sync",
        "setting:silent_github_sync_failure" -> 2
        "manual:webdav_sync",
        "manual:webdav_auto_sync" -> 3
        "manual:backup_messages" -> 4
        else -> 0
    }
}

private fun motionCardIndex(targetId: String): Int {
    return when (targetId) {
        "setting:advanced_blur_enabled",
        "setting:enhanced_advanced_blur_enabled",
        "setting:enhanced_advanced_blur_radius_dp",
        "setting:advanced_blur_quality" -> 1
        "setting:nowplaying_cover_blur_background_enabled",
        "setting:nowplaying_cover_blur_amount",
        "setting:nowplaying_cover_blur_darken",
        "setting:nowplaying_audio_reactive_enabled",
        "setting:nowplaying_dynamic_background_enabled" -> 2
        "setting:lyric_blur_enabled",
        "setting:lyric_blur_amount" -> 3
        else -> 0
    }
}

private val MobileDataQualityHiddenSearchTargets = setOf(
    "setting:mobile_data_netease_audio_quality",
    "setting:mobile_data_bili_audio_quality"
)

private val BackgroundImageHiddenSearchTargets = setOf(
    "setting:background_image_blur",
    "setting:background_image_alpha"
)

private fun personalizationCardIndex(targetId: String): Int {
    return when (targetId) {
        in PersonalizationStartTargets -> 0
        in PersonalizationPlaybackInfoTargets -> 1
        in PersonalizationPlaybackControlTargets -> 2
        in PersonalizationBackgroundTargets -> 3
        else -> 0
    }
}

private val PersonalizationStartTargets = setOf(
    "setting:default_start_destination",
    "setting:auto_show_keyboard"
)

private val PersonalizationPlaybackInfoTargets = setOf(
    "setting:nowplaying_song_title_marquee_enabled",
    "setting:nowplaying_progress_show_audio_codec",
    "setting:nowplaying_progress_show_audio_spec"
)

private val PersonalizationPlaybackControlTargets = setOf(
    "setting:nowplaying_keep_screen_on",
    "setting:lyrics_control_size"
)

private val LyricAppearanceSettingKeys = setOf(
    "show_lyric_translation",
    "lyric_translation_use_phonetic",
    "lyric_font_scale",
    "lyrics_page_lyric_font_scale",
    "lyrics_page_translation_font_scale"
)

private val LyricAppearanceSearchTargets = setOf(
    "setting:lyric_font_scale",
    "setting:lyrics_page_lyric_font_scale",
    "setting:lyrics_page_translation_font_scale",
    "setting:show_lyric_translation",
    "setting:lyric_translation_use_phonetic"
)

private val PersonalizationBackgroundTargets = setOf(
    "setting:background_image_uri",
    "setting:background_image_blur",
    "setting:background_image_alpha"
)

private fun SettingsSearchEntry.visibleDedupeKey(): String {
    return listOf(page.name, title.normalizedDedupeText(), description.normalizedDedupeText())
        .joinToString("|")
}

private fun String.normalizedDedupeText(): String {
    return trim().lowercase()
}

private fun SettingsPage.searchAliases(): List<String> {
    return PageSearchAliases[this].orEmpty()
}

internal fun manualSettingsSearchEntries(context: Context): List<SettingsSearchEntry> {
    fun entry(
        page: SettingsPage,
        titleRes: Int,
        descriptionRes: Int,
        id: String,
        aliases: List<String>,
        targetId: String = "manual:$id",
        order: Int = page.ordinal * 100 + 80
    ): SettingsSearchEntry {
        val title = context.safeString(titleRes)
        val description = context.safeString(descriptionRes)
        return SettingsSearchEntry(
            id = "manual:$id",
            page = page,
            title = title,
            description = description,
            tokens = listOf(
                title,
                description,
                id
            ) + aliases,
            targetId = targetId,
            order = order
        )
    }

    return listOf(
        entry(
            page = SettingsPage.General,
            titleRes = R.string.language_setting_title,
            descriptionRes = R.string.language_select_title,
            id = "language",
            aliases = listOf(
                "语言设置",
                "设置语言",
                "选择语言",
                "language",
                "language setting",
                "app language",
                "locale",
                "lang",
                "yuyan",
                "yy",
                "zhongwen",
                "yingyu",
                "english",
                "chinese"
            ),
            order = 4
        ),
        entry(
            page = SettingsPage.General,
            titleRes = R.string.settings_notification_permission_title,
            descriptionRes = R.string.settings_notification_permission_denied,
            id = "notification_permission",
            aliases = listOf(
                "通知",
                "播放通知",
                "notification",
                "notifications",
                "permission",
                "tongzhi",
                "quanxian"
            )
        ),
        entry(
            page = SettingsPage.Theme,
            titleRes = R.string.settings_theme_mode,
            descriptionRes = R.string.settings_theme_mode_desc,
            id = "theme_mode",
            aliases = listOf("dark", "light", "system", "auto", "shense", "qianse", "zidong")
        ),
        entry(
            page = SettingsPage.Theme,
            titleRes = R.string.settings_accent_color,
            descriptionRes = R.string.settings_accent_color_desc,
            id = "accent_color",
            aliases = listOf(
                "accent",
                "color",
                "theme color",
                "custom color",
                "qiangdiao se",
                "zhutise",
                "yanse"
            )
        ),
        entry(
            page = SettingsPage.Personalization,
            titleRes = R.string.settings_lyrics_control_size,
            descriptionRes = R.string.settings_lyrics_control_size_desc,
            id = "lyrics_control_size",
            aliases = listOf(
                "歌词页控件大小",
                "歌词按钮大小",
                "lyrics controls size",
                "lyrics button size",
                "gecikongjian"
            ),
            targetId = "setting:lyrics_control_size",
            order = SettingsPage.Personalization.ordinal * 100 + 36
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.playlist_export,
            descriptionRes = R.string.playlist_export_desc,
            id = "playlist_export",
            aliases = listOf(
                "导出歌单",
                "歌单导出",
                "备份歌单",
                "导出播放列表",
                "playlist export",
                "export playlist",
                "daochugedan",
                "dcgd",
                "beifengedan",
                "bfgd"
            ),
            targetId = "manual:playlist_export",
            order = SettingsPage.Backup.ordinal * 100 + 10
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.playlist_import,
            descriptionRes = R.string.playlist_import_desc,
            id = "playlist_import",
            aliases = listOf(
                "导入歌单",
                "歌单导入",
                "恢复歌单",
                "导入播放列表",
                "playlist import",
                "import playlist",
                "daorugedan",
                "drgd",
                "huifugedan",
                "hfgd"
            ),
            targetId = "manual:playlist_import",
            order = SettingsPage.Backup.ordinal * 100 + 12
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.settings_export_config,
            descriptionRes = R.string.settings_export_config_desc,
            id = "config_export",
            aliases = listOf(
                "导出配置",
                "配置导出",
                "备份配置",
                "导出设置",
                "config export",
                "export config",
                "settings backup",
                "daochupeizhi",
                "dcpz",
                "beifenpeizhi",
                "bfpz"
            ),
            targetId = "manual:config_export",
            order = SettingsPage.Backup.ordinal * 100 + 14
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.settings_import_config,
            descriptionRes = R.string.settings_import_config_desc,
            id = "config_import",
            aliases = listOf(
                "导入配置",
                "配置导入",
                "恢复配置",
                "导入设置",
                "config import",
                "import config",
                "settings restore",
                "daorupeizhi",
                "drpz",
                "huifupeizhi",
                "hfpz"
            ),
            targetId = "manual:config_import",
            order = SettingsPage.Backup.ordinal * 100 + 16
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.github_auto_sync,
            descriptionRes = R.string.sync_config_desc,
            id = "github_sync",
            aliases = listOf("github", "git", "token", "sync", "tongbu", "beifen"),
            targetId = "manual:github_sync",
            order = SettingsPage.Backup.ordinal * 100 + 70
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.webdav_sync_title,
            descriptionRes = R.string.webdav_sync_desc,
            id = "webdav_sync",
            aliases = listOf("webdav", "dav", "sync", "tongbu", "beifen", "server", "url"),
            targetId = "manual:webdav_sync",
            order = SettingsPage.Backup.ordinal * 100 + 72
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.sync_auto,
            descriptionRes = R.string.sync_auto_desc,
            id = "github_auto_sync",
            aliases = listOf(
                "github",
                "git",
                "token",
                "sync",
                "automatic sync",
                "auto sync",
                "zidongtongbu",
                "tongbu"
            ),
            targetId = "manual:github_auto_sync",
            order = SettingsPage.Backup.ordinal * 100 + 74
        ),
        entry(
            page = SettingsPage.Backup,
            titleRes = R.string.webdav_sync_title,
            descriptionRes = R.string.webdav_auto_sync_desc,
            id = "webdav_auto_sync",
            aliases = listOf("webdav", "dav", "sync", "auto sync", "zidongtongbu", "tongbu"),
            targetId = "manual:webdav_auto_sync",
            order = SettingsPage.Backup.ordinal * 100 + 76
        ),
        entry(
            page = SettingsPage.Storage,
            titleRes = R.string.settings_clear_cache,
            descriptionRes = R.string.settings_clear_cache_desc,
            id = "clear_cache",
            aliases = listOf("cache", "clean", "qingli", "huancun", "storage", "space"),
            targetId = "manual:clear_cache"
        ),
        entry(
            page = SettingsPage.About,
            titleRes = R.string.settings_about,
            descriptionRes = R.string.settings_about_desc,
            id = "about_debug",
            aliases = listOf("about", "version", "debug", "banben", "tiaoshi")
        )
    )
}

private fun Context.safeString(resId: Int): String {
    if (resId == 0) return ""
    return runCatching { getString(resId) }.getOrDefault("")
}

private val PageSearchAliases = mapOf(
    SettingsPage.General to listOf("general", "basic", "tongyong", "jichu"),
    SettingsPage.Theme to listOf("theme", "dark", "light", "color", "palette", "zhuti", "yanse"),
    SettingsPage.Accounts to listOf(
        "account",
        "login",
        "cookie",
        "netease",
        "bili",
        "youtube",
        "zhanghao",
        "denglu",
        "pingtai",
        "sanfang",
        "third party",
        "platform"
    ),
    SettingsPage.Personalization to listOf("display", "home", "font", "dpi", "background", "tab", "xianshi"),
    SettingsPage.Motion to listOf("motion", "animation", "glass", "blur", "dynamic", "dongxiao", "mohu"),
    SettingsPage.Lyrics to listOf("lyrics", "lrc", "amll", "lyricon", "floating", "bluetooth", "geci"),
    SettingsPage.Network to listOf("network", "proxy", "bypass", "daili", "wangluo"),
    SettingsPage.Playback to listOf("playback", "audio", "queue", "volume", "fade", "crossfade", "bofang"),
    SettingsPage.UsbExclusive to listOf("usb", "dac", "pcm", "uac", "exclusive", "bit perfect", "dizhan"),
    SettingsPage.PlaybackSource to listOf("source", "fallback", "bili", "netease", "yinyuan", "huanyuan"),
    SettingsPage.Storage to listOf(
        "storage",
        "cache",
        "clean",
        "space",
        "folder",
        "filename",
        "cunchu",
        "huancun"
    ),
    SettingsPage.StorageCacheDetails to listOf(
        "storage",
        "cache",
        "details",
        "space",
        "huancun",
        "xiangqing"
    ),
    SettingsPage.TrafficManagement to listOf("traffic", "mobile", "roaming", "data", "liuliang"),
    SettingsPage.Downloads to listOf("download", "downloads", "threads", "parallel", "concurrency", "xiazai"),
    SettingsPage.Backup to listOf("backup", "sync", "github", "webdav", "import", "export", "beifen"),
    SettingsPage.About to listOf("about", "version", "debug", "guanyu")
)

private val SettingSearchAliases = mapOf(
    "haptic_feedback_enabled" to listOf("vibration", "touch", "zhendong", "chugan"),
    "playback_service_idle_shutdown_minutes" to listOf("notification", "foreground", "idle", "tuichu"),
    "always_record_logs_enabled" to listOf("log", "crash", "diagnostic", "rizhi"),
    "audio_quality" to listOf("netease", "wy", "wyy", "lossless", "hires", "wusun"),
    "bili_audio_quality" to listOf("哔哩哔哩", "bzhan", "dolby", "hires"),
    "mobile_data_follow_default_audio_quality" to listOf("cellular", "4g", "5g", "liuliang"),
    "mobile_data_netease_audio_quality" to listOf("cellular netease", "wy liuliang"),
    "mobile_data_bili_audio_quality" to listOf("cellular bili", "哔哩哔哩流量", "bzhan liuliang"),
    "default_start_destination" to listOf("start page", "home", "tab", "qidongye"),
    "auto_show_keyboard" to listOf("keyboard", "input", "shurufa", "jianpan"),
    "nowplaying_song_title_marquee_enabled" to listOf(
        "marquee",
        "scrolling title",
        "long song title",
        "song name",
        "chang geming"
    ),
    "nowplaying_keep_screen_on" to listOf("screen on", "wakelock", "changliang"),
    "nowplaying_progress_show_audio_codec" to listOf("codec", "aac", "flac", "bianma"),
    "nowplaying_progress_show_audio_spec" to listOf("sample rate", "bit depth", "guige"),
    "lyric_font_scale" to listOf("font", "size", "geci daxiao", "ziti"),
    "lyrics_page_lyric_font_scale" to listOf("lyrics page font", "full screen lyric", "geciye"),
    "lyrics_page_translation_font_scale" to listOf("lyrics page translation", "translation", "fanyi"),
    "ui_density_scale" to listOf("dpi", "scale", "zoom", "suofang"),
    "background_image_uri" to listOf("wallpaper", "custom background", "beijingtu"),
    "background_image_blur" to listOf("wallpaper blur", "mohu", "gaosi"),
    "background_image_alpha" to listOf("opacity", "transparent", "touming"),
    "show_lyric_translation" to listOf("translation", "fanyi", "yiyu"),
    "lyric_translation_use_phonetic" to listOf("phonetic", "romaji", "kana", "zhuyin"),
    "advanced_lyrics_enabled" to listOf("lyrics animation", "amll", "dongci"),
    "coherent_feedback_enabled" to listOf("transition", "handoff", "drawer", "fankui"),
    "advanced_blur_enabled" to listOf("glass", "haze", "blur", "gaoji mohu"),
    "enhanced_advanced_blur_enabled" to listOf("glass", "background sampling", "jinjie mohu"),
    "enhanced_advanced_blur_radius_dp" to listOf("radius", "mohuqiangdu"),
    "advanced_blur_quality" to listOf("blur quality", "sampling", "mohu zhiliang"),
    "nowplaying_audio_reactive_enabled" to listOf("audio reactive", "visualizer", "yinpin fanying"),
    "nowplaying_dynamic_background_enabled" to listOf("dynamic background", "album color", "dongtai beijing"),
    "nowplaying_cover_blur_background_enabled" to listOf("cover blur", "fengmian mohu"),
    "nowplaying_cover_blur_amount" to listOf("cover blur radius", "mohu qiangdu"),
    "nowplaying_cover_blur_darken" to listOf("darken", "dim", "bianan"),
    "lyric_blur_enabled" to listOf("lyrics blur", "geci mohu", "gaosi"),
    "lyric_blur_amount" to listOf("lyrics blur amount", "geci qiangdu"),
    "lyricon_enabled" to listOf("lyricon", "external lyrics", "cimu"),
    "amll_lyrics_enabled" to listOf("amll", "ttml", "word timed"),
    "online_lyrics_enabled" to listOf(
        "online lyrics",
        "lrclib",
        "zaixian geci",
        "geci pipei",
        "在线歌词",
        "歌词解析"
    ),
    "status_bar_lyrics_enabled" to listOf("status bar", "zhuangtailan", "lyric"),
    "floating_lyrics_enabled" to listOf("floating", "desktop lyrics", "xuanfu"),
    "external_bluetooth_lyrics_enabled" to listOf("bluetooth", "car", "lyric", "lanyageci", "bt"),
    "dynamic_island_lyrics_enabled" to listOf(
        "灵动岛",
        "dynamic island",
        "live activity",
        "always send",
        "bluetooth lyrics"
    ),
    "cloud_music_lyric_default_offset_ms" to listOf("netease lyrics offset", "wy geci pianyi"),
    "qq_music_lyric_default_offset_ms" to listOf("qq lyrics offset", "qq geci pianyi"),
    "bypass_proxy" to listOf("proxy", "vpn", "direct", "daili"),
    "download_directory_uri" to listOf("folder", "path", "saf", "xiazai mulu"),
    "download_file_name_template" to listOf("filename", "template", "mingming"),
    "download_metadata_post_processing_enabled" to listOf("taglib", "metadata", "lyrics embed"),
    "standardized_lyric_embedding_enabled" to listOf("lyrics embed", "lrc", "tag"),
    "download_parallelism" to listOf("threads", "parallel", "concurrency", "bingfa"),
    "mobile_data_high_risk_prompt_enabled" to listOf("traffic warning", "roaming", "liuliang tishi"),
    "max_cache_size_bytes" to listOf("cache limit", "space", "huancun daxiao"),
    "silent_github_sync_failure" to listOf(
        "github",
        "sync notification",
        "sync failure",
        "failure notification",
        "zidongtongbu",
        "tongbushibai",
        "butishi",
        "jingmo"
    ),
    "playback_fade_in" to listOf("fade", "fade in", "fade out", "danru", "danchu", "drdc"),
    "playback_crossfade_next" to listOf("crossfade", "next fade", "qiege", "xiayishou", "drdc"),
    "playback_sleep_timer_finish_current_on_expiry" to listOf("sleep timer", "timer", "dingshi"),
    "playback_fade_in_duration_ms" to listOf("fade duration", "danru shichang"),
    "playback_fade_out_duration_ms" to listOf("fade duration", "danchu shichang"),
    "playback_crossfade_in_duration_ms" to listOf("crossfade duration", "xiayishou danru"),
    "playback_crossfade_out_duration_ms" to listOf("crossfade duration", "dangqianqu danchu"),
    "playback_volume_normalization_enabled" to listOf("normalization", "loudness", "yinliang yizhi"),
    "playback_high_resolution_output_enabled" to listOf("32 bit", "float", "hires", "gao jiexi"),
    "playback_volume_balance" to listOf("balance", "left right", "shengdao"),
    "keep_last_playback_progress" to listOf("resume", "progress", "jindu"),
    "remember_long_form_playback_progress" to listOf("podcast", "audiobook", "long form", "changyinpin"),
    "keep_playback_mode_state" to listOf("repeat", "shuffle", "mode", "suiji", "xunhuan"),
    "stop_on_bluetooth_disconnect" to listOf("bluetooth", "headset", "car", "lanyadukai"),
    "usb_exclusive_playback" to listOf("usb", "dac", "exclusive", "pcm", "bit perfect", "uac"),
    "allow_mixed_playback" to listOf("mix", "focus", "background audio", "hunbo"),
    "preempt_audio_focus" to listOf("audio focus", "qiangzhan", "ducking")
)
