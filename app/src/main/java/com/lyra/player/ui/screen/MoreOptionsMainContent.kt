package com.lyra.player.ui.screen

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import com.lyra.player.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.ui.feedback.showNeriSnackbar
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.download.DownloadStatus
import com.lyra.player.core.download.DownloadTask
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.core.download.isDownloadTaskCancellable
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.download.AudioDownloadManager
import com.lyra.player.core.player.model.PlaybackAudioInfo
import com.lyra.player.data.local.media.LocalMediaSupport
import com.lyra.player.data.local.media.isLocalSong
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.data.model.stableKey
import com.lyra.player.data.stats.TrackStat
import com.lyra.player.ui.component.sheet.bottomSheetScrollGuard
import com.lyra.player.ui.haptic.HapticTextButton
import com.lyra.player.ui.melox.components.MeloxIosGroupedList
import com.lyra.player.ui.melox.components.MeloxIosListIcon
import com.lyra.player.ui.melox.components.MeloxIosListRow
import com.lyra.player.ui.viewmodel.NowPlayingViewModel
import com.lyra.player.ui.viewmodel.album.isNeteaseAlbumNavigationSource
import com.lyra.player.ui.viewmodel.album.neteaseAlbumDisplayName
import com.lyra.player.ui.viewmodel.album.resolveNeteaseAlbum
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.util.media.buildRemoteSongShareUrl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun MoreOptionsMainContent(
    viewModel: NowPlayingViewModel,
    originalSong: SongItem,
    queue: List<SongItem>,
    isLocalSong: Boolean,
    lyricFontScale: Float,
    translationFontScale: Float,
    currentPlaybackAudioInfo: PlaybackAudioInfo?,
    isDismissing: Boolean,
    snackbarHostState: SnackbarHostState,
    onOpenSearch: () -> Unit,
    onOpenEditInfo: () -> Unit,
    onOpenPlaybackSound: () -> Unit,
    onOpenLyricBehavior: () -> Unit,
    onOpenFontSize: () -> Unit,
    onOpenBiliVideoSkip: () -> Unit,
    onShowSongDetails: () -> Unit,
    onShowQualitySwitch: () -> Unit,
    onEnterAlbum: (AlbumSummary) -> Unit,
    onDismissSheet: (() -> Unit) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .bottomSheetScrollGuard { scrollState.value == 0 }
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetadataAndPlaybackActions(
            audioInfo = currentPlaybackAudioInfo,
            isDismissing = isDismissing,
            onOpenSearch = onOpenSearch,
            onOpenEditInfo = onOpenEditInfo,
            onOpenPlaybackSound = onOpenPlaybackSound,
            onShowQualitySwitch = onShowQualitySwitch
        )
        DownloadOrDetailsAction(
            viewModel = viewModel,
            song = originalSong,
            isLocalSong = isLocalSong,
            onShowSongDetails = onShowSongDetails
        )
        LyricsAndAlbumActions(
            song = originalSong,
            lyricFontScale = lyricFontScale,
            translationFontScale = translationFontScale,
            onOpenLyricBehavior = onOpenLyricBehavior,
            onOpenFontSize = onOpenFontSize,
            onEnterAlbum = onEnterAlbum,
            snackbarHostState = snackbarHostState
        )
        if (PlayerManager.isBiliTrack(originalSong)) {
            MeloxIosGroupedList {
                MeloxIosListRow(
                    title = stringResource(R.string.bili_video_skip_manage),
                    leading = { MeloxIosListIcon(Icons.Outlined.SkipNext) },
                    showTopSeparator = false,
                    onClick = onOpenBiliVideoSkip
                )
            }
        }
        ShareSongAction(
            song = originalSong,
            queue = queue,
            snackbarHostState = snackbarHostState,
            onDismissSheet = onDismissSheet
        )
        PlaybackStatsAction(originalSong)
    }
}

@Composable
private fun MetadataAndPlaybackActions(
    audioInfo: PlaybackAudioInfo?,
    isDismissing: Boolean,
    onOpenSearch: () -> Unit,
    onOpenEditInfo: () -> Unit,
    onOpenPlaybackSound: () -> Unit,
    onShowQualitySwitch: () -> Unit
) {
    MeloxIosGroupedList {
        MeloxIosListRow(
            title = stringResource(R.string.music_get_info),
            leading = { MeloxIosListIcon(Icons.Outlined.Info) },
            showTopSeparator = false,
            onClick = if (isDismissing) null else onOpenSearch
        )
        MeloxIosListRow(
            title = stringResource(R.string.music_edit_info),
            leading = { MeloxIosListIcon(Icons.Outlined.Edit) },
            onClick = onOpenEditInfo
        )
        if (audioInfo?.qualityOptions.orEmpty().size > 1) {
            MeloxIosListRow(
                title = stringResource(R.string.nowplaying_quality_switch_title),
                leading = { MeloxIosListIcon(Icons.Outlined.MusicNote) },
                detail = audioInfo?.qualityLabel?.takeIf { it.isNotBlank() },
                onClick = onShowQualitySwitch
            )
        }
        MeloxIosListRow(
            title = stringResource(R.string.nowplaying_audio_effects_title),
            subtitle = stringResource(R.string.nowplaying_audio_effects_desc),
            leading = { MeloxIosListIcon(Icons.Outlined.Tune) },
            onClick = onOpenPlaybackSound
        )
    }
}

