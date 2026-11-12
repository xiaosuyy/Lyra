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
 * File: com.lyra.player.ui.screen.tab/HomeScreen
 * Created: 2025/8/8
 */

import android.app.Application
import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.lyra.player.ui.screen.account.AccountAvatar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.core.download.toPlaybackSongItem
import com.lyra.player.core.player.HeartModeRuntime
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.playlist.favorite.FavoritePlaylistRepository
import com.lyra.player.data.local.playlist.LocalPlaylistRepository
import com.lyra.player.data.local.playlist.model.LocalPlaylist
import com.lyra.player.data.playlist.usage.PlaylistUsageRepository
import com.lyra.player.data.local.playlist.system.FavoritesPlaylist
import com.lyra.player.data.local.playlist.system.LocalFilesPlaylist
import com.lyra.player.data.local.playlist.system.SystemLocalPlaylists
import com.lyra.player.data.playlist.usage.UsageEntry
import com.lyra.player.data.playlist.usage.buildLocalPlaylistUsageLookup
import com.lyra.player.data.platform.youtube.buildYouTubeMusicMediaUri
import com.lyra.player.data.local.media.displayAlbum
import com.lyra.player.ui.melox.theme.MeloxType
import com.lyra.player.ui.component.song.SongActionMenuItem
import com.lyra.player.ui.component.song.SongActionsMenu
import com.lyra.player.ui.component.song.rememberShowSongArtist
import com.lyra.player.ui.util.shouldAllowCollapsingTopAppBar
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.data.model.sameIdentityAs
import com.lyra.player.data.model.stableKey
import com.lyra.player.ui.theme.designsystem.LyraSongRow
import com.lyra.player.data.platform.youtube.stableYouTubeMusicId
import com.lyra.player.ui.LocalMiniPlayerHeight
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.screen.playlist.NeteaseSongCollectionDetailScreen
import com.lyra.player.ui.viewmodel.tab.HomeNeteasePlaylistSectionState
import com.lyra.player.ui.viewmodel.tab.HomeNeteaseSongSectionState
import com.lyra.player.ui.viewmodel.tab.HomeSectionState
import com.lyra.player.ui.viewmodel.tab.HomeViewModel
import com.lyra.player.ui.viewmodel.tab.NETEASE_PRIVATE_RADAR_PLAYLIST_ID
import com.lyra.player.ui.viewmodel.tab.NETEASE_TOPLIST_HOT_ID
import com.lyra.player.ui.viewmodel.tab.NeteaseHomeSongSource
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary
import com.lyra.player.ui.viewmodel.tab.YouTubeMusicPlaylist
import com.lyra.player.ui.viewmodel.tab.favoriteId
import com.lyra.player.ui.util.rememberPlaylistDisplayCoverUrl
import com.lyra.player.ui.util.rememberSongDisplayCoverUrl
import com.lyra.player.ui.util.currentWindowWidthDp
import com.lyra.player.ui.feedback.LyraOverlaySnackbarHost
import com.lyra.player.ui.feedback.showNeriSnackbar
import com.lyra.player.core.api.youtube.YouTubeMusicHomeShelf
import com.lyra.player.core.api.youtube.YouTubeMusicHomeItem
import com.lyra.player.core.api.youtube.YouTubeMusicParser
import com.lyra.player.data.settings.HomeLayoutConfig
import com.lyra.player.data.settings.HomeSectionId
import com.lyra.player.ui.screen.tab.home.HomeHero
import com.lyra.player.ui.theme.designsystem.LyraSectionHeader
import com.lyra.player.ui.viewmodel.tab.home.HomeFeedGroup
import com.lyra.player.ui.viewmodel.tab.home.HomeFeedIcon
import com.lyra.player.ui.viewmodel.tab.home.HomeFeedPresentation
import com.lyra.player.ui.viewmodel.tab.home.resolveEffectiveHomeSourceId
import com.lyra.player.ui.haptic.HapticIconButton
import com.lyra.player.util.media.fastScrollableImageRequest
import com.lyra.player.util.format.formatPlayCount
import kotlin.math.ceil
import kotlin.math.min
import java.time.LocalTime
import java.util.Locale

private const val HomeContinueHorizontalPaddingDp = 8f
private const val HomeContinueCardSpacingDp = 12f
private const val HomeContinueCardMaxWidthDp = 140f
private const val HomeContinueThreeSlotWidthDp = 300f
private const val HomeContinueTabletWidthDp = 600f
/** 渠道分区每次刷新时窗口偏移的步长（刷新后拿到下一批歌曲） */
private const val HOME_CHANNEL_WINDOW_SIZE = 3
/** 首页每个分区最多展示的数量（歌曲或歌单），刷新后轮换到下一批。 */
private const val HOME_SECTION_DISPLAY_LIMIT = 3
private const val HomeScrollKeyContinueHeader = "home:continue:header"
private const val HomeScrollKeyContinueContent = "home:continue:content"
private const val HomeScrollKeyYtGuess = "home:ytmusic:guess"
private const val HomeScrollKeyYtDaily = "home:ytmusic:daily"
private const val HomeScrollKeyYtMoreHeader = "home:ytmusic:more:header"
private const val HomeScrollKeyYtMoreLoading = "home:ytmusic:more:loading"
private const val HomeScrollKeyYtMoreError = "home:ytmusic:more:error"
private const val HomeScrollKeyYtShelvesLoading = "home:ytmusic:shelves:loading"
private const val HomeScrollKeyYtShelvesError = "home:ytmusic:shelves:error"
private const val HomeScrollKeyYtEmptyFeedLoading = "home:ytmusic:empty-feed:loading"
private const val HomeScrollKeyYtEmptyFeedError = "home:ytmusic:empty-feed:error"
private const val HomeScrollKeyNeteaseRadarPlaylists = "home:netease:radar-playlists"

private data class MeloxHomeQuickAction(
    val key: String,
    val title: String,
    val eyebrow: String,
    val subtitle: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val songs: List<SongItem>,
    val requiresLogin: Boolean = false,
    val playlist: PlaylistSummary? = null,
    /** 可选雷达模式的接口 Mode 值（空 = 该卡片不支持模式切换）。 */
    val radarModes: List<Int> = emptyList(),
    /** 当前雷达模式（接口 Mode 值）。 */
    val radarMode: Int = HomeLayoutConfig.RADAR_MODE_DEFAULT
)

internal fun shouldShowHomeContinueSection(
    usageLoaded: Boolean,
    hasUsage: Boolean
): Boolean = !usageLoaded || hasUsage

internal fun homeNeteaseRadarPlaylistScrollKey(id: Long): String {
    return "$HomeScrollKeyNeteaseRadarPlaylists:playlist:$id"
}

private fun homeYtMusicPlaylistScrollKey(playlist: YouTubeMusicPlaylist): String {
    return "home:ytmusic:playlist:${playlist.favoriteId()}"
}

private fun homeYtMusicShelfScrollKey(shelfIndex: Int, title: String): String {
    return "home:ytmusic:shelf:$shelfIndex:${title.hashCode()}"
}

