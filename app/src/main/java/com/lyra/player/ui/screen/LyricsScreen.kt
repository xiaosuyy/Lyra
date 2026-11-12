package com.lyra.player.ui.screen

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
 * File: com.lyra.player.ui.screen/LyricsScreen
 * Created: 2025/8/13
 */

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.res.Configuration
import android.os.PowerManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import com.lyra.player.ui.screen.nowplaying.NowPlayingAmbientBackground
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.data.local.playlist.system.FavoritesPlaylist
import com.lyra.player.ui.melox.theme.MeloxSystemColors
import com.lyra.player.data.local.playlist.system.LocalFilesPlaylist
import com.lyra.player.data.settings.LyricFontScalePage
import com.lyra.player.data.settings.LyricFontScaleTarget
import com.lyra.player.data.settings.LyricFontScales
import com.lyra.player.data.settings.PlaybackControlLayoutPreferences
import com.lyra.player.data.settings.scaledLyricFontSize
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayCoverUrl
import com.lyra.player.data.model.displayName
import com.lyra.player.data.local.media.isLocalSong
import com.lyra.player.data.model.isSyncableRemoteSong
import com.lyra.player.data.model.sameFavoriteSongAs
import com.lyra.player.data.model.sameIdentityAs
import com.lyra.player.data.model.stableKey
import com.lyra.player.ui.component.lyrics.AdvancedLyricsView
import com.lyra.player.ui.component.lyrics.SyncedLyricsView
import com.lyra.player.ui.component.lyrics.buildPhoneticLyricEntries
import com.lyra.player.ui.component.lyrics.flattenWordTimedEntries
import com.lyra.player.ui.component.lyrics.LyricEntry
import com.lyra.player.ui.component.lyrics.LyricShareSheet
import com.lyra.player.ui.component.local.LocalSongDetailsDialog
import com.lyra.player.ui.component.local.LocalSongSyncConfirmDialog
import com.lyra.player.ui.component.lyrics.LyricVisualSpec
import com.lyra.player.ui.component.playback.PlaybackControlIndicator
import com.lyra.player.ui.component.playback.NowPlayingSongTitle
import com.lyra.player.ui.theme.LocalNeriTargetColorScheme
import com.lyra.player.ui.component.playback.scaleButtonSize
import com.lyra.player.ui.component.playback.scaleIconSize
import com.lyra.player.ui.component.playback.rememberDelayedPlaybackWaiting
import com.lyra.player.ui.component.playback.WaveformSlider
import com.lyra.player.ui.component.playback.resolvePlaybackWaiting
import com.lyra.player.ui.component.overlay.DensityScaledModalBottomSheet
import com.lyra.player.ui.component.sheet.bottomSheetScrollGuard
import com.lyra.player.ui.feedback.LyraOverlaySnackbarHost
import com.lyra.player.ui.feedback.showNeriSnackbar
import com.lyra.player.ui.component.lyrics.rememberLyricSeekHapticFeedback
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.haptic.HapticFeedbackEffect
import com.lyra.player.ui.haptic.HapticIconButton
import com.lyra.player.util.format.formatDuration
import com.lyra.player.util.media.offlineCachedImageRequest
import com.lyra.player.ui.haptic.performHapticFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun LyricsScreen(
    lyrics: List<LyricEntry>,
    rawLyrics: String? = null,
    rawTranslatedLyrics: String? = null,
    lyricBlurEnabled: Boolean,
    lyricBlurAmount: Float,
    lyricFontScales: LyricFontScales,
    onLyricFontScaleChange: (LyricFontScaleTarget, Float) -> Unit,
    onEnterAlbum: (AlbumSummary) -> Unit,
    onExitNowPlaying: () -> Unit,
    onOpenCurrentArtist: () -> Unit = {},
    onOpenCurrentPlaybackSource: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onOpenQueuePage: () -> Unit = {},
    onOpenOutputPicker: () -> Unit = {},
    onSeekTo: (Long) -> Unit,
    progressSeekEnabled: Boolean = true,
    advancedLyricsEnabled: Boolean = true,
    translatedLyrics: List<LyricEntry>? = null,
    phoneticLyrics: List<LyricEntry> = emptyList(),
    lyricOffsetMs: Long,
    showLyricTranslation: Boolean = true,
    lyricTranslationUsePhonetic: Boolean = false,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedContentScope: androidx.compose.animation.AnimatedContentScope? = null,
    offlineMode: Boolean = false,
) {
    // 处理返回键
    androidx.activity.compose.BackHandler(onBack = onNavigateBack)
    val lyricFontScale = lyricFontScales.lyricsPageLyric
    val translationFontScale = lyricFontScales.lyricsPageTranslation

    val currentSong by PlayerManager.currentSongFlow.collectAsState()
    val settingsRepo = remember { AppContainer.settingsRepo }
    val nowPlayingSongTitleMarqueeEnabled by settingsRepo
        .nowPlayingSongTitleMarqueeEnabledFlow
        .collectAsState(initial = true)
    val playbackControlLayoutPreferences by settingsRepo
        .playbackControlLayoutPreferencesFlow
        .collectAsState(initial = PlaybackControlLayoutPreferences())
    val queue by PlayerManager.currentQueueFlow.collectAsState()
    val queueDisplayRevision by PlayerManager.currentQueueDisplayRevisionFlow.collectAsState()
    val queueDisplayState = remember(queue, currentSong, queueDisplayRevision) {
        PlayerManager.currentQueueDisplaySnapshot()
    }
    val displayedQueueItems = queueDisplayState.items
    val displayedQueue = remember(displayedQueueItems) { displayedQueueItems.map { it.song } }
    val currentIndexInDisplay = queueDisplayState.currentDisplayIndex
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()
    val isPlaybackControlPlaying by PlayerManager.playbackControlPlayingFlow.collectAsState()
    val isAudioRouteMuted by PlayerManager.audioRouteMuteSuppressedFlow.collectAsState()
    val usbPlaybackPreparing by PlayerManager.usbExclusivePlaybackPreparingFlow.collectAsState()
    val isPlaybackWaiting = resolvePlaybackWaiting(
        playbackRequested = isPlaybackControlPlaying,
        isPlaying = isPlaying,
        usbPlaybackPreparing = usbPlaybackPreparing
    )
    val lyricsPlaybackSoundState by PlayerManager.playbackSoundStateFlow.collectAsState()
    val plainLyrics = remember(lyrics) { lyrics.flattenWordTimedEntries() }
    val plainTranslatedLyrics = remember(translatedLyrics) {
        translatedLyrics.orEmpty().flattenWordTimedEntries()
    }
    val embeddedPhoneticLyrics = remember(rawLyrics, lyrics) {
        buildPhoneticLyricEntries(
            rawLyrics = rawLyrics,
            lyrics = lyrics
        )
    }
    val effectivePhoneticLyrics = remember(phoneticLyrics, embeddedPhoneticLyrics) {
        phoneticLyrics.takeIf { it.isNotEmpty() } ?: embeddedPhoneticLyrics
    }
    val durationMs = currentSong?.durationMs ?: 0L
    val favoriteActionLabel = stringResource(R.string.favorite_add)
    val playlistAddActionLabel = stringResource(R.string.playlist_add_to)

    val context = LocalContext.current
    val lowPowerLyricsRendering = remember(context) {
        context.isSystemPowerSaveMode()
    }
    val downloadPresenceVersion by GlobalDownloadManager.downloadPresenceVersion.collectAsState()
    val currentCoverUrl = remember(currentSong, context, downloadPresenceVersion) {
        currentSong?.displayCoverUrl(context)
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var showSongNameMenu by remember { mutableStateOf(false) }
    var showArtistMenu by remember { mutableStateOf(false) }
    var detailSong by remember { mutableStateOf<SongItem?>(null) }
    var pendingSyncConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSyncConfirmLabel by remember { mutableStateOf("") }
    var lyricShareInitialLine by remember(currentSong?.stableKey()) {
        mutableStateOf<LyricEntry?>(null)
    }

    // 动画状态
    var isLyricsMode by remember { mutableStateOf(false) }
    var previewPositionOverrideMs by remember(currentSong?.id) { mutableStateOf<Long?>(null) }
    val configuration = LocalConfiguration.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val isTabletLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        with(density) { windowInfo.containerSize.width.toDp() } >= 720.dp
    val horizontalPadding = if (isTabletLandscape) 36.dp else 20.dp
    val verticalPadding = if (isTabletLandscape) 14.dp else 12.dp
    val contentWidthFraction = if (isTabletLandscape) 0.86f else 1f
    val lyricsWidthFraction = if (isTabletLandscape) 0.58f else 1f
    val controlWidthFraction = if (isTabletLandscape) 0.72f else 1f
    val toolbarWidthFraction = if (isTabletLandscape) 0.58f else 1f
    val lyricsControlSize = playbackControlLayoutPreferences.lyricsSize
    val baseToolbarIconSize = if (isTabletLandscape) 22.dp else 20.dp
    val lyricsTopActionButtonSize = lyricsControlSize.scaleButtonSize(48.dp)
    val lyricsTopActionIconSize = lyricsControlSize.scaleIconSize(24.dp)
    val lyricsMainButtonSize = lyricsControlSize.scaleButtonSize(64.dp)
    val lyricsMainIconSize = lyricsControlSize.scaleIconSize(34.dp)
    val lyricsPlayIndicatorSize = lyricsControlSize.scaleIconSize(48.dp)
    val lyricsMainControlRowHeight = lyricsControlSize.scaleButtonSize(82.dp)
    val lyricsTopBarHeight = maxOf(56.dp, lyricsTopActionButtonSize)
    val toolbarIconSize = lyricsControlSize.scaleIconSize(baseToolbarIconSize)
    val toolbarMinimumTouchTarget = lyricsControlSize.scaleButtonSize(48.dp)
    val snackbarHostState = remember { SnackbarHostState() }

    // 启动进入动画
    LaunchedEffect(Unit) {
        isLyricsMode = true
    }

    fun launchWithLocalSyncWarning(
        song: SongItem?,
        actionLabel: String,
        warnForLocalSync: Boolean = true,
        action: () -> Unit
    ) {
        if (warnForLocalSync && song?.isSyncableRemoteSong(context) == false) {
            pendingSyncConfirmLabel = actionLabel
            pendingSyncConfirmAction = action
        } else {
            action()
        }
    }

    lyricShareInitialLine?.let { initialLine ->
        val song = currentSong
        if (song != null) {
            LyricShareSheet(
                song = song,
                lyrics = plainLyrics,
                initialLine = initialLine,
                queue = queue,
                onDismiss = { lyricShareInitialLine = null },
                onShowMessage = { message ->
                    scope.launch {
                        snackbarHostState.showNeriSnackbar(message)
                    }
                }
            )
        }
    }

    // 封面动画
    val coverScale by animateFloatAsState(
        targetValue = if (isLyricsMode) 0.6f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cover_scale"
    )
    // 垂直偏移控制在标题栏内 (约-8dp) , 避免飞出界面
    val coverOffsetY by animateFloatAsState(
        targetValue = if (isLyricsMode) -8f else 0f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cover_offset_y"
    )

    // 播放控件动画 - 轻微上浮/下沉, 保持常驻在安全区域内

    Box(modifier = Modifier.fillMaxSize()) {
        // 沉浸式氛围底：复用 Full Player 的极轻动态氛围，让歌词成为绝对主角
        NowPlayingAmbientBackground()
        // 使用填充整个屏幕, 不创建新背景, 复用现有背景
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        // 右滑返回
                        if (dragAmount > 50) {
                            onNavigateBack()
                        }
                    }
                }
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // 顶部区域 - 包含缩小的封面 + 收藏 + 更多
        Row(
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
                .widthIn(max = 1320.dp)
                .height(lyricsTopBarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            HapticIconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(lyricsTopActionButtonSize)
                    .then(
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    rememberSharedContentState(
                                        key = NowPlayingLyricsSharedTransitionElement.BACK.key
                                    ),
                                    animatedVisibilityScope = animatedContentScope,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None
                                ).zIndex(1f)
                            }
                        } else Modifier
                    )
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_back),
                    modifier = Modifier.size(lyricsTopActionIconSize)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 封面 - 紧邻返回键, 缩小时约48dp
            Box(
                modifier = Modifier
                    .size((64 * coverScale).dp)
                    .then(
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    rememberSharedContentState(
                                        key = NowPlayingLyricsSharedTransitionElement.COVER.key
                                    ),
                                    animatedVisibilityScope = animatedContentScope
                                )
                            }
                        } else Modifier
                    )
                    .graphicsLayer { translationY = coverOffsetY }
                    .clip(RoundedCornerShape(10.dp))
            ) {
                currentCoverUrl?.let { cover ->
                    AsyncImage(
                        model = remember(context, cover, offlineMode) {
                            offlineCachedImageRequest(
                                context = context,
                                data = cover,
                                sizePx = 192,
                                allowHardware = false,
                                offlineMode = offlineMode
                            )
                        },
                        contentDescription = currentSong?.displayName() ?: "",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 标题区始终占用剩余空间, 避免挤出边界
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                BoxWithConstraints {
                    NowPlayingSongTitle(
                        text = currentSong?.displayName() ?: stringResource(R.string.lyrics_unknown_song),
                        marqueeEnabled = nowPlayingSongTitleMarqueeEnabled,
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalNeriTargetColorScheme.current.onSurface,
                        modifier = Modifier
                            .widthIn(max = maxWidth)
                            .clip(RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showSongNameMenu = true }
                            )
                    )
                    DropdownMenu(
                        expanded = showSongNameMenu,
                        onDismissRequest = { showSongNameMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_copy_song_name)) },
                            onClick = {
                                currentSong?.displayName()?.let { text ->
                                    scope.launch {
                                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("text", text)))
                                    }
                                }
                                showSongNameMenu = false
                            }
                        )
                    }
                }
                Box {
                    Text(
                        text = currentSong?.displayArtist() ?: stringResource(R.string.lyrics_unknown_artist),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(
                                if (sharedTransitionScope != null && animatedContentScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(
                                                key = NowPlayingLyricsSharedTransitionElement.ARTIST.key
                                            ),
                                            animatedVisibilityScope = animatedContentScope
                                        )
                                    }
                                } else Modifier
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = onOpenCurrentArtist,
                                onLongClick = { showArtistMenu = true }
                            )
                    )
                    DropdownMenu(
                        expanded = showArtistMenu,
                        onDismissRequest = { showArtistMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_copy_artist)) },
                            onClick = {
                                currentSong?.displayArtist()?.let { text ->
                                    scope.launch {
                                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("text", text)))
                                    }
                                }
                                showArtistMenu = false
                            }
                        )
                    }
                }
            }

            // 收藏按钮 (与 NowPlaying 保持一致的逻辑)
            val playlists by PlayerManager.playlistsFlow.collectAsState()
            val localPlaylistsReady by PlayerManager.localPlaylistsReadyFlow.collectAsState()
            val isFavoriteComputed = remember(currentSong, playlists) {
                val song = currentSong
                if (song == null) {
                    false
                } else {
                    val fav = playlists.firstOrNull { FavoritesPlaylist.isSystemPlaylist(it, context) }
                    fav?.songs?.any { it.sameFavoriteSongAs(song) } == true
                }
            }
            var favOverride by remember(currentSong) { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(isFavoriteComputed) {
                if (favOverride == isFavoriteComputed) {
                    favOverride = null
                }
            }
            val isFavorite = favOverride ?: isFavoriteComputed

            HapticIconButton(
                onClick = {
                    val song = currentSong ?: return@HapticIconButton
                    val willFav = nextFavoriteStateAfterTap(isFavorite)
                    launchWithLocalSyncWarning(
                        song = song,
                        actionLabel = favoriteActionLabel,
                        warnForLocalSync = willFav
                    ) {
                        favOverride = willFav
                        PlayerManager.toggleCurrentFavorite()
                    }
                },
                enabled = localPlaylistsReady,
                modifier = Modifier.size(lyricsTopActionButtonSize)
                    .then(
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    rememberSharedContentState(key = "btn_favorite"),
                                    animatedVisibilityScope = animatedContentScope
                                ).zIndex(1f)
                            }
                        } else Modifier
                    )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (isFavorite) stringResource(R.string.lyrics_favorited) else stringResource(R.string.lyrics_favorite),
                    modifier = Modifier.size(lyricsTopActionIconSize),
                    tint = if (isFavorite) {
                        MeloxSystemColors.Blue
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // 更多按钮
            var showMoreOptions by remember { mutableStateOf(false) }
            HapticIconButton(
                onClick = { showMoreOptions = true },
                modifier = Modifier.size(lyricsTopActionButtonSize)
                    .then(
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    rememberSharedContentState(key = "btn_more"),
                                    animatedVisibilityScope = animatedContentScope,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None,
                                ).zIndex(1f)
                            }
                        } else Modifier
                    )
            ) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = stringResource(R.string.lyrics_more_options),
                    modifier = Modifier.size(lyricsTopActionIconSize)
                )
            }
            if (showMoreOptions && currentSong != null) {
                val queue by PlayerManager.currentQueueFlow.collectAsState()
                val displayedQueue = remember(queue) { queue }
                val nowPlayingViewModel: com.lyra.player.ui.viewmodel.NowPlayingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                MoreOptionsSheet(
                    viewModel = nowPlayingViewModel,
                    originalSong = currentSong!!,
                    queue = displayedQueue,
                    displayedLyrics = lyrics,
                    displayedTranslatedLyrics = translatedLyrics.orEmpty(),
                    hasPhoneticLyrics = effectivePhoneticLyrics.isNotEmpty(),
                    onDismiss = { showMoreOptions = false },
                    onShowSongDetails = { detailSong = it },
                    onEnterAlbum = onEnterAlbum,
                    onNavigateUp = onExitNowPlaying,
                    snackbarHostState = snackbarHostState,
                    lyricFontScalePage = LyricFontScalePage.LYRICS,
                    lyricFontScales = lyricFontScales,
                    onLyricFontScaleChange = onLyricFontScaleChange
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 歌词区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(lyricsWidthFraction)
                .widthIn(max = 860.dp)
        ) {
            LyricsContentPane(
                lyrics = lyrics,
                plainLyrics = plainLyrics,
                plainTranslatedLyrics = plainTranslatedLyrics,
                translatedLyrics = translatedLyrics.orEmpty(),
                phoneticLyrics = effectivePhoneticLyrics,
                playbackSessionKey = currentSong?.stableKey(),
                previewPositionOverrideMs = previewPositionOverrideMs,
                advancedLyricsEnabled = advancedLyricsEnabled,
                showLyricTranslation = showLyricTranslation,
                lyricTranslationUsePhonetic = lyricTranslationUsePhonetic,
                lyricFontScale = lyricFontScale,
                translationFontScale = translationFontScale,
                lyricOffsetMs = lyricOffsetMs,
                lyricBlurEnabled = lyricBlurEnabled,
                lyricBlurAmount = lyricBlurAmount,
                textColor = MaterialTheme.colorScheme.onBackground,
                rawLyrics = rawLyrics,
                rawTranslatedLyrics = rawTranslatedLyrics,
                playbackSpeed = lyricsPlaybackSoundState.speed,
                isPlaying = isPlaying,
                lowPowerRendering = lowPowerLyricsRendering,
                useTabletLayout = isTabletLandscape,
                onLyricLongClick = { line -> lyricShareInitialLine = line },
                onSeekTo = onSeekTo
            )
        }

        // 底部控件 - 使用共享元素动画
        Column(
            modifier = Modifier
                .fillMaxWidth(controlWidthFraction)
                .widthIn(max = 980.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    horizontal = if (isTabletLandscape) 8.dp else 20.dp,
                    vertical = if (isTabletLandscape) 6.dp else 10.dp
                )
        ) {
            // 进度条
            LyricsProgressSection(
                songKey = currentSong?.stableKey(),
                durationMs = durationMs,
                lyrics = plainLyrics,
                lyricOffsetMs = lyricOffsetMs,
                isPlaying = isPlaying,
                isPlaybackWaiting = isPlaybackWaiting,
                playbackSpeed = lyricsPlaybackSoundState.speed,
                onSeekTo = onSeekTo,
                seekEnabled = progressSeekEnabled,
                onPreviewPositionChange = { previewPositionOverrideMs = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    rememberSharedContentState(
                                        key = NowPlayingLyricsSharedTransitionElement.PROGRESS.key
                                    ),
                                    animatedVisibilityScope = animatedContentScope
                                ).zIndex(1f)
                            }
                        } else Modifier
                    )
            )

            Spacer(modifier = Modifier.height(19.dp))

            // 播放控制按钮（参考播放器口径：82dp 走带行、字形成在背景上）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lyricsMainControlRowHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HapticIconButton(onClick = { PlayerManager.previous() },
                    modifier = Modifier
                    .then(
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    rememberSharedContentState(
                                        key = NowPlayingLyricsSharedTransitionElement.PREVIOUS.key
                                    ),
                                    animatedVisibilityScope = animatedContentScope
                                )
                            }
                        } else Modifier
                    )
                    .size(lyricsMainButtonSize)
                ) {
                    Icon(
                        Icons.Outlined.SkipPrevious,
                        contentDescription = stringResource(R.string.lyrics_previous),
                        modifier = Modifier.size(lyricsMainIconSize),
                        tint = Color.White
                    )
                }

                HapticIconButton(
                    onClick = { PlayerManager.togglePlayPause() },
                    enabled = !usbPlaybackPreparing,
                    modifier = Modifier
                        .then(
                            if (sharedTransitionScope != null && animatedContentScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        rememberSharedContentState(
                                            key = NowPlayingLyricsSharedTransitionElement.PLAY.key
                                        ),
                                        animatedVisibilityScope = animatedContentScope
                                    )
                                }
                            } else Modifier
                        )
                        .size(lyricsMainButtonSize)
                ) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        PlaybackControlIndicator(
                            isPlaying = isPlaybackControlPlaying,
                            isPlaybackWaiting = isPlaybackWaiting,
                            isAudioRouteMuted = isAudioRouteMuted,
                            playContentDescription = stringResource(R.string.lyrics_play),
                            pauseContentDescription = stringResource(R.string.lyrics_pause),
                            restoreVolumeContentDescription = stringResource(R.string.player_restore_volume),
                            waitingContentDescription = stringResource(R.string.player_waiting),
                            modifier = Modifier.size(lyricsPlayIndicatorSize),
                            progressIndicatorSize = lyricsPlayIndicatorSize
                        )
                    }
                }

                HapticIconButton(onClick = { PlayerManager.next() },
                    modifier = Modifier
                        .then(
                            if (sharedTransitionScope != null && animatedContentScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        rememberSharedContentState(
                                            key = NowPlayingLyricsSharedTransitionElement.NEXT.key
                                        ),
                                        animatedVisibilityScope = animatedContentScope
                                    )
                                }
                            } else Modifier
                        )
                        .size(lyricsMainButtonSize)
                ) {
                    Icon(
                        Icons.Outlined.SkipNext,
                        contentDescription = stringResource(R.string.lyrics_next),
                        modifier = Modifier.size(lyricsMainIconSize),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 底部操作栏 (固定在底部, 与 NowPlayingScreen 完全一致)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(toolbarWidthFraction)
                .widthIn(max = 720.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            val toolbarLayout = resolvePlaybackActionToolbarLayout(
                availableWidth = maxWidth,
                preferredHorizontalPadding = if (isTabletLandscape) 18.dp else 16.dp,
                defaultIconSize = toolbarIconSize,
                preferredMinimumTouchTarget = toolbarMinimumTouchTarget
            )
            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides
                    toolbarLayout.minimumInteractiveComponentSize
            ) {
                val meloxToolbarIconSize =
                    if (isTabletLandscape) toolbarLayout.iconSize
                    else lyricsControlSize.scaleIconSize(22.dp)
                val meloxIdleGlyphColor = Color.White.copy(alpha = 0.72f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isTabletLandscape) Modifier
                            else Modifier.height(72.dp)
                        )
                        .clip(RoundedCornerShape(if (isTabletLandscape) 30.dp else 0.dp))
                        .background(
                            if (isTabletLandscape) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(
                            horizontal = if (isTabletLandscape) {
                                toolbarLayout.horizontalPadding
                            } else {
                                // 与播放页/队列页同一条基线，三键中心约在 104/212/319dp。
                                60.dp
                            },
                            vertical = 8.dp
                        ),
                    horizontalArrangement = if (toolbarLayout.useEqualWidthSlots) {
                        Arrangement.Start
                    } else {
                        Arrangement.SpaceBetween
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val toolbarActionModifier = if (toolbarLayout.useEqualWidthSlots) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                    }
            // 歌词（最左，当前页选中：白色圆盘 + 深色歌词气泡）
            HapticIconButton(onClick = onNavigateBack,
                modifier = toolbarActionModifier.then(
                if (sharedTransitionScope != null && animatedContentScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            rememberSharedContentState(key = "btn_lyrics"),
                            animatedVisibilityScope = animatedContentScope,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ).zIndex(1f)
                    }
                } else Modifier
            )) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.88f))
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = stringResource(R.string.lyrics_back_to_cover),
                        tint = Color.Black.copy(alpha = 0.82f),
                        modifier = Modifier.size(meloxToolbarIconSize)
                    )
                }
            }

            // 投屏 / 输出设备（中间，与播放页同图标）
            HapticIconButton(onClick = onOpenOutputPicker,
                modifier = toolbarActionModifier.then(
                    if (sharedTransitionScope != null && animatedContentScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "btn_volume"),
                                animatedVisibilityScope = animatedContentScope,
                                enter = EnterTransition.None,
                                exit = ExitTransition.None,
                            ).zIndex(1f)
                        }
                    } else Modifier
                )) {
                Icon(
                    MeloxCastGlyph,
                    contentDescription = stringResource(R.string.cd_audio_device),
                    tint = meloxIdleGlyphColor,
                    modifier = Modifier.size(meloxToolbarIconSize)
                )
            }

            // 播放队列（最右，打开全屏队列页）
            HapticIconButton(onClick = onOpenQueuePage,
                modifier = toolbarActionModifier.then(
                    if (sharedTransitionScope != null && animatedContentScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "btn_queue"),
                                animatedVisibilityScope = animatedContentScope,
                                enter = EnterTransition.None,
                                exit = ExitTransition.None,
                            ).zIndex(1f)
                        }
                    } else Modifier
                )) {
                Icon(
                    Icons.Outlined.FormatListBulleted,
                    contentDescription = stringResource(R.string.lyrics_playlist),
                    tint = meloxIdleGlyphColor,
                    modifier = Modifier.size(meloxToolbarIconSize)
                )
            }

        }
            }
        }
        }

        LyraOverlaySnackbarHost(hostState = snackbarHostState)

        detailSong?.let { song ->
            LocalSongDetailsDialog(
                song = song,
                onDismiss = { detailSong = null },
                onShowMessage = { message ->
                    scope.launch {
                        snackbarHostState.showNeriSnackbar(message)
                    }
                }
            )
        }

        pendingSyncConfirmAction?.let { action ->
            LocalSongSyncConfirmDialog(
                actionLabel = pendingSyncConfirmLabel,
                onConfirm = {
                    pendingSyncConfirmAction = null
                    pendingSyncConfirmLabel = ""
                    action()
                },
                onDismiss = {
                    pendingSyncConfirmAction = null
                    pendingSyncConfirmLabel = ""
                }
            )
        }
    }
}

