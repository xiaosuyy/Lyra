package com.lyra.player.ui.screen.tab

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
 * File: com.lyra.player.ui.screen.tab/ExploreScreen
 * Created: 2025/8/8
 */

import android.app.Application
import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.ErrorOutline
import com.lyra.player.ui.melox.theme.MeloxType
import com.lyra.player.ui.theme.designsystem.LyraIconSize
import com.lyra.player.ui.theme.designsystem.LyraRadius
import com.lyra.player.ui.theme.designsystem.LocalNeriTextStyles
import com.lyra.player.ui.theme.designsystem.LocalNeriColors
import com.lyra.player.ui.theme.designsystem.LyraSectionHeader
import com.lyra.player.ui.theme.designsystem.LyraSpacing
import com.lyra.player.ui.theme.designsystem.LyraTextButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import com.lyra.player.ui.component.overlay.DensityScaledModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import com.lyra.player.ui.util.shouldAllowCollapsingTopAppBar
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lyra.player.R
import com.lyra.player.core.api.bili.BiliClient
import com.lyra.player.core.api.youtube.YouTubeMusicCreatorSummary
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.data.platform.youtube.YouTubeFeatureGate
import com.lyra.player.data.search.ExploreSearchHistoryRepository
import com.lyra.player.data.search.exploreSearchHistoryRecordKeyword
import com.lyra.player.data.search.exploreSearchHistoryForDisplay
import com.lyra.player.data.search.resolveExploreSearchKeyword
import com.lyra.player.ui.effect.glass.AdvancedGlassRole
import com.lyra.player.ui.effect.glass.AdvancedGlassSurface
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.data.local.playlist.system.FavoritesPlaylist
import com.lyra.player.data.local.playlist.system.LocalFilesPlaylist
import com.lyra.player.data.local.playlist.LocalPlaylistRepository
import com.lyra.player.data.local.playlist.launchLocalPlaylistMutation
import com.lyra.player.data.local.media.displayAlbum
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.data.model.sameIdentityAs
import com.lyra.player.data.model.stableKey
import com.lyra.player.data.model.NeteaseArtistSummary
import com.lyra.player.data.playlist.favorite.FavoritePlaylistRepository
import com.lyra.player.ui.LocalMiniPlayerHeight
import com.lyra.player.ui.component.playlist.PlaylistExportSheet
import com.lyra.player.ui.component.playlist.showPlaylistBatchExportAddedResult
import com.lyra.player.ui.component.playlist.showPlaylistBatchExportCreatedResult
import com.lyra.player.ui.component.sheet.bottomSheetScrollGuard
import com.lyra.player.ui.feedback.LyraSnackbarHost
import com.lyra.player.ui.feedback.showNeriSnackbar
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.ExploreSearchResult
import com.lyra.player.ui.viewmodel.tab.ExploreUiState
import com.lyra.player.ui.viewmodel.tab.ExploreViewModel
import com.lyra.player.ui.viewmodel.tab.BiliPlaylist
import com.lyra.player.ui.viewmodel.tab.NeteaseExploreSearchType
import com.lyra.player.ui.viewmodel.tab.NeteaseSearchArtistResult
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary
import com.lyra.player.ui.viewmodel.tab.SearchSource
import com.lyra.player.ui.viewmodel.tab.ChannelSearchType
import com.lyra.player.core.source.SourceSearchType
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.ui.viewmodel.tab.EXPLORE_SEARCH_TYPE_ORDER
import com.lyra.player.ui.viewmodel.tab.musicSourceId
import com.lyra.player.ui.viewmodel.tab.searchTypesFor
import com.lyra.player.ui.viewmodel.tab.selectedSearchTypeOf
import com.lyra.player.ui.viewmodel.tab.search.SourceSearchRegistry
import com.lyra.player.core.api.search.CustomChannelArtist
import com.lyra.player.core.api.search.CustomChannelPlaylist
import com.lyra.player.ui.viewmodel.tab.YouTubeExploreSearchType
import com.lyra.player.ui.viewmodel.tab.YouTubeMusicPlaylist
import com.lyra.player.ui.viewmodel.tab.shouldLoadExploreSearchMore
import com.lyra.player.ui.util.currentWindowWidthDp
import com.lyra.player.ui.util.rememberSongDisplayCoverUrl
import com.lyra.player.ui.util.ClipboardCopyResult
import com.lyra.player.ui.util.copyPlainTextSafely
import com.lyra.player.ui.haptic.HapticIconButton
import com.lyra.player.ui.haptic.HapticTextButton
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.util.media.fastScrollableImageRequest
import com.lyra.player.util.format.formatDuration
import com.lyra.player.util.format.formatPlayCount
import com.lyra.player.ui.haptic.performHapticFeedback

private const val SEARCH_INPUT_DEBOUNCE_MS = 300L
private val ExplorePrimaryTabShape = RoundedCornerShape(20.dp)
private val ExplorePillShape = RoundedCornerShape(999.dp)
private val ExploreSearchFieldShape = RoundedCornerShape(16.dp)

/**
 * 生成 Explore 的搜索渠道 Tab。
 *
 * 音源列表由 ViewModel 从统一的 [com.lyra.player.core.source.SourceManager] 计算后传入，
 * 这里只负责：前置聚合入口「全部」、追加工具入口「链接」。不再维护任何平台名单。
 */
internal fun buildExploreSourceTabs(
    availableSources: List<SearchSource>
): List<SearchSource> = buildList {
    add(SearchSource.ALL)
    availableSources.forEach { source ->
        if (
            source != SearchSource.ALL &&
            source != SearchSource.LINK_RECOGNITION &&
            source != SearchSource.YOUTUBE_MUSIC
        ) {
            add(source)
        }
    }
    add(SearchSource.LINK_RECOGNITION)
}

/**
 * 是否在搜索结果中显示「歌曲」Section Header。
 *
 * 规则（与平台无关，只看搜索类型与实际结果结构）：
 * - 用户主动选择「歌曲」类型 -> 不显示
 * - 结果中存在歌曲 + 其他类型 -> 显示
 * - 结果只有歌曲、或没有歌曲 -> 不显示
 */
internal fun shouldShowExploreSongSectionHeader(
    searchType: SourceSearchType,
    hasSongResults: Boolean,
    hasOtherTypeResults: Boolean
): Boolean = shouldShowExploreSectionHeader(
    sectionType = SourceSearchType.SONG,
    currentSearchType = searchType,
    hasSectionResults = hasSongResults,
    hasOtherTypeResults = hasOtherTypeResults
)

/**
 * 通用 Section Header 显示规则：用户主动选择该类型时不显示；聚合且同时存在该类型与其他类型时显示。
 * 与平台无关。
 */
internal fun shouldShowExploreSectionHeader(
    sectionType: SourceSearchType,
    currentSearchType: SourceSearchType,
    hasSectionResults: Boolean,
    hasOtherTypeResults: Boolean
): Boolean = currentSearchType != sectionType && hasSectionResults && hasOtherTypeResults

/** 「歌手」Section Header 规则（复用通用规则，与平台无关）。 */
internal fun shouldShowExploreArtistSectionHeader(
    searchType: SourceSearchType,
    hasArtistResults: Boolean,
    hasOtherTypeResults: Boolean
): Boolean = shouldShowExploreSectionHeader(
    sectionType = SourceSearchType.ARTIST,
    currentSearchType = searchType,
    hasSectionResults = hasArtistResults,
    hasOtherTypeResults = hasOtherTypeResults
)

/** Error 判定：有 query、已结束，且（单源报错 或 聚合整体失败）。Error 优先于 Empty。 */
internal fun isExploreSearchError(
    hasQuery: Boolean,
    searching: Boolean,
    searchError: String?,
    allSourcesFailed: Boolean
): Boolean = hasQuery && !searching && (searchError != null || allSourcesFailed)

/** 「搜索后无结果」判定：有 query 且已结束、非 Loading、非 Error、且结果为空。 */
internal fun isExploreSearchEmpty(
    hasQuery: Boolean,
    searching: Boolean,
    hasError: Boolean,
    hasResults: Boolean
): Boolean = hasQuery && !searching && !hasError && !hasResults

/**
 * 切换搜索类型后是否需要自动重搜，并返回应使用的关键词。
 *
 * - 关键词为空 / 未执行过搜索 -> null（不搜索）
 * - 搜索类型未变化 -> null（不重复搜索）
 * - 否则返回（去空白后的）关键词，复用现有搜索入口重搜
 */
internal fun resolveExploreReSearchKeyword(
    executedKeyword: String,
    previousType: SourceSearchType,
    newType: SourceSearchType
): String? {
    if (previousType == newType) return null
    return executedKeyword.trim().takeIf { it.isNotBlank() }
}

/**
 * 音源 Tab 的选中索引收敛：列表会随 capability 过滤变短，索引必须落在合法范围内，
 * 否则 Material3 ScrollableTabRow 内部会 tabs[index] 越界崩溃。
 */
internal fun resolveExploreSelectedTabIndex(
    currentPage: Int,
    tabCount: Int
): Int = currentPage.coerceIn(0, (tabCount - 1).coerceAtLeast(0))

/**
 * 按当前搜索类型对音源做只读的 capability 过滤（UI-only，不改动数据层）。
 *
 * - 「全部」「链接识别」始终保留
 * - 普通音源仅保留 [supports] 为 true 的项
 * - 保持传入顺序（即上游的 enabled / availability / sortOrder 结果）
 */
internal fun filterExploreSourcesForType(
    sources: List<SearchSource>,
    type: SourceSearchType,
    supports: (sourceId: String, type: SourceSearchType) -> Boolean
): List<SearchSource> = sources.filter { source ->
    when (source) {
        SearchSource.ALL,
        SearchSource.LINK_RECOGNITION -> true
        else -> source.musicSourceId?.let { sourceId -> supports(sourceId, type) } == true
    }
}