@Composable
private fun DownloadOrDetailsAction(
    viewModel: NowPlayingViewModel,
    song: SongItem,
    isLocalSong: Boolean,
    onShowSongDetails: () -> Unit
) {
    if (isLocalSong) {
        MeloxIosGroupedList {
            MeloxIosListRow(
                title = stringResource(R.string.local_song_open_details),
                leading = { MeloxIosListIcon(Icons.Outlined.Info) },
                showTopSeparator = false,
                onClick = onShowSongDetails
            )
        }
        return
    }

    val context = LocalContext.current
    val downloadPresenceVersion by GlobalDownloadManager.downloadPresenceVersion
        .collectAsStateWithLifecycle()
    val hasLocalDownload = remember(downloadPresenceVersion, song) {
        hasCachedLocalDownload(song)
    }
    val downloadSongKey = remember(song) { song.stableKey() }
    val currentTaskFlow = remember(downloadSongKey) {
        GlobalDownloadManager.downloadTasks
            .map { tasks -> tasks.firstOrNull { it.song.stableKey() == downloadSongKey } }
            .distinctUntilChanged()
    }
    val currentTask by currentTaskFlow.collectAsStateWithLifecycle(initialValue = null)
    if (shouldHideDownloadActionForSong(hasLocalDownload, currentTask)) return

    val canCancel = remember(currentTask) { isDownloadTaskCancellable(currentTask) }
    val status = currentTask?.status
    val canClick = (
        status != DownloadStatus.QUEUED &&
            status != DownloadStatus.DOWNLOADING &&
            status != DownloadStatus.WAITING_NETWORK
        ) || canCancel
    MeloxIosGroupedList {
        MeloxIosListRow(
            title = stringResource(downloadActionLabel(currentTask)),
            leading = { MeloxIosListIcon(Icons.Outlined.Download) },
            showTopSeparator = false,
            onClick = if (canClick) {
                {
                    when (currentTask?.status) {
                        DownloadStatus.QUEUED,
                        DownloadStatus.WAITING_NETWORK,
                        DownloadStatus.DOWNLOADING -> viewModel.cancelDownload(downloadSongKey)
                        DownloadStatus.CANCELLED -> viewModel.resumeDownload(context, downloadSongKey)
                        DownloadStatus.FAILED -> viewModel.retryDownload(context, song)
                        else -> viewModel.downloadSong(context, song)
                    }
                }
            } else null
        )
        if (currentTask != null) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                DownloadProgressContent(currentTask)
            }
        }
    }
}

private fun downloadActionLabel(task: DownloadTask?): Int {
    return when (task?.status) {
        DownloadStatus.QUEUED,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.WAITING_NETWORK -> R.string.download_cancel_download
        DownloadStatus.FAILED -> R.string.action_retry
        else -> R.string.download_to_local
    }
}

