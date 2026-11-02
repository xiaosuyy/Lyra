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
 * File: com.lyra.player.ui.screen.tab/LibraryScreen
 * Created: 2025/8/8
 */

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import com.lyra.player.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import com.lyra.player.ui.theme.designsystem.LocalNeriColors
import com.lyra.player.ui.theme.designsystem.LocalNeriTextStyles
import com.lyra.player.ui.theme.designsystem.LyraIconSize
import com.lyra.player.ui.theme.designsystem.LyraRadius
import com.lyra.player.ui.theme.designsystem.LyraSpacing
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.core.di.AppContainer
import com.lyra.player.data.stats.PlaybackStatsPeriod
import com.lyra.player.data.stats.PlaybackStatsHotPlaylist
import com.lyra.player.data.stats.buildPlaybackStatsHotPlaylist
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.playlist.favorite.FAVORITE_SOURCE_NETEASE_ARTIST
import com.lyra.player.ui.screen.tab.library.LibraryUnifiedContent
import com.lyra.player.ui.viewmodel.tab.library.LOCAL_SOURCE_ID
import com.lyra.player.ui.viewmodel.tab.library.LibraryPlaylistRef
import com.lyra.player.data.playlist.favorite.FavoritePlaylist
import com.lyra.player.data.playlist.favorite.FavoritePlaylistRepository
import com.lyra.player.ui.viewmodel.tab.toBiliPlaylist
import com.lyra.player.data.local.playlist.system.FavoritesPlaylist
import com.lyra.player.data.local.playlist.system.LocalFilesPlaylist
import com.lyra.player.data.local.playlist.model.LocalArtistSummary
import com.lyra.player.ui.effect.glass.AdvancedGlassRole
import com.lyra.player.ui.effect.glass.AdvancedGlassSurface
import com.lyra.player.data.local.playlist.model.LocalPlaylist
import com.lyra.player.data.local.playlist.model.buildLocalArtistSummaries
import com.lyra.player.data.local.playlist.LocalPlaylistRepository
import com.lyra.player.data.local.playlist.system.SystemLocalPlaylists
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.ui.LocalMiniPlayerHeight
import com.lyra.player.ui.feedback.AppFeedback
import com.lyra.player.ui.component.playlist.showPlaylistDeleteResultGlobally
import com.lyra.player.ui.util.shouldAllowCollapsingTopAppBar
import com.lyra.player.data.model.NeteaseArtistSummary
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.ui.viewmodel.tab.BiliPlaylist
import com.lyra.player.ui.viewmodel.tab.BiliPlaylistKind
import com.lyra.player.ui.viewmodel.tab.LibraryViewModel
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary
import com.lyra.player.ui.viewmodel.tab.YouTubeMusicPlaylist
import com.lyra.player.ui.viewmodel.tab.favoriteId
import com.lyra.player.ui.util.rememberPlaylistDisplayCoverUrl
import com.lyra.player.util.media.fastScrollableImageRequest
import com.lyra.player.ui.haptic.HapticIconButton
import com.lyra.player.ui.haptic.HapticTextButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsDialogContent
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsTextButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsTextField
import com.lyra.player.ui.util.currentWindowWidthDp
import com.lyra.player.util.format.formatPlayCount
import com.lyra.player.util.media.offlineCachedImageRequest
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

enum class LibraryTab(val labelResId: Int) {
    LOCAL(R.string.library_tab_local),
    FAVORITE(R.string.library_tab_favorite),
    YTMUSIC(R.string.library_tab_youtube_music),
    NETEASE(R.string.library_tab_netease),
    NETEASEALBUM(R.string.library_tab_netease_album),
    BILI(R.string.library_tab_bilibili),
    QQMUSIC(R.string.library_tab_qqmusic)
}

private const val NETEASE_CATEGORY_PLAYLIST = 0
private const val NETEASE_CATEGORY_ALBUM = 1
private const val FAVORITE_CATEGORY_PLAYLIST = 0
private const val FAVORITE_CATEGORY_ARTIST = 1
private const val FAVORITE_CATEGORY_HOT = 2
private const val LOCAL_CATEGORY_PLAYLIST = 0
private const val LOCAL_CATEGORY_ARTIST = 1
private const val LIBRARY_UI_PREFS = "library_ui_preferences"
private const val KEY_LOCAL_ARTIST_SORT_MODE = "local_artist_sort_mode"
private val LibraryPrimaryTabShape = RoundedCornerShape(20.dp)
private val LibrarySearchFieldShape = RoundedCornerShape(16.dp)

private val HotPlaylistPeriods = listOf(
    PlaybackStatsPeriod.WEEK,
    PlaybackStatsPeriod.MONTH
)

private fun hotPlaylistTitleResId(period: PlaybackStatsPeriod): Int = when (period) {
    PlaybackStatsPeriod.MONTH -> R.string.library_hot_playlist_month
    else -> R.string.library_hot_playlist_week
}

internal enum class LocalArtistSortMode {
    SONG_COUNT,
    RECENT_ADDED,
    NAME
}

internal fun resolveLocalArtistSortMode(storageValue: String?): LocalArtistSortMode {
    return LocalArtistSortMode.entries.firstOrNull { it.name == storageValue }
        ?: LocalArtistSortMode.SONG_COUNT
}

internal fun localArtistSortModeStorageValue(sortMode: LocalArtistSortMode): String {
    return sortMode.name
}

private fun readLocalArtistSortMode(context: Context): LocalArtistSortMode {
    val prefs = context.getSharedPreferences(LIBRARY_UI_PREFS, Context.MODE_PRIVATE)
    return resolveLocalArtistSortMode(prefs.getString(KEY_LOCAL_ARTIST_SORT_MODE, null))
}

private fun persistLocalArtistSortMode(context: Context, sortMode: LocalArtistSortMode) {
    context.getSharedPreferences(LIBRARY_UI_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(KEY_LOCAL_ARTIST_SORT_MODE, localArtistSortModeStorageValue(sortMode))
        }
}

@Composable
private fun rememberHotPlaylists(): List<PlaybackStatsHotPlaylist>? {
    val hotPlaylists by produceState<List<PlaybackStatsHotPlaylist>?>(initialValue = null) {
        val statsRepository = withContext(Dispatchers.IO) {
            AppContainer.playbackStatsRepo
        }
        combine(
            statsRepository.statsFlow,
            statsRepository.dailyStatsFlow
        ) { stats, dailyStats ->
            stats to dailyStats
        }.collect { (stats, dailyStats) ->
            value = withContext(Dispatchers.Default) {
                HotPlaylistPeriods.map { period ->
                    buildPlaybackStatsHotPlaylist(
                        stats = stats,
                        dailyStats = dailyStats,
                        period = period
                    )
                }
            }
        }
    }
    return hotPlaylists
}

/**
 * 媒体库顶部页签顺序。
 *
 * 当前设计：只保留「本地」与「收藏」，其余渠道页签（含已移除的 YouTube）不再展示，
 * 因此不再依赖国际化 / YouTube 开关。
 */
internal fun libraryTabDisplayOrder(): List<LibraryTab> = listOf(
    LibraryTab.LOCAL,
    LibraryTab.FAVORITE
)