private const val LyricsLoadingGraceMs = 1_200L

/**
 * 歌词页状态占位（加载中 / 无歌词 / 加载失败）。
 * 轻量、居中，不出现巨大空白，也不影响播放。
 */
@Composable
private fun LyricsStatusPlaceholder(
    icon: ImageVector,
    text: String,
    onRetry: (() -> Unit)? = null,
    showProgress: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.lyrics_retry))
                }
            }
        }
    }
}

@Composable
private fun LyricsContentPane(
    lyrics: List<LyricEntry>,
    plainLyrics: List<LyricEntry>,
    plainTranslatedLyrics: List<LyricEntry>,
    translatedLyrics: List<LyricEntry>,
    phoneticLyrics: List<LyricEntry>,
    playbackSessionKey: String?,
    previewPositionOverrideMs: Long?,
    advancedLyricsEnabled: Boolean,
    showLyricTranslation: Boolean,
    lyricTranslationUsePhonetic: Boolean,
    lyricFontScale: Float,
    translationFontScale: Float,
    lyricOffsetMs: Long,
    lyricBlurEnabled: Boolean,
    lyricBlurAmount: Float,
    textColor: Color,
    rawLyrics: String?,
    rawTranslatedLyrics: String?,
    playbackSpeed: Float,
    isPlaying: Boolean,
    lowPowerRendering: Boolean,
    useTabletLayout: Boolean = false,
    lyricsError: Boolean = false,
    onRetryLyrics: (() -> Unit)? = null,
    onLyricLongClick: (LyricEntry) -> Unit,
    onSeekTo: (Long) -> Unit
) {
    var lyricsGraceElapsed by remember(playbackSessionKey) { mutableStateOf(false) }
    LaunchedEffect(lyrics.isEmpty(), playbackSessionKey) {
        if (lyrics.isEmpty()) {
            lyricsGraceElapsed = false
            delay(LyricsLoadingGraceMs)
            lyricsGraceElapsed = true
        } else {
            lyricsGraceElapsed = true
        }
    }
    when (
        resolveLyricsUiState(
            lyricsCount = lyrics.size,
            loading = isPlaying && lyrics.isEmpty() && !lyricsGraceElapsed,
            error = lyricsError
        )
    ) {
        LyricsUiState.CONTENT -> Unit
        LyricsUiState.LOADING -> {
            LyricsStatusPlaceholder(
                icon = Icons.Outlined.LibraryMusic,
                text = stringResource(R.string.lyrics_loading),
                showProgress = true
            )
            return
        }
        LyricsUiState.ERROR -> {
            LyricsStatusPlaceholder(
                icon = Icons.Outlined.ErrorOutline,
                text = stringResource(R.string.lyrics_load_failed),
                onRetry = onRetryLyrics
            )
            return
        }
        LyricsUiState.EMPTY -> {
            LyricsStatusPlaceholder(
                icon = Icons.Outlined.LibraryMusic,
                text = stringResource(R.string.lyrics_no_lyrics)
            )
            return
        }
    }

    val currentPosition by PlayerManager.playbackPositionFlow.collectAsState()
    val effectiveLyricTimeMs = previewPositionOverrideMs ?: currentPosition
    val isPreviewingSeek = previewPositionOverrideMs != null
    val shouldAnimateFromPlayback = isPlaying && !isPreviewingSeek
    val usePhoneticTranslation = showLyricTranslation &&
        lyricTranslationUsePhonetic &&
        phoneticLyrics.isNotEmpty()
    val effectivePlainTranslatedLyrics = if (usePhoneticTranslation) {
        phoneticLyrics
    } else {
        plainTranslatedLyrics
    }
    val effectiveTranslatedLyrics = if (usePhoneticTranslation) {
        phoneticLyrics
    } else {
        translatedLyrics
    }
    val effectiveRawTranslatedLyrics = if (usePhoneticTranslation) null else rawTranslatedLyrics

    if (advancedLyricsEnabled || useTabletLayout) {
        AdvancedLyricsView(
            lyrics = lyrics,
            currentTimeMs = effectiveLyricTimeMs,
            modifier = Modifier.fillMaxSize(),
            textColor = textColor,
            lyricFontScale = lyricFontScale,
            translationFontScale = translationFontScale,
            lyricOffsetMs = lyricOffsetMs,
            lyricBlurEnabled = lyricBlurEnabled,
            lyricBlurAmount = lyricBlurAmount,
            translatedLyrics = effectiveTranslatedLyrics,
            showLyricTranslation = showLyricTranslation,
            showPhoneticAsTranslation = usePhoneticTranslation,
            rawLyrics = rawLyrics,
            rawTranslatedLyrics = effectiveRawTranslatedLyrics,
            isPlaying = shouldAnimateFromPlayback,
            animateViewportScroll = isPreviewingSeek,
            playbackSpeed = playbackSpeed,
            lowPowerRendering = lowPowerRendering,
            baseFontSizeSp = if (useTabletLayout) 22f else 20f,
            offset = if (useTabletLayout) 72.dp else 48.dp,
            keepAliveZone = if (useTabletLayout) 128.dp else 108.dp,
            playedLyricViewportFraction = if (useTabletLayout) 0.36f else 0.30f,
            topFadeLength = if (useTabletLayout) 132.dp else 80.dp,
            bottomFadeLength = if (useTabletLayout) 220.dp else 196.dp,
            bottomContentInset = if (useTabletLayout) 40.dp else 0.dp,
            onLyricLongClick = onLyricLongClick,
            onSeekTo = onSeekTo
        )
        return
    }

    SyncedLyricsView(
        lyrics = plainLyrics,
        currentTimeMs = effectiveLyricTimeMs,
        modifier = Modifier.fillMaxSize(),
        textColor = textColor,
        fontSize = scaledLyricFontSize(20f, lyricFontScale).sp,
        centerPadding = 24.dp,
        visualSpec = LyricVisualSpec(
            activeScale = 1.06f,
            nearScale = 0.95f,
            farScale = 0.88f,
            inactiveBlurNear = 0.dp,
            inactiveBlurFar = 0.dp
        ),
        lyricOffsetMs = lyricOffsetMs,
        lyricBlurEnabled = lyricBlurEnabled,
        lyricBlurAmount = lyricBlurAmount,
        onLyricClick = { lyricEntry ->
            onSeekTo(lyricEntry.startTimeMs)
        },
        onLyricLongClick = onLyricLongClick,
        translatedLyrics = if (showLyricTranslation) effectivePlainTranslatedLyrics else null,
        showEmbeddedTranslations = showLyricTranslation && !usePhoneticTranslation,
        translationFontSize = scaledLyricFontSize(16f, translationFontScale).sp,
        isPlaying = shouldAnimateFromPlayback,
        playbackSpeed = playbackSpeed,
        interpolatePlaybackPosition = !lowPowerRendering,
        playbackSessionKey = playbackSessionKey,
        stableEmbeddedViewport = true
    )
}

