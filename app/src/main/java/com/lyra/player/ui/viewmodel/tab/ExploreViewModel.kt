package com.lyra.player.ui.viewmodel.tab

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.ui.viewmodel.tab/ExploreViewModel
 * Created: 2025/8/11
 */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.core.api.bili.BiliClient
import com.lyra.player.core.api.bili.buildBiliPartSong
import com.lyra.player.core.api.bili.buildBiliSongAlbum
import com.lyra.player.core.api.search.CustomChannelSearchApi
import com.lyra.player.core.api.search.CustomChannelArtist
import com.lyra.player.core.api.search.CustomChannelPlaylist
import com.lyra.player.core.api.search.CustomChannelSong
import com.lyra.player.core.customsource.CustomSourceManager
import com.lyra.player.core.customsource.KUGOU_SOURCE_TAG
import com.lyra.player.core.customsource.KUWO_SOURCE_TAG
import com.lyra.player.core.customsource.MIGU_SOURCE_TAG
import com.lyra.player.core.customsource.TENCENT_SOURCE_TAG
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.core.source.SourceManager
import com.lyra.player.core.source.SourceManagerState
import com.lyra.player.core.source.SourceSearchType
import com.lyra.player.ui.viewmodel.tab.search.BilibiliSearchProvider
import com.lyra.player.ui.viewmodel.tab.search.CustomChannelSearchProvider
import com.lyra.player.ui.viewmodel.tab.search.NeteaseSearchProvider
import com.lyra.player.ui.viewmodel.tab.search.SourceSearchPage
import com.lyra.player.ui.viewmodel.tab.search.toNeteaseType
import com.lyra.player.ui.viewmodel.tab.search.SourceSearchProvider
import com.lyra.player.ui.viewmodel.tab.search.SourceSearchRegistry
import com.lyra.player.ui.viewmodel.tab.search.SourceSearchRequest
import com.lyra.player.core.api.youtube.YouTubeMusicCreatorSummary
import com.lyra.player.core.api.youtube.YouTubeMusicSearchFilter
import com.lyra.player.core.api.youtube.YouTubeMusicSearchResult
import com.lyra.player.core.api.youtube.YouTubeMusicSearchResultType
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.player.PlayerManager.biliClient
import com.lyra.player.core.player.PlayerManager.neteaseClient
import com.lyra.player.data.auth.common.SavedCookieAuthState
import com.lyra.player.data.model.NeteaseArtistSummary
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.platform.youtube.YouTubeFeatureGate
import com.lyra.player.data.platform.youtube.buildYouTubeMusicMediaUri
import com.lyra.player.data.platform.youtube.stableYouTubeMusicId
import com.lyra.player.data.platform.youtube.youtubeMusicThumbnailUrl
import com.lyra.player.util.search.SearchTextMatcher
import com.lyra.player.util.search.searchValues
import org.json.JSONObject

private const val TAG = "LYRA-ExploreVM"
private const val BILI_RESOURCE_TYPE_COLLECTION = 21

/**
 * Tag key to Chinese API category mapping
 */
val TAG_TO_API_CATEGORY = mapOf(
    "tag_all" to "全部",
    "tag_pop" to "流行",
    "tag_soundtrack" to "影视原声",
    "tag_chinese" to "华语",
    "tag_nostalgia" to "怀旧",
    "tag_rock" to "摇滚",
    "tag_acg" to "ACG",
    "tag_western" to "欧美",
    "tag_fresh" to "清新",
    "tag_night" to "夜晚",
    "tag_children" to "儿童",
    "tag_folk" to "民谣",
    "tag_japanese" to "日语",
    "tag_romantic" to "浪漫",
    "tag_study" to "学习",
    "tag_korean" to "韩语",
    "tag_work" to "工作",
    "tag_electronic" to "电子",
    "tag_cantonese" to "粤语",
    "tag_dance" to "舞曲",
    "tag_sad" to "伤感",
    "tag_game" to "游戏",
    "tag_afternoon_tea" to "下午茶",
    "tag_healing" to "治愈",
    "tag_rap" to "说唱",
    "tag_light_music" to "轻音乐"
)

/** 定义搜索源。 */
enum class SearchSource {
    /** 聚合入口：不是真实平台，对全部可用音源并发搜索。 */
    ALL,
    YOUTUBE_MUSIC,
    NETEASE,
    BILIBILI,
    KUWO,
    KUGOU,
    MIGU,
    TENCENT,
    LINK_RECOGNITION
}

enum class NeteaseExploreSearchType(val apiType: Int) {
    SONG(apiType = 1),
    PLAYLIST(apiType = 1000),
    ARTIST(apiType = 100)
}

/** [SearchSource] → 统一音源 id（见 [MusicSourceIds]）；聚合与链接识别不是音源，返回 null。 */
internal val SearchSource.musicSourceId: String?
    get() = when (this) {
        SearchSource.NETEASE -> MusicSourceIds.NETEASE
        SearchSource.BILIBILI -> MusicSourceIds.BILIBILI
        SearchSource.YOUTUBE_MUSIC -> MusicSourceIds.YOUTUBE_MUSIC
        SearchSource.KUWO -> MusicSourceIds.KUWO
        SearchSource.KUGOU -> MusicSourceIds.KUGOU
        SearchSource.MIGU -> MusicSourceIds.MIGU
        SearchSource.TENCENT -> MusicSourceIds.TENCENT
        SearchSource.ALL,
        SearchSource.LINK_RECOGNITION -> null
    }

/** 统一音源 id → [SearchSource]（反向映射，供动态 Tab 生成）。 */
internal fun searchSourceForMusicSourceId(sourceId: String): SearchSource? = when (sourceId) {
    MusicSourceIds.NETEASE -> SearchSource.NETEASE
    MusicSourceIds.BILIBILI -> SearchSource.BILIBILI
    MusicSourceIds.YOUTUBE_MUSIC -> SearchSource.YOUTUBE_MUSIC
    MusicSourceIds.KUWO -> SearchSource.KUWO
    MusicSourceIds.KUGOU -> SearchSource.KUGOU
    MusicSourceIds.MIGU -> SearchSource.MIGU
    MusicSourceIds.TENCENT -> SearchSource.TENCENT
    else -> null
}

/** 网易云搜索类型 → 统一搜索类型。 */
internal fun NeteaseExploreSearchType.toSourceSearchType(): SourceSearchType = when (this) {
    NeteaseExploreSearchType.SONG -> SourceSearchType.SONG
    NeteaseExploreSearchType.PLAYLIST -> SourceSearchType.PLAYLIST
    NeteaseExploreSearchType.ARTIST -> SourceSearchType.ARTIST
}

/** 渠道搜索类型 → 统一搜索类型。 */
internal fun ChannelSearchType.toSourceSearchType(): SourceSearchType = when (this) {
    ChannelSearchType.SONG -> SourceSearchType.SONG
    ChannelSearchType.ARTIST -> SourceSearchType.ARTIST
    ChannelSearchType.PLAYLIST -> SourceSearchType.PLAYLIST
}

/** 统一搜索类型 → 渠道搜索类型（渠道不支持的返回 null）。 */
internal fun SourceSearchType.toChannelSearchType(): ChannelSearchType? = when (this) {
    SourceSearchType.SONG -> ChannelSearchType.SONG
    SourceSearchType.ARTIST -> ChannelSearchType.ARTIST
    SourceSearchType.PLAYLIST -> ChannelSearchType.PLAYLIST
    SourceSearchType.ALBUM,
    SourceSearchType.RANKING -> null
}

/** 搜索类型子标签展示顺序。 */
internal val EXPLORE_SEARCH_TYPE_ORDER: List<SourceSearchType> = listOf(
    SourceSearchType.SONG,
    SourceSearchType.ARTIST,
    SourceSearchType.ALBUM,
    SourceSearchType.PLAYLIST,
    SourceSearchType.RANKING
)