@Composable
private fun DownloadProgressContent(task: DownloadTask?) {
    val progress = task?.progress
    when {
        progress?.stage == AudioDownloadManager.DownloadStage.FINALIZING -> {
            Column {
                Text(stringResource(R.string.download_finalizing))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        progress != null -> {
            Column {
                Text(
                    stringResource(
                        R.string.download_progress_file_label,
                        progress.percentage,
                        progress.fileName
                    )
                )
                if (progress.totalBytes > 0L) {
                    LinearProgressIndicator(
                        progress = {
                            (progress.bytesRead.toFloat() / progress.totalBytes.toFloat())
                                .coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        task?.status == DownloadStatus.FAILED -> Text(stringResource(R.string.download_failed))
        task?.status == DownloadStatus.WAITING_NETWORK -> {
            Text(stringResource(R.string.download_waiting_network_recovery))
        }
    }
}

@Composable
private fun LyricsAndAlbumActions(
    song: SongItem,
    lyricFontScale: Float,
    translationFontScale: Float,
    onOpenLyricBehavior: () -> Unit,
    onOpenFontSize: () -> Unit,
    onEnterAlbum: (AlbumSummary) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    MeloxIosGroupedList {
        MeloxIosListRow(
            title = stringResource(R.string.lyrics_adjust_behavior),
            leading = { MeloxIosListIcon(Icons.Outlined.Timer) },
            showTopSeparator = false,
            onClick = onOpenLyricBehavior
        )
        MeloxIosListRow(
            title = stringResource(R.string.lyrics_font_size),
            leading = { MeloxIosListIcon(Icons.Outlined.FormatSize) },
            detail = stringResource(
                R.string.settings_lyrics_font_scale_pair_value,
                (lyricFontScale * 100).roundToInt(),
                (translationFontScale * 100).roundToInt()
            ),
            onClick = onOpenFontSize
        )
    }
    if (!isNeteaseAlbumNavigationSource(song)) return

    val albumName = neteaseAlbumDisplayName(song)
    val context = LocalContext.current
    val composeResources = LocalResources.current
    var albumResolveRequest by remember(song) { mutableIntStateOf(0) }
    var resolvingAlbum by remember(song) { mutableStateOf(false) }

    LaunchedEffect(song, albumResolveRequest) {
        if (albumResolveRequest == 0) return@LaunchedEffect
        val album = try {
            resolveNeteaseAlbum(song)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LyraLogger.e("MoreOptionsMainContent", "解析网易云专辑失败", error)
            null
        }
        resolvingAlbum = false
        if (album != null) {
            onEnterAlbum(album)
        } else {
            snackbarHostState.showNeriSnackbar(composeResources.getString(R.string.music_get_detail_failed))
        }
    }

    MeloxIosGroupedList {
        MeloxIosListRow(
            title = stringResource(R.string.music_view_album, albumName),
            leading = {
                if (resolvingAlbum) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    MeloxIosListIcon(Icons.Outlined.LibraryMusic)
                }
            },
            showTopSeparator = false,
            onClick = if (resolvingAlbum) null else {
                {
                    resolvingAlbum = true
                    albumResolveRequest++
                }
            }
        )
    }
}

@Composable
private fun ShareSongAction(
    song: SongItem,
    queue: List<SongItem>,
    snackbarHostState: SnackbarHostState,
    onDismissSheet: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    MeloxIosGroupedList {
        MeloxIosListRow(
            title = stringResource(R.string.action_share),
            leading = { MeloxIosListIcon(Icons.Outlined.Share) },
            showTopSeparator = false,
            onClick = {
            if (song.isLocalSong()) {
                coroutineScope.launch {
                    val shared = runCatching {
                        LocalMediaSupport.shareSongFile(context, song)
                    }.getOrDefault(false)
                    if (shared) {
                        onDismissSheet {}
                    } else {
                        snackbarHostState.showNeriSnackbar(
                            composeResources.getString(R.string.local_song_share_failed)
                        )
                    }
                }
                return@MeloxIosListRow
            }

            val shareUrl = buildRemoteSongShareUrl(song, queue)
            val shareText = if (shareUrl.isNullOrBlank()) {
                "${song.displayName()} - ${song.displayArtist()}"
            } else {
                composeResources.getString(
                    R.string.nowplaying_share_song,
                    song.displayName(),
                    song.displayArtist(),
                    shareUrl,
                )
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            onDismissSheet { context.startActivity(shareIntent) }
            }
        )
    }
}

@Composable
private fun PlaybackStatsAction(song: SongItem) {
    val songKey = remember(song) { song.stableKey() }
    val trackStat by produceState<TrackStat?>(initialValue = null, songKey) {
        value = withContext(Dispatchers.IO) {
            AppContainer.playbackStatsRepo.getStatForTrack(songKey)
        }
    }
    val resolvedTrackStat = trackStat ?: return
    var showDialog by remember { mutableStateOf(false) }
    MeloxIosGroupedList {
        MeloxIosListRow(
            title = stringResource(R.string.stats_title),
            leading = { MeloxIosListIcon(Icons.Outlined.BarChart) },
            showTopSeparator = false,
            onClick = { showDialog = true }
        )
    }
    if (!showDialog) return

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val firstPlayedText = remember(resolvedTrackStat.firstPlayedAt) {
        dateFormat.format(Date(resolvedTrackStat.firstPlayedAt))
    }
    val totalListenText = remember(resolvedTrackStat.totalListenMs) {
        val totalSeconds = resolvedTrackStat.totalListenMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalSeconds}s"
        }
    }
    AlertDialog(
        onDismissRequest = { showDialog = false },
        icon = { Icon(Icons.Outlined.BarChart, null) },
        title = { Text(stringResource(R.string.stats_title)) },
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsCard(R.string.stats_song_first_played, firstPlayedText)
                StatsCard(R.string.stats_song_total_listen, totalListenText)
                StatsCard(
                    labelRes = R.string.stats_song_play_count_label,
                    value = pluralStringResource(
                        R.plurals.stats_play_count_value,
                        resolvedTrackStat.playCount,
                        resolvedTrackStat.playCount
                    )
                )
            }
        },
        confirmButton = {
            HapticTextButton(onClick = { showDialog = false }) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun StatsCard(labelRes: Int, value: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        )
    ) {
        ListItem(
            headlineContent = { Text(stringResource(labelRes)) },
            supportingContent = { Text(value) }
        )
    }
}
