package com.lyra.player.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.lyra.player.R
import com.lyra.player.core.download.DownloadStatus
import com.lyra.player.core.download.DownloadTask
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.core.download.isDownloadTaskCancellable
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.download.AudioDownloadManager
import com.lyra.player.core.player.model.PlaybackAudioInfo
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.stableKey
import com.lyra.player.ui.component.sheet.bottomSheetScrollGuard
import com.lyra.player.ui.melox.components.MeloxIosGroupedList
import com.lyra.player.ui.melox.components.MeloxIosListIcon
import com.lyra.player.ui.melox.components.MeloxIosListRow
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.melox.theme.MeloxType
import com.lyra.player.ui.viewmodel.NowPlayingViewModel
import kotlin.math.roundToInt

@Composable
internal fun MoreOptionsMainContent(
    viewModel: NowPlayingViewModel,
    originalSong: SongItem,
    isLocalSong: Boolean,
    lyricFontScale: Float,
    translationFontScale: Float,
    currentPlaybackAudioInfo: PlaybackAudioInfo?,
    isDismissing: Boolean,
    onOpenSearch: () -> Unit,
    onOpenEditInfo: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    onOpenPlaybackSound: () -> Unit,
    onOpenLyricBehavior: () -> Unit,
    onOpenFontSize: () -> Unit,
    onOpenBiliVideoSkip: () -> Unit,
    onShowSongDetails: () -> Unit,
    onShowQualitySwitch: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .bottomSheetScrollGuard { scrollState.value == 0 }
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActions(
            viewModel = viewModel,
            song = originalSong,
            isLocalSong = isLocalSong,
            onOpenSleepTimer = onOpenSleepTimer,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onShowSongDetails = onShowSongDetails
        )
        MetadataAndPlaybackActions(
            audioInfo = currentPlaybackAudioInfo,
            isDismissing = isDismissing,
            onOpenSearch = onOpenSearch,
            onOpenEditInfo = onOpenEditInfo,
            onOpenPlaybackSound = onOpenPlaybackSound,
            onShowQualitySwitch = onShowQualitySwitch
        )
        LyricsActions(
            lyricFontScale = lyricFontScale,
            translationFontScale = translationFontScale,
            onOpenLyricBehavior = onOpenLyricBehavior,
            onOpenFontSize = onOpenFontSize
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

private data class QuickAction(
    val labelRes: Int,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun QuickActions(
    viewModel: NowPlayingViewModel,
    song: SongItem,
    isLocalSong: Boolean,
    onOpenSleepTimer: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    onShowSongDetails: () -> Unit
) {
    val context = LocalContext.current
    val downloadPresenceVersion by GlobalDownloadManager.downloadPresenceVersion
        .collectAsStateWithLifecycle()
    val hasLocalDownload = remember(downloadPresenceVersion, song) {
        !isLocalSong && hasCachedLocalDownload(song)
    }
    val downloadSongKey = remember(song) { song.stableKey() }
    val currentTaskFlow = remember(downloadSongKey) {
        GlobalDownloadManager.downloadTasks
            .map { tasks -> tasks.firstOrNull { it.song.stableKey() == downloadSongKey } }
            .distinctUntilChanged()
    }
    val currentTask by currentTaskFlow.collectAsStateWithLifecycle(initialValue = null)

    val downloadAction = when {
        isLocalSong -> QuickAction(
            labelRes = R.string.local_song_open_details,
            icon = Icons.Outlined.Info,
            onClick = onShowSongDetails
        )
        currentTask.isActiveDownload() -> QuickAction(
            labelRes = R.string.download_cancel_download,
            icon = Icons.Outlined.Close,
            enabled = isDownloadTaskCancellable(currentTask),
            onClick = { viewModel.cancelDownload(downloadSongKey) }
        )
        currentTask?.status == DownloadStatus.FAILED -> QuickAction(
            labelRes = R.string.download_retry,
            icon = Icons.Outlined.Refresh,
            onClick = { viewModel.retryDownload(context, song) }
        )
        hasLocalDownload -> QuickAction(
            labelRes = R.string.downloaded,
            icon = Icons.Outlined.CheckCircle,
            enabled = false,
            onClick = {}
        )
        else -> QuickAction(
            labelRes = R.string.action_download,
            icon = Icons.Outlined.Download,
            onClick = {
                when (currentTask?.status) {
                    DownloadStatus.CANCELLED -> viewModel.resumeDownload(context, downloadSongKey)
                    else -> viewModel.downloadSong(context, song)
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                action = QuickAction(
                    labelRes = R.string.nowplaying_quick_sleep_timer,
                    icon = Icons.Outlined.Timer,
                    onClick = onOpenSleepTimer
                ),
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                action = downloadAction,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                action = QuickAction(
                    labelRes = R.string.playlist_add_to,
                    icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    onClick = onOpenAddToPlaylist
                ),
                modifier = Modifier.weight(1f)
            )
        }
        if (hasVisibleDownloadProgress(currentTask)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.045f),
                        MeloxShapes.card
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                DownloadProgressContent(currentTask)
            }
        }
    }
}

private fun DownloadTask?.isActiveDownload(): Boolean {
    return this?.status == DownloadStatus.QUEUED ||
        this?.status == DownloadStatus.DOWNLOADING ||
        this?.status == DownloadStatus.WAITING_NETWORK
}

private fun hasVisibleDownloadProgress(task: DownloadTask?): Boolean {
    return task != null && (
        task.progress != null ||
            task.status == DownloadStatus.FAILED ||
            task.status == DownloadStatus.WAITING_NETWORK
        )
}

@Composable
private fun QuickActionTile(
    action: QuickAction,
    modifier: Modifier = Modifier
) {
    val contentColor = if (action.enabled) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    }
    Column(
        modifier = modifier
            .height(84.dp)
            .background(
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.045f),
                MeloxShapes.card
            )
            .then(
                if (action.enabled) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = action.onClick
                    )
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = if (action.enabled) MaterialTheme.colorScheme.primary else contentColor
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(action.labelRes),
            style = MeloxType.caption,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
private fun LyricsActions(
    lyricFontScale: Float,
    translationFontScale: Float,
    onOpenLyricBehavior: () -> Unit,
    onOpenFontSize: () -> Unit
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
}