/** Library Tab → 音源 id（轻量映射，仅用于向 Registry 请求刷新；平台取数逻辑不在 UI）。 */
private fun LibraryTab.librarySourceId(): String? = when (this) {
    LibraryTab.NETEASE,
    LibraryTab.NETEASEALBUM -> MusicSourceIds.NETEASE
    LibraryTab.BILI -> MusicSourceIds.BILIBILI
    LibraryTab.YTMUSIC -> MusicSourceIds.YOUTUBE_MUSIC
    LibraryTab.LOCAL -> LOCAL_SOURCE_ID
    LibraryTab.FAVORITE,
    LibraryTab.QQMUSIC -> null
}

/**
 * 统一 [LibraryPlaylistRef] → 现有平台回调（按 sourceId 做轻量映射，播放/详情仍走原链路）。
 */
private fun openLibraryPlaylistRef(
    ref: LibraryPlaylistRef,
    neteasePlaylists: List<PlaylistSummary>,
    biliPlaylists: List<BiliPlaylist>,
    youtubeMusicPlaylists: List<YouTubeMusicPlaylist>,
    onNeteasePlaylistClick: (PlaylistSummary) -> Unit,
    onBiliPlaylistClick: (BiliPlaylist) -> Unit,
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit
) {
    when (ref.sourceId) {
        MusicSourceIds.NETEASE -> neteasePlaylists
            .firstOrNull { it.id.toString() == ref.id }
            ?.let(onNeteasePlaylistClick)

        MusicSourceIds.BILIBILI -> biliPlaylists
            .firstOrNull { it.mediaId.toString() == ref.id }
            ?.let(onBiliPlaylistClick)

        MusicSourceIds.YOUTUBE_MUSIC -> youtubeMusicPlaylists
            .firstOrNull { it.playlistId.ifBlank { it.browseId } == ref.id }
            ?.let(onYouTubeMusicPlaylistClick)

        else -> Unit
    }
}

private fun LibraryTab.asVisibleLibraryTab(): LibraryTab {
    return if (this == LibraryTab.NETEASEALBUM) LibraryTab.NETEASE else this
}