internal fun shouldClearExploreSearchQuery(
    previous: SearchSource,
    current: SearchSource
): Boolean {
    return previous != current && (
        previous == SearchSource.LINK_RECOGNITION ||
            current == SearchSource.LINK_RECOGNITION
        )
}

internal fun exploreSearchScrollContextKey(
    keyword: String,
    source: SearchSource,
    neteaseSearchType: NeteaseExploreSearchType,
    youtubeSearchType: YouTubeExploreSearchType = YouTubeExploreSearchType.SONG,
    channelSearchType: ChannelSearchType = ChannelSearchType.SONG,
    allSearchType: com.lyra.player.core.source.SourceSearchType =
        com.lyra.player.core.source.SourceSearchType.SONG
): String? {
    val normalizedKeyword = keyword.trim()
    if (normalizedKeyword.isBlank()) return null
    val sourceType = when (source) {
        SearchSource.ALL -> allSearchType.name
        SearchSource.NETEASE -> neteaseSearchType.name
        SearchSource.YOUTUBE_MUSIC -> youtubeSearchType.name
        SearchSource.TENCENT,
        SearchSource.KUGOU,
        SearchSource.MIGU,
        SearchSource.KUWO -> channelSearchType.name
        else -> "-"
    }
    return "${source.name}|$sourceType|$normalizedKeyword"
}

internal fun shouldResetExploreSearchScroll(
    previousContextKey: String?,
    currentContextKey: String?
): Boolean {
    return currentContextKey != null && previousContextKey != currentContextKey
}

internal fun shouldRenderExploreSearchResults(
    page: Int,
    currentPage: Int
): Boolean = page == currentPage

internal fun exploreSearchResultsBottomPadding(miniPlayerHeight: Dp): Dp =
    16.dp + miniPlayerHeight

internal fun shouldShowBiliPartsPicker(song: SongItem): Boolean {
    return song.album == PlayerManager.BILI_SOURCE_TAG ||
        song.album.startsWith("${PlayerManager.BILI_SOURCE_TAG}|")
}

@Composable
private fun searchSourceLabel(source: SearchSource): String {
    return when (source) {
        SearchSource.ALL -> stringResource(R.string.explore_tab_all)
        SearchSource.YOUTUBE_MUSIC -> stringResource(R.string.explore_tab_youtube)
        SearchSource.NETEASE -> stringResource(R.string.platform_netease_short)
        SearchSource.TENCENT -> stringResource(R.string.explore_tab_tencent)
        SearchSource.KUWO -> stringResource(R.string.explore_tab_kuwo)
        SearchSource.KUGOU -> stringResource(R.string.explore_tab_kugou)
        SearchSource.MIGU -> stringResource(R.string.explore_tab_migu)
        SearchSource.GDSTUDIO -> stringResource(R.string.explore_tab_gdstudio)
        SearchSource.BILIBILI -> stringResource(R.string.platform_bilibili)
        SearchSource.LINK_RECOGNITION -> stringResource(R.string.explore_tab_links)
    }
}

@Composable
private fun neteaseSearchTypeLabel(type: NeteaseExploreSearchType): String {
    return when (type) {
        NeteaseExploreSearchType.SONG -> stringResource(R.string.explore_search_type_song)
        NeteaseExploreSearchType.PLAYLIST -> stringResource(R.string.explore_search_type_playlist)
        NeteaseExploreSearchType.ARTIST -> stringResource(R.string.explore_search_type_artist)
    }
}

private fun neteaseSearchTypeIcon(type: NeteaseExploreSearchType): ImageVector {
    return when (type) {
        NeteaseExploreSearchType.SONG -> Icons.Outlined.MusicNote
        NeteaseExploreSearchType.PLAYLIST -> Icons.AutoMirrored.Outlined.QueueMusic
        NeteaseExploreSearchType.ARTIST -> Icons.Filled.AccountCircle
    }
}

@Composable
private fun youtubeSearchTypeLabel(type: YouTubeExploreSearchType): String {
    return when (type) {
        YouTubeExploreSearchType.SONG -> stringResource(R.string.explore_search_type_song)
        YouTubeExploreSearchType.VIDEO -> stringResource(R.string.explore_search_type_video)
        YouTubeExploreSearchType.CREATOR -> stringResource(R.string.explore_search_type_creator)
    }
}