/** 某个音源 / 聚合入口当前选中的搜索类型（纯状态函数，UI 可直接消费）。 */
internal fun ExploreUiState.selectedSearchTypeOf(source: SearchSource): SourceSearchType = when (source) {
    SearchSource.ALL -> selectedAllSearchType
    SearchSource.NETEASE -> selectedNeteaseSearchType.toSourceSearchType()
    SearchSource.KUWO,
    SearchSource.KUGOU,
    SearchSource.MIGU,
    SearchSource.TENCENT -> selectedChannelSearchType.toSourceSearchType()
    // YouTube 入口已移除；这里仅为保持 when 完备，实际不可达。
    SearchSource.YOUTUBE_MUSIC -> SourceSearchType.SONG
    SearchSource.BILIBILI,
    SearchSource.LINK_RECOGNITION -> SourceSearchType.SONG
}

/**
 * 某个音源当前可用的搜索类型，由已注册 Provider 的能力决定。
 *
 * UI 用它生成搜索类型子标签，从而避免「标签显示了但平台不支持」。
 * 「全部」聚合入口取其可用音源支持类型的并集。
 */
internal fun ExploreUiState.searchTypesFor(source: SearchSource): List<SourceSearchType> {
    if (source == SearchSource.LINK_RECOGNITION) return emptyList()
    return EXPLORE_SEARCH_TYPE_ORDER.filter { type ->
        when (source) {
            SearchSource.ALL -> availableSearchSources.any { candidate ->
                candidate.musicSourceId
                    ?.let(SourceSearchRegistry::providerFor)
                    ?.supports(type) == true
            }
            else -> source.musicSourceId
                ?.let(SourceSearchRegistry::providerFor)
                ?.supports(type) == true
        }
    }
}

enum class YouTubeExploreSearchType(
    val filter: YouTubeMusicSearchFilter?
) {
    SONG(YouTubeMusicSearchFilter.Song),
    VIDEO(YouTubeMusicSearchFilter.Video),
    CREATOR(null)
}

data class NeteaseSearchArtistResult(
    val artist: NeteaseArtistSummary,
    val picUrl: String?,
    val musicSize: Int,
    val albumSize: Int
)

sealed class ExploreSearchResult {
    abstract val stableKey: String

    data class Song(val song: SongItem) : ExploreSearchResult() {
        override val stableKey: String = listOfNotNull(
            "song",
            song.channelId,
            song.audioId,
            song.subAudioId,
            song.mediaUri,
            song.id.toString()
        ).joinToString("|")
    }

    data class Playlist(val playlist: PlaylistSummary) : ExploreSearchResult() {
        override val stableKey: String = "netease|playlist|${playlist.id}"
    }

    data class YouTubePlaylist(val playlist: YouTubeMusicPlaylist) : ExploreSearchResult() {
        override val stableKey: String = "youtubeMusic|playlist|${playlist.playlistId.ifBlank { playlist.browseId }}"
    }

    data class BilibiliPlaylist(val playlist: BiliPlaylist) : ExploreSearchResult() {
        override val stableKey: String =
            "bilibili|playlist|${playlist.kind}|${playlist.mediaId}|${playlist.mid}"
    }

    data class Artist(val result: NeteaseSearchArtistResult) : ExploreSearchResult() {
        override val stableKey: String = "netease|artist|${result.artist.id}"
    }

    data class YouTubeCreator(val creator: YouTubeMusicCreatorSummary) : ExploreSearchResult() {
        override val stableKey: String = "youtubeMusic|creator|${creator.browseId}"
    }

    data class Notice(
        val title: String,
        val message: String
    ) : ExploreSearchResult() {
        override val stableKey: String = "notice|$title|$message"
    }

    /** 渠道（QQ / 酷狗 / 咪咕）歌手结果 */
    data class ChannelArtist(val artist: CustomChannelArtist) : ExploreSearchResult() {
        override val stableKey: String = "channel|artist|${artist.name}"
    }

    /** 渠道（QQ / 酷狗 / 咪咕）歌单结果 */
    data class ChannelPlaylist(val playlist: CustomChannelPlaylist) : ExploreSearchResult() {
        override val stableKey: String = "channel|playlist|${playlist.id}"
    }
}

/** 渠道搜索分类：歌曲 / 歌手 / 歌单 */
enum class ChannelSearchType {
    SONG,
    ARTIST,
    PLAYLIST
}

data class ExploreUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val playlists: List<PlaylistSummary> = emptyList(),
    val selectedTag: String = "tag_all",  // String resource key
    val searching: Boolean = false,
    val searchError: String? = null,
    val searchResults: List<SongItem> = emptyList(),
    val searchItems: List<ExploreSearchResult> = emptyList(),
    val searchHasMore: Boolean = false,
    val searchLoadingMore: Boolean = false,
    val searchLoadMoreError: String? = null,
    val searchPage: Int = 0,
    val searchKeyword: String = "",
    val searchDisplayQuery: String = "",
    /** ALL 聚合搜索：所有目标音源全部失败（用于区分 “聚合整体失败” 与 “正常但无结果”）。 */
    val allSourcesFailed: Boolean = false,
    val selectedSearchSource: SearchSource = SearchSource.ALL,
    val selectedNeteaseSearchType: NeteaseExploreSearchType = NeteaseExploreSearchType.SONG,
    val selectedChannelSearchType: ChannelSearchType = ChannelSearchType.SONG,
    val selectedYouTubeMusicSearchType: YouTubeExploreSearchType = YouTubeExploreSearchType.SONG,
    /** 「全部」聚合入口的当前搜索类型。 */
    val selectedAllSearchType: SourceSearchType = SourceSearchType.SONG,
    /** 当前可用（已启用 / 就绪 / 可搜索）的音源，动态生成，不含「全部」与「链接」。 */
    val availableSearchSources: List<SearchSource> = emptyList(),
    val isNeteaseLoggedIn: Boolean = false,
    val ytMusicPlaylists: List<YouTubeMusicPlaylist> = emptyList(),
    val ytMusicPlaylistsLoading: Boolean = false,
    val ytMusicPlaylistsError: String? = null
)

internal fun isNeteaseExploreSearchAvailable(authState: SavedCookieAuthState): Boolean {
    return authState != SavedCookieAuthState.Missing
}

/** ALL 聚合：目标非空且无任何成功（但确有失败）时，视为「聚合整体失败」。 */
internal fun resolveAggregateAllSourcesFailed(
    targetCount: Int,
    failureCount: Int,
    successCount: Int
): Boolean = targetCount > 0 && successCount == 0 && failureCount > 0

internal fun ExploreUiState.withNeteaseAuthRequired(error: String): ExploreUiState = copy(
    searching = false,
    searchError = error,
    searchResults = emptyList(),
    searchItems = emptyList(),
    searchHasMore = false,
    searchLoadingMore = false,
    searchLoadMoreError = null,
    searchPage = 0
)

internal fun ExploreUiState.withYouTubeDisabled(): ExploreUiState {
    val youtubeWasSelected = selectedSearchSource == SearchSource.YOUTUBE_MUSIC
    return copy(
        selectedSearchSource = if (youtubeWasSelected) SearchSource.NETEASE else selectedSearchSource,
        searching = if (youtubeWasSelected) false else searching,
        searchResults = if (youtubeWasSelected) emptyList() else searchResults,
        searchItems = if (youtubeWasSelected) emptyList() else searchItems,
        searchHasMore = if (youtubeWasSelected) false else searchHasMore,
        searchLoadingMore = if (youtubeWasSelected) false else searchLoadingMore,
        searchLoadMoreError = if (youtubeWasSelected) null else searchLoadMoreError,
        searchPage = if (youtubeWasSelected) 0 else searchPage,
        searchKeyword = if (youtubeWasSelected) "" else searchKeyword,
        searchDisplayQuery = if (youtubeWasSelected) "" else searchDisplayQuery,
        searchError = if (youtubeWasSelected) null else searchError,
        ytMusicPlaylists = emptyList(),
        ytMusicPlaylistsLoading = false,
        ytMusicPlaylistsError = null
    )
}

internal fun rankExploreSongSearchResults(
    query: String,
    songs: List<SongItem>
): List<SongItem> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank() || songs.size <= 1) return songs

    val scoredSongs = songs.mapIndexed { index, song ->
        RankedSongSearchResult(
            song = song,
            index = index,
            score = SearchTextMatcher.score(normalizedQuery, song.searchTokens())
        )
    }
    val matchedSongs = scoredSongs.filter { it.score != null }
    if (matchedSongs.isEmpty()) return songs

    return matchedSongs
        .sortedWith(
            compareBy<RankedSongSearchResult> { it.score ?: Int.MAX_VALUE }
                .thenBy { it.index }
        )
        .map { it.song } +
        scoredSongs
            .filter { it.score == null }
            .map { it.song }
}