private fun LibraryTab?.isRefreshable(): Boolean {
    return when (this?.asVisibleLibraryTab()) {
        LibraryTab.BILI,
        LibraryTab.YTMUSIC,
        LibraryTab.NETEASE -> true
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    initialTab: LibraryTab = LibraryTab.LOCAL,
    onTabChange: (LibraryTab) -> Unit = {},
    localListState: LazyListState,
    favoriteListState: LazyListState,
    neteaseAlbumState: LazyListState,
    neteaseListState: LazyListState,
    youtubeMusicListState: LazyListState,
    biliListState: LazyListState,
    qqMusicListState: LazyListState,
    topAppBarState: TopAppBarState,
    onLocalPlaylistClick: (LocalPlaylist) -> Unit = {},
    onLocalArtistClick: (LocalArtistSummary) -> Unit = {},
    onHotPlaylistClick: (PlaybackStatsPeriod) -> Unit = {},
    onNeteasePlaylistClick: (PlaylistSummary) -> Unit = {},
    onNeteaseAlbumClick: (AlbumSummary) -> Unit = {},
    onNeteaseArtistClick: (NeteaseArtistSummary) -> Unit = {},
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit = {},
    onBiliPlaylistClick: (BiliPlaylist) -> Unit = {},
    onOpenRecent: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    offlineMode: Boolean = false
) {
    val vm: LibraryViewModel = viewModel()
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val librarySections by vm.librarySections.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultPlaylistName = stringResource(R.string.library_create_playlist_default)
    val localPlaylistRepo = remember(context) {
        LocalPlaylistRepository.getInstance(context)
    }
    val orderedTabs = remember { libraryTabDisplayOrder() }
    val initialPage = remember(orderedTabs, initialTab) {
        orderedTabs.indexOf(initialTab.asVisibleLibraryTab()).takeIf { it >= 0 } ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { orderedTabs.size }
    )
    var selectedNeteaseCategory by rememberSaveable {
        mutableIntStateOf(NETEASE_CATEGORY_PLAYLIST)
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        canScroll = {
            when (orderedTabs.getOrNull(pagerState.currentPage)) {
                LibraryTab.LOCAL -> shouldAllowCollapsingTopAppBar(
                    localListState.canScrollForward,
                    localListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.FAVORITE -> shouldAllowCollapsingTopAppBar(
                    favoriteListState.canScrollForward,
                    favoriteListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.NETEASE,
                LibraryTab.NETEASEALBUM -> {
                    val activeListState = if (
                        selectedNeteaseCategory == NETEASE_CATEGORY_ALBUM
                    ) {
                        neteaseAlbumState
                    } else {
                        neteaseListState
                    }
                    shouldAllowCollapsingTopAppBar(
                        activeListState.canScrollForward,
                        activeListState.canScrollBackward,
                        topAppBarState.collapsedFraction
                    )
                }
                LibraryTab.YTMUSIC -> shouldAllowCollapsingTopAppBar(
                    youtubeMusicListState.canScrollForward,
                    youtubeMusicListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.BILI -> shouldAllowCollapsingTopAppBar(
                    biliListState.canScrollForward,
                    biliListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.QQMUSIC -> shouldAllowCollapsingTopAppBar(
                    qqMusicListState.canScrollForward,
                    qqMusicListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                null -> shouldAllowCollapsingTopAppBar(
                    canScrollForward = false,
                    canScrollBackward = false,
                    collapsedFraction = topAppBarState.collapsedFraction
                )
            }
        }
    )
    val scope = rememberCoroutineScope()
    val windowWidthDp = currentWindowWidthDp()
    val isTabletLayout = windowWidthDp >= 720.dp
    val pageHorizontalPadding = if (isTabletLayout) LyraSpacing.XL else 0.dp

    LaunchedEffect(initialTab, orderedTabs) {
        val targetPage = orderedTabs.indexOf(initialTab.asVisibleLibraryTab()).takeIf { it >= 0 } ?: 0
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage, orderedTabs, initialTab) {
        val currentTab = orderedTabs.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (currentTab != initialTab) {
            onTabChange(currentTab)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LargeTopAppBar(
            title = { Text(stringResource(R.string.library_title)) },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            actions = {
                HapticIconButton(onClick = onOpenStats) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = stringResource(R.string.stats_title),
                        tint = LocalNeriColors.current.secondaryText,
                        modifier = Modifier.size(LyraIconSize.Large)
                    )
                }
                HapticIconButton(onClick = onOpenRecent) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = stringResource(R.string.library_recent_played),
                        tint = LocalNeriColors.current.secondaryText,
                        modifier = Modifier.size(LyraIconSize.Large)
                    )
                }
            }
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .padding(
                    horizontal = pageHorizontalPadding,
                    vertical = LyraSpacing.MD
                )
                .widthIn(max = 1180.dp)
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(Modifier.fillMaxSize()) {
                val currentTab = orderedTabs.getOrNull(pagerState.currentPage)
                LibraryMainTabs(
                    tabs = orderedTabs,
                    selectedTabIndex = pagerState.currentPage,
                    refreshEnabled = currentTab.isRefreshable(),
                    onTabSelected = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    onRefresh = {
                        // UI 只按 sourceId 请求刷新；平台取数逻辑在对应 Provider 内。
                        currentTab?.librarySourceId()?.let(vm::refreshLibrarySource)
                    }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 0.dp
                ) { page ->
                    when (orderedTabs[page]) {
                        LibraryTab.LOCAL -> LocalPlaylistList(
                            playlists = ui.localPlaylists,
                            listState = localListState,
                            onCreate = { name ->
                                val finalName = name.trim().ifBlank { defaultPlaylistName }
                                vm.createLocalPlaylist(finalName)
                            },
                            onClick = onLocalPlaylistClick,
                            onArtistClick = onLocalArtistClick,
                            onRename = { playlistId, newName ->
                                vm.renameLocalPlaylist(playlistId, newName)
                            },
                            onDelete = { playlistIds ->
                                vm.deleteLocalPlaylists(playlistIds) { result ->
                                    showPlaylistDeleteResultGlobally(
                                        context = context,
                                        repository = localPlaylistRepo,
                                        result = result
                                    )
                                }
                            },
                            onReorder = { order ->
                                vm.reorderLocalPlaylists(order)
                            },
                            offlineMode = offlineMode
                        )

                        LibraryTab.FAVORITE -> FavoritePlaylistList(
                            listState = favoriteListState,
                            onHotPlaylistClick = onHotPlaylistClick,
                            onNeteasePlaylistClick = onNeteasePlaylistClick,
                            onNeteaseAlbumClick = onNeteaseAlbumClick,
                            onNeteaseArtistClick = onNeteaseArtistClick,
                            onBiliPlaylistClick = onBiliPlaylistClick,
                            onYouTubeMusicPlaylistClick = onYouTubeMusicPlaylistClick,
                            offlineMode = offlineMode
                        )

                        // 平台来源统一渲染：只按 LibrarySection 渲染，不再有平台专属列表。
                        LibraryTab.NETEASE,
                        LibraryTab.NETEASEALBUM,
                        LibraryTab.YTMUSIC,
                        LibraryTab.BILI -> LibraryUnifiedContent(
                            sections = librarySections.filter {
                                it.sourceId == orderedTabs[page].librarySourceId()
                            },
                            onPlaylistClick = { ref ->
                                openLibraryPlaylistRef(
                                    ref = ref,
                                    neteasePlaylists = ui.neteasePlaylists,
                                    biliPlaylists = ui.biliPlaylists,
                                    youtubeMusicPlaylists = ui.youtubeMusicPlaylists,
                                    onNeteasePlaylistClick = onNeteasePlaylistClick,
                                    onBiliPlaylistClick = onBiliPlaylistClick,
                                    onYouTubeMusicPlaylistClick = onYouTubeMusicPlaylistClick
                                )
                            },
                            onAlbumClick = { ref ->
                                ui.neteaseAlbums
                                    .firstOrNull { it.id.toString() == ref.id }
                                    ?.let(onNeteaseAlbumClick)
                            }
                        )

                        LibraryTab.QQMUSIC -> QqMusicPlaylistList(
                            listState = qqMusicListState
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryMainTabs(
    tabs: List<LibraryTab>,
    selectedTabIndex: Int,
    refreshEnabled: Boolean,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LyraSpacing.LG)
            .padding(bottom = LyraSpacing.SM)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.ScreenTopTab,
            modifier = Modifier
                .weight(1f)
                .clip(LibraryPrimaryTabShape),
            shape = LibraryPrimaryTabShape
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { onTabSelected(index) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = stringResource(tab.labelResId),
                                style = LocalNeriTextStyles.current.caption
                            )
                        }
                    )
                }
            }
        }

        HapticIconButton(
            onClick = onRefresh,
            enabled = refreshEnabled
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.action_refresh),
                tint = LocalNeriColors.current.secondaryText,
                modifier = Modifier.size(LyraIconSize.Large)
            )
        }
    }
}

@Composable
private fun LocalPlaylistList(
    playlists: List<LocalPlaylist>,
    listState: LazyListState,
    onCreate: (String) -> Unit,
    onClick: (LocalPlaylist) -> Unit,
    onArtistClick: (LocalArtistSummary) -> Unit,
    onRename: (Long, String) -> Unit = { _, _ -> },
    onDelete: (List<Long>) -> Unit = {},
    onReorder: (List<Long>) -> Unit = {},
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    var selectedLocalCategory by rememberSaveable {
        mutableIntStateOf(LOCAL_CATEGORY_PLAYLIST)
    }
    var localSearchQuery by rememberSaveable { mutableStateOf("") }
    var localArtistSortMode by rememberSaveable {
        mutableStateOf(readLocalArtistSortMode(context))
    }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val defaultPlaylistName = composeResources.getString(R.string.library_create_playlist_default)
    val maxNameLength = LocalPlaylistRepository.MAX_PLAYLIST_NAME_LENGTH
    val autoShowKeyboard by AppContainer.settingsRepo.autoShowKeyboardFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val localArtists = remember(playlists, context) {
        buildLocalArtistSummaries(playlists, context)
    }
    val filteredLocalArtists = remember(localArtists, localSearchQuery) {
        filterLocalArtists(localArtists, localSearchQuery)
    }
    val displayedLocalArtists = remember(filteredLocalArtists, localArtistSortMode) {
        sortLocalArtists(filteredLocalArtists, localArtistSortMode)
    }
    val editablePlaylists = remember(playlists, context) {
        playlists.filterNot { SystemLocalPlaylists.isSystemPlaylist(it, context) }
    }
    val reorderablePlaylists = remember(editablePlaylists) {
        mutableStateListOf<LocalPlaylist>().apply {
            addAll(editablePlaylists)
        }
    }

    LaunchedEffect(showDialog) {
        if (showDialog && autoShowKeyboard) focusRequester.requestFocus()
    }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
        showDeleteSelectedConfirm = false
    }

    fun toggleSelection(playlistId: Long) {
        selectedIds =
            if (selectedIds.contains(playlistId)) selectedIds - playlistId else selectedIds + playlistId
    }

    fun deleteSelected() {
        if (selectedIds.isEmpty()) return
        showDeleteSelectedConfirm = true
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    LaunchedEffect(editablePlaylists) {
        val validIds = editablePlaylists.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
        if (selectionMode && editablePlaylists.isEmpty()) {
            exitSelection()
        }
    }

    fun tryCreate(): Boolean {
        val trimmedInput = newName.trim().take(maxNameLength)
        val finalName = trimmedInput.ifBlank { defaultPlaylistName }.take(maxNameLength)

        val favoritesName = composeResources.getString(R.string.favorite_my_music)
        val localFilesName = composeResources.getString(R.string.local_files)
        if (FavoritesPlaylist.matches(finalName, context)) {
            nameError = composeResources.getString(R.string.library_name_reserved, favoritesName)
            return false
        }
        if (LocalFilesPlaylist.matches(finalName, context)) {
            nameError = composeResources.getString(R.string.library_name_reserved, localFilesName)
            return false
        }
        if (playlists.any { it.name.equals(finalName, ignoreCase = true) }) {
            nameError = composeResources.getString(R.string.library_name_exists)
            return false
        }

        onCreate(finalName)
        showDialog = false
        newName = ""
        nameError = null
        return true
    }

    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val favoritesPlaylist = playlists.firstOrNull { FavoritesPlaylist.isSystemPlaylist(it, context) }
    val localFilesPlaylist = playlists.firstOrNull { LocalFilesPlaylist.isSystemPlaylist(it, context) }
    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from: ItemPosition, to: ItemPosition ->
            if (!selectionMode) return@rememberReorderableLazyListState
            val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
            val toId = to.key as? Long ?: return@rememberReorderableLazyListState
            val fromIdx = reorderablePlaylists.indexOfFirst { it.id == fromId }
            val toIdx = reorderablePlaylists.indexOfFirst { it.id == toId }
            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                reorderablePlaylists.add(toIdx, reorderablePlaylists.removeAt(fromIdx))
            }
        },
        canDragOver = { _, over ->
            selectionMode && over.key is Long
        },
        onDragEnd = { _, _ ->
            if (selectionMode) {
                onReorder(reorderablePlaylists.map { it.id })
            }
        }
    )

    val displayedFavoritesPlaylist = favoritesPlaylist
        ?.takeIf { playlist -> playlist.matchesLocalPlaylistSearch(localSearchQuery, context) }
    val displayedLocalFilesPlaylist = localFilesPlaylist
        ?.takeIf { playlist -> playlist.matchesLocalPlaylistSearch(localSearchQuery, context) }
    val displayedPlaylists = reorderablePlaylists
        .filter { playlist -> playlist.matchesLocalPlaylistSearch(localSearchQuery, context) }
    val hasPlaylistSearchMatches =
        displayedFavoritesPlaylist != null ||
            displayedPlaylists.isNotEmpty() ||
            displayedLocalFilesPlaylist != null
    val windowWidthDp = currentWindowWidthDp()
    val localArtistColumnCount = remember(windowWidthDp) {
        ((windowWidthDp.value - 16f + 10f) / 130f).toInt().coerceAtLeast(1)
    }
    val localArtistRows = remember(displayedLocalArtists, localArtistColumnCount) {
        displayedLocalArtists.chunked(localArtistColumnCount)
    }

    LazyColumn(
        state = reorderState.listState,
        contentPadding = PaddingValues(
            start = 0.dp,
            end = 0.dp,
            top = LyraSpacing.SM,
            bottom = LyraSpacing.SM + miniPlayerHeight
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState)
    ) {
        val cardShape = RoundedCornerShape(LyraRadius.Medium)
        item(key = "local_library_header") {
            LocalLibraryHeaderContent(
                selectedLocalCategory = selectedLocalCategory,
                selectionMode = selectionMode,
                searchQuery = localSearchQuery,
                onSearchQueryChange = { localSearchQuery = it },
                artistSortMode = localArtistSortMode,
                onArtistSortModeChange = { sortMode ->
                    localArtistSortMode = sortMode
                    persistLocalArtistSortMode(context, sortMode)
                },
                onPlaylistSelected = {
                    if (selectedLocalCategory != LOCAL_CATEGORY_PLAYLIST) {
                        exitSelection()
                        selectedLocalCategory = LOCAL_CATEGORY_PLAYLIST
                    }
                },
                onArtistSelected = {
                    if (selectedLocalCategory != LOCAL_CATEGORY_ARTIST) {
                        exitSelection()
                        selectedLocalCategory = LOCAL_CATEGORY_ARTIST
                    }
                }
            )
        }

        if (selectedLocalCategory == LOCAL_CATEGORY_ARTIST) {
            if (displayedLocalArtists.isEmpty()) {
                item(key = "local_artist_empty") {
                    LibrarySearchEmptyCard(
                        titleResId = if (localSearchQuery.isBlank()) {
                            R.string.library_local_artist_empty
                        } else {
                            R.string.library_local_search_empty
                        },
                        hintResId = if (localSearchQuery.isBlank()) {
                            R.string.library_local_artist_hint
                        } else {
                            R.string.library_local_search_empty_hint
                        },
                        iconIsArtist = true
                    )
                }
            }
            items(
                items = localArtistRows,
                key = { row -> row.joinToString(separator = "|") { artist -> artist.stableKey } }
            ) { rowArtists ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LyraSpacing.LG)
                        .padding(vertical = 5.dp)
                ) {
                    rowArtists.forEach { artist ->
                        Box(modifier = Modifier.weight(1f)) {
                            LocalArtistGridCard(
                                artist = artist,
                                onClick = { onArtistClick(artist) },
                                offlineMode = offlineMode
                            )
                        }
                    }
                    repeat(localArtistColumnCount - rowArtists.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
                if (selectionMode) {
                    item(key = "local_playlist_selection_header") {
                        val allSelected =
                            selectedIds.size == displayedPlaylists.size && displayedPlaylists.isNotEmpty()
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                pluralStringResource(
                                    R.plurals.common_selected_count,
                                    selectedIds.size,
                                    selectedIds.size
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            HapticIconButton(onClick = { exitSelection() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_exit_multi_select)
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HapticTextButton(
                                    onClick = {
                                        selectedIds = if (allSelected) {
                                            emptySet()
                                        } else {
                                            displayedPlaylists.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        if (allSelected) {
                                            stringResource(R.string.action_deselect_all)
                                        } else {
                                            stringResource(R.string.action_select_all)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                HapticTextButton(
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = { deleteSelected() }
                                ) {
                                    Text(stringResource(R.string.common_delete_selected))
                                }
                            }
                        }
                    )
                }
            }
        }
        if (localSearchQuery.isBlank()) {
            item(key = "local_playlist_create") {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                    .animateItem()
                    .clip(cardShape)
                    .clickable(enabled = !selectionMode) { showDialog = true }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.library_create_new)) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }

            if (showDialog) {
                MiuixSettingsDialog(
                    onDismissRequest = {
                        showDialog = false
                        newName = ""
                        nameError = null
                    },
                    title = { Text(stringResource(R.string.playlist_create)) },
                    text = {
                        MiuixSettingsDialogContent(verticalSpacing = 12.dp) {
                            MiuixSettingsTextField(
                                value = newName,
                                onValueChange = {
                                    newName = it.take(maxNameLength)
                                    if (nameError != null) nameError = null
                                },
                                placeholder = { Text(stringResource(R.string.playlist_enter_name)) },
                                singleLine = true,
                                modifier = Modifier.focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { tryCreate() })
                            )
                            if (nameError != null) {
                                Text(
                                    text = nameError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        MiuixSettingsButton(
                            onClick = { tryCreate() },
                            enabled = newName.trim().isNotBlank()
                        ) {
                            Text(stringResource(R.string.action_create))
                        }
                    },
                    dismissButton = {
                        MiuixSettingsTextButton(
                            onClick = {
                                showDialog = false
                                newName = ""
                                nameError = null
                            }
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }
                )
            }

            if (showDeleteSelectedConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteSelectedConfirm = false },
                    title = { Text(stringResource(R.string.dialog_confirm_delete)) },
                    text = {
                        Text(
                            pluralStringResource(
                                R.plurals.library_delete_selected_confirm,
                                selectedIds.size,
                                selectedIds.size
                            )
                        )
                    },
                    confirmButton = {
                        HapticTextButton(
                            onClick = {
                                val idsToDelete = selectedIds.toList()
                                exitSelection()
                                onDelete(idsToDelete)
                            }
                        ) { Text(stringResource(R.string.action_delete)) }
                    },
                    dismissButton = {
                        HapticTextButton(
                            onClick = { showDeleteSelectedConfirm = false }
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }
                )
            }
            }
        }

        if (localSearchQuery.isNotBlank() && !hasPlaylistSearchMatches) {
            item(key = "local_playlist_search_empty") {
                LibrarySearchEmptyCard(
                    titleResId = R.string.library_local_search_empty,
                    hintResId = R.string.library_local_search_empty_hint,
                    iconIsArtist = false
                )
            }
        }

        displayedFavoritesPlaylist?.let { system ->
            item(key = "local_playlist_favorites") {
                val displayName = SystemLocalPlaylists.resolve(system.id, system.name, context)?.currentName ?: system.name
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                        .clip(cardShape)
                        .combinedClickable(
                            onClick = {
                                if (!selectionMode) onClick(system)
                            }
                        )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(R.plurals.library_song_count, system.songs.size, system.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Spacer(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = system,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

            items(
                items = displayedPlaylists,
                key = { it.id }
            ) { pl ->
            ReorderableItem(state = reorderState, key = pl.id) { _ ->
                val systemPlaylist = SystemLocalPlaylists.resolve(pl.id, pl.name, context)
                val displayName = systemPlaylist?.currentName ?: pl.name
                val isSystemPlaylist = systemPlaylist != null
                val isSelected = selectionMode && selectedIds.contains(pl.id)
                val rowContainerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                }

                var showMenu by remember { mutableStateOf(false) }
                var showRenameDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var renameText by remember { mutableStateOf(pl.name.take(maxNameLength)) }

                if (selectionMode && showMenu) showMenu = false

                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = rowContainerColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                        .animateItem()
                        .clip(cardShape)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    toggleSelection(pl.id)
                                } else {
                                    onClick(pl)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode && !isSystemPlaylist) {
                                    selectionMode = true
                                    selectedIds = setOf(pl.id)
                                }
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = LyraSpacing.SM,
                                vertical = LyraSpacing.SM
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (!isSystemPlaylist) toggleSelection(pl.id)
                                },
                                enabled = !isSystemPlaylist
                            )
                            Spacer(modifier = Modifier.width(LyraSpacing.XS))
                        }
                        val cover = rememberPlaylistDisplayCoverUrl(
                            playlist = pl,
                            resolveLocalFallback = true
                        )
                        if (!cover.isNullOrEmpty()) {
                            AsyncImage(
                                model = fastScrollableImageRequest(
                                    context = context,
                                    data = cover,
                                    sizePx = 160,
                                    offlineMode = offlineMode
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(LyraRadius.Medium))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = LocalNeriColors.current.tertiaryText,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(LyraSpacing.MD))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = LocalNeriTextStyles.current.body,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.library_song_count,
                                    pl.songs.size,
                                    pl.songs.size
                                ),
                                style = LocalNeriTextStyles.current.captionSecondary,
                                color = LocalNeriColors.current.secondaryText
                            )
                        }
                        Spacer(modifier = Modifier.width(LyraSpacing.SM))
                        if (selectionMode && !isSystemPlaylist) {
                            Box(
                                modifier = Modifier
                                    .detectReorder(reorderState)
                                    .padding(LyraSpacing.SM)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DragHandle,
                                    contentDescription = stringResource(R.string.common_drag_handle),
                                    modifier = Modifier.size(LyraIconSize.Large)
                                )
                            }
                        } else if (!selectionMode && !isSystemPlaylist) {
                            Box {
                                HapticIconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = stringResource(R.string.common_more_options),
                                        tint = LocalNeriColors.current.secondaryText,
                                        modifier = Modifier.size(LyraIconSize.Large)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_rename)) },
                                        onClick = {
                                            showMenu = false
                                            renameText = pl.name.take(maxNameLength)
                                            showRenameDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_delete)) },
                                        onClick = {
                                            showMenu = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showRenameDialog) {
                    MiuixSettingsDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text(stringResource(R.string.action_rename)) },
                        text = {
                            MiuixSettingsDialogContent(verticalSpacing = 12.dp) {
                                MiuixSettingsTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it.take(maxNameLength) },
                                    placeholder = { Text(displayName) },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            MiuixSettingsButton(
                                onClick = {
                                    val trimmed = renameText.trim().take(maxNameLength)
                                    if (trimmed.isNotBlank()) {
                                        onRename(pl.id, trimmed)
                                        showRenameDialog = false
                                    }
                                },
                                enabled = renameText.trim().isNotBlank()
                            ) { Text(stringResource(R.string.action_confirm)) }
                        },
                        dismissButton = {
                            MiuixSettingsTextButton(
                                onClick = { showRenameDialog = false }
                            ) { Text(stringResource(R.string.action_cancel)) }
                        }
                    )
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringResource(R.string.action_delete)) },
                        text = {
                            Text(stringResource(R.string.library_delete_playlist_confirm, displayName))
                        },
                        confirmButton = {
                            HapticTextButton(
                                onClick = {
                                    val playlistId = pl.id
                                    showDeleteDialog = false
                                    onDelete(listOf(playlistId))
                                }
                            ) { Text(stringResource(R.string.action_delete)) }
                        },
                        dismissButton = {
                            HapticTextButton(
                                onClick = { showDeleteDialog = false }
                            ) { Text(stringResource(R.string.action_cancel)) }
                        }
                    )
                }
            }
        }

        displayedLocalFilesPlaylist?.let { system ->
            item(key = "local_playlist_local_files") {
                val displayName = SystemLocalPlaylists.resolve(system.id, system.name, context)?.currentName ?: system.name
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                        .clip(cardShape)
                        .combinedClickable(
                            onClick = {
                                if (!selectionMode) onClick(system)
                            }
                        )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(R.plurals.library_song_count, system.songs.size, system.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Spacer(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = system,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun LocalLibraryHeaderContent(
    selectedLocalCategory: Int,
    selectionMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    artistSortMode: LocalArtistSortMode,
    onArtistSortModeChange: (LocalArtistSortMode) -> Unit,
    onPlaylistSelected: () -> Unit,
    onArtistSelected: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LocalCategoryTabs(
            selectedCategory = selectedLocalCategory,
            onPlaylistSelected = onPlaylistSelected,
            onArtistSelected = onArtistSelected
        )
        if (!selectionMode) {
            if (selectedLocalCategory == LOCAL_CATEGORY_ARTIST) {
                LocalArtistSearchAndSortRow(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    sortMode = artistSortMode,
                    onSortModeChange = onArtistSortModeChange
                )
            } else {
                LibraryInlineSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholderResId = R.string.library_local_playlist_search_hint
                )
            }
        }
    }
}

@Composable
private fun LocalArtistSearchAndSortRow(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: LocalArtistSortMode,
    onSortModeChange: (LocalArtistSortMode) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.library_local_artist_search_hint)) },
            singleLine = true,
            shape = LibrarySearchFieldShape,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    HapticIconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_clear)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Box {
            HapticIconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.library_local_artist_sort)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.SONG_COUNT,
                    text = stringResource(R.string.library_local_artist_sort_count),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.SONG_COUNT)
                        menuExpanded = false
                    }
                )
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.RECENT_ADDED,
                    text = stringResource(R.string.library_local_artist_sort_recent),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.RECENT_ADDED)
                        menuExpanded = false
                    }
                )
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.NAME,
                    text = stringResource(R.string.library_local_artist_sort_name),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.NAME)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LocalArtistSortMenuItem(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text)
        },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.common_selected)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        },
        onClick = onClick
    )
}

