package com.lyra.player.ui.component

import androidx.compose.runtime.Composable
import com.lyra.player.data.model.SongItem

@Composable
fun LocalSongDetailsDialog(
    song: SongItem,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    com.lyra.player.ui.component.local.LocalSongDetailsDialog(
        song = song,
        onDismiss = onDismiss,
        onShowMessage = onShowMessage
    )
}

@Composable
fun LocalSongSyncConfirmDialog(
    actionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    com.lyra.player.ui.component.local.LocalSongSyncConfirmDialog(
        actionLabel = actionLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