private fun Context.isSystemPowerSaveMode(): Boolean {
    val powerManager = getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isPowerSaveMode
}

@Composable
private fun LyricsProgressSection(
    songKey: String?,
    durationMs: Long,
    lyrics: List<LyricEntry>,
    lyricOffsetMs: Long,
    isPlaying: Boolean,
    isPlaybackWaiting: Boolean,
    playbackSpeed: Float,
    onSeekTo: (Long) -> Unit,
    seekEnabled: Boolean,
    onPreviewPositionChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val delayedPlaybackWaiting = rememberDelayedPlaybackWaiting(isPlaybackWaiting)
    val context = LocalContext.current
    val currentPosition by PlayerManager.playbackPositionFlow.collectAsState()
    val latestOnPreviewPositionChange by rememberUpdatedState(onPreviewPositionChange)
    val lyricSeekHaptic = rememberLyricSeekHapticFeedback(
        lyrics = lyrics,
        lyricOffsetMs = lyricOffsetMs
    )
    var isUserDraggingSlider by remember(songKey) { mutableStateOf(false) }
    var sliderPosition by remember(songKey) {
        mutableFloatStateOf(PlayerManager.playbackPositionFlow.value.toFloat())
    }
    var pendingSeekPreviewPositionMs by remember(songKey) { mutableStateOf<Long?>(null) }
    val effectivePreviewPositionMs = resolveLyricPreviewTimeMs(
        isDraggingSlider = isUserDraggingSlider,
        sliderPreviewPositionMs = sliderPosition.toLong(),
        pendingSeekPreviewPositionMs = pendingSeekPreviewPositionMs,
        playbackPositionMs = currentPosition
    )
    val previewOverridePositionMs = remember(
        effectivePreviewPositionMs,
        isUserDraggingSlider,
        pendingSeekPreviewPositionMs
    ) {
        if (isUserDraggingSlider || pendingSeekPreviewPositionMs != null) {
            effectivePreviewPositionMs
        } else {
            null
        }
    }

    LaunchedEffect(currentPosition, isUserDraggingSlider, pendingSeekPreviewPositionMs) {
        if (!isUserDraggingSlider && pendingSeekPreviewPositionMs == null) {
            sliderPosition = currentPosition.toFloat()
        }
        val pendingPreview = pendingSeekPreviewPositionMs
        if (!isUserDraggingSlider &&
            pendingPreview != null &&
            shouldReleaseLyricSeekPreview(
                playbackPositionMs = currentPosition,
                pendingSeekPreviewPositionMs = pendingPreview
            )
        ) {
            pendingSeekPreviewPositionMs = null
        }
    }
    LaunchedEffect(previewOverridePositionMs) {
        latestOnPreviewPositionChange(previewOverridePositionMs)
    }
    DisposableEffect(Unit) {
        onDispose {
            latestOnPreviewPositionChange(null)
        }
    }

    // 与播放页同口径：52dp = 20dp 细滑条 + 26dp 时间行（左侧已播放、右侧剩余）。
    Column(
        modifier = modifier.height(52.dp),
        verticalArrangement = Arrangement.Center
    ) {
        WaveformSlider(
            modifier = Modifier.fillMaxWidth(),
            value = if (durationMs > 0) {
                effectivePreviewPositionMs.toFloat() / durationMs
            } else {
                0f
            },
            onValueChange = { newValue ->
                val previewPosition = newValue * durationMs.toFloat()
                isUserDraggingSlider = true
                sliderPosition = previewPosition
                lyricSeekHaptic.onSeekMove(previewPosition.toLong())
            },
            onValueChangeStarted = { startValue ->
                val previewPosition = startValue * durationMs.toFloat()
                isUserDraggingSlider = true
                sliderPosition = previewPosition
                lyricSeekHaptic.onSeekStart(previewPosition.toLong())
                context.performHapticFeedback(HapticFeedbackEffect.Click)
            },
            onValueChangeFinished = {
                val previewTarget = sliderPosition.toLong()
                pendingSeekPreviewPositionMs = previewTarget
                onSeekTo(previewTarget)
                isUserDraggingSlider = false
                lyricSeekHaptic.onSeekEnd()
                context.performHapticFeedback(HapticFeedbackEffect.Confirm)
            },
            onValueChangeCanceled = {
                sliderPosition = currentPosition.toFloat()
                pendingSeekPreviewPositionMs = null
                isUserDraggingSlider = false
                lyricSeekHaptic.onSeekEnd()
            },
            isPlaying = isPlaying,
            enabled = seekEnabled,
            isPlaybackWaiting = delayedPlaybackWaiting,
            isProgressStalled = isPlaybackWaiting,
            isProgressPreviewing = isUserDraggingSlider ||
                pendingSeekPreviewPositionMs != null,
            durationMs = durationMs,
            playbackSpeed = playbackSpeed,
            playbackSessionKey = songKey,
            meloxStyle = true
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            Text(
                text = formatDuration(effectivePreviewPositionMs),
                modifier = Modifier.align(Alignment.CenterStart),
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "-" + formatDuration(
                    (durationMs - effectivePreviewPositionMs).coerceAtLeast(0L)
                ),
                modifier = Modifier.align(Alignment.CenterEnd),
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