private fun youtubeSearchTypeIcon(type: YouTubeExploreSearchType): ImageVector {
    return when (type) {
        YouTubeExploreSearchType.SONG -> Icons.Outlined.MusicNote
        YouTubeExploreSearchType.VIDEO -> Icons.Filled.PlayCircle
        YouTubeExploreSearchType.CREATOR -> Icons.Filled.AccountCircle
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
@Suppress("AssignedValueIsNeverRead")
fun ExploreScreen(
    gridState: LazyGridState,
    topAppBarState: TopAppBarState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchListState: LazyListState,
    searchScrollContextKey: String?,
    onSearchScrollContextKeyChange: (String?) -> Unit,
    offlineMode: Boolean = false,
    onPlay: (PlaylistSummary) -> Unit,
    onBiliPlaylistClick: (BiliPlaylist) -> Unit = {},
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit = {},
    onYouTubeCreatorClick: (YouTubeMusicCreatorSummary) -> Unit = {},
    onNeteaseArtistClick: (NeteaseArtistSummary) -> Unit = {},
    onTencentArtistClick: (CustomChannelArtist) -> Unit = {},
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onSongPlayPreservingQueue: (SongItem) -> Unit = {},
    onSongPlayNext: (SongItem) -> Unit = {},
    onSongAddToQueueEnd: (SongItem) -> Unit = {},
    onPlayParts: (BiliClient.VideoBasicInfo, Int, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    if (offlineMode) {
        ExploreOfflineContent(topAppBarState)
        return
    }

    val vm: ExploreViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ExploreViewModel(context.applicationContext as Application) }
        }
    )
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val searchHistoryRepository = remember(context) {
        ExploreSearchHistoryRepository(context)
    }
    val searchHistory by searchHistoryRepository.historyFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val searchHistoryEnabled by AppContainer.settingsRepo.exploreSearchHistoryEnabledFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val availableSearchHistory = remember(searchHistoryEnabled, searchHistory) {
        exploreSearchHistoryForDisplay(searchHistoryEnabled, searchHistory)
    }
    val effectiveSearchKeyword = remember(searchQuery, availableSearchHistory, ui.selectedSearchSource) {
        if (ui.selectedSearchSource == SearchSource.LINK_RECOGNITION) {
            searchQuery.trim()
        } else {
            resolveExploreSearchKeyword(searchQuery, availableSearchHistory)
        }
    }
    val visibleSearchHistory = remember(availableSearchHistory, ui.selectedSearchSource) {
        if (ui.selectedSearchSource == SearchSource.LINK_RECOGNITION) {
            emptyList()
        } else {
            filteredExploreSearchHistory(availableSearchHistory)
        }
    }
    val backgroundImageUri by AppContainer.settingsRepo.backgroundImageUriFlow.collectAsStateWithLifecycle(
        initialValue = null
    )

    val repo = remember(context) { LocalPlaylistRepository.getInstance(context) }
    val allLocalPlaylists by repo.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val localPlaylistsReady by repo.initializationReadyFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val favoriteSongKeys = remember(allLocalPlaylists, context) {
        FavoritesPlaylist.firstOrNull(allLocalPlaylists, context)
            ?.songs
            .orEmpty()
            .mapTo(mutableSetOf()) { it.stableKey() }
    }
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val favoriteKeys = remember(favorites) {
        favorites.mapTo(mutableSetOf()) { "${it.source}:${it.id}" }
    }

    var showPartsSheet by remember { mutableStateOf(false) }
    var partsInfo by remember { mutableStateOf<BiliClient.VideoBasicInfo?>(null) }
    var clickedSongCoverUrl by remember { mutableStateOf("") }
    val partsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var channelPlaylistSheet by remember { mutableStateOf<ChannelPlaylistSheetState?>(null) }
    val channelPlaylistSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(channelPlaylistSheet?.playlistId, channelPlaylistSheet?.source) {
        val sheet = channelPlaylistSheet ?: return@LaunchedEffect
        if (!sheet.loading) return@LaunchedEffect
        val songs = runCatching {
            vm.loadChannelPlaylistSongs(sheet.source, sheet.playlistId)
        }.getOrElse { emptyList() }
        channelPlaylistSheet = sheet.copy(
            loading = false,
            songs = songs,
            failed = songs.isEmpty()
        )
    }

    var partsSelectionMode by remember { mutableStateOf(false) }
    var selectedParts by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var showExportSheet by remember { mutableStateOf(false) }
    var showChannelMenu by remember { mutableStateOf(false) }
    // 只有聚合搜索真正返回了结果、且至少有某个渠道有结果时，才允许按渠道筛选。
    val filterableSearchSources = remember(ui.resultSearchSources) {
        buildList {
            add(SearchSource.ALL)
            ui.resultSearchSources.forEach { source ->
                if (source != SearchSource.ALL) add(source)
            }
        }
    }
    val canFilterSearchSources = ui.selectedSearchSource != SearchSource.LINK_RECOGNITION &&
        ui.searchItems.isNotEmpty() &&
        ui.resultSearchSources.isNotEmpty()
    LaunchedEffect(canFilterSearchSources) {
        if (!canFilterSearchSources) showChannelMenu = false
    }

    val isInternational by AppContainer.settingsRepo.internationalizationEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val youtubeEnabled by AppContainer.settingsRepo.youtubeEnabledFlow
        .collectAsStateWithLifecycle(initialValue = YouTubeFeatureGate.isEnabled())
    // 搜索类型子标签由当前音源的 Provider 能力决定；单类型（如 Bilibili）不显示类型栏。
    val searchTypeBarTypes = ui.searchTypesFor(ui.selectedSearchSource)
    val selectedSearchType = ui.selectedSearchTypeOf(ui.selectedSearchSource)
    val orderedSearchSources = remember(ui.availableSearchSources, selectedSearchType) {
        buildExploreSourceTabs(
            filterExploreSourcesForType(
                sources = ui.availableSearchSources,
                type = selectedSearchType,
                supports = { sourceId, type ->
                    SourceSearchRegistry.providerFor(sourceId)?.supports(type) == true
                }
            )
        )
    }
    val initialSearchPage = remember(orderedSearchSources, ui.selectedSearchSource) {
        orderedSearchSources.indexOf(ui.selectedSearchSource).takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialSearchPage,
        pageCount = { orderedSearchSources.size }
    )
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val snackbarHostState = remember { SnackbarHostState() }
    val windowWidthDp = currentWindowWidthDp()
    val isTabletLayout = windowWidthDp >= 720.dp
    val searchPanelHorizontalPadding = if (isTabletLayout) 28.dp else 16.dp
    val searchResultHorizontalPadding = if (isTabletLayout) 88.dp else 0.dp
    val youtubeGridState = rememberLazyGridState()
    val tagChipSelectedAlpha = if (backgroundImageUri == null) 1f else 0.86f
    val tagChipUnselectedAlpha = if (backgroundImageUri == null) 1f else 0.74f
    val tagChipBorderAlpha = if (backgroundImageUri == null) 1f else 0.58f
    var previousSearchSource by remember { mutableStateOf(ui.selectedSearchSource) }
    val isExploreContentScrolled by remember(
        searchQuery,
        ui.selectedSearchSource,
        searchListState,
        gridState,
        youtubeGridState,
        topAppBarState
    ) {
        derivedStateOf {
            when {
                topAppBarState.collapsedFraction > 0f -> true
                searchQuery.isNotBlank() -> searchListState.canScrollBackward
                ui.selectedSearchSource == SearchSource.NETEASE -> gridState.canScrollBackward
                ui.selectedSearchSource == SearchSource.YOUTUBE_MUSIC -> {
                    youtubeGridState.canScrollBackward
                }
                else -> false
            }
        }
    }
    val shouldShowSearchHistory = shouldShowExploreSearchHistory(
        history = visibleSearchHistory,
        contentScrolled = isExploreContentScrolled
    )
    // 搜索类型子标签由当前音源的 Provider 能力决定；有结果类型可选时固定在搜索框下方。
    val searchTypeBarSource = when {
        searchQuery.isNotEmpty() && searchTypeBarTypes.size > 1 -> ui.selectedSearchSource
        else -> null
    }

    val shouldLoadMoreSearch by remember(
        searchListState,
        ui.searchItems.size,
        ui.searchHasMore,
        ui.searching,
        ui.searchLoadingMore,
        ui.searchLoadMoreError
    ) {
        derivedStateOf {
            shouldLoadExploreSearchMore(
                resultCount = ui.searchItems.size,
                lastVisibleItemIndex = searchListState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index,
                hasMore = ui.searchHasMore,
                searching = ui.searching,
                loadingMore = ui.searchLoadingMore,
                loadMoreFailed = ui.searchLoadMoreError != null
            )
        }
    }

    fun exitPartsSelection() {
        partsSelectionMode = false
        selectedParts = emptySet()
    }

    LaunchedEffect(Unit) {
        if (ui.playlists.isEmpty()) vm.loadHighQuality()
    }

    LaunchedEffect(ui.selectedSearchSource, orderedSearchSources) {
        val targetPage = orderedSearchSources.indexOf(ui.selectedSearchSource)
            .takeIf { it >= 0 }
            ?: return@LaunchedEffect
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // 搜索类型切换后若当前音源不再支持该类型，回落到「全部聚合」
    LaunchedEffect(orderedSearchSources, ui.selectedSearchSource) {
        val selected = ui.selectedSearchSource
        if (
            selected != SearchSource.ALL &&
            selected != SearchSource.LINK_RECOGNITION &&
            selected !in orderedSearchSources
        ) {
            vm.setSearchSource(SearchSource.ALL)
        }
    }

    LaunchedEffect(pagerState.currentPage, orderedSearchSources, ui.selectedSearchSource) {
        // 搜索中固定使用「全部」聚合结果，翻页不再反向切换搜索渠道；
        // 只有空查询（浏览首页）时才允许滑动切源。
        if (searchQuery.isNotEmpty()) return@LaunchedEffect
        val currentSource = orderedSearchSources.getOrNull(pagerState.currentPage)
            ?: return@LaunchedEffect
        if (ui.selectedSearchSource != currentSource) {
            vm.setSearchSource(currentSource)
        }
        if (currentSource == SearchSource.YOUTUBE_MUSIC && ui.ytMusicPlaylists.isEmpty()) {
            vm.loadYtMusicPlaylists()
        }
    }

    // 国际化模式默认跳到 YouTube Music 标签
    LaunchedEffect(isInternational, youtubeEnabled) {
        if (isInternational && youtubeEnabled) {
            if (ui.selectedSearchSource != SearchSource.YOUTUBE_MUSIC) {
                vm.setSearchSource(SearchSource.YOUTUBE_MUSIC)
            }
            if (ui.ytMusicPlaylists.isEmpty()) {
                vm.loadYtMusicPlaylists()
            }
        }
    }

    // Tag keys for API calls
    val tagKeys = listOf(
        "tag_all", "tag_pop", "tag_soundtrack", "tag_chinese", "tag_nostalgia", "tag_rock", "tag_acg", "tag_western", "tag_fresh", "tag_night", "tag_children", "tag_folk", "tag_japanese", "tag_romantic",
        "tag_study", "tag_korean", "tag_work", "tag_electronic", "tag_cantonese", "tag_dance", "tag_sad", "tag_game", "tag_afternoon_tea", "tag_healing", "tag_rap", "tag_light_music"
    )

    // Translated tag labels for display
    val tagLabels = listOf(
        stringResource(R.string.tag_all), stringResource(R.string.tag_pop), stringResource(R.string.tag_soundtrack), stringResource(R.string.tag_chinese), stringResource(R.string.tag_nostalgia), stringResource(R.string.tag_rock), stringResource(R.string.tag_acg), stringResource(R.string.tag_western), stringResource(R.string.tag_fresh), stringResource(R.string.tag_night), stringResource(R.string.tag_children), stringResource(R.string.tag_folk), stringResource(R.string.tag_japanese), stringResource(R.string.tag_romantic),
        stringResource(R.string.tag_study), stringResource(R.string.tag_korean), stringResource(R.string.tag_work), stringResource(R.string.tag_electronic), stringResource(R.string.tag_cantonese), stringResource(R.string.tag_dance), stringResource(R.string.tag_sad), stringResource(R.string.tag_game), stringResource(R.string.tag_afternoon_tea), stringResource(R.string.tag_healing), stringResource(R.string.tag_rap), stringResource(R.string.tag_light_music)
    )

    // Initialize with default tag
    LaunchedEffect(Unit) {
        if (ui.selectedTag == "tag_all" && ui.playlists.isEmpty()) {
            vm.loadHighQuality("tag_all")
        }
    }

    var lastRecordedSearchKeyword by remember { mutableStateOf<String?>(null) }
    var pendingSearchHistoryRecord by remember { mutableStateOf<String?>(null) }

    fun queueExploreSearchRecord(query: String) {
        if (ui.selectedSearchSource == SearchSource.LINK_RECOGNITION) {
            return
        }
        val keyword = exploreSearchHistoryRecordKeyword(
            query = query,
            enabled = searchHistoryEnabled,
            history = searchHistory
        ) ?: return
        if (keyword.equals(lastRecordedSearchKeyword, ignoreCase = true)) {
            return
        }
        lastRecordedSearchKeyword = keyword
        pendingSearchHistoryRecord = keyword
    }

    LaunchedEffect(pendingSearchHistoryRecord) {
        val keyword = pendingSearchHistoryRecord ?: return@LaunchedEffect
        searchHistoryRepository.record(keyword)
        if (pendingSearchHistoryRecord == keyword) {
            pendingSearchHistoryRecord = null
        }
    }

    LaunchedEffect(
        searchQuery,
        effectiveSearchKeyword,
        searchHistoryEnabled,
        ui.selectedSearchSource,
        ui.selectedNeteaseSearchType,
        ui.selectedYouTubeMusicSearchType,
        ui.selectedChannelSearchType
    ) {
        if (searchQuery.isBlank()) {
            lastRecordedSearchKeyword = null
            pendingSearchHistoryRecord = null
            vm.search("")
            return@LaunchedEffect
        }
        delay(SEARCH_INPUT_DEBOUNCE_MS)
        val displayQuery = searchQuery.trim()
        if (
            ui.searchKeyword != effectiveSearchKeyword ||
            ui.searchDisplayQuery != displayQuery
        ) {
            vm.search(effectiveSearchKeyword, displayQuery = displayQuery)
        }
    }

    LaunchedEffect(ui.selectedSearchSource) {
        if (shouldClearExploreSearchQuery(previousSearchSource, ui.selectedSearchSource)) {
            onSearchQueryChange("")
        }
        previousSearchSource = ui.selectedSearchSource
    }

    val currentSearchScrollContextKey = remember(
        effectiveSearchKeyword,
        ui.selectedSearchSource,
        ui.selectedNeteaseSearchType,
        ui.selectedYouTubeMusicSearchType,
        ui.selectedChannelSearchType
    ) {
        exploreSearchScrollContextKey(
            keyword = effectiveSearchKeyword,
            source = ui.selectedSearchSource,
            neteaseSearchType = ui.selectedNeteaseSearchType,
            youtubeSearchType = ui.selectedYouTubeMusicSearchType,
            channelSearchType = ui.selectedChannelSearchType
        )
    }

    LaunchedEffect(
        currentSearchScrollContextKey,
        searchScrollContextKey
    ) {
        if (
            shouldResetExploreSearchScroll(
                previousContextKey = searchScrollContextKey,
                currentContextKey = currentSearchScrollContextKey
            )
        ) {
            searchListState.scrollToItem(0)
        }
        if (searchScrollContextKey != currentSearchScrollContextKey) {
            onSearchScrollContextKeyChange(currentSearchScrollContextKey)
        }
    }

    LaunchedEffect(
        shouldLoadMoreSearch,
        ui.searchItems.size,
        ui.selectedSearchSource,
        ui.selectedNeteaseSearchType,
        ui.selectedYouTubeMusicSearchType
    ) {
        if (shouldLoadMoreSearch) {
            vm.loadMoreSearchResults()
        }
    }

    fun submitExploreSearch(query: String = searchQuery) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return
        val keyword = if (ui.selectedSearchSource == SearchSource.LINK_RECOGNITION) {
            normalizedQuery
        } else {
            resolveExploreSearchKeyword(normalizedQuery, availableSearchHistory)
        }
        onSearchQueryChange(normalizedQuery)
        focusManager.clearFocus()
        vm.search(keyword, displayQuery = normalizedQuery)
        queueExploreSearchRecord(normalizedQuery)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        canScroll = {
            when {
                searchQuery.isNotEmpty() -> shouldAllowCollapsingTopAppBar(
                    canScrollForward = searchListState.canScrollForward,
                    canScrollBackward = searchListState.canScrollBackward,
                    collapsedFraction = topAppBarState.collapsedFraction
                )
                ui.selectedSearchSource == SearchSource.NETEASE ->
                    shouldAllowCollapsingTopAppBar(
                        canScrollForward = gridState.canScrollForward,
                        canScrollBackward = gridState.canScrollBackward,
                        collapsedFraction = topAppBarState.collapsedFraction
                    )
                ui.selectedSearchSource == SearchSource.YOUTUBE_MUSIC ->
                    shouldAllowCollapsingTopAppBar(
                        canScrollForward = youtubeGridState.canScrollForward,
                        canScrollBackward = youtubeGridState.canScrollBackward,
                        collapsedFraction = topAppBarState.collapsedFraction
                    )
                else -> shouldAllowCollapsingTopAppBar(
                    canScrollForward = false,
                    canScrollBackward = false,
                    collapsedFraction = topAppBarState.collapsedFraction
                )
            }
        }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        snackbarHost = {
            LyraSnackbarHost(
                hostState = snackbarHostState,
                bottomPadding = miniPlayerHeight
            )
        },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.action_search),
                        style = MeloxType.largeTitle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth()
                    .padding(horizontal = searchPanelHorizontalPadding, vertical = 8.dp)
            ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            onSearchQueryChange(it)
                        },
                        placeholder = {
                            Text(
                                stringResource(
                                    if (ui.selectedSearchSource == SearchSource.LINK_RECOGNITION) {
                                        R.string.explore_link_input_placeholder
                                    } else {
                                        R.string.search_keyword
                                    }
                                )
                            )
                        },
                        leadingIcon = {
                            // 先搜索，再筛选：没有结果前图标不可点；有结果后只列出真正有结果的渠道。
                            Box {
                                HapticIconButton(
                                    onClick = { showChannelMenu = true },
                                    enabled = canFilterSearchSources,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search_channel_select),
                                        tint = when {
                                            showChannelMenu -> MaterialTheme.colorScheme.primary
                                            canFilterSearchSources ->
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
                                            else ->
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                        }
                                    )
                                }
                                DropdownMenu(
                                    expanded = showChannelMenu && canFilterSearchSources,
                                    onDismissRequest = { showChannelMenu = false }
                                ) {
                                    filterableSearchSources.forEach { source ->
                                        val selected = source == ui.searchResultFilter
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = searchSourceLabel(source),
                                                    style = MeloxType.body,
                                                    color = if (selected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            },
                                            trailingIcon = {
                                                if (selected) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showChannelMenu = false
                                                vm.setSearchResultFilter(source)
                                                scope.launch {
                                                    if (
                                                        searchListState.layoutInfo.totalItemsCount > 0
                                                    ) {
                                                        searchListState.scrollToItem(0)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                HapticIconButton(onClick = {
                                    onSearchQueryChange("")
                                    vm.search("")
                                }) { Icon(Icons.Default.Clear, "Clear") }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            submitExploreSearch()
                        }),
                        singleLine = true,
                        shape = RoundedCornerShape(LyraRadius.Full),
                        textStyle = MeloxType.body,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    )
                    ExploreSearchHistoryRow(
                        history = visibleSearchHistory,
                        visible = shouldShowSearchHistory && searchQuery.isBlank(),
                        query = searchQuery,
                        onHistoryClick = { item -> submitExploreSearch(item) },
                        onClearHistory = {
                            lastRecordedSearchKeyword = null
                            pendingSearchHistoryRecord = null
                            scope.launch {
                                searchHistoryRepository.clear()
                            }
                        }
                    )
                    if (ui.selectedSearchSource == SearchSource.LINK_RECOGNITION) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.explore_link_input_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    ExploreSearchTypeBar(
                        source = searchTypeBarSource,
                        types = searchTypeBarTypes,
                        selectedType = selectedSearchType,
                        onSearchTypeClick = { type ->
                            // 已执行过搜索时（searchKeyword 非空），切换类型后用同一关键词自动重搜；
                            // 复用现有搜索入口，不新增搜索状态。query 为空/从未搜索时不触发。
                            val reSearchKeyword = resolveExploreReSearchKeyword(
                                executedKeyword = ui.searchKeyword,
                                previousType = selectedSearchType,
                                newType = type
                            )
                            vm.setSourceSearchType(ui.selectedSearchSource, type)
                            if (reSearchKeyword != null) {
                                submitExploreSearch(reSearchKeyword)
                            }
                        },
                        selectedAlpha = tagChipSelectedAlpha,
                        unselectedAlpha = tagChipUnselectedAlpha,
                        borderAlpha = tagChipBorderAlpha
                    )
                }

            HorizontalPager(
                state = pagerState,
                // 搜索结果页固定显示聚合结果，禁止左右滑动切渠道，避免误触发“先选渠道再搜索”。
                userScrollEnabled = searchQuery.isEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                if (searchQuery.isNotEmpty()) {
                    if (shouldRenderExploreSearchResults(page, pagerState.currentPage)) {
                        when {
                            ui.searching -> {
                                ExploreSearchSkeleton(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = miniPlayerHeight)
                                )
                            }
                            isExploreSearchError(
                                hasQuery = searchQuery.isNotEmpty(),
                                searching = ui.searching,
                                searchError = ui.searchError,
                                allSourcesFailed = ui.allSourcesFailed
                            ) -> {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(bottom = miniPlayerHeight),
                                    Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = LyraSpacing.XL)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = LocalNeriColors.current.error,
                                            modifier = Modifier.size(LyraIconSize.XLarge)
                                        )
                                        Spacer(Modifier.height(LyraSpacing.MD))
                                        Text(
                                            text = stringResource(R.string.explore_search_failed),
                                            style = LocalNeriTextStyles.current.body,
                                            color = LocalNeriColors.current.secondaryText
                                        )
                                        Spacer(Modifier.height(LyraSpacing.SM))
                                        LyraTextButton(
                                            onClick = {
                                                submitExploreSearch(
                                                    ui.searchKeyword.ifBlank { searchQuery }
                                                )
                                            }
                                        ) {
                                            Text(stringResource(R.string.explore_search_retry))
                                        }
                                    }
                                }
                            }
                            isExploreSearchEmpty(
                                hasQuery = searchQuery.isNotEmpty(),
                                searching = ui.searching,
                                hasError = ui.searchError != null,
                                hasResults = ui.searchItems.isNotEmpty()
                            ) -> {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(bottom = miniPlayerHeight),
                                    Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = LyraSpacing.XL)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = LocalNeriColors.current.tertiaryText,
                                            modifier = Modifier.size(LyraIconSize.XLarge)
                                        )
                                        Spacer(Modifier.height(LyraSpacing.MD))
                                        Text(
                                            text = stringResource(R.string.search_no_result),
                                            style = LocalNeriTextStyles.current.body,
                                            color = LocalNeriColors.current.secondaryText
                                        )
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    state = searchListState,
                                    contentPadding = PaddingValues(
                                        start = searchResultHorizontalPadding,
                                        end = searchResultHorizontalPadding,
                                        top = 8.dp,
                                        bottom = exploreSearchResultsBottomPadding(miniPlayerHeight)
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                val showArtistSectionHeader = shouldShowExploreArtistSectionHeader(
                                    searchType = selectedSearchType,
                                    hasArtistResults = ui.searchItems.any {
                                        it is ExploreSearchResult.Artist ||
                                            it is ExploreSearchResult.YouTubeCreator ||
                                            it is ExploreSearchResult.ChannelArtist
                                    },
                                    hasOtherTypeResults = ui.searchItems.any {
                                        it !is ExploreSearchResult.Artist &&
                                            it !is ExploreSearchResult.YouTubeCreator &&
                                            it !is ExploreSearchResult.ChannelArtist
                                    }
                                )
                                val firstArtistIndex = ui.searchItems.indexOfFirst {
                                    it is ExploreSearchResult.Artist ||
                                        it is ExploreSearchResult.YouTubeCreator ||
                                        it is ExploreSearchResult.ChannelArtist
                                }
                                if (
                                    shouldShowExploreSongSectionHeader(
                                        searchType = selectedSearchType,
                                        hasSongResults = ui.searchItems.any { it is ExploreSearchResult.Song },
                                        hasOtherTypeResults = ui.searchItems.any { it !is ExploreSearchResult.Song }
                                    )
                                ) {
                                    item(key = "explore:section:songs") {
                                        LyraSectionHeader(
                                            title = sourceSearchTypeLabel(SourceSearchType.SONG)
                                        )
                                    }
                                }
                                itemsIndexed(
                                    items = ui.searchItems,
                                    key = { _, item -> item.stableKey }
                                ) { index, item ->
                                    when (item) {
                                        is ExploreSearchResult.Song -> {
                                            val song = item.song
                                            val songListIndex = ui.searchResults.indexOfFirst {
                                                it.stableKey() == song.stableKey()
                                            }.takeIf { it >= 0 } ?: index
                                            val isFavoriteSong = favoriteSongKeys.contains(song.stableKey())
                                            SongRow(
                                                song = song,
                                                isFavorite = isFavoriteSong,
                                                favoriteActionEnabled = localPlaylistsReady,
                                                offlineMode = offlineMode,
                                                snackbarHostState = snackbarHostState,
                                                onClick = {
                                                    if (shouldShowBiliPartsPicker(song)) {
                                                        scope.launch {
                                                            try {
                                                                val info = vm.getVideoInfoByAvid(song.id)
                                                                if (info.pages.size <= 1) {
                                                                    onSongClick(ui.searchResults, songListIndex)
                                                                } else {
                                                                    partsInfo = info
                                                                    clickedSongCoverUrl = song.coverUrl ?: ""
                                                                    showPartsSheet = true
                                                                }
                                                            } catch (e: Exception) {
                                                                LyraLogger.e("ExploreScreen", composeResources.getString(R.string.search_error), e)
                                                            }
                                                        }
                                                    } else {
                                                        onSongClick(ui.searchResults, songListIndex)
                                                    }
                                                },
                                                onPlayNow = { onSongPlayPreservingQueue(song) },
                                                onPlayNext = { onSongPlayNext(song) },
                                                onAddToQueueEnd = { onSongAddToQueueEnd(song) },
                                                onDownload = {
                                                    GlobalDownloadManager.startDownload(context, song)
                                                    scope.launch {
                                                        snackbarHostState.showNeriSnackbar(
                                                            composeResources.getString(
                                                                R.string.download_starting,
                                                                song.displayName()
                                                            )
                                                        )
                                                    }
                                                },
                                                onToggleFavorite = {
                                                    if (localPlaylistsReady) {
                                                        scope.launchLocalPlaylistMutation(
                                                            "toggleFavoriteFromExplore"
                                                        ) {
                                                            val isFavoriteAtAction = FavoritesPlaylist
                                                                .firstOrNull(repo.playlists.value, context)
                                                                ?.songs
                                                                ?.any { it.sameIdentityAs(song) } == true
                                                            if (isFavoriteAtAction) {
                                                                repo.removeFromFavorites(song)
                                                            } else {
                                                                repo.addToFavorites(song)
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        is ExploreSearchResult.Playlist -> {
                                            NeteasePlaylistSearchRow(
                                                playlist = item.playlist,
                                                offlineMode = offlineMode,
                                                onClick = { onPlay(item.playlist) }
                                            )
                                        }
                                        is ExploreSearchResult.YouTubePlaylist -> {
                                            YouTubePlaylistSearchRow(
                                                playlist = item.playlist,
                                                offlineMode = offlineMode,
                                                onClick = { onYouTubeMusicPlaylistClick(item.playlist) }
                                            )
                                        }
                                        is ExploreSearchResult.BilibiliPlaylist -> {
                                            BiliPlaylistSearchRow(
                                                playlist = item.playlist,
                                                offlineMode = offlineMode,
                                                onClick = { onBiliPlaylistClick(item.playlist) }
                                            )
                                        }
                                        is ExploreSearchResult.Artist -> {
                                            Column {
                                                if (showArtistSectionHeader && index == firstArtistIndex) {
                                                    LyraSectionHeader(
                                                        title = sourceSearchTypeLabel(SourceSearchType.ARTIST)
                                                    )
                                                }
                                                NeteaseArtistSearchRow(
                                                    result = item.result,
                                                    offlineMode = offlineMode,
                                                    onClick = { onNeteaseArtistClick(item.result.artist) }
                                                )
                                            }
                                        }
                                        is ExploreSearchResult.YouTubeCreator -> {
                                            Column {
                                                if (showArtistSectionHeader && index == firstArtistIndex) {
                                                    LyraSectionHeader(
                                                        title = sourceSearchTypeLabel(SourceSearchType.ARTIST)
                                                    )
                                                }
                                                YouTubeCreatorSearchRow(
                                                    creator = item.creator,
                                                    offlineMode = offlineMode,
                                                    onClick = { onYouTubeCreatorClick(item.creator) }
                                                )
                                            }
                                        }
                                        is ExploreSearchResult.Notice -> {
                                            ExploreSearchNoticeRow(item)
                                        }
                                        is ExploreSearchResult.ChannelArtist -> {
                                            Column {
                                                if (showArtistSectionHeader && index == firstArtistIndex) {
                                                    LyraSectionHeader(
                                                        title = sourceSearchTypeLabel(SourceSearchType.ARTIST)
                                                    )
                                                }
                                                ChannelArtistSearchRow(
                                                    artist = item.artist,
                                                    offlineMode = offlineMode,
                                                    onClick = {
                                                        if (item.sourceId == MusicSourceIds.TENCENT) {
                                                            onTencentArtistClick(item.artist)
                                                        } else {
                                                            // 其他渠道没有歌手详情，退化为按歌手名搜歌
                                                            vm.setChannelSearchType(ChannelSearchType.SONG)
                                                            onSearchQueryChange(item.artist.name)
                                                            vm.search(item.artist.name)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        is ExploreSearchResult.ChannelPlaylist -> {
                                            ChannelPlaylistSearchRow(
                                                playlist = item.playlist,
                                                offlineMode = offlineMode,
                                                onClick = {
                                                    channelPlaylistSheet = ChannelPlaylistSheetState(
                                                        playlistId = item.playlist.id,
                                                        source = ui.selectedSearchSource,
                                                        name = item.playlist.name
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                if (ui.searchLoadingMore) {
                                    item(key = "search-loading-more") {
                                        SearchLoadingMoreRow()
                                    }
                                } else if (ui.searchLoadMoreError != null) {
                                    item(key = "search-load-more-error") {
                                        SearchLoadMoreErrorRow(
                                            error = ui.searchLoadMoreError!!,
                                            onRetry = vm::loadMoreSearchResults
                                        )
                                    }
                                }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize())
                    }
                } else {
                    // 无关键词时只保留上方的搜索历史，不再展示分类与歌单推荐。
                    Box(Modifier.fillMaxSize())
                }
            }
        }
    }

    if (showPartsSheet && partsInfo != null) {
        val currentPartsInfo = partsInfo!!
        BackHandler(enabled = partsSelectionMode) { exitPartsSelection() }
        ModalBottomSheet(
            onDismissRequest = {
                showPartsSheet = false
                exitPartsSelection()
            },
            sheetState = partsSheetState,
            sheetGesturesEnabled = false
        ) {
            Column(
                Modifier
                    .bottomSheetScrollGuard()
                    .padding(bottom = 12.dp)
            ) {
                AnimatedVisibility(visible = partsSelectionMode) {
                    val allSelected = selectedParts.size == currentPartsInfo.pages.size
                    TopAppBar(
                    title = {
                        Text(
                            pluralStringResource(
                                R.plurals.common_selected_count,
                                selectedParts.size,
                                selectedParts.size
                            )
                        )
                    },
                        navigationIcon = {
                            HapticIconButton(onClick = { exitPartsSelection() }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.explore_exit_selection))
                            }
                        },
                        actions = {
                            HapticIconButton(onClick = {
                                selectedParts = if (allSelected) {
                                    emptySet()
                                } else {
                                    currentPartsInfo.pages.map { it.page }.toSet()
                                }
                            }) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    contentDescription = if (allSelected) stringResource(R.string.explore_deselect_all) else stringResource(R.string.explore_select_all)
                                )
                            }
                            HapticIconButton(
                                onClick = {
                                    if (selectedParts.isNotEmpty()) {
                                        scope.launch { partsSheetState.hide() }.invokeOnCompletion {
                                            if (!partsSheetState.isVisible) {
                                                showPartsSheet = false
                                                showExportSheet = true
                                            }
                                        }
                                    }
                                },
                                enabled = selectedParts.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = stringResource(R.string.explore_export_to_playlist))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }

                AnimatedVisibility(visible = !partsSelectionMode) {
                    Text(
                        text = currentPartsInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider()

                LazyColumn {
                    itemsIndexed(currentPartsInfo.pages, key = { _, page -> page.page }) { index, page ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (partsSelectionMode) {
                                            selectedParts = if (selectedParts.contains(page.page)) {
                                                selectedParts - page.page
                                            } else {
                                                selectedParts + page.page
                                            }
                                        } else {
                                            onPlayParts(currentPartsInfo, index, clickedSongCoverUrl)
                                            scope.launch { partsSheetState.hide() }.invokeOnCompletion {
                                                if (!partsSheetState.isVisible) showPartsSheet = false
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!partsSelectionMode) {
                                            partsSelectionMode = true
                                            selectedParts = setOf(page.page)
                                        }
                                    }
                                )
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (partsSelectionMode) {
                                Checkbox(
                                    checked = selectedParts.contains(page.page),
                                    onCheckedChange = {
                                        selectedParts = if (selectedParts.contains(page.page)) {
                                            selectedParts - page.page
                                        } else {
                                            selectedParts + page.page
                                        }
                                    }
                                )
                                Spacer(Modifier.width(16.dp))
                            }
                            Text(
                                text = "P${page.page}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                text = page.part,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExportSheet) {
        PlaylistExportSheet(
            title = stringResource(R.string.playlist_export_to_local),
            playlists = allLocalPlaylists.filterNot {
                LocalFilesPlaylist.isSystemPlaylist(it, context)
            },
            selectedCount = selectedParts.size,
            onDismissRequest = { showExportSheet = false },
            onCreateAndExport = { name ->
                val songs = partsInfo!!.pages
                    .filter { selectedParts.contains(it.page) }
                    .map { page -> vm.toSongItem(page, partsInfo!!, clickedSongCoverUrl) }
                scope.launchLocalPlaylistMutation(
                    operation = "createPlaylistFromExplore",
                    onResult = { result ->
                        scope.showPlaylistBatchExportCreatedResult(
                            context = context,
                            snackbarHostState = snackbarHostState,
                            repository = repo,
                            result = result
                        )
                    }
                ) {
                    repo.createPlaylistWithSongs(name, songs)
                }
                exitPartsSelection()
            },
            onExportToPlaylist = { playlist ->
                val songs = partsInfo!!.pages
                    .filter { selectedParts.contains(it.page) }
                    .map { page -> vm.toSongItem(page, partsInfo!!, clickedSongCoverUrl) }
                scope.launchLocalPlaylistMutation(
                    operation = "exportSongsFromExplore",
                    onResult = { result ->
                        scope.showPlaylistBatchExportAddedResult(
                            context = context,
                            snackbarHostState = snackbarHostState,
                            repository = repo,
                            targetPlaylistId = playlist.id,
                            targetPlaylistName = playlist.name,
                            result = result
                        )
                    }
                ) {
                    repo.addSongsToPlaylistWithResult(playlist.id, songs)
                }
                exitPartsSelection()
            }
        )
    }

    val playlistSheet = channelPlaylistSheet
    if (playlistSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { channelPlaylistSheet = null },
            sheetState = channelPlaylistSheetState
        ) {
            Text(
                text = playlistSheet.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider()
            when {
                playlistSheet.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                playlistSheet.songs.isNotEmpty() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 24.dp + miniPlayerHeight
                        )
                    ) {
                        itemsIndexed(
                            items = playlistSheet.songs,
                            key = { _, song -> song.stableKey() }
                        ) { index, song ->
                            SongRow(
                                song = song,
                                isFavorite = favoriteSongKeys.contains(song.stableKey()),
                                favoriteActionEnabled = localPlaylistsReady,
                                offlineMode = offlineMode,
                                snackbarHostState = snackbarHostState,
                                onClick = {
                                    onSongClick(playlistSheet.songs, index)
                                    channelPlaylistSheet = null
                                },
                                onPlayNow = { onSongPlayPreservingQueue(song) },
                                onPlayNext = { onSongPlayNext(song) },
                                onAddToQueueEnd = { onSongAddToQueueEnd(song) },
                                onDownload = {
                                    GlobalDownloadManager.startDownload(context, song)
                                    scope.launch {
                                        snackbarHostState.showNeriSnackbar(
                                            composeResources.getString(
                                                R.string.download_starting,
                                                song.displayName()
                                            )
                                        )
                                    }
                                },
                                onToggleFavorite = {
                                    if (localPlaylistsReady) {
                                        scope.launchLocalPlaylistMutation(
                                            "toggleFavoriteFromExplore"
                                        ) {
                                            val isFavoriteAtAction = FavoritesPlaylist
                                                .firstOrNull(repo.playlists.value, context)
                                                ?.songs
                                                ?.any { it.sameIdentityAs(song) } == true
                                            if (isFavoriteAtAction) {
                                                repo.removeFromFavorites(song)
                                            } else {
                                                repo.addToFavorites(song)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.search_no_result),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    )
                }
            }
        }
    }
}

private data class ChannelPlaylistSheetState(
    val playlistId: String,
    val source: SearchSource,
    val name: String,
    val loading: Boolean = true,
    val songs: List<SongItem> = emptyList(),
    val failed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreOfflineContent(topAppBarState: TopAppBarState) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        canScroll = {
            shouldAllowCollapsingTopAppBar(
                canScrollForward = false,
                canScrollBackward = false,
                collapsedFraction = topAppBarState.collapsedFraction
            )
        }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.action_search),
                        style = MeloxType.largeTitle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 32.dp, end = 32.dp, bottom = miniPlayerHeight),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.offline_mode_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.explore_offline_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreSearchHistoryRow(
    history: List<String>,
    visible: Boolean,
    query: String,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    AnimatedVisibility(visible = visible && history.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.search_history),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (query.isBlank()) {
                    HapticTextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
            }
            val historyListState = rememberLazyListState()
            val showStartFade by remember(historyListState) {
                derivedStateOf { historyListState.canScrollBackward }
            }
            val showEndFade by remember(historyListState) {
                derivedStateOf { historyListState.canScrollForward }
            }
            LazyRow(
                state = historyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .exploreHorizontalEdgeFade(
                        showStartFade = showStartFade,
                        showEndFade = showEndFade
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(history) { _, item ->
                    ExploreSearchHistoryChip(
                        text = item,
                        onClick = { onHistoryClick(item) }
                    )
                }
            }
        }
    }
}

internal fun Modifier.exploreHorizontalEdgeFade(
    showStartFade: Boolean,
    showEndFade: Boolean,
    fadeWidth: Dp = 28.dp
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val resolvedFadeWidth = fadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (resolvedFadeWidth <= 0f) return@drawWithContent
        if (showStartFade) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = resolvedFadeWidth
                ),
                topLeft = Offset.Zero,
                size = Size(resolvedFadeWidth, size.height),
                blendMode = BlendMode.DstIn
            )
        }
        if (showEndFade) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - resolvedFadeWidth,
                    endX = size.width
                ),
                topLeft = Offset(size.width - resolvedFadeWidth, 0f),
                size = Size(resolvedFadeWidth, size.height),
                blendMode = BlendMode.DstIn
            )
        }
    }

@Composable
private fun ExploreSearchHistoryChip(
    text: String,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
    ExploreGlassPillSurface(
        fallbackColor = containerColor,
        tintColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun filteredExploreSearchHistory(history: List<String>): List<String> {
    return history.take(EXPLORE_HISTORY_DISPLAY_LIMIT)
}

internal fun shouldShowExploreSearchHistory(
    history: List<String>,
    contentScrolled: Boolean
): Boolean {
    return history.isNotEmpty() && !contentScrolled
}

internal fun shouldShowExploreNeteaseSearchTypeBar(
    selectedSearchSource: SearchSource,
    contentScrolled: Boolean
): Boolean {
    return exploreSearchTypeBarSource(selectedSearchSource, contentScrolled) ==
        SearchSource.NETEASE
}

internal fun shouldShowExploreYouTubeSearchTypeBar(
    selectedSearchSource: SearchSource,
    contentScrolled: Boolean
): Boolean {
    return exploreSearchTypeBarSource(selectedSearchSource, contentScrolled) ==
        SearchSource.YOUTUBE_MUSIC
}

/**
 * 是否显示某音源的搜索类型栏：由已注册 Provider 提供的类型数量决定（>1 才显示）。
 * 纯函数，供测试使用；[ExploreScreen] 内使用等价的 [ExploreUiState.searchTypesFor]。
 */
internal fun exploreSearchTypeBarSource(
    selectedSearchSource: SearchSource,
    contentScrolled: Boolean
): SearchSource? {
    if (contentScrolled) return null
    if (selectedSearchSource == SearchSource.ALL) return selectedSearchSource
    val provider = selectedSearchSource.musicSourceId
        ?.let(SourceSearchRegistry::providerFor)
        ?: return null
    val typeCount = EXPLORE_SEARCH_TYPE_ORDER.count { provider.supports(it) }
    return selectedSearchSource.takeIf { typeCount > 1 }
}

internal fun isExploreSearchTypeBarSourceSwap(
    initialSource: SearchSource?,
    targetSource: SearchSource?
): Boolean = initialSource != null && targetSource != null && initialSource != targetSource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExploreSearchTypeBar(
    source: SearchSource?,
    types: List<SourceSearchType>,
    selectedType: SourceSearchType,
    onSearchTypeClick: (SourceSearchType) -> Unit,
    selectedAlpha: Float,
    unselectedAlpha: Float,
    borderAlpha: Float
) {
    AnimatedContent(
        targetState = source,
        modifier = Modifier.testTag(EXPLORE_SEARCH_TYPE_BAR_CONTAINER_TAG),
        transitionSpec = {
            val sourceSwap = isExploreSearchTypeBarSourceSwap(initialState, targetState)
            val enter = if (sourceSwap) {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_ENTER_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + slideInVertically(
                    initialOffsetY = { height ->
                        if (targetState == SearchSource.YOUTUBE_MUSIC) {
                            height / EXPLORE_SEARCH_TYPE_BAR_SLIDE_DIVISOR
                        } else {
                            -height / EXPLORE_SEARCH_TYPE_BAR_SLIDE_DIVISOR
                        }
                    },
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_ENTER_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            } else {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_ENTER_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_SIZE_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            val exit = if (sourceSwap) {
                fadeOut(
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_EXIT_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + slideOutVertically(
                    targetOffsetY = { height ->
                        if (targetState == SearchSource.YOUTUBE_MUSIC) {
                            -height / EXPLORE_SEARCH_TYPE_BAR_SLIDE_DIVISOR
                        } else {
                            height / EXPLORE_SEARCH_TYPE_BAR_SLIDE_DIVISOR
                        }
                    },
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_EXIT_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            } else {
                fadeOut(
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_EXIT_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_SIZE_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            enter togetherWith exit using SizeTransform(
                clip = true,
                sizeAnimationSpec = { _, _ ->
                    tween(
                        durationMillis = EXPLORE_SEARCH_TYPE_BAR_SIZE_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                }
            )
        },
        label = "explore_search_type_bar"
    ) { displayedSource ->
        if (displayedSource == null) {
            return@AnimatedContent
        }
        val barTag = when (displayedSource) {
            SearchSource.NETEASE -> EXPLORE_NETEASE_SEARCH_TYPE_BAR_TAG
            SearchSource.YOUTUBE_MUSIC -> EXPLORE_YOUTUBE_SEARCH_TYPE_BAR_TAG
            else -> EXPLORE_CHANNEL_SEARCH_TYPE_BAR_TAG
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(barTag),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(types) { _, type ->
                ExploreTagChip(
                    label = sourceSearchTypeLabel(type),
                    icon = sourceSearchTypeIcon(type),
                    selected = selectedType == type,
                    onClick = {
                        if (source == displayedSource) {
                            onSearchTypeClick(type)
                        }
                    },
                    selectedAlpha = selectedAlpha,
                    unselectedAlpha = unselectedAlpha,
                    borderAlpha = borderAlpha
                )
            }
        }
    }
}

/** 搜索类型子标签文案（与平台无关）。 */
@Composable
private fun sourceSearchTypeLabel(type: SourceSearchType): String = when (type) {
    SourceSearchType.SONG -> stringResource(R.string.explore_search_type_song)
    SourceSearchType.ARTIST -> stringResource(R.string.explore_search_type_artist)
    SourceSearchType.ALBUM -> stringResource(R.string.explore_search_type_album)
    SourceSearchType.PLAYLIST -> stringResource(R.string.explore_search_type_playlist)
    SourceSearchType.RANKING -> stringResource(R.string.explore_search_type_ranking)
}

/** 搜索类型子标签图标（与平台无关）。 */
private fun sourceSearchTypeIcon(type: SourceSearchType): ImageVector = when (type) {
    SourceSearchType.SONG -> Icons.Outlined.MusicNote
    SourceSearchType.ARTIST -> Icons.Filled.AccountCircle
    SourceSearchType.ALBUM -> Icons.Filled.Album
    SourceSearchType.PLAYLIST -> Icons.AutoMirrored.Outlined.QueueMusic
    SourceSearchType.RANKING -> Icons.Filled.BarChart
}

@Composable
private fun channelSearchTypeLabel(type: ChannelSearchType): String {
    return when (type) {
        ChannelSearchType.SONG -> stringResource(R.string.explore_search_type_song)
        ChannelSearchType.ARTIST -> stringResource(R.string.explore_search_type_artist)
        ChannelSearchType.PLAYLIST -> stringResource(R.string.explore_search_type_playlist)
    }
}

private fun channelSearchTypeIcon(type: ChannelSearchType): ImageVector {
    return when (type) {
        ChannelSearchType.SONG -> Icons.Outlined.MusicNote
        ChannelSearchType.ARTIST -> Icons.Filled.AccountCircle
        ChannelSearchType.PLAYLIST -> Icons.AutoMirrored.Outlined.QueueMusic
    }
}

private const val EXPLORE_HISTORY_DISPLAY_LIMIT = 15
private const val EXPLORE_SEARCH_TYPE_BAR_ENTER_DURATION_MS = 180
private const val EXPLORE_SEARCH_TYPE_BAR_EXIT_DURATION_MS = 140
private const val EXPLORE_SEARCH_TYPE_BAR_SIZE_DURATION_MS = 220
private const val EXPLORE_SEARCH_TYPE_BAR_SLIDE_DIVISOR = 5
internal const val EXPLORE_SEARCH_TYPE_BAR_CONTAINER_TAG = "explore_search_type_bar"
internal const val EXPLORE_CHANNEL_SEARCH_TYPE_BAR_TAG = "explore_channel_search_type_bar"
internal const val EXPLORE_NETEASE_SEARCH_TYPE_BAR_TAG = "explore_netease_search_type_bar"
internal const val EXPLORE_YOUTUBE_SEARCH_TYPE_BAR_TAG = "explore_youtube_search_type_bar"

/** 各渠道的默认热词：没有输入关键词时用来铺满页面，点一下就直接搜 */
internal val ExploreChannelHotKeywords: Map<SearchSource, List<String>> = mapOf(
    SearchSource.TENCENT to listOf(
        "热歌", "新歌", "经典老歌", "粤语", "国风", "纯音乐", "伤感", "摇滚", "民谣", "电子", "ACG", "影视原声"
    ),
    SearchSource.KUGOU to listOf(
        "热歌榜", "新歌", "经典老歌", "粤语", "网络歌曲", "纯音乐", "伤感", "摇滚", "民谣", "DJ", "ACG", "影视原声"
    ),
    SearchSource.MIGU to listOf(
        "热歌", "新歌", "经典老歌", "国风", "民歌", "纯音乐", "轻音乐", "伤感", "摇滚", "民谣", "影视原声", "儿歌"
    ),
    SearchSource.BILIBILI to listOf(
        "音乐", "纯音乐", "ACG", "古风", "翻唱", "钢琴", "吉他", "合集", "背景音乐", "粤语", "日语", "摇滚"
    )
)

/** 各渠道的默认热门歌手 */
internal val ExploreChannelHotArtists: Map<SearchSource, List<String>> = mapOf(
    SearchSource.TENCENT to listOf(
        "周杰伦", "林俊杰", "邓紫棋", "陈奕迅", "薛之谦", "五月天", "毛不易", "张学友", "王菲", "李荣浩"
    ),
    SearchSource.KUGOU to listOf(
        "周杰伦", "林俊杰", "邓紫棋", "陈奕迅", "薛之谦", "张杰", "五月天", "汪苏泷", "李荣浩", "张学友"
    ),
    SearchSource.MIGU to listOf(
        "周杰伦", "林俊杰", "邓紫棋", "陈奕迅", "毛不易", "五月天", "王菲", "张学友", "刘德华", "宋祖英"
    ),
    SearchSource.BILIBILI to listOf(
        "周杰伦", "罗大佑", "李荣浩", "五月天", "双笙", "银临", "周深", "邓紫棋"
    )
)

@Composable
private fun ExploreChannelDefaultContent(
    source: SearchSource,
    onKeywordClick: (String) -> Unit,
    isTabletLayout: Boolean = false
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val horizontalPadding = if (isTabletLayout) 56.dp else 16.dp
    val hotKeywords = ExploreChannelHotKeywords[source].orEmpty()
    val hotArtists = ExploreChannelHotArtists[source].orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, bottom = miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (hotKeywords.isNotEmpty()) {
            ExploreChannelKeywordRow(
                title = stringResource(R.string.explore_hot_search),
                keywords = hotKeywords,
                horizontalPadding = horizontalPadding,
                onKeywordClick = onKeywordClick
            )
        }
        if (hotArtists.isNotEmpty()) {
            ExploreChannelKeywordRow(
                title = stringResource(R.string.explore_hot_artist),
                keywords = hotArtists,
                horizontalPadding = horizontalPadding,
                onKeywordClick = onKeywordClick
            )
        }
        Text(
            text = stringResource(R.string.explore_channel_search_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
    }
}

@Composable
private fun ExploreChannelKeywordRow(
    title: String,
    keywords: List<String>,
    horizontalPadding: Dp,
    onKeywordClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(keywords) { _, keyword ->
                ExploreSearchHistoryChip(
                    text = keyword,
                    onClick = { onKeywordClick(keyword) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun NeteaseDefaultContent(
    gridState: LazyGridState,
    ui: ExploreUiState,
    tagKeys: List<String>,
    tagLabels: List<String>,
    favoriteKeys: Set<String>,
    vm: ExploreViewModel,
    onPlay: (PlaylistSummary) -> Unit,
    tagChipSelectedAlpha: Float,
    tagChipUnselectedAlpha: Float,
    tagChipBorderAlpha: Float,
    isTabletLayout: Boolean = false
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val gridHorizontalPadding = if (isTabletLayout) 56.dp else 16.dp
    val gridMinCellSize = if (isTabletLayout) 170.dp else 150.dp
    val gridSpacing = if (isTabletLayout) 16.dp else 12.dp
    val tagListState = rememberLazyListState()
    val showTagStartFade by remember(tagListState) {
        derivedStateOf { tagListState.canScrollBackward }
    }
    val showTagEndFade by remember(tagListState) {
        derivedStateOf { tagListState.canScrollForward }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(gridMinCellSize),
        verticalArrangement = Arrangement.spacedBy(gridSpacing),
        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
        contentPadding = PaddingValues(
            start = gridHorizontalPadding,
            end = gridHorizontalPadding,
            top = 16.dp,
            bottom = 16.dp + miniPlayerHeight
        ),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                val displayKeys = tagKeys
                val displayLabels = tagLabels
                LazyRow(
                    state = tagListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .exploreHorizontalEdgeFade(
                            showStartFade = showTagStartFade,
                            showEndFade = showTagEndFade
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayKeys) { index, tagKey ->
                        val selected = (ui.selectedTag == tagKey)
                        ExploreTagChip(
                            label = displayLabels[index],
                            selected = selected,
                            onClick = { if (!selected) vm.loadHighQuality(tagKey) },
                            selectedAlpha = tagChipSelectedAlpha,
                            unselectedAlpha = tagChipUnselectedAlpha,
                            borderAlpha = tagChipBorderAlpha
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (ui.loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        if (ui.playlists.isNotEmpty()) {
            items(items = ui.playlists, key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    isFavorite = favoriteKeys.contains("netease:${playlist.id}"),
                    onClick = { onPlay(playlist) }
                )
            }
        } else if (ui.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (ui.error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(ui.error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ExploreTagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedAlpha: Float,
    unselectedAlpha: Float,
    borderAlpha: Float,
    icon: ImageVector? = null
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = selectedAlpha)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = unselectedAlpha)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.secondary.copy(alpha = borderAlpha)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
    }

    ExploreGlassPillSurface(
        fallbackColor = containerColor,
        tintColor = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ExploreGlassPillSurface(
    fallbackColor: Color,
    tintColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.ExploreTag,
            shape = ExplorePillShape,
            fallbackColor = fallbackColor,
            tintColor = tintColor
        ) {
            Surface(
                modifier = Modifier
                    .clip(ExplorePillShape)
                    .indication(interactionSource, ripple()),
                shape = ExplorePillShape,
                color = Color.Transparent,
                contentColor = contentColor,
                border = border,
                content = content
            )
        }
    }
}

/**
 * 搜索结果 Loading 骨架：结构接近真实结果行（歌曲行 / 集合行），避免整页只有一个转圈。
 * 纯展示、复用 Design System 语义色；不参与任何搜索状态。
 */
@Composable
private fun ExploreSearchSkeleton(modifier: Modifier = Modifier) {
    val barColor = LocalNeriColors.current.elevatedSurface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LyraSpacing.LG, vertical = LyraSpacing.SM)
    ) {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = LyraSpacing.SM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(LyraRadius.Medium))
                        .background(barColor)
                )
                Spacer(Modifier.width(LyraSpacing.MD))
                Column(Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(LyraRadius.Small))
                            .background(barColor)
                    )
                    Spacer(Modifier.height(LyraSpacing.SM))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(LyraRadius.Small))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun NeteasePlaylistSearchRow(
    playlist: PlaylistSummary,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    LinkedCollectionRow(
        title = playlist.name,
        subtitle = stringResource(
            R.string.playlist_play_count_format,
            formatPlayCount(context, playlist.playCount),
            playlist.trackCount
        ),
        coverUrl = playlist.picUrl,
        offlineMode = offlineMode,
        fallbackIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun BiliPlaylistSearchRow(
    playlist: BiliPlaylist,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    LinkedCollectionRow(
        title = playlist.title,
        subtitle = listOfNotNull(
            playlist.subtitle.takeIf { it.isNotBlank() },
            pluralStringResource(
                R.plurals.bili_content_count,
                playlist.count,
                playlist.count
            )
        ).joinToString(" · "),
        coverUrl = playlist.coverUrl,
        offlineMode = offlineMode,
        fallbackIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun YouTubePlaylistSearchRow(
    playlist: YouTubeMusicPlaylist,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    LinkedCollectionRow(
        title = playlist.title,
        subtitle = listOfNotNull(
            playlist.subtitle.takeIf { it.isNotBlank() },
            pluralStringResource(
                R.plurals.count_songs_format,
                playlist.trackCount,
                playlist.trackCount
            ).takeIf { playlist.trackCount > 0 }
        ).joinToString(" · "),
        coverUrl = playlist.coverUrl,
        offlineMode = offlineMode,
        fallbackIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun NeteaseArtistSearchRow(
    result: NeteaseSearchArtistResult,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    LinkedCollectionRow(
        title = result.artist.name,
        subtitle = listOf(
            pluralStringResource(
                R.plurals.artist_song_count,
                result.musicSize,
                result.musicSize
            ),
            pluralStringResource(
                R.plurals.artist_album_count,
                result.albumSize,
                result.albumSize
            )
        ).joinToString(" · "),
        coverUrl = result.picUrl,
        offlineMode = offlineMode,
        circularCover = true,
        fallbackIcon = {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun ChannelArtistSearchRow(
    artist: CustomChannelArtist,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    LinkedCollectionRow(
        title = artist.name,
        subtitle = stringResource(R.string.explore_search_type_artist),
        coverUrl = artist.coverUrl,
        offlineMode = offlineMode,
        circularCover = true,
        fallbackIcon = {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun ChannelPlaylistSearchRow(
    playlist: CustomChannelPlaylist,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val subtitle = buildList {
        if (playlist.songCount > 0) {
            add(
                pluralStringResource(
                    R.plurals.explore_song_count,
                    playlist.songCount,
                    playlist.songCount
                )
            )
        }
        playlist.creator?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString(" · ")
    LinkedCollectionRow(
        title = playlist.name,
        subtitle = subtitle.ifBlank { stringResource(R.string.explore_search_type_playlist) },
        coverUrl = playlist.coverUrl,
        offlineMode = offlineMode,
        fallbackIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun YouTubeCreatorSearchRow(
    creator: YouTubeMusicCreatorSummary,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    LinkedCollectionRow(
        title = creator.title,
        subtitle = creator.subtitle.ifBlank {
            stringResource(R.string.explore_search_type_creator)
        },
        coverUrl = creator.coverUrl,
        offlineMode = offlineMode,
        circularCover = true,
        fallbackIcon = {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun LinkedCollectionRow(
    title: String,
    subtitle: String,
    coverUrl: String?,
    offlineMode: Boolean,
    fallbackIcon: @Composable () -> Unit,
    circularCover: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverShape = if (circularCover) CircleShape else RoundedCornerShape(6.dp)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clickable {
                    context.performHapticFeedback()
                    onClick()
                }
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(coverShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = fastScrollableImageRequest(
                            context = context,
                            data = coverUrl,
                            sizePx = 160,
                            offlineMode = offlineMode
                        ),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(coverShape)
                    )
                } else {
                    fallbackIcon()
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
                )
            }
        }
        ExploreInsetDivider()
    }
}

@Composable
private fun ExploreSearchNoticeRow(item: ExploreSearchResult.Notice) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = item.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchLoadingMoreRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SearchLoadMoreErrorRow(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        HapticTextButton(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
internal fun SongRow(
    song: SongItem,
    isFavorite: Boolean,
    favoriteActionEnabled: Boolean,
    offlineMode: Boolean,
    snackbarHostState: SnackbarHostState,
    onClick: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueueEnd: () -> Unit,
    onDownload: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val coverUrl = rememberSongDisplayCoverUrl(song)
    var showMoreMenu by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clickable {
                    context.performHapticFeedback()
                    onClick()
                }
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = fastScrollableImageRequest(
                    context = context,
                    data = coverUrl,
                    sizePx = 192,
                    offlineMode = offlineMode
                ),
                contentDescription = song.displayName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            )
            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = song.displayName(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = listOfNotNull(
                        song.displayArtist().takeIf { it.isNotBlank() },
                        song.displayAlbum(context).takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
                )
            }

            if (song.durationMs > 0L) {
                Text(
                    text = formatDuration(song.durationMs),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
                )
            }

            Spacer(Modifier.width(4.dp))
            Box {
                HapticIconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_actions),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.search_result_play_keep_queue)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        context.performHapticFeedback()
                        onPlayNow()
                        showMoreMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.local_playlist_play_next)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        context.performHapticFeedback()
                        onPlayNext()
                        showMoreMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.search_result_add_to_current_queue)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        context.performHapticFeedback()
                        onAddToQueueEnd()
                        showMoreMenu = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isFavorite) {
                                stringResource(R.string.favorite_remove)
                            } else {
                                stringResource(R.string.favorite_add)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = null
                        )
                    },
                    enabled = favoriteActionEnabled,
                    onClick = {
                        context.performHapticFeedback()
                        onToggleFavorite()
                        showMoreMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.download_to_local)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        context.performHapticFeedback()
                        onDownload()
                        showMoreMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy_song_info)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        scope.launch {
                            val messageRes = when (
                                val result = clipboard.copyPlainTextSafely(
                                    label = "text",
                                    text = buildExploreSongInfo(song)
                                )
                            ) {
                                is ClipboardCopyResult.Copied -> if (result.wasTruncated) {
                                    R.string.toast_copy_truncated
                                } else {
                                    R.string.toast_copied
                                }
                                ClipboardCopyResult.TransactionTooLarge -> R.string.toast_copy_failed
                            }
                            snackbarHostState.showNeriSnackbar(
                                composeResources.getString(messageRes)
                            )
                        }
                        showMoreMenu = false
                    }
                )
                }
            }
        }
        ExploreInsetDivider()
    }
}

@Composable
private fun ExploreInsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 18.dp),
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
    )
}

internal fun buildExploreSongInfo(song: SongItem): String {
    return "${song.displayName()}-${song.displayArtist()}"
}

@Composable
private fun YouTubeMusicExploreContent(
    ui: ExploreUiState,
    vm: ExploreViewModel,
    onClick: (YouTubeMusicPlaylist) -> Unit,
    offlineMode: Boolean,
    gridState: LazyGridState,
    isTabletLayout: Boolean = false
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val gridHorizontalPadding = if (isTabletLayout) 56.dp else 16.dp
    val gridMinCellSize = if (isTabletLayout) 156.dp else 120.dp
    val gridSpacing = if (isTabletLayout) 14.dp else 10.dp
    when {
        ui.ytMusicPlaylistsLoading -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = miniPlayerHeight),
                Alignment.Center
            ) { CircularProgressIndicator() }
        }
        ui.ytMusicPlaylistsError != null -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = miniPlayerHeight),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        ui.ytMusicPlaylistsError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    HapticTextButton(onClick = { vm.loadYtMusicPlaylists() }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
        ui.ytMusicPlaylists.isEmpty() -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = miniPlayerHeight),
                Alignment.Center
            ) {
                Text(
                    stringResource(R.string.explore_tag_youtube_music),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        else -> {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(gridMinCellSize),
                contentPadding = PaddingValues(
                    start = gridHorizontalPadding, end = gridHorizontalPadding,
                    top = 8.dp,
                    bottom = 16.dp + miniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = ui.ytMusicPlaylists,
                    key = { it.browseId }
                ) { playlist ->
                    YtMusicExploreCard(
                        playlist = playlist,
                        onClick = { onClick(playlist) },
                        offlineMode = offlineMode
                    )
                }
            }
        }
    }
}

@Composable
private fun YtMusicExploreCard(
    playlist: YouTubeMusicPlaylist,
    onClick: () -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = fastScrollableImageRequest(
                context = context,
                data = playlist.coverUrl,
                sizePx = 384,
                offlineMode = offlineMode
            ),
            contentDescription = playlist.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            Text(
                text = playlist.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            if (playlist.subtitle.isNotBlank()) {
                Text(
                    text = playlist.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