private data class RankedSongSearchResult(
    val song: SongItem,
    val index: Int,
    val score: Int?
)

private fun SongItem.searchTokens(): List<Any?> {
    return searchValues()
}

internal fun mergeExploreSearchResults(
    existing: List<ExploreSearchResult>,
    incoming: List<ExploreSearchResult>
): List<ExploreSearchResult> {
    if (existing.isEmpty()) return incoming.distinctBy { it.stableKey }
    if (incoming.isEmpty()) return existing

    val seen = existing.mapTo(mutableSetOf()) { it.stableKey }
    val merged = ArrayList<ExploreSearchResult>(existing.size + incoming.size)
    merged += existing
    incoming.forEach { item ->
        if (seen.add(item.stableKey)) {
            merged += item
        }
    }
    return merged
}

internal fun searchSongItems(items: List<ExploreSearchResult>): List<SongItem> {
    return items.mapNotNull { (it as? ExploreSearchResult.Song)?.song }
}

internal fun hasMoreExploreSearchResults(
    totalCount: Int?,
    loadedCount: Int,
    pageItemCount: Int,
    pageSize: Int
): Boolean {
    return if (totalCount != null) {
        loadedCount < totalCount
    } else {
        pageItemCount >= pageSize
    }
}

internal fun shouldLoadExploreSearchMore(
    resultCount: Int,
    lastVisibleItemIndex: Int?,
    hasMore: Boolean,
    searching: Boolean,
    loadingMore: Boolean,
    loadMoreFailed: Boolean = false,
    prefetchDistance: Int = 6
): Boolean {
    if (!hasMore || searching || loadingMore || loadMoreFailed || resultCount <= 0) return false
    val lastVisible = lastVisibleItemIndex ?: return false
    return lastVisible >= resultCount - prefetchDistance
}

private data class ExploreSearchFetchResult(
    val items: List<ExploreSearchResult>,
    val page: Int,
    val hasMore: Boolean
) {
    val songs: List<SongItem> = searchSongItems(items)
}

private fun SourceSearchPage.toFetchResult(): ExploreSearchFetchResult =
    ExploreSearchFetchResult(items = items, page = page, hasMore = hasMore)

