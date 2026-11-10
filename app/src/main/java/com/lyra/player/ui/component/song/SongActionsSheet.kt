package com.lyra.player.ui.component.song

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.ui.component.overlay.DensityScaledModalBottomSheet
import com.lyra.player.ui.component.sheet.bottomSheetScrollGuard
import com.lyra.player.ui.melox.components.MeloxIosGroupedList
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.melox.theme.MeloxType
import com.lyra.player.ui.util.rememberSongDisplayCoverUrl
import com.lyra.player.util.media.fastScrollableImageRequest

internal data class SongActionMenuItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color = Color.Unspecified,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * 歌曲「更多」统一操作面板：底部滑动选择，按压行有遮罩反馈。
 * 全部入口共用，保证图标、行高、间距与信息展示口径一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongActionsSheet(
    visible: Boolean,
    song: SongItem,
    actions: List<SongActionMenuItem>,
    onDismissRequest: () -> Unit,
    offlineMode: Boolean = false
) {
    if (!visible) return
    val scrollState = rememberScrollState()
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    val rowBounds = remember { mutableStateMapOf<Int, Rect>() }
    DensityScaledModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .bottomSheetScrollGuard { scrollState.value == 0 }
                .verticalScroll(scrollState)
                .padding(bottom = 28.dp)
        ) {
            SongActionsHeader(song = song, offlineMode = offlineMode)
            Spacer(Modifier.height(14.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Box(Modifier.clip(MeloxShapes.largeCard)) {
                    MeloxIosGroupedList(
                        modifier = Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pointer = event.changes.firstOrNull()
                                    hoveredIndex = if (pointer == null || !pointer.pressed) {
                                        -1
                                    } else {
                                        rowBounds.entries
                                            .firstOrNull { it.value.contains(pointer.position) }
                                            ?.key ?: -1
                                    }
                                }
                            }
                        }
                    ) {
                        actions.forEachIndexed { index, item ->
                            SongActionsRow(
                                item = item,
                                showTopSeparator = index > 0,
                                hovered = index == hoveredIndex,
                                onBoundsChange = { rowBounds[index] = it },
                                onSelect = {
                                    onDismissRequest()
                                    item.onClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongActionsHeader(
    song: SongItem,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val coverUrl = rememberSongDisplayCoverUrl(song)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fastScrollableImageRequest(
                        context = context,
                        data = coverUrl,
                        sizePx = 192,
                        offlineMode = offlineMode
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.displayName(),
                style = MeloxType.body,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val artist = song.displayArtist()
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MeloxType.subheadline,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SongActionsRow(
    item: SongActionMenuItem,
    showTopSeparator: Boolean,
    hovered: Boolean,
    onBoundsChange: (Rect) -> Unit,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val highlight by animateColorAsState(
        targetValue = if (pressed || hovered) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
        } else {
            Color.Transparent
        },
        label = "song-action-highlight"
    )
    val separatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val iconTint = if (item.tint == Color.Unspecified) {
        MaterialTheme.colorScheme.primary
    } else {
        item.tint
    }
    val contentAlpha = if (item.enabled) 1f else 0.38f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .onGloballyPositioned { onBoundsChange(it.boundsInParent()) }
            .drawBehind {
                if (showTopSeparator) {
                    drawLine(
                        color = separatorColor,
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .background(highlight)
            .then(
                if (item.enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onSelect
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint.copy(alpha = iconTint.alpha * contentAlpha)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = item.label,
            style = MeloxType.body,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
            maxLines = Int.MAX_VALUE
        )
    }
}
