package com.lyra.player.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lyra.player.R
import com.lyra.player.core.source.MusicSourceIds

/**
 * 首页分区的稳定标识。
 *
 * 顺序：继续播放 / 热门 / 私人雷达 / 推荐（除继续播放外都可以换来源）。
 */
enum class HomeSectionId(val titleResId: Int) {
    CONTINUE(R.string.player_continue),
    TRENDING(R.string.settings_home_card_netease_trending),
    RADAR(R.string.settings_home_card_netease_radar),
    RECOMMENDED(R.string.settings_home_card_netease_recommended)
}

/**
 * 首页排版配置。
 *
 * 各在线分区的来源用**音源 id 字符串**表示（见 [MusicSourceIds]），不再硬编码平台枚举。
 * 具体有哪些来源可选由 [com.lyra.player.ui.viewmodel.tab.home.HomeSourceCatalog] 动态给出。
 */
data class HomeLayoutConfig(
    val order: List<HomeSectionId> = DEFAULT_HOME_ORDER,
    val sources: Map<HomeSectionId, String> = defaultHomeSources(),
    val qishuiMode: String = "default"
) {
    companion object {
        val DEFAULT_HOME_ORDER: List<HomeSectionId> = listOf(
            HomeSectionId.CONTINUE,
            HomeSectionId.RADAR,
            HomeSectionId.TRENDING,
            HomeSectionId.RECOMMENDED
        )

        fun defaultHomeSources(): Map<HomeSectionId, String> = mapOf(
            HomeSectionId.TRENDING to MusicSourceIds.NETEASE,
            HomeSectionId.RADAR to MusicSourceIds.NETEASE,
            HomeSectionId.RECOMMENDED to MusicSourceIds.NETEASE
        )
    }
}

/** 首页排版（分区顺序 + 各在线分区来源）的本地持久化。 */
class HomeLayoutRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<HomeLayoutConfig> = _config.asStateFlow()

    fun moveSection(section: HomeSectionId, direction: Int) {
        val order = _config.value.order.toMutableList()
        val index = order.indexOf(section)
        if (index < 0) return
        val target = index + direction
        if (target !in order.indices) return
        order[index] = order[target]
        order[target] = section
        setOrder(order)
    }

    fun setOrder(order: List<HomeSectionId>) {
        val present = order.distinct()
        val normalized = present + HomeLayoutConfig.DEFAULT_HOME_ORDER.filter { it !in present }
        prefs.edit { putString(KEY_ORDER, encodeOrder(normalized)) }
        _config.value = _config.value.copy(order = normalized)
    }

    fun setSource(section: HomeSectionId, sourceId: String) {
        val sources = _config.value.sources.toMutableMap()
        sources[section] = sourceId
        prefs.edit { putString(KEY_SOURCES, encodeSources(sources)) }
        _config.value = _config.value.copy(sources = sources)
    }

    fun setQishuiMode(mode: String) {
        val normalized = mode.takeIf(String::isNotBlank) ?: "default"
        prefs.edit { putString(KEY_QISHUI_MODE, normalized) }
        _config.value = _config.value.copy(qishuiMode = normalized)
    }

    private fun readConfig(): HomeLayoutConfig {
        val order = decodeOrder(prefs.getString(KEY_ORDER, null))
        val sources = decodeSources(prefs.getString(KEY_SOURCES, null))
        val qishuiMode = prefs.getString(KEY_QISHUI_MODE, null)
            ?.takeIf(String::isNotBlank)
            ?: "default"
        return HomeLayoutConfig(order = order, sources = sources, qishuiMode = qishuiMode)
    }

    private fun encodeOrder(order: List<HomeSectionId>): String =
        order.joinToString(",") { it.name }

    private fun decodeOrder(raw: String?): List<HomeSectionId> {
        val parsed = raw
            ?.split(',')
            ?.mapNotNull { name -> HomeSectionId.entries.firstOrNull { it.name == name } }
            .orEmpty()
        return parsed.distinct() + HomeLayoutConfig.DEFAULT_HOME_ORDER.filter { it !in parsed }
    }

    private fun encodeSources(sources: Map<HomeSectionId, String>): String =
        sources.entries.joinToString(",") { "${it.key.name}=${it.value}" }

    private fun decodeSources(raw: String?): Map<HomeSectionId, String> {
        val parsed = raw
            ?.split(',')
            ?.mapNotNull { entry ->
                val parts = entry.split('=')
                if (parts.size != 2) return@mapNotNull null
                val section = HomeSectionId.entries.firstOrNull { it.name == parts[0] }
                    ?: return@mapNotNull null
                val sourceId = migrateLegacySourceId(parts[1]) ?: return@mapNotNull null
                section to sourceId
            }
            .orEmpty()
            .toMap()
        return HomeLayoutConfig.defaultHomeSources() + parsed
    }

    /**
     * 兼容旧版本以枚举名存储的来源（NETEASE / TENCENT），迁移为音源 id 字符串。
     */
    private fun migrateLegacySourceId(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        return when (value) {
            "NETEASE" -> MusicSourceIds.NETEASE
            "TENCENT" -> MusicSourceIds.TENCENT
            else -> value
        }
    }

    private companion object {
        const val PREFS_NAME = "home_layout_preferences"
        const val KEY_ORDER = "section_order"
        const val KEY_SOURCES = "section_sources"
        const val KEY_QISHUI_MODE = "qishui_mode"
    }
}
