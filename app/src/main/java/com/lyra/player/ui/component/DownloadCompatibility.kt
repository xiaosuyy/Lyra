package com.lyra.player.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyra.player.core.download.DownloadTask
import com.lyra.player.core.player.download.AudioDownloadManager

@Composable
fun ActiveDownloadTaskList(
    tasks: List<DownloadTask>,
    modifier: Modifier = Modifier,
    maxVisibleTasks: Int = AudioDownloadManager.DEFAULT_MAX_CONCURRENT_DOWNLOADS,
    maxHeight: Dp = 320.dp
) {
    com.lyra.player.ui.component.download.ActiveDownloadTaskList(
        tasks = tasks,
        modifier = modifier,
        maxVisibleTasks = maxVisibleTasks,
        maxHeight = maxHeight
    )
}

@Composable
fun BatchDownloadManagerSheet(
    batchDownloadProgress: AudioDownloadManager.BatchDownloadProgress?,
    downloadTasks: List<DownloadTask>,
    progressSummaryText: String,
    onDismiss: () -> Unit
) {
    com.lyra.player.ui.component.download.BatchDownloadManagerSheet(
        batchDownloadProgress = batchDownloadProgress,
        downloadTasks = downloadTasks,
        progressSummaryText = progressSummaryText,
        onDismiss = onDismiss
    )
}

@Composable
internal fun SongDownloadSubtitle(
    text: String,
    downloaded: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    com.lyra.player.ui.component.download.SongDownloadSubtitle(
        text = text,
        downloaded = downloaded,
        modifier = modifier,
        contentDescription = contentDescription
    )
}
