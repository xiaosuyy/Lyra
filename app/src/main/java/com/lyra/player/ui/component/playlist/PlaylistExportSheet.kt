package com.lyra.player.ui.component.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.lyra.player.ui.component.overlay.DensityScaledModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.lyra.player.R
import com.lyra.player.data.local.playlist.model.LocalPlaylist
import com.lyra.player.ui.component.sheet.bottomSheetScrollGuard
import com.lyra.player.ui.haptic.performHapticFeedback
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsTextButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsTextField
import com.lyra.player.ui.util.rememberPlaylistDisplayCoverUrl
import com.lyra.player.util.media.fastScrollableImageRequest
import java.util.concurrent.atomic.AtomicBoolean

private class PendingPlaylistExport(
    val targetName: String,
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistExportSheet(
    title: String,
    playlists: List<LocalPlaylist>,
    selectedCount: Int,
    onDismissRequest: () -> Unit,
    onCreateAndExport: (String) -> Unit,
    onExportToPlaylist: (LocalPlaylist) -> Unit,
    createActionLabel: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playlistListState = rememberLazyListState()
    var newName by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<PendingPlaylistExport?>(null) }
    val resolvedCreateActionLabel =
        createActionLabel ?: stringResource(R.string.playlist_create_and_export)

    fun clearPendingExport() {
        pendingExport = null
    }

    fun dismissAnimated() {
        clearPendingExport()
        scope.launch {
            runCatching { sheetState.hide() }
            onDismissRequest()
        }
    }

    fun runThenDismiss(action: () -> Unit) {
        action()
        dismissAnimated()
    }

    fun requestExportConfirmation(targetName: String, action: () -> Unit) {
        if (pendingExport != null) return
        pendingExport = PendingPlaylistExport(targetName) {
            runThenDismiss(action)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { dismissAnimated() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .bottomSheetScrollGuard()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.common_selected_count,
                        selectedCount,
                        selectedCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                MiuixSettingsTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text(stringResource(R.string.playlist_create_name)) },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MiuixSettingsButton(
                        enabled = newName.isNotBlank() && selectedCount > 0,
                        onClick = {
                            val name = newName.trim()
                            if (name.isBlank()) return@MiuixSettingsButton
                            requestExportConfirmation(name) { onCreateAndExport(name) }
                        }
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(resolvedCreateActionLabel)
                    }
                }

                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                )
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                state = playlistListState,
                modifier = Modifier.playlistExportListHeight()
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistExportRow(
                        playlist = playlist,
                        enabled = pendingExport == null,
                        onClick = {
                            context.performHapticFeedback()
                            requestExportConfirmation(playlist.name) {
                                onExportToPlaylist(playlist)
                            }
                        }
                    )
                }
            }
        }
    }

    pendingExport?.let { export ->
        MiuixSettingsDialog(
            onDismissRequest = {
                clearPendingExport()
            },
            title = { Text(stringResource(R.string.playlist_batch_export_confirm_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.playlist_batch_export_confirm_message,
                        selectedCount,
                        selectedCount,
                        export.targetName
                    )
                )
            },
            confirmButton = {
                MiuixSettingsButton(
                    onClick = {
                        val action = export.onConfirm
                        clearPendingExport()
                        action()
                    }
                ) {
                    Text(stringResource(R.string.playlist_batch_export_confirm_button))
                }
            },
            dismissButton = {
                MiuixSettingsTextButton(
                    onClick = {
                        clearPendingExport()
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun PlaylistExportRow(
    playlist: LocalPlaylist,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val cover = rememberPlaylistDisplayCoverUrl(
        playlist = playlist,
        resolveLocalFallback = true
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .playlistExportRowClick(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (!cover.isNullOrBlank()) {
                    AsyncImage(
                        model = fastScrollableImageRequest(
                            context = context,
                            data = cover,
                            sizePx = 160,
                            offlineMode = false
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = playlist.name,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.explore_song_count,
                        playlist.songs.size,
                        playlist.songs.size
                    ),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp, end = 18.dp),
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        )
    }
}

internal fun Modifier.playlistExportRowClick(
    enabled: Boolean,
    onClick: () -> Unit
): Modifier = composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val regularClickHandled = remember { AtomicBoolean(false) }

    pointerInput(enabled) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial
            )
            regularClickHandled.set(false)
            val touchSlopSquared = viewConfiguration.touchSlop * viewConfiguration.touchSlop
            var movedBeyondTapSlop = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val distanceX = change.position.x - down.position.x
                val distanceY = change.position.y - down.position.y
                if (distanceX * distanceX + distanceY * distanceY > touchSlopSquared) {
                    movedBeyondTapSlop = true
                }
                if (!change.pressed) {
                    if (enabled && !movedBeyondTapSlop && !regularClickHandled.get()) {
                        currentOnClick()
                    }
                    break
                }
            }
        }
    }.clickable(
        enabled = enabled,
        onClick = {
            regularClickHandled.set(true)
            currentOnClick()
        }
    )
}