private fun homeYtMusicHomeItemScrollKey(
    shelfKey: String,
    itemIndex: Int,
    item: YouTubeMusicHomeItem
): String {
    val stableId = item.browseId.ifBlank { item.videoId }.ifBlank { item.title }
    return "$shelfKey:item:$itemIndex:${stableId.hashCode()}"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    showTrendingCard: Boolean = true,
    showRadarCard: Boolean = true,
    showRecommendedCard: Boolean = true,
    usageEntries: List<UsageEntry> = emptyList(),
    usageLoaded: Boolean = true,
    offlineMode: Boolean = false,
    onItemClick: (PlaylistSummary) -> Unit = {},
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit = {},
    gridState: LazyGridState,
    radarPlaylistListState: LazyListState,
    topAppBarState: TopAppBarState,
    onScrollAnchorIndexesChanged: (Map<String, Int>) -> Unit = {},
    onOpenRecent: (UsageEntry) -> Unit = {},
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {},
    avatarUri: String? = null,
    onOpenAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = appContext as Application
                HomeViewModel(app)
            }
        }
    )
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val localPlaylistRepo = remember(appContext) { LocalPlaylistRepository.getInstance(appContext) }
    var localPlaylists by remember { mutableStateOf<List<LocalPlaylist>>(emptyList()) }
    var localPlaylistsReady by remember { mutableStateOf(false) }
    LaunchedEffect(appContext, localPlaylistRepo) {
        localPlaylistsReady = false
        val initializedRepo = withContext(Dispatchers.IO) {
            if (localPlaylistRepo.awaitInitialized()) localPlaylistRepo else null
        }
        if (initializedRepo == null) return@LaunchedEffect
        initializedRepo.playlists.collect { playlists ->
            localPlaylists = playlists
            localPlaylistsReady = true
        }
    }
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val downloadedSongs by GlobalDownloadManager.downloadedSongs.collectAsStateWithLifecycle()
    val downloadedPlaybackCoverCandidates = remember(downloadedSongs) {
        downloadedSongs.map { it.toPlaybackSongItem() }
    }
    val favoriteKeys = remember(favorites) {
        favorites.mapTo(mutableSetOf()) { "${it.source}:${it.id}" }
    }
    val favoriteSongs = remember(localPlaylists, context) {
        localPlaylists
            .firstOrNull { FavoritesPlaylist.isSystemPlaylist(it, context) }
            ?.songs
            .orEmpty()
    }
    val showSongArtist = rememberShowSongArtist()
    val localPlaylistUsageLookup = remember(localPlaylists, context) {
        buildLocalPlaylistUsageLookup(localPlaylists, context)
    }

    val hasLocalUsage = remember(usageEntries) {
        usageEntries.any {
            it.source == PlaylistUsageRepository.SOURCE_LOCAL ||
                it.source == PlaylistUsageRepository.SOURCE_LOCAL_ARTIST
        }
    }
    LaunchedEffect(
        hasLocalUsage,
        localPlaylistsReady,
        localPlaylists,
        downloadedPlaybackCoverCandidates
    ) {
        if (hasLocalUsage && localPlaylistsReady) {
            withContext(Dispatchers.Default) {
                AppContainer.playlistUsageRepo.syncLocalEntries(
                    playlists = localPlaylists,
                    localFilesCoverCandidates = downloadedPlaybackCoverCandidates
                )
                AppContainer.playlistUsageRepo.syncLocalArtistEntries(localPlaylists)
            }
        }
    }

    val appBarTitle = stringResource(R.string.nav_home)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        canScroll = {
            shouldAllowCollapsingTopAppBar(
                canScrollForward = gridState.canScrollForward,
                canScrollBackward = gridState.canScrollBackward,
                collapsedFraction = topAppBarState.collapsedFraction
            )
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val guessYouLikeTitle = stringResource(R.string.home_ytmusic_guess_you_like)
    val dailyDiscoverTitle = stringResource(R.string.home_ytmusic_daily_discover)
    val moreRecommendationsTitle = stringResource(R.string.home_ytmusic_more_recommendations)
    val favoriteAddedText = stringResource(R.string.favorite_added)
    val favoriteRemovedText = stringResource(R.string.favorite_removed)
    val ytmSections = remember(ui.ytMusicHomeShelves.items) {
        classifyYouTubeMusicShelves(ui.ytMusicHomeShelves.items)
    }
    val hasVisibleYtMusicFeed = remember(ytmSections) {
        ytmSections.guessYouLike != null ||
            ytmSections.dailyDiscover != null ||
            ytmSections.remaining.any { shelf ->
                shelf.shouldRenderAsSongShelf() || shelf.hasRenderablePlaylistItems()
            }
    }
    val scope = rememberCoroutineScope()
    val showContinue = shouldShowHomeContinueSection(
        usageLoaded = usageLoaded,
        hasUsage = usageEntries.isNotEmpty()
    )
    val isInternational = ui.internationalizationEnabled
    val showNeteaseTrending = showTrendingCard
    val showNeteaseRadar = showRadarCard
    val showOnlineFeeds = !offlineMode
    var wasOffline by remember { mutableStateOf(offlineMode) }
    val windowWidthDp = currentWindowWidthDp()
    val isTabletLayout = windowWidthDp >= 720.dp
    val pageHorizontalPadding = if (isTabletLayout) 28.dp else 16.dp
    val gridMinCellSize = if (isTabletLayout) 156.dp else 120.dp
    val gridContentPadding = if (isTabletLayout) 14.dp else 8.dp
    val gridSpacing = if (isTabletLayout) 14.dp else 10.dp

    fun toggleHomeSongFavorite(song: SongItem, isFavorite: Boolean) {
        scope.launch {
            if (isFavorite) {
                localPlaylistRepo.removeFromFavorites(song)
                snackbarHostState.showNeriSnackbar(favoriteRemovedText)
            } else {
                localPlaylistRepo.addToFavorites(song)
                snackbarHostState.showNeriSnackbar(favoriteAddedText)
            }
        }
    }
    val showHomeSnackbar: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showNeriSnackbar(message)
        }
    }

    val homeLayout by AppContainer.homeLayoutRepository.config.collectAsStateWithLifecycle()
    // 统一首页分组：网易云与渠道都收敛为 HomeFeedGroup，UI 只负责渲染。
    val homeFeedGroups by vm.homeFeedGroups.collectAsStateWithLifecycle()
    // 当前播放歌曲（只读，仅用于 Home 歌曲列表高亮，不改变播放逻辑）
    val currentSong by PlayerManager.currentSongFlow.collectAsStateWithLifecycle()
    val homeSourceOptions by vm.homeSourceOptions.collectAsStateWithLifecycle()
    var homeRefreshGeneration by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val isMeloxNeteaseHome = !isInternational && !offlineMode
    // 首页整体推荐来源：网易云或 QQ 音乐（QQ 音源未就绪时自动回落到网易云）。
    val homeSourceIds = remember(homeSourceOptions) {
        homeSourceOptions.mapTo(HashSet()) { it.sourceId }
    }
    val isQqHome = isMeloxNeteaseHome &&
        resolveEffectiveHomeSourceId(homeLayout.mainSourceId, homeSourceIds) ==
        MusicSourceIds.TENCENT
    val qqSession by AppContainer.qqMusicSessionRepo.sessionFlow.collectAsStateWithLifecycle()
    var qqQuickLoading by remember { mutableStateOf<String?>(null) }
    var qqPlaylistLoading by remember { mutableStateOf(false) }
    var quickActionDetail by remember { mutableStateOf<MeloxHomeQuickAction?>(null) }
    var heartModeLoading by remember { mutableStateOf(false) }
    val dailyRecommendSongs = ui.radarSongSections
        .firstOrNull { it.source == NeteaseHomeSongSource.DAILY_RECOMMEND }
        ?.section?.items.orEmpty()
    val hotSongs = ui.trendingSongSections
        .firstOrNull { it.source == NeteaseHomeSongSource.TOP_HOT }
        ?.section?.items.orEmpty()
        .ifEmpty {
            ui.trendingSongSections
                .firstOrNull { it.source == NeteaseHomeSongSource.TOP_SOARING }
                ?.section?.items.orEmpty()
        }
    val radarSongs = ui.radarSongSections
        .firstOrNull { it.source == NeteaseHomeSongSource.PERSONAL_RADAR }
        ?.section?.items.orEmpty()
    val roamSongs = ui.radarSongSections
        .firstOrNull { it.source == NeteaseHomeSongSource.PRIVATE_FM }
        ?.section?.items.orEmpty()
    val neteaseQuickActions = listOf(
        MeloxHomeQuickAction(
            key = "daily",
            title = stringResource(R.string.home_netease_daily_songs),
            eyebrow = stringResource(R.string.home_eyebrow_daily),
            subtitle = stringResource(R.string.home_subtitle_daily),
            icon = Icons.Outlined.CalendarMonth,
            colors = listOf(Color(0xFFFF5B8A), Color(0xFFFF3147)),
            songs = dailyRecommendSongs,
            requiresLogin = true
        ),
        MeloxHomeQuickAction(
            key = "hot",
            title = stringResource(R.string.home_netease_hot_rank),
            eyebrow = stringResource(R.string.home_eyebrow_hot),
            subtitle = stringResource(R.string.home_subtitle_hot),
            icon = Icons.Outlined.LocalFireDepartment,
            colors = listOf(Color(0xFFFFA14A), Color(0xFFFF5A36)),
            songs = hotSongs,
            playlist = PlaylistSummary(
                id = NETEASE_TOPLIST_HOT_ID,
                name = stringResource(R.string.home_netease_hot_rank),
                picUrl = hotSongs.firstOrNull()?.coverUrl.orEmpty(),
                playCount = 0L,
                trackCount = hotSongs.size
            )
        ),
        MeloxHomeQuickAction(
            key = "heart",
            title = stringResource(R.string.home_netease_heart_mode),
            eyebrow = stringResource(R.string.home_eyebrow_heart),
            subtitle = stringResource(R.string.home_subtitle_heart),
            icon = Icons.Outlined.FavoriteBorder,
            colors = listOf(Color(0xFFFF6EAC), Color(0xFF9B5DE5)),
            songs = emptyList(),
            requiresLogin = true
        ),
        MeloxHomeQuickAction(
            key = "radar",
            title = stringResource(R.string.recommend_radar),
            eyebrow = stringResource(R.string.home_eyebrow_radar),
            subtitle = stringResource(R.string.home_subtitle_radar),
            icon = Icons.Outlined.Radar,
            colors = listOf(Color(0xFF6B7BFF), Color(0xFF8C52FF)),
            songs = radarSongs,
            playlist = PlaylistSummary(
                id = NETEASE_PRIVATE_RADAR_PLAYLIST_ID,
                name = stringResource(R.string.recommend_radar),
                picUrl = radarSongs.firstOrNull()?.coverUrl.orEmpty(),
                playCount = 0L,
                trackCount = radarSongs.size
            )
        ),
        MeloxHomeQuickAction(
            key = "roam",
            title = stringResource(R.string.home_netease_private_fm),
            eyebrow = stringResource(R.string.home_eyebrow_roam),
            subtitle = stringResource(R.string.home_subtitle_roam),
            icon = Icons.Outlined.Explore,
            colors = listOf(Color(0xFF26C6DA), Color(0xFF4285F4)),
            songs = roamSongs,
            requiresLogin = true
        )
    )
    // QQ 音乐首页首行卡片：雷达（按账号推荐，点开即播）、热歌榜、新歌首发、私人漫游。
    val qqRadarMode = homeLayout.qqRadarMode
    val qqRadarEyebrowRes = when (qqRadarMode) {
        HomeLayoutConfig.RADAR_MODE_EXPLORE -> R.string.home_qq_radar_eyebrow_explore
        HomeLayoutConfig.RADAR_MODE_FAMILIAR -> R.string.home_qq_radar_eyebrow_familiar
        else -> R.string.home_qq_radar_eyebrow
    }
    val qqRadarSubtitleRes = when (qqRadarMode) {
        HomeLayoutConfig.RADAR_MODE_EXPLORE -> R.string.home_qq_radar_subtitle_explore
        HomeLayoutConfig.RADAR_MODE_FAMILIAR -> R.string.home_qq_radar_subtitle_familiar
        else -> R.string.home_qq_radar_subtitle
    }
    val qqQuickActions = listOf(
        MeloxHomeQuickAction(
            key = "qq_radar",
            title = stringResource(R.string.home_qq_radar),
            eyebrow = stringResource(qqRadarEyebrowRes),
            subtitle = stringResource(qqRadarSubtitleRes),
            icon = Icons.Outlined.Radar,
            colors = listOf(Color(0xFF6B7BFF), Color(0xFF8C52FF)),
            songs = emptyList(),
            requiresLogin = true,
            radarModes = listOf(
                HomeLayoutConfig.RADAR_MODE_EXPLORE,
                HomeLayoutConfig.RADAR_MODE_DEFAULT,
                HomeLayoutConfig.RADAR_MODE_FAMILIAR
            ),
            radarMode = qqRadarMode
        ),
        MeloxHomeQuickAction(
            key = "qq_hot",
            title = stringResource(R.string.home_netease_hot_rank),
            eyebrow = stringResource(R.string.home_eyebrow_hot),
            subtitle = stringResource(R.string.home_subtitle_hot),
            icon = Icons.Outlined.LocalFireDepartment,
            colors = listOf(Color(0xFFFFA14A), Color(0xFFFF5A36)),
            songs = emptyList()
        ),
        MeloxHomeQuickAction(
            key = "qq_new",
            title = stringResource(R.string.home_qq_new_songs),
            eyebrow = stringResource(R.string.home_qq_new_eyebrow),
            subtitle = stringResource(R.string.home_qq_new_subtitle),
            icon = Icons.Outlined.Star,
            colors = listOf(Color(0xFF59C173), Color(0xFF26A69A)),
            songs = emptyList()
        ),
        MeloxHomeQuickAction(
            key = "qq_roam",
            title = stringResource(R.string.home_qq_roam),
            eyebrow = stringResource(R.string.home_eyebrow_roam),
            subtitle = stringResource(R.string.home_subtitle_roam),
            icon = Icons.Outlined.Explore,
            colors = listOf(Color(0xFF26C6DA), Color(0xFF4285F4)),
            songs = emptyList(),
            requiresLogin = true
        )
    )
    val meloxQuickActions = if (isQqHome) qqQuickActions else neteaseQuickActions
    val qqPlaylistEyebrow = stringResource(R.string.home_qq_recommend_playlists)
    val meloxGreeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }
    val allRecentTrendingSongs = remember(ui.trendingSongSections) {
        ui.trendingSongSections
            .flatMap { it.section.items }
            .distinctBy { it.stableKey() }
            .take(12)
    }
    fun triggerHomeRefresh() {
        if (offlineMode) return
        if (isInternational) {
            vm.refreshYtMusicHome()
        } else if (!isQqHome) {
            vm.refreshNeteaseHome()
        }
        // 刷新即换批：新的 Home generation + seed（force 绕过初始化守卫）
        vm.beginHomeGeneration(force = true)
        homeRefreshGeneration += 1
    }
    // QQ 音乐的推荐歌单没有网易云详情页，点击后先取歌单内歌曲，再用统一详情页展示。
    fun openPlaylist(playlist: PlaylistSummary) {
        if (!isQqHome) {
            onItemClick(playlist)
            return
        }
        if (qqPlaylistLoading) return
        qqPlaylistLoading = true
        scope.launch {
            try {
                val songs = if (playlist.id < 0L) {
                    vm.loadQqToplistSongs(-playlist.id)
                } else {
                    vm.loadQqPlaylistSongs(playlist.id.toString())
                }
                if (songs.isEmpty()) {
                    showHomeSnackbar(context.getString(R.string.home_no_recommendations))
                } else {
                    quickActionDetail = MeloxHomeQuickAction(
                        key = "qq_playlist_${playlist.id}",
                        title = playlist.name,
                        eyebrow = if (playlist.id < 0L) {
                            context.getString(R.string.home_qq_toplist)
                        } else {
                            qqPlaylistEyebrow
                        },
                        subtitle = "",
                        icon = Icons.Outlined.MusicNote,
                        colors = listOf(Color(0xFF5B8DEF), Color(0xFF8C52FF)),
                        songs = songs
                    )
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                showHomeSnackbar(
                    error.message ?: context.getString(R.string.home_qq_load_failed)
                )
            } finally {
                qqPlaylistLoading = false
            }
        }
    }
    fun openQqQuickAction(action: MeloxHomeQuickAction) {
        if (action.requiresLogin && !qqSession.isLoggedIn) {
            showHomeSnackbar(context.getString(R.string.home_qq_login_required))
            return
        }
        if (qqQuickLoading != null) return
        qqQuickLoading = action.key
        scope.launch {
            try {
                val songs = when (action.key) {
                    "qq_radar" -> vm.loadQqRadarSongs(homeLayout.qqRadarMode)
                    "qq_roam" -> vm.loadQqRoamSongs()
                    "qq_hot" -> vm.loadQqHotSongs()
                    else -> vm.loadQqNewSongs()
                }
                when {
                    songs.isEmpty() -> showHomeSnackbar(
                        context.getString(R.string.home_no_recommendations)
                    )
                    action.key == "qq_radar" -> onSongClick(songs, 0)
                    else -> quickActionDetail = action.copy(songs = songs)
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                showHomeSnackbar(
                    error.message ?: context.getString(R.string.home_qq_load_failed)
                )
            } finally {
                qqQuickLoading = null
            }
        }
    }
    // Fix-01B：是否换批由 ViewModel 跨 Tab 存活的状态决定（首次进入换批；普通返回不换批）。
    LaunchedEffect(Unit) {
        vm.beginHomeGeneration()
    }
    val enabledOnlineSections = remember(showNeteaseTrending, showNeteaseRadar, showRecommendedCard) {
        buildSet {
            if (showNeteaseTrending) add(HomeSectionId.TRENDING)
            if (showNeteaseRadar) add(HomeSectionId.RADAR)
            if (showRecommendedCard) add(HomeSectionId.RECOMMENDED)
        }
    }
    // Fix-01B：渠道请求去重由 ViewModel 负责；此处仅按现有条件调用。
    LaunchedEffect(
        homeLayout.sources,
        homeLayout.mainSourceId,
        homeSourceOptions,
        enabledOnlineSections,
        homeRefreshGeneration
    ) {
        // 数据加载全部在 ViewModel 内完成；UI 只消费统一分组。
        vm.refreshChannelHomeSections(
            enabledSections = enabledOnlineSections,
            windowOffset = homeRefreshGeneration * HOME_CHANNEL_WINDOW_SIZE
        )
    }

    LaunchedEffect(offlineMode, isInternational) {
        vm.setOfflineMode(offlineMode)
        if (offlineMode) {
            wasOffline = true
            return@LaunchedEffect
        }

        if (!wasOffline) return@LaunchedEffect

        wasOffline = false
        if (isInternational) {
            vm.refreshYtMusicHome()
        } else {
            vm.refreshNeteaseHome()
        }
    }

    val hasVisibleSections =
            showContinue || (showOnlineFeeds && (showNeteaseTrending || showNeteaseRadar || showRecommendedCard || isInternational))

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LargeTopAppBar(
                title = {
                    Text(
                        appBarTitle,
                        style = MeloxType.largeTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    AccountAvatar(
                        avatarUri = avatarUri,
                        size = 36.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable(onClick = onOpenAccount),
                    )
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = pageHorizontalPadding, vertical = 4.dp)
                    .widthIn(max = 1240.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
            ) {
                if (!hasVisibleSections) {
                    SideEffect {
                        onScrollAnchorIndexesChanged(emptyMap())
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (offlineMode) {
                                stringResource(R.string.home_offline_no_continue)
                            } else {
                                stringResource(R.string.home_all_cards_hidden)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    return@Box
                }

                val miniPlayerHeight = LocalMiniPlayerHeight.current
                val homeLoadingText = stringResource(R.string.home_loading)
                val scrollAnchorIndexes = linkedMapOf<String, Int>()
                var nextGridItemIndex = 0
                fun registerGridItemKey(key: String): String {
                    scrollAnchorIndexes[key] = nextGridItemIndex
                    nextGridItemIndex += 1
                    return key
                }

                fun LazyGridScope.renderContinueSection() {
                    // Fix-01C：Continue Playing = 当前播放单曲（不再展示歌单）；无当前歌曲则隐藏该 Section。
                    val continueSong = currentSong ?: return
                    item(
                        key = registerGridItemKey(HomeScrollKeyContinueHeader),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        SectionHeader(
                            icon = Icons.Outlined.History,
                            title = stringResource(R.string.player_continue)
                        )
                    }
                    item(
                        key = registerGridItemKey(HomeScrollKeyContinueContent),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        LyraSongRow(
                            song = continueSong,
                            isCurrent = true,
                            showArtist = showSongArtist,
                            onClick = { onSongClick(listOf(continueSong), 0) }
                        )
                    }
                }

                // 统一渲染：只按 Group 的 presentation 选择组件，不区分平台。
                fun LazyGridScope.addHomeFeedGroup(group: HomeFeedGroup) {
                    val sectionKey = "home:group:${group.key}"
                    item(
                        key = registerGridItemKey("$sectionKey:header"),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        LyraSectionHeader(
                            title = stringResource(group.titleResId),
                            icon = homeFeedIcon(group.icon),
                            modifier = Modifier.animateItem()
                        )
                    }
                    when (group.presentation) {
                        HomeFeedPresentation.SONG_LIST -> {
                            val songs = group.songs
                            when {
                                songs.items.isNotEmpty() -> {
                                    val displaySongs = songs.items
                                        .take(HOME_SECTION_DISPLAY_LIMIT)
                                    displaySongs.forEachIndexed { songIndex, song ->
                                        item(
                                            key = registerGridItemKey(
                                                "$sectionKey:song:${song.stableKey()}"
                                            ),
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            LyraSongRow(
                                                song = song,
                                                isCurrent = currentSong?.sameIdentityAs(song) == true,
                                                showArtist = showSongArtist,
                                                onClick = { onSongClick(displaySongs, songIndex) },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }

                                songs.loading -> item(
                                    key = registerGridItemKey("$sectionKey:loading"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) { SectionLoadingState(homeLoadingText) }

                                !songs.error.isNullOrBlank() -> item(
                                    key = registerGridItemKey("$sectionKey:error"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) { SectionErrorState(detail = songs.error.orEmpty()) }
                            }
                        }

                        HomeFeedPresentation.PLAYLIST_GRID -> {
                            val playlists = group.playlists
                            when {
                                playlists.items.isNotEmpty() -> item(
                                    key = registerGridItemKey("$sectionKey:content"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = gridContentPadding),
                                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth()
                                    ) {
                                        items(
                                            items = playlists.items
                                                .take(HOME_SECTION_DISPLAY_LIMIT),
                                            key = { it.id }
                                        ) { playlist ->
                                            Box(Modifier.width(148.dp)) {
                                                PlaylistCard(
                                                    playlist = playlist,
                                                    isFavorite = favoriteKeys.contains("netease:${playlist.id}"),
                                                    onClick = { openPlaylist(playlist) },
                                                    onShowSnackbar = showHomeSnackbar,
                                                    offlineMode = offlineMode,
                                                    allowFavorite = !isQqHome
                                                )
                                            }
                                        }
                                    }
                                }

                                playlists.loading -> item(
                                    key = registerGridItemKey("$sectionKey:loading"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) { SectionLoadingState(homeLoadingText) }

                                !playlists.error.isNullOrBlank() -> item(
                                    key = registerGridItemKey("$sectionKey:error"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) { SectionErrorState(detail = playlists.error.orEmpty()) }
                            }
                        }

                        HomeFeedPresentation.PLAYLIST_STRIP -> {
                            val playlists = group.playlists
                            when {
                                playlists.items.isNotEmpty() -> item(
                                    key = registerGridItemKey("$sectionKey:content"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    RadarPlaylistStrip(
                                        playlists = playlists.items
                                            .take(HOME_SECTION_DISPLAY_LIMIT),
                                        favoriteKeys = favoriteKeys,
                                        listState = radarPlaylistListState,
                                        onClick = onItemClick,
                                        onShowSnackbar = showHomeSnackbar,
                                        offlineMode = offlineMode
                                    )
                                }

                                playlists.loading -> item(
                                    key = registerGridItemKey("$sectionKey:loading"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) { SectionLoadingState(homeLoadingText) }

                                !playlists.error.isNullOrBlank() -> item(
                                    key = registerGridItemKey("$sectionKey:error"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) { SectionErrorState(detail = playlists.error.orEmpty()) }
                            }
                        }
                    }
                }

                fun LazyGridScope.addMeloxPlaylistSection(
                    group: HomeFeedGroup,
                    titleRes: Int = group.titleResId
                ) {
                    val sectionKey = "home:melox:${group.key}"
                    val items = group.playlists.items
                    if (items.isEmpty() && !group.playlists.loading && group.playlists.error.isNullOrBlank()) {
                        return
                    }
                    item(
                        key = registerGridItemKey("$sectionKey:header"),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        MeloxSectionTitle(
                            title = stringResource(titleRes),
                            trailing = null,
                            modifier = Modifier
                                .animateItem()
                        )
                    }
                    item(
                        key = registerGridItemKey("$sectionKey:content"),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        when {
                            items.isNotEmpty() -> LazyRow(
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = items.take(10),
                                    key = { _, playlist -> playlist.id }
                                ) { index, playlist ->
                                    Box(
                                        Modifier.size(
                                            width = if (index == 0) 246.dp else 174.dp,
                                            height = 254.dp
                                        )
                                    ) {
                                        PlaylistCard(
                                            playlist = playlist,
                                            isFavorite = favoriteKeys.contains("netease:${playlist.id}"),
                                            onClick = { openPlaylist(playlist) },
                                            onShowSnackbar = showHomeSnackbar,
                                            offlineMode = offlineMode,
                                            large = true,
                                            allowFavorite = !isQqHome
                                        )
                                    }
                                }
                            }

                            group.playlists.loading -> SectionLoadingState(homeLoadingText)

                            else -> SectionErrorState(
                                detail = group.playlists.error.orEmpty()
                            )
                        }
                    }
                }

                fun LazyGridScope.addMeloxSongSection(
                    group: HomeFeedGroup,
                    titleRes: Int = group.titleResId
                ) {
                    val sectionKey = "home:melox:${group.key}"
                    val songs = group.songs.items
                    if (songs.isEmpty() && !group.songs.loading && group.songs.error.isNullOrBlank()) {
                        return
                    }
                    item(
                        key = registerGridItemKey("$sectionKey:header"),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        MeloxSectionTitle(
                            title = stringResource(titleRes),
                            trailing = null,
                            modifier = Modifier
                                .animateItem()
                        )
                    }
                    item(
                        key = registerGridItemKey("$sectionKey:content"),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        when {
                            songs.isNotEmpty() -> MeloxSongCarousel(
                                songs = songs.take(12),
                                isTabletLayout = isTabletLayout,
                                offlineMode = offlineMode,
                                onSongClick = onSongClick,
                                modifier = Modifier.animateItem()
                            )

                            group.songs.loading -> SectionLoadingState(homeLoadingText)

                            else -> SectionErrorState(
                                detail = group.songs.error.orEmpty()
                            )
                        }
                    }
                }

                fun LazyGridScope.addMeloxHomeContent() {
                    item(
                        key = registerGridItemKey("home:melox:greeting"),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Text(
                            text = stringResource(meloxGreeting),
                            style = MeloxType.title2,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                            modifier = Modifier
                                .padding(
                                    horizontal = 0.dp,
                                    vertical = gridContentPadding
                                )
                                .animateItem()
                        )
                    }
                    item(
                        key = registerGridItemKey("home:melox:quick_actions"),
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        MeloxQuickActionRow(
                            actions = meloxQuickActions,
                            contentPadding = 0.dp,
                            loadingKey = when {
                                isQqHome -> qqQuickLoading
                                heartModeLoading -> "heart"
                                else -> null
                            },
                            onRadarModeChange = { mode ->
                                AppContainer.homeLayoutRepository.setQqRadarMode(mode)
                            },
                            onAction = { action ->
                                when {
                                    isQqHome -> openQqQuickAction(action)
                                    action.key == "heart" -> {
                                        when {
                                            !ui.hasLogin -> showHomeSnackbar(
                                                context.getString(R.string.home_login_required)
                                            )
                                            heartModeLoading -> Unit
                                            else -> {
                                                heartModeLoading = true
                                                scope.launch {
                                                    try {
                                                        val songs = vm.loadHeartModeSongs()
                                                        if (songs.isNotEmpty()) {
                                                            HeartModeRuntime.markQueue(songs)
                                                            onSongClick(songs, 0)
                                                        } else {
                                                            showHomeSnackbar(
                                                                context.getString(R.string.home_no_recommendations)
                                                            )
                                                        }
                                                    } catch (error: kotlinx.coroutines.CancellationException) {
                                                        throw error
                                                    } catch (error: Exception) {
                                                        showHomeSnackbar(
                                                            error.message
                                                                ?: context.getString(R.string.home_heart_mode_empty)
                                                        )
                                                    } finally {
                                                        heartModeLoading = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    action.playlist != null -> onItemClick(action.playlist)
                                    action.songs.isNotEmpty() -> {
                                        quickActionDetail = action
                                    }
                                    else -> {
                                        showHomeSnackbar(
                                            if (action.requiresLogin && !ui.hasLogin) {
                                                context.getString(R.string.home_login_required)
                                            } else {
                                                context.getString(R.string.home_no_recommendations)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }

                    if (isQqHome) {
                        // QQ 音乐推荐页节奏：每日30首 → 为你精选 → 排行榜 → 推荐歌单 → 新歌首发。
                        // 雷达已由首行卡片承担，不再重复出分区。
                        if (showOnlineFeeds) {
                            val qqTrending = homeFeedGroups[HomeSectionId.TRENDING].orEmpty()
                            if (showNeteaseTrending) {
                                qqTrending
                                    .filter { it.key.endsWith(":DAILY") }
                                    .forEach { addMeloxSongSection(it) }
                            }
                            if (showRecommendedCard) {
                                homeFeedGroups[HomeSectionId.RECOMMENDED]
                                    .orEmpty()
                                    .forEach { addMeloxPlaylistSection(it) }
                            }
                            if (showNeteaseTrending) {
                                qqTrending
                                    .filterNot { it.key.endsWith(":DAILY") }
                                    .forEach { addMeloxSongSection(it) }
                            }
                        }
                    } else {
                        val recommendedGroups = homeFeedGroups[HomeSectionId.RECOMMENDED].orEmpty()
                        val featuredRecommended = recommendedGroups.firstOrNull {
                            it.presentation == HomeFeedPresentation.PLAYLIST_GRID &&
                                it.playlists.items.isNotEmpty()
                        } ?: recommendedGroups.firstOrNull()
                        if (showOnlineFeeds && showRecommendedCard) {
                            if (featuredRecommended != null) {
                                addMeloxPlaylistSection(
                                    group = featuredRecommended,
                                    titleRes = R.string.home_netease_daily_songs
                                )
                            }
                        }

                        if (showOnlineFeeds && showNeteaseTrending) {
                            val trendingGroups = homeFeedGroups[HomeSectionId.TRENDING].orEmpty()
                            trendingGroups
                                .firstOrNull { it.key.contains("PERSONALIZED_NEW_SONGS") }
                                ?.let { group ->
                                    addMeloxSongSection(group, titleRes = R.string.recommend_for_you)
                                }
                            if (allRecentTrendingSongs.isNotEmpty()) {
                                item(
                                    key = registerGridItemKey("home:melox:all_recent_trending:header"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    MeloxSectionTitle(
                                        title = stringResource(R.string.home_all_recent_trending),
                                        trailing = null,
                                        modifier = Modifier
                                            .animateItem()
                                    )
                                }
                                item(
                                    key = registerGridItemKey("home:melox:all_recent_trending:content"),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    MeloxSongCarousel(
                                        songs = allRecentTrendingSongs,
                                        isTabletLayout = isTabletLayout,
                                        offlineMode = offlineMode,
                                        onSongClick = onSongClick,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }

                        if (showOnlineFeeds && showRecommendedCard) {
                            recommendedGroups
                                .filter { it.key != featuredRecommended?.key }
                                .forEach { addMeloxPlaylistSection(it) }
                        }

                        if (showOnlineFeeds && showNeteaseRadar) {
                            homeFeedGroups[HomeSectionId.RADAR].orEmpty().forEach { group ->
                                if (group.presentation == HomeFeedPresentation.SONG_LIST) {
                                    addMeloxSongSection(group)
                                } else {
                                    addMeloxPlaylistSection(group)
                                }
                            }
                        }
                    }
                }

                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        triggerHomeRefresh()
                    },
                    modifier = Modifier.fillMaxSize(),
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(gridMinCellSize),
                    contentPadding = PaddingValues(
                        start = if (isMeloxNeteaseHome) 0.dp else gridContentPadding,
                        end = if (isMeloxNeteaseHome) 0.dp else gridContentPadding,
                        top = gridContentPadding,
                        bottom = gridContentPadding + miniPlayerHeight
                    ),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isMeloxNeteaseHome) {
                        addMeloxHomeContent()
                    } else {
                    // Hero：使用当前推荐歌单作为视觉重点（大封面 + 克制 Dynamic Color 氛围）
                    val heroPlaylist = homeFeedGroups[HomeSectionId.RECOMMENDED].orEmpty()
                        .firstOrNull {
                            it.presentation == HomeFeedPresentation.PLAYLIST_GRID &&
                                it.playlists.items.isNotEmpty()
                        }
                        ?.playlists?.items?.firstOrNull()
                    if (heroPlaylist != null) {
                        item(
                            key = registerGridItemKey("home:hero"),
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            HomeHero(
                                title = heroPlaylist.name,
                                subtitle = null,
                                coverUrl = heroPlaylist.picUrl,
                                onClick = { onItemClick(heroPlaylist) },
                                modifier = Modifier.padding(
                                    start = gridContentPadding,
                                    end = gridContentPadding,
                                    top = gridContentPadding,
                                    bottom = gridSpacing
                                )
                            )
                        }
                    }
                    // Fix-01C-Diag：若 Home 布局的 order 未包含 CONTINUE（既有配置语义），
                    // 但在播歌曲存在，则在原 Continue 位置（各 Section 之前）兜底渲染一次单曲。
                    if (currentSong != null && HomeSectionId.CONTINUE !in homeLayout.order) {
                        renderContinueSection()
                    }
                    for (section in homeLayout.order) {
                        when (section) {
                            HomeSectionId.CONTINUE -> renderContinueSection()
                            HomeSectionId.TRENDING -> if (showOnlineFeeds && showNeteaseTrending) {
                                homeFeedGroups[HomeSectionId.TRENDING].orEmpty().forEach { addHomeFeedGroup(it) }
                            }
                            HomeSectionId.RADAR -> if (showOnlineFeeds && showNeteaseRadar) {
                                homeFeedGroups[HomeSectionId.RADAR].orEmpty().forEach { addHomeFeedGroup(it) }
                            }
                            HomeSectionId.RECOMMENDED -> if (showOnlineFeeds && showRecommendedCard) {
                                homeFeedGroups[HomeSectionId.RECOMMENDED].orEmpty().forEach { addHomeFeedGroup(it) }
                            }
                        }
                    }
                    }
                }
                }
                LaunchedEffect(homeRefreshGeneration) {
                    if (isRefreshing) {
                        delay(700)
                        isRefreshing = false
                    }
                }
                SideEffect {
                    onScrollAnchorIndexesChanged(scrollAnchorIndexes.toMap())
                }
            }
        }

        quickActionDetail?.let { action ->
            BackHandler { quickActionDetail = null }
            NeteaseSongCollectionDetailScreen(
                title = action.title,
                subtitle = stringResource(
                    R.string.playlist_creator_track_format,
                    action.title,
                    action.songs.size
                ),
                songs = action.songs,
                coverUrl = action.songs.firstOrNull()?.coverUrl,
                onBack = { quickActionDetail = null },
                onSongClick = onSongClick,
                offlineMode = offlineMode
            )
        }

        LyraOverlaySnackbarHost(
            hostState = snackbarHostState,
            bottomPadding = LocalMiniPlayerHeight.current
        )
    }
}

@Composable
private fun MeloxSectionTitle(
    title: String,
    trailing: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MeloxType.title2,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun MeloxQuickActionRow(
    actions: List<MeloxHomeQuickAction>,
    contentPadding: Dp,
    loadingKey: String? = null,
    onRadarModeChange: (Int) -> Unit = {},
    onAction: (MeloxHomeQuickAction) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(actions, key = { it.key }) { action ->
            Column(
                modifier = Modifier
                    .width(310.dp)
                    .clickable { onAction(action) }
            ) {
                Text(
                    text = action.eyebrow.uppercase(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = action.title,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = action.subtitle,
                    modifier = Modifier.padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .aspectRatio(1.48f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(action.colors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.24f),
                        modifier = Modifier.size(72.dp)
                    )
                    MeloxQuickActionCaption(
                        action = action,
                        onRadarModeChange = onRadarModeChange,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    if (loadingKey == action.key) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 卡片底部标题区。
 *
 * 雷达卡片标题带箭头：点标题展开三种模式的分段选择器（按下横滑也能切换），
 * 点卡片其余区域仍按当前模式直接播放。
 */
@Composable
private fun MeloxQuickActionCaption(
    action: MeloxHomeQuickAction,
    onRadarModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (action.radarModes.isEmpty()) {
        Text(
            text = action.title,
            modifier = modifier.padding(18.dp),
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        return
    }

    var modesExpanded by rememberSaveable(action.key) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (modesExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "radarModeChevron"
    )
    val selectedIndex = action.radarModes
        .indexOf(action.radarMode)
        .coerceAtLeast(0)
    val modeLabels = action.radarModes.map { mode ->
        stringResource(
            when (mode) {
                HomeLayoutConfig.RADAR_MODE_EXPLORE -> R.string.home_qq_radar_mode_explore
                HomeLayoutConfig.RADAR_MODE_FAMILIAR -> R.string.home_qq_radar_mode_familiar
                else -> R.string.home_qq_radar_mode_default
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 14.dp, bottom = 14.dp)
    ) {
        AnimatedVisibility(
            visible = modesExpanded,
            enter = fadeIn(tween(durationMillis = 160)) +
                expandVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Bottom
                ),
            exit = fadeOut(tween(durationMillis = 120)) +
                shrinkVertically(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Bottom
                )
        ) {
            MeloxRadarModeSelector(
                labels = modeLabels,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    onRadarModeChange(action.radarModes[index])
                    modesExpanded = false
                },
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { modesExpanded = !modesExpanded }
        ) {
            Text(
                text = action.title,
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = stringResource(
                    if (modesExpanded) R.string.action_collapse else R.string.action_expand
                ),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(22.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}

@Composable
private fun MeloxRadarModeSelector(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = labels.size
    if (count == 0) return
    var previewIndex by remember { mutableIntStateOf(-1) }
    val activeIndex = (if (previewIndex in 0 until count) previewIndex else selectedIndex)
        .coerceIn(0, count - 1)
    LaunchedEffect(selectedIndex) {
        previewIndex = -1
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(3.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val segmentWidth = maxWidth / count
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * activeIndex,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "radarModeIndicator"
            )
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.94f))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(count) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            fun indexAt(x: Float): Int =
                                ((x / width) * count).toInt().coerceIn(0, count - 1)

                            previewIndex = indexAt(down.position.x)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (!change.pressed) break
                                val next = indexAt(change.position.x)
                                if (next != previewIndex) previewIndex = next
                            }
                            if (previewIndex in 0 until count) onSelect(previewIndex)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                labels.forEachIndexed { index, label ->
                    val labelColor by animateColorAsState(
                        targetValue = if (index == activeIndex) {
                            Color(0xFF2B2B3D)
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "radarModeLabel$index"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = labelColor,
                            fontSize = 12.sp,
                            fontWeight = if (index == activeIndex) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Medium
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeloxSongCarousel(
    songs: List<SongItem>,
    isTabletLayout: Boolean,
    offlineMode: Boolean,
    onSongClick: (List<SongItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showSongArtist = rememberShowSongArtist()
    val columnWidth = if (isTabletLayout) 390.dp else 320.dp
    val columns = songs.chunked(3)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(
            items = columns,
            key = { _, column -> column.joinToString("-") { it.stableKey() } }
        ) { columnIndex, column ->
            Column(modifier = Modifier.width(columnWidth)) {
                column.forEachIndexed { rowIndex, song ->
                    val songIndex = columnIndex * 3 + rowIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .clickable { onSongClick(songs, songIndex) }
                            .padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(9.dp))
                        ) {
                            AsyncImage(
                                model = fastScrollableImageRequest(
                                    context = context,
                                    data = song.coverUrl,
                                    sizePx = 192,
                                    offlineMode = offlineMode
                                ),
                                contentDescription = song.displayName(),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = song.displayName(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (showSongArtist) {
                                Text(
                                    text = song.displayArtist(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp, end = 10.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }
}

private fun homeFeedIcon(icon: HomeFeedIcon): ImageVector {
    return when (icon) {
        HomeFeedIcon.BOLT -> Icons.Outlined.Bolt
        HomeFeedIcon.RADAR -> Icons.Outlined.Radar
        HomeFeedIcon.EXPLORE -> Icons.Outlined.Explore
        HomeFeedIcon.STAR -> Icons.Outlined.Star
        HomeFeedIcon.MUSIC -> Icons.Outlined.MusicNote
    }
}


@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MeloxType.title2,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SectionLoadingState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionErrorState(detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = detail,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.home_retry_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SongRowMini(
    index: Int,
    song: SongItem,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: (SongItem, Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val coverUrl = rememberSongDisplayCoverUrl(song)
    val showSongArtist = rememberShowSongArtist()
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = fastScrollableImageRequest(
                    context = context,
                    data = coverUrl,
                    sizePx = 128,
                    offlineMode = offlineMode
                ),
                contentDescription = song.displayName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(44.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .padding(end = 4.dp)
        ) {
            Text(
                text = song.displayName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            if (showSongArtist) {
                Text(
                    text = listOfNotNull(
                        song.displayArtist().takeIf { it.isNotBlank() },
                        song.displayAlbum(context).takeIf { it.isNotBlank() }
                    ).joinToString(" / "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.common_more_actions),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SongActionsMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                actions = listOf(
                    SongActionMenuItem(
                        label = stringResource(R.string.local_playlist_play_next),
                        icon = Icons.AutoMirrored.Outlined.PlaylistPlay
                    ) { PlayerManager.addToQueueNext(song) },
                    SongActionMenuItem(
                        label = stringResource(R.string.playlist_add_to_end),
                        icon = Icons.AutoMirrored.Outlined.PlaylistAdd
                    ) { PlayerManager.addToQueueEnd(song) },
                    SongActionMenuItem(
                        label = stringResource(
                            if (isFavorite) R.string.favorite_remove else R.string.favorite_add
                        ),
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                    ) { onFavoriteToggle(song, isFavorite) },
                    SongActionMenuItem(
                        label = stringResource(R.string.action_copy_song_info),
                        icon = Icons.Outlined.ContentCopy
                    ) {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText("text", buildHomeSongInfo(song))
                                )
                            )
                            onShowSnackbar(composeResources.getString(R.string.toast_copied))
                        }
                    }
                )
            )
        }
    }
}

@Composable
private fun RadarPlaylistStrip(
    playlists: List<PlaylistSummary>,
    favoriteKeys: Set<String>,
    listState: LazyListState,
    onClick: (PlaylistSummary) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val cardWidth = if (currentWindowWidthDp() >= 600.dp) 172.dp else 148.dp
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        playlists.forEach { playlist ->
            item(key = homeNeteaseRadarPlaylistScrollKey(playlist.id)) {
                RadarPlaylistCard(
                    playlist = playlist,
                    isFavorite = favoriteKeys.contains("netease:${playlist.id}"),
                    onClick = { onClick(playlist) },
                    onShowSnackbar = onShowSnackbar,
                    offlineMode = offlineMode,
                    modifier = Modifier.width(cardWidth)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadarPlaylistCard(
    playlist: PlaylistSummary,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    var showMenu by remember { mutableStateOf(false) }
    val unfavoritedText = stringResource(R.string.home_unfavorited)
    val favoriteSuccessText = stringResource(R.string.favorite_success)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.picUrl.isNotBlank()) {
                AsyncImage(
                    model = fastScrollableImageRequest(
                        context = context,
                        data = playlist.picUrl,
                        sizePx = 384,
                        offlineMode = offlineMode
                    ),
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Radar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(42.dp)
                )
            }
            Text(
                text = stringResource(R.string.home_netease_radar_badge),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.86f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            Text(
                text = playlist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.home_play_count_format,
                    formatPlayCount(context, playlist.playCount),
                    playlist.trackCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) {
                            stringResource(R.string.home_unfavorite_playlist)
                        } else {
                            stringResource(R.string.home_favorite_playlist)
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
                onClick = {
                    showMenu = false
                    scope.launch {
                        if (isFavorite) {
                            favoriteRepo.removeFavorite(playlist.id, "netease")
                            onShowSnackbar(unfavoritedText)
                        } else {
                            favoriteRepo.addFavorite(
                                id = playlist.id,
                                name = playlist.name,
                                coverUrl = playlist.picUrl,
                                trackCount = playlist.trackCount,
                                source = "netease",
                                songs = emptyList()
                            )
                            onShowSnackbar(favoriteSuccessText)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: PlaylistSummary,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onShowSnackbar: (String) -> Unit = {},
    offlineMode: Boolean = false,
    large: Boolean = false,
    allowFavorite: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    var showMenu by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(if (large) 16.dp else 8.dp)

    val unfavoritedText = stringResource(R.string.home_unfavorited)
    val favoriteSuccessText = stringResource(R.string.favorite_success)

    Column(
        modifier = Modifier
            .then(if (large) Modifier.fillMaxHeight() else Modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (allowFavorite) ({ showMenu = true }) else null
            )
            .padding(if (large) 8.dp else 0.dp)
    ) {
        AsyncImage(
            model = fastScrollableImageRequest(
                context = context,
                data = playlist.picUrl,
                sizePx = 384,
                offlineMode = offlineMode
            ),
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (large) Modifier.height(174.dp)
                    else Modifier.aspectRatio(1f)
                )
                .clip(cardShape)
        )
        Column(
            modifier = if (large) {
                Modifier.weight(1f)
            } else {
                Modifier.padding(
                    top = 6.dp,
                    start = 4.dp,
                    end = 4.dp,
                    bottom = 4.dp
                )
            }
        ) {
            Text(
                text = playlist.name,
                maxLines = if (large) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = if (large) 15.sp else TextUnit.Unspecified,
                lineHeight = if (large) 19.sp else TextUnit.Unspecified,
                style = if (large) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.titleSmall
                },
                fontWeight = if (large) FontWeight.SemiBold else null,
                modifier = if (large) Modifier.padding(top = 7.dp) else Modifier
            )
            if (large) {
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = if (large) {
                    stringResource(
                        R.string.home_play_count_only_format,
                        formatPlayCount(context, playlist.playCount)
                    )
                } else {
                    stringResource(
                        R.string.home_play_count_format,
                        formatPlayCount(context, playlist.playCount),
                        playlist.trackCount
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                fontSize = if (large) 11.sp else TextUnit.Unspecified,
                lineHeight = if (large) 14.sp else TextUnit.Unspecified,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = if (large) Modifier.padding(top = 3.dp) else Modifier
            )
        }

        if (allowFavorite) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isFavorite) {
                                stringResource(R.string.home_unfavorite_playlist)
                            } else {
                                stringResource(R.string.home_favorite_playlist)
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
                    onClick = {
                        showMenu = false
                        scope.launch {
                            if (isFavorite) {
                                favoriteRepo.removeFavorite(playlist.id, "netease")
                                onShowSnackbar(unfavoritedText)
                            } else {
                                favoriteRepo.addFavorite(
                                    id = playlist.id,
                                    name = playlist.name,
                                    coverUrl = playlist.picUrl,
                                    trackCount = playlist.trackCount,
                                    source = "netease",
                                    songs = emptyList()
                                )
                                onShowSnackbar(favoriteSuccessText)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun YtMusicPlaylistCard(
    playlist: YouTubeMusicPlaylist,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val playlistFavoriteId = remember(playlist.playlistId, playlist.browseId) {
        playlist.favoriteId()
    }
    var showMenu by remember { mutableStateOf(false) }
    val unfavoritedText = stringResource(R.string.home_unfavorited)
    val favoriteSuccessText = stringResource(R.string.favorite_success)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
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
                .clip(RoundedCornerShape(8.dp))
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

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) {
                            stringResource(R.string.home_unfavorite_playlist)
                        } else {
                            stringResource(R.string.home_favorite_playlist)
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
                onClick = {
                    showMenu = false
                    scope.launch {
                        if (isFavorite) {
                            favoriteRepo.removeFavorite(playlistFavoriteId, "youtubeMusic")
                            onShowSnackbar(unfavoritedText)
                        } else {
                            favoriteRepo.addFavorite(
                                id = playlistFavoriteId,
                                name = playlist.title,
                                coverUrl = playlist.coverUrl,
                                trackCount = playlist.trackCount,
                                source = "youtubeMusic",
                                browseId = playlist.browseId,
                                playlistId = playlist.playlistId,
                                subtitle = playlist.subtitle,
                                songs = emptyList()
                            )
                            onShowSnackbar(favoriteSuccessText)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun YtMusicHomeItemCard(
    item: YouTubeMusicHomeItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val playlist = remember(item) { item.toPlaylist() }
    val playlistFavoriteId = remember(playlist?.playlistId, playlist?.browseId) {
        playlist?.favoriteId()
    }
    var showMenu by remember { mutableStateOf(false) }
    val unfavoritedText = stringResource(R.string.home_unfavorited)
    val favoriteSuccessText = stringResource(R.string.favorite_success)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (playlist != null) {
                        showMenu = true
                    }
                }
            )
    ) {
        AsyncImage(
            model = fastScrollableImageRequest(
                context = context,
                data = item.coverUrl,
                sizePx = 384,
                offlineMode = offlineMode
            ),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            Text(
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            playlist?.let { resolvedPlaylist ->
                val favoriteId = playlistFavoriteId ?: return@let
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isFavorite) {
                                stringResource(R.string.home_unfavorite_playlist)
                            } else {
                                stringResource(R.string.home_favorite_playlist)
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
                    onClick = {
                        showMenu = false
                        scope.launch {
                            if (isFavorite) {
                                favoriteRepo.removeFavorite(favoriteId, "youtubeMusic")
                                onShowSnackbar(unfavoritedText)
                            } else {
                                favoriteRepo.addFavorite(
                                    id = favoriteId,
                                    name = resolvedPlaylist.title,
                                    coverUrl = resolvedPlaylist.coverUrl,
                                    trackCount = resolvedPlaylist.trackCount,
                                    source = "youtubeMusic",
                                    browseId = resolvedPlaylist.browseId,
                                    playlistId = resolvedPlaylist.playlistId,
                                    subtitle = resolvedPlaylist.subtitle,
                                    songs = emptyList()
                                )
                                onShowSnackbar(favoriteSuccessText)
                            }
                        }
                    }
                )
            }
        }
    }
}

private data class ClassifiedYouTubeMusicShelves(
    val guessYouLike: YouTubeMusicHomeShelf?,
    val dailyDiscover: YouTubeMusicHomeShelf?,
    val remaining: List<YouTubeMusicHomeShelf>
)

private val YouTubeMusicGuessYouLikeKeywords = listOf(
    "猜你喜欢",
    "guess you like",
    "recommended for you"
)

private val YouTubeMusicDailyDiscoverKeywords = listOf(
    "每日发现",
    "daily discover",
    "discover daily"
)

private val YouTubeMusicSongShelfKeywords = listOf(
    "再听一遍",
    "老歌重温",
    "翻唱与混音",
    "每日发现",
    "猜你喜欢",
    "listen again",
    "oldies",
    "covers and remixes",
    "daily discover"
)

private fun classifyYouTubeMusicShelves(
    shelves: List<YouTubeMusicHomeShelf>
): ClassifiedYouTubeMusicShelves {
    val guessYouLike = shelves.firstOrNull { shelf ->
        shelf.shouldRenderAsSongShelf() &&
            shelf.title.matchesYouTubeMusicShelfKeywords(YouTubeMusicGuessYouLikeKeywords)
    }
    val dailyDiscover = shelves.firstOrNull { shelf ->
        shelf != guessYouLike &&
            shelf.shouldRenderAsSongShelf() &&
            shelf.title.matchesYouTubeMusicShelfKeywords(YouTubeMusicDailyDiscoverKeywords)
    }
    val remaining = shelves.filterNot { shelf ->
        shelf == guessYouLike || shelf == dailyDiscover
    }
    return ClassifiedYouTubeMusicShelves(
        guessYouLike = guessYouLike,
        dailyDiscover = dailyDiscover,
        remaining = remaining
    )
}

private fun YouTubeMusicHomeShelf.shouldRenderAsSongShelf(): Boolean {
    if (items.isEmpty()) {
        return false
    }
    val playableCount = items.count { it.videoId.isNotBlank() }
    if (playableCount == 0) {
        return false
    }
    if (playableCount == items.size) {
        return true
    }
    return title.matchesYouTubeMusicShelfKeywords(YouTubeMusicSongShelfKeywords)
}

private fun YouTubeMusicHomeShelf.hasRenderablePlaylistItems(): Boolean {
    return items.any { it.isPlaylistItem() }
}

private fun YouTubeMusicHomeItem.isPlaylistItem(): Boolean {
    val normalizedPageType = pageType.uppercase(Locale.US)
    return when {
        normalizedPageType.contains("PLAYLIST") -> true
        normalizedPageType.isNotBlank() -> false
        else -> browseId.startsWith("VL")
    }
}

private fun YouTubeMusicHomeItem.toPlaylist(): YouTubeMusicPlaylist? {
    if (!isPlaylistItem()) {
        return null
    }
    return YouTubeMusicPlaylist(
        browseId = browseId,
        playlistId = browseId.removePrefix("VL"),
        title = title,
        subtitle = subtitle,
        coverUrl = coverUrl,
        trackCount = 0
    )
}

private fun String.matchesYouTubeMusicShelfKeywords(keywords: List<String>): Boolean {
    val normalized = lowercase(Locale.ROOT)
        .replace(Regex("[\\s·•・/\\\\|:_-]+"), "")
    return keywords.any { keyword ->
        val normalizedKeyword = keyword.lowercase(Locale.ROOT)
            .replace(Regex("[\\s·•・/\\\\|:_-]+"), "")
        normalized.contains(normalizedKeyword)
    }
}

internal fun YouTubeMusicHomeItem.toPlayableSongItem(sectionTitle: String): SongItem? {
    if (videoId.isBlank()) {
        return null
    }
    val metadata = YouTubeMusicParser.parseHomeSongMetadata(
        subtitle = subtitle,
        fallbackAlbum = sectionTitle
    )
    val playlistId = browseId.removePrefix("VL").ifBlank { null }
    return SongItem(
        id = stableYouTubeMusicId(videoId),
        name = title,
        artist = metadata.artist,
        album = metadata.album,
        albumId = stableYouTubeMusicId((playlistId ?: sectionTitle).ifBlank { videoId }),
        durationMs = durationMs,
        coverUrl = coverUrl.ifBlank { null },
        mediaUri = buildYouTubeMusicMediaUri(
            videoId = videoId,
            playlistId = playlistId
        ),
        originalName = title,
        originalArtist = metadata.artist,
        originalCoverUrl = coverUrl.ifBlank { null }
    )
}

private fun LazyGridScope.addYouTubeMusicSongShelfSection(
    sectionKey: String,
    registerKey: (String) -> String,
    shelf: YouTubeMusicHomeShelf,
    icon: ImageVector,
    title: String,
    onSongClick: (List<SongItem>, Int) -> Unit,
    favoriteSongs: List<SongItem>,
    onFavoriteToggle: (SongItem, Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val songs = shelf.items.mapNotNull { it.toPlayableSongItem(shelf.title) }
    if (songs.isEmpty()) {
        return
    }
    item(
        key = registerKey("$sectionKey:header"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        SectionHeader(
            icon = icon,
            title = title
        )
    }
    item(
        key = registerKey("$sectionKey:content"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        val warmupKey = remember(songs) {
            songs.joinToString("|") { song ->
                song.audioId ?: song.mediaUri.orEmpty()
            }
        }
        LaunchedEffect(warmupKey) {
            PlayerManager.prefetchYouTubePlayableUrlWindow(
                playlist = songs,
                startIndex = 0,
                source = "yt_home_shelf_visible"
            )
        }
        ResponsiveSongPagerList(
            songs = songs,
            onSongClick = onSongClick,
            favoriteSongs = favoriteSongs,
            onFavoriteToggle = onFavoriteToggle,
            onShowSnackbar = onShowSnackbar,
            offlineMode = offlineMode
        )
    }
}

@Composable
private fun ContinueSection(
    items: List<UsageEntry>,
    localPlaylistLookup: Map<Long, LocalPlaylist>,
    localFilesCoverCandidates: List<SongItem>,
    onClick: (UsageEntry) -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cardsPerPage = remember(maxWidth) {
            resolveHomeContinueCardsPerPage(maxWidth.value)
        }
        val cardWidth = remember(maxWidth, cardsPerPage) {
            resolveHomeContinueCardWidthDp(
                containerWidthDp = maxWidth.value,
                cardsPerPage = cardsPerPage
            ).dp
        }
        val pageCount = remember(items.size, cardsPerPage) {
            ceil(items.size / cardsPerPage.toFloat()).toInt().coerceAtLeast(1)
        }
        val pagerState = rememberPagerState(pageCount = { pageCount })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
        ) { page ->
            Box(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HomeContinueHorizontalPaddingDp.dp),
                    horizontalArrangement = Arrangement.spacedBy(HomeContinueCardSpacingDp.dp)
                ) {
                    repeat(cardsPerPage) { slot ->
                        val entry = items.getOrNull(page * cardsPerPage + slot)
                        if (entry == null) {
                            Spacer(Modifier.width(cardWidth))
                        } else {
                            val localPlaylist = if (
                                entry.source == PlaylistUsageRepository.SOURCE_LOCAL
                            ) {
                                localPlaylistLookup[entry.id]
                            } else {
                                null
                            }
                            ContinueCard(
                                entry = entry,
                                localPlaylist = localPlaylist,
                                localFilesCoverCandidates = localFilesCoverCandidates,
                                onClick = { onClick(entry) },
                                onRemove = {
                                    AppContainer.launchBackgroundIo {
                                        AppContainer.playlistUsageRepo.removeEntry(
                                            entry.id,
                                            entry.source,
                                            entry.subtype
                                        )
                                    }
                                },
                                offlineMode = offlineMode,
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCard(
    entry: UsageEntry,
    localPlaylist: LocalPlaylist?,
    localFilesCoverCandidates: List<SongItem>,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val view = androidx.compose.ui.platform.LocalView.current
    var showMenu by remember { mutableStateOf(false) }
    val displayName = remember(entry.id, entry.name, entry.source, configuration) {
        SystemLocalPlaylists.resolve(entry.id, entry.name, context)?.currentName ?: entry.name
    }
    val resolvedLocalCoverUrl = if (localPlaylist != null) {
        rememberPlaylistDisplayCoverUrl(
            playlist = localPlaylist,
            additionalCoverCandidates = if (localPlaylist.id == LocalFilesPlaylist.SYSTEM_ID) {
                localFilesCoverCandidates
            } else {
                emptyList()
            }
        )
    } else {
        null
    }
    val coverUrl = resolvedLocalCoverUrl ?: entry.picUrl

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    showMenu = true
                }
            )
    ) {
        AsyncImage(
            model = fastScrollableImageRequest(
                context = context,
                data = coverUrl,
                sizePx = 384,
                offlineMode = offlineMode
            ),
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = pluralStringResource(
                    R.plurals.home_song_count_format,
                    entry.trackCount,
                    entry.trackCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.continue_playing_remove)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null
                    )
                },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun ResponsiveSongPagerList(
    songs: List<SongItem>,
    onSongClick: (List<SongItem>, Int) -> Unit,
    favoriteSongs: List<SongItem>,
    onFavoriteToggle: (SongItem, Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val widthDp = currentWindowWidthDp().value
    val columns = when {
        widthDp >= 840 -> 3
        widthDp >= 600 -> 2
        else -> 1
    }
    val rowsPerColumn = 3
    val perPage = (columns * rowsPerColumn).coerceAtLeast(1)

    val pageCount = ceil(songs.size / perPage.toFloat()).toInt().coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) { page ->
        val start = page * perPage
        val end = min(start + perPage, songs.size)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (columnIndex in 0 until columns) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (rowIndex in 0 until rowsPerColumn) {
                        val absoluteIndex = start + (columnIndex * rowsPerColumn + rowIndex)
                        if (absoluteIndex < end) {
                            val song = songs[absoluteIndex]
                            SongRowMini(
                                index = absoluteIndex + 1,
                                song = song,
                                onClick = { onSongClick(songs, absoluteIndex) },
                                isFavorite = favoriteSongs.any { it.sameIdentityAs(song) },
                                onFavoriteToggle = onFavoriteToggle,
                                onShowSnackbar = onShowSnackbar,
                                offlineMode = offlineMode
                            )
                        } else {
                            Spacer(Modifier.height(0.dp))
                        }
                    }
                }
            }
        }
    }
}

internal fun resolveHomeContinueCardsPerPage(containerWidthDp: Float): Int {
    val preferredMinimumSlots = if (containerWidthDp >= HomeContinueThreeSlotWidthDp) 3 else 2
    val availableWidth = (containerWidthDp - HomeContinueHorizontalPaddingDp * 2f)
        .coerceAtLeast(0f)
    val slotsNeededToAvoidSlack = ceil(
        (availableWidth + HomeContinueCardSpacingDp) /
            (HomeContinueCardMaxWidthDp + HomeContinueCardSpacingDp)
    ).toInt()
    val tabletMinimumSlots = if (containerWidthDp >= HomeContinueTabletWidthDp) 4 else 0
    return maxOf(preferredMinimumSlots, tabletMinimumSlots, slotsNeededToAvoidSlack, 1)
}

internal fun resolveHomeContinueCardWidthDp(
    containerWidthDp: Float,
    cardsPerPage: Int
): Float {
    val slots = cardsPerPage.coerceAtLeast(1)
    val availableWidth = containerWidthDp -
        HomeContinueHorizontalPaddingDp * 2f -
        HomeContinueCardSpacingDp * (slots - 1)
    return (availableWidth / slots)
        .coerceAtLeast(0f)
}

internal fun buildHomeSongInfo(song: SongItem): String {
    return "${song.displayName()}-${song.displayArtist()}"
}