@Composable
private fun LocalCategoryTabs(
    selectedCategory: Int,
    onPlaylistSelected: () -> Unit,
    onArtistSelected: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.ScreenTopTab,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            tintColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedCategory,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedCategory == LOCAL_CATEGORY_PLAYLIST,
                    onClick = onPlaylistSelected,
                    text = { Text(stringResource(R.string.library_favorite_tab_playlists)) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null
                        )
                    }
                )
                Tab(
                    selected = selectedCategory == LOCAL_CATEGORY_ARTIST,
                    onClick = onArtistSelected,
                    text = { Text(stringResource(R.string.library_favorite_tab_artists)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryInlineSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderResId: Int
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LyraSpacing.LG, vertical = LyraSpacing.XS),
        placeholder = { Text(stringResource(placeholderResId)) },
        singleLine = true,
        shape = RoundedCornerShape(LyraRadius.Large),
        trailingIcon = {
            if (query.isNotEmpty()) {
                HapticIconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_clear),
                        tint = LocalNeriColors.current.secondaryText,
                        modifier = Modifier.size(LyraIconSize.Large)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun LibrarySearchEmptyCard(
    titleResId: Int,
    hintResId: Int,
    iconIsArtist: Boolean
) {
    Card(
        shape = RoundedCornerShape(LyraRadius.Medium),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
            .clip(RoundedCornerShape(LyraRadius.Medium))
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(titleResId),
                    style = LocalNeriTextStyles.current.body
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(hintResId),
                    style = LocalNeriTextStyles.current.captionSecondary,
                    color = LocalNeriColors.current.secondaryText
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = if (iconIsArtist) {
                        Icons.Filled.AccountCircle
                    } else {
                        Icons.AutoMirrored.Filled.QueueMusic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
            }
        )
    }
}

private fun filterLocalArtists(
    artists: List<LocalArtistSummary>,
    query: String
): List<LocalArtistSummary> {
    if (query.isBlank()) return artists
    return artists.filter { artist -> artist.matchesLocalArtistSearch(query) }
}

internal fun sortLocalArtists(
    artists: List<LocalArtistSummary>,
    sortMode: LocalArtistSortMode
): List<LocalArtistSummary> {
    return when (sortMode) {
        LocalArtistSortMode.SONG_COUNT -> artists.sortedWith(
            compareByDescending<LocalArtistSummary> { artist ->
                artist.songs.size
            }.thenBy { artist ->
                artist.name.lowercase()
            }
        )
        LocalArtistSortMode.RECENT_ADDED -> artists.sortedWith(
            compareByDescending<LocalArtistSummary> { artist ->
                artist.coverSong?.addedAt ?: 0L
            }.thenBy { artist ->
                artist.name.lowercase()
            }
        )
        LocalArtistSortMode.NAME -> artists.sortedBy { artist ->
            artist.name.lowercase()
        }
    }
}

private fun LocalArtistSummary.matchesLocalArtistSearch(query: String): Boolean {
    return queryMatches(query, name) ||
        songs.any { song ->
            queryMatches(
                query,
                song.displayName(),
                song.displayArtist(),
                song.album,
                song.localFileName
            )
        }
}

private fun LocalPlaylist.matchesLocalPlaylistSearch(query: String, context: Context): Boolean {
    if (query.isBlank()) return true
    val displayName = SystemLocalPlaylists.resolve(id, name, context)?.currentName ?: name
    return queryMatches(query, id, name, displayName, songs.size) ||
        songs.any { song ->
            queryMatches(
                query,
                song.displayName(),
                song.displayArtist(),
                song.album,
                song.localFileName
            )
        }
}

private fun filterFavoritePlaylists(
    favorites: List<FavoritePlaylist>,
    query: String
): List<FavoritePlaylist> {
    if (query.isBlank()) return favorites
    return favorites.filter { favorite -> favorite.matchesFavoriteSearch(query) }
}

private fun FavoritePlaylist.matchesFavoriteSearch(query: String): Boolean {
    return queryMatches(
        query,
        id,
        name,
        subtitle,
        source,
        browseId,
        playlistId,
        trackCount,
        favoriteSourceSearchAliases(source)
    ) || songs.any { song ->
        queryMatches(
            query,
            song.displayName(),
            song.displayArtist(),
            song.album,
            song.localFileName
        )
    }
}

private fun favoriteSourceSearchAliases(source: String): List<String> {
    return when (source) {
        "youtubeMusic" -> listOf("YouTube", "YouTube Music")
        "neteaseAlbum" -> listOf("Netease Album", "网易云专辑", "专辑")
        "netease" -> listOf("Netease", "网易云", "歌单")
        "bili" -> listOf("Bilibili", "哔哩哔哩", "B站")
        FAVORITE_SOURCE_NETEASE_ARTIST -> listOf("Artist", "歌手")
        else -> listOf(source)
    }
}

private fun queryMatches(query: String, vararg values: Any?): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return true
    return values.any { value ->
        when (value) {
            null -> false
            is Iterable<*> -> value.any { item ->
                item?.toString()?.contains(normalizedQuery, ignoreCase = true) == true
            }
            else -> value.toString().contains(normalizedQuery, ignoreCase = true)
        }
    }
}

@Composable
private fun FavoritePlaylistList(
    listState: LazyListState,
    onHotPlaylistClick: (PlaybackStatsPeriod) -> Unit,
    onNeteasePlaylistClick: (PlaylistSummary) -> Unit,
    onNeteaseAlbumClick: (AlbumSummary) -> Unit,
    onNeteaseArtistClick: (NeteaseArtistSummary) -> Unit,
    onBiliPlaylistClick: (BiliPlaylist) -> Unit,
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val scope = rememberCoroutineScope()
    var sortMode by rememberSaveable { mutableStateOf(false) }
    var selectedFavoriteCategory by rememberSaveable {
        mutableIntStateOf(FAVORITE_CATEGORY_PLAYLIST)
    }
    var favoriteSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    val reorderableFavorites = remember { mutableStateListOf<FavoritePlaylist>() }
    val playlistFavorites = remember(favorites) {
        favorites.filterNot { it.source == FAVORITE_SOURCE_NETEASE_ARTIST }
    }
    val artistFavorites = remember(favorites) {
        favorites.filter { it.source == FAVORITE_SOURCE_NETEASE_ARTIST }
    }
    val isHotCategory = selectedFavoriteCategory == FAVORITE_CATEGORY_HOT
    val visibleFavorites = remember(playlistFavorites, artistFavorites, selectedFavoriteCategory) {
        when (selectedFavoriteCategory) {
            FAVORITE_CATEGORY_ARTIST -> artistFavorites
            FAVORITE_CATEGORY_PLAYLIST -> playlistFavorites
            else -> emptyList()
        }
    }
    val hotPlaylists = if (isHotCategory) rememberHotPlaylists() else null

    fun favoriteKey(favorite: FavoritePlaylist): String {
        return "${favorite.source}:${favorite.id}"
    }

    fun exitEditMode() {
        sortMode = false
        selectedKeys = emptySet()
        showDeleteSelectedConfirm = false
    }

    fun toggleSelection(key: String) {
        selectedKeys = if (selectedKeys.contains(key)) {
            selectedKeys - key
        } else {
            selectedKeys + key
        }
    }

    BackHandler(enabled = sortMode) { exitEditMode() }

    LaunchedEffect(visibleFavorites) {
        reorderableFavorites.clear()
        reorderableFavorites.addAll(visibleFavorites)
        val validKeys = visibleFavorites.map(::favoriteKey).toSet()
        selectedKeys = selectedKeys.intersect(validKeys)
        if (sortMode && visibleFavorites.isEmpty()) {
            exitEditMode()
        }
    }

    LaunchedEffect(sortMode, visibleFavorites) {
        if (sortMode && visibleFavorites.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from: ItemPosition, to: ItemPosition ->
            if (!sortMode) return@rememberReorderableLazyListState
            val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
            val toKey = to.key as? String ?: return@rememberReorderableLazyListState
            val fromIndex = reorderableFavorites.indexOfFirst { favoriteKey(it) == fromKey }
            val toIndex = reorderableFavorites.indexOfFirst { favoriteKey(it) == toKey }
            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                reorderableFavorites.add(toIndex, reorderableFavorites.removeAt(fromIndex))
            }
        },
        canDragOver = { _, over -> sortMode && over.key is String },
        onDragEnd = { _, _ ->
            if (sortMode) {
                scope.launch {
                    favoriteRepo.reorderFavorites(
                        reorderableFavorites.map { "${it.source}:${it.id}" }
                    )
                }
            }
        }
    )

    LazyColumn(
        state = reorderState.listState,
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 8.dp + miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState)
    ) {
        val cardShape = RoundedCornerShape(LyraRadius.Medium)
        val displayedFavorites = filterFavoritePlaylists(reorderableFavorites, favoriteSearchQuery)
        item(key = "favorite_category_tabs") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                AdvancedGlassSurface(
                    role = AdvancedGlassRole.ScreenTopTab,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    tintColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedFavoriteCategory,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedFavoriteCategory == FAVORITE_CATEGORY_PLAYLIST,
                            onClick = {
                                if (selectedFavoriteCategory != FAVORITE_CATEGORY_PLAYLIST) {
                                    selectedFavoriteCategory = FAVORITE_CATEGORY_PLAYLIST
                                    exitEditMode()
                                }
                            },
                            text = { Text(stringResource(R.string.library_favorite_tab_playlists)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null
                                )
                            }
                        )
                        Tab(
                            selected = selectedFavoriteCategory == FAVORITE_CATEGORY_ARTIST,
                            onClick = {
                                if (selectedFavoriteCategory != FAVORITE_CATEGORY_ARTIST) {
                                    selectedFavoriteCategory = FAVORITE_CATEGORY_ARTIST
                                    exitEditMode()
                                }
                            },
                            text = { Text(stringResource(R.string.library_favorite_tab_artists)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null
                                )
                            }
                        )
                        Tab(
                            selected = selectedFavoriteCategory == FAVORITE_CATEGORY_HOT,
                            onClick = {
                                if (selectedFavoriteCategory != FAVORITE_CATEGORY_HOT) {
                                    selectedFavoriteCategory = FAVORITE_CATEGORY_HOT
                                    exitEditMode()
                                }
                            },
                            text = { Text(stringResource(R.string.library_favorite_tab_hot)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Bolt,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
        if (!sortMode && !isHotCategory) {
            item(key = "favorite_search") {
                LibraryInlineSearchField(
                    query = favoriteSearchQuery,
                    onQueryChange = { favoriteSearchQuery = it },
                    placeholderResId = R.string.library_favorite_search_hint
                )
            }
        }
        if (sortMode && !isHotCategory) {
            item(key = "favorite_sort_mode_header") {
                val allSelected =
                    selectedKeys.size == displayedFavorites.size && displayedFavorites.isNotEmpty()
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                pluralStringResource(
                                    R.plurals.common_selected_count,
                                    selectedKeys.size,
                                    selectedKeys.size
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            HapticIconButton(onClick = { exitEditMode() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_exit_multi_select)
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HapticTextButton(
                                    onClick = {
                                        selectedKeys = if (allSelected) {
                                            emptySet()
                                        } else {
                                            displayedFavorites.map(::favoriteKey).toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        if (allSelected) {
                                            stringResource(R.string.action_deselect_all)
                                        } else {
                                            stringResource(R.string.action_select_all)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                HapticTextButton(
                                    enabled = selectedKeys.isNotEmpty(),
                                    onClick = { showDeleteSelectedConfirm = true }
                                ) {
                                    Text(stringResource(R.string.common_delete_selected))
                                }
                            }
                        }
                    )
                }
            }
        }
        if (isHotCategory) {
            if (hotPlaylists == null) {
                item(key = "favorite_hot_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(
                    items = hotPlaylists,
                    key = { playlist -> "hot_playlist_${playlist.period.name}" }
                ) { playlist ->
                    val titleResId = hotPlaylistTitleResId(playlist.period)
                    val subtitle = if (playlist.tracks.isEmpty()) {
                        stringResource(R.string.library_hot_empty_hint)
                    } else {
                        stringResource(
                            R.string.library_hot_playlist_summary,
                            playlist.tracks.size,
                            formatPlayCount(context, playlist.totalPlayCount)
                        )
                    }
                    Card(
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                            .animateItem()
                            .clip(cardShape)
                            .clickable { onHotPlaylistClick(playlist.period) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = LyraSpacing.SM,
                                    vertical = LyraSpacing.SM
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bolt,
                                    contentDescription = null,
                                    tint = LocalNeriColors.current.accent,
                                    modifier = Modifier.size(LyraIconSize.XLarge)
                                )
                            }
                            Spacer(modifier = Modifier.width(LyraSpacing.MD))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(titleResId),
                                    style = LocalNeriTextStyles.current.body,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitle,
                                    style = LocalNeriTextStyles.current.captionSecondary,
                                    color = LocalNeriColors.current.secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else if (displayedFavorites.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                        .clip(cardShape)
                ) {
                    val isArtistCategory = selectedFavoriteCategory == FAVORITE_CATEGORY_ARTIST
                    val isSearchEmpty = favoriteSearchQuery.isNotBlank() && visibleFavorites.isNotEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = LyraSpacing.SM,
                                vertical = LyraSpacing.LG
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isArtistCategory) {
                                    Icons.Filled.AccountCircle
                                } else {
                                    Icons.AutoMirrored.Filled.QueueMusic
                                },
                                contentDescription = null,
                                tint = LocalNeriColors.current.tertiaryText,
                                modifier = Modifier.size(LyraIconSize.XLarge)
                            )
                        }
                        Spacer(modifier = Modifier.width(LyraSpacing.MD))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    if (isSearchEmpty) {
                                        R.string.library_favorite_search_empty
                                    } else if (isArtistCategory) {
                                        R.string.library_no_favorite_artist
                                    } else {
                                        R.string.playlist_no_favorite
                                    }
                                ),
                                style = LocalNeriTextStyles.current.body
                            )
                            Text(
                                text = stringResource(
                                    if (isSearchEmpty) {
                                        R.string.library_favorite_search_empty_hint
                                    } else if (isArtistCategory) {
                                        R.string.library_favorite_artist_hint
                                    } else {
                                        R.string.playlist_favorite_hint
                                    }
                                ),
                                style = LocalNeriTextStyles.current.captionSecondary,
                                color = LocalNeriColors.current.secondaryText
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = displayedFavorites,
                key = { favoriteKey(it) }
            ) { favorite ->
                val itemKey = favoriteKey(favorite)
                val isSelected = sortMode && selectedKeys.contains(itemKey)
                ReorderableItem(state = reorderState, key = itemKey) {
                    Card(
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                            } else if (sortMode) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                            .animateItem()
                            .clip(cardShape)
                            .combinedClickable(
                                onClick = {
                                    if (sortMode) {
                                        toggleSelection(itemKey)
                                        return@combinedClickable
                                    }
                                    when (favorite.source) {
                                        "netease" -> {
                                            onNeteasePlaylistClick(
                                                PlaylistSummary(
                                                    id = favorite.id,
                                                    name = favorite.name,
                                                    picUrl = favorite.coverUrl ?: "",
                                                    playCount = 0,
                                                    trackCount = favorite.trackCount
                                                )
                                            )
                                        }
                                        "neteaseAlbum" -> {
                                            onNeteaseAlbumClick(
                                                AlbumSummary(
                                                    id = favorite.id,
                                                    name = favorite.name,
                                                    picUrl = favorite.coverUrl.orEmpty(),
                                                    size = favorite.trackCount
                                                )
                                            )
                                        }
                                        FAVORITE_SOURCE_NETEASE_ARTIST -> {
                                            onNeteaseArtistClick(
                                                NeteaseArtistSummary(
                                                    id = favorite.id,
                                                    name = favorite.name
                                                )
                                            )
                                        }
                                        "youtubeMusic" -> {
                                            val resolvedBrowseId = favorite.browseId
                                                ?.takeIf { it.isNotBlank() }
                                                ?: favorite.playlistId
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?.let { "VL$it" }
                                            val resolvedPlaylistId = favorite.playlistId
                                                ?.takeIf { it.isNotBlank() }
                                                ?: resolvedBrowseId?.removePrefix("VL")
                                            if (
                                                !resolvedBrowseId.isNullOrBlank() &&
                                                !resolvedPlaylistId.isNullOrBlank()
                                            ) {
                                                onYouTubeMusicPlaylistClick(
                                                    YouTubeMusicPlaylist(
                                                        browseId = resolvedBrowseId,
                                                        playlistId = resolvedPlaylistId,
                                                        title = favorite.name,
                                                        subtitle = favorite.subtitle.orEmpty(),
                                                        coverUrl = favorite.coverUrl.orEmpty(),
                                                        trackCount = favorite.trackCount
                                                    )
                                                )
                                            }
                                        }
                                        "bili" -> {
                                            onBiliPlaylistClick(favorite.toBiliPlaylist())
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!sortMode) sortMode = true
                                    toggleSelection(itemKey)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = LyraSpacing.SM,
                                    vertical = LyraSpacing.SM
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!favorite.coverUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = offlineCachedImageRequest(
                                        context = context,
                                        data = favorite.coverUrl,
                                        sizePx = 192,
                                        allowHardware = false,
                                        offlineMode = offlineMode
                                    ),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = when (favorite.source) {
                                        FAVORITE_SOURCE_NETEASE_ARTIST -> Icons.Filled.AccountCircle
                                        "neteaseAlbum" -> Icons.Filled.Album
                                        else -> Icons.AutoMirrored.Filled.QueueMusic
                                    },
                                    contentDescription = null,
                                    tint = LocalNeriColors.current.tertiaryText,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(LyraSpacing.MD))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = favorite.name,
                                    style = LocalNeriTextStyles.current.body,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        R.string.library_favorite_source_format,
                                        favorite.trackCount,
                                        favoriteSourceLabel(favorite.source)
                                    ),
                                    style = LocalNeriTextStyles.current.captionSecondary,
                                    color = LocalNeriColors.current.secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (sortMode) {
                                Spacer(modifier = Modifier.width(LyraSpacing.SM))
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { toggleSelection(itemKey) }
                                )
                                Box(
                                    modifier = Modifier
                                        .detectReorder(reorderState)
                                        .padding(LyraSpacing.SM)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = stringResource(R.string.common_drag_handle),
                                        modifier = Modifier.size(LyraIconSize.Large)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = { Text(stringResource(R.string.dialog_confirm_delete)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.library_delete_selected_confirm,
                        selectedKeys.size,
                        selectedKeys.size
                    )
                )
            },
            confirmButton = {
                HapticTextButton(
                    onClick = {
                        val targets = reorderableFavorites.filter { favoriteKey(it) in selectedKeys }
                        scope.launch {
                            targets.forEach { favoriteRepo.removeFavorite(it.id, it.source) }
                            exitEditMode()
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                HapticTextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun favoriteSourceLabel(source: String): String {
    return when (source) {
        "youtubeMusic" -> "YouTube"
        "neteaseAlbum" -> "Netease Album"
        "netease" -> "Netease"
        "bili" -> "Bilibili"
        FAVORITE_SOURCE_NETEASE_ARTIST -> stringResource(R.string.library_favorite_source_artist)
        else -> source
    }
}

@Composable
private fun QqMusicPlaylistList(
    listState: LazyListState
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 8.dp + miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val cardShape = RoundedCornerShape(LyraRadius.Medium)
        // TODO: Implement QQ Music playlist list when type is available
        item {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = LyraSpacing.SM, vertical = LyraSpacing.XS)
                    .clip(cardShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LyraSpacing.SM,
                            vertical = LyraSpacing.LG
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = LocalNeriColors.current.tertiaryText,
                            modifier = Modifier.size(LyraIconSize.XLarge)
                        )
                    }
                    Spacer(modifier = Modifier.width(LyraSpacing.MD))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.library_qqmusic_coming),
                            style = LocalNeriTextStyles.current.body
                        )
                        Text(
                            text = stringResource(R.string.library_coming_soon),
                            style = LocalNeriTextStyles.current.captionSecondary,
                            color = LocalNeriColors.current.secondaryText
                        )
                    }
                }
            }
        }
    }
}