class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val neteaseRepo = AppContainer.neteaseCookieRepo
    private var highQualityLoadJob: Job? = null
    private var searchJob: Job? = null
    private var searchMoreJob: Job? = null
    private var ytMusicPlaylistsJob: Job? = null
    private var ytMusicPlaylistsPending = false
    private var searchRequestVersion = 0L
    private var youtubeEnabled = YouTubeFeatureGate.isEnabled()

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState

    init {
        // 3B：注册已迁移的搜索 Provider（幂等）。新增平台在此注册即可。
        SourceSearchRegistry.register(NeteaseSearchProvider)
        SourceSearchRegistry.register(BilibiliSearchProvider)
        CustomChannelSearchProvider.all.forEach(SourceSearchRegistry::register)
        viewModelScope.launch {
            neteaseRepo.authHealthFlow.collect { health ->
                val isLoggedIn = isNeteaseExploreSearchAvailable(health.state)
                val currentState = _uiState.value
                if (
                    !isLoggedIn &&
                    currentState.selectedSearchSource == SearchSource.NETEASE &&
                    currentState.searchKeyword.isNotBlank()
                ) {
                    clearNeteaseSearchForAuthRequired()
                }
                _uiState.value = _uiState.value.copy(isNeteaseLoggedIn = isLoggedIn)
            }
        }
        viewModelScope.launch {
            neteaseRepo.cookieFlow.collect {
                LyraLogger.d(TAG, "cookieFlow updated, reload high quality playlists tag=${_uiState.value.selectedTag}")
                loadHighQuality()
            }
        }
        viewModelScope.launch {
            AppContainer.settingsRepo.youtubeEnabledFlow.collect { enabled ->
                youtubeEnabled = enabled
                if (!enabled) {
                    disableYouTubeSource()
                }
                refreshAvailableSources()
            }
        }
        // 动态音源列表：随已启用脚本声明的音源变化刷新（酷我 / 酷狗 / 咪咕 / QQ…）
        viewModelScope.launch {
            CustomSourceManager.declaredSourceIds.collect { refreshAvailableSources() }
        }
        viewModelScope.launch {
            CustomSourceManager.ensureLoaded(app)
            refreshAvailableSources()
        }
    }

    /**
     * 从统一的 [SourceManager] 重新计算「当前可用的搜索音源」，并落进 [ExploreUiState]。
     *
     * UI 只消费这个列表，不再维护任何平台名单；新增音源只需注册 Source + Provider。
     * YouTube 入口已移除，这里始终排除。
     */
    private fun refreshAvailableSources() {
        val sources = SourceManager.searchSources(
            SourceManagerState(
                youtubeEnabled = youtubeEnabled,
                declaredCustomSourceIds = CustomSourceManager.declaredSourceIds.value
            )
        )
            .mapNotNull { searchSourceForMusicSourceId(it.id) }
            .filterNot { it == SearchSource.YOUTUBE_MUSIC }
        val current = _uiState.value
        if (current.availableSearchSources == sources) return
        val selected = current.selectedSearchSource
        val stillAvailable = selected == SearchSource.ALL ||
            selected == SearchSource.LINK_RECOGNITION ||
            selected in sources
        _uiState.value = current.copy(
            availableSearchSources = sources,
            selectedSearchSource = if (stillAvailable) selected else SearchSource.ALL
        )
        LyraLogger.d(TAG, "availableSearchSources updated: ${sources.joinToString(",")}")
    }

    /** 设置当前搜索源 */
    fun setSearchSource(source: SearchSource) {
        if (source == SearchSource.YOUTUBE_MUSIC && !youtubeEnabled) return
        if (source == _uiState.value.selectedSearchSource) return
        LyraLogger.d(TAG, "setSearchSource: ${_uiState.value.selectedSearchSource} -> $source")
        searchJob?.cancel()
        searchMoreJob?.cancel()
        invalidateSearchRequest()
        _uiState.value = _uiState.value.copy(
            selectedSearchSource = source,
            searching = false,
            searchResults = emptyList(), // 切换源时清空结果
            searchItems = emptyList(),
            searchHasMore = false,
            searchLoadingMore = false,
            searchLoadMoreError = null,
            searchPage = 0,
            searchKeyword = "",
            searchDisplayQuery = "",
            searchError = null
        )
    }

    fun setNeteaseSearchType(type: NeteaseExploreSearchType) {
        if (type == _uiState.value.selectedNeteaseSearchType) return
        LyraLogger.d(TAG, "setNeteaseSearchType: ${_uiState.value.selectedNeteaseSearchType} -> $type")
        searchJob?.cancel()
        searchMoreJob?.cancel()
        invalidateSearchRequest()
        _uiState.value = _uiState.value.copy(
            selectedNeteaseSearchType = type,
            searching = false,
            searchError = null,
            searchResults = emptyList(),
            searchItems = emptyList(),
            searchHasMore = false,
            searchLoadingMore = false,
            searchLoadMoreError = null,
            searchPage = 0,
            searchKeyword = "",
            searchDisplayQuery = ""
        )
    }

    fun setYouTubeMusicSearchType(type: YouTubeExploreSearchType) {
        if (type == _uiState.value.selectedYouTubeMusicSearchType) return
        LyraLogger.d(
            TAG,
            "setYouTubeMusicSearchType: ${_uiState.value.selectedYouTubeMusicSearchType} -> $type"
        )
        searchJob?.cancel()
        searchMoreJob?.cancel()
        invalidateSearchRequest()
        _uiState.value = _uiState.value.copy(
            selectedYouTubeMusicSearchType = type,
            searching = false,
            searchError = null,
            searchResults = emptyList(),
            searchItems = emptyList(),
            searchHasMore = false,
            searchLoadingMore = false,
            searchLoadMoreError = null,
            searchPage = 0,
            searchKeyword = "",
            searchDisplayQuery = ""
        )
    }

    /** 渠道（QQ / 酷狗 / 咪咕）搜索分类：歌曲 / 歌手 / 歌单 */
    fun setChannelSearchType(type: ChannelSearchType) {
        if (type == _uiState.value.selectedChannelSearchType) return
        LyraLogger.d(TAG, "setChannelSearchType: ${_uiState.value.selectedChannelSearchType} -> $type")
        searchJob?.cancel()
        searchMoreJob?.cancel()
        invalidateSearchRequest()
        _uiState.value = _uiState.value.copy(
            selectedChannelSearchType = type,
            searching = false,
            searchError = null,
            searchResults = emptyList(),
            searchItems = emptyList(),
            searchHasMore = false,
            searchLoadingMore = false,
            searchLoadMoreError = null,
            searchPage = 0,
            // 清空已记录的关键词，让界面的搜索副作用按新分类重新发起搜索
            searchKeyword = "",
            searchDisplayQuery = ""
        )
    }

    /** 统一搜索入口 */
    fun search(keyword: String, displayQuery: String = keyword) {
        val apiKeyword = keyword.trim()
        val matchQuery = displayQuery.trim().ifBlank { apiKeyword }
        if (apiKeyword.isBlank()) {
            LyraLogger.d(TAG, "search cleared because keyword is blank")
            searchJob?.cancel()
            searchMoreJob?.cancel()
            invalidateSearchRequest()
            _uiState.value = _uiState.value.copy(
                searching = false,
                searchResults = emptyList(),
                searchItems = emptyList(),
                searchHasMore = false,
                searchLoadingMore = false,
                searchLoadMoreError = null,
                searchPage = 0,
                searchKeyword = "",
                searchDisplayQuery = "",
                searchError = null
            )
            return
        }
        val source = _uiState.value.selectedSearchSource
        if (source == SearchSource.YOUTUBE_MUSIC && !youtubeEnabled) {
            LyraLogger.d(TAG, "skip search: YouTube disabled")
            return
        }
        val requestVersion = beginSearchRequest(apiKeyword, matchQuery)
        LyraLogger.d(
            TAG,
            "search start: source=$source, request=$requestVersion, keyword=$apiKeyword, display=$matchQuery"
        )
        if (source == SearchSource.ALL) {
            searchAllSources(apiKeyword, matchQuery, requestVersion)
            return
        }
        // 已迁移到统一 SourceSearchProvider 的平台优先走这里。
        val provider = source.musicSourceId?.let(SourceSearchRegistry::providerFor)
        if (provider != null) {
            searchViaProvider(provider, source, apiKeyword, matchQuery, requestVersion)
            return
        }
        // [MIGRATION COMPAT] 尚未迁移的平台仍走旧分发；每迁移一个平台就删除对应分支。
        // 新平台不要继续加分支，应实现并注册 SourceSearchProvider。
        when (source) {
            SearchSource.LINK_RECOGNITION -> searchRecognizedLink(apiKeyword, requestVersion)
            else -> Unit
        }
    }

    /**
     * 「全部」聚合搜索：对当前所有 enabled + 就绪 + 支持当前类型的音源并发搜索。
     *
     * 单个音源失败不会影响其它音源，失败只记录日志，其余结果照常返回。
     */
    private fun searchAllSources(keyword: String, matchQuery: String, requestVersion: Long) {
        val type = _uiState.value.selectedAllSearchType
        val targets = resolveAggregateSources(type)
        LyraLogger.d(
            TAG,
            "aggregate search start: type=$type, targets=${targets.joinToString(",")}"
        )
        searchJob = viewModelScope.launch {
            val pages = coroutineScope {
                targets.map { source ->
                    async(Dispatchers.IO) {
                        val provider = source.musicSourceId
                            ?.let(SourceSearchRegistry::providerFor)
                            ?: return@async null
                        try {
                            provider.search(
                                SourceSearchRequest(
                                    sourceId = provider.sourceId,
                                    keyword = keyword,
                                    matchQuery = matchQuery,
                                    type = type,
                                    page = 1
                                )
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            LyraLogger.w(TAG, "聚合搜索失败(忽略): source=$source, ${e.message}")
                            null
                        }
                    }
                }.awaitAll()
            }
            val merged = ArrayList<ExploreSearchResult>()
            val seen = HashSet<String>()
            pages.forEach { page ->
                page?.items?.forEach { item ->
                    if (seen.add(item.stableKey)) merged.add(item)
                }
            }
            val successCount = pages.count { it != null }
            val failureCount = pages.count { it == null }
            val allSourcesFailed = resolveAggregateAllSourcesFailed(
                targetCount = targets.size,
                failureCount = failureCount,
                successCount = successCount
            )
            updateSearchStateIfCurrent(requestVersion, SearchSource.ALL) {
                it.copy(
                    searching = false,
                    searchError = null,
                    allSourcesFailed = allSourcesFailed,
                    searchLoadMoreError = null,
                    searchResults = searchSongItems(merged),
                    searchItems = merged,
                    searchPage = 1,
                    searchHasMore = false
                )
            }
        }
    }

    /**
     * 统一的 Provider 搜索流程：负责错误处理与状态更新，具体平台逻辑交给 [SourceSearchProvider]。
     */
    private fun searchViaProvider(
        provider: SourceSearchProvider,
        source: SearchSource,
        keyword: String,
        matchQuery: String,
        requestVersion: Long
    ) {
        val type = currentSourceSearchType(source)
        searchJob = viewModelScope.launch {
            try {
                val result = provider.search(
                    SourceSearchRequest(
                        sourceId = provider.sourceId,
                        keyword = keyword,
                        matchQuery = matchQuery,
                        type = type,
                        page = 1
                    )
                )
                LyraLogger.d(
                    TAG,
                    "search via provider success: source=$source, request=$requestVersion, " +
                        "type=$type, keyword=$keyword, count=${result.items.size}"
                )
                updateSearchStateIfCurrent(requestVersion, source) {
                    it.copy(
                        searching = false,
                        searchError = null,
                        allSourcesFailed = false,
                        searchLoadMoreError = null,
                        searchResults = result.songs,
                        searchItems = result.items,
                        searchPage = result.page,
                        searchHasMore = result.hasMore
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LyraLogger.e(
                    TAG,
                    "search via provider failed: source=$source, request=$requestVersion, keyword=$keyword",
                    e
                )
                updateSearchStateIfCurrent(requestVersion, source) {
                    it.copy(
                        searching = false,
                        searchError = searchErrorMessage(source, e),
                        allSourcesFailed = false,
                        searchResults = emptyList(),
                        searchItems = emptyList(),
                        searchHasMore = false,
                        searchLoadingMore = false,
                        searchLoadMoreError = null,
                        searchPage = 0
                    )
                }
            }
        }
    }

    /** 当前选中平台对应的统一搜索类型。 */
    private fun currentSourceSearchType(source: SearchSource): SourceSearchType =
        _uiState.value.selectedSearchTypeOf(source)

    /** 统一设置某个音源 / 聚合入口的搜索类型（UI 只调用这一个入口）。 */
    fun setSourceSearchType(source: SearchSource, type: SourceSearchType) {
        when (source) {
            SearchSource.ALL -> {
                if (_uiState.value.selectedAllSearchType == type) return
                _uiState.value = _uiState.value.copy(
                    selectedAllSearchType = type,
                    searching = false,
                    searchError = null,
                    allSourcesFailed = false,
                    searchResults = emptyList(),
                    searchItems = emptyList(),
                    searchHasMore = false,
                    searchLoadingMore = false,
                    searchLoadMoreError = null,
                    searchPage = 0,
                    searchKeyword = "",
                    searchDisplayQuery = ""
                )
            }
            SearchSource.NETEASE -> type.toNeteaseType()?.let(::setNeteaseSearchType)
            SearchSource.KUWO,
            SearchSource.KUGOU,
            SearchSource.MIGU,
            SearchSource.TENCENT -> type.toChannelSearchType()?.let(::setChannelSearchType)
            else -> Unit
        }
        searchJob?.cancel()
        searchMoreJob?.cancel()
        invalidateSearchRequest()
    }

    /** 聚合入口实际参与搜索的音源（enabled + 就绪 + 支持当前类型）。 */
    private fun resolveAggregateSources(type: SourceSearchType): List<SearchSource> =
        _uiState.value.availableSearchSources.filter { source ->
            source.musicSourceId
                ?.let(SourceSearchRegistry::providerFor)
                ?.supports(type) == true
        }

    fun loadMoreSearchResults() {
        val state = _uiState.value
        val source = state.selectedSearchSource
        if (source == SearchSource.NETEASE && !isNeteaseSearchAllowed()) {
            clearNeteaseSearchForAuthRequired()
            return
        }
        if (
            state.searchKeyword.isBlank() ||
            !state.searchHasMore ||
            state.searching ||
            state.searchLoadingMore ||
            searchMoreJob?.isActive == true
        ) {
            return
        }

        val neteaseType = state.selectedNeteaseSearchType
        val keyword = state.searchKeyword
        val matchQuery = state.searchDisplayQuery.ifBlank { keyword }
        val nextPage = state.searchPage + 1
        val requestVersion = searchRequestVersion
        _uiState.value = state.copy(searchLoadingMore = true, searchLoadMoreError = null)
        LyraLogger.d(
            TAG,
            "search load more: source=$source, request=$requestVersion, keyword=$keyword, page=$nextPage, type=$neteaseType"
        )
        searchMoreJob = viewModelScope.launch {
            try {
                if (source == SearchSource.NETEASE && !isNeteaseSearchAllowed()) {
                    clearNeteaseSearchForAuthRequired()
                    return@launch
                }
                val provider = source.musicSourceId?.let(SourceSearchRegistry::providerFor)
                val result: ExploreSearchFetchResult = if (provider != null) {
                    provider.search(
                        SourceSearchRequest(
                            sourceId = provider.sourceId,
                            keyword = keyword,
                            matchQuery = matchQuery,
                            type = currentSourceSearchType(source),
                            page = nextPage
                        )
                    ).toFetchResult()
                } else {
                    // 其余平台当前不支持分页（分页能力后续接入 Provider 后在此扩展）
                    return@launch
                }
                if (source == SearchSource.NETEASE && !isNeteaseSearchAllowed()) {
                    clearNeteaseSearchForAuthRequired()
                    return@launch
                }
                updateSearchStateIfCurrent(requestVersion, source) {
                    val merged = mergeExploreSearchResults(it.searchItems, result.items)
                    it.copy(
                        searching = false,
                        searchLoadingMore = false,
                        searchLoadMoreError = null,
                        searchError = null,
                        searchItems = merged,
                        searchResults = searchSongItems(merged),
                        searchPage = result.page,
                        searchHasMore = result.hasMore
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LyraLogger.e(
                    TAG,
                    "search load more failed: source=$source, request=$requestVersion, keyword=$keyword, page=$nextPage",
                    e
                )
                updateSearchStateIfCurrent(requestVersion, source) {
                    it.copy(
                        searchLoadingMore = false,
                        searchLoadMoreError = searchErrorMessage(source, e),
                        searchHasMore = true
                    )
                }
            }
        }
    }

    private fun beginSearchRequest(keyword: String, displayQuery: String): Long {
        searchJob?.cancel()
        searchMoreJob?.cancel()
        val requestVersion = invalidateSearchRequest()
        _uiState.value = _uiState.value.copy(
            searching = true,
            searchError = null,
            allSourcesFailed = false,
            searchResults = emptyList(),
            searchItems = emptyList(),
            searchHasMore = false,
            searchLoadingMore = false,
            searchLoadMoreError = null,
            searchPage = 0,
            searchKeyword = keyword,
            searchDisplayQuery = displayQuery
        )
        return requestVersion
    }

    private fun invalidateSearchRequest(): Long {
        searchRequestVersion += 1
        return searchRequestVersion
    }

    private fun isSearchRequestCurrent(requestVersion: Long, source: SearchSource): Boolean {
        val currentState = _uiState.value
        return searchRequestVersion == requestVersion && currentState.selectedSearchSource == source
    }

    private inline fun updateSearchStateIfCurrent(
        requestVersion: Long,
        source: SearchSource,
        transform: (ExploreUiState) -> ExploreUiState
    ) {
        if (!isSearchRequestCurrent(requestVersion, source)) {
            val currentState = _uiState.value
            LyraLogger.d(
                TAG,
                "drop stale search update: source=$source, request=$requestVersion, currentRequest=$searchRequestVersion, currentSource=${currentState.selectedSearchSource}"
            )
            return
        }
        _uiState.value = transform(_uiState.value)
    }

    fun loadHighQuality(cat: String? = null) {
        val currentState = _uiState.value
        val realCat = cat ?: currentState.selectedTag
        val previousTag = currentState.selectedTag
        val previousPlaylists = currentState.playlists

        highQualityLoadJob?.cancel()
        _uiState.value = currentState.copy(
            loading = true,
            error = null,
            selectedTag = realCat
        )
        LyraLogger.d(
            TAG,
            "loadHighQuality start: tag=$realCat, apiCategory=${TAG_TO_API_CATEGORY[realCat] ?: realCat}, previousCount=${previousPlaylists.size}"
        )
        highQualityLoadJob = viewModelScope.launch {
            try {
                // Convert tag key to Chinese API category
                val apiCategory = TAG_TO_API_CATEGORY[realCat] ?: realCat
                val raw = withContext(Dispatchers.IO) {
                    neteaseClient.getHighQualityPlaylists(apiCategory, 50, 0L)
                }
                val mapped = parsePlaylists(raw)
                LyraLogger.d(TAG, "loadHighQuality success: tag=$realCat, count=${mapped.size}")

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = null,
                    playlists = mapped,
                    selectedTag = realCat
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val shouldRestorePreviousContent = previousPlaylists.isNotEmpty() && realCat != previousTag
                LyraLogger.e(
                    TAG,
                    "loadHighQuality failed: tag=$realCat, restorePrevious=$shouldRestorePreviousContent",
                    e
                )
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = app.getString(
                        R.string.error_load_playlist,
                        e.message ?: app.getString(R.string.github_sync_failed_message)
                    ),
                    playlists = if (shouldRestorePreviousContent) previousPlaylists else emptyList(),
                    selectedTag = if (shouldRestorePreviousContent) previousTag else realCat
                )
            }
        }
    }

    private fun parsePlaylists(raw: String): List<PlaylistSummary> {
        val result = mutableListOf<PlaylistSummary>()
        val root = JSONObject(raw)
        val code = root.optInt("code", -1)
        if (code != 200) {
            LyraLogger.w(TAG, "parsePlaylists unexpected code=$code")
            return emptyList()
        }
        val arr = root.optJSONArray("playlists") ?: return emptyList()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            result.add(PlaylistSummary(
                id = obj.optLong("id"),
                name = obj.optString("name"),
                picUrl = obj.optString("coverImgUrl").replace("http://", "https://"),
                playCount = obj.optLong("playCount"),
                trackCount = obj.optInt("trackCount")
            ))
        }
        return result
    }

    /**
     * 打开渠道歌单时按歌单 id 拉取歌曲（QQ / 酷狗支持；咪咕匿名接口暂不提供）。
     */
    suspend fun loadChannelPlaylistSongs(
        source: SearchSource,
        playlistId: String
    ): List<SongItem> {
        // 平台实现全部下移到 Provider；不支持「歌单内歌曲」的平台由 capabilities 表达（返回空）。
        val provider = source.musicSourceId
            ?.let(SourceSearchRegistry::providerFor)
            ?: return emptyList()
        return provider.fetchPlaylistSongs(playlistId)
    }

    private fun isNeteaseSearchAllowed(): Boolean {
        // 网易云搜索接口本身支持匿名访问，不再强制要求登录
        return true
    }

    private fun clearNeteaseSearchForAuthRequired() {
        searchJob?.cancel()
        searchMoreJob?.cancel()
        invalidateSearchRequest()
        _uiState.value = _uiState.value.withNeteaseAuthRequired(
            error = app.getString(R.string.netease_login_required_search)
        )
    }

    private fun searchRecognizedLink(input: String, requestVersion: Long) {
        searchJob = viewModelScope.launch {
            try {
                val target = recognizeExploreLink(input)
                if (target == null) {
                    updateSearchStateIfCurrent(requestVersion, SearchSource.LINK_RECOGNITION) {
                        it.copy(
                            searching = false,
                            searchError = app.getString(R.string.explore_link_invalid),
                            searchResults = emptyList(),
                            searchItems = emptyList(),
                            searchHasMore = false,
                            searchPage = 0
                        )
                    }
                    return@launch
                }

                val item = withContext(Dispatchers.IO) {
                    resolveExploreLinkTarget(target)
                }
                LyraLogger.d(
                    TAG,
                    "link recognized: request=$requestVersion, target=$target, item=${item.stableKey}"
                )
                updateSearchStateIfCurrent(requestVersion, SearchSource.LINK_RECOGNITION) {
                    val items = listOf(item)
                    it.copy(
                        searching = false,
                        searchError = null,
                        searchLoadMoreError = null,
                        searchResults = searchSongItems(items),
                        searchItems = items,
                        searchPage = 1,
                        searchHasMore = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LyraLogger.e(TAG, "link recognition failed: request=$requestVersion, input=$input", e)
                updateSearchStateIfCurrent(requestVersion, SearchSource.LINK_RECOGNITION) {
                    it.copy(
                        searching = false,
                        searchError = app.getString(
                            R.string.error_link_recognition,
                            e.message ?: app.getString(R.string.github_sync_failed_message)
                        ),
                        searchResults = emptyList(),
                        searchItems = emptyList(),
                        searchHasMore = false,
                        searchLoadingMore = false,
                        searchLoadMoreError = null,
                        searchPage = 0
                    )
                }
            }
        }
    }

    private suspend fun resolveExploreLinkTarget(target: ExploreLinkTarget): ExploreSearchResult {
        return when (target) {
            is ExploreLinkTarget.NeteaseSong -> ExploreSearchResult.Song(
                fetchLinkedNeteaseSong(target.id)
            )
            is ExploreLinkTarget.NeteasePlaylist -> ExploreSearchResult.Playlist(
                fetchLinkedNeteasePlaylist(target.id)
            )
            is ExploreLinkTarget.NeteaseArtist -> ExploreSearchResult.Artist(
                fetchLinkedNeteaseArtist(target.id)
            )
            is ExploreLinkTarget.NeteaseShortLink -> resolveExploreLinkTarget(
                resolveNeteaseShortLink(target.url)
            )
            is ExploreLinkTarget.BiliVideo -> fetchLinkedBiliVideo(target)
            is ExploreLinkTarget.BiliFavoriteFolder -> ExploreSearchResult.BilibiliPlaylist(
                fetchLinkedBiliFavoriteFolder(target.mediaId)
            )
            is ExploreLinkTarget.BiliFavoriteFolderByOwner -> ExploreSearchResult.BilibiliPlaylist(
                fetchLinkedBiliFavoriteFolder(target.ownerMid, target.folderId)
            )
            is ExploreLinkTarget.BiliCollection -> ExploreSearchResult.BilibiliPlaylist(
                fetchLinkedBiliCollection(target.ownerMid, target.seasonId)
            )
            is ExploreLinkTarget.BiliShortLink -> resolveExploreLinkTarget(
                resolveBiliShortLink(target.url)
            )
            is ExploreLinkTarget.YouTubeVideo -> ExploreSearchResult.Song(
                fetchLinkedYouTubeVideo(target)
            )
            is ExploreLinkTarget.YouTubePlaylist -> ExploreSearchResult.YouTubePlaylist(
                fetchLinkedYouTubePlaylist(target.playlistId)
            )
            is ExploreLinkTarget.Unsupported -> ExploreSearchResult.Notice(
                title = app.getString(R.string.explore_link_unsupported_title),
                message = app.getString(
                    R.string.explore_link_unsupported_message,
                    target.platform,
                    target.type
                )
            )
        }
    }

    private suspend fun resolveBiliShortLink(url: String): ExploreLinkTarget {
        val finalUrl = expandExploreRedirectUrl(url, AppContainer.sharedOkHttpClient)
        return recognizeExploreLink(finalUrl)
            ?.takeIf { it !is ExploreLinkTarget.BiliShortLink }
            ?: error(app.getString(R.string.explore_link_invalid))
    }

    private suspend fun resolveNeteaseShortLink(url: String): ExploreLinkTarget {
        val finalUrl = expandExploreRedirectUrl(url, AppContainer.sharedOkHttpClient)
        return recognizeExploreLink(finalUrl)
            ?.takeIf { it !is ExploreLinkTarget.NeteaseShortLink }
            ?: error(app.getString(R.string.explore_link_invalid))
    }

    private fun fetchLinkedNeteaseSong(songId: Long): SongItem {
        val raw = neteaseClient.getSongDetail(listOf(songId))
        return parseNeteaseSongDetail(raw) ?: SongItem(
            id = songId,
            name = app.getString(R.string.explore_link_netease_song_fallback, songId),
            artist = "",
            album = "",
            albumId = 0L,
            durationMs = 0L,
            coverUrl = null,
            mediaUri = "https://music.163.com/#/song?id=$songId",
            channelId = "netease",
            audioId = songId.toString()
        )
    }

    private fun fetchLinkedNeteasePlaylist(playlistId: Long): PlaylistSummary {
        return runCatching {
            parseLinkedNeteasePlaylist(
                raw = neteaseClient.getPlaylistDetail(playlistId),
                fallbackId = playlistId
            )
        }.getOrElse {
            PlaylistSummary(
                id = playlistId,
                name = app.getString(R.string.explore_link_netease_playlist_fallback, playlistId),
                picUrl = "",
                playCount = 0L,
                trackCount = 0
            )
        }
    }

    private fun fetchLinkedNeteaseArtist(artistId: Long): NeteaseSearchArtistResult {
        return runCatching {
            parseLinkedNeteaseArtist(
                raw = neteaseClient.getArtistDetail(artistId),
                fallbackId = artistId
            )
        }.getOrElse {
            NeteaseSearchArtistResult(
                artist = NeteaseArtistSummary(
                    id = artistId,
                    name = app.getString(R.string.explore_link_netease_artist_fallback, artistId)
                ),
                picUrl = null,
                musicSize = 0,
                albumSize = 0
            )
        }
    }

    private suspend fun fetchLinkedBiliVideo(
        target: ExploreLinkTarget.BiliVideo
    ): ExploreSearchResult {
        val info = target.bvid
            ?.let { biliClient.getVideoBasicInfoByBvid(it) }
            ?: target.avid
                ?.let { biliClient.getVideoBasicInfoByAvid(it) }
            ?: error(app.getString(R.string.explore_link_invalid))
        val collectionTarget = info.toExploreLinkCollectionTarget(target)
        if (collectionTarget != null) {
            return ExploreSearchResult.BilibiliPlaylist(
                fetchLinkedBiliCollection(
                    ownerMid = collectionTarget.ownerMid,
                    seasonId = collectionTarget.seasonId
                )
            )
        }
        return ExploreSearchResult.Song(info.toExploreLinkSong(target))
    }

    private suspend fun fetchLinkedBiliFavoriteFolder(mediaId: Long): BiliPlaylist {
        val folder = biliClient.getFavFolderInfo(mediaId)
        return folder.toExploreBiliPlaylist(BiliPlaylistKind.CREATED_FAVORITE)
    }

    private suspend fun fetchLinkedBiliFavoriteFolder(
        ownerMid: Long,
        folderId: Long
    ): BiliPlaylist {
        val created = biliClient.getUserCreatedFavFolders(ownerMid)
        created.firstOrNull { it.fid == folderId || it.mediaId == folderId }?.let { folder ->
            return hydrateLinkedBiliFolder(folder, BiliPlaylistKind.CREATED_FAVORITE)
        }

        val collected = runCatching {
            biliClient.getUserCollectedFavFolders(ownerMid)
        }.getOrDefault(emptyList())
        collected.firstOrNull { it.fid == folderId || it.mediaId == folderId }?.let { folder ->
            return hydrateLinkedBiliFolder(folder, BiliPlaylistKind.COLLECTED_FAVORITE)
        }
        error(app.getString(R.string.explore_link_invalid))
    }

    private suspend fun hydrateLinkedBiliFolder(
        folder: BiliClient.FavFolder,
        fallbackKind: BiliPlaylistKind
    ): BiliPlaylist {
        if (folder.itemType == BILI_RESOURCE_TYPE_COLLECTION) {
            return fetchLinkedBiliCollection(folder.mid, folder.mediaId)
        }
        val detail = runCatching {
            biliClient.getFavFolderInfo(folder.mediaId)
        }.getOrDefault(folder)
        return detail.toExploreBiliPlaylist(fallbackKind)
    }

    private suspend fun fetchLinkedBiliCollection(
        ownerMid: Long,
        seasonId: Long
    ): BiliPlaylist {
        val page = biliClient.getCollectionArchives(
            mid = ownerMid,
            seasonId = seasonId,
            page = 1
        )
        val meta = page.meta
        return BiliPlaylist(
            mediaId = meta.seasonId.takeIf { it > 0L } ?: seasonId,
            fid = 0L,
            mid = meta.mid.takeIf { it > 0L } ?: ownerMid,
            title = meta.title.ifBlank {
                app.getString(R.string.explore_link_bili_playlist_fallback, seasonId)
            },
            count = meta.total,
            coverUrl = meta.coverUrl,
            kind = BiliPlaylistKind.COLLECTION
        )
    }

    private fun BiliClient.FavFolder.toExploreBiliPlaylist(
        fallbackKind: BiliPlaylistKind
    ): BiliPlaylist {
        val resolvedKind = if (itemType == BILI_RESOURCE_TYPE_COLLECTION) {
            BiliPlaylistKind.COLLECTION
        } else {
            fallbackKind
        }
        return BiliPlaylist(
            mediaId = mediaId,
            fid = fid,
            mid = mid,
            title = title.ifBlank {
                app.getString(R.string.explore_link_bili_playlist_fallback, mediaId)
            },
            count = count,
            coverUrl = coverUrl.replaceFirst("http://", "https://"),
            kind = resolvedKind,
            subtitle = upperName
        )
    }

    private suspend fun fetchLinkedYouTubeVideo(target: ExploreLinkTarget.YouTubeVideo): SongItem {
        val matched = runCatching {
            AppContainer.youtubeMusicClient.search(target.videoId, limit = 5)
                .firstOrNull { it.videoId == target.videoId }
        }.getOrNull()
        if (matched != null) {
            val matchedSong = matched.toSongItem(app)
            return matchedSong.copy(
                coverUrl = matchedSong.coverUrl ?: youtubeMusicThumbnailUrl(matched.videoId),
                originalCoverUrl = matchedSong.originalCoverUrl
                    ?: youtubeMusicThumbnailUrl(matched.videoId),
                mediaUri = buildYouTubeMusicMediaUri(
                    videoId = matched.videoId,
                    playlistId = target.playlistId
                ),
                playlistContextId = target.playlistId
            )
        }

        val metadata = runCatching {
            AppContainer.youtubeMusicClient.getVideoMetadata(target.videoId)
        }.getOrNull()
        val displayAlbum = app.getString(R.string.youtube_search_type_video)
        val displayName = metadata?.title.orEmpty().ifBlank {
            app.getString(R.string.explore_link_youtube_video_fallback, target.videoId)
        }
        val displayArtist = metadata?.authorName.orEmpty().ifBlank { "YouTube" }
        val coverUrl = metadata?.thumbnailUrl.orEmpty().ifBlank {
            youtubeMusicThumbnailUrl(target.videoId)
        }
        return SongItem(
            id = stableYouTubeMusicId(target.videoId),
            name = displayName,
            artist = displayArtist,
            album = displayAlbum,
            albumId = stableYouTubeMusicId("${target.videoId}|$displayAlbum"),
            durationMs = 0L,
            coverUrl = coverUrl,
            mediaUri = buildYouTubeMusicMediaUri(
                videoId = target.videoId,
                playlistId = target.playlistId
            ),
            originalName = displayName,
            originalArtist = displayArtist,
            originalCoverUrl = coverUrl,
            channelId = "youtubeMusic",
            audioId = target.videoId,
            playlistContextId = target.playlistId
        )
    }

    private suspend fun fetchLinkedYouTubePlaylist(playlistId: String): YouTubeMusicPlaylist {
        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        return runCatching {
            val detail = AppContainer.youtubeMusicClient.getPlaylistDetailPreview(browseId)
            YouTubeMusicPlaylist(
                browseId = detail.browseId.ifBlank { browseId },
                playlistId = detail.playlistId.ifBlank { playlistId.removePrefix("VL") },
                title = detail.title.ifBlank {
                    app.getString(R.string.explore_link_youtube_playlist_fallback, playlistId)
                },
                subtitle = detail.subtitle,
                coverUrl = detail.coverUrl,
                trackCount = detail.trackCount ?: detail.tracks.size
            )
        }.getOrElse {
            YouTubeMusicPlaylist(
                browseId = browseId,
                playlistId = playlistId.removePrefix("VL"),
                title = app.getString(R.string.explore_link_youtube_playlist_fallback, playlistId),
                subtitle = "",
                coverUrl = "",
                trackCount = 0
            )
        }
    }

    private fun parseLinkedNeteasePlaylist(
        raw: String,
        fallbackId: Long
    ): PlaylistSummary {
        val root = JSONObject(raw)
        val playlist = root.optJSONObject("playlist")
            ?: return PlaylistSummary(
                id = fallbackId,
                name = app.getString(R.string.explore_link_netease_playlist_fallback, fallbackId),
                picUrl = "",
                playCount = 0L,
                trackCount = 0
            )
        return PlaylistSummary(
            id = playlist.optLong("id", fallbackId),
            name = playlist.optString(
                "name",
                app.getString(R.string.explore_link_netease_playlist_fallback, fallbackId)
            ),
            picUrl = playlist.optString("coverImgUrl", "")
                .replaceFirst("http://", "https://"),
            playCount = playlist.optLong("playCount", 0L),
            trackCount = playlist.optInt("trackCount", 0)
        )
    }

    private fun parseLinkedNeteaseArtist(
        raw: String,
        fallbackId: Long
    ): NeteaseSearchArtistResult {
        val root = JSONObject(raw)
        val artist = root.optJSONObject("data")?.optJSONObject("artist")
            ?: root.optJSONObject("artist")
        val fallbackName = app.getString(R.string.explore_link_netease_artist_fallback, fallbackId)
        if (artist == null) {
            return NeteaseSearchArtistResult(
                artist = NeteaseArtistSummary(id = fallbackId, name = fallbackName),
                picUrl = null,
                musicSize = 0,
                albumSize = 0
            )
        }
        return NeteaseSearchArtistResult(
            artist = NeteaseArtistSummary(
                id = artist.optLong("id", fallbackId),
                name = artist.optString("name", fallbackName).ifBlank { fallbackName }
            ),
            picUrl = artist.optString("cover", "")
                .ifBlank { artist.optString("picUrl", "") }
                .ifBlank { artist.optString("avatar", "") }
                .ifBlank { artist.optString("img1v1Url", "") }
                .replaceFirst("http://", "https://")
                .takeIf { it.isNotBlank() },
            musicSize = artist.optInt("musicSize", 0),
            albumSize = artist.optInt("albumSize", 0)
        )
    }

    suspend fun getVideoInfoByAvid(avid: Long): BiliClient.VideoBasicInfo {
        return withContext(Dispatchers.IO) {
            biliClient.getVideoBasicInfoByAvid(avid)
        }
    }

    /**
     * 将 Bilibili 视频的分P转换为通用的 SongItem
     * @param page 分P信息
     * @param basicInfo 视频的基本信息
     * @param coverUrl 视频封面
     * @return 转换后的 SongItem
     */
    fun toSongItem(page: BiliClient.VideoPage, basicInfo: BiliClient.VideoBasicInfo, coverUrl: String): SongItem {
        return buildBiliPartSong(page, basicInfo, coverUrl)
    }

    /** 加载 YouTube Music 歌单列表 */
    fun loadYtMusicPlaylists() {
        if (!youtubeEnabled) return
        if (ytMusicPlaylistsJob?.isActive == true) {
            ytMusicPlaylistsPending = true
            LyraLogger.d(TAG, "loadYtMusicPlaylists coalesced while loading")
            return
        }
        ytMusicPlaylistsPending = false
        _uiState.value = _uiState.value.copy(ytMusicPlaylistsLoading = true, ytMusicPlaylistsError = null)
        LyraLogger.d(TAG, "loadYtMusicPlaylists start")
        ytMusicPlaylistsJob = viewModelScope.launch {
            try {
                val library = withContext(Dispatchers.IO) {
                    AppContainer.youtubeMusicClient.getLibraryPlaylists(
                        resolveMissingTrackCounts = false
                    )
                }
                val playlists = library.map { pl ->
                    YouTubeMusicPlaylist(
                        browseId = pl.browseId,
                        playlistId = pl.playlistId,
                        title = pl.title,
                        subtitle = pl.subtitle,
                        coverUrl = pl.coverUrl,
                        trackCount = pl.trackCount ?: 0
                    )
                }
                LyraLogger.d(TAG, "loadYtMusicPlaylists success: count=${playlists.size}")
                _uiState.value = _uiState.value.copy(
                    ytMusicPlaylistsLoading = false,
                    ytMusicPlaylists = playlists,
                    ytMusicPlaylistsError = null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                LyraLogger.e(TAG, "loadYtMusicPlaylists failed", e)
                _uiState.value = _uiState.value.copy(
                    ytMusicPlaylistsLoading = false,
                    ytMusicPlaylistsError = "YouTube Music: ${e.message ?: "unknown error"}"
                )
            } finally {
                val completedJob = coroutineContext[Job]
                if (ytMusicPlaylistsJob === completedJob) {
                    ytMusicPlaylistsJob = null
                    if (ytMusicPlaylistsPending && youtubeEnabled) {
                        ytMusicPlaylistsPending = false
                        loadYtMusicPlaylists()
                    }
                }
            }
        }
    }

    private fun disableYouTubeSource() {
        if (_uiState.value.selectedSearchSource == SearchSource.YOUTUBE_MUSIC) {
            searchJob?.cancel()
            searchMoreJob?.cancel()
            invalidateSearchRequest()
        }
        ytMusicPlaylistsJob?.cancel()
        ytMusicPlaylistsJob = null
        ytMusicPlaylistsPending = false
        _uiState.value = _uiState.value.withYouTubeDisabled()
    }

    private fun searchErrorMessage(source: SearchSource, error: Exception): String {
        val fallback = error.message ?: app.getString(R.string.github_sync_failed_message)
        return when (source) {
            SearchSource.ALL -> fallback
            SearchSource.NETEASE -> app.getString(R.string.error_netease_search, fallback)
            SearchSource.BILIBILI -> app.getString(R.string.error_bilibili_search, fallback)
            SearchSource.YOUTUBE_MUSIC -> app.getString(R.string.error_youtube_search, fallback)
            SearchSource.KUGOU,
            SearchSource.MIGU,
            SearchSource.KUWO,
            SearchSource.TENCENT -> fallback
            SearchSource.LINK_RECOGNITION -> app.getString(R.string.error_link_recognition, fallback)
        }
    }
}

/** Bilibili 搜索结果到通用 SongItem 的转换器（Search Provider 复用） */
internal fun BiliClient.SearchVideoItem.toSongItem(): SongItem {
    return SongItem(
        id = this.aid, // 使用 avid 作为唯一ID
        name = this.titlePlain,
        artist = this.author,
        album = buildBiliSongAlbum(bvid = this.bvid), // 标记来源
        albumId = 0L,
        durationMs = this.durationSec * 1000L,
        coverUrl = this.coverUrl,
        channelId = "bilibili",
        audioId = this.aid.toString()
    )
}

private fun BiliClient.VideoBasicInfo.toSongItem(): SongItem {
    return SongItem(
        id = aid,
        name = title,
        artist = ownerName,
        album = buildBiliSongAlbum(bvid = bvid),
        albumId = 0L,
        durationMs = durationSec * 1000L,
        coverUrl = coverUrl,
        channelId = "bilibili",
        audioId = aid.toString()
    )
}

internal fun BiliClient.VideoBasicInfo.toExploreLinkSong(
    target: ExploreLinkTarget.BiliVideo
): SongItem {
    val selectedPage = target.cid
        ?.let { cid -> pages.firstOrNull { page -> page.cid == cid } }
        ?: target.page?.let { pageNumber ->
            pages.firstOrNull { page -> page.page == pageNumber }
        }
    return selectedPage?.let { page ->
        toSongItem().copy(
            album = buildBiliSongAlbum(cid = page.cid, bvid = bvid),
            durationMs = page.durationSec * 1_000L,
            subAudioId = page.cid.toString()
        )
    } ?: toSongItem()
}

internal fun BiliClient.VideoBasicInfo.toExploreLinkCollectionTarget(
    target: ExploreLinkTarget.BiliVideo
): ExploreLinkTarget.BiliCollection? {
    if (!target.isCollectionShare) return null
    val season = ugcSeason
    val seasonId = target.seasonId ?: season?.id ?: return null
    val collectionOwnerMid = season?.mid?.takeIf { it > 0L }
        ?: ownerMid.takeIf { it > 0L }
        ?: return null
    return ExploreLinkTarget.BiliCollection(
        ownerMid = collectionOwnerMid,
        seasonId = seasonId
    )
}

internal fun YouTubeMusicSearchResult.toSongItem(app: Application): SongItem {
    val displayArtist = artist.ifBlank { "YouTube" }
    val displayAlbum = album.ifBlank {
        when (type) {
            YouTubeMusicSearchResultType.Song -> app.getString(R.string.youtube_search_type_song)
            YouTubeMusicSearchResultType.Video -> app.getString(R.string.youtube_search_type_video)
        }
    }
    return SongItem(
        id = stableYouTubeMusicId(videoId),
        name = title,
        artist = displayArtist,
        album = displayAlbum,
        albumId = stableYouTubeMusicId("$videoId|$displayAlbum"),
        durationMs = durationMs,
        coverUrl = coverUrl.ifBlank { youtubeMusicThumbnailUrl(videoId) },
        mediaUri = buildYouTubeMusicMediaUri(videoId),
        originalName = title,
        originalArtist = displayArtist,
        originalCoverUrl = coverUrl.ifBlank { youtubeMusicThumbnailUrl(videoId) },
        channelId = "youtubeMusic",
        audioId = videoId
    )
}

/** 渠道搜索结果到通用 SongItem 的转换器（Search Provider 复用）。 */
internal fun CustomChannelSong.toSongItem(app: Application, sourceId: String): SongItem {
    val tag = when (sourceId) {
        MusicSourceIds.KUWO -> KUWO_SOURCE_TAG
        MusicSourceIds.KUGOU -> KUGOU_SOURCE_TAG
        MusicSourceIds.MIGU -> MIGU_SOURCE_TAG
        else -> TENCENT_SOURCE_TAG
    }
    val fallbackAlbum = when (sourceId) {
        MusicSourceIds.KUWO -> app.getString(R.string.explore_tab_kuwo)
        MusicSourceIds.KUGOU -> app.getString(R.string.explore_tab_kugou)
        MusicSourceIds.MIGU -> app.getString(R.string.explore_tab_migu)
        else -> app.getString(R.string.explore_tab_tencent)
    }
    // 本地化后的封面走 customCoverUrl（显示时优先用它），
    // 远端地址留在 coverUrl 里做备份：备份/同步不会因为本地文件丢了就没封面。
    val localCover = localCoverUrl ?: coverUrl?.takeIf { it.startsWith("file://") }
    return SongItem(
        id = CustomChannelSearchApi.stableChannelSongId("$tag|$platformId"),
        name = name,
        artist = artist,
        album = album.ifBlank { fallbackAlbum },
        albumId = albumId,
        durationMs = durationMs,
        coverUrl = coverUrl?.takeUnless { it.startsWith("file://") },
        customCoverUrl = localCover,
        channelId = tag.lowercase(),
        audioId = platformId,
        subAudioId = albumAudioId
    )
}
